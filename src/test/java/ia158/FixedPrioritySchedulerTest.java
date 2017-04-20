package ia158;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testing the FixedPriorityScheduler.
 */
class FixedPrioritySchedulerTest {

    @Test
    void run() {
        // Initialization
        File resource1 = new File("a");
        List<String> stamps = new ArrayList<>();
        FixedPriorityScheduler scheduler = new FixedPriorityScheduler();
        scheduler.addResource("r1", resource1);
        scheduler.addResource("stamps", stamps);

        // prepare jobs
        Job firstJob = new Job(10) {
            @Override
            public void run() {
                // get resources
                File r1 = getResource("r1", File.class);
                List stamps = getResource("stamps", List.class);
                // rotate
                stamps.add("first");
            }
        };

        Job secondJob = new Job(9) {
            @Override
            public void run() {
                List stamps = getResource("stamps", List.class);
                stamps.add("second");
            }
        };
        Job thirdJob = new Job(10) {
            @Override
            public void run() {
                List stamps = getResource("stamps", List.class);
                stamps.add("third");
            }
        };

        Job forthJob = new Job(10) {
            @Override
            public void run() {
                File r1 = getResource("r1", File.class);
                List stamps = getResource("stamps", List.class);
                stamps.add("forth");
            }
        };

        // plan jobs
        scheduler.planJob(firstJob);
        scheduler.planJob(secondJob, 30);
        scheduler.planJob(thirdJob, 30);
        scheduler.planTask(forthJob, 20);

        // run
        scheduler.run(50);

        // assert
        List<String> expectedOrder = new ArrayList<>();
        expectedOrder.add("first");
        expectedOrder.add("forth");
        expectedOrder.add("forth");
        expectedOrder.add("third");
        expectedOrder.add("second");
        expectedOrder.add("forth");
        assertEquals(expectedOrder, scheduler.getResource("stamps", List.class));
    }

}