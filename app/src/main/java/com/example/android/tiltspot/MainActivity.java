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
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity
        implements SensorEventListener, SettingDialog.SettingDialogListener {

    private float mAzzimuth;


//    private float[] rMat = new float[9];
//    private float[] orientation = new float[9];

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

    TextView mAl;

    //camera
    private Size imageDimension;
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
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private float mBarometer;
    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];
    // System sensor manager instance.
    private SensorManager mSensorManager;

    //For knowing the vector by using compass
    ImageView mImage;
    TextView azziText;
    Sensor accelometer,magnetometer;
    //compas 2
    private float[] mGravity  = new float[3];
    private float[] mGeomagnetic =  new float[3];


    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    double pitch;
    private float[] mRotation = null;
    float[] rotationMatrix = new float[9];
    private Sensor mSensorAccelerometer;
    private Sensor mSensorMagnetometer;
    private Sensor mSensorBaromoter;
    private Sensor mSensorRotationV;

    // TextViews to display current sensor values.
    private TextView mTextSensorPitch;

    //Texture View

    private TextureView textureView ;

    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;

    //location
    TextView accuracy,mEL,mSL;
    private TextView longitude,latitude;
    private double utmLatitude;
    private double utmLongitude;

    FusedLocationProviderClient mFusedLocationClient;
    Location mLastLocation;
    Integer REQUEST_LOCATION_PERMISSION = 2;

    //count
    private TextView height;
    private Button dist;
    private Button get_location;
    private Button bHeight;
    private double distance;

    private double realHeight;
    private double degree;
    //display height and Display Distance
    private TextView mDisplayD , mDisplayH, mtX,mtY;
    private double azzimuthformeasurement;
    private double pitchHeight;
    private boolean clicked = false;
    private float[] I =  new float [9];
    private float[] K =  new float [9];
    private float[] rMat =  new float[16];
    private float[] orientation =  new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lock the orientation to portrait (for now)

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//        mAl = findViewById(R.id.value_altitude);

        azziText = findViewById(R.id.degree);
        mTextSensorPitch = (TextView) findViewById(R.id.value_pitch);

        //location
        mSL = findViewById(R.id.value_SL);
        mEL = findViewById(R.id.value_EL);
        longitude = findViewById(R.id.value_longitude);
        latitude = findViewById(R.id.value_latitude);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        accuracy = findViewById(R.id.Accuracy);
        getLocation();

        textureView = findViewById(R.id.textureView);


        //count distance
        height = findViewById(R.id.height);
        dist = findViewById(R.id.distance);
        get_location = findViewById(R.id.calculate);
        bHeight = findViewById(R.id.getHeight);
        mDisplayD = findViewById(R.id.displayDistance);
        mDisplayH = findViewById(R.id.displayHeight);
        final String  h = height.getText().toString();
        // get coordinat object
        mtX = findViewById(R.id.mX);
        mtY = findViewById(R.id.mY);
        //        final double  derajat =  Math.toDegrees(degree);
        dist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                get_location.setVisibility(View.VISIBLE);
                mDisplayD.setVisibility(View.VISIBLE);

                clicked = true;
                distance =  Float.parseFloat(height.getText().toString()) * Math.tan(pitch);
//                tempPitch = pitch;
                if(distance < 0 ){
                    distance = -distance;
                }
                Toast.makeText(MainActivity.this, "tan " + pitch, Toast.LENGTH_SHORT).show();

                mDisplayD.setVisibility(View.VISIBLE);
                mDisplayD.setText(getResources().getString(R.string.value_format,distance)+ " M");
                dist.setVisibility(View.INVISIBLE);
            }
        });
        //count convert UTM

            //Dx and Dy

        get_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                Toast.makeText(MainActivity.this, "Nilai Azzimuth" + azzimuthformeasurement, Toast.LENGTH_LONG).show();

                final double mDx = utmLongitude+(distance * Math.sin(azzimuthformeasurement));
                final double mDy = utmLatitude+(distance * Math.cos(azzimuthformeasurement));

                mtX.setVisibility(View.VISIBLE);
                mtX.setText(getResources().getString(R.string.value_format,mDx));
                mtY.setVisibility(View.VISIBLE);
                mtY.setText(getResources().getString(R.string.value_format,mDy));

            }
        });
        bHeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pitchHeight = pitch - 1.5 ;
                if(pitchHeight < 0 ){
                    pitchHeight = -pitchHeight;
                }
                mDisplayH.setVisibility(View.VISIBLE);
                realHeight =   Float.parseFloat(height.getText().toString())+ (distance * Math.tan(pitchHeight));
                mDisplayH.setText(getResources().getString(R.string.value_format,realHeight));

            }
        });


        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        FloatingActionButton send = findViewById(R.id.send);
        FloatingActionButton setting =  findViewById(R.id.setting);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                sendingCoordinat();
            }
        });
        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                configuration();
            }
        });


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        startCompass();
        assert mSensorManager != null;

        mSensorBaromoter = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorRotationV = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);


        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
    }

    private void configuration() {
         SettingDialog setting_dialog =  new SettingDialog();
         setting_dialog.show(getSupportFragmentManager(),"Setting Dialog");
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        startCompass();
    }

    private void startCompass() {
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null){
            if(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)==null || mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)== null ){
                alert();
            }else{
                accelometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            }
        }
    }

    private void alert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setMessage("Your Device doesn't supported")
                .setCancelable(false)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
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
        } if (mSensorRotationV != null) {
            mSensorManager.registerListener(this, mSensorRotationV,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        mSensorManager.unregisterListener(this);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;
//        mBarometer = sensorEvent.values[0];
        synchronized (this) {
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER ){
                mAccelerometerData = sensorEvent.values.clone();
                mGravity[0] = alpha*mGravity[0]+(1-alpha)*sensorEvent.values[0];
                mGravity[1] = alpha*mGravity[1]+(1-alpha)*sensorEvent.values[1];
                mGravity[2] = alpha*mGravity[2]+(1-alpha)*sensorEvent.values[2];
            }
            if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD ){
                mMagnetometerData = sensorEvent.values.clone();
                mGeomagnetic[0] = alpha*mGeomagnetic[0]+(1-alpha)*sensorEvent.values[0];
                mGeomagnetic[1] = alpha*mGeomagnetic[1]+(1-alpha)*sensorEvent.values[1];
                mGeomagnetic[2] = alpha*mGeomagnetic[2]+(1-alpha)*sensorEvent.values[2];
            }
            if(sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE ){
                mBarometer = sensorEvent.values[0];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                mRotation = sensorEvent.values.clone();
                if (mRotation != null) {
                    float RmatVector[] = new float[16];
                    SensorManager.getRotationMatrixFromVector(RmatVector, mRotation);
                    SensorManager.remapCoordinateSystem(RmatVector, SensorManager.AXIS_X, SensorManager.AXIS_Z, rMat);
                    SensorManager.getOrientation(rMat, orientation);
                    mAzzimuth = (float)Math.toDegrees((double)orientation[0]);  //azimuth

                    azzimuthformeasurement = orientation[0];
                    if (mAzzimuth < 0) {
                        mAzzimuth = 360 + mAzzimuth;
                    }
                }
                mRotation = null;
            }

        }

        float altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE,mBarometer);
        SensorManager.getRotationMatrix(rotationMatrix,null, mAccelerometerData, mMagnetometerData);
