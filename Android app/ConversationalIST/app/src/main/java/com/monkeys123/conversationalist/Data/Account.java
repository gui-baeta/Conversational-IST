package com.monkeys123.conversationalist.Data;

public class Account {
    public enum Type {
        ephemeral,
        permanent
    }

    int id = -1;
    String username = "guest";
    String password = "guest";
    Account.Type type = Account.Type.permanent;
}
