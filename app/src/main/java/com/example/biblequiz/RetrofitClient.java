package com.example.biblequiz;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://10.0.2.2:3000/"; // Maps to localhost:3000 for emulator
    private static Retrofit retrofit;

    private static QuestionInterface questionInterface;  // ✅ Use your existing interface


    public static Retrofit getInstance() {
        if (retrofit == null) {
            // Configure OkHttpClient with increased timeout and retry on failure
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(120, TimeUnit.SECONDS)  // Wait up to 120 seconds to connect
                    .readTimeout(120, TimeUnit.SECONDS)     // Wait up to 120 seconds for data
                    .writeTimeout(120, TimeUnit.SECONDS)    // Wait up to 120 seconds to send data
                    .retryOnConnectionFailure(true)         // Retry if connection fails
                    .build();

            // Build Retrofit instance with new client
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)  // Use custom OkHttpClient
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            questionInterface = retrofit.create(QuestionInterface.class);  // ✅ Initialize your interface
        }
        return retrofit;
    }

    // ✅ Now provides QuestionInterface instead of APIService
    public static QuestionInterface getQuestionAPI() {
        if (questionInterface == null) {
            getInstance();  // Ensure Retrofit is initialized
        }
        return questionInterface;
    }


}
