package com.monkeys123.conversationalist;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.google.type.LatLng;
import com.monkeys123.conversationalist.ChatroomListingRecyclerView.SearchGroupActivity;
import com.monkeys123.conversationalist.Data.ChatMessage;
import com.monkeys123.conversationalist.Data.GroupChat;
import com.monkeys123.conversationalist.MessageCache.CacheKey;
import com.monkeys123.conversationalist.MessageCache.MessageCache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class CustomApplication extends Application {
    public static final String CHILD_DIR = "files";

    public ConcurrentHashMap<Long, GroupChat> groups = new ConcurrentHashMap<>();
    public MessageCache cache;

    Activity currentActivity = null;
    public static int userID = -1;
    public static String userName = "";

    public static Location latestLatLng;
    public boolean unmetered = false;

    private LocationManager locationManager;
    private LocationListener listener;

    public void openLocationActivity(ChatMessage message) {

        String msg = message.getMessage();
        String[] lat_lng = msg.split(",");
        double latitude = Double.parseDouble(lat_lng[0]);
        double longitude = Double.parseDouble(lat_lng[1]);

        ChatGroupActivity chatGroupActivity = (ChatGroupActivity) currentActivity;
        chatGroupActivity.openMap(latitude, longitude);;
    }

    public void openFileActivity(ChatMessage message) {
        ChatGroupActivity chatGroupActivity = (ChatGroupActivity) currentActivity;
        chatGroupActivity.openFile(message);
    }


    public void removeMessage(long groupId, ChatMessage message) {
        try {
            groups.get(groupId).removeMessage(message);
        } catch (NullPointerException e) {
            Log.e("CustomApplication", "No group exists with id: " + groupId + ". Failed to remove message.");
        }
    }

    public void registerReceivedMessage(ChatMessage chatMessage, long groupID, boolean notify) {
        // Check if message was already registered
        Log.e("Msg Register", "I am trying to register a msg " + chatMessage.getMessage());
        CacheKey key = new CacheKey(groupID, chatMessage.getChatOrder());

        ChatMessage prev = cache.get(key);

        if (prev == null) {
            //message hasn't been handled yet
            cache.put(key, chatMessage);
        } else {
            chatMessage = prev;
        }

        if (unmetered &&
                ((chatMessage.getMessageType() == ChatMessage.MessageType.File)
                        || (chatMessage.getMessageType() == ChatMessage.MessageType.Image))
                && chatMessage.getDownloadedFile() == null) {

            chatMessage.downloadFile(currentActivity, groupID);
        }

        if (groups.get(groupID).appendMessage(chatMessage) && notify)
            notifyUser(chatMessage, groupID);

        if (currentActivity != null && currentActivity instanceof ChatGroupActivity) {
            Log.e("AddingMessageViews", "" + groupID + ":" + groups.get(groupID).getMessages().size());
            ((ChatGroupActivity) currentActivity).updateMessageView();
        }
    }

    public boolean isLoggedIn(){
        return userID != -1;
    }



    public void notifyUser(ChatMessage chatMessage, long groupID){
        if (currentActivity != null && currentActivity instanceof ChatGroupActivity) {
            if(((ChatGroupActivity) currentActivity).groupID == groupID)
                return;
        }

        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(this, ChatGroupActivity.class);
        resultIntent.putExtra("chat_name", groups.get(groupID).getGroupName());
        resultIntent.putExtra("chat_id", groupID);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String notification_title = "ConversationalIST Group " + groups.get(groupID).getGroupName();
        String notification_message = chatMessage.getMessage() + " from " + chatMessage.getUserName();

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.nsa_logo)
                        .setContentTitle(notification_title)
                        .setContentText(notification_message);
        mBuilder.setContentIntent(resultPendingIntent);


        int mNotificationId = (int) System.currentTimeMillis();
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel("NOTIFICATION_CHANNEL_ID", "NOTIFICATION_CHANNEL_NAME", importance);

            mBuilder.setChannelId("NOTIFICATION_CHANNEL_ID");
            mNotifyMgr.createNotificationChannel(notificationChannel);
        }
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

    }


    public void login(String username, String password) {
        userName = username;

        Intent i = new Intent(this, CommunicationService.class);
        i.setAction("getUID");
        i.putExtra("userName", username);
        i.putExtra("password", password);
        startService(i);
    }

    public void sendTextMessage(String text, long groupID) {
        Intent i = new Intent(this, CommunicationService.class);
        i.setAction("sendMsg");
        i.putExtra("userID", userID);
        i.putExtra("userName", userName);
        i.putExtra("textMessage", text);
        i.putExtra("groupID", groupID);
        startService(i);
    }

    public void sendLocationMessage(LatLng location, long groupID) {
        Intent i = new Intent(this, CommunicationService.class);
        i.setAction("sendLocation");
        i.putExtra("userID", userID);
        i.putExtra("userName", userName);
        i.putExtra("lat", location.getLatitude());
        i.putExtra("lng", location.getLongitude());
        i.putExtra("groupID", groupID);
        startService(i);
    }

    public void sendFileMessage(Uri fileUri, long groupID) {
        DocumentFile documentFile = DocumentFile.fromSingleUri(getApplicationContext(), fileUri);

        String fileName = documentFile.getName();

        Intent i = new Intent(this, CommunicationService.class);
        i.setAction("sendFile");
        i.putExtra("userID", userID);
        i.putExtra("userName", userName);
        i.putExtra("fileUri", fileUri.toString());
        i.putExtra("groupID", groupID);
        startService(i);
    }

    public void sendImageMessage(Uri imageUri, long groupID) {
        DocumentFile documentFileImage = DocumentFile.fromSingleUri(getApplicationContext(), imageUri);
        String imageName = documentFileImage.getName();

        Intent i = new Intent(this, CommunicationService.class);
        i.setAction("sendImage");
        i.putExtra("userID", userID);
        i.putExtra("userName", userName);
        i.putExtra("fileUri", imageUri.toString());
        i.putExtra("groupID", groupID);
        startService(i);
    }

    public void setCurrentActivity(Activity activity) {
        currentActivity = activity;
    }

    public void addChatrooms(ArrayList<ChatroomDTO> rooms) {
        ((SearchGroupActivity) currentActivity).addChatrooms(rooms);
    }

    public Activity getCurrentActivity() {
        return currentActivity;
    }

    public boolean createGroupChat(GroupChat groupChat) {
        if(groups.containsKey(groupChat.getGroupID()))
            return false;

        groups.put(groupChat.getGroupID(), groupChat);

        if (currentActivity instanceof MainActivity) {
            ((MainActivity) currentActivity).addGroupChat(groupChat);
        }
        return true;
    }


    public void exitLoginMenu() {
        if (currentActivity instanceof LoginActivity) {
            currentActivity.finish();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences sharedPrefs = getSharedPreferences("com.monkeys123.conversationalist.preferences", MODE_PRIVATE);
        userID = sharedPrefs.getInt("user_id", userID);
        userName = sharedPrefs.getString("user_name", userName);

         cache = new MessageCache(128, this);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                boolean previous = unmetered;
                unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);

                if (unmetered && (previous != unmetered)) {
                    getMessages();
                    getRooms();
                }
            }
        };

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(ConnectivityManager.class);

        connectivityManager.requestNetwork(networkRequest, networkCallback);
        Network currentNetwork = connectivityManager.getActiveNetwork();
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);

        if (caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
            unmetered = true;
        }

        createGPSListener();
    }

    private void getRooms() {
        Intent intent = new Intent(this, CommunicationService.class);
        intent.setAction("getRooms");
        intent.putExtra("userId", userID);
        startService(intent);
    }

    public void getMessages() {
        //I cry because tf do I do if Im reading older msgs
        if (currentActivity instanceof ChatGroupActivity) {

        } else {
            //preload msgs
            Intent intent = new Intent(this, CommunicationService.class);
            intent.setAction("getLatestMessages");
            startService(intent);
        }
    }

    public void createGPSListener(){

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
               currentActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                        ,10);
            }
            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);


        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.e("GPS","\n " + location.getLongitude() + " " + location.getLatitude());
                latestLatLng = location;

                if(currentActivity instanceof ChatGroupActivity){
                    GroupChat groupChat = groups.get(((ChatGroupActivity) currentActivity).getGroupID());
                    Location groupLocation = groupChat.getLocation();
                    double radius = groupChat.getRadius();

                    ((ChatGroupActivity) currentActivity).verifyLocation(location, groupLocation, radius);
                }

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

        locationManager.requestLocationUpdates("gps", 20000, 0, listener);


    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public static CustomApplication getInstance(Context context) {
        return (CustomApplication) context.getApplicationContext();
    }



    public Uri getCacheFileUri(File directory, String fileName) {
        File newFile = new File(directory, fileName);
        return FileProvider.getUriForFile(this, getPackageName() + ".provider", newFile);
    }

    public Pair<File, String> getCopy(Uri uri) {
        DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
        File copied = null;
        File outputDir = new File(getApplicationContext().getCacheDir(), CustomApplication.CHILD_DIR); // context being the Activity pointer
        outputDir.mkdirs();

        Log.e("getCopy", "" + outputDir);

        copied = new File(outputDir + "/" + documentFile.getName());

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

        return new Pair<>(outputDir, documentFile.getName());
    }

    public File saveFile(InputStream inputStream, String fileName) {

        File outputDir = new File(getApplicationContext().getCacheDir(), CustomApplication.CHILD_DIR);
        File newFile = new File(outputDir + "/" + fileName);;

        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile));

            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return outputDir;
    }

    public void exitUpgradeMenu() {
        if (currentActivity instanceof UpgradeAccountActivity) {
            currentActivity.finish();
        }
    }
}
