package com.example.watacrab;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;

public class WataCrabApplication extends Application {
    private static final String TAG = "WataCrabApplication";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    
    // Kênh thông báo mặc định
    public static final String DEFAULT_NOTIFICATION_CHANNEL_ID = "water_reminder_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Create notification channels immediately
        createNotificationChannels();
        
        // Check for Google Play Services
        if (checkGooglePlayServices()) {
            // Initialize Firebase
            try {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
            }
        }
    }

    private boolean checkGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Log.e(TAG, "Google Play Services is not available: " + 
                    apiAvailability.getErrorString(resultCode));
            } else {
                Log.e(TAG, "This device is not supported by Google Play Services");
            }
            return false;
        }
        
        Log.d(TAG, "Google Play Services is available");
        return true;
    }
    
    /**
     * Tạo tất cả các kênh thông báo cần thiết cho ứng dụng
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channels");
            
            // Kênh thông báo cho các lời nhắc uống nước
            NotificationChannel reminderChannel = new NotificationChannel(
                    DEFAULT_NOTIFICATION_CHANNEL_ID,
                    "Lời nhắc uống nước",
                    NotificationManager.IMPORTANCE_HIGH);
            
            reminderChannel.setDescription("Thông báo nhắc nhở uống nước");
            reminderChannel.enableVibration(true);
            reminderChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            reminderChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            reminderChannel.setBypassDnd(true);
            reminderChannel.setShowBadge(true);
            
            // Đăng ký kênh với hệ thống
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(reminderChannel);
                Log.d(TAG, "Notification channel created: " + DEFAULT_NOTIFICATION_CHANNEL_ID);
            } else {
                Log.e(TAG, "Could not get NotificationManager");
            }
        }
    }
    
    /**
     * Helper method để các thành phần khác có thể gửi thông báo dễ dàng
     */
    public static void sendTestNotification(Context context, String title, String message) {
        if (context == null) return;
        
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null");
            return;
        }
        
        androidx.core.app.NotificationCompat.Builder builder = 
                new androidx.core.app.NotificationCompat.Builder(context, DEFAULT_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_water)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        
        notificationManager.notify(9999, builder.build());
        Log.d(TAG, "Test notification sent: " + title);
    }
} 