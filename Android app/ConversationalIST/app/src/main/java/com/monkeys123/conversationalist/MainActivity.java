package com.monkeys123.conversationalist;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.monkeys123.conversationalist.ChatroomListingRecyclerView.RecyclerViewAdapterRooms;
import com.monkeys123.conversationalist.ChatroomListingRecyclerView.SearchGroupActivity;
import com.monkeys123.conversationalist.Data.GroupChat;

import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {
    CustomApplication myApp;

    private LocationManager locationManager;
    private LocationListener listener;
    private RecyclerView recyclerView;
    private ChatlistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //! Fix this, allows service to have http connection in main thread
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);

        myApp = (CustomApplication) this.getApplicationContext();
        myApp.setCurrentActivity(this);


        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.chatlist_actionbar);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.statusbar_custom));

        Intent intent = new Intent(this, CommunicationService.class);
        //startService(intent);
        getApplicationContext().startForegroundService(intent);
        SharedPreferences sharedPrefs = getSharedPreferences("com.monkeys123.conversationalist.preferences", MODE_PRIVATE);
        System.out.println(CustomApplication.userID);
        CustomApplication.userID = sharedPrefs.getInt("user_id", CustomApplication.userID);
        System.out.println(CustomApplication.userID);
        CustomApplication.userName = sharedPrefs.getString("user_name", CustomApplication.userName);


        if (CustomApplication.userID == -1) {
            Intent loginActivity = new Intent(this, LoginActivity.class);
            startActivity(loginActivity);
        }

        recyclerView = findViewById(R.id.main_scroll_container);
        adapter = new ChatlistAdapter(this, new ArrayList<GroupChat>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);


        FloatingActionButton add_groupchat = (FloatingActionButton) findViewById(R.id.add_groupchat_floating_button);
        FloatingActionButton search_groupchat = (FloatingActionButton) findViewById(R.id.search_groupchat_floating_button);

        add_groupchat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CreateGroupActivity.class);
                startActivity(intent);
            }
        });

        search_groupchat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SearchGroupActivity.class);
                startActivity(intent);
            }
        });

        FloatingActionButton create_perma_account = (FloatingActionButton) findViewById(R.id.create_permanent_account);
        create_perma_account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UpgradeAccountActivity.class);
                startActivity(intent);
            }
        });

        ImageButton logout_button = (ImageButton) findViewById(R.id.logout_button);
        logout_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Do you want to logout?")
                        .setTitle(R.string.dialog_title)
                        .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                CustomApplication.userID = -1;
                                SharedPreferences sharedPrefs = getSharedPreferences("com.monkeys123.conversationalist.preferences", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPrefs.edit();
                                editor.putInt("user_id", CustomApplication.userID);
                                editor.commit();
                                adapter.clear();
                                adapter.notifyDataSetChanged();
                                myApp.groups.clear();
                                Intent intent = new Intent(MainActivity.this, CommunicationService.class);
                                intent.setAction("logout");
                                startService(intent);

                                intent = new Intent(MainActivity.this, LoginActivity.class);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // User cancelled report. Nothing to do
                            }
                        })
                        .create()
                        .show();
            }
        });

        getChatrooms();
        System.out.println("this is the way onCreate");

        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();

        if(appLinkData != null){
            String groupId = appLinkData.getLastPathSegment();


            Log.e("MAINACTIVITY","app link intent=" + groupId);

            Intent joinIntent = new Intent(MainActivity.this, CommunicationService.class);
            joinIntent.setAction("joinGroup");
            joinIntent.putExtra("groupId", Integer.parseInt(groupId));
            startService(joinIntent);
        }
    }


    public void updateAllRooms() {
        ArrayList<GroupChat> chats = new ArrayList<>(myApp.groups.values());
        if (myApp.unmetered)
            myApp.getMessages();

        adapter.setRooms(chats);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }


    public void getChatrooms() {
        Intent intent = new Intent(this, CommunicationService.class);
        intent.setAction("getRooms");
        intent.putExtra("userId", CustomApplication.userID);

        startService(intent);
    }

    public void addGroupChat(GroupChat groupChat) {
        Log.e("add group", groupChat.getGroupName());

        adapter.add(groupChat);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        myApp.setCurrentActivity(this);

        System.out.println("this is the way onResume");
        //updateAllRooms();
        handleIntent(getIntent());
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        System.out.println("this is the way onRestart");
        updateAllRooms();
        //these 2 calls are causing double up on requests, gotta research this
        //getChatrooms();
        //drawAllChatgroups();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        clearReference();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        clearReference();
        super.onDestroy();

    }

    private void clearReference() {
        Activity activity = myApp.getCurrentActivity();
        if (this.equals(activity)) {
            myApp.setCurrentActivity(null);
        }
    }

    protected void handleIntent(Intent intent) {
        String linkAction = intent.getAction();
        Uri linkData = intent.getData();

        if (!(Objects.equals(linkAction, Intent.ACTION_VIEW)) || linkData == null)
            return;


        String linkDataStr = linkData.toString();

        if (!linkDataStr.contains("group_name") && !linkDataStr.contains("group_id"))
            return;

        // TODO tried with linkData.getQueryParameter("group_name"), it didn't work for some reason
        String groupName = linkDataStr.substring(linkDataStr.lastIndexOf("group_name") + "group_name=".length(), linkDataStr.lastIndexOf("&"));

        Log.e("handleIntent", groupName);

        // TODO idk what im doing this needs fixing maybe. Idk if the problem's here

        Intent serviceIntent = new Intent(this, CommunicationService.class);
        serviceIntent.setAction("createGroup");
        serviceIntent.putExtra("groupName", groupName);
        startService(serviceIntent);
    }
}