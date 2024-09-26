package com.monkeys123.conversationalist.ChatroomListingRecyclerView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.monkeys123.conversationalist.ChatroomDTO;
import com.monkeys123.conversationalist.CommunicationService;
import com.monkeys123.conversationalist.CustomApplication;
import com.monkeys123.conversationalist.R;
import com.monkeys123.conversationalist.RecyclerViewButtonInterface;

import java.util.ArrayList;

public class SearchGroupActivity extends AppCompatActivity implements RecyclerViewButtonInterface {
    CustomApplication myApp;
    EditText groupName;
    ImageButton searchButton;
    private EditText searchTextBox;
    private RecyclerView recyclerView;
    private RecyclerViewAdapterRooms adapter;

    private ArrayList<ChatroomDTO> chatrooms = new ArrayList<>();


    public void addChatrooms(ArrayList<ChatroomDTO> rooms) {
        chatrooms.clear();
        chatrooms.addAll(rooms);
        adapter.setRooms(rooms);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_group);

        myApp = (CustomApplication) this.getApplicationContext();
        myApp.setCurrentActivity(this);

        View view;
            //= findViewById(R.id.search_textbox_groups);
        searchButton = findViewById(R.id.search_button);
        searchTextBox = findViewById(R.id.search_text);

        recyclerView = findViewById(R.id.search_results);
        adapter = new RecyclerViewAdapterRooms(this, new ArrayList<ChatroomDTO>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);


        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.searchgroup_actionbar);

        getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.statusbar_custom));
        view = getSupportActionBar().getCustomView();
        LinearLayout backbuttonarea = (LinearLayout) view.findViewById(R.id.creategroup_back_area);
        ImageButton backbutton = (ImageButton) view.findViewById(R.id.creategroup_back);


        backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        backbuttonarea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if no view has focus:
                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                String searchText = searchTextBox.getText().toString();
                Log.e("SearchActivity", "Searching for group: " + searchText);

                Intent intent = new Intent(SearchGroupActivity.this, CommunicationService.class);
                intent.setAction("searchRooms");
                intent.putExtra("searchText", searchText);
                intent.putExtra("lat", myApp.latestLatLng.getLatitude());
                intent.putExtra("lng", myApp.latestLatLng.getLongitude());
                startService(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        myApp.setCurrentActivity(this);
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

    @Override
    public void onClick(int position) {
        Log.e("SearchGroupActivity", "Join Group Registered");

        Log.e("SearchGroupActivity", "Join Group Registered" + chatrooms.get(position).getName());

        Intent intent = new Intent(SearchGroupActivity.this, CommunicationService.class);
        intent.setAction("joinGroup");
        intent.putExtra("groupId", chatrooms.get(position).getId());
        intent.putExtra("groupName", chatrooms.get(position).getName());
        intent.putExtra("groupType", chatrooms.get(position).getType().toString());

        intent.putExtra("lat", chatrooms.get(position).getLat());
        intent.putExtra("lng", chatrooms.get(position).getLng());
        intent.putExtra("radius", chatrooms.get(position).getRadius());

        startService(intent);
    }
}