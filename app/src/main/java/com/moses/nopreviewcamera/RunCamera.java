package com.moses.nopreviewcamera;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import java.io.IOException;
import java.util.Arrays;

import static android.content.ContentValues.TAG;

public class RunCamera extends Service {
    public RunCamera() {
    }

    private MediaRecorder mediaRecorder;
    private final CameraCaptureSessionStateCallback cameraCaptureSessionStateCallback = new CameraCaptureSessionStateCallback();
    private final CameraDeviceStateCallback cameraDeviceStateCallback = new CameraDeviceStateCallback();
    private CameraDevice cameraDevice;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Use this to initialize the camera profile
            setupCamera();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Use this to start recording video
        start();
    }

    @Override
    public void onDestroy() {
        stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public void setupCamera() throws IOException {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            final String filename = "/storage/emulated/0/" + System.currentTimeMillis() + ".mp4";
            CamcorderProfile profile = CamcorderProfile.get(CameraMetadata.LENS_FACING_FRONT, CamcorderProfile.QUALITY_HIGH);
            mediaRecorder.setOutputFile(filename);
            mediaRecorder.setOrientationHint(0);
            mediaRecorder.setProfile(profile);
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.d(TAG, "start: exception" + e.getMessage());
        }

    }

    public void start() {
        // This allocates and starts all camera and recording proccesses
        Log.d(TAG, "start: ");
        CameraManager cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            // The "1" indicates the front camera, 0 would be back and other values change for different devices
            cameraManager.openCamera("1", cameraDeviceStateCallback, handler);
        } catch (CameraAccessException | SecurityException e) {
            Log.d(TAG, "start: exception " + e.getMessage());
        }

    }

    public void stop() {
        // This stops and release all camera and recording proccesses
        Log.d(TAG, "stop: ");
        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();
        mediaRecorder = null;
        cameraDevice.close();
    }

    // Callbacks used to init camera, and set some parameters
    private class CameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
        private final static String TAG = "CamCaptSessionStCb";

        @Override
        public void onActive(CameraCaptureSession session) {
            Log.d(TAG, "onActive: ");
            super.onActive(session);
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            Log.d(TAG, "onClosed: ");
            super.onClosed(session);
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.d(TAG, "onConfigured: ");
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.d(TAG, "onConfigureFailed: ");
        }

        @Override
        public void onReady(CameraCaptureSession session) {
            Log.d(TAG, "onReady: ");
            super.onReady(session);
            try {
                // This makes the camera ready for recording
                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

                // Two lines right here set the FPS of the recording, used if you want high quality (60fps), slow mo (120fps+) or just more stable FPS recording
                Range<Integer> fpsRange = Range.create(30, 30);
                builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);

                builder.addTarget(mediaRecorder.getSurface());
                CaptureRequest request = builder.build();
                session.setRepeatingRequest(request, null, handler);
                mediaRecorder.start();
            } catch (CameraAccessException e) {
                Log.d(TAG, "onConfigured: " + e.getMessage());

            }
        }

        @Override
        public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {
            Log.d(TAG, "onSurfacePrepared: ");
            super.onSurfacePrepared(session, surface);
        }
    }

    // Another callback for the camera
    private class CameraDeviceStateCallback extends CameraDevice.StateCallback {
        private final static String TAG = "CamDeviceStateCb";

        @Override
        public void onClosed(CameraDevice camera) {
            Log.d(TAG, "onClosed: ");
            super.onClosed(camera);
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "onDisconnected: ");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG, "onError: ");
        }

        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "onOpened: ");
            cameraDevice = camera;
            try {
                camera.createCaptureSession(Arrays.asList(mediaRecorder.getSurface()), cameraCaptureSessionStateCallback, handler);
            } catch (CameraAccessException e) {
                Log.d(TAG, "onOpened: " + e.getMessage());
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
