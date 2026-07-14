package com.harvesthub.mobile.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

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

        RecyclerView rv = popupView.findViewById(R.id.rvNotifications);
        rv.setLayoutManager(new LinearLayoutManager(context));
        
        // Use dummy data for demo, in real app this would fetch from a ViewModel or shared state
        List<NotificationItem> dummyData = new ArrayList<>();
        dummyData.add(new NotificationItem("1", "DELIVERY_SUCCESS", "Delivery Successful", "LKR 140,000 credited to your wallet.", "2 mins ago", false));
        dummyData.add(new NotificationItem("2", "PENALTY", "Penalty Notice", "Small deduction due to moisture levels.", "1 hour ago", true));
        dummyData.add(new NotificationItem("3", "LISTING_APPROVED", "Listing Approved", "Your Carrot listing is now live.", "3 hours ago", true));

        NotificationAdapter adapter = new NotificationAdapter(dummyData, (notification, position) -> {
            notification.setRead(true);
            rv.getAdapter().notifyItemChanged(position);
        });
        rv.setAdapter(adapter);

        popupView.findViewById(R.id.tvViewAll).setOnClickListener(v -> {
            notificationPopup.dismiss();
            Intent intent = new Intent(context, NotificationsActivity.class);
            intent.putExtra(NotificationsActivity.EXTRA_HIDE_NAV, true);
            context.startActivity(intent);
        });

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int xOffset = -(popupView.getMeasuredWidth() - anchor.getWidth() + 24);
        notificationPopup.showAsDropDown(anchor, xOffset, 12);
    }
}
