package com.autorentpro.model;

import java.time.LocalDate;

public class Contract {
    private int id;
    private String contractNumber;
    private int vehicleId;
    private int customerId;
    private String vehicleLabel;
    private String customerLabel;
    private LocalDate startDate;
    private LocalDate endDate;
    private double dailyPrice;
    private double totalAmount;
    private double paidAmount;
    private String status;

    public Contract(int id, String contractNumber, int vehicleId, int customerId, String vehicleLabel, String customerLabel,
                    LocalDate startDate, LocalDate endDate, double dailyPrice, double totalAmount, double paidAmount, String status) {
        this.id = id;
        this.contractNumber = contractNumber;
        this.vehicleId = vehicleId;
        this.customerId = customerId;
        this.vehicleLabel = vehicleLabel;
        this.customerLabel = customerLabel;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dailyPrice = dailyPrice;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.status = status;
    }

    public int getId() { return id; }
    public String getContractNumber() { return contractNumber; }
    public int getVehicleId() { return vehicleId; }
    public int getCustomerId() { return customerId; }
    public String getVehicleLabel() { return vehicleLabel; }
    public String getCustomerLabel() { return customerLabel; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public double getDailyPrice() { return dailyPrice; }
    public double getTotalAmount() { return totalAmount; }
    public double getPaidAmount() { return paidAmount; }
    public double getRestAmount() { return totalAmount - paidAmount; }
    public String getStatus() { return status; }
}
