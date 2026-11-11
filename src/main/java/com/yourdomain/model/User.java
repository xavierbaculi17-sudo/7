package com.yourdomain.model;

/** Row of the users table (Java Record). */
public record User(int id, String username, String fullName, String contactNo,
                   String email, String roles, String status) {
}
