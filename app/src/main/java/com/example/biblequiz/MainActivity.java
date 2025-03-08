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
import java.util.HashSet;
import java.util.List;

//de.hdodenhof.circleimageview.CircleImageView;

import de.hdodenhof.circleimageview.CircleImageView;
import java.util.Map;

import android.widget.Button;
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

    private Spinner categorySpinner;
    private List<String> categories;
    private SharedPreferences sharedPreferences;
    private TextView usernameTextView;
    private CircleImageView profileIcon;
    private TextView questionTypeText, timeLeftText, referenceText, questionText, correctAnswerText;
    private RadioButton option1, option2, option3, option4;
    private RadioGroup answerGroup;
    private Button nextQuestionButton;

    private Questions currentQuestion;


    // private String reference = "Exodus 3:10";
    //private String correctAnswer = "Moses";

  //  String reference = currentQuestion.getReference(); // Ensure `currentQuestion` is defined
//referenceText.setText("Reference: " + reference);


    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 30000; // 30 seconds

    private QuestionInterface apiService;

    private String correctAnswer;

    private List<Questions> questionList = new ArrayList<>();
    private int currentIndex = 0;
    private int questionNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        questionText = findViewById(R.id.questionText);
        questionTypeText = findViewById(R.id.questionTypeText);
        timeLeftText = findViewById(R.id.timeLeftText);
        referenceText = findViewById(R.id.referenceText);
        correctAnswerText = findViewById(R.id.correctAnswerText);

        startCountdown();

        option1 = findViewById(R.id.option1);
        option2 = findViewById(R.id.option2);
        option3 = findViewById(R.id.option3);
        option4 = findViewById(R.id.option4);
        nextQuestionButton = findViewById(R.id.nextQuestionButton);
        answerGroup = findViewById(R.id.answerGroup);


      //  fetchQuestions();

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
        fetchQuestions();

        nextQuestionButton.setOnClickListener(v -> showNextQuestion());
        answerGroup.setOnCheckedChangeListener((group, checkedId) -> checkAnswer(checkedId));
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

        Toast.makeText(this, selectedAnswer.equals(correctAnswer) ? "Correct!" : "Wrong! The correct answer is: " + correctAnswer, Toast.LENGTH_SHORT).show();


       // correctAnswerText.setVisibility(View.VISIBLE);

        //Toast.makeText(this, selectedAnswer.equals(correctAnswer) ? "Correct!" : "Wrong! The correct answer is: " + correctAnswer, Toast.LENGTH_SHORT).show();
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
        Retrofit retrofit = RetrofitClient.getInstance();
        QuestionInterface service = retrofit.create(QuestionInterface.class);

        //QuestionInterface service = RetrofitClient.getInstance().create(QuestionInterface.class);

        service.getRandomQuestions().enqueue(new Callback<QuestionResponse>() {
            @Override
            public void onResponse(Call<QuestionResponse> call, Response<QuestionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    questionList = response.body().getQuestions(); // Store all 203 questions
                    usedQuestions.clear(); // Reset used questions tracking
                    if (!questionList.isEmpty()) {
                        showNextQuestion(); // Show first question
                    } else {
                        Toast.makeText(MainActivity.this, "No questions found!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to load questions!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<QuestionResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void displayQuestion(Questions question) {

        currentQuestion = question; // Assign the current question
        //questionText.setText(question.getQuestion());
        //correctAnswer = question.getCorrectAnswer();

        if (question == null) return; // Prevent crashes

       // currentQuestion = question; // Assign the current question

        questionText.setText(question.getQuestion());
        referenceText.setText("Reference: " + question.getReference()); // Fixing the reference issue
        correctAnswer = question.getCorrectAnswer(); // Store correct answer for validation

      //  Map<String, String> options = question.getOptions();

        option1.setText(question.getOption1());
        option2.setText(question.getOption2());
        option3.setText(question.getOption3());
        option4.setText(question.getOption4());

        }


private void checkAnswer(int checkedId) {
    RadioButton selectedOption = findViewById(checkedId);
    if (selectedOption != null) {
        correctAnswerText.setText("Correct Answer: " + correctAnswer);
        Toast.makeText(this, selectedOption.getText().equals(correctAnswer) ? "Correct!" : "Wrong!", Toast.LENGTH_SHORT).show();
    }
}

    private Set<Integer> usedQuestions = new HashSet<>(); // Keep track of used questions
    private Random random = new Random();


    private void showNextQuestion() {
        if (usedQuestions.size() >= questionList.size()) {
            Toast.makeText(MainActivity.this, "You've completed the quiz!", Toast.LENGTH_SHORT).show();
            resetQuiz();
            return;
        }

        int index;
        do {
            index = random.nextInt(questionList.size()); // Pick a random question
        } while (usedQuestions.contains(index)); // Ensure it's not a duplicate

        usedQuestions.add(index); // Mark as used
        displayQuestion(questionList.get(index)); // Show the question
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
