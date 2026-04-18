package com.example.st_meet;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * DEMONSTRATION FILE: AI Summary API Integration using OkHttp.
 * This class shows how we will connect to ApyHub for real-time summarization.
 */
public class ApiDemoActivity extends AppCompatActivity {

    private TextView textApiResponse;
    private ProgressBar progressBar;
    private OkHttpClient client;

    // TODO: Replace with your actual ApyHub Token for the demo
    private static final String APY_TOKEN = "";
    private static final String API_URL = "https://api.apyhub.com/ai/summarize-text";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_demo);

        textApiResponse = findViewById(R.id.textApiResponse);
        progressBar = findViewById(R.id.progressBar);
        Button btnGenerate = findViewById(R.id.btnGenerateDemo);

        client = new OkHttpClient();

        btnGenerate.setOnClickListener(v -> {
            String sampleText = ((TextView)findViewById(R.id.textSampleSource)).getText().toString();
            generateAiSummary(sampleText);
        });
    }

    private void generateAiSummary(String textToSummarize) {
        // 1. Show loading state
        progressBar.setVisibility(View.VISIBLE);
        textApiResponse.setText("Connecting to AI Engine...");

        // 2. Build JSON Request Body
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("text", textToSummarize);
            jsonBody.put("summary_length", "medium");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody.toString(), mediaType);

        // 3. Create Request with Headers (Apy-Token)
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("apy-token", APY_TOKEN)
                .addHeader("Content-Type", "application/json")
                .build();

        // 4. Execute Async Call
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    textApiResponse.setText("Network Error: " + e.getMessage());
                    Log.e("API_DEMO", "Failed: ", e);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body() != null ? response.body().string() : "";
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        try {
                            // 5. Parse JSON Response
                            JSONObject jsonResponse = new JSONObject(responseData);
                            // Assuming the API returns {"data": "summary text..."}
                            String summary = jsonResponse.optString("data", "No summary found in response.");
                            textApiResponse.setText("AI Summary:\n\n" + summary);
                        } catch (JSONException e) {
                            textApiResponse.setText("Parse Error: " + responseData);
                        }
                    } else {
                        textApiResponse.setText("API Error (" + response.code() + "): " + responseData);
                    }
                });
            }
        });
    }
}
