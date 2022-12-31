package com.irveni.doorbell.FaceDetection;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
//import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
//import androidx.camera.core.PreviewConfig;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.irveni.doorbell.R;

import java.util.concurrent.ExecutionException;


public class RealTimeFaceDetectionActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_PERMISSION = 101;
    public static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private PreviewView tv;
    private ImageView iv;
    private static final String TAG = "RealTimeFaceDetectionActivity";

    //public static CameraX.LensFacing lens = CameraX.LensFacing.FRONT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_face_detection);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        tv = findViewById(R.id.face_texture_view);
        iv = findViewById(R.id.face_image_view);
        if (allPermissionsGranted()) {
            tv.post(this::startCamera);
      //      moveTaskToBack(true);
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION);
        }


    }

    @SuppressLint({"RestrictedApi", "LongLogTag"})
    private void startCamera() {
        initCamera();
        ImageButton ibSwitch = findViewById(R.id.btn_switch_face);
       /* ibSwitch.setOnClickListener(v -> {
            if (lens == CameraX.LensFacing.FRONT)
                lens = CameraX.LensFacing.BACK;
            else
                lens = CameraX.LensFacing.FRONT;
            try {
                Log.i(TAG, "" + lens);
                CameraX.getCameraWithLensFacing(lens);
                initCamera();
            } catch (CameraInfoUnavailableException e) {
                Log.e(TAG, e.toString());
            }
        });*/
    }

    private void initCamera() {
/*        CameraX.unbindAll();
        PreviewConfig pc = new PreviewConfig
                .Builder
                .setTargetResolution(new Size(tv.getWidth(), tv.getHeight()))
                .setLensFacing(lens)
                .build();

        Preview preview = new Preview(pc);
        preview.setOnPreviewOutputUpdateListener(output -> {
            ViewGroup vg = (ViewGroup) tv.getParent();
            vg.removeView(tv);

            vg.addView(tv, 0);
            tv.setSurfaceTexture(output.getSurfaceTexture());
        });



        ImageAnalysisConfig iac = new ImageAnalysisConfig
                .Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                //.setTargetResolution(new Size(getWindow().getAttributes().width,getWindow().getAttributes().height))

                //.setTargetResolution(new Size(1280, 720))
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)

                .setLensFacing(lens)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(iac);
        imageAnalysis.setAnalyzer(Runnable::run,new MLKitFacesAnalyzer(this,tv*//*, iv, lens*//*));
        CameraX.bindToLifecycle(this, imageAnalysis,preview);*/


        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();


                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                @SuppressLint("RestrictedApi")
                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                // enable the following line if RGBA output is needed.
                                //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                //.setTargetResolution(new Size(2000, 1500))
                                //.setDefaultResolution(new Size(1920, 1440))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                                .build();


//                imageAnalysis.setAnalyzer(Runnable::run,new MLKitFacesAnalyzer(this,tv));

                cameraProvider.unbindAll();

                        // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle(
                        ((LifecycleOwner) this),
                        cameraSelector,
                        preview,
                        imageAnalysis);

                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(tv.getSurfaceProvider());
            } catch (InterruptedException | ExecutionException e) {
                // Currently no exceptions thrown. cameraProviderFuture.get()
                // shouldn't block since the listener is being called, so no need to
                // handle InterruptedException.
            }
        }, ContextCompat.getMainExecutor(this));



    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                tv.post(this::startCamera);
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}