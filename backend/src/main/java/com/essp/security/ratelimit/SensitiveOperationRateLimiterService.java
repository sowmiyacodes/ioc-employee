package com.essp.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SensitiveOperationRateLimiterService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final int refillMinutes;

    public SensitiveOperationRateLimiterService(
            @Value("${app.security.rate-limit.payslip-upload-capacity:10}") int capacity,
            @Value("${app.security.rate-limit.payslip-upload-refill-minutes:1}") int refillMinutes) {
        if (capacity < 1 || refillMinutes < 1) {
            throw new IllegalArgumentException("Rate-limit capacity and refill interval must be positive");
        }
        if (capacity < 1 || refillMinutes < 1) {
            throw new IllegalArgumentException("Rate-limit capacity and refill interval must be positive");
        }
        this.capacity = capacity;
        this.refillMinutes = refillMinutes;
    }

    public boolean tryConsumePayslipUpload(String username) {
        Bucket bucket = buckets.computeIfAbsent(username, ignored -> createBucket());
        return bucket.tryConsume(1);
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.greedy(capacity, Duration.ofMinutes(refillMinutes)));
        return Bucket.builder().addLimit(limit).build();
    }
}
