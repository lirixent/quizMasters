/*package com.example.biblequiz;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QuestionUpdateWorker extends Worker {

    private final QuestionInterface apiService;
    private final DBHelper dbHelper;

    public QuestionUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.apiService = RetrofitClient.getQuestionAPI();  // ✅ Use your existing RetrofitClient
        this.dbHelper = new DBHelper(context);              // ✅ Your local DB helper
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SharedPreferences preferences = getApplicationContext().getSharedPreferences("prefs", Context.MODE_PRIVATE);
            int lastSavedApiCount = preferences.getInt("lastApiCount", 0);

            int dbCount = getLocalDbQuestionCount();
            Log.d("Worker", "Local DB has " + dbCount + " questions");

            // Fill DB to 201 if not yet full
            if (dbCount < 201) {
                Log.d("Worker", "Filling DB to reach 201 questions...");
                fetchAndSaveQuestions(201 - dbCount);
                return Result.success();
            }

            // Now compare API count with stored count
            int apiCount = getApiQuestionCount();
            Log.d("Worker", "API reports " + apiCount + " questions");

            if (apiCount > lastSavedApiCount) {
                sendNotificationToUser(); // Notify user for update
            }

            return Result.success();
        } catch (Exception e) {
            Log.e("Worker", "Error: " + e.getMessage());
            return Result.failure();
        }
    }

    private int getLocalDbQuestionCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM questions", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    private int getApiQuestionCount() throws IOException {
        Response<List<Questions>> response = apiService.getQuestions().execute();
        if (response.isSuccessful() && response.body() != null) {
            return response.body().size();
        } else {
            return 0;
        }
    }

    private void fetchAndSaveQuestions(int howManyToFetch) {
        try {
            Response<List<Questions>> response = apiService.getQuestions().execute();
            if (response.isSuccessful() && response.body() != null) {
                List<Questions> fetched = response.body();

                // Trim to required size
                List<Questions> trimmed = fetched.subList(0, Math.min(howManyToFetch, fetched.size()));
                dbHelper.insertQuestions(trimmed);  // You must define this method in your DBHelper

                Log.d("Worker", "Saved " + trimmed.size() + " questions into DB.");
            }
        } catch (IOException e) {
            Log.e("Worker", "Error fetching questions: " + e.getMessage());
        }
    }

    private void sendNotificationToUser() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("question_update_channel", "Question Updates", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class); // You can change this to a specific update screen
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), "question_update_channel")
                .setContentTitle("New Questions Available")
                .setContentText("Click here to update your questions.")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(101, notification);
    }
}
*/