package net.sourceforge.scrollrack;

import org.eclipse.swt.widgets.Display;

public class EventQueue {

    private Display display;

    private Object[] queue;

    private int size;

    public EventQueue(Display display) {
        this.display = display;
        this.queue = new Object[4];
        this.size = 0;
    }

    /**
 * Add an event to the queue to be processed by the SWT thread.
 */
    public synchronized void enqueue(Object event) {
        if (queue.length == size) {
            Object[] n = new Object[queue.length * 2];
            for (int i = 0; i < size; i++) n[i] = queue[i];
            queue = n;
        }
        queue[size] = event;
        size++;
        display.wake();
    }

    /**
 * Return a ConnectEvent, or a DisconnectEvent, or an Exception that
 * interruped the connection, or a received line of text.
 */
    public synchronized Object get_event() {
        if (size == 0) return null;
        Object event = queue[0];
        size--;
        for (int i = 0; i < size; i++) queue[i] = queue[i + 1];
        queue[size] = null;
        return event;
    }
}
