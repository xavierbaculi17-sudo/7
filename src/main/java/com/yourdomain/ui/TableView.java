package com.yourdomain.ui;

import com.yourdomain.model.ActivityLog;
import com.yourdomain.model.Booking;
import com.yourdomain.model.CateringPackage;
import com.yourdomain.model.Payment;
import com.yourdomain.model.User;

import java.util.ArrayList;
import java.util.List;

/** Console table rendering (display only, no SQL). */
public final class TableView {

    private static final String USER_ROW = "| %-5s | %-15s | %-12s | %-10s |%n";
    private static final String USER_BORDER = "+-" + "-".repeat(5) + "-+-" + "-".repeat(15)
            + "-+-" + "-".repeat(12) + "-+-" + "-".repeat(10) + "-+";

    private static final String LOG_FORMAT = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
    private static final int LOG_DESC_WIDTH = 35;

    private TableView() {
    }

    public static String money(double amount) {
        return String.format("P%,.2f", amount);
    }

    static String cut(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }

    // ---------- Users (Deliverable 5 formatters: %-5s, %-15s, %-12s, %-10s) ----------

    public static void printUsers(String title, List<User> users) {
        System.out.println("\n" + title);
        System.out.println(USER_BORDER);
        System.out.printf(USER_ROW, "ID", "Username", "Role", "Status");
        System.out.println(USER_BORDER);
        if (users.isEmpty()) {
            System.out.printf("| %-" + (USER_BORDER.length() - 4) + "s |%n", "(no users to display)");
        }
        for (User u : users) {
            System.out.printf(USER_ROW, u.id(), cut(u.username(), 15), u.roles(), u.status());
        }
        System.out.println(USER_BORDER);
    }

    // ---------- Generic aligned table ----------

    public static void printTable(String title, String[] headers, int[] widths, List<String[]> rows) {
        StringBuilder fmt = new StringBuilder("|");
        StringBuilder border = new StringBuilder("+");
        for (int w : widths) {
            fmt.append(" %-").append(w).append("s |");
            border.append("-".repeat(w + 2)).append("+");
        }
        fmt.append("%n");

        if (title != null) {
            System.out.println("\n" + title);
        }
        System.out.println(border);
        System.out.printf(fmt.toString(), (Object[]) headers);
        System.out.println(border);
        if (rows.isEmpty()) {
            System.out.printf("| %-" + (border.length() - 4) + "s |%n", "(nothing to display)");
        }
        for (String[] row : rows) {
            System.out.printf(fmt.toString(), (Object[]) row);
        }
        System.out.println(border);
    }

    // ---------- Packages ----------

    public static void printPackages(String title, List<CateringPackage> packages) {
        List<String[]> rows = new ArrayList<>();
        for (CateringPackage p : packages) {
            rows.add(new String[]{String.valueOf(p.id()), cut(p.name(), 26), cut(p.eventType(), 12),
                    money(p.pricePerHead()), String.valueOf(p.minGuests()), p.status()});
        }
        printTable(title, new String[]{"ID", "Package", "Event Type", "Price/Head", "Min Guests", "Status"},
                new int[]{4, 26, 12, 12, 10, 12}, rows);
        for (CateringPackage p : packages) {
            System.out.println("  Package " + p.id() + " menu: " + p.description());
        }
    }

    // ---------- Bookings ----------

    public static void printBookings(String title, List<Booking> bookings, boolean showCustomer) {
        List<String[]> rows = new ArrayList<>();
        for (Booking b : bookings) {
            if (showCustomer) {
                rows.add(new String[]{String.valueOf(b.id()), cut(b.customerName(), 15), cut(b.packageName(), 22),
                        b.eventDate(), String.valueOf(b.guestCount()), money(b.totalAmount()),
                        money(b.paid()), b.status()});
            } else {
                rows.add(new String[]{String.valueOf(b.id()), cut(b.packageName(), 22),
                        b.eventDate(), String.valueOf(b.guestCount()), money(b.totalAmount()),
                        money(b.paid()), b.status()});
            }
        }
        if (showCustomer) {
            printTable(title, new String[]{"ID", "Customer", "Package", "Event Date", "Guests", "Total", "Paid", "Status"},
                    new int[]{4, 15, 22, 10, 6, 13, 13, 10}, rows);
        } else {
            printTable(title, new String[]{"ID", "Package", "Event Date", "Guests", "Total", "Paid", "Status"},
                    new int[]{4, 22, 10, 6, 13, 13, 10}, rows);
        }
    }

    public static void printPayments(String title, List<Payment> payments) {
        List<String[]> rows = new ArrayList<>();
        for (Payment p : payments) {
            rows.add(new String[]{String.valueOf(p.id()), money(p.amount()), p.type(), p.method(),
                    p.paymentDate(), cut(p.receivedBy(), 15)});
        }
        printTable(title, new String[]{"Pay ID", "Amount", "Type", "Method", "Date", "Received By"},
                new int[]{6, 13, 12, 13, 19, 15}, rows);
    }

    // ---------- Audit log (Deliverable 8 format) ----------

    public static void printLogs(String title, List<ActivityLog> logs) {
        int width = 5 + 3 + 8 + 3 + 18 + 3 + LOG_DESC_WIDTH + 3 + 20;
        System.out.println("\n" + title);
        System.out.printf(LOG_FORMAT, "ID", "User ID", "Action", "Description", "Timestamp");
        System.out.println("-".repeat(width));
        if (logs.isEmpty()) {
            System.out.println("(no log entries)");
        }
        for (ActivityLog log : logs) {
            List<String> parts = wrap(log.description(), LOG_DESC_WIDTH);
            System.out.printf(LOG_FORMAT, log.id(), log.userId(), log.actionType(), parts.get(0), log.timestamp());
            for (int i = 1; i < parts.size(); i++) {
                System.out.printf(LOG_FORMAT, "", "", "", parts.get(i), "");
            }
        }
        System.out.println("-".repeat(width));
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            while (word.length() > width) {
                if (cur.length() > 0) {
                    lines.add(cur.toString());
                    cur.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (cur.length() == 0) {
                cur.append(word);
            } else if (cur.length() + 1 + word.length() <= width) {
                cur.append(' ').append(word);
            } else {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(word);
            }
        }
        if (cur.length() > 0 || lines.isEmpty()) {
            lines.add(cur.toString());
        }
        return lines;
    }
}
