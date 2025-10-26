#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <ArduinoJson.h>

// Forward declarations
void showTime(String timeStr);
void showNotification(String app, String title);

// Display configuration
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1
#define SCREEN_ADDRESS 0x3C

// BLE configuration
#define DEVICE_NAME "AR_Glasses"
#define SERVICE_UUID "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

// Global variables
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;

String currentTime = "00:00";
unsigned long lastNotifyTime = 0;
bool showingNotification = false;

// BLE Server Callbacks
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("Client connected");
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("Client disconnected");
    }
};

// BLE Characteristic Callbacks
class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      String rxValue = pCharacteristic->getValue();
      
      if (rxValue.length() > 0) {
        Serial.println("Received: " + rxValue);
        
        // Parse JSON
        StaticJsonDocument<256> doc;
        DeserializationError error = deserializeJson(doc, rxValue);
        
        if (error) {
          Serial.println("JSON parse failed: " + String(error.c_str()));
          return;
        }
        
        if (doc["type"] == "time") {
          currentTime = doc["time"].as<String>();
          showTime(currentTime);
          showingNotification = false;
          Serial.println("Time updated: " + currentTime);
        }
        else if (doc["type"] == "notify") {
          String app = doc["app"].as<String>();
          String title = doc["title"].as<String>();
          showNotification(app, title);
          lastNotifyTime = millis();
          showingNotification = true;
          Serial.println("Notification: " + app + " - " + title);
        }
      }
    }
};

// Display function to show time
void showTime(String timeStr) {
  display.clearDisplay();
  display.setTextSize(2);
  display.setTextColor(SSD1306_WHITE);

  // Get the width and height of the text
  int16_t x1, y1;
  uint16_t w, h;
  display.getTextBounds(timeStr, 0, 0, &x1, &y1, &w, &h);
  
  // Calculate cursor position to center the text
  int16_t cursorX = (display.width() - w) / 2;
  int16_t cursorY = (display.height() - h) / 2;

  display.setCursor(cursorX, cursorY);
  display.println(timeStr);
  display.display();
}

// Display function to show notification
void showNotification(String app, String title) {
  display.clearDisplay();
  display.setTextSize(2);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  
  // Line 1: App name (truncated to 12 chars)
  String appLine = app;
  if (appLine.length() > 12) {
    appLine = appLine.substring(0, 12);
  }
  display.println(appLine);
  
  // Line 2: Title first 12 chars
  if (title.length() > 0) {
    String titleLine1 = title.substring(0, min(12, (int)title.length()));
    display.println(titleLine1);
  }
  
  // Line 3: Title chars 13-24
  if (title.length() > 12) {
    String titleLine2 = title.substring(12, min(24, (int)title.length()));
    display.println(titleLine2);
  }
  
  // Line 4: Title chars 25-36
  if (title.length() > 24) {
    String titleLine3 = title.substring(24, min(36, (int)title.length()));
    display.println(titleLine3);
  }
  
  display.display();
}

void setup() {
  Serial.begin(115200);
  Serial.println("Starting AR Glasses...");
  
  // Initialize display
  if(!display.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS)) {
    Serial.println(F("SSD1306 allocation failed"));
    for(;;);
  }
  
  display.ssd1306_command(0xA0);

  display.clearDisplay();
  display.setTextSize(2);
  display.setTextColor(SSD1306_WHITE);
  display.setCursor(0, 0);
  display.println("AR_Glasses");
  display.println("Starting...");
  display.display();
  delay(2000);
  
  // Initialize BLE
  BLEDevice::init(DEVICE_NAME);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);

  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_WRITE
                    );

  pCharacteristic->setCallbacks(new MyCallbacks());

  pService->start();
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMaxPreferred(0x12);
  BLEDevice::startAdvertising();
  
  Serial.println("BLE Device initialized and advertising");
  Serial.println("Device Name: AR_Glasses");
  Serial.println("Service UUID: 4fafc201-1fb5-459e-8fcc-c5c9c331914b");
  Serial.println("Waiting for connection...");
  
  // Show initial time
  showTime("00:00");
}

void loop() {
  // Handle BLE connection state
  if (deviceConnected && !oldDeviceConnected) {
    oldDeviceConnected = deviceConnected;
    Serial.println("Client connected - ready to receive data");
  }
  
  if (!deviceConnected && oldDeviceConnected) {
    oldDeviceConnected = deviceConnected;
    Serial.println("Client disconnected - restarting advertising");
    delay(500);
    BLEDevice::startAdvertising();
  }
  
  // Check if notification should be cleared (after 10 seconds)
  if (showingNotification && (millis() - lastNotifyTime > 10000)) {
    showTime(currentTime);
    showingNotification = false;
    Serial.println("Reverted to time display");
  }
  
  delay(100);
}
