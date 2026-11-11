package com.yourdomain.model;

/** Row of the payments table. */
public record Payment(int id, int bookingId, double amount, String method, String type,
                      String paymentDate, String receivedBy) {
}
