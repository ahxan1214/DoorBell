package com.irveni.doorbell.Agora;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.irveni.doorbell.R;
import com.tyorikan.voicerecordingvisualizer.RecordingSampler;
import com.tyorikan.voicerecordingvisualizer.VisualizerView;

import java.util.Date;
import java.util.Locale;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;


public class VoiceChatViewActivity extends AppCompatActivity implements
        RecordingSampler.CalculateVolumeListener{

    private static final String LOG_TAG = VoiceChatViewActivity.class.getSimpleName();

    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;

    private RecordingSampler mRecordingSampler;
    private VisualizerView mVisualizerView;
    private VisualizerView mVisualizerView2;
    private VisualizerView mVisualizerView3;

    VisualizerView visualizerView;
    private FloatingActionButton mFloatingActionButton;

    private RtcEngine mRtcEngine; // Tutorial Step 1
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() { // Tutorial Step 1

        /**
         * Occurs when a remote user (Communication)/host (Live Broadcast) leaves the channel.
         *
         * There are two reasons for users to become offline:
         *
         *     Leave the channel: When the user/host leaves the channel, the user/host sends a goodbye message. When this message is received, the SDK determines that the user/host leaves the channel.
         *     Drop offline: When no data packet of the user or host is received for a certain period of time (20 seconds for the communication profile, and more for the live broadcast profile), the SDK assumes that the user/host drops offline. A poor network connection may lead to false detections, so we recommend using the Agora RTM SDK for reliable offline detection.
         *
         * @param uid ID of the user or host who
         * leaves
         * the channel or goes offline.
         * @param reason Reason why the user goes offline:
         *
         *     USER_OFFLINE_QUIT(0): The user left the current channel.
         *     USER_OFFLINE_DROPPED(1): The SDK timed out and the user dropped offline because no data packet was received within a certain period of time. If a user quits the call and the message is not passed to the SDK (due to an unreliable channel), the SDK assumes the user dropped offline.
         *     USER_OFFLINE_BECOME_AUDIENCE(2): (Live broadcast only.) The client role switched from the host to the audience.
         */
        @Override
        public void onUserOffline(final int uid, final int reason) { // Tutorial Step 4
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserLeft(uid, reason);
                }
            });
        }




        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);

            mRtcEngine.setEnableSpeakerphone(true);

            setup_timer();
        }

        /**
         * Occurs when a remote user stops/resumes sending the audio stream.
         * The SDK triggers this callback when the remote user stops or resumes sending the audio stream by calling the muteLocalAudioStream method.
         *
         * @param uid ID of the remote user.
         * @param muted Whether the remote user's audio stream is muted/unmuted:
         *
         *     true: Muted.
         *     false: Unmuted.
         */
        @Override
        public void onUserMuteAudio(final int uid, final boolean muted) { // Tutorial Step 6
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserVoiceMuted(uid, muted);
                }
            });
        }
    };

    @Override
    public void onBackPressed() {

        Toast.makeText(this,"Operation Not Allowed",Toast.LENGTH_LONG).show();
        return;

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR
                && (keyCode == KeyEvent.KEYCODE_BACK    || keyCode == KeyEvent.KEYCODE_HOME)
                && event.getRepeatCount() == 0)
        {
            onBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }
    ImageView voicegif;

    TextView timer;

    MediaRecorder recorder;
    String roomid;

    int counter = 0;


    double _timer = 0.0;
    void setup_timer(){

        Date currentdate = new Date();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Date newdate = new Date();

                            //newdate.

                            //long i = newdate.getTime() - currentdate.getTime();

                            int rounded = (int) Math.round(_timer++);

                            int seconds = ((rounded % 86400) % 3600) % 60;
                            int minutes = ((rounded % 86400) % 3600) / 60;
                            int hours = ((rounded % 86400) / 3600);


                            String timerstr = String.format("%02d",hours) + " : " + String.format("%02d",minutes) + " : " + String.format("%02d",seconds);

                            timer.setText(timerstr);

                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_call);

        Intent intent = getIntent();
        timer = findViewById(R.id.tv_1);



        roomid = intent.getStringExtra("roomid");
        if(roomid == null)
            roomid = "12345Happy";

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            initAgoraEngineAndJoinChannel();
        }

        mVisualizerView3 = findViewById(R.id.visualizer);

        mRecordingSampler = new RecordingSampler();
        mRecordingSampler.setVolumeListener(new RecordingSampler.CalculateVolumeListener() {
            @Override
            public void onCalculateVolume(int volume) {

            }
        });
        mRecordingSampler.setSamplingInterval(100);

        mRecordingSampler.link(mVisualizerView3);

        mRecordingSampler.startRecording();

/*
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
*/


