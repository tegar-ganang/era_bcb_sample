package org.kobjects.util;

import java.io.*;

public final class Util {

    /** 
     * Writes the contents of the input stream to the 
     * given output stream and closes the input stream.
     * The output stream is returned */
    public static OutputStream streamcopy(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[Runtime.getRuntime().freeMemory() >= 1048576 ? 16384 : 128];
        while (true) {
            int count = is.read(buf, 0, buf.length);
            if (count == -1) break;
            os.write(buf, 0, count);
        }
        is.close();
        return os;
    }

    public static int indexOf(Object[] arr, Object find) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(find)) return i;
        }
        return -1;
    }

    public static String buildUrl(String base, String local) {
        int ci = local.indexOf(':');
        if (local.startsWith("/") || ci == 1) return "file:///" + local;
        if (ci > 2 && ci < 6) return local;
        if (base == null) base = "file:///"; else {
            if (base.indexOf(':') == -1) base = "file:///" + base;
            if (!base.endsWith("/")) base = base + ("/");
        }
        return base + local;
    }

    public static void sort(Object[] arr, int start, int end) {
        if (end - start <= 2) {
            if (end - start == 2 && arr[start].toString().compareTo(arr[start + 1].toString()) > 0) {
                Object tmp = arr[start];
                arr[start] = arr[start + 1];
                arr[start + 1] = tmp;
            }
            return;
        }
        if (end - start == 3) {
            sort(arr, start, start + 2);
            sort(arr, start + 1, start + 3);
            sort(arr, start, start + 2);
            return;
        }
        int middle = (start + end) / 2;
        sort(arr, start, middle);
        sort(arr, middle, end);
        Object[] tmp = new Object[end - start];
        int i0 = start;
        int i1 = middle;
        for (int i = 0; i < tmp.length; i++) {
            if (i0 == middle) {
                tmp[i] = arr[i1++];
            } else if (i1 == end || arr[i0].toString().compareTo(arr[i1].toString()) < 0) {
                tmp[i] = arr[i0++];
            } else {
                tmp[i] = arr[i1++];
            }
        }
        System.arraycopy(tmp, 0, arr, start, tmp.length);
    }
}
