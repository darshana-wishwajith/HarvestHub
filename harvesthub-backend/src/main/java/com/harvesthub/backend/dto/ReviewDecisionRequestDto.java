package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecisionRequestDto {
    private String decision; // APPROVE | RETAKE | REJECT
    private String grade;    // "A" | "B" | "C" — only for APPROVE
    private String notes;
}
