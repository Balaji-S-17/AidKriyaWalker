package com.example.aidkriyawalker;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class WalkRequestActivity extends AppCompatActivity {

    private EditText editDate, editTime, editDuration, editLocation, editPreferences, editNotes;
    private Button buttonSubmitRequest;

    private int selectedYear, selectedMonth, selectedDay;
    private int selectedHour, selectedMinute;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_request);

        editDate = findViewById(R.id.editDate);
        editTime = findViewById(R.id.editTime);
        editDuration = findViewById(R.id.editDuration);
        editLocation = findViewById(R.id.editLocation);
        editPreferences = findViewById(R.id.editPreferences);
        editNotes = findViewById(R.id.editNotes);
        buttonSubmitRequest = findViewById(R.id.buttonSubmitRequest);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Handle date picker
        editDate.setOnClickListener(v -> showDatePicker());
        // Handle time picker
        editTime.setOnClickListener(v -> showTimePicker());

        buttonSubmitRequest.setOnClickListener(v -> submitWalkRequest());
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
            selectedYear = y;
            selectedMonth = m;
            selectedDay = d;
            editDate.setText(String.format("%04d-%02d-%02d", y, m + 1, d));
        }, year, month, day);
        dpd.show();
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog tpd = new TimePickerDialog(this, (view, h, m) -> {
            selectedHour = h;
            selectedMinute = m;
            editTime.setText(String.format("%02d:%02d", h, m));
        }, hour, minute, true);
        tpd.show();
    }

    private void submitWalkRequest() {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        String walkDate = editDate.getText().toString().trim();
        String walkTime = editTime.getText().toString().trim();
        String durationStr = editDuration.getText().toString().trim();
        String location = editLocation.getText().toString().trim();
        String preferences = editPreferences.getText().toString().trim();
        String notes = editNotes.getText().toString().trim();

        if (walkDate.isEmpty() || walkTime.isEmpty() || durationStr.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(durationStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid duration.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Combine date and time into a single string or timestamp for simplicity
        String walkDateTime = walkDate + " " + walkTime;

        Map<String, Object> walkRequest = new HashMap<>();
        walkRequest.put("userId", currentUser.getUid());
        walkRequest.put("requestedAt", System.currentTimeMillis());
        walkRequest.put("walkDateTime", walkDateTime);
        walkRequest.put("location", location);
        walkRequest.put("duration", duration);
        walkRequest.put("preferences", preferences);
        walkRequest.put("notes", notes);
        walkRequest.put("status", "pending"); // default status
        walkRequest.put("walkerId", null);

        ProgressDialog pd = ProgressDialog.show(this, "Submitting", "Please wait...", true, false);

        db.collection("walk_requests").add(walkRequest)
                .addOnSuccessListener(documentReference -> {
                    pd.dismiss();
                    Toast.makeText(WalkRequestActivity.this, "Walk request submitted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(WalkRequestActivity.this, "Failed to submit request.", Toast.LENGTH_SHORT).show();
                });
    }
}