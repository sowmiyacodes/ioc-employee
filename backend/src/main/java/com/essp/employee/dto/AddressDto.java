package com.essp.employee.dto;

import com.essp.employee.entity.Address;

public record AddressDto(
        String residentialStreet, String residentialCity, String residentialState,
        String residentialPostalCode, String residentialCountry,
        String mailingStreet, String mailingCity, String mailingState,
        String mailingPostalCode, String mailingCountry) {
    public static AddressDto from(Address address) {
        if (address == null) return null;
        return new AddressDto(address.getResidentialStreet(), address.getResidentialCity(),
                address.getResidentialState(), address.getResidentialPostalCode(), address.getResidentialCountry(),
                address.getMailingStreet(), address.getMailingCity(), address.getMailingState(),
                address.getMailingPostalCode(), address.getMailingCountry());
    }
}
