import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * Problem 5: Real-Time Analytics Dashboard for Website Traffic
 * 
 * Processes streaming page view events to maintain analytics.
 * - Tracks top visited pages
 * - Counts unique visitors
 * - Tracks traffic sources
 * - Updates dashboard in real-time
 */
public class Problem5_AnalyticsDashboard {
    
    /**
     * Represents a page view event
     */
    private static class PageViewEvent {
        String url;
        String userId;
        String source; // Google, Facebook, Direct, etc.
        long timestamp;
        
        PageViewEvent(String url, String userId, String source) {
            this.url = url;
            this.userId = userId;
            this.source = source;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    // Page URL -> visit count
    private ConcurrentHashMap<String, AtomicInteger> pageViewCounts;
    
    // Page URL -> Set of unique user IDs
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> uniqueVisitors;
    
    // Traffic source -> visit count
    private ConcurrentHashMap<String, AtomicInteger> sourceCount;
    
    // Event log for analysis
    private ConcurrentLinkedQueue<PageViewEvent> eventLog;
    
    // Dashboard update timestamp
    private long lastDashboardUpdate;
    
    public Problem5_AnalyticsDashboard() {
        this.pageViewCounts = new ConcurrentHashMap<>();
        this.uniqueVisitors = new ConcurrentHashMap<>();
        this.sourceCount = new ConcurrentHashMap<>();
        this.eventLog = new ConcurrentLinkedQueue<>();
        this.lastDashboardUpdate = System.currentTimeMillis();
    }
    
    /**
     * Process an incoming page view event
     */
    public void processEvent(PageViewEvent event) {
        // Track page view
        pageViewCounts.computeIfAbsent(event.url, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Track unique visitor
        uniqueVisitors.computeIfAbsent(event.url, k -> new ConcurrentHashMap<>())
                     .put(event.userId, true);
        
        // Track traffic source
        sourceCount.computeIfAbsent(event.source, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Log event
        eventLog.offer(event);
    }
    
    /**
     * Get top N visited pages
     */
    public List<Map.Entry<String, Integer>> getTopPages(int topN) {
        return pageViewCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue().get().compareTo(a.getValue().get()))
                .limit(topN)
                .collect(Collectors.toList());
    }
    
    /**
     * Get unique visitors count for a page
     */
    public int getUniqueVisitors(String url) {
        ConcurrentHashMap<String, Boolean> visitors = uniqueVisitors.get(url);
        return (visitors != null) ? visitors.size() : 0;
    }
    
    /**
     * Get total page views for a URL
     */
    public int getPageViews(String url) {
        AtomicInteger count = pageViewCounts.get(url);
        return (count != null) ? count.get() : 0;
    }
    
    /**
     * Get traffic source distribution
     */
    public Map<String, Double> getSourceDistribution() {
        long totalViews = sourceCount.values().stream()
                                     .mapToLong(a -> a.get())
                                     .sum();
        
        Map<String, Double> distribution = new HashMap<>();
        for (Map.Entry<String, AtomicInteger> entry : sourceCount.entrySet()) {
            double percentage = (entry.getValue().get() / (double) totalViews) * 100;
            distribution.put(entry.getKey(), percentage);
        }
        
        return distribution;
    }
    
    /**
     * Get detailed page analytics
     */
    public String getPageAnalytics(String url) {
        int totalViews = getPageViews(url);
        int uniqueVisitors = getUniqueVisitors(url);
        double bounceRate = (totalViews > 0) ? 
                           (100.0 * uniqueVisitors / totalViews) : 0;
        
        return String.format("Page: %s | Views: %d | Unique Visitors: %d | Bounce Rate: %.2f%%", 
                           url, totalViews, uniqueVisitors, bounceRate);
    }
    
    /**
     * Get comprehensive dashboard report
     */
    public void displayDashboard() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║          REAL-TIME ANALYTICS DASHBOARD                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        
        // Top pages section
        System.out.println("📊 TOP 5 PAGES:");
        System.out.println("─".repeat(60));
        List<Map.Entry<String, Integer>> topPages = getTopPages(5);
        int rank = 1;
        for (Map.Entry<String, Integer> entry : topPages) {
            int views = entry.getValue();
            int unique = getUniqueVisitors(entry.getKey());
            System.out.printf("%d. %-40s | Views: %6d | Unique: %5d%n", 
                            rank++, entry.getKey(), views, unique);
        }
        
        // Traffic sources section
        System.out.println("\n📈 TRAFFIC SOURCES:");
        System.out.println("─".repeat(60));
        Map<String, Double> sources = getSourceDistribution();
        sources.entrySet().stream()
               .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
               .forEach(e -> System.out.printf("%-15s: %.1f%%%n", e.getKey(), e.getValue()));
        
        // Overall statistics
        long totalViews = sourceCount.values().stream()
                                     .mapToLong(a -> a.get())
                                     .sum();
        long uniqueUsers = uniqueVisitors.values().stream()
                                         .mapToLong(set -> set.size())
                                         .sum();
        
        System.out.println("\n📋 OVERALL STATISTICS:");
        System.out.println("─".repeat(60));
        System.out.printf("Total Page Views: %,d%n", totalViews);
        System.out.printf("Unique Users (Global): %,d%n", uniqueUsers);
        System.out.printf("Unique Pages: %d%n", pageViewCounts.size());
        System.out.printf("Last Updated: %s%n", new java.util.Date());
        System.out.println();
    }
    
    /**
     * Simulate incoming events
     */
    public void simulateTraffic(int numberOfEvents) {
        String[] pages = {"/article/breaking-news", "/sports/championship", 
                         "/tech/ai-advances", "/health/wellness", 
                         "/entertainment/movies"};
        String[] sources = {"google", "facebook", "direct", "twitter", "reddit"};
        
        System.out.println("Simulating " + numberOfEvents + " page view events...\n");
        
        for (int i = 0; i < numberOfEvents; i++) {
            String page = pages[(int)(Math.random() * pages.length)];
            String source = sources[(int)(Math.random() * sources.length)];
            String userId = "user_" + (int)(Math.random() * 10000);
            
            PageViewEvent event = new PageViewEvent(page, userId, source);
            processEvent(event);
        }
        
        System.out.println("✓ Events processed successfully\n");
    }
    
    // Main method for testing
    public static void main(String[] args) {
        Problem5_AnalyticsDashboard dashboard = new Problem5_AnalyticsDashboard();
        
        System.out.println("=== Problem 5: Real-Time Analytics Dashboard ===\n");
        
        System.out.println("Test Case 1: Process sample events");
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_123", "google"));
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_456", "facebook"));
        dashboard.processEvent(new PageViewEvent("/sports/championship", "user_789", "direct"));
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_123", "google"));
        dashboard.processEvent(new PageViewEvent("/tech/ai-advances", "user_999", "twitter"));
        System.out.println("✓ Sample events processed\n");
        
        System.out.println("Test Case 2: Get specific page analytics");
        System.out.println(dashboard.getPageAnalytics("/article/breaking-news"));
        System.out.println(dashboard.getPageAnalytics("/sports/championship"));
        
        System.out.println("\nTest Case 3: Display initial dashboard");
        dashboard.displayDashboard();
        
        System.out.println("Test Case 4: Simulate heavier traffic (1000 events)");
        dashboard.simulateTraffic(1000);
        
        System.out.println("Test Case 5: Display updated dashboard");
        dashboard.displayDashboard();
    }
}
