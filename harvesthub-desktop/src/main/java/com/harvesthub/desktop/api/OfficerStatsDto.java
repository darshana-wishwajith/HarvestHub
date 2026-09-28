package com.harvesthub.desktop.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Mirrors GET /api/dashboard/stats response shape. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfficerStatsDto {
    public long pendingCount;
    public long reviewedTodayCount;
    public int approvalRate;
}
