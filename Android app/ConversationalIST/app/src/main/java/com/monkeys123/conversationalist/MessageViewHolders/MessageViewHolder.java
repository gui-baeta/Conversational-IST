package com.monkeys123.conversationalist.MessageViewHolders;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.monkeys123.conversationalist.CustomApplication;
import com.monkeys123.conversationalist.Data.ChatMessage;
import com.monkeys123.conversationalist.R;
import com.monkeys123.conversationalist.RecyclerViewAdapterRoom;
import com.monkeys123.conversationalist.RecyclerViewButtonInterface;

import java.util.Objects;

public abstract class MessageViewHolder extends RecyclerView.ViewHolder {
    TextView sender;
    TextView timeSent;
    RecyclerViewAdapterRoom adapter;

    public MessageViewHolder(@NonNull View itemView, Context context, RecyclerViewAdapterRoom adapter) {
        super(itemView);
        sender = itemView.findViewById(R.id.sender_name);
        timeSent = itemView.findViewById(R.id.text_message_time);
        this.adapter = adapter;

        itemView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Do you want to report this message?" + "\nUser: " + sender + "\nTime Stamp: " + timeSent)
                        .setTitle(R.string.dialog_title)
                        .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // TODO send report and
                                //  (server side ->) consequently the person if X messages reported in total

                                // TODO either add "visible: boolean" attribute in the server
                                //  or delete the message when reported. Both work perfectly!
                                view.setVisibility(View.GONE);

                                Toast.makeText(context, "We will take your report into account! Thank you!", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // User cancelled report. Nothing to do
                            }
                        })
                        .create()
                        .show();
                return true;
            }
        });
    }

    public void updateValues(ChatMessage item) {
        sender.setText(Objects.equals(item.getUserName(), CustomApplication.userName) ? "You" : item.getUserName());
        timeSent.setText(item.getTimeStampString());
    }
}
