# Debug Navigation Route Issues

## ✅ What's Been Fixed:
1. ✅ Origin changed from "current+location" to "University+of+Central+Florida,+Orlando,FL"
2. ✅ API key is present: `YAIzaSyDe1_CxAvxSoPDzxPqMdm49k3wMKvg_waY`
3. ✅ Error logging added
4. ✅ File copied to Android Studio project

## 🔍 How to Debug:

### Step 1: Check Logcat
1. Open Android Studio
2. Go to **Logcat** (bottom panel)
3. Filter by: **NavigationActivity**
4. Clear old logs: Click 🗑️ (trash icon)

### Step 2: Try Navigation
1. Enter a destination like: **"Disney World, Orlando, FL"**
2. Tap **"Start Navigation"**
3. Watch Logcat for these messages:

#### What You Should See:
```
NavigationActivity: API Response Code: 200
NavigationActivity: API Response: {"routes":[...]}
NavigationActivity: Directions API Status: OK
```

#### Common Error Messages:

**If you see: "REQUEST_DENIED"**
```
NavigationActivity: Directions API Status: REQUEST_DENIED
NavigationActivity: Error message: The provided API key is invalid
```
**Fix:** Your API key might not have Directions API enabled

**If you see: "OVER_QUERY_LIMIT"**
```
NavigationActivity: Directions API Status: OVER_QUERY_LIMIT
```
**Fix:** You've hit the free tier limit

**If you see: "ZERO_RESULTS"**
```
NavigationActivity: Directions API Status: ZERO_RESULTS
```
**Fix:** Try a different destination address

**If you see: HTTP code 403**
```
NavigationActivity: HTTP request failed with code: 403
```
**Fix:** API key restrictions might be blocking the request

## 🔧 Verify Your API Key Setup:

### Go to Google Cloud Console:
1. Visit: https://console.cloud.google.com/
2. Select your project
3. Go to **APIs & Services → Enabled APIs**
4. Check if **"Directions API"** is enabled
5. If not, click **"+ ENABLE APIS AND SERVICES"**
6. Search for "Directions API"
7. Click **ENABLE**

### Check API Key Restrictions:
1. Go to **APIs & Services → Credentials**
2. Click on your API key
3. Check **Application restrictions:**
   - If set to "Android apps", make sure package name is: `com.arglasses.app`
   - Or set to "None" for testing
4. Check **API restrictions:**
   - Should include "Directions API"
   - Or set to "Don't restrict key" for testing

## 🧪 Test URLs:

### Manual Test (paste in browser):
```
https://maps.googleapis.com/maps/api/directions/json?origin=University+of+Central+Florida,+Orlando,+FL&destination=Disney+World,+Orlando,+FL&key=YAIzaSyDe1_CxAvxSoPDzxPqMdm49k3wMKvg_waY
```

**If this works in browser but not in app:**
- Check Android app restrictions on API key
- Make sure INTERNET permission is granted

## 📋 Checklist:

- [ ] Rebuilt app in Android Studio
- [ ] Checked Logcat for errors
- [ ] Verified Directions API is enabled
- [ ] Checked API key restrictions
- [ ] Tested API URL in browser
- [ ] Tried full address with city/state
- [ ] Confirmed INTERNET permission in manifest

## 💡 Quick Test Destinations:
Try these exact addresses:
1. `Disney World, Orlando, FL`
2. `Universal Studios, Orlando, FL`
3. `Orlando International Airport, FL`
4. `Downtown Orlando, FL`

---

**After trying, paste your Logcat output here and I can help debug further!**

