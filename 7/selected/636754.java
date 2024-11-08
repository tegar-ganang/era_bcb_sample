package org.jpedal.utils.repositories;

import java.io.Serializable;

/**
 * Provides the functionality/convenience of a Vector for floats
 *
 * Much faster because not synchronized and no cast
 * Does not double in size each time
 */
public class Vector_Float implements Serializable {

    int increment_size = 1000;

    protected int current_item = 0;

    int max_size = 250;

    private float[] items = new float[max_size];

    public Vector_Float() {
    }

    public Vector_Float(int number) {
        max_size = number;
        items = new float[max_size];
    }

    protected int incrementSize(int increment_size) {
        if (increment_size < 8000) increment_size = increment_size * 4; else if (increment_size < 16000) increment_size = increment_size * 2; else increment_size = increment_size + 2000;
        return increment_size;
    }

    /**
	 * extract underlying data
	 */
    public final float[] get() {
        return items;
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
	 * add an item
	 */
    public final void addElement(float value) {
        checkSize(current_item);
        items[current_item] = value;
        current_item++;
    }

    /**
	 * replace underlying data
	 */
    public final void set(float[] new_items) {
        items = new_items;
    }

    /**
	 * remove element at
	 */
    public final float elementAt(int id) {
        if (id >= max_size) checkSize(id);
        return items[id];
    }

    /**
	 * clear the array
	 */
    public final void clear() {
        if (current_item > 0) {
            for (int i = 0; i < current_item; i++) items[i] = 0f;
        } else {
            for (int i = 0; i < max_size; i++) items[i] = 0f;
        }
        current_item = 0;
    }

    /**
	 * recycle the array by just resetting the pointer
	 */
    public final void reuse() {
        current_item = 0;
    }

    /**
	 * return the size
	 */
    public final int size() {
        return current_item + 1;
    }

    /**
	 * set an element
	 */
    public final void setElementAt(float new_name, int id) {
        if (id >= max_size) checkSize(id);
        items[id] = new_name;
    }

    /**
	 * check the size of the array and increase if needed
	 */
    private final void checkSize(int i) {
        if (i >= max_size) {
            int old_size = max_size;
            max_size = max_size + increment_size;
            if (max_size <= i) max_size = i + increment_size + 2;
            float[] temp = items;
            items = new float[max_size];
            System.arraycopy(temp, 0, items, 0, old_size);
            increment_size = incrementSize(increment_size);
        }
    }

    public void trim() {
        float[] newItems = new float[current_item];
        System.arraycopy(items, 0, newItems, 0, current_item);
        items = newItems;
        max_size = current_item;
    }

    /**reset pointer used in add to remove items above*/
    public void setSize(int currentItem) {
        current_item = currentItem;
    }
}