package com.yourdomain.model;

/** Row of the bookings table joined with customer name, package name and amount paid. */
public record Booking(int id, int customerId, String customerName, int packageId, String packageName,
                      String eventType, String eventDate, String venue, int guestCount,
                      double totalAmount, String status, double paid) {

    public double balance() {
        return totalAmount - paid;
    }
}
