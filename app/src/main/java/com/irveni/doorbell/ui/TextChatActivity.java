package com.irveni.doorbell.ui;

import static android.content.ContentValues.TAG;
import static com.irveni.doorbell.functions.Common.BASEURL;
import static com.irveni.doorbell.functions.Common.BASEURL_Image;
import static com.irveni.doorbell.functions.Common.prefs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.chibde.visualizer.LineBarVisualizer;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.irveni.doorbell.R;
import com.irveni.doorbell.functions.Common;
import com.tyorikan.voicerecordingvisualizer.RecordingSampler;
import com.tyorikan.voicerecordingvisualizer.VisualizerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class TextChatActivity extends AppCompatActivity {

    Button b,voiceRecord;
    EditText sendMessage;
    Thread t;
    ImageButton send,pause_play,cancel_button;
    int id = prefs.getInt("userid", -1);
    SimpleDateFormat format = new SimpleDateFormat("MMM dd,yyyy hh:mm a");
    TextView timer;

    LinearLayout voiceRecordingLayout,edTextLinear;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_chat);
//        getSupportActionBar().hide();

        b=findViewById(R.id.buttonSend);
        sendMessage = findViewById(R.id.sendMessage);
        voiceRecord = findViewById(R.id.buttonVoice);
        timer=findViewById(R.id.recordingTime);

        send = findViewById(R.id.send);
        cancel_button = findViewById(R.id.cancel_button);
        pause_play = findViewById(R.id.pause_play);


        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        uploadAudio();
                    }
                }).start();
                voiceRecordingLayout.setVisibility(View.GONE);
                edTextLinear.setVisibility(View.VISIBLE);
            }
        });

        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecording();
                voiceRecordingLayout.setVisibility(View.GONE);
                edTextLinear.setVisibility(View.VISIBLE);

            }
        });

        pause_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(isRecording){
                   mRecorder.pause();
                   //pause_play.setImageDrawable(getResources().getDrawable(R.id.pla));
                }else{
                    mRecorder.resume();
                }

            }
        });

        voiceRecordingLayout = findViewById(R.id.voiceRecordingLayout);
        edTextLinear = findViewById(R.id.edTextLinear);

        sendMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
              if(!s.equals(null)) {
                    b.setVisibility(View.VISIBLE);
                    voiceRecord.setVisibility(View.GONE);
                }else{
                    b.setVisibility(View.GONE);
                    voiceRecord.setVisibility(View.VISIBLE);

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        voiceRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(voiceRecordingLayout.getVisibility() == View.GONE){
                    voiceRecordingLayout.setVisibility(View.VISIBLE);
                    edTextLinear.setVisibility(View.GONE);
                    setup_audio();
                    setup_timer();

                }else{
                    voiceRecordingLayout.setVisibility(View.GONE);
                    edTextLinear.setVisibility(View.VISIBLE);
                }

            }
        });

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if(sendMessage.getText().toString().equals("")){
                    Toast.makeText(TextChatActivity.this,"Cannot Send Empty Message",Toast.LENGTH_SHORT).show();
                    return;
                }
                TextMessage temp  = new TextMessage(sendMessage.getText().toString(),"Visitor",format.format(new Date()),0,"text");
                //send_message(temp);
                sendMessageToServer(sendMessage.getText().toString());

                JSONObject jsonObject = temp.toJson();

                try {
                    jsonObject.put("roomid",id+"");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSocket.emit("send_message",jsonObject);
                sendMessage.setText("");
                b.setVisibility(View.GONE);
                voiceRecord.setVisibility(View.VISIBLE);
            }
        });


        Common.activeactivity = this;
        socketsetup();
        getMessageFromServer();


    }


    RecordingSampler mRecordingSampler;
    VisualizerView mVisualizerView3;
    MediaRecorder mRecorder;
    StorageReference mFirebaseStorage;
    ProgressDialog mProgress;
    String mLocalFilePath = null;
    boolean isRecording = false;

    double _timer = 0.0;
    void setup_timer(){

        _timer = 0.0;
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



    void setup_audio(){


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
        mVisualizerView3 = findViewById(R.id.mainvisualizer);

        // create AudioRecord
        mRecordingSampler = new RecordingSampler();
        mRecordingSampler.setVolumeListener(new RecordingSampler.CalculateVolumeListener() {
            @Override
            public void onCalculateVolume(int volume) {

            }
        });
        mRecordingSampler.setSamplingInterval(100);
        mRecordingSampler.link(mVisualizerView3);
        startRecording();
        isRecording = true;

    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mLocalFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

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

        }catch (Exception x){
            ///mStatusTv.setText(getString(R.string.tab_and_hold));

        }
    }
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("audio/*");
    int counter = 0;

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
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(image.getPath());
        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int millSecond = Integer.parseInt(durationStr);
        RequestBody requestBody1 = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("userid", id+"")
                .addFormDataPart("message", "")
                .addFormDataPart("type", "audio")
                .addFormDataPart("duration", millSecond+"")
                .addFormDataPart("sent_by","Visitor") //                params.put();
                .addFormDataPart("file", "audiorecordtest.mp3", RequestBody.create(MEDIA_TYPE_PNG, image))
                .build();

        okhttp3.Request request1 = new okhttp3.Request
                .Builder()
                .url(BASEURL+"send-message")
                .post(requestBody1)
                .build();
        try {
            okhttp3.Response response = egarClient.newCall(request1).execute();
            String something = new String(response.body().bytes());
            System.out.println("MY Response : "+something);



            TextMessage temp  = new TextMessage(something,"Visitor",format.format(new Date()),millSecond,"audio");

            //sendMessageToServer(sendMessage.getText().toString());

            JSONObject jsonObject = temp.toJson();

            try {
                jsonObject.put("roomid",id+"");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mSocket.emit("send_message",jsonObject);


        }
        catch (Exception ex){
            System.out.println("Error uploading : reason -> "+ex.getMessage());

            if(counter++ == 3){
                counter = 0;
                return;
            }else{
                uploadAudio();
            }

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


    private void sendMessageToServer(String message) {
        // TODO: Implement this method to send token to your app server.
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        StringRequest stringRequest=new StringRequest(Request.Method.POST, BASEURL+"send-message",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        System.out.println(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Toast.makeText(getApplicationContext(), "Error Volley" + error, Toast.LENGTH_LONG).show();
                    }
                })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String,String> params=new HashMap<>();

                int id = prefs.getInt("userid",-1);

                params.put("userid",id+"");
                params.put("message",message);
                params.put("sent_by","Visitor");
                params.put("type","text");
                params.put("duration","0");

                return params;

            }
        };
        RequestQueue requestQueue= Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);

    }



    private void getMessageFromServer() {



        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        StringRequest stringRequest=new StringRequest(Request.Method.POST, BASEURL+"get-message",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {


                        System.out.println(response);
                        LinearLayout options_layout = (LinearLayout) findViewById(R.id.messages);

                        try {
                            JSONArray json = new JSONArray(response);

                            options_layout.removeAllViews();



                            for(int i=0;i<json.length();i++){

                                JSONObject object = json.getJSONObject(i);
                                String message = object.getString("message");
                                String sender = object.getString("orignator");
                                String date = object.getString("created_date");
                                String type = object.getString("messagetype");
                                int duration = object.has("duration") ? object.getInt("duration") : 0;


                                TextMessage temp = new TextMessage(message,sender,date,duration,type);


                                JSONObject jsonObject = temp.toJson();

                               /* try {
                                    jsonObject.put("roomid",id+"");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                mSocket.emit("send_message",jsonObject);*/
                                send_message(temp);


                            }



                        } catch (JSONException e) {
                            //e.printStackTrace();
                        }



                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Toast.makeText(getApplicationContext(), "Error Volley" + error, Toast.LENGTH_LONG).show();
                    }
                })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String,String> params=new HashMap<>();

                //int id = prefs.getInt("userid",-1);

                params.put("userid",id+"");

                return params;

            }
        };
        RequestQueue requestQueue= Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);

    }



    private void send_message(TextMessage item) {

        LinearLayout options_layout = (LinearLayout) findViewById(R.id.messages);
        LayoutInflater inflater = getLayoutInflater();

        View v = inflater.inflate(R.layout.chat_items,null);

        LinearLayout incoming_message_container = v.findViewById(R.id.incoming_message_container);
        LinearLayout incoming_message_container_vm = v.findViewById(R.id.incomingVOiceMsg);

        TextView message_rec_date = v.findViewById(R.id.in_coming_date);

        ImageButton inplaymessage = v.findViewById(R.id.inComingplayvoicemesssage);

        if(item.messagetype.equals("audio")){


            inplaymessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //inplaymessage.setImageDrawable(getDrawable(R.drawable.pause));
                    setup(item,inplaymessage,v.findViewById(R.id.loader_outgoing),v);
                }
            });
            TextView duration;
            if(item.orignator.equals("Visitor")){
                duration = v.findViewById(R.id.duaration);
            }
            else{
                duration = v.findViewById(R.id.inComingduaration);

            }
            duration.setText(durationToTime(Math.round(item.duration/1000)));

           /* if(!item.orignator.equals("Visitor")) {
                setup(item,playmessage,v);
            }else{
                setup(item,inplaymessage,v);
            }*/



        }
        else{



        }


        if(item.orignator.equals("Visitor")) {
            if(item.messagetype.equals("audio")){

                ImageButton playmessage = v.findViewById(R.id.playvoicemesssage);
                LinearLayout out_going_message_container_vm = v.findViewById(R.id.outGoingVOiceMsg);

                playmessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //playmessage.setImageDrawable(getDrawable(R.drawable.pause));
                        setup(item,playmessage,v.findViewById(R.id.loader_outgoing),v);

                    }
                });

                TextView message_sent_date = v.findViewById(R.id.dateFromNotification);

                message_sent_date.setText(item.created_date);

                out_going_message_container_vm.setVisibility(View.VISIBLE);
            }else{

                LinearLayout out_going_message_container = v.findViewById(R.id.out_going_message_container);
                TextView message_sent_date = v.findViewById(R.id.out_going_date);
                TextView message_sent = v.findViewById(R.id.message_sent);
                message_sent.setText(item.message);
                message_sent_date.setText(item.created_date);
                out_going_message_container.setVisibility(View.VISIBLE);
            }

        }
        else {


            if(item.messagetype.equals("audio")){

                ImageButton playmessage = v.findViewById(R.id.inComingplayvoicemesssage);
                LinearLayout out_going_message_container_vm = v.findViewById(R.id.incomingVOiceMsg);

                playmessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //playmessage.setImageDrawable(getDrawable(R.drawable.pause));
                        setup(item,playmessage,v.findViewById(R.id.loader_incoming),v);

                    }
                });

                TextView message_sent_date = v.findViewById(R.id.inComingdateFromNotification);

                message_sent_date.setText(item.created_date);

                out_going_message_container_vm.setVisibility(View.VISIBLE);
            }else{

                LinearLayout out_going_message_container = v.findViewById(R.id.incoming_message_container);
                TextView message_sent_date = v.findViewById(R.id.in_coming_date);
                TextView message_sent = v.findViewById(R.id.message_received);
                message_sent.setText(item.message);
                message_sent_date.setText(item.created_date);
                out_going_message_container.setVisibility(View.VISIBLE);
            }

        }


        options_layout.addView(v);

        ScrollView scrollView = findViewById(R.id.scroll_view_chat);
        scrollView.post(new Runnable() {
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });





    }
    MediaPlayer mPlayer = new MediaPlayer();
    String prevurl="";
    ImageButton prevbtn;
    LineBarVisualizer plineBarVisualizer = null;
    double timerAudio = 0.0;

    String durationToTime(int duration){

        int rounded = duration;

        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);


        String timerstr = /*String.format("%02d",hours) + " : " +*/ String.format("%02d",minutes) + " : " + String.format("%02d",seconds);

        return timerstr;

    }


    void play_timer_audio(TextView durationview,int duration){


        new Thread(new Runnable() {
            @Override
            public void run() {

                timerAudio = 0.0;
                for(int i=0;i<Math.ceil(duration/1000)+1;i++){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Date newdate = new Date();

                            //newdate.

                            //long i = newdate.getTime() - currentdate.getTime();

                            int rounded = (int) Math.round(timerAudio++);

                            int seconds = ((rounded % 86400) % 3600) % 60;
                            int minutes = ((rounded % 86400) % 3600) / 60;
                            int hours = ((rounded % 86400) / 3600);


                            String timerstr = /*String.format("%02d",hours) + " : " +*/ String.format("%02d",minutes) + " : " + String.format("%02d",seconds);

                            durationview.setText(timerstr);

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

    void setup(TextMessage item, ImageButton playbtn, SpinKitView spinkit, View v){

        String url = BASEURL_Image+item.message; // your URL here

        if(mPlayer.isPlaying()){

            mPlayer.pause();
            prevbtn.setImageDrawable(getDrawable(R.drawable.play));
        }
        else if(url.equals(prevurl)){
            mPlayer.start();
            prevbtn.setImageDrawable(
                    getDrawable(item.orignator.equals("Visitor") ?
                            R.drawable.ic_baseline_pause_24_white :
                            R.drawable.ic_baseline_pause_24));

            return;
        }



        if(!url.equals(prevurl)){

            spinkit.setVisibility(View.VISIBLE);
            playbtn.setVisibility(View.GONE);
            prevbtn = playbtn;
            mPlayer.reset();

            prevurl = url;
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                mPlayer.setDataSource(url);
                mPlayer.prepareAsync(); // might take long! (for buffering, etc)
                mPlayer.start();

            } catch (IOException e) {
                e.printStackTrace();
            }



//        duration.setText(mPlayer.getDuration());
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    playbtn.setImageDrawable(
                            getDrawable(item.orignator.equals("Visitor") ?
                                    R.drawable.ic_baseline_pause_24_white :
                                    R.drawable.ic_baseline_pause_24));
                    playbtn.setVisibility(View.VISIBLE);
                    spinkit.setVisibility(View.GONE);
                    mPlayer.start();
                    TextView duration = null;
                    LineBarVisualizer lineBarVisualizer = null;

                    if(item.orignator.equals("Visitor")){
                        lineBarVisualizer = v.findViewById(R.id.visualizer);
                        duration = v.findViewById(R.id.duaration);
                        lineBarVisualizer.setColor(ContextCompat.getColor(TextChatActivity.this, R.color.white));

                    }else{
                        lineBarVisualizer = v.findViewById(R.id.visualizer1);
                        duration = v.findViewById(R.id.inComingduaration);
                        lineBarVisualizer.setColor(ContextCompat.getColor(TextChatActivity.this, R.color.colorPrimary));

                    }
                    play_timer_audio(duration,mPlayer.getDuration());

                    //mPlayer.start();

                    lineBarVisualizer.setDensity(70);
                    lineBarVisualizer.setPlayer(mPlayer.getAudioSessionId());


                }
            });

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    playbtn.setImageDrawable(getDrawable(R.drawable.play));
                    timerAudio = 0.0;

                }
            });






        }



    }



    class TextMessage{

        String message;
        String orignator;
        String created_date;
        String messagetype;
        int duration;

        public TextMessage(String message, String orignator, String created_date, int duration, String messagetype) {
            this.message = message;
            this.orignator = orignator;
            this.created_date = created_date;
            this.duration = duration;
            this.messagetype = messagetype;

        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getOrignator() {
            return orignator;
        }

        public void setOrignator(String orignator) {
            this.orignator = orignator;
        }

        public String getCreated_date() {
            return created_date;
        }

        public void setCreated_date(String created_date) {
            this.created_date = created_date;
        }

        public String getMessagetype() {
            return messagetype;
        }

        public void setMessagetype(String messagetype) {
            this.messagetype = messagetype;
        }


        public JSONObject toJson(){

            JSONObject object = new JSONObject();

            try {
                object.put("message", message);
                object.put("orignator", orignator);
                object.put("created_date", created_date);
                object.put("duration", duration);
                object.put("messagetype", messagetype);
            }catch (Exception ex){

            }

            return object;
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            t.interrupt();

        }catch (Exception ex){
            System.out.println(ex.getMessage());
        }

    }


    private Socket mSocket;

    void socketsetup() {


        try {
            //mSocket = IO.socket("http://192.168.100.155:8000");
            mSocket = IO.socket("http://20.227.167.170:8080");

            LinearLayout options_layout = (LinearLayout) findViewById(R.id.messages);

            mSocket.on("doorbell_joined", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    JSONObject temp = (JSONObject) args[0];


                }
            });
            getMessageFromServer();
            mSocket.on("response", new Emitter.Listener() {
                @Override
                public void call(Object... args) {


                    System.out.println(args[0]);

                    try {
                        JSONObject object = (JSONObject) args[0];

                    /*    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                options_layout.removeAllViews();
                            }
                        });
*/

                        //                      for(int i=0;i<json.length();i++){

                        //JSONObject object = json.getJSONObject(i);
                        String message = object.getString("message");
                        String sender = object.getString("orignator");
                        int duration = object.has("duration") ? object.getInt("duration") : 0;

                        String date = object.getString("created_date");
                        String type = object.getString("messagetype");


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                send_message(new TextMessage(message,sender,date,duration,type));
                            }
                        });


                        //                    }


                    } catch (JSONException e) {
                        //e.printStackTrace();
                    }



                }
            });


            mSocket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Transport transport = (Transport) args[0];

                    transport.on(Transport.EVENT_ERROR, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Exception e = (Exception) args[0];
                            Log.e(TAG, "Transport error " + e);
                            e.printStackTrace();
                            e.getCause().printStackTrace();
                        }
                    });


                }
            });

            mSocket.connect();

            mSocket.emit("begin_text_chat", id);

        } catch (URISyntaxException e) {


        }
    }


}