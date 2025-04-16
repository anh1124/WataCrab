package com.example.watacrab.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.watacrab.R;
import com.example.watacrab.auth.LoginActivity;
import com.example.watacrab.managers.UserManager;
import com.example.watacrab.models.User;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class UserProfileFragment extends Fragment {
    private static final String TAG = "UserProfileFragment";
    private ImageView profileImageView;
    private TextView usernameTextView;
    private TextView emailTextView;
    private TextView joinDateTextView;
    private Button logoutButton;
    private UserManager userManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

            profileImageView = view.findViewById(R.id.profileImageView);
            usernameTextView = view.findViewById(R.id.usernameTextView);
            emailTextView = view.findViewById(R.id.emailTextView);
            joinDateTextView = view.findViewById(R.id.joinDateTextView);
            logoutButton = view.findViewById(R.id.logoutButton);

            setupUserManager();
            loadUserData();
            setupLogoutButton();

            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage(), e);
            throw e;
        }
    }

    private void setupUserManager() {
        try {
            userManager = UserManager.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Error setting up UserManager: " + e.getMessage(), e);
            throw e;
        }
    }

    private void loadUserData() {
        try {
            userManager.getCurrentUser(new UserManager.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) {
                        emailTextView.setText(user.getEmail());
                        usernameTextView.setText(user.getUsername());
                        
                        // Format join date
                        if (user.getCreatedAt() != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            String joinDate = "Tham gia: " + sdf.format(user.getCreatedAt());
                            joinDateTextView.setText(joinDate);
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error loading user data: " + error);
                    Toast.makeText(getContext(), "Lỗi khi tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading user data: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Lỗi khi tải thông tin người dùng", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupLogoutButton() {
        try {
            logoutButton.setOnClickListener(v -> {
                try {
                    userManager.logout();
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                    requireActivity().finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error during logout: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Lỗi khi đăng xuất", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up logout button: " + e.getMessage(), e);
            throw e;
        }
    }
} 