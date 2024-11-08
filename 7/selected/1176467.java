package preprocessing.methods.DataEnrichement;

/**
 * Created by IntelliJ IDEA.
 * User: lagon
 * Date: Feb 1, 2009
 * Time: 11:43:56 PM
 * To change this template use File | Settings | File Templates.
 */
class OrderedQueue {

    private Object[] items;

    private double[] weights;

    int itemsInQueue;

    public OrderedQueue(int capacity) {
        items = new Object[capacity];
        weights = new double[capacity];
        itemsInQueue = 0;
        for (int i = 0; i < capacity; i++) {
            weights[i] = Double.POSITIVE_INFINITY;
            items[i] = null;
        }
    }

    public void addItem(double weight, Object item) {
        if (weight >= weights[weights.length - 1]) {
            return;
        }
        int index = items.length - 1;
        items[index] = item;
        weights[index] = weight;
        index--;
        while (index >= 0) {
            if (weights[index] > weights[index + 1]) {
                Object tmpO = items[index];
                double tmpW = weights[index];
                items[index] = items[index + 1];
                weights[index] = weights[index + 1];
                items[index + 1] = tmpO;
                weights[index + 1] = tmpW;
                index--;
            } else {
                break;
            }
        }
        itemsInQueue = itemsInQueue < items.length ? itemsInQueue + 1 : items.length;
    }

    public int getNumItemsInQueue() {
        return itemsInQueue;
    }

    public Object getItem(int index) {
        return items[index];
    }

    public int getLength() {
        return items.length;
    }
}
