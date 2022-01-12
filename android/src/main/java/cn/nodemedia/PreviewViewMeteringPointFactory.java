package cn.nodemedia;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Size;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.impl.utils.Threads;


class PreviewViewMeteringPointFactory extends MeteringPointFactory {

    static final PointF INVALID_POINT = new PointF(2F, 2F);

    @NonNull
    private final PreviewTransformation mPreviewTransformation;

    @GuardedBy("this")
    @Nullable
    private Matrix mMatrix;

    PreviewViewMeteringPointFactory(@NonNull PreviewTransformation previewTransformation) {
        mPreviewTransformation = previewTransformation;
    }

    @AnyThread
    @NonNull
    @Override
    protected PointF convertPoint(float x, float y) {
        float[] point = new float[]{x, y};
        synchronized (this) {
            if (mMatrix == null) {
                return INVALID_POINT;
            }
            mMatrix.mapPoints(point);
        }
        return new PointF(point[0], point[1]);
    }

    @UiThread
    void recalculate(@NonNull Size previewViewSize, int layoutDirection) {
        Threads.checkMainThread();
        synchronized (this) {
            if (previewViewSize.getWidth() == 0 || previewViewSize.getHeight() == 0) {
                mMatrix = null;
                return;
            }
            mMatrix = mPreviewTransformation.getPreviewViewToNormalizedSurfaceMatrix(
                    previewViewSize,
                    layoutDirection);
        }
    }
}
