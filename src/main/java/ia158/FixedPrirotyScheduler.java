package ia158;

import java.util.*;

/**
 * Scheduler for jobs to be executed on the robot.
 * Accepts jobs in a queue and executes them according their priority.
 */
public class FixedPrirotyScheduler {

    private Queue<Job> jobs = new PriorityQueue<>(Comparator.comparingInt(Job::getPriority).reversed());
    private Map<Long, List<Job>> postponedJobs = new HashMap<>();

    private Map<java.lang.String, Resource> resources = new HashMap<>();
    private long startTime;

    public FixedPrirotyScheduler() {
        startTime = System.currentTimeMillis();
    }

    public <T> T getResource(String name, Class<T> type) {
        return type.cast(resources.get(name).getResource());
    }

    public void addResource(java.lang.String name, Object resourceObject) {
        Resource resource = new Resource(name, resourceObject);
        resources.put(name, resource);
    }

    public void run(long endTime) {
        while(System.currentTimeMillis() < startTime + endTime) {

            // check postponed jobs
            Iterator<Long> it = postponedJobs.keySet().iterator();
            while (it.hasNext()) {
                Long scheduleTime = it.next();
                if (System.currentTimeMillis() > startTime + scheduleTime) {
                    jobs.addAll(postponedJobs.get(scheduleTime));
                    it.remove();
                }
            }

            // run job
            if (!jobs.isEmpty()) {
                Job job = jobs.poll();
                job.setResources(resources);
                job.run();
            }
        }
    }

    public void planJob(Job job) {
        jobs.add(job);
    }

    public void planJob(Job job, long atTime) {
        List<Job> jobs = postponedJobs.get(atTime);
        if (jobs == null) {
            List<Job> jobsToAdd = new ArrayList<>();
            jobsToAdd.add(job);
            postponedJobs.put(atTime, jobsToAdd);
        } else {
            jobs.add(job);
        }
    }

}
