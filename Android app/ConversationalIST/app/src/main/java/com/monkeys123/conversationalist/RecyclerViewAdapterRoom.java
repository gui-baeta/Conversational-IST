package com.monkeys123.conversationalist;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.monkeys123.conversationalist.MessageViewHolders.ImageMessageHolder;
import com.monkeys123.conversationalist.Data.ChatMessage;
import com.monkeys123.conversationalist.MessageViewHolders.FileMessageHolder;
import com.monkeys123.conversationalist.MessageViewHolders.LocationMessageHolder;
import com.monkeys123.conversationalist.MessageViewHolders.MessageViewHolder;
import com.monkeys123.conversationalist.MessageViewHolders.TextMessageHolder;

public class RecyclerViewAdapterRoom extends ListAdapter<ChatMessage, MessageViewHolder> {
    private final Context context;

    public RecyclerViewAdapterRoom(Context context) {
        super(new ChatMessageDC());
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getViewType();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(viewType, parent, false);;

        MessageViewHolder holder = null;


        switch (viewType) {
            case(R.layout.location_chat_message_row_self):
            case(R.layout.location_chat_message_row_other):
                holder = new LocationMessageHolder(view, context,this);
                break;
            case(R.layout.text_chat_message_row_self):
            case(R.layout.text_chat_message_row_other):
                holder = new TextMessageHolder(view, context, this);
                break;
            case(R.layout.file_chat_message_row_self):
            case(R.layout.file_chat_message_row_other):
                holder = new FileMessageHolder(view, context, this);
                break;
            case(R.layout.image_chat_message_row_self):
            case(R.layout.image_chat_message_row_other):
                holder = new ImageMessageHolder(view, context, this);
                break;
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.updateValues(getItem(position));
    }

    public int getMessageOrder(int position) {
        try {
            return getItem(position).getChatOrder();
        } catch(IndexOutOfBoundsException e) {
            return -1;
        }
    }

    public ChatMessage getMessage(int pos) {
        try {
            return getItem(pos);
        } catch(IndexOutOfBoundsException e) {
            e.printStackTrace();
            return null;
        }
    }


    private static class ChatMessageDC extends DiffUtil.ItemCallback<ChatMessage> {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getChatOrder() == newItem.getChatOrder();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            //TODO implement the censorship here
            boolean modified = newItem.isModified();

            if (modified)
                newItem.setModified(false);

            return oldItem.equals(newItem) && !modified;
        }
    }
}
