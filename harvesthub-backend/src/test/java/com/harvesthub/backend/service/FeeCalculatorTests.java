package com.harvesthub.backend.service;

import com.harvesthub.backend.config.MarketplaceRules;
import com.harvesthub.backend.model.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Money and vehicle rules (workflow 3.1, 3.2, 5.2; user decisions 14 Nov). */
class FeeCalculatorTests {

    private final MarketplaceRules rules = new MarketplaceRules();

    @Test
    void picksSmallestVehicleThatFits() {
        assertEquals("THREE_WHEELER", FeeCalculator.selectVehicle(500, rules).getType());
        assertEquals("MINI_LORRY", FeeCalculator.selectVehicle(500.5, rules).getType());
        assertEquals("MINI_LORRY", FeeCalculator.selectVehicle(1000, rules).getType());
        assertEquals("LORRY", FeeCalculator.selectVehicle(800 * 2, rules).getType());
        assertEquals("TRUCK", FeeCalculator.selectVehicle(10_000, rules).getType());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> FeeCalculator.selectVehicle(10_001, rules));
        assertTrue(e.getMessage().contains("Split the order"));
    }

    @Test
    void transportFeeIsDistanceTimesRatePlusWeightTimesHandling() {
        MarketplaceRules.Vehicle v = FeeCalculator.selectVehicle(500, rules); // three-wheeler, LKR 80/km
        assertEquals(4_000, FeeCalculator.distanceCost(50, v));
        assertEquals(1_000, FeeCalculator.handlingCost(500, rules)); // LKR 2/kg
    }

    @Test
    void advanceAndBalanceFollowTheAgreedFormula() {
        // 500 kg × LKR 250 = 125,000 crop; transport 5,000
        Order.Payment p = FeeCalculator.quote(500, 250, 5_000, rules);
        assertEquals(125_000, p.getCropValue());
        assertEquals(3_900, p.getBuyerFee());             // 3% of 130,000
        assertEquals(25_000, p.getAdvanceCropPortion());  // 20% of crop
        assertEquals(33_900, p.getAdvanceDue());          // 25,000 + 5,000 + 3,900
        assertEquals(100_000, p.getBalanceDue());         // 80% of crop
        assertEquals(4_750, FeeCalculator.transporterPayout(p, rules));   // 95% of transport
        assertEquals(250, FeeCalculator.platformCommission(p, rules));    // 5% of transport
        // everything the buyer pays ends up somewhere: farmer + transporter + platform
        double buyerPays = p.getAdvanceDue() + p.getBalanceDue();
        double distributed = p.getCropValue() + FeeCalculator.transporterPayout(p, rules)
                + FeeCalculator.platformCommission(p, rules) + p.getBuyerFee();
        assertEquals(buyerPays, distributed, 0.001);
    }

    @Test
    void roundsToCents() {
        Order.Payment p = FeeCalculator.quote(333, 101.11, 1_234.567, rules);
        assertEquals(33_669.63, p.getCropValue());
        assertEquals(p.getAdvanceCropPortion() + p.getBalanceDue(), p.getCropValue(), 0.0001);
    }

    @Test
    void haversineIsRoughlyRight() {
        // Dambulla → Colombo is about 130 km in a straight line
        double km = FeeCalculator.haversineKm(7.8742, 80.6511, 6.9271, 79.8612);
        assertTrue(km > 120 && km < 140, "got " + km);
    }

    @Test
    void trustScoreFormula() {
        assertEquals(75, TrustService.score(null, 0, 0));      // new user: neutral
        assertEquals(100, TrustService.score(5.0, 20, 0));     // perfect
        assertEquals(88, TrustService.score(4.5, 10, 0));      // 0.45 + 0.35 + 0.075 = 87.5
        assertTrue(TrustService.score(4.5, 10, 5) < TrustService.score(4.5, 10, 0)); // failures lower trust
    }
}
