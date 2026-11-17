package com.harvesthub.mobile.ui.common;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Photo that fits the screen and can be pinch-zoomed (up to 5x), dragged when zoomed, and double-tapped to
 * zoom in / back out. While zoomed it keeps the swipe for itself, so the pager only changes page at 1x.
 */
public class ZoomableImageView extends AppCompatImageView {

    private static final float MAX_ZOOM = 5f;

    private final Matrix base = new Matrix();      // fit-centre
    private final Matrix user = new Matrix();      // zoom + pan on top
    private final Matrix draw = new Matrix();
    private float zoom = 1f;
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    public ZoomableImageView(Context context) {
        this(context, null);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                float target = Math.max(1f, Math.min(MAX_ZOOM, zoom * d.getScaleFactor()));
                float f = target / zoom;
                zoom = target;
                user.postScale(f, f, d.getFocusX(), d.getFocusY());
                clampAndApply();
                return true;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                if (zoom <= 1f) return false;
                user.postTranslate(-dx, -dy);
                clampAndApply();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (zoom > 1f) {
                    reset();
                } else {
                    zoom = 2.5f;
                    user.setScale(zoom, zoom, e.getX(), e.getY());
                    clampAndApply();
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return performClick();
            }
        });
    }

    public void reset() {
        zoom = 1f;
        user.reset();
        clampAndApply();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        zoom = 1f;
        user.reset();
        computeBase();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeBase();
    }

    private void computeBase() {
        Drawable d = getDrawable();
        if (d == null || getWidth() == 0 || getHeight() == 0 || d.getIntrinsicWidth() <= 0) return;
        base.setRectToRect(new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight()),
                new RectF(0, 0, getWidth(), getHeight()), Matrix.ScaleToFit.CENTER);
        clampAndApply();
    }

    /** Keeps the zoomed photo covering the screen (no empty gaps while panning). */
    private void clampAndApply() {
        Drawable d = getDrawable();
        draw.set(base);
        draw.postConcat(user);
        if (d != null) {
            RectF r = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            draw.mapRect(r);
            float dx = 0, dy = 0;
            if (r.width() <= getWidth()) dx = (getWidth() - r.width()) / 2 - r.left;
            else if (r.left > 0) dx = -r.left;
            else if (r.right < getWidth()) dx = getWidth() - r.right;
            if (r.height() <= getHeight()) dy = (getHeight() - r.height()) / 2 - r.top;
            else if (r.top > 0) dy = -r.top;
            else if (r.bottom < getHeight()) dy = getHeight() - r.bottom;
            user.postTranslate(dx, dy);
            draw.set(base);
            draw.postConcat(user);
        }
        setImageMatrix(draw);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        // while zoomed (or pinching), don't let the pager steal the gesture
        getParent().requestDisallowInterceptTouchEvent(zoom > 1f || event.getPointerCount() > 1);
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
