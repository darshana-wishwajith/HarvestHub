package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.DashboardResponse;
import com.harvesthub.backend.dto.FarmerSummaryResponse;
import com.harvesthub.backend.model.Listing;
import com.harvesthub.backend.model.Order;
import com.harvesthub.backend.model.Transaction;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.ListingRepository;
import com.harvesthub.backend.repository.OrderRepository;
import com.harvesthub.backend.repository.TransactionRepository;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class DashboardService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ListingRepository listingRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    public DashboardResponse getFarmerDashboard(String farmerId) {
        User user = userRepository.findById(farmerId).orElse(null);
        String farmerName = user != null ? user.getFullName() : "Farmer";

        List<Listing> activeListings = listingRepository.findByFarmerIdAndStatusIn(
                farmerId, Arrays.asList("ACTIVE", "LISTED", "ONGOING"));
        
        List<Order> completedOrders = orderRepository.findByFarmerId(farmerId).stream()
                .filter(o -> "DELIVERED".equals(o.getStatus())).toList();

        List<Transaction> transactions = transactionRepository.findByFarmerId(farmerId);
        double totalRevenue = transactions.stream()
                .filter(Transaction::isCredit)
                .mapToDouble(t -> {
                    try { return Double.parseDouble(t.getAmount().replace("Rs. ", "").replace(",", "")); }
                    catch (Exception e) { return 0; }
                }).sum();

        DashboardResponse response = new DashboardResponse();
        response.setFarmerName(farmerName);
        
        java.time.LocalTime now = java.time.LocalTime.now();
        String greeting = "Good evening";
        if (now.isBefore(java.time.LocalTime.NOON)) {
            greeting = "Good morning";
        } else if (now.isBefore(java.time.LocalTime.of(17, 0))) {
            greeting = "Good afternoon";
        }
        response.setGreeting(greeting);

        response.setTotalRevenue(String.format("Rs. %,.2f", totalRevenue));
        response.setRevenueChange("+12.5%");
        response.setPreviousRevenue(String.format("Rs. %,.2f", totalRevenue * 0.89));
        response.setActiveListings(String.valueOf(activeListings.size()));
        response.setCompletedOrders(String.valueOf(completedOrders.size()));
        response.setTrustScore("4.8");
        return response;
    }

    public FarmerSummaryResponse getFarmerSummary(String farmerId) {
        List<Transaction> transactions = transactionRepository.findByFarmerId(farmerId);
        double monthlyRevenue = transactions.stream()
                .filter(Transaction::isCredit)
                .mapToDouble(t -> {
                    try { return Double.parseDouble(t.getAmount().replace("Rs. ", "").replace(",", "")); }
                    catch (Exception e) { return 0; }
                }).sum();
        return new FarmerSummaryResponse(String.format("Rs. %,.2f", monthlyRevenue), "4.8");
    }
}
