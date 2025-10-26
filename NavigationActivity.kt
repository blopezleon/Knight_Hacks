package com.arglasses.app

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class NavigationActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "NavigationActivity"
        const val EXTRA_GATT = "bluetooth_gatt"
        const val EXTRA_CHARACTERISTIC = "bluetooth_characteristic"
        
        // Replace with your Google Maps API key
        private const val GOOGLE_MAPS_API_KEY = "YOUR_GOOGLE_MAPS_API_KEY_HERE"
    }
    
    private lateinit var etDestination: EditText
    private lateinit var btnStartNav: Button
    private lateinit var btnStopNav: Button
    private lateinit var tvNavStatus: TextView
    private lateinit var tvCurrentStep: TextView
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null
    
    private var navigationSteps: List<NavigationStep> = emptyList()
    private var currentStepIndex = 0
    private var isNavigating = false
    
    private val handler = Handler(Looper.getMainLooper())
    private val navigationRunnable = object : Runnable {
        override fun run() {
            if (isNavigating && currentStepIndex < navigationSteps.size) {
                val step = navigationSteps[currentStepIndex]
                sendNavigationToESP32(step.direction, step.distance)
                tvCurrentStep.text = "Current Step: ${step.instruction}"
                
                // Move to next step after delay (simulate progression)
                currentStepIndex++
                if (currentStepIndex < navigationSteps.size) {
                    handler.postDelayed(this, 15000) // 15 seconds per step
                } else {
                    stopNavigation()
                    Toast.makeText(this@NavigationActivity, "Navigation complete!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
        
        // Get BLE objects from MainActivity
        bluetoothGatt = MainActivity.bluetoothGatt
        characteristic = MainActivity.bluetoothCharacteristic
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        etDestination = findViewById(R.id.etDestination)
        btnStartNav = findViewById(R.id.btnStartNav)
        btnStopNav = findViewById(R.id.btnStopNav)
        tvNavStatus = findViewById(R.id.tvNavStatus)
        tvCurrentStep = findViewById(R.id.tvCurrentStep)
    }
    
    private fun setupListeners() {
        btnStartNav.setOnClickListener {
            val destination = etDestination.text.toString().trim()
            if (destination.isEmpty()) {
                Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (bluetoothGatt == null || characteristic == null) {
                Toast.makeText(this, "Not connected to AR Glasses", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            startNavigation(destination)
        }
        
        btnStopNav.setOnClickListener {
            stopNavigation()
        }
    }
    
    private fun startNavigation(destination: String) {
        tvNavStatus.text = "Status: Fetching directions..."
        btnStartNav.isEnabled = false
        
        // Fetch directions from Google Maps API
        thread {
            try {
                val directions = fetchDirections(destination)
                runOnUiThread {
                    if (directions.isNotEmpty()) {
                        navigationSteps = directions
                        currentStepIndex = 0
                        isNavigating = true
                        
                        btnStartNav.isEnabled = false
                        btnStopNav.isEnabled = true
                        tvNavStatus.text = "Status: Navigating"
                        
                        // Start sending navigation steps
                        handler.post(navigationRunnable)
                    } else {
                        tvNavStatus.text = "Status: No route found"
                        btnStartNav.isEnabled = true
                        Toast.makeText(this, "Could not find route", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching directions", e)
                runOnUiThread {
                    tvNavStatus.text = "Status: Error"
                    btnStartNav.isEnabled = true
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun stopNavigation() {
        isNavigating = false
        handler.removeCallbacks(navigationRunnable)
        
        btnStartNav.isEnabled = true
        btnStopNav.isEnabled = false
        tvNavStatus.text = "Status: Stopped"
        tvCurrentStep.text = "Current Step: None"
        
        Toast.makeText(this, "Navigation stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun fetchDirections(destination: String): List<NavigationStep> {
        val steps = mutableListOf<NavigationStep>()
        
        try {
            // Use a default location (Orlando, FL - near UCF for hackathon)
            // Replace with actual GPS coordinates for real usage
            val origin = "University+of+Central+Florida,+Orlando,+FL"
            val encodedDestination = URLEncoder.encode(destination, "UTF-8")
            
            val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=$origin&destination=$encodedDestination&key=$GOOGLE_MAPS_API_KEY"
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            Log.d(TAG, "API Response Code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                Log.d(TAG, "API Response: $response")
                
                // Parse JSON response
                val jsonResponse = JSONObject(response)
                val status = jsonResponse.getString("status")
                
                Log.d(TAG, "Directions API Status: $status")
                
                if (status == "OK") {
                    val routes = jsonResponse.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val legs = route.getJSONArray("legs")
                        
                        for (i in 0 until legs.length()) {
                            val leg = legs.getJSONObject(i)
                            val stepsArray = leg.getJSONArray("steps")
                            
                            for (j in 0 until stepsArray.length()) {
                                val step = stepsArray.getJSONObject(j)
                                val distance = step.getJSONObject("distance").getString("text")
                                val instruction = step.getString("html_instructions")
                                    .replace(Regex("<[^>]*>"), "") // Remove HTML tags
                                
                                // Get maneuver if available
                                val maneuver = if (step.has("maneuver")) {
                                    step.getString("maneuver")
                                } else {
                                    "straight"
                                }
                                
                                // Convert maneuver to arrow
                                val direction = when {
                                    maneuver.contains("left") -> "←"
                                    maneuver.contains("right") -> "→"
                                    maneuver.contains("straight") || maneuver.contains("continue") -> "↑"
                                    else -> "↑"
                                }
                                
                                steps.add(NavigationStep(direction, distance, instruction))
                            }
                        }
                    }
                } else {
                    // API returned non-OK status
                    Log.e(TAG, "Directions API error: $status")
                    if (jsonResponse.has("error_message")) {
                        val errorMsg = jsonResponse.getString("error_message")
                        Log.e(TAG, "Error message: $errorMsg")
                    }
                }
            } else {
                Log.e(TAG, "HTTP request failed with code: $responseCode")
            }
            
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching directions", e)
            throw e
        }
        
        return steps
    }
    
    private fun sendNavigationToESP32(direction: String, distance: String) {
        val json = JSONObject().apply {
            put("type", "nav")
            put("direction", direction)
            put("distance", distance)
        }
        
        MainActivity.sendDataToESP32(json.toString())
        Log.d(TAG, "Sent navigation: $direction $distance")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopNavigation()
    }
    
    data class NavigationStep(
        val direction: String,
        val distance: String,
        val instruction: String
    )
}
