package com.example.astra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.astra.ai.LocalAI;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "astra_ai";



    TextView tvAssistantMessage;
    EditText etMessage;
    Button btnSend;
    Button btnSettings;
    Button btnVoice;

    private LocalAI localAI;
    private boolean modelLoaded = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        tvAssistantMessage =
                findViewById(R.id.tvAssistantMessage);

        etMessage =
                findViewById(R.id.etMessage);

        btnSend =
                findViewById(R.id.btnSend);

        btnVoice =
                findViewById(R.id.btnVoice);

        btnSettings =
                findViewById(R.id.btnSettings);


        // ----------------------------------------------------
        // CREATE NATIVE AI ENGINE
        // ----------------------------------------------------

        localAI = new LocalAI();


        // ----------------------------------------------------
        // LOAD MODEL
        // ----------------------------------------------------

        tvAssistantMessage.setText(
                "Preparing ASTRA AI model..."
        );


        String modelPath =
                com.example.astra.ai.LocalAI.resolveModelPath(this);

        Log.i(
                TAG,
                "Internal model path: " + modelPath
        );

        new Thread(() -> {

            boolean loaded =
                    localAI.loadModel(modelPath);

            runOnUiThread(() -> {

                if (loaded) {

                    modelLoaded = true;

                    tvAssistantMessage.setText(
                            "ASTRA AI is ready! 🤖"
                    );

                } else {

                    tvAssistantMessage.setText(
                            "Failed to load AI model."
                    );
                }

            });

        }).start();


        // ----------------------------------------------------
        // SEND BUTTON
        // ----------------------------------------------------

        btnSend.setOnClickListener(v -> {

            String message =
                    etMessage
                            .getText()
                            .toString()
                            .trim();


            if (message.isEmpty()) {
                return;
            }


            if (!modelLoaded) {

                tvAssistantMessage.setText(
                        "AI model is still loading..."
                );

                return;
            }


            tvAssistantMessage.setText(
                    "ASTRA is thinking..."
            );


            new Thread(() -> {

                String response =
                        localAI.generateResponse(
                                message
                        );


                runOnUiThread(() -> {

                    tvAssistantMessage.setText(
                            response
                    );

                    etMessage.setText("");

                });

            }).start();

        });


        // ----------------------------------------------------
        // VOICE BUTTON
        // ----------------------------------------------------

        btnVoice.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            ChatActivity.class
                    );

            startActivity(intent);

        });


        // ----------------------------------------------------
        // SETTINGS BUTTON
        // ----------------------------------------------------

        btnSettings.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            SettingsActivity.class
                    );

            startActivity(intent);

        });

    }




    // ========================================================
    // CLEANUP
    // ========================================================

    @Override
    protected void onDestroy() {

        if (localAI != null) {

            localAI.close();
        }

        super.onDestroy();
    }
}