# CPU Scheduling Simulator

A Java-based CPU Scheduling Simulator that implements and compares multiple CPU scheduling algorithms commonly studied in Operating Systems. The project calculates scheduling metrics such as waiting time and turnaround time while simulating process execution.

## Features

- Shortest Job First (SJF)
- Priority Scheduling
- Round Robin (RR)
- AG Scheduling
- Waiting Time Calculation
- Turnaround Time Calculation
- Process Execution Simulation
- JSON-based Test Cases
- Unit Testing with JUnit

## Technologies Used

- Java
- Maven
- JUnit 5
- Gson
- Object-Oriented Programming (OOP)

## Project Structure

```
CPU-Scheduling-Simulator
│
├── src
│   ├── main
│   │   └── java
│   │       ├── models
│   │       └── schedulers
│   │
│   └── test
│       └── java
│
├── test_cases
│   ├── AG
│   └── Other_Schedulers
│
├── pom.xml
└── README.md
```

## Implemented Algorithms

### Shortest Job First (SJF)

Schedules the process with the smallest CPU burst time first.

### Priority Scheduling

Executes processes according to their priorities while considering arrival times.

### Round Robin

Uses a fixed time quantum to fairly distribute CPU time among processes.

### AG Scheduling

Implements the AG scheduling algorithm using dynamic quantum adjustments.

## Performance Metrics

The simulator calculates:

- Waiting Time
- Turnaround Time
- Execution Order

## Running the Project

Clone the repository:

```bash
git clone https://github.com/farida-tot/CPU-Scheduling-Simulator.git
```

Navigate to the project folder:

```bash
cd CPU-Scheduling-Simulator
```

Run the project using your IDE or Maven.

Run unit tests:

```bash
mvn test
```

## Future Improvements

- GUI visualization
- Gantt Chart visualization
- Additional scheduling algorithms
- Performance comparison dashboard

## Author

**Farida Ahmed**

Computer Science & Artificial Intelligence Student

Cairo University
