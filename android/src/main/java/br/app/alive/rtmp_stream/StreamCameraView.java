package br.app.alive.rtmp_stream;;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.pedro.encoder.input.video.CameraCallbacks;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.rtmp.utils.ConnectCheckerRtmp;
import com.pedro.rtplibrary.rtmp.RtmpCamera2;
import com.pedro.rtplibrary.view.AspectRatioMode;
import com.pedro.rtplibrary.view.OpenGlView;

@SuppressLint("RestrictedApi")
public class StreamCameraView extends FrameLayout implements ConnectCheckerRtmp, CameraCallbacks, SurfaceHolder.Callback {

    private final OpenGlView surfaceView;
    private final ThemedReactContext reactContext;

    RtmpCamera2 rtmpCamera;
    private String outputUrl = "";
    private String lensFacing = "1";

    private boolean isPublishing;
    private int videoBitrate = 2750000;
    private int videoFps = 18;
    private int videoWidth = 1080;
    private int videoHeight = 1920;
    private int audioBitrate = 64000;
    private int audioSampleRate = 44100;
    private Boolean autoPreview = false;
    private boolean surfaceReady;
    private boolean mute = false;


    public StreamCameraView(@NonNull ThemedReactContext context) {
        super(context);
        context.getCurrentActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        reactContext = context;

        surfaceView = new OpenGlView(reactContext);
        surfaceView.setKeepAspectRatio(true);
        surfaceView.setAspectRatioMode(AspectRatioMode.Fill);

        rtmpCamera = new RtmpCamera2(surfaceView, this);

        rtmpCamera.setReTries(10);
        rtmpCamera.setLogs(false);
        rtmpCamera.setCameraCallbacks(this);
        addView(surfaceView);
        surfaceView.getHolder().addCallback(this);

    }


    int getCameraRotation() {
        return CameraHelper.getCameraOrientation(reactContext);
    }

    public void startPublish() {
        if (!isPublishing) {
            boolean audioOk = rtmpCamera.prepareAudio(audioBitrate, audioSampleRate, true, false, false);

            boolean videoOk = rtmpCamera.prepareVideo(videoHeight, videoWidth, videoFps, videoBitrate, getCameraRotation());

            if (audioOk && videoOk) {
                rtmpCamera.startPreview();
                rtmpCamera.startStream(outputUrl);
            }
        }
    }

    public void stopPublish() {
        rtmpCamera.stopStream();
    }

    public void setOutputUrl(String outputUrl) {
        this.outputUrl = outputUrl;
    }


    public void switchCam() {
        Log.d(TAG, "switchCam: called");
        rtmpCamera.switchCamera();
    }


    public void setFlashEnable(boolean enable) {
        if (enable)
            try {
                rtmpCamera.enableLantern();
            } catch (Exception e) {
                e.printStackTrace();
            }
        else rtmpCamera.disableLantern();

    }


    public void startPreview() {

        if (rtmpCamera.isStreaming()) rtmpCamera.replaceView(surfaceView);
        else rtmpCamera.stopPreview();

        /* i dont know why video and height are reversed params*/
        rtmpCamera.startPreview(lensFacing, videoHeight, videoWidth, videoFps, getCameraRotation());
    }

    public void stopPreview() {
        if (rtmpCamera.isStreaming()) rtmpCamera.replaceView(reactContext);
        else rtmpCamera.stopPreview();
    }

    public void setAutoPreview(Boolean autoPreview) {
        this.autoPreview = autoPreview;
        if (surfaceReady) {
            if (autoPreview != null && autoPreview) startPreview();
            else stopPreview();

        }
    }

    public void setMute(boolean mute) {
        this.mute = mute;
        if (mute) rtmpCamera.disableAudio();
        else rtmpCamera.enableAudio();


    }

    public void setCamera(String cameraId) {
        lensFacing = cameraId;
        Log.d(TAG, "setCamera: " + cameraId);
        rtmpCamera.switchCamera(cameraId);
    }

    public void setAudio(int bitrate, int sampleRate) {
        audioBitrate = bitrate;
        audioSampleRate = sampleRate;
    }

    public void setVideo(int preset, int fps, int bitrate) {
        switch (preset) {
            case 0:
                this.videoWidth = 270;
            case 1:
                this.videoWidth = 360;
            case 2:
                this.videoWidth = 480;
            case 3:
                this.videoWidth = 540;
            case 4:
                this.videoWidth = 720;
            case 5:
                this.videoWidth = 1080;
        }
        this.videoHeight = (int) Math.round(this.videoWidth * 1.77);
        this.videoFps = fps;
        this.videoBitrate = bitrate;
    }

    void sendEvent(String event, WritableMap payload) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(event, payload);

    }

    void sendNativeEvent(String event, WritableMap payload) {
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), event, payload);
    }

    @Override
    public void onCameraChanged(CameraHelper.Facing facing) {

        lensFacing = String.valueOf(facing.ordinal());
    }

    @Override
    public void onCameraError(String error) {

    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        surfaceReady = true;

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        surfaceReady = true;
        if (autoPreview) {
            // WritableMap params = Arguments.createMap();
            // params.putString("status", "resumed");

            startPreview();
            // sendNativeEvent("onCameraStatus", params);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

        surfaceReady = false;
//        if (rtmpCamera.isStreaming()) {
//            rtmpCamera.stopStream();
//        }
        //WritableMap params = Arguments.createMap();
        //params.putString("status", "paused");
        if (rtmpCamera.isStreaming()) {
            rtmpCamera.replaceView(reactContext);
            //sendNativeEvent("onCameraStatus", params);
        } else {

            rtmpCamera.stopPreview();
            //sendNativeEvent("onCameraStatus", params);
        }

    }

    @Override
    public void onAuthErrorRtmp() {
    }

    @Override
    public void onAuthSuccessRtmp() {
    }

    @Override
    public void onConnectionFailedRtmp(@NonNull String s) {
        WritableMap params = Arguments.createMap();
        params.putInt("code", 2002);
        params.putString("status", s);
        sendNativeEvent("onChange", params);

        isPublishing = false;
    }

    @Override
    public void onConnectionStartedRtmp(@NonNull String s) {
        WritableMap params = Arguments.createMap();
        params.putInt("code", 2000);
        params.putString("msg", s);
        sendNativeEvent("onChange", params);

        isPublishing = true;
    }

    @Override
    public void onConnectionSuccessRtmp() {

        WritableMap params = Arguments.createMap();
        params.putInt("code", 2001);
        params.putString("msg", outputUrl);
        sendNativeEvent("onChange", params);

        isPublishing = true;

    }

    @Override
    public void onDisconnectRtmp() {
        WritableMap params = Arguments.createMap();
        params.putInt("code", 2004);
        params.putString("msg", "connection closed");
        sendNativeEvent("onChange", params);

        isPublishing = false;

    }

    @Override
    public void onNewBitrateRtmp(long l) {
        WritableMap params = Arguments.createMap();
        params.putDouble("bitrate", l);

        sendEvent("onBitrate", params);
    }
}
