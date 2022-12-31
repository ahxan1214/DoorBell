package com.irveni.doorbell.ui;

/**
 - Flow -> Opens themes
 - Methods details
 -
 *
 *
 * */

import static com.irveni.doorbell.functions.Common.BASEURL;
import static com.irveni.doorbell.functions.Common.bellpressed;
import static com.irveni.doorbell.functions.Common.maintext;
import static com.irveni.doorbell.functions.Common.notificationid;
import static com.irveni.doorbell.functions.Common.playsound;
import static com.irveni.doorbell.functions.Common.prefs;
import static com.irveni.doorbell.functions.Common.requestWait;

import android.Manifest;
import android.app.IntentService;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.irveni.doorbell.FaceDetection.RealTimeFaceDetectionActivity;
import com.irveni.doorbell.R;
import com.irveni.doorbell.functions.BackgroundCamera;
import com.irveni.doorbell.functions.BackgroundFaceDetection;
import com.irveni.doorbell.functions.Common;
import com.irveni.doorbell.functions.Functions;
import com.irveni.doorbell.functions.GPSTracker;
import com.irveni.doorbell.functions.LiveStream;
import com.irveni.doorbell.functions.NetworkUtil;
import com.squareup.seismic.ShakeDetector;
import com.tyorikan.voicerecordingvisualizer.RecordingSampler;
import com.tyorikan.voicerecordingvisualizer.VisualizerView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * VoiceChatViewActity
 * VideoChatViewActity
 * */

/**
 * In this class we call all the main functionality which is working 
 * on the backend as well as
 *
 * Live Streaming -> Called by createing Object Of (LiveStream::class and BackgroundCamera::class)
 * Audio message - > Created Function for that to send the audio message named as (uplaod_audio())
 * (Face Recongition/Bell Functionality) -> called RealtimeFaceDetectionActivity for fae detection ->uses Camera X
 *      third party libraries
 *              Camera X v-1.2.0.rc01
 *                     this library is used to fetch live camera feed from the deivce camera and
 *                     send the feed directly to the cloud for further processing
 * SharedPreferences
 *          usage at usaage
 *
 * */

public class MainActivity extends AppCompatActivity implements ShakeDetector.Listener {

    ImageButton imgBtn;
    TextView temperature;
    TextView address;
    TextView helpingcontainer;
    TextView currenttime;

    ImageView btn_record;

    Handler handler;
    Runnable timeRunnable;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private String date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_can_ihelp);
//        getSupportActionBar().hide();


        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        ShakeDetector sd = new ShakeDetector(this);

        // A non-zero delay is required for Android 12 and up (https://github.com/square/seismic/issues/24)
        int sensorDelay = SensorManager.SENSOR_DELAY_GAME;

        sd.start(sensorManager, sensorDelay);

        temperature = findViewById(R.id.tv_1);
        address = findViewById(R.id.tv2);
        currenttime = findViewById(R.id.currenttime);
        helpingcontainer = findViewById(R.id.tv3);
        btn_record = findViewById(R.id.btn_record);

        btn_record.setVisibility(View.GONE);


        calendar = Calendar.getInstance();

        dateFormat = new SimpleDateFormat("MM-dd-yyyy,h:mm a");
        date = dateFormat.format(calendar.getTime());
        currenttime.setText(date);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                },
                1);


        imgBtn = findViewById(R.id.imgBtn);

        imgBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    imgBtn.setAlpha(128);
                }else{
                    imgBtn.setAlpha(256);
                }

                return false;
            }
        });

        imgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {





                if (bellpressed){
                    return;
                }
                playsound(MainActivity.this,R.raw.dingdongbell);

                bellpressed = true;
                /*
                Intent intent = new Intent(MainActivity.this, RealTimeFaceDetectionActivity.class);
                startActivity(intent);
                */


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if(!handler.hasCallbacks(timeRunnable))
                        handler.postDelayed(timeRunnable, 45000);
                }
                requestWait = true;


            }
        });
        ImageView imageView = (ImageView) findViewById(R.id.main);
        Glide.with(this)
                .asGif()
                .load(R.raw.waterfall)
                .into(imageView);


        //if(Common.temperature == "") {
        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                Common.temperature = Functions.get_weather(MainActivity.this);
                Functions.get_user_address(MainActivity.this, temperature, address, helpingcontainer);
                Functions.getBackground(MainActivity.this);
