package schedulers;

import models.Process;
import java.util.*;

public class RoundRobinScheduler implements Scheduler {
    private int quantum;

    public RoundRobinScheduler(int quantum) {
        this.quantum = quantum;
    }

    @Override
    public SchedulerResult schedule(List<Process> processes, int contextSwitch) {
        SchedulerResult result = new SchedulerResult();

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

        Queue<Process> readyQueue = new LinkedList<>();
        int currentTime = 0;
        String lastProcess = null;

        // Track if we need to add context switch
        boolean addContextSwitch = false;

        while (true) {
            // Add arriving processes to ready queue
            for (Process p : processList) {
                if (p.getArrivalTime() == currentTime &&
                        remainingTime.get(p.getName()) > 0 &&
                        !readyQueue.contains(p)) {
                    readyQueue.add(p);
                }
            }

            // Check if all processes are finished
            boolean allFinished = true;
            for (Process p : processList) {
                if (remainingTime.get(p.getName()) > 0) {
                    allFinished = false;
                    break;
                }
            }
            if (allFinished) break;

            // If ready queue is empty, advance time
            if (readyQueue.isEmpty()) {
                currentTime++;
                continue;
            }

            // Get next process from ready queue
            Process current = readyQueue.poll();

            // Add context switch time if switching between different processes
            if (lastProcess != null && !lastProcess.equals(current.getName()) && addContextSwitch) {
                currentTime += contextSwitch;
                // After context switch, check for new arrivals again
                for (Process p : processList) {
                    if (p.getArrivalTime() <= currentTime &&
                            remainingTime.get(p.getName()) > 0 &&
                            !readyQueue.contains(p) && p != current) {
                        readyQueue.add(p);
                    }
                }
            }

            // Record execution start
            if (!result.executionOrder.contains(current.getName()) ||
                    (result.executionOrder.size() > 0 &&
                            !result.executionOrder.get(result.executionOrder.size()-1).equals(current.getName()))) {
                result.executionOrder.add(current.getName());
            }

            // Determine how long to execute (min of quantum or remaining time)
            int timeToExecute = Math.min(quantum, remainingTime.get(current.getName()));
            int executedThisTurn = 0;

            // Execute 1 time unit at a time
            for (int i = 0; i < timeToExecute; i++) {
                // Execute for 1 time unit
                remainingTime.put(current.getName(),
                        remainingTime.get(current.getName()) - 1);
                executedThisTurn++;
                currentTime++;

                // Check for new arrivals at EACH time unit
                for (Process p : processList) {
                    if (p.getArrivalTime() == currentTime &&
                            remainingTime.get(p.getName()) > 0 &&
                            !readyQueue.contains(p) && p != current) {
                        readyQueue.add(p);
                    }
                }

                // Check if process finished
                if (remainingTime.get(current.getName()) == 0) {
                    finishTime.put(current.getName(), currentTime);
                    break;
                }
            }

            // If process still has remaining time, add it back to ready queue
            if (remainingTime.get(current.getName()) > 0) {
                readyQueue.add(current);
            }

            lastProcess = current.getName();
            addContextSwitch = true; // Enable context switching for next switch
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

        if (!processes.isEmpty()) {
            result.averageWaitingTime = totalWT / processes.size();
            result.averageTurnaroundTime = totalTAT / processes.size();
        }
    }
}