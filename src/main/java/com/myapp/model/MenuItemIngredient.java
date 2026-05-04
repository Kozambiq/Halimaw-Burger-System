package com.myapp.model;

public class MenuItemIngredient {
    private int ingredientId;
    private String ingredientName;
    private String unit;
    private double quantity;

    public MenuItemIngredient(int ingredientId, String ingredientName, String unit, double quantity) {
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.unit = unit;
        this.quantity = quantity;
    }

    public int getIngredientId() { return ingredientId; }
    public String getIngredientName() { return ingredientName; }
    public String getUnit() { return unit; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
}