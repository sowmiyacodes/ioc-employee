package com.essp.payslip.service;

import com.essp.employee.entity.EmployeeProfile;
import com.essp.employee.repository.EmployeeProfileRepository;
import com.essp.payslip.dto.PayslipDto;
import com.essp.payslip.dto.PayslipPageDto;
import com.essp.payslip.entity.Payslip;
import com.essp.payslip.repository.PayslipRepository;
import com.essp.security.audit.SecurityAuditService;
import com.essp.security.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PayslipService {

    private static final Logger logger = LoggerFactory.getLogger(PayslipService.class);
    private static final int PDF_HEADER_LENGTH = 5;
    private static final byte[] PDF_HEADER = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final Pattern STORAGE_KEY_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.pdf");

    private final EmployeeProfileRepository profileRepository;
    private final PayslipRepository payslipRepository;
    private final SecurityAuditService auditService;
    private final Path storageDirectory;
    private final long maxFileSizeBytes;

    public PayslipService(EmployeeProfileRepository profileRepository,
                          PayslipRepository payslipRepository,
                          SecurityAuditService auditService,
                          @Value("${app.payslip.storage-directory:./protected-payslips}") String storageDirectory,
                          @Value("${app.payslip.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.profileRepository = profileRepository;
        this.payslipRepository = payslipRepository;
        this.auditService = auditService;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Transactional(readOnly = true)
    public PayslipPageDto getPayslips(String username, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100");
        }
        EmployeeProfile profile = getEmployeeProfile(username);
        Page<Payslip> payslips = payslipRepository.findByEmployeeProfileIdOrderByPayPeriodStartDesc(
                profile.getId(), PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, "payPeriodStart").and(Sort.by("id"))));
        return PayslipPageDto.from(payslips);
    }

    @Transactional
    public PayslipDto provision(String actor, Long employeeProfileId, LocalDate payPeriodStart,
                                LocalDate payPeriodEnd, MultipartFile file) {
        if (employeeProfileId == null || employeeProfileId < 1
                || payPeriodStart == null || payPeriodEnd == null
                || payPeriodEnd.isBefore(payPeriodStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid employee and pay period are required");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a non-empty PDF payslip");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "The uploaded file exceeds the maximum allowed size");
        }

        EmployeeProfile profile = profileRepository.findByIdForUpdate(employeeProfileId)
                .filter(candidate -> candidate.getUser().isActive()
                        && candidate.getUser().getRole() == Role.EMPLOYEE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Active employee profile not found"));
        if (payslipRepository.existsByEmployeeProfileIdAndPayPeriodStart(employeeProfileId, payPeriodStart)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A payslip already exists for this employee and pay period");
        }

        Path temporaryFile = null;
        Path storedFile = null;
        try {
            byte[] contents = file.getBytes();
            if (contents.length > maxFileSizeBytes) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "The uploaded file exceeds the maximum allowed size");
            }
            if (!hasPdfHeader(contents)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The uploaded file is not a valid PDF");
            }

            Files.createDirectories(storageDirectory);
            temporaryFile = Files.createTempFile(storageDirectory, ".payslip-", ".tmp");
            Files.write(temporaryFile, contents);
            String storageKey = UUID.randomUUID() + ".pdf";
            storedFile = storageDirectory.resolve(storageKey);
            try {
                Files.move(temporaryFile, storedFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, storedFile);
            }
            temporaryFile = null;

            Path fileToCleanUpOnRollback = storedFile;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        deleteAfterRollback(fileToCleanUpOnRollback);
                    }
                }
            });

            Payslip payslip = new Payslip();
            payslip.setEmployeeProfile(profile);
            payslip.setPayPeriodStart(payPeriodStart);
            payslip.setPayPeriodEnd(payPeriodEnd);
            payslip.setStorageKey(storageKey);
            Payslip saved = payslipRepository.save(payslip);
            auditService.logAdministrationEvent(actor, "PAYSLIP_PROVISIONED", saved.getId(),
                    "employeeProfileId=" + profile.getId() + ";payPeriodStart=" + payPeriodStart
                            + ";payPeriodEnd=" + payPeriodEnd);
            return toDto(saved);
        } catch (IOException exception) {
            deleteAfterRollback(temporaryFile);
            deleteAfterRollback(storedFile);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Payslip file could not be stored");
        }
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

    private boolean hasPdfHeader(byte[] contents) {
        if (contents.length < PDF_HEADER_LENGTH) return false;
        for (int index = 0; index < PDF_HEADER_LENGTH; index++) {
            if (contents[index] != PDF_HEADER[index]) return false;
        }
        return true;
    }

    private void deleteAfterRollback(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            logger.error("Could not remove an uncommitted payslip file at {}", path, exception);
        }
    }

    public record PayslipDownload(Payslip payslip, Resource resource) {}
}
