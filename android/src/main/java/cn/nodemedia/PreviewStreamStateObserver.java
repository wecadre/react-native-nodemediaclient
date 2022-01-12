package cn.nodemedia;

import android.annotation.SuppressLint;

import androidx.annotation.GuardedBy;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.FutureChain;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.view.PreviewView;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("RestrictedApi")
final class PreviewStreamStateObserver implements Observable.Observer<CameraInternal.State> {

    private static final String TAG = "StreamStateObserver";

    private final CameraInfoInternal mCameraInfoInternal;
    private final MutableLiveData<PreviewView.StreamState> mPreviewStreamStateLiveData;
    @GuardedBy("this")
    private PreviewView.StreamState mPreviewStreamState;
    private final CameraSurfaceView mPreviewViewImplementation;

            ListenableFuture<Void> mFlowFuture;
    private boolean mHasStartedPreviewStreamFlow = false;

    PreviewStreamStateObserver(CameraInfoInternal cameraInfoInternal,
                               MutableLiveData<PreviewView.StreamState> previewStreamLiveData,
                               CameraSurfaceView implementation) {
        mCameraInfoInternal = cameraInfoInternal;
        mPreviewStreamStateLiveData = previewStreamLiveData;
        mPreviewViewImplementation = implementation;

        synchronized (this) {
            mPreviewStreamState = previewStreamLiveData.getValue();
        }
    }

    @Override
    @MainThread
    public void onNewData(@Nullable CameraInternal.State value) {
        if (value == CameraInternal.State.CLOSING
                || value == CameraInternal.State.CLOSED
                || value == CameraInternal.State.RELEASING
                || value == CameraInternal.State.RELEASED) {
            updatePreviewStreamState(PreviewView.StreamState.IDLE);
            if (mHasStartedPreviewStreamFlow) {
                mHasStartedPreviewStreamFlow = false;
                cancelFlow();
            }
        } else if (value == CameraInternal.State.OPENING
                || value == CameraInternal.State.OPEN
                || value == CameraInternal.State.PENDING_OPEN) {
            if (!mHasStartedPreviewStreamFlow) {
                startPreviewStreamStateFlow(mCameraInfoInternal);
                mHasStartedPreviewStreamFlow = true;
            }
        }
    }

    @Override
    @MainThread
    public void onError(@NonNull Throwable t) {
        clear();
        updatePreviewStreamState(PreviewView.StreamState.IDLE);
    }

    void clear() {
        cancelFlow();
    }

    private void cancelFlow() {
        if (mFlowFuture != null) {
            mFlowFuture.cancel(false);
            mFlowFuture = null;
        }
    }


    @MainThread
    private void startPreviewStreamStateFlow(CameraInfo cameraInfo) {
        updatePreviewStreamState(PreviewView.StreamState.IDLE);

        List<CameraCaptureCallback> callbacksToClear = new ArrayList<>();
        mFlowFuture =
                FutureChain.from(waitForCaptureResult(cameraInfo, callbacksToClear))
                        .transformAsync(v -> mPreviewViewImplementation.waitForNextFrame(),
                                CameraXExecutors.directExecutor())
                        .transform(v -> {
                            updatePreviewStreamState(PreviewView.StreamState.STREAMING);
                            return null;
                        }, CameraXExecutors.directExecutor());

        Futures.addCallback(mFlowFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                mFlowFuture = null;
            }

            @Override
            public void onFailure(Throwable t) {
                mFlowFuture = null;

                if (!callbacksToClear.isEmpty()) {
                    for (CameraCaptureCallback callback : callbacksToClear) {
                        ((CameraInfoInternal) cameraInfo).removeSessionCaptureCallback(
                                callback);
                    }
                    callbacksToClear.clear();
                }
            }
        }, CameraXExecutors.directExecutor());
    }

    void updatePreviewStreamState(PreviewView.StreamState streamState) {
        // Prevent from notifying same states.
        synchronized (this) {
            if (mPreviewStreamState.equals(streamState)) {
                return;
            }
            mPreviewStreamState = streamState;
        }

        Logger.d(TAG, "Update Preview stream state to " + streamState);
        mPreviewStreamStateLiveData.postValue(streamState);
    }

    /**
     * Returns a ListenableFuture which will complete when the session onCaptureCompleted happens.
     * Please note that the future could complete in background thread.
     */
    private ListenableFuture<Void> waitForCaptureResult(CameraInfo cameraInfo,
                                                        List<CameraCaptureCallback> callbacksToClear) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    // The callback will be invoked in camera executor thread.
                    CameraCaptureCallback callback = new CameraCaptureCallback() {
                        @Override
                        public void onCaptureCompleted(
                                @NonNull CameraCaptureResult result) {
                            completer.set(null);
                            ((CameraInfoInternal) cameraInfo).removeSessionCaptureCallback(
                                    this);
                        }
                    };
                    callbacksToClear.add(callback);
                    ((CameraInfoInternal) cameraInfo).addSessionCaptureCallback(
                            CameraXExecutors.directExecutor(), callback);
                    return "waitForCaptureResult";
                }
        );
    }
}
