package com.yourdomain.ui;

import com.yourdomain.model.Booking;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Admin reports: monthly bookings & income, and upcoming events & unpaid balances. */
public class ReportConsole {

    private final AppContext ctx;

    public ReportConsole(AppContext ctx) {
        this.ctx = ctx;
    }

    public void show() {
        while (true) {
            System.out.println("\n--- REPORTS ---");
            System.out.println("1. Monthly Bookings & Income Report");
            System.out.println("2. Upcoming Events & Unpaid Balance Report");
            System.out.println("0. Back");
            int choice = ctx.input.readInt("Choose an option: ");

            switch (choice) {
                case 1 -> monthlyReport();
                case 2 -> upcomingReport();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option. Please choose 0, 1, or 2.");
            }
        }
    }

    private void monthlyReport() {
        String month = ctx.input.readYearMonth("Month to report (yyyy-MM, 0 to cancel): ");
        if (month.equals("0")) {
            System.out.println("Cancelled.");
            return;
        }

        Map<String, Integer> counts = ctx.reportDao.getStatusCounts(month);
        List<String[]> rows = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            rows.add(new String[]{e.getKey(), String.valueOf(e.getValue())});
            total += e.getValue();
        }
        TableView.printTable("MONTHLY BOOKINGS & INCOME REPORT - " + month,
                new String[]{"Booking Status", "Count"}, new int[]{16, 8}, rows);
        System.out.println("Total bookings for events this month : " + total);
        System.out.println("Total sales (CONFIRMED + COMPLETED)  : "
                + TableView.money(ctx.reportDao.getTotalSales(month)));
        System.out.println("Payments collected this month        : "
                + TableView.money(ctx.reportDao.getPaymentsCollected(month)));
    }

    private void upcomingReport() {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(30);

        TableView.printBookings("UPCOMING CONFIRMED EVENTS (" + today + " to " + until + ")",
                ctx.bookingDao.getConfirmedInRange(today.toString(), until.toString()), true);

        List<String[]> rows = new ArrayList<>();
        double totalBalance = 0;
        for (Booking b : ctx.bookingDao.getAllBookings()) {
            boolean counts = b.status().equals("CONFIRMED") || b.status().equals("COMPLETED");
            double balance = Math.round(b.balance() * 100.0) / 100.0;
            if (counts && balance > 0) {
                rows.add(new String[]{String.valueOf(b.id()), TableView.cut(b.customerName(), 15), b.eventDate(),
                        b.status(), TableView.money(b.totalAmount()), TableView.money(b.paid()),
                        TableView.money(balance)});
                totalBalance += balance;
            }
        }
        TableView.printTable("BOOKINGS WITH UNPAID BALANCE",
                new String[]{"ID", "Customer", "Event Date", "Status", "Total", "Paid", "Balance"},
                new int[]{4, 15, 10, 10, 13, 13, 13}, rows);
        System.out.println("Total outstanding balance: " + TableView.money(totalBalance));
    }
}
