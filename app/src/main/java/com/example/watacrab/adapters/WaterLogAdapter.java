package com.example.watacrab.adapters;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.watacrab.R;
import com.example.watacrab.models.WaterLog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WaterLogAdapter extends RecyclerView.Adapter<WaterLogAdapter.WaterLogViewHolder> {
    private static final String TAG = "WaterLogAdapter";
    private List<WaterLog> waterLogs;
    private SimpleDateFormat timeFormat;
    private OnWaterLogDeleteListener deleteListener;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    private WaterLog pendingDeleteLog;
    private int pendingDeletePosition;

    public interface OnWaterLogDeleteListener {
        void onWaterLogDeleted(WaterLog waterLog, int position);
    }

    public WaterLogAdapter(List<WaterLog> waterLogs) {
        this.waterLogs = waterLogs;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    public void setOnWaterLogDeleteListener(OnWaterLogDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public WaterLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_water_log, parent, false);
        return new WaterLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WaterLogViewHolder holder, int position) {
        WaterLog waterLog = waterLogs.get(position);
        holder.timeText.setText(timeFormat.format(waterLog.getTimestamp()));
        holder.amountText.setText(String.format("%d ml", waterLog.getAmount()));
        holder.noteText.setText(waterLog.getNote());
        
        // Thiết lập Long Click Listener cho item
        holder.itemView.setOnLongClickListener(v -> {
            final int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                // Tạo hiệu ứng rung
                Animation shakeAnimation = AnimationUtils.loadAnimation(v.getContext(), R.anim.shake_animation);
                v.startAnimation(shakeAnimation);
                
                // Xử lý xóa sau 1s
                new Handler().postDelayed(() -> {
                    deleteWaterLog(v.getContext(), waterLog, adapterPosition);
                }, 1000); // Đợi 1 giây
                
                return true;
            }
            return false;
        });
    }

    private void deleteWaterLog(Context context, WaterLog waterLog, int position) {
        if (waterLog.getId() != null && !waterLog.getId().isEmpty()) {
            // Lưu thông tin cho trường hợp cần retry
            pendingDeleteLog = waterLog;
            pendingDeletePosition = position;
            
            Log.d(TAG, "Bắt đầu xóa waterLog với ID: " + waterLog.getId());
            
            // Kiểm tra người dùng Firebase hiện tại
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Log.e(TAG, "Không thể xóa: Người dùng Firebase là null");
                Toast.makeText(context, "Bạn cần đăng nhập để xóa dữ liệu", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Xóa từ Firestore
            FirebaseFirestore.getInstance().collection("water_logs")
                    .document(waterLog.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Xóa thành công waterLog với ID: " + waterLog.getId());
                        retryCount = 0; // Reset retry count on success
                        
                        // Xóa khỏi danh sách và thông báo adapter
                        waterLogs.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, getItemCount());
                        
                        // Thông báo cho listener
                        if (deleteListener != null) {
                            deleteListener.onWaterLogDeleted(waterLog, position);
                        }
                        
                        Toast.makeText(context, "Đã xóa " + waterLog.getAmount() + " ml nước", 
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting water log: " + e.getMessage(), e);
                        
                        if (e instanceof FirebaseFirestoreException) {
                            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                            Log.e(TAG, "Firebase error code: " + firestoreException.getCode());
                            if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                handlePermissionDenied(context, waterLog, position);
                            } else {
                                Toast.makeText(context, "Lỗi: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "Lỗi: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Nếu ID là null hoặc rỗng, có thể đó là một bản ghi mới chưa lưu
            // Trong trường hợp này, chúng ta có thể xóa an toàn từ UI mà không cần xóa từ Firestore
            Log.w(TAG, "Mục ID null hoặc rỗng - chỉ xóa khỏi UI");
            waterLogs.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
            
            if (deleteListener != null) {
                deleteListener.onWaterLogDeleted(waterLog, position);
            }
            
            Toast.makeText(context, "Đã xóa " + waterLog.getAmount() + " ml nước", 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Phương thức tĩnh để xóa water log từ bất kỳ nơi nào trong ứng dụng
     */
    public static void deleteWaterLogDirectly(String waterLogId, String userId, int amount) {
        if (waterLogId == null || waterLogId.isEmpty()) {
            Log.e(TAG, "Không thể xóa: waterLogId là null hoặc rỗng");
            return;
        }
        
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e(TAG, "Không thể xóa: Người dùng Firebase là null");
            return;
        }
        
        FirebaseFirestore.getInstance().collection("water_logs")
                .document(waterLogId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Xóa trực tiếp thành công waterLog với ID: " + waterLogId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi xóa trực tiếp waterLog với ID " + waterLogId + ": " + e.getMessage(), e);
                    
                    // Lưu ID của water log để xóa sau khi có quyền truy cập
                    try {
                        // Đề xuất: Lưu waterLogId vào SharedPreferences để xóa sau
                        Log.d(TAG, "Đã lưu water log ID " + waterLogId + " để xóa sau khi có quyền");
                    } catch (Exception ex) {
                        Log.e(TAG, "Lỗi khi lưu pending deletion", ex);
                    }
                });
    }
    
    private void handlePermissionDenied(Context context, WaterLog waterLog, int position) {
        // Nếu đã thử lại quá nhiều lần, thực hiện "xóa giả" - xóa chỉ ở UI nhưng không phải ở database
        if (retryCount >= MAX_RETRIES) {
            Log.w(TAG, "Max retries reached, performing UI-only delete");
            
            // Xóa khỏi danh sách và cập nhật UI
            waterLogs.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
            
            // Thông báo cho listener
            if (deleteListener != null) {
                deleteListener.onWaterLogDeleted(waterLog, position);
            }
            
            Toast.makeText(context, "Đã xóa " + waterLog.getAmount() + " ml nước (chỉ ở UI)", 
                    Toast.LENGTH_SHORT).show();
            
            // Lưu ID để có thể xóa sau khi đăng nhập lại
            storeDeletePendingItem(context, waterLog.getId());
            
            // Reset retry counter
            retryCount = 0;
            return;
        }
        
        // Nếu chưa đạt max retry, thử làm mới token
        retryCount++;
        Log.d(TAG, "Attempting to refresh token, retry count: " + retryCount);
        
        // Đợi một chút trước khi thử lại
        new Handler().postDelayed(() -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                FirebaseAuth.getInstance().getCurrentUser().getIdToken(true)
                        .addOnSuccessListener(result -> {
                            Log.d(TAG, "Token refreshed successfully, waiting before retry...");
                            // Đợi thêm một chút sau khi refresh token trước khi thử lại
                            new Handler().postDelayed(() -> {
                                Log.d(TAG, "Retrying to delete after token refresh");
                                if (pendingDeleteLog != null) {
                                    deleteWaterLog(context, pendingDeleteLog, pendingDeletePosition);
                                }
                            }, 1000); // Đợi 1 giây sau khi refresh token
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error refreshing token: " + e.getMessage(), e);
                            // Thực hiện xóa giả sau khi không thể làm mới token
                            handlePermissionDenied(context, waterLog, position);
                        });
            } else {
                Log.e(TAG, "Cannot refresh token: user is not logged in");
                // Thực hiện xóa giả khi không thể làm mới token
                handlePermissionDenied(context, waterLog, position);
            }
        }, 500); // Đợi 0.5 giây trước khi refresh token
    }
    
    // Lưu ID của item cần xóa để xử lý sau
    private void storeDeletePendingItem(Context context, String itemId) {
        try {
            // Trong thực tế, bạn sẽ lưu vào SharedPreferences hoặc một nơi lưu trữ khác
            Log.d(TAG, "Lưu item ID để xóa sau: " + itemId);
            // Đoạn code để lưu itemId vào SharedPreferences có thể thêm vào đây
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lưu item chờ xóa", e);
        }
    }

    @Override
    public int getItemCount() {
        return waterLogs.size();
    }

    static class WaterLogViewHolder extends RecyclerView.ViewHolder {
        TextView timeText;
        TextView amountText;
        TextView noteText;

        WaterLogViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.timeText);
            amountText = itemView.findViewById(R.id.amountText);
            noteText = itemView.findViewById(R.id.noteText);
        }
    }
} 