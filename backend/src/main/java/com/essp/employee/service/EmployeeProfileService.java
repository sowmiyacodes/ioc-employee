package com.essp.employee.service;

import com.essp.employee.dto.*;
import com.essp.employee.entity.Address;
import com.essp.employee.entity.EmployeeProfile;
import com.essp.employee.repository.AddressRepository;
import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.security.audit.SecurityAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EmployeeProfileService {
    private final EmployeeProfileRepository profileRepository;
    private final AddressRepository addressRepository;
    private final SecurityAuditService auditService;

    public EmployeeProfileService(EmployeeProfileRepository profileRepository,
                                  AddressRepository addressRepository,
                                  SecurityAuditService auditService) {
        this.profileRepository = profileRepository;
        this.addressRepository = addressRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public EmployeeProfileDto getMyProfile(String username) {
        EmployeeProfile profile = findProfile(username);
        return EmployeeProfileDto.from(profile, AddressDto.from(
                addressRepository.findByEmployeeProfileId(profile.getId()).orElse(null)));
    }

    @Transactional
    public EmployeeProfileDto updateMyAddress(String username, AddressUpdateRequest request) {
        EmployeeProfile profile = findProfile(username);
        Address address = addressRepository.findByEmployeeProfileId(profile.getId()).orElseGet(() -> {
            Address created = new Address();
            created.setEmployeeProfile(profile);
            return created;
        });
        Map<String, String> before = addressSnapshot(address);
        applyIfPresent(request.getResidentialStreet(), address::setResidentialStreet);
        applyIfPresent(request.getResidentialCity(), address::setResidentialCity);
        applyIfPresent(request.getResidentialState(), address::setResidentialState);
        applyIfPresent(request.getResidentialPostalCode(), address::setResidentialPostalCode);
        applyIfPresent(request.getResidentialCountry(), address::setResidentialCountry);
        applyIfPresent(request.getMailingStreet(), address::setMailingStreet);
        applyIfPresent(request.getMailingCity(), address::setMailingCity);
        applyIfPresent(request.getMailingState(), address::setMailingState);
        applyIfPresent(request.getMailingPostalCode(), address::setMailingPostalCode);
        applyIfPresent(request.getMailingCountry(), address::setMailingCountry);
        validateComplete(address);
        Address saved = addressRepository.save(address);
        auditService.logAddressChange(username, before, addressSnapshot(saved));
        return EmployeeProfileDto.from(profile, AddressDto.from(saved));
    }

    private EmployeeProfile findProfile(String username) {
        return profileRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Employee profile was not found"));
    }

    private void applyIfPresent(String value, java.util.function.Consumer<String> setter) {
        if (value != null) {
            if (value.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address fields cannot be blank");
            }
            setter.accept(value.trim());
        }
    }

    private void validateComplete(Address address) {
        for (String value : addressSnapshot(address).values()) {
            if (value == null || value.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "All residential and mailing address fields are required");
            }
        }
    }

    private Map<String, String> addressSnapshot(Address address) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("residentialStreet", address.getResidentialStreet());
        values.put("residentialCity", address.getResidentialCity());
        values.put("residentialState", address.getResidentialState());
        values.put("residentialPostalCode", address.getResidentialPostalCode());
        values.put("residentialCountry", address.getResidentialCountry());
        values.put("mailingStreet", address.getMailingStreet());
        values.put("mailingCity", address.getMailingCity());
        values.put("mailingState", address.getMailingState());
        values.put("mailingPostalCode", address.getMailingPostalCode());
        values.put("mailingCountry", address.getMailingCountry());
        return values;
    }
}
