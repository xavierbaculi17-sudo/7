package com.yourdomain.ui;

import com.yourdomain.model.Booking;
import com.yourdomain.model.CateringPackage;
import com.yourdomain.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Customer: browse packages, check dates, book, track and cancel own bookings. */
public class CustomerMenu {

    private final AppContext ctx;
    private final User customer;

    public CustomerMenu(AppContext ctx, User customer) {
        this.ctx = ctx;
        this.customer = customer;
    }

    public void show() {
        while (true) {
            System.out.println("\n===== CUSTOMER MENU (" + customer.username() + ") =====");
            System.out.println("1. View My Profile");
            System.out.println("2. Browse / Search Packages");
            System.out.println("3. Check Available Dates");
            System.out.println("4. Submit a Booking Request");
            System.out.println("5. My Bookings (history, status, balance)");
            System.out.println("6. Cancel a Pending Booking");
            System.out.println("0. Logout");
            int choice = ctx.input.readInt("Choose an option: ");

            switch (choice) {
                case 1 -> viewProfile();
                case 2 -> browsePackages();
                case 3 -> checkDates();
                case 4 -> submitBooking();
                case 5 -> TableView.printBookings("MY BOOKINGS",
                        ctx.bookingDao.getBookingsByCustomer(customer.id()), false);
                case 6 -> cancelBooking();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option. Please choose 0 to 6.");
            }
        }
    }

    private void viewProfile() {
        System.out.println("\n--- My Profile ---");
        System.out.println("ID        : " + customer.id());
        System.out.println("Username  : " + customer.username());
        System.out.println("Full name : " + customer.fullName());
        System.out.println("Contact   : " + customer.contactNo());
        System.out.println("Email     : " + customer.email());
        System.out.println("Role      : " + customer.roles());
        System.out.println("Status    : " + customer.status());
    }

    private void browsePackages() {
        System.out.println("Leave a filter blank (press Enter) to skip it.");
        String eventType = ctx.input.readLine("Event type (e.g. wedding): ");
        Double maxPrice = ctx.input.readOptionalAmount("Maximum price per head: ");
        List<CateringPackage> found = ctx.packageDao.searchActivePackages(eventType, maxPrice);
        TableView.printPackages("AVAILABLE PACKAGES", found);
    }

    private void checkDates() {
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = from.plusDays(90);
        List<String> taken = ctx.bookingDao.getConfirmedDates(from.toString(), to.toString());

        List<String[]> rows = new ArrayList<>();
        for (String d : taken) {
            rows.add(new String[]{d, "NOT AVAILABLE"});
        }
        TableView.printTable("BOOKED DATES (" + from + " to " + to + ")",
                new String[]{"Date", "Availability"}, new int[]{10, 14}, rows);
        System.out.println("Every other date in this range is open for booking.");

        String date = ctx.input.readFutureDate("Check a specific date (yyyy-MM-dd, 0 to go back): ");
        if (date.equals("0")) {
            return;
        }
        if (ctx.bookingDao.countConfirmedOnDate(date, 0) > 0) {
            System.out.println(date + " is NOT available.");
        } else {
            System.out.println(date + " is available.");
        }
    }

    private void submitBooking() {
        List<CateringPackage> packages = ctx.packageDao.searchActivePackages(null, null);
        TableView.printPackages("AVAILABLE PACKAGES", packages);
        if (packages.isEmpty()) {
            return;
        }

        CateringPackage chosen = null;
        while (chosen == null) {
            int id = ctx.input.readInt("Choose a Package ID (0 to cancel): ");
            if (id == 0) {
                System.out.println("Booking cancelled.");
                return;
            }
            for (CateringPackage p : packages) {
                if (p.id() == id) {
                    chosen = p;
                    break;
                }
            }
            if (chosen == null) {
                System.out.println("Package " + id + " is not in the list. Try again.");
            }
        }

        String date;
        while (true) {
            date = ctx.input.readFutureDate("Event date (yyyy-MM-dd, 0 to cancel): ");
            if (date.equals("0")) {
                System.out.println("Booking cancelled.");
                return;
            }
            if (ctx.bookingDao.countConfirmedOnDate(date, 0) > 0) {
                System.out.println(date + " is already taken. Please choose another date.");
                continue;
            }
            break;
        }

        String venue = ctx.input.readNonEmpty("Venue (0 to cancel): ");
        if (venue.equals("0")) {
            System.out.println("Booking cancelled.");
            return;
        }

        int guests;
        while (true) {
            guests = ctx.input.readInt("Number of guests (minimum " + chosen.minGuests() + ", 0 to cancel): ");
            if (guests == 0) {
                System.out.println("Booking cancelled.");
                return;
            }
            if (guests >= chosen.minGuests()) {
                break;
            }
            System.out.println("This package needs at least " + chosen.minGuests() + " guests.");
        }

        double total = guests * chosen.pricePerHead();
        System.out.println("Estimated total: " + TableView.money(total));
        int bookingId = ctx.bookingDao.createBooking(customer.id(), chosen, date, venue, guests);
        if (bookingId > 0) {
            System.out.println("SUCCESS: Booking #" + bookingId + " submitted (PENDING). "
                    + "Staff will confirm it and record your down payment.");
        } else {
            System.out.println("Booking failed. Please try again.");
        }
    }

    private void cancelBooking() {
        List<Booking> pending = new ArrayList<>();
        for (Booking b : ctx.bookingDao.getBookingsByCustomer(customer.id())) {
            if (b.status().equals("PENDING")) {
                pending.add(b);
            }
        }
        TableView.printBookings("MY PENDING BOOKINGS", pending, false);
        if (pending.isEmpty()) {
            return;
        }

        int id = ctx.input.readInt("Enter Booking ID to cancel (0 to go back): ");
        if (id == 0) {
            System.out.println("Cancelled.");
            return;
        }
        boolean listed = pending.stream().anyMatch(b -> b.id() == id);
        if (!listed) {
            System.out.println("Booking " + id + " is not one of your pending bookings.");
            return;
        }
        if (!ctx.input.confirm("Cancel Booking #" + id + "?")) {
            System.out.println("Nothing was cancelled.");
            return;
        }
        System.out.println(ctx.bookingDao.cancelOwnBooking(customer.id(), id)
                ? "SUCCESS: Booking #" + id + " cancelled."
                : "Could not cancel the booking.");
    }
}
