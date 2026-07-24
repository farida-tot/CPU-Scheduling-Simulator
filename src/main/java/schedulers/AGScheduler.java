package schedulers;

import models.Process;
import java.util.*;

public class AGScheduler implements Scheduler {

    private static class AGProcess {
        String name;
        int arrivalTime;
        int burstTime;
        int remainingTime;
        int priority;
        int quantum;
        int finishTime;
        List<Integer> quantumHistory;
        boolean hasArrived;

        AGProcess(Process p) {
            this.name = p.getName();
            this.arrivalTime = p.getArrivalTime();
            this.burstTime = p.getBurstTime();
            this.remainingTime = p.getBurstTime();
            this.priority = p.getPriority();
            this.quantum = p.getQuantum();
            this.finishTime = -1;
            this.quantumHistory = new ArrayList<>();
            this.quantumHistory.add(p.getQuantum());
            this.hasArrived = false;
        }

        void consumeCPU() {
            remainingTime--;
        }

        boolean isCompleted() {
            return remainingTime == 0;
        }
    }

    @Override
    public SchedulerResult schedule(List<Process> processes, int contextSwitch) {
        SchedulerResult result = new SchedulerResult();
        List<AGProcess> processList = new ArrayList<>();
        LinkedList<AGProcess> readyQueue = new LinkedList<>();

        // Create process copies
        for(Process p : processes) {
            AGProcess agp = new AGProcess(p);
            processList.add(agp);
        }

        // Sort by arrival time
        processList.sort(Comparator.comparingInt(p -> p.arrivalTime));

        int currentTime = 0;
        AGProcess currentProcess = null;

        while(!allCompleted(processList)) {
            // Add arriving processes to ready queue
            addArrivals(processList, readyQueue, currentTime);

            // If no ready process and no current process, advance time
            if(readyQueue.isEmpty() && currentProcess == null) {
                currentTime++;
                continue;
            }

            // Select new process if none is currently running
            if (currentProcess == null) {
                if(readyQueue.isEmpty()) {
                    currentTime++;
                    continue;
                }
                currentProcess = readyQueue.removeFirst(); // FCFS - take from front
                result.executionOrder.add(currentProcess.name);
            }

            int Q = currentProcess.quantum;
            int fcfsTime = (int) Math.ceil(Q * 0.25);
            int priorityTime = (int) Math.ceil(Q * 0.25);
            int sjfTime = Q - fcfsTime - priorityTime;

            // =================== FCFS Phase (25%) ===================
            for (int i = 0; i < fcfsTime && currentProcess.remainingTime > 0; i++) {
                currentProcess.consumeCPU();
                currentTime++;
                addArrivals(processList, readyQueue, currentTime);
            }

            if (currentProcess.isCompleted()) {
                finishProcess(currentProcess, currentTime);
                currentProcess = null;
                continue;
            }

            // =================== Priority Phase (25%) ===================
            // Check for higher priority preemption ONLY at the start of priority phase
            int higherPriorityIdx = getHighestPriority(currentProcess, readyQueue);

            if (higherPriorityIdx != -1) {
                // Preempted by higher priority process before priority phase starts
                int usedTime = fcfsTime;
                int remainingQuantum = Q - usedTime;
                int quantumIncrease = (remainingQuantum + 1) / 2; // ceil(remainingQuantum/2)
                currentProcess.quantum = Q + quantumIncrease;
                currentProcess.quantumHistory.add(currentProcess.quantum);
                readyQueue.addLast(currentProcess);

                currentProcess = readyQueue.remove(higherPriorityIdx);
                result.executionOrder.add(currentProcess.name);
                continue;
            }

            // Execute priority phase fully
            for (int i = 0; i < priorityTime && currentProcess.remainingTime > 0; i++) {
                currentProcess.consumeCPU();
                currentTime++;
                addArrivals(processList, readyQueue, currentTime);
            }

            if (currentProcess.isCompleted()) {
                finishProcess(currentProcess, currentTime);
                currentProcess = null;
                continue;
            }

            // =================== SJF Phase (50%) ===================
            // Check for shorter job preemption ONLY at the start of SJF phase
            int shorterJobIdx = getShortestJob(currentProcess, readyQueue);

            if (shorterJobIdx != -1) {
                // Preempted by shorter job before SJF phase starts
                int remainingSJF = sjfTime;
                currentProcess.quantum = Q + remainingSJF;
                currentProcess.quantumHistory.add(currentProcess.quantum);
                readyQueue.addLast(currentProcess);

                currentProcess = readyQueue.remove(shorterJobIdx);
                result.executionOrder.add(currentProcess.name);
                continue;
            }

            // Execute SJF phase fully
            for (int i = 0; i < sjfTime && currentProcess.remainingTime > 0; i++) {
                currentProcess.consumeCPU();
                currentTime++;
                addArrivals(processList, readyQueue, currentTime);
            }

            if (currentProcess.isCompleted()) {
                finishProcess(currentProcess, currentTime);
                currentProcess = null;
                continue;
            }

            // =================== Quantum Exhausted ===================
            // Process used all quantum without completing
            currentProcess.quantum = Q + 2;
            currentProcess.quantumHistory.add(currentProcess.quantum);
            readyQueue.addLast(currentProcess);
            currentProcess = null;
        }

        // Calculate final statistics
        calculateMetrics(processes, processList, result);
        return result;
    }

