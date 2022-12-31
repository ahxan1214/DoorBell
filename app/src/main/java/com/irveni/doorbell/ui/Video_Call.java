package com.irveni.doorbell.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.irveni.doorbell.R;

public class Video_Call extends AppCompatActivity {

    ImageView close_video;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);
        getSupportActionBar().hide();

        close_video = findViewById(R.id.close_video);
        close_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent intent = new Intent(Video_Call.this,AudioRecording.class);
                startActivity(intent);*/
            }
        });
    }
}