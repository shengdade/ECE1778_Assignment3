package com.example.assignmentthree;

import android.app.Fragment;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.location.Location;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

/**
 * Created by shengdad on 2/2/16.
 */
public class CameraFragment extends Fragment implements SurfaceHolder.Callback, ConnectionCallbacks, OnConnectionFailedListener {

    public final String TAG = "CameraFragment";
    private Camera cam;
    SurfaceView cameraView;
    SurfaceHolder holder;

    private SensorManager mSensorManager;
    private float mAccel; // acceleration apart from gravity
    private float mAccelCurrent; // current acceleration including gravity
    private float mAccelLast; // last acceleration including gravity

    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Configure the Accelerometer
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        buildGoogleApiClient();

        Log.e(TAG, "onCreate finished");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View resultView = inflater.inflate(R.layout.frag_camera, container, false);
        cameraView = (SurfaceView) resultView.findViewById(R.id.cameraView);

        Log.e(TAG, "onCreateView finished");
        return (resultView);
    }

    private final SensorEventListener mSensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent se) {
            float x = se.values[0];
            float y = se.values[1];
            float z = se.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta; // perform low-cut filter

            if (mAccel > 8) {
                Toast.makeText(getActivity().getApplicationContext(), "Taking picture in one second...", Toast.LENGTH_SHORT).show();
                mSensorManager.unregisterListener(mSensorListener);
                Log.e(TAG, "'mAccel > 8' detected");
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        try {
                            cam.stopPreview();
                            cam.takePicture(null, null, mPicture);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }, 1000);
                Log.e("Photo", "Taken successfully");
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.e(TAG, "onPictureTaken begin");
            if (mLastLocation == null) {
                Log.e(TAG, "Returned location is null, save the default coordinates");
                //The location service has been disabled, return the address of SF
                saveToFile(new PhotoWithLocation(data, 43.660047, -79.395105));
            } else {
                saveToFile(new PhotoWithLocation(data, mLastLocation));
            }
            Log.e(TAG, "onPictureTaken finished, saved to file");
            //start the next taking picture
            try {
                cam.setPreviewDisplay(holder);
                cam.startPreview();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }
    };

    private void saveToFile(PhotoWithLocation photo) {
        try {
            String filename = DateFormat.getDateTimeInstance().format(new Date());
            FileOutputStream fos = getActivity().openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(photo);
            os.close();
            fos.close();
            Toast.makeText(getActivity(), "stored", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e(TAG, "saveToFile finished");
    }

    @Override
    public void onStart() {
        super.onStart();
        cam = Camera.open(findCameraId());
        setCamParameter();
        holder = cameraView.getHolder();
        holder.addCallback(this);
        mGoogleApiClient.connect();
        Log.e(TAG, "onStart finished");
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            cam.setPreviewDisplay(holder);
            cam.startPreview();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        Log.e(TAG, "onResume finished");
    }

    // find the first back camera, return an int
    private int findCameraId() {
        int chosen = -1;
        int count = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int cameraId = 0; cameraId < count; cameraId++) {
            Camera.getCameraInfo(cameraId, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                chosen = cameraId;
                break;
            }
        }
        Log.e(TAG, "findCameraId finished");
        return chosen;
    }

    // set camera parameters
    public void setCamParameter() {
        Camera.Parameters parameters = cam.getParameters();

        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = supportedPreviewSizes.get(supportedPreviewSizes.size() - 1);//The last element, normally the largest size
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        List<Camera.Size> supportedPhotoSizes = parameters.getSupportedPictureSizes();
        Camera.Size photoSize = supportedPhotoSizes.get(0);
        Log.e(TAG, photoSize.toString());
        // Camera.Size photoSize = supportedPhotoSizes.get(supportedPhotoSizes.size() - 1);//The last element, normally the best quality photo size
        parameters.setPictureSize(photoSize.width, photoSize.height);

        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        parameters.setJpegQuality(100);//the best quality
        cam.setParameters(parameters);
        cam.setDisplayOrientation(90);
        Log.e(TAG, "setCamParameter finished");
    }

    @Override
    public void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        super.onPause();
        Log.e(TAG, "onPause finished");
    }

    @Override
    public void onStop() {
        cam.stopPreview();
        cam.release();
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        Log.e(TAG, "onStop finished");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            cam.setPreviewDisplay(holder);
            cam.startPreview();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Log.e(TAG, "surfaceCreated finished");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(TAG, "surface Changed");

        if (holder.getSurface() == null) {
            return;
        }

        try {
            cam.stopPreview();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Camera.Parameters parameters = cam.getParameters();
        Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        if (display.getRotation() == Surface.ROTATION_0) {//The rotation of surface
            parameters.set("rotation", 90);//The rotation of taken picture
            cam.setDisplayOrientation(90);//The rotation of preview
        } else if (display.getRotation() == Surface.ROTATION_90) {//1
            parameters.set("rotation", 0);
            cam.setDisplayOrientation(0);
        } else if (display.getRotation() == Surface.ROTATION_180) {//2
            parameters.set("rotation", 270);
            cam.setDisplayOrientation(270);
        } else if (display.getRotation() == Surface.ROTATION_270) {//3
            parameters.set("rotation", 180);
            cam.setDisplayOrientation(180);
        }

        try {
            cam.setParameters(parameters);
            cam.setPreviewDisplay(holder);
            cam.startPreview();

        } catch (Exception e) {
            Log.e(TAG, "Error starting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this.getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation == null) {
            Toast.makeText(this.getActivity(), R.string.no_location_detected, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }
}
