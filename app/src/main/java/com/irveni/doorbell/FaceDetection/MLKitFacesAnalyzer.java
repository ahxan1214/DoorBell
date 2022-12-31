package com.irveni.doorbell.FaceDetection;


import static com.irveni.doorbell.functions.Common.bellpressed;
import static com.irveni.doorbell.functions.Common.gesture;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.irveni.doorbell.functions.Functions;
import com.irveni.doorbell.ui.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;


public class MLKitFacesAnalyzer implements ImageAnalysis.Analyzer {
    private static final String TAG = "MLKitFacesAnalyzer";
    private FirebaseVisionFaceDetector faceDetector;
    private PreviewView tv;
    private ImageView iv;
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint dotPaint, linePaint;
    private float widthScaleFactor = 1.0f;
    private float heightScaleFactor = 1.0f;
    private FirebaseVisionImage fbImage;
    private int lens = CameraSelector.LENS_FACING_FRONT;
    Context context;
    String imageFolderPath = "";
    private String imageName;

    ImageAnalysis imageAnalysis;
    public MLKitFacesAnalyzer(Context context,/*, PreviewView tv, ImageView iv, CameraX.LensFacing lens*/ImageAnalysis imageAnalysis) {
        //this.tv = tv;
        /*


        this.iv = iv;
        this.lens = lens;*/
        this.context  = context;
        this.imageAnalysis = imageAnalysis;
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void analyze(ImageProxy image) {
        if (image == null || image.getImage() == null || !bellpressed) {
            image.close();
            return;
        }
        int rotation = degreesToFirebaseRotation(image.getImageInfo().getRotationDegrees());
        fbImage = FirebaseVisionImage.fromMediaImage(image.getImage(), rotation);
        //initDrawingUtils();

        Bitmap bitmap = fbImage.getBitmap();
        initDetector();
        detectFaces();
        image.close();
    }

    private void initDrawingUtils() {
        bitmap = Bitmap.createBitmap(tv.getWidth(), tv.getHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        dotPaint = new Paint();
        dotPaint.setColor(Color.RED);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setStrokeWidth(2f);
        dotPaint.setAntiAlias(true);
        linePaint = new Paint();
        linePaint.setColor(Color.GREEN);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2f);
        widthScaleFactor = canvas.getWidth() / (fbImage.getBitmap().getWidth() * 1.0f);
        heightScaleFactor = canvas.getHeight() / (fbImage.getBitmap().getHeight() * 1.0f);
    }

    private void initDetector() {
        FirebaseVisionFaceDetectorOptions detectorOptions = new FirebaseVisionFaceDetectorOptions
                .Builder()
                //.setContourMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.0000001f)
                .enableTracking()
                .build();
        faceDetector = FirebaseVision
                .getInstance()
                .getVisionFaceDetector(detectorOptions);
    }

    public static Bitmap cropBitmap(Bitmap bitmap, Rect rect) {
        int w = rect.right - rect.left;
        int h = rect.bottom - rect.top;
        Bitmap ret = Bitmap.createBitmap(w+100, h+150, bitmap.getConfig());
        Canvas canvas = new Canvas(ret);
        canvas.drawBitmap(bitmap, -rect.left, -rect.top+50, null);
        return ret;
    }

    int count = 1;
    private void detectFaces() {
        faceDetector.detectInImage(fbImage)
                .addOnSuccessListener(firebaseVisionFaces -> {
                    if (!firebaseVisionFaces.isEmpty()) {
                        //processFaces(firebaseVisionFaces);
                        Bitmap m = fbImage.getBitmap();
                        //iv.setImageBitmap(firebaseVisionFaces.get(0));
                        if(firebaseVisionFaces.size()>0) {
                            Rect land = firebaseVisionFaces.get(0).getBoundingBox();

                            int x =land.centerX()-100;
                            int y =land.centerY()-100;
                            Bitmap croped = cropBitmap(m,land);
//                            iv.setImageBitmap(croped);
                            if(count==1){
                                count++;
                                send(m);
                            }

                        }

                    } else {
                        //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
                    }
                }).addOnFailureListener(e -> Log.i(TAG, e.toString()));
    }


    private void processFaces(List<FirebaseVisionFace> faces) {
        for (FirebaseVisionFace face : faces) {
            drawContours(face.getContour(FirebaseVisionFaceContour.FACE).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_BOTTOM).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_BOTTOM).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.LEFT_EYE).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.RIGHT_EYE).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.LEFT_EYEBROW_TOP).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.RIGHT_EYEBROW_TOP).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.LOWER_LIP_BOTTOM).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.LOWER_LIP_TOP).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.UPPER_LIP_TOP).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.NOSE_BRIDGE).getPoints());
            drawContours(face.getContour(FirebaseVisionFaceContour.NOSE_BOTTOM).getPoints());
        }
        iv.setImageBitmap(bitmap);
    }

    private void drawContours(List<FirebaseVisionPoint> points) {
        int counter = 0;
        for (FirebaseVisionPoint point : points) {
            if (counter != points.size() - 1) {
                canvas.drawLine(translateX(point.getX()),
                        translateY(point.getY()),
                        translateX(points.get(counter + 1).getX()),
                        translateY(points.get(counter + 1).getY()),
                        linePaint);
            } else {
                canvas.drawLine(translateX(point.getX()),
                        translateY(point.getY()),
                        translateX(points.get(0).getX()),
                        translateY(points.get(0).getY()),
                        linePaint);
            }
            counter++;
            canvas.drawCircle(translateX(point.getX()), translateY(point.getY()), 6, dotPaint);
        }
    }

    private float translateY(float y) {
        return y * heightScaleFactor;
    }

    private float translateX(float x) {
        float scaledX = x * widthScaleFactor;
        if (lens == CameraSelector.LENS_FACING_FRONT) {
            return canvas.getWidth() - scaledX;
        } else {
            return scaledX;
        }
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
    private void send(Bitmap bitmap_){

        /*imageAnalysis.setAnalyzer(new Executor() {
            @Override
            public void execute(Runnable runnable) {

            }
        }, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {

            }
        });
*/


        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in airplane mode it will be null
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        int downSpeed = nc.getLinkDownstreamBandwidthKbps();
        int upSpeed = nc.getLinkUpstreamBandwidthKbps();

        int quality = 100;

        if(downSpeed > 10000){
            quality = 80;
        }else if(downSpeed > 8000 && downSpeed < 10000){
            quality = 60;
        }



        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap_.compress(Bitmap.CompressFormat.JPEG, quality /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();
        //write the bytes in file
        try {
                                        /*Intent intent = new Intent();
                                        intent.putExtra("image", bitmapdata);
                                        setResult(RESULT_OK, intent);*/

            String tt = context.getExternalFilesDir("images/").getPath();

            imageFolderPath = tt + "/"/*Environment.getExternalStorageDirectory().toString()
                + "/DoorBell/"*/;
            File imagesFolder = new File(imageFolderPath);
            imagesFolder.mkdirs();

            // Generating file name
            imageName = "visitor" + new Random().nextInt(10000000) + ".png";

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


                if (bitmapdata != null)
                    fos.write(bitmapdata);

                //test_notifier();
                try {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Functions.uploadImage(context,imageFolderPath + imageName);
                                count = 1;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();


                } catch (Exception e) {
                    e.printStackTrace();
                    //          //Toast.makeText(Main2Activity.this,e.getMessage(),//Toast.LENGTH_LONG).show();
                    /*context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(Main2Activity.this, e.getMessage(),
                            //Toast.LENGTH_SHORT).show();
                        }
                    });*/
                }



            } catch (FileNotFoundException e) {
                System.out.println("File not found" + e);
            } catch (IOException ioe) {
                System.out.println("Exception while writing file " + ioe);
            } catch (Exception ex) {

                // Toast.makeText(FaceDetectRGBActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                System.out.println(ex.getMessage());

            }
            //Intent intent = new Intent();
            //intent.putExtra("value",1);
            //context.setResult(1,intent);
            //context.finish();
        } catch (Exception ex) {
            /*context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });*/
        }
    }
}
