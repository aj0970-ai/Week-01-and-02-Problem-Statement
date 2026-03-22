import java.util.*;
import java.util.stream.*;

/**
 * Problem 7: Autocomplete System for Search Engine
 * 
 * Provides autocomplete suggestions based on search history.
 * - Stores query frequencies
 * - Returns top 10 suggestions for any prefix
 * - Updates frequencies based on new searches
 * - Optimized for memory and performance
 */
public class Problem7_AutocompleteSystem {
    
    /**
     * Trie node for prefix matching
     */
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        PriorityQueue<SuggestionItem> topSuggestions = 
            new PriorityQueue<>((a, b) -> a.frequency - b.frequency);
        int maxSuggestions = 10;
    }
    
    /**
     * Suggestion item
     */
    private static class SuggestionItem implements Comparable<SuggestionItem> {
        String query;
        int frequency;
        
        SuggestionItem(String query, int frequency) {
            this.query = query;
            this.frequency = frequency;
        }
        
        @Override
        public int compareTo(SuggestionItem other) {
            if (this.frequency != other.frequency) {
                return Integer.compare(this.frequency, other.frequency);
            }
            return this.query.compareTo(other.query);
        }
        
        @Override
        public String toString() {
            return String.format("\"%s\" (%d searches)", query, frequency);
        }
    }
    
    private TrieNode root;
    private HashMap<String, Integer> globalFrequency;
    private HashMap<String, Long> lastUpdateTime;
    
    public Problem7_AutocompleteSystem() {
        this.root = new TrieNode();
        this.globalFrequency = new HashMap<>();
        this.lastUpdateTime = new HashMap<>();
    }
    
    /**
     * Add a query or update its frequency
     */
    public void addQuery(String query) {
        query = query.toLowerCase();
        globalFrequency.put(query, globalFrequency.getOrDefault(query, 0) + 1);
        lastUpdateTime.put(query, System.currentTimeMillis());
        
        // Insert into Trie
        TrieNode node = root;
        
        for (char c : query.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        
        // Update all ancestor nodes with this suggestion
        updateTrieWithQuery(query);
    }
    
    /**
     * Update all Trie prefixes with this query
     */
    private void updateTrieWithQuery(String query) {
        int frequency = globalFrequency.get(query);
        TrieNode node = root;
        
        for (Character c : query.toCharArray()) {
            node = node.children.get(c);
            
            SuggestionItem item = new SuggestionItem(query, frequency);
            
            // Maintain top 10 suggestions
            if (node.topSuggestions.size() < node.maxSuggestions) {
                node.topSuggestions.offer(item);
            } else if (frequency > node.topSuggestions.peek().frequency) {
                node.topSuggestions.poll();
                node.topSuggestions.offer(item);
            }
        }
    }
    
    /**
     * Get suggestions for a prefix
     */
    public List<String> getSuggestions(String prefix) {
        prefix = prefix.toLowerCase();
        TrieNode node = root;
        
        // Navigate to prefix node
        for (char c : prefix.toCharArray()) {
            if (!node.children.containsKey(c)) {
                return new ArrayList<>(); // No suggestions
            }
            node = node.children.get(c);
        }
        
        // Get top suggestions from this node
        return node.topSuggestions.stream()
                  .sorted((a, b) -> Integer.compare(b.frequency, a.frequency))
                  .limit(10)
                  .map(item -> item.query)
                  .collect(Collectors.toList());
    }
    
    /**
     * Get detailed suggestions with frequencies
     */
    public List<SuggestionItem> getDetailedSuggestions(String prefix) {
        prefix = prefix.toLowerCase();
        TrieNode node = root;
        
        // Navigate to prefix node
        for (char c : prefix.toCharArray()) {
            if (!node.children.containsKey(c)) {
                return new ArrayList<>();
            }
            node = node.children.get(c);
        }
        
        // Get top suggestions from this node
        return node.topSuggestions.stream()
                  .sorted((a, b) -> Integer.compare(b.frequency, a.frequency))
                  .limit(10)
                  .collect(Collectors.toList());
    }
    
    /**
     * Get frequency of a query
     */
    public int getQueryFrequency(String query) {
        return globalFrequency.getOrDefault(query.toLowerCase(), 0);
    }
    
    /**
     * Get trending queries (most searched recently)
     */
    public List<String> getTrendingQueries(int topN) {
        return globalFrequency.entrySet().stream()
                .sorted((a, b) -> {
                    int freqCompare = b.getValue().compareTo(a.getValue());
                    if (freqCompare != 0) return freqCompare;
                    return lastUpdateTime.get(b.getKey())
                                        .compareTo(lastUpdateTime.get(a.getKey()));
                })
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Search - same as addQuery but used for display purposes
     */
    public void search(String query) {
        addQuery(query);
        System.out.printf("Search recorded: \"%s\" (Total: %d times)%n", 
                        query, globalFrequency.get(query.toLowerCase()));
    }
    
    /**
     * Get autocomplete display
     */
    public void displayAutocompleteSuggestions(String prefix) {
        System.out.printf("\n🔍 Autocomplete for: \"%s\"%n", prefix);
        System.out.println("─".repeat(50));
        
        List<SuggestionItem> suggestions = getDetailedSuggestions(prefix);
        
        if (suggestions.isEmpty()) {
            System.out.println("  (No suggestions)");
            return;
        }
        
        int rank = 1;
        for (SuggestionItem item : suggestions) {
            System.out.printf("%2d. %s%n", rank++, item);
        }
    }
    
    // Main method for testing
    public static void main(String[] args) {
        Problem7_AutocompleteSystem autocomplete = new Problem7_AutocompleteSystem();
        
        System.out.println("=== Problem 7: Autocomplete System for Search Engine ===\n");
        
        System.out.println("Test Case 1: Add search queries with frequencies");
        autocomplete.search("java tutorial");
        autocomplete.search("java tutorial");
        autocomplete.search("javascript");
        autocomplete.search("javascript framework");
        autocomplete.search("java download");
        autocomplete.search("java download");
        autocomplete.search("java download");
        autocomplete.search("java features");
        autocomplete.search("java arrays");
        autocomplete.search("java string");
        autocomplete.search("javascript tutorial");
        
        System.out.println("\nTest Case 2: Get suggestions for prefix \"java\"");
        autocomplete.displayAutocompleteSuggestions("java");
        
        System.out.println("\nTest Case 3: Get suggestions for prefix \"java \"");
        autocomplete.displayAutocompleteSuggestions("java ");
        
        System.out.println("\nTest Case 4: Get simple suggestions list");
        List<String> suggestions = autocomplete.getSuggestions("jav");
        System.out.println("Suggestions for \"jav\": " + suggestions);
        
        System.out.println("\nTest Case 5: Get query frequency");
        System.out.println("Frequency of \"java tutorial\": " + 
                          autocomplete.getQueryFrequency("java tutorial"));
        System.out.println("Frequency of \"javascript\": " + 
                          autocomplete.getQueryFrequency("javascript"));
        
        System.out.println("\nTest Case 6: Get trending queries");
        List<String> trending = autocomplete.getTrendingQueries(5);
        System.out.println("Top 5 Trending Queries:");
        for (int i = 0; i < trending.size(); i++) {
            String query = trending.get(i);
            System.out.printf("  %d. %s (%d searches)%n", 
                            i + 1, query, autocomplete.getQueryFrequency(query));
        }
        
        System.out.println("\nTest Case 7: Partial prefix matching");
        autocomplete.displayAutocompleteSuggestions("j");
        autocomplete.displayAutocompleteSuggestions("ja");
        
        System.out.println("\nTest Case 8: Update frequencies");
        System.out.println("\nSearching for \"java tutorial\" 5 more times...");
        for (int i = 0; i < 5; i++) {
            autocomplete.search("java tutorial");
        }
        
        System.out.println("\nTop trending after update:");
        List<String> newTrending = autocomplete.getTrendingQueries(3);
        for (int i = 0; i < newTrending.size(); i++) {
            String query = newTrending.get(i);
            System.out.printf("  %d. %s (%d searches)%n", 
                            i + 1, query, autocomplete.getQueryFrequency(query));
        }
    }
}
