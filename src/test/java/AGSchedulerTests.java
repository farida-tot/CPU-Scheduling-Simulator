//package test.java;

import schedulers.*;
import com.google.gson.*;
import models.Process;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AGSchedulerTests {

    private static final String TESTS_FOLDER = "test_cases/AG";
    private static int passedTests = 0;
    private static int totalTests = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   AG Scheduler Unit Tests");
        System.out.println("========================================\n");

        try {
            // Get all JSON test files
            List<Path> testFiles = new ArrayList<>();
            Path folderPath = Paths.get(TESTS_FOLDER);

            if (!Files.exists(folderPath)) {
                System.out.println("❌ Test folder not found: " + TESTS_FOLDER);
                System.out.println("Please ensure test files are in: " + new File(TESTS_FOLDER).getAbsolutePath());
                return;
            }

            Files.list(folderPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .forEach(testFiles::add);

            if (testFiles.isEmpty()) {
                System.out.println("❌ No test files found in " + TESTS_FOLDER);
                return;
            }

            System.out.println("Found " + testFiles.size() + " test files\n");

            // Run each test
            for (Path path : testFiles) {
                try {
                    runTest(path.toFile());
                } catch (Exception e) {
                    System.out.println("❌ ERROR running test " + path.getFileName());
                    e.printStackTrace();
                    System.out.println();
                }
            }

            // Print summary
            System.out.println("========================================");
            System.out.println("   Test Summary");
            System.out.println("========================================");
            System.out.printf("Total Tests: %d%n", totalTests);
            System.out.printf("Passed: %d%n", passedTests);
            System.out.printf("Failed: %d%n", totalTests - passedTests);

            if (totalTests > 0) {
                System.out.printf("Success Rate: %.2f%%%n",
                        (passedTests * 100.0 / totalTests));

                if (passedTests == totalTests) {
                    System.out.println("\n🎉 ALL TESTS PASSED! 🎉");
                }
            }
            System.out.println("========================================");

        } catch (IOException e) {
            System.err.println("Error accessing test files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runTest(File file) throws IOException {
        totalTests++;

        Gson gson = new Gson();
        JsonObject testCase = gson.fromJson(new FileReader(file), JsonObject.class);

        String testName = file.getName().replace(".json", "");
        JsonObject input = testCase.getAsJsonObject("input");

        // Build process list
        List<Process> processes = new ArrayList<>();
        for (JsonElement el : input.getAsJsonArray("processes")) {
            JsonObject p = el.getAsJsonObject();
            processes.add(new Process(
                    p.get("name").getAsString(),
                    p.get("arrival").getAsInt(),
                    p.get("burst").getAsInt(),
                    p.get("priority").getAsInt(),
                    p.get("quantum").getAsInt()
            ));
        }

        // Context switch (default to 0 if not specified)
        int contextSwitch = input.has("contextSwitch") ?
                input.get("contextSwitch").getAsInt() : 0;

        JsonObject expectedOutput = testCase.getAsJsonObject("expectedOutput");

        // Run AG Scheduler
        AGScheduler scheduler = new AGScheduler();
        SchedulerResult result = scheduler.schedule(processes, contextSwitch);

        // Check results
        boolean passed = checkResult(testName, result, expectedOutput);

        if (passed) {
            passedTests++;
        }

        System.out.println();
    }

    private static boolean checkResult(String testName, SchedulerResult actual,
                                       JsonObject expected) {
        List<String> failures = new ArrayList<>();

        // 1. Check execution order
        List<String> expectedOrder = new ArrayList<>();
        for (JsonElement el : expected.getAsJsonArray("executionOrder")) {
            expectedOrder.add(el.getAsString());
        }

        if (!actual.executionOrder.equals(expectedOrder)) {
            failures.add("Execution Order mismatch");
        }

        // 2. Check average waiting time
        double expectedAvgWT = expected.get("averageWaitingTime").getAsDouble();
        if (Math.abs(expectedAvgWT - actual.averageWaitingTime) > 0.01) {
            failures.add(String.format("Average Waiting Time: expected %.2f, got %.2f",
                    expectedAvgWT, actual.averageWaitingTime));
        }

        // 3. Check average turnaround time
        double expectedAvgTT = expected.get("averageTurnaroundTime").getAsDouble();
        if (Math.abs(expectedAvgTT - actual.averageTurnaroundTime) > 0.01) {
            failures.add(String.format("Average Turnaround Time: expected %.2f, got %.2f",
                    expectedAvgTT, actual.averageTurnaroundTime));
        }

        // 4. Check per-process results
        JsonArray expectedProcessResults = expected.getAsJsonArray("processResults");
        for (JsonElement el : expectedProcessResults) {
            JsonObject p = el.getAsJsonObject();
            String name = p.get("name").getAsString();
            int expWT = p.get("waitingTime").getAsInt();
            int expTT = p.get("turnaroundTime").getAsInt();

            // Check waiting time
            if (!actual.waitingTime.containsKey(name)) {
                failures.add("Process " + name + " not found in results");
            } else if (actual.waitingTime.get(name) != expWT) {
                failures.add(String.format("Process %s Waiting Time: expected %d, got %d",
                        name, expWT, actual.waitingTime.get(name)));
            }

            // Check turnaround time
            if (!actual.turnaroundTime.containsKey(name)) {
                failures.add("Process " + name + " not found in results");
            } else if (actual.turnaroundTime.get(name) != expTT) {
                failures.add(String.format("Process %s Turnaround Time: expected %d, got %d",
                        name, expTT, actual.turnaroundTime.get(name)));
            }

            // Check quantum history
            if (p.has("quantumHistory")) {
                List<Integer> expQuantumHistory = new ArrayList<>();
                for (JsonElement qEl : p.getAsJsonArray("quantumHistory")) {
                    expQuantumHistory.add(qEl.getAsInt());
                }

                if (!actual.quantumHistory.containsKey(name)) {
                    failures.add("Process " + name + " quantum history not found");
                } else {
                    List<Integer> actQuantumHistory = actual.quantumHistory.get(name);
                    if (!actQuantumHistory.equals(expQuantumHistory)) {
                        failures.add(String.format(
                                "Process %s Quantum History: expected %s, got %s",
                                name, expQuantumHistory, actQuantumHistory));
                    }
                }
            }
        }

        // Print results
        if (failures.isEmpty()) {
            System.out.println("✅ Test '" + testName + "' PASSED");
            printTestDetails(actual);
            return true;
        } else {
            System.out.println("❌ Test '" + testName + "' FAILED");
            System.out.println("  Failures:");
            for (String failure : failures) {
                System.out.println("    - " + failure);
            }
            System.out.println("\n  Expected execution order: " + expectedOrder);
            System.out.println("  Actual execution order:   " + actual.executionOrder);
            printTestDetails(actual);
            return false;
        }
    }

    private static void printTestDetails(SchedulerResult result) {
        System.out.println("  Results:");
        System.out.printf("    Avg Waiting Time: %.2f%n", result.averageWaitingTime);
        System.out.printf("    Avg Turnaround Time: %.2f%n", result.averageTurnaroundTime);

        System.out.println("  Process Details:");
        for (String process : result.waitingTime.keySet()) {
            System.out.printf("    %s: WT=%d, TAT=%d, Quantum=%s%n",
                    process,
                    result.waitingTime.get(process),
                    result.turnaroundTime.get(process),
                    result.quantumHistory.getOrDefault(process, new ArrayList<>()));
        }
    }

    // Helper method for manual testing
    public static void runSingleTest(List<Process> processes, int contextSwitch,
                                     String testName) {
        System.out.println("Running manual test: " + testName);
        System.out.println("========================================");

        AGScheduler scheduler = new AGScheduler();
        SchedulerResult result = scheduler.schedule(processes, contextSwitch);

        System.out.println("Execution Order: " + result.executionOrder);
        System.out.printf("Average Waiting Time: %.2f%n", result.averageWaitingTime);
        System.out.printf("Average Turnaround Time: %.2f%n", result.averageTurnaroundTime);
        System.out.println("\nProcess Details:");

        for (String process : result.waitingTime.keySet()) {
            System.out.printf("  %s:%n", process);
            System.out.printf("    Waiting Time: %d%n", result.waitingTime.get(process));
            System.out.printf("    Turnaround Time: %d%n", result.turnaroundTime.get(process));
            System.out.printf("    Quantum History: %s%n",
                    result.quantumHistory.get(process));
        }

        System.out.println("========================================\n");
    }
}