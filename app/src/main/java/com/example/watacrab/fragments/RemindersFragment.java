package com.example.watacrab.fragments;

import android.Manifest;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watacrab.R;
import com.example.watacrab.WataCrabApplication;
import com.example.watacrab.adapters.ReminderAdapter;
import com.example.watacrab.model.Reminder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemindersFragment extends Fragment implements ReminderAdapter.OnReminderClickListener {

    private static final String PREF_NAME = "water_reminders";
    private static final String REMINDERS_KEY = "reminders_list";
    private static final String DEFAULT_CREATED_KEY = "default_created";
    
    private RecyclerView recyclerView;
    private ReminderAdapter reminderAdapter;
    private List<Reminder> reminderList;
    private TextView tvEmptyState;
    private SharedPreferences sharedPreferences;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reminders, container, false);
        
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewReminders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize empty state view
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        
        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Get current user ID (for multiple user support)
        userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "default_user";
        
        // Initialize reminder list and adapter
        reminderList = new ArrayList<>();
        reminderAdapter = new ReminderAdapter(reminderList, this);
        recyclerView.setAdapter(reminderAdapter);
        
        // Setup FloatingActionButton for adding new reminders
        FloatingActionButton fabAddReminder = view.findViewById(R.id.fabAddReminder);
        fabAddReminder.setOnClickListener(v -> openAddReminderDialog());
        
        // Thêm một nút cho phép kiểm tra thông báo ngay lập tức
        Button btnTestNotification = new Button(requireContext());
        btnTestNotification.setText("Kiểm tra thông báo ngay");
        btnTestNotification.setBackgroundColor(getResources().getColor(R.color.primary));
        btnTestNotification.setTextColor(Color.WHITE);
        btnTestNotification.setPadding(20, 10, 20, 10);
        
        btnTestNotification.setOnClickListener(v -> {
            // Gửi thông báo test ngay lập tức
            sendTestNotification();
            
            // Gửi một thông báo khác sau 5 giây
            new Handler().postDelayed(this::sendTestNotification, 5000);
        });
        
        // Thêm nút vào layout
        ViewGroup layout = (ViewGroup) view;
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layout.addView(btnTestNotification, params);
        
        // Check and request notification permission
        checkNotificationPermission();
        
        // Load reminders from SharedPreferences
        loadRemindersFromLocal();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload reminders every time the fragment is resumed
        loadRemindersFromLocal();
        
        // Lên lịch lại các thông báo khi quay lại fragment
        rescheduleActiveReminders();
        
        // Kiểm tra các hạn chế thông báo
        checkForNotificationRestrictions();
    }
    
    private void loadRemindersFromLocal() {
        reminderList.clear();
        
        // Get saved reminders from SharedPreferences
        String remindersJson = sharedPreferences.getString(REMINDERS_KEY + "_" + userId, null);
        boolean defaultCreated = sharedPreferences.getBoolean(DEFAULT_CREATED_KEY + "_" + userId, false);
        
        if (remindersJson != null) {
            // Convert JSON to List
            Gson gson = new Gson();
            Type type = new TypeToken<List<Reminder>>() {}.getType();
            List<Reminder> savedReminders = gson.fromJson(remindersJson, type);
            
            if (savedReminders != null) {
                reminderList.addAll(savedReminders);
                
                // Chỉ tạo reminders mặc định nếu chưa từng được tạo và lưu trước đó
                if (reminderList.isEmpty() && !defaultCreated) {
                    createDefaultReminders();
                }
            } else if (!defaultCreated) {
                // Nếu chưa từng tạo reminders mặc định, tạo bây giờ
                createDefaultReminders();
            }
        } else if (!defaultCreated) {
            // Nếu chưa từng tạo reminders mặc định, tạo bây giờ
            createDefaultReminders();
        }
        
        reminderAdapter.notifyDataSetChanged();
        updateEmptyStateVisibility();
    }
    
    private void saveRemindersToLocal() {
        // Convert reminders list to JSON and save to SharedPreferences
        Gson gson = new Gson();
        String remindersJson = gson.toJson(reminderList);
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(REMINDERS_KEY + "_" + userId, remindersJson);
        editor.putBoolean(DEFAULT_CREATED_KEY + "_" + userId, true);
        editor.apply();
    }
    
    private void createDefaultReminders() {
        // Tạo lời nhắc mặc định vào lúc 8:00 sáng
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        Reminder reminder = new Reminder();
        reminder.setTitle("Uống nước buổi sáng");
        reminder.setTime(calendar.getTime());
        reminder.setDaily(true);
        reminder.setActive(true);
        reminder.setUserId(userId);
        
        reminderList.add(reminder);
        
        // Tạo lời nhắc lúc 12:00 trưa
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        
        reminder = new Reminder();
        reminder.setTitle("Uống nước buổi trưa");
        reminder.setTime(calendar.getTime());
        reminder.setDaily(true);
        reminder.setActive(true);
        reminder.setUserId(userId);
        
        reminderList.add(reminder);
        
        // Tạo lời nhắc lúc 18:00 chiều
        calendar.set(Calendar.HOUR_OF_DAY, 18);
        calendar.set(Calendar.MINUTE, 0);
        
        reminder = new Reminder();
        reminder.setTitle("Uống nước buổi tối");
        reminder.setTime(calendar.getTime());
        reminder.setDaily(true);
        reminder.setActive(true);
        reminder.setUserId(userId);
        
        reminderList.add(reminder);
        
        // Đánh dấu đã tạo reminders mặc định và lưu danh sách
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DEFAULT_CREATED_KEY + "_" + userId, true);
        editor.apply();
        
        // Lưu danh sách mặc định
        saveRemindersToLocal();
        
        // Thiết lập báo thức cho các lời nhắc mặc định
        for (Reminder r : reminderList) {
            if (r.isActive() && getContext() != null) {
                ReminderAdapter.scheduleReminder(getContext(), r);
            }
        }
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
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    
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
            reminder.setTitle(title);
            reminder.setTime(calendar.getTime());
            reminder.setDaily(true);
            reminder.setActive(true);
            reminder.setUserId(userId);
            
            // Thêm vào danh sách và lưu
            reminderList.add(reminder);
            reminderAdapter.notifyDataSetChanged();
            updateEmptyStateVisibility();
            saveRemindersToLocal();
            
            // Đặt lịch nhắc nhở
            if (getContext() != null) {
                ReminderAdapter.scheduleReminder(getContext(), reminder);
            }
            
            Toast.makeText(getContext(), "Đã tạo lời nhắc mới", Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    private void openEditReminderDialog(Reminder reminder, int position) {
        // Tạo calendar từ time của reminder
        final Calendar calendar = Calendar.getInstance();
        if (reminder.getTime() != null) {
            calendar.setTime(reminder.getTime());
        }
        
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        // Hiển thị time picker
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, selectedMinute) -> {
                    // Cập nhật calendar
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, selectedMinute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    
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
                        
                        // Cập nhật adapter và lưu
                        reminderAdapter.notifyItemChanged(position);
                        saveRemindersToLocal();
                        
                        // Cập nhật lịch nhắc nhở
                        if (reminder.isActive() && getContext() != null) {
                            ReminderAdapter.scheduleReminder(getContext(), reminder);
                        }
                        
                        Toast.makeText(getContext(), "Đã cập nhật lời nhắc", Toast.LENGTH_SHORT).show();
                    });
                    
                    builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
                    
                    builder.setNeutralButton("Xóa", (dialog, which) -> {
                        // Xóa reminder
                        onReminderDelete(reminder, position);
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
        
        // Lưu thay đổi
        saveRemindersToLocal();
        
        // Cập nhật lịch nhắc nhở
        if (getContext() != null) {
            if (isActive) {
                ReminderAdapter.scheduleReminder(getContext(), reminder);
            } else {
                ReminderAdapter.cancelReminder(getContext(), reminder);
            }
        }
    }
    
    @Override
    public void onReminderDelete(Reminder reminder, int position) {
        // Hủy lịch nhắc nhở
        if (getContext() != null) {
            ReminderAdapter.cancelReminder(getContext(), reminder);
        }
        
        // Xóa khỏi danh sách
        reminderList.remove(position);
        reminderAdapter.notifyItemRemoved(position);
        updateEmptyStateVisibility();
        
        // Lưu thay đổi
        saveRemindersToLocal();
        
        Toast.makeText(getContext(), "Đã xóa lời nhắc", Toast.LENGTH_SHORT).show();
    }

    private void checkNotificationPermission() {
        // Với Android 13 (API 33) trở lên cần xin quyền POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.POST_NOTIFICATIONS) != 
                    PackageManager.PERMISSION_GRANTED) {
                
                // Hiển thị giải thích nếu cần
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Cần quyền thông báo")
                            .setMessage("Ứng dụng cần quyền thông báo để gửi lời nhắc uống nước cho bạn.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                            })
                            .setNegativeButton("Không", null)
                            .show();
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        }
        
        // Kiểm tra xem thông báo có bị tắt trong cài đặt hệ thống không
        checkNotificationSettings();
    }
    
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Nếu được cấp quyền, lên lịch lại cho tất cả các thông báo
                    rescheduleActiveReminders();
                } else {
                    Toast.makeText(requireContext(), 
                            "Không thể gửi thông báo vì không có quyền", 
                            Toast.LENGTH_LONG).show();
                }
            });
    
    private void checkNotificationSettings() {
        NotificationManager notificationManager = 
                (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
                
        // Kiểm tra xem thông báo có bị tắt không
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && 
                !notificationManager.areNotificationsEnabled()) {
            // Thông báo bị tắt, hiển thị hộp thoại yêu cầu bật
            new AlertDialog.Builder(requireContext())
                    .setTitle("Thông báo bị tắt")
                    .setMessage("Vui lòng bật thông báo trong cài đặt hệ thống để nhận lời nhắc uống nước.")
                    .setPositiveButton("Mở cài đặt", (dialog, which) -> {
                        // Mở cài đặt ứng dụng
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    })
                    .setNegativeButton("Để sau", null)
                    .show();
        }
    }
    
    private void rescheduleActiveReminders() {
        // Thiết lập lại tất cả các báo thức cho các lời nhắc đang hoạt động
        Log.d("RemindersFragment", "Bắt đầu lên lịch lại các thông báo...");
        Log.d("RemindersFragment", "Tổng số lời nhắc: " + reminderList.size());
        
        int count = 0;
        for (Reminder reminder : reminderList) {
            if (reminder.isActive() && getContext() != null) {
                // Hủy báo thức cũ
                ReminderAdapter.cancelReminder(getContext(), reminder);
                // Đặt lại báo thức mới
                ReminderAdapter.scheduleReminder(getContext(), reminder);
                
                // Tính toán thời gian báo thức sẽ kích hoạt
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(reminder.getTime());
                Calendar now = Calendar.getInstance();
                
                if (calendar.before(now) && reminder.isDaily()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }
                
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                Log.d("RemindersFragment", "Lời nhắc #" + (++count) + ": " + reminder.getTitle());
                Log.d("RemindersFragment", "ID: " + reminder.getId());
                Log.d("RemindersFragment", "Sẽ thông báo vào: " + sdf.format(calendar.getTime()));
                Log.d("RemindersFragment", "Trạng thái: " + (reminder.isActive() ? "Đang bật" : "Đã tắt"));
                Log.d("RemindersFragment", "Lặp lại: " + (reminder.isDaily() ? "Hàng ngày" : "Một lần"));
                Log.d("RemindersFragment", "------------------------------");
            }
        }
        
        if (count == 0) {
            Log.d("RemindersFragment", "Không có lời nhắc nào được lên lịch!");
        } else {
            Log.d("RemindersFragment", "Đã lên lịch cho " + count + " lời nhắc");
        }
    }

    /**
     * Kiểm tra tất cả các hạn chế có thể ảnh hưởng đến thông báo
     */
    private void checkForNotificationRestrictions() {
        Context context = getContext();
        if (context == null) return;
        
        StringBuilder restrictions = new StringBuilder();
        boolean hasRestrictions = false;
        
        // 1. Kiểm tra quyền thông báo
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && 
                !notificationManager.areNotificationsEnabled()) {
            restrictions.append("- Thông báo bị tắt trong cài đặt hệ thống\n");
            hasRestrictions = true;
        }
        
        // 2. Kiểm tra chế độ Không làm phiền
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int filter = notificationManager.getCurrentInterruptionFilter();
            if (filter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                restrictions.append("- Chế độ 'Không làm phiền' đang bật\n");
                hasRestrictions = true;
            }
        }
        
        // 3. Kiểm tra chế độ tiết kiệm pin
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
                restrictions.append("- Tối ưu hóa pin đang hạn chế ứng dụng\n");
                hasRestrictions = true;
            }
        }
        
        // 4. Các cảnh báo cho thiết bị cụ thể
        String manufacturer = Build.MANUFACTURER.toLowerCase(Locale.getDefault());
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
            restrictions.append("- Thiết bị Xiaomi: Cần kiểm tra cài đặt 'Autostart'\n");
            hasRestrictions = true;
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            restrictions.append("- Thiết bị Huawei: Cần kiểm tra cài đặt 'Protected apps'\n");
            hasRestrictions = true;
        } else if (manufacturer.contains("samsung")) {
            restrictions.append("- Thiết bị Samsung: Cần kiểm tra 'App sleeping' trong cài đặt pin\n");
            hasRestrictions = true;
        }
        
        // Hiển thị thông báo nếu có hạn chế
        if (hasRestrictions) {
            new AlertDialog.Builder(context)
                    .setTitle("Cảnh báo giới hạn thông báo")
                    .setMessage("Phát hiện các giới hạn có thể ngăn thông báo hiển thị:\n\n" + 
                            restrictions.toString() + 
                            "\nBạn cần mở cài đặt hệ thống để kiểm tra.")
                    .setPositiveButton("Mở cài đặt", (dialog, which) -> {
                        try {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(context, "Không thể mở cài đặt", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Để sau", null)
                    .show();
            
            // Log warning
            Log.w("RemindersFragment", "Phát hiện các hạn chế thông báo: \n" + restrictions.toString());
        } else {
            Log.d("RemindersFragment", "Không phát hiện hạn chế thông báo");
        }
    }

    /**
     * Gửi một thông báo test ngay lập tức để kiểm tra hệ thống thông báo
     */
    private void sendTestNotification() {
        if (getContext() == null) return;
        
        // Phương pháp 1: Sử dụng helper method từ Application
        WataCrabApplication.sendTestNotification(
                requireContext(),
                "Kiểm tra thông báo",
                "Đây là thông báo test từ WataCrab lúc " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date())
        );
        
        // Phương pháp 2: Gửi thông báo trực tiếp
        try {
            NotificationManager notificationManager = 
                    (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(
                        requireContext(), 
                        WataCrabApplication.DEFAULT_NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification_water)
                        .setContentTitle("Test thông báo #2")
                        .setContentText("Thông báo test (trực tiếp) lúc " + 
                                new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true);
                
                int notificationId = (int) (System.currentTimeMillis() % 10000);
                notificationManager.notify(notificationId, builder.build());
                
                Log.d("RemindersFragment", "Đã gửi thông báo test trực tiếp với ID: " + notificationId);
            }
        } catch (Exception e) {
            Log.e("RemindersFragment", "Lỗi khi gửi thông báo test: " + e.getMessage());
        }
        
        // Hiển thị cả Toast
        Toast.makeText(requireContext(), "Đã gửi thông báo test", Toast.LENGTH_SHORT).show();
    }
} 