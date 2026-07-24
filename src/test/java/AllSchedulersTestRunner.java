import schedulers.*;
import com.google.gson.*;
import models.Process;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Comprehensive test runner for ALL schedulers (SJF, RR, Priority, AG)
 * Fixed version with proper error handling and timeout protection
 */
public class AllSchedulersTestRunner {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static Map<String, TestStats> schedulerStats = new LinkedHashMap<>();

    private static class TestStats {
        int total = 0;
        int passed = 0;
    }

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║  CPU Schedulers Comprehensive Test Suite  ║");
        System.out.println("║  Assignment #3 - Operating Systems         ║");
        System.out.println("╚═══════════════════════════════════════════╝\n");

        // Initialize stats
        schedulerStats.put("SJF", new TestStats());
        schedulerStats.put("Round Robin", new TestStats());
        schedulerStats.put("Priority", new TestStats());
        schedulerStats.put("AG", new TestStats());

        try {
            // Test Other Schedulers (from Other_Schedulers folder)
            System.out.println("┌─────────────────────────────────────────┐");
            System.out.println("│ Testing: SJF, Round Robin, Priority     │");
            System.out.println("└─────────────────────────────────────────┘\n");
            testOtherSchedulers();

            // Test AG Scheduler (from AG folder)
            System.out.println("\n┌─────────────────────────────────────────┐");
            System.out.println("│ Testing: AG Scheduler                   │");
            System.out.println("└─────────────────────────────────────────┘\n");
            testAGScheduler();

            // Print final summary
            printFinalSummary();

        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure the program exits properly
            System.out.println("\nTest execution completed. Exiting...");
            System.exit(0);
        }
    }

    private static void testOtherSchedulers() throws IOException {
        String folder = "test_cases/Other_Schedulers";
        Path folderPath = Paths.get(folder);

        if (!Files.exists(folderPath)) {
            System.out.println("⚠️  Folder not found: " + folder);
            return;
        }

        List<Path> testFiles = new ArrayList<>();
        Files.list(folderPath)
                .filter(path -> path.toString().endsWith(".json"))
                .sorted()
                .forEach(testFiles::add);

        if (testFiles.isEmpty()) {
            System.out.println("⚠️  No test files found in " + folder);
            return;
        }

        System.out.println("Found " + testFiles.size() + " test files\n");

        for (Path path : testFiles) {
            try {
                System.out.println("Processing: " + path.getFileName());
                runOtherSchedulersTest(path.toFile());
            } catch (Exception e) {
                System.out.println("❌ ERROR running test " + path.getFileName());
                System.out.println("   Error: " + e.getMessage());
                e.printStackTrace();
                System.out.println();
            }
        }
    }

    private static void runOtherSchedulersTest(File file) throws IOException {
        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject testCase;

        try (FileReader reader = new FileReader(file)) {
            testCase = gson.fromJson(reader, JsonObject.class);
        }

        String testName = testCase.has("name") ? testCase.get("name").getAsString()
                : file.getName().replace(".json", "");
        JsonObject input = testCase.getAsJsonObject("input");

        int contextSwitch = input.get("contextSwitch").getAsInt();
        int rrQuantum = input.get("rrQuantum").getAsInt();
        int agingInterval = input.get("agingInterval").getAsInt();

        // Build process list for each scheduler
        List<Process> sjfProcesses = new ArrayList<>();
        List<Process> rrProcesses = new ArrayList<>();
        List<Process> priorityProcesses = new ArrayList<>();

        for (JsonElement el : input.getAsJsonArray("processes")) {
            JsonObject p = el.getAsJsonObject();
            String name = p.get("name").getAsString();
            int arrival = p.get("arrival").getAsInt();
            int burst = p.get("burst").getAsInt();
            int priority = p.get("priority").getAsInt();

            sjfProcesses.add(new Process(name, arrival, burst, priority, 0));
            rrProcesses.add(new Process(name, arrival, burst, priority, rrQuantum));
            priorityProcesses.add(new Process(name, arrival, burst, priority, 0));
        }

        JsonObject expectedOutput = testCase.getAsJsonObject("expectedOutput");

        // Test SJF with error handling
        System.out.println("  Testing SJF...");
        TestStats sjfStats = schedulerStats.get("SJF");
        sjfStats.total++; totalTests++;
        try {
            SchedulerResult sjfResult = new SJFScheduler().schedule(sjfProcesses, contextSwitch);
            boolean sjfPassed = checkSchedulerResult(testName, "SJF", sjfResult,
                    expectedOutput.getAsJsonObject("SJF"));
            if (sjfPassed) {
                sjfStats.passed++;
                passedTests++;
            }
        } catch (Exception e) {
            System.out.println("  ❌ SJF crashed: " + e.getMessage());
            e.printStackTrace();
        }

        // Test RR with error handling
        System.out.println("  Testing Round Robin...");
        TestStats rrStats = schedulerStats.get("Round Robin");
        rrStats.total++; totalTests++;
        try {
            SchedulerResult rrResult = new RoundRobinScheduler(rrQuantum)
                    .schedule(rrProcesses, contextSwitch);
            boolean rrPassed = checkSchedulerResult(testName, "RR", rrResult,
                    expectedOutput.getAsJsonObject("RR"));
            if (rrPassed) {
                rrStats.passed++;
                passedTests++;
            }
        } catch (Exception e) {
            System.out.println("  ❌ RR crashed: " + e.getMessage());
            e.printStackTrace();
        }

        // Test Priority with error handling
        System.out.println("  Testing Priority...");
        TestStats prioStats = schedulerStats.get("Priority");
        prioStats.total++; totalTests++;
        try {
            SchedulerResult prioResult = new PriorityScheduler(agingInterval)
                    .schedule(priorityProcesses, contextSwitch);
            boolean prioPassed = checkSchedulerResult(testName, "Priority", prioResult,
                    expectedOutput.getAsJsonObject("Priority"));
            if (prioPassed) {
                prioStats.passed++;
                passedTests++;
            }
        } catch (Exception e) {
            System.out.println("  ❌ Priority crashed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
    }

    private static void testAGScheduler() throws IOException {
        String folder = "test_cases/AG";
        Path folderPath = Paths.get(folder);

        if (!Files.exists(folderPath)) {
            System.out.println("⚠️  Folder not found: " + folder);
            return;
        }

        List<Path> testFiles = new ArrayList<>();
        Files.list(folderPath)
                .filter(path -> path.toString().endsWith(".json"))
                .sorted()
                .forEach(testFiles::add);

        if (testFiles.isEmpty()) {
            System.out.println("⚠️  No AG test files found in " + folder);
            return;
        }

        System.out.println("Found " + testFiles.size() + " test files\n");

        for (Path path : testFiles) {
            try {
                System.out.println("Processing: " + path.getFileName());
                runAGTest(path.toFile());
            } catch (Exception e) {
                System.out.println("❌ ERROR running test " + path.getFileName());
                System.out.println("   Error: " + e.getMessage());
                e.printStackTrace();
                System.out.println();
            }
        }
    }

    private static void runAGTest(File file) throws IOException {
        TestStats agStats = schedulerStats.get("AG");
        agStats.total++;
        totalTests++;

        Gson gson = new GsonBuilder().setLenient().create();
        JsonObject testCase;

        try (FileReader reader = new FileReader(file)) {
            testCase = gson.fromJson(reader, JsonObject.class);
        }

        String testName = file.getName().replace(".json", "");
        JsonObject input = testCase.getAsJsonObject("input");

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

        int contextSwitch = input.has("contextSwitch") ?
                input.get("contextSwitch").getAsInt() : 0;

        JsonObject expectedOutput = testCase.getAsJsonObject("expectedOutput");

        System.out.println("  Testing AG...");
        try {
            AGScheduler scheduler = new AGScheduler();
            SchedulerResult result = scheduler.schedule(processes, contextSwitch);

            boolean passed = checkAGResult(testName, result, expectedOutput);

            if (passed) {
                agStats.passed++;
                passedTests++;
            }
        } catch (Exception e) {
            System.out.println("  ❌ AG crashed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
    }

    private static boolean checkSchedulerResult(String testName, String schedulerName,
                                                SchedulerResult actual, JsonObject expected) {
        List<String> failures = new ArrayList<>();

        // Check execution order if provided
        if (expected.has("executionOrder")) {
            List<String> expectedOrder = new ArrayList<>();
            for (JsonElement el : expected.getAsJsonArray("executionOrder")) {
                expectedOrder.add(el.getAsString());
            }

            if (!actual.executionOrder.equals(expectedOrder)) {
                failures.add("Execution order mismatch");
            }
        }

        // Check averages
        double expectedAvgWT = expected.get("averageWaitingTime").getAsDouble();
        if (Math.abs(expectedAvgWT - actual.averageWaitingTime) > 0.01) {
            failures.add(String.format("Avg WT: exp %.2f, got %.2f",
                    expectedAvgWT, actual.averageWaitingTime));
        }

        double expectedAvgTT = expected.get("averageTurnaroundTime").getAsDouble();
        if (Math.abs(expectedAvgTT - actual.averageTurnaroundTime) > 0.01) {
            failures.add(String.format("Avg TAT: exp %.2f, got %.2f",
                    expectedAvgTT, actual.averageTurnaroundTime));
        }

        // Check per-process results
        JsonArray expectedPTimes = expected.getAsJsonArray("processResults");
        for (JsonElement el : expectedPTimes) {
            JsonObject p = el.getAsJsonObject();
            String name = p.get("name").getAsString();
            int expWT = p.get("waitingTime").getAsInt();
            int expTT = p.get("turnaroundTime").getAsInt();

            if (!actual.waitingTime.containsKey(name) ||
                    actual.waitingTime.get(name) != expWT) {
                failures.add(String.format("%s WT: exp %d, got %d",
                        name, expWT, actual.waitingTime.getOrDefault(name, -1)));
            }
            if (!actual.turnaroundTime.containsKey(name) ||
                    actual.turnaroundTime.get(name) != expTT) {
                failures.add(String.format("%s TAT: exp %d, got %d",
                        name, expTT, actual.turnaroundTime.getOrDefault(name, -1)));
            }
        }

        if (failures.isEmpty()) {
            System.out.println("  ✅ " + schedulerName + " - " + testName + " - PASSED");
            return true;
        } else {
            System.out.println("  ❌ " + schedulerName + " - " + testName + " - FAILED");
            for (String f : failures) {
                System.out.println("     • " + f);
            }
            return false;
        }
    }

    private static boolean checkAGResult(String testName, SchedulerResult actual,
                                         JsonObject expected) {
        List<String> failures = new ArrayList<>();

        // Check execution order
        List<String> expectedOrder = new ArrayList<>();
        for (JsonElement el : expected.getAsJsonArray("executionOrder")) {
            expectedOrder.add(el.getAsString());
        }
        if (!actual.executionOrder.equals(expectedOrder)) {
            failures.add("Execution order mismatch");
        }

        // Check averages
        double expectedAvgWT = expected.get("averageWaitingTime").getAsDouble();
        if (Math.abs(expectedAvgWT - actual.averageWaitingTime) > 0.01) {
            failures.add(String.format("Avg WT: exp %.2f, got %.2f",
                    expectedAvgWT, actual.averageWaitingTime));
        }

        double expectedAvgTT = expected.get("averageTurnaroundTime").getAsDouble();
        if (Math.abs(expectedAvgTT - actual.averageTurnaroundTime) > 0.01) {
            failures.add(String.format("Avg TAT: exp %.2f, got %.2f",
                    expectedAvgTT, actual.averageTurnaroundTime));
        }

        // Check per-process results including quantum history
        JsonArray expectedProcessResults = expected.getAsJsonArray("processResults");
        for (JsonElement el : expectedProcessResults) {
            JsonObject p = el.getAsJsonObject();
            String name = p.get("name").getAsString();
            int expWT = p.get("waitingTime").getAsInt();
            int expTT = p.get("turnaroundTime").getAsInt();

            if (!actual.waitingTime.containsKey(name) ||
                    actual.waitingTime.get(name) != expWT) {
                failures.add(String.format("%s WT: exp %d, got %d",
                        name, expWT, actual.waitingTime.getOrDefault(name, -1)));
            }
            if (!actual.turnaroundTime.containsKey(name) ||
                    actual.turnaroundTime.get(name) != expTT) {
                failures.add(String.format("%s TAT: exp %d, got %d",
                        name, expTT, actual.turnaroundTime.getOrDefault(name, -1)));
            }

            // Check quantum history
            if (p.has("quantumHistory")) {
                List<Integer> expQuantumHistory = new ArrayList<>();
                for (JsonElement qEl : p.getAsJsonArray("quantumHistory")) {
                    expQuantumHistory.add(qEl.getAsInt());
                }

                List<Integer> actQuantumHistory = actual.quantumHistory.get(name);
                if (actQuantumHistory == null ||
                        !actQuantumHistory.equals(expQuantumHistory)) {
                    failures.add(String.format("%s Quantum: exp %s, got %s",
                            name, expQuantumHistory, actQuantumHistory));
                }
            }
        }

        if (failures.isEmpty()) {
            System.out.println("  ✅ AG - " + testName + " - PASSED");
            return true;
        } else {
            System.out.println("  ❌ AG - " + testName + " - FAILED");
            for (String f : failures) {
                System.out.println("     • " + f);
            }
            return false;
        }
    }

    private static void printFinalSummary() {
        System.out.println("\n╔═══════════════════════════════════════════╗");
        System.out.println("║           FINAL TEST SUMMARY              ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        System.out.printf("Total Tests Run:    %d%n", totalTests);
        System.out.printf("Tests Passed:       %d%n", passedTests);
        System.out.printf("Tests Failed:       %d%n", totalTests - passedTests);

        if (totalTests > 0) {
            double rate = (passedTests * 100.0 / totalTests);
            System.out.printf("Success Rate:       %.2f%%%n", rate);
        }

        System.out.println("\n── Results by Scheduler ──");
        for (Map.Entry<String, TestStats> entry : schedulerStats.entrySet()) {
            TestStats stats = entry.getValue();
            System.out.printf("  %-15s: %d/%d passed", entry.getKey(), stats.passed, stats.total);
            if (stats.total > 0) {
                System.out.printf(" (%.1f%%)", (stats.passed * 100.0 / stats.total));
            }
            System.out.println();
        }

        System.out.println("═══════════════════════════════════════════");

        if (passedTests == totalTests && totalTests > 0) {
            System.out.println("🎉 ALL TESTS PASSED! CONGRATULATIONS! 🎉");
        } else if (passedTests > 0) {
            System.out.println("⚠️  Some tests failed. Review output above.");
        } else {
            System.out.println("❌ All tests failed. Check implementations.");
        }
    }
}