package com.example.watacrab.dialogs;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.watacrab.R;
import com.example.watacrab.model.Reminder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderDialog extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    private TextView tvTimeDisplay;
    private EditText etTitle;
    private RadioGroup rgRepeat;
    private RadioButton rbDaily, rbWeekdays, rbWeekends, rbOnce;
    private Button btnSave, btnCancel, btnDelete;
    
    private Reminder editReminder;
    private boolean isEditMode = false;
    private int selectedHour = 8;
    private int selectedMinute = 0;
    
    private OnReminderSavedListener reminderSavedListener;
    
    public interface OnReminderSavedListener {
        void onReminderSaved(Reminder reminder);
        void onReminderDeleted(Reminder reminder);
    }
    
    public static ReminderDialog newInstance(Reminder reminder) {
        ReminderDialog dialog = new ReminderDialog();
        if (reminder != null) {
            Bundle args = new Bundle();
            args.putSerializable("reminder", reminder);
            dialog.setArguments(args);
        }
        return dialog;
    }
    
    public void setOnReminderSavedListener(OnReminderSavedListener listener) {
        this.reminderSavedListener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null && getArguments().containsKey("reminder")) {
            editReminder = (Reminder) getArguments().getSerializable("reminder");
            isEditMode = true;
            
            // Get hour and minute from Date object
            if (editReminder.getTime() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(editReminder.getTime());
                selectedHour = calendar.get(Calendar.HOUR_OF_DAY);
                selectedMinute = calendar.get(Calendar.MINUTE);
            }
        }
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_reminder, null);
        
        // Initialize views
        tvTimeDisplay = view.findViewById(R.id.tvTimeDisplay);
        etTitle = view.findViewById(R.id.etTitle);
        rgRepeat = view.findViewById(R.id.rgRepeat);
        rbDaily = view.findViewById(R.id.rbDaily);
        rbWeekdays = view.findViewById(R.id.rbWeekdays);
        rbWeekends = view.findViewById(R.id.rbWeekends);
        rbOnce = view.findViewById(R.id.rbOnce);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnDelete = view.findViewById(R.id.btnDelete);
        
        // Set up time display with current values
        updateTimeDisplay();
        
        // Set click listener for time selection
        tvTimeDisplay.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    getContext(),
                    this,
                    selectedHour,
                    selectedMinute,
                    true);
            timePickerDialog.show();
        });
        
        // Fill with data if in edit mode
        if (isEditMode) {
            etTitle.setText(editReminder.getTitle());
            
            // Set appropriate radio button based on isDaily flag
            if (editReminder.isDaily()) {
                rbDaily.setChecked(true);
            } else {
                rbOnce.setChecked(true);
            }
            
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnDelete.setVisibility(View.GONE);
        }
        
        // Set button click listeners
        btnSave.setOnClickListener(v -> saveReminder());
        btnCancel.setOnClickListener(v -> dismiss());
        btnDelete.setOnClickListener(v -> {
            if (isEditMode && reminderSavedListener != null) {
                reminderSavedListener.onReminderDeleted(editReminder);
                dismiss();
            }
        });
        
        builder.setView(view);
        return builder.create();
    }
    
    private void updateTimeDisplay() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
        calendar.set(Calendar.MINUTE, selectedMinute);
        tvTimeDisplay.setText(timeFormat.format(calendar.getTime()));
    }
    
    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        selectedHour = hourOfDay;
        selectedMinute = minute;
        updateTimeDisplay();
    }
    
    private void saveReminder() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            return;
        }
        
        // Create a Date object for the selected time
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
        calendar.set(Calendar.MINUTE, selectedMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date reminderTime = calendar.getTime();
        
        // Determine if reminder is daily based on radio selection
        boolean isDaily = rgRepeat.getCheckedRadioButtonId() == R.id.rbDaily;
        
        // Get current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "default_user";
        
        Reminder reminder;
        if (isEditMode) {
            // Update existing reminder
            reminder = editReminder;
            reminder.setTime(reminderTime);
            reminder.setTitle(title);
            reminder.setDaily(isDaily);
        } else {
            // Create new reminder
            reminder = new Reminder(title, reminderTime, isDaily, userId);
        }
        
        // Save to Firebase
        DatabaseReference remindersRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("reminders");
        
        remindersRef.child(reminder.getId()).setValue(reminder);
        
        // Notify listener
        if (reminderSavedListener != null) {
            reminderSavedListener.onReminderSaved(reminder);
        }
        
        dismiss();
    }
} 