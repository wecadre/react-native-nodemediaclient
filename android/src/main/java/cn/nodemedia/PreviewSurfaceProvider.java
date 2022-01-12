package cn.nodemedia;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;


public class PreviewSurfaceProvider implements Preview.SurfaceProvider {

    private Context mContext;

    @Nullable
    final AtomicReference<PreviewStreamStateObserver> mActiveStreamStateObserver =
            new AtomicReference<>();

    @NonNull
    final MutableLiveData<PreviewView.StreamState> mPreviewStreamStateLiveData =
            new MutableLiveData<>(PreviewView.StreamState.IDLE);


    CameraGLSurfaceView cameraGLSurfaceView;

    public PreviewSurfaceProvider(Context context, CameraGLSurfaceView parent) {
        this.mContext = context;
        cameraGLSurfaceView = parent;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onSurfaceRequested(@NonNull SurfaceRequest request) {

        if (!Threads.isMainThread()) {
            // Post on main thread to ensure thread safety.
            ContextCompat.getMainExecutor(mContext).execute(
                    () -> this.onSurfaceRequested(request));
            return;
        }


        CameraInternal camera = request.getCamera();
        Executor executorService = ContextCompat.getMainExecutor(mContext);
        request.setTransformationInfoListener(executorService, transformationInfo -> {
            Log.d(TAG, "onTransformationInfoUpdate: " + transformationInfo);

            boolean isFrontCamera = camera.getCameraInfoInternal().getLensFacing() == CameraSelector.LENS_FACING_FRONT;
            cameraGLSurfaceView.getPreviewTransform().setTransformationInfo(transformationInfo, request.getResolution(), isFrontCamera);

            cameraGLSurfaceView.redrawPreview();
        });

        cameraGLSurfaceView.setImplementation(new CameraSurfaceView(cameraGLSurfaceView.getParentFrameLayout(), cameraGLSurfaceView.getPreviewTransform()));

        PreviewStreamStateObserver streamStateObserver =
                new PreviewStreamStateObserver(camera.getCameraInfoInternal(),
                        mPreviewStreamStateLiveData, cameraGLSurfaceView.getImplementation());
        mActiveStreamStateObserver.set(streamStateObserver);

        camera.getCameraState().addObserver(
                ContextCompat.getMainExecutor(mContext), streamStateObserver);
        cameraGLSurfaceView.getImplementation().onSurfaceRequested(request, () -> {
            // We've no longer needed this observer, if there is no new StreamStateObserver
            // (another SurfaceRequest), reset the streamState to IDLE.
            // This is needed for the case when unbinding preview while other use cases are
            // still bound.
            if (mActiveStreamStateObserver.compareAndSet(streamStateObserver, null)) {
                streamStateObserver.updatePreviewStreamState(PreviewView.StreamState.IDLE);
            }
            streamStateObserver.clear();
            camera.getCameraState().removeObserver(streamStateObserver);
        });

    }


}