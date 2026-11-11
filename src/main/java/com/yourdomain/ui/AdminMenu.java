package com.yourdomain.ui;

import com.yourdomain.model.User;

import java.util.List;
import java.util.Set;

/**
 * Admin dashboard. Covers: user approval (D5), role/status update with validation and the
 * self-edit restriction (D7), the System Audit Log viewer (D8), plus packages, bookings and reports.
 */
public class AdminMenu {

    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "STAFF", "CUSTOMER");
    private static final Set<String> VALID_STATUSES = Set.of("APPROVED", "PENDING");

    private final AppContext ctx;
    private final User currentAdmin;
    private final BookingConsole bookings;
    private final PackageConsole packages;
    private final ReportConsole reports;

    public AdminMenu(AppContext ctx, User currentAdmin) {
        this.ctx = ctx;
        this.currentAdmin = currentAdmin;
        this.bookings = new BookingConsole(ctx, currentAdmin);
        this.packages = new PackageConsole(ctx, currentAdmin);
        this.reports = new ReportConsole(ctx);
    }

    public void show() {
        while (true) {
            System.out.println("\n===== ADMIN MENU (" + currentAdmin.username() + ") =====");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Update User (Role / Status)");
            System.out.println("5. Deactivate / Reactivate User");
            System.out.println("6. Manage Packages");
            System.out.println("7. Manage Bookings & Payments");
            System.out.println("8. Reports");
            System.out.println("9. View System Audit Logs");
            System.out.println("0. Logout");
            int choice = readIntInput("Choose an option: ");

            switch (choice) {
                case 1 -> TableView.printUsers("ALL SYSTEM USERS", ctx.userDao.getAllUsers());
                case 2 -> TableView.printUsers("PENDING REGISTRATIONS", ctx.userDao.getPendingUsers());
                case 3 -> approveUserAccount();
                case 4 -> updateUser();
                case 5 -> toggleUserActive();
                case 6 -> packages.show();
                case 7 -> bookingMenu();
                case 8 -> reports.show();
                case 9 -> auditLogMenu();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option. Please choose 0 to 9.");
            }
        }
    }

    // ================= Input validation wrappers (Deliverable 7) =================

    /** Re-prompts until a whole number is typed; letters/symbols never crash the app. */
    public int readIntInput(String prompt) {
        return ctx.input.readInt(prompt);
    }

    /** Keeps asking until a valid role is entered. Returns "0" if the admin cancels. */
    public String readValidRole() {
        while (true) {
            String input = ctx.input.readLine("Enter new role (ADMIN / STAFF / CUSTOMER, 0 to cancel): ")
                    .toUpperCase();
            if (input.equals("0")) {
                return "0";
            }
            if (VALID_ROLES.contains(input)) {
                return input;
            }
            System.out.println("Invalid role. Valid roles: ADMIN, STAFF, CUSTOMER.");
        }
    }

    /** Keeps asking until a valid status is entered. Returns "0" if the admin cancels. */
    public String readValidStatus() {
        while (true) {
            String input = ctx.input.readLine("Enter new status (APPROVED / PENDING, 0 to cancel): ")
                    .toUpperCase();
            if (input.equals("0")) {
                return "0";
            }
            if (VALID_STATUSES.contains(input)) {
                return input;
            }
            System.out.println("Invalid status. Valid statuses: APPROVED, PENDING.");
        }
    }

    // ================= Deliverable 5: approval =================

    private void approveUserAccount() {
        List<User> pending = ctx.userDao.getPendingUsers();
        if (pending.isEmpty()) {
            System.out.println("\nNo accounts are waiting for approval.");
            return;
        }
        TableView.printUsers("PENDING REGISTRATIONS", pending);

        while (true) {
            int targetUserId = readIntInput("Enter User ID to approve (0 to cancel): ");
            if (targetUserId == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            User target = null;
            for (User u : pending) {
                if (u.id() == targetUserId) {
                    target = u;
                    break;
                }
            }
            if (target == null) {
                System.out.println("User ID " + targetUserId + " is not in the pending list. Try again.");
                continue;
            }

            if (ctx.userDao.approveUser(currentAdmin.id(), targetUserId)) {
                System.out.println("SUCCESS: " + target.roles() + " account '" + target.username()
                        + "' (ID " + targetUserId + ") has been APPROVED.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    // ================= Deliverable 7: update role & status =================

    private void updateUser() {
        List<User> users = ctx.userDao.getAllUsers();
        TableView.printUsers("ALL SYSTEM USERS", users);

        int targetUserId = readIntInput("Enter User ID to update (0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("Update cancelled.");
            return;
        }

        if (targetUserId == currentAdmin.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        boolean exists = users.stream().anyMatch(u -> u.id() == targetUserId);
        if (!exists) {
            System.out.println("No user found with ID " + targetUserId + ".");
            return;
        }

        String newRole = readValidRole();
        if (newRole.equals("0")) {
            System.out.println("Update cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus.equals("0")) {
            System.out.println("Update cancelled.");
            return;
        }

        if (ctx.userDao.updateUserRoleAndStatus(currentAdmin.id(), targetUserId, newRole, newStatus)) {
            System.out.println("SUCCESS: User ID " + targetUserId + " updated to role "
                    + newRole + " and status " + newStatus + ".");
        } else {
            System.out.println("Update failed. Please try again.");
        }
    }

    // ================= Soft delete of accounts =================

    private void toggleUserActive() {
        List<User> users = ctx.userDao.getAllUsers();
        TableView.printUsers("ALL SYSTEM USERS", users);

        int targetUserId = readIntInput("Enter User ID to deactivate/reactivate (0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("Cancelled.");
            return;
        }
        if (targetUserId == currentAdmin.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        User target = null;
        for (User u : users) {
            if (u.id() == targetUserId) {
                target = u;
                break;
            }
        }
        if (target == null) {
            System.out.println("No user found with ID " + targetUserId + ".");
            return;
        }

        if (target.status().equals("INACTIVE")) {
            if (ctx.input.confirm("Reactivate '" + target.username() + "'?")
                    && ctx.userDao.reactivateUser(currentAdmin.id(), targetUserId)) {
                System.out.println("SUCCESS: '" + target.username() + "' is active again (APPROVED).");
            } else {
                System.out.println("No change made.");
            }
        } else {
            if (ctx.input.confirm("Deactivate '" + target.username() + "'? They will not be able to log in.")
                    && ctx.userDao.deactivateUser(currentAdmin.id(), targetUserId)) {
                System.out.println("SUCCESS: '" + target.username() + "' is now INACTIVE (record kept).");
            } else {
                System.out.println("No change made.");
            }
        }
    }

    // ================= Bookings & payments =================

    private void bookingMenu() {
        while (true) {
            System.out.println("\n--- MANAGE BOOKINGS & PAYMENTS ---");
            System.out.println("1. View all bookings");
            System.out.println("2. Update booking status");
            System.out.println("3. Edit event details (date / venue / guests)");
            System.out.println("4. Record a payment");
            System.out.println("5. View booking calendar");
            System.out.println("6. Remove unconfirmed / cancelled booking");
            System.out.println("0. Back");
            int choice = readIntInput("Choose an option: ");

            switch (choice) {
                case 1 -> bookings.viewAll();
                case 2 -> bookings.updateStatus();
                case 3 -> bookings.editDetails();
                case 4 -> bookings.recordPayment();
                case 5 -> bookings.viewCalendar();
                case 6 -> bookings.removeUnconfirmed();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option. Please choose 0 to 6.");
            }
        }
    }

    // ================= Deliverable 8: audit logs =================

    private void auditLogMenu() {
        while (true) {
            System.out.println("\n--- SYSTEM AUDIT LOGS ---");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Booking Audit Log (bookings & payments)");
            System.out.println("4. User Activity Log (login / logout / registration)");
            System.out.println("5. Clear All Logs");
            System.out.println("0. Back");
            int choice = readIntInput("Choose an option: ");

            switch (choice) {
                case 1 -> TableView.printLogs("ALL SYSTEM LOGS", ctx.logDao.getAllLogs());
                case 2 -> filterLogsByUser();
                case 3 -> TableView.printLogs("BOOKING AUDIT LOG", ctx.logDao.getBookingAuditLogs());
                case 4 -> TableView.printLogs("USER ACTIVITY LOG", ctx.logDao.getUserActivityLogs());
                case 5 -> clearLogs();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option. Please choose 0 to 5.");
            }
        }
    }

    private void filterLogsByUser() {
        int userId = readIntInput("Enter User ID to filter by (0 to cancel): ");
        if (userId == 0) {
            System.out.println("Filter cancelled.");
            return;
        }
        var logs = ctx.logDao.getLogsByUserId(userId);
        if (logs.isEmpty()) {
            System.out.println("No log entries found for User ID " + userId + ".");
            return;
        }
        TableView.printLogs("LOGS FOR USER ID " + userId, logs);
    }

    private void clearLogs() {
        if (!ctx.input.confirm("This permanently deletes ALL log entries.")) {
            System.out.println("Clear cancelled. Nothing was deleted.");
            return;
        }
        int removed = ctx.logDao.clearAllLogs();
        if (removed < 0) {
            System.out.println("Could not clear the logs.");
            return;
        }
        System.out.println("Cleared " + removed + " log entries.");
        ctx.logDao.logAction(currentAdmin.id(), "LOGS_CLEARED",
                String.format("Admin %d cleared %d log entries", currentAdmin.id(), removed));
    }
}
