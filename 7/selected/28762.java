package com.algorithmstudy.visualizer.client.sort;

import com.algorithmstudy.visualizer.client.model.ListBasedAlgorithm;
import com.algorithmstudy.visualizer.client.model.ListVisualizer;

/**
 * Implementation of the bubble sort algorithm that interfaces with a ListVisualizer implementation.
 */
public class BubbleSort implements ListBasedAlgorithm {

    private int[] elements;

    private ListVisualizer v;

    private boolean canMakeMove = false;

    private int rightPtr;

    private int movingPtr;

    public boolean canMakeMove() {
        return canMakeMove;
    }

    public void makeMove() {
        if (!canMakeMove) {
            throw new IllegalStateException("No more moves can be made!");
        }
        if (elements[movingPtr] > elements[movingPtr + 1]) {
            int tmp = elements[movingPtr];
            elements[movingPtr] = elements[movingPtr + 1];
            elements[movingPtr + 1] = tmp;
            v.swap(movingPtr, movingPtr + 1);
        }
        movingPtr++;
        if (movingPtr == rightPtr) {
            movingPtr = 0;
            --rightPtr;
            if (0 == rightPtr) {
                canMakeMove = false;
            }
        }
    }

    public void setListToSort(int[] elements) {
        if (null == elements) {
            throw new NullPointerException("A list of elements is required");
        }
        if (elements.length == 0) {
            throw new IllegalArgumentException("One or more elements is required, zero is not allowed");
        }
        this.elements = elements;
        canMakeMove = true;
        movingPtr = 0;
        rightPtr = elements.length - 1;
    }

    public void setVisualizer(ListVisualizer v) {
        this.v = v;
    }
}
