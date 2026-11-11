package com.yourdomain.ui;

import com.yourdomain.dao.BookingDao;
import com.yourdomain.model.Booking;
import com.yourdomain.model.CateringPackage;
import com.yourdomain.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Booking operations shared by the Admin and Staff menus. */
public class BookingConsole {

    private final AppContext ctx;
    private final User actor;

    public BookingConsole(AppContext ctx, User actor) {
        this.ctx = ctx;
        this.actor = actor;
    }

    public void viewAll() {
        TableView.printBookings("ALL BOOKINGS", ctx.bookingDao.getAllBookings(), true);
    }

    /** Asks for a booking ID. Returns null when cancelled or not found. */
    private Booking askBooking(String prompt) {
        int id = ctx.input.readInt(prompt);
        if (id == 0) {
            System.out.println("Cancelled.");
            return null;
        }
        Booking b = ctx.bookingDao.findById(id);
        if (b == null) {
            System.out.println("No booking found with ID " + id + ".");
        }
        return b;
    }

    // ---------------- status ----------------

    public void updateStatus() {
        List<Booking> list = ctx.bookingDao.getAllBookings();
        TableView.printBookings("ALL BOOKINGS", list, true);
        if (list.isEmpty()) {
            return;
        }
        Booking b = askBooking("Enter Booking ID to update (0 to cancel): ");
        if (b == null) {
            return;
        }

        String status;
        while (true) {
            status = ctx.input.readLine("New status (PENDING / CONFIRMED / COMPLETED / CANCELLED, 0 to cancel): ")
                    .toUpperCase();
            if (status.equals("0")) {
                System.out.println("Update cancelled.");
                return;
            }
            if (BookingDao.VALID_STATUSES.contains(status)) {
                break;
            }
            System.out.println("Invalid booking status.");
        }

        String error = ctx.bookingDao.updateBookingStatus(actor.id(), b.id(), status);
        if (error == null) {
            System.out.println("SUCCESS: Booking #" + b.id() + " is now " + status + ".");
        } else {
            System.out.println("Not updated: " + error);
        }
    }

    // ---------------- event details ----------------

