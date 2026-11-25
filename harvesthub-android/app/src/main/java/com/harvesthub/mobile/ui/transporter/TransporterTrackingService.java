package com.harvesthub.mobile.ui.transporter;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.TransporterEventRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Background GPS during trips: a foreground service with a visible notification that sends the
 * phone's position every 30 s as a LOCATION event for each IN_TRANSIT order, from Start Journey until arrival.
 * Stops itself when the driver has no order in transit.
 */
public class TransporterTrackingService extends Service {

    private static final long INTERVAL_MS = 30_000;
    private static final String CHANNEL = "trip_tracking";
    private static final int NOTIFICATION_ID = 4471;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::tick;
    private int idleTicks;

    public static void start(Context ctx) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try {
            ContextCompat.startForegroundService(ctx, new Intent(ctx, TransporterTrackingService.class));
        } catch (Exception ignored) { } // e.g. started from the background on Android 12+
    }

    public static void stop(Context ctx) {
        ctx.stopService(new Intent(ctx, TransporterTrackingService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = notification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
        handler.removeCallbacks(tick);
        tick();
        return START_STICKY;
    }

    private Notification notification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Trip tracking", NotificationManager.IMPORTANCE_LOW));
        }
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, DeliveryDashboardActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_truck)
                .setContentTitle("HarvestHub trip in progress")
                .setContentText("Sharing your location with the buyer every 30 seconds")
                .setOngoing(true)
                .setContentIntent(open)
                .build();
    }

    private void tick() {
        String me = new SessionManager(this).getUserId();
        if (me == null) {
            stopSelf();
            return;
        }
        ApiClient.getApiService().getTransporterOrders(me, "IN_TRANSIT").enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(@NonNull Call<List<Order>> call, @NonNull Response<List<Order>> r) {
                if (r.isSuccessful() && r.body() != null && !r.body().isEmpty()) {
                    idleTicks = 0;
                    send(me, r.body());
                } else if (r.isSuccessful() && ++idleTicks >= 2) {
                    stopSelf(); // no trip in transit any more
                    return;
                }
                handler.postDelayed(tick, INTERVAL_MS);
            }

            @Override
            public void onFailure(@NonNull Call<List<Order>> call, @NonNull Throwable t) {
                handler.postDelayed(tick, INTERVAL_MS); // offline for a moment – keep trying
            }
        });
    }

    private void send(String me, List<Order> orders) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try {
            CurrentLocationRequest req = new CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY).setMaxUpdateAgeMillis(20_000).build();
            LocationServices.getFusedLocationProviderClient(this).getCurrentLocation(req, null).addOnSuccessListener(loc -> {
                if (loc == null) return;
                for (Order o : orders) {
                    TransporterEventRequest e = new TransporterEventRequest(me, "LOCATION", null);
                    e.setLocation(loc.getLatitude(), loc.getLongitude());
                    ApiClient.getApiService().applyTransporterEvent(o.getId(), e).enqueue(new Callback<Order>() {
                        @Override
                        public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) { }

                        @Override
                        public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) { }
                    });
                }
            });
        } catch (SecurityException ignored) { }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
