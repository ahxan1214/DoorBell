package com.irveni.doorbell.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.irveni.doorbell.R;

public class AudioRecording extends AppCompatActivity {

    ImageView close,start,send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recording);
        getSupportActionBar().hide();
        close = findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AudioRecording.this,TextChatActivity.class);
                startActivity(intent);
            }
        });
        start = findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start.setVisibility(View.GONE);
                send.setVisibility(View.VISIBLE);
            }
        });
        send = findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start.setVisibility(View.VISIBLE);
                send.setVisibility(View.GONE);
            }
        });

    }
}