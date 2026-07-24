package schedulers;

import java.util.List;
import java.util.Map;
import models.Process;

public interface Scheduler {
    SchedulerResult schedule(List<Process> processes, int contextSwitchTime);
}
