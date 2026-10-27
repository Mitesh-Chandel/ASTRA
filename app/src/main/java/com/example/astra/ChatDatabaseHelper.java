package com.example.astra;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "astra_chat.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_IS_USER = "is_user";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_MESSAGES + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TEXT + " TEXT NOT NULL, "
                + COLUMN_IS_USER + " INTEGER NOT NULL, "
                + COLUMN_TIMESTAMP + " INTEGER NOT NULL"
                + ");";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    public void insertMessage(Message message) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT, message.getText());
        values.put(COLUMN_IS_USER, message.isUser() ? 1 : 0);
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        db.insert(TABLE_MESSAGES, null, values);
    }

    public List<Message> getMessages() {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_MESSAGES,
                null,
                null,
                null,
                null,
                null,
                COLUMN_TIMESTAMP + " ASC"
        );

        if (cursor != null) {
            try {
                int textIndex = cursor.getColumnIndexOrThrow(COLUMN_TEXT);
                int userIndex = cursor.getColumnIndexOrThrow(COLUMN_IS_USER);
                int timestampIndex = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP);

                while (cursor.moveToNext()) {
                    String text = cursor.getString(textIndex);
                    boolean isUser = cursor.getInt(userIndex) == 1;
                    long timestamp = cursor.getLong(timestampIndex);
                    messages.add(new Message(text, isUser, timestamp));
                }
            } finally {
                cursor.close();
            }
        }

        return messages;
    }

    public void clearMessages() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MESSAGES, null, null);
    }
}
