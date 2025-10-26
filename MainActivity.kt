package com.arglasses.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_PERMISSIONS = 1
        
        // BLE UUIDs (matching ESP32)
        private const val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
        private const val CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
        
        // Notification listener constants
        private const val NOTIFICATION_RECEIVED = "com.arglasses.app.NOTIFICATION_RECEIVED"
        private const val EXTRA_APP_NAME = "app_name"
        private const val EXTRA_TITLE = "title"
        
        // Shared BLE objects for NavigationActivity
        var bluetoothGatt: BluetoothGatt? = null
        var bluetoothCharacteristic: BluetoothGattCharacteristic? = null
        
        fun sendDataToESP32(data: String) {
            if (bluetoothCharacteristic != null && bluetoothGatt != null) {
                bluetoothCharacteristic?.value = data.toByteArray()
                bluetoothGatt?.writeCharacteristic(bluetoothCharacteristic)
                Log.d(TAG, "Sent data: $data")
            } else {
                Log.w(TAG, "Cannot send data - not connected")
            }
        }
    }
    
    private lateinit var btnScan: Button
    private lateinit var btnPermission: Button
    private lateinit var btnNavigation: Button
    private lateinit var btnSyncTime: Button
    private lateinit var tvStatus: TextView
    
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private val timeSyncRunnable = object : Runnable {
        override fun run() {
            syncTime()
            handler.postDelayed(this, 60000) // Run every 60 seconds
        }
    }
    
    // Broadcast receiver for notifications
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NOTIFICATION_RECEIVED) {
                val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
                val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
                
                if (appName.isNotEmpty() && title.isNotEmpty()) {
                    sendNotificationToESP32(appName, title)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        initBluetooth()
        checkPermissions()
        registerNotificationReceiver()
        
        btnScan.setOnClickListener {
            if (hasPermissions()) {
                startScanning()
            } else {
                requestPermissions()
            }
        }
        
        btnPermission.setOnClickListener {
            openNotificationSettings()
        }
        
        btnNavigation.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }
        
        btnSyncTime.setOnClickListener {
            syncTime()
            Toast.makeText(this, "Time synced!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun initViews() {
        btnScan = findViewById(R.id.btnScan)
        btnPermission = findViewById(R.id.btnPermission)
        btnNavigation = findViewById(R.id.btnNavigation)
        btnSyncTime = findViewById(R.id.btnSyncTime)
        tvStatus = findViewById(R.id.tvStatus)
    }
    
    private fun initBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }
    
    private fun checkPermissions(): Boolean {
        return hasPermissions()
    }
    
    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_PERMISSIONS
        )
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions required for BLE", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startScanning() {
        if (!hasPermissions()) {
            requestPermissions()
            return
        }
        
        tvStatus.text = "Status: Scanning..."
        bluetoothLeScanner?.startScan(scanCallback)
        
        // Stop scanning after 10 seconds
        handler.postDelayed({
            bluetoothLeScanner?.stopScan(scanCallback)
            if (bluetoothGatt == null) {
                tvStatus.text = "Status: Device not found"
            }
        }, 10000)
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name
            
            Log.d(TAG, "Found device: $deviceName")
            
            if (deviceName == "AR_Glasses") {
                bluetoothLeScanner?.stopScan(this)
                connectToDevice(device)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed: $errorCode")
            tvStatus.text = "Status: Scan failed"
        }
    }
    
    private fun connectToDevice(device: BluetoothDevice) {
        tvStatus.text = "Status: Connecting..."
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            MainActivity.bluetoothGatt = device.connectGatt(this, false, gattCallback)
        }
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    tvStatus.text = "Status: Connected"
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    tvStatus.text = "Status: Disconnected"
                    MainActivity.bluetoothGatt = null
                    MainActivity.bluetoothCharacteristic = null
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(java.util.UUID.fromString(SERVICE_UUID))
                MainActivity.bluetoothCharacteristic = service?.getCharacteristic(java.util.UUID.fromString(CHARACTERISTIC_UUID))
                
                if (MainActivity.bluetoothCharacteristic != null) {
                    Log.d(TAG, "Characteristic found")
                    // Send initial time sync
                    syncTime()
                    // Start periodic time sync
                    handler.post(timeSyncRunnable)
                } else {
                    Log.e(TAG, "Characteristic not found")
                    tvStatus.text = "Status: Service not found"
                }
            }
        }
    }
    
    private fun syncTime() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val json = JSONObject().apply {
            put("type", "time")
            put("time", currentTime)
        }
        
        sendDataToESP32(json.toString())
    }
    
    private fun sendNotificationToESP32(appName: String, title: String) {
        val json = JSONObject().apply {
            put("type", "notify")
            put("app", appName)
            put("title", title)
        }
        
        sendDataToESP32(json.toString())
    }
    
    private fun sendDataToESP32(data: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            MainActivity.sendDataToESP32(data)
        }
    }
    
    private fun registerNotificationReceiver() {
        val filter = IntentFilter(NOTIFICATION_RECEIVED)
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver, filter)
    }
    
    private fun openNotificationSettings() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        startActivity(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver)
        handler.removeCallbacks(timeSyncRunnable)
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            MainActivity.bluetoothGatt?.close()
        }
    }
}
