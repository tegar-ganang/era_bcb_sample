package com.google.code.b0rx0r.advancedSamplerEngine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/** The play queue is the place where playables are enqueued for playing. 
 *  The vst interface and gui place them here, the AudioProcessor reads them from here.
 *  
 * @author Lui
 *
 */
public class AudioProcessor {

    private long now = 0;

    private float[] empty = new float[1024];

    private SortedSet<EnqueuedEnqueueable> queue = new TreeSet<EnqueuedEnqueueable>();

    private void increaseTime(int amount) {
        now += amount;
    }

    public long getTime() {
        return now;
    }

    public EnqueuedEnqueueable enqueue(Enqueueable enqueueable, long startTime) {
        EnqueuedEnqueueable ee = new EnqueuedEnqueueable(enqueueable, startTime);
        queue.add(ee);
        return ee;
    }

    public void dequeue(EnqueuedEnqueueable ee) {
        queue.remove(ee);
    }

    private List<EnqueuedEnqueueable> finished = new LinkedList<EnqueuedEnqueueable>();

    public void processReplacing(float[][] inputs, float[][] outputs, int sampleFrames) {
        for (int i = 0; i < outputs.length; i++) {
            if (outputs[i] != null) {
                System.arraycopy(empty, 0, outputs[i], 0, sampleFrames);
            }
        }
        for (EnqueuedEnqueueable ee : new ArrayList<EnqueuedEnqueueable>(queue)) {
            if (ee.start > now + sampleFrames) break;
            boolean keep = handleEnqueueable(ee, now, outputs, sampleFrames);
            if (!keep) {
                finished.add(ee);
            }
        }
        queue.removeAll(finished);
        finished.clear();
        increaseTime(sampleFrames);
    }

    private boolean handleEnqueueable(EnqueuedEnqueueable ee, long now, float[][] outputs, int sampleFrames) {
        Enqueueable e = ee.getEnqueueable();
        long start = ee.getStart();
        long offsetIntoEnqueueable = now - start;
        e.setModulationGlobalOffset(start);
        if (offsetIntoEnqueueable < 0) return true;
        for (int i = 0; i < sampleFrames; i++) {
            long offset = i + offsetIntoEnqueueable;
            for (int channel = 0; channel < e.getChannelCount(); channel++) {
                for (int output : e.getOutputMap().getOutputs(channel)) {
                    float data = e.getAudioData(channel, offset);
                    if (data == Enqueueable.NO_MORE_AUDIO_DATA) return false;
                    outputs[output][i] += data;
                }
            }
        }
        return true;
    }

    public static class EnqueuedEnqueueable implements Comparable<EnqueuedEnqueueable> {

        private Enqueueable enqueueable;

        private long start;

        public EnqueuedEnqueueable(Enqueueable enqueueable, long start) {
            this.enqueueable = enqueueable;
            this.start = start;
        }

        public Enqueueable getEnqueueable() {
            return enqueueable;
        }

        public long getStart() {
            return start;
        }

        @Override
        public int compareTo(EnqueuedEnqueueable other) {
            if (other == this) return 0;
            if (start == other.start) {
                return System.identityHashCode(this) - System.identityHashCode(other);
            }
            return (int) (start - other.start);
        }
    }
}
