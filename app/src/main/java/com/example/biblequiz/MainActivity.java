package com.example.biblequiz;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import androidx.work.*;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;

//de.hdodenhof.circleimageview.CircleImageView;

import de.hdodenhof.circleimageview.CircleImageView;
import java.util.Map;

import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private ProgressBarFragment progressBarFragment;

    private static final String TAG = "QuizApp"; // Define log tag

    private Spinner categorySpinner;
    private List<String> categories;

    private List<Questions> allQuestions = new ArrayList<>();
    private List<Questions> shuffledQuestions = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private TextView usernameTextView;
    private CircleImageView profileIcon;
    private TextView questionTypeText, timeLeftText, referenceText, questionText, correctAnswerText, textView;
    private RadioButton option1, option2, option3, option4;
    private RadioGroup answerGroup;
    private Button nextQuestionButton;

    private Questions currentQuestion;


    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 30000; // 30 seconds

    private QuestionInterface apiService;

    private String correctAnswer;

    private int id, no;

    private String reference;

  //  private Map<String, String> options;

    private List<Questions> questionList = new ArrayList<>();
    private int currentIndex = 0;
    private int questionNumber = 1;

    private Context context;
    private List<Questions> selectedQuestions; // Store selected questions
   // private int currentIndex = 0;      // Track current question index

    private DBHelper dbHelper;

 /*   public MainActivity(){


    }
*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);






        // Initialize Retrofit
        apiService = RetrofitClient.getInstance().create(QuestionInterface.class);

        // Fetch all questions from the API
//        fetchAllQuestions();


        // Fetch 5 random questions and store them




        // Initialize UI elements
        questionText = findViewById(R.id.questionText);
        questionTypeText = findViewById(R.id.questionTypeText);
        timeLeftText = findViewById(R.id.timeLeftText);
        referenceText = findViewById(R.id.referenceText);
        correctAnswerText = findViewById(R.id.correctAnswerText);

        progressBarFragment = new ProgressBarFragment();



      //  startCountdown();

        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        nextQuestionButton = findViewById(R.id.nextQuestionButton);
        answerGroup = findViewById(R.id.answerGroup);
       // textView = findViewById(R.id.textView);




        referenceText.setText("Reference: [Book Name]");
        correctAnswerText.setVisibility(View.GONE);

        // Listener for answer selection
        answerGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedOption = findViewById(checkedId);
            if (selectedOption != null) {
             //   startCountdown();
                onAnswerSelected(selectedOption.getText().toString());
            }
        });

        updateQuizDetails("MCQ", "00:30", "[Book Name]");

        categorySpinner = findViewById(R.id.categorySpinner);
        categories = new ArrayList<>();
        categories.add("Bible Quiz");
        categories.add("Truth or Dare");
       // fetchCategoriesFromMongoDB();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(adapter);

        sharedPreferences = getSharedPreferences("QuizApp", Context.MODE_PRIVATE);
        usernameTextView = findViewById(R.id.usernameTextView);
        profileIcon = findViewById(R.id.profileIcon);
        checkAndPromptUsername();
        //fetchQuestions();

        nextQuestionButton.setOnClickListener(v -> showNextQuestion());
        answerGroup.setOnCheckedChangeListener((group, checkedId) -> checkAnswer(checkedId));


        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

// Check how many questions exist in the database
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM questions", null);
        int questionCount = 0;
        if (cursor.moveToFirst()) {
            questionCount = cursor.getInt(0);
        }
        cursor.close();
        db.close(); // Always close the database after use

        if (questionCount == 0) {
            Log.d("MainActivity", "Database is empty. Fetching questions from API...");
            fetchAndSaveQuestions();
        } else {
            Log.d("MainActivity", "Database is populated. Using questions from database.");
            sqlite2List();
        }

        selectedQuestions = new ArrayList<>();
        //currentQuestionIndex = 0;
        selectedQuestions = getRandomQuestions(5);

// Display the first question
        Log.d("QuizApp", "Total selected questions: " + selectedQuestions.size());
        for (Questions q : selectedQuestions) {
            Log.d("QuizApp", "Question: " + q.getQuestion());
        }



    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetQuiz(); // Clear stored question numbers when the app closes
    }


    private void checkAndPromptUsername() {
        String savedUsername = sharedPreferences.getString("username", null);
        if (savedUsername == null) {
            showUsernameDialog();
        } else {
            displayUserInfo(savedUsername);
            showGameModeDialog();
        }
    }

    private void fetchAllQuestions() {
        Log.d("QuizApp", "fetchAllQuestions() called");

        // Show the progress fragment
        showProgressFragment();



        QuestionInterface apiService = RetrofitClient.getInstance().create(QuestionInterface.class);
        Call<QuestionResponse> call = apiService.getAllQuestions();

        call.enqueue(new Callback<QuestionResponse>() {
            @Override
            public void onResponse(Call<QuestionResponse> call, Response<QuestionResponse> response) {
                // Hide the progress fragment
                hideProgressFragment();

                if (response.isSuccessful() && response.body() != null) {
                    List<Questions> questions = response.body().getQuestions();

                    Log.d("QuizApp", "Total Questions Fetched: " + questions.size());

                    allQuestions.clear(); // Clear previous questions before storing new ones
                    allQuestions.addAll(questions);
                    if (allQuestions.size() >= 5) {
                        selectedQuestions = getRandomQuestions(5);
                        currentIndex = 0;

                        Log.d("QuizApp", "Selected 5 questions for quiz:");
                        for (Questions q : selectedQuestions) {
                            Log.d("QuizApp", "Selected Question: " + q.getQuestion());
                        }

                        // Display the first question
                        showNextQuestion();
                    } else {
                        Log.e("QuizApp", "Not enough questions received!");
                        Toast.makeText(getApplicationContext(), "Not enough questions available!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("QuizApp", "Response unsuccessful or body is null");
                    Toast.makeText(getApplicationContext(), "Failed to load questions.", Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onFailure(Call<QuestionResponse> call, Throwable t) {
                // Hide the progress fragment on failure
             //   hideProgressFragment();

                Log.e("QuizApp", "API Call Failed: " + t.getMessage());
                //textView.setText("Error loading questions.");

                Toast.makeText(getApplicationContext(), "Error loading questions.", Toast.LENGTH_SHORT).show();



                // Hide the progress fragment on failure
                hideProgressFragment();
            }
        });
    }



    // Function to get random questions
    public List<Questions> getRandomQuestions(int count) {
        if (allQuestions.isEmpty()) {
            Log.e("QUESTION_ERROR", "No questions available yet!");
            return new ArrayList<>();
        }

        List<Questions> randomQuestions = new ArrayList<>(allQuestions);

        // Log all questions before shuffling
        for (int i = 0; i < randomQuestions.size(); i++) {
            Questions q = randomQuestions.get(i);
            Log.d("QUESTION_DEBUG", "Question " + (i + 1) + ": " + q.getQuestion());

            if (q.getOptions() == null) {
                Log.e("QUESTION_DEBUG", "Options are NULL for question: " + q.getQuestion());
            } else {
                Log.d("QUESTION_DEBUG", "Options: " + q.getOptions().toString() + "CorrectAnswer" + q.getCorrectAnswer());
            }
        }


        Collections.shuffle(randomQuestions); // Shuffle to randomize
        //return randomQuestions.subList(0, Math.min(count, randomQuestions.size())); // Get 'count' questions

        List<Questions> selectedQuestions = randomQuestions.subList(0, Math.min(count, randomQuestions.size()));

        // Log selected questions after shuffle
        for (int i = 0; i < selectedQuestions.size(); i++) {
            Questions q = selectedQuestions.get(i);
            Log.d("SELECTED_QUESTIONS", "Selected Question " + (i + 1) + ": " + q.getQuestion());

            if (q.getOptions() == null) {
                Log.e("SELECTED_QUESTIONS", "Options are NULL for selected question: " + q.getQuestion());
            } else {
                Log.d("SELECTED_QUESTIONS", "Options: " + q.getOptions().toString());
            }
        }

        return selectedQuestions;

    }



    // Show the progress bar fragment




    private void showProgressFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Ensure the container is visible before adding the fragment
        View fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer != null && fragmentContainer.getVisibility() != View.VISIBLE) {
            fragmentContainer.setVisibility(View.VISIBLE);
        }

        // Check if the ProgressBarFragment already exists
        ProgressBarFragment progressBarFragment = (ProgressBarFragment) fragmentManager.findFragmentByTag("ProgressBarFragment");

        if (progressBarFragment != null) {
            if (progressBarFragment.isVisible()) {
                Log.d("ProgressBarDebug", "ProgressBarFragment is already visible, skipping.");
                return;
            } else {
                Log.d("ProgressBarDebug", "Re-showing existing ProgressBarFragment");
                fragmentManager.beginTransaction().show(progressBarFragment).commit();
                return;
            }
        }

        // Creating a new instance if not found
        Log.d("ProgressBarDebug", "Creating new ProgressBarFragment instance");
        progressBarFragment = new ProgressBarFragment();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, progressBarFragment, "ProgressBarFragment");
        transaction.addToBackStack(null);
        transaction.commit();

        Log.d("ProgressBarDebug", "ProgressBarFragment added successfully");
    }

    private void hideProgressFragment() {
        Log.d("ProgressBarDebug", "Attempting to remove ProgressBarFragment");

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);

        if (fragment != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(fragment);
            transaction.commitNow();
            Log.d("ProgressBarDebug", "ProgressBarFragment removed successfully");
        } else {
            Log.d("ProgressBarDebug", "No fragment found to remove");
        }

        // Ensure the FrameLayout is completely hidden
        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.GONE);
            fragmentContainer.getLayoutParams().height = 0; // Ensures it does not block views
            fragmentContainer.requestLayout();
            Log.d("ProgressBarDebug", "FrameLayout hidden successfully");
        }
    }

    private void showUsernameDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_username, null);
        EditText editText = dialogView.findViewById(R.id.usernameEditText);

        new AlertDialog.Builder(this)
                .setTitle("Enter Your Username")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Save", (dialogInterface, i) -> {
                    String username = editText.getText().toString().trim();
                    if (!username.isEmpty()) {
                        sharedPreferences.edit().putString("username", username).apply();
                        displayUserInfo(username);
                        showGameModeDialog();
                    }
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> finish())
                .show();
    }

    private void displayUserInfo(String username) {
        usernameTextView.setText(username);
        createProfileIcon(getInitials(username));
    }

    private String getInitials(String name) {
        String[] words = name.split(" ");
        if (words.length == 1) return words[0].substring(0, 1).toUpperCase();
        return words[0].substring(0, 1).toUpperCase() + words[1].substring(0, 1).toUpperCase();
    }

    private void createProfileIcon(String initials) {
        int size = 150;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(this, R.color.teal_200));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2, size / 2, size / 2, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float x = size / 2;
        float y = (size / 2) - (fontMetrics.ascent + fontMetrics.descent) / 2;
        canvas.drawText(initials, x, y, paint);
        profileIcon.setImageBitmap(bitmap);
    }

    private void showGameModeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Choose Game Mode")
                .setSingleChoiceItems(new String[]{"Single Player", "Multiplayer"}, -1, (dialog, which) -> {
                    if (which == 0) startSinglePlayerMode();
                    else startMultiplayerMode();
                })
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void fetchCategoriesFromMongoDB() {
        // TODO: Fetch categories dynamically from MongoDB and update UI
    }

    private void updateQuizDetails(String questionType, String timeLeft, String reference) {
        questionTypeText.setText("Question Type: " + questionType);
        timeLeftText.setText("Time Left: " + timeLeft);
        referenceText.setText("Reference: " + reference);
        correctAnswerText.setVisibility(View.GONE);
    }

    private void onAnswerSelected(String selectedAnswer) {
       // correctAnswerText.setText("Correct Answer: " + correctAnswer);

        if (currentQuestion != null) {








            referenceText.setText("Reference: " + currentQuestion.getReference());
            correctAnswerText.setText("Correct Answer: " + correctAnswer);

        } else {
            referenceText.setText("Reference: N/A"); // Default text if no question is loaded
            correctAnswerText.setText("Correct Answer: N/A");


        }
        correctAnswerText.setVisibility(View.VISIBLE);
// Debugging Log
        Log.d("QuizApp", "Selected Answer: " + selectedAnswer);
        Log.d("QuizApp", "Correct Answer: " + correctAnswer);

        // Compare the selected answer to the correct one
        if (selectedAnswer.equals(correctAnswer)) {
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();
        } else {
            // Show the correct answer if the selected answer is wrong
            Toast.makeText(this, "Wrong! The correct answer is: " + correctAnswer, Toast.LENGTH_SHORT).show();
        }
       }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }


            @Override
            public void onFinish() {
                timeLeftText.setText("Time Up!");
            }
        }.start();
    }

    private void updateTimerText() {
        timeLeftText.setText("Time Left: " + (int) (timeLeftInMillis / 1000) + "s");
    }

    private void startSinglePlayerMode() {
        Toast.makeText(this, "Starting Single Player Mode...", Toast.LENGTH_SHORT).show();
        // TODO: Implement the logic for single-player mode
    }

    private void startMultiplayerMode() {
        Toast.makeText(this, "Starting Multiplayer Mode...", Toast.LENGTH_SHORT).show();
        // TODO: Implement the logic for multiplayer mode
    }

    private void scheduleQuestionCheck() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(QuestionCheckWorker.class, 6, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.MINUTES) // First check after 1 min
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("QuestionCheck", ExistingPeriodicWorkPolicy.KEEP, request);
    }


    private void fetchQuestions() {
        apiService = RetrofitClient.getInstance().create(QuestionInterface.class);
        Call<QuestionResponse> call = apiService.getAllQuestions();

        call.enqueue(new Callback<QuestionResponse>() {
            @Override
            public void onResponse(Call<QuestionResponse> call, Response<QuestionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Questions> questionsList = response.body().getQuestions();

                    if (questionsList == null || questionsList.isEmpty()) {
                        Log.e("API Response", "Question list is empty or null!");
                        return;
                    }

                    // Get the first question (modify if you need multiple)
                    Questions firstQuestion = questionsList.get(0);

                    Log.d("API Response", "Fetched Question: " + firstQuestion.getQuestion());
                    Log.d("API Response", "Reference: " + firstQuestion.getReference());

                    // Update UI
                    runOnUiThread(() -> {
                        questionText.setText(safeText(firstQuestion.getQuestion(), "Loading Questions..."));
                        option1.setText(safeText(firstQuestion.getOptions().get("a"), "Option1"));
                        option2.setText(safeText(firstQuestion.getOptions().get("b"), "Option2"));
                        option3.setText(safeText(firstQuestion.getOptions().get("c"), "Option3"));
                        option4.setText(safeText(firstQuestion.getOptions().get("d"), "Option4"));

                        referenceText.setText(safeText(firstQuestion.getReference(), "text reference"));
                    });

                } else {
                    Log.e("API Error", "Response not successful: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<QuestionResponse> call, Throwable t) {
                Log.e("API Failure", "Failed to fetch questions: " + t.getMessage());
            }
        });
    }
    // Utility function to prevent null values



    private String safeText(String text, String defaultText) {
        return (text != null && !text.isEmpty()) ? text : defaultText;
    }

    private void sqlite2List() {
        allQuestions.clear(); // Clear previous questions before loading new ones

        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM questions", null);
        if (cursor.moveToFirst()) {
            do {
                try {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String question = cursor.getString(cursor.getColumnIndexOrThrow("question"));
                    String optionsJson = cursor.getString(cursor.getColumnIndexOrThrow("options"));
                    String answer = cursor.getString(cursor.getColumnIndexOrThrow("answer"));
                    String reference = cursor.getString(cursor.getColumnIndexOrThrow("reference"));

                    // Convert JSON string to Map<String, String>
                    Map<String, String> options = new Gson().fromJson(optionsJson, new TypeToken<Map<String, String>>(){}.getType());

                    allQuestions.add(new Questions(id, question, options, answer, reference));

                } catch (IllegalArgumentException e) {
                    Log.e("DB_DEBUG", "❌ Column not found: " + e.getMessage());
                    break; // Exit loop if there's a problem with the database schema
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        if (allQuestions.size() >= 5) {
            selectedQuestions = getRandomQuestions(5);
            currentIndex = 0;
            showNextQuestion();
        } else {
            Log.e("QuizApp", "❌ Not enough questions in SQLite!");
            Toast.makeText(getApplicationContext(), "Not enough questions available!", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Override `fetchAndSaveQuestions` to Auto-Reload SQLite
    public void fetchAndSaveQuestions() {

        Log.d("QuizApp", "fetchAndSaveQuestions() called");





        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();



        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();

// Check for NULL values in any column
        try {
            // Check how many questions exist
            Cursor cursor = writableDb.rawQuery("SELECT COUNT(*) FROM questions", null);
            int questionCount = 0;
            if (cursor.moveToFirst()) {
                questionCount = cursor.getInt(0);
            }
            cursor.close();

            Log.d("QuizApp", "Questions in SQLite: " + questionCount);

            if (questionCount > 0) {
                //Validate and fix missing/ null values
                Log.d("QuizApp", "Checking and fixing null values in SQLite before loading.");

                fixNullValuesDatabase(writableDb);
                // Load from SQLite
                sqlite2List();
            }
            else {
                // Fetch from API if no data is found
                Log.d("QuizApp", "No questions in SQLite, fetching from API...");
                fetchQuestionsFromAPI(dbHelper, writableDb);
            }

        } catch (Exception e) {
            Log.e("DB_ERROR", "Error in fetchAndSaveQuestions(): " + e.getMessage());
        } finally {
            if (writableDb != null && writableDb.isOpen()) {
                writableDb.close();
            }
        }
    }

    private void fetchQuestionsFromAPI(DBHelper dbHelper, SQLiteDatabase writableDb) {
        RetrofitClient.getQuestionAPI().getAllQuestions().enqueue(new Callback<QuestionResponse>() {
            @Override
            public void onResponse(Call<QuestionResponse> call, Response<QuestionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Questions> questionsList = response.body().getQuestions();
                    dbHelper.saveQuestions(questionsList);
                    logAllQuestions(writableDb);  // Log after saving
                    sqlite2List();
                }
                hideProgressFragment();
            }

            @Override
            public void onFailure(Call<QuestionResponse> call, Throwable t) {
                Log.e("API_ERROR", "Fetch failed: " + t.getMessage());
                hideProgressFragment();
            }
        });
    }

    private void fixNullValuesDatabase(SQLiteDatabase writableDb) {
        Log.d("DB_DEBUG", "Checking and fixing null values in SQLite before loading.");

        Cursor checkCursor = writableDb.rawQuery("SELECT * FROM questions", null);

        if (checkCursor.moveToFirst()) {
            do {
                int idIndex = checkCursor.getColumnIndex("id");
                int questionIndex = checkCursor.getColumnIndex("question");
                int optionsIndex = checkCursor.getColumnIndex("options");
                int answerIndex = checkCursor.getColumnIndex("answer");
                int referenceIndex = checkCursor.getColumnIndex("reference");

                // Ensure columns exist before proceeding
                if (idIndex == -1 || questionIndex == -1 || optionsIndex == -1 ||
                        answerIndex == -1 || referenceIndex == -1) {
                    Log.e("DB_DEBUG", "❌ Column index not found! Check your database schema.");
                    break;
                }

                int id = checkCursor.getInt(idIndex);
                String question = checkCursor.getString(questionIndex);
                String options = checkCursor.getString(optionsIndex);
                String answer = checkCursor.getString(answerIndex);
                String reference = checkCursor.getString(referenceIndex);

                ContentValues values = new ContentValues();

                // Only fix fields that are null
                if (question == null) values.put("question", "No question available");
                if (options == null) values.put("options", "[]"); // Default empty JSON array
                if (answer == null) values.put("answer", "No answer available");
                if (reference == null) values.put("reference", "N/A");

                if (values.size() > 0) { // Update only if there's something to fix
                    writableDb.update("questions", values, "id=?", new String[]{String.valueOf(id)});
                    Log.d("DB_DEBUG", "✅ Fixed null values for ID: " + id);
                }

            } while (checkCursor.moveToNext());
        } else {
            Log.d("DB_DEBUG", "No records found in the database.");
        }

        checkCursor.close();
    }




    private void logAllQuestions(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT * FROM questions", null); // Removed LIMIT 1 to log all

        if (cursor == null || cursor.getCount() == 0) {
            Log.d("QuizApp", "No questions found in the database.");
            return;
        }

        String[] columnNames = cursor.getColumnNames();
        Log.d("QuizApp", "Columns in DB: " + Arrays.toString(columnNames));

        Log.d("QuizApp", "=== All Questions in SQLite ===");

        while (cursor.moveToNext()) {
            int idIndex = cursor.getColumnIndex("id");
            int questionIndex = cursor.getColumnIndex("question");
            int optionsIndex = cursor.getColumnIndex("options");
            int correctAnswerIndex = cursor.getColumnIndex("correct_answer");
            int referenceIndex = cursor.getColumnIndex("reference");

            if (idIndex == -1 || questionIndex == -1 || optionsIndex == -1 ||
                    correctAnswerIndex == -1 || referenceIndex == -1) {
                Log.e("QuizApp", "One or more columns are missing in the database!");
                break;
            }

            int id = cursor.getInt(idIndex);
            String question = cursor.isNull(questionIndex) ? "NULL" : cursor.getString(questionIndex);
            String optionsJson = cursor.isNull(optionsIndex) ? "{}" : cursor.getString(optionsIndex);
            String correctAnswer = cursor.isNull(correctAnswerIndex) ? "NULL" : cursor.getString(correctAnswerIndex);
            String reference = cursor.isNull(referenceIndex) ? "NULL" : cursor.getString(referenceIndex);

            // Parse options JSON to extract Option A, B, C, D
            String optionA = "N/A", optionB = "N/A", optionC = "N/A", optionD = "N/A";
            try {
                JSONObject optionsObj = new JSONObject(optionsJson);
                optionA = optionsObj.optString("A", "Option A");
                optionB = optionsObj.optString("B", "Option B");
                optionC = optionsObj.optString("C", "Option C");
                optionD = optionsObj.optString("D", "Option D");
            } catch (JSONException e) {
                Log.e("QuizApp", "Error parsing options JSON: " + e.getMessage());
            }

            Log.d("QuizApp", "ID: " + id + " | Question: " + question);
            Log.d("QuizApp", "Options: A) " + optionA + " | B) " + optionB + " | C) " + optionC + " | D) " + optionD);
            Log.d("QuizApp", "Correct Answer: " + correctAnswer);
            Log.d("QuizApp", "Reference: " + reference);
        }

        cursor.close(); // ✅ Close the cursor only after all processing is done
    }




    // 🛠 Reset Shuffle on Pause/Restart
    @Override
    protected void onPause() {
        super.onPause();
        shuffledQuestions.clear(); // 🔄 Reset for next session
    }

    private void displayQuestion(Questions randomQuestion) {
        if (randomQuestion == null) {
            Log.e("QuizApp", "displayQuestion called with null question");
            return; // Prevent crashes
        }

        currentQuestion = randomQuestion; // Assign the current question

        // Ensure all fields are valid before using them
        questionText.setText(randomQuestion.getQuestion() != null ? randomQuestion.getQuestion() : "No question available.");
        referenceText.setText(randomQuestion.getReference() != null ? "Reference: " + randomQuestion.getReference() : "Reference: N/A");

        correctAnswer = randomQuestion.getCorrectAnswer(); // Store correct answer for validation

        Log.d("QuizApp", "Correct Answer Set: " + correctAnswer);




        // Ensure options map is not null before accessing it
        Map<String, String> options = randomQuestion.getOptions();
        if (options != null) {
            Log.d("QuizApp", "Options retrieved: " + options.toString());
            option1.setText(options.containsKey("A") ? options.get("A") : "Option A");
            option2.setText(options.containsKey("B") ? options.get("B") : "Option B");
            option3.setText(options.containsKey("C") ? options.get("C") : "Option C");
            option4.setText(options.containsKey("D") ? options.get("D") : "Option D");



        } else {
            Log.e("QuizApp", "randomQuestion options are null, setting default values.");
            setDefaultOptions(); // Call helper method to set defaults
        }


    }


    // Helper method to set default options
    private void setDefaultOptions() {
        option1.setText("Option A");
        option2.setText("Option B");
        option3.setText("Option C");
        option4.setText("Option D");
    }

    private void checkAnswer(int checkedId) {
      //  startCountdown();
    RadioButton selectedOption = findViewById(checkedId);
    if (selectedOption != null) {
       // correctAnswerText.setText("Correct Answer: " + correctAnswer);
        onAnswerSelected(selectedOption.getText().toString());
        Toast.makeText(this, selectedOption.getText().equals(correctAnswer) ? "Correct!" : "Wrong!", Toast.LENGTH_SHORT).show();

        if(selectedOption.equals(correctAnswer)){
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(this, "Wrong! The correct answer is: " + selectedOption.getText().equals(correctAnswer), Toast.LENGTH_SHORT).show();
        }


    }
}







    private Set<Integer> usedQuestions = new HashSet<>(); // Keep track of used questions
    private Random random = new Random();


    public void showNextQuestion() {
        // Cancel the existing countdown if it's running
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Reset timer to the initial time (e.g., 30 seconds)
        timeLeftInMillis = 30000; // Adjust this value as needed

        // Restart the countdown
        startCountdown();


        // Reset UI components before setting new question
        answerGroup.clearCheck(); // Clears selected radio button
        correctAnswerText.setText(""); // Clears the correct answer display
        correctAnswerText.setVisibility(View.INVISIBLE); // Hide correct answer text
        referenceText.setText(""); // Clears reference
        questionText.setText(""); // Clears the question






        if (selectedQuestions == null || selectedQuestions.isEmpty()) {
            Toast.makeText(this, "No questions available!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentIndex >= selectedQuestions.size()) {
            Toast.makeText(this, "You've completed the quiz!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the next question from the list
        Questions nextQuestion = selectedQuestions.get(currentIndex);


        // Log question details for debugging
        Log.d("QuizApp", "Displaying question at index: " + currentIndex);
        Log.d("QuizApp", "Question: " + nextQuestion.getQuestion());


        // Retrieve options and confirm they're properly fetched
        Map<String, String> options = nextQuestion.getOptions();
        if (options != null) {
            if (options.isEmpty()) {
                Log.w("QuizApp", "Options map is empty for question: " + nextQuestion.getQuestion());
            } else {
                for (Map.Entry<String, String> entry : options.entrySet()) {
                    Log.d("QuizApp", "Option " + entry.getKey() + ": " + entry.getValue());
                }
            }
        } else {
            Log.e("QuizApp", "Options are null for question: " + nextQuestion.getQuestion());
        }




        // Display the question
        displayQuestion(nextQuestion);

      //  correctAnswer = nextQuestion.getCorrectAnswer(); // Store correct answer for validation
        //Log.d("QuizApp", "Correct Answer Set: " + correctAnswer);


        // Move to the next index
        currentIndex++;


    }

    private Questions getUniqueQuestion(List<Questions> newQuestions) {
        SharedPreferences prefs = getSharedPreferences("QuizPrefs", MODE_PRIVATE);
        Set<String> usedQuestions = prefs.getStringSet("used_questions", new HashSet<>());

        for (Questions q : newQuestions) {
            if (!usedQuestions.contains(q.getNo())) {
                // ✅ Store new question
                usedQuestions.add(String.valueOf(q.getNo()));

                prefs.edit().putStringSet("used_questions", usedQuestions).apply();
                return q;
            }
        }
        return null; // No unique question found
    }

    private void resetQuiz() {
        usedQuestions.clear(); // Reset used questions
        Toast.makeText(this, "Quiz Reset! Starting again...", Toast.LENGTH_SHORT).show();
        showNextQuestion();
    }


}
