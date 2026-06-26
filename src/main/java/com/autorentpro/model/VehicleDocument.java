package com.autorentpro.model;

import java.time.LocalDate;

public class VehicleDocument {
    private int id;
    private int vehicleId;
    private String vehicleLabel;
    private String documentType;
    private String documentNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String filePath;
    private String notes;

    public VehicleDocument(int id, int vehicleId, String vehicleLabel, String documentType, String documentNumber, LocalDate issueDate, LocalDate expiryDate, String filePath, String notes) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.vehicleLabel = vehicleLabel;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.filePath = filePath;
        this.notes = notes;
    }

    public int getId() { return id; }
    public int getVehicleId() { return vehicleId; }
    public String getVehicleLabel() { return vehicleLabel; }
    public String getDocumentType() { return documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getFilePath() { return filePath; }
    public String getNotes() { return notes; }
}
