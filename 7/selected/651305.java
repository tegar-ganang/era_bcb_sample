package org.jpedal.utils.repositories;

import java.io.Serializable;

/**
 * Provides the functionality/convenience of a Vector for shorts
 *
 * Much faster because not synchronized and no cast
 * Does not double in size each time
 */
public class Vector_Short implements Serializable {

    int increment_size = 1000;

    protected int current_item = 0;

    int max_size = 250;

    private short[] items = new short[max_size];

    public Vector_Short() {
    }

    protected int incrementSize(int increment_size) {
        if (increment_size < 8000) increment_size = increment_size * 4; else if (increment_size < 16000) increment_size = increment_size * 2; else increment_size = increment_size + 2000;
        return increment_size;
    }

    public Vector_Short(int number) {
        max_size = number;
        items = new short[max_size];
    }

    /**
	 * get element at
	 */
    public final short elementAt(int id) {
        if (id >= max_size) checkSize(id);
        return items[id];
    }

    /**
	 * extract underlying data
	 */
    public final short[] get() {
        return items;
    }

    /**
	 * set an element
	 */
    public final void setElementAt(short new_name, int id) {
        if (id >= max_size) checkSize(id);
        items[id] = new_name;
    }

    /**
	 * replace underlying data
	 */
    public final void set(short[] new_items) {
        items = new_items;
    }

    public final void keep_larger(short master, short child) {
        if (items[master] < items[child]) items[master] = items[child];
    }

    public final void keep_smaller(short master, short child) {
        if (items[master] > items[child]) items[master] = items[child];
    }

    /**
	 * clear the array
	 */
    public final void clear() {
        if (current_item > 0) {
            for (int i = 0; i < current_item; i++) items[i] = 0;
        } else {
            for (int i = 0; i < max_size; i++) items[i] = 0;
        }
        current_item = 0;
    }

    /**
	 * return the size
	 */
    public final int size() {
        return current_item + 1;
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
	 * see if value present
	 */
    public final boolean contains(short value) {
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
    public final short pull() {
        if (current_item > 0) current_item--;
        return (items[current_item]);
    }

    /**
	 * put item at top as in LIFO stack
	 */
    public final void push(short value) {
        checkSize(current_item);
        items[current_item] = value;
        current_item++;
    }

    /**
	 * add an item
	 */
    public final void addElement(short value) {
        checkSize(current_item);
        items[current_item] = value;
        current_item++;
    }

    public final void add_together(short master, short child) {
        items[master] = (short) (items[master] + items[child]);
    }

    /**
	 * check the size of the array and increase if needed
	 */
    private final void checkSize(int i) {
        if (i >= max_size) {
            int old_size = max_size;
            max_size = max_size + increment_size;
            if (max_size <= i) max_size = i + increment_size + 2;
            short[] temp = items;
            items = new short[max_size];
            System.arraycopy(temp, 0, items, 0, old_size);
            increment_size = incrementSize(increment_size);
        }
    }

    public void trim() {
        short[] newItems = new short[current_item];
        System.arraycopy(items, 0, newItems, 0, current_item);
        items = newItems;
        max_size = current_item;
    }

    /**reset pointer used in add to remove items above*/
    public void setSize(int currentItem) {
        current_item = currentItem;
    }
}
