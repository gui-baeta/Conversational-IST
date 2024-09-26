package com.monkeys123.conversationalist.MessageViewHolders;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.google.type.LatLng;
import com.monkeys123.conversationalist.ChatGroupActivity;
import com.monkeys123.conversationalist.CustomApplication;
import com.monkeys123.conversationalist.Data.ChatMessage;
import com.monkeys123.conversationalist.R;
import com.monkeys123.conversationalist.RecyclerViewAdapterRoom;

public class LocationMessageHolder extends MessageViewHolder {

    public LocationMessageHolder(View view, Context context, RecyclerViewAdapterRoom adapter) {
        super(view, context, adapter);

        LinearLayout locationContainer = view.findViewById(R.id.location_container);

        locationContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = getAdapterPosition();
                ChatMessage msg = adapter.getMessage(pos);
                ((CustomApplication) context.getApplicationContext()).openLocationActivity(msg);
            }
        });
    }

    public void updateValues(ChatMessage item) {
        super.updateValues(item);
        //TODO update location gui here
    }
}
