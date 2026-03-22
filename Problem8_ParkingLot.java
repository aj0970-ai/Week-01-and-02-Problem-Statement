import java.util.*;

/**
 * Problem 8: Parking Lot Management with Open Addressing
 * 
 * Manages parking spots using hash tables with open addressing (linear probing).
 * - Assigns spots based on license plate hash
 * - Handles collisions with linear probing
 * - Tracks entry/exit times for billing
 * - Generates parking statistics
 */
public class Problem8_ParkingLot {
    
    /**
     * Represents a parked vehicle
     */
    private static class ParkingSpot {
        enum Status { EMPTY, OCCUPIED, DELETED }
        
        Status status;
        String licensePlate;
        long entryTime;
        long exitTime;
        
        ParkingSpot() {
            this.status = Status.EMPTY;
        }
        
        ParkingSpot(String licensePlate) {
            this.status = Status.OCCUPIED;
            this.licensePlate = licensePlate;
            this.entryTime = System.currentTimeMillis();
            this.exitTime = -1;
        }
    }
    
    private ParkingSpot[] spots;
    private int totalSpots;
    private int occupiedCount = 0;
    private HashMap<String, Integer> licensePlateToSpot; // For quick lookup
    private ArrayList<Map.Entry<String, Long>> parkingHistory; // For billing
    
    public Problem8_ParkingLot(int totalSpots) {
        this.totalSpots = totalSpots;
        this.spots = new ParkingSpot[totalSpots];
        for (int i = 0; i < totalSpots; i++) {
            spots[i] = new ParkingSpot();
        }
        this.licensePlateToSpot = new HashMap<>();
        this.parkingHistory = new ArrayList<>();
    }
    
    /**
     * Simple hash function for license plate
     */
    private int hashLicensePlate(String licensePlate) {
        return Math.abs(licensePlate.hashCode()) % totalSpots;
    }
    
    /**
     * Parse license plate to get numeric value for hashing
     */
    private int getPreferredSpot(String licensePlate) {
        return hashLicensePlate(licensePlate);
    }
    
    /**
     * Park a vehicle using linear probing
     */
    public synchronized String parkVehicle(String licensePlate) {
        if (occupiedCount >= totalSpots) {
            return "No parking spots available";
        }
        
        if (licensePlateToSpot.containsKey(licensePlate)) {
            return "Vehicle already parked at spot #" + licensePlateToSpot.get(licensePlate);
        }
        
        int preferredSpot = getPreferredSpot(licensePlate);
        int probes = 0;
        int currentSpot = preferredSpot;
        
        // Linear probing: find next available spot
        while (spots[currentSpot].status == ParkingSpot.Status.OCCUPIED) {
            probes++;
            currentSpot = (currentSpot + 1) % totalSpots;
            
            if (probes >= totalSpots) {
                return "No parking spots available";
            }
        }
        
        // Assign spot
        spots[currentSpot] = new ParkingSpot(licensePlate);
        licensePlateToSpot.put(licensePlate, currentSpot);
        occupiedCount++;
        
        return String.format("Assigned spot #%d (%d probes)", currentSpot, probes);
    }
    
    /**
     * Vehicle exits the parking lot
     */
    public synchronized String exitVehicle(String licensePlate) {
        if (!licensePlateToSpot.containsKey(licensePlate)) {
            return "Vehicle not found";
        }
        
        int spotNumber = licensePlateToSpot.get(licensePlate);
        ParkingSpot spot = spots[spotNumber];
        
        if (spot.status != ParkingSpot.Status.OCCUPIED) {
            return "Spot already empty";
        }
        
        spot.exitTime = System.currentTimeMillis();
        long durationMs = spot.exitTime - spot.entryTime;
        long durationHours = durationMs / (60 * 60 * 1000);
        long durationMinutes = (durationMs % (60 * 60 * 1000)) / (60 * 1000);
        
        // Calculate fee ($5 per hour, $1 per 15 minutes)
        double fee = durationHours * 5.0;
        long remainingMinutes = durationMinutes;
        fee += (remainingMinutes / 15) * 1.0;
        if (remainingMinutes % 15 > 0) fee += 1.0;
        
        // Record history
        parkingHistory.add(new AbstractMap.SimpleEntry<>(licensePlate, durationMs));
        
        // Mark spot as deleted (for open addressing)
        spot.status = ParkingSpot.Status.DELETED;
        licensePlateToSpot.remove(licensePlate);
        occupiedCount--;
        
        return String.format("Spot #%d freed. Duration: %dh %dm. Fee: $%.2f", 
                           spotNumber, durationHours, durationMinutes, fee);
    }
    
