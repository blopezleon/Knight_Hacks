# AR Glasses - BLE Notification Display

A 2-part hackathon project consisting of an Android app that sends phone notifications to an ESP32 over Bluetooth Low Energy (BLE), which displays them on a small OLED screen.

## Project Structure

### Part 1: ESP32 Firmware
- **File**: `esp32_ble_display.ino`
- **Hardware**: ESP32 with 0.96" SSD1306 128x64 I2C OLED
- **Features**:
  - BLE server with device name "AR_Glasses"
  - JSON-based communication protocol
  - Time display and notification display with text wrapping
  - Auto-revert to time display after 10 seconds

### Part 2: Android App
- **Language**: Native Kotlin
- **Features**:
  - BLE client that connects to ESP32
  - Notification listener service
  - Automatic time synchronization
  - Real-time notification forwarding

## Hardware Setup

### ESP32 Connections
- **SDA**: GPIO 21
- **SCL**: GPIO 22
- **VCC**: 3.3V
- **GND**: Ground

## Software Setup

### ESP32 (Arduino IDE)
1. Install required libraries:
   - `BLEDevice.h`
   - `Adafruit_GFX.h`
   - `Adafruit_SSD1306.h`
   - `ArduinoJson.h`
   - `Wire.h`

2. Upload `esp32_ble_display.ino` to your ESP32

### Android App
1. Open the project in Android Studio
2. Build and install on your Android device
3. Grant all required permissions when prompted
4. Enable notification access in device settings

## Usage

1. **ESP32**: Power on the device - it will start advertising as "AR_Glasses"
2. **Android**: Open the app and tap "Scan & Connect"
3. **Permissions**: Tap "Notification Permission" to enable notification access
4. **Notifications**: Any notification received on your phone will be displayed on the ESP32 screen

## Communication Protocol

The app communicates with the ESP32 using JSON messages:

### Time Sync
```json
{"type":"time", "time":"14:30"}
```

### Notification
```json
{"type":"notify", "app":"WhatsApp", "title":"New message from John"}
```

## Display Layout

The ESP32 display shows:
- **Time mode**: Current time in HH:MM format
- **Notification mode**: 
  - Line 1: App name (12 chars max)
  - Line 2-4: Notification title (wrapped to fit)

## Features

- **Automatic time sync**: Updates every 60 seconds
- **Notification capture**: Captures all phone notifications
- **Text wrapping**: Long notifications are properly wrapped
- **Auto-revert**: Returns to time display after 10 seconds
- **BLE connection management**: Handles connection/disconnection

## Troubleshooting

1. **Connection issues**: Ensure ESP32 is powered and advertising
2. **No notifications**: Check notification permission in Android settings
3. **Display issues**: Verify I2C connections and OLED address (0x3C)
4. **BLE issues**: Ensure device is within range and not connected to other devices

## Dependencies

### ESP32 Libraries
- ESP32 BLE Arduino
- Adafruit SSD1306
- Adafruit GFX Library
- ArduinoJson

### Android Dependencies
- AndroidX Core
- AndroidX AppCompat
- Material Design Components
- LocalBroadcastManager
