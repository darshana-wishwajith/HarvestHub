package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** One user's 1–5 star rating of another after an order. One per (order, from, to). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ratings")
@CompoundIndex(name = "order_from_to", def = "{'orderId': 1, 'fromUserId': 1, 'toUserId': 1}", unique = true)
public class Rating {
    @Id
    private String id;
    private String orderId;
    private String fromUserId;
    private String toUserId;
    private String toRole;
    private int stars;
    private String comment;
    private String createdAt;
}
