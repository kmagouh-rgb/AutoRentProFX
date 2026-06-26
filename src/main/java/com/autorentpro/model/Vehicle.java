package com.autorentpro.model;

public class Vehicle {
    private int id;
    private String registration;
    private String brand;
    private String model;
    private int year;
    private String fuel;
    private String transmission;
    private int mileage;
    private double dailyPrice;
    private String status;
    private String photoPath;

    public Vehicle(int id, String registration, String brand, String model, int year, String fuel, String transmission, int mileage, double dailyPrice, String status) {
        this(id, registration, brand, model, year, fuel, transmission, mileage, dailyPrice, status, "");
    }

    public Vehicle(int id, String registration, String brand, String model, int year, String fuel, String transmission, int mileage, double dailyPrice, String status, String photoPath) {
        this.id = id;
        this.registration = registration;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.fuel = fuel;
        this.transmission = transmission;
        this.mileage = mileage;
        this.dailyPrice = dailyPrice;
        this.status = status;
        this.photoPath = photoPath;
    }
    public int getId() { return id; }
    public String getRegistration() { return registration; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public String getFuel() { return fuel; }
    public String getTransmission() { return transmission; }
    public int getMileage() { return mileage; }
    public double getDailyPrice() { return dailyPrice; }
    public String getStatus() { return status; }
    public String getPhotoPath() { return photoPath; }
    public void setId(int id) { this.id = id; }
    public void setRegistration(String registration) { this.registration = registration; }
    public void setBrand(String brand) { this.brand = brand; }
    public void setModel(String model) { this.model = model; }
    public void setYear(int year) { this.year = year; }
    public void setFuel(String fuel) { this.fuel = fuel; }
    public void setTransmission(String transmission) { this.transmission = transmission; }
    public void setMileage(int mileage) { this.mileage = mileage; }
    public void setDailyPrice(double dailyPrice) { this.dailyPrice = dailyPrice; }
    public void setStatus(String status) { this.status = status; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
}
