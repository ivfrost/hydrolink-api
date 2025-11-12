package dev.ivfrost.hydro_backend.util;

import dev.ivfrost.hydro_backend.entity.User;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AllArgsConstructor
@NoArgsConstructor
public class RateLimitUtils {

    private ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Optional<Bucket> getBucketByUserOrIp(User user, String ipAddress) {
        if (user != null) {
            return Optional.of(getBucketByUserId(user.getId().toString()));
        } else if (ipAddress != null && !ipAddress.isEmpty()) {
            return Optional.of(getBucketByIp(ipAddress));
        } else {
            return Optional.empty();
        }
    }

    private Bucket getBucketByIp(String ipAddres) {
        return buckets.computeIfAbsent(ipAddres, k -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(10)
                    .refillGreedy(10, Duration.ofMinutes(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }

    private Bucket getBucketByUserId(String userId) {
        return buckets.computeIfAbsent(userId, k -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(20)
                    .refillGreedy(20, Duration.ofMinutes(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        });
    }

    public static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}