import java.util.*;
import java.util.stream.*;

/**
 * Problem 4: Plagiarism Detection System
 * 
 * Detects plagiarism using n-gram matching.
 * - Breaks documents into n-grams
 * - Uses hash tables for O(1) n-gram lookup
 * - Calculates similarity percentage
 * - Identifies matching documents efficiently
 */
public class Problem4_PlagiarismDetector {
    
    /**
     * Represents a document with its n-grams
     */
    private static class Document {
        int documentId;
        String title;
        Set<String> nGrams;
        
        Document(int documentId, String title, Set<String> nGrams) {
            this.documentId = documentId;
            this.title = title;
            this.nGrams = nGrams;
        }
    }
    
    // Maps n-gram to set of documents containing it
    private HashMap<String, Set<Integer>> nGramIndex;
    
    // Maps document ID to document
    private HashMap<Integer, Document> documents;
    
    private static final int N_GRAM_SIZE = 5; // 5-gram by default
    
    public Problem4_PlagiarismDetector() {
        this.nGramIndex = new HashMap<>();
        this.documents = new HashMap<>();
    }
    
    /**
     * Extract n-grams from text
     */
    private Set<String> extractNGrams(String text, int nGramSize) {
        Set<String> nGrams = new HashSet<>();
        String[] words = text.toLowerCase()
                            .replaceAll("[^a-z0-9\\s]", "") // Remove special chars
                            .split("\\s+");
        
        for (int i = 0; i <= words.length - nGramSize; i++) {
            StringBuilder nGram = new StringBuilder();
            for (int j = 0; j < nGramSize; j++) {
                if (j > 0) nGram.append(" ");
                nGram.append(words[i + j]);
            }
            nGrams.add(nGram.toString());
        }
        
        return nGrams;
    }
    
    /**
     * Add a document to the detection system
     */
    public void addDocument(int documentId, String title, String content) {
        Set<String> nGrams = extractNGrams(content, N_GRAM_SIZE);
        Document doc = new Document(documentId, title, nGrams);
        documents.put(documentId, doc);
        
        // Add n-grams to index
        for (String nGram : nGrams) {
            nGramIndex.computeIfAbsent(nGram, k -> new HashSet<>()).add(documentId);
        }
        
        System.out.printf("Document %d (\"%s\") analyzed: %d n-grams extracted%n", 
                         documentId, title, nGrams.size());
    }
    
    /**
     * Find similar documents to a given document
     */
    public List<Map.Entry<Integer, Double>> findSimilarDocuments(int documentId) {
        Document document = documents.get(documentId);
        if (document == null) {
            return new ArrayList<>();
        }
        
        // Count matching n-grams with each other document
        Map<Integer, Integer> matchingNGrams = new HashMap<>();
        
        for (String nGram : document.nGrams) {
            Set<Integer> docsWithNGram = nGramIndex.get(nGram);
            if (docsWithNGram != null) {
                for (Integer otherDocId : docsWithNGram) {
                    if (!otherDocId.equals(documentId)) {
                        matchingNGrams.put(otherDocId, 
                                         matchingNGrams.getOrDefault(otherDocId, 0) + 1);
                    }
                }
            }
        }
        
        // Calculate similarity scores
        Map<Integer, Double> similarityScores = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : matchingNGrams.entrySet()) {
            int otherDocId = entry.getKey();
            int matches = entry.getValue();
            
            Document otherDoc = documents.get(otherDocId);
            double totalNGrams = Math.max(document.nGrams.size(), 
                                         otherDoc.nGrams.size());
            double similarity = (matches / totalNGrams) * 100;
            
            similarityScores.put(otherDocId, similarity);
        }
        
        // Sort by similarity score (descending)
        return similarityScores.entrySet().stream()
                              .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                              .collect(Collectors.toList());
    }
    
    /**
     * Analyze a document and report suspicious similarities
     */
    public void analyzeDocument(int documentId) {
        Document doc = documents.get(documentId);
        if (doc == null) {
            System.out.println("Document not found!");
            return;
        }
        
        System.out.printf("\n=== Plagiarism Analysis for Document %d: \"%s\" ===%n", 
                         documentId, doc.title);
        System.out.printf("Total n-grams: %d%n%n", doc.nGrams.size());
        
        List<Map.Entry<Integer, Double>> similarities = findSimilarDocuments(documentId);
        
        for (Map.Entry<Integer, Double> entry : similarities) {
            Document otherDoc = documents.get(entry.getKey());
            double similarity = entry.getValue();
            String suspicion;
            
            if (similarity >= 70) {
                suspicion = "PLAGIARISM DETECTED";
            } else if (similarity >= 50) {
                suspicion = "HIGHLY SUSPICIOUS";
            } else if (similarity >= 20) {
                suspicion = "SUSPICIOUS";
            } else {
                suspicion = "OK";
            }
            
            System.out.printf("  Found %d matching n-grams with \"%s\"%n", 
                            (int)(similarity * doc.nGrams.size() / 100), 
                            otherDoc.title);
            System.out.printf("  Similarity: %.2f%% [%s]%n%n", similarity, suspicion);
        }
    }
    
    /**
     * Get plagiarism report for a document
     */
    public String getPlagiarismReport(int documentId) {
        Document doc = documents.get(documentId);
        if (doc == null) return "Document not found";
        
        List<Map.Entry<Integer, Double>> similarities = findSimilarDocuments(documentId);
        
        if (similarities.isEmpty()) {
            return "No similarities found";
        }
        
        Map.Entry<Integer, Double> topMatch = similarities.get(0);
        double maxSimilarity = topMatch.getValue();
        Document suspectedDoc = documents.get(topMatch.getKey());
        
        if (maxSimilarity >= 70) {
            return String.format("PLAGIARISM DETECTED: %.2f%% match with \"%s\"", 
                                maxSimilarity, suspectedDoc.title);
        }
        
        return String.format("Maximum similarity: %.2f%% with \"%s\"", 
                            maxSimilarity, suspectedDoc.title);
    }
    
    // Main method for testing
    public static void main(String[] args) {
        Problem4_PlagiarismDetector detector = new Problem4_PlagiarismDetector();
        
        System.out.println("=== Problem 4: Plagiarism Detection System ===\n");
        
        // Add sample documents
        String doc1 = "Java is a programming language with strong object oriented programming " +
                     "features. Java is widely used for building scalable applications.";
        String doc2 = "Python is a programming language with strong dynamic typing features. " +
                     "Python is widely used for data science and machine learning applications.";
        String doc3 = "Java is a programming language with strong object oriented programming " +
                     "features. Java is widely used for building scalable applications. " +
                     "This is a copy of document 1 with some minor modifications.";
        String doc4 = "C++ is a compiled programming language that provides low level memory access.";
        
        detector.addDocument(1, "essay_001", doc1);
        detector.addDocument(2, "essay_002", doc2);
        detector.addDocument(3, "essay_003", doc3);
        detector.addDocument(4, "essay_004", doc4);
        
        System.out.println("\nTest Case 1: Analyze Document 1");
        detector.analyzeDocument(1);
        
        System.out.println("Test Case 2: Analyze Document 3 (should match Document 1)");
        detector.analyzeDocument(3);
        
        System.out.println("Test Case 3: Get Plagiarism Report");
        System.out.println("Document 1: " + detector.getPlagiarismReport(1));
        System.out.println("Document 3: " + detector.getPlagiarismReport(3));
        System.out.println("Document 4: " + detector.getPlagiarismReport(4));
    }
}
