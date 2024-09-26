package com.monkeys123.conversationalist.ChatroomListingRecyclerView;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.monkeys123.conversationalist.ChatroomDTO;
import com.monkeys123.conversationalist.CustomApplication;
import com.monkeys123.conversationalist.Data.GroupChat;
import com.monkeys123.conversationalist.R;
import com.monkeys123.conversationalist.RecyclerViewButtonInterface;

import java.util.ArrayList;

public class RecyclerViewAdapterRooms extends RecyclerView.Adapter<RecyclerViewAdapterRooms.MyViewHolder> {
    private final Context context;
    private ArrayList<ChatroomDTO> chatrooms;
    private RecyclerViewButtonInterface buttonInterface;

    public RecyclerViewAdapterRooms(Context context, ArrayList<ChatroomDTO> chatrooms, RecyclerViewButtonInterface buttonInterface) {
        this.context = context;
        this.chatrooms = chatrooms;
        this.buttonInterface = buttonInterface;
    }

    public void setRooms(ArrayList<ChatroomDTO> rooms) {
        this.chatrooms.clear();

        this.chatrooms.addAll(rooms);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.chatroom_search_row, parent, false);
        CustomApplication appCtx = (CustomApplication) context.getApplicationContext();
        return new RecyclerViewAdapterRooms.MyViewHolder(view, buttonInterface, appCtx);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        //assign values to the views
        holder.nameView.setText(chatrooms.get(position).getName());
        holder.iconView.setText(GroupChat.createLogo(holder.nameView.getText().toString()));
    }

    @Override
    public int getItemCount() {
        if (chatrooms != null)
            return chatrooms.size();
        else
            return 0;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView nameView;
        TextView iconView;


        public MyViewHolder(@NonNull View itemView, RecyclerViewButtonInterface buttonInterface, CustomApplication appCtx) {
            super(itemView);
            iconView = itemView.findViewById(R.id.chat_logo2);
            nameView = itemView.findViewById(R.id.chat_name_sr);

            nameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("FDS", "please work");

                    if (buttonInterface != null) {
                        int pos = getAdapterPosition();

                        if (pos != RecyclerView.NO_POSITION)
                            buttonInterface.onClick(pos);

                        appCtx.getCurrentActivity().finish();
                    }
                }
            });
        }
    }
}
