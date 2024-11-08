package systemobject.snmp.utils;

import java.util.Vector;

public abstract class SnmpOctetStringUtils {

    protected static final String exceptionString = "wrong format of the string";

    protected static final int firstPrintCharacter = 33;

    protected static final int lastPrintCharacter = 126;

    protected static final int dot = '.';

    protected static final String dotStr = ".";

    protected static final char[] separators = new char[] { ' ', '.', ':', ';' };

    public static final char OCTET_STRING_SEPERATOR = '.';

    /**
	 * returns the given String separator character
	 * 
	 * @param str
	 *            String to get the separator from
	 * @return Character, String separator character or "null" if no separator
	 *         has been found
	 */
    protected static Character getSeparator(String str) {
        for (int i = 0; str != null && i < SnmpOctetStringUtils.separators.length; i++) {
            if (str.contains("" + SnmpOctetStringUtils.separators[i])) {
                return SnmpOctetStringUtils.separators[i];
            }
        }
        return null;
    }

    protected static int[] sort(int[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            for (int j = 0; j < i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
        return arr;
    }

    /**
	 * return the given String as String[] containing
	 * 
	 * @param str
	 *            String to parse
	 * @return String[] or null.
	 */
    protected static String[] getBytes(String str) {
        String[] arr = null;
        if (str != null) {
            if (str.length() > 0) {
                Character c = getSeparator(str);
                if (c == null) {
                    Vector<String> v = new Vector<String>();
                    if (str.length() % 2 == 1) {
                        str = "0" + str;
                    }
                    while (str.length() > 2) {
                        v.add(str.substring(0, 2));
                        str = str.substring(2);
                    }
                    v.add(str);
                    arr = v.toArray(new String[v.size()]);
                } else {
                    Vector<String> v = new Vector<String>();
                    do {
                        int index = str.indexOf(c);
                        if (index != -1) {
                            v.add(str.substring(0, index));
                            str = str.substring(str.indexOf(c));
                            while (str.startsWith(c.toString())) {
                                str = str.substring(1);
                            }
                        } else {
                            v.add(str);
                            str = "";
                        }
                    } while (str.length() > 0);
                    arr = v.toArray(new String[v.size()]);
                }
            } else {
                return new String[] { "" };
            }
        }
        return arr;
    }

    protected static String toHexString(int b) {
        return Integer.toHexString(b | 0x100).toUpperCase().substring(1);
    }

    protected static int[] toArray(String str, int radix) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return new int[0];
        }
        String[] arr = getBytes(str);
        if (arr == null) {
            return null;
        }
        int[] bytes = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            bytes[i] = Integer.parseInt(arr[i], radix);
        }
        return bytes;
    }

    protected static byte[] toArray(int[] arr) {
        if (arr == null) {
            return null;
        }
        if (arr.length == 0) {
            return new byte[0];
        }
        byte[] bytes = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) {
            bytes[i] = (new Integer(arr[i])).byteValue();
        }
        return bytes;
    }

    protected static int[] toArray(Integer[] arr) {
        if (arr == null) {
            return new int[0];
        }
        int[] retArr = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            retArr[i] = arr[i];
        }
        return retArr;
    }

    /**
	 * convert from byte[] to dotted Hex String
	 * 
	 * @param bytes
	 *            byte[], array of byte values
	 * @return String, dotted Hex String
	 */
    public static String toHexDottedString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length == 0) {
            return "";
        }
        int[] arr = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            arr[i] = 0;
            arr[i] = arr[i] | bytes[i];
        }
        return toHexDottedString(arr);
    }

    /**
	 * convert from char[] to dotted Hex String
	 * 
	 * @param characters
	 *            char[], array of char values
	 * @return String, dotted Hex String
	 */
    public static String toHexDottedString(char[] characters) {
        if (characters == null) {
            return null;
        }
        if (characters.length == 0) {
            return "";
        }
        int[] arr = new int[characters.length];
        for (int i = 0; i < characters.length; i++) {
            arr[i] = 0;
            arr[i] = arr[i] | characters[i];
        }
        return toHexDottedString(arr);
    }

    /**
	 * convert from int[] (chars) to dotted Hex String
	 * 
	 * @param ints
	 *            int[], array of integers representing char values
	 * @return String, dotted Hex String
	 */
    public static String toHexDottedString(int[] ints) {
        if (ints == null) {
            return null;
        }
        if (ints.length == 0) {
            return "";
        }
        String str = "";
        for (int i = 0; i < ints.length; i++) {
            str += OCTET_STRING_SEPERATOR + toHexString(ints[i]);
        }
        if (str.length() > 0) {
            return str.substring(1);
        }
        return str;
    }

    /**
	 * convert from Hex dotted octet String to not dotted hex String or Ascii
	 * text String
	 * 
	 * @param hexOctetString
	 *            String, hex String separated with "."
	 * @return String, not dotted hex String or Ascii text String
	 */
    public static String fromHexDottedString(String hexOctetString, boolean toAscii) {
        if (hexOctetString == null) {
            return null;
        }
        if (hexOctetString.length() == 0) {
            return "";
        }
        int[] arr = toArray(hexOctetString, 16);
        if (arr == null) {
            return null;
        }
        String str = "";
        for (int i = 0; i < arr.length; i++) {
            if (toAscii) {
                str += (char) arr[i];
            } else {
                str += toHexString(arr[i]);
            }
        }
        return str;
    }

    /**
	 * converts a given hex string into byte array
	 * 
	 * @param hexOctetString
	 *            String, hex string to convert to bytes array
	 * @return byte[], content of the given String as byte array
	 */
    public static byte[] fromHexStrToByteArray(String hexOctetString) {
        if (hexOctetString == null) {
            return null;
        }
        if (hexOctetString.length() == 0) {
            return new byte[0];
        }
        int[] arr = toArray(hexOctetString, 16);
        return toArray(arr);
    }

    /**
	 * convert from byte[] to Ascii dotted octet String
	 * 
	 * @param bytes
	 *            byte[], array of bytes representing char values
	 * @return String, Ascii dotted octet String
	 */
    public static String toOctetDottedString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length == 0) {
            return "";
        }
        int[] arr = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            arr[i] = 0;
            arr[i] = arr[i] | bytes[i];
        }
        return toOctetDottedString(arr);
    }

    /**
	 * convert from char[] to Ascii dotted octet String
	 * 
	 * @param characters
	 *            char[], array of char values
	 * @return String, Ascii dotted octet String
	 */
    public static String toOctetDottedString(char[] characters) {
        if (characters == null) {
            return null;
        }
        if (characters.length == 0) {
            return "";
        }
        int[] arr = new int[characters.length];
        for (int i = 0; i < characters.length; i++) {
            arr[i] = 0;
            arr[i] = arr[i] | characters[i];
        }
        return toOctetDottedString(arr);
    }

    /**
	 * convert from Ascii text String to Ascii dotted octet String
	 * 
	 * @param str
	 *            String, representing char values
	 * @return String, Ascii dotted octet String
	 */
    public static String toOctetDottedString(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() == 0) {
            return "";
        }
        return toOctetDottedString(str.toCharArray());
    }

    /**
	 * convert from int[] (chars) to Ascii dotted octet String
	 * 
	 * @param ints
	 *            int[], array of integers representing char values
	 * @return String, Ascii dotted octet String
	 */
    public static String toOctetDottedString(int[] ints) {
        if (ints == null) {
            return null;
        }
        if (ints.length == 0) {
            return "";
        }
        String str = "";
        for (int i = 0; i < ints.length; i++) {
            str += OCTET_STRING_SEPERATOR + Integer.toString(ints[i]);
        }
        if (str.length() > 0) {
            return str.substring(1);
        }
        return str;
    }

    /**
	 * convert from Ascii dotted octet String to Ascii text String
	 * 
	 * @param decString
	 *            String, octet String separated with "."
	 * @return String, Ascii text String
	 */
    public static String fromOctetDottedString(String decString) {
        if (decString == null) {
            return null;
        }
        if (decString.length() == 0) {
            return "";
        }
        int[] arr = toArray(decString, 10);
        if (arr == null) {
            return null;
        }
        String str = "";
        for (int i = 0; i < arr.length; i++) {
            str += (char) arr[i];
        }
        return str;
    }

    /**
	 * set a bitmap according to the given bits indexes (indexes = count FROM
	 * the LSB). example 1: input = int[]{1,4,5,8}, output = 0x132 (100110010
	 * binary) example 2: input = int[]{1,4,5,0}, output = 0x33 (110011 binary)
	 * 
	 * @param arr
	 *            int[], array of integers represents the indexes of the bits to
	 *            "turn on" starting from the LSB (LSB index = 0)
	 * @return long, number after the bits were turned on
	 */
    public static long setLongMask(int[] arr) {
        long mask = 0, temp = 1;
        for (int i = 0; arr != null && i < arr.length; i++) {
            temp = temp << arr[i];
            mask = mask | temp;
            temp = 1;
        }
        return mask;
    }

    /**
	 * generates hex string according to the given bits positions
	 * 
	 * @param arr
	 *            array of integers - represents the bits positions to turn on
	 *            (0 = LSB)
	 * @return hex string separated with "."
	 */
    public static String setHexStringMask(int[] arr) {
        return setHexStringMask(arr, -1, -1);
    }

    /**
	 * generates hex string according to the given bits positions and min-max
	 * bytes limitations
	 * 
	 * @param arr
	 *            array of integers - represents the bits positions to turn on
	 *            (index 0 = LSB)
	 * @return hex string separated with "."
	 */
    public static String setHexStringMask(int[] arr, int minByteCount, int maxByteCount) {
        String str = "";
        int num = 0;
        int maxForThisByte = 0;
        if (arr != null) {
            arr = sort(arr);
            for (int i = 0; i < arr.length; i++) {
                while ((arr[i] - (arr[i] % 8)) > maxForThisByte) {
                    maxForThisByte += 8;
                    str = ("." + Integer.toHexString(num | 0x100).substring(1)) + str;
                    num = 0;
                }
                num = num | (1 << (arr[i] - (arr[i] - (arr[i] % 8))));
            }
            str = ("." + Integer.toHexString(num | 0x100).substring(1)) + str;
            str = str.substring(1);
        }
        if (minByteCount > 0) {
            minByteCount = ((minByteCount * 3) - 1);
            while (str.length() < minByteCount) {
                str = "00." + str;
            }
        }
        if (maxByteCount > 0 && maxByteCount >= minByteCount) {
            maxByteCount = (maxByteCount * 3);
            while (str.length() >= maxByteCount) {
                str = str.substring(3);
            }
        }
        if (str.endsWith(".")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
	 * returns mask "on" bit positions
	 * 
	 * @param mask
	 *            long value
	 * @return array of integers containing all bits positions that are "on"
	 *         (LSB = index 0)
	 */
    public static int[] parseMask(long mask) {
        String temp = Long.toBinaryString(mask);
        Vector<Integer> v = new Vector<Integer>();
        for (int i = 0; i < temp.length(); i++) {
            if (temp.charAt(i) == '1') {
                v.add((temp.length() - 1) - i);
            }
        }
        return sort(toArray(v.toArray(new Integer[v.size()])));
    }

    /**
	 * returns mask "on" bit positions
	 * 
	 * @param mask
	 *            hex string value
	 * @return array of integers containing all bits positions that are "on"
	 *         (LSB = index 0)
	 */
    public static int[] parseMask(String mask) {
        String[] arr = getBytes(mask);
        Vector<Integer> v = new Vector<Integer>();
        if (arr != null) {
            int add = 0;
            for (int i = arr.length - 1; i >= 0; i--) {
                if (arr[i] != null) {
                    int[] bits = parseMask(Long.parseLong(arr[i], 16));
                    for (int j = 0; bits != null && j < bits.length; j++) {
                        v.add(bits[j] + add);
                    }
                }
                add += 8;
            }
        }
        return sort(toArray(v.toArray(new Integer[v.size()])));
    }
}
