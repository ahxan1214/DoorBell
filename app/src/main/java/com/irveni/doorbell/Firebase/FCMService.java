package com.irveni.doorbell.Firebase;

import static android.content.ContentValues.TAG;


import static com.irveni.doorbell.functions.Common.BASEURL;
import static com.irveni.doorbell.functions.Common.activeactivity;
import static com.irveni.doorbell.functions.Common.bellpressed;
import static com.irveni.doorbell.functions.Common.python_server_ip;
import static com.irveni.doorbell.functions.Common.requestWait;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.irveni.doorbell.Agora.VideoChatViewActivity;
import com.irveni.doorbell.Agora.VoiceChatViewActivity;
import com.irveni.doorbell.Models.IPCameras;
import com.irveni.doorbell.functions.DAO;
import com.irveni.doorbell.functions.Functions;
import com.irveni.doorbell.ui.BarCode;
import com.irveni.doorbell.ui.MainActivity;
import com.irveni.doorbell.ui.TextChatActivity;
import com.macrovideo.sdk.custom.DeviceInfo;
import com.macrovideo.sdk.tools.DeviceScanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class FCMService extends FirebaseMessagingService {

    SharedPreferences prefs;
    Intent nextscreen = null;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob();
            } else {
                // Handle message within 10 seconds
                handleNow();
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());

            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            // String image = remoteMessage.getNotification().getIcon();
            Map<String, String> data = remoteMessage.getData();
            String roomid = data.get("roomid");
            bellpressed = false;
            requestWait = false;
            if (title.equals("removevideo")) {
                //removeFrame = true;
                //fullscreenRenderer.setVisibility(View.GONE);
            }else if (title.equals("blocked")) {
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putInt("userid", -1).commit();
                Intent intent = new Intent(getApplicationContext(), BarCode.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else if (title.equals("chat")) {

                Intent intent = new Intent(getApplicationContext(), TextChatActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);

            } else if (title.equals("voice")) {

                /*Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("roomid", roomid);
                intent.putExtra("VideoEnable", false);
                startActivity(intent);*/

                getRoom2(false,roomid);


            } else if (title.equals("video")) {

                /*Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             //   intent.putExtra("roomid", roomid);
                intent.putExtra("roomid",roomid);

                intent.putExtra("VideoEnable", true);
                startActivity(intent);*/

                getRoom2(true,roomid);


            } else if (title.equals("multicam")) {

                JSONArray jsonArray = new JSONArray();

                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        ArrayList<DeviceInfo> devices = DeviceScanner.getDeviceListFromLan();

                        if (devices.size() > 0) {
                            for (int i = 0; i < devices.size(); i++) {
                                JSONObject object = new JSONObject();
                                try {
                                    object.put("id", devices.get(i).getStrName());
                                    object.put("ipaddress", devices.get(i).getStrIP());

                                    jsonArray.put(object);


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }


                            }
                        }

                        try {
                            Functions.sendresponse(getApplicationContext(), "findcams", jsonArray);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                t.start();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {



                            t.interrupt();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Functions.sendresponse(getApplicationContext(), "findcams", jsonArray);

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();

                    }
                },5000);





                /*
                Intent intent = new Intent(getApplicationContext(), MultiCam.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);*/

                } else if (title.equals("ipcamlist")) {

                    DAO dao = new DAO(getApplicationContext());
                    ArrayList<IPCameras> ipCameras = dao.getAllIpCams();

                    JSONArray jsonArray = new JSONArray();
                    for (int i = 0; i < ipCameras.size(); i++) {
                        JSONObject object = new JSONObject();
                        try {
                            object.put("id", ipCameras.get(i).getId());
                            object.put("title", ipCameras.get(i).getTitle());
                            object.put("ipaddress", ipCameras.get(i).getIp());

                            jsonArray.put(object);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }

                    try {
                        Functions.sendresponse(getApplicationContext(), "fetchcams", jsonArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } else  if (title.equals("connectcam")) {

                    String url = data.get("cameraipandpassword");
                    String _title = data.get("cameratitle");
                    String cameraip = data.get("cameraip");
                    start(url,cameraip,_title);

                }else  if (title.equals("broadcast")) {

                    /*Intent intent = new Intent();
                    intent.putExtra(Constants.KEY_CLIENT_ROLE, 1);
                    intent.putExtra("roomid", roomid);
                    intent.putExtra("VideoEnable", true);

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    intent.setClass(getApplicationContext(), MainActivity.class);
                    startActivity(intent);*/

                getRoom2(true,roomid);


                }
                else {

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);


                }


                //sendNotification(title,body);
                //sendNotification(remoteMessage.getNotification().getBody());

            }

            // Also if you intend on generating your own notifications as a result of a received FCM
            // message, here is where that should be initiated. See sendNotification method below.
    }

    void getRoom2(boolean video,String roomid){

        Intent intent = null;
        if(video) {

            intent = new Intent(getApplicationContext(), VideoChatViewActivity.class);
            //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("roomid",roomid);
            startActivity(intent);

        }else{

            intent = new Intent(getApplicationContext(), VoiceChatViewActivity.class);
            intent.putExtra("roomid",roomid);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

    }




    void start(String url,String ipaddress,String title){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();


                long camid = -1;

                IPCameras ipcam = new IPCameras(0,title,ipaddress,"connected");
                DAO dao = new DAO(getApplicationContext());
                dao.deleteAll();
                camid = dao.insertCam(ipcam);

                int userid = prefs.getInt("userid",-1);
                String serverurl = "rtmp://"+python_server_ip+"/live/"+title+"-"+userid;
               /* AssetManager assetManager = getAssets();

                try {
                    String appFileDirectory = getFilesDir().getPath();
                    String executableFilePath = appFileDirectory + "/ffmpeg";
                    File execFile = new File(executableFilePath);
                    execFile.setExecutable(true);
                    Process proc = null;
                    proc = Runtime.getRuntime().exec(executableFilePath+"-i \""+url+"\" -preset slower -crf 17 -c:a copy -s 720x480 -f flv "+serverurl);
                    System.out.println(proc.exitValue());

                } catch (IOException e) {
                    e.printStackTrace();
                }*/
                FFmpegSession rc1 = FFmpegKit.execute("-i \""+url+"\" -preset slower -crf 17 -c:a copy -s 720x480 -f flv "+serverurl);
                String done = rc1.toString();
                /*int rc1 = FFmpeg.execute("-codecs");
                int rc = FFmpeg.execute("-i \""+url+"\" -preset slower -crf 17 -c:a copy -s 720x480 -f flv "+serverurl);
                if (rc == RETURN_CODE_SUCCESS) {
                    Log.i(Config.TAG, "Command execution completed successfully.");
                } else if (rc == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
                    Config.printLastCommandOutput(Log.INFO);
                }*/
                //dao.deleteCam(camid);

/*
                        long camid = -1;

                        try {
                            FaceDetector faceDetector;
                            FirebaseVisionImage fbImage;
                            Bitmap bmp = null;
                            FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
                            mmr.setDataSource(url);

                            IPCameras ipcam = new IPCameras(0,title.getText().toString(),ip.getText().toString());

                            camid = dao.insertCam(ipcam);
                            update = true;
                            MediaExtractor mediaExtractor = new MediaExtractor();
                            mediaExtractor.setDataSource(url);

                            mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_AUDIO_CODEC);
                            mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST);
                            faceDetector = new FaceDetector.Builder(getApplicationContext())
                                    .setTrackingEnabled(false)
                                    .setMode(FaceDetector.ACCURATE_MODE)
                                    .build();

                            while(true) {

                                try {


                                    Frame frame = new Frame.Builder().setBitmap(mmr.getFrameAtTime()).build();

                                    // faceDetector.setFocus(frame.getMetadata().getId());
                                    SparseArray face = faceDetector.detect(frame);
                                    if(face.size()>0) {
                                        System.out.println("Detected and Processing");
                                        send(frame.getBitmap());
                                    }else{
                                        System.out.println("Not Detected");
                                    }

                                } catch (Exception ex) {
                                    System.out.println(ex.getMessage());
                                }
                            }


                        }catch (Exception ex){
                            System.out.println(ex.getMessage());
                            dao.deleteCam(camid);
                        }*/

            }
        }).start();

    }

    // [END receive_message]


    // [START on_new_token]

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }
    // [END on_new_token]

    /**
     * Schedule async work using WorkManager.
     */
    private void scheduleJob() {
        // [START dispatch_job]
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(FCMWorker.class)
                .build();
        WorkManager.getInstance().beginWith(work).enqueue();
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(final String token) {
        // TODO: Implement this method to send token to your app server.
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        StringRequest stringRequest=new StringRequest(com.android.volley.Request.Method.POST, BASEURL+"update-devicetoken",
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
                params.put("device_token_doorside",token);
                return params;

            }
        };
        RequestQueue requestQueue= Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);

    }



}
