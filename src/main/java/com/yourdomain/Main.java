package com.yourdomain;

import com.yourdomain.dao.DatabaseInitializer;
import com.yourdomain.ui.Authentication;

import java.util.Scanner;

/** Single entry point: initialize the database first, then hand control to Authentication. */
public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        try (Scanner scanner = new Scanner(System.in)) {
            new Authentication(scanner).start();
        }
    }
}
