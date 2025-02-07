package com.example.biblequiz;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private Spinner categorySpinner;
    private List<String> categories;

    private SharedPreferences sharedPreferences;
    private TextView usernameTextView;
    private CircleImageView profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        categorySpinner = findViewById(R.id.categorySpinner);

        // Default categories
        categories = new ArrayList<>();
        categories.add("Bible Quiz");
        categories.add("Truth or Dare");

        // Fetch additional categories from MongoDB (Cloud)
        fetchCategoriesFromMongoDB();

        // Set up spinner with default categories
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

        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle("Enter Your Username")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Save", (dialogInterface, i) -> {
                    String username = editText.getText().toString().trim();
                    if (!username.isEmpty()) {
                        sharedPreferences.edit().putString("username", username).apply();
                        displayUserInfo(username);

                       // 🚀 Now call the game mode selection dialog!
                        showGameModeDialog();





                    }
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> finish());

        dialog.show();
    }

    private void displayUserInfo(String username) {
        usernameTextView.setText(username);
        String initials = getInitials(username);
        createProfileIcon(initials);
    }

    private String getInitials(String name) {
        String[] words = name.split(" ");
        if (words.length == 0) return "";
        if (words.length == 1) return words[0].substring(0, 1).toUpperCase();
        return words[0].substring(0, 1).toUpperCase() + words[1].substring(0, 1).toUpperCase();
    }

    private void createProfileIcon(String initials) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(ContextCompat.getColor(this, R.color.teal_200));

        profileIcon.setImageDrawable(drawable);
        profileIcon.setPadding(20, 20, 20, 20);

        // Set initials as username text
        usernameTextView.setText(initials);
    }
    private void showGameModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Game Mode");

        String[] gameModes = {"Single Player", "Multiplayer"};
        builder.setSingleChoiceItems(gameModes, -1, (dialog, which) -> {
            if (which == 0) {
                // Single Player Mode selected
                startSinglePlayerMode();
            } else if (which == 1) {
                // Multiplayer Mode selected
                startMultiplayerMode();
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        builder.show();
    }

    private void fetchCategoriesFromMongoDB() {
        // TODO: Fetch categories from MongoDB and update the list dynamically.
        // Use an API call to get categories and add them to the `categories` list.
        // After fetching, call adapter.notifyDataSetChanged();
    }

}