package org.jpedal.utils.repositories;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.jpedal.io.PathSerializer;

/**
 * Provides the functionality/convenience of a Vector for ints
 *
 * Much faster because not synchronized and no cast
 * Does not double in size each time
 */
public class Vector_Shape implements Serializable {

    int increment_size = 1000;

    protected int current_item = 0;

    int max_size = 250;

    private Area[] items = new Area[max_size];

    public Vector_Shape() {
    }

    protected int incrementSize(int increment_size) {
        if (increment_size < 8000) increment_size = increment_size * 4; else if (increment_size < 16000) increment_size = increment_size * 2; else increment_size = increment_size + 2000;
        return increment_size;
    }

    public Vector_Shape(int number) {
        max_size = number;
        items = new Area[max_size];
    }

    /**
	 * extract underlying data
	 */
    public final Area[] get() {
        return items;
    }

    /**
	 * remove element at
	 */
    public final void removeElementAt(int id) {
        if (id >= 0) {
            for (int i = id; i < current_item - 1; i++) items[i] = items[i + 1];
            items[current_item - 1] = new Area();
        } else items[0] = new Area();
        current_item--;
    }

    /**
	 * does nothing
	 */
    public final boolean contains(Shape value) {
        return false;
    }

    /**
	 * replace underlying data
	 */
    public final void set(Area[] new_items) {
        items = new_items;
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
	 * remove element at
	 */
    public final Area elementAt(int id) {
        if (id >= max_size) checkSize(id);
        return items[id];
    }

    /**
	 * add an item
	 */
    public final void addElement(Area value) {
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
    public final void setElementAt(Area new_name, int id) {
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
            Area[] temp = items;
            items = new Area[max_size];
            System.arraycopy(temp, 0, items, 0, old_size);
            increment_size = incrementSize(increment_size);
        }
    }

    /**
	 * writes out the shapes in this collection to the ByteArrayOutputStream
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param bos - the ByteArrayOutputStream to write out to
	 * @throws IOException
	 */
    public void writeToStream(ByteArrayOutputStream bos) throws IOException {
        ObjectOutput os = new ObjectOutputStream(bos);
        os.writeObject(new Integer(max_size));
        for (int i = 0; i < max_size; i++) {
            Area nextObj = items[i];
            if (nextObj == null) os.writeObject(null); else {
                PathIterator pathIterator = nextObj.getPathIterator(new AffineTransform());
                PathSerializer.serializePath(os, pathIterator);
            }
        }
    }

    /**
	 * restore the shapes from the input stream into this collections
	 * 
	 * NOT PART OF API and subject to change (DO NOT USE)
	 * 
	 * @param bis - ByteArrayInputStream to read from
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    public void restoreFromStream(ByteArrayInputStream bis) throws IOException, ClassNotFoundException {
        ObjectInput os = new ObjectInputStream(bis);
        int size = ((Integer) os.readObject()).intValue();
        max_size = size;
        items = new Area[size];
        for (int i = 0; i < size; i++) {
            GeneralPath path = PathSerializer.deserializePath(os);
            if (path == null) items[i] = null; else items[i] = new Area(path);
        }
    }

    public void trim() {
        Area[] newItems = new Area[current_item];
        System.arraycopy(items, 0, newItems, 0, current_item);
        items = newItems;
        max_size = current_item;
    }

    /**reset pointer used in add to remove items above*/
    public void setSize(int currentItem) {
        current_item = currentItem;
    }
}
