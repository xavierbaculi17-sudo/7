package com.yourdomain.ui;

import com.yourdomain.model.CateringPackage;
import com.yourdomain.model.User;

import java.util.List;

/** Admin package management: view, add, edit, remove (soft delete). */
public class PackageConsole {

    private final AppContext ctx;
    private final User admin;

    public PackageConsole(AppContext ctx, User admin) {
        this.ctx = ctx;
        this.admin = admin;
    }

    public void show() {
        while (true) {
            System.out.println("\n--- MANAGE PACKAGES ---");
            System.out.println("1. View all packages");
            System.out.println("2. Add package");
            System.out.println("3. Edit package");
            System.out.println("4. Remove (discontinue) package");
            System.out.println("0. Back");
            int choice = ctx.input.readInt("Choose an option: ");

            switch (choice) {
                case 1 -> TableView.printPackages("ALL PACKAGES", ctx.packageDao.getAllPackages());
                case 2 -> addPackage();
                case 3 -> editPackage();
                case 4 -> removePackage();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid option. Please choose 0 to 4.");
            }
        }
    }

    private void addPackage() {
        String name = ctx.input.readNonEmpty("Package name (0 to cancel): ");
        if (name.equals("0")) {
            System.out.println("Cancelled.");
            return;
        }
        String menu = ctx.input.readNonEmpty("Menu items (0 to cancel): ");
        if (menu.equals("0")) {
            System.out.println("Cancelled.");
            return;
        }
        String eventType = ctx.input.readNonEmpty("Event type, e.g. WEDDING (0 to cancel): ");
        if (eventType.equals("0")) {
            System.out.println("Cancelled.");
            return;
        }
        double price = readPrice();
        if (price == 0) {
            System.out.println("Cancelled.");
            return;
        }
        int minGuests = readMinGuests();
        if (minGuests == 0) {
            System.out.println("Cancelled.");
            return;
        }

        int id = ctx.packageDao.createPackage(admin.id(), name, menu, eventType, price, minGuests);
        if (id > 0) {
            System.out.println("SUCCESS: Package #" + id + " added.");
        } else {
            System.out.println("Could not add the package.");
        }
    }

    private double readPrice() {
        while (true) {
            double price = ctx.input.readAmount("Price per head (0 to cancel): ");
            if (price >= 0) {
                return price;
            }
        }
    }

    private int readMinGuests() {
        while (true) {
            int guests = ctx.input.readInt("Minimum guests (0 to cancel): ");
            if (guests >= 0) {
                return guests;
            }
            System.out.println("Minimum guests cannot be negative.");
        }
    }

    private void editPackage() {
        List<CateringPackage> all = ctx.packageDao.getAllPackages();
        TableView.printPackages("ALL PACKAGES", all);
        int id = ctx.input.readInt("Enter Package ID to edit (0 to cancel): ");
        if (id == 0) {
            System.out.println("Cancelled.");
            return;
        }
        CateringPackage p = ctx.packageDao.findById(id);
        if (p == null) {
            System.out.println("No package found with ID " + id + ".");
            return;
        }

        System.out.println("Press Enter to keep the current value.");
        String name = ctx.input.readLine("Name [" + p.name() + "]: ");
        String menu = ctx.input.readLine("Menu items [" + p.description() + "]: ");
        String type = ctx.input.readLine("Event type [" + p.eventType() + "]: ");
        Double price = ctx.input.readOptionalAmount("Price per head [" + p.pricePerHead() + "]: ");
        Integer min = ctx.input.readOptionalInt("Minimum guests [" + p.minGuests() + "]: ");
        if (min != null && min <= 0) {
            System.out.println("Minimum guests must be greater than 0. Nothing was changed.");
            return;
        }

        boolean ok = ctx.packageDao.updatePackage(admin.id(), id,
                name.isEmpty() ? p.name() : name,
                menu.isEmpty() ? p.description() : menu,
                type.isEmpty() ? p.eventType() : type,
                price == null ? p.pricePerHead() : price,
                min == null ? p.minGuests() : min);
        System.out.println(ok ? "SUCCESS: Package #" + id + " updated." : "Update failed.");
    }

    private void removePackage() {
        TableView.printPackages("ALL PACKAGES", ctx.packageDao.getAllPackages());
        int id = ctx.input.readInt("Enter Package ID to discontinue (0 to cancel): ");
        if (id == 0) {
            System.out.println("Cancelled.");
            return;
        }
        CateringPackage p = ctx.packageDao.findById(id);
        if (p == null || !p.status().equals("ACTIVE")) {
            System.out.println("No ACTIVE package found with ID " + id + ".");
            return;
        }
        if (!ctx.input.confirm("Customers will no longer see '" + p.name() + "'.")) {
            System.out.println("Cancelled.");
            return;
        }
        System.out.println(ctx.packageDao.discontinuePackage(admin.id(), id)
                ? "SUCCESS: Package #" + id + " discontinued (kept in the database)."
                : "Could not discontinue the package.");
    }
}