//        boolean success =  SensorManager.getRotationMatrix(K,I, mGravity, mGeomagnetic);
//        if(success){
//            float orientation[] = new float[3];
//            SensorManager.getOrientation(K,orientation);
//            mAzzimuth = Math.toDegrees(orientation[0]);
//            mAzzimuth = (mAzzimuth+360)%360;
//            azzimuthformeasurement = orientation[0];
//
//            azziText.setText(getResources().getString(R.string.value_format,mAzzimuth));
//        }
        float orientationValues []= new float[3];
        SensorManager.getOrientation(rotationMatrix, orientationValues);
        pitch = orientationValues[1];
//        mAzzimuth = (float)Math.toDegrees(orientationValues[0]);
//        mAzzimuth = (mAzzimuth+360)%360;
        direction(mAzzimuth);



        if(pitch <= 0 ){
            pitch = -pitch;
        }
        if(pitch >= 1.5 && clicked){
            bHeight.setVisibility(View.VISIBLE) ;
        }
        this.degree = Math.toDegrees(pitch);
//        mAl.setText(getResources().getString(
//                R.string.value_format, altitude));
        mTextSensorPitch.setText(getResources().getString(
                R.string.value_format, pitch));
    }

    private void direction(float mAzzimuth) {
        String where = "no Direction";
        if(mAzzimuth >= 350 || mAzzimuth <= 10){
            where = "° N";
        }else if(mAzzimuth < 350 && mAzzimuth > 200){
            where = "° NW";
        }else if(mAzzimuth < 280 && mAzzimuth > 260){
            where = "° W";
        }else if(mAzzimuth <= 260 && mAzzimuth > 190 ){
            where = "° SW";
        }else if(mAzzimuth <= 190 && mAzzimuth > 170 ){
            where = "° S";
        }else if(mAzzimuth <= 170 && mAzzimuth > 100 ){
            where = "° SE";
        }else if(mAzzimuth <= 100 && mAzzimuth > 80 ){
            where = "° E";
        }else if(mAzzimuth <= 80 && mAzzimuth > 10 ){
            where = "° NE";
        }
        azziText.setText(getResources().getString(R.string.value_format,mAzzimuth) + where);
    }


    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

        mFusedLocationClient.getLastLocation().addOnSuccessListener(
                new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            mLastLocation = location;
                            accuracy.setText(getResources().getString(R.string.value_format,mLastLocation.getAccuracy()));

                        }
                    }

                });
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
            mBackgroundHandler = null;

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
        switch (requestCode) {
            case 2:
// If the permission is granted, get the location,
// otherwise, show a Toast
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();
                } else {
                    Toast.makeText(this,
                            "Permission Denied",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

    public void getLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(
                    new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                mLastLocation = location;
                                utmLatitude = mLastLocation.getLatitude();
                                latitude.setText(getString(R.string.format,utmLatitude));
                                utmLongitude = mLastLocation.getLongitude();
                                longitude.setText(getString(R.string.format,utmLongitude));
                                accuracy.setText(getString(R.string.format,mLastLocation.getAccuracy()));

                                utmLatitude = utmLatitude * 110574.610;
                                if(utmLatitude <= 0 ){
                                    utmLatitude = -utmLatitude;
                                }
                                if(utmLongitude <= 0){
                                    utmLongitude =-utmLongitude;
                                }
                                utmLongitude = (utmLongitude - 108.0)*(111302.617) +166021.41;
                                mSL.setText(getString(R.string.utm_format,utmLatitude));
                                mEL.setText(getString(R.string.utm_format,utmLongitude));

                            } else {
                                longitude.setText("No location founded");
                                latitude.setText("No location founded");
                            }
                        }

                    });

        }
    }

    @Override
    public void applyValue(double Height) {
        height.setText(""+ Height);

    }
}