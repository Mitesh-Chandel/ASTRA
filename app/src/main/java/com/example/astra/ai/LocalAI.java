package com.example.astra.ai;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LocalAI {
    private static final String TAG = "astra_ai";
    private static final String MODEL_FILE_NAME = "qwen2.5-0.5b-instruct-q4_k_m.gguf";

    static {
        System.loadLibrary("astra_ai");
    }

    private long nativeHandle = 0;

    private native void nativeInit();
    private native boolean nativeLoadModel(String path);
    private native String nativeGenerate(String prompt);
    private native void nativeCleanup();

    public LocalAI() {
        nativeInit();
    }

    public static String resolveModelPath(Context context) {
        if (context == null) {
            return null;
        }

        File modelDir = new File(context.getFilesDir(), "models");
        if (!modelDir.exists()) {
            boolean created = modelDir.mkdirs();
            Log.i(TAG, "Model directory created: " + created + " at " + modelDir.getAbsolutePath());
        }

        File modelFile = new File(modelDir, MODEL_FILE_NAME);
        if (!modelFile.exists() || modelFile.length() <= 0L) {
            copyModelFromAssets(context, modelFile);
        }

        Log.i(TAG, "Resolved model path: " + modelFile.getAbsolutePath() + ", exists=" + modelFile.exists() + ", size=" + modelFile.length());
        return modelFile.getAbsolutePath();
    }

    private static void copyModelFromAssets(Context context, File targetFile) {
        if (context == null || targetFile == null) {
            Log.e(TAG, "Cannot copy model: invalid context or target");
            return;
        }

        try {
            InputStream inputStream = context.getAssets().open("models/" + MODEL_FILE_NAME);
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            OutputStream outputStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Log.i(TAG, "Copied model from assets to: " + targetFile.getAbsolutePath()
                    + ", size=" + targetFile.length());
        } catch (IOException e) {
            Log.e(TAG, "Model copy from assets failed: " + e.getMessage(), e);
        }
    }

    public boolean loadModel(String path) {
        if (path == null || path.trim().isEmpty()) {
            Log.e(TAG, "Model load requested with empty path");
            return false;
        }
        File file = new File(path);
        Log.i(TAG, "Loading model from path: " + path + ", exists=" + file.exists() + ", size=" + file.length());
        return nativeLoadModel(path);
    }

    public String generateResponse(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Prompt is empty.";
        }
        return nativeGenerate(prompt);
    }

    public void close() {
        if (nativeHandle == 0L) {
            return;
        }

        nativeCleanup();
        nativeHandle = 0L;
    }
}
