import java.util.*;

/**
 * Problem 10: Multi-Level Cache System with Hash Tables
 * 
 * Implements a 3-level cache hierarchy (L1, L2, L3).
 * - L1: In-memory HashMap (10,000 items max)
 * - L2: SSD-backed HashMap (100,000 items max)
 * - L3: Database (slowest, all items)
 * - LRU eviction at each level
 * - Promotes videos between levels
 */
public class Problem10_MultiLevelCache {
    
    /**
     * Video metadata
     */
    private static class VideoData {
        String videoId;
        String title;
        long fileSize;
        String filePath;
        
        VideoData(String videoId, String title, long fileSize) {
            this.videoId = videoId;
            this.title = title;
            this.fileSize = fileSize;
            this.filePath = "/storage/videos/" + videoId + ".mp4";
        }
        
        @Override
        public String toString() {
            return String.format("%s (\"%s\", %dMB)", videoId, title, fileSize);
        }
    }
    
    /**
     * Cache level metrics
     */
    private static class CacheMetrics {
        long hits = 0;
        long misses = 0;
        long totalAccessTime = 0;
        long accessCount = 0;
        
        double getHitRate() {
            long total = hits + misses;
            return total > 0 ? (100.0 * hits / total) : 0;
        }
        
        double getAverageAccessTime() {
            return accessCount > 0 ? (totalAccessTime / (double) accessCount) : 0;
        }
        
        void recordHit(long accessTimeMs) {
            hits++;
            totalAccessTime += accessTimeMs;
            accessCount++;
        }
        
        void recordMiss() {
            misses++;
        }
    }
    
    // L1 Cache: In-memory (max 10,000 items)
    LinkedHashMap<String, VideoData> l1Cache;
    
    // L2 Cache: SSD-backed (max 100,000 items, stores file paths)
    LinkedHashMap<String, String> l2Cache;
    
    // L3: Database (all items)
    HashMap<String, VideoData> l3Database;
    
    // Access count tracking for promotion
    HashMap<String, Long> accessCounts;
    
    // Metrics
    CacheMetrics l1Metrics = new CacheMetrics();
    CacheMetrics l2Metrics = new CacheMetrics();
    CacheMetrics l3Metrics = new CacheMetrics();
    
    private static final int L1_MAX_SIZE = 10000;
    private static final int L2_MAX_SIZE = 100000;
    private static final long L2_TO_L1_PROMOTION_THRESHOLD = 5;
    
    public Problem10_MultiLevelCache() {
        // L1: LinkedHashMap for LRU access-order
        this.l1Cache = new LinkedHashMap<String, VideoData>(L1_MAX_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > L1_MAX_SIZE;
            }
        };
        
