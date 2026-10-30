package com.essp.employee.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public class AddressUpdateRequest {
    private static final String TEXT_FORMAT = "^[\\p{L}\\p{N}][\\p{L}\\p{N} .,'/#-]*$";
    private static final String TEXT_FORMAT_MESSAGE = "may contain letters, numbers, spaces, and . , ' / # -";

    @NotBlank(message = "Residential street is required")
    @Size(max = 200, message = "Residential street must be at most 200 characters")
    @Pattern(regexp = TEXT_FORMAT, message = "Residential street " + TEXT_FORMAT_MESSAGE)
    private String residentialStreet;
    @NotBlank(message = "Residential city is required")
    @Size(max = 100, message = "Residential city must be at most 100 characters")
    @Pattern(regexp = TEXT_FORMAT, message = "Residential city " + TEXT_FORMAT_MESSAGE)
    private String residentialCity;
    @NotBlank(message = "Residential state is required")
    @Size(max = 100, message = "Residential state must be at most 100 characters")
    @Pattern(regexp = TEXT_FORMAT, message = "Residential state " + TEXT_FORMAT_MESSAGE)
    private String residentialState;
    @NotBlank(message = "Residential postal code is required")
    @Size(max = 20, message = "Residential postal code must be at most 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9 -]*$", message = "Residential postal code has an invalid format")
    private String residentialPostalCode;
    @NotBlank(message = "Residential country is required")
    @Size(max = 100, message = "Residential country must be at most 100 characters")
    @Pattern(regexp = TEXT_FORMAT, message = "Residential country " + TEXT_FORMAT_MESSAGE)
    private String residentialCountry;
    @NotBlank(message = "Mailing street is required")
    @Size(max = 200, message = "Mailing street must be at most 200 characters")
    @Pattern(regexp = TEXT_FORMAT, message = "Mailing street " + TEXT_FORMAT_MESSAGE)
    private String mailingStreet;
    @NotBlank(message = "Mailing city is required")
    @Size(max = 100, message = "Mailing city must be at most 100 characters")
    @Pattern(regexp = TEXT_FORMAT, message = "Mailing city " + TEXT_FORMAT_MESSAGE)
    private String mailingCity;
    @NotBlank(message = "Mailing state is required")
    @Size(max = 100, message = "Mailing state must be at most 100 characters")
    @Pattern(regexp = TEXT_FORMAT, message = "Mailing state " + TEXT_FORMAT_MESSAGE)
    private String mailingState;
    @NotBlank(message = "Mailing postal code is required")
    @Size(max = 20, message = "Mailing postal code must be at most 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9 -]*$", message = "Mailing postal code has an invalid format")
    private String mailingPostalCode;
    @NotBlank(message = "Mailing country is required")
    @Size(max = 100, message = "Mailing country must be at most 100 characters")
    @Pattern(regexp = TEXT_FORMAT, message = "Mailing country " + TEXT_FORMAT_MESSAGE)
    private String mailingCountry;

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
