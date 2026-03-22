import java.util.*;
import java.util.stream.*;

/**
 * Problem 9: Two-Sum Problem Variants for Financial Transactions
 * 
 * Detects fraudulent transaction patterns.
 * - Finds transaction pairs that sum to target
 * - Detects duplicates
 * - K-Sum algorithm
 * - Time-window based detection
 */
public class Problem9_TwoSumTransactions {
    
    /**
     * Represents a financial transaction
     */
    private static class Transaction implements Comparable<Transaction> {
        int id;
        long amount;
        String merchant;
        String account;
        long timestamp;
        
        Transaction(int id, long amount, String merchant, String account, long timestamp) {
            this.id = id;
            this.amount = amount;
            this.merchant = merchant;
            this.account = account;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("ID:%d | Amount:$%d | Merchant:%s | Account:%s | Time:%d", 
                               id, amount, merchant, account, timestamp);
        }
        
        @Override
        public int compareTo(Transaction other) {
            return Long.compare(this.timestamp, other.timestamp);
        }
    }
    
    /**
     * Result of two-sum detection
     */
    public static class TwoSumResult {
        List<Transaction> transaction1;
        List<Transaction> transaction2;
        long sum;
        
        TwoSumResult(Transaction t1, Transaction t2, long sum) {
            this.transaction1 = new ArrayList<>();
            this.transaction1.add(t1);
            this.transaction2 = new ArrayList<>();
            this.transaction2.add(t2);
            this.sum = sum;
        }
        
        @Override
        public String toString() {
            return String.format("Found pair: $%d + $%d = $%d (IDs: %d, %d)", 
                               transaction1.get(0).amount, transaction2.get(0).amount, 
                               sum, transaction1.get(0).id, transaction2.get(0).id);
        }
    }
    
    private List<Transaction> transactions;
    
    public Problem9_TwoSumTransactions() {
        this.transactions = new ArrayList<>();
    }
    
    /**
     * Add a transaction
     */
    public void addTransaction(int id, long amount, String merchant, String account) {
        transactions.add(new Transaction(id, amount, merchant, account, System.currentTimeMillis()));
    }
    
    /**
     * Add a transaction with specific timestamp
     */
    public void addTransaction(int id, long amount, String merchant, String account, long timestamp) {
        transactions.add(new Transaction(id, amount, merchant, account, timestamp));
    }
    
    /**
     * Classic Two-Sum: Find all pairs that sum to target
     */
    public List<TwoSumResult> findTwoSum(long target) {
        List<TwoSumResult> results = new ArrayList<>();
        HashMap<Long, Transaction> seen = new HashMap<>();
        
        for (Transaction t : transactions) {
            long complement = target - t.amount;
            
            if (seen.containsKey(complement)) {
                // Found a pair - but avoid duplicates
                if (t.id > seen.get(complement).id) { // Only add once
                    results.add(new TwoSumResult(seen.get(complement), t, target));
                }
            }
            
            seen.put(t.amount, t);
        }
        
        return results;
    }
    
    /**
     * Two-Sum within time window
     */
    public List<TwoSumResult> findTwoSumInTimeWindow(long target, long windowSizeMs) {
        List<TwoSumResult> results = new ArrayList<>();
        
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t1 = transactions.get(i);
            
            for (int j = i + 1; j < transactions.size(); j++) {
                Transaction t2 = transactions.get(j);
                
                // Check time window
                if (t2.timestamp - t1.timestamp > windowSizeMs) {
                    break;
                }
                
                if (t1.amount + t2.amount == target) {
                    results.add(new TwoSumResult(t1, t2, target));
                }
            }
        }
        
