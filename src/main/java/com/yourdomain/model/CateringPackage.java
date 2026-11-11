package com.yourdomain.model;

/** Row of the packages table. description holds the menu items. */
public record CateringPackage(int id, String name, String description, String eventType,
                              double pricePerHead, int minGuests, String status) {
}
