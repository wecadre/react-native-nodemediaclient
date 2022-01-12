package cn.nodemedia;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.utils.Threads;

import com.facebook.react.bridge.ReactContext;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@SuppressLint("RestrictedApi")
public class CameraGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, SurfaceHolder.Callback, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "TAG";
    private final NodeCameraViewX parent;
    ReactContext mContext;
    //Capturing frames from an image stream in the form of OpenGL ES textures, I call them texture layers
    SurfaceTexture mSurface;
    //Texture id used
    int mTextureId = -1;
    private CameraSurfaceView mImplementation;
    private int mCameraWidth;
    private int mCameraHeight;

    public CameraSurfaceView getImplementation() {
        return mImplementation;
    }

    private NodeCameraViewX.NodeCameraViewCallback mNodeCameraViewCallback;

    private final PreviewTransformation mPreviewTransform = new PreviewTransformation();

    PreviewViewMeteringPointFactory mPreviewViewMeteringPointFactory = new PreviewViewMeteringPointFactory(mPreviewTransform);
    public Preview.SurfaceProvider mSurfaceProvider;


    public CameraGLSurfaceView(Context context, NodeCameraViewX parent) {
        super(context);
        this.parent = parent;
        mContext = (ReactContext) context;
        setEGLContextClientVersion(2);
        mSurfaceProvider = new PreviewSurfaceProvider(context, this);

        //According to the monitoring of the texture layer, the data is drawn.

        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        SurfaceHolder holder = getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);
        holder.addCallback(this);
        holder.setKeepScreenOn(true);
        setZOrderMediaOverlay(false);
    }


    public void setImplementation(CameraSurfaceView mImplementation) {
        this.mImplementation = mImplementation;
    }

    public PreviewTransformation getPreviewTransform() {
        return mPreviewTransform;
    }

    public FrameLayout getParentFrameLayout() {
        return this.parent;
    }

    void redrawPreview() {
        if (mImplementation != null) {
            mImplementation.redrawPreview();
        }
        mPreviewViewMeteringPointFactory.recalculate(new Size(getWidth(), getHeight()),
                getLayoutDirection());
    }

    @UiThread
    @NonNull
    public Preview.SurfaceProvider getSurfaceProvider() {
        Threads.checkMainThread();
        return mSurfaceProvider;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated: ");
        //Get the texture id of view surface
        mTextureId = createTextureID();
        //Use this texture id to get the texture layer Surface Texture
        mSurface = new SurfaceTexture(mTextureId);
        //Monitor Texture Layer
        mSurface.setOnFrameAvailableListener(this);
        if (mNodeCameraViewCallback != null) {
            mNodeCameraViewCallback.OnCreate();
        }

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //GLES20.glViewport(0, 0, width, height);

        mCameraWidth = parent.getPreviewSize().getWidth();
        mCameraHeight = parent.getPreviewSize().getHeight();

        Log.d(TAG, "onSurfaceChanged: " + width + ", height: " + height+ ", cameraWidth: "+ mCameraWidth+", cameraHeight: "+ mCameraHeight);

        if (mNodeCameraViewCallback != null) {
            mNodeCameraViewCallback.OnChange(width, height, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        try {
            mSurface.updateTexImage();
            if (mNodeCameraViewCallback != null) {
                mNodeCameraViewCallback.OnDraw(mTextureId);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }


    private int createTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.requestRender();
    }


    public void setNodeCameraViewCallback(NodeCameraViewX.NodeCameraViewCallback mNodeCameraViewCallback) {
        this.mNodeCameraViewCallback = mNodeCameraViewCallback;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        Log.d(TAG, "SV surfaceDestroyed");
        if (mNodeCameraViewCallback != null) {
            mNodeCameraViewCallback.OnDestroy();
        }
//        if (!isStarting) {
//            destroyTexture();
//            if (mCamera != null) {
//                mCamera.stopPreview();
//                mCamera.release();
//                mCamera = null;
//            }
//
//        }
    }
}