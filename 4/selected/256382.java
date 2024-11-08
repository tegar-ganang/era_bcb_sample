package com.dustedpixels.jasmin.cpu;

import com.dustedpixels.jasmin.common.Timeline;
import com.dustedpixels.jasmin.memory.Memory;

/**
 * @author micapolos@gmail.com (Michal Pociecha-Los)
 */
public final class MemoryWriter implements Runnable {

    private final Timeline timeline;

    private final Memory memory;

    private int steps;

    public MemoryWriter(Timeline timeline, Memory memory, int steps) {
        this.timeline = timeline;
        this.memory = memory;
        this.steps = steps;
    }

    public void run() {
        while (steps-- != 0) {
            memory.write(0, memory.read(0));
            timeline.sleep(1);
        }
        memory.flush();
    }
}
