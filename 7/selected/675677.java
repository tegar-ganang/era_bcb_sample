package org.jpedal.utils.repositories;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.io.Serializable;

/**
 * Provides the functionality/convenience of a Vector for ints
 *
 * Much faster because not synchronized and no cast
 * Does not double in size each time
 */
public class Vector_Path implements Serializable {

    int increment_size = 1000;

    protected int current_item = 0;

    int max_size = 250;

    private GeneralPath[] items = new GeneralPath[max_size];

    public Vector_Path() {
    }

    protected int incrementSize(int increment_size) {
        if (increment_size < 8000) increment_size = increment_size * 4; else if (increment_size < 16000) increment_size = increment_size * 2; else increment_size = increment_size + 2000;
        return increment_size;
    }

    public Vector_Path(int number) {
        max_size = number;
        items = new GeneralPath[max_size];
    }

    /**
	 * extract underlying data
	 */
    public final GeneralPath[] get() {
        return items;
    }

    /**
	 * remove element at
	 */
    public final void removeElementAt(int id) {
        if (id >= 0) {
            for (int i = id; i < current_item - 1; i++) items[i] = items[i + 1];
            items[current_item - 1] = new GeneralPath();
        } else items[0] = new GeneralPath();
        current_item--;
    }

    /**
	 * does nothing
	 */
    public final boolean contains(Shape value) {
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
	 * replace underlying data
	 */
    public final void set(GeneralPath[] new_items) {
        items = new_items;
    }

    /**
	 * remove element at
	 */
    public final GeneralPath elementAt(int id) {
        if (id >= max_size) checkSize(id);
        return items[id];
    }

    /**
	 * add an item
	 */
    public final void addElement(GeneralPath value) {
        checkSize(current_item);
        items[current_item] = value;
        current_item++;
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
    public final void setElementAt(GeneralPath new_name, int id) {
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
            GeneralPath[] temp = items;
            items = new GeneralPath[max_size];
            System.arraycopy(temp, 0, items, 0, old_size);
            increment_size = incrementSize(increment_size);
        }
    }

    /**
	 * sets the current item
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param current_item
	 */
    public void setCurrent_item(int current_item) {
        this.current_item = current_item;
    }

    public void trim() {
        GeneralPath[] newItems = new GeneralPath[current_item];
        System.arraycopy(items, 0, newItems, 0, current_item);
        items = newItems;
        max_size = current_item;
    }

    /**reset pointer used in add to remove items above*/
    public void setSize(int currentItem) {
        current_item = currentItem;
    }
}
