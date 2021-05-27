package org.opentripplanner.routing.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;


/**
 * This class logs the performance for a particular event like this:
 * <pre>
 * 13:34:42.494 INFO (PerformanceLogger.java:53) PERFORMANCE Test - OK: 1 1,69s (1,69s - 1,69s) - FAILED: NONE
 * 13:34:45.004 INFO (PerformanceLogger.java:53) PERFORMANCE Test - OK: 196 2,49s (0,06s - 4,99s) - FAILED: 32 2,32s (0,44s - 4,72s)
 * 13:34:50.004 INFO (PerformanceLogger.java:53) PERFORMANCE Test - OK: 387 2,54s (0,02s - 5,00s) - FAILED: NONE
 * </pre>
 *
 * The log is constructed to be as "light-weight" as possible. It keeps track of:
 * <ol>
 *     <li>number of events</li>
 *     <li>the total time used (to calculate avarage time)</li>
 *     <li>min time</li>
 *     <li>max time</li>
 * </ol>
 * The logger will log the performance periodically, like every 10 seconds.
 * <p>
 * This should only be used for events witch take "at least" one second.
 * <p>
 * The logger is THREAD-SAFE and
 */
public class PerformanceLogger {

    private final Logger log;
    private final String name;
    private final int logIntervalMs;
    private final Map<String, LogStatistics> events = new HashMap<>();
    private final List<String> keys = new ArrayList<>();
    private boolean firstLogEvent = true;
    private long nextLogTime;

    public PerformanceLogger(Logger log, String name, int logIntervalSeconds) {
        this.log = log;
        this.name = name;
        this.logIntervalMs = logIntervalSeconds * 1000;
        this.nextLogTime = netLogTime();
    }

    /**
     * Add an event.
     * @param time the time the event toke in milliseconds. Negative times are ignored.
     */
    public void add(String event, long time) {
        synchronized (this) {
            getEvent(event).add(time);
            logResultOnTime();
        }
    }

    /**
     * Log the accumulated results at once, without checking the time. This may be useful
     * to call if the server is going down and you want to logg the result of the pending
     * events.
     */
    public void logResultNow() {
        synchronized (this) {
            StringBuilder buf = new StringBuilder();
            for (String key : keys) {
                buf.append(", ").append(events.get(key).result());
            }
            log.info("PERFORMANCE {}  {}", name, buf.substring(2));
        }
    }

    /**
     * Check if the current time have passed the next log interval, and if so, logs the
     * result. If not, do nothing.
     */
    private void logResultOnTime() {
        if(System.currentTimeMillis() > nextLogTime) {
            logResultNow();
            nextLogTime = netLogTime();
        }
        else if(firstLogEvent) {
            logResultNow();
            firstLogEvent = false;
        }
    }

    LogStatistics getEvent(String event) {
        var instance = events.get(event);
        if(instance == null) {
            keys.add(event);
            instance = new LogStatistics(event);
            events.put(event, instance);
        }
        return instance;
    }

    private long netLogTime() {
        return (System.currentTimeMillis() / logIntervalMs + 1) * logIntervalMs;
    }

    private static class LogStatistics {
        private final String name;
        private long min;
        private long max;
        private long total;
        private long counter;

        private LogStatistics(String name) {
            this.name = name;
            reset();
        }

        void reset() {
            min = 999_999;
            max = 0;
            total = 0;
            counter = 0;
        }

        void add(long time) {
            if(time < 0) { return; }
            min = Math.min(min, time);
            max = Math.max(max, time);
            total += time;
            ++counter;
        }

        public String result() {
            String msg = toString();
            reset();
            return msg;
        }

        @Override
        public String toString() {
            if(counter == 0) {
                return name + ": NONE";
            }
            return String.format("%s: %d %.2fs (%.2fs %.2fs)",
                    name, counter, (total/counter)/1000.0, min/1000.0, max/1000.0
            );
        }
    }
}
