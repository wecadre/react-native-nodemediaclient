package cn.nodemedia.react_native_nodemediaclient;

import static android.app.AppOpsManager.OPSTR_PICTURE_IN_PICTURE;

import android.app.AppOpsManager;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.util.Rational;

import com.facebook.react.ReactActivity;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;

import cn.nodemedia.NodePublisher;

public class ReactPipActivity extends ReactActivity {
    boolean shouldEnterPip = false;
    NodePublisher mNodePublisher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mNodePublisher = new NodePublisher(this.getApplicationContext(), RCTNodeMediaClient.getLicense());
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);

        getReactInstanceManager().getCurrentReactContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("onPipChange", isInPictureInPictureMode ? "in_pip" : "no_pip");
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (shouldEnterPip) {
            enterPip();

        }
    }

    public NodePublisher getPublisher(Context context) {
        if(this.mNodePublisher == null) {
            Log.d("TAG", "getPublisher: creating new instance");
            mNodePublisher = new NodePublisher(context, RCTNodeMediaClient.getLicense());
        }
        return this.mNodePublisher;
    }

    public boolean getShouldEnterPip() {
        return this.shouldEnterPip;
    }

    public void setShouldEnterPip(boolean shouldEnterPip) {
        this.shouldEnterPip = shouldEnterPip;
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


        Rational aspectRatio = new Rational(9, 16);
        PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setSourceRectHint(new Rect())
                //.setAutoEnterEnabled(true)
                .build();
        this.enterPictureInPictureMode(params);


    }

}

