package com.autorentpro.model;

public class AvailabilityRow {
    private final int vehicleId;
    private final String registration;
    private final String vehicleLabel;
    private final double dailyPrice;
    private final String generalStatus;
    private final String dateStatus;
    private final String conflict;

    public AvailabilityRow(int vehicleId, String registration, String vehicleLabel, double dailyPrice, String generalStatus, String dateStatus, String conflict) {
        this.vehicleId = vehicleId;
        this.registration = registration;
        this.vehicleLabel = vehicleLabel;
        this.dailyPrice = dailyPrice;
        this.generalStatus = generalStatus;
        this.dateStatus = dateStatus;
        this.conflict = conflict;
    }

    public int getVehicleId() { return vehicleId; }
    public String getRegistration() { return registration; }
    public String getVehicleLabel() { return vehicleLabel; }
    public double getDailyPrice() { return dailyPrice; }
    public String getGeneralStatus() { return generalStatus; }
    public String getDateStatus() { return dateStatus; }
    public String getConflict() { return conflict; }
}
