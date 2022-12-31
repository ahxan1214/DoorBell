package com.irveni.doorbell.functions;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.irveni.doorbell.FaceDetection.RealTimeFaceDetectionActivity;
import com.irveni.doorbell.R;
import com.irveni.doorbell.ui.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Common {

    public static String BASEURL = "https://beta.irvinei.com/api/api/";
    public static String BASEURL_Image = "https://beta.irvinei.com/api/";
    static Handler handler;
    static Runnable timeRunnable;
    public static boolean requestWait=false;
    public static SharedPreferences prefs;
    public static String python_server_ip = "20.227.167.170";
    //public static String python_server_ip = "18.221.116.244";
    public static String notificationid = "";
    public static Activity activeactivity = null;
    public static String temperature = "";
    public static String address = "";
    public static String maintext = "";
    public static String helptext = "";
    public static Thread background ;
    public static Socket mSocket;
    public static boolean bellpressed = false;
    public static boolean gesture = false;
    private MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
    private static MediaPlayer mPlayer;

    public static void playsound(Activity context,int id){

        //MediaPlayer mPlayer ;
        //mPlayer = MediaPlayer.create(context, Uri.parse("http://beta.irvinei.com/api/uploads/dingdongbell.wav"));
        
        mPlayer = MediaPlayer.create(context, id);
        //mPlayer.setLooping(true);
        ///mPlayer.setVolume(100f,100f);
        mPlayer.setAudioAttributes(new AudioAttributes
                        .Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());

        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });

        try {

            if(mPlayer.isPlaying()){
                mPlayer.reset();
                mPlayer.prepare();
                mPlayer.start();
            }else{
                mPlayer.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


//        mPlayer.setVolume(100f,100f);

    }
    public static void playsound(String url){

    }

    public static File create_file(Activity activity,byte[] bitmapdata){

        String tt = activity.getExternalFilesDir("images/").getPath();

        String imageFolderPath = tt + "/";
        File imagesFolder = new File(imageFolderPath);
        imagesFolder.mkdirs();

        // Generating file name
        String imageName = "visitor" + new Random().nextInt(10000000) + ".png";

        File filee = new File(imageFolderPath, imageName);
        if (!filee.exists()) {
            try {
                filee.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filee);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        if (bitmapdata != null) {
            try {
                fos.write(bitmapdata);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return filee;



    }


}
