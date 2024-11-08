package eu.mpower.framework.fsa.j2me.db;

/**
 *
 * @author Grabadora
 */
public class ObjectQueue {

    private int size;

    private Object[] queue;

    public ObjectQueue(int size) {
        this.size = size;
        queue = new Object[size];
        for (int i = 0; i < size; i++) {
            queue[i] = null;
        }
    }

    public Object pop() {
        Object result = queue[0];
        for (int i = 0; i < size - 1; i++) {
            queue[i] = queue[i + 1];
        }
        return result;
    }

    public void push(Object number) {
        for (int i = 0; i < size - 1; i++) {
            queue[i] = queue[i + 1];
        }
        queue[size - 1] = number;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Object elementAt(int index) {
        if (index >= 0 || index < size) {
            return queue[index];
        } else {
            return null;
        }
    }
}
