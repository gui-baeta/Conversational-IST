package com.monkeys123.conversationalist.Data;

import static androidx.core.app.ActivityCompat.requestPermissions;
import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.Nullable;

import com.monkeys123.conversationalist.CustomApplication;
import com.monkeys123.conversationalist.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ChatMessage {
    public enum MessageType {
        Location,
        Text,
        Image,
        File,
    }

    public static int ViewTypes[] = {
            R.layout.location_chat_message_row_self,
            R.layout.location_chat_message_row_other,

            R.layout.text_chat_message_row_self,
            R.layout.text_chat_message_row_other,

            R.layout.image_chat_message_row_self,
            R.layout.image_chat_message_row_other,

            R.layout.file_chat_message_row_self,
            R.layout.file_chat_message_row_other,
    };

    int chatOrder;
    long timeStamp;
    int userID;
    String userName;
    ChatMessage.MessageType messageType;
    String message;
    boolean visible = true;
    File downloadedFile;
    boolean modified = false;
    String fileType;

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public File getDownloadedFile() {
        return downloadedFile;
    }

    public void setDownloadedFile(File downloadedFile) {
        this.downloadedFile = downloadedFile;
    }

    public void downloadFile(Context context, long groupID) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder().url("http://10.0.2.2:8080/files/" + getMessage()).build();
        CustomApplication app = ((CustomApplication) context.getApplicationContext());
        Log.e("Kill me","Downloading file pog");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                            requestPermissions(app.getCurrentActivity(), permissions, 1000);
                        }
                        System.out.println(response.headers());
                        System.out.println(getMessage());
                        String path = context.getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/";

                        String extension = "";
                        if (getFileType() != null && !getFileType().equals(""))
                            extension = "."+getFileType();

                        downloadedFile = new File(path + getMessage() + extension);
                        InputStream is = response.body().byteStream();

                        BufferedInputStream input = new BufferedInputStream(is);
                        OutputStream output = null;
                        try {
                            output = new FileOutputStream(downloadedFile);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                        byte[] data = new byte[1024];

                        long total = 0;
                        int count = 0;

                        try {
                            while ((count = input.read(data)) != -1) {
                                total += count;
                                output.write(data, 0, count);
                            }
                            output.flush();
                            output.close();
                            input.close();
                            Log.e("OnStart", "Received " + total + ":" + downloadedFile.getName());
                            setMessage(downloadedFile.getName());
                            modified = true;
                            app.registerReceivedMessage(ChatMessage.this, groupID, false);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public ChatMessage() {
    }

    public ChatMessage(int userID, String userName, String message, ChatMessage.MessageType messageType) {
        this.chatOrder = -1;
        this.timeStamp = Time.from(Instant.now()).getTime();
        this.userID = userID;
        this.userName = userName;
        this.message = message;
        this.messageType = messageType;
    }

    public ChatMessage(int chatOrder, long timeStamp, int userID, String userName, String message, ChatMessage.MessageType messageType) {
        this.chatOrder = chatOrder;
        this.timeStamp = timeStamp;
        this.userID = userID;
        this.userName = userName;
        this.message = message;
        this.messageType = messageType;
    }

    public int getViewType() {
        return ViewTypes[messageType.ordinal() * 2 + (CustomApplication.userID == userID ? 0 : 1)];
    }

    public int getChatOrder() {
        return chatOrder;
    }

    public void setChatOrder(int chatOrder) {
        this.chatOrder = chatOrder;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public ChatMessage.MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(ChatMessage.MessageType messageType) {
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimeStampString() {
        return new SimpleDateFormat("HH:mm").format(Date.from(Instant.ofEpochMilli(this.timeStamp)));
    }

    public boolean reported() {
        return !this.visible;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ChatMessage) {
            ChatMessage other = (ChatMessage) obj;
            return (other.getMessage().equals(getMessage()) && (other.getDownloadedFile() == getDownloadedFile()));
        }

        return false;
    }
}


