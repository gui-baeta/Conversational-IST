package com.monkeys123.conversationalist;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.monkeys123.conversationalist.Data.GroupChat;

import java.util.ArrayList;

public class ChatlistAdapter extends RecyclerView.Adapter<ChatlistAdapter.MyViewHolder> {
    private final Context context;
    private ArrayList<GroupChat> chatrooms;

    public ChatlistAdapter(Context context, ArrayList<GroupChat> chatrooms) {
        this.context = context;
        this.chatrooms = chatrooms;
    }

    public void setRooms(ArrayList<GroupChat> rooms) {
        this.chatrooms.clear();

        this.chatrooms.addAll(rooms);
    }

    public void add(GroupChat chat) {
        this.chatrooms.add(chat);
        System.out.println(chatrooms);
    }

    public void clear() {
        this.chatrooms.clear();
    }

    @NonNull
    @Override
    public ChatlistAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.chatlist_group_row, parent, false);
        CustomApplication appCtx = (CustomApplication) context.getApplicationContext();

        return new ChatlistAdapter.MyViewHolder(view, appCtx, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatlistAdapter.MyViewHolder holder, int position) {
        //assign values to the views

        if (chatrooms.get(position) == null)
            return;

        String name = chatrooms.get(position).getGroupName();
        holder.nameView.setText(name);
        holder.iconView.setText(GroupChat.createLogo(name));
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


        public MyViewHolder(@NonNull View itemView, CustomApplication appCtx, ChatlistAdapter adapter) {
            super(itemView);
            iconView = itemView.findViewById(R.id.chat_logo);
            nameView = itemView.findViewById(R.id.chat_name);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();

                    if (pos != RecyclerView.NO_POSITION) {
                        GroupChat chat = adapter.chatrooms.get(pos);
                        if(chat.getType() == GroupChat.GroupType.Geofenced && CustomApplication.latestLatLng != null) {
                            float distanceInMeters = chat.getLocation().distanceTo(CustomApplication.latestLatLng);

                            if(distanceInMeters > chat.getRadius()){
                                Toast.makeText(appCtx.currentActivity, "Out of range", Toast.LENGTH_SHORT).show();
                                return;
                            } else {
                                Intent chat_group = new Intent(appCtx.currentActivity, CommunicationService.class);
                                chat_group.setAction("unsubRoom");
                                chat_group.putExtra("roomId", chat.getGroupID());
                                appCtx.currentActivity.startService(chat_group);
                            }
                        }

                        Intent chat_group = new Intent(appCtx.currentActivity, ChatGroupActivity.class);

                        chat_group.putExtra("chat_name", chat.getGroupName());
                        chat_group.putExtra("chat_id", chat.getGroupID());
                        chat_group.putExtra("chat_type", chat.getType().toString());
                        appCtx.currentActivity.startActivity(chat_group);
                    }
                }
            });
        }
    }
}
