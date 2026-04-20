package com.myapp.model;

public class MenuItemModel {
    private int id;
    private String name;
    private String category;
    private double price;
    private String availability;

    public MenuItemModel(int id, String name, String category, double price, String availability) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.availability = availability;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getAvailability() { return availability; }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getFormattedPrice() {
        return String.format("₱%.2f", price);
    }
}