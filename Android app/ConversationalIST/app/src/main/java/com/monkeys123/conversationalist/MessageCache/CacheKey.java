package com.monkeys123.conversationalist.MessageCache;

import java.util.Objects;

public class CacheKey {
    long groupId;
    long messageOrder;
    int hashCode;

    public long getGroupId() {
        return groupId;
    }

    public long getMessageOrder() {
        return messageOrder;
    }

    public CacheKey(long groupId, long messageOrder) {
        this.groupId = groupId;
        this.messageOrder = messageOrder;
        this.hashCode = Objects.hash(groupId, messageOrder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        CacheKey that = (CacheKey) o;

        return groupId == that.getGroupId() && messageOrder == that.getMessageOrder();
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}