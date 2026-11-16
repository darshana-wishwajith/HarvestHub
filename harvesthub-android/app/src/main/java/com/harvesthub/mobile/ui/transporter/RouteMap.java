package com.harvesthub.mobile.ui.transporter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Tracking;

/** Google map in the scaffold's map area (Figma "Pickup Route Map", in-transit map). */
final class RouteMap {

    private final AppCompatActivity activity;
    private GoogleMap map;
    private Tracking pending;
    private boolean toDrop;

    RouteMap(AppCompatActivity activity) {
        this.activity = activity;
        activity.findViewById(R.id.tpMapHost).setVisibility(View.VISIBLE);
        SupportMapFragment f = SupportMapFragment.newInstance();
        activity.getSupportFragmentManager().beginTransaction().replace(R.id.tpMapHost, f).commitNow();
        f.getMapAsync(m -> {
            map = m;
            map.getUiSettings().setZoomControlsEnabled(true);
            map.getUiSettings().setMapToolbarEnabled(false);
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                try { map.setMyLocationEnabled(true); } catch (SecurityException ignored) { }
            }
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(7.8731, 80.7718), 7f));
            if (pending != null) show(pending, toDrop);
        });
    }

    /** Pickup and drop-off pins, the vehicle's last GPS point, and a line for the current leg. */
    void show(Tracking t, boolean legToDrop) {
        pending = t;
        toDrop = legToDrop;
        if (map == null || t == null) return;
        map.clear();
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        int n = 0;
        LatLng from = t.pickupLatitude == null ? null : new LatLng(t.pickupLatitude, t.pickupLongitude);
        LatLng to = t.dropLatitude == null ? null : new LatLng(t.dropLatitude, t.dropLongitude);
        LatLng now = t.currentLatitude == null ? null : new LatLng(t.currentLatitude, t.currentLongitude);
        if (from != null) {
            map.addMarker(new MarkerOptions().position(from).title("Pickup: " + (t.pickupAddress == null ? "farm" : t.pickupAddress))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            b.include(from);
            n++;
        }
        if (to != null && legToDrop) {
            map.addMarker(new MarkerOptions().position(to).title("Drop-off: " + (t.dropAddress == null ? "buyer" : t.dropAddress)));
            b.include(to);
            n++;
        }
        if (now != null) {
            map.addMarker(new MarkerOptions().position(now).title("Last GPS point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            b.include(now);
            n++;
        }
        LatLng target = legToDrop ? to : from;
        if (now != null && target != null) {
            map.addPolyline(new PolylineOptions().color(0xFF1B4332).width(8f).add(now, target));
        } else if (from != null && to != null && legToDrop) {
            map.addPolyline(new PolylineOptions().color(0xFF1B4332).width(8f).add(from, to));
        }
        try {
            if (n >= 2) map.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 80));
            else if (n == 1) map.animateCamera(CameraUpdateFactory.newLatLngZoom(from != null ? from : to != null ? to : now, 13f));
        } catch (IllegalStateException ignored) { } // map not laid out yet
    }

    /** Opens turn-by-turn navigation in Google Maps (the app itself does not draw turn instructions). */
    static void navigate(AppCompatActivity a, Double lat, Double lng) {
        if (lat == null || lng == null) {
            Toast.makeText(a, "No GPS point for this stop.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + lat + "," + lng));
        i.setPackage("com.google.android.apps.maps");
        try {
            a.startActivity(i);
        } catch (Exception e) {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng)));
        }
    }
}
