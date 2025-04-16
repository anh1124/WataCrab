package com.example.watacrab.receivers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
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
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    private static final String CHANNEL_ID = WataCrabApplication.DEFAULT_NOTIFICATION_CHANNEL_ID;
    private static final int NOTIFICATION_ID = 1001;
    
    // Biến dùng để theo dõi thông báo đã được hiển thị hay chưa (tránh hiển thị nhiều lần)
    private static final AtomicBoolean isProcessing = new AtomicBoolean(false);
    // Timestamp của lần thông báo cuối cùng
    private static long lastNotificationTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Sử dụng PowerManager để đảm bảo thiết bị thức dậy hoàn toàn để hiển thị thông báo
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        
        try {
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "WataCrab:ReminderWakeLock");
                wakeLock.acquire(10*60*1000L /*10 phút*/);
            }
            
            // Kiểm tra xem có quá nhiều thông báo cùng lúc
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastNotificationTime < 10000) { // Nếu cách nhau < 10 giây
                Log.d(TAG, "Bỏ qua thông báo vì quá gần với thông báo trước đó");
                return;
            }
            
            // Kiểm tra xem có đang xử lý thông báo khác không
            if (!isProcessing.compareAndSet(false, true)) {
                Log.d(TAG, "Đang xử lý thông báo khác, bỏ qua thông báo này");
                return;
            }
            
            // Đặt hẹn giờ để đảm bảo isProcessing không bị kẹt là 'true'
            new Handler().postDelayed(() -> isProcessing.set(false), 5000);
            
            // Log thời gian thực tế nhận thông báo
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String currentTimeStr = sdf.format(new Date());
            Log.d(TAG, "=========== NHẬN BÁO THỨC ===========");
            Log.d(TAG, "Thời gian nhận báo thức: " + currentTimeStr);
            
            // Kiểm tra action xem có phải là thông báo uống nước không
            String action = intent.getAction();
            if (action != null && action.equals("com.example.watacrab.ACTION_SHOW_REMINDER")) {
                Log.d(TAG, "Nhận action ACTION_SHOW_REMINDER");
            } else {
                Log.d(TAG, "Intent không có action cụ thể, xử lý mặc định");
            }
            
            // Kiểm tra thông tin hệ thống
            logSystemInfo(context);
            
            // Get reminder details from intent
            String reminderId = intent.getStringExtra("REMINDER_ID");
            String reminderTitle = intent.getStringExtra("REMINDER_TITLE");
            long timestamp = intent.getLongExtra("TIMESTAMP", 0);
            
            Log.d(TAG, "Reminder ID: " + reminderId);
            Log.d(TAG, "Reminder Title: " + reminderTitle);
            Log.d(TAG, "Reminder Timestamp: " + timestamp);
            
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
            
            // Create notification channel for Android 8.0+ (đảm bảo kênh luôn tồn tại)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager = 
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                
                NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (channel == null) {
                    Log.w(TAG, "Kênh thông báo không tồn tại, tạo mới: " + CHANNEL_ID);
                    
                    channel = new NotificationChannel(
                            CHANNEL_ID,
                            "Lời nhắc uống nước",
                            NotificationManager.IMPORTANCE_HIGH);
                    
                    channel.setDescription("Thông báo nhắc nhở uống nước");
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 500, 200, 500});
                    channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    channel.setBypassDnd(true);
                    channel.setShowBadge(true);
                    
                    notificationManager.createNotificationChannel(channel);
                }
            }
            
            // Create intent for when notification is tapped
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationIntent.putExtra("REMINDER_ID", reminderId);
            
            PendingIntent pendingIntent;
            try {
                pendingIntent = PendingIntent.getActivity(
                        context,
                        new Random().nextInt(10000), // ID ngẫu nhiên để tránh xung đột
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
            
            // Tạo nội dung thông báo
            String notificationTitle = "WataCrab - Nhắc nhở";
            String notificationContent = reminderTitle;
            if (notificationContent.equals("Uống nước")) {
                // Thêm biến đổi ngẫu nhiên để thông báo thêm sinh động
                String[] variations = {
                    "Đã đến giờ uống nước rồi!",
                    "Bạn đã uống nước chưa?",
                    "Uống nước để khỏe mạnh nhé!",
                    "Đừng quên uống nước nhé!",
                    "Uống đủ nước mỗi ngày rất tốt cho sức khỏe!"
                };
                int randomIndex = (int) (System.currentTimeMillis() % variations.length);
                notificationContent = variations[randomIndex];
            }
            
            // Gửi thông báo bằng 3 cách (để tối đa hóa khả năng thông báo sẽ hiển thị)
            try {
                // [1] Cách 1: Sử dụng NotificationManager truyền thống
                NotificationManager notificationManager = 
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                
                if (notificationManager != null) {
                    // Build the notification
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(iconResId)
                            .setContentTitle(notificationTitle)
                            .setContentText(notificationContent)
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
                
                // [2] Cách 2: Sử dụng NotificationManagerCompat
                NotificationCompat.Builder compatBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(iconResId)
                        .setContentTitle(notificationTitle + " (Compat)")
                        .setContentText(notificationContent)
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
                
                // [3] Cách 3: Sử dụng WataCrabApplication helper
                WataCrabApplication.sendTestNotification(
                        context,
                        notificationTitle + " (Helper)",
                        notificationContent
                );
                
                // Hiển thị Toast cùng lúc với thông báo (đề phòng thông báo không hiển thị)
                Toast.makeText(context, "Nhắc nhở: " + reminderTitle, Toast.LENGTH_LONG).show();
                
                // Cập nhật thời gian thông báo cuối cùng
                lastNotificationTime = currentTime;
                
            } catch (Exception e) {
                Log.e(TAG, "Error showing notification: " + e.getMessage());
                Log.e(TAG, "Stack trace: ", e);
                
                // Hiển thị Toast thay thế nếu thông báo thất bại
                try {
                    Toast.makeText(context, "Nhắc nhở: " + reminderTitle, Toast.LENGTH_LONG).show();
                } catch (Exception e2) {
                    Log.e(TAG, "Không thể hiển thị Toast: " + e2.getMessage());
                }
            } finally {
                // Đảm bảo trạng thái luôn được đặt về false
                isProcessing.set(false);
            }
            
            Log.d(TAG, "===============================");
            
        } finally {
            // Đảm bảo giải phóng wakelock
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
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
        
        // Kiểm tra chế độ tiết kiệm pin
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                boolean isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(context.getPackageName());
                Log.d(TAG, "- Ứng dụng có được bỏ qua tối ưu pin: " + isIgnoringBatteryOptimizations);
            }
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