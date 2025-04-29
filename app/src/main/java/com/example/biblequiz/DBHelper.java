package com.example.biblequiz;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.google.gson.reflect.TypeToken;



import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "bible_quiz.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "questions";

    // Columns

    private static final String COLUMN_ID = "id";
  //  private static final String COLUMN_NO = "no";
    private static final String COLUMN_QUESTION = "question";
    private static final String COLUMN_OPTIONS = "options";
    private static final String COLUMN_ANSWER = "answer";
    private static final String COLUMN_REFERENCE = "reference";

    private Gson gson = new Gson();

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +

                COLUMN_QUESTION + " TEXT, " +
                COLUMN_OPTIONS + " TEXT, " +
                COLUMN_ANSWER + " TEXT, " +
                COLUMN_REFERENCE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // ✅ Check if the table exists
    private boolean isTableExists(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{TABLE_NAME}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // ✅ Check if the table is populated
    private boolean isTablePopulated(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        boolean populated = false;
        if (cursor.moveToFirst()) {
            populated = cursor.getInt(0) > 0;
        }
        cursor.close();
        return populated;
    }





    // ✅ Save Questions from API to SQLite
    public void saveQuestions(List<Questions> questions) {
        SQLiteDatabase db = this.getWritableDatabase();
      //  Log.d("DBHelper", "Saving questions to the database...");
        Log.d("DBHelper", "Saving " + questions.size() + " questions to the database...");




        // db.delete(TABLE_NAME, null, null); // Clear old data
        db.beginTransaction();
        int successCount = 0;

        try {
            for (Questions q : questions) {
                ContentValues values = new ContentValues();
                // values.put(COLUMN_NO, q.getNo());
                // Watch out for duplicate IDs
             //   Log.d("DBHelper", "Inserting question with ID: " + q.getId());


             //   values.put(COLUMN_ID, q.getId());
                values.put(COLUMN_QUESTION, q.getQuestion());
                values.put(COLUMN_OPTIONS, gson.toJson(q.getOptions())); // Convert Map to JSON
                values.put(COLUMN_ANSWER, q.getCorrectAnswer());
                values.put(COLUMN_REFERENCE, q.getReference());

                long result = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                if (result != -1) {
                    successCount++;
                } else {
                    Log.e("DBHelper", "❌ Failed to insert question: " + q.getQuestion());
                }
            }

            db.setTransactionSuccessful();
            Log.d("DBHelper", "✅ Transaction successful. Inserted " + successCount + " questions.");
        } catch (Exception e) {
            Log.e("DBHelper", "❌ Error during insert transaction", e);
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    // ✅ Fetch Questions from SQLite
    public List<Questions> getAllQuestions() {
        List<Questions> questionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);



        if (!isTableExists(db)) {
            Log.e("DBHelper", "Table does not exist!");
            return questionList;
        }

        if (!isTablePopulated(db)) {
            Log.e("DBHelper", "Table is empty!");
            return questionList;
        }




        Gson gson = new Gson();

      //  Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);



        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
               // int no = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NO));
                String question = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUESTION));
                String optionsJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPTIONS));

                // Convert JSON string (options) to Map<String, String>
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> optionsMap = gson.fromJson(optionsJson, type);

                String correctAnswer = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANSWER));
                String reference = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REFERENCE));

               // Questions q = new Questions(id, question, optionsJson, correctAnswer, reference);
                //questionList.add(new Questions(id, /*no*/, question, optionsMap, correctAnswer, reference));

                questionList.add(new Questions(id, question, optionsMap, correctAnswer, reference));

            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return questionList;
    }

    public int getQuestionCount() {
        int count = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM questions", null);

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();

        return count;
    }





  /*  public void logTableSchema(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(questions);", null);
        Log.d("DB_DEBUG", "Table Schema:");
        while (cursor.moveToNext()) {
            Log.d("DB_DEBUG", "Column: " + cursor.getString(1)); // Column name
        }
        cursor.close();
    }*/





}
