import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Problem 2: E-commerce Flash Sale Inventory Manager
 * 
 * Manages inventory during flash sales with concurrent access handling.
 * - O(1) stock lookup and updates
 * - Thread-safe with atomic operations
 * - Waiting list for out-of-stock items
 * - Real-time stock availability checks
 */
public class Problem2_InventoryManager {
    
    // Thread-safe inventory: productId -> stock count
    private final ConcurrentHashMap<String, AtomicInteger> inventory;
    
    // Waiting list: productId -> LinkedList of users (FIFO)
    private final ConcurrentHashMap<String, Queue<Integer>> waitingLists;
    
    // Purchase history for auditing
    private final ConcurrentHashMap<Integer, List<String>> purchaseHistory;
    
    public Problem2_InventoryManager() {
        this.inventory = new ConcurrentHashMap<>();
        this.waitingLists = new ConcurrentHashMap<>();
        this.purchaseHistory = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialize a product with stock count
     */
    public void addProduct(String productId, int initialStock) {
        inventory.put(productId, new AtomicInteger(initialStock));
        waitingLists.put(productId, new ConcurrentLinkedQueue<>());
    }
    
    /**
     * Check current stock level in O(1) time
     */
    public int checkStock(String productId) {
        AtomicInteger stock = inventory.get(productId);
        return (stock != null) ? stock.get() : -1; // -1 indicates product doesn't exist
    }
    
    /**
     * Attempt to purchase an item
     * Returns: "Success" or "Added to waiting list with position"
     */
    public synchronized String purchaseItem(String productId, int userId) {
        AtomicInteger stock = inventory.get(productId);
        
        if (stock == null) {
            return "Error: Product not found";
        }
        
        // Try to decrease stock atomically
        int currentStock = stock.getAndDecrement();
        
        if (currentStock > 0) {
            // Purchase successful
            recordPurchase(userId, productId, true);
            return "Success, " + (currentStock - 1) + " units remaining";
        } else {
            // Stock exhausted, add to waiting list
            stock.incrementAndGet(); // Restore the decremented value
            Queue<Integer> waitingList = waitingLists.get(productId);
            waitingList.offer(userId);
            recordPurchase(userId, productId, false);
            return "Added to waiting list, position #" + waitingList.size();
        }
    }
    
    /**
     * Process item return
     */
    public synchronized String returnItem(String productId, int userId) {
        AtomicInteger stock = inventory.get(productId);
        
        if (stock == null) {
            return "Error: Product not found";
        }
        
        stock.incrementAndGet();
        Queue<Integer> waitingList = waitingLists.get(productId);
        
        // Check if there's someone waiting
        if (!waitingList.isEmpty()) {
            Integer waitingUser = waitingList.poll();
            int newStock = stock.decrementAndGet();
            recordPurchase(waitingUser, productId, true);
            return "Item returned. Waiting customer " + waitingUser + 
                   " automatically fulfilled. Stock: " + newStock;
        }
        
        return "Item returned. New stock: " + stock.get();
    }
    
    /**
     * Get waiting list size for a product
     */
    public int getWaitingListSize(String productId) {
        Queue<Integer> waitingList = waitingLists.get(productId);
        return (waitingList != null) ? waitingList.size() : -1;
    }
    
    /**
     * Get detailed inventory status
     */
    public String getInventoryStatus(String productId) {
        int stock = checkStock(productId);
        int waiting = getWaitingListSize(productId);
        
        if (stock == -1) {
            return "Product not found";
        }
        
        return String.format("Product: %s | Stock: %d | Waiting: %d", 
                            productId, stock, waiting);
    }
    
    /**
     * Record purchase in history
     */
    private void recordPurchase(int userId, String productId, boolean successful) {
        String record = String.format("[%s] %s - %s", 
                                     System.currentTimeMillis(), 
                                     productId, 
                                     successful ? "PURCHASED" : "WAITLISTED");
        purchaseHistory.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(record);
    }
    
    /**
     * Get purchase history for a user
     */
    public List<String> getPurchaseHistory(int userId) {
        List<String> history = purchaseHistory.get(userId);
        return history != null ? history : new ArrayList<>();
    }
    
    /**
     * Simulate concurrent purchases
     */
    public void simulateConcurrentPurchases(String productId, int numBuyers) 
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        System.out.println("\n--- Simulating " + numBuyers + " concurrent purchases ---");
        
        for (int i = 1; i <= numBuyers; i++) {
            final int userId = 10000 + i;
            executor.execute(() -> {
                String result = purchaseItem(productId, userId);
                System.out.println("User " + userId + ": " + result);
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
    
    // Main method for testing
    public static void main(String[] args) throws InterruptedException {
        Problem2_InventoryManager manager = new Problem2_InventoryManager();
        
        System.out.println("=== Problem 2: E-commerce Flash Sale Inventory Manager ===\n");
        
        // Setup products
        manager.addProduct("IPHONE15_256GB", 5);
        manager.addProduct("LAPTOP_PRO", 3);
        
        System.out.println("Test Case 1: Initial Stock Check");
        System.out.println("checkStock(\"IPHONE15_256GB\") = " + manager.checkStock("IPHONE15_256GB"));
        System.out.println("checkStock(\"LAPTOP_PRO\") = " + manager.checkStock("LAPTOP_PRO"));
        
        System.out.println("\nTest Case 2: Sequential Purchases");
        for (int i = 1; i <= 3; i++) {
            String result = manager.purchaseItem("IPHONE15_256GB", 1000 + i);
            System.out.println("User " + (1000 + i) + ": " + result);
        }
        
        System.out.println("\nTest Case 3: Inventory Status");
        System.out.println(manager.getInventoryStatus("IPHONE15_256GB"));
        
        System.out.println("\nTest Case 4: Concurrent Purchases (10 users buying 5 items)");
        manager.simulateConcurrentPurchases("IPHONE15_256GB", 10);
        
        System.out.println("\nTest Case 5: Final Status");
        System.out.println(manager.getInventoryStatus("IPHONE15_256GB"));
        System.out.println("Waiting List Size: " + manager.getWaitingListSize("IPHONE15_256GB"));
        
        System.out.println("\nTest Case 6: Process Return");
        System.out.println(manager.returnItem("IPHONE15_256GB", 1001));
    }
}
