package org.opentripplanner.routing.framework;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceLoggerTest {
    private static final Logger LOG = LoggerFactory.getLogger(PerformanceLoggerTest.class);

    @Test
    @Ignore("This test can be used for manual verification.")
    public void myTest() {
        String[] events = new String[] { "OK", "FAILED" };
        PerformanceLogger logger = new PerformanceLogger(LOG, "Test", 5);

        for (int i = 0; i < 10_000; i++) {
            long time = (long)(5 * Math.random() * 1000) + 12;
            if(i%7==6) {
                logger.add("OK", time);
            }
            else {
                logger.add("FAILED", time);
            }
            sleep37Ms();
        }
    }

    private void sleep37Ms() {
        try {
            Thread.sleep(37);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}