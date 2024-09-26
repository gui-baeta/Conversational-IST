package com.monkeys123.conversationalist.MessageViewHolders;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.monkeys123.conversationalist.Data.ChatMessage;
import com.monkeys123.conversationalist.R;
import com.monkeys123.conversationalist.RecyclerViewAdapterRoom;

public class TextMessageHolder extends MessageViewHolder {
    TextView textMessage;

    public TextMessageHolder(@NonNull View itemView, Context context, RecyclerViewAdapterRoom adapter) {
        super(itemView, context, adapter);
        textMessage = itemView.findViewById(R.id.text_message_body);
        sender = itemView.findViewById(R.id.sender_name);
        timeSent = itemView.findViewById(R.id.text_message_time);
    }


    public void updateValues(ChatMessage item) {
        super.updateValues(item);
        textMessage.setText(item.getMessage());
    }
}
