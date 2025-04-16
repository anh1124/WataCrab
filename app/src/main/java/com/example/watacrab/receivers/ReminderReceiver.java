package com.example.watacrab.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.watacrab.R;
import com.example.watacrab.MainActivity;
import com.example.watacrab.WataCrabApplication;
import com.example.watacrab.model.Reminder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    private static final String CHANNEL_ID = WataCrabApplication.DEFAULT_NOTIFICATION_CHANNEL_ID;
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Log thời gian thực tế nhận thông báo
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        Log.d(TAG, "=========== NHẬN BÁO THỨC ===========");
        Log.d(TAG, "Thời gian nhận báo thức: " + currentTime);
        
        // Kiểm tra thông tin hệ thống
        logSystemInfo(context);
        
        // Get reminder details from intent
        String reminderId = intent.getStringExtra("REMINDER_ID");
        String reminderTitle = intent.getStringExtra("REMINDER_TITLE");
        
        Log.d(TAG, "Reminder ID: " + reminderId);
        Log.d(TAG, "Reminder Title: " + reminderTitle);
        
        if (reminderTitle == null) {
            reminderTitle = "Uống nước";
            Log.w(TAG, "Reminder title is null, using default");
        }
        
        // Kiểm tra quyền thông báo trước khi hiển thị
        boolean hasNotificationPermission = checkNotificationPermission(context);
        Log.d(TAG, "Quyền thông báo: " + (hasNotificationPermission ? "CÓ" : "KHÔNG"));
        
        if (!hasNotificationPermission) {
            Log.e(TAG, "Không có quyền hiển thị thông báo");
            // Hiển thị Toast thay thế cho thông báo
            try {
                Toast.makeText(context, "Nhắc nhở: " + reminderTitle, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, "Không thể hiển thị Toast: " + e.getMessage());
            }
            return;
        }
        
        // Create intent for when notification is tapped
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra("REMINDER_ID", reminderId);
        
        PendingIntent pendingIntent;
        try {
            pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } catch (Exception e) {
            Log.e(TAG, "Error creating pending intent: " + e.getMessage());
            return;
        }
        
        // Kiểm tra icon có tồn tại
        int iconResId = R.drawable.ic_notification_water;
        if (iconResId == 0) {
            Log.e(TAG, "Notification icon resource not found, using app icon");
            iconResId = R.mipmap.ic_launcher;
        }
        
        // Kiểm tra xem hệ thống có đang ở chế độ không làm phiền không
        boolean isDoNotDisturbEnabled = isDoNotDisturbEnabled(context);
        Log.d(TAG, "Trạng thái Không làm phiền: " + (isDoNotDisturbEnabled ? "BẬT" : "TẮT"));
        
        // Gửi thông báo bằng 2 cách (để tối đa hóa khả năng thông báo sẽ hiển thị)
        try {
            // Cách 1: Sử dụng NotificationManager truyền thống
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null");
            } else {
                // Build the notification
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(iconResId)
                        .setContentTitle("WataCrab")
                        .setContentText(reminderTitle)
                        .setPriority(NotificationCompat.PRIORITY_MAX) // Tăng ưu tiên thông báo
                        .setCategory(NotificationCompat.CATEGORY_ALARM) // Thiết lập danh mục thông báo
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Hiển thị trên màn hình khóa
                        .setAutoCancel(true)
                        .setVibrate(new long[]{0, 500, 200, 500}) // Thêm rung
                        .setDefaults(NotificationCompat.DEFAULT_ALL) // Thêm âm thanh mặc định
                        .setContentIntent(pendingIntent);
                
                // Use different notification ID for each reminder
                int notificationId = NOTIFICATION_ID;
                if (reminderId != null) {
                    notificationId = reminderId.hashCode();
                }
                
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Notification displayed with ID: " + notificationId + " (thông qua NotificationManager)");
            }
            
            // Cách 2: Sử dụng NotificationManagerCompat
            NotificationCompat.Builder compatBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(iconResId)
                    .setContentTitle("WataCrab (Compat)")
                    .setContentText(reminderTitle)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 500, 200, 500})
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent);
            
            int compatNotificationId = reminderId != null ? (reminderId.hashCode() + 1000) : (NOTIFICATION_ID + 1000);
            NotificationManagerCompat.from(context).notify(compatNotificationId, compatBuilder.build());
            Log.d(TAG, "Notification displayed with ID: " + compatNotificationId + " (thông qua NotificationManagerCompat)");
            
            // Hiển thị Toast cùng lúc với thông báo (đề phòng thông báo không hiển thị)
            Toast.makeText(context, "Nhắc nhở: " + reminderTitle, Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage());
            Log.e(TAG, "Stack trace: ", e);
            
            // Hiển thị Toast thay thế nếu thông báo thất bại
            try {
                Toast.makeText(context, "Nhắc nhở: " + reminderTitle, Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                Log.e(TAG, "Không thể hiển thị Toast: " + e2.getMessage());
            }
        }
        
        Log.d(TAG, "===============================");
    }
    
    private boolean checkNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            return notificationManager.areNotificationsEnabled();
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }
    
    private boolean isDoNotDisturbEnabled(Context context) {
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager.getCurrentInterruptionFilter() != 
                   NotificationManager.INTERRUPTION_FILTER_ALL;
        }
        return false;
    }
    
    private void logSystemInfo(Context context) {
        Log.d(TAG, "Thông tin hệ thống:");
        Log.d(TAG, "- Android phiên bản: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        Log.d(TAG, "- Thiết bị: " + Build.MANUFACTURER + " " + Build.MODEL);
        
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            Log.d(TAG, "- Phiên bản ứng dụng: " + pInfo.versionName + " (" + pInfo.versionCode + ")");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Không thể lấy thông tin phiên bản ứng dụng");
        }
        
        // Kiểm tra chế độ tiết kiệm pin
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent powerUsageIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
            boolean hasPowerUsageSummary = powerUsageIntent.resolveActivity(context.getPackageManager()) != null;
            Log.d(TAG, "- Có hỗ trợ hiển thị thông tin pin: " + hasPowerUsageSummary);
        }
        
        // Thông tin thêm cho thiết bị MIUI (Xiaomi)
        if (isMIUI()) {
            Log.d(TAG, "- Phát hiện thiết bị MIUI (Xiaomi)");
            Log.d(TAG, "- Cần kiểm tra cài đặt 'Autostart' và 'Battery saver'");
        }
        
        // Thông tin thêm cho thiết bị EMUI (Huawei)
        if (isEMUI()) {
            Log.d(TAG, "- Phát hiện thiết bị EMUI (Huawei)");
            Log.d(TAG, "- Cần kiểm tra cài đặt 'Protected apps' và 'Battery optimization'");
        }
    }
    
    private boolean isMIUI() {
        try {
            return Build.MANUFACTURER.equalsIgnoreCase("Xiaomi") ||
                   Build.MANUFACTURER.equalsIgnoreCase("Redmi");
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isEMUI() {
        try {
            return Build.MANUFACTURER.equalsIgnoreCase("HUAWEI") ||
                   Build.MANUFACTURER.equalsIgnoreCase("HONOR");
        } catch (Exception e) {
            return false;
        }
    }
} 