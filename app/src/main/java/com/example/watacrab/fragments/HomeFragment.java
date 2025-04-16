package com.example.watacrab.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
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
import com.example.watacrab.views.WaterProgressView;
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

public class HomeFragment extends Fragment implements WaterLogAdapter.OnWaterLogDeleteListener {
    private static final String TAG = "HomeFragment";
    private static final int MAX_WATER = 2000;

    private TextView dateTextView;
    private TextView waterAmountText;
    private TextView welcomeUserTextView;
    private WaterProgressView waterProgressView;
    private SeekBar waterSeekBar;
    private RecyclerView waterLogsRecyclerView;
    private WaterLogAdapter waterLogAdapter;
    private List<WaterLog> waterLogs;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;
    private FirebaseFirestore db;
    private User currentUser;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        waterLogs = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        dateTextView = view.findViewById(R.id.dateTextView);
        waterAmountText = view.findViewById(R.id.waterAmountText);
        waterProgressView = view.findViewById(R.id.waterProgressView);
        waterSeekBar = view.findViewById(R.id.waterSeekBar);
        waterLogsRecyclerView = view.findViewById(R.id.waterLogsRecyclerView);
        welcomeUserTextView = view.findViewById(R.id.welcomeUserTextView);
        ImageButton previousDayButton = view.findViewById(R.id.previousDayButton);
        ImageButton nextDayButton = view.findViewById(R.id.nextDayButton);

        // Setup RecyclerView
        waterLogAdapter = new WaterLogAdapter(waterLogs);
        waterLogAdapter.setOnWaterLogDeleteListener(this);
        waterLogsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        waterLogsRecyclerView.setAdapter(waterLogAdapter);

        // Setup date navigation
        previousDayButton.setOnClickListener(v -> changeDate(-1));
        nextDayButton.setOnClickListener(v -> changeDate(1));

        // Setup water input
        waterSeekBar.setMax(MAX_WATER);
        waterSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateWaterAmount(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Setup water amount text click
        waterAmountText.setOnClickListener(v -> showWaterInputDialog());

        // Setup add water button
        view.findViewById(R.id.addWaterButton).setOnClickListener(v -> addWaterLog());

        // Update UI
        updateDateText();
        loadCurrentUser();

        return view;
    }

