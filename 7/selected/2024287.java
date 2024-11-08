package com.jme3.light;

import com.jme3.export.*;
import com.jme3.scene.Spatial;
import com.jme3.util.SortUtil;
import java.io.IOException;
import java.util.*;

/**
 * <code>LightList</code> is used internally by {@link Spatial}s to manage
 * lights that are attached to them.
 * 
 * @author Kirill Vainer
 */
public final class LightList implements Iterable<Light>, Savable, Cloneable {

    private Light[] list, tlist;

    private float[] distToOwner;

    private int listSize;

    private Spatial owner;

    private static final int DEFAULT_SIZE = 1;

    private static final Comparator<Light> c = new Comparator<Light>() {

        /**
         * This assumes lastDistance have been computed in a previous step.
         */
        public int compare(Light l1, Light l2) {
            if (l1.lastDistance < l2.lastDistance) return -1; else if (l1.lastDistance > l2.lastDistance) return 1; else return 0;
        }
    };

    /**
     * Default constructor for serialization. Do not use
     */
    public LightList() {
    }

    /**
     * Creates a <code>LightList</code> for the given {@link Spatial}.
     * 
     * @param owner The spatial owner
     */
    public LightList(Spatial owner) {
        listSize = 0;
        list = new Light[DEFAULT_SIZE];
        distToOwner = new float[DEFAULT_SIZE];
        Arrays.fill(distToOwner, Float.NEGATIVE_INFINITY);
        this.owner = owner;
    }

    /**
     * Set the owner of the LightList. Only used for cloning.
     * @param owner 
     */
    public void setOwner(Spatial owner) {
        this.owner = owner;
    }

    private void doubleSize() {
        Light[] temp = new Light[list.length * 2];
        float[] temp2 = new float[list.length * 2];
        System.arraycopy(list, 0, temp, 0, list.length);
        System.arraycopy(distToOwner, 0, temp2, 0, list.length);
        list = temp;
        distToOwner = temp2;
    }

    /**
     * Adds a light to the list. List size is doubled if there is no room.
     *
     * @param l
     *            The light to add.
     */
    public void add(Light l) {
        if (listSize == list.length) {
            doubleSize();
        }
        list[listSize] = l;
        distToOwner[listSize++] = Float.NEGATIVE_INFINITY;
    }

    /**
     * Remove the light at the given index.
     * 
     * @param index
     */
    public void remove(int index) {
        if (index >= listSize || index < 0) throw new IndexOutOfBoundsException();
        listSize--;
        if (index == listSize) {
            list[listSize] = null;
            return;
        }
        for (int i = index; i < listSize; i++) {
            list[i] = list[i + 1];
        }
        list[listSize] = null;
    }

    /**
     * Removes the given light from the LightList.
     * 
     * @param l the light to remove
     */
    public void remove(Light l) {
        for (int i = 0; i < listSize; i++) {
            if (list[i] == l) {
                remove(i);
                return;
            }
        }
    }

    /**
     * @return The size of the list.
     */
    public int size() {
        return listSize;
    }

    /**
     * @return the light at the given index.
     * @throws IndexOutOfBoundsException If the given index is outside bounds.
     */
    public Light get(int num) {
        if (num >= listSize || num < 0) throw new IndexOutOfBoundsException();
        return list[num];
    }

    /**
     * Resets list size to 0.
     */
    public void clear() {
        if (listSize == 0) return;
        for (int i = 0; i < listSize; i++) list[i] = null;
        if (tlist != null) Arrays.fill(tlist, null);
        listSize = 0;
    }

    /**
     * Sorts the elements in the list acording to their Comparator.
     * There are two reasons why lights should be resorted. 
     * First, if the lights have moved, that means their distance to 
     * the spatial changed. 
     * Second, if the spatial itself moved, it means the distance from it to 
     * the individual lights might have changed.
     * 
     *
     * @param transformChanged Whether the spatial's transform has changed
     */
    public void sort(boolean transformChanged) {
        if (listSize > 1) {
            if (tlist == null || tlist.length != list.length) {
                tlist = list.clone();
            } else {
                System.arraycopy(list, 0, tlist, 0, list.length);
            }
            if (transformChanged) {
                for (int i = 0; i < listSize; i++) {
                    list[i].computeLastDistance(owner);
                }
            }
            SortUtil.msort(tlist, list, 0, listSize - 1, c);
        }
    }

    /**
     * Updates a "world-space" light list, using the spatial's local-space
     * light list and its parent's world-space light list.
     *
     * @param local
     * @param parent
     */
    public void update(LightList local, LightList parent) {
        clear();
        while (list.length <= local.listSize) {
            doubleSize();
        }
        System.arraycopy(local.list, 0, list, 0, local.listSize);
        for (int i = 0; i < local.listSize; i++) {
            distToOwner[i] = Float.NEGATIVE_INFINITY;
        }
        if (parent != null) {
            int sz = local.listSize + parent.listSize;
            while (list.length <= sz) doubleSize();
            for (int i = 0; i < parent.listSize; i++) {
                int p = i + local.listSize;
                list[p] = parent.list[i];
                distToOwner[p] = Float.NEGATIVE_INFINITY;
            }
            listSize = local.listSize + parent.listSize;
        } else {
            listSize = local.listSize;
        }
    }

    /**
     * Returns an iterator that can be used to iterate over this LightList.
     * 
     * @return an iterator that can be used to iterate over this LightList.
     */
    public Iterator<Light> iterator() {
        return new Iterator<Light>() {

            int index = 0;

            public boolean hasNext() {
                return index < size();
            }

            public Light next() {
                if (!hasNext()) throw new NoSuchElementException();
                return list[index++];
            }

            public void remove() {
                LightList.this.remove(--index);
            }
        };
    }

    @Override
    public LightList clone() {
        try {
            LightList clone = (LightList) super.clone();
            clone.owner = null;
            clone.list = list.clone();
            clone.distToOwner = distToOwner.clone();
            clone.tlist = null;
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        ArrayList<Light> lights = new ArrayList<Light>();
        for (int i = 0; i < listSize; i++) {
            lights.add(list[i]);
        }
        oc.writeSavableArrayList(lights, "lights", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        List<Light> lights = ic.readSavableArrayList("lights", null);
        listSize = lights.size();
        int arraySize = Math.max(DEFAULT_SIZE, listSize);
        list = new Light[arraySize];
        distToOwner = new float[arraySize];
        for (int i = 0; i < listSize; i++) {
            list[i] = lights.get(i);
        }
        Arrays.fill(distToOwner, Float.NEGATIVE_INFINITY);
    }
}
