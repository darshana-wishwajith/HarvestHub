package com.harvesthub.mobile.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.harvesthub.mobile.BuildConfig;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;

import java.io.File;

/**
 * Shared OpenStreetMap (osmdroid) set-up for every map screen.
 * Tiles come from the public OSM server, which asks for a real app User-Agent and the
 * "© OpenStreetMap contributors" credit on the map. Tiles are cached in the app's cache folder,
 * so no storage permission is needed.
 */
public final class OsmMaps {

    private OsmMaps() {
    }

    /** Call once before inflating a layout that contains a MapView. */
    public static void init(Context context) {
        Context app = context.getApplicationContext();
        IConfigurationProvider cfg = Configuration.getInstance();
        cfg.load(app, app.getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        cfg.setUserAgentValue(BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME);
        File base = new File(app.getCacheDir(), "osmdroid");
        cfg.setOsmdroidBasePath(base);
        cfg.setOsmdroidTileCache(new File(base, "tiles"));
    }

    /** Blue "you are here" dot with a white ring (osmdroid's default is a yellow person icon). */
    public static Bitmap locationDot(Context context) {
        float d = context.getResources().getDisplayMetrics().density;
        int size = Math.round(20 * d);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        c.drawCircle(size / 2f, size / 2f, size / 2f, p);
        p.setColor(Color.parseColor("#1A73E8"));
        c.drawCircle(size / 2f, size / 2f, size / 2f - 3 * d, p);
        return b;
    }

    /** Standard map: OSM street tiles, pinch zoom, no +/- buttons, credit line. */
    public static void setUp(MapView map) {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.setMinZoomLevel(6.0);
        map.setMaxZoomLevel(19.0);
        map.setTilesScaledToDpi(true);
        CopyrightOverlay credit = new CopyrightOverlay(map.getContext());
        credit.setAlignRight(false);
        credit.setTextSize(10);
        map.getOverlays().add(credit);
    }
}