    private boolean allCompleted(List<AGProcess> processes) {
        for (AGProcess p : processes) {
            if (!p.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    private void addArrivals(List<AGProcess> all, LinkedList<AGProcess> ready, int time) {
        for (AGProcess p : all) {
            if (p.arrivalTime == time && !p.isCompleted() && !p.hasArrived) {
                if (!ready.contains(p)) {
                    ready.addLast(p);
                    p.hasArrived = true;
                }
            }
        }
    }

    private int getHighestPriority(AGProcess current, LinkedList<AGProcess> ready) {
        int minPriority = current.priority;
        int minIdx = -1;

        // Find process with highest priority (lowest number)
        // If multiple have same priority, take the first (FCFS)
        for (int i = 0; i < ready.size(); i++) {
            if (ready.get(i).priority < minPriority) {
                minPriority = ready.get(i).priority;
                minIdx = i;
            }
        }
        return minIdx;
    }

    private int getShortestJob(AGProcess current, LinkedList<AGProcess> ready) {
        int minRemaining = current.remainingTime;
        int minIdx = -1;

        // Find process with shortest remaining time
        // If multiple have same time, take the first (FCFS)
        for (int i = 0; i < ready.size(); i++) {
            if (ready.get(i).remainingTime < minRemaining) {
                minRemaining = ready.get(i).remainingTime;
                minIdx = i;
            }
        }
        return minIdx;
    }

    private void finishProcess(AGProcess p, int time) {
        p.finishTime = time;
        p.quantum = 0;
        p.quantumHistory.add(0);
    }

    private void calculateMetrics(List<Process> originalProcesses,
                                  List<AGProcess> processList,
                                  SchedulerResult result) {
        double totalWT = 0, totalTAT = 0;

        // Create a map for quick lookup
        Map<String, AGProcess> processMap = new HashMap<>();
        for (AGProcess p : processList) {
            processMap.put(p.name, p);
        }

        for (Process p : originalProcesses) {
            AGProcess agp = processMap.get(p.getName());

            // Ensure finish time is set
            if (agp.finishTime == -1) {
                // Find max finish time
                int maxTime = 0;
                for (AGProcess proc : processList) {
                    if (proc.finishTime > maxTime) {
                        maxTime = proc.finishTime;
                    }
                }
                agp.finishTime = maxTime;
            }

            int tat = agp.finishTime - agp.arrivalTime;
            int wt = tat - agp.burstTime;

            result.turnaroundTime.put(p.getName(), tat);
            result.waitingTime.put(p.getName(), wt);

            // Clean up quantum history (remove consecutive duplicates)
            List<Integer> cleanHistory = new ArrayList<>();
            Integer lastValue = null;
            for (Integer value : agp.quantumHistory) {
                if (lastValue == null || !value.equals(lastValue)) {
                    cleanHistory.add(value);
                    lastValue = value;
                }
            }
            result.quantumHistory.put(p.getName(), cleanHistory);

            totalWT += wt;
            totalTAT += tat;
        }

        if (!originalProcesses.isEmpty()) {
            result.averageWaitingTime = totalWT / originalProcesses.size();
            result.averageTurnaroundTime = totalTAT / originalProcesses.size();
        }
    }
}