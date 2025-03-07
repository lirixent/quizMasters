package com.example.biblequiz;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

//de.hdodenhof.circleimageview.CircleImageView;

import de.hdodenhof.circleimageview.CircleImageView;


import android.widget.Button;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private Spinner categorySpinner;
    private List<String> categories;
    private SharedPreferences sharedPreferences;
    private TextView usernameTextView;
    private CircleImageView profileIcon;
    private TextView questionTypeText, timeLeftText, referenceText, correctAnswerText;
    private RadioButton option1, option2, option3, option4;
    private RadioGroup answerGroup;

   // private String reference = "Exodus 3:10";
    //private String correctAnswer = "Moses";

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 30000; // 30 seconds

    private question apiService;
    private String correctAnswer;

    private List<Question> questionList = new ArrayList<>();
    private int currentIndex = 0;
    private int questionNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        questionTypeText = findViewById(R.id.questionTypeText);
        timeLeftText = findViewById(R.id.timeLeftText);
        referenceText = findViewById(R.id.referenceText);
        correctAnswerText = findViewById(R.id.correctAnswerText);

        startCountdown();

        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        answerGroup = findViewById(R.id.answerGroup);


        fetchQuestions();

        referenceText.setText("Reference: [Book Name]");
        correctAnswerText.setVisibility(View.GONE);

        // Listener for answer selection
        answerGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedOption = findViewById(checkedId);
            if (selectedOption != null) {
                onAnswerSelected(selectedOption.getText().toString());
            }
        });

        updateQuizDetails("MCQ", "00:30", "[Book Name]");

        categorySpinner = findViewById(R.id.categorySpinner);
        categories = new ArrayList<>();
        categories.add("Bible Quiz");
        categories.add("Truth or Dare");
        fetchCategoriesFromMongoDB();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(adapter);

        sharedPreferences = getSharedPreferences("QuizApp", Context.MODE_PRIVATE);
        usernameTextView = findViewById(R.id.usernameTextView);
        profileIcon = findViewById(R.id.profileIcon);
        checkAndPromptUsername();
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
        correctAnswerText.setText("Correct Answer: " + correctAnswer);
        referenceText.setText("Reference: " + reference);
        correctAnswerText.setVisibility(View.VISIBLE);

        Toast.makeText(this, selectedAnswer.equals(correctAnswer) ? "Correct!" : "Wrong! The correct answer is: " + correctAnswer, Toast.LENGTH_SHORT).show();
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

    private void fetchQuestions() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        question apiService = retrofit.create(question.class);
        Call<QuestionResponse> call = apiService.getQuestions();

        call.enqueue(new Callback<QuestionResponse>() {
            @Override
            public void onResponse(Call<QuestionResponse> call, Response<QuestionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    questionList = response.body().getQuestions();
                    Collections.shuffle(questionList); // Shuffle questions
                    showNextQuestion();
                }
            }

            @Override
            public void onFailure(Call<QuestionResponse> call, Throwable t) {
                // Handle failure
            }
        });
    }

    private void showNextQuestion() {
        if (currentIndex < questionList.size()) {
            Question currentQuestion = questionList.get(currentIndex);
            questionText.setText(currentQuestion.getQuestion());
            referenceText.setText(currentQuestion.getBibleReference());
            option1.setText(currentQuestion.getOptions().get("a"));
            option2.setText(currentQuestion.getOptions().get("b"));
            option3.setText(currentQuestion.getOptions().get("c"));
            option4.setText(currentQuestion.getOptions().get("d"));
            currentIndex++;
        }
    }


}
