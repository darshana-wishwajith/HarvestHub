package com.harvesthub.mobile.ui.transporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.ui.farmer.OrderFlowRouter;

import org.junit.Test;

/** Transporter screen routing per workflow status, payout and timer helpers (Figma transporter page 612:58). */
public class TransporterFlowTest {

    private static Order order(String status, String transporterId) {
        Order o = new Order();
        o.setStatus(status);
        o.setTransporterId(transporterId);
        o.setBuyerId("buyer-1");
        return o;
    }

    private static Class<?> screen(String status) {
        return TransporterOrderRouter.screenFor(order(status, "driver-1"), "driver-1");
    }

    @Test
    public void everyStatusOpensTheMatchingDriverScreen() {
        assertEquals(JobOfferActivity.class, screen("TRANSPORTER_ASSIGNED"));
        assertEquals(ArrivingPickupActivity.class, screen("TRANSPORTER_ACCEPTED"));
        assertEquals(ArrivingPickupActivity.class, screen("HARVEST_READY"));
        assertEquals(LiabilityCheckpointActivity.class, screen("ARRIVED_AT_PICKUP"));
        assertEquals(LoadingApprovalActivity.class, screen("AWAITING_BUYER_LOADING_APPROVAL"));
        assertEquals(LoadingApprovalActivity.class, screen("LOADING_APPROVED"));
        assertEquals(NavigationTrackingActivity.class, screen("IN_TRANSIT"));
        assertEquals(DestinationArrivedActivity.class, screen("ARRIVED_AT_DESTINATION"));
        assertEquals(DestinationArrivedActivity.class, screen("BALANCE_PAID"));
        assertEquals(DestinationArrivedActivity.class, screen("DELIVERED"));
        assertEquals(RateExperienceActivity.class, screen("COMPLETED"));
        assertEquals(CargoRejectedActivity.class, screen("PRELOAD_REJECTED"));
        assertEquals(DeliveryDisputedActivity.class, screen("RETURN_JOURNEY"));
        assertEquals(DeliveryDisputedActivity.class, screen("RETURNED"));
    }

    @Test
    public void requestMovedToAnotherDriverGoesToHistory() {
        assertEquals(JobHistoryActivity.class, TransporterOrderRouter.screenFor(order("TRANSPORTER_ASSIGNED", "driver-2"), "driver-1"));
    }

    @Test
    public void sharedRouterSendsTheDriverToDriverScreens() {
        assertEquals(LoadingApprovalActivity.class, OrderFlowRouter.screenFor(order("LOADING_APPROVED", "driver-1"), "driver-1"));
        assertTrue(OrderFlowRouter.screenFor(order("LOADING_APPROVED", "driver-1"), "farmer-1").getName().contains(".farmer."));
    }

    @Test
    public void payoutIsNinetyFivePercentOfTheTransportFee() {
        Order fromServer = new Gson().fromJson("{\"payment\":{\"transportFee\":14000,\"transporterPayout\":13300}}", Order.class);
        assertEquals(13300, fromServer.driverPayout(), 0.001);
        Order older = new Gson().fromJson("{\"payment\":{\"transportFee\":11400}}", Order.class); // created before 15 Nov
        assertEquals(10830, older.driverPayout(), 0.001);
        assertEquals(0, new Order().driverPayout(), 0.001);
    }

    @Test
    public void timersAndDurations() {
        assertEquals("Expired – being sent to another driver", JobOfferActivity.countdown(System.currentTimeMillis() - 1000));
        assertTrue(JobOfferActivity.countdown(System.currentTimeMillis() + 9 * 60_000 + 30_000).startsWith("⏱ Expires in 09:"));
        assertEquals("1h 30m", JobOfferActivity.duration(60));
        assertEquals("45 min", JobOfferActivity.duration(30));
        assertEquals("Waiting for buyer approval", JobStatus.label("AWAITING_BUYER_LOADING_APPROVAL"));
    }
}
