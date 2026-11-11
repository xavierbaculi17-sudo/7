package com.yourdomain.ui;

import com.yourdomain.dao.ActivityLogDao;
import com.yourdomain.dao.BookingDao;
import com.yourdomain.dao.PackageDao;
import com.yourdomain.dao.PaymentDao;
import com.yourdomain.dao.ReportDao;
import com.yourdomain.dao.UserDao;

import java.util.Scanner;

/** Holds the shared DAOs and input helper so every menu uses the same instances. */
public class AppContext {

    public final Input input;
    public final UserDao userDao = new UserDao();
    public final PackageDao packageDao = new PackageDao();
    public final BookingDao bookingDao = new BookingDao();
    public final PaymentDao paymentDao = new PaymentDao();
    public final ReportDao reportDao = new ReportDao();
    public final ActivityLogDao logDao = new ActivityLogDao();

    public AppContext(Scanner scanner) {
        this.input = new Input(scanner);
    }
}
