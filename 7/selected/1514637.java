package org.jpedal.utils.repositories;

import java.awt.Rectangle;
import java.io.Serializable;

/**
 * Provides the functionality/convenience of a Vector for Rectangle
 *
 * Much faster because not synchronized and no cast
 * Does not double in size each time
 */
public class Vector_Rectangle implements Serializable {

    int increment_size = 1000;

    protected int current_item = 0;

    int max_size = 250;

    private Rectangle[] items = new Rectangle[max_size];

    public Vector_Rectangle(int number) {
        max_size = number;
        items = new Rectangle[max_size];
    }

    public Vector_Rectangle() {
    }

    protected int incrementSize(int increment_size) {
        if (increment_size < 8000) increment_size = increment_size * 4; else if (increment_size < 16000) increment_size = increment_size * 2; else increment_size = increment_size + 2000;
        return increment_size;
    }

    /**
	 * add an item
	 */
    public synchronized void addElement(Rectangle value) {
        checkSize(current_item);
        items[current_item] = value;
        current_item++;
    }

    /**
	 * remove element at
	 */
    public final void removeElementAt(int id) {
        if (id >= 0) {
            for (int i = id; i < current_item - 1; i++) items[i] = items[i + 1];
            items[current_item - 1] = new Rectangle();
        } else items[0] = new Rectangle();
        current_item--;
    }

    /**
	 * does nothing
	 */
    public final boolean contains(Rectangle value) {
        return false;
    }

    /**
	 * clear the array
	 */
    public final void clear() {
        if (current_item > 0) {
            for (int i = 0; i < current_item; i++) items[i] = null;
        } else {
            for (int i = 0; i < max_size; i++) items[i] = null;
        }
        current_item = 0;
    }

    /**
	 * extract underlying data
	 */
    public final Rectangle[] get() {
        return items;
    }

    /**
	 * remove element at
	 */
    public final synchronized Rectangle elementAt(int id) {
        if (id >= max_size) checkSize(id);
        return items[id];
    }

    /**
	 * replace underlying data
	 */
    public final void set(Rectangle[] new_items) {
        items = new_items;
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
    public final synchronized void setElementAt(Rectangle new_name, int id) {
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
            Rectangle[] temp = items;
            items = new Rectangle[max_size];
            System.arraycopy(temp, 0, items, 0, old_size);
            increment_size = incrementSize(increment_size);
        }
    }

    public void trim() {
        Rectangle[] newItems = new Rectangle[current_item];
        System.arraycopy(items, 0, newItems, 0, current_item);
        items = newItems;
        max_size = current_item;
    }

    /**reset pointer used in add to remove items above*/
    public void setSize(int currentItem) {
        current_item = currentItem;
    }
}
