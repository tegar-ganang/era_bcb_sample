package com.apelon.common.util;

import java.io.*;
import java.util.*;
import com.apelon.common.log4j.Categories;

public class Cons implements Serializable {

    public static int EXPFACTOR = 10;

    protected Object[] list;

    protected int current;

    private int size;

    public Cons(int n) {
        list = new Object[n];
        current = 0;
        size = n;
    }

    public Cons() {
        list = new Object[0];
        current = 0;
        size = 0;
    }

    public void zap() {
        current = 0;
    }

    public Object pop() {
        if (current > 0) {
            return list[--current];
        } else return null;
    }

    public Object top() {
        if (current > 0) {
            return list[current - 1];
        } else {
            return null;
        }
    }

    public void push(Object item) {
        if (current < size) {
            list[current++] = item;
        } else {
            expand();
            list[current++] = item;
        }
    }

    public Object[] toArray() {
        Object[] tmp = new Object[size()];
        System.arraycopy(list, 0, tmp, 0, size());
        return tmp;
    }

    public boolean isEmpty() {
        return (current == 0);
    }

    public int size() {
        return current;
    }

    public void expand() {
        Object[] newlist;
        int exp = 0;
        if (size > 300) {
            newlist = new Object[size + 100];
            exp = 100;
        } else {
            newlist = new Object[size + Cons.EXPFACTOR];
            exp = Cons.EXPFACTOR;
        }
        System.arraycopy(list, 0, newlist, 0, current);
        size = size + exp;
        Categories.data().debug("Expanded array to " + size);
        list = newlist;
        if (size > 200) {
            Categories.data().debug("expanding Cons to " + size);
        }
    }

    public Enumeration elements() {
        return new ConsIterator(list, current);
    }

    public Object at(int n) {
        if (n < 0 || n > size()) return null;
        return list[n];
    }

    public boolean contains(Object obj) {
        Enumeration e = elements();
        while (e.hasMoreElements()) {
            if (obj.equals(e.nextElement())) {
                return true;
            }
        }
        return false;
    }

    public boolean remove(Object obj) {
        for (int i = 0; i < current; i++) {
            if (obj.equals(list[i])) {
                Object[] newList = new Object[size - 1];
                for (int j = 0; j < i; j++) {
                    newList[j] = list[j];
                }
                for (int k = i; k < current - 1; k++) {
                    newList[k] = list[k + 1];
                }
                list = newList;
                --current;
                --size;
                return true;
            }
        }
        return false;
    }

    public String toString() {
        String res = "{ ";
        for (Enumeration e = elements(); e.hasMoreElements(); ) {
            res = res + e.nextElement().toString() + " ";
        }
        return (res + "}");
    }

    /**
 * This returns the size of the actual array we are using to store
 * the objects.
 * Creation date: (9/28/2001 12:48:41 PM)
 * @return int
 */
    public int storageSize() {
        return list.length;
    }
}
