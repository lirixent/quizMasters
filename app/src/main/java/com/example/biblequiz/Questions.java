package com.example.biblequiz;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Questions {
    @SerializedName("Question")
    private String question;
    @SerializedName("Option")
    private Map<String, String> options; // Maps "a", "b", "c", "d" dynamically

    @SerializedName("Answer")
    private String correctAnswer;
    @SerializedName("Bible_reference")
    private String reference;
    @SerializedName("no")
    private int no;

    public String getQuestion() { return question; }
    public Map<String, String> getOptions() { return options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getReference() { return reference; }
    public int getNo() { return no; } // Getter for no


}
