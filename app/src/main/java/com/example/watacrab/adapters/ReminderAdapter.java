package com.example.watacrab.adapters;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watacrab.R;
import com.example.watacrab.models.Reminder;
import com.example.watacrab.receivers.ReminderReceiver;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
        holder.tvTime.setText(timeFormat.format(reminder.getTime()));
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
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    public static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvRepeat;
        Switch switchActive;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvReminderTitle);
            tvTime = itemView.findViewById(R.id.tvReminderTime);
            tvRepeat = itemView.findViewById(R.id.tvRepeatIndicator);
            switchActive = itemView.findViewById(R.id.switchReminderActive);
        }
    }

    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder, int position);
        void onReminderToggle(Reminder reminder, boolean isActive);
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

    // Hàm thiết lập lời nhắc sử dụng AlarmManager
    public static void scheduleReminder(Context context, Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("REMINDER_ID", reminder.getId());
        intent.putExtra("REMINDER_TITLE", reminder.getTitle());
        
        // Tạo ID duy nhất cho PendingIntent dựa trên ID của reminder
        int requestCode = reminder.getId().hashCode();
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Thiết lập thời gian
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(reminder.getTime());
        
        // So sánh với thời gian hiện tại
        Calendar now = Calendar.getInstance();
        
        // Nếu thời gian đã qua và là lời nhắc lặp lại, đặt cho ngày tiếp theo
        if (calendar.before(now) && reminder.isDaily()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        // Nếu thời gian đã qua và không lặp lại, không thiết lập
        if (calendar.before(now) && !reminder.isDaily()) {
            Log.d(TAG, "Thời gian nhắc nhở đã qua và không lặp lại, bỏ qua");
            return;
        }
        
        Log.d(TAG, "Thiết lập lời nhắc tại: " + calendar.getTime());
        
        if (reminder.isDaily()) {
            // Nếu lặp lại hàng ngày, sử dụng setRepeating
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        } else {
            // Nếu chỉ một lần
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }
    
    // Hàm hủy lời nhắc
    public static void cancelReminder(Context context, Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        int requestCode = reminder.getId().hashCode();
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Đã hủy lời nhắc với ID: " + reminder.getId());
    }
} 