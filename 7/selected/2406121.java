package org.perfmon4j.demo.sample;

import java.util.Arrays;
import org.perfmon4j.PerfMon;
import org.perfmon4j.PerfMonConfiguration;
import org.perfmon4j.TextAppender;
import org.perfmon4j.instrument.DeclarePerfMonTimer;

public class AnnotationExample extends SampleRunner {

    @DeclarePerfMonTimer("SortingTest")
    public void sampleMethod() {
        bruteForceSort(cloneArray());
        bubbleSort(cloneArray());
        arraySort(cloneArray());
    }

    @DeclarePerfMonTimer("SortingTest.bubbleSort")
    private void bubbleSort(int values[]) {
        int len = values.length - 1;
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len - i; j++) {
                if (values[j] > values[j + 1]) {
                    int tmp = values[j];
                    values[j] = values[j + 1];
                    values[j + 1] = tmp;
                }
            }
        }
    }

    @DeclarePerfMonTimer("SortingTest.arraySort")
    private void arraySort(int values[]) {
        Arrays.sort(values);
    }

    @DeclarePerfMonTimer("SortingTest.bruteForceSort")
    private void bruteForceSort(int values[]) {
        int len = values.length;
        for (int i = len; i > 1; i--) {
            int v = values[len - 1];
            int insertionPoint = 0;
            for (insertionPoint = 0; insertionPoint < values.length - i; insertionPoint++) {
                if (v <= values[insertionPoint]) {
                    break;
                }
            }
            int numToMove = len - insertionPoint - 1;
            System.arraycopy(values, insertionPoint, values, insertionPoint + 1, numToMove);
            values[insertionPoint] = v;
        }
    }

    public static void main(String args[]) throws Exception {
        PerfMonConfiguration config = new PerfMonConfiguration();
        config.defineAppender("Basic", TextAppender.class.getName(), "10 seconds");
        config.defineMonitor("SortingTest");
        config.attachAppenderToMonitor("SortingTest", "Basic", PerfMon.APPENDER_PATTERN_PARENT_AND_ALL_DESCENDENTS);
        PerfMon.configure(config);
        launchSamplers(AnnotationExample.class);
    }
}
