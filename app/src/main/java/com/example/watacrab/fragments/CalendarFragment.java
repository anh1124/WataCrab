package com.example.watacrab.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watacrab.R;
import com.example.watacrab.adapters.WaterLogAdapter;
import com.example.watacrab.managers.UserManager;
import com.example.watacrab.models.User;
import com.example.watacrab.models.WaterLog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {
    private static final String TAG = "CalendarFragment";
    private CalendarView calendarView;
    private TextView selectedDateText;
    private TextView totalWaterAmountText;
    private RecyclerView waterLogsRecyclerView;
    private WaterLogAdapter waterLogAdapter;
    private List<WaterLog> waterLogs;
    private FirebaseFirestore db;
    private User currentUser;
    private SimpleDateFormat dateFormat;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    private Date currentlyLoadingDate;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        waterLogs = new ArrayList<>();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        
        calendarView = view.findViewById(R.id.calendarView);
        selectedDateText = view.findViewById(R.id.selectedDateText);
        totalWaterAmountText = view.findViewById(R.id.totalWaterAmountText);
        waterLogsRecyclerView = view.findViewById(R.id.waterLogsRecyclerView);

        setupCalendar();
        setupRecyclerView();
        loadCurrentUser();

        return view;
    }

    private void setupCalendar() {
        // Set minimum date to today
        calendarView.setMinDate(System.currentTimeMillis());

        // Set date selection listener
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                String selectedDate = String.format("%d/%d/%d", 
                    dayOfMonth,
                    month + 1,
                    year);
                selectedDateText.setText(selectedDate);
                loadWaterLogsForDate(calendar.getTime());
            }
        });

        // Set initial date
        Calendar calendar = Calendar.getInstance();
        String initialDate = dateFormat.format(calendar.getTime());
        selectedDateText.setText(initialDate);
        loadWaterLogsForDate(calendar.getTime());
    }

    private void setupRecyclerView() {
        waterLogAdapter = new WaterLogAdapter(waterLogs);
        waterLogsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        waterLogsRecyclerView.setAdapter(waterLogAdapter);
    }

    private void loadCurrentUser() {
        UserManager.getInstance().getCurrentUser(new UserManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                loadWaterLogsForDate(Calendar.getInstance().getTime());
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading user: " + errorMessage);
            }
        });
    }

    private void loadWaterLogsForDate(Date date) {
        if (currentUser == null) return;
        
        currentlyLoadingDate = date;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endOfDay = calendar.getTime();

        db.collection("water_logs")
                .whereEqualTo("userId", currentUser.getId())
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThanOrEqualTo("timestamp", endOfDay)
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    waterLogs.clear();
                    int totalAmount = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        WaterLog waterLog = document.toObject(WaterLog.class);
                        waterLog.setId(document.getId());
                        waterLogs.add(waterLog);
                        totalAmount += waterLog.getAmount();
                    }
                    waterLogAdapter.notifyDataSetChanged();
                    // Update the total water amount text
                    totalWaterAmountText.setText(String.format(Locale.getDefault(), 
                        "Tổng lượng nước: %d ml", totalAmount));
                    
                    // Reset retry count on successful load
                    retryCount = 0;
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading water logs: " + e.getMessage());
                    
                    if (e instanceof FirebaseFirestoreException) {
                        FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                        if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            handlePermissionDenied();
                        } else {
                            totalWaterAmountText.setText("Tổng lượng nước: 0 ml");
                            Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                        }
                    } else {
                        totalWaterAmountText.setText("Tổng lượng nước: 0 ml");
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    private void handlePermissionDenied() {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            Log.d(TAG, "Attempting to refresh token, retry count: " + retryCount);
            
            // Đợi một chút trước khi thử lại
            new android.os.Handler().postDelayed(() -> {
                if (getActivity() == null || !isAdded()) return; // Kiểm tra fragment đã detach chưa
                
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                            .addOnSuccessListener(result -> {
                                if (getActivity() == null || !isAdded()) return; // Kiểm tra fragment đã detach chưa
                                
                                Log.d(TAG, "Token refreshed successfully, waiting before retry...");
                                // Đợi thêm một chút sau khi refresh token trước khi load lại dữ liệu
                                new android.os.Handler().postDelayed(() -> {
                                    if (getActivity() == null || !isAdded()) return; // Kiểm tra fragment đã detach chưa
                                    
                                    Log.d(TAG, "Retrying to load data after token refresh");
                                    if (currentlyLoadingDate != null) {
                                        loadWaterLogsForDate(currentlyLoadingDate);
                                    }
                                }, 1000); // Đợi 1 giây sau khi refresh token
                            })
                            .addOnFailureListener(e -> {
                                if (getActivity() == null || !isAdded()) return; // Kiểm tra fragment đã detach chưa
                                
                                Log.e(TAG, "Error refreshing token: " + e.getMessage());
                                
                                // Kiểm tra context trước khi hiển thị Toast
                                Context context = getContext();
                                if (context != null) {
                                    Toast.makeText(context, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", 
                                        Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Log.e(TAG, "Cannot refresh token: user is not logged in");
                    
                    // Kiểm tra context trước khi hiển thị Toast
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, "Vui lòng đăng nhập để xem dữ liệu", 
                            Toast.LENGTH_SHORT).show();
                    }
                }
            }, 500); // Đợi 0.5 giây trước khi refresh token
        } else {
            Log.e(TAG, "Max retries reached (" + MAX_RETRIES + "), redirecting to login");
            
            // Kiểm tra context trước khi hiển thị Toast
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, "Không thể truy cập dữ liệu. Vui lòng đăng nhập lại.", 
                    Toast.LENGTH_SHORT).show();
            }
            retryCount = 0;
        }
    }
} 