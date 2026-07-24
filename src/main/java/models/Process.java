package models;

public class Process {
    private String name;
    private int arrivalTime;
    private int burstTime;
    private int remainingTime;
    private int priority;
    private int quantum;

    public Process(String name, int arrivalTime, int burstTime,
                   int priority, int quantum) {
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = priority;
        this.quantum = quantum;
    }


    public Process(Process p) {
        this.name = p.name;
        this.arrivalTime = p.arrivalTime;
        this.burstTime = p.burstTime;
        this.remainingTime = p.remainingTime;
        this.priority = p.priority;
        this.quantum = p.quantum;
    }

    // getters & setters
    public String getName() { return name; }
    public int getArrivalTime() { return arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public int getRemainingTime() { return remainingTime; }
    public int getPriority() { return priority; }
    public int getQuantum() { return quantum; }

    public void setRemainingTime(int t) { remainingTime = t; }
    public void setQuantum(int q) { quantum = q; }

    public void setPriority(int priority) { this.priority = priority; }
}
