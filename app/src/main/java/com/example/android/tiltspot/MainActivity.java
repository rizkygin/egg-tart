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
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    private CameraDevice cameraDevice;
    int mAzzimuth;
    TextView azzimuth ;

    Sensor mRotation;

    float[] rMat = new float[9];
    float[] orientation = new float[9];

    private CaptureRequest.Builder  captureRequestBuilder ;
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createCameraPreview() {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        assert texture != null;
        texture.setDefaultBufferSize(600,800);
        Surface surface = new Surface(texture);
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        captureRequestBuilder.addTarget(surface);
    }

    TextView mAl ;

    ImageView mImage;
    private String cameraId;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
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
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
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
    private void openCamera() throws CameraAccessException {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            assert manager != null;
            cameraId = manager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
//
//        CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
//
//        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//
//        assert map != null;
//        imageDimension map.getOutput

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.CAMERA
                }, REQUEST_CAMERA_PERMISSION);
                return;
            }


        manager.openCamera(cameraId,statecallback,null);

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

//TODO camera doesnt render


    }


    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //
    }

}