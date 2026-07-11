package com.harvesthub.mobile.ui.farmer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MarketTrendChartView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointHolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private List<Double> trendValues = new ArrayList<>(Arrays.asList(
            122.0, 124.0, 121.0, 128.0, 152.0, 162.0, 184.0,
            174.0, 146.0, 154.0, 176.0, 168.0, 188.0, 214.0
    ));

    public MarketTrendChartView(Context context) {
        super(context);
        init();
    }

    public MarketTrendChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MarketTrendChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setColor(Color.parseColor("#13A34A"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeWidth(dp(3));

        pointPaint.setColor(Color.parseColor("#13A34A"));
        pointPaint.setStyle(Paint.Style.FILL);

        pointHolePaint.setColor(Color.WHITE);
        pointHolePaint.setStyle(Paint.Style.FILL);
    }

    public void setTrendValues(List<Double> values) {
        if (values == null || values.size() < 2) {
            return;
        }
        trendValues = new ArrayList<>(values);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (trendValues.size() < 2) {
            return;
        }

        float width = getWidth();
        float height = getHeight();
        float top = dp(12);
        float bottom = height - dp(18);
        float left = dp(10);
        float right = width - dp(10);

        double min = trendValues.get(0);
        double max = trendValues.get(0);
        for (Double value : trendValues) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (Math.abs(max - min) < 0.01) {
            max = min + 1;
        }

        int count = trendValues.size();
        float[] xs = new float[count];
        float[] ys = new float[count];
        for (int i = 0; i < count; i++) {
            float progress = count == 1 ? 0 : (float) i / (float) (count - 1);
            xs[i] = left + progress * (right - left);
            float normalized = (float) ((trendValues.get(i) - min) / (max - min));
            ys[i] = bottom - normalized * (bottom - top);
        }

        buildSmoothPath(xs, ys, linePath);

        fillPath.reset();
        fillPath.addPath(linePath);
        fillPath.lineTo(xs[count - 1], bottom);
        fillPath.lineTo(xs[0], bottom);
        fillPath.close();

        fillPaint.setShader(new LinearGradient(
                0, top, 0, bottom,
                Color.parseColor("#3AE873"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        ));
        fillPaint.setAlpha(48);
        canvas.drawPath(fillPath, fillPaint);
        fillPaint.setShader(null);
        fillPaint.setAlpha(255);

        canvas.drawPath(linePath, linePaint);

        int highlightIndex = Math.min(6, count - 1);
        canvas.drawCircle(xs[highlightIndex], ys[highlightIndex], dp(4.6f), pointPaint);
        canvas.drawCircle(xs[highlightIndex], ys[highlightIndex], dp(1.8f), pointHolePaint);
    }

    private void buildSmoothPath(float[] xs, float[] ys, Path path) {
        path.reset();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2f;
            float midY = (ys[i - 1] + ys[i]) / 2f;
            path.quadTo(xs[i - 1], ys[i - 1], midX, midY);
        }
        int last = xs.length - 1;
        path.quadTo(xs[last - 1], ys[last - 1], xs[last], ys[last]);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
