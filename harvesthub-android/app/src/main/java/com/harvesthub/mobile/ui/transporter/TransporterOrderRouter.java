package com.harvesthub.mobile.ui.transporter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.ui.farmer.OrderFlowRouter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Opens the transporter screen for the order's workflow status (Figma transporter page 612:58). */
public final class TransporterOrderRouter {

    public static final String EXTRA_ORDER_ID = OrderFlowRouter.EXTRA_ORDER_ID;

    private TransporterOrderRouter() {}

    public static Class<? extends Activity> screenFor(Order o, String myUserId) {
        String s = o.getStatus() == null ? "" : o.getStatus();
        boolean mine = myUserId != null && myUserId.equals(o.getTransporterId());
        if (!mine) return JobHistoryActivity.class; // request moved to another driver
        switch (s) {
            case "TRANSPORTER_ASSIGNED":
                return JobOfferActivity.class;
            case "TRANSPORTER_ACCEPTED":
            case "HARVESTING":
            case "HARVEST_READY":
                return ArrivingPickupActivity.class;
            case "ARRIVED_AT_PICKUP":
                return LiabilityCheckpointActivity.class;
            case "AWAITING_BUYER_LOADING_APPROVAL":
            case "LOADING_APPROVED":
                return LoadingApprovalActivity.class;
            case "IN_TRANSIT":
                return NavigationTrackingActivity.class;
            case "ARRIVED_AT_DESTINATION":
            case "BALANCE_PAID":
            case "DELIVERED":
                return DestinationArrivedActivity.class;
            case "COMPLETED":
                return RateExperienceActivity.class;
            case "PRELOAD_REJECTED":
                return CargoRejectedActivity.class;
            case "RETURN_JOURNEY":
            case "RETURNED":
                return DeliveryDisputedActivity.class;
            default:
                return JobHistoryActivity.class;
        }
    }

    public static Intent intentFor(Context ctx, Order o, String myUserId) {
        Intent i = new Intent(ctx, screenFor(o, myUserId));
        i.putExtra(EXTRA_ORDER_ID, o.getId());
        return i;
    }

    public static void open(@NonNull Context ctx, String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(ctx, "No order is linked to this item yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        String me = new com.harvesthub.mobile.utils.SessionManager(ctx).getUserId();
        ApiClient.getApiService().getOrder(orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> r) {
                if (!r.isSuccessful() || r.body() == null) {
                    Toast.makeText(ctx, "This job is no longer available.", Toast.LENGTH_SHORT).show();
                    return;
                }
                ctx.startActivity(intentFor(ctx, r.body(), me));
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                Toast.makeText(ctx, "Can't reach the server.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
