package com.monkeys123.conversationalist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.monkeys123.conversationalist.Data.Account;
import com.monkeys123.conversationalist.databinding.ActivityLoginBinding;

import java.util.ArrayList;
import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {
    CustomApplication myApp;
    private ActivityLoginBinding binding;

    EditText usernameEditText;
    EditText passwordEditText;
    Button loginButton;
    Button registerButton;

    Account.Type accountType;

    ArrayList<Boolean> errorBias = new ArrayList<>(Arrays.asList(false, false, false));

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

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        usernameEditText = binding.username;
        passwordEditText = binding.password;


        binding.loginPermanentChoice.setChecked(true);
        loginButton = binding.loginButton;
        registerButton = binding.registerButton;

        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() <= 4) {
                    errorBias.set(0, false);
                    usernameEditText.setError(getResources().getString(R.string.username_length_error));
                } else
                    errorBias.set(0, true);

                if (!s.toString().matches("^[A-Za-z0-9]*$")) {
                    errorBias.set(1, false);
                    usernameEditText.setError(getResources().getString(R.string.username_characters_error));
                } else
                    errorBias.set(1, true);

                loginButton.setEnabled(!errorBias.contains(false) || accountType == Account.Type.ephemeral && errorBias.get(0) && errorBias.get(1));
                registerButton.setEnabled(!errorBias.contains(false) || accountType == Account.Type.ephemeral && errorBias.get(0) && errorBias.get(1));
            }
        });

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
                    errorBias.set(2, false);
                    passwordEditText.setError(getResources().getString(R.string.password_length_error));
                } else
                    errorBias.set(2, true);

                loginButton.setEnabled(!errorBias.contains(false) || accountType == Account.Type.ephemeral && errorBias.get(0) && errorBias.get(1));
                registerButton.setEnabled(!errorBias.contains(false) || accountType == Account.Type.ephemeral && errorBias.get(0) && errorBias.get(1));
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                Log.e("LoginActivity", "Sign in button registered: " + username + ", " + password);

                Intent intent = new Intent(LoginActivity.this, CommunicationService.class);
                intent.setAction("login");
                intent.putExtra("username", username);
                intent.putExtra("password", password);
                startService(intent);
            }
        });
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                Log.e("LoginActivity", "Register button registered: " + username + ", " + password);

                Intent intent = new Intent(LoginActivity.this, CommunicationService.class);
                intent.setAction("register");
                intent.putExtra("username", username);

                if (accountType != Account.Type.ephemeral) {
                    intent.putExtra("password", password);
                }

                startService(intent);
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    public void onAccountTypeChoice(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.login_ephemeral_choice:
                if (checked) {
                    accountType = Account.Type.ephemeral;
                    passwordEditText.setVisibility(View.GONE);
                    loginButton.setVisibility(View.GONE);
                }
                break;
            case R.id.login_permanent_choice:
                if (checked) {
                    accountType = Account.Type.permanent;
                    passwordEditText.setVisibility(View.VISIBLE);
                    loginButton.setVisibility(View.VISIBLE);
                    loginButton.setEnabled(!errorBias.contains(false) || accountType == Account.Type.ephemeral && errorBias.get(0) && errorBias.get(1));
                    registerButton.setEnabled(!errorBias.contains(false) || accountType == Account.Type.ephemeral && errorBias.get(0) && errorBias.get(1));
                }
                break;
        }
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