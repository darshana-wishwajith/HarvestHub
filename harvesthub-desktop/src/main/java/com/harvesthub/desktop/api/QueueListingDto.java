package com.harvesthub.desktop.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors GET /api/queue row shape — same fields as the existing QueueListing model. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueListingDto {
    public Long id;
    public String cropName;
    public String farmerName;
    public String location;
    public String submittedAgo;
    public String quantity;
    public String pricePerKg;
    public String statusLabel;
    public boolean claimable;
    public String tone;
    public String iconLiteral;
}
