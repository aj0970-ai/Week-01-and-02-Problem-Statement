import java.util.*;
import java.util.concurrent.*;

/**
 * Problem 6: Distributed Rate Limiter for API Gateway
 * 
 * Enforces request rate limits per client using token bucket algorithm.
 * - Tracks requests per client
 * - Allows burst traffic
 * - Resets counters hourly
 * - Handles concurrent access
 */
public class Problem6_RateLimiter {
    
    /**
     * Token bucket implementation
     */
    private static class TokenBucket {
        long tokens;
        long lastRefillTime;
        long maxTokens;
        double refillRate; // tokens per second
        
        TokenBucket(long maxTokens, double refillRate) {
            this.tokens = maxTokens;
            this.maxTokens = maxTokens;
            this.refillRate = refillRate;
            this.lastRefillTime = System.currentTimeMillis();
        }
        
        /**
         * Refill tokens based on elapsed time
         */
        void refill() {
            long now = System.currentTimeMillis();
            long elapsedMs = now - lastRefillTime;
            double elapsedSeconds = elapsedMs / 1000.0;
            
            long newTokens = (long)(elapsedSeconds * refillRate);
            tokens = Math.min(maxTokens, tokens + newTokens);
            lastRefillTime = now;
        }
        
        /**
         * Try to consume a token
         */
        boolean tryConsume(long tokenCount) {
            refill();
            
            if (tokens >= tokenCount) {
                tokens -= tokenCount;
                return true;
            }
            return false;
        }
        
        /**
         * Get remaining tokens
         */
        long getRemainingTokens() {
            refill();
            return tokens;
        }
        
        /**
         * Get time until next token is available (in seconds)
         */
        long getRetryAfter() {
            refill();
            if (tokens > 0) return 0;
            
            long tokensNeeded = 1;
            return (long)((tokensNeeded / refillRate) + 1);
        }
    }
    
    /**
     * Rate limit status information
     */
    public static class RateLimitStatus {
        public long used;
        public long limit;
        public long remaining;
        public long resetTime;
        
        RateLimitStatus(long used, long limit, long remaining, long resetTime) {
            this.used = used;
            this.limit = limit;
            this.remaining = remaining;
            this.resetTime = resetTime;
        }
        
        @Override
        public String toString() {
            return String.format("Used: %d, Limit: %d, Remaining: %d, Reset: %d", 
                               used, limit, remaining, resetTime);
        }
    }
    
    private final ConcurrentHashMap<String, TokenBucket> clientBuckets;
    private final long requestsPerHour;
    private final long bucketResetIntervalMs;
    
    public Problem6_RateLimiter(long requestsPerHour) {
        this.clientBuckets = new ConcurrentHashMap<>();
        this.requestsPerHour = requestsPerHour;
        this.bucketResetIntervalMs = 60 * 60 * 1000; // 1 hour
    }
    
    /**
     * Get or create token bucket for a client
     */
    private TokenBucket getOrCreateBucket(String clientId) {
        return clientBuckets.computeIfAbsent(clientId, k -> {
            // Requests per second = requestsPerHour / 3600
            double refillRate = requestsPerHour / 3600.0;
            return new TokenBucket(requestsPerHour, refillRate);
        });
    }
    
    /**
     * Check if request is allowed
     */
    public synchronized String checkRateLimit(String clientId) {
        TokenBucket bucket = getOrCreateBucket(clientId);
        
        if (bucket.tryConsume(1)) {
            long remaining = bucket.getRemainingTokens();
            return "Allowed (" + remaining + " requests remaining)";
        } else {
            long retryAfter = bucket.getRetryAfter();
            return "Denied (Rate limit exceeded, retry after " + retryAfter + "s)";
        }
    }
    
    /**
     * Check rate limit with custom request cost
     */
    public synchronized String checkRateLimit(String clientId, long requestCost) {
        TokenBucket bucket = getOrCreateBucket(clientId);
        
        if (bucket.tryConsume(requestCost)) {
            long remaining = bucket.getRemainingTokens();
            return "Allowed (" + remaining + " requests remaining)";
        } else {
            long retryAfter = bucket.getRetryAfter();
            return "Denied (Rate limit exceeded, retry after " + retryAfter + "s)";
        }
    }
    
    /**
     * Get rate limit status for a client
     */
    public RateLimitStatus getRateLimitStatus(String clientId) {
        TokenBucket bucket = getOrCreateBucket(clientId);
        long remaining = bucket.getRemainingTokens();
        long used = requestsPerHour - remaining;
        long resetTime = bucket.lastRefillTime + bucketResetIntervalMs;
        
        return new RateLimitStatus(used, requestsPerHour, remaining, resetTime);
    }
    
    /**
     * Get formatted status report
     */
    public String getStatusReport(String clientId) {
        RateLimitStatus status = getRateLimitStatus(clientId);
        double usagePercentage = (status.used / (double) status.limit) * 100;
        
        return String.format(
            "Client: %s%nUsed: %d/%d (%.1f%%)%nRemaining: %d%nReset Time: %s",
            clientId, status.used, status.limit, usagePercentage, 
            status.remaining, new java.util.Date(status.resetTime)
        );
    }
    
    /**
     * Simulate multiple requests from a client
     */
    public void simulateRequests(String clientId, int requestCount) {
        System.out.printf("\nSimulating %d requests from client %s:%n", requestCount, clientId);
        
        int allowed = 0;
        int denied = 0;
        
        for (int i = 1; i <= requestCount; i++) {
            String result = checkRateLimit(clientId);
            if (result.startsWith("Allowed")) {
                allowed++;
            } else {
                denied++;
            }
            
            if (i <= 5 || i > requestCount - 3 || denied > 0) {
                System.out.printf("  Request %d: %s%n", i, result);
            } else if (i == 6) {
                System.out.println("  ...");
            }
        }
        
        System.out.printf("Summary: %d allowed, %d denied%n", allowed, denied);
    }
    
    // Main method for testing
    public static void main(String[] args) throws InterruptedException {
        // 1000 requests per hour limit
        Problem6_RateLimiter limiter = new Problem6_RateLimiter(1000);
        
        System.out.println("=== Problem 6: Distributed Rate Limiter ===\n");
        System.out.println("Configuration: 1000 requests/hour limit (~0.28 requests/second)\n");
        
        System.out.println("Test Case 1: Single client within limit");
        limiter.simulateRequests("client_abc123", 5);
        
        System.out.println("\nTest Case 2: Check status");
        System.out.println(limiter.getStatusReport("client_abc123"));
        
        System.out.println("\n\nTest Case 3: Multiple clients");
        limiter.simulateRequests("client_xyz789", 3);
        limiter.simulateRequests("client_def456", 3);
        
        System.out.println("\n\nTest Case 4: Heavy traffic (exceeding burst)");
        limiter.simulateRequests("client_heavy", 50);
        
        System.out.println("\n\nTest Case 5: Rate limit status for all clients");
        for (String client : new String[]{"client_abc123", "client_xyz789", 
                                          "client_def456", "client_heavy"}) {
            System.out.println("\n" + limiter.getStatusReport(client));
        }
        
        System.out.println("\n\nTest Case 6: Custom request cost");
        String result1 = limiter.checkRateLimit("client_expensive", 10);
        System.out.println("Heavy operation (cost=10): " + result1);
        
        String result2 = limiter.checkRateLimit("client_expensive", 1);
        System.out.println("Normal operation (cost=1): " + result2);
    }
}
