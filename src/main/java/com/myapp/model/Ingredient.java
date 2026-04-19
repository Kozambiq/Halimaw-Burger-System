package com.myapp.model;

public class Ingredient {
    private int id;
    private String name;
    private String unit;
    private double quantity;
    private double minThreshold;
    private String status;

    public Ingredient(int id, String name, String unit, double quantity, double minThreshold) {
        this.id = id;
        this.name = name;
        this.unit = unit;
        this.quantity = quantity;
        this.minThreshold = minThreshold;
        this.status = calculateStatus();
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
    public String getStatus() { return status; }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
        this.status = calculateStatus();
    }

    public double getPercentage() {
        if (minThreshold <= 0) return 100;
        return Math.min(100, (quantity / minThreshold) * 50);
    }

    public int getStockPercentage() {
        if (quantity <= 0) return 0;
        if (minThreshold <= 0) return 100;
        int pct = (int) ((quantity / minThreshold) * 100);
        return Math.min(100, pct);
    }
}