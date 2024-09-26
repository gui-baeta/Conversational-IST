package com.monkeys123.conversationalist.MessageViewHolders;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.monkeys123.conversationalist.ChatGroupActivity;
import com.monkeys123.conversationalist.CustomApplication;
import com.monkeys123.conversationalist.Data.ChatMessage;
import com.monkeys123.conversationalist.R;
import com.monkeys123.conversationalist.RecyclerViewAdapterRoom;

public class FileMessageHolder extends MessageViewHolder {
    TextView fileMessage;

    public FileMessageHolder(View view, Context context, RecyclerViewAdapterRoom adapter) {
        super(view, context, adapter);

        fileMessage = view.findViewById(R.id.sent_file_message);

        fileMessage.setOnClickListener(new View.OnClickListener() {
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
        //TODO update file ui here
    }
}
