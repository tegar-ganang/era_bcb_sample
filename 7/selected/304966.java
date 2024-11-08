package Set;

public class ArraySet<R> implements SetADT<R> {

    private final int DEFAULT_CAPACITY = 25;

    private R[] set;

    private int size;

    public ArraySet() {
        set = (R[]) new Object[DEFAULT_CAPACITY];
    }

    @Override
    public boolean add(R element) {
        if (!contains(element)) {
            if (size >= set.length) increaseCapacity();
            set[size] = element;
            size++;
            return true;
        }
        return false;
    }

    public boolean add(R... elements) {
        boolean out = false;
        for (R e : elements) {
            if (add(e) == true) out = true;
        }
        return out;
    }

    public R get(int index) {
        if (index >= size) throw new IndexOutOfBoundsException();
        return set[index];
    }

    @Override
    public boolean contains(R element) {
        for (int i = 0; i < size; i++) if (set[i].equals(element)) return true;
        return false;
    }

    @Override
    public boolean equals(SetADT<R> s) {
        if (s.size() != size()) return false;
        for (int i = 0; i < size; i++) if (!s.contains(set[i])) return false;
        return true;
    }

    @Override
    public SetADT<R> intersection(SetADT<R> s) {
        SetADT<R> temp = new ArraySet<R>();
        for (R data : set) {
            if (s.contains(data)) {
                temp.add(data);
            }
        }
        return temp;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean remove(R element) {
        for (int i = 0; i < size; i++) {
            if (element.equals(set[i])) {
                for (; i < size - 1; i++) {
                    set[i] = set[i + 1];
                }
                size--;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean subset(SetADT<R> s) {
        try {
            for (int i = 0; true; i++) {
                if (!contains(s.get(i))) return false;
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return true;
    }

    @Override
    public SetADT<R> union(SetADT<R> s) {
        SetADT<R> temp = s.clone();
        for (int i = 0; i < size; i++) {
            temp.add(set[i]);
        }
        return temp;
    }

    private void increaseCapacity() {
        R[] temp = (R[]) new Object[set.length * 2];
        for (int i = 0; i < size; i++) {
            temp[i] = set[i];
        }
        set = temp;
    }

    @Override
    public SetADT<R> clone() {
        ArraySet<R> temp = new ArraySet<R>();
        for (int i = 0; i < size; i++) {
            temp.add(set[i]);
        }
        return temp;
    }

    public String toString() {
        String out = "{";
        for (int i = 0; i < size; i++) {
            out += set[i].toString() + ",";
        }
        if (out.endsWith(",")) out = out.substring(0, out.length() - 1);
        out += "}";
        return out;
    }
}
