package edu.iastate.cs.ja_panc.runtime;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines the basic functions for an event, and for management
 * of handlers and effect sets.
 * 
 * All these fields are changed each time something registers or
 * a publishier is added.  The caller needs to make sure to
 * retrieve these fields to their local versions!
 * @author csgzlong, smooney
 *
 */
public class EventFrameManager {

    public static IEventPublisher[] eps;

    public static int publisherSize = 0;

    public static IEventHandler[][] elementData;

    public static IEventHandler[] elements;

    public static int elSize;

    public static int[] levelSize;

    public static int size;

    public static AbstractReferenceSet readwriteset = new AbstractReferenceSet();

    private static void ensureCapacity(int minCapacity) {
        if (elementData != null) {
            int oldCapacity = elementData.length;
            if (minCapacity > oldCapacity) {
                int newCapacity = oldCapacity << 1;
                if (newCapacity < minCapacity) newCapacity = minCapacity;
                elementData = Arrays.copyOf(elementData, newCapacity);
                levelSize = Arrays.copyOf(levelSize, newCapacity);
            }
            elementData[minCapacity - 1] = new IEventHandler[2];
            return;
        }
        elementData = new IEventHandler[2][];
    }

    /**
	 * Push all of the event frame state into the static fields for our class
	 * @param publisherSize
	 * @param elementData
	 * @param elements
	 * @param elSize
	 * @param levelSize
	 * @param size
	 * @param readwriteset
	 */
    private static void loadEventFrameState(final int apublisherSize, final IEventHandler[][] aelementData, final IEventHandler[] aelements, final int aelSize, final int[] alevelSize, final int asize, final AbstractReferenceSet areadwriteset) {
        publisherSize = apublisherSize;
        elementData = aelementData;
        elements = aelements;
        elSize = aelSize;
        levelSize = alevelSize;
        readwriteset = areadwriteset;
        size = asize;
        if (readwriteset == null) {
            readwriteset = new AbstractReferenceSet();
        }
    }

    /**
	 *     public  static IEventPublisher[] eps;
    //public, so that we can access this other places
    //without using a getter method.
    public  static int publisherSize = 0;

    public  static IEventHandler[][] elementData;
    public  static IEventHandler[] elements;
    public  static int elSize;
    public  static int[] levelSize;

    public  static int size;

    public static AbstractReferenceSet  readwriteset = new AbstractReferenceSet();

	 * @param subscriber
	 */
    public static void register(IEventHandler subscriber, final int publisherSize, final IEventHandler[][] elementData, final IEventHandler[] elements, final int elSize, final int[] levelSize, final int size, final AbstractReferenceSet readwriteset) {
        loadEventFrameState(publisherSize, elementData, elements, elSize, levelSize, size, readwriteset);
        register(subscriber);
    }

    private static void register(IEventHandler subscriber) {
        if (elementData == null) {
            elementData = new IEventHandler[2][];
            elementData[0] = new IEventHandler[2];
            levelSize = new int[2];
            elementData[0][0] = subscriber;
            size = 1;
            levelSize[0] = 1;
            elements = new IEventHandler[2];
            elSize = 1;
            elements[0] = subscriber;
            AbstractReferenceSet other = subscriber.readwriteset();
            if (readwriteset.add(other)) {
                for (int i = 0; i < publisherSize; i++) {
                    eps[i].addChanges(other);
                }
            }
            return;
        }
        for (int i = size - 1; i >= 0; i--) {
            IEventHandler[] temp = elementData[i];
            int tempSize = levelSize[i];
            for (int j = 0; j < tempSize; j++) if (subscriber == temp[j]) return;
        }
        AbstractReferenceSet other = subscriber.readwriteset();
        if (readwriteset.add(other)) {
            for (int i = 0; i < publisherSize; i++) {
                eps[i].addChanges(other);
            }
        }
        ensureSubscriberCapacity(elSize + 1);
        elements[elSize++] = subscriber;
        IEventHandler[] temp;
        for (int i = size - 1; i >= 0; i--) {
            temp = elementData[i];
            int tempSize = levelSize[i];
            for (int j = 0; j < tempSize; j++) {
                if (comparePredecessors(subscriber, temp[j])) {
                    if (size == i + 1) {
                        ensureCapacity(size + 1);
                        elementData[size][0] = subscriber;
                        levelSize[size++] = 1;
                        return;
                    }
                    temp = elementData[i + 1];
                    int oldCapacity = temp.length;
                    if (temp[oldCapacity - 1] != null) {
                        int newCapacity = oldCapacity << 1;
                        elementData[i + 1] = Arrays.copyOf(elementData[i + 1], newCapacity);
                    }
                    elementData[i + 1][levelSize[i + 1]] = subscriber;
                    levelSize[i + 1]++;
                    return;
                }
            }
        }
        temp = elementData[0];
        int oldCapacity = temp.length;
        if (temp[oldCapacity - 1] != null) {
            int newCapacity = oldCapacity << 1;
            elementData[0] = Arrays.copyOf(elementData[0], newCapacity);
        }
        elementData[0][levelSize[0]] = subscriber;
        levelSize[0]++;
    }