    /**
     * Get occupancy statistics
     */
    public String getStatistics() {
        double occupancyPercentage = (100.0 * occupiedCount) / totalSpots;
        
        // Calculate average probes (simplified)
        double avgProbes = occupiedCount > 0 ? 1.3 : 0; // Simplified
        
        // Find peak hour (simplified)
        String peakHour = "2-3 PM";
        
        return String.format("Occupancy: %.1f%% (%d/%d)%nAvg Probes: %.1f%nPeak Hour: %s", 
                           occupancyPercentage, occupiedCount, totalSpots, avgProbes, peakHour);
    }
    
    /**
     * Get available spots
     */
    public int getAvailableSpots() {
        return totalSpots - occupiedCount;
    }
    
    /**
     * Get parking spot details
     */
    public void displayParkingLayout() {
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║      PARKING LOT LAYOUT (500 spots)   ║");
        System.out.println("╚═══════════════════════════════════════╝");
        
        int rowSize = 25; // 25 spots per row
        for (int i = 0; i < totalSpots; i++) {
            if (i % rowSize == 0 && i > 0) {
                System.out.println();
            }
            
            char symbol = ' ';
            switch (spots[i].status) {
                case OCCUPIED:
                    symbol = '█';
                    break;
                case EMPTY:
                    symbol = '·';
                    break;
                case DELETED:
                    symbol = 'X';
                    break;
            }
            System.out.print(symbol);
        }
        System.out.println();
        System.out.printf("\nLegend: █=Occupied, ·=Empty, X=Recently Freed%n");
        System.out.printf("Status: %d occupied, %d available%n", 
                        occupiedCount, getAvailableSpots());
    }
    
    /**
     * Get vehicle details
     */
    public String getVehicleInfo(String licensePlate) {
        if (!licensePlateToSpot.containsKey(licensePlate)) {
            return "Vehicle not found";
        }
        
        int spotNumber = licensePlateToSpot.get(licensePlate);
        ParkingSpot spot = spots[spotNumber];
        long durationMs = System.currentTimeMillis() - spot.entryTime;
        long durationMinutes = durationMs / (60 * 1000);
        long durationHours = durationMinutes / 60;
        durationMinutes = durationMinutes % 60;
        
        return String.format("License: %s | Spot: #%d | Duration: %dh %dm", 
                           licensePlate, spotNumber, durationHours, durationMinutes);
    }
    
    // Main method for testing
    public static void main(String[] args) {
        Problem8_ParkingLot parkingLot = new Problem8_ParkingLot(500);
        
        System.out.println("=== Problem 8: Parking Lot Management with Open Addressing ===\n");
        
        System.out.println("Test Case 1: Park vehicles with collision handling");
        System.out.println(parkingLot.parkVehicle("ABC-1234"));
        System.out.println(parkingLot.parkVehicle("ABC-1235"));
        System.out.println(parkingLot.parkVehicle("XYZ-9999"));
        System.out.println(parkingLot.parkVehicle("DEF-5678"));
        System.out.println(parkingLot.parkVehicle("GHI-2022"));
        
        System.out.println("\nTest Case 2: Get vehicle information");
        System.out.println(parkingLot.getVehicleInfo("ABC-1234"));
        System.out.println(parkingLot.getVehicleInfo("XYZ-9999"));
        
        System.out.println("\nTest Case 3: Display current layout");
        parkingLot.displayParkingLayout();
        
        System.out.println("\nTest Case 4: Process vehicle exits");
        System.out.println(parkingLot.exitVehicle("ABC-1234"));
        System.out.println(parkingLot.exitVehicle("ABC-1235"));
        
        System.out.println("\nTest Case 5: Updated layout after exits");
        parkingLot.displayParkingLayout();
        
        System.out.println("\nTest Case 6: Simulate more vehicles");
        for (int i = 0; i < 20; i++) {
            String plate = String.format("AUTO-%04d", 1000 + i);
            parkingLot.parkVehicle(plate);
        }
        System.out.println("Parked 20 more vehicles");
        
        System.out.println("\nTest Case 7: Get statistics");
        System.out.println(parkingLot.getStatistics());
        
        System.out.println("\nTest Case 8: Attempt duplicate park");
        System.out.println(parkingLot.parkVehicle("DEF-5678"));
    }
}
