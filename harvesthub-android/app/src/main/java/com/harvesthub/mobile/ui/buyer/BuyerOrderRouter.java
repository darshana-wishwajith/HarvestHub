package com.harvesthub.mobile.ui.buyer;

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

/**
 * Opens the buyer screen that matches the order's workflow status (Figma buyer 3.2 – 6.4), always with
 * the real order id. Used by Orders, Home, notifications and the order screens themselves.
 */
public final class BuyerOrderRouter {

    public static final String EXTRA_ORDER_ID = OrderFlowRouter.EXTRA_ORDER_ID;

    private BuyerOrderRouter() {}

    public static Class<? extends Activity> screenFor(Order o, String myUserId) {
        String s = o.getStatus() == null ? "" : o.getStatus();
        switch (s) {
            case "NEGOTIATING":
                return NegotiationChatActivity.class;
            case "AWAITING_PAYMENT":
            case "CANCELLED":
                return OrderSummaryActivity.class;
            case "DISPATCHING":
            case "PAUSED":
            case "TRANSPORTER_ASSIGNED":
                return TransactionLedgerActivity.class;
            case "TRANSPORTER_ACCEPTED":
            case "HARVESTING":
            case "HARVEST_READY":
            case "ARRIVED_AT_PICKUP":
            case "LOADING_APPROVED":
                return HarvestTrackerActivity.class;
            case "AWAITING_BUYER_LOADING_APPROVAL":
                return VerifyCargoActivity.class;
            case "IN_TRANSIT":
                return TransitTrackingActivity.class;
            case "ARRIVED_AT_DESTINATION":
                return ArrivalInspectionActivity.class;
            case "BALANCE_PAID":
            case "DELIVERED":
                return DeliveryHandshakeActivity.class;
            case "COMPLETED":
                boolean rated = o.getRatedBy() != null && o.getRatedBy().contains(myUserId);
                return rated ? DeliveryHandshakeActivity.class : BuyerRatingActivity.class;
            case "PRELOAD_REJECTED":
            case "RETURN_JOURNEY":
                return DisputeResolvedActivity.class;
            default:
                return OrderSummaryActivity.class;
        }
    }

    public static Intent intentFor(Context ctx, Order o, String myUserId) {
        Intent i = new Intent(ctx, screenFor(o, myUserId));
        i.putExtra(EXTRA_ORDER_ID, o.getId());
        return i;
    }

    /** Loads the order and opens the right screen. */
    public static void open(@NonNull Context ctx, String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(ctx, "No order is linked to this item yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        String me = new com.harvesthub.mobile.utils.SessionManager(ctx).getUserId();
        ApiClient.getApiService().getOrder(orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ctx, "Could not load the order.", Toast.LENGTH_SHORT).show();
                    return;
                }
                ctx.startActivity(intentFor(ctx, response.body(), me));
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                Toast.makeText(ctx, "Can't reach the server. Check your internet connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
