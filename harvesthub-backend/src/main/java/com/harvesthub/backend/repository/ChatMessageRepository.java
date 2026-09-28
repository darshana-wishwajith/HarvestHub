package com.harvesthub.backend.repository;
import com.harvesthub.backend.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByOrderIdOrderByTimestampAsc(String orderId);
}
