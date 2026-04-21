package com.myapp.model;

public class Ingredient {
    private int id;
    private String name;
    private String unit;
    private double quantity;
    private double minThreshold;
    private double maxStock;
    private String status;
    private String availabilityStatus;

    public Ingredient(int id, String name, String unit, double quantity, double minThreshold, double maxStock) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.quantity = quantity;
        this.minThreshold = minThreshold;
        this.maxStock = maxStock;
        this.status = calculateStatus();
        this.availabilityStatus = "Available";
    }

    public Ingredient(int id, String name, String unit, double quantity, double minThreshold, double maxStock, String availabilityStatus) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.quantity = quantity;
        this.minThreshold = minThreshold;
        this.maxStock = maxStock;
        this.status = calculateStatus();
        this.availabilityStatus = availabilityStatus != null ? availabilityStatus : "Available";
    }

    private String calculateStatus() {
        if (quantity <= 0) {
            return "Out";
        } else if (quantity <= minThreshold) {
            return "Low";
        } else {
            return "OK";
        }
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public double getQuantity() { return quantity; }
    public double getMinThreshold() { return minThreshold; }
    public double getMaxStock() { return maxStock; }

    public double getCriticalThreshold() {
        return maxStock * 0.25;
    }

    public String getStatus() { return status; }
    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
        this.status = calculateStatus();
    }

    public double getPercentage() {
        if (minThreshold <= 0) return 100;
        return Math.min(100, (quantity / minThreshold) * 50);
    }

    public int getStockPercentage() {
        if (maxStock <= 0) return 0;
        if (quantity <= 0) return 0;
        return (int) Math.min((quantity / maxStock) * 100, 100);
    }
}