package com.monkeys123.conversationalist.MessageViewHolders;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;

import androidx.documentfile.provider.DocumentFile;

import com.monkeys123.conversationalist.ChatGroupActivity;
import com.monkeys123.conversationalist.CustomApplication;
import com.monkeys123.conversationalist.Data.ChatMessage;
import com.monkeys123.conversationalist.MessageViewHolders.MessageViewHolder;
import com.monkeys123.conversationalist.R;
import com.monkeys123.conversationalist.RecyclerViewAdapterRoom;

import java.io.File;
import java.io.IOException;

public class ImageMessageHolder extends MessageViewHolder {
    ImageView imageView;

    public ImageMessageHolder(View view, Context context, RecyclerViewAdapterRoom adapter) {
        super(view, context, adapter);
        imageView = view.findViewById(R.id.message_image);

        imageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int pos = getAdapterPosition();
                ChatMessage msg = adapter.getMessage(pos);

                if (msg.getDownloadedFile() == null) {
                    // Download file
                    CustomApplication app = (CustomApplication) context.getApplicationContext();
                    if (app.getCurrentActivity() instanceof ChatGroupActivity) {
                        ChatGroupActivity activity = (ChatGroupActivity) app.getCurrentActivity();
                        long groupID = activity.getGroupID();

                        msg.downloadFile(context, groupID);
                    }
                } else {
                    ((CustomApplication) context.getApplicationContext()).openFileActivity(msg);
                    // Open file handling activity
                }
            }
        });
    }

    public void updateValues(ChatMessage item) {
        super.updateValues(item);

        if (item.getDownloadedFile() != null) {
            imageView.setImageURI(Uri.fromFile(item.getDownloadedFile()));
        }
    }
}
