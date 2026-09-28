package com.harvesthub.desktop.api;

/** Mirrors POST /api/listings/{id}/review request body. */
public class ReviewDecisionRequestDto {
    public String decision; // APPROVE | RETAKE | REJECT
    public String grade;    // "A" | "B" | "C" — only for APPROVE
    public String notes;

    public ReviewDecisionRequestDto(String decision, String grade, String notes) {
        this.decision = decision;
        this.grade = grade;
        this.notes = notes;
    }
}
