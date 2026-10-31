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
public class LoginRateLimiterService {

    private final Map<String, Bucket> accountBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final int accountCapacity;
    private final int ipCapacity;
    private final int refillMinutes;

    public LoginRateLimiterService(
            @Value("${app.security.rate-limit.login-account-capacity:5}") int accountCapacity,
            @Value("${app.security.rate-limit.login-ip-capacity:20}") int ipCapacity,
            @Value("${app.security.rate-limit.login-refill-minutes:1}") int refillMinutes) {
        if (accountCapacity < 1 || ipCapacity < 1 || refillMinutes < 1) {
            throw new IllegalArgumentException("Rate-limit capacities and refill interval must be positive");
        }
        this.accountCapacity = accountCapacity;
        this.ipCapacity = ipCapacity;
        this.refillMinutes = refillMinutes;
    }

    private Bucket createNewBucket(int capacity) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, Duration.ofMinutes(refillMinutes)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryConsume(String clientIp, String accountIdentifier) {
        Bucket ipBucket = ipBuckets.computeIfAbsent(clientIp, ignored -> createNewBucket(ipCapacity));
        Bucket accountBucket = accountBuckets.computeIfAbsent(
                accountIdentifier.toLowerCase(java.util.Locale.ROOT),
                ignored -> createNewBucket(accountCapacity));
        boolean ipAllowed = ipBucket.tryConsume(1);
        boolean accountAllowed = accountBucket.tryConsume(1);
        return ipAllowed && accountAllowed;
    }
}
