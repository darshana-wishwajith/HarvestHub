package com.harvesthub.mobile.ui.transporter;

/** Driver-facing names of the order statuses. */
final class JobStatus {
    private JobStatus() {}

    static String label(String status) {
        if (status == null) return "";
        switch (status) {
            case "TRANSPORTER_ASSIGNED": return "Job request";
            case "TRANSPORTER_ACCEPTED":
            case "HARVESTING": return "Farmer harvesting";
            case "HARVEST_READY": return "Go to pickup";
            case "ARRIVED_AT_PICKUP": return "At the farm – take photos";
            case "AWAITING_BUYER_LOADING_APPROVAL": return "Waiting for buyer approval";
            case "LOADING_APPROVED": return "Ready for departure";
            case "IN_TRANSIT": return "In transit";
            case "ARRIVED_AT_DESTINATION": return "Buyer inspecting";
            case "BALANCE_PAID": return "Unloading handshake";
            case "DELIVERED": return "Waiting for buyer confirmation";
            case "COMPLETED": return "Completed";
            case "PRELOAD_REJECTED": return "Empty trip – rejected at loading";
            case "RETURN_JOURNEY": return "Rejected – return to farm";
            case "RETURNED": return "Rejected – goods returned";
            case "CANCELLED": return "Cancelled";
            default: return status.replace('_', ' ');
        }
    }
}