    public static void register2(IEventHandler subscriber) {
        if (elementData == null) {
            elementData = new IEventHandler[2][];
            elementData[0] = new IEventHandler[2];
            levelSize = new int[2];
            elementData[0][0] = subscriber;
            size = 1;
            levelSize[0] = 1;
            elements = new IEventHandler[2];
            elSize = 1;
            elements[0] = subscriber;
            return;
        }
        for (int i = size - 1; i >= 0; i--) {
            IEventHandler[] temp = elementData[i];
            int tempSize = levelSize[i];
            for (int j = 0; j < tempSize; j++) if (subscriber == temp[j]) return;
        }
        ensureSubscriberCapacity(elSize + 1);
        elements[elSize++] = subscriber;
        IEventHandler[] temp;
        for (int i = size - 1; i >= 0; i--) {
            temp = elementData[i];
            int tempSize = levelSize[i];
            for (int j = 0; j < tempSize; j++) {
                if (comparePredecessors(subscriber, temp[j])) {
                    if (size == i + 1) {
                        ensureCapacity(size + 1);
                        elementData[size][0] = subscriber;
                        levelSize[size++] = 1;
                        return;
                    }
                    temp = elementData[i + 1];
                    int oldCapacity = temp.length;
                    if (temp[oldCapacity - 1] != null) {
                        int newCapacity = oldCapacity << 1;
                        elementData[i + 1] = Arrays.copyOf(elementData[i + 1], newCapacity);
                    }
                    elementData[i + 1][levelSize[i + 1]] = subscriber;
                    levelSize[i + 1]++;
                    return;
                }
            }
        }
        temp = elementData[0];
        int oldCapacity = temp.length;
        if (temp[oldCapacity - 1] != null) {
            int newCapacity = oldCapacity << 1;
            elementData[0] = Arrays.copyOf(elementData[0], newCapacity);
        }
        elementData[0][levelSize[0]] = subscriber;
        levelSize[0]++;
    }

    /**
	 * Force a reorder of the Handler(?) dependencies.
	 * Must pass in the state to use and retrieve it after the method finishes.
	 * @param other
	 * @param publisherSize
	 * @param elementData
	 * @param elements
	 * @param elSize
	 * @param levelSize
	 * @param size
	 * @param readwriteset
	 */
    public static void reorder(AbstractReferenceSet other, final int publisherSize, final IEventHandler[][] elementData, final IEventHandler[] elements, final int elSize, final int[] levelSize, final int size, final AbstractReferenceSet readwriteset) {
        loadEventFrameState(publisherSize, elementData, elements, elSize, levelSize, size, readwriteset);
        reorder(other);
    }

    private static void reorder(AbstractReferenceSet other) {
        if (elSize == 0) {
            return;
        }
        if (readwriteset.add(other)) {
            for (int i = 0; i < publisherSize; i++) {
                eps[i].addChanges(other);
            }
        }
        IEventHandler[] ele = Arrays.copyOf(elements, elSize);
        int tempSize = elSize;
        elementData = null;
        elements = null;
        elSize = 0;
        levelSize = null;
        size = 0;
        for (int i = 0; i < tempSize; i++) {
            register2(ele[i]);
        }
    }

