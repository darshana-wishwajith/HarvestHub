package com.harvesthub.mobile.ui.common;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.harvesthub.mobile.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-screen view of a listing's captured media (farmer and buyer): swipe between the wide-angle photo, the
 * close-up photo and the 10-second video. Photos pinch/double-tap zoom; the video streams with play/seek controls.
 */
public class MediaViewerActivity extends AppCompatActivity {

    public static final String EXTRA_URLS = "mediaUrls";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_START = "start";

    private static final String[] CAPTIONS = {"Wide angle – cultivated area", "Close-up – harvested sample", "10-second field video"};

    private final List<String> urls = new ArrayList<>();
    private ViewPager2 pager;
    private TextView tvCounter, tvCaption;
    private LinearLayout thumbs;
    private VideoView activeVideo;

    /** Opens the viewer for "/uploads/..." paths (or full URLs). Does nothing if there is no media. */
    public static void open(Context context, List<String> mediaUrls, String title, int start) {
        if (mediaUrls == null || mediaUrls.isEmpty()) return;
        context.startActivity(new Intent(context, MediaViewerActivity.class)
                .putStringArrayListExtra(EXTRA_URLS, new ArrayList<>(mediaUrls))
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_START, start));
    }

    public static String absolute(String path) {
        if (path == null || path.startsWith("http")) return path;
        String base = com.harvesthub.mobile.BuildConfig.API_BASE_URL;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + (path.startsWith("/") ? path : "/" + path);
    }

    private static boolean isVideo(String url) {
        String u = url.toLowerCase(Locale.ROOT);
        return u.endsWith(".mp4") || u.endsWith(".3gp") || u.endsWith(".webm");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_viewer);
        getWindow().setStatusBarColor(Color.BLACK);
        ArrayList<String> in = getIntent().getStringArrayListExtra(EXTRA_URLS);
        if (in != null) for (String u : in) if (u != null && !u.isEmpty()) urls.add(u);
        if (urls.isEmpty()) {
            finish();
            return;
        }
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        ((TextView) findViewById(R.id.tvMediaTitle)).setText(title != null ? title : "Harvest media");
        tvCounter = findViewById(R.id.tvMediaCounter);
        tvCaption = findViewById(R.id.tvMediaCaption);
        thumbs = findViewById(R.id.layoutMediaThumbs);
        findViewById(R.id.btnMediaClose).setOnClickListener(v -> finish());

        pager = findViewById(R.id.pagerMedia);
        pager.setAdapter(new PageAdapter());
        pager.setOffscreenPageLimit(1);
        buildThumbs();
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                stopVideo();
                showPosition(position);
            }
        });
        int start = Math.max(0, Math.min(urls.size() - 1, getIntent().getIntExtra(EXTRA_START, 0)));
        pager.setCurrentItem(start, false);
        showPosition(start);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (activeVideo != null) activeVideo.pause();
    }

    private void stopVideo() {
        if (activeVideo != null) {
            activeVideo.stopPlayback();
            activeVideo = null;
        }
    }

    private String caption(int i) {
        String url = urls.get(i);
        if (urls.size() == 3 && i < CAPTIONS.length && isVideo(url) == (i == 2)) return CAPTIONS[i];
        return isVideo(url) ? "Video" : "Photo " + (i + 1);
    }

    private void showPosition(int position) {
        tvCounter.setText((position + 1) + " of " + urls.size());
        tvCaption.setText(caption(position));
        for (int i = 0; i < thumbs.getChildCount(); i++) {
            View t = thumbs.getChildAt(i);
            t.setAlpha(i == position ? 1f : 0.5f);
            t.setBackgroundResource(i == position ? R.drawable.bg_media_thumb_selected : 0);
        }
    }

    private void buildThumbs() {
        int size = Math.round(56 * getResources().getDisplayMetrics().density);
        int pad = Math.round(2 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < urls.size(); i++) {
            FrameLayout box = new FrameLayout(this);
            box.setPadding(pad, pad, pad, pad);
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            box.addView(iv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            String url = absolute(urls.get(i));
            Glide.with(this).load(url).centerCrop().into(iv);   // video: Glide shows a frame
            if (isVideo(url)) {
                ImageView play = new ImageView(this);
                play.setImageResource(R.drawable.ic_play_badge);
                box.addView(play, new FrameLayout.LayoutParams(size / 2, size / 2, Gravity.CENTER));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(Math.round(10 * getResources().getDisplayMetrics().density));
            final int index = i;
            box.setOnClickListener(v -> pager.setCurrentItem(index, true));
            thumbs.addView(box, lp);
        }
    }

    // ---------------------------------------------------------------- pages

    private class PageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return isVideo(urls.get(position)) ? 1 : 0;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout frame = new FrameLayout(parent.getContext());
            frame.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ProgressBar spinner = new ProgressBar(parent.getContext());
            spinner.setId(R.id.mediaSpinner);
            if (viewType == 0) {
                ZoomableImageView photo = new ZoomableImageView(parent.getContext());
                photo.setId(R.id.mediaPhoto);
                frame.addView(photo, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            } else {
                VideoView video = new VideoView(parent.getContext());
                video.setId(R.id.mediaVideo);
                frame.addView(video, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
                ImageView play = new ImageView(parent.getContext());
                play.setId(R.id.mediaPlay);
                play.setImageResource(R.drawable.ic_play_badge);
                int s = Math.round(72 * parent.getResources().getDisplayMetrics().density);
                frame.addView(play, new FrameLayout.LayoutParams(s, s, Gravity.CENTER));
            }
            frame.addView(spinner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            return new RecyclerView.ViewHolder(frame) { };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
            String url = absolute(urls.get(position));
            View spinner = h.itemView.findViewById(R.id.mediaSpinner);
            if (getItemViewType(position) == 0) {
                ZoomableImageView photo = h.itemView.findViewById(R.id.mediaPhoto);
                photo.reset();
                spinner.setVisibility(View.VISIBLE);
                Glide.with(photo).load(url)
                        .override(2400, 2400)   // sharp enough to zoom into the crop
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object m,
                                                        com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> t, boolean f) {
                                spinner.setVisibility(View.GONE);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(@NonNull android.graphics.drawable.Drawable r, @NonNull Object m,
                                                           com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> t,
                                                           @NonNull com.bumptech.glide.load.DataSource s, boolean f) {
                                spinner.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .error(R.drawable.ic_leaf_outline)
                        .into(photo);
                return;
            }
            VideoView video = h.itemView.findViewById(R.id.mediaVideo);
            View play = h.itemView.findViewById(R.id.mediaPlay);
            spinner.setVisibility(View.GONE);
            play.setVisibility(View.VISIBLE);
            play.setOnClickListener(v -> {
                stopVideo();
                activeVideo = video;
                play.setVisibility(View.GONE);
                spinner.setVisibility(View.VISIBLE);
                MediaController controls = new MediaController(MediaViewerActivity.this);
                controls.setAnchorView(video);
                video.setMediaController(controls);
                video.setVideoURI(Uri.parse(url));
                video.setOnPreparedListener(mp -> {
                    spinner.setVisibility(View.GONE);
                    video.start();
                    controls.show(3000);
                });
                video.setOnCompletionListener(mp -> play.setVisibility(View.VISIBLE));
                video.setOnErrorListener((mp, what, extra) -> {
                    spinner.setVisibility(View.GONE);
                    play.setVisibility(View.VISIBLE);
                    android.widget.Toast.makeText(MediaViewerActivity.this, "The video could not be played. Check your internet.",
                            android.widget.Toast.LENGTH_LONG).show();
                    return true;
                });
            });
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }
    }
}
