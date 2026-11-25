package com.example.aidkriyawalker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChooseRoleActivity extends AppCompatActivity {

    private static final String TAG = "ChooseRoleActivity";
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<Intent> loginLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_role);

        mAuth = FirebaseAuth.getInstance();

        // This launcher handles the result from the MainActivity (login screen).
        loginLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // After returning from MainActivity
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        // If the user is STILL null (e.g., they pressed the back button on the login screen),
                        // then we can't continue. Close the app.
                        Log.d(TAG, "User did not complete sign-in. Finishing activity.");
                        finish();
                    } else {
                        // User successfully signed in. They can now see the role buttons.
                        Log.d(TAG, "User is signed in. Ready to choose role.");
                    }
                });

        // Check login status when the activity is first created.
        if (mAuth.getCurrentUser() == null) {
            // If not logged in, launch the login screen.
            Log.d(TAG, "No user signed in, launching MainActivity for login.");
            Intent intent = new Intent(this, MainActivity.class);
            loginLauncher.launch(intent);
        }

        // --- Button Click Listeners ---
        Button buttonWalker = findViewById(R.id.buttonWalker);
        Button buttonUser = findViewById(R.id.buttonUser);

        buttonWalker.setOnClickListener(v -> navigateToMaps("Walker"));
        buttonUser.setOnClickListener(v -> navigateToMaps("User"));
    }

    // The onStart() method is no longer needed, as this logic is now handled robustly in onCreate.

    private void navigateToMaps(String role) {
        Intent intent = new Intent(ChooseRoleActivity.this, MapsActivity.class);
        intent.putExtra("USER_ROLE", role);
        startActivity(intent);
    }
}