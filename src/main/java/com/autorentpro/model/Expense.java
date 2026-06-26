package com.autorentpro.model;

import java.time.LocalDate;

public class Expense {
    private final int id;
    private final int vehicleId;
    private final String vehicleLabel;
    private final LocalDate expenseDate;
    private final String category;
    private final String label;
    private final double amount;
    private final String notes;

    public Expense(int id, int vehicleId, String vehicleLabel, LocalDate expenseDate, String category, String label, double amount, String notes) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.vehicleLabel = vehicleLabel;
        this.expenseDate = expenseDate;
        this.category = category;
        this.label = label;
        this.amount = amount;
        this.notes = notes;
    }

    public int getId() { return id; }
    public int getVehicleId() { return vehicleId; }
    public String getVehicleLabel() { return vehicleLabel; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public String getCategory() { return category; }
    public String getLabel() { return label; }
    public double getAmount() { return amount; }
    public String getNotes() { return notes; }
}
