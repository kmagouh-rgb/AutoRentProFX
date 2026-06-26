package com.autorentpro.model;

import java.time.LocalDate;

public class Maintenance {
    private final int id;
    private final int vehicleId;
    private final String vehicleLabel;
    private final LocalDate maintenanceDate;
    private final String type;
    private final int mileage;
    private final double amount;
    private final String status;
    private final String notes;

    public Maintenance(int id, int vehicleId, String vehicleLabel, LocalDate maintenanceDate, String type, int mileage, double amount, String status, String notes) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.vehicleLabel = vehicleLabel;
        this.maintenanceDate = maintenanceDate;
        this.type = type;
        this.mileage = mileage;
        this.amount = amount;
        this.status = status;
        this.notes = notes;
    }

    public int getId() { return id; }
    public int getVehicleId() { return vehicleId; }
    public String getVehicleLabel() { return vehicleLabel; }
    public LocalDate getMaintenanceDate() { return maintenanceDate; }
    public String getType() { return type; }
    public int getMileage() { return mileage; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
}
