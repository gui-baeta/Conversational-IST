package com.monkeys123.conversationalist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.monkeys123.conversationalist.Data.Account;
import com.monkeys123.conversationalist.databinding.ActivityUpgradeAccountBinding;

import java.util.ArrayList;
import java.util.Arrays;

public class UpgradeAccountActivity extends AppCompatActivity {
    CustomApplication myApp;
    private ActivityUpgradeAccountBinding binding;

    EditText usernameEditText;
    EditText passwordEditText;
    Button upgradeButton;
    Button registerButton;

    Account.Type accountType;

    boolean validPassword = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        myApp = (CustomApplication) this.getApplicationContext();
        myApp.setCurrentActivity(this);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.login_actionbar);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.statusbar_custom));

        binding = ActivityUpgradeAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        passwordEditText = binding.password;


        upgradeButton = binding.upgradeButton;

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() <= 7) {
                    validPassword = false;
                    passwordEditText.setError(getResources().getString(R.string.password_length_error));
                } else
                    validPassword = true;

                upgradeButton.setEnabled(validPassword);
            }
        });

        upgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = passwordEditText.getText().toString();
                Log.e("LoginActivity", "Upgrade account registered: ");

                Intent intent = new Intent(UpgradeAccountActivity.this, CommunicationService.class);
                intent.setAction("upgradeAccount");
                intent.putExtra("password", password);
                startService(intent);
            }
        });
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
}
