package com.monkeys123.conversationalist;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.type.LatLng;
import com.monkeys123.conversationalist.Data.GroupChat;

import java.io.IOException;
import java.util.List;

public class CreateGroupActivity extends AppCompatActivity {
    CustomApplication myApp;

    String newGroupName = "";
    GroupChat.GroupType groupType = GroupChat.GroupType.Public;
    Location location = null;

    TextView locationTextBox;
    TextView radiusTextBox;

    LinearLayout geofenced_container = null;
    View pick_location_button = null;

    boolean orientation_portrait = true;

    LatLng latLng = null;
    double radius = 0D;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.create_group);
        } else {
            setContentView(R.layout.create_group_rotated);
            orientation_portrait = false;
        }

        myApp = (CustomApplication) this.getApplicationContext();
        myApp.setCurrentActivity(this);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.creategroup_actionbar);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.statusbar_custom));

        View view = getSupportActionBar().getCustomView();
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

        TextView newgroup_logo = (TextView) findViewById(R.id.newgroup_logo);
        EditText newgroup_name_edittext = (EditText) findViewById(R.id.new_groupname_edit_text);

        newgroup_name_edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty()) {
                    newgroup_logo.setText(R.string.heart);
                } else {
                    newgroup_logo.setText(createLogo(s.toString().trim()));
                }
                newGroupName = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        View creategroup_button = findViewById(R.id.creategroup_button);
        creategroup_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newGroupName.isEmpty()) {
                    Toast.makeText(CreateGroupActivity.this, "Please name the new group!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(groupType == GroupChat.GroupType.Geofenced && latLng == null){
                    Toast.makeText(CreateGroupActivity.this, "Please insert a location!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(myApp.getCurrentActivity(), CommunicationService.class);
                intent.setAction("createGroup");
                intent.putExtra("groupName", newGroupName);
                intent.putExtra("groupType", groupType.toString());

                if(groupType == GroupChat.GroupType.Geofenced){
                    intent.putExtra("lat", latLng.getLatitude());
                    intent.putExtra("lng", latLng.getLongitude());
                    intent.putExtra("radius", radius);
                }

                startService(intent);
                finish();
            }
        });

        geofenced_container = (LinearLayout) findViewById(R.id.newgroup_geo_fenced_container);
        pick_location_button = findViewById(R.id.newgroup_pick_location);

        locationTextBox = (TextView) findViewById(R.id.group_location);
        radiusTextBox = (TextView) findViewById(R.id.group_location_radius);

        pick_location_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(locationTextBox.getText() == "" || radiusTextBox.getText() == ""){
                    Toast.makeText(CreateGroupActivity.this, "Please insert an address and radius", Toast.LENGTH_SHORT).show();
                    return;
                }

                latLng = getLongLatFromAddress(locationTextBox.getText().toString());
                radius = Double.parseDouble(radiusTextBox.getText().toString());
                if(latLng != null)
                    Log.e("ADDRESS", "from=" + locationTextBox.getText().toString() + "lat=" + latLng.getLatitude() +  "long=" + latLng.getLongitude());
            }
        });
    }

    public LatLng getLongLatFromAddress(String addressInput) {
        Geocoder coder = new Geocoder(getApplicationContext());
        List<Address> address;
        LatLng p1 = null;

        try {
            // May throw an IOException
            address = coder.getFromLocationName(addressInput, 5);
            if (address == null) {
                return null;
            }

            Address location = address.get(0);
            p1 = LatLng.newBuilder().
                    setLatitude(location.getLatitude()).
                    setLongitude(location.getLongitude()).
                    build();

        } catch (IOException ex) {

            ex.printStackTrace();
        }

        return p1;
    }

    public void onGroupTypeChoice(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.newgroup_public_choice:
                if (checked) {
                    groupType = GroupChat.GroupType.Public;
                    geofenced_container.setVisibility(View.GONE);
                }
                break;
            case R.id.newgroup_private_choice:
                if (checked) {
                    groupType = GroupChat.GroupType.Private;
                    geofenced_container.setVisibility(View.GONE);
                }
                break;
            case R.id.newgroup_geo_fenced_choice:
                if (checked) {
                    View _view = findViewById(R.id.create_group_scrollview);
                    if (_view != null) {
                        ScrollView scrollView = (ScrollView) _view;
                        scrollView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        },100);
                    }
                    groupType = GroupChat.GroupType.Geofenced;
                    geofenced_container.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    private String createLogo(String groupName) {
        long word_count = groupName.chars().filter(ch -> ch == ' ').count() + 1;
        if (word_count == 1) {
            return "" + Character.toUpperCase(groupName.charAt(0)) + Character.toUpperCase(groupName.charAt(groupName.length() - 1));
        } else {
            return "" + Character.toUpperCase(groupName.charAt(0)) + Character.toUpperCase(groupName.charAt(groupName.lastIndexOf(' ') + 1));
        }
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
}