    public void editDetails() {
        List<Booking> list = ctx.bookingDao.getAllBookings();
        TableView.printBookings("ALL BOOKINGS", list, true);
        if (list.isEmpty()) {
            return;
        }
        Booking b = askBooking("Enter Booking ID to edit (0 to cancel): ");
        if (b == null) {
            return;
        }
        if (!b.status().equals("PENDING") && !b.status().equals("CONFIRMED")) {
            System.out.println("Only PENDING or CONFIRMED bookings can be edited.");
            return;
        }

        CateringPackage pkg = ctx.packageDao.findById(b.packageId());
        int minGuests = pkg == null ? 1 : pkg.minGuests();

        String date = ctx.input.readFutureDate("New event date (yyyy-MM-dd, 0 to cancel): ");
        if (date.equals("0")) {
            System.out.println("Update cancelled.");
            return;
        }
        String venue = ctx.input.readNonEmpty("New venue (0 to cancel): ");
        if (venue.equals("0")) {
            System.out.println("Update cancelled.");
            return;
        }
        int guests;
        while (true) {
            guests = ctx.input.readInt("Number of guests (minimum " + minGuests + ", 0 to cancel): ");
            if (guests == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            if (guests >= minGuests) {
                break;
            }
            System.out.println("This package needs at least " + minGuests + " guests.");
        }

        String error = ctx.bookingDao.updateBookingDetails(actor.id(), b.id(), date, venue, guests);
        if (error == null) {
            System.out.println("SUCCESS: Booking #" + b.id() + " details updated.");
        } else {
            System.out.println("Not updated: " + error);
        }
    }

    // ---------------- payments ----------------

    public void recordPayment() {
        List<Booking> list = ctx.bookingDao.getAllBookings();
        TableView.printBookings("ALL BOOKINGS", list, true);
        if (list.isEmpty()) {
            return;
        }
        Booking b = askBooking("Enter Booking ID to record a payment for (0 to cancel): ");
        if (b == null) {
            return;
        }
        if (b.status().equals("CANCELLED")) {
            System.out.println("Cannot record a payment for a CANCELLED booking.");
            return;
        }

        TableView.printPayments("PAYMENTS FOR BOOKING #" + b.id(), ctx.paymentDao.getPaymentsByBooking(b.id()));
        double balance = Math.round(b.balance() * 100.0) / 100.0;
        System.out.println("Total: " + TableView.money(b.totalAmount()) + " | Paid: "
                + TableView.money(b.paid()) + " | Balance: " + TableView.money(balance));
        if (balance <= 0) {
            System.out.println("This booking is already fully paid.");
            return;
        }

        String type;
        while (true) {
            int choice = ctx.input.readInt("Payment type (1 = Down payment, 2 = Full payment of balance, 0 = cancel): ");
            if (choice == 0) {
                System.out.println("Payment cancelled.");
                return;
            }
            if (choice == 1) {
                type = "DOWN_PAYMENT";
                break;
            }
            if (choice == 2) {
                type = "FULL_PAYMENT";
                break;
            }
            System.out.println("Please choose 1 or 2.");
        }

        double amount;
        if (type.equals("FULL_PAYMENT")) {
            amount = balance;
            System.out.println("Full payment amount: " + TableView.money(amount));
        } else {
            while (true) {
                amount = ctx.input.readAmount("Amount received (0 to cancel): ");
                if (amount == 0) {
                    System.out.println("Payment cancelled.");
                    return;
                }
                if (amount > balance) {
                    System.out.println("Amount is more than the balance of " + TableView.money(balance) + ".");
                    continue;
                }
                break;
            }
        }

        String method;
        while (true) {
            int choice = ctx.input.readInt("Method (1 = Cash, 2 = GCash, 3 = Bank transfer, 0 = cancel): ");
            if (choice == 0) {
                System.out.println("Payment cancelled.");
                return;
            }
            if (choice == 1) {
                method = "CASH";
                break;
            }
            if (choice == 2) {
                method = "GCASH";
                break;
            }
            if (choice == 3) {
                method = "BANK_TRANSFER";
                break;
            }
            System.out.println("Please choose 1, 2, or 3.");
        }

        int paymentId = ctx.paymentDao.recordPayment(actor.id(), b.id(), amount, method, type);
        if (paymentId > 0) {
            System.out.println("SUCCESS: Payment #" + paymentId + " of " + TableView.money(amount)
                    + " recorded. Remaining balance: " + TableView.money(balance - amount));
        } else {
            System.out.println("Payment failed. Please try again.");
        }
    }

    // ---------------- calendar ----------------

    public void viewCalendar() {
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = from.plusDays(60);
        List<Booking> list = ctx.bookingDao.getBookingsInRange(from.toString(), to.toString());

        List<String[]> rows = new ArrayList<>();
        for (Booking b : list) {
            rows.add(new String[]{b.eventDate(), String.valueOf(b.id()), TableView.cut(b.customerName(), 15),
                    TableView.cut(b.packageName(), 22), b.status()});
        }
        TableView.printTable("BOOKING CALENDAR (" + from + " to " + to + ")",
                new String[]{"Date", "Booking", "Customer", "Package", "Status"},
                new int[]{10, 7, 15, 22, 10}, rows);
        System.out.println("A date with a CONFIRMED booking is not available to other customers.");
    }

    // ---------------- admin only ----------------

    public void removeUnconfirmed() {
        List<Booking> removable = new ArrayList<>();
        for (Booking b : ctx.bookingDao.getAllBookings()) {
            if (b.status().equals("PENDING") || b.status().equals("CANCELLED")) {
                removable.add(b);
            }
        }
        TableView.printBookings("UNCONFIRMED / CANCELLED BOOKINGS", removable, true);
        if (removable.isEmpty()) {
            return;
        }

        int id = ctx.input.readInt("Enter Booking ID to remove (0 to cancel): ");
        if (id == 0) {
            System.out.println("Removal cancelled.");
            return;
        }
        boolean listed = removable.stream().anyMatch(b -> b.id() == id);
        if (!listed) {
            System.out.println("Booking " + id + " is not in the removable list.");
            return;
        }
        if (!ctx.input.confirm("This permanently deletes Booking #" + id + ".")) {
            System.out.println("Removal cancelled.");
            return;
        }
        if (ctx.bookingDao.deleteUnconfirmedBooking(actor.id(), id)) {
            System.out.println("SUCCESS: Booking #" + id + " removed.");
        } else {
            System.out.println("Removal failed.");
        }
    }
}
