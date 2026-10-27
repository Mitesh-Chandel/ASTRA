package com.example.astra;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.astra.ai.LocalAI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO = 1001;

    private EditText etChatMessage;
    private Button btnChatSend;
    private Button btnVoiceInput;
    private RecyclerView chatRecyclerView;
    private TextView tvVoiceStatus;
    private ChatAdapter chatAdapter;
    private final List<Message> messages = new ArrayList<>();
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor();
    private ChatDatabaseHelper databaseHelper;
    private LocalAI localAI;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private TextToSpeech textToSpeech;
    private boolean modelLoaded;
    private boolean generationInProgress;
    private boolean isListening;
    private boolean textToSpeechReady;
    private int typingPosition = -1;

    private static final int CONTEXT_MESSAGE_LIMIT = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        databaseHelper = new ChatDatabaseHelper(this);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnChatSend = findViewById(R.id.btnChatSend);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        tvVoiceStatus = findViewById(R.id.tvVoiceStatus);

        chatAdapter = new ChatAdapter(messages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        loadSavedMessages();
        if (messages.isEmpty()) {
            addMessage("Hello! I'm ASTRA. How can I help you today?", false);
            saveCurrentMessage();
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnChatMenu).setOnClickListener(this::showChatMenu);
        btnChatSend.setEnabled(false);
        btnVoiceInput.setEnabled(true);
        initializeModel();
        initializeTextToSpeech();

        btnChatSend.setOnClickListener(v -> sendMessage());
        btnVoiceInput.setOnClickListener(v -> toggleVoiceInput());
        etChatMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });

        initializeSpeechRecognizer();
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS || textToSpeech == null) {
                textToSpeechReady = false;
                return;
            }

            int languageStatus = textToSpeech.setLanguage(Locale.US);
            if (languageStatus == TextToSpeech.LANG_MISSING_DATA
                    || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeechReady = false;
                return;
            }

            textToSpeech.setSpeechRate(1.0f);
            textToSpeech.setPitch(1.0f);
            textToSpeechReady = true;
        });
    }

    private void initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            btnVoiceInput.setEnabled(false);
            showVoiceStatus("Voice input unavailable on this device.");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                showVoiceStatus("Listening...");
            }

            @Override
            public void onBeginningOfSpeech() {
                showVoiceStatus("Listening...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                showVoiceStatus("Processing...");
            }

            @Override
            public void onError(int error) {
                stopVoiceListening();
                String message = getSpeechErrorMessage(error);
                showVoiceStatus(message);
                Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0).trim();
                    if (!TextUtils.isEmpty(recognizedText)) {
                        etChatMessage.setText(recognizedText);
                        etChatMessage.setSelection(recognizedText.length());
                        sendMessage();
                    } else {
                        showVoiceStatus("No speech detected.");
                    }
                } else {
                    showVoiceStatus("No speech detected.");
                }
                stopVoiceListening();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
    }

    private void toggleVoiceInput() {
        if (isListening) {
            stopVoiceListening();
            return;
        }

        if (!hasRecordAudioPermission()) {
            requestRecordAudioPermission();
            return;
        }

        if (speechRecognizer == null) {
            initializeSpeechRecognizer();
        }

        if (speechRecognizer != null) {
            isListening = true;
            btnVoiceInput.setText("■");
            btnVoiceInput.setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
            showVoiceStatus("Listening...");
            speechRecognizer.startListening(speechRecognizerIntent);
        }
    }

    private boolean hasRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRecordAudioPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleVoiceInput();
            } else {
                showVoiceStatus("Microphone permission denied.");
                Toast.makeText(this, "Microphone permission is required for voice input.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopVoiceListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
        }
        isListening = false;
        btnVoiceInput.setText("🎙");
        btnVoiceInput.setBackgroundTintList(getColorStateList(android.R.color.darker_gray));
        tvVoiceStatus.setVisibility(View.GONE);
        tvVoiceStatus.setText("");
    }

    private void showVoiceStatus(String message) {
        if (TextUtils.isEmpty(message)) {
            tvVoiceStatus.setVisibility(View.GONE);
            tvVoiceStatus.setText("");
            return;
        }
        tvVoiceStatus.setText(message);
        tvVoiceStatus.setVisibility(View.VISIBLE);
    }

    private String getSpeechErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error.";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Voice input is unavailable.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Microphone permission is required.";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network is unavailable.";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Voice recognition timed out.";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech detected.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service is busy.";
            case SpeechRecognizer.ERROR_SERVER:
                return "Speech service is unavailable.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech detected.";
            default:
                return "Voice recognition failed.";
        }
    }

    private void initializeModel() {
        localAI = new LocalAI();
        String modelPath = LocalAI.resolveModelPath(this);
        aiExecutor.execute(() -> {
            boolean loaded = localAI.loadModel(modelPath);
            runOnUiThread(() -> {
                modelLoaded = loaded;
                btnChatSend.setEnabled(loaded);
                if (!loaded) {
                    addMessage("I couldn't load the local AI model.", false);
                    saveCurrentMessage();
                }
            });
        });
    }

    private void loadSavedMessages() {
        messages.clear();
        messages.addAll(databaseHelper.getMessages());
        chatAdapter.notifyDataSetChanged();
        scrollToBottom();
    }

    private void saveCurrentMessage() {
        if (messages.isEmpty()) {
            return;
        }
        Message last = messages.get(messages.size() - 1);
        if (last != null && !last.isTyping()) {
            databaseHelper.insertMessage(last);
        }
    }

    private void clearConversation() {
        if (typingPosition >= 0) {
            typingPosition = -1;
        }
        messages.clear();
        databaseHelper.clearMessages();
        chatAdapter.notifyDataSetChanged();
    }

    private void sendMessage() {
        String prompt = etChatMessage.getText().toString().trim();
        if (prompt.isEmpty() || !modelLoaded || generationInProgress) {
            return;
        }

        addMessage(prompt, true);
        saveCurrentMessage();
        etChatMessage.setText("");
        generationInProgress = true;
        typingPosition = messages.size();
        messages.add(Message.typing());
        chatAdapter.notifyItemInserted(typingPosition);
        scrollToBottom();
        String contextPrompt = buildContextPrompt();

        aiExecutor.execute(() -> {
            String response = localAI.generateResponse(contextPrompt);
            runOnUiThread(() -> {
                generationInProgress = false;
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                messages.remove(typingPosition);
                chatAdapter.notifyItemRemoved(typingPosition);
                typingPosition = -1;
                String finalResponse = response == null || response.trim().isEmpty()
                        ? "I wasn't able to generate a response." : response;
                addMessage(finalResponse, false);
                saveCurrentMessage();
                speakResponse(finalResponse);
            });
        });
    }

    private void speakResponse(String response) {
        if (!textToSpeechReady || textToSpeech == null || TextUtils.isEmpty(response)) {
            return;
        }

        int maxLength = TextToSpeech.getMaxSpeechInputLength();
        int start = 0;
        boolean firstChunk = true;
        String utterancePrefix = "astra-response-" + System.nanoTime();

        while (start < response.length()) {
            int end = Math.min(start + maxLength, response.length());
            if (end < response.length()) {
                int breakPoint = response.lastIndexOf(' ', end);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }

            String chunk = response.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                int queueMode = firstChunk ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
                int result = textToSpeech.speak(chunk, queueMode, null,
                        utterancePrefix + "-" + start);
                if (result == TextToSpeech.ERROR) {
                    return;
                }
                firstChunk = false;
            }

            if (end <= start) {
                return;
            }
            start = end;
        }
    }

    private String buildContextPrompt() {
        StringBuilder prompt = new StringBuilder("Conversation:\n");
        int start = Math.max(0, messages.size() - 1 - CONTEXT_MESSAGE_LIMIT);
        for (int i = start; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (message.isTyping()) {
                continue;
            }
            prompt.append(message.isUser() ? "User: " : "Assistant: ")
                    .append(message.getText()).append('\n');
        }
        prompt.append("Assistant:");
        return prompt.toString();
    }

    private void addMessage(String text, boolean isUser) {
        Message message = new Message(text, isUser, System.currentTimeMillis());
        messages.add(message);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        chatRecyclerView.post(() -> {
            if (messages.isEmpty()) {
                return;
            }
            chatRecyclerView.scrollToPosition(messages.size() - 1);
        });
    }

    private void showChatMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("New Conversation");
        menu.getMenu().add("Clear Conversation");
        menu.setOnMenuItemClickListener(this::handleMenuItem);
        menu.show();
    }

    private boolean handleMenuItem(MenuItem item) {
        if ("New Conversation".contentEquals(item.getTitle())) {
            clearConversation();
            addMessage("Hello! I'm ASTRA. How can I help you today?", false);
            saveCurrentMessage();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("Clear this conversation?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear", (dialog, which) -> clearConversation())
                    .show();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        stopVoiceListening();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        aiExecutor.shutdownNow();
        if (localAI != null) {
            localAI.close();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
            textToSpeechReady = false;
        }
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}