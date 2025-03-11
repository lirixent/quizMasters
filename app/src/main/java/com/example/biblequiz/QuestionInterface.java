package com.example.biblequiz;

import retrofit2.Call;
import retrofit2.http.GET;

public interface QuestionInterface {
    @GET("question/test") // This matches your API endpoint
    Call<QuestionResponse> getAllQuestions();
}
