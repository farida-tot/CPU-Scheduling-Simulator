package schedulers;

import java.util.*;

public class SchedulerResult {
    public List<String> executionOrder = new ArrayList<>();

    public Map<String, Integer> waitingTime = new LinkedHashMap<>();
    public Map<String, Integer> turnaroundTime = new LinkedHashMap<>();

    public double averageWaitingTime;
    public double averageTurnaroundTime;


    public Map<String, List<Integer>> quantumHistory = new LinkedHashMap<>();
}
