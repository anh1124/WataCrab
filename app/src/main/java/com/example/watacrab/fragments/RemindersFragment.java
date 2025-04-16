package com.example.watacrab.fragments;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watacrab.R;
import com.example.watacrab.adapters.ReminderAdapter;
import com.example.watacrab.models.Reminder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RemindersFragment extends Fragment implements ReminderAdapter.OnReminderClickListener {

    private RecyclerView recyclerView;
    private ReminderAdapter reminderAdapter;
    private List<Reminder> reminderList;
    private TextView tvEmptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reminders, container, false);
        
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewReminders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize empty state view
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        
        // Initialize reminder list and adapter
        reminderList = new ArrayList<>();
        reminderAdapter = new ReminderAdapter(reminderList, this);
        recyclerView.setAdapter(reminderAdapter);
        
        // Setup FloatingActionButton for adding new reminders
        FloatingActionButton fabAddReminder = view.findViewById(R.id.fabAddReminder);
        fabAddReminder.setOnClickListener(v -> openAddReminderDialog());
        
        // Add some sample reminders
        loadSampleReminders();
        
        return view;
    }
    
    private void loadSampleReminders() {
        // Thêm một số mẫu nhắc nhở
        if (reminderList.isEmpty()) {
            // Tạo lời nhắc mặc định vào lúc 8:00 sáng
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 8);
            calendar.set(Calendar.MINUTE, 0);
            
            Reminder reminder = new Reminder();
            reminder.setId(UUID.randomUUID().toString());
            reminder.setTitle("Uống nước buổi sáng");
            reminder.setTime(calendar.getTime());
            reminder.setDaily(true);
            reminder.setActive(true);
            
            reminderList.add(reminder);
            
            // Tạo lời nhắc lúc 12:00 trưa
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 0);
            
            reminder = new Reminder();
            reminder.setId(UUID.randomUUID().toString());
            reminder.setTitle("Uống nước buổi trưa");
            reminder.setTime(calendar.getTime());
            reminder.setDaily(true);
            reminder.setActive(true);
            
            reminderList.add(reminder);
            
            // Tạo lời nhắc lúc 18:00 chiều
            calendar.set(Calendar.HOUR_OF_DAY, 18);
            calendar.set(Calendar.MINUTE, 0);
            
            reminder = new Reminder();
            reminder.setId(UUID.randomUUID().toString());
            reminder.setTitle("Uống nước buổi tối");
            reminder.setTime(calendar.getTime());
            reminder.setDaily(true);
            reminder.setActive(true);
            
            reminderList.add(reminder);
        }
        
        updateEmptyStateVisibility();
        reminderAdapter.notifyDataSetChanged();
    }
    
    private void updateEmptyStateVisibility() {
        if (reminderList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void openAddReminderDialog() {
        // Tạo calendar với thời gian hiện tại
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        // Hiển thị time picker
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, selectedMinute) -> {
                    // Cập nhật calendar
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    
                    // Tạo dialog nhập tiêu đề
                    showTitleInputDialog(calendar);
                },
                hour, 
                minute, 
                true);
        
        timePickerDialog.show();
    }
    
    private void showTitleInputDialog(final Calendar calendar) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Tạo lời nhắc uống nước mới");
        
        // Tạo layout để nhập tiêu đề
        View view = getLayoutInflater().inflate(R.layout.dialog_input_title, null);
        final EditText input = view.findViewById(R.id.etReminderTitle);
        input.setText("Uống nước");
        builder.setView(view);
        
        // Format time để hiển thị
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = sdf.format(calendar.getTime());
        
        builder.setMessage("Thời gian đã chọn: " + timeStr);
        
        // Thêm nút
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) {
                title = "Uống nước";
            }
            
            // Tạo reminder mới
            Reminder reminder = new Reminder();
            reminder.setId(UUID.randomUUID().toString());
            reminder.setTitle(title);
            reminder.setTime(calendar.getTime());
            reminder.setDaily(true);
            reminder.setActive(true);
            
            // Thêm vào danh sách và cập nhật adapter
            reminderList.add(reminder);
            reminderAdapter.notifyDataSetChanged();
            updateEmptyStateVisibility();
            
            Toast.makeText(getContext(), "Đã tạo lời nhắc mới", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    private void openEditReminderDialog(Reminder reminder, int position) {
        // Tạo calendar từ time của reminder
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(reminder.getTime());
        
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        // Hiển thị time picker
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, selectedMinute) -> {
                    // Cập nhật calendar
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    
                    // Tạo dialog nhập tiêu đề
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("Chỉnh sửa lời nhắc");
                    
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_input_title, null);
                    final EditText input = dialogView.findViewById(R.id.etReminderTitle);
                    input.setText(reminder.getTitle());
                    builder.setView(dialogView);
                    
                    // Format time để hiển thị
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String timeStr = sdf.format(calendar.getTime());
                    
                    builder.setMessage("Thời gian đã chọn: " + timeStr);
                    
                    // Thêm nút
                    builder.setPositiveButton("Lưu", (dialog, which) -> {
                        String title = input.getText().toString().trim();
                        if (title.isEmpty()) {
                            title = "Uống nước";
                        }
                        
                        // Cập nhật reminder
                        reminder.setTitle(title);
                        reminder.setTime(calendar.getTime());
                        
                        // Cập nhật adapter
                        reminderAdapter.notifyItemChanged(position);
                        
                        Toast.makeText(getContext(), "Đã cập nhật lời nhắc", Toast.LENGTH_SHORT).show();
                    });
                    
                    builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
                    
                    builder.setNeutralButton("Xóa", (dialog, which) -> {
                        // Xóa reminder
                        reminderList.remove(position);
                        reminderAdapter.notifyItemRemoved(position);
                        updateEmptyStateVisibility();
                        
                        Toast.makeText(getContext(), "Đã xóa lời nhắc", Toast.LENGTH_SHORT).show();
                    });
                    
                    builder.show();
                },
                hour, 
                minute, 
                true);
        
        timePickerDialog.show();
    }

    @Override
    public void onReminderClick(Reminder reminder, int position) {
        // Mở dialog chỉnh sửa khi click vào reminder
        openEditReminderDialog(reminder, position);
    }

    @Override
    public void onReminderToggle(Reminder reminder, boolean isActive) {
        // Cập nhật trạng thái bật/tắt của reminder
        reminder.setActive(isActive);
        // Không cần lưu vào database nữa
    }
} 