package com.harvesthub.mobile.ui.buyer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.ui.farmer.OrderFlowRouter;

import org.junit.Test;

/** Buyer screen routing per workflow status and the simulated card checks (Figma buyer 4.1 – 6.4). */
public class BuyerFlowTest {

    private static Order order(String status) {
        Order o = new Order();
        o.setStatus(status);
        o.setBuyerId("buyer-1");
        return o;
    }

    @Test
    public void everyStatusOpensTheMatchingBuyerScreen() {
        assertEquals(NegotiationChatActivity.class, BuyerOrderRouter.screenFor(order("NEGOTIATING"), "buyer-1"));
        assertEquals(OrderSummaryActivity.class, BuyerOrderRouter.screenFor(order("AWAITING_PAYMENT"), "buyer-1"));
        assertEquals(TransactionLedgerActivity.class, BuyerOrderRouter.screenFor(order("PAUSED"), "buyer-1"));
        assertEquals(TransactionLedgerActivity.class, BuyerOrderRouter.screenFor(order("TRANSPORTER_ASSIGNED"), "buyer-1"));
        assertEquals(HarvestTrackerActivity.class, BuyerOrderRouter.screenFor(order("HARVESTING"), "buyer-1"));
        assertEquals(HarvestTrackerActivity.class, BuyerOrderRouter.screenFor(order("LOADING_APPROVED"), "buyer-1"));
        assertEquals(VerifyCargoActivity.class, BuyerOrderRouter.screenFor(order("AWAITING_BUYER_LOADING_APPROVAL"), "buyer-1"));
        assertEquals(TransitTrackingActivity.class, BuyerOrderRouter.screenFor(order("IN_TRANSIT"), "buyer-1"));
        assertEquals(ArrivalInspectionActivity.class, BuyerOrderRouter.screenFor(order("ARRIVED_AT_DESTINATION"), "buyer-1"));
        assertEquals(DeliveryHandshakeActivity.class, BuyerOrderRouter.screenFor(order("BALANCE_PAID"), "buyer-1"));
        assertEquals(BuyerRatingActivity.class, BuyerOrderRouter.screenFor(order("COMPLETED"), "buyer-1"));
        assertEquals(DisputeResolvedActivity.class, BuyerOrderRouter.screenFor(order("PRELOAD_REJECTED"), "buyer-1"));
        assertEquals(DisputeResolvedActivity.class, BuyerOrderRouter.screenFor(order("RETURN_JOURNEY"), "buyer-1"));
    }

    @Test
    public void sharedRouterSendsTheBuyerToBuyerScreens() {
        assertEquals(VerifyCargoActivity.class, OrderFlowRouter.screenFor(order("AWAITING_BUYER_LOADING_APPROVAL"), "buyer-1"));
        // the farmer of the same order still gets the farmer screen
        assertFalse(OrderFlowRouter.screenFor(order("AWAITING_BUYER_LOADING_APPROVAL"), "farmer-1").getName().contains(".buyer."));
    }

    @Test
    public void simulatedCardChecks() {
        assertTrue(SecureCheckoutActivity.luhn("4532015112830366"));
        assertFalse(SecureCheckoutActivity.luhn("4532015112830367"));
        assertTrue(SecureCheckoutActivity.expired("01/20"));
        assertFalse(SecureCheckoutActivity.expired("12/99"));
    }

    @Test
    public void moneyFormat() {
        assertEquals("Rs. 11,845", BaseBuyerOrderActivity.rs(11845));
        assertEquals("Rs. 9,476.50", BaseBuyerOrderActivity.rs(9476.5));
    }
}
