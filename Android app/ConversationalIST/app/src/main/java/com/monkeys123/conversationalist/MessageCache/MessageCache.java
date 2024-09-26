package com.monkeys123.conversationalist.MessageCache;

import android.util.LruCache;

import com.monkeys123.conversationalist.CustomApplication;
import com.monkeys123.conversationalist.Data.ChatMessage;

public class MessageCache extends LruCache<CacheKey, ChatMessage> {

    private final CustomApplication app;

    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *                the maximum number of entries in the cache. For all other caches,
     *                this is the maximum sum of the sizes of the entries in this cache.
     */


    public MessageCache(int maxSize, CustomApplication app) {
        super(maxSize);
        this.app = app;
    }

    @Override
    protected void entryRemoved(boolean evicted, CacheKey key, ChatMessage oldValue, ChatMessage newValue) {
        if (!evicted)
            return;

        app.removeMessage(key.getGroupId(), oldValue);
        app.groups.get(key.getGroupId()).setPreloaded(false);
    }


}


