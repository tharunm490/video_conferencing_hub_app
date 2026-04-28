package com.example.st_meet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.ViewHolder> {

    private List<Meeting> meetings;
    private OnMeetingListener listener;

    public interface OnMeetingListener {
        void onItemClick(Meeting meeting);
        void onDeleteClick(Meeting meeting, int position);
    }

    public MeetingAdapter(List<Meeting> meetings, OnMeetingListener listener) {
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
        holder.textMeetingId.setText(holder.itemView.getContext().getString(R.string.meeting_id_label, meeting.getMeetingId()));
        holder.textCreatedAt.setText(meeting.getCreatedAt());
        holder.textSummary.setText(meeting.getSummary());
        
        if (meeting.getFullText() != null && !meeting.getFullText().isEmpty()) {
            holder.textFullText.setVisibility(View.VISIBLE);
            holder.textFullText.setText(meeting.getFullText());
            if (holder.textTranscriptLabel != null) holder.textTranscriptLabel.setVisibility(View.VISIBLE);
        } else {
            holder.textFullText.setVisibility(View.GONE);
            if (holder.textTranscriptLabel != null) holder.textTranscriptLabel.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(meeting);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(meeting, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMeetingId, textCreatedAt, textSummary, textFullText, textTranscriptLabel;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textMeetingId = itemView.findViewById(R.id.textMeetingId);
            textCreatedAt = itemView.findViewById(R.id.textCreatedAt);
            textSummary = itemView.findViewById(R.id.textSummary);
            textFullText = itemView.findViewById(R.id.textFullText);
            textTranscriptLabel = itemView.findViewById(R.id.textTranscriptLabel);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
