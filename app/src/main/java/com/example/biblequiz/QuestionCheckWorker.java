package com.example.biblequiz;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import retrofit2.Call;
import retrofit2.Response;


import java.util.List;
import java.util.Random;

public class QuestionCheckWorker extends Worker {
    public QuestionCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("QuestionCheckWorker", "🔁 Worker triggered: Checking for new questions...");

        boolean initialFetch = getInputData().getBoolean("initial_fetch", false);

        if (initialFetch) {
            fetchAndSaveQuestionsToSQLite(); // ✅ run only if needed
            return Result.success();
        }


        boolean newQuestionsAvailable = checkForNewQuestions();
        if (newQuestionsAvailable) {
            Log.d("QuestionCheckWorker", "📣 New questions found! Sending notification...");
            sendNotification();

            Log.d("QuestionCheckWorker", "📥 Fetching and saving new questions to DB...");

            fetchAndSaveQuestionsToSQLite(); // ✅ save updated questions
        }
        return Result.success();
    }

    private boolean checkForNewQuestions() {
        try {
            // 1. Get local row count
            DBHelper dbHelper = new DBHelper(getApplicationContext());
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM questions", null);
            int localCount = 0;
            if (cursor.moveToFirst()) {
                localCount = cursor.getInt(0);
            }
            cursor.close();
            db.close();

            // 2. Get count from API
            QuestionInterface api = RetrofitClient.getQuestionAPI();
            Call<QuestionResponse> call = api.getAllQuestions(); // ✅ match interface
            Response<QuestionResponse> response = call.execute(); // synchronous call

            if (response.isSuccessful() && response.body() != null) {
                List<Questions> apiQuestions = response.body().getQuestions(); // ✅ Extract from wrapper

                int apiCount = apiQuestions.size();

                // 3. Get last saved count from SharedPreferences
                SharedPreferences prefs = getApplicationContext().getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE);
                int savedCount = prefs.getInt("saved_api_count", 0);

                Log.d("Worker", "Local DB: " + localCount + ", API: " + apiCount + ", Saved: " + savedCount);

                return (apiCount > savedCount || apiCount > localCount);
            } else {
                Log.e("Worker", "API call failed: " + response.message());
            }

        } catch (Exception e) {
            Log.e("Worker", "Error checking questions", e);
        }

        return false;
    }

    private void sendNotification() {
        Log.d("QuestionCheckWorker", "📲 sendNotification() called");


        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "questions_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "New Questions", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Default Android icon

                .setContentTitle("New Questions Available!")
                .setContentText("Update your question bank now.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(1, builder.build());

        Log.d("QuestionCheckWorker", "✅ Notification pushed successfully");
    }

    private void showToast(String message) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                android.widget.Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show()
        );
    }


    private void fetchAndSaveQuestionsToSQLite() {
        try {
            // 1. Create Retrofit API instance
            QuestionInterface api = RetrofitClient.getQuestionAPI();

            // 2. Make a synchronous API call
            Response<QuestionResponse> response = api.getAllQuestions().execute();

            // 3. Handle successful response
            if (response.isSuccessful() && response.body() != null) {
                List<Questions> questionList = response.body().getQuestions();

                if (questionList != null && !questionList.isEmpty()) {
                    Log.d("Worker", "✅ Fetched " + questionList.size() + " questions from API.");

                    // 4. Save to SQLite database
                    DBHelper dbHelper = new DBHelper(getApplicationContext());
                    dbHelper.saveQuestions(questionList);

                    // 5. Get current question count in DB
                    int currentCount = dbHelper.getQuestionCount();

                    // 6. Save to SharedPreferences
                    SharedPreferences prefs = getApplicationContext().getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("saved_api_count", currentCount);
                    editor.apply();

                    Log.d("Worker", "✅ Saved questions to DB. Current count: " + currentCount);

                    // 7. If DB has 190+, log success and safely show toast
                    if (currentCount >= 190) {
                        Log.d("Worker", "🎉 DB now has 190+ questions! Background population complete.");

                        // Run toast safely on the main thread
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(getApplicationContext(), "190 Questions saved successfully!", Toast.LENGTH_LONG).show()
                        );
                    }

                } else {
                    Log.e("Worker", "⚠️ API returned an empty question list.");
                }

            } else {
                Log.e("Worker", "❌ API call failed: " + (response != null ? response.message() : "Null response"));
            }

        } catch (Exception e) {
            Log.e("Worker", "❌ Exception in fetchAndSaveQuestionsToSQLite()", e);
        }
    }


}
