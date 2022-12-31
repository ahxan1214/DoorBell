package com.irveni.doorbell.functions;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;

import io.socket.client.Socket;

public class BackgroundAudioRecording {


    Activity context;

    BackgroundAudioRecording(){
        this.context = context;
        startRecording();
    }

    AudioRecord audioRecord;
    private static final int BUFFER_SIZE_FACTOR = 2;
    private static final int SAMPLING_RATE_IN_HZ = 44100;

    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;


    /**
     * Size of the buffer where the audio data is stored by Android
     */
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;

    public void startRecording() {

        if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
                CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        audioRecord.startRecording();
        final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

        while (true){
            int result = audioRecord.read(buffer, BUFFER_SIZE);
            if (result < 0) {
                /*throw new RuntimeException("Reading of audio buffer failed: " +
                        getBufferReadFailureReason(result));*/
            }
            //outStream.write(buffer.array(), 0, BUFFER_SIZE);

            if(Common.mSocket.connected()){


            }


            buffer.clear();

        }


    }

}
