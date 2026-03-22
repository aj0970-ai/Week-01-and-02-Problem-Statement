import java.util.*;
import java.util.concurrent.*;

/**
 * Problem 3: DNS Cache with TTL (Time To Live)
 * 
 * Caches domain-to-IP mappings with TTL-based expiration.
 * - O(1) lookup performance
 * - TTL-based automatic expiration
 * - Cache hit/miss statistics
 * - Background cleanup of expired entries
 * - LRU eviction when cache is full
 */
public class Problem3_DNSCache {
    
    /**
     * Entry class storing DNS information with timestamp
     */
    private static class DNSEntry {
        String domain;
        String ipAddress;
        long createdTime;
        long ttl; // in seconds
        long accessCount;
        
        DNSEntry(String domain, String ipAddress, long ttl) {
            this.domain = domain;
            this.ipAddress = ipAddress;
            this.createdTime = System.currentTimeMillis();
            this.ttl = ttl;
            this.accessCount = 0;
        }
        
        boolean isExpired() {
            long ageInSeconds = (System.currentTimeMillis() - createdTime) / 1000;
            return ageInSeconds > ttl;
        }
        
        @Override
        public String toString() {
            return ipAddress + " (TTL: " + ttl + "s, Age: " + 
                   ((System.currentTimeMillis() - createdTime) / 1000) + "s)";
        }
    }
    
    private final Map<String, DNSEntry> cache;
    private final int maxCacheSize;
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private final Object lock = new Object();
    
    public Problem3_DNSCache(int maxCacheSize) {
        // Using LinkedHashMap for LRU eviction capability
        this.cache = new LinkedHashMap<String, DNSEntry>(maxCacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > maxCacheSize;
            }
        };
        this.maxCacheSize = maxCacheSize;
        
        // Start background cleanup thread
        startCleanupThread();
    }
    
    /**
     * Resolve a domain name with caching
     */
    public String resolve(String domain) {
        synchronized (lock) {
            DNSEntry entry = cache.get(domain);
            
            // Cache HIT - entry exists and not expired
            if (entry != null && !entry.isExpired()) {
                cacheHits++;
                entry.accessCount++;
                System.out.printf("Cache HIT: %s -> %s (Lookups: %d)%n", 
                                domain, entry.ipAddress, entry.accessCount);
                return entry.ipAddress;
            }
            
            // Cache MISS or EXPIRED
            cacheMisses++;
            if (entry != null) {
                cache.remove(domain); // Remove expired entry
                System.out.printf("Cache EXPIRED: %s%n", domain);
            } else {
                System.out.printf("Cache MISS: %s%n", domain);
            }
            
            // Simulate upstream DNS query
            String ipAddress = queryUpstreamDNS(domain);
            
            // Cache the result with default TTL of 300 seconds
            cache.put(domain, new DNSEntry(domain, ipAddress, 300));
            System.out.printf("Cached result: %s -> %s (TTL: 300s)%n%n", 
                            domain, ipAddress);
            
            return ipAddress;
        }
    }
    
    /**
     * Resolve with custom TTL
     */
    public String resolveWithTTL(String domain, long ttl) {
        synchronized (lock) {
            DNSEntry entry = cache.get(domain);
            
            if (entry != null && !entry.isExpired()) {
                cacheHits++;
                entry.accessCount++;
                return entry.ipAddress;
            }
            
            cacheMisses++;
            if (entry != null) {
                cache.remove(domain);
            }
            
            String ipAddress = queryUpstreamDNS(domain);
            cache.put(domain, new DNSEntry(domain, ipAddress, ttl));
            
            return ipAddress;
        }
    }
    
    /**
     * Simulate upstream DNS query
     */
    private String queryUpstreamDNS(String domain) {
        // Mock implementation - returns random IP
        return String.format("192.168.%d.%d", 
                            (int)(Math.random() * 256), 
                            (int)(Math.random() * 256));
    }
    
    /**
     * Clear expired entries from cache
     */
    private void cleanupExpiredEntries() {
        synchronized (lock) {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
    }
    
    /**
     * Start background cleanup thread
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Clean every 60 seconds
                    cleanupExpiredEntries();
                    System.out.println("[Cleanup] Removed expired entries. Cache size: " + cache.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setName("DNSCache-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    /**
     * Get cache statistics
     */
    public void printStatistics() {
        synchronized (lock) {
            long total = cacheHits + cacheMisses;
            double hitRate = (total > 0) ? (100.0 * cacheHits / total) : 0;
            
            System.out.println("\n=== DNS Cache Statistics ===");
            System.out.printf("Cache Hits: %d%n", cacheHits);
            System.out.printf("Cache Misses: %d%n", cacheMisses);
            System.out.printf("Total Queries: %d%n", total);
            System.out.printf("Hit Rate: %.2f%%%n", hitRate);
            System.out.printf("Current Cache Size: %d / %d%n", cache.size(), maxCacheSize);
            
            System.out.println("\nCached Entries:");
            for (Map.Entry<String, DNSEntry> entry : cache.entrySet()) {
                DNSEntry dns = entry.getValue();
                String status = dns.isExpired() ? "[EXPIRED]" : "[VALID]";
                System.out.printf("  %s %s -> %s%n", status, entry.getKey(), dns);
            }
        }
    }
    
    /**
     * Clear entire cache
     */
    public void clear() {
        synchronized (lock) {
            cache.clear();
            cacheHits = 0;
            cacheMisses = 0;
        }
    }
    
    // Main method for testing
    public static void main(String[] args) throws InterruptedException {
        Problem3_DNSCache dnsCache = new Problem3_DNSCache(10);
        
        System.out.println("=== Problem 3: DNS Cache with TTL ===\n");
        
        System.out.println("Test Case 1: First Resolution (Cache MISS)");
        String result1 = dnsCache.resolve("google.com");
        System.out.println("Result: " + result1);
        
        System.out.println("Test Case 2: Second Resolution (Cache HIT)");
        String result2 = dnsCache.resolve("google.com");
        System.out.println("Result: " + result2);
        
        System.out.println("Test Case 3: Multiple Different Domains");
        dnsCache.resolve("facebook.com");
        dnsCache.resolve("github.com");
        dnsCache.resolve("stackoverflow.com");
        
        System.out.println("Test Case 4: Repeated Access (Access Count Increases)");
        for (int i = 0; i < 3; i++) {
            dnsCache.resolve("google.com");
        }
        
        System.out.println("Test Case 5: Custom TTL (Short expiration)");
        dnsCache.resolveWithTTL("temp.com", 2); // 2 seconds TTL
        System.out.println("Waiting 3 seconds for expiration...");
        Thread.sleep(3000);
        dnsCache.resolve("temp.com"); // Should be expired
        
        System.out.println("Test Case 6: Cache Fill-up (Max size = 10)");
        for (int i = 0; i < 8; i++) {
            dnsCache.resolve("domain" + i + ".com");
        }
        
        dnsCache.printStatistics();
    }
}
