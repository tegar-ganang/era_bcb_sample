package indiji.struct;

import indiji.io.Log;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;

public class IList implements Iterable<Integer> {

    private int pos;

    private int[] data;

    public static void main(String[] args) {
        IList l = new IList();
        Log.log(l);
        for (int n = 0; n < 15; n++) l.add(2 * n);
        Log.log(l);
        l.insertAt(11, 7);
        Log.log(l);
        l.insertAt(0, 17);
        Log.log(l);
        System.exit(0);
    }

    public IList() {
        clear();
    }

    public final void add(final int i) {
        if (++pos >= data.length) {
            final int[] newdata = new int[data.length < 1000000 ? data.length * 2 : data.length + 1000000];
            for (int j = 0; j < data.length; j++) newdata[j] = data[j];
            data = newdata;
        }
        data[pos] = i;
    }

    public final int size() {
        return pos + 1;
    }

    public final void clear() {
        pos = -1;
        data = new int[10];
    }

    public final int get(int idx) {
        if (idx >= 0 && idx <= pos) return data[idx]; else try {
            throw new Exception();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public final int get(int idx, int defaultvalue) {
        if (idx >= 0 && idx <= pos) return data[idx]; else return defaultvalue;
    }

    public final void addAll(final IList l) {
        if (l == null || l.size() == 0) return;
        final int oldpos = pos;
        pos += l.size();
        if (pos >= data.length) {
            final int[] newdata = new int[pos < 500000 ? pos * 2 : pos + 100000];
            for (int j = 0; j < data.length; j++) newdata[j] = data[j];
            data = newdata;
        }
        for (int j = 0; j < l.size(); j++) data[oldpos + j + 1] = l.get(j);
    }

    public final void addAll(final ISet l) {
        if (l == null || l.size() == 0) return;
        final int oldpos = pos;
        pos += l.size();
        if (pos >= data.length) {
            final int[] newdata = new int[pos < 500000 ? pos * 2 : pos + 100000];
            for (int j = 0; j < data.length; j++) newdata[j] = data[j];
            data = newdata;
        }
        int j = 0;
        for (int id : l) data[oldpos + (j++) + 1] = id;
    }

    public final void removeFirst(final int id) {
        int shift = 0;
        for (int n = 0; n < data.length - 1; n++) {
            if (shift == 0 && data[n] == id) {
                shift++;
                pos--;
            }
            data[n] = data[n + shift];
        }
    }

    public final void removeAll(final int id) {
        int shift = 0;
        for (int n = 0; n < data.length - 1; n++) {
            if (data[n] == id) {
                shift++;
                pos--;
            }
            data[n] = data[n + shift];
        }
    }

    public final void removeAll(final IList l) {
        int shift = 0;
        for (int n = 0; n < pos - 1; n++) {
            if (l.contains(data[n])) {
                shift++;
                pos--;
            }
            data[n] = data[n + shift];
        }
    }

    public void removeAll(ISet s) {
        int shift = 0;
        for (int n = 0; n < pos - 1; n++) {
            if (s.contains(data[n])) {
                shift++;
                pos--;
            }
            data[n] = data[n + shift];
        }
    }

    public final int removeAt(final int idx) {
        if (idx < 0 || idx > pos) {
            try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        } else {
            final int result = data[idx];
            pos--;
            for (int n = idx; n <= pos; n++) data[n] = data[n + 1];
            return result;
        }
    }

    public final void insertAt(final int id, final int idx) {
        if (idx < 0 || idx > pos + 1) {
            try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        if (idx == pos + 1) add(id); else {
            final int[] newdata = ++pos >= data.length ? new int[data.length < 1000000 ? data.length * 2 : data.length + 1000000] : data;
            int i = 1;
            for (int j = pos; j >= 0; j--) {
                if (j == idx) {
                    i = 0;
                    newdata[j] = id;
                } else newdata[j] = data[j - i];
            }
            data = newdata;
        }
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {

            private int p = -1;

            @Override
            public boolean hasNext() {
                return ++p <= pos;
            }

            @Override
            public Integer next() {
                return data[p];
            }

            @Override
            public void remove() {
            }
        };
    }

    @Override
    public String toString() {
        String result = new String("");
        for (int n = 0; n <= pos; n++) result += data[n] + " ";
        result = "[" + result.trim() + "]";
        return result;
    }

    public final void sort() {
        Arrays.sort(data, 0, pos + 1);
    }

    public final boolean contains(final int id) {
        for (int n = 0; n <= pos; n++) if (data[n] == id) return true;
        return false;
    }

    public final void retainAll(final IList l) {
        int p = -1;
        final int[] newdata = new int[data.length];
        for (int n = 0; n <= pos; n++) if (l.contains(data[n])) newdata[++p] = data[n];
        data = newdata;
        pos = p;
    }

    public final void retainAll(final ISet l) {
        int p = -1;
        final int[] newdata = new int[data.length];
        for (int n = 0; n <= pos; n++) if (l.contains(data[n])) newdata[++p] = data[n];
        data = newdata;
        pos = p;
    }

    public final int first() {
        if (pos == -1) {
            try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        } else return data[0];
    }

    public final void sort(final Comparator<Integer> c) {
        final Vector<Integer> tmp = new Vector<Integer>();
        for (int n = 0; n <= pos; n++) tmp.add(data[n]);
        Collections.sort(tmp, c);
        for (int n = 0; n <= pos; n++) data[n] = tmp.get(n);
    }

    public final void shuffle() {
        final Vector<Integer> tmp = new Vector<Integer>();
        for (int n = 0; n <= pos; n++) tmp.add(data[n]);
        Collections.shuffle(tmp);
        for (int n = 0; n <= pos; n++) data[n] = tmp.get(n);
    }

    public final int indexOf(final int id) {
        for (int n = 0; n <= pos; n++) if (data[n] == id) return n;
        return -1;
    }
}
