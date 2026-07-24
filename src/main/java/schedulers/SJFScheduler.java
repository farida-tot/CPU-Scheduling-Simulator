package schedulers;

import models.Process;
import java.util.*;

public class SJFScheduler implements Scheduler {

    @Override
    public SchedulerResult schedule(List<Process> processes, int contextSwitch) {
        SchedulerResult result = new SchedulerResult();

        if (processes == null || processes.isEmpty()) {
            return result;
        }

        // Create working copies
        List<Process> processList = new ArrayList<>();
        Map<String, Integer> remainingTime = new HashMap<>();
        Map<String, Integer> finishTime = new HashMap<>();
        Map<String, Integer> startTime = new HashMap<>();

        for (Process p : processes) {
            Process copy = new Process(p);
            processList.add(copy);
            remainingTime.put(p.getName(), p.getBurstTime());
        }

        // Sort by arrival time
        processList.sort(Comparator.comparingInt(Process::getArrivalTime));

        int currentTime = 0;
        String lastProcess = null;

        while (finishTime.size() < processes.size()) {
            // Get available processes
            List<Process> available = new ArrayList<>();
            for (Process p : processList) {
                if (p.getArrivalTime() <= currentTime &&
                        remainingTime.get(p.getName()) > 0 &&
                        !finishTime.containsKey(p.getName())) {
                    available.add(p);
                }
            }

            if (available.isEmpty()) {
                currentTime++;
                continue;
            }

            // Find process with shortest remaining time
            Process current = null;
            int shortestRemaining = Integer.MAX_VALUE;

            for (Process p : available) {
                int remaining = remainingTime.get(p.getName());
                if (remaining < shortestRemaining) {
                    current = p;
                    shortestRemaining = remaining;
                } else if (remaining == shortestRemaining && current != null) {
                    // Tie-breaker: earlier arrival time
                    if (p.getArrivalTime() < current.getArrivalTime()) {
                        current = p;
                    }
                }
            }

            // Handle context switch if switching processes
            if (lastProcess != null && !lastProcess.equals(current.getName())) {
                currentTime += contextSwitch;
            }

            // Add to execution order when process starts (not every time unit)
            if (lastProcess == null || !lastProcess.equals(current.getName())) {
                result.executionOrder.add(current.getName());
            }

            // Execute for 1 time unit (preemptive)
            remainingTime.put(current.getName(), remainingTime.get(current.getName()) - 1);
            currentTime++;

            // Check if finished
            if (remainingTime.get(current.getName()) == 0) {
                finishTime.put(current.getName(), currentTime);
            }

            lastProcess = current.getName();
        }

        // Calculate metrics
        calculateMetrics(processes, finishTime, result);
        return result;
    }

    private void calculateMetrics(List<Process> processes,
                                  Map<String, Integer> finishTime,
                                  SchedulerResult result) {
        double totalWT = 0, totalTAT = 0;

        for (Process p : processes) {
            int tat = finishTime.get(p.getName()) - p.getArrivalTime();
            int wt = tat - p.getBurstTime();

            result.turnaroundTime.put(p.getName(), tat);
            result.waitingTime.put(p.getName(), wt);

            totalWT += wt;
            totalTAT += tat;
        }

        result.averageWaitingTime = totalWT / processes.size();
        result.averageTurnaroundTime = totalTAT / processes.size();
    }
}