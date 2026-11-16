package com.harvesthub.mobile.ui.farmer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Opens the farmer screen that matches the order's current workflow status (Figma 25–32),
 * always passing the real order id. Used by My Listings, notifications and the order screens themselves.
 */
public final class OrderFlowRouter {

    public static final String EXTRA_ORDER_ID = "ORDER_ID";

    private OrderFlowRouter() {}

    /** Which screen belongs to a status, from the farmer's point of view. */
    public static Class<? extends Activity> screenFor(Order o, String myUserId) {
        if (myUserId != null && myUserId.equals(o.getBuyerId())) { // the buyer has its own screens
            return com.harvesthub.mobile.ui.buyer.BuyerOrderRouter.screenFor(o, myUserId);
        }
        if (myUserId != null && myUserId.equals(o.getTransporterId())) { // so does the driver
            return com.harvesthub.mobile.ui.transporter.TransporterOrderRouter.screenFor(o, myUserId);
        }
        String s = o.getStatus() == null ? "" : o.getStatus();
        switch (s) {
            case "NEGOTIATING":
                return o.isFarmerConfirmed() ? WaitForBuyerApprovalActivity.class : ChatActivity.class;
            case "AWAITING_PAYMENT":
                return WaitingForAdvancePaymentActivity.class;
            case "DISPATCHING":
            case "PAUSED":
            case "TRANSPORTER_ASSIGNED":
                return ActiveOrderTrackerActivity.class;
            case "TRANSPORTER_ACCEPTED":
                return StartHarvestingActivity.class;
            case "HARVESTING":
                return HarvestReadyActivity.class;
            case "HARVEST_READY":
            case "ARRIVED_AT_PICKUP":
            case "AWAITING_BUYER_LOADING_APPROVAL":
            case "LOADING_APPROVED":
                return VehicleArrivalActivity.class;
            case "IN_TRANSIT":
            case "ARRIVED_AT_DESTINATION":
            case "BALANCE_PAID":
            case "DELIVERED":
                return OrderInTransitActivity.class;
            case "COMPLETED":
                boolean rated = o.getRatedBy() != null && o.getRatedBy().contains(myUserId);
                return rated ? ActiveOrderTrackerActivity.class : RatingFeedbackActivity.class;
            default: // PRELOAD_REJECTED, RETURN_JOURNEY, CANCELLED
                return ActiveOrderTrackerActivity.class;
        }
    }

    public static Intent intentFor(Context ctx, Order o, String myUserId) {
        Intent i = new Intent(ctx, screenFor(o, myUserId));
        i.putExtra(EXTRA_ORDER_ID, o.getId());
        return i;
    }

    /** Loads the order and opens the right screen. finishCaller closes the calling activity. */
    public static void open(@NonNull Context ctx, String orderId, boolean finishCaller) {
        if (orderId == null) {
            Toast.makeText(ctx, "No order is linked to this item yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        String me = new SessionManager(ctx).getUserId();
        ApiClient.getApiService().getOrder(orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ctx, "Could not load the order.", Toast.LENGTH_SHORT).show();
                    return;
                }
                ctx.startActivity(intentFor(ctx, response.body(), me));
                if (finishCaller && ctx instanceof Activity) ((Activity) ctx).finish();
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                Toast.makeText(ctx, "Can't reach the server. Check your internet connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
