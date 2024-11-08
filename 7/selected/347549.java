package game.engine;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

/**
 * This class is effectively a modified version of the Java ArrayList class. It
 * has been modified to permit public access to an internal array of game
 * objects. The class also supports sorting of game objects based upon either
 * their layer x or y offset (to permit overlap between objects to be quickly
 * determined).
 * 
 * @version $Revision: 1 $ $Date: 2007/08 $
 */
public final class GameObjectCollection {

    /**
	 * String name of this game object collection
	 */
    public String gameObjectCollectionName;

    /**
	 * The Java ArrayList class provides support for an array of objects, this
	 * modification restricts access to game objects.
	 */
    public GameObject[] gameObjects;

    /**
	 * The number of game objects contained within this game object collection
	 */
    public int size;

    /**
	 * All game objects added to the game object collection are sorted depending
	 * upon either their layer x or y location (the choice of x or y sorting
	 * should depend upon if the game layer is wider than it is higher, etc.).
	 */
    public enum SortOrder {

        X, Y
    }

    private SortOrder sortOrder = SortOrder.X;

    /**
	 * Constructs an empty game object collection with an initial capacity of
	 * ten.
	 */
    public GameObjectCollection(String gameObjectCollectionName) {
        this(gameObjectCollectionName, 10);
    }

    /**
	 * Constructs an empty game object collection with an initial specified
	 * capacity.
	 * 
	 * @param initialCapacity
	 *            the initial capacity of the list
	 * @exception IllegalArgumentException
	 *                if the specified initial capacity is negative
	 */
    public GameObjectCollection(String gameObjectCollectionName, int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        this.gameObjectCollectionName = gameObjectCollectionName;
        gameObjects = new GameObject[initialCapacity];
    }

    /**
	 * Trims the capacity of this instance to be the list's current size. An
	 * application can use this operation to minimize the storage of the
	 * instance.
	 */
    public void trimToSize() {
        int oldCapacity = gameObjects.length;
        if (size < oldCapacity) {
            gameObjects = Arrays.copyOf(gameObjects, size);
        }
    }

    /**
	 * Increases the capacity of this instance, if necessary, to ensure that it
	 * can hold at least the number of elements specified by the minimum
	 * capacity argument.
	 * 
	 * @param minCapacity
	 *            the desired minimum capacity
	 */
    public void ensureCapacity(int minCapacity) {
        int oldCapacity = gameObjects.length;
        if (minCapacity >= oldCapacity) {
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            gameObjects = Arrays.copyOf(gameObjects, newCapacity);
        }
    }

    /**
	 * Returns the number of elements in this list.
	 * 
	 * @return the number of elements in this list
	 */
    public int size() {
        return size;
    }

