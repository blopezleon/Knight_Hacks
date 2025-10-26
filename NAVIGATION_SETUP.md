# Google Maps Navigation Feature - Setup Guide

## Overview
Your AR Glasses project now includes turn-by-turn navigation with Google Maps Directions API integration!

## What Was Added

### ESP32 Firmware (`esp32_ble_display.ino`)
✅ Navigation state tracking variables
✅ New message type handler for `{"type":"nav"}`
✅ `showNavigation()` function to display arrows and distance
✅ Smart notification interrupt logic (notifications show for 10s, then revert to navigation)

### Android App

#### New Files Created:
1. **NavigationActivity.kt** - Full navigation implementation with:
   - Google Maps Directions API integration
   - Turn-by-turn step parsing
   - BLE communication to ESP32
   - Auto-progression through steps (15 seconds per step)

2. **activity_navigation.xml** - Navigation UI with:
   - Destination address input
   - Start/Stop navigation buttons
   - Status display
   - Current step display

#### Updated Files:
1. **MainActivity.kt**:
   - Added navigation button
   - Made BLE objects static/shared with NavigationActivity
   - Added `sendDataToESP32()` helper method

2. **activity_main.xml**:
   - Added "Navigation" button

3. **AndroidManifest.xml**:
   - Added INTERNET permission
   - Registered NavigationActivity

## Setup Instructions

### 1. Add Your Google Maps API Key

Open `NavigationActivity.kt` and replace the placeholder on line 27:

```kotlin
private const val GOOGLE_MAPS_API_KEY = "YOUR_API_KEY_HERE"
```

Replace `"YOUR_API_KEY_HERE"` with your actual Google Maps API key.

### 2. Upload ESP32 Firmware

Upload the updated `esp32_ble_display.ino` to your ESP32.

### 3. Build and Install Android App

1. Open the project in Android Studio
2. Build the app (Ctrl/Cmd + F9)
3. Install on your phone

## How to Use

### Starting Navigation:

1. **Connect to ESP32**: Open app → "Scan & Connect"
2. **Open Navigation**: Tap "Navigation" button
3. **Enter Destination**: Type an address (e.g., "123 Main St, City, State")
4. **Start**: Tap "Start Navigation"
5. **Watch Display**: ESP32 will show arrows and distances

### Navigation Display Format:

The ESP32 will display:
```
    →
  500ft
```

- **Arrow**: Direction to turn (←, →, ↑)
- **Distance**: Distance until turn

### Navigation Features:

- **Auto-progression**: Steps advance every 15 seconds
- **Notification interrupts**: Phone notifications show for 10s, then navigation resumes
- **Stop anytime**: Tap "Stop Navigation" button

## Arrow Symbol Meanings

| Symbol | Meaning |
|--------|---------|
| ← | Turn left |
| → | Turn right |
| ↑ | Continue straight |

## Communication Protocol

Navigation messages sent to ESP32:
```json
{"type":"nav", "direction":"→", "distance":"500ft"}
```

## Troubleshooting

### "No route found"
- Check your internet connection
- Verify destination address is correct
- Make sure API key is valid

### ESP32 not showing navigation
- Verify ESP32 is connected (Status: Connected)
- Check Serial Monitor for received messages
- Re-upload ESP32 firmware if needed

### API errors
- Check API key is correct
- Verify "Directions API" is enabled in Google Cloud Console
- Check API quota hasn't been exceeded

## Advanced Customization

### Change step duration:
In `NavigationActivity.kt`, line 81:
```kotlin
handler.postDelayed(this, 15000) // Change 15000 to desired milliseconds
```

### Change notification timeout:
In `esp32_ble_display.ino`, line 227:
```cpp
if (showingNotification && (millis() - lastNotifyTime > 10000)) // Change 10000 to desired ms
```

## Notes

- Navigation uses simulated progression (time-based steps)
- For production, integrate with GPS to track actual location
- Current location is approximated in API call
- Distance shown is from Google Maps API response

---

**Your AR Glasses are now ready for navigation! 🎉**

