package com.monkeys123.conversationalist;

import android.Manifest;
import android.app.Activity;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.type.LatLng;
import com.monkeys123.conversationalist.Data.ChatMessage;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Executor;

public class ChatGroupActivity extends AppCompatActivity {
    CustomApplication myApp;

    TreeSet<Integer> drawnMessages = new TreeSet<>();
    Uri imageUri;

    boolean sendingLocation = false;

    Location currentLocation = null;

    private String textBoxText;

    int lastUserID = 0;
    String chatName;

    public long getGroupID() {
        return groupID;
    }

    long groupID;

    long lastMsgID = 0;

    String chatType = "";

    final int RESULT_LOAD_IMG = 1;
    final int RESULT_LOAD_FILE = 2;
    final int RESULT_OPEN_FILE = 3;

    private RecyclerView recyclerView;
    private RecyclerViewAdapterRoom adapter;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            String savedText = savedInstanceState.getString("text_box_text");
            if (savedText != null)
                textBoxText = savedText;
        }
        setContentView(R.layout.chat_group);

        myApp = (CustomApplication) this.getApplicationContext();
        myApp.setCurrentActivity(this);

        chatName = getIntent().getExtras().getString("chat_name");
        groupID = getIntent().getExtras().getLong("chat_id");
        chatType = getIntent().getExtras().getString("chatType");
        recyclerView = findViewById(R.id.messages_scroll);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Log.d("-----", "end");
                    getOlderMessages();
                }
            }
        });

        getMostRecentMessages();

        ArrayList<ChatMessage> msgs = new ArrayList<ChatMessage>(myApp.groups.get(groupID).getMessages());
        adapter = new RecyclerViewAdapterRoom(this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        recyclerView.setLayoutManager(manager);

        recyclerView.setAdapter(adapter);
        adapter.submitList(msgs);


        /*for (ChatMessage chatMessage : myApp.groups.get(groupID).messages) {
            Log.e("ChatGroupActivity", chatMessage.getMessage() + " ----->>>>>this is the msg");
            appendMessageView(chatMessage);
        }*/

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.chatgroup_actionbar);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.statusbar_custom));

        View view = getSupportActionBar().getCustomView();
        LinearLayout backbuttonarea = (LinearLayout) view.findViewById(R.id.chatgroup_backarea);
        ImageButton backbutton = (ImageButton) view.findViewById(R.id.chatgroup_back);
        TextView chatGroupName = (TextView) view.findViewById(R.id.chatgroup_name);
        ImageButton share_button = (ImageButton) view.findViewById(R.id.group_share_button);
        chatGroupName.setText(chatName);

        view = (View) findViewById(R.id.include_msg_send);

        ImageButton add_image_button = (ImageButton) view.findViewById(R.id.take_photo_button);
        ImageButton send_message_button = (ImageButton) view.findViewById(R.id.send_message);
        ImageButton add_file_button = (ImageButton) view.findViewById(R.id.add_file_button);

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

        share_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareGroupActivity();
            }
        });

        add_image_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getImage();
            }
        });

        add_file_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFile();
            }
        });

        EditText editText = (EditText) findViewById(R.id.message_box);
        editText.setText(textBoxText);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textBoxText = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        EditText locationTextBox = (EditText) findViewById(R.id.location_to_send);
        findViewById(R.id.my_location_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocation != null)
                    sendLocationMessage(LatLng.newBuilder()
                            .setLatitude(currentLocation.getLatitude())
                            .setLongitude(currentLocation.getLongitude())
                            .build());
            }
        });

        send_message_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendingLocation) {
                    if (locationTextBox.getText().length() == 0) {
                        Toast.makeText(ChatGroupActivity.this, "Please insert an address and radius", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    LatLng latLng = getLongLatFromAddress(locationTextBox.getText().toString());
                    if (latLng != null)
                        Log.e("ADDRESS", "from=" + locationTextBox.getText().toString() + "lat=" + latLng.getLatitude() + "long=" + latLng.getLongitude());

                    sendLocationMessage(latLng);
                    return;
                }
                if (editText.getText().toString().isEmpty()) {
                    return;
                }
                String textMessage = editText.getText().toString();
                sendTextMessage(textMessage);
                editText.setText("");
            }
        });

        findViewById(R.id.send_location_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View location_container = findViewById(R.id.location_message_maker_container);
                location_container.setVisibility(location_container.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                sendingLocation = !sendingLocation;
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

    public void verifyLocation(Location location, Location groupLocation, double radius) {
        currentLocation = location;
        Log.e("Verify Location", "AHSDh asdsadsadsadsadsa");
        if (chatType != null && chatType.equals("Geofenced")) {
            float distanceInMeters = groupLocation.distanceTo(location);
            Log.e("Verify Location", "why the fuck");
            if (distanceInMeters > radius) {
                Intent chat_group = new Intent(this, CommunicationService.class);
                chat_group.setAction("unsubRoom");
                chat_group.putExtra("roomId", groupID);
                startService(chat_group);
                finish();
            }
        }
    }

    private void getImage() {
        capturePhoto();
        /*Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        Intent.createChooser(photoPickerIntent, "Choose a Photo");
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);*/
    }

    private void getFile() {
        Intent filePickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        filePickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerIntent.setType("*/*");
        filePickerIntent = Intent.createChooser(filePickerIntent, "Choose a File");
        startActivityForResult(filePickerIntent, RESULT_LOAD_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2000) {
            sendImageMessage(imageUri);
            Log.e("Camera", imageUri.toString());
            return;
        }

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_LOAD_IMG:
                    final Uri imageUri = data.getData();

                    Pair<File, String> imageFile = myApp.getCopy(imageUri);
                    Uri copyImageUri = myApp.getCacheFileUri(imageFile.first, imageFile.second);

                    sendImageMessage(copyImageUri);
                    break;
                case RESULT_LOAD_FILE:
                    Uri fileUri = data.getData();

                    Pair<File, String> file = myApp.getCopy(fileUri);
                    Uri fileCopyUri = myApp.getCacheFileUri(file.first, file.second);

                    sendFileMessage(fileCopyUri);
                    break;
                case RESULT_OPEN_FILE:
                    // Nothing to do
                    break;
            }
        } else {
            if (requestCode == RESULT_LOAD_IMG)
                Toast.makeText(this, "You haven't picked an Image", Toast.LENGTH_LONG).show();
            if (requestCode == RESULT_LOAD_FILE)
                Toast.makeText(this, "You haven't picked a File", Toast.LENGTH_LONG).show();

        }
    }

    public void getOlderMessages() {
        int oldestId = adapter.getMessageOrder(0);
        System.out.println(oldestId);
        Intent intent = new Intent(this, CommunicationService.class);
        intent.setAction("getMessages");
        intent.putExtra("groupID", groupID);
        intent.putExtra("oldestID", oldestId);

        startService(intent);
    }

    public void getMostRecentMessages() {
        Intent intent = new Intent(this, CommunicationService.class);
        intent.setAction("getMessages");
        intent.putExtra("groupID", groupID);
        intent.putExtra("oldestID", -1);

        //Log.e("ChatGroup", myApp.groups.get(groupID).getMessages().toString());

        startService(intent);
    }


    protected void sendFileMessage(Uri fileUri) {
        myApp.sendFileMessage(fileUri, groupID);
    }


    protected void sendImageMessage(Uri imageUri) {

        Log.e("CHAT", "Sending Image");
        // TODO delete/comment these 3 lines since Msg ID is retrieved from server later
        ChatMessage chatMessage = new ChatMessage(CustomApplication.userID, CustomApplication.userName, imageUri.toString(), ChatMessage.MessageType.Image);

//        appendMessageView(chatMessage);
//        myApp.groups.get(groupID).appendMessage(chatMessage);

        myApp.sendImageMessage(imageUri, groupID);
    }

    protected void sendTextMessage(String textMessage) {
        myApp.sendTextMessage(textMessage, groupID);
    }

    protected void sendLocationMessage(LatLng location) {
        myApp.sendLocationMessage(location, groupID);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("text_box_text", textBoxText);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        myApp.setCurrentActivity(this);
        //getMostRecentMessages();

        //updateMessageView();
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

    private void shareGroupActivity() {
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        // TODO uncomment this when shareLink is ready to use
        String shareBody = myApp.groups.get(groupID).getShareLink();
//        String shareBody = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        intent.setType("text/plain")
                .putExtra(android.content.Intent.EXTRA_SUBJECT, CustomApplication.userName.trim() + " message")
                .putExtra(android.content.Intent.EXTRA_TEXT, shareBody);

        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser)));
    }

    public void updateMessageView() {
        ArrayList<ChatMessage> l = new ArrayList<>(myApp.groups.get(groupID).getMessages());
        adapter.submitList(l);
    }

    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    private void capturePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permissions, 1000);
            } else {
                loadCamera();
            }
        }

    }

    public void loadCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "newImage");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, 2000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadCamera();
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }

    }


    public void openMap(double latitude, double longitude) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    public void openFile(ChatMessage message) {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        Uri data = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider" , message.getDownloadedFile());
        DocumentFile file = DocumentFile.fromSingleUri(getApplicationContext(), data);
        String fileName = file.getName();
        String type;
        // Check what kind of file you are trying to open, by comparing the url with extensions.
        // When the if condition is matched, plugin sets the correct intent (mime) type,
        // so Android knew what application to use to open the file
        if (fileName.contains(".doc") || fileName.contains(".docx")) {
            // Word document
            type = "application/msword";
        } else if (fileName.contains(".pdf")) {
            // PDF file
            type = "application/pdf";
        } else if (fileName.contains(".ppt") || fileName.contains(".pptx")) {
            // Powerpoint file
            type = "application/vnd.ms-powerpoint";
        } else if (fileName.contains(".xls") || fileName.contains(".xlsx")) {
            // Excel file
            type = "application/vnd.ms-excel";
        } else if (fileName.contains(".zip") || fileName.contains(".rar")) {
            // WAV audio file
            type = "application/zip";
        } else if (fileName.contains(".rtf")) {
            // RTF file
            type = "application/rtf";
        } else if (fileName.contains(".wav") || fileName.contains(".mp3")) {
            // WAV audio file
            type = "audio/x-wav";
        } else if (fileName.contains(".gif")) {
            // GIF file
            type = "image/gif";
        } else if (fileName.contains(".jpg") || fileName.contains(".jpeg") || fileName.contains(".png")) {
            // JPG file
            type = "image/jpeg";
        } else if (fileName.contains(".txt")) {
            // Text file
            type = "text/plain";
        } else if (fileName.contains(".html")) {
            // HTML file
            type = "text/html";
        } else if (fileName.contains(".3gp") || fileName.contains(".mpg") || fileName.contains(".mpeg") || fileName.contains(".mpe") || fileName.contains(".mp4") || fileName.contains(".avi")) {
            // Video files
            type = "video/*";
        } else {
            //if you want you can also define the intent type for any other file

            //additionally use else clause below, to manage other unknown extensions
            //in this case, Android will show all applications installed on the device
            //so you can choose which application to use
            type = "*/*";
        }

        intent.setDataAndType(data, type)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
}