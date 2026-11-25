package com.example.aidkriyawalker;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ProfileEditActivity extends AppCompatActivity {
    private ImageView imageProfile;
    private EditText editName, editBio, editPhone, editInterests;
    private Spinner spinnerRole;
    private Button buttonSaveProfile;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        imageProfile = findViewById(R.id.imageProfile);
        editName = findViewById(R.id.editName);
        editBio = findViewById(R.id.editBio);
        editPhone = findViewById(R.id.editPhone);
        editInterests = findViewById(R.id.editInterests);
        spinnerRole = findViewById(R.id.spinnerRole);
        buttonSaveProfile = findViewById(R.id.buttonSaveProfile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Set up role spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.profile_roles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        if (currentUser != null) {
            editName.setText(currentUser.getDisplayName());
            // Load Google photo if available
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl().toString())
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(imageProfile);
            }
            loadProfile();
        }

        buttonSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        editBio.setText(documentSnapshot.getString("bio"));
                        editPhone.setText(documentSnapshot.getString("phone"));
                        Object interestsObj = documentSnapshot.get("interests");
                        if (interestsObj instanceof java.util.List<?>) {
                            editInterests.setText(TextUtils.join(", ", (java.util.List<?>) interestsObj));
                        }
                        String role = documentSnapshot.getString("role");
                        int pos = role != null && role.equals("Wanderer") ? 1 : 0;
                        spinnerRole.setSelection(pos);
                    }
                });
    }

    private void saveProfile() {
        if (currentUser == null) return;

        String displayName = editName.getText().toString().trim();
        String bio = editBio.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String interestsStr = editInterests.getText().toString().trim();
        String role = spinnerRole.getSelectedItem().toString();

        if (displayName.isEmpty()) {
            Toast.makeText(this, "Name is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", currentUser.getUid());
        profile.put("displayName", displayName);
        profile.put("photoUrl", (currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : ""));
        profile.put("bio", bio);
        profile.put("phone", phone);
        profile.put("interests", Arrays.asList(interestsStr.split(",")));
        profile.put("role", role);
        profile.put("isVerified", false);

        ProgressDialog pd = ProgressDialog.show(this, "Saving Profile", "Please wait...", true, false);

        db.collection("users").document(currentUser.getUid()).set(profile)
                .addOnSuccessListener(aVoid -> {
                    pd.dismiss();
                    Toast.makeText(ProfileEditActivity.this, "Profile saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(ProfileEditActivity.this, "Failed to save profile.", Toast.LENGTH_SHORT).show();
                });
    }
}