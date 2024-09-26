package com.monkeys123.conversationalist.Data;

import android.location.Location;
import android.util.Log;

import com.monkeys123.conversationalist.Data.ChatMessage;

import java.util.Comparator;
import java.util.TreeSet;

public class Helper implements Comparator<ChatMessage> {

    @Override
    public int compare(ChatMessage o1, ChatMessage o2) {
//        return -1;
        if (o1.getChatOrder() == o2.getChatOrder())
            return 0;

        int comparison = Long.compare(o1.timeStamp, o2.timeStamp);
        if (comparison == 0)
            return +1;
        return comparison;
    }
}



