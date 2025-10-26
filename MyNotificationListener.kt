package com.arglasses.app

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MyNotificationListener : NotificationListenerService() {
    
    companion object {
        const val NOTIFICATION_RECEIVED = "com.arglasses.app.NOTIFICATION_RECEIVED"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_TITLE = "title"
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        sbn?.let { notification ->
            val packageName = notification.packageName
            val extras = notification.notification.extras
            
            // Extract title from notification
            val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
            
            // Get app name from package manager
            val appName = try {
                val packageManager = packageManager
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName
            }
            
            // Send broadcast to MainActivity
            val intent = Intent(NOTIFICATION_RECEIVED).apply {
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_TITLE, title)
            }
            
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Handle notification removal if needed
    }
}