    private void loadCurrentUser() {
        UserManager.getInstance().getCurrentUser(new UserManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (getActivity() == null || !isAdded()) return;
                
                currentUser = user;
                if (user != null) {
                    // Cập nhật lời chào với tên người dùng
                    String username = user.getUsername();
                    if (username != null && !username.isEmpty()) {
                        welcomeUserTextView.setText("Xin chào, " + username + "!");
                    } else {
                        welcomeUserTextView.setText("Xin chào, bạn!");
                    }
                    
                    loadWaterLogs();
                } else {
                    welcomeUserTextView.setText("Xin chào, bạn!");
                    Toast.makeText(getContext(), "Vui lòng đăng nhập để theo dõi lượng nước", 
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (getActivity() == null || !isAdded()) return;
                
                Log.e(TAG, "Error loading user: " + errorMessage);
                Toast.makeText(getContext(), "Lỗi tải thông tin người dùng: " + errorMessage, 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void changeDate(int days) {
        selectedDate.add(Calendar.DAY_OF_MONTH, days);
        updateDateText();
        loadWaterLogs();
    }

    private void updateDateText() {
        Calendar today = Calendar.getInstance();
        String dateText;
        
        if (selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            dateText = "Hôm nay";
        } else {
            dateText = dateFormat.format(selectedDate.getTime());
        }
        
        dateTextView.setText(dateText);
    }

    private void updateWaterAmount(int amount) {
        waterAmountText.setText(String.format(Locale.getDefault(), "%d/%d ml", amount, MAX_WATER));
        waterProgressView.setProgress((float) amount / MAX_WATER);
    }

    private void showWaterInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Nhập lượng nước");

        final EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(waterSeekBar.getProgress()));
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            try {
                int amount = Integer.parseInt(input.getText().toString());
                if (amount > MAX_WATER) {
                    amount = MAX_WATER;
                    Toast.makeText(getContext(), "Đã đặt về mức tối đa: " + MAX_WATER + " ml", 
                        Toast.LENGTH_SHORT).show();
                }
                waterSeekBar.setProgress(amount);
                updateWaterAmount(amount);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addWaterLog() {
        try {
            if (currentUser == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để thêm lượng nước", 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            int amount = waterSeekBar.getProgress();
            if (amount <= 0) {
                Toast.makeText(getContext(), "Vui lòng nhập lượng nước lớn hơn 0", 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo một bản sao của selectedDate để tránh lỗi
            Date currentDate = selectedDate != null ? selectedDate.getTime() : new Date();
            
            // Create WaterLog using the constructor with safe null handling
            WaterLog waterLog = new WaterLog(
                currentUser.getId(), 
                amount, 
                ""  // Để trống ghi chú
            );
            
            // Set timestamp manually
            waterLog.setTimestamp(currentDate);

            Log.d(TAG, "Thêm nước: " + amount + "ml, userId: " + currentUser.getId());

            db.collection("water_logs")
                    .add(waterLog)
                    .addOnSuccessListener(documentReference -> {
                        if (getActivity() == null || !isAdded()) return;
                        
                        try {
                            Toast.makeText(getContext(), "Đã thêm " + amount + " ml nước", 
                                Toast.LENGTH_SHORT).show();
                            // Reset waterSeekBar an toàn
                            if (waterSeekBar != null) {
                                waterSeekBar.setProgress(0);
                                updateWaterAmount(0);
                            }
                            loadWaterLogs();
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi sau khi thêm nước: " + e.getMessage());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getActivity() == null || !isAdded()) return;
                        
                        Log.e(TAG, "Lỗi khi thêm nước: " + e.getMessage());
                        
                        if (e instanceof FirebaseFirestoreException) {
                            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                            if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                handlePermissionDenied();
                            } else {
                                Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), 
                                    Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Lỗi nghiêm trọng khi thêm nước: " + e.getMessage());
            Toast.makeText(getContext(), "Không thể thêm nước: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }

    private void handlePermissionDenied() {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            Log.d(TAG, "Attempting to refresh token, retry count: " + retryCount);
            
            // Đợi một chút trước khi thử lại
            new android.os.Handler().postDelayed(() -> {
                FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                        .addOnSuccessListener(result -> {
                            Log.d(TAG, "Token refreshed successfully, waiting before retry...");
                            // Đợi thêm một chút sau khi refresh token trước khi load lại dữ liệu
                            new android.os.Handler().postDelayed(() -> {
                                Log.d(TAG, "Retrying to load data after token refresh");
                                loadWaterLogs();
                            }, 1000); // Đợi 1 giây sau khi refresh token
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error refreshing token: " + e.getMessage());
                            Toast.makeText(getContext(), "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", 
                                Toast.LENGTH_SHORT).show();
                        });
            }, 500); // Đợi 0.5 giây trước khi refresh token
        } else {
            Log.e(TAG, "Max retries reached (" + MAX_RETRIES + "), redirecting to login");
            Toast.makeText(getContext(), "Không thể truy cập dữ liệu. Vui lòng đăng nhập lại.", 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWaterLogs() {
        if (currentUser == null) {
            return;
        }

        Log.d(TAG, "Loading water logs for user: " + currentUser.getId());

        Calendar startOfDay = (Calendar) selectedDate.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);

        Calendar endOfDay = (Calendar) selectedDate.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);

        Log.d(TAG, "Query range - Start: " + startOfDay.getTime() + ", End: " + endOfDay.getTime());

        db.collection("water_logs")
                .whereEqualTo("userId", currentUser.getId())
                .whereGreaterThanOrEqualTo("timestamp", startOfDay.getTime())
                .whereLessThanOrEqualTo("timestamp", endOfDay.getTime())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (getActivity() == null || !isAdded()) return;
                    
                    Log.d(TAG, "Successfully loaded " + queryDocumentSnapshots.size() + " water logs");
                    waterLogs.clear();
                    int totalAmount = 0;
                    
                    // Thay đổi cách xử lý kết quả để lấy ID từ document
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        WaterLog log = document.toObject(WaterLog.class);
                        log.setId(document.getId()); // Thiết lập ID từ document
                        waterLogs.add(log);
                        totalAmount += log.getAmount();
                        Log.d(TAG, "Loaded water log with ID: " + log.getId() + ", amount: " + log.getAmount());
                    }
                    
                    waterLogAdapter.notifyDataSetChanged();
                    waterSeekBar.setProgress(totalAmount > MAX_WATER ? MAX_WATER : totalAmount);
                    updateWaterAmount(totalAmount > MAX_WATER ? MAX_WATER : totalAmount);
                })
                .addOnFailureListener(e -> {
                    if (getActivity() == null || !isAdded()) return;
                    
                    Log.e(TAG, "Error loading water logs: " + e.getMessage() + 
                              "\nUser ID: " + currentUser.getId() + 
                              "\nAuth Token: " + (FirebaseAuth.getInstance().getCurrentUser() != null ? "Present" : "Null"));
                    
                    if (e instanceof FirebaseFirestoreException) {
                        FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                        if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            handlePermissionDenied();
                        } else {
                            Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onWaterLogDeleted(WaterLog waterLog, int position) {
        try {
            // Cập nhật lại tổng lượng nước sau khi xóa
            loadWaterLogs();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi cập nhật sau khi xóa: " + e.getMessage());
        }
    }
} 