import java.util.*;

/**
 * Problem 1: Social Media Username Availability Checker
 * 
 * This system checks username availability in real-time using hash tables.
 * - O(1) lookup performance
 * - Handles concurrent checks
 * - Suggests alternatives if username is taken
 * - Tracks popularity of attempted usernames
 */
public class Problem1_UsernameAvailability {
    
    private HashMap<String, Integer> usernameToUserId;  // username -> userId mapping
    private HashMap<String, Integer> usernameAttempts;   // Track attempts for each username
    
    public Problem1_UsernameAvailability() {
        this.usernameToUserId = new HashMap<>();
        this.usernameAttempts = new HashMap<>();
    }
    
    /**
     * Check if username is available in O(1) time
     */
    public boolean checkAvailability(String username) {
        // Track attempt
        usernameAttempts.put(username, usernameAttempts.getOrDefault(username, 0) + 1);
        
        // Check availability
        return !usernameToUserId.containsKey(username.toLowerCase());
    }
    
    /**
     * Register a new username
     */
    public boolean registerUsername(String username, int userId) {
        String lowerUsername = username.toLowerCase();
        
        if (usernameToUserId.containsKey(lowerUsername)) {
            return false; // Already taken
        }
        
        usernameToUserId.put(lowerUsername, userId);
        return true;
    }
    
    /**
     * Suggest alternatives if username is taken
     */
    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();
        String base = username.toLowerCase();
        
        // Try appending numbers
        for (int i = 1; i <= 5; i++) {
            String suggestion = base + i;
            if (!usernameToUserId.containsKey(suggestion)) {
                suggestions.add(suggestion);
            }
        }
        
        // Try with underscore and numbers
        for (int i = 1; i <= 5; i++) {
            String suggestion = base + "_" + i;
            if (!usernameToUserId.containsKey(suggestion)) {
                suggestions.add(suggestion);
            }
        }
        
        // Try replacing characters
        if (base.length() >= 2) {
            String withDot = base.replace("_", ".");
            if (!usernameToUserId.containsKey(withDot)) {
                suggestions.add(withDot);
            }
        }
        
        return suggestions;
    }
    
    /**
     * Get most attempted usernames (popularity ranking)
     */
    public List<String> getMostAttempted(int topN) {
        return usernameAttempts.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }
    
    /**
     * Get attempt count for a username
     */
    public int getAttemptCount(String username) {
        return usernameAttempts.getOrDefault(username.toLowerCase(), 0);
    }
    
    // Main method for testing
    public static void main(String[] args) {
        Problem1_UsernameAvailability checker = new Problem1_UsernameAvailability();
        
        System.out.println("=== Problem 1: Username Availability Checker ===\n");
        
        // Register some users
        checker.registerUsername("john_doe", 1001);
        checker.registerUsername("jane_smith", 1002);
        checker.registerUsername("admin", 1003);
        checker.registerUsername("admin1", 1004);
        
        // Simulate multiple attempts
        for (int i = 0; i < 100; i++) {
            checker.checkAvailability("admin");
        }
        for (int i = 0; i < 50; i++) {
            checker.checkAvailability("john_doe");
        }
        for (int i = 0; i < 120; i++) {
            checker.checkAvailability("newuser");
        }
        
        // Test cases
        System.out.println("Test Case 1: Check Availability");
        System.out.println("checkAvailability(\"john_doe\") = " + checker.checkAvailability("john_doe"));
        System.out.println("checkAvailability(\"jane_smith\") = " + checker.checkAvailability("jane_smith"));
        System.out.println("checkAvailability(\"available_name\") = " + checker.checkAvailability("available_name"));
        
        System.out.println("\nTest Case 2: Suggest Alternatives for taken username");
        List<String> suggestions = checker.suggestAlternatives("john_doe");
        System.out.println("suggestAlternatives(\"john_doe\") = " + suggestions);
        
        System.out.println("\nTest Case 3: Most Attempted Usernames");
        List<String> mostAttempted = checker.getMostAttempted(3);
        System.out.println("getMostAttempted(3) = " + mostAttempted);
        
        System.out.println("\nTest Case 4: Attempt Counts");
        System.out.println("getAttemptCount(\"admin\") = " + checker.getAttemptCount("admin"));
        System.out.println("getAttemptCount(\"john_doe\") = " + checker.getAttemptCount("john_doe"));
        System.out.println("getAttemptCount(\"newuser\") = " + checker.getAttemptCount("newuser"));
    }
}
