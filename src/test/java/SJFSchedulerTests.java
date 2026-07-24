
import schedulers.*;
import com.google.gson.*;
import models.Process;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SJFSchedulerTests {

    private static final String TESTS_FOLDER = "test_cases/Other_Schedulers";
    private static int passedTests = 0;
    private static int totalTests = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   SJF Scheduler Unit Tests");
        System.out.println("========================================\n");

        try {
            Path folderPath = Paths.get(TESTS_FOLDER);

            if (!Files.exists(folderPath)) {
                System.out.println("⚠️  Test folder not found: " + TESTS_FOLDER);
                return;
            }

            List<Path> testFiles = new ArrayList<>();
            Files.list(folderPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted()
                    .forEach(testFiles::add);

            if (testFiles.isEmpty()) {
                System.out.println("⚠️  No test files found in " + TESTS_FOLDER);
                return;
            }

            System.out.println("Found " + testFiles.size() + " test files\n");

            for (Path path : testFiles) {
                try {
                    runTest(path.toFile());
                } catch (Exception e) {
                    System.out.println("❌ ERROR running test " + path.getFileName());
                    e.printStackTrace();
                    System.out.println();
                }
            }

            printSummary();

        } catch (IOException e) {
            System.err.println("Error accessing test files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runTest(File file) throws IOException {
        totalTests++;

        Gson gson = new Gson();
        JsonObject testCase = gson.fromJson(new FileReader(file), JsonObject.class);

        String testName = testCase.has("name") ? testCase.get("name").getAsString()
                : file.getName().replace(".json", "");
        JsonObject input = testCase.getAsJsonObject("input");

        // Build process list
        List<Process> processes = new ArrayList<>();
        for (JsonElement el : input.getAsJsonArray("processes")) {
            JsonObject p = el.getAsJsonObject();
            processes.add(new Process(
                    p.get("name").getAsString(),
                    p.get("arrival").getAsInt(),
                    p.get("burst").getAsInt(),
                    p.has("priority") ? p.get("priority").getAsInt() : 0,
                    0
            ));
        }

        int contextSwitch = input.has("contextSwitch") ?
                input.get("contextSwitch").getAsInt() : 0;

        // Get SJF expected output from nested structure
        JsonObject expectedOutput = testCase.getAsJsonObject("expectedOutput");
        JsonObject sjfExpected = expectedOutput.getAsJsonObject("SJF");

        // Run SJF Scheduler
        SJFScheduler scheduler = new SJFScheduler();
        SchedulerResult result = scheduler.schedule(processes, contextSwitch);

        // Check results
        boolean passed = checkResult(testName, result, sjfExpected);

        if (passed) {
            passedTests++;
        }

        System.out.println();
    }

    private static boolean checkResult(String testName, SchedulerResult actual,
                                       JsonObject expected) {
        List<String> failures = new ArrayList<>();

        // Check execution order if provided
        if (expected.has("executionOrder")) {
            List<String> expectedOrder = new ArrayList<>();
            for (JsonElement el : expected.getAsJsonArray("executionOrder")) {
                expectedOrder.add(el.getAsString());
            }

            if (!actual.executionOrder.equals(expectedOrder)) {
                failures.add("Execution Order mismatch");
                failures.add("  Expected: " + expectedOrder);
                failures.add("  Actual:   " + actual.executionOrder);
            }
        }

        // Check average waiting time
        double expectedAvgWT = expected.get("averageWaitingTime").getAsDouble();
        if (Math.abs(expectedAvgWT - actual.averageWaitingTime) > 0.01) {
            failures.add(String.format("Average Waiting Time: expected %.2f, got %.2f",
                    expectedAvgWT, actual.averageWaitingTime));
        }

        // Check average turnaround time
        double expectedAvgTT = expected.get("averageTurnaroundTime").getAsDouble();
        if (Math.abs(expectedAvgTT - actual.averageTurnaroundTime) > 0.01) {
            failures.add(String.format("Average Turnaround Time: expected %.2f, got %.2f",
                    expectedAvgTT, actual.averageTurnaroundTime));
        }

        // Check per-process results
        JsonArray expectedProcessResults = expected.getAsJsonArray("processResults");
        for (JsonElement el : expectedProcessResults) {
            JsonObject p = el.getAsJsonObject();
            String name = p.get("name").getAsString();
            int expWT = p.get("waitingTime").getAsInt();
            int expTT = p.get("turnaroundTime").getAsInt();

            if (!actual.waitingTime.containsKey(name)) {
                failures.add("Process " + name + " not found in results");
            } else if (actual.waitingTime.get(name) != expWT) {
                failures.add(String.format("Process %s Waiting Time: expected %d, got %d",
                        name, expWT, actual.waitingTime.get(name)));
            }

            if (!actual.turnaroundTime.containsKey(name)) {
                failures.add("Process " + name + " not found in results");
            } else if (actual.turnaroundTime.get(name) != expTT) {
                failures.add(String.format("Process %s Turnaround Time: expected %d, got %d",
                        name, expTT, actual.turnaroundTime.get(name)));
            }
        }

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
            printTestDetails(actual);
            return false;
        }
    }

    private static void printTestDetails(SchedulerResult result) {
        System.out.println("  Execution Order: " + result.executionOrder);
        System.out.printf("  Avg WT: %.2f | Avg TAT: %.2f%n",
                result.averageWaitingTime, result.averageTurnaroundTime);

        System.out.println("  Process Details:");
        for (String process : result.waitingTime.keySet()) {
            System.out.printf("    %s: WT=%d, TAT=%d%n",
                    process,
                    result.waitingTime.get(process),
                    result.turnaroundTime.get(process));
        }
    }

    private static void printSummary() {
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
    }
}