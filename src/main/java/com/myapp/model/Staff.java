package com.myapp.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class Staff {
    private int id;
    private String name;
    private String email;
    private String role;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private String status;
    private LocalDateTime createdAt;

    public Staff() {
    }

    public Staff(int id, String name, String email, String role, LocalTime shiftStart, LocalTime shiftEnd, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalTime getShiftStart() {
        return shiftStart;
    }

    public void setShiftStart(LocalTime shiftStart) {
        this.shiftStart = shiftStart;
    }

    public LocalTime getShiftEnd() {
        return shiftEnd;
    }

    public void setShiftEnd(LocalTime shiftEnd) {
        this.shiftEnd = shiftEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getInitials() {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return String.valueOf(name.charAt(0)).toUpperCase();
    }

    public String getShiftHours() {
        if (shiftStart == null || shiftEnd == null) {
            return "--:--";
        }
        return formatTo12Hour(shiftStart) + " – " + formatTo12Hour(shiftEnd);
    }

    private String formatTo12Hour(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        String amPm = hour >= 12 ? "PM" : "AM";
        int hour12 = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
        return String.format("%d:%02d %s", hour12, minute, amPm);
    }
}