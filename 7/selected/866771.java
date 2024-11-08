package org.jpedal.utils.repositories;

import java.io.Serializable;

/**
 * Provides the functionality/convenience of a Vector for ints - 
 *
 * Much faster because not synchronized and no cast - 
 * Does not double in size each time
 */
public class Vector_Int implements Serializable {

    int increment_size = 1000;

    protected int current_item = 0;

    int max_size = 250;

    protected int[] items = new int[max_size];

    /**new value for an empty array*/
    protected int defaultValue = 0;

    public Vector_Int() {
    }

    protected int incrementSize(int increment_size) {
        if (increment_size < 8000) increment_size = increment_size * 4; else if (increment_size < 16000) increment_size = increment_size * 2; else increment_size = increment_size + 2000;
        return increment_size;
    }

    public Vector_Int(int number) {
        max_size = number;
        items = new int[max_size];
    }

    /**
	 * get element at
	 */
    public final synchronized int elementAt(int id) {
        if (id >= max_size) checkSize(id);
        return items[id];
    }

    /**
	 * extract underlying data
	 */
    public final int[] get() {
        return items;
    }

    /**
	 * set an element
	 */
    public final void setElementAt(int new_name, int id) {
        if (id >= max_size) checkSize(id);
        items[id] = new_name;
    }

    /**
	 * replace underlying data
	 */
    public final void set(int[] new_items) {
        items = new_items;
    }

    public final void keep_larger(int master, int child) {
        if (items[master] < items[child]) items[master] = items[child];
    }

    public final void keep_smaller(int master, int child) {
        if (items[master] > items[child]) items[master] = items[child];
    }

    /**
	 * clear the array
	 */
    public final void clear() {
        items = null;
        items = new int[max_size];
        if (defaultValue != 0) {
            for (int i = 0; i < max_size; i++) items[i] = defaultValue;
        } else {
            if (current_item > 0) {
                for (int i = 0; i < current_item; i++) items[i] = 0;
            } else {
                for (int i = 0; i < max_size; i++) items[i] = 0;
            }
        }
        current_item = 0;
    }

    /**
	 * return the size+1 as in last item (so an array of 0 values is 1) if added
	 * If using set, use checkCapacity
	 */
    public final synchronized int size() {
        return current_item + 1;
    }

    /**
	 * return the sizeof array
	 */
    public final synchronized int getCapacity() {
        return items.length;
    }

    /**
	 * remove element at
	 */
    public final void removeElementAt(int id) {
        if (id >= 0) {
            for (int i = id; i < current_item - 1; i++) items[i] = items[i + 1];
            items[current_item - 1] = 0;
        } else items[0] = 0;
        current_item--;
    }

    /**
	 * delete element at
	 */
    public final synchronized void deleteElementWithValue(int id) {
        int currentSize = items.length;
        int[] newItems = new int[currentSize - 1];
        int counter = 0;
        for (int i = 0; i < currentSize; i++) {
            if (items[i] != id) {
                newItems[counter] = items[i];
                counter++;
            }
        }
        items = newItems;
        current_item--;
    }

    public String toString() {
        String returnString = "{";
        for (int i = 0; i < items.length; i++) returnString = returnString + " " + items[i];
        return returnString + "} " + current_item;
    }

    /**
	 * see if value present
	 */
    public final boolean contains(int value) {
        boolean flag = false;
        for (int i = 0; i < current_item; i++) {
            if (items[i] == value) {
                i = current_item + 1;
                flag = true;
            }
        }
        return flag;
    }

    /**
	 * pull item from top as in LIFO stack
	 */
    public final int pull() {
        if (current_item > 0) current_item--;
        return (items[current_item]);
    }

    /**
	 * put item at top as in LIFO stack
	 */
    public final void push(int value) {
        checkSize(current_item);
        items[current_item] = value;
        current_item++;
        checkSize(current_item);
    }

    /**
	 * add an item
	 */
    public final void addElement(int value) {
        checkSize(current_item);
        items[current_item] = value;
        current_item++;
        checkSize(current_item);
    }

    public final void add_together(int master, int child) {
        items[master] = items[master] + items[child];
    }

    /**
	 * check the size of the array and increase if needed
	 */
    private final void checkSize(int i) {
        if (i >= max_size) {
            int old_size = max_size;
            max_size = max_size + increment_size;
            if (max_size <= i) max_size = i + increment_size + 2;
            int[] temp = items;
            items = new int[max_size];
            int i1 = 0;
            if (defaultValue != 0) {
                for (i1 = old_size; i1 < max_size; i1++) items[i1] = defaultValue;
            }
            System.arraycopy(temp, 0, items, 0, old_size);
            increment_size = incrementSize(increment_size);
        }
    }

    /**
	 * recycle the array by just resetting the pointer
	 */
    public final void reuse() {
        current_item = 0;
    }

    public void trim() {
        int[] newItems = new int[current_item];
        System.arraycopy(items, 0, newItems, 0, current_item);
        items = newItems;
        max_size = current_item;
    }

    /**reset pointer used in add to remove items above*/
    public void setSize(int currentItem) {
        current_item = currentItem;
    }
}
