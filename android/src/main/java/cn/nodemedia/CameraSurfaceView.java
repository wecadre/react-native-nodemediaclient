package cn.nodemedia;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Size;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.utils.futures.Futures;

import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

@SuppressLint("RestrictedApi")
final class CameraSurfaceView {

    private static final String TAG = "SurfaceViewImpl";

    @Nullable
    Size mResolution;

    @NonNull
    FrameLayout mParent;

    @NonNull
    private final PreviewTransformation mPreviewTransform;

    private boolean mWasSurfaceProvided = false;


    SurfaceView mSurfaceView;

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    final CameraSurfaceView.SurfaceRequestCallback mSurfaceRequestCallback = new CameraSurfaceView.SurfaceRequestCallback();

    @Nullable
    private OnSurfaceNotInUseListener mOnSurfaceNotInUseListener;

    CameraSurfaceView(@NonNull FrameLayout parent,
                      @NonNull PreviewTransformation previewTransform) {


        mParent = parent;
        mPreviewTransform = previewTransform;
    }

    void onSurfaceRequested(@NonNull SurfaceRequest surfaceRequest,
                            @Nullable OnSurfaceNotInUseListener onSurfaceNotInUseListener) {
        mResolution = surfaceRequest.getResolution();
        mOnSurfaceNotInUseListener = onSurfaceNotInUseListener;
        initializePreview();
        surfaceRequest.addRequestCancellationListener(
                ContextCompat.getMainExecutor(mSurfaceView.getContext()),
                this::notifySurfaceNotInUse);
        mSurfaceView.post(() -> mSurfaceRequestCallback.setSurfaceRequest(surfaceRequest));
    }

    void redrawPreview() {
        View preview = getPreview();
        // Only calls setScaleX/Y and setTranslationX/Y after the surface has been provided.
        // Otherwise, it might cause some preview stretched issue when using PERFORMANCE mode
        // together with Compose UI. For more details, please see b/183864890.
        if (preview == null || !mWasSurfaceProvided) {
            return;
        }
        mPreviewTransform.transformView(new Size(mParent.getWidth(),
                mParent.getHeight()), mParent.getLayoutDirection(), preview);
    }
    void onSurfaceProvided() {
        mWasSurfaceProvided = true;
        redrawPreview();
    }

    interface OnSurfaceNotInUseListener {
        void onSurfaceNotInUse();
    }

    @Nullable
    Bitmap getBitmap() {
        final Bitmap bitmap = getPreviewBitmap();
        if (bitmap == null) {
            return null;
        }
        return mPreviewTransform.createTransformedBitmap(bitmap,
                new Size(mParent.getWidth(), mParent.getHeight()),
                mParent.getLayoutDirection());
    }



    void initializePreview() {
        Preconditions.checkNotNull(mParent);
        Preconditions.checkNotNull(mResolution);

        mSurfaceView = new SurfaceView(mParent.getContext());
        mSurfaceView.setLayoutParams(
                new FrameLayout.LayoutParams(mResolution.getWidth(), mResolution.getHeight()));
        mParent.removeAllViews();
        mParent.addView(mSurfaceView);
        mSurfaceView.getHolder().addCallback(mSurfaceRequestCallback);
    }

    @Nullable

    View getPreview() {
        return mSurfaceView;
    }


    void onAttachedToWindow() {
        // Do nothing currently.
    }


    void onDetachedFromWindow() {
        // Do nothing currently.
    }

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    void notifySurfaceNotInUse() {
        if (mOnSurfaceNotInUseListener != null) {
            mOnSurfaceNotInUseListener.onSurfaceNotInUse();
            mOnSurfaceNotInUseListener = null;
        }
    }

    /**
     * Getting a Bitmap from a Surface is achieved using the `PixelCopy#request()` API, which
     * would introduced in API level 24. PreviewView doesn't currently use a SurfaceView on API
     * levels below 24.
     */
    @RequiresApi(24)
    @Nullable
    Bitmap getPreviewBitmap() {
        // If the preview surface isn't ready yet or isn't valid, return null
        if (mSurfaceView == null || mSurfaceView.getHolder().getSurface() == null
                || !mSurfaceView.getHolder().getSurface().isValid()) {
            return null;
        }

        // Copy display contents of the surfaceView's surface into a Bitmap.
        final Bitmap bitmap = Bitmap.createBitmap(mSurfaceView.getWidth(), mSurfaceView.getHeight(),
                Bitmap.Config.ARGB_8888);
        CameraSurfaceView.Api24Impl.pixelCopyRequest(mSurfaceView, bitmap, copyResult -> {
            if (copyResult == PixelCopy.SUCCESS) {
                Logger.d(TAG, "PreviewView.SurfaceViewImplementation.getBitmap() succeeded");
            } else {
                Logger.e(TAG, "PreviewView.SurfaceViewImplementation.getBitmap() failed with error "
                        + copyResult);
            }
        }, mSurfaceView.getHandler());

        return bitmap;
    }


