package com.essp.payslip.service;

import com.essp.employee.entity.EmployeeProfile;
import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.payslip.dto.PayslipDto;
import com.essp.payslip.entity.Payslip;
import com.essp.payslip.repository.PayslipRepository;
import com.essp.security.audit.SecurityAuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PayslipService {

    private static final Pattern STORAGE_KEY_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf");

    private final EmployeeProfileRepository profileRepository;
    private final PayslipRepository payslipRepository;
    private final SecurityAuditService auditService;
    private final Path storageDirectory;

    public PayslipService(EmployeeProfileRepository profileRepository,
                          PayslipRepository payslipRepository,
                          SecurityAuditService auditService,
                          @Value("${app.payslip.storage-directory:./protected-payslips}") String storageDirectory) {
        this.profileRepository = profileRepository;
        this.payslipRepository = payslipRepository;
        this.auditService = auditService;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<PayslipDto> getPayslips(String username) {
        EmployeeProfile profile = getEmployeeProfile(username);
        return payslipRepository.findByEmployeeProfileIdOrderByPayPeriodStartDesc(profile.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PayslipDownload getDownload(String username, Long payslipId, String sourceIp) {
        EmployeeProfile profile = getEmployeeProfile(username);
        Payslip payslip = payslipRepository.findByIdAndEmployeeProfileId(payslipId, profile.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payslip not found"));

        String storageKey = payslip.getStorageKey();
        if (storageKey == null || !STORAGE_KEY_PATTERN.matcher(storageKey).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payslip file not found");
        }
        Path file = storageDirectory.resolve(storageKey).normalize();
        if (!file.startsWith(storageDirectory) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payslip file not found");
        }

        Resource resource = new FileSystemResource(file);
        auditService.logPayslipDownload(username, payslip.getId(), sourceIp);
        return new PayslipDownload(payslip, resource);
    }

    private EmployeeProfile getEmployeeProfile(String username) {
        return profileRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Employee profile not found"));
    }

    private PayslipDto toDto(Payslip payslip) {
        return new PayslipDto(payslip.getId(), payslip.getPayPeriodStart(),
                payslip.getPayPeriodEnd(), payslip.getIssuedAt());
    }

    public record PayslipDownload(Payslip payslip, Resource resource) {}
}
