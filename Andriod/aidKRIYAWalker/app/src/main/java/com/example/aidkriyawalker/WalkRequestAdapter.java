package com.example.aidkriyawalker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WalkRequestAdapter extends RecyclerView.Adapter<WalkRequestAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(WalkRequest request, int position);
        void onDecline(WalkRequest request, int position);
    }

    private List<WalkRequest> walkRequests;
    private OnRequestActionListener actionListener;

    public WalkRequestAdapter(List<WalkRequest> walkRequests, OnRequestActionListener actionListener) {
        this.walkRequests = walkRequests;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_walk_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalkRequest request = walkRequests.get(position);
        holder.textDateTime.setText(request.walkDateTime);
        holder.textLocation.setText("Location: " + request.location);
        holder.textDuration.setText("Duration: " + request.duration + " min");
        holder.textPreferences.setText("Preferences: " + request.preferences);
        holder.textNotes.setText("Notes: " + request.notes);

        holder.buttonAccept.setOnClickListener(v -> actionListener.onAccept(request, position));
        holder.buttonDecline.setOnClickListener(v -> actionListener.onDecline(request, position));
    }

    @Override
    public int getItemCount() {
        return walkRequests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textDateTime, textLocation, textDuration, textPreferences, textNotes;
        Button buttonAccept, buttonDecline;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDateTime = itemView.findViewById(R.id.textDateTime);
            textLocation = itemView.findViewById(R.id.textLocation);
            textDuration = itemView.findViewById(R.id.textDuration);
            textPreferences = itemView.findViewById(R.id.textPreferences);
            textNotes = itemView.findViewById(R.id.textNotes);
            buttonAccept = itemView.findViewById(R.id.buttonAccept);
            buttonDecline = itemView.findViewById(R.id.buttonDecline);
        }
    }
}