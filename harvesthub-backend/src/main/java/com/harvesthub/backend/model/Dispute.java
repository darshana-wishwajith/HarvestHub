package com.harvesthub.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Issue raised after the goods were accepted (Figma buyer 6.2 "Report Issue with Handover").
 * Decision 15 Nov: a ticket for the officers; money stays as settled, no automatic refund.
 */
@Data
@NoArgsConstructor
@Document(collection = "disputes")
public class Dispute {
    @Id
    private String id;
    private String orderId;
    private String raisedByUserId;
    private String raisedByName;
    private String raisedByRole;   // BUYER | FARMER | TRANSPORTER
    private String reason;
    private String details;
    private String orderStatusAtOpen;
    private String status;         // OPEN | RESOLVED | DISMISSED
    private String createdAt;
    private String resolvedAt;
    private String resolutionNote;
    private String resolvedById;     // Agricultural Officer who closed the ticket (desktop, 15 Nov)
    private String resolvedByName;
    private String type;            // ISSUE (buyer/farmer after handover) | APPEAL (driver after a delivery rejection)
    private java.util.List<String> photoUrls;
}
