package cn.nodemedia.react_native_nodemediaclient;

/**
 * Created by aliang on 2018/2/28.
 */

import android.app.PictureInPictureParams;
import android.util.Rational;
import com.facebook.react.bridge.ReactApplicationContext;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class RCTNodeMediaClient extends ReactContextBaseJavaModule {
    private static String mLicense = "";

    public RCTNodeMediaClient(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "NodeMediaClient";
    }

    @ReactMethod
    public void setLicense(String license) {
        mLicense = license;
    }

    public static String getLicense() {
        return mLicense;
    }

    @ReactMethod
    public void enterPip() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(9, 16);
            PictureInPictureParams params = new PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build();
            getCurrentActivity().enterPictureInPictureMode(params);
        }
    }
}
