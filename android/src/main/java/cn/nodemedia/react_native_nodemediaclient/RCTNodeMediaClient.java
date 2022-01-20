package cn.nodemedia.react_native_nodemediaclient;

/**
 * Created by aliang on 2018/2/28.
 */

import static android.content.ContentValues.TAG;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import cn.nodemedia.NodePublisher;


public class RCTNodeMediaClient extends ReactContextBaseJavaModule {
    private static String mLicense = "";
    private NodePublisher nodePublisher;
    private ReactApplicationContext reactContext;

    public RCTNodeMediaClient(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "NodeMediaClient";
    }

    @ReactMethod
    public void setLicense(String license) {
        mLicense = license;
    }

    @Override
    public void initialize() {
        super.initialize();

        ReactPipActivity activity = (ReactPipActivity) reactContext.getCurrentActivity();
        assert activity != null;

    }

    public static String getLicense() {
        return mLicense;
    }

    @ReactMethod
    public void enterPip() {
        ReactPipActivity activity = (ReactPipActivity) getCurrentActivity();
        activity.enterPip();
    }

    @ReactMethod
    public void setShouldEnterPip(Boolean shouldEnter) {
        Log.d(TAG, "setShouldEnterPip: called" + shouldEnter);
        ReactPipActivity activity = (ReactPipActivity) getCurrentActivity();
        activity.setShouldEnterPip(shouldEnter);
    }
    
    @ReactMethod
    public boolean getShouldEnterPip() {
        ReactPipActivity activity = (ReactPipActivity) getCurrentActivity();
        return activity.getShouldEnterPip();
    }

}
