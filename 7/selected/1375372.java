package clutrfree;

import java.text.DecimalFormat;

public class Utils {

    public static int sq(int x) {
        return (x * x);
    }

    public static double sq(double x) {
        return (x * x);
    }

    public static float sq(float x) {
        return (x * x);
    }

    public static int U(int x) {
        if (x > 0) {
            return (x);
        } else {
            return (0);
        }
    }

    public static boolean testSign(int x) {
        if (x >= 0) return true; else return false;
    }

    public static boolean testSign(float x) {
        if (x >= 0) return true; else return false;
    }

    public static boolean testSign(double x) {
        if (x >= 0) return true; else return false;
    }

    public static boolean testBit(int n, int b) {
        n >>= b;
        n = n & 0x0001;
        return (n == 0x0001);
    }

    public static int setBit(int n, int b) {
        int i = 0x001;
        i <<= b;
        return (n | i);
    }

    public static int clearBit(int n, int b) {
        int r = 0;
        for (int i = 0; i < 32; i++) {
            if (testBit(n, i) & (b != i)) {
                r = setBit(r, i);
            }
        }
        return r;
    }

    public static boolean testBit(long n, int b) {
        n >>= b;
        n = n & 0x0001;
        return (n == 0x0001);
    }

    public static long setBit(long n, int b) {
        long i = 0x001;
        i <<= b;
        return (n | i);
    }

    public static long clearBit(long n, int b) {
        long r = 0;
        for (int i = 0; i < 64; i++) {
            if (testBit(n, i) & (b != i)) {
                r = setBit(r, i);
            }
        }
        return r;
    }

    public static boolean testBit(String s, int b) {
        return (testBit(Character.digit(s.charAt(s.length() - 1 - b / 4), 16), b % 4));
    }

    public static StringBuffer setBit(StringBuffer s, int b) {
        s.setCharAt(s.length() - 1 - b / 4, Character.forDigit(setBit(Character.digit(s.charAt(s.length() - 1 - b / 4), 16), b % 4), 16));
        return (s);
    }

    public static long hexTolong(String hex) {
        int val;
        long r = 0;
        for (int i = 0; i < hex.length(); i++) {
            val = (int) hex.charAt(i);
            if (val >= 97) val -= 87; else {
                if (val >= 65) val -= 55; else val -= 48;
            }
            System.out.println(val);
            r += val * (long) Math.pow(16, i);
        }
        return r;
    }

    /**
	 * return extension of a name
	 */
    public static String getExtension(String s) {
        return s.substring(s.lastIndexOf(".") + 1);
    }

    /**
	  * Check if a string represents an integer
	  */
    public static boolean isParseableAsInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	  * replace a roman number to an int, a letter to an int, 
	  * or an int to an int.
	  */
    public static int decodeToInt(String strNumber) {
        if (strNumber.compareToIgnoreCase("I") == 0) return 0;
        if (strNumber.compareToIgnoreCase("II") == 0) return 1;
        if (strNumber.compareToIgnoreCase("III") == 0) return 2;
        if (strNumber.compareToIgnoreCase("IV") == 0) return 3;
        if (strNumber.compareToIgnoreCase("V") == 0) return 4;
        if (strNumber.compareToIgnoreCase("VI") == 0) return 5;
        if (strNumber.compareToIgnoreCase("VII") == 0) return 6;
        if (strNumber.compareToIgnoreCase("VIII") == 0) return 7;
        if (strNumber.compareToIgnoreCase("IX") == 0) return 8;
        if (strNumber.compareToIgnoreCase("X") == 0) return 9;
        String num;
        int index;
        int tmp;
        if (strNumber.length() > 1) {
            if ((index = strNumber.indexOf('a')) >= 0) {
                num = new String(strNumber.substring(0, index));
                tmp = (Integer.decode(num)).intValue();
                return tmp;
            }
            if ((index = strNumber.indexOf('b')) >= 0) {
                num = new String(strNumber.substring(0, index));
                tmp = (Integer.decode(num)).intValue();
                return tmp + 1;
            }
            if ((index = strNumber.indexOf('c')) >= 0) {
                num = new String(strNumber.substring(0, index));
                tmp = (Integer.decode(num)).intValue();
                return tmp + 2;
            }
            if ((index = strNumber.indexOf('A')) >= 0) {
                num = new String(strNumber.substring(0, index));
                tmp = (Integer.decode(num)).intValue();
                return tmp;
            }
            if ((index = strNumber.indexOf('B')) >= 0) {
                num = new String(strNumber.substring(0, index));
                tmp = (Integer.decode(num)).intValue();
                return tmp + 1;
            }
            if ((index = strNumber.indexOf('C')) >= 0) {
                num = new String(strNumber.substring(0, index));
                tmp = (Integer.decode(num)).intValue();
                return tmp + 2;
            }
        }
        char tmpchar = strNumber.charAt(0);
        if (tmpchar >= 65 && tmpchar <= 90) {
            return (int) tmpchar - 65;
        }
        if (tmpchar >= 97 && tmpchar <= 122) {
            return (int) tmpchar - 97;
        }
        tmp = Integer.parseInt(strNumber);
        return tmp;
    }

    /**
	  * convert a float to engineer notation
	  */
    public static String eng(float f) {
        DecimalFormat formatter = new DecimalFormat("0.###E0");
        String s = formatter.format(f);
        return s;
    }

    /**
	  * Sort an int[] vector by increasing order.
	  */
    public static int[] sort(int[] v) {
        int i;
        int l = v.length;
        int[] index = new int[l];
        for (i = 0; i < l; i++) index[i] = i;
        int tmp;
        boolean change = true;
        while (change) {
            change = false;
            for (i = 0; i < l - 1; i++) {
                if (v[index[i]] > v[index[i + 1]]) {
                    tmp = index[i];
                    index[i] = index[i + 1];
                    index[i + 1] = tmp;
                    change = true;
                }
            }
        }
        return index;
    }

    /**
	  * This function removes the comma around a string, if applicable
	  * (""aa"" is returned as "aa")
	  * ("aa" is unchanged)
	  */
    public static String trimComma(String s) {
        String[] ls = s.split("\"");
        if (ls.length == 1) return ls[0]; else return ls[1];
    }

    /**
	  * This function removes the Mac excel header, if applicable
	  * (A buch of garbage followed by the file name in front of
	  * the first line!!!!!!!!)
	  */
    public static String trimExcelHeader(String s, String name) {
        System.out.println("name = " + name);
        String[] ls = s.split(name);
        if (ls.length == 1) return ls[0]; else return ls[1];
    }

    /**
	  * keep only the file name of a full path
	  * (i.e. foo/text.txt is returned as text.txt
	  */
    public static String keepFileName(String s) {
        String[] ls = s.split("/");
        return ls[ls.length - 1];
    }
}