    private static boolean comparePredecessors(IEventHandler e, IEventHandler e1) {
        AbstractReferenceSet ars = e.readwriteset();
        AbstractReferenceReadWriteSet arrs = ars.arrs;
        AbstractReferenceReadWriteSet arws = ars.arws;
        AbstractReferenceSet ars1 = e1.readwriteset();
        AbstractReferenceReadWriteSet arrs1 = ars1.arrs;
        AbstractReferenceReadWriteSet arws1 = ars1.arws;
        if (!AbstractReferenceSet.compare(arrs, arws1)) {
            return true;
        }
        if (!AbstractReferenceSet.compare(arws, arrs1)) {
            return true;
        }
        if (!AbstractReferenceSet.compare(arws, arws1)) {
            return true;
        }
        return false;
    }

    private static void addPublisher(IEventPublisher ep) {
        AbstractReferenceReadWriteSet arrs = readwriteset.arrs;
        AbstractReferenceReadWriteSet arws = readwriteset.arws;
        if (arrs.count > 0 || arws.count > 0) ep.addChanges(readwriteset);
        if (publisherSize == 0) {
            eps = new IEventPublisher[2];
            eps[publisherSize++] = ep;
            return;
        }
        ensurePublishersCapacity(publisherSize + 1);
        eps[publisherSize++] = ep;
    }

    /**
	 * public version of add publisher.
	 * Caller must pass in the state to user for the EventFrame. After
	 * calling, the caller should retrieve the state from the 
	 * EventFrameManager.
	 * 
	 * @param ep
	 * @param publisherSize
	 * @param elementData
	 * @param elements
	 * @param elSize
	 * @param levelSize
	 * @param size
	 * @param readwriteset
	 * @author Sean Mooney
	 */
    public static void addPublisher(IEventPublisher ep, final int publisherSize, final IEventHandler[][] elementData, final IEventHandler[] elements, final int elSize, final int[] levelSize, final int size, final AbstractReferenceSet readwriteset) {
        loadEventFrameState(publisherSize, elementData, elements, elSize, levelSize, size, readwriteset);
        addPublisher(ep);
    }

    private static void ensurePublishersCapacity(int minCapacity) {
        int oldCapacity = eps.length;
        if (minCapacity > oldCapacity) {
            int newCapacity = oldCapacity << 1;
            if (newCapacity < minCapacity) newCapacity = minCapacity;
            eps = Arrays.copyOf(eps, newCapacity);
        }
    }

    private static void ensureSubscriberCapacity(int minCapacity) {
        int oldCapacity = elements.length;
        if (minCapacity > oldCapacity) {
            int newCapacity = oldCapacity << 1;
            if (newCapacity < minCapacity) newCapacity = minCapacity;
            elements = Arrays.copyOf(elements, newCapacity);
        }
    }

    private static String showEffectSet() {
        StringBuilder sb = new StringBuilder();
        sb.append("Effect Set Information:\n");
        for (int i = 0; i < levelSize.length; i++) {
            sb.append("Level: ");
            sb.append(i);
            sb.append("\n");
            for (int j = 0; j < levelSize[i]; j++) {
                sb.append(elementData[i][j].getClass().toString());
                sb.append("\n");
                sb.append("read set: ");
                sb.append(elementData[i][j].readwriteset().arrs.toString());
                sb.append("\n");
                sb.append("write set: ");
                sb.append(elementData[i][j].readwriteset().arws.toString());
            }
            sb.append("\n\n");
        }
        return sb.toString();
    }

    /**
	 * Get a string representation of the handlers at each level.
	 * @return
	 */
    private static String handlerOrder() {
        StringBuilder sb = new StringBuilder();
        sb.append("Handler Orderings:\n");
        for (int i = 0; i < levelSize.length; i++) {
            sb.append("Level: ");
            sb.append(i);
            sb.append(" ");
            for (int j = 0; j < levelSize[i]; j++) {
                sb.append(elementData[i][j].toString());
                sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
