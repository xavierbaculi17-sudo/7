package com.yourdomain.model;

/** Row of the activity_logs table (audit trail). */
public record ActivityLog(int id, int userId, String actionType, String description, String timestamp) {
}
