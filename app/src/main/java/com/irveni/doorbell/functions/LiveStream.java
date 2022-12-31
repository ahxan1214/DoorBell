package com.irveni.doorbell.functions;

import static android.content.ContentValues.TAG;

import static com.irveni.doorbell.functions.Common.bellpressed;
import static com.irveni.doorbell.functions.Common.create_file;
import static com.irveni.doorbell.functions.Common.gesture;
import static com.irveni.doorbell.functions.Common.handler;
import static com.irveni.doorbell.functions.Common.mSocket;
import static com.irveni.doorbell.functions.Common.prefs;
import static com.irveni.doorbell.functions.Common.requestWait;
import static com.irveni.doorbell.functions.Common.timeRunnable;
import static com.irveni.doorbell.functions.Functions.MEDIA_TYPE_PNG;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.irveni.doorbell.FaceDetection.RealTimeFaceDetectionActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LiveStream {



    public LiveStream(Activity activity) {
        setup(activity);
    }

    void setup(Activity activity){

        try {
            //mSocket = IO.socket("http://"+ipaddress.getText().toString()+":8000");
            mSocket = IO.socket("http://20.227.167.170:8000");
            //mSocket = IO.socket("http://192.168.100.155:8000");
            mSocket.connect();
            handler = new Handler();

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

            mSocket.on("gesture", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    bellpressed = true;


                }
            });
            mSocket.on("message", new Emitter.Listener() {
                @Override
                public void call(Object... args) {


                /*    if(requestWait){
                        //requestWait = true;
                        return;
                    }
                    //handler.postDelayed(timeRunnable,15000);
                    requestWait = true;*/

                    int id = prefs.getInt("userid", -1);
                    OkHttpClient client = new OkHttpClient();
                    OkHttpClient egarClient = client.newBuilder().readTimeout(0, TimeUnit.SECONDS).build();

                    JSONObject json = (JSONObject) args[0];
                    String message = "";
                    File image = new File("");
                    try {
                        message = json.getString("result");
                        byte[] decodedString = (byte[]) json.get("image");

                        //byte[] decodedString = (byte[]) args[0]; //Base64.decode((String) args[0], Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0,decodedString.length);
                        image = create_file(activity,decodedString);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //MEDIA_TYPE_PNG = "";
                    RequestBody requestBody1 = new MultipartBody
                            .Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("image", image.getName(), RequestBody.create(MEDIA_TYPE_PNG, image))
                            .addFormDataPart("userid", id + "")
                            .addFormDataPart("message", message)
                            .build();

                    Request request1 = new Request
                            .Builder()
                            .url(Common.BASEURL+"send-notification")
                            .post(requestBody1)
                            .build();

                    try {
                        Response response = egarClient.newCall(request1).execute();

                        String something = new String(response.body().bytes());

                        System.out.println(something);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
        } catch (URISyntaxException e) {


        }
    }



}
