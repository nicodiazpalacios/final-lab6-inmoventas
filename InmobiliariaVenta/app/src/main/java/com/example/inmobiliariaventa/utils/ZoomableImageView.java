package com.example.inmobiliariaventa.utils;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView {

    private Matrix matrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private PointF last = new PointF();
    private PointF start = new PointF();

    private float minScale = 1f;
    private float maxScale = 5f;
    private float currentScale = 1f;

    private float[] m = new float[9];
    private int viewWidth, viewHeight;
    private int origWidth, origHeight;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public ZoomableImageView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        scaleDetector.setQuickScaleEnabled(false);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        fitImageToView();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        fitImageToView();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        fitImageToView();
        post(this::fitImageToView);
    }

    @Override
    public void setImageBitmap(android.graphics.Bitmap bm) {
        super.setImageBitmap(bm);
        fitImageToView();
        post(this::fitImageToView);
    }

    public void resetZoom() {
        currentScale = 1f;
        mode = NONE;
        fitImageToView();
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
    }

    public float getCurrentScale() {
        return currentScale;
    }

    private void fitImageToView() {
        Drawable drawable = getDrawable();
        if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            return;
        }

        if (viewWidth <= 0 || viewHeight <= 0) {
            viewWidth = getWidth();
            viewHeight = getHeight();
        }
        if (viewWidth <= 0 || viewHeight <= 0) {
            post(this::fitImageToView);
            return;
        }

        origWidth = drawable.getIntrinsicWidth();
        origHeight = drawable.getIntrinsicHeight();

        // Escala FIT_CENTER: la imagen completa entra siempre en la pantalla
        float scaleX = (float) viewWidth / (float) origWidth;
        float scaleY = (float) viewHeight / (float) origHeight;
        float scale = Math.min(scaleX, scaleY);

        matrix.reset();
        matrix.setScale(scale, scale);

        float redundantXSpace = (float) viewWidth - (scale * (float) origWidth);
        float redundantYSpace = (float) viewHeight - (scale * (float) origHeight);
        redundantXSpace /= 2f;
        redundantYSpace /= 2f;

        matrix.postTranslate(redundantXSpace, redundantYSpace);
        currentScale = 1f;
        mode = NONE;
        setImageMatrix(matrix);
        invalidate();
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        if (currentScale <= 1.05f) {
            return false;
        }
        if (origWidth <= 0 || viewWidth <= 0) {
            return false;
        }
        matrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];

        float scaleX = (float) viewWidth / (float) origWidth;
        float scaleY = (float) viewHeight / (float) origHeight;
        float baseScale = Math.min(scaleX, scaleY);
        float totalScale = baseScale * currentScale;
        float contentWidth = origWidth * totalScale;

        if (contentWidth <= viewWidth) {
            return false;
        }

        if (direction < 0) {
            return transX < -1f;
        } else {
            return transX > (viewWidth - contentWidth + 1f);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerCount = event.getPointerCount();

        // Si hay más de un dedo en la pantalla, la escala es mayor a 1.05x,
        // o hay un pellizco de escala en progreso, bloquear interceptación de ViewPager2
        boolean disallowIntercept = (pointerCount > 1)
                || (currentScale > 1.05f)
                || (scaleDetector != null && scaleDetector.isInProgress());

        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(disallowIntercept);
        }

        if (scaleDetector != null) {
            scaleDetector.onTouchEvent(event);
        }
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(event);
        }

        PointF curr = new PointF(event.getX(), event.getY());

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                last.set(curr.x, curr.y);
                start.set(curr.x, curr.y);
                mode = DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                mode = ZOOM;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG && (scaleDetector == null || !scaleDetector.isInProgress())) {
                    if (currentScale > 1.05f) {
                        float deltaX = curr.x - last.x;
                        float deltaY = curr.y - last.y;
                        matrix.postTranslate(deltaX, deltaY);
                        fixTranslation();
                        setImageMatrix(matrix);
                        invalidate();
                    }
                }
                last.set(curr.x, curr.y);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                int actionIndex = event.getActionIndex();
                int remainingIndex = (actionIndex == 0) ? 1 : 0;
                if (pointerCount > remainingIndex) {
                    last.set(event.getX(remainingIndex), event.getY(remainingIndex));
                }
                mode = DRAG;
                break;

            case MotionEvent.ACTION_UP:
                mode = NONE;
                if (currentScale <= 1.05f) {
                    resetZoom();
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mode = NONE;
                break;
        }

        return true;
    }

    private void fixTranslation() {
        matrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];

        Drawable drawable = getDrawable();
        if (drawable == null || origWidth <= 0 || origHeight <= 0) return;

        float scaleX = (float) viewWidth / (float) origWidth;
        float scaleY = (float) viewHeight / (float) origHeight;
        float baseScale = Math.min(scaleX, scaleY);
        float totalScale = baseScale * currentScale;

        float contentWidth = origWidth * totalScale;
        float contentHeight = origHeight * totalScale;

        float fixTransX = getFixTranslation(transX, viewWidth, contentWidth);
        float fixTransY = getFixTranslation(transY, viewHeight, contentHeight);

        if (fixTransX != 0 || fixTransY != 0) {
            matrix.postTranslate(fixTransX, fixTransY);
        }
    }

    private float getFixTranslation(float trans, float viewSize, float contentSize) {
        float minTrans, maxTrans;

        if (contentSize <= viewSize) {
            minTrans = (viewSize - contentSize) / 2f;
            maxTrans = minTrans;
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans) {
            return -trans + minTrans;
        }
        if (trans > maxTrans) {
            return -trans + maxTrans;
        }
        return 0;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = ZOOM;
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float origScale = currentScale;
            currentScale *= scaleFactor;

            if (currentScale > maxScale) {
                currentScale = maxScale;
                scaleFactor = maxScale / origScale;
            } else if (currentScale < minScale) {
                currentScale = minScale;
                scaleFactor = minScale / origScale;
            }

            matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            fixTranslation();
            setImageMatrix(matrix);
            invalidate();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
            if (currentScale < 1.05f) {
                resetZoom();
            }
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (currentScale > 1.2f) {
                resetZoom();
            } else {
                float targetScale = 2.5f;
                float factor = targetScale / currentScale;
                currentScale = targetScale;
                matrix.postScale(factor, factor, e.getX(), e.getY());
                fixTranslation();
                setImageMatrix(matrix);
                invalidate();
            }
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(currentScale > 1.05f);
            }
            return true;
        }
    }
}
