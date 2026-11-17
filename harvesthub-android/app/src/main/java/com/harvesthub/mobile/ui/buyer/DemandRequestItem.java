package com.harvesthub.mobile.ui.buyer;

import com.harvesthub.mobile.R;

public class DemandRequestItem {
    private String id;
    private DemandStatus status;
    private String timeAgo;
    private String title;
    private String deadline;
    private String targetGrade;

    public DemandRequestItem(String id, DemandStatus status, String timeAgo, String title, String deadline, String targetGrade) {
        this.id = id;
        this.status = status;
        this.timeAgo = timeAgo;
        this.title = title;
        this.deadline = deadline;
        this.targetGrade = targetGrade;
    }

    public DemandRequestItem(String id, DemandStatus status, String timeAgo, String title, String targetGrade) {
        this(id, status, timeAgo, title, null, targetGrade);
    }

    private int offerCount;
    private int newOfferCount;

    public DemandRequestItem offers(int total, int fresh) {
        this.offerCount = total;
        this.newOfferCount = fresh;
        return this;
    }

    public int getOfferCount() { return offerCount; }
    public int getNewOfferCount() { return newOfferCount; }

    public String getId() { return id; }
    public DemandStatus getStatus() { return status; }
    public String getTimeAgo() { return timeAgo; }
    public String getTitle() { return title; }
    public String getDeadline() { return deadline; }
    public String getTargetGrade() { return targetGrade; }
}

