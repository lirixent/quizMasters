package com.example.biblequiz;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Questions {
    @SerializedName("Question")
    private String question;
    @SerializedName("Options")
    private Map<String, String> options; // Maps "a", "b", "c", "d" dynamically

    @SerializedName("Answer")
    private String correctAnswer;
    @SerializedName("Bible_reference")
    private String reference;
    @SerializedName("no")
    private int no;

    @SerializedName("Category")
    private String category;

    private int id;  // <-- Add ID here

    // ✅ Constructor
    public Questions(int id,  String question, Map<String, String> options, String correctAnswer, String reference) {
        this.id = id;
       // this.no = no;
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.reference = reference;
    }

    public int getId() { return id; } // Getter for ID

    public String getCategory() {
        return category;
    }

    public String getQuestion() { return question; }
    public Map<String, String> getOptions() { return options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getReference() { return reference; }
    public int getNo() { return no; } // Getter for no

    // ✅ Add a setter method for options
    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    // ✅ Getter method if needed



}
