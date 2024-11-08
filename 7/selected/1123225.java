package helper;

public abstract class Utils {

    public static boolean[] int2booleanArray(int value) {
        int width;
        if (value == 0) {
            width = 1;
        } else {
            width = (int) Math.floor(Math.log(value) / Math.log(2)) + 1;
        }
        boolean[] b = new boolean[width];
        int i = 0;
        while (value != 0) {
            b[i++] = value % 2 == 1;
            value /= 2;
        }
        return b;
    }

    public static String int2binaryString(int value) {
        String s = "";
        boolean[] booleanArray = int2booleanArray(value);
        for (boolean booleanDigit : booleanArray) {
            s += booleanDigit ? 1 : 0;
        }
        return new StringBuffer(s).reverse().toString();
    }

    /**
	 * Gets boolean[] and expands/shortens it to a given length.
	 */
    public static boolean[] tailorBooleanArray(boolean[] b, int length) {
        boolean[] result = new boolean[length];
        int i;
        if (length > b.length) {
            for (i = 0; i < b.length; i++) {
                result[i] = b[i];
            }
            for (; i < length; i++) {
                result[i] = false;
            }
        } else {
            for (i = 0; i < length; i++) {
                result[i] = b[i];
            }
        }
        return result;
    }

    public static int booleanArray2int(boolean[] b) {
        int result = 0;
        for (int i = 0; i < b.length; i++) {
            result += boolDigit2int(b[i]) * Math.pow(2, i);
        }
        return result;
    }

    public static int boolDigit2int(boolean b) {
        return b ? 1 : 0;
    }

    public static boolean[] joinBooleanArrays(boolean[]... bools) {
        if (bools.length == 0) {
            return new boolean[] {};
        }
        if (bools.length == 1) {
            return bools[0];
        } else {
            boolean[] c = joinBooleanArrays(bools[0], bools[1]);
            boolean[][] rek = new boolean[bools.length - 1][];
            rek[0] = c;
            for (int i = 1; i < rek.length; i++) {
                rek[i] = bools[i + 1];
            }
            return joinBooleanArrays(rek);
        }
    }

    private static boolean[] joinBooleanArrays(boolean[] a, boolean[] b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        boolean[] c = new boolean[a.length + b.length];
        int i = 0;
        for (; i < a.length; i++) {
            c[i] = a[i];
        }
        for (; i < a.length + b.length; i++) {
            c[i] = b[i - a.length];
        }
        return c;
    }

    /**
	 * fills a String with zeros, while its shorter then the 2nd Parameter.
	 */
    public static String zeroFiller(String s, int length) {
        StringBuffer buff = new StringBuffer(s);
        for (int i = s.length(); i < length; i++) {
            buff = new StringBuffer("0").append(buff);
        }
        return buff.toString();
    }
}
