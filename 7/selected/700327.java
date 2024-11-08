package java.util;

public class ScarseArrayList<E> implements List<E> {

    private short size;

    private short growSize;

    private E elements[];

    public ScarseArrayList(short growSize) {
        this.growSize = growSize;
    }

    public ScarseArrayList() {
        this((short) 4);
    }

    public E get(short index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        return elements[index];
    }

    public short indexOf(E element) {
        for (short i = 0; i < size; i++) if (elements[i] == element) return i;
        return -1;
    }

    public E remove(short index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        E ret = elements[index];
        removeAt(index);
        return ret;
    }

    public E set(short index, E element) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException();
        E ret = elements[index];
        elements[index] = element;
        return ret;
    }

    public void add(E element) {
        if (elements == null) elements = (E[]) (new Object[growSize]);
        if (size >= elements.length) {
            E[] newElements = (E[]) (new Object[size + growSize]);
            for (int i = 0; i < elements.length; i++) newElements[i] = elements[i];
            elements = newElements;
        }
        elements[size] = element;
        size++;
    }

    public void clear() {
        size = 0;
        elements = null;
    }

    public boolean contains(E element) {
        for (int i = 0; i < size; i++) if (elements[i] == element) return true;
        return false;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    private void removeAt(short index) {
        size--;
        for (short i = index; i < size; i++) elements[i] = elements[i + 1];
    }

    public void remove(E element) {
        short pos = indexOf(element);
        if (pos != -1) removeAt(pos);
    }

    public short size() {
        return size;
    }

    public Object[] toArray() {
        E[] ret = (E[]) (new Object[size]);
        for (int i = 0; i < size; i++) ret[i] = elements[i];
        return ret;
    }

    public Iterator<E> iterator() {
        return new ScarseArrayListIterator();
    }

    private class ScarseArrayListIterator implements Iterator<E> {

        private short index;

        public boolean hasNext() {
            return index < size;
        }

        public E next() {
            E ret = (E) elements[index];
            index++;
            return ret;
        }

        public void remove() {
            removeAt(index);
        }
    }
}
