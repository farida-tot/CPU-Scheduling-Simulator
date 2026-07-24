/*import java.util.ArrayList;
import java.util.List;
import java.util.*;

enum State{
    FCFS,
    PRIORITY,
    SJF
}

class AGProcess {
    String name;
    private int arrival;
    private int burst;
    private int remainingBurst;
    private int priority;
    private int quantum;
    private List<Integer> qHistory = new ArrayList<>();

    private int waitingTime;
    private int turnAround;
    private int completionTime;

    // Constructor
    public AGProcess(String name, int arrival, int burst, int priority, int quantum) {
        this.name = name;
        this.arrival = arrival;
        this.burst = burst;
        this.remainingBurst = burst;
        this.priority = priority;
        this.quantum = quantum;
    }
    // Getters
    public String getName() { return name; }
    public int getArrival() { return arrival; }
    public int getBurst() { return burst; }
    public int getRemainingBurst() { return remainingBurst; }
    public int getPriority() { return priority; }
    public int getQuantum() { return quantum; }
    public List<Integer> getQHistory() { return qHistory; }
    public int getWaitingTime() { return waitingTime; }
    public int getTurnAround() { return turnAround; }
    public int getCompletionTime() { return completionTime; }
    public void setTimeOut(int t) { setCompletionTime(t); }
    public int getTimeOut() { return getCompletionTime(); }
    public int getOrigBurst() { return getBurst(); }


    // setters
    public void setRemainingBurst(int r) { remainingBurst = r; }
    public void setQuantum(int q) { quantum = q; }
    public void addToQHistory(int q) { qHistory.add(q); }
    public void setWaitingTime(int w) { waitingTime = w; }
    public void setTurnAround(int t) { turnAround = t; }
    public void setCompletionTime(int c) { completionTime = c; }
}

class SJFc {
    public static void SJFSchedule(List<AGProcess> processes, int cs) {
        int n = processes.size();
        int currentTime = 0;
        int completed = 0;
        AGProcess lastProcess = null;
        List<String> executionOrder = new ArrayList<>();

        int[] remainingBurst = new int[n];
        for (int i = 0; i < n; i++) {
            remainingBurst[i] = processes.get(i).getBurst();
        }

        while (completed < n) {
            AGProcess shortest = null;
            int minRemaining = Integer.MAX_VALUE;
            int shortestIdx = -1;

            for (int i = 0; i < n; i++) {
                AGProcess p = processes.get(i);
                if (p.getArrival() <= currentTime && remainingBurst[i] > 0) {
                    if (remainingBurst[i] < minRemaining) {
                        minRemaining = remainingBurst[i];
                        shortest = p;
                        shortestIdx = i;
                    }
                }
            }

            if (shortest == null) {
                currentTime++;
                continue;
            }
            if (lastProcess != null && lastProcess != shortest) {
                currentTime += cs;
            }
            if (lastProcess != shortest) {
                executionOrder.add(shortest.name);
            }
            remainingBurst[shortestIdx]--;
            currentTime++;
            lastProcess = shortest;
            if (remainingBurst[shortestIdx] == 0) {
                completed++;
                shortest.setTimeOut(currentTime);
                shortest.setTurnAround(shortest.getTimeOut() - shortest.getArrival());
                shortest.setWaitingTime(shortest.getTurnAround() - shortest.getOrigBurst());
            }
        }
    }
}

class RoundRobinScheduler {
    public static void schedule(List<AGProcess> processes, int timeQuantum, int contextSwitchTime) {
        for (AGProcess p : processes) p.setRemainingBurst(p.getBurst());
        Queue<AGProcess> readyQueue = new LinkedList<>();
        int currentTime = 0, completed = 0, n = processes.size();
        System.out.println("\n--- Round Robin Execution ---");
        while (completed < n) {
            for (AGProcess p : processes) {
                if (p.getArrival() <= currentTime && p.getRemainingBurst() > 0 && !readyQueue.contains(p)) {
                    readyQueue.add(p);
                }
            }
            if (readyQueue.isEmpty()) {
                System.out.println("Time " + currentTime + ": Idle");
                currentTime++;
                continue;
            }
            AGProcess cur = readyQueue.poll();
            System.out.println("Time " + currentTime + ": " + cur.getName() + " starts executing.");
            int exec = Math.min(timeQuantum, cur.getRemainingBurst());
            cur.setRemainingBurst(cur.getRemainingBurst() - exec);
            currentTime += exec;
            for (AGProcess p : processes) {
                if (p.getArrival() <= currentTime && p.getRemainingBurst() > 0 && !readyQueue.contains(p) && p != cur) {
                    readyQueue.add(p);
                }
            }
            if (cur.getRemainingBurst() > 0) {
                readyQueue.add(cur);
            } else {
                completed++;
                cur.setCompletionTime(currentTime);
                cur.setTurnAround(cur.getCompletionTime() - cur.getArrival());
                cur.setWaitingTime(cur.getTurnAround() - cur.getBurst());
                System.out.println("Time " + currentTime + ": " + cur.getName() + " finished.");
            }
            if (!readyQueue.isEmpty() && contextSwitchTime > 0) {
                System.out.println("Time " + currentTime + ": Context Switch (" + contextSwitchTime + " units)");
                currentTime += contextSwitchTime;
            }
        }
    }
}

class PriorityScheduler {

    public static void schedule(List<AGProcess> processes, int contextSwitch, int agingInterval) {
        int n = processes.size();

        // Create working copies of priority and remaining burst
        int[] currentPriority = new int[n];
        int[] remainingBurst = new int[n];
        Map<String, Integer> lastAgingTime = new HashMap<>();
        Map<String, Integer> indexMap = new HashMap<>();

        for (int i = 0; i < n; i++) {
            AGProcess p = processes.get(i);
            currentPriority[i] = p.getPriority();
            remainingBurst[i] = p.getBurst();
            lastAgingTime.put(p.getName(), p.getArrival());
            indexMap.put(p.getName(), i);
        }

        int time = 0;
        int finished = 0;
        Integer currentIdx = null;
        String prevRunning = null;
        List<String> executionOrder = new ArrayList<>();

        while (finished < n) {
            // Apply aging for all waiting processes
            for (int i = 0; i < n; i++) {
                AGProcess p = processes.get(i);
                if (remainingBurst[i] > 0 && p.getArrival() <= time) {
                    int lastAge = lastAgingTime.get(p.getName());
                    while (agingInterval > 0 && (time - lastAge) >= agingInterval) {
                        if (currentPriority[i] > 1) {
                            currentPriority[i]--;
                        }
                        lastAge += agingInterval;
                        lastAgingTime.put(p.getName(), lastAge);
                    }
                }
            }

            // Build ready list - find indices of ready processes
            List<Integer> readyIndices = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                AGProcess p = processes.get(i);
                if (p.getArrival() <= time && remainingBurst[i] > 0) {
                    readyIndices.add(i);
                }
            }

            if (readyIndices.isEmpty()) {
                time++;
                continue;
            }

            // Sort by priority (lowest = highest priority), then arrival, then name
            readyIndices.sort((a, b) -> {
                if (currentPriority[a] != currentPriority[b]) {
                    return Integer.compare(currentPriority[a], currentPriority[b]);
                }
                AGProcess pa = processes.get(a);
                AGProcess pb = processes.get(b);
                if (pa.getArrival() != pb.getArrival()) {
                    return Integer.compare(pa.getArrival(), pb.getArrival());
                }
                return pa.getName().compareTo(pb.getName());
            });

            int nextIdx = readyIndices.get(0);
            AGProcess next = processes.get(nextIdx);

            // Handle context switch if switching processes
            if (currentIdx != null && currentIdx != nextIdx) {
                // Record the process we intend to switch to
                if (prevRunning == null || !prevRunning.equals(next.getName())) {
                    executionOrder.add(next.getName());
                    prevRunning = next.getName();
                }

                // Perform context switch with aging
                for (int i = 0; i < contextSwitch; i++) {
                    time++;
                    // Aging during context switch
                    for (int j = 0; j < n; j++) {
                        AGProcess p = processes.get(j);
                        if (remainingBurst[j] > 0 && p.getArrival() <= time
                                && !p.getName().equals(next.getName())) {
                            int lastAge = lastAgingTime.get(p.getName());
                            while (agingInterval > 0 && (time - lastAge) >= agingInterval) {
                                if (currentPriority[j] > 1) {
                                    currentPriority[j]--;
                                }
                                lastAge += agingInterval;
                                lastAgingTime.put(p.getName(), lastAge);
                            }
                        }
                    }
                }

                // Re-evaluate after context switch
                readyIndices.clear();
                for (int i = 0; i < n; i++) {
                    AGProcess p = processes.get(i);
                    if (p.getArrival() <= time && remainingBurst[i] > 0) {
                        readyIndices.add(i);
                    }
                }

                if (!readyIndices.isEmpty()) {
                    readyIndices.sort((a, b) -> {
                        if (currentPriority[a] != currentPriority[b]) {
                            return Integer.compare(currentPriority[a], currentPriority[b]);
                        }
                        AGProcess pa = processes.get(a);
                        AGProcess pb = processes.get(b);
                        if (pa.getArrival() != pb.getArrival()) {
                            return Integer.compare(pa.getArrival(), pb.getArrival());
                        }
                        return pa.getName().compareTo(pb.getName());
                    });

                    int reevaluatedIdx = readyIndices.get(0);
                    AGProcess reevaluated = processes.get(reevaluatedIdx);

                    // If priority changed during CS, need another context switch
                    if (reevaluatedIdx != nextIdx) {
                        if (!prevRunning.equals(reevaluated.getName())) {
                            executionOrder.add(reevaluated.getName());
                            prevRunning = reevaluated.getName();
                        }

                        for (int i = 0; i < contextSwitch; i++) {
                            time++;
                            // Aging during second context switch
                            for (int j = 0; j < n; j++) {
                                AGProcess p = processes.get(j);
                                if (remainingBurst[j] > 0 && p.getArrival() <= time
                                        && !p.getName().equals(reevaluated.getName())) {
                                    int lastAge = lastAgingTime.get(p.getName());
                                    while (agingInterval > 0 && (time - lastAge) >= agingInterval) {
                                        if (currentPriority[j] > 1) {
                                            currentPriority[j]--;
                                        }
                                        lastAge += agingInterval;
                                        lastAgingTime.put(p.getName(), lastAge);
                                    }
                                }
                            }
                        }
                    }
                    nextIdx = reevaluatedIdx;
                    next = reevaluated;
                }
            } else {
                // Not switching - record if needed
                if (prevRunning == null || !prevRunning.equals(next.getName())) {
                    executionOrder.add(next.getName());
                    prevRunning = next.getName();
                }
            }

            currentIdx = nextIdx;
            remainingBurst[currentIdx]--;
            lastAgingTime.put(next.getName(), time + 1);
            time++;

            if (remainingBurst[currentIdx] == 0) {
                next.setCompletionTime(time);
                next.setTurnAround(time - next.getArrival());
                next.setWaitingTime(next.getTurnAround() - next.getBurst());
                finished++;
                prevRunning = null;
            }
        }

        // Print execution order for debugging
        System.out.println("Execution Order: " + executionOrder);
    }
}


class AG_Schedule {

    public void terminateProcess(AGProcess p, int time, List<AGProcess> waiting, List<AGProcess> finished){
        p.setTimeOut(time);
        p.setQuantum(0);
        p.addToQHistory(p.getQuantum());
        p.setTurnAround(p.getTimeOut() - p.getArrival());
        p.setWaitingTime(p.getTurnAround() - p.getOrigBurst());
        waiting.remove(p);
        finished.add(p);
    }

    public AGProcess getHighestPriority(List<AGProcess> waiting){
        AGProcess highest = waiting.get(0);
        for(AGProcess p : waiting){
            if (p.getPriority() < highest.getPriority()) {
                highest = p;
            }
        }
        return highest;
    }

    public AGProcess getShortestJob(List<AGProcess> waiting){
        AGProcess shortest = waiting.get(0);
        for (AGProcess p : waiting){
            if (p.getBurst() < shortest.getBurst()) {
                shortest = p;
            }
        }
        return shortest;
    }


    public void agScheduler(){
        List<AGProcess> pending = new ArrayList<>();
        List<AGProcess> waiting = new ArrayList<>();
        List<AGProcess> finished = new ArrayList<>();

        int AGTime = 0;
        State state = State.FCFS;
        int working_time = 0;
        AGProcess curr_process = null;

        while (!waiting.isEmpty() || !pending.isEmpty()) {
            //AG logic
        }
    }
}

class ComprehensiveTest1 {
    public static void main(String[] args) {
        int cs = 1;
        int rrQuantum = 2;
        int agingInterval = 5;

        // Create processes for each scheduler
        List<AGProcess> sjfProcesses = new ArrayList<>();
        sjfProcesses.add(new AGProcess("P1", 0, 8, 3, 0));
        sjfProcesses.add(new AGProcess("P2", 1, 4, 1, 0));
        sjfProcesses.add(new AGProcess("P3", 2, 2, 4, 0));
        sjfProcesses.add(new AGProcess("P4", 3, 1, 2, 0));
        sjfProcesses.add(new AGProcess("P5", 4, 3, 5, 0));

        List<AGProcess> rrProcesses = new ArrayList<>();
        rrProcesses.add(new AGProcess("P1", 0, 8, 3, rrQuantum));
        rrProcesses.add(new AGProcess("P2", 1, 4, 1, rrQuantum));
        rrProcesses.add(new AGProcess("P3", 2, 2, 4, rrQuantum));
        rrProcesses.add(new AGProcess("P4", 3, 1, 2, rrQuantum));
        rrProcesses.add(new AGProcess("P5", 4, 3, 5, rrQuantum));

        List<AGProcess> priorityProcesses = new ArrayList<>();
        priorityProcesses.add(new AGProcess("P1", 0, 8, 3, 0));
        priorityProcesses.add(new AGProcess("P2", 1, 4, 1, 0));
        priorityProcesses.add(new AGProcess("P3", 2, 2, 4, 0));
        priorityProcesses.add(new AGProcess("P4", 3, 1, 2, 0));
        priorityProcesses.add(new AGProcess("P5", 4, 3, 5, 0));

        // Run SJF
        System.out.println("===== TEST CASE 1: BASIC MIXED ARRIVALS =====");
        System.out.println("\n--- SJF Scheduler ---");
        SJFc.SJFSchedule(sjfProcesses, cs);
        printResults("SJF", sjfProcesses, 7.4, 11.0);

        // Run RR
        System.out.println("\n--- Round Robin Scheduler (quantum=" + rrQuantum + ") ---");
        RoundRobinScheduler.schedule(rrProcesses, rrQuantum, cs);
        printResults("RR", rrProcesses, 12.6, 16.2);

        // Run Priority
        System.out.println("\n--- Priority Scheduler ---");
        PriorityScheduler.schedule(priorityProcesses, cs, agingInterval);
        printResults("Priority", priorityProcesses, 9.8, 13.4);
    }

    private static void printResults(String name, List<AGProcess> processes,
                                     double expectedWT, double expectedTAT) {
        double totalW = 0, totalT = 0;
        System.out.println("Process Results:");
        for (AGProcess p : processes) {
            System.out.printf("  %s: WT=%d, TAT=%d%n", p.getName(), p.getWaitingTime(), p.getTurnAround());
            totalW += p.getWaitingTime();
            totalT += p.getTurnAround();
        }
        double avgW = totalW / processes.size();
        double avgT = totalT / processes.size();
        System.out.printf("Avg WT=%.1f (expected %.1f) | Avg TAT=%.1f (expected %.1f)%n",
                avgW, expectedWT, avgT, expectedTAT);
    }
}

class ComprehensiveTest2 {
    public static void main(String[] args) {
        int cs = 1;
        int rrQuantum = 3;
        int agingInterval = 5;

        // Create processes for each scheduler
        List<AGProcess> sjfProcesses = new ArrayList<>();
        sjfProcesses.add(new AGProcess("P1", 0, 6, 3, 0));
        sjfProcesses.add(new AGProcess("P2", 0, 3, 1, 0));
        sjfProcesses.add(new AGProcess("P3", 0, 8, 2, 0));
        sjfProcesses.add(new AGProcess("P4", 0, 4, 4, 0));
        sjfProcesses.add(new AGProcess("P5", 0, 2, 5, 0));

        List<AGProcess> rrProcesses = new ArrayList<>();
        rrProcesses.add(new AGProcess("P1", 0, 6, 3, rrQuantum));
        rrProcesses.add(new AGProcess("P2", 0, 3, 1, rrQuantum));
        rrProcesses.add(new AGProcess("P3", 0, 8, 2, rrQuantum));
        rrProcesses.add(new AGProcess("P4", 0, 4, 4, rrQuantum));
        rrProcesses.add(new AGProcess("P5", 0, 2, 5, rrQuantum));

        List<AGProcess> priorityProcesses = new ArrayList<>();
        priorityProcesses.add(new AGProcess("P1", 0, 6, 3, 0));
        priorityProcesses.add(new AGProcess("P2", 0, 3, 1, 0));
        priorityProcesses.add(new AGProcess("P3", 0, 8, 2, 0));
        priorityProcesses.add(new AGProcess("P4", 0, 4, 4, 0));
        priorityProcesses.add(new AGProcess("P5", 0, 2, 5, 0));

        // Run SJF
        System.out.println("===== TEST CASE 2: ALL PROCESSES ARRIVE AT TIME 0 =====");
        System.out.println("\n--- SJF Scheduler ---");
        SJFc.SJFSchedule(sjfProcesses, cs);
        printResults("SJF", sjfProcesses, 8.2, 12.8);

        // Run RR
        System.out.println("\n--- Round Robin Scheduler (quantum=" + rrQuantum + ") ---");
        RoundRobinScheduler.schedule(rrProcesses, rrQuantum, cs);
        printResults("RR", rrProcesses, 16.6, 21.2);

        // Run Priority
        System.out.println("\n--- Priority Scheduler ---");
        PriorityScheduler.schedule(priorityProcesses, cs, agingInterval);
        printResults("Priority", priorityProcesses, 15.4, 20.0);
    }

    private static void printResults(String name, List<AGProcess> processes,
                                     double expectedWT, double expectedTAT) {
        double totalW = 0, totalT = 0;
        System.out.println("Process Results:");
        for (AGProcess p : processes) {
            System.out.printf("  %s: WT=%d, TAT=%d%n", p.getName(), p.getWaitingTime(), p.getTurnAround());
            totalW += p.getWaitingTime();
            totalT += p.getTurnAround();
        }
        double avgW = totalW / processes.size();
        double avgT = totalT / processes.size();
        System.out.printf("Avg WT=%.1f (expected %.1f) | Avg TAT=%.1f (expected %.1f)%n",
                avgW, expectedWT, avgT, expectedTAT);
    }
}

class ComprehensiveTest3 {
    public static void main(String[] args) {
        int cs = 1;
        int rrQuantum = 4;
        int agingInterval = 4;

        List<AGProcess> sjfProcesses = new ArrayList<>();
        sjfProcesses.add(new AGProcess("P1", 0, 10, 5, 0));
        sjfProcesses.add(new AGProcess("P2", 2, 5, 1, 0));
        sjfProcesses.add(new AGProcess("P3", 5, 3, 2, 0));
        sjfProcesses.add(new AGProcess("P4", 8, 7, 1, 0));
        sjfProcesses.add(new AGProcess("P5", 10, 2, 3, 0));

        List<AGProcess> rrProcesses = new ArrayList<>();
        rrProcesses.add(new AGProcess("P1", 0, 10, 5, rrQuantum));
        rrProcesses.add(new AGProcess("P2", 2, 5, 1, rrQuantum));
        rrProcesses.add(new AGProcess("P3", 5, 3, 2, rrQuantum));
        rrProcesses.add(new AGProcess("P4", 8, 7, 1, rrQuantum));
        rrProcesses.add(new AGProcess("P5", 10, 2, 3, rrQuantum));

        List<AGProcess> priorityProcesses = new ArrayList<>();
        priorityProcesses.add(new AGProcess("P1", 0, 10, 5, 0));
        priorityProcesses.add(new AGProcess("P2", 2, 5, 1, 0));
        priorityProcesses.add(new AGProcess("P3", 5, 3, 2, 0));
        priorityProcesses.add(new AGProcess("P4", 8, 7, 1, 0));
        priorityProcesses.add(new AGProcess("P5", 10, 2, 3, 0));

        System.out.println("===== TEST CASE 3: VARIED BURST TIMES WITH STARVATION RISK =====");
        System.out.println("\n--- SJF Scheduler ---");
        SJFc.SJFSchedule(sjfProcesses, cs);
        printResults("SJF", sjfProcesses, 7.6, 13.0);

        System.out.println("\n--- Round Robin Scheduler (quantum=" + rrQuantum + ") ---");
        RoundRobinScheduler.schedule(rrProcesses, rrQuantum, cs);
        printResults("RR", rrProcesses, 17.0, 22.4);

        System.out.println("\n--- Priority Scheduler ---");
        PriorityScheduler.schedule(priorityProcesses, cs, agingInterval);
        printResults("Priority", priorityProcesses, 12.2, 17.6);
    }

    private static void printResults(String name, List<AGProcess> processes,
                                     double expectedWT, double expectedTAT) {
        double totalW = 0, totalT = 0;
        System.out.println("Process Results:");
        for (AGProcess p : processes) {
            System.out.printf("  %s: WT=%d, TAT=%d%n", p.getName(), p.getWaitingTime(), p.getTurnAround());
            totalW += p.getWaitingTime();
            totalT += p.getTurnAround();
        }
        double avgW = totalW / processes.size();
        double avgT = totalT / processes.size();
        System.out.printf("Avg WT=%.1f (expected %.1f) | Avg TAT=%.1f (expected %.1f)%n",
                avgW, expectedWT, avgT, expectedTAT);
    }
}

class ComprehensiveTest4 {
    public static void main(String[] args) {
        int cs = 2;
        int rrQuantum = 5;
        int agingInterval = 6;

        List<AGProcess> sjfProcesses = new ArrayList<>();
        sjfProcesses.add(new AGProcess("P1", 0, 12, 2, 0));
        sjfProcesses.add(new AGProcess("P2", 4, 9, 3, 0));
        sjfProcesses.add(new AGProcess("P3", 8, 15, 1, 0));
        sjfProcesses.add(new AGProcess("P4", 12, 6, 4, 0));
        sjfProcesses.add(new AGProcess("P5", 16, 11, 2, 0));
        sjfProcesses.add(new AGProcess("P6", 20, 5, 5, 0));

        List<AGProcess> rrProcesses = new ArrayList<>();
        rrProcesses.add(new AGProcess("P1", 0, 12, 2, rrQuantum));
        rrProcesses.add(new AGProcess("P2", 4, 9, 3, rrQuantum));
        rrProcesses.add(new AGProcess("P3", 8, 15, 1, rrQuantum));
        rrProcesses.add(new AGProcess("P4", 12, 6, 4, rrQuantum));
        rrProcesses.add(new AGProcess("P5", 16, 11, 2, rrQuantum));
        rrProcesses.add(new AGProcess("P6", 20, 5, 5, rrQuantum));

        List<AGProcess> priorityProcesses = new ArrayList<>();
        priorityProcesses.add(new AGProcess("P1", 0, 12, 2, 0));
        priorityProcesses.add(new AGProcess("P2", 4, 9, 3, 0));
        priorityProcesses.add(new AGProcess("P3", 8, 15, 1, 0));
        priorityProcesses.add(new AGProcess("P4", 12, 6, 4, 0));
        priorityProcesses.add(new AGProcess("P5", 16, 11, 2, 0));
        priorityProcesses.add(new AGProcess("P6", 20, 5, 5, 0));

        System.out.println("===== TEST CASE 4: LARGE BURSTS WITH GAPS IN ARRIVALS =====");
        System.out.println("\n--- SJF Scheduler ---");
        SJFc.SJFSchedule(sjfProcesses, cs);
        printResults("SJF", sjfProcesses, 16.33, 26.0);

        System.out.println("\n--- Round Robin Scheduler (quantum=" + rrQuantum + ") ---");
        RoundRobinScheduler.schedule(rrProcesses, rrQuantum, cs);
        printResults("RR", rrProcesses, 43.33, 53.0);

        System.out.println("\n--- Priority Scheduler ---");
        PriorityScheduler.schedule(priorityProcesses, cs, agingInterval);
        printResults("Priority", priorityProcesses, 27.67, 37.33);
    }

    private static void printResults(String name, List<AGProcess> processes,
                                     double expectedWT, double expectedTAT) {
        double totalW = 0, totalT = 0;
        System.out.println("Process Results:");
        for (AGProcess p : processes) {
            System.out.printf("  %s: WT=%d, TAT=%d%n", p.getName(), p.getWaitingTime(), p.getTurnAround());
            totalW += p.getWaitingTime();
            totalT += p.getTurnAround();
        }
        double avgW = totalW / processes.size();
        double avgT = totalT / processes.size();
        System.out.printf("Avg WT=%.2f (expected %.2f) | Avg TAT=%.1f (expected %.2f)%n",
                avgW, expectedWT, avgT, expectedTAT);
    }
}

class ComprehensiveTest5 {
    public static void main(String[] args) {
        int cs = 1;
        int rrQuantum = 2;
        int agingInterval = 3;

        List<AGProcess> sjfProcesses = new ArrayList<>();
        sjfProcesses.add(new AGProcess("P1", 0, 3, 3, 0));
        sjfProcesses.add(new AGProcess("P2", 1, 2, 1, 0));
        sjfProcesses.add(new AGProcess("P3", 2, 4, 2, 0));
        sjfProcesses.add(new AGProcess("P4", 3, 1, 4, 0));
        sjfProcesses.add(new AGProcess("P5", 4, 3, 5, 0));

        List<AGProcess> rrProcesses = new ArrayList<>();
        rrProcesses.add(new AGProcess("P1", 0, 3, 3, rrQuantum));
        rrProcesses.add(new AGProcess("P2", 1, 2, 1, rrQuantum));
        rrProcesses.add(new AGProcess("P3", 2, 4, 2, rrQuantum));
        rrProcesses.add(new AGProcess("P4", 3, 1, 4, rrQuantum));
        rrProcesses.add(new AGProcess("P5", 4, 3, 5, rrQuantum));

        List<AGProcess> priorityProcesses = new ArrayList<>();
        priorityProcesses.add(new AGProcess("P1", 0, 3, 3, 0));
        priorityProcesses.add(new AGProcess("P2", 1, 2, 1, 0));
        priorityProcesses.add(new AGProcess("P3", 2, 4, 2, 0));
        priorityProcesses.add(new AGProcess("P4", 3, 1, 4, 0));
        priorityProcesses.add(new AGProcess("P5", 4, 3, 5, 0));

        System.out.println("===== TEST CASE 5: SHORT BURSTS WITH HIGH FREQUENCY =====");
        System.out.println("\n--- SJF Scheduler ---");
        SJFc.SJFSchedule(sjfProcesses, cs);
        printResults("SJF", sjfProcesses, 4.4, 7.0);

        System.out.println("\n--- Round Robin Scheduler (quantum=" + rrQuantum + ") ---");
        RoundRobinScheduler.schedule(rrProcesses, rrQuantum, cs);
        printResults("RR", rrProcesses, 8.4, 11.0);

        System.out.println("\n--- Priority Scheduler ---");
        PriorityScheduler.schedule(priorityProcesses, cs, agingInterval);
        printResults("Priority", priorityProcesses, 8.2, 10.8);
    }

    private static void printResults(String name, List<AGProcess> processes,
                                     double expectedWT, double expectedTAT) {
        double totalW = 0, totalT = 0;
        System.out.println("Process Results:");
        for (AGProcess p : processes) {
            System.out.printf("  %s: WT=%d, TAT=%d%n", p.getName(), p.getWaitingTime(), p.getTurnAround());
            totalW += p.getWaitingTime();
            totalT += p.getTurnAround();
        }
        double avgW = totalW / processes.size();
        double avgT = totalT / processes.size();
        System.out.printf("Avg WT=%.1f (expected %.1f) | Avg TAT=%.1f (expected %.1f)%n",
                avgW, expectedWT, avgT, expectedTAT);
    }
}

class ComprehensiveTest6 {
    public static void main(String[] args) {
        int cs = 1;
        int rrQuantum = 4;
        int agingInterval = 5;

        List<AGProcess> sjfProcesses = new ArrayList<>();
        sjfProcesses.add(new AGProcess("P1", 0, 14, 4, 0));
        sjfProcesses.add(new AGProcess("P2", 3, 7, 2, 0));
        sjfProcesses.add(new AGProcess("P3", 6, 10, 5, 0));
        sjfProcesses.add(new AGProcess("P4", 9, 5, 1, 0));
        sjfProcesses.add(new AGProcess("P5", 12, 8, 3, 0));
        sjfProcesses.add(new AGProcess("P6", 15, 4, 6, 0));

        List<AGProcess> rrProcesses = new ArrayList<>();
        rrProcesses.add(new AGProcess("P1", 0, 14, 4, rrQuantum));
        rrProcesses.add(new AGProcess("P2", 3, 7, 2, rrQuantum));
        rrProcesses.add(new AGProcess("P3", 6, 10, 5, rrQuantum));
        rrProcesses.add(new AGProcess("P4", 9, 5, 1, rrQuantum));
        rrProcesses.add(new AGProcess("P5", 12, 8, 3, rrQuantum));
        rrProcesses.add(new AGProcess("P6", 15, 4, 6, rrQuantum));

        List<AGProcess> priorityProcesses = new ArrayList<>();
        priorityProcesses.add(new AGProcess("P1", 0, 14, 4, 0));
        priorityProcesses.add(new AGProcess("P2", 3, 7, 2, 0));
        priorityProcesses.add(new AGProcess("P3", 6, 10, 5, 0));
        priorityProcesses.add(new AGProcess("P4", 9, 5, 1, 0));
        priorityProcesses.add(new AGProcess("P5", 12, 8, 3, 0));
        priorityProcesses.add(new AGProcess("P6", 15, 4, 6, 0));

        System.out.println("===== TEST CASE 6: MIXED SCENARIO - COMPREHENSIVE TEST =====");
        System.out.println("\n--- SJF Scheduler ---");
        SJFc.SJFSchedule(sjfProcesses, cs);
        printResults("SJF", sjfProcesses, 14.0, 22.0);

        System.out.println("\n--- Round Robin Scheduler (quantum=" + rrQuantum + ") ---");
        RoundRobinScheduler.schedule(rrProcesses, rrQuantum, cs);
        printResults("RR", rrProcesses, 33.67, 41.67);

        System.out.println("\n--- Priority Scheduler ---");
        PriorityScheduler.schedule(priorityProcesses, cs, agingInterval);
        printResults("Priority", priorityProcesses, 24.5, 32.5);
    }

    private static void printResults(String name, List<AGProcess> processes,
                                     double expectedWT, double expectedTAT) {
        double totalW = 0, totalT = 0;
        System.out.println("Process Results:");
        for (AGProcess p : processes) {
            System.out.printf("  %s: WT=%d, TAT=%d%n", p.getName(), p.getWaitingTime(), p.getTurnAround());
            totalW += p.getWaitingTime();
            totalT += p.getTurnAround();
        }
        double avgW = totalW / processes.size();
        double avgT = totalT / processes.size();
        System.out.printf("Avg WT=%.2f (expected %.2f) | Avg TAT=%.2f (expected %.2f)%n",
                avgW, expectedWT, avgT, expectedTAT);
    }
}
*/