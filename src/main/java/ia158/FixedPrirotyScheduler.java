package ia158;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Processing unit of the robot.
 * Accepts jobs in a queue and executes them according their priority.
 */
public class FixedPrirotyScheduler {

    private Queue<Job> jobs = new PriorityQueue<>(Comparator.comparingInt(Job::getPriority));
    private Map<java.lang.String, Resource> resources = new HashMap<>();

    public void addResource(java.lang.String name, Object resourceObject) {
        Resource resource = new Resource(name, resourceObject);
        resources.put(name, resource);
    }

    public void executeNext() {
        if (!jobs.isEmpty()) {
            Job job = jobs.poll();
            job.setResources(resources);
            job.run();
        }
    }

    public void planJob(Job job) {
        jobs.add(job);
    }

}
