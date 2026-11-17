package com.harvesthub.mobile.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.NotificationItem;
import com.harvesthub.mobile.ui.farmer.NotificationAdapter;
import com.harvesthub.mobile.ui.farmer.NotificationsActivity;

import java.util.ArrayList;
import java.util.List;

public class NotificationPanelHelper {

    private static PopupWindow notificationPopup;

    public static void showNotificationPanel(Context context, View anchor) {
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.dismiss();
            return;
        }

        View popupView = LayoutInflater.from(context).inflate(R.layout.layout_notification_panel, null);
        
        notificationPopup = new PopupWindow(popupView, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                true);

        notificationPopup.setOutsideTouchable(true);
        notificationPopup.setFocusable(true);
        notificationPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        notificationPopup.setElevation(24f);
        notificationPopup.setAnimationStyle(R.style.PopupAnimation);

        RecyclerView rv = popupView.findViewById(R.id.rvNotifications);
        rv.setLayoutManager(new LinearLayoutManager(context));
        
        // the signed-in user's real notifications (latest 5); the full list is under "View all"
        List<NotificationItem> items = new ArrayList<>();
        NotificationAdapter adapter = new NotificationAdapter(items, (notification, position) -> {
            if (!notification.isRead()) {
                notification.setRead(true);
                com.harvesthub.mobile.network.ApiClient.getApiService().markNotificationRead(notification.getId())
                        .enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                            @Override public void onResponse(retrofit2.Call<okhttp3.ResponseBody> c, retrofit2.Response<okhttp3.ResponseBody> r) { }
                            @Override public void onFailure(retrofit2.Call<okhttp3.ResponseBody> c, Throwable t) { }
                        });
            }
            if (rv.getAdapter() != null) {
                rv.getAdapter().notifyItemChanged(position);
            }
        });
        rv.setAdapter(adapter);
        String me = new SessionManager(context).getUserId();
        if (me != null) {
            com.harvesthub.mobile.network.ApiClient.getApiService().getNotifications(me)
                    .enqueue(new retrofit2.Callback<List<NotificationItem>>() {
                        @Override
                        public void onResponse(retrofit2.Call<List<NotificationItem>> c, retrofit2.Response<List<NotificationItem>> r) {
                            if (!r.isSuccessful() || r.body() == null) return;
                            items.clear();
                            for (int i = 0; i < Math.min(5, r.body().size()); i++) items.add(r.body().get(i));
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailure(retrofit2.Call<List<NotificationItem>> c, Throwable t) { }
                    });
        }

        popupView.findViewById(R.id.tvViewAll).setOnClickListener(v -> {
            notificationPopup.dismiss();
            Intent intent = new Intent(context, NotificationsActivity.class);
            intent.putExtra(NotificationsActivity.EXTRA_HIDE_NAV, true);
            context.startActivity(intent);
        });

        popupView.findViewById(R.id.tvMarkAllRead).setOnClickListener(v -> {
            for (NotificationItem item : items) {
                item.setRead(true);
            }
            adapter.notifyDataSetChanged();
            if (me != null) {
                com.harvesthub.mobile.network.ApiClient.getApiService().markAllNotificationsRead(me)
                        .enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                            @Override public void onResponse(retrofit2.Call<okhttp3.ResponseBody> c, retrofit2.Response<okhttp3.ResponseBody> r) { }
                            @Override public void onFailure(retrofit2.Call<okhttp3.ResponseBody> c, Throwable t) { }
                        });
            }
        });

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int xOffset = -(popupView.getMeasuredWidth() - anchor.getWidth() + 24);
        notificationPopup.showAsDropDown(anchor, xOffset, 12);
    }
}