    /**
	 * Returns <tt>true</tt> if this list contains no elements.
	 * 
	 * @return <tt>true</tt> if this list contains no elements
	 */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
	 * Returns <tt>true</tt> if this list contains the specified game object.
	 * More formally, returns <tt>true</tt> if and only if this list contains at
	 * least one game object <tt>e</tt> such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
	 * 
	 * @param o
	 *            element whose presence in this list is to be tested
	 * @return <tt>true</tt> if this list contains the specified element
	 */
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    /**
	 * Returns the index of the first occurrence of the specified element in
	 * this list, or -1 if this list does not contain the element. More
	 * formally, returns the lowest index <tt>i</tt> such that
	 * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
	 * or -1 if there is no such index.
	 */
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++) {
                if (gameObjects[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (o.equals(gameObjects[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
	 * Returns the index of the last occurrence of the specified element in this
	 * list, or -1 if this list does not contain the element. More formally,
	 * returns the highest index <tt>i</tt> such that
	 * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
	 * or -1 if there is no such index.
	 */
    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size - 1; i >= 0; i--) {
                if (gameObjects[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = size - 1; i >= 0; i--) {
                if (o.equals(gameObjects[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
	 * Returns an array containing all of the elements in this list in proper
	 * sequence (from first to last element).
	 * 
	 * <p>
	 * The returned array will be "safe" in that no references to it are
	 * maintained by this list. (In other words, this method must allocate a new
	 * array). The caller is thus free to modify the returned array.
	 * 
	 * <p>
	 * This method acts as bridge between array-based and collection-based APIs.
	 * 
	 * @return an array containing all of the elements in this list in proper
	 *         sequence
	 */
    public Object[] toArray() {
        return Arrays.copyOf(gameObjects, size);
    }

    /**
	 * Returns the element at the specified position in this list.
	 * 
	 * @param index
	 *            index of the element to return
	 * @return the element at the specified position in this list
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
    public GameObject get(int index) {
        return gameObjects[index];
    }

    /**
	 * Replaces the element at the specified position in this list with the
	 * specified element.
	 * 
	 * @param index
	 *            index of the element to replace
	 * @param element
	 *            element to be stored at the specified position
	 * @return the element previously at the specified position
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
    public GameObject set(int index, GameObject gameObject) {
        GameObject oldValue = gameObjects[index];
        gameObjects[index] = gameObject;
        return oldValue;
    }

    /**
	 * Appends the specified element to the end of this list.
	 * 
	 * @param e
	 *            element to be appended to this list
	 * @return <tt>true</tt> (as specified by {@link Collection#add})
	 */
    public boolean add(GameObject e) {
        ensureCapacity(size + 1);
        gameObjects[size++] = e;
        return true;
    }

    /**
	 * Inserts the specified element at the specified position in this list.
	 * Shifts the element currently at that position (if any) and any subsequent
	 * elements to the right (adds one to their indices).
	 * 
	 * @param index
	 *            index at which the specified element is to be inserted
	 * @param element
	 *            element to be inserted
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
    public void add(int index, GameObject element) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        ensureCapacity(size + 1);
        System.arraycopy(gameObjects, index, gameObjects, index + 1, size - index);
        gameObjects[index] = element;
        size++;
    }

    /**
	 * Removes the element at the specified position in this list. Shifts any
	 * subsequent elements to the left (subtracts one from their indices).
	 * 
	 * @param index
	 *            the index of the element to be removed
	 * @return the element that was removed from the list
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
    public GameObject remove(int index) {
        GameObject oldValue = gameObjects[index];
        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(gameObjects, index + 1, gameObjects, index, numMoved);
        }
        gameObjects[--size] = null;
        return oldValue;
    }

    /**
	 * Removes the first occurrence of the specified element from this list, if
	 * it is present. If the list does not contain the element, it is unchanged.
	 * More formally, removes the element with the lowest index <tt>i</tt> such
	 * that
	 * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
	 * (if such an element exists). Returns <tt>true</tt> if this list contained
	 * the specified element (or equivalently, if this list changed as a result
	 * of the call).
	 * 
	 * @param o
	 *            element to be removed from this list, if present
	 * @return <tt>true</tt> if this list contained the specified element
	 */
    public boolean remove(Object o) {
        if (o == null) {
            for (int index = 0; index < size; index++) {
                if (gameObjects[index] == null) {
                    fastRemove(index);
                    return true;
                }
            }
        } else {
            for (int index = 0; index < size; index++) {
                if (o.equals(gameObjects[index])) {
                    fastRemove(index);
                    return true;
                }
            }
        }
        return false;
    }

    private void fastRemove(int index) {
        int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(gameObjects, index + 1, gameObjects, index, numMoved);
        }
        gameObjects[--size] = null;
    }

    /**
	 * Removes all of the elements from this list. The list will be empty after
	 * this call returns.
	 */
    public void clear() {
        for (int i = 0; i < size; i++) {
            gameObjects[i] = null;
        }
        size = 0;
    }

    /**
	 * Appends all of the elements in the specified collection to the end of
	 * this list, in the order that they are returned by the specified
	 * collection's Iterator. The behavior of this operation is undefined if the
	 * specified collection is modified while the operation is in progress.
	 * (This implies that the behavior of this call is undefined if the
	 * specified collection is this list, and this list is nonempty.)
	 * 
	 * @param c
	 *            collection containing elements to be added to this list
	 * @return <tt>true</tt> if this list changed as a result of the call
	 * @throws NullPointerException
	 *             if the specified collection is null
	 */
    public boolean addAll(Collection<GameObject> c) {
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacity(size + numNew);
        System.arraycopy(a, 0, gameObjects, size, numNew);
        size += numNew;
        return numNew != 0;
    }

    /**
	 * Inserts all of the elements in the specified collection into this list,
	 * starting at the specified position. Shifts the element currently at that
	 * position (if any) and any subsequent elements to the right (increases
	 * their indices). The new elements will appear in the list in the order
	 * that they are returned by the specified collection's iterator.
	 * 
	 * @param index
	 *            index at which to insert the first element from the specified
	 *            collection
	 * @param c
	 *            collection containing elements to be added to this list
	 * @return <tt>true</tt> if this list changed as a result of the call
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified collection is null
	 */
    public boolean addAll(int index, Collection<GameObject> c) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacity(size + numNew);
        int numMoved = size - index;
        if (numMoved > 0) {
            System.arraycopy(gameObjects, index, gameObjects, index + numNew, numMoved);
        }
        System.arraycopy(a, 0, gameObjects, index, numNew);
        size += numNew;
        return numNew != 0;
    }

    /**
	 * Removes from this list all of the elements whose index is between
	 * <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive. Shifts
	 * any succeeding elements to the left (reduces their index). This call
	 * shortens the list by <tt>(toIndex - fromIndex)</tt> elements. (If
	 * <tt>toIndex==fromIndex</tt>, this operation has no effect.)
	 * 
	 * @param fromIndex
	 *            index of first element to be removed
	 * @param toIndex
	 *            index after last element to be removed
	 * @throws IndexOutOfBoundsException
	 *             if fromIndex or toIndex out of range (fromIndex &lt; 0 ||
	 *             fromIndex &gt;= size() || toIndex &gt; size() || toIndex &lt;
	 *             fromIndex)
	 */
    protected void removeRange(int fromIndex, int toIndex) {
        int numMoved = size - toIndex;
        System.arraycopy(gameObjects, toIndex, gameObjects, fromIndex, numMoved);
        int newSize = size - (toIndex - fromIndex);
        while (size != newSize) {
            gameObjects[--size] = null;
        }
    }

    /**
	 * Set the sort order this game object collection to that specified.
	 * 
	 * @param sortOrder
	 *            SortOrder to use for this collection
	 */
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
	 * Return the sort order used by this game object collection.
	 * 
	 * @return SortOrder used by this collection
	 */
    public SortOrder getSortOrder() {
        return sortOrder;
    }

    /**
	 * Sort the game object contained within this game object collection based
	 * on the specified sort order.
	 * <P>
	 * Note: The built in Java sort algorithm is used to perform this sort (i.e.
	 * merge/quick sort based). As such this method should be called sort large
	 * numbers of out of sequence game object. It should not be used to sort the
	 * game object collection following the addition of a small number of game
	 * objects.
	 */
    public void sortGameObjects() {
        for (int idx = size; idx < gameObjects.length; idx++) {
            gameObjects[idx] = null;
        }
        Comparator<GameObject> comparator;
        if (sortOrder == SortOrder.X) {
            comparator = new Comparator<GameObject>() {

                public int compare(GameObject gameObject1, GameObject gameObject2) {
                    if (gameObject1 == null || gameObject2 == null) {
                        if (gameObject1 != null) {
                            return -1;
                        } else if (gameObject2 != null) {
                            return 1;
                        } else {
                            return 0;
                        }
                    } else {
                        if (gameObject1.x < gameObject2.x) {
                            return -1;
                        } else if (gameObject1.x > gameObject2.x) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }
            };
        } else {
            comparator = new Comparator<GameObject>() {

                public int compare(GameObject gameObject1, GameObject gameObject2) {
                    if (gameObject1 == null || gameObject2 == null) {
                        if (gameObject1 != null) {
                            return -1;
                        } else if (gameObject2 != null) {
                            return 1;
                        } else {
                            return 0;
                        }
                    } else {
                        if (gameObject1.y < gameObject2.y) {
                            return -1;
                        } else if (gameObject1.y > gameObject2.y) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                }
            };
        }
        Arrays.sort(gameObjects, comparator);
    }

    /**
	 * Incrementally sort the game object contained within this game object
	 * collection based on the specified sort order.
	 * <P>
	 * Note: This method uses a modified form of bubble sort to order the game
	 * objects into the correct sequence. This form of algorithm will work
	 * faster than other forms of sort algorithm, e.g. quick sort, merge sort,
	 * etc. if the data is nearly in sorted order and only a few elements need
	 * to be 'corrected' - which will generally be the case for an existing
	 * population of games objects to which a few objects are added.
	 */
    public void incrementalSortGameObjects() {
        if (sortOrder == SortOrder.X) {
            incrementalSortGameObjectsOnXPosition();
        } else {
            incrementalSortGameObjectsOnYPosition();
        }
    }

    /**
	 * Incrementally sort the game object based on their x layer location
	 */
    private void incrementalSortGameObjectsOnXPosition() {
        GameObject tempGameObject;
        for (int i = 0; i < size - 1; i++) {
            if (gameObjects[i].x - gameObjects[i].boundingDimension / 2 > gameObjects[i + 1].x - gameObjects[i + 1].boundingDimension / 2) {
                tempGameObject = gameObjects[i];
                gameObjects[i] = gameObjects[i + 1];
                gameObjects[i + 1] = tempGameObject;
                int j = i;
                while (j > 0 && gameObjects[j].x - gameObjects[j].boundingDimension / 2 < gameObjects[j - 1].x - gameObjects[j - 1].boundingDimension / 2) {
                    tempGameObject = gameObjects[j];
                    gameObjects[j] = gameObjects[j - 1];
                    gameObjects[j - 1] = tempGameObject;
                    j--;
                }
                j = i + 1;
                while (j < size - 1 && gameObjects[j].x - gameObjects[j].boundingDimension / 2 > gameObjects[j + 1].x - gameObjects[j + 1].boundingDimension / 2) {
                    tempGameObject = gameObjects[j];
                    gameObjects[j] = gameObjects[j + 1];
                    gameObjects[j + 1] = tempGameObject;
                    j++;
                }
            }
        }
    }

    /**
	 * Incrementally sort the game object based on their x layer location
	 */
    private void incrementalSortGameObjectsOnYPosition() {
        GameObject tempGameObject;
        for (int i = 0; i < size - 1; i++) {
            if (gameObjects[i].y - gameObjects[i].boundingDimension / 2 > gameObjects[i + 1].y - gameObjects[i + 1].boundingDimension / 2) {
                tempGameObject = gameObjects[i];
                gameObjects[i] = gameObjects[i + 1];
                gameObjects[i + 1] = tempGameObject;
                int j = i;
                while (j > 0 && gameObjects[j].y - gameObjects[j].boundingDimension / 2 < gameObjects[j - 1].y - gameObjects[j - 1].boundingDimension / 2) {
                    tempGameObject = gameObjects[j];
                    gameObjects[j] = gameObjects[j - 1];
                    gameObjects[j - 1] = tempGameObject;
                    j--;
                }
                j = i + 1;
                while (j < size - 1 && gameObjects[j].y - gameObjects[j].boundingDimension / 2 > gameObjects[j + 1].y - gameObjects[j + 1].boundingDimension / 2) {
                    tempGameObject = gameObjects[j];
                    gameObjects[j] = gameObjects[j + 1];
                    gameObjects[j + 1] = tempGameObject;
                    j++;
                }
            }
        }
    }
}
