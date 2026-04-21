package com.myapp.model;

import java.time.LocalDate;

public class Combo {
    private int id;
    private String name;
    private String includes;
    private double promoPrice;
    private double originalPrice;
    private LocalDate validUntil;
    private String status;

    public Combo(int id, String name, String includes, double promoPrice, double originalPrice, LocalDate validUntil, String status) {
        this.id = id;
        this.name = name;
        this.includes = includes;
        this.promoPrice = promoPrice;
        this.originalPrice = originalPrice;
        this.validUntil = validUntil;
        this.status = status;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getIncludes() { return includes; }
    public double getPromoPrice() { return promoPrice; }
    public double getOriginalPrice() { return originalPrice; }
    public LocalDate getValidUntil() { return validUntil; }
    public String getStatus() { return status; }

    public double getSavings() {
        return originalPrice - promoPrice;
    }

    public String getFormattedPromoPrice() {
        return String.format("₱%.2f", promoPrice);
    }

    public String getFormattedSavings() {
        return String.format("₱%.2f", getSavings());
    }
}
