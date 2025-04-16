package com.example.watacrab.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.watacrab.R;
import com.example.watacrab.managers.UserManager;
import com.example.watacrab.models.User;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText usernameEditText;
    private Button registerButton;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Log.d(TAG, "onCreate: Activity started");

        userManager = UserManager.getInstance();
        initializeViews();
        setupRegisterButton();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        registerButton = findViewById(R.id.registerButton);
    }

    private void setupRegisterButton() {
        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();

            if (validateInput(email, password, username)) {
                registerUser(email, password, username);
            }
        });
    }

    private boolean validateInput(String email, String password, String username) {
        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void registerUser(String email, String password, String username) {
        Log.d(TAG, "Attempting to register user: " + email);
        registerButton.setEnabled(false);

        userManager.register(email, password, username, new UserManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                Log.d(TAG, "Registration successful for user: " + user.getEmail());
                Toast.makeText(RegisterActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Registration failed: " + errorMessage);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                registerButton.setEnabled(true);
            }
        });
    }
} 