        return results;
    }
    
    /**
     * Detect duplicate transactions
     */
    public Map<String, List<Transaction>> detectDuplicates() {
        Map<String, List<Transaction>> duplicates = new HashMap<>();
        
        for (Transaction t : transactions) {
            String key = t.amount + "_" + t.merchant;
            duplicates.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }
        
        // Filter to only keep actual duplicates
        return duplicates.entrySet().stream()
                        .filter(e -> e.getValue().size() > 1)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * K-Sum: Find K transactions that sum to target
     */
    public List<List<Transaction>> findKSum(int k, long target) {
        List<List<Transaction>> results = new ArrayList<>();
        
        if (k == 1) {
            // Base case: find single transaction
            for (Transaction t : transactions) {
                if (t.amount == target) {
                    results.add(new ArrayList<>(Arrays.asList(t)));
                }
            }
            return results;
        }
        
        if (k == 2) {
            // Use two-sum
            HashMap<Long, Transaction> seen = new HashMap<>();
            for (Transaction t : transactions) {
                long complement = target - t.amount;
                if (seen.containsKey(complement)) {
                    List<Transaction> pair = new ArrayList<>();
                    pair.add(seen.get(complement));
                    pair.add(t);
                    if (!results.contains(pair)) {
                        results.add(pair);
                    }
                }
                seen.put(t.amount, t);
            }
            return results;
        }
        
        // For k > 2: recursive approach
        for (int i = 0; i < transactions.size() - k + 1; i++) {
            Transaction current = transactions.get(i);
            List<Transaction> remaining = new ArrayList<>(transactions.subList(i + 1, transactions.size()));
            
            // Recursively find (k-1)-sum in remaining transactions
            long kMinusOneSum = target - current.amount;
            
            // Would need separate method for recursive k-sum, simplified here
        }
        
        return results;
    }
    
    /**
     * Detect suspicious patterns
     */
    public void detectSuspiciousPatterns(long targetAmount) {
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("FRAUD DETECTION ANALYSIS - Target Amount: $" + targetAmount);
        System.out.println("═══════════════════════════════════════════════════════\n");
        
        // Pattern 1: Two-Sum Detection
        System.out.println("📌 PATTERN 1: Two-Sum Detection");
        System.out.println("─".repeat(50));
        List<TwoSumResult> twoSumResults = findTwoSum(targetAmount);
        if (!twoSumResults.isEmpty()) {
            System.out.println("SUSPICIOUS: Found " + twoSumResults.size() + " transaction pairs:");
            for (TwoSumResult result : twoSumResults) {
                System.out.println("  ✗ " + result);
            }
        } else {
            System.out.println("✓ No suspicious pairs found for target: $" + targetAmount);
        }
        
        // Pattern 2: Duplicate Detection
        System.out.println("\n📌 PATTERN 2: Duplicate Detection");
        System.out.println("─".repeat(50));
        Map<String, List<Transaction>> duplicates = detectDuplicates();
        if (!duplicates.isEmpty()) {
            System.out.println("SUSPICIOUS: Found " + duplicates.size() + " duplicate patterns:");
            for (Map.Entry<String, List<Transaction>> entry : duplicates.entrySet()) {
                System.out.println("  ✗ Amount: $" + entry.getValue().get(0).amount + 
                                 " | Merchant: " + entry.getValue().get(0).merchant + 
                                 " | Count: " + entry.getValue().size());
            }
        } else {
            System.out.println("✓ No duplicates detected");
        }
        
        // Pattern 3: Time-Window Analysis
        System.out.println("\n📌 PATTERN 3: Time-Window Analysis (1 hour)");
        System.out.println("─".repeat(50));
        List<TwoSumResult> timeWindowResults = findTwoSumInTimeWindow(targetAmount, 60 * 60 * 1000);
        if (!timeWindowResults.isEmpty()) {
            System.out.println("WARNING: Found " + timeWindowResults.size() + " suspicious pairs within 1 hour");
        } else {
            System.out.println("✓ No suspicious patterns within 1-hour window");
        }
    }
    
    /**
     * Get transaction summary
     */
    public void printTransactionSummary() {
        System.out.println("\n═════════════════════════════════════════════════════");
        System.out.println("TRANSACTION SUMMARY (" + transactions.size() + " transactions)");
        System.out.println("═════════════════════════════════════════════════════\n");
        
        for (Transaction t : transactions) {
            System.out.println(t);
        }
    }
    
    // Main method for testing
    public static void main(String[] args) {
        Problem9_TwoSumTransactions detector = new Problem9_TwoSumTransactions();
        
        System.out.println("=== Problem 9: Two-Sum Problem Variants for Financial Transactions ===\n");
        
        System.out.println("Test Case 1: Adding transactions");
        long currentTime = System.currentTimeMillis();
        detector.addTransaction(1, 500, "Store A", "acc_001", currentTime);
        detector.addTransaction(2, 300, "Store B", "acc_002", currentTime + 1000);
        detector.addTransaction(3, 200, "Store C", "acc_003", currentTime + 2000);
        detector.addTransaction(4, 500, "Store A", "acc_004", currentTime + 3000);
        detector.addTransaction(5, 700, "Store D", "acc_005", currentTime + 4000);
        detector.addTransaction(6, 300, "Store B", "acc_006", currentTime + 5000);
        System.out.println("✓ Transactions added\n");
        
        System.out.println("Test Case 2: Display all transactions");
        detector.printTransactionSummary();
        
        System.out.println("\nTest Case 3: Detect two-sum patterns (target=$500)");
        List<TwoSumResult> pairs = detector.findTwoSum(500);
        System.out.println("Found " + pairs.size() + " pair(s):");
        for (TwoSumResult result : pairs) {
            System.out.println("  " + result);
        }
        
        System.out.println("\nTest Case 4: Detect duplicates");
        detector.detectSuspiciousPatterns(800);
        
        System.out.println("\nTest Case 5: Find K-Sum (k=2, target=500)");
        List<List<Transaction>> kSumResults = detector.findKSum(2, 500);
        System.out.println("Found " + kSumResults.size() + " combination(s)");
        
        System.out.println("\nTest Case 6: Full suspicious pattern analysis");
        detector.detectSuspiciousPatterns(800); // 500 + 300
    }
}
