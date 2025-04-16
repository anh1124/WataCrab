package com.example.watacrab.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.watacrab.MainActivity;
import com.example.watacrab.R;
import com.example.watacrab.managers.UserManager;
import com.example.watacrab.models.User;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerLink;
    private Button testConnectionButton;
    private UserManager userManager;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_login);
            Log.d(TAG, "onCreate: Activity started");

            // Initialize Firebase Auth
            mAuth = FirebaseAuth.getInstance();

            // Initialize UserManager
            userManager = UserManager.getInstance();

            // Initialize views
            emailEditText = findViewById(R.id.emailEditText);
            passwordEditText = findViewById(R.id.passwordEditText);
            loginButton = findViewById(R.id.loginButton);
            registerLink = findViewById(R.id.registerLink);
            testConnectionButton = findViewById(R.id.testConnectionButton);

            // Initialize progress dialog
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Đang đăng nhập...");
            progressDialog.setCancelable(false);

            // Setup click listeners
            setupLoginButton();
            setupRegisterLink();
            setupTestConnectionButton();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khởi tạo ứng dụng. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupTestConnectionButton() {
        testConnectionButton.setOnClickListener(v -> {
            try {
                testConnectionButton.setEnabled(false);
                Log.d(TAG, "Test Connection button clicked");

                // Check Firebase initialization
                FirebaseApp app = FirebaseApp.getInstance();
                Log.d(TAG, "FirebaseApp name: " + app.getName());
                Log.d(TAG, "FirebaseApp options: " + app.getOptions());

                // Test Firebase connection by checking if we can get the current user
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    Log.d(TAG, "User is already signed in: " + currentUser.getEmail());
                    Toast.makeText(this, "Kết nối Firebase thành công!\nNgười dùng đã đăng nhập: " + currentUser.getEmail(), 
                        Toast.LENGTH_LONG).show();
                } else {
                    Log.d(TAG, "No user is currently signed in");
                    Toast.makeText(this, "Kết nối Firebase thành công!\nChưa có người dùng đăng nhập", 
                        Toast.LENGTH_LONG).show();
                }

                testConnectionButton.setEnabled(true);
            } catch (Exception e) {
                Log.e(TAG, "Error during connection test", e);
                Toast.makeText(this, "Lỗi kiểm tra kết nối: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
                testConnectionButton.setEnabled(true);
            }
        });
    }

    private void setupLoginButton() {
        loginButton.setOnClickListener(v -> {
            try {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressDialog.show();
                userManager.login(email, password, new UserManager.AuthCallback() {
                    @Override
                    public void onSuccess(User user) {
                        progressDialog.dismiss();
                        Log.d(TAG, "Login successful for user: " + user.getEmail());
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Login failed: " + errorMessage);
                        if (errorMessage.contains("email")) {
                            Toast.makeText(LoginActivity.this, "Email không tồn tại", Toast.LENGTH_SHORT).show();
                        } else if (errorMessage.contains("password")) {
                            Toast.makeText(LoginActivity.this, "Mật khẩu không chính xác", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error during login: " + e.getMessage(), e);
                Toast.makeText(this, "Lỗi đăng nhập. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRegisterLink() {
        registerLink.setOnClickListener(v -> {
            try {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            } catch (Exception e) {
                Log.e(TAG, "Error starting RegisterActivity: " + e.getMessage(), e);
                Toast.makeText(this, "Lỗi mở màn hình đăng ký. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            // Check if user is already logged in
            userManager.getCurrentUser(new UserManager.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) {
                        Log.d(TAG, "User already logged in: " + user.getEmail());
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.d(TAG, "No user logged in");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onStart: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
} 