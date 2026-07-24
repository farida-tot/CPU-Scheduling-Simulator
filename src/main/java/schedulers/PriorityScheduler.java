package schedulers;

import models.Process;
import java.util.*;

public class PriorityScheduler implements Scheduler {
    private int agingInterval = 5;

    public PriorityScheduler() {}

    public PriorityScheduler(int agingInterval) {
        this.agingInterval = agingInterval;
    }

    @Override
    public SchedulerResult schedule(List<Process> processes, int contextSwitch) {
        SchedulerResult result = new SchedulerResult();

        int n = processes.size();

        // Create working copies of priority and remaining burst
        int[] currentPriority = new int[n];
        int[] remainingBurst = new int[n];
        int[] completionTime = new int[n];
        Map<String, Integer> lastAgingTime = new HashMap<>();
        Map<String, Integer> indexMap = new HashMap<>();

        // Initialize process info
        for (int i = 0; i < n; i++) {
            Process p = processes.get(i);
            currentPriority[i] = p.getPriority();
            remainingBurst[i] = p.getBurstTime();
            completionTime[i] = -1; // Not completed yet
            lastAgingTime.put(p.getName(), p.getArrivalTime());
            indexMap.put(p.getName(), i);
        }

        int time = 0;
        int finished = 0;
        Integer currentIdx = null;
        String prevRunning = null;

        while (finished < n) {
            // Apply aging for all waiting processes
            for (int i = 0; i < n; i++) {
                Process p = processes.get(i);
                if (remainingBurst[i] > 0 && p.getArrivalTime() <= time) {
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
                Process p = processes.get(i);
                if (p.getArrivalTime() <= time && remainingBurst[i] > 0) {
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
                Process pa = processes.get(a);
                Process pb = processes.get(b);
                if (pa.getArrivalTime() != pb.getArrivalTime()) {
                    return Integer.compare(pa.getArrivalTime(), pb.getArrivalTime());
                }
                return pa.getName().compareTo(pb.getName());
            });

            int nextIdx = readyIndices.get(0);
            Process next = processes.get(nextIdx);

            // Handle context switch if switching processes
            if (currentIdx != null && currentIdx != nextIdx) {
                // Record the process we intend to switch to
                if (prevRunning == null || !prevRunning.equals(next.getName())) {
                    result.executionOrder.add(next.getName());
                    prevRunning = next.getName();
                }

                // Perform context switch with aging
                for (int i = 0; i < contextSwitch; i++) {
                    time++;
                    // Aging during context switch
                    for (int j = 0; j < n; j++) {
                        Process p = processes.get(j);
                        if (remainingBurst[j] > 0 && p.getArrivalTime() <= time
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
                    Process p = processes.get(i);
                    if (p.getArrivalTime() <= time && remainingBurst[i] > 0) {
                        readyIndices.add(i);
                    }
                }

                if (!readyIndices.isEmpty()) {
                    readyIndices.sort((a, b) -> {
                        if (currentPriority[a] != currentPriority[b]) {
                            return Integer.compare(currentPriority[a], currentPriority[b]);
                        }
                        Process pa = processes.get(a);
                        Process pb = processes.get(b);
                        if (pa.getArrivalTime() != pb.getArrivalTime()) {
                            return Integer.compare(pa.getArrivalTime(), pb.getArrivalTime());
                        }
                        return pa.getName().compareTo(pb.getName());
                    });

                    int reevaluatedIdx = readyIndices.get(0);
                    Process reevaluated = processes.get(reevaluatedIdx);

                    // If priority changed during CS, need another context switch
                    if (reevaluatedIdx != nextIdx) {
                        if (!prevRunning.equals(reevaluated.getName())) {
                            result.executionOrder.add(reevaluated.getName());
                            prevRunning = reevaluated.getName();
                        }

                        for (int i = 0; i < contextSwitch; i++) {
                            time++;
                            // Aging during second context switch
                            for (int j = 0; j < n; j++) {
                                Process p = processes.get(j);
                                if (remainingBurst[j] > 0 && p.getArrivalTime() <= time
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
                    result.executionOrder.add(next.getName());
                    prevRunning = next.getName();
                }
            }

            currentIdx = nextIdx;
            remainingBurst[currentIdx]--;
            lastAgingTime.put(next.getName(), time + 1);
            time++;

            if (remainingBurst[currentIdx] == 0) {
                completionTime[currentIdx] = time;
                finished++;
                prevRunning = null;
            }
        }

        // Calculate metrics using completion times
        calculateMetrics(processes, completionTime, indexMap, result);
        return result;
    }

    private void calculateMetrics(List<Process> processes, int[] completionTime,
                                  Map<String, Integer> indexMap, SchedulerResult result) {
        double totalWT = 0, totalTAT = 0;

        for (Process p : processes) {
            int idx = indexMap.get(p.getName());
            int arrivalTime = p.getArrivalTime();
            int burstTime = p.getBurstTime();
            int compTime = completionTime[idx];

            int tat = compTime - arrivalTime;
            int wt = tat - burstTime;

            result.turnaroundTime.put(p.getName(), tat);
            result.waitingTime.put(p.getName(), wt);

            totalWT += wt;
            totalTAT += tat;
        }

        if (!processes.isEmpty()) {
            result.averageWaitingTime = totalWT / processes.size();
            result.averageTurnaroundTime = totalTAT / processes.size();
        }
    }
}