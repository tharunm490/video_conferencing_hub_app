package com.example.st_meet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder> {
    private List<Message> messages;

    public ChatHistoryAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.senderName.setText(message.getUserName());
        holder.messageContent.setText(message.getMessageText());
        // Since it's history, we might want to change text color to black/dark for readability on light bg
        holder.messageContent.setTextColor(android.graphics.Color.BLACK);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView senderName, messageContent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.textSenderName);
            messageContent = itemView.findViewById(R.id.textMessageContent);
        }
    }
}