/*
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if(maintext.equals("temperature"))
                                temperature.setText(Common.temperature + "\u00B0");

                        }
                    });
*/


            }
        }).start();
       /* }else{
            temperature.setText(Common.temperature + "\u00B0");
            address.setText(Common.address);
            helpingcontainer.setText(Common.helptext);


        }

*/
        handler = new Handler();
        timeRunnable = new Runnable() {
            @Override
            public void run() {

                if (btn_record.getVisibility() == View.GONE) {
                    btn_record.setVisibility(View.VISIBLE);
                    if (requestWait)
                        playsound(MainActivity.this, R.raw.audio_message);
                    bellpressed = false;
                    handler.removeCallbacks(timeRunnable);
                    handler.postDelayed(timeRunnable, 15000);

                } else {
                    btn_record.setVisibility(View.GONE);
                }
                //String url = "https://seomagnifier.com/core/audio/c2fb12cdb910960f4c05cbace11c548d.mp3"; // your URL here


            }
        };

        setup_audio();

        new LiveStream(this);

        Common.activeactivity = this;


        //BackgroundFaceDetection bfd = new BackgroundFaceDetection(this,Runnable::run);
        Intent backgroundservice = new Intent(this, BackgroundFaceDetection.class);

        startService(backgroundservice);

        Intent backgroundservice_gps = new Intent(this, GPSTracker.class);

        startService(backgroundservice_gps);


        if(Common.temperature.isEmpty()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NetworkUtil.getConnectivityStatus(MainActivity.this);
                }
            }).start();
        }

        //bindService(backgroundservice);
        //new BackgroundCamera(MainActivity.this, Runnable::run);
        //new BackgroundFaceDetection(MainActivity.this, Runnable::run);

        //if(!maintext.isEmpty() && bellpressed)


    }




    @Override
    protected void onResume() {
        super.onResume();
/*

        if(Common.background != null){

            Common.background = new Thread(new Runnable() {
                @Override
                public void run() {
                    new BackgroundCamera(MainActivity.this, Runnable::run);
                    new BackgroundFaceDetection(MainActivity.this, Runnable::run);
                }
            });
            Common.background.start();



        }
*/


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(getBaseContext(), BackgroundFaceDetection.class));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    RecordingSampler mRecordingSampler;
    VisualizerView mVisualizerView3;
    MediaRecorder mRecorder;
    StorageReference mFirebaseStorage;
    ProgressDialog mProgress;
    String mLocalFilePath = null;

    void setup_audio() {


        mFirebaseStorage = FirebaseStorage.getInstance().getReference();

        mProgress = new ProgressDialog(this);

        // Record to the external cache directory for visibility
        mLocalFilePath = getExternalCacheDir().getAbsolutePath();
        mLocalFilePath += "/audiorecordtest.3gp";


        mRecordingSampler = new RecordingSampler();
        mRecordingSampler.setVolumeListener(new RecordingSampler.CalculateVolumeListener() {
            @Override
            public void onCalculateVolume(int volume) {

            }
        });
        mVisualizerView3 = findViewById(R.id.visualizer);

        // create AudioRecord
        mRecordingSampler = new RecordingSampler();
        mRecordingSampler.setVolumeListener(new RecordingSampler.CalculateVolumeListener() {
            @Override
            public void onCalculateVolume(int volume) {

            }
        });
        mRecordingSampler.setSamplingInterval(100);
        mRecordingSampler.link(mVisualizerView3);
        //startRecording();

        btn_record.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {

                    //voicegif.setVisibility(View.VISIBLE);

                    mRecordingSampler.startRecording();
                    mVisualizerView3.setVisibility(View.VISIBLE);

                    startRecording();
                    return true;
                } else if (action == MotionEvent.ACTION_UP) {
                    //voicegif.setVisibility(View.GONE);
                    if (mRecordingSampler.isRecording()) {
                        mRecordingSampler.stopRecording();
                        //mRecordingSampler.release();
                        mVisualizerView3.setVisibility(View.GONE);

                    }

                    stopRecording();
                    return true;
                }
                return false;
            }
        });


    }



    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mLocalFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        handler.removeCallbacks(timeRunnable);

        //mRecordingSampler.startRecording();
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            //  Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();

    }

    private void stopRecording() {

        try {
            if (mRecorder != null) {
                mRecorder.stop();
                mRecorder.release();
            }
            //      mRecordingSampler.release();
            //mVisualizerView3.setVisibility(View.GONE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    uploadAudio();
                }
            }).start();
        }catch (Exception x){
            ///mStatusTv.setText(getString(R.string.tab_and_hold));

        }
    }
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("video/3gpp");

    private void uploadAudio() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mStatusTv.setText(getString(R.string.upload_started));
                mProgress.setMessage("Uploading...");
                mProgress.show();
            }
        });
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int id = prefs.getInt("userid", -1);

        //imagecompress img = new imagecompress();

        //File image = img.decodeFile(imageName);

        // Uri localUri = Uri.fromFile(new File(mLocalFilePath));
        File image = new File(mLocalFilePath);
        OkHttpClient client = new OkHttpClient();
        OkHttpClient egarClient = client.newBuilder().readTimeout(0, TimeUnit.SECONDS).build();

        OkHttpClient.Builder build = client.newBuilder();
        build.connectTimeout(0, TimeUnit.SECONDS);

        RequestBody requestBody1 = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("userid", id+"")
                .addFormDataPart("file", "audiorecordtest.3gp", RequestBody.create(MEDIA_TYPE_PNG, image))
                .build();

        Request request1 = new Request
                .Builder()
                .url(BASEURL+"upload-audio/"+notificationid)
                .post(requestBody1)
                .build();
        try {
            Response response = egarClient.newCall(request1).execute();
            String something = new String(response.body().bytes());
            System.out.println("MY Response : "+something);
            handler.postDelayed(timeRunnable,15000);
        }catch (Exception ex){
            System.out.println("Error uploading : reason -> "+ex.getMessage());
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgress.dismiss();
//                mStatusTv.setText(getString(R.string.upload_finished));
                //btn_record.setVisibility(View.GONE);


            }
        });


    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Toast.makeText(this,"Exit Not Allowed",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void hearShake() {
        Toast.makeText(this, "Don't shake me, bro!", Toast.LENGTH_SHORT).show();
    }
}