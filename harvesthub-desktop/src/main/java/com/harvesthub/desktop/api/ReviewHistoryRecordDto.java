package com.harvesthub.desktop.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors GET /api/review-history row shape — same fields as the existing ReviewHistoryRecord model. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewHistoryRecordDto {
    public Long id;
    public String avatarCode;
    public String cropName;
    public String variety;
    public String farmerName;
    public String location;
    public String decisionLabel;
    public String decisionKind;
    public String dateReviewed;
}
