package com.yourdomain.ui;

import com.yourdomain.model.User;

/** Catering staff: confirm bookings, update status/details, record payments. Cannot manage users. */
public class StaffMenu {

    private final AppContext ctx;
    private final User staff;
    private final BookingConsole bookings;

    public StaffMenu(AppContext ctx, User staff) {
        this.ctx = ctx;
        this.staff = staff;
        this.bookings = new BookingConsole(ctx, staff);
    }

    public void show() {
        while (true) {
            System.out.println("\n===== STAFF MENU (" + staff.username() + ") =====");
            System.out.println("1. Browse Packages");
            System.out.println("2. View All Bookings");
            System.out.println("3. Update Booking Status");
            System.out.println("4. Edit Event Details");
            System.out.println("5. Record a Payment");
            System.out.println("6. View Booking Calendar");
            System.out.println("0. Logout");
            int choice = ctx.input.readInt("Choose an option: ");

            switch (choice) {
                case 1 -> TableView.printPackages("ACTIVE PACKAGES", ctx.packageDao.searchActivePackages(null, null));
                case 2 -> bookings.viewAll();
                case 3 -> bookings.updateStatus();
                case 4 -> bookings.editDetails();
                case 5 -> bookings.recordPayment();
                case 6 -> bookings.viewCalendar();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option. Please choose 0 to 6.");
            }
        }
    }
}
