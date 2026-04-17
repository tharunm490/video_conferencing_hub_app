package com.example.st_meet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.ViewHolder> {

    private List<Meeting> meetings;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Meeting meeting);
    }

    public MeetingAdapter(List<Meeting> meetings, OnItemClickListener listener) {
        this.meetings = meetings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meeting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Meeting meeting = meetings.get(position);
        holder.textMeetingId.setText("ID: " + meeting.getMeetingId());
        holder.textCreatedAt.setText(meeting.getCreatedAt());
        holder.textSummary.setText(meeting.getSummary());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(meeting);
            }
        });
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMeetingId, textCreatedAt, textSummary;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textMeetingId = itemView.findViewById(R.id.textMeetingId);
            textCreatedAt = itemView.findViewById(R.id.textCreatedAt);
            textSummary = itemView.findViewById(R.id.textSummary);
        }
    }
}
