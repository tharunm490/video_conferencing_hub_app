package com.example.st_meet;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.st_meet.databinding.ItemParticipantBinding;

import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {

    private final List<String> participants;

    public ParticipantAdapter(List<String> participants) {
        this.participants = participants;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemParticipantBinding binding = ItemParticipantBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = participants.get(position);
        holder.binding.textName.setText(name);
        holder.binding.textInitial.setText(name.substring(0, 1).toUpperCase());
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemParticipantBinding binding;

        ViewHolder(ItemParticipantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}