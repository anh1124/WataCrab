package com.example.watacrab;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;

public class WataCrabApplication extends Application {
    private static final String TAG = "WataCrabApplication";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Check for Google Play Services
        if (checkGooglePlayServices()) {
            // Initialize Firebase
            try {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
            }
        }
    }

    private boolean checkGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Log.e(TAG, "Google Play Services is not available: " + 
                    apiAvailability.getErrorString(resultCode));
            } else {
                Log.e(TAG, "This device is not supported by Google Play Services");
            }
            return false;
        }
        
        Log.d(TAG, "Google Play Services is available");
        return true;
    }
} 