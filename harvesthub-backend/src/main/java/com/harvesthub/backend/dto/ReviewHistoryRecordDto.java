package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewHistoryRecordDto {
    private String id;
    private String avatarCode;
    private String cropName;
    private String variety;
    private String farmerName;
    private String location;
    private String decisionLabel;
    private String decisionKind; // GRADE_A, GRADE_B, GRADE_C, RETAKE, REJECTED
    private String dateReviewed;
}