/* speechRecognizer.setRecognitionListener(new RecognitionListener() {
    @Override
    public void onReadyForSpeech(Bundle bundle) {
        //listing_status.setText("Listining Speak Now");
        System.out.println("Ready for speach");

    }

    @Override
    public void onBeginningOfSpeech() {
        //Toast.makeText(VoiceControl.this,"Listining Speak Now",Toast.LENGTH_SHORT).show();
        System.out.println("Begin for speach");
        //voicegif.setVisibility(View.VISIBLE);
        voicegif.setVisibility(View.VISIBLE);

    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

        //visualizerView.addAmplitude(bytes.length/2);

    }

    @Override
    public void onEndOfSpeech() {
        //listing_status.setText("Press to Start Listening");
        System.out.println("End for speach");
        voicegif.setVisibility(View.GONE);
        */
/*speechRecognizer.stopListening();
        speechRecognizer.startListening(speechRecognizerIntent);*/
/*
        System.out.println("Finally");

    }

    @Override
    public void onError(int i) {
        System.out.println("Error here also");
//                voicegif.setVisibility(View.GONE);

        speechRecognizer.startListening(speechRecognizerIntent);
    }

    @Override
    public void onResults(Bundle bundle) {
        //voicegif.setVisibility(View.GONE);
        //  micButton.setImageResource(R.drawable.ic_mic_black_off);
        //ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        //Toast.makeText(com.irveni.admin.VoiceControl.this,data.get(0)+"", Toast.LENGTH_SHORT).show();
        //String result = data.get(0)+"";
        */
/*speechRecognizer.stopListening();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                speechRecognizer.startListening(speechRecognizerIntent);
            }
        },3000);*/
/*


        //editText.setText(data.get(0));
        speechRecognizer.startListening(speechRecognizerIntent);

    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {



    }
});
speechRecognizer.startListening(speechRecognizerIntent);

*/




    }

    private void initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine();     // Tutorial Step 1
        joinChannel();               // Tutorial Step 2
    }

    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i(LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(LOG_TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initAgoraEngineAndJoinChannel();
                } else {
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                    finish();
                }
                break;
            }
        }
    }

    public final void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
               // Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;
//        mRecordingSampler.release();
    }

    // Tutorial Step 7
    public void onLocalAudioMuteClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        // Stops/Resumes sending the local audio stream.
        mRtcEngine.muteLocalAudioStream(iv.isSelected());
    }

    // Tutorial Step 5
    public void onSwitchSpeakerphoneClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        // Enables/Disables the audio playback route to the speakerphone.
        //
        // This method sets whether the audio is routed to the speakerphone or earpiece. After calling this method, the SDK returns the onAudioRouteChanged callback to indicate the changes.
        mRtcEngine.setEnableSpeakerphone(view.isSelected());
    }

    // Tutorial Step 3
    public void onEncCallClicked(View view) {
        finish();
    }

    // Tutorial Step 1
    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
            // Sets the channel profile of the Agora RtcEngine.
            // CHANNEL_PROFILE_COMMUNICATION(0): (Default) The Communication profile. Use this profile in one-on-one calls or group calls, where all users can talk freely.
            // CHANNEL_PROFILE_LIVE_BROADCASTING(1): The Live-Broadcast profile. Users in a live-broadcast channel have a role as either broadcaster or audience. A broadcaster can both send and receive streams; an audience can only receive streams.
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);

            mRtcEngine.setEnableSpeakerphone(true);
            mRtcEngine.setDefaultAudioRoutetoSpeakerphone(true);


          /*  new Thread(new Runnable() {
                @Override
                public void run() {
                    mRecordingSampler.startRecording();

                }
            }).start();*/
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    // Tutorial Step 2
    private void joinChannel() {
        String accessToken = getString(R.string.agora_access_token);
        if (TextUtils.equals(accessToken, "") || TextUtils.equals(accessToken, "#YOUR ACCESS TOKEN#")) {
            accessToken = null; // default, no token
        }

        // Allows a user to join a channel.
        mRtcEngine.joinChannel(accessToken, roomid, "Extra Optional Data", 0); // if you do not specify the uid, we will generate the uid for you
        mRtcEngine.setEnableSpeakerphone(true);


    }

    // Tutorial Step 3
    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    // Tutorial Step 4
    private void onRemoteUserLeft(int uid, int reason) {
        showLongToast(String.format(Locale.US, "user %d left %d", (uid & 0xFFFFFFFFL), reason));
      //  View tipMsg = findViewById(R.id.quick_tips_when_use_agora_sdk); // optional UI
      //  tipMsg.setVisibility(View.VISIBLE);
    }

    // Tutorial Step 6
    private void onRemoteUserVoiceMuted(int uid, boolean muted) {
        showLongToast(String.format(Locale.US, "user %d muted or unmuted %b", (uid & 0xFFFFFFFFL), muted));
    }

    @Override
    public void onCalculateVolume(int volume) {

    }
}
