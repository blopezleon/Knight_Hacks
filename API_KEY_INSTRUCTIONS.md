# Google Maps API Key Setup

## ⚠️ IMPORTANT - API Key Not Included

For security reasons, the Google Maps API key is not included in this repository.

## To Set Up:

1. Get your own Google Maps API key from: https://console.cloud.google.com/
2. Enable the **Directions API**
3. Open `NavigationActivity.kt`
4. Replace line 30:
   ```kotlin
   private const val GOOGLE_MAPS_API_KEY = "YOUR_GOOGLE_MAPS_API_KEY_HERE"
   ```
   with your actual API key:
   ```kotlin
   private const val GOOGLE_MAPS_API_KEY = "AIzaSy..."
   ```

## Note:
- Never commit your actual API key to the repository
- Keep it in `local.properties` or environment variables
- The key has been removed for security

