package com.example.watacrab.adapters;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watacrab.R;
import com.example.watacrab.model.Reminder;
import com.example.watacrab.receivers.ReminderReceiver;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {
    private static final String TAG = "ReminderAdapter";
    private final List<Reminder> reminderList;
    private final OnReminderClickListener listener;
    private final SimpleDateFormat timeFormat;

    public ReminderAdapter(List<Reminder> reminderList, OnReminderClickListener listener) {
        this.reminderList = reminderList;
        this.listener = listener;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);
        
        holder.tvTitle.setText(reminder.getTitle());
        holder.tvTime.setText(reminder.getTime() != null ? timeFormat.format(reminder.getTime()) : "00:00");
        holder.tvRepeat.setVisibility(reminder.isDaily() ? View.VISIBLE : View.GONE);
        holder.switchActive.setChecked(reminder.isActive());
        
        // Thiết lập sự kiện click
        holder.itemView.setOnClickListener(v -> listener.onReminderClick(reminder, position));
        
        // Thiết lập sự kiện toggle switch
        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) { // Chỉ phản hồi khi người dùng thực sự thay đổi switch
                listener.onReminderToggle(reminder, isChecked);
            }
        });
        
        // Thiết lập sự kiện cho nút xóa
        holder.btnDelete.setOnClickListener(v -> {
            listener.onReminderDelete(reminder, position);
        });
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    public static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvRepeat;
        Switch switchActive;
        ImageButton btnDelete;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvReminderTitle);
            tvTime = itemView.findViewById(R.id.tvReminderTime);
            tvRepeat = itemView.findViewById(R.id.tvRepeatIndicator);
            switchActive = itemView.findViewById(R.id.switchReminderActive);
            btnDelete = itemView.findViewById(R.id.btnDeleteReminder);
        }
    }

    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder, int position);
        void onReminderToggle(Reminder reminder, boolean isActive);
        void onReminderDelete(Reminder reminder, int position);
    }

    private void updateReminderStatus(Context context, Reminder reminder, boolean isActive) {
        if (reminder.getId() == null || reminder.getId().isEmpty()) {
            Log.w(TAG, "Cannot update reminder with null ID");
            return;
        }

        // Cập nhật trong Firestore
        FirebaseFirestore.getInstance().collection("reminders")
                .document(reminder.getId())
                .update("enabled", isActive)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Reminder status updated successfully");
                    
                    // Cập nhật hoặc hủy báo thức
                    if (isActive) {
                        scheduleReminder(context, reminder);
                    } else {
                        cancelReminder(context, reminder);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating reminder status", e);
                    Toast.makeText(context, "Lỗi: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    // Khôi phục trạng thái
                    reminder.setActive(!isActive);
                    notifyDataSetChanged();
                });
    }

    private void deleteReminder(Context context, Reminder reminder, int position) {
        if (reminder.getId() == null || reminder.getId().isEmpty()) {
            Log.w(TAG, "Cannot delete reminder with null ID");
            return;
        }

        // Xóa trong Firestore
        FirebaseFirestore.getInstance().collection("reminders")
                .document(reminder.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Reminder deleted successfully");
                    
                    // Hủy báo thức
                    cancelReminder(context, reminder);
                    
                    // Xóa khỏi danh sách
                    reminderList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, getItemCount());
                    
                    listener.onReminderToggle(reminder, false);
                    
                    Toast.makeText(context, "Đã xóa lời nhắc", 
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting reminder", e);
                    Toast.makeText(context, "Lỗi: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Helper methods for scheduling reminders
    public static void scheduleReminder(Context context, Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager không khả dụng");
            return;
        }
        
        // Tạo Intent với action cụ thể để tăng độ chính xác
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.example.watacrab.ACTION_SHOW_REMINDER");
        intent.putExtra("REMINDER_ID", reminder.getId());
        intent.putExtra("REMINDER_TITLE", reminder.getTitle());
        // Thêm timestamp để đảm bảo intent luôn mới và độc nhất
        intent.putExtra("TIMESTAMP", System.currentTimeMillis());
        
        // Tạo ID duy nhất cho PendingIntent dựa trên ID của reminder
        int requestCode = reminder.getId().hashCode();
        
        // Log chi tiết để debug
        Log.d(TAG, "====== ĐẶT LỊCH BÁO THỨC MỚI ======");
        Log.d(TAG, "Đặt lịch cho lời nhắc ID: " + reminder.getId());
        Log.d(TAG, "Tiêu đề: " + reminder.getTitle());
        Log.d(TAG, "Thời gian: " + reminder.getTime());
        Log.d(TAG, "Request code: " + requestCode);
        
        PendingIntent pendingIntent;
        try {
            // Sử dụng FLAG_UPDATE_CURRENT để cập nhật nếu đã tồn tại
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } catch (Exception e) {
            Log.e(TAG, "Lỗi tạo PendingIntent: " + e.getMessage());
            return;
        }
        
        if (pendingIntent == null) {
            Log.e(TAG, "PendingIntent là null");
            return;
        }
        
        // Thiết lập thời gian
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(reminder.getTime());
        
        // Chỉ giữ giờ và phút, đặt báo thức cho ngày hôm nay
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        // Lấy thời gian hiện tại
        Calendar now = Calendar.getInstance();
        
        // Đặt thời gian vào hôm nay
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.set(Calendar.HOUR_OF_DAY, hour);
        alarmTime.set(Calendar.MINUTE, minute);
        alarmTime.set(Calendar.SECOND, 0);
        alarmTime.set(Calendar.MILLISECOND, 0);
        
        // Nếu thời gian đã qua và là lời nhắc lặp lại, đặt cho ngày tiếp theo
        if (alarmTime.before(now) && reminder.isDaily()) {
            alarmTime.add(Calendar.DAY_OF_YEAR, 1);
            Log.d(TAG, "Thời gian đã qua, đặt cho ngày mai: " + alarmTime.getTime());
        }
        
        // Nếu thời gian đã qua và không lặp lại, không thiết lập
        if (alarmTime.before(now) && !reminder.isDaily()) {
            Log.d(TAG, "Thời gian nhắc nhở đã qua và không lặp lại, bỏ qua");
            return;
        }
        
        // Tính thời gian chờ đến báo thức theo giây và phút
        long timeUntilAlarm = (alarmTime.getTimeInMillis() - now.getTimeInMillis()) / 1000;
        long minutes = timeUntilAlarm / 60;
        long seconds = timeUntilAlarm % 60;
        
        Log.d(TAG, "Thiết lập lời nhắc tại: " + alarmTime.getTime());
        Log.d(TAG, String.format("Báo thức sẽ kích hoạt sau: %d phút %d giây", minutes, seconds));
        
        try {
            // Hủy báo thức hiện tại (nếu có) để đảm bảo không bị trùng lặp
            alarmManager.cancel(pendingIntent);
            
            // Sử dụng các phương pháp khác nhau để đảm bảo báo thức hoạt động
            if (reminder.isDaily()) {
                // Đối với lời nhắc hàng ngày
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ cần quyền SCHEDULE_EXACT_ALARM
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                alarmTime.getTimeInMillis(),
                                pendingIntent
                        );
                        Log.d(TAG, "Android 12+: Báo thức chính xác đã được đặt");
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                alarmTime.getTimeInMillis(),
                                pendingIntent
                        );
                        Log.d(TAG, "Android 12+ (không có quyền EXACT): Báo thức không chính xác đã được đặt");
                    }
                    
                    // Đặt báo thức lặp lại riêng biệt (cho phép báo thức hàng ngày)
                    PendingIntent repeatingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode + 100000, // ID khác để tránh xung đột
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime.getTimeInMillis() + AlarmManager.INTERVAL_DAY, // Bắt đầu từ ngày mai
                            AlarmManager.INTERVAL_DAY,
                            repeatingIntent
                    );
                    Log.d(TAG, "Đã đặt báo thức lặp lại riêng biệt");
                    
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6.0+ nhưng dưới Android 12
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d(TAG, "Android 6.0+: Báo thức chính xác đã được đặt");
                    
                    // Sử dụng setRepeating để báo thức hàng ngày
                    PendingIntent repeatingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode + 100000, // ID khác để tránh xung đột 
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime.getTimeInMillis() + AlarmManager.INTERVAL_DAY, // Bắt đầu từ ngày mai
                            AlarmManager.INTERVAL_DAY,
                            repeatingIntent
                    );
                    Log.d(TAG, "Đã đặt báo thức lặp lại riêng biệt");
                    
                } else {
                    // Phiên bản Android cũ
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                    );
                    Log.d(TAG, "Android cũ: Đã đặt lời nhắc lặp lại hàng ngày");
                }
            } else {
                // Đối với thông báo một lần
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ cần quyền SCHEDULE_EXACT_ALARM
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                alarmTime.getTimeInMillis(),
                                pendingIntent
                        );
                        Log.d(TAG, "Android 12+: Báo thức chính xác đã được đặt (một lần)");
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                alarmTime.getTimeInMillis(),
                                pendingIntent
                        );
                        Log.d(TAG, "Android 12+ (không có quyền EXACT): Báo thức không chính xác đã được đặt (một lần)");
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Android 6.0+ nhưng dưới Android 12
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d(TAG, "Android 6.0+: Báo thức chính xác đã được đặt (một lần)");
                } else {
                    // Phiên bản Android cũ
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d(TAG, "Android cũ: Đã đặt báo thức chính xác (một lần)");
                }
            }
            
            // Đặt báo thức cho 1 phút trước thời gian chính (dự phòng)
            if (alarmTime.getTimeInMillis() - now.getTimeInMillis() > 60 * 1000) {
                Calendar backupAlarmTime = (Calendar) alarmTime.clone();
                backupAlarmTime.add(Calendar.MINUTE, -1);
                
                PendingIntent backupIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode + 200000, // ID khác để tránh xung đột
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            backupAlarmTime.getTimeInMillis(),
                            backupIntent
                    );
                    Log.d(TAG, "Đã đặt báo thức dự phòng (1 phút trước): " + backupAlarmTime.getTime());
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            backupAlarmTime.getTimeInMillis(),
                            backupIntent
                    );
                    Log.d(TAG, "Đã đặt báo thức dự phòng (1 phút trước): " + backupAlarmTime.getTime());
                }
            }
            
            // Kiểm tra xem PendingIntent có hoạt động không
            boolean pendingIntentExists = (PendingIntent.getBroadcast(context, requestCode, intent, 
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE) != null);
            Log.d(TAG, "PendingIntent tồn tại: " + pendingIntentExists);
            Log.d(TAG, "===================================");
            
            // Một số thiết bị (như Xiaomi) cần hỏi người dùng về autostart
            checkForRestrictedDevices(context, reminder.getTitle());
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi đặt báo thức: " + e.getMessage());
        }
    }
    
    /**
     * Kiểm tra các thiết bị có giới hạn đặc biệt và hiển thị hướng dẫn phù hợp
     */
    private static void checkForRestrictedDevices(Context context, String reminderTitle) {
        String manufacturer = Build.MANUFACTURER.toLowerCase(Locale.getDefault());
        
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")) {
            Toast.makeText(context, 
                    "Lời nhắc '" + reminderTitle + "' đã được đặt. Vui lòng vào Cài đặt > Ứng dụng > WataCrab > Tự khởi động để đảm bảo thông báo hoạt động.", 
                    Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "Thiết bị Xiaomi/MIUI được phát hiện, có thể cần mở 'Autostart'");
        } 
        else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            Toast.makeText(context, 
                    "Lời nhắc '" + reminderTitle + "' đã được đặt. Vui lòng vào Cài đặt > Ứng dụng > WataCrab > Ứng dụng được bảo vệ để đảm bảo thông báo hoạt động.", 
                    Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "Thiết bị Huawei/EMUI được phát hiện, có thể cần mở 'Protected apps'");
        }
        else if (manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus")) {
            Toast.makeText(context, 
                    "Lời nhắc '" + reminderTitle + "' đã được đặt. Vui lòng vào Cài đặt > Quản lý pin > Tự khởi động để đảm bảo thông báo hoạt động.", 
                    Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "Thiết bị OPPO/ColorOS được phát hiện, có thể cần mở 'Auto-launch'");
        }
        else if (manufacturer.contains("vivo")) {
            Toast.makeText(context, 
                    "Lời nhắc '" + reminderTitle + "' đã được đặt. Vui lòng vào i Manager > Quản lý App > Khởi động tự động để đảm bảo thông báo hoạt động.", 
                    Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "Thiết bị Vivo được phát hiện, có thể cần mở 'Auto-start'");
        }
        else if (manufacturer.contains("samsung")) {
            Toast.makeText(context, 
                    "Lời nhắc '" + reminderTitle + "' đã được đặt. Vui lòng vào Cài đặt > Ứng dụng > WataCrab > Pin > không tối ưu hóa để đảm bảo thông báo hoạt động.", 
                    Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "Thiết bị Samsung được phát hiện, có thể cần tắt 'Optimizing battery usage'");
        }
        else {
            Toast.makeText(context, 
                    "Lời nhắc '" + reminderTitle + "' đã được đặt lúc " + 
                    new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()), 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    // Hàm hủy lời nhắc
    public static void cancelReminder(Context context, Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager không khả dụng");
            return;
        }
        
        // Tạo Intent giống hệt lúc đặt lịch
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.example.watacrab.ACTION_SHOW_REMINDER");
        intent.putExtra("REMINDER_ID", reminder.getId());
        
        int requestCode = reminder.getId().hashCode();
        
        // Hủy pending intent chính
        try {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            
            // Hủy pending intent lặp lại (nếu có)
            PendingIntent repeatingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode + 100000,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(repeatingIntent);
            repeatingIntent.cancel();
            
            // Hủy pending intent dự phòng (nếu có)
            PendingIntent backupIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode + 200000,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(backupIntent);
            backupIntent.cancel();
            
            Log.d(TAG, "Đã hủy lời nhắc với ID: " + reminder.getId());
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi hủy lời nhắc: " + e.getMessage());
        }
    }
} 