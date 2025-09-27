package org.acme;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.acme.dto.TransactionRequestDTO;

public class TransactionLoadTest {
    private static final Logger LOGGER = Logger.getLogger(TransactionLoadTest.class.getName());
    private static final String BASE_URL = "http://localhost:8080/api/transaction";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(100);
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    
    private final HttpClient httpClient;
    private final ExecutorService executorService;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger mismatchCount = new AtomicInteger(0);
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private final List<Long> responseTimes = new ArrayList<>();
    private volatile boolean isShuttingDown = false;

    public TransactionLoadTest(int threadPoolSize) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT)
                .executor(Executors.newFixedThreadPool(threadPoolSize))
                .build();
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    private TransactionRequestDTO createRandomTransaction() {
        TransactionRequestDTO transaction = new TransactionRequestDTO();
        transaction.setTrxId(UUID.randomUUID().toString());
        transaction.setInit(true);
        
        Map<String, String> fields = new HashMap<>();
        fields.put("amount", String.valueOf(Math.random() * 1000));
        fields.put("currency", "USD");
        fields.put("timestamp", String.valueOf(System.currentTimeMillis()));
        transaction.setFields(fields);
        
        return transaction;
    }

    private CompletableFuture<Void> sendTransactionWithRetry() {
        return CompletableFuture.runAsync(() -> {
            int retries = 0;
            while (retries < MAX_RETRIES && !isShuttingDown) {
                try {
                    TransactionRequestDTO transaction = createRandomTransaction();
                    String requestBody = objectMapper.writeValueAsString(transaction);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL))
                            .header("Content-Type", "application/json")
                            .timeout(REQUEST_TIMEOUT)
                            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                            .build();

                    long startTime = System.currentTimeMillis();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    long endTime = System.currentTimeMillis();

                    synchronized (responseTimes) {
                        responseTimes.add(endTime - startTime);
                    }

                    if (response.statusCode() == 200) {
                        Map<String, String> responseFields = objectMapper.readValue(
                            response.body(), 
                            new TypeReference<Map<String, String>>() {}
                        );
                        
                        if (responseFields.equals(transaction.getFields())) {
                            successCount.incrementAndGet();
                            return;
                        } else {
                            mismatchCount.incrementAndGet();
                            LOGGER.warning("Field mismatch for transaction: " + transaction.getTrxId());
                            LOGGER.warning("Expected: " + transaction.getFields());
                            LOGGER.warning("Received: " + responseFields);
                        }
                    } else if (response.statusCode() == 503) {
                        LOGGER.warning("Service unavailable (503) for transaction: " + transaction.getTrxId());
                        retries++;
                        retryCount.incrementAndGet();
                        Thread.sleep(RETRY_DELAY.toMillis() * (retries + 1));
                        continue;
                    } else {
                        LOGGER.severe("Failed request: " + response.statusCode() + " - " + response.body());
                        failureCount.incrementAndGet();
                        return;
                    }
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    LOGGER.log(Level.SEVERE, "Error sending request: " + e.getMessage(), e);
                    retries++;
                    retryCount.incrementAndGet();
                    try {
                        Thread.sleep(RETRY_DELAY.toMillis() * (retries + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            if (!isShuttingDown) {
                failureCount.incrementAndGet();
            }
        }, executorService);
    }

    public void runLoadTest(int numRequests, int concurrentRequests) {
        System.out.println("Starting load test with " + numRequests + " total requests and " + concurrentRequests + " concurrent requests");
        Instant startTime = Instant.now();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < numRequests && !isShuttingDown; i++) {
            futures.add(sendTransactionWithRetry());
            if (futures.size() >= concurrentRequests) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                futures.clear();
            }
        }
        
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);

        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);

        System.out.println("\nLoad Test Results:");
        System.out.println("Total Duration: " + duration.getSeconds() + " seconds");
        System.out.println("Total Requests: " + numRequests);
        System.out.println("Successful Requests: " + successCount.get());
        System.out.println("Failed Requests: " + failureCount.get());
        System.out.println("Mismatched Responses: " + mismatchCount.get());
        System.out.println("Retry Attempts: " + retryCount.get());
        System.out.println("Average Response Time: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("Requests per Second: " + String.format("%.2f", (double) numRequests / duration.getSeconds()));

        shutdown();
    }

    private void shutdown() {
        isShuttingDown = true;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        // Default values
        int numRequests = 1000;
        int concurrentRequests = 10;

        // Parse command line arguments if provided
        if (args.length >= 1) {
            numRequests = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            concurrentRequests = Integer.parseInt(args[1]);
        }

        TransactionLoadTest loadTest = new TransactionLoadTest(concurrentRequests);
        loadTest.runLoadTest(numRequests, concurrentRequests);
    }
} 