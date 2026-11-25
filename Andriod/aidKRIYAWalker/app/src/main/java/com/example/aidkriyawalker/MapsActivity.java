package com.example.aidkriyawalker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.aidkriyawalker.databinding.ActivityMapsBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private String userRole;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // For User Role
    private final HashMap<String, Marker> walkerMarkers = new HashMap<>();

    // For Walker Role
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Launcher for location permission request
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted.
                    Log.d(TAG, "Location permission granted.");
                    setupMapAndLocation();
                } else {
                    // Permission is denied.
                    Log.d(TAG, "Location permission denied.");
                    Toast.makeText(this, "Location permission is required to use the map.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        userRole = getIntent().getStringExtra("USER_ROLE");
        if (userRole == null) {
            userRole = "User"; // Default to User if something goes wrong
        }
        setTitle("Map - " + userRole); // Set the title bar

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            setupMapAndLocation();
        } else {
            // Permission is not granted, request it
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void setupMapAndLocation() {
        if (mMap == null) return;
        try {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            // Move camera to a default location initially
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(20.5937, 78.9629), 5)); // Default to India

            if ("Walker".equals(userRole)) {
                startLocationUpdates();
            } else { // "User"
                listenForWalkers();
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception, permission missing", e);
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 seconds
                .setMinUpdateIntervalMillis(5000) // 5 seconds
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null && currentUser != null) {
                        Log.d(TAG, "Walker location updated: " + location.getLatitude() + ", " + location.getLongitude());
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        Map<String, Object> walkerData = new HashMap<>();
                        walkerData.put("location", geoPoint);
                        walkerData.put("name", currentUser.getDisplayName());

                        db.collection("walkers").document(currentUser.getUid()).set(walkerData);
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception on starting location updates", e);
        }
    }

    private void listenForWalkers() {
        Log.d(TAG, "listenForWalkers: Called!");
        Toast.makeText(this, "Listening for walkers...", Toast.LENGTH_SHORT).show();
        db.collection("walkers").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                Toast.makeText(this, "Listen failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (snapshots != null) {
                Log.d(TAG, "Snapshots received. Document count: " + snapshots.size());
                Toast.makeText(this, "Snapshots received: " + snapshots.size(), Toast.LENGTH_SHORT).show();

                if (snapshots.size() == 0) {
                    Log.d(TAG, "No walkers found!");
                    Toast.makeText(this, "No walkers found!", Toast.LENGTH_SHORT).show();
                }

                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String walkerId = doc.getId();
                    String walkerName = doc.getString("name");
                    GeoPoint geoPoint = doc.getGeoPoint("location");

                    Log.d(TAG, "Walker document id: " + walkerId
                            + ", name: " + walkerName
                            + ", location: " + (geoPoint != null ? geoPoint.toString() : "null"));

                    Toast.makeText(this, "Walker: " + walkerName, Toast.LENGTH_SHORT).show();

                    if (geoPoint != null) {
                        LatLng walkerPosition = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

                        // If marker already exists, update its position
                        if (walkerMarkers.containsKey(walkerId)) {
                            Marker marker = walkerMarkers.get(walkerId);
                            if (marker != null) {
                                marker.setPosition(walkerPosition);
                            }
                        } else {
                            // Otherwise, create a new marker
                            Marker newMarker = mMap.addMarker(new MarkerOptions()
                                    .position(walkerPosition)
                                    .title(walkerName != null ? walkerName : "Walker"));
                            walkerMarkers.put(walkerId, newMarker);
                        }
                    }
                }
            } else {
                Log.d(TAG, "Snapshots is null!");
                Toast.makeText(this, "Snapshots is null!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Menu Integration ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // Role-based visibility for menu items
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem requestWalk = menu.findItem(R.id.action_request_walk);
        MenuItem viewRequests = menu.findItem(R.id.action_view_requests);
        if ("User".equals(userRole)) {
            if (requestWalk != null) requestWalk.setVisible(true);
            if (viewRequests != null) viewRequests.setVisible(false);
        } else if ("Walker".equals(userRole)) {
            if (requestWalk != null) requestWalk.setVisible(false);
            if (viewRequests != null) viewRequests.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        } else if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileEditActivity.class));
            return true;
        } else if (id == R.id.action_request_walk) {
            startActivity(new Intent(this, WalkRequestActivity.class));
            return true;
        } else if (id == R.id.action_view_requests) {
            startActivity(new Intent(this, WalkRequestsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        // If user is a walker, remove their location from Firestore
        if ("Walker".equals(userRole) && currentUser != null) {
            db.collection("walkers").document(currentUser.getUid()).delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Walker document successfully deleted!"))
                    .addOnFailureListener(e -> Log.w(TAG, "Error deleting walker document", e));
        }

        // Stop location updates if they are running
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Sign out from Firebase
        mAuth.signOut();

        // Sign out from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // After sign out, navigate back to the role selection screen
            Toast.makeText(MapsActivity.this, "Signed out successfully.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MapsActivity.this, ChooseRoleActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop location updates when the activity is no longer visible
        // to save battery, but don't remove the document from firestore.
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}