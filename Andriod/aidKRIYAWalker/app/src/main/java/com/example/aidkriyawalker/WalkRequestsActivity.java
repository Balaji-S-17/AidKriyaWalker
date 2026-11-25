package com.example.aidkriyawalker;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class WalkRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRequests;
    private WalkRequestAdapter adapter;
    private List<WalkRequest> requestList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentWalkerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_requests);

        recyclerViewRequests = findViewById(R.id.recyclerViewRequests);
        recyclerViewRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WalkRequestAdapter(requestList, new WalkRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(WalkRequest request, int position) {
                acceptRequest(request, position);
            }
            @Override
            public void onDecline(WalkRequest request, int position) {
                declineRequest(request, position);
            }
        });
        recyclerViewRequests.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentWalkerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadRequests();
    }

    private void loadRequests() {
        db.collection("walk_requests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    requestList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        WalkRequest req = doc.toObject(WalkRequest.class);
                        req.id = doc.getId();
                        requestList.add(req);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load requests", Toast.LENGTH_SHORT).show());
    }

    private void acceptRequest(WalkRequest request, int position) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "accepted");
        updates.put("walkerId", currentWalkerId);
        db.collection("walk_requests").document(request.id)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request accepted!", Toast.LENGTH_SHORT).show();
                    requestList.remove(position);
                    adapter.notifyItemRemoved(position);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to accept request", Toast.LENGTH_SHORT).show());
    }

    private void declineRequest(WalkRequest request, int position) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "declined");
        updates.put("walkerId", currentWalkerId);
        db.collection("walk_requests").document(request.id)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request declined.", Toast.LENGTH_SHORT).show();
                    requestList.remove(position);
                    adapter.notifyItemRemoved(position);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to decline request", Toast.LENGTH_SHORT).show());
    }
}