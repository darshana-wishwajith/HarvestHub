package com.harvesthub.mobile.ui.farmer;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;
import com.harvesthub.mobile.model.Order;

import org.junit.Test;

/** Each backend order status opens the matching farmer screen. */
public class OrderFlowRouterTest {

    private final Gson gson = new Gson();

    private Order order(String status, boolean farmerConfirmed, String ratedBy) {
        String json = "{\"orderId\":\"o1\",\"status\":\"" + status + "\",\"farmerConfirmed\":" + farmerConfirmed
                + (ratedBy == null ? "" : ",\"ratedBy\":[\"" + ratedBy + "\"]") + "}";
        return gson.fromJson(json, Order.class);
    }

    private Class<?> screen(String status) {
        return OrderFlowRouter.screenFor(order(status, false, null), "farmer1");
    }

    @Test
    public void negotiationGoesToChatOrWaiting() {
        assertEquals(ChatActivity.class, screen("NEGOTIATING"));
        assertEquals(WaitForBuyerApprovalActivity.class, OrderFlowRouter.screenFor(order("NEGOTIATING", true, null), "farmer1"));
    }

    @Test
    public void everyWorkflowStepHasAScreen() {
        assertEquals(WaitingForAdvancePaymentActivity.class, screen("AWAITING_PAYMENT"));
        assertEquals(ActiveOrderTrackerActivity.class, screen("PAUSED"));
        assertEquals(ActiveOrderTrackerActivity.class, screen("TRANSPORTER_ASSIGNED"));
        assertEquals(StartHarvestingActivity.class, screen("TRANSPORTER_ACCEPTED"));
        assertEquals(HarvestReadyActivity.class, screen("HARVESTING"));
        assertEquals(VehicleArrivalActivity.class, screen("HARVEST_READY"));
        assertEquals(VehicleArrivalActivity.class, screen("AWAITING_BUYER_LOADING_APPROVAL"));
        assertEquals(OrderInTransitActivity.class, screen("IN_TRANSIT"));
        assertEquals(OrderInTransitActivity.class, screen("DELIVERED"));
        assertEquals(ActiveOrderTrackerActivity.class, screen("PRELOAD_REJECTED"));
    }

    @Test
    public void completedAsksForRatingOnlyOnce() {
        assertEquals(RatingFeedbackActivity.class, OrderFlowRouter.screenFor(order("COMPLETED", true, null), "farmer1"));
        assertEquals(ActiveOrderTrackerActivity.class, OrderFlowRouter.screenFor(order("COMPLETED", true, "farmer1"), "farmer1"));
    }
}
