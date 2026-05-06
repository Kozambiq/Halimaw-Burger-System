package com.myapp.model;

import java.time.LocalDateTime;

public class RestockLog {
    private int id;
    private int ingredientId;
    private String ingredientName;
    private double quantityAdded;
    private double previousQuantity;
    private String restockedBy;
    private LocalDateTime restockedAt;
    private String notes;
    private String unit;

    public RestockLog(int id, int ingredientId, String ingredientName, double quantityAdded, 
                      double previousQuantity, LocalDateTime restockedAt, String notes, String unit) {
        this.id = id;
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.quantityAdded = quantityAdded;
        this.previousQuantity = previousQuantity;
        this.restockedAt = restockedAt;
        this.notes = notes;
        this.unit = unit;
    }

    public int getId() { return id; }
    public int getIngredientId() { return ingredientId; }
    public String getIngredientName() { return ingredientName; }
    public double getQuantityAdded() { return quantityAdded; }
    public double getPreviousQuantity() { return previousQuantity; }
    public LocalDateTime getRestockedAt() { return restockedAt; }
    public String getNotes() { return notes; }
    public String getUnit() { return unit; }
    public double getAfterQuantity() { return previousQuantity + quantityAdded; }
}
