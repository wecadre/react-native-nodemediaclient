package br.app.alive.rtmp_stream;;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

import javax.annotation.Nullable;


public class StreamCameraViewManager extends SimpleViewManager<StreamCameraView> {

    public static final String REACT_CLASS = "RCTNodeCamera";

    private static final String COMMAND_STARTPREV_NAME = "startprev";
    private static final String COMMAND_STOPPREV_NAME = "stopprev";
    private static final String COMMAND_START_NAME = "start";
    private static final String COMMAND_STOP_NAME = "stop";
    private static final String COMMAND_SWITCH_CAM_NAME = "switchCamera";
    private static final String COMMAND_SWITCH_FLASH_NAME = "flashEnable";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.builder()
                .put("onChange", MapBuilder.of("registrationName", "onChange"))
//                .put("onCameraStatus", MapBuilder.of("registrationName", "onCameraStatus"))
                .build();
    }

    @NonNull
    @Override
    protected StreamCameraView createViewInstance(@NonNull ThemedReactContext reactContext) {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

        ActivityCompat.requestPermissions(reactContext.getCurrentActivity(), permissions, 1);
        return new StreamCameraView(reactContext);
    }

    @ReactProp(name = "mute")
    public void setMute(StreamCameraView view, boolean mute) {
        view.setMute(mute);
    }

    @ReactProp(name = "camera")
    public void setCameraParam(StreamCameraView view, ReadableMap cameraParam) {
        view.setCamera(String.valueOf(cameraParam.getInt("cameraId")));
    }

    @ReactProp(name = "audio")
    public void setAudioParam(StreamCameraView view, ReadableMap audioParam) {
        view.setAudio(audioParam.getInt("bitrate"), audioParam.getInt("samplerate"));
    }

    @ReactProp(name = "video")
    public void setVideoParam(StreamCameraView view, ReadableMap videoParam) {
        view.setVideo(videoParam.getInt("preset"), videoParam.getInt("fps"), videoParam.getInt("bitrate"));
    }


    @ReactProp(name = "outputUrl")
    public void setOutputUrl(StreamCameraView view, @Nullable String outputUrl) {
        view.setOutputUrl(outputUrl);
    }

    @ReactProp(name = "autopreview")
    public void setOutputUrl(StreamCameraView view, Boolean autoPreview) {
        view.setAutoPreview(autoPreview);

    }


    @Override
    public void onDropViewInstance(@NonNull StreamCameraView view) {
        super.onDropViewInstance(view);
        view.stopPreview();
        view.stopPublish();
    }


    @Override
    public void receiveCommand(StreamCameraView root, String commandId, @javax.annotation.Nullable ReadableArray args) {
        switch (commandId) {
            case COMMAND_STARTPREV_NAME:
                root.startPreview();
                break;
            case COMMAND_STOPPREV_NAME:
                root.stopPreview();
                break;
            case COMMAND_START_NAME:
                root.startPublish();
                break;
            case COMMAND_STOP_NAME:
                root.stopPublish();
                break;
            case COMMAND_SWITCH_CAM_NAME:
                root.switchCam();
                break;
            case COMMAND_SWITCH_FLASH_NAME:
                root.setFlashEnable(args.getBoolean(0));
                break;
        }
    }
}