package bluetooth;

/**
 * @author pmp
 */
public class BtVector {

    private Object[] vector;

    private int size;

    public BtVector() {
        size = 0;
        vector = new Object[10];
    }

    public void add(Object object) {
        if (size < 10) {
            vector[size] = object;
            size++;
        }
    }

    public void remove(int id) {
        if (id >= 0 && id <= size - 1) {
            for (int i = id; i < size - 1; i++) {
                vector[i] = vector[i + 1];
            }
            vector[size - 1] = null;
            size--;
        }
    }

    public void clean() {
        for (int i = 0; i < size - 1; i++) {
            vector[i] = null;
        }
        size = 0;
    }

    public int size() {
        return size;
    }

    public Object get(int id) {
        return vector[id];
    }
}
