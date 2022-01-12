package cn.nodemedia;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraManager;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;

import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static cn.nodemedia.NodePublisher.CAMERA_BACK;
import static cn.nodemedia.NodePublisher.CAMERA_FRONT;

import com.facebook.react.uimanager.ThemedReactContext;

import cn.nodemedia.react_native_nodemediaclient.ReactPipActivity;


/**
 * Created by Mingliang Chen on 17/3/6.
 */
public class NodeCameraView extends FrameLayout implements GLSurfaceView.Renderer, SurfaceHolder.Callback, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "NodeMedia.CameraView";
    public static final int NO_TEXTURE = -1;

    private GLSurfaceView mGLSurfaceView;
    private SurfaceTexture mSurfaceTexture;
    private Context mContext;
    private Camera mCamera;
    private int mTextureId = -1;

    private boolean isStarting;
    private boolean isAutoFocus = true;
    private int mCameraId = 0;
    private int mCameraNum = 0;
    private int mCameraWidth = 1920;
    private int mCameraHeight = 1080;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private boolean isMediaOverlay = false;
    private boolean appInBackground;

    Handler cameraEventHandler = new Handler();
    CameraManager cameraManager;

    private NodeCameraViewCallback mNodeCameraViewCallback;
    private String cameraAvailable;
    private boolean inPipMode;
    private ReactPipActivity mainActivity;


    public NodeCameraView(ThemedReactContext context) {
        super(context);
        initView(context);
    }

    public NodeCameraView(ThemedReactContext context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public NodeCameraView(ThemedReactContext context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NodeCameraView(ThemedReactContext context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }


    private void initView(ThemedReactContext context) {

        mContext = context;
        mCameraNum = Camera.getNumberOfCameras();
        ReactPipActivity activity = (ReactPipActivity) context.getCurrentActivity();
        mainActivity = activity;
        assert activity != null;


        initCameraManager(activity);


    }


    void initCameraManager(ReactPipActivity activity) {


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);


            cameraManager.registerAvailabilityCallback(new CameraManager.AvailabilityCallback() {


                @Override
                public void onCameraAvailable(String cameraId) {
                    super.onCameraAvailable(cameraId);
                    if (!appInBackground) {
                        cameraAvailable = cameraId;
                        mainActivity.sendEvent(CameraEvents.STATE_CHANGE, CameraEvents.EVENT_AVAILABLE);
                    }

                }

                @Override
                public void onCameraUnavailable(String cameraId) {
                    super.onCameraUnavailable(cameraId);
                    if (appInBackground) {
                        cameraAvailable = null;
                        mainActivity.sendEvent(CameraEvents.STATE_CHANGE, CameraEvents.EVENT_UNAVAILABLE);
                    }
                }
            }, cameraEventHandler);

        }

    }

    private void createTexture() {
        if (mTextureId == NO_TEXTURE) {
            Log.d(TAG, "GL createTexture");
            mTextureId = getExternalOESTextureID();
            mSurfaceTexture = new SurfaceTexture(mTextureId);
            mSurfaceTexture.setOnFrameAvailableListener(this);
        }
    }

    private void destroyTexture() {
        if (mTextureId > NO_TEXTURE) {
            Log.d(TAG, "GL destroyTexture");
            mTextureId = NO_TEXTURE;
            mSurfaceTexture.setOnFrameAvailableListener(null);
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
    }

    public GLSurfaceView getGLSurfaceView() {
        return mGLSurfaceView;
    }

    public void setCameraSize(int width, int height) {
        this.mCameraWidth = width;
        this.mCameraHeight = height;
    }

    public int startPreview(int cameraId) {

        if (isStarting) return -1;
        try {
            mCameraId = cameraId > mCameraNum - 1 ? 0 : cameraId;
            mCamera = Camera.open(mCameraId);
        } catch (Exception e) {
            return -2;
        }
        try {
            Camera.Parameters para = mCamera.getParameters();
            choosePreviewSize(para, this.mCameraWidth, this.mCameraHeight);
            mCamera.setParameters(para);
            setAutoFocus(this.isAutoFocus);
        } catch (Exception e) {
            Log.w(TAG, "startPreview setParameters:" + e.getMessage());
            return -1;
        }

        mGLSurfaceView = new GLSurfaceView(mContext);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(this);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGLSurfaceView.getHolder().addCallback(this);
        mGLSurfaceView.getHolder().setKeepScreenOn(true);
        mGLSurfaceView.setZOrderMediaOverlay(isMediaOverlay);
        addView(mGLSurfaceView);
        isStarting = true;
        return 0;
    }

    public int stopPreview() {
        if (!isStarting) return -1;
        isStarting = false;

        mGLSurfaceView.queueEvent(() -> {
            if (mNodeCameraViewCallback != null) {
                mNodeCameraViewCallback.OnDestroy();
            }
        });
        removeView(mGLSurfaceView);
        mGLSurfaceView = null;
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }


    private CameraInfo getCameraInfo() {
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);
        return cameraInfo;
    }

    public Camera.Size getPreviewSize() {
        return mCamera.getParameters().getPreviewSize();
    }

    public boolean isFrontCamera() {
        CameraInfo info = getCameraInfo();
        return info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }


    public int getCameraOrientation() {
        return getCameraInfo().orientation;
    }

    private void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            this.mCameraWidth = ppsfv.width;
            this.mCameraHeight = ppsfv.height;

            parms.setPreviewSize(this.mCameraWidth, this.mCameraHeight);
        }
    }

    public int setAutoFocus(boolean isAutoFocus) {
        if (mCamera == null) {
            return -1;
        }
        try {
            Parameters parameters = mCamera.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (isAutoFocus) {
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }
            } else {
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                mCamera.autoFocus(null);
            }
            mCamera.setParameters(parameters);
            this.isAutoFocus = isAutoFocus;
        } catch (Exception e) {
            return -2;
        }

        return 0;
    }

    public int setFlashEnable(boolean on) {
        if (mCamera == null) {
            return -1;
        }
        try {
            Parameters parameters = mCamera.getParameters();
            List<String> flashModes = parameters.getSupportedFlashModes();
            if (flashModes == null) {
                return -1;
            }
            if (flashModes.contains(Parameters.FLASH_MODE_TORCH) && flashModes.contains(Parameters.FLASH_MODE_OFF)) {
                if (on) {
                    parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                } else {
                    parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                }
                mCamera.setParameters(parameters);
            }
        } catch (Exception e) {
            return -2;
        }
        return on ? 1 : 0;
    }

    public int switchCamera() {
        if (mCameraNum <= 1) {
            return -1;
        }

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        mCameraId = mCameraId == CAMERA_BACK ? CAMERA_FRONT : CAMERA_BACK;

        try {
            mCamera = Camera.open(mCameraId);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return -2;
        }

        try {
            Camera.Parameters para = mCamera.getParameters();
            choosePreviewSize(para, mCameraWidth, mCameraHeight);
            mCamera.setParameters(para);
        } catch (Exception e) {
            Log.w(TAG, "switchCamera setParameters:" + e.getMessage());
        }
        setAutoFocus(this.isAutoFocus);
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();
            mCameraWidth = getPreviewSize().width;
            mCameraHeight = getPreviewSize().height;
            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (mNodeCameraViewCallback != null) {
                        mNodeCameraViewCallback.OnChange(mCameraWidth, mCameraHeight, mSurfaceWidth, mSurfaceHeight);
                    }
                }
            });
            return mCameraId;
        } catch (Exception e) {
            return -3;
        }
    }

    //GLSurface callback
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "GL onSurfaceCreated");
        createTexture();
        if (mNodeCameraViewCallback != null) {
            mNodeCameraViewCallback.OnCreate();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "GL onSurfaceChanged");
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            mCameraWidth = getPreviewSize().width;
            mCameraHeight = getPreviewSize().height;
            if (mNodeCameraViewCallback != null) {
                mNodeCameraViewCallback.OnChange(mCameraWidth, mCameraHeight, mSurfaceWidth, mSurfaceHeight);
            }
        } catch (IOException e) {

            e.printStackTrace();
            mainActivity.sendEvent(CameraEvents.STATE_CHANGE, CameraEvents.EVENT_ERROR);

        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        try {

            mSurfaceTexture.updateTexImage();
            if (mNodeCameraViewCallback != null) {
                mNodeCameraViewCallback.OnDraw(mTextureId);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }


    interface NodeCameraViewCallback {

        void OnCreate();

        void OnChange(int cameraWidth, int cameraHeight, int surfaceWidth, int surfaceHeight);

        void OnDraw(int textureId);

        void OnDestroy();
    }

    public void setNodeCameraViewCallback(NodeCameraViewCallback callback) {
        mNodeCameraViewCallback = callback;
    }

    //Surface callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "SV surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "SV surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "SV surfaceDestroyed");
        if (mNodeCameraViewCallback != null) {
            mNodeCameraViewCallback.OnDestroy();
        }
        if (!isStarting) {
            destroyTexture();
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }

        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mGLSurfaceView != null) {
            mGLSurfaceView.requestRender();
        }

    }

    public int getExternalOESTextureID() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }


}

class CameraEvents {
    public static final String EVENT_ERROR = "error";
    public static final String EVENT_AVAILABLE = "available";
    public static final String EVENT_UNAVAILABLE = "unavailable";
    public static final String STATE_CHANGE = "onStateChange";

}