package cn.nodemedia.react_native_nodemediaclient;

import static android.app.AppOpsManager.OPSTR_PICTURE_IN_PICTURE;

import android.app.AppOpsManager;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Rational;


import androidx.camera.core.CameraXConfig;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.facebook.react.ReactActivity;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import cn.nodemedia.NodePublisher;

public class ReactPipActivity extends ReactActivity implements LifecycleOwner{
    boolean shouldEnterPip = false;
    NodePublisher mNodePublisher;
    private boolean mInPipMode;
    public static final String PIP_STATE_CHANGED = "onPipChange";
    public static final String IN_PICTURE_MODE = "in_pip";
    public static final String NOT_IN_PICTURE_MODE = "no_pip";
    private LifecycleRegistry lifecycleRegistry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mNodePublisher = new NodePublisher(this.getApplicationContext(), RCTNodeMediaClient.getLicense());
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
    }


    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);


        if (isInPictureInPictureMode != this.mInPipMode) {
            String event = isInPictureInPictureMode ? IN_PICTURE_MODE : NOT_IN_PICTURE_MODE;


            this.mInPipMode = isInPictureInPictureMode;
            sendEvent(PIP_STATE_CHANGED, event);
        }
    }

    public void sendEvent(String event, Object payload) {
        getReactInstanceManager().getCurrentReactContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(event, payload);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (shouldEnterPip) {
            enterPip();

        }
    }

    public NodePublisher getPublisher(Context context) {
        if (this.mNodePublisher == null) {

            mNodePublisher = new NodePublisher(context, RCTNodeMediaClient.getLicense());
        }
        return this.mNodePublisher;
    }

    public boolean getShouldEnterPip() {
        return this.shouldEnterPip;
    }

    public void setShouldEnterPip(boolean shouldEnter) {
        shouldEnterPip = shouldEnter;
    }

    public void enterPip() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        Context context = getApplicationContext();
        AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        if (manager == null) {
            return;
        }

        int modeAllowed = manager.checkOpNoThrow(OPSTR_PICTURE_IN_PICTURE, Process.myUid(),
                context.getPackageName());

        if (modeAllowed != AppOpsManager.MODE_ALLOWED) {
            return;
        }

        this.mInPipMode = true;
        sendEvent(PIP_STATE_CHANGED, IN_PICTURE_MODE);

        Rational aspectRatio = new Rational(9, 16);
        PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setSourceRectHint(new Rect())
                .build();
        this.enterPictureInPictureMode(params);


    }

}

