/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.tiltspot;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    int mAzzimuth;
    TextView azzimuth ;

    Sensor mRotation;

    float[] rMat = new float[9];
    float[] orientation = new float[9];

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private CameraDevice.StateCallback statecallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onError(@NonNull CameraDevice camera, int i) {
            camera.close();
            cameraDevice= null;
        }
    };
    private boolean mLastAccelometerSet = false;

    TextView mAl ;
    private Button btnCapture;

    //check state orientation of output camera
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,180);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,360);
    }


    ImageView mImage;
    //camera


    private CaptureRequest.Builder captureBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder  captureRequestBuilder ;
    private String cameraId;
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            try {
                openCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    //save to file
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private float mBarometer;
    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];
    // System sensor manager instance.
    private SensorManager mSensorManager;

    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetometer;
    private Sensor mSensorBaromoter;
    private Sensor mSensorRotation;

    // TextViews to display current sensor values.
    private TextView mTextSensorAzimuth,mTextSensorPitch;
    private TextView mTextSensorRoll;

    //Texture View

    private TextureView textureView ;

    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lock the orientation to portrait (for now)

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mAl = findViewById(R.id.value_altitude);
        mImage = findViewById(R.id.com);

        azzimuth = findViewById(R.id.degree);
        mTextSensorAzimuth = (TextView) findViewById(R.id.value_azimuth);
        mTextSensorPitch = (TextView) findViewById(R.id.value_pitch);
        mTextSensorRoll = (TextView) findViewById(R.id.value_roll);

        textureView = findViewById(R.id.textureView) ;

        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        btnCapture = findViewById(R.id.capture);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });


        mSensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);
        assert mSensorManager != null;

        mSensorBaromoter = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);


        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void takePicture() {
        if(cameraDevice ==  null){
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try{
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
                android.util.Size[] jpegSizes = null;
                if(characteristics != null){
                    jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.JPEG);
                }
                //capture image with custom Size

                int width = 640;
                int height = 480 ;
                if(jpegSizes != null && jpegSizes.length > 0 ){
                    width = jpegSizes[0].getWidth();
                    height = jpegSizes[0].getHeight();
                }
                final ImageReader reader = ImageReader.newInstance(width,height,ImageFormat.JPEG,1);

                final List<Surface> outputSurfce = new ArrayList<>(2);
                outputSurfce.add(reader.getSurface());
                outputSurfce.add(new Surface(textureView.getSurfaceTexture()));

                final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureBuilder.addTarget(reader.getSurface());
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                //check orientation

                int rotation = getWindowManager().getDefaultDisplay().getOrientation();
                captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));

                file = new File(Environment.getExternalStorageDirectory()+"/" + UUID.randomUUID().toString()+".jpeg");
                ImageReader.OnImageAvailableListener readerListener =  new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader imageReader) {
                        Image image = null;

                        try{
                            image = reader.acquireLatestImage();
                            ByteBuffer buffer =  image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.capacity()];

                            buffer.get(bytes);
                            save(bytes);
                        }
                        catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }
                        finally {
                            if(image!= null){
                                image.close();
                            }
                        }
                    }

                    private void save(byte[] bytes) throws IOException {
                        OutputStream outputStream = null;
                        try{
                            outputStream = new FileOutputStream(file);
                            outputStream.write(bytes);
                        }finally {
                            if ( outputStream != null) {
                                outputStream.close();
                            }
                        }
                    }
                };
                reader.setOnImageAvailableListener(readerListener,mBackgroundHandler);
                final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        Toast.makeText(MainActivity.this, "Saved" + file, Toast.LENGTH_SHORT).show();
                    }
                };
                cameraDevice.createCaptureSession(outputSurfce, new CameraCaptureSession.StateCallback(){


                    @Override
                    public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                        try {
                            cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgroundHandler );
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                    }
                },mBackgroundHandler);


            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createCameraPreview() {
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
            Surface surface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void updatePreview() {
        if(cameraDevice == null){
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        }try{
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),null,mBackgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void  openCamera() throws CameraAccessException {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            assert manager != null;
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics =  manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            //check realtime permission if run higest API 23
            if(ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },REQUEST_CAMERA_PERMISSION);
                return;
            }

            manager.openCamera(cameraId,statecallback,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }



    }

    @Override
    protected void onStart() {
        super.onStart();

        // Listeners for the sensors are registered in this callback and
        // can be unregistered in onStop().
        //
        // Check to ensure sensors are available before registering listeners.
        // Both listeners are registered with a "normal" amount of delay
        // (SENSOR_DELAY_NORMAL).
        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensorBaromoter != null ){
            mSensorManager.registerListener(this,mSensorBaromoter,SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorMagnetometer != null) {
            mSensorManager.registerListener(this, mSensorMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mRotation != null){
            mSensorManager.registerListener(this,mSensorRotation,mSensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
//        mBarometer = sensorEvent.values[0];
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerData = sensorEvent.values.clone();
                mLastAccelometerSet = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetometerData = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_PRESSURE:
                mBarometer = sensorEvent.values[0];
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                mAzzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat,orientation)[0] + 360 ) %360);

                mAzzimuth = Math.round(mAzzimuth);
                mImage.setRotation(-mAzzimuth);
                break;
            default:
            return;
        }

        float altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE,mBarometer);
        float[] rotationMatrix = new float[9];
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                null, mAccelerometerData, mMagnetometerData);

        float orientationValues[] = new float[3];
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrix, orientationValues);
        }


        float azimuth = orientationValues[0] ;
        float pitch = orientationValues[1];
        float roll = orientationValues[2];


        String where = "No";

        if(mAzzimuth >=  350 || mAzzimuth <= 10){
            where = "N";
        }if(mAzzimuth >=  350 || mAzzimuth > 280){
            where = "NW";
        }if(mAzzimuth >=  280 || mAzzimuth > 260){
            where = "W";
        }if(mAzzimuth >=  260 || mAzzimuth > 190){
            where = "SW";
        }if(mAzzimuth >=  190 || mAzzimuth > 170){
            where = "S";
        }if(mAzzimuth >=  170 || mAzzimuth > 100){
            where = "SE";
        }if(mAzzimuth >=  100 || mAzzimuth >80 ){
            where = "E";
        }if(mAzzimuth >=  80 || mAzzimuth > 10){
            where = "NE";
        }
        azzimuth.setText(mAzzimuth + " ° " + where);

        mAl.setText(getResources().getString(
                R.string.value_format, altitude));

        mTextSensorAzimuth.setText(getResources().getString(
                R.string.value_format, azimuth));
        mTextSensorPitch.setText(getResources().getString(
                R.string.value_format, pitch ));
        mTextSensorRoll.setText(getResources().getString(R.string.value_format, roll));



    }


    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if(textureView.isAvailable()){
            try {
                openCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }else{
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread =  new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler =  new Handler(mBackgroundThread.getLooper());
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }
    private void stopBackgroundThread() {
            mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler =null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "You Cant use Camera Without Permission on it", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }
}