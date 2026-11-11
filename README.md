# Online Event Catering and Booking System (StudentConsoleApp)

Java 17 + Maven + SQLite. `students.db` is created automatically on the first run.
Implements Deliverables 5, 7 and 8 inside one system built around the approved proposal.

## Structure
```
src/main/java/com/yourdomain/
├── Main.java                    single entry point (DatabaseInitializer -> Authentication)
├── dao/   DatabaseConnection, DatabaseInitializer, UserDao, PackageDao, BookingDao,
│          PaymentDao, ReportDao, ActivityLogDao
├── model/ User, CateringPackage, Booking, Payment, ActivityLog   (Java records)
├── util/  PasswordUtil          SHA-256 hashing
└── ui/    Authentication, AdminMenu, StaffMenu, CustomerMenu,
           BookingConsole, PackageConsole, ReportConsole, AppContext, Input, TableView
```

## Run in IntelliJ
1. JDK 17+. File > Open > this folder (with pom.xml) > Trust Project.
2. Let Maven import (internet needed the first time).
3. Run the "Main" configuration (or right-click Main.java > Run).

## Seed accounts
| Username  | Password | Role     | Status   |
|-----------|----------|----------|----------|
| admin     | admin123 | ADMIN    | APPROVED |
| staff1    | staff123 | STAFF    | APPROVED |
| customer1 | cust123  | CUSTOMER | APPROVED |
| customer2 | cust123  | CUSTOMER | PENDING  |

## Where each deliverable lives
| Deliverable | Code |
|---|---|
| D5 approval | `UserDao.getAllUsers/getPendingUsers/approveUser`, AdminMenu options 1-3, `TableView.printUsers` |
| D7 update | `UserDao.updateUserRoleAndStatus`, AdminMenu option 4 (`readIntInput`, `readValidRole`, `readValidStatus`, self-edit warning, 0 = cancel) |
| D8 audit | `ActivityLogDao`, `DatabaseInitializer` (activity_logs), AdminMenu option 9 |

## Logged events (action_type)
USER_LOGIN, USER_LOGOUT, USER_REGISTERED, USER_APPROVED, USER_UPDATED, USER_DEACTIVATED,
USER_REACTIVATED, PACKAGE_CREATED, PACKAGE_UPDATED, PACKAGE_REMOVED, BOOKING_CREATED,
BOOKING_UPDATED, BOOKING_CANCELLED, BOOKING_DELETED, PAYMENT_RECORDED, LOGS_CLEARED

## Demo script for the screenshots
**D7 (update user):** admin > 4 > `abc` (validation) > `1` (self-edit warning) > `3`, role `boss` (invalid),
`staff`, status `approved` > SUCCESS. DB Browser: users row 3 shows STAFF / APPROVED.

**D5 (approval):** register a customer (menu 2) > admin > 2 (pending table) > 3 > approve that ID.

**D8 (audit):** log in as customer1 > 4 submit booking > logout > admin > 7 > 2 confirm it > 4 record a payment
> 9 > 1 view all logs. DB Browser: open `activity_logs`.
