package com.irveni.doorbell.functions;

import static com.irveni.doorbell.functions.Common.*;
import static com.irveni.doorbell.functions.Common.BASEURL;
import static com.irveni.doorbell.functions.Common.python_server_ip;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.irveni.doorbell.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Functions {


    public static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

    public static int uploadImage(Context context,String imageName) throws IOException {

        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        bellpressed = false;
        int id = prefs.getInt("userid", -1);
        String device = prefs.getString("device_name", "");
        //File image = img.decodeFile(imageName);
        File image = new File(imageName);

        System.out.println(image.getPath());

        OkHttpClient client = new OkHttpClient();
        OkHttpClient egarClient = client.newBuilder().readTimeout(0, TimeUnit.SECONDS).build();

        OkHttpClient.Builder build = client.newBuilder();
        build.connectTimeout(0, TimeUnit.SECONDS);

        RequestBody requestBody1 = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("userid", id+"")
                .addFormDataPart("file", imageName, RequestBody.create(MEDIA_TYPE_PNG, image))
                .build();

        String url = "http://"+python_server_ip+":1000/uploader";
        Request request1 = new Request
                .Builder()
                .url(url)
                .post(requestBody1)
                .build();

        try {

            System.out.println( new Date());
            System.out.println("Sending Server Start Time");

            Response response = egarClient.newCall(request1).execute();


            System.out.println( new Date());
            System.out.println("Sending Server End Time");

            Response finalResponse = response;

            if (!response.isSuccessful()) {
                //Main2Activity.progress.dismiss();
                File temp = new File(imageName);
                if(temp.exists()){
                    temp.delete();
                }
                prefs.edit().putString("imagedetected","no").commit();

                return 1;
                //throw new IOException("Unexpected code " + response);
            } else {

                String something = new String(response.body().bytes());

                try {

                    JSONObject obj = new JSONObject(something);

                    /*if (obj.has("person_name")) {

                        System.out.println( new Date());
                        System.out.println("Sending notifcation Start Time");
                        requestBody1 = new MultipartBody
                                .Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("image", imageName, RequestBody.create(MEDIA_TYPE_PNG, image))
                                .addFormDataPart("userid", id + "")
                                .addFormDataPart("message", obj.getString("person_name")+" is on door")
                                .addFormDataPart("device_name", device)
                                .build();

                        request1 = new Request
                                .Builder()
                                .url(BASEURL+"send-notification")
                                .post(requestBody1)
                                .build();



                    }
                    else {

                        System.out.println( new Date());
                        System.out.println("Sending notifcation Start Time");

                        requestBody1 = new MultipartBody
                                .Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("image", imageName, RequestBody.create(MEDIA_TYPE_PNG, image))
                                .addFormDataPart("userid", id + "")
                                .addFormDataPart("message", obj.getString("error")+" is on door")
                                .build();

                        request1 = new Request
                                .Builder()
                                .url(BASEURL+"send-notification")
                                .post(requestBody1)
                                .build();


                    }
                    response = egarClient.newCall(request1).execute();
                    something = new String(response.body().bytes());*/

                    notificationid = obj.getString("data");
                    System.out.println( new Date());
                    System.out.println("Sending notifcation End Time");

                    //Main2Activity.progress.dismiss();
                    /*context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //MainPage.progress.dismiss();
                            File temp = new File(imageName);
                           *//* if(temp.exists()){
                                 temp.delete();
                            }*//*
                            prefs.edit().putString("imagedetected","no").commit();

                        }
                    });*/
                    return 1;
                } catch (Exception ex) {

                    System.out.println(ex);

                }

                System.out.println(something);

            }
        }catch (SocketTimeoutException ex){

            return uploadImage(context,imageName);

        }



        return 0;

    }


    public static void sendresponse(Context context, String command, JSONArray message) throws IOException {

        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int id = prefs.getInt("userid", -1);

        OkHttpClient client = new OkHttpClient();
        OkHttpClient egarClient = client.newBuilder().readTimeout(10, TimeUnit.SECONDS).build();


        RequestBody requestBody1 = new MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("userid", id + "")
                .addFormDataPart("command", command)
                .addFormDataPart("message", message.toString())
                .build();

        Request request1  = new Request
                .Builder()
                .url(BASEURL+"send-notification-1")
                .post(requestBody1)
                .build();
        Response response = egarClient.newCall(request1).execute();
        String something = new String(response.body().bytes());

        System.out.println(something);

    }


    static double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    public static String get_weather(Activity context) {


        GPSTracker gpsTracker = new GPSTracker(context);

        if(temperature.isEmpty()){
            //prefs.edit().putString("latitude", gpsTracker.latitude+"");
            //prefs.edit().putString("longitude",gpsTracker.longitude+"");
            //prefs.edit().commit();
        }else{


        }



        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://weatherapi-com.p.rapidapi.com/current.json?q=" + gpsTracker.latitude + "," + gpsTracker.longitude + "&dt=2022-12-25")
                .get()
                .addHeader("X-RapidAPI-Key", "77944d9c22msh9fc7d63c177ed95p128c1bjsnb29f87027eab")
                .addHeader("X-RapidAPI-Host", "weatherapi-com.p.rapidapi.com")
                .build();

        try {

            Response response = client.newCall(request).execute();

            String resp = new String(response.body().bytes());

            JSONObject jsonObject = new JSONObject(resp);

            JSONObject current = jsonObject.getJSONObject("current");

            return current.getString("temp_c");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return "";
    }



    public static void get_user_address(Activity context, TextView mainheading, TextView address, TextView helpingcontainer) {
        // TODO: Implement this method to send token to your app server.

        prefs = PreferenceManager.getDefaultSharedPreferences(context);


        StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.POST, BASEURL + "get-address?rand="+new Random().nextInt(3000),
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject jsonObject = new JSONObject(response);

                            System.out.println(response);
                            Common.address = jsonObject.getString("address");
                            helptext = jsonObject.getString("custom_message");
                            maintext = jsonObject.getString("heading");

                            if(maintext.equals("address")){
                                mainheading.setText(Common.address);
                                mainheading.setTextSize(TypedValue.COMPLEX_UNIT_SP,30);
                                if(!temperature.isEmpty()){
                                    address.setText(temperature +"\u00B0");
                                    address.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
                                }
                                helpingcontainer.setText(helptext);

                            }else if(maintext.equals("family_name")){
                                mainheading.setText("Welcome to Smith Familty");
                                mainheading.setTextSize(TypedValue.COMPLEX_UNIT_SP,30);
                                if(!temperature.isEmpty()){
                                    address.setText(temperature +"\u00B0");
                                    address.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
                                }
                                helpingcontainer.setText(helptext);

                            }else if(maintext.equals("custom_message")){
                                mainheading.setText(helptext);
                                mainheading.setTextSize(TypedValue.COMPLEX_UNIT_SP,30);
                                if(!temperature.isEmpty()){
                                    address.setText(temperature +"\u00B0");
                                    address.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
                                }
                                helpingcontainer.setText(helptext);

                            }else{
                                mainheading.setText(temperature);
                                address.setText(Common.address);
                                helpingcontainer.setText(helptext);
                            }



                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //////Toast.makeText(getApplicationContext(), "Error Volley" + error, //Toast.LENGTH_LONG).show();

                        /*sweetAlertDialog = new SweetAlertDialog(MainPage.this, SweetAlertDialog.ERROR_TYPE);
                        sweetAlertDialog.setTitleText("Error");
                        sweetAlertDialog.setContentText("Cannot Reach Server");
                        sweetAlertDialog.show();*/

                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String, String> params = new HashMap<>();
                int id = prefs.getInt("userid", -1);
                String s = prefs.getString("device_name","");
                params.put("userid", id + "");
                params.put("device_name",s);

                return params;

            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(stringRequest);



    }


    public static void getBackground(Activity context){


        VideoView videoLayout = context.findViewById(R.id.videoView);
        ImageView imageView = (ImageView) context.findViewById(R.id.main);



        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int id = prefs.getInt("userid", -1);

        StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.GET,
                BASEURL+"preview-image/"+id+"?rand="+new Random().nextInt(1000),
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject obj = new JSONObject(response);

                            String image = obj.getString("image");
                            String textcolor = obj.getString("color");


                            String extension = image.substring(image.lastIndexOf("."));

                            if(extension.contains(".mp4") || extension.contains(".3gp")) {
                                Uri uri=Uri.parse(image);
                                //                              try {


                                MediaPlayer m = new MediaPlayer();
                                videoLayout.setVideoURI(uri);

                                videoLayout.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    @Override
                                    public void onPrepared(MediaPlayer mp) {

                                        context.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                imageView.setVisibility(View.GONE);
                                                videoLayout.setVisibility(View.VISIBLE);
                                            }
                                        });

                                        mp.start();
                                        videoLayout.start();
                                    }
                                });
                                videoLayout.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {

                                        context.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                imageView.setVisibility(View.GONE);
                                                videoLayout.setVisibility(View.VISIBLE);
                                            }
                                        });
                                        mp.start();
                                        videoLayout.start();
                                    }
                                });

                                videoLayout.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                                    @Override
                                    public boolean onError(MediaPlayer mp, int what, int extra) {
                                        context.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(context,"Error Playing Video",Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                        return false;
                                    }
                                });
                                imageView.setVisibility(View.GONE);
                                videoLayout.setVisibility(View.VISIBLE);

                            }
                            else
                                Glide.with(context).load(image).into(imageView);

                        } catch (Exception e) {
                            e.printStackTrace();
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Glide.with(context).asGif().load(R.drawable.waterfall).into(imageView);
                                    }catch (Exception ex){

                                    }
                                }
                            });

                        }

                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {


                    }
                });

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(stringRequest);

    }





}
