package com.autorentpro.model;

public class Customer {
    private int id;
    private String fullName;
    private String sex;
    private String birthDate;
    private String birthPlace;
    private String nationality;
    private String address;
    private String city;
    private String phone;
    private String email;
    private String cin;
    private String cinExpiry;
    private String drivingLicense;
    private String licenseIssueDate;
    private String licenseIssuePlace;
    private String licenseExpiry;
    private String passportNumber;
    private String passportExpiry;
    private String entryNumber;
    private String profession;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String observations;
    private String docCinRecto;
    private String docCinVerso;
    private String docPermisRecto;
    private String docPermisVerso;
    private String docPassport;
    private String photoPath;

    public Customer(int id, String fullName, String sex, String birthDate, String birthPlace, String nationality,
                    String address, String city, String phone, String email, String cin, String cinExpiry,
                    String drivingLicense, String licenseIssueDate, String licenseIssuePlace, String licenseExpiry,
                    String passportNumber, String passportExpiry, String entryNumber, String profession,
                    String emergencyContactName, String emergencyContactPhone, String observations,
                    String docCinRecto, String docCinVerso, String docPermisRecto, String docPermisVerso,
                    String docPassport, String photoPath) {
        this.id = id;
        this.fullName = n(fullName);
        this.sex = n(sex);
        this.birthDate = n(birthDate);
        this.birthPlace = n(birthPlace);
        this.nationality = n(nationality);
        this.address = n(address);
        this.city = n(city);
        this.phone = n(phone);
        this.email = n(email);
        this.cin = n(cin);
        this.cinExpiry = n(cinExpiry);
        this.drivingLicense = n(drivingLicense);
        this.licenseIssueDate = n(licenseIssueDate);
        this.licenseIssuePlace = n(licenseIssuePlace);
        this.licenseExpiry = n(licenseExpiry);
        this.passportNumber = n(passportNumber);
        this.passportExpiry = n(passportExpiry);
        this.entryNumber = n(entryNumber);
        this.profession = n(profession);
        this.emergencyContactName = n(emergencyContactName);
        this.emergencyContactPhone = n(emergencyContactPhone);
        this.observations = n(observations);
        this.docCinRecto = n(docCinRecto);
        this.docCinVerso = n(docCinVerso);
        this.docPermisRecto = n(docPermisRecto);
        this.docPermisVerso = n(docPermisVerso);
        this.docPassport = n(docPassport);
        this.photoPath = n(photoPath);
    }

    private static String n(String v) { return v == null ? "" : v; }

    public int getId() { return id; }
    public String getFullName() { return fullName; }
    public String getSex() { return sex; }
    public String getBirthDate() { return birthDate; }
    public String getBirthPlace() { return birthPlace; }
    public String getNationality() { return nationality; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getCin() { return cin; }
    public String getCinExpiry() { return cinExpiry; }
    public String getDrivingLicense() { return drivingLicense; }
    public String getLicenseIssueDate() { return licenseIssueDate; }
    public String getLicenseIssuePlace() { return licenseIssuePlace; }
    public String getLicenseExpiry() { return licenseExpiry; }
    public String getPassportNumber() { return passportNumber; }
    public String getPassportExpiry() { return passportExpiry; }
    public String getEntryNumber() { return entryNumber; }
    public String getProfession() { return profession; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public String getObservations() { return observations; }
    public String getDocCinRecto() { return docCinRecto; }
    public String getDocCinVerso() { return docCinVerso; }
    public String getDocPermisRecto() { return docPermisRecto; }
    public String getDocPermisVerso() { return docPermisVerso; }
    public String getDocPassport() { return docPassport; }
    public String getPhotoPath() { return photoPath; }
}
