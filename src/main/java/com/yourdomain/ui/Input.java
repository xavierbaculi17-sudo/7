package com.yourdomain.ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Safe console input: every method re-prompts instead of throwing on bad data. */
public class Input {

    private final Scanner scanner;

    public Input(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Re-prompts until a whole number is typed. */
    public int readInt(String prompt) {
        while (true) {
            String line = readLine(prompt);
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter numbers only.");
            }
        }
    }

    /** Whole number, or null when the line is blank (keep current value). */
    public Integer readOptionalInt(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number or press Enter.");
            }
        }
    }

    /** Money amount >= 0 (0 usually means cancel). */
    public double readAmount(String prompt) {
        while (true) {
            String line = readLine(prompt);
            try {
                double value = Double.parseDouble(line);
                if (Double.isFinite(value) && value >= 0) {
                    return Math.round(value * 100.0) / 100.0;
                }
                System.out.println("Amount cannot be negative.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a number such as 5000 or 2500.50.");
            }
        }
    }

    /** Positive number, or null when the line is blank. */
    public Double readOptionalAmount(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line.isEmpty()) {
                return null;
            }
            try {
                double value = Double.parseDouble(line);
                if (Double.isFinite(value) && value > 0) {
                    return Math.round(value * 100.0) / 100.0;
                }
                System.out.println("Amount must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Enter a number or press Enter.");
            }
        }
    }

    /** Non-empty text. Returns "0" if the user cancels. */
    public String readNonEmpty(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line.equals("0")) {
                return "0";
            }
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    /** ISO date (yyyy-MM-dd) after today. Returns "0" if the user cancels. */
    public String readFutureDate(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line.equals("0")) {
                return "0";
            }
            try {
                LocalDate date = LocalDate.parse(line);
                if (!date.isAfter(LocalDate.now())) {
                    System.out.println("The date must be in the future.");
                    continue;
                }
                return date.toString();
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use yyyy-MM-dd (for example 2026-12-25).");
            }
        }
    }

    /** Month as yyyy-MM. Returns "0" if the user cancels. */
    public String readYearMonth(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line.equals("0")) {
                return "0";
            }
            try {
                return YearMonth.parse(line).toString();
            } catch (DateTimeParseException e) {
                System.out.println("Invalid month. Use yyyy-MM (for example 2026-12).");
            }
        }
    }

    /** True only when the user types YES. */
    public boolean confirm(String prompt) {
        return readLine(prompt + " Type YES to confirm: ").equals("YES");
    }
}