    class SurfaceRequestCallback implements SurfaceHolder.Callback {

        // Target Surface size. Only complete the SurfaceRequest when the size of the Surface
        // matches this value.
        // Guarded by the UI thread.
        @Nullable
        private Size mTargetSize;

        // SurfaceRequest to set when the target size is met.
        // Guarded by the UI thread.
        @Nullable
        private SurfaceRequest mSurfaceRequest;

        // The cached size of the current Surface.
        // Guarded by the UI thread.
        @Nullable
        private Size mCurrentSurfaceSize;

        // Guarded by the UI thread.
        private boolean mWasSurfaceProvided = false;


        @UiThread
        void setSurfaceRequest(@NonNull SurfaceRequest surfaceRequest) {
            // Cancel the previous request, if any
            cancelPreviousRequest();

            mSurfaceRequest = surfaceRequest;
            Size targetSize = surfaceRequest.getResolution();
            mTargetSize = targetSize;
            mWasSurfaceProvided = false;

            if (!tryToComplete()) {
                // The current size is incorrect. Wait for it to change.
                Logger.d(TAG, "Wait for new Surface creation.");
                mSurfaceView.getHolder().setFixedSize(targetSize.getWidth(),
                        targetSize.getHeight());
            }
        }


        @UiThread
        private boolean tryToComplete() {
            final Surface surface = mSurfaceView.getHolder().getSurface();
            if (canProvideSurface()) {
                Logger.d(TAG, "Surface set on Preview.");
                mSurfaceRequest.provideSurface(surface,
                        ContextCompat.getMainExecutor(mSurfaceView.getContext()),
                        (result) -> {
                            Logger.d(TAG, "Safe to release surface.");
                            notifySurfaceNotInUse();
                        });
                mWasSurfaceProvided = true;
                onSurfaceProvided();
                return true;
            }
            return false;
        }

        private boolean canProvideSurface() {
            return !mWasSurfaceProvided && mSurfaceRequest != null && mTargetSize != null
                    && mTargetSize.equals(mCurrentSurfaceSize);
        }

        @UiThread
        @SuppressWarnings("ObjectToString")
        private void cancelPreviousRequest() {
            if (mSurfaceRequest != null) {
                Logger.d(TAG, "Request canceled: " + mSurfaceRequest);
                mSurfaceRequest.willNotProvideSurface();
            }
        }


        @UiThread
        @SuppressWarnings("ObjectToString")
        private void invalidateSurface() {
            if (mSurfaceRequest != null) {
                Logger.d(TAG, "Surface invalidated " + mSurfaceRequest);
                mSurfaceRequest.getDeferrableSurface().close();
            }
        }

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
            Logger.d(TAG, "Surface created.");
            // No-op. Handling surfaceChanged() is enough because it's always called afterwards.
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width,
                                   int height) {
            Logger.d(TAG, "Surface changed. Size: " + width + "x" + height);
            mCurrentSurfaceSize = new Size(width, height);
            tryToComplete();
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            Logger.d(TAG, "Surface destroyed.");

            // If a surface was already provided to the camera, invalidate it so that it requests
            // a new valid one. Otherwise, cancel the surface request.
            if (mWasSurfaceProvided) {
                invalidateSurface();
            } else {
                cancelPreviousRequest();
            }

            // Reset state
            mWasSurfaceProvided = false;
            mSurfaceRequest = null;
            mCurrentSurfaceSize = null;
            mTargetSize = null;
        }
    }


    @NonNull
    ListenableFuture<Void> waitForNextFrame() {
        return Futures.immediateFuture(null);
    }


    @RequiresApi(24)
    private static class Api24Impl {

        private Api24Impl() {
        }

        @DoNotInline
        static void pixelCopyRequest(@NonNull SurfaceView source, @NonNull Bitmap dest,
                                     @NonNull PixelCopy.OnPixelCopyFinishedListener listener, @NonNull Handler handler) {
            PixelCopy.request(source, dest, listener, handler);
        }
    }
}
