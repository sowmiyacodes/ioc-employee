package com.essp.employee.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "employee_addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_profile_id", nullable = false, unique = true)
    private EmployeeProfile employeeProfile;

    @Column(name = "residential_street", nullable = false, length = 200)
    private String residentialStreet;
    @Column(name = "residential_city", nullable = false, length = 100)
    private String residentialCity;
    @Column(name = "residential_state", nullable = false, length = 100)
    private String residentialState;
    @Column(name = "residential_postal_code", nullable = false, length = 20)
    private String residentialPostalCode;
    @Column(name = "residential_country", nullable = false, length = 100)
    private String residentialCountry;

    @Column(name = "mailing_street", nullable = false, length = 200)
    private String mailingStreet;
    @Column(name = "mailing_city", nullable = false, length = 100)
    private String mailingCity;
    @Column(name = "mailing_state", nullable = false, length = 100)
    private String mailingState;
    @Column(name = "mailing_postal_code", nullable = false, length = 20)
    private String mailingPostalCode;
    @Column(name = "mailing_country", nullable = false, length = 100)
    private String mailingCountry;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public EmployeeProfile getEmployeeProfile() { return employeeProfile; }
    public void setEmployeeProfile(EmployeeProfile employeeProfile) { this.employeeProfile = employeeProfile; }
    public String getResidentialStreet() { return residentialStreet; }
    public void setResidentialStreet(String value) { residentialStreet = value; }
    public String getResidentialCity() { return residentialCity; }
    public void setResidentialCity(String value) { residentialCity = value; }
    public String getResidentialState() { return residentialState; }
    public void setResidentialState(String value) { residentialState = value; }
    public String getResidentialPostalCode() { return residentialPostalCode; }
    public void setResidentialPostalCode(String value) { residentialPostalCode = value; }
    public String getResidentialCountry() { return residentialCountry; }
    public void setResidentialCountry(String value) { residentialCountry = value; }
    public String getMailingStreet() { return mailingStreet; }
    public void setMailingStreet(String value) { mailingStreet = value; }
    public String getMailingCity() { return mailingCity; }
    public void setMailingCity(String value) { mailingCity = value; }
    public String getMailingState() { return mailingState; }
    public void setMailingState(String value) { mailingState = value; }
    public String getMailingPostalCode() { return mailingPostalCode; }
    public void setMailingPostalCode(String value) { mailingPostalCode = value; }
    public String getMailingCountry() { return mailingCountry; }
    public void setMailingCountry(String value) { mailingCountry = value; }
}
