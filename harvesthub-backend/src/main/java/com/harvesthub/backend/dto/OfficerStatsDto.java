package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficerStatsDto {
    private long pendingCount;
    private long reviewedTodayCount;
    private int approvalRate;
}
