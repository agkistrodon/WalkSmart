package com.example.walksmart;

import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class MainActivity extends AppCompatActivity {

    Button browseButton;
    TextView resultText;

    AnimationDrawable footAnimation;

    private String url = "http://127.0.0.1:5000";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("WalkSmart", "onCreate called");
        browseButton = findViewById(R.id.browse);
        resultText = findViewById(R.id.result);

        ImageView footImage = (ImageView) findViewById(R.id.footsteps);
        footImage.setBackgroundResource(R.drawable.footsteps);
        footAnimation = (AnimationDrawable) footImage.getBackground();

        footImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                footAnimation.start();
            }
        });

        browseButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showFileChooser(); }
        });

        // creating a client
        OkHttpClient okHttpClient = new OkHttpClient();

        Log.d("WalkSmart", "OkHttpClient created");
    }

    @Override
    protected void onStart() {
        super.onStart();
        footAnimation.start();
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 100);
        } catch (Exception e) {
            Toast.makeText(this, "Please install a file manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        Log.d("WalkSmart", "File chosen, onActivityResult");

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Log.d("WalkSmart", "reading file");

            Uri uri = data.getData();
            String path = uri.getPath().split(":")[1];
            Log.d("WalkSmart", "path = " + path);
            // path = "/storage/emulated/0/Download/testfiles/user01-50Hz-normal-1.csv";
            // path = "/sdcard/testfiles/user01-50Hz-normal-1.csv";
            File file = new File(path);
            try {
                upload(file);
            } catch(Exception e) {
                Log.d("WalkSmart", e.toString());
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
     void upload(File file) throws IOException, JSONException {
        Log.d("WalkSmart", "upload");

        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("csv", file.getName(),
                        RequestBody.create(MediaType.parse("text/csv"), file))
                .addFormDataPart("other_field", "other_field_value")
                .build();
        Request request = new Request.Builder().url(url).post(formBody).build();

        Log.d("WalkSmart", "before execute: " + request.toString());
        try {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            Log.d("WalkSmart", "request body: " + buffer.readUtf8());
        }
        catch (Exception e)
        {
            Log.d("WalkSmart", "exception: " + e.toString());
        }

        Call call  = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d("WalkSmart", "received response");
                if (response.isSuccessful()) {
                    Log.d("WalkSmart", "response successful");
                    try {
                        String resp = response.body().string();
                        Log.d("WalkSmart", "response received: " + resp);
                        JSONObject result = new JSONObject(resp);
                        double prob = result.getDouble("abnormal-prob");
                        int status = result.getInt("status");

                        Log.d("WalkSmart", "prob = " + prob);
                        Log.d("WalkSmart", "status = " + status);

                        runOnUiThread((new Runnable() {
                            @Override
                            public void run() {
                                if (status == 0) {
                                    if (prob > 0.5) {
                                        resultText.setText("Gait: Unsteady\n\nConfidence: "+ (int) (prob * 100) + "%");
                                        resultText.setTextColor(Color.rgb(193, 18, 31));
                                    }
                                    else {
                                        resultText.setText("Gait: Steady\n\nConfidence: " + (int)((1-prob) * 100) + "%");
                                        resultText.setTextColor(Color.rgb(239, 246, 224));
                                    }
                                } else {
                                    resultText.setText("Error");
                                }
                            }
                        }));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // Request not successful
                    Log.d("WalkSmart", "response unsuccessful");
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("WalkSmart", "response failure: " + e.toString());
            }
        });
    }

}
