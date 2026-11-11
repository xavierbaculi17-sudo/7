package com.yourdomain.ui;

import com.yourdomain.model.User;

import java.util.Scanner;

/** Login / logout (both audit-logged), customer registration, and role-based menu routing. */
public class Authentication {

    private final AppContext ctx;

    public Authentication(Scanner scanner) {
        this.ctx = new AppContext(scanner);
    }

    public void start() {
        while (true) {
            System.out.println("\n===== ONLINE EVENT CATERING & BOOKING SYSTEM =====");
            System.out.println("1. Login");
            System.out.println("2. Register as a Customer");
            System.out.println("0. Exit");
            int choice = ctx.input.readInt("Choose an option: ");

            switch (choice) {
                case 1 -> login();
                case 2 -> register();
                case 0 -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid option. Please choose 0, 1, or 2.");
            }
        }
    }

    public void login() {
        String username = ctx.input.readLine("Username: ");
        String password = ctx.input.readLine("Password: ");

        User user = ctx.userDao.login(username, password);
        if (user == null) {
            System.out.println("Invalid username or password.");
            return;
        }
        if (user.status().equals("PENDING")) {
            System.out.println("Your account is PENDING. Please wait for an admin to approve it.");
            return;
        }
        if (!user.status().equals("APPROVED")) {
            System.out.println("Your account is " + user.status() + ". Please contact the administrator.");
            return;
        }

        ctx.logDao.logAction(user.id(), "USER_LOGIN",
                String.format("User %s (ID: %d) logged in.", user.username(), user.id()));
        System.out.println("Welcome, " + user.fullName() + "! (" + user.roles() + ")");

        switch (user.roles()) {
            case "ADMIN" -> new AdminMenu(ctx, user).show();
            case "STAFF" -> new StaffMenu(ctx, user).show();
            case "CUSTOMER" -> new CustomerMenu(ctx, user).show();
            default -> System.out.println("Unknown role: " + user.roles());
        }

        logout(user);
    }

    public void logout(User user) {
        ctx.logDao.logAction(user.id(), "USER_LOGOUT",
                String.format("User %s (ID: %d) logged out.", user.username(), user.id()));
        System.out.println("Logged out.");
    }

    private void register() {
        String username = ctx.input.readLine("Choose a username (max 15 chars, no spaces, 0 to cancel): ");
        if (username.equals("0")) {
            System.out.println("Registration cancelled.");
            return;
        }
        if (username.isEmpty() || username.contains(" ") || username.length() > 15) {
            System.out.println("Username must be 1-15 characters with no spaces.");
            return;
        }
        if (ctx.userDao.usernameExists(username)) {
            System.out.println("That username is already taken.");
            return;
        }

        String password = ctx.input.readLine("Choose a password (at least 4 characters, 0 to cancel): ");
        if (password.equals("0")) {
            System.out.println("Registration cancelled.");
            return;
        }
        if (password.length() < 4) {
            System.out.println("Password is too short.");
            return;
        }

        String fullName = ctx.input.readNonEmpty("Full name (0 to cancel): ");
        if (fullName.equals("0")) {
            System.out.println("Registration cancelled.");
            return;
        }
        String contact = ctx.input.readNonEmpty("Contact number (0 to cancel): ");
        if (contact.equals("0")) {
            System.out.println("Registration cancelled.");
            return;
        }
        String email;
        while (true) {
            email = ctx.input.readNonEmpty("Email (0 to cancel): ");
            if (email.equals("0")) {
                System.out.println("Registration cancelled.");
                return;
            }
            if (email.contains("@") && email.indexOf('.', email.indexOf('@')) > 0) {
                break;
            }
            System.out.println("Please enter a valid email address.");
        }

        if (ctx.userDao.register(username, password, fullName, contact, email) > 0) {
            System.out.println("Registered! Your account is PENDING until an admin approves it.");
        } else {
            System.out.println("Registration failed.");
        }
    }
}
