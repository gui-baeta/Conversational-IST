package com.monkeys123.conversationalist;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monkeys123.conversationalist.Data.ChatMessage;
import com.monkeys123.conversationalist.Data.GroupChat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class CommunicationService extends Service {
    private CustomApplication myApp;
    private static final String TAG = "CommunicationService";
    private ConcurrentHashMap<Long, Disposable> subscriptions = new ConcurrentHashMap<>();

    private StompClient stompClient;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        myApp = (CustomApplication) getApplicationContext();

        Log.e(TAG, "Service Created");

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "ws://10.0.2.2:8080/gs-guide-websocket/websocket");

        connectStomp();
    }

    @Override
    public void onDestroy() {
        stompClient.disconnect();
        Log.e(TAG, "Service Destroyed");

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void checkStompStatus() {
        if (!stompClient.isConnected()) {
            connectStomp();
        }
    }


    @SuppressLint("CheckResult")
    public void connectStomp() {
        stompClient.withClientHeartbeat(10000).withServerHeartbeat(10000);

        stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            Log.e(TAG, "Stomp connection opened");
                            break;
                        case ERROR:
                            Log.e(TAG, "Stomp connection error", lifecycleEvent.getException());
                            break;
                        case CLOSED:
                            Log.e(TAG, "Stomp connection closed");
                            break;
                        case FAILED_SERVER_HEARTBEAT:
                            Log.e(TAG, "Stomp failed server heartbeat");
                    }
                });

        stompClient.connect();
    }

    @SuppressLint("CheckResult")
    public void sendMsgViaStomp(int userID, String userName, String message, long groupID, ChatMessage.MessageType messageType, String type) {
        try {
            ChatMessageDTO chatMessage = new ChatMessageDTO(userID, userName, message, messageType, type);
            ObjectMapper mapper = new ObjectMapper();

            String jsonString = mapper.writeValueAsString(chatMessage);

            Log.e(TAG, "Sending message " + jsonString);

            checkStompStatus();
            stompClient.send("/app/send/" + groupID, jsonString)
                    .subscribe(() -> {
                        Log.e(TAG, "STOMP echo send successfully");
                    }, throwable -> {
                        Log.e(TAG, "Error send STOMP echo", throwable);
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void createGeoChatroom(String groupName, double lat, double lng, double radius) {
        int userID = CustomApplication.userID;

        String uri = "/chatrooms/geo/" + lat + "/" + lng + "/" + radius + "/" + groupName + "/" + userID;

        OkHttpClient client = new OkHttpClient();
        System.out.println(uri);

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080" + uri)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                createWarning("Couldn't reach server");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        communicationError(response);
                        throw new IOException("Unexpected code " + response);
                    }

                    String groupResponse = responseBody.string();
                    Log.e(TAG, "Joined chatroom " + groupName + " : " + groupResponse);
                    ObjectMapper mapper = new ObjectMapper();
                    ChatroomDTO chatroomDTO = mapper.readValue(groupResponse, ChatroomDTO.class);


                    Location location = new Location("");
                    location.setLatitude(lat);
                    location.setLongitude(lng);

                    GroupChat groupChat = new GroupChat(chatroomDTO.getId(), groupName, GroupChat.GroupType.Geofenced, location, radius);

                    myApp.createGroupChat(groupChat);
                    subscribeToGroup(chatroomDTO.getId());
                }
            }
        });
    }


    private void createChatroom(String groupName, String groupType) {
        OkHttpClient client = new OkHttpClient();

        int userID = CustomApplication.userID;

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/chatrooms/" + groupType.toLowerCase() + "/" + groupName + "/" + userID)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                createWarning("Couldn't reach server");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        communicationError(response);
                        throw new IOException("Unexpected code " + response);
                    }

                    String groupResponse = responseBody.string();
                    Log.e(TAG, "Joined chatroom " + groupResponse);
                    ObjectMapper mapper = new ObjectMapper();
                    ChatroomDTO chatroomDto = mapper.readValue(groupResponse, ChatroomDTO.class);

                    GroupChat groupChat = new GroupChat(chatroomDto.getId(), groupName, GroupChat.GroupType.valueOf(groupType));

                    myApp.createGroupChat(groupChat);
                    subscribeToGroup(chatroomDto.getId());
                }
            }
        });
    }


    private void joinChatroom(Intent intent) {
        int groupId = intent.getExtras().getInt("groupId");
        String groupName = intent.getExtras().getString("groupName");
        String groupType = intent.getExtras().getString("groupType");

        OkHttpClient client = new OkHttpClient();

        int userID = CustomApplication.userID;

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/chatrooms/" + groupId + "/" + userID)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                createWarning("Couldn't reach server");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        communicationError(response);
                        throw new IOException("Unexpected code " + response);
                    }

                    String stringResponse = responseBody.string();
                    Log.e(TAG, "Joined chatroom " + stringResponse);
                    ObjectMapper mapper = new ObjectMapper();
                    ChatroomDTO chatroomDtos = mapper.readValue(stringResponse, ChatroomDTO.class);

                    GroupChat groupChat = null;

                    if (groupType.equals("Geofenced")) {

                        double lat = intent.getExtras().getDouble("lat");
                        double lng = intent.getExtras().getDouble("lng");
                        double radius = intent.getExtras().getDouble("radius");

                        Location location = new Location("");
                        location.setLatitude(lat);
                        location.setLongitude(lng);


                        groupChat = new GroupChat(groupId, groupName, GroupChat.GroupType.Geofenced, location, radius);
                    } else if (groupType.equals("Private")) {
                        groupChat = new GroupChat(groupId, groupName, GroupChat.GroupType.Private);
                    } else if (groupType.equals("Public")) {
                        groupChat = new GroupChat(groupId, groupName, GroupChat.GroupType.Public);

                    } else {
                        return;
                    }

                    myApp.createGroupChat(groupChat);
                    subscribeToGroup(groupId);
                }
            }
        });


    }


    private void createWarning(String warningText) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;

                Toast.makeText(context, warningText, duration).show();
            }
        });

    }


    private void register(String userName, String password) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/accounts/permanent/register/" + userName + "/" + password)
                .build();

        //String response = client.newCall(request).execute().body().string();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                createWarning("Couldn't reach server");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                authenticate(response, userName);
            }
        });
    }


    private void login(String userName, String password) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/accounts/permanent/login/" + userName + "/" + password)
                .build();

        //String response = client.newCall(request).execute().body().string();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                createWarning("Couldn't reach server");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                authenticate(response, userName);
            }
        });
    }

    private void authenticate(Response response, String userName) throws IOException {
        try (ResponseBody responseBody = response.body()) {
            if (!response.isSuccessful()) {
                communicationError(response);
                throw new IOException("Unexpected code " + response);
            }

            String userId = responseBody.string();

            CustomApplication.userID = Integer.parseInt(userId);
            CustomApplication.userName = userName;
            myApp.exitLoginMenu();

            Log.e(TAG, "Got User ID " + CustomApplication.userID);
            SharedPreferences sharedPrefs = getSharedPreferences("com.monkeys123.conversationalist.preferences", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putInt("user_id", CustomApplication.userID);
            editor.putString("user_name", CustomApplication.userName);
            editor.commit();
        }
    }



    @SuppressLint("CheckResult")
    protected void unsubscribeToGroup(long groupID) {
        Log.e(TAG, "Unsubscribing to group " + groupID);
        int userID = CustomApplication.userID;

        Disposable disposable = subscriptions.get(groupID);

        disposable.dispose();

        subscriptions.remove(groupID);

    }


    @SuppressLint("CheckResult")
    protected void subscribeToGroup(long groupID) {
        Log.e(TAG, "Subscribing to group " + groupID);

        if(subscriptions.get(groupID) != null) {
            Log.e(TAG, "User Already subscribed to group " + groupID);
            return;
        }


        if (!stompClient.isConnected()) {
            Log.e(TAG, " Stomp client not Connected");
            checkStompStatus();
        }

        Disposable subscription = stompClient.topic("/topic/" + groupID)
                .subscribe(topicMessage -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ChatMessage chatMessage = mapper.readValue(topicMessage.getPayload(), ChatMessage.class);

                    Log.e(TAG, "Received message " + topicMessage.getPayload());

                    if (chatMessage.getMessageType().equals(ChatMessage.MessageType.Image) ||
                            chatMessage.getMessageType().equals(ChatMessage.MessageType.File)) {
                        Log.e(TAG, "Getting Image/File related to message " + Integer.parseInt(chatMessage.getMessage()));


                    }
                    ((CustomApplication) getApplication()).registerReceivedMessage(chatMessage, groupID, true);
                });

        subscriptions.put(groupID, subscription);
    }

    private void sendLocationMessage(int userID, String userName, Double lat, Double lng, long groupID) {
        try {
            ChatMessageDTO chatMessage = new ChatMessageDTO(userID, userName, "" + lat + "," + lng, ChatMessage.MessageType.Location, "");
            ObjectMapper mapper = new ObjectMapper();

            String jsonString = mapper.writeValueAsString(chatMessage);

            Log.e(TAG, "Sending message " + jsonString);

            checkStompStatus();
            stompClient.send("/app/send/" + groupID, jsonString)
                    .subscribe(() -> {
                        Log.e(TAG, "STOMP echo send successfully");
                    }, throwable -> {
                        Log.e(TAG, "Error send STOMP echo", throwable);
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendImageMessage(int userID, String userName, String fileUri, long groupID) {
        Uri uri = Uri.parse(fileUri);

        DocumentFile documentFile = DocumentFile.fromSingleUri(getApplicationContext(), uri);
        File copied = null;
        File outputDir = getApplicationContext().getCacheDir(); // context being the Activity pointer
        try {
            copied = File.createTempFile("temp", ".extension", outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            InputStream in = getContentResolver().openInputStream(documentFile.getUri());
            OutputStream out = new BufferedOutputStream(new FileOutputStream(copied));

            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", documentFile.getName(),
                        RequestBody.create(MediaType.parse("multipart/form-data"), copied))
                .addFormDataPart("some-field", "some-value")
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/upload")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);

                    Headers responseHeaders = response.headers();

                    String fileId = responseBody.string();

                    sendMsgViaStomp(userID, userName, fileId, groupID, ChatMessage.MessageType.Image, "jpg");
                }
            }
        });
    }


    public void sendFileMessage(int userID, String userName, String fileUri, long groupID) {
        Uri uri = Uri.parse(fileUri);
        int index = uri.toString().lastIndexOf('.');
        String extension = "";
        Log.e("SendFileMessage", uri.toString());
        if(index > 0)
            extension = uri.toString().substring(index + 1);

        DocumentFile documentFile = DocumentFile.fromSingleUri(getApplicationContext(), uri);
        File copied = null;
        File outputDir = getApplicationContext().getCacheDir(); // context being the Activity pointer
        try {
            copied = File.createTempFile("temp", ".extension", outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            InputStream in = getContentResolver().openInputStream(documentFile.getUri());
            OutputStream out = new BufferedOutputStream(new FileOutputStream(copied));

            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", Base64.getEncoder().encodeToString(documentFile.getName().getBytes()),
                        RequestBody.create(MediaType.parse("multipart/form-data"), copied))
                .addFormDataPart("some-field", "some-value")
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/upload")
                .post(requestBody)
                .build();


        String finalExtension = extension;
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);

                    Headers responseHeaders = response.headers();

                    String fileId = responseBody.string();


                    ChatMessage.MessageType t;

                    if (finalExtension.equals("jpeg") || finalExtension.contains("png") || finalExtension.contains("jpg"))
                        t = ChatMessage.MessageType.Image;
                    else {
                        t = ChatMessage.MessageType.File;
                    }

                    sendMsgViaStomp(userID, userName, fileId, groupID, t, finalExtension);
                }
            }
        });


    }

    public void getChatrooms(int userID) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/user/" + userID + "/chatrooms")
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                createWarning("Couldn't reach server");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        communicationError(response);
                        throw new IOException("Unexpected code " + response);
                    }

                    String chatrooms = responseBody.string();

                    Log.e("getting chatrooms", chatrooms);
                    ObjectMapper mapper = new ObjectMapper();
                    ChatroomDTO[] chatroomDtos = mapper.readValue(chatrooms, ChatroomDTO[].class);

                    for (ChatroomDTO chatroomDTO : chatroomDtos) {
                        GroupChat groupChat = null;

                        if (chatroomDTO.getType().equals(GroupChat.GroupType.Geofenced)) {
                            Location location = new Location("");
                            location.setLatitude(chatroomDTO.getLat());
                            location.setLongitude(chatroomDTO.getLng());

                            groupChat = new GroupChat(chatroomDTO.getId(), chatroomDTO.getName(), chatroomDTO.getType(), location, chatroomDTO.getRadius());
                        } else {
                            groupChat = new GroupChat(chatroomDTO.getId(), chatroomDTO.getName(), chatroomDTO.getType());

                        }

                        if (myApp.createGroupChat(groupChat))
                            subscribeToGroup(chatroomDTO.getId());

                        if (myApp.currentActivity instanceof MainActivity) {
                            MainActivity activity = (MainActivity) myApp.currentActivity;
                            activity.updateAllRooms();
                        }
                    }
                }
            }
        });

    }

    public void getOlderMessages(long groupID, int oldestId) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/messages/older/" + groupID + "/" + oldestId)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        communicationError(response);
                        throw new IOException("Unexpected code " + response);
                    }

                    Headers responseHeaders = response.headers();

                    String messages = responseBody.string();

                    Log.e("getting older messages", messages);
                    ObjectMapper mapper = new ObjectMapper();
                    ChatMessage[] chatMessages = mapper.readValue(messages, ChatMessage[].class);
                    ChatMessage message;

                    for (int i = chatMessages.length - 1; i >= 0; i--) {
                        message = chatMessages[i];

                        if (message.getMessageType().equals(ChatMessage.MessageType.Image) ||
                                message.getMessageType().equals(ChatMessage.MessageType.File)) {
                            Log.e(TAG, "Getting Image/File related to message " + Integer.parseInt(message.getMessage()));

                        }
                        ((CustomApplication) getApplication()).registerReceivedMessage(message, groupID, false);
                    }
                    myApp.groups.get(groupID).setPreloaded(true);
                }
            }
        });
    }

    public void getLatestMessages() {
        for (GroupChat g : myApp.groups.values()) {
            getOlderMessages(g.getGroupID(), -1);
        }
    }

    private void searchRooms(String searchText, Double lat, Double lng) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/search/" + searchText)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SearchRooms", "Couldn't reach server");
                createWarning("Couldn't reach server");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        communicationError(response);
                        throw new IOException("Unexpected code " + response);
                    }

                    //throw new IOException("Unexpected code " + response);

                    String chatrooms = responseBody.string();

                    Log.e("searching rooms", chatrooms);
                    ObjectMapper mapper = new ObjectMapper();
                    ChatroomDTO[] chatroomDtos = mapper.readValue(chatrooms, ChatroomDTO[].class);

                    ArrayList<ChatroomDTO> rooms = new ArrayList<ChatroomDTO>(Arrays.asList(chatroomDtos));
                    ArrayList<ChatroomDTO> rooms2 = new ArrayList<ChatroomDTO>();
                    Location l = new Location("");
                    l.setLatitude(lat);
                    l.setLongitude(lng);


                    for (ChatroomDTO r : rooms) {
                        if (r.getType() == GroupChat.GroupType.Geofenced) {
                            Location location = new Location("");
                            location.setLatitude(r.getLat());
                            location.setLongitude(r.getLng());
                            float distanceInMeters = location.distanceTo(CustomApplication.latestLatLng);

                            if (!(distanceInMeters > r.getRadius()))
                                continue;
                        }

                        rooms2.add(r);
                    }

                    myApp.addChatrooms(rooms2);

                    if (rooms.size() == 0)
                        createWarning("No groups found.");
                }
            }
        });
    }

    private void communicationError(Response r) {
        createWarning("Error communicating with server, code: "
                + r.code() + " " + r.message());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("sendMsg")) {
                Log.e("OnStart", "Sending Message");
                int userID = intent.getExtras().getInt("userID");
                String userName = intent.getExtras().getString("userName");
                String text = intent.getExtras().getString("textMessage");
                long groupID = intent.getExtras().getLong("groupID");
                sendMsgViaStomp(userID, userName, text, groupID, ChatMessage.MessageType.Text, "");

            } else if (intent.getAction().equals("sendFile")) {
                Log.e("OnStart", "Sending File Message");
                int userID = intent.getExtras().getInt("userID");
                String userName = intent.getExtras().getString("userName");
                String fileUri = intent.getExtras().getString("fileUri");
                long groupID = intent.getExtras().getLong("groupID");
                sendFileMessage(userID, userName, fileUri, groupID);
            } else if (intent.getAction().equals("sendLocation")) {
                int userID = intent.getExtras().getInt("userID");
                String userName = intent.getExtras().getString("userName");
                Double lat = intent.getExtras().getDouble("lat");
                Double lng = intent.getExtras().getDouble("lng");
                long groupID = intent.getExtras().getLong("groupID");
                sendLocationMessage(userID, userName, lat, lng, groupID);
            } else if (intent.getAction().equals("sendImage")) {
                Log.e("OnStart", "Sending Image Message");
                int userID = intent.getExtras().getInt("userID");
                String userName = intent.getExtras().getString("userName");
                String fileUri = intent.getExtras().getString("fileUri");
                long groupID = intent.getExtras().getLong("groupID");
                sendImageMessage(userID, userName, fileUri, groupID);
            } else if (intent.getAction().equals("getMessages")) {
                Log.e("OnStart", "Getting Older Messages");
                long groupID = intent.getExtras().getLong("groupID");
                int oldestID = intent.getExtras().getInt("oldestID");
                getOlderMessages(groupID, oldestID);

            } else if (intent.getAction().equals("login")) {
                String userName = intent.getExtras().getString("username");
                String password = intent.getExtras().getString("password");
                Log.e("OnStart", "Getting User ID");
                login(userName, password);

            } else if (intent.getAction().equals("register")) {
                String username = intent.getExtras().getString("username");
                Log.e("OnStart", "Getting User ID");

                if (intent.hasExtra("password")) {
                    String password = intent.getExtras().getString("password");
                    register(username, password);
                } else {
                    registerNoPassword(username);
                }
            } else if (intent.getAction().equals("createGroup")) {
                Log.e("OnStart", "Creating chatroom");
                String groupName = intent.getExtras().getString("groupName");
                String groupType = intent.getExtras().getString("groupType");

                if (groupType.equals("Geofenced")) {
                    double lat = intent.getExtras().getDouble("lat");
                    double lng = intent.getExtras().getDouble("lng");
                    double radius = intent.getExtras().getDouble("radius");

                    createGeoChatroom(groupName, lat, lng, radius);
                } else {

                    createChatroom(groupName, groupType);
                }


            } else if (intent.getAction().equals("joinGroup")) {
                Log.e("OnStart", "Joining chatroom");
                joinChatroom(intent);

            } else if (intent.getAction().equals("getRooms")) {
                Log.e("OnStart", "Getting all chatrooms");
                int userId = intent.getExtras().getInt("userId");
                getChatrooms(userId);
            } else if (intent.getAction().equals("unsubRoom")) {
                Log.e("OnStart", "Unsubbing to room");
                long roomId = intent.getExtras().getLong("roomId");
                unsubscribeToGroup(roomId);
            } else if (intent.getAction().equals("subRoom")) {
                Log.e("OnStart", "subbing to room");
                long roomId = intent.getExtras().getLong("roomId");
                subscribeToGroup(roomId);

            } else if (intent.getAction().equals("searchRooms")) {
                Log.e("OnStart", "Searching chatrooms");
                String searchText = intent.getExtras().getString("searchText");
                double lng = intent.getExtras().getDouble("lng");
                double lat = intent.getExtras().getDouble("lat");

                searchRooms(searchText, lat, lng);
            } else if (intent.getAction().equals("logout")) {
                Log.e("OnStart", "Logging out by terminating stomp client.");
                logout();
            } else if (intent.getAction().equals("getLatestMessages")) {
                Log.e("OnStart", "Getting latest messages from all groups.");
                getLatestMessages();
            } else if (intent.getAction().equals("reportMessage")) {
                Log.e("OnStart", "Reporting message");
                int chatOrder = intent.getExtras().getInt("chatOrder");
                int searchText = intent.getExtras().getInt("groupId");

                reportMessage();
            }else if (intent.getAction().equals("upgradeAccount")) {
                Log.e("OnStart", "Upgrading Account");
                int userid = CustomApplication.userID;
                String password = intent.getExtras().getString("password");

                upgradeAccount(userid, password);
            }
        }

        return START_STICKY; // or whatever floats your boat
    }

    private void upgradeAccount(int id, String password) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/accounts/permanent/upgrade/" + id + "/" + password)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                createWarning("Couldn't reach server");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        communicationError(response);
                        throw new IOException("Unexpected code " + response);
                    }

                    myApp.exitUpgradeMenu();

                    Log.e(TAG, "Got User ID " + CustomApplication.userID);
                }
            }
        });
    }

    private void reportMessage() {
    }

    private void registerNoPassword(String username) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/accounts/permanent/login/" + username)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                createWarning("Couldn't reach server");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                authenticate(response, username);
            }
        });
    }

    private void logout() {
        stompClient.disconnect();
    }
}
