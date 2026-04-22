package com.myapp.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class Staff {
    private int id;
    private String name;
    private String role;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private String status;
    private LocalDateTime createdAt;

    public Staff() {
    }

    public Staff(int id, String name, String role, LocalTime shiftStart, LocalTime shiftEnd, String status) {
        this.id = id;
        this.name = name;
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
        String start = String.format("%02d:%02d", shiftStart.getHour(), shiftStart.getMinute());
        String end = String.format("%02d:%02d", shiftEnd.getHour(), shiftEnd.getMinute());
        return start + "–" + end;
    }
}