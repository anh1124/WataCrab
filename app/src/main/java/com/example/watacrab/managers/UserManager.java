package com.example.watacrab.managers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.watacrab.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private static final String TAG = "UserManager";
    private static UserManager instance;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    private UserManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public interface DocumentCallback {
        void onSuccess(DocumentSnapshot document);
        void onError(String errorMessage);
    }

    // Register a new user
    public void register(String email, String password, String username, AuthCallback callback) {
        Log.d(TAG, "Attempting to register user: " + email);
        
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase Auth registration successful");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            User user = new User(userId, email, username);
                            
                            // Save user data to Firestore
                            saveUserToFirestore(user, callback);
                        } else {
                            callback.onError("Không thể tạo người dùng");
                        }
                    } else {
                        Log.e(TAG, "Registration failed", task.getException());
                        String errorMessage = getErrorMessage(task.getException());
                        callback.onError(errorMessage);
                    }
                });
    }

    // Login user
    public void login(String email, String password, AuthCallback callback) {
        Log.d(TAG, "Attempting to login user: " + email);
        
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Login successful");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Create a wrapper UserCallback that delegates to the AuthCallback
                            getUserFromFirestore(firebaseUser.getUid(), new UserCallback() {
                                @Override
                                public void onSuccess(User user) {
                                    callback.onSuccess(user);
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    callback.onError(errorMessage);
                                }
                            });
                        } else {
                            callback.onError("Không thể đăng nhập");
                        }
                    } else {
                        Log.e(TAG, "Login failed", task.getException());
                        String errorMessage = getErrorMessage(task.getException());
                        callback.onError(errorMessage);
                    }
                });
    }

    // Logout user
    public void logout() {
        Log.d(TAG, "Logging out user");
        mAuth.signOut();
    }

    // Get current user
    public void getCurrentUser(UserCallback callback) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            Log.d(TAG, "Firebase Auth user found: " + firebaseUser.getEmail());
            getUserFromFirestore(firebaseUser.getUid(), new UserCallback() {
                @Override
                public void onSuccess(User user) {
                    Log.d(TAG, "Firestore user data found for: " + user.getEmail());
                    callback.onSuccess(user);
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error getting Firestore data: " + errorMessage);
                    // If Firestore data is missing, create a basic user from Firebase Auth data
                    User basicUser = new User();
                    basicUser.setId(firebaseUser.getUid());
                    basicUser.setEmail(firebaseUser.getEmail());
                    basicUser.setUsername(firebaseUser.getDisplayName() != null ? 
                        firebaseUser.getDisplayName() : "User");
                    basicUser.setCreatedAt(new java.util.Date());
                    
                    // Save the basic user data to Firestore
                    saveUserToFirestore(basicUser, new AuthCallback() {
                        @Override
                        public void onSuccess(User savedUser) {
                            Log.d(TAG, "Basic user data saved to Firestore");
                            callback.onSuccess(savedUser);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Error saving basic user data: " + errorMessage);
                            callback.onError("Lỗi khi lưu thông tin người dùng");
                        }
                    });
                }
            });
        } else {
            Log.d(TAG, "No Firebase Auth user found");
            callback.onError("Không có người dùng đang đăng nhập");
        }
    }

    // Save user to Firestore
    private void saveUserToFirestore(User user, AuthCallback callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("username", user.getUsername());
        userData.put("createdAt", user.getCreatedAt());

        db.collection("users").document(user.getId())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data saved successfully to Firestore");
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user data to Firestore", e);
                    callback.onError("Lỗi khi lưu thông tin người dùng");
                });
    }

    // Get user from Firestore
    public void getUserFromFirestore(String userId, UserCallback callback) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = new User();
                        user.setId(userId);
                        user.setEmail(documentSnapshot.getString("email"));
                        user.setUsername(documentSnapshot.getString("username"));
                        user.setCreatedAt(documentSnapshot.getDate("createdAt"));
                        callback.onSuccess(user);
                    } else {
                        callback.onError("Không tìm thấy thông tin người dùng trong Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user data from Firestore", e);
                    callback.onError("Lỗi khi lấy thông tin người dùng từ Firestore");
                });
    }

    // Helper method to get error messages
    private String getErrorMessage(Exception exception) {
        if (exception == null) return "Lỗi không xác định";
        
        String errorMessage;
        if (exception instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            errorMessage = "Email đã được sử dụng. Vui lòng sử dụng email khác";
        } else if (exception instanceof com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            errorMessage = "Mật khẩu quá yếu. Vui lòng sử dụng mật khẩu mạnh hơn";
        } else if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            errorMessage = "Email hoặc mật khẩu không chính xác";
        } else if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            errorMessage = "Email không tồn tại";
        } else {
            errorMessage = "Lỗi: " + exception.getMessage();
        }
        return errorMessage;
    }
} 