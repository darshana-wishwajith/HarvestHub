package com.harvesthub.backend.repository;
import com.harvesthub.backend.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByFarmerId(String farmerId);
    List<Order> findByBuyerId(String buyerId);
    List<Order> findByTransporterId(String transporterId);
    List<Order> findByTransporterIdAndStatusIn(String transporterId, List<String> statuses);
    List<Order> findByStatus(String status);
    /** Open job requests broadcast to this driver (array field contains the id). */
    List<Order> findByStatusAndOfferedTransporterIds(String status, String transporterId);
    List<Order> findByListingId(String listingId);
}
