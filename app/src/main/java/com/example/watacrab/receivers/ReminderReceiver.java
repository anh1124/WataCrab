package com.example.watacrab.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.watacrab.MainActivity;
import com.example.watacrab.R;
import com.example.watacrab.adapters.ReminderAdapter;
import com.example.watacrab.models.Reminder;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    private static final String CHANNEL_ID = "water_reminder_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Đã nhận broadcast");
        
        String reminderId = intent.getStringExtra("REMINDER_ID");
        String title = intent.getStringExtra("REMINDER_TITLE");
        
        if (reminderId == null) {
            Log.e(TAG, "Reminder ID is null");
            return;
        }
        
        // Tạo thông báo
        createNotification(context, title != null ? title : "Nhắc nhở uống nước");
        
        // Nếu là lời nhắc một lần, hãy kiểm tra và cập nhật trạng thái trong Firestore
        FirebaseFirestore.getInstance().collection("reminders")
                .document(reminderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Reminder reminder = documentSnapshot.toObject(Reminder.class);
                        if (reminder != null && !reminder.isDaily()) {
                            // Nếu không lặp lại, vô hiệu hóa lời nhắc sau khi đã thông báo
                            reminder.setId(reminderId);
                            reminder.setActive(false);
                            
                            FirebaseFirestore.getInstance().collection("reminders")
                                    .document(reminderId)
                                    .update("active", false)
                                    .addOnSuccessListener(aVoid -> 
                                            Log.d(TAG, "Đã vô hiệu hóa lời nhắc một lần"))
                                    .addOnFailureListener(e -> 
                                            Log.e(TAG, "Lỗi khi vô hiệu hóa lời nhắc", e));
                        }
                    }
                })
                .addOnFailureListener(e -> 
                        Log.e(TAG, "Lỗi khi truy vấn thông tin lời nhắc", e));
    }
    
    private void createNotification(Context context, String title) {
        // Tạo notification channel cho Android 8.0+
        createNotificationChannel(context);
        
        // Intent để mở app khi nhấn vào thông báo
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE);
        
        // Xây dựng thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText("Đừng quên uống nước để giữ cơ thể khỏe mạnh!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        // Hiển thị thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Không có quyền hiển thị thông báo", e);
        }
    }
    
    private void createNotificationChannel(Context context) {
        // Tạo notification channel chỉ cho Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "WaterCrab Reminders";
            String description = "Notifications for water drinking reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            // Đăng ký channel
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
} 