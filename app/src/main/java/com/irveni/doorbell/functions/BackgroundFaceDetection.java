package com.irveni.doorbell.functions;

import static com.irveni.doorbell.functions.Common.activeactivity;
import static com.irveni.doorbell.functions.Common.mSocket;
import static com.irveni.doorbell.functions.Common.prefs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleService;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.irveni.doorbell.FaceDetection.MLKitFacesAnalyzer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class BackgroundFaceDetection extends LifecycleService  {





    Context context;
    private FirebaseVisionImage fbImage;
    int userid;

    Executor executor;


    /*public BackgroundFaceDetection(Activity context,Executor executor) {
        this.context = context;
        this.executor = executor;

    }*/

    ProcessCameraProvider cameraProvider;

    @SuppressLint("UnsafeOptInUsageError")
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void initCamera() {

        //context = getApplicationContext();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        userid = prefs.getInt("userid",-1);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = null;
                cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                //@SuppressLint("RestrictedApi")
                ImageAnalysis.Builder imageAnalysisBuilder =
                        new ImageAnalysis.Builder()
                                // enable the following line if RGBA output is needed.
                                //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                //.setTargetResolution(new Size(2000, 1500))
                                //.setDefaultResolution(new Size(1920, 1440))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        //.setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        ;
                    /*Camera2Interop.Extender ext = new Camera2Interop.Extender<>(imageAnalysisBuilder);
                    ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST);
                    ext.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<Integer>(60, 60));*/
                ImageAnalysis imageAnalysis = imageAnalysisBuilder.build();

                imageAnalysis.setAnalyzer(Runnable::run, new ImageAnalysis.Analyzer() {
                    @SuppressLint("UnsafeOptInUsageError")
                    @Override
                    public void analyze(@NonNull ImageProxy image) {

                        int rotate = degreesToFirebaseRotation(image.getImageInfo().getRotationDegrees());
                        fbImage = FirebaseVisionImage.fromMediaImage(image.getImage(), rotate);
                        Bitmap bmp = fbImage.getBitmap();
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();



                        if (mSocket != null) {
                            if (!mSocket.connected())
                                mSocket.connect();


                            JSONObject push = new JSONObject();

                            try {
                                push.put("userid", userid + "");
                                push.put("image", byteArray);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mSocket.emit("display", push);
                            mSocket.emit("message", push);
                        }
                        image.close();
                    }

                });

                @SuppressLint("RestrictedApi")
                ImageAnalysis imageAnalysis1 =
                        new ImageAnalysis.Builder()
                                // enable the following line if RGBA output is needed.
                                //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                //.setTargetResolution(new Size(2000, 1500))
                                //.setDefaultResolution(new Size(1920, 1440))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                                .build();


                imageAnalysis1.setAnalyzer(Runnable::run,new MLKitFacesAnalyzer(this,imageAnalysis1));


                try {
                    // Attach use cases to the camera with the same lifecycle owner
                    Camera camera = cameraProvider.bindToLifecycle(
                            (this),
                            cameraSelector,
                            imageAnalysis,
                            imageAnalysis1
                    );
                    camera.getCameraInfo();
                }catch (Exception ex){
                    System.out.println("Resource Captured");
                }


                //CameraInfo cameraInfo = camera.getCameraInfo();

                CameraXConfig camconfig = Camera2Config.defaultConfig();

            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get()
                // shouldn't block since the listener is being called, so no need to
                // handle InterruptedException.
            }
        }, this.getMainExecutor());


      /*  imageAnalysis.setAnalyzer(Runnable::run, new ImageAnalysis.Analyzer() {
            @SuppressLint("UnsafeOptInUsageError")
            @Override
            public void analyze(ImageProxy image) {
                if (image == null) {
                    return;
                }


                int rotate = degreesToFirebaseRotation(image.getImageInfo().getRotationDegrees());

                fbImage = FirebaseVisionImage.fromMediaImage(image.getImage(),rotate);

                Bitmap bmp = fbImage.getBitmap();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();

                if(mSocket != null)
                {
                    if(!mSocket.connected())
                        mSocket.connect();


                    JSONObject push = new JSONObject();

                    try {
                        push.put("userid",userid+"");
                        push.put("image",byteArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    mSocket.emit("display",byteArray);
                    mSocket.emit("message",push);
                }

            }
        });

        CameraX.bindToLifecycle((LifecycleOwner) context, (UseCase) imageAnalysis);*/


    }

    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException("Rotation must be 0, 90, 180, or 270.");
        }
    }

    private byte[] toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        int m = image.getFormat();

        //YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, 640, 480, null);
        YuvImage yuvimage = new YuvImage(nv21, ImageFormat.NV21,
                image.getWidth(),
                image.getHeight()
                ,null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0,0,yuvimage.getWidth(), yuvimage.getHeight()), 80, baos);

        Bitmap bmp = rotateBitmap(yuvimage,-90,new Rect(0,0,640,480));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        //byteArray = byteArrayOutputStream .toByteArray();

        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return imageBytes;
    }

    private Bitmap rotateBitmap(YuvImage yuvImage, int orientation, Rect rectangle){
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(rectangle, 100, os);

        Matrix matrix = new Matrix();
        matrix.postRotate(orientation);
        byte[] bytes = os.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0 , 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        return  Bitmap.createScaledBitmap(bmp, 640,
                480, false);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //userid = prefs.getInt("userid",-1);
        super.onStartCommand(intent, flags, startId);
        //Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            initCamera();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();



    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}
