package com.nakul.wealthwise.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter for authentication endpoints.
 *
 * <p>Uses the Token Bucket algorithm (via Bucket4j) to allow at most
 * {@code auth.rate-limit.requests} requests per {@code auth.rate-limit.minutes}
 * minute(s) per client IP address. When a client exceeds the limit it receives
 * HTTP 429 Too Many Requests and must wait for the bucket to refill before
 * retrying.
 *
 * <p>Only applies to paths under {@code /api/auth/}. All other paths pass through
 * without any accounting.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    /** Maximum number of auth requests allowed within the refill window. */
    @Value("${auth.rate-limit.requests:10}")
    private int maxRequests;

    /** Length of the refill window in minutes. */
    @Value("${auth.rate-limit.minutes:1}")
    private int windowMinutes;

    /** One independent bucket per client IP address. */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only rate-limit the auth endpoints; skip everything else.
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::newBucket);

        if (bucket.tryConsume(1)) {
            // Token consumed — allow the request through.
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP {} on {}", clientIp, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"error\":\"Too many requests. Please wait a minute before trying again.\"}"
            );
        }
    }

    /**
     * Creates a new bucket that allows {@code maxRequests} tokens and refills
     * completely every {@code windowMinutes} minute(s).
     */
    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(maxRequests)
                .refillGreedy(maxRequests, Duration.ofMinutes(windowMinutes))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Resolves the real client IP, honouring the {@code X-Forwarded-For} header
     * that is set by Nginx / load balancers in front of the application.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can be a comma-separated chain; the first entry is the real client.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
