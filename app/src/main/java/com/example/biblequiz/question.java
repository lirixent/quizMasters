import retrofit2.Call;
import retrofit2.http.GET;

public interface question {
    @GET("question/test") // This matches your API endpoint
    Call<Question> getRandomQuestion();
}
