package com.example.watacrab;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.watacrab.databinding.ActivityMainBinding;
import com.example.watacrab.managers.UserManager;
import com.example.watacrab.models.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private UserManager userManager;
    private NavController navController;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            Log.d(TAG, "onCreate: Activity started");

            // Initialize progress dialog
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Đang tải dữ liệu...");
            progressDialog.setCancelable(false);

            // Initialize UserManager
            userManager = UserManager.getInstance();

            // Setup navigation
            setupNavigation();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khởi tạo ứng dụng. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupNavigation() {
        try {
            // Get NavHostFragment
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            
            if (navHostFragment == null) {
                throw new IllegalStateException("NavHostFragment not found");
            }

            // Get NavController
            navController = navHostFragment.getNavController();
            
            // Setup BottomNavigationView
            BottomNavigationView bottomNav = binding.bottomNavigationView;
            NavigationUI.setupWithNavController(bottomNav, navController);
            
            Log.d(TAG, "Navigation setup completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up navigation: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khởi tạo điều hướng. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            progressDialog.show();
            // Check if user is still logged in
            userManager.getCurrentUser(new UserManager.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    progressDialog.dismiss();
                    Log.d(TAG, "User is logged in: " + user.getEmail());
                }

                @Override
                public void onError(String errorMessage) {
                    progressDialog.dismiss();
                    Log.d(TAG, "User is not logged in: " + errorMessage);
                    finish();
                }
            });
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error in onStart: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi kiểm tra đăng nhập. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
            finish();
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