package com.harvesthub.mobile.ui.farmer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class DeliveryMapView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path roadPath = new Path();
    private final Path routePath = new Path();

    public DeliveryMapView(Context context) {
        super(context);
    }

    public DeliveryMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DeliveryMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float d = getResources().getDisplayMetrics().density;
        float s = getResources().getDisplayMetrics().scaledDensity;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#F4FBF8"));
        canvas.drawRect(0, 0, w, h, paint);

        drawSoftFields(canvas, w, h);
        drawRoads(canvas, w, h, d);
        drawRoute(canvas, w, h, d);
        drawLabels(canvas, w, h, s);
        drawTruck(canvas, w, h, d);
        drawLiveBadge(canvas, w, d, s);
    }

    private void drawSoftFields(Canvas canvas, float w, float h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#E3EFEC"));

        Path field = new Path();
        field.moveTo(w * 0.43f, 0);
        field.cubicTo(w * 0.55f, h * 0.16f, w * 0.33f, h * 0.26f, w * 0.40f, h * 0.42f);
        field.cubicTo(w * 0.55f, h * 0.53f, w * 0.72f, h * 0.38f, w, h * 0.50f);
        field.lineTo(w, 0);
        field.close();
        canvas.drawPath(field, paint);

        field.reset();
        field.moveTo(0, h * 0.64f);
        field.cubicTo(w * 0.20f, h * 0.57f, w * 0.25f, h * 0.78f, w * 0.45f, h * 0.78f);
        field.cubicTo(w * 0.62f, h * 0.78f, w * 0.74f, h * 0.66f, w, h * 0.72f);
        field.lineTo(w, h);
        field.lineTo(0, h);
        field.close();
        canvas.drawPath(field, paint);
    }

    private void drawRoads(Canvas canvas, float w, float h, float d) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(2.6f * d);
        paint.setColor(Color.parseColor("#D7E1DE"));

        roadPath.reset();
        roadPath.moveTo(0, h * 0.22f);
        roadPath.cubicTo(w * 0.20f, h * 0.30f, w * 0.16f, h * 0.54f, w * 0.36f, h * 0.58f);
        roadPath.cubicTo(w * 0.54f, h * 0.62f, w * 0.57f, h * 0.35f, w * 0.78f, h * 0.46f);
        roadPath.cubicTo(w * 0.90f, h * 0.52f, w * 0.94f, h * 0.42f, w, h * 0.38f);
        canvas.drawPath(roadPath, paint);

        roadPath.reset();
        roadPath.moveTo(w * 0.31f, 0);
        roadPath.cubicTo(w * 0.35f, h * 0.18f, w * 0.16f, h * 0.32f, w * 0.23f, h * 0.47f);
        roadPath.cubicTo(w * 0.30f, h * 0.61f, w * 0.48f, h * 0.58f, w * 0.42f, h);
        canvas.drawPath(roadPath, paint);
    }

    private void drawRoute(Canvas canvas, float w, float h, float d) {
        paint.setStrokeWidth(3.6f * d);
        paint.setColor(Color.parseColor("#A4D2A9"));

        routePath.reset();
        routePath.moveTo(w * 0.24f, h * 0.70f);
        routePath.cubicTo(w * 0.32f, h * 0.54f, w * 0.40f, h * 0.50f, w * 0.47f, h * 0.44f);
        routePath.cubicTo(w * 0.59f, h * 0.35f, w * 0.68f, h * 0.54f, w * 0.83f, h * 0.65f);
        canvas.drawPath(routePath, paint);
    }

    private void drawLabels(Canvas canvas, float w, float h, float s) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));

        paint.setColor(Color.parseColor("#073B27"));
        paint.setTextSize(15f * s);
        canvas.drawText("HarvestHub", w * 0.05f, h * 0.17f, paint);

        paint.setColor(Color.parseColor("#254238"));
        paint.setTextSize(10.5f * s);
        canvas.drawText("MIHINTALE", w * 0.15f, h * 0.50f, paint);
        canvas.drawText("TOWN", w * 0.19f, h * 0.57f, paint);
        canvas.drawText("FARM", w * 0.78f, h * 0.86f, paint);
    }

    private void drawTruck(Canvas canvas, float w, float h, float d) {
        canvas.save();
        canvas.translate(w * 0.57f, h * 0.47f);
        canvas.rotate(25);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#174D36"));
        canvas.drawRoundRect(new RectF(-7f * d, -4.5f * d, 5.5f * d, 4f * d), 1.5f * d, 1.5f * d, paint);
        canvas.drawRect(5.5f * d, -2f * d, 10.5f * d, 4f * d, paint);

        paint.setColor(Color.WHITE);
        canvas.drawCircle(-4f * d, 5f * d, 1.8f * d, paint);
        canvas.drawCircle(6.5f * d, 5f * d, 1.8f * d, paint);
        canvas.restore();
    }

    private void drawLiveBadge(Canvas canvas, float w, float d, float s) {
        float badgeWidth = 132f * d;
        float badgeHeight = 28f * d;
        float left = w - badgeWidth - (14f * d);
        float top = 15f * d;
        RectF badge = new RectF(left, top, left + badgeWidth, top + badgeHeight);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(3f * d, 0, 1f * d, Color.parseColor("#22000000"));
        canvas.drawRoundRect(badge, 5f * d, 5f * d, paint);
        paint.clearShadowLayer();

        paint.setColor(Color.parseColor("#56D179"));
        canvas.drawCircle(left + 14f * d, top + 14f * d, 5.5f * d, paint);

        paint.setColor(Color.parseColor("#58665F"));
        paint.setTextSize(10.5f * s);
        paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        canvas.drawText("Live Tracking Active", left + 26f * d, top + 18f * d, paint);
    }
}
