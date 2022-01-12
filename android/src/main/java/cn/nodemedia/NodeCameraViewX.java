package cn.nodemedia;

import static android.content.ContentValues.TAG;
import static androidx.camera.core.CameraSelector.LENS_FACING_BACK;
import static androidx.camera.core.CameraSelector.LENS_FACING_FRONT;
import static cn.nodemedia.NodeCameraView.NO_TEXTURE;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.ResolutionInfo;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.facebook.react.uimanager.ThemedReactContext;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

import cn.nodemedia.react_native_nodemediaclient.R;
import cn.nodemedia.react_native_nodemediaclient.ReactPipActivity;

public class NodeCameraViewX extends FrameLayout {

    private ThemedReactContext reactContext;

    private CameraGLSurfaceView glSurfaceView;
    private SurfaceTexture surfaceTexture;
    private PreviewView previewView;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private CameraSelector cameraSelector;
    private Camera camera;
    private NodeCameraViewCallback mNodeCameraViewCallback;


    private boolean isMediaOverlay = false;

    private int mTextureId = NO_TEXTURE;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mCameraWidth;
    private int mCameraHeight;
    private FrameLayout layout;
    private boolean isStarting;
    private PreviewSurfaceProvider previewSurfaceProvider;

    public NodeCameraViewX(ThemedReactContext context) {
        super(context);
        init(context);

    }

    @SuppressLint("ClickableViewAccessibility")
    void init(ThemedReactContext context) {
        Log.d(TAG, "init: INIT THE CLASS");
        reactContext = context;

        layout = (FrameLayout) LayoutInflater.from(reactContext).inflate(R.layout.camera_view, this, true);

        glSurfaceView = new CameraGLSurfaceView(reactContext, this);
        layout.addView(glSurfaceView);

        cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(cameraFutureListener, ContextCompat.getMainExecutor(context));

    }

    Runnable cameraFutureListener = () -> {

        try {
            // Camera provider is now guaranteed to be available
            cameraProvider = cameraProviderFuture.get();

            // Set up the view finder use case to display camera preview
            preview = new Preview.Builder().build();


            preview.setSurfaceProvider(glSurfaceView.getSurfaceProvider());

        } catch (InterruptedException | ExecutionException e) {
            // Currently no exceptions thrown. cameraProviderFuture.get()
            // shouldn't block since the listener is being called, so no need to
            // handle InterruptedException.
        }
    };


    public void setNodeCameraViewCallback(NodeCameraViewX.NodeCameraViewCallback callback) {
        Log.d(TAG, "setNodeCameraViewCallback: called");
        //if(glSurfaceView != null)
        glSurfaceView.setNodeCameraViewCallback(callback);

    }

    private ReactPipActivity getMainActivity() {
        ReactPipActivity activity = (ReactPipActivity) reactContext.getCurrentActivity();
        return activity;
    }


    public int startPreview(int cameraId) {
        Log.d(TAG, "startPreview: " + cameraId);
        isStarting = true;

        cameraSelector = new CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build();

        camera = cameraProvider.bindToLifecycle(getMainActivity(), cameraSelector, preview);

        return 0;
    }


    public boolean isFrontCamera() {
        return false;
    }

    public int getCameraOrientation() {
        return 0;
    }

    public int stopPreview() {
        if(cameraProvider != null){

            isStarting = false;
            cameraProvider.unbindAll();
        }
        camera = null;
        return 0;
    }

    public int setFlashEnable(boolean flashEnable) {
        camera.getCameraControl().enableTorch(flashEnable);
        return 0;
    }

    public int setAutoFocus(boolean autoFocus) {
        return 0;
    }

    @SuppressLint("RestrictedApi")
    public int switchCamera() {
        if (camera == null || cameraSelector == null) return -1;

        if (cameraSelector.getLensFacing() == LENS_FACING_FRONT) {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(LENS_FACING_BACK).build();
        } else {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build();
        }
        cameraProvider.unbindAll();

        camera = cameraProvider.bindToLifecycle(getMainActivity(), cameraSelector, preview);

        return 0;
    }


    public Size getPreviewSize() {
        if (preview != null) {

            ResolutionInfo res = preview.getResolutionInfo();

            if (res != null) return res.getResolution();
        }

        return new Size(mSurfaceWidth, mSurfaceHeight);
    }


    public interface NodeCameraViewCallback {

        void OnCreate();

        void OnChange(int cameraWidth, int cameraHeight, int surfaceWidth, int surfaceHeight);

        void OnDraw(int textureId);

        void OnDestroy();
    }

    class StreamPreview extends UseCase {


        @SuppressLint("RestrictedApi")
        protected StreamPreview(@NonNull UseCaseConfig<?> currentConfig, ThemedReactContext context) {
            super(currentConfig);
        }

        @Nullable
        @Override
        public UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig, @NonNull UseCaseConfigFactory factory) {
            return null;
        }

        @NonNull
        @Override
        public UseCaseConfig.Builder<?, ?, ?> getUseCaseConfigBuilder(@NonNull Config config) {
            return null;
        }

        @NonNull
        @Override
        protected Size onSuggestedResolutionUpdated(@NonNull Size suggestedResolution) {
            return null;
        }
    }


}