        // L2: LinkedHashMap for LRU
        this.l2Cache = new LinkedHashMap<String, String>(L2_MAX_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > L2_MAX_SIZE;
            }
        };
        
        this.l3Database = new HashMap<>();
        this.accessCounts = new HashMap<>();
    }
    
    /**
     * Get video, checking caches in order
     */
    public VideoData getVideo(String videoId) {
        System.out.printf("Getting video: %s%n", videoId);
        long startTime = System.nanoTime();
        
        // Check L1 Cache
        if (l1Cache.containsKey(videoId)) {
            VideoData hit = l1Cache.get(videoId);
            long accessTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            l1Metrics.recordHit(accessTimeMs);
            System.out.printf("  ✓ L1 Cache HIT (%.1fms)%n%n", accessTimeMs);
            recordAccess(videoId);
            return hit;
        }
        
        l1Metrics.recordMiss();
        
        // Check L2 Cache
        if (l2Cache.containsKey(videoId)) {
            String filePath = l2Cache.get(videoId);
            long accessTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            l2Metrics.recordHit(accessTimeMs);
            System.out.printf("  ✓ L2 Cache HIT (%.1fms)%n", accessTimeMs);
            
            // Get from L3 and load to L1
            VideoData video = l3Database.get(videoId);
            if (video != null) {
                promoteToL1(videoId, video);
                System.out.printf("  ↑ Promoted to L1%n%n");
            }
            
            recordAccess(videoId);
            return video;
        }
        
        l2Metrics.recordMiss();
        
        // Check L3 Database
        if (l3Database.containsKey(videoId)) {
            long accessTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            l3Metrics.recordHit(accessTimeMs);
            System.out.printf("  ✓ L3 Database HIT (%.1fms)%n", accessTimeMs);
            
            VideoData video = l3Database.get(videoId);
            
            // Move to L2 first
            l2Cache.put(videoId, video.filePath);
            
            // Check if should promote to L1
            if (accessCounts.getOrDefault(videoId, 0L) > L2_TO_L1_PROMOTION_THRESHOLD) {
                promoteToL1(videoId, video);
                System.out.printf("  ↑ Promoted to L1%n");
            } else {
                System.out.printf("  ↑ Added to L2 (access count: %d)%n", 
                                accessCounts.getOrDefault(videoId, 1L));
            }
            System.out.println();
            
            recordAccess(videoId);
            return video;
        }
        
        l3Metrics.recordMiss();
        System.out.printf("  ✗ L3 Database MISS%n%n");
        return null;
    }
    
    /**
     * Add video to L3 database
     */
    public void addVideo(String videoId, String title, long fileSizeMB) {
        VideoData video = new VideoData(videoId, title, fileSizeMB);
        l3Database.put(videoId, video);
        accessCounts.put(videoId, 0L);
    }
    
    /**
     * Promote video to L1 cache
     */
    private void promoteToL1(String videoId, VideoData video) {
        l1Cache.put(videoId, video);
    }
    
    /**
     * Record access for promotion tracking
     */
    private void recordAccess(String videoId) {
        accessCounts.put(videoId, accessCounts.getOrDefault(videoId, 0L) + 1);
    }
    
    /**
     * Display cache statistics
     */
    public void printStatistics() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║              MULTI-LEVEL CACHE STATISTICS                      ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("📊 L1 CACHE (In-Memory):");
        System.out.printf("  Size: %d / %d%n", l1Cache.size(), L1_MAX_SIZE);
        System.out.printf("  Hit Rate: %.2f%%%n", l1Metrics.getHitRate());
        System.out.printf("  Avg Access Time: %.2fms%n", l1Metrics.getAverageAccessTime());
        System.out.printf("  Hits: %d, Misses: %d%n", l1Metrics.hits, l1Metrics.misses);
        
        System.out.println("\n📊 L2 CACHE (SSD-Backed):");
        System.out.printf("  Size: %d / %d%n", l2Cache.size(), L2_MAX_SIZE);
        System.out.printf("  Hit Rate: %.2f%%%n", l2Metrics.getHitRate());
        System.out.printf("  Avg Access Time: %.2fms%n", l2Metrics.getAverageAccessTime());
        System.out.printf("  Hits: %d, Misses: %d%n", l2Metrics.hits, l2Metrics.misses);
        
        System.out.println("\n📊 L3 DATABASE (All Videos):");
        System.out.printf("  Total Videos: %d%n", l3Database.size());
        System.out.printf("  Hit Rate: %.2f%%%n", l3Metrics.getHitRate());
        System.out.printf("  Avg Access Time: %.2fms%n", l3Metrics.getAverageAccessTime());
        System.out.printf("  Hits: %d, Misses: %d%n", l3Metrics.hits, l3Metrics.misses);
        
        // Overall statistics
        long totalHits = l1Metrics.hits + l2Metrics.hits + l3Metrics.hits;
        long totalAccess = totalHits + l1Metrics.misses;
        double overallHitRate = totalAccess > 0 ? (100.0 * totalHits / totalAccess) : 0;
        long totalTime = l1Metrics.totalAccessTime + l2Metrics.totalAccessTime + l3Metrics.totalAccessTime;
        long totalCount = l1Metrics.accessCount + l2Metrics.accessCount + l3Metrics.accessCount;
        double overallAvgTime = totalCount > 0 ? (totalTime / (double) totalCount) : 0;
        
        System.out.println("\n📈 OVERALL STATISTICS:");
        System.out.printf("  Overall Hit Rate: %.2f%%%n", overallHitRate);
        System.out.printf("  Overall Avg Access Time: %.2fms%n", overallAvgTime);
        System.out.printf("  Total Requests: %d%n%n", totalAccess);
    }
    
    /**
     * Display L1 cache contents
     */
    public void displayL1Contents() {
        System.out.println("\n📋 L1 CACHE CONTENTS:");
        System.out.println("─".repeat(50));
        if (l1Cache.isEmpty()) {
            System.out.println("(L1 Cache is empty)");
        } else {
            int count = 1;
            for (Map.Entry<String, VideoData> entry : l1Cache.entrySet()) {
                System.out.printf("  %d. %s%n", count++, entry.getValue());
            }
        }
    }
    
    // Main method for testing
    public static void main(String[] args) {
        Problem10_MultiLevelCache cache = new Problem10_MultiLevelCache();
        
        System.out.println("=== Problem 10: Multi-Level Cache System ===\n");
        
        System.out.println("Test Case 1: Add videos to database");
        cache.addVideo("video_001", "Introduction to Java", 500);
        cache.addVideo("video_002", "Advanced Algorithms", 1200);
        cache.addVideo("video_003", "Web Development", 800);
        cache.addVideo("video_999", "Popular Tutorial", 2000);
        System.out.println("✓ 4 videos added to L3 Database\n");
        
        System.out.println("Test Case 2: First access (L3 Database HIT)");
        cache.getVideo("video_001");
        
        System.out.println("Test Case 3: Second access to same video (L2 → L1)");
        cache.getVideo("video_001");
        
        System.out.println("Test Case 4: Access different videos");
        cache.getVideo("video_999");
        cache.getVideo("video_002");
        
        System.out.println("Test Case 5: Multiple accesses (promotion threshold)");
        for (int i = 0; i < 6; i++) {
            cache.getVideo("video_999");
        }
        
        System.out.println("Test Case 6: Non-existent video");
        cache.getVideo("video_nonexistent");
        
        System.out.println("Test Case 7: Display L1 Cache");
        cache.displayL1Contents();
        
        System.out.println("\nTest Case 8: Final Statistics");
        cache.printStatistics();
    }
}
