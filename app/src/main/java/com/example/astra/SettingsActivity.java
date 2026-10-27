package com.example.astra;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "astra_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_VOICE_INPUT = "voice_input";

    private Button btnSettingsBack;
    private Switch switchDarkMode;
    private Switch switchVoice;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        btnSettingsBack = findViewById(R.id.btnSettingsBack);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchVoice = findViewById(R.id.switchVoice);

        switchDarkMode.setChecked(preferences.getBoolean(KEY_DARK_MODE, false));
        switchVoice.setChecked(preferences.getBoolean(KEY_VOICE_INPUT, true));

        btnSettingsBack.setOnClickListener(v -> finish());

        switchVoice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_VOICE_INPUT, isChecked).apply();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            if (isChecked) {
                getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }
}