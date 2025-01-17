package cn.com.pxto.commons;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.BreakIterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import org.apache.log4j.Logger;

/**
 * @author user
 *
 */
public class StringUtils {

    protected static Logger log = Logger.getLogger("StringUtils");

    private static final char[] QUOTE_ENCODE = "&quot;".toCharArray();

    private static final char[] AMP_ENCODE = "&amp;".toCharArray();

    private static final char[] LT_ENCODE = "&lt;".toCharArray();

    private static final char[] GT_ENCODE = "&gt;".toCharArray();

    /**
	 * Replaces all instances of oldString with newString in string.
	 *
	 * @param string the String to search to perform replacements on
	 * @param oldString the String that should be replaced by newString
	 * @param newString the String that will replace all instances of oldString
	 *
	 * @return a String will all instances of oldString replaced by newString
	 */
    public static final String replace(String string, String oldString, String newString) {
        if (string == null) {
            return null;
        }
        if (newString == null) {
            return string;
        }
        int i = 0;
        if ((i = string.indexOf(oldString, i)) >= 0) {
            char[] string2 = string.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(string2.length);
            buf.append(string2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = string.indexOf(oldString, i)) > 0) {
                buf.append(string2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(string2, j, string2.length - j);
            return buf.toString();
        }
        return string;
    }

    /**
	 * Replaces all instances of oldString with newString in line with the
	 * added feature that matches of newString in oldString ignore case.
	 *
	 * @param line the String to search to perform replacements on
	 * @param oldString the String that should be replaced by newString
	 * @param newString the String that will replace all instances of oldString
	 *
	 * @return a String will all instances of oldString replaced by newString
	 */
    public static final String replaceIgnoreCase(String line, String oldString, String newString) {
        if (line == null) {
            return null;
        }
        String lcLine = line.toLowerCase();
        String lcOldString = oldString.toLowerCase();
        int i = 0;
        if ((i = lcLine.indexOf(lcOldString, i)) >= 0) {
            char[] line2 = line.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = lcLine.indexOf(lcOldString, i)) > 0) {
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            return buf.toString();
        }
        return line;
    }

    /**
	 * Replaces all instances of oldString with newString in line with the
	 * added feature that matches of newString in oldString ignore case.
	 * The count paramater is set to the number of replaces performed.
	 *
	 * @param line the String to search to perform replacements on
	 * @param oldString the String that should be replaced by newString
	 * @param newString the String that will replace all instances of oldString
	 * @param count a value that will be updated with the number of replaces
	 *      performed.
	 *
	 * @return a String will all instances of oldString replaced by newString
	 */
    public static final String replaceIgnoreCase(String line, String oldString, String newString, int[] count) {
        if (line == null) {
            return null;
        }
        String lcLine = line.toLowerCase();
        String lcOldString = oldString.toLowerCase();
        int i = 0;
        if ((i = lcLine.indexOf(lcOldString, i)) >= 0) {
            int counter = 1;
            char[] line2 = line.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = lcLine.indexOf(lcOldString, i)) > 0) {
                counter++;
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            count[0] = counter;
            return buf.toString();
        }
        return line;
    }

    /**ѭ���滻stringbuffer������ַ� 
	 * @author prewind
	 * @param buffer Ҫ�滻���ַ�
	 * @param startString ��ʼλ�õı�־
	 * @param endString ����λ�õı�־
	 * @param replaceWith 
	 * @return StringBuffer
	 * modified  2004-5-8 9:03:44
	 */
    public static final StringBuffer replace(StringBuffer buffer, String startString, String endString, String replaceWith) {
        if (buffer.indexOf(startString) > -1) {
            buffer = buffer.replace(buffer.indexOf(startString), buffer.indexOf(endString, buffer.indexOf(startString)) + 1, replaceWith);
            if (buffer.indexOf(startString) > -1) buffer = replace(buffer, startString, endString, replaceWith);
        }
        return buffer;
    }

    /**
	 * Replaces all instances of oldString with newString in line.
	 * The count Integer is updated with number of replaces.
	 *
	 * @param line the String to search to perform replacements on
	 * @param oldString the String that should be replaced by newString
	 * @param newString the String that will replace all instances of oldString
	 *
	 * @return a String will all instances of oldString replaced by newString
	 */
    public static final String replace(String line, String oldString, String newString, int[] count) {
        if (line == null) {
            return null;
        }
        int i = 0;
        if ((i = line.indexOf(oldString, i)) >= 0) {
            int counter = 1;
            char[] line2 = line.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i = line.indexOf(oldString, i)) > 0) {
                counter++;
                buf.append(line2, j, i - j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            count[0] = counter;
            return buf.toString();
        }
        return line;
    }

    /**
	 * This method takes a string and strips out all tags except <br> tags while still leaving
	 * the tag body intact.
	 *
	 * @param in the text to be converted.
	 * @return the input string with all tags removed.
	 */
    public static final String stripTags(String in) {
        if (in == null) {
            return null;
        }
        char ch;
        int i = 0;
        int last = 0;
        char[] input = in.toCharArray();
        int len = input.length;
        StringBuffer out = new StringBuffer((int) (len * 1.3));
        for (; i < len; i++) {
            ch = input[i];
            if (ch > '>') {
                continue;
            } else if (ch == '<') {
                if (i + 3 < len && input[i + 1] == 'b' && input[i + 2] == 'r' && input[i + 3] == '>') {
                    i += 3;
                    continue;
                }
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
            } else if (ch == '>') {
                last = i + 1;
            }
        }
        if (last == 0) {
            return in;
        }
        if (i > last) {
            out.append(input, last, i - last);
        }
        return out.toString();
    }

    /**
	 * This method takes a string which may contain HTML tags (ie, &lt;b&gt;,
	 * &lt;table&gt;, etc) and converts the '&lt'' and '&gt;' characters to
	 * their HTML escape sequences.
	 *
	 * @param in the text to be converted.
	 * @return the input string with the characters '&lt;' and '&gt;' replaced
	 *  with their HTML escape sequences.
	 */
    public static final String escapeHTMLTags(String in) {
        if (in == null) {
            return null;
        }
        char ch;
        int i = 0;
        int last = 0;
        char[] input = in.toCharArray();
        int len = input.length;
        StringBuffer out = new StringBuffer((int) (len * 1.3));
        for (; i < len; i++) {
            ch = input[i];
            if (ch > '>') {
                continue;
            } else if (ch == '<') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(LT_ENCODE);
            } else if (ch == '>') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(GT_ENCODE);
            }
        }
        if (last == 0) {
            return in;
        }
        if (i > last) {
            out.append(input, last, i - last);
        }
        return out.toString();
    }

    /**
	 * Used by the hash method.
	 */
    private static MessageDigest digest = null;

    /**
	 * Hashes a String using the Md5 algorithm and returns the result as a
	 * String of hexadecimal numbers. This method is synchronized to avoid
	 * excessive MessageDigest object creation. If calling this method becomes
	 * a bottleneck in your code, you may wish to maintain a pool of
	 * MessageDigest objects instead of using this method.
	 * <p>
	 * A hash is a one-way function -- that is, given an
	 * input, an output is easily computed. However, given the output, the
	 * input is almost impossible to compute. This is useful for passwords
	 * since we can store the hash and a hacker will then have a very hard time
	 * determining the original password.
	 * <p>
	 * In Jive, every time a user logs in, we simply
	 * take their plain text password, compute the hash, and compare the
	 * generated hash to the stored hash. Since it is almost impossible that
	 * two passwords will generate the same hash, we know if the user gave us
	 * the correct password or not. The only negative to this system is that
	 * password recovery is basically impossible. Therefore, a reset password
	 * method is used instead.
	 *
	 * @param data the String to compute the hash of.
	 * @return a hashed version of the passed-in String
	 */
    public static final synchronized String hash(String data) {
        if (digest == null) {
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException nsae) {
                log.error("Failed to load the MD5 MessageDigest. " + "Jive will be unable to function normally.", nsae);
            }
        }
        try {
            digest.update(data.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }
        return encodeHex(digest.digest());
    }

    /**
	 * Turns an array of bytes into a String representing each byte as an
	 * unsigned hex number.
	 * <p>
	 * Method by Santeri Paavolainen, Helsinki Finland 1996<br>
	 * (c) Santeri Paavolainen, Helsinki Finland 1996<br>
	 * Distributed under LGPL.
	 *
	 * @param bytes an array of bytes to convert to a hex-string
	 * @return generated hex string
	 */
    public static final String encodeHex(byte[] bytes) {
        StringBuffer buf = new StringBuffer(bytes.length * 2);
        int i;
        for (i = 0; i < bytes.length; i++) {
            if (((int) bytes[i] & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString((int) bytes[i] & 0xff, 16));
        }
        return buf.toString();
    }

    /**
	 * Turns a hex encoded string into a byte array. It is specifically meant
	 * to "reverse" the toHex(byte[]) method.
	 *
	 * @param hex a hex encoded String to transform into a byte array.
	 * @return a byte array representing the hex String[
	 */
    public static final byte[] decodeHex(String hex) {
        char[] chars = hex.toCharArray();
        byte[] bytes = new byte[chars.length / 2];
        int byteCount = 0;
        for (int i = 0; i < chars.length; i += 2) {
            int newByte = 0x00;
            newByte |= hexCharToByte(chars[i]);
            newByte <<= 4;
            newByte |= hexCharToByte(chars[i + 1]);
            bytes[byteCount] = (byte) newByte;
            byteCount++;
        }
        return bytes;
    }

    /**
	 * Returns the the byte value of a hexadecmical char (0-f). It's assumed
	 * that the hexidecimal chars are lower case as appropriate.
	 *
	 * @param ch a hexedicmal character (0-f)
	 * @return the byte value of the character (0x00-0x0F)
	 */
    private static final byte hexCharToByte(char ch) {
        switch(ch) {
            case '0':
                return 0x00;
            case '1':
                return 0x01;
            case '2':
                return 0x02;
            case '3':
                return 0x03;
            case '4':
                return 0x04;
            case '5':
                return 0x05;
            case '6':
                return 0x06;
            case '7':
                return 0x07;
            case '8':
                return 0x08;
            case '9':
                return 0x09;
            case 'a':
                return 0x0A;
            case 'b':
                return 0x0B;
            case 'c':
                return 0x0C;
            case 'd':
                return 0x0D;
            case 'e':
                return 0x0E;
            case 'f':
                return 0x0F;
        }
        return 0x00;
    }

    /**
	 * Encodes a String as a base64 String.
	 *
	 * @param data a String to encode.
	 * @return a base64 encoded String.
	 */
    public static String encodeBase64(String data) {
        byte[] bytes = null;
        try {
            bytes = data.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException uee) {
            log.error(uee);
        }
        return encodeBase64(bytes);
    }

    /**
	 * Encodes a byte array into a base64 String.
	 *
	 * @param data a byte array to encode.
	 * @return a base64 encode String.
	 */
    public static String encodeBase64(byte[] data) {
        int c;
        int len = data.length;
        StringBuffer ret = new StringBuffer(((len / 3) + 1) * 4);
        for (int i = 0; i < len; ++i) {
            c = (data[i] >> 2) & 0x3f;
            ret.append(cvt.charAt(c));
            c = (data[i] << 4) & 0x3f;
            if (++i < len) c |= (data[i] >> 4) & 0x0f;
            ret.append(cvt.charAt(c));
            if (i < len) {
                c = (data[i] << 2) & 0x3f;
                if (++i < len) c |= (data[i] >> 6) & 0x03;
                ret.append(cvt.charAt(c));
            } else {
                ++i;
                ret.append((char) fillchar);
            }
            if (i < len) {
                c = data[i] & 0x3f;
                ret.append(cvt.charAt(c));
            } else {
                ret.append((char) fillchar);
            }
        }
        return ret.toString();
    }

    /**
	 * Decodes a base64 String.
	 *
	 * @param data a base64 encoded String to decode.
	 * @return the decoded String.
	 */
    public static String decodeBase64(String data) {
        byte[] bytes = null;
        try {
            bytes = data.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException uee) {
            log.error(uee);
        }
        return decodeBase64(bytes);
    }

    /**
	 * Decodes a base64 aray of bytes.
	 *
	 * @param data a base64 encode byte array to decode.
	 * @return the decoded String.
	 */
    public static String decodeBase64(byte[] data) {
        int c, c1;
        int len = data.length;
        StringBuffer ret = new StringBuffer((len * 3) / 4);
        for (int i = 0; i < len; ++i) {
            c = cvt.indexOf(data[i]);
            ++i;
            c1 = cvt.indexOf(data[i]);
            c = ((c << 2) | ((c1 >> 4) & 0x3));
            ret.append((char) c);
            if (++i < len) {
                c = data[i];
                if (fillchar == c) break;
                c = cvt.indexOf(c);
                c1 = ((c1 << 4) & 0xf0) | ((c >> 2) & 0xf);
                ret.append((char) c1);
            }
            if (++i < len) {
                c1 = data[i];
                if (fillchar == c1) break;
                c1 = cvt.indexOf(c1);
                c = ((c << 6) & 0xc0) | c1;
                ret.append((char) c);
            }
        }
        return ret.toString();
    }

    private static final int fillchar = '=';

    private static final String cvt = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789+/";

    /**
	 * Converts a line of text into an array of lower case words using a
	 * BreakIterator.wordInstance(). <p>
	 *
	 * This method is under the Jive Open Source Software License and was
	 * written by Mark Imbriaco.
	 *
	 * @param text a String of text to convert into an array of words
	 * @return text broken up into an array of words.
	 */
    public static final String[] toLowerCaseWordArray(String text) {
        if (text == null || text.length() == 0) {
            return new String[0];
        }
        ArrayList wordList = new ArrayList();
        BreakIterator boundary = BreakIterator.getWordInstance();
        boundary.setText(text);
        int start = 0;
        for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary.next()) {
            String tmp = text.substring(start, end).trim();
            tmp = replace(tmp, "+", "");
            tmp = replace(tmp, "/", "");
            tmp = replace(tmp, "\\", "");
            tmp = replace(tmp, "#", "");
            tmp = replace(tmp, "*", "");
            tmp = replace(tmp, ")", "");
            tmp = replace(tmp, "(", "");
            tmp = replace(tmp, "&", "");
            if (tmp.length() > 0) {
                wordList.add(tmp);
            }
        }
        return (String[]) wordList.toArray(new String[wordList.size()]);
    }

    /**
	 * Pseudo-random number generator object for use with randomString().
	 * The Random class is not considered to be cryptographically secure, so
	 * only use these random Strings for low to medium security applications.
	 */
    private static Random randGen = new Random();

    /**
	 * Array of numbers and letters of mixed case. Numbers appear in the list
	 * twice so that there is a more equal chance that a number will be picked.
	 * We can use the array to get a random number or letter by picking a random
	 * array index.
	 */
    private static char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyz" + "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();

    /**
	 * Returns a random String of numbers and letters (lower and upper case)
	 * of the specified length. The method uses the Random class that is
	 * built-in to Java which is suitable for low to medium grade security uses.
	 * This means that the output is only pseudo random, i.e., each number is
	 * mathematically generated so is not truly random.<p>
	 *
	 * The specified length must be at least one. If not, the method will return
	 * null.
	 *
	 * @param length the desired length of the random String to return.
	 * @return a random String of numbers and letters of the specified length.
	 */
    public static final String randomString(int length) {
        if (length < 1) {
            return null;
        }
        char[] randBuffer = new char[length];
        for (int i = 0; i < randBuffer.length; i++) {
            randBuffer[i] = numbersAndLetters[randGen.nextInt(71)];
        }
        return new String(randBuffer);
    }

    /**
	 * Intelligently chops a String at a word boundary (whitespace) that occurs
	 * at the specified index in the argument or before. However, if there is a
	 * newline character before <code>length</code>, the String will be chopped
	 * there. If no newline or whitespace is found in <code>string</code> up to
	 * the index <code>length</code>, the String will chopped at <code>length</code>.
	 * <p>
	 * For example, chopAtWord("This is a nice String", 10) will return
	 * "This is a" which is the first word boundary less than or equal to 10
	 * characters into the original String.
	 *
	 * @param string the String to chop.
	 * @param length the index in <code>string</code> to start looking for a
	 *       whitespace boundary at.
	 * @return a substring of <code>string</code> whose length is less than or
	 *       equal to <code>length</code>, and that is chopped at whitespace.
	 */
    public static final String chopAtWord(String string, int length) {
        if (string == null || string.length() == 0) {
            return string;
        }
        char[] charArray = string.toCharArray();
        int sLength = string.length();
        if (length < sLength) {
            sLength = length;
        }
        for (int i = 0; i < sLength - 1; i++) {
            if (charArray[i] == '\r' && charArray[i + 1] == '\n') {
                return string.substring(0, i + 1);
            } else if (charArray[i] == '\n') {
                return string.substring(0, i);
            }
        }
        if (charArray[sLength - 1] == '\n') {
            return string.substring(0, sLength - 1);
        }
        if (string.length() <= length) {
            return string;
        }
        for (int i = length - 1; i > 0; i--) {
            if (charArray[i] == ' ') {
                return string.substring(0, i).trim();
            }
        }
        return string.substring(0, length);
    }

    /**
	 * Returns a substring of the given string which represents the words around the given word.
	 * For example, passing in "This is a quick test a test", "{a,test}" and 5 would return a string
	 * of "This is a quick" - that's 5 characters (or to the end of the word, whichever
	 * is greater) on either side of "a". Also, since {a,test} is passed in a "a" is found
	 * first in the string, we base the substring off of the position of "a". The wordList is
	 * really just a list of strings to try - the first one found is used.<p>
	 *
	 * Note: The wordList passed in should be lowercase.
	 *
	 * @param input The string to parse.
	 * @param wordList The words to look for - the first one found in the string is used.
	 * @param numChars The number of characters on either side to include in the chop.
	 * @return a substring of the given string matching the criteria, otherwise null.
	 */
    public static String chopAtWordsAround(String input, String[] wordList, int numChars) {
        if (input == null || "".equals(input.trim()) || wordList == null || wordList.length == 0 || numChars == 0) {
            return null;
        }
        String lc = input.toLowerCase();
        for (int i = 0; i < wordList.length; i++) {
            int pos = lc.indexOf(wordList[i]);
            if (pos > -1) {
                int beginIdx = pos - numChars;
                if (beginIdx < 0) {
                    beginIdx = 0;
                }
                int endIdx = pos + numChars;
                if (endIdx > input.length() - 1) {
                    endIdx = input.length() - 1;
                }
                char[] chars = input.toCharArray();
                while (beginIdx > 0 && chars[beginIdx] != ' ' && chars[beginIdx] != '\n' && chars[beginIdx] != '\r') {
                    beginIdx--;
                }
                while (endIdx < input.length() && chars[endIdx] != ' ' && chars[endIdx] != '\n' && chars[endIdx] != '\r') {
                    endIdx++;
                }
                return input.substring(beginIdx, endIdx);
            }
        }
        return input.substring(0, (input.length() >= 200) ? 200 : input.length());
    }

    /**
	 * Reformats a string where lines that are longer than <tt>width</tt>
	 * are split apart at the earliest wordbreak or at maxLength, whichever is
	 * sooner. If the width specified is less than 5 or greater than the input
	 * Strings length the string will be returned as is.
	 * <p>
	 * Please note that this method can be lossy - trailing spaces on wrapped
	 * lines may be trimmed.
	 *
	 * @param input the String to reformat.
	 * @param width the maximum length of any one line.
	 * @return a new String with reformatted as needed.
	 */
    public static String wordWrap(String input, int width, Locale locale) {
        if (input == null) {
            return "";
        } else if (width < 5) {
            return input;
        } else if (width >= input.length()) {
            return input;
        }
        if (locale == null) {
        }
        StringBuffer buf = new StringBuffer(input);
        boolean endOfLine = false;
        int lineStart = 0;
        for (int i = 0; i < buf.length(); i++) {
            if (buf.charAt(i) == '\n') {
                lineStart = i + 1;
                endOfLine = true;
            }
            if (i > lineStart + width - 1) {
                if (!endOfLine) {
                    int limit = i - lineStart - 1;
                    BreakIterator breaks = BreakIterator.getLineInstance(locale);
                    breaks.setText(buf.substring(lineStart, i));
                    int end = breaks.last();
                    if (end == limit + 1) {
                        if (!Character.isWhitespace(buf.charAt(lineStart + end))) {
                            end = breaks.preceding(end - 1);
                        }
                    }
                    if (end != BreakIterator.DONE && end == limit + 1) {
                        buf.replace(lineStart + end, lineStart + end + 1, "\n");
                        lineStart = lineStart + end;
                    } else if (end != BreakIterator.DONE && end != 0) {
                        buf.insert(lineStart + end, '\n');
                        lineStart = lineStart + end + 1;
                    } else {
                        buf.insert(i, '\n');
                        lineStart = i + 1;
                    }
                } else {
                    buf.insert(i, '\n');
                    lineStart = i + 1;
                    endOfLine = false;
                }
            }
        }
        return buf.toString();
    }

    /**
	 * Escapes all necessary characters in the String so that it can be used
	 * in an XML doc.
	 *
	 * @param string the string to escape.
	 * @return the string with appropriate characters escaped.
	 */
    public static final String escapeForXML(String string) {
        if (string == null) {
            return null;
        }
        char ch;
        int i = 0;
        int last = 0;
        char[] input = string.toCharArray();
        int len = input.length;
        StringBuffer out = new StringBuffer((int) (len * 1.3));
        for (; i < len; i++) {
            ch = input[i];
            if (ch > '>') {
                continue;
            } else if (ch == '<') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(LT_ENCODE);
            } else if (ch == '&') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(AMP_ENCODE);
            } else if (ch == '"') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(QUOTE_ENCODE);
            } else if (ch == 10 || ch == 13 || ch == 9) {
                continue;
            } else if (ch < 32) {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
            }
        }
        if (last == 0) {
            return string;
        }
        if (i > last) {
            out.append(input, last, i - last);
        }
        return out.toString();
    }

    /**
	 * Unescapes the String by converting XML escape sequences back into normal
	 * characters.
	 *
	 * @param string the string to unescape.
	 * @return the string with appropriate characters unescaped.
	 */
    public static final String unescapeFromXML(String string) {
        string = replace(string, "&lt;", "<");
        string = replace(string, "&gt;", ">");
        string = replace(string, "&quot;", "\"");
        return replace(string, "&amp;", "&");
    }

    private static final char[] zeroArray = "0000000000000000000000000000000000000000000000000000000000000000".toCharArray();

    /**
	 * Pads the supplied String with 0's to the specified length and returns
	 * the result as a new String. For example, if the initial String is
	 * "9999" and the desired length is 8, the result would be "00009999".
	 * This type of padding is useful for creating numerical values that need
	 * to be stored and sorted as character data. Note: the current
	 * implementation of this method allows for a maximum <tt>length</tt> of
	 * 64.
	 *
	 * @param string the original String to pad.
	 * @param length the desired length of the new padded String.
	 * @return a new String padded with the required number of 0's.
	 */
    public static final String zeroPadString(String string, int length) {
        if (string == null || string.length() > length) {
            return string;
        }
        StringBuffer buf = new StringBuffer(length);
        buf.append(zeroArray, 0, length - string.length()).append(string);
        return buf.toString();
    }

    /**
	 * Formats a Date as a fifteen character long String made up of the Date's
	 * padded millisecond value.
	 *
	 * @return a Date encoded as a String.
	 */
    public static final String dateToMillis(Date date) {
        return Long.toString(date.getTime());
    }

    /**String[] to String
	 * eg: {"liu" ,"xian" ,"feng" } -> "liu,xian,feng"
	 * */
    public static String Strings2String(String[] strings) {
        String tempString = "";
        for (int i = 0; i < strings.length; i++) tempString = tempString + strings[i] + ",";
        tempString = tempString.substring(0, tempString.length() - 1);
        return tempString;
    }

    public static Long dateString2Long(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date thisDate = null;
        try {
            thisDate = format.parse(date);
        } catch (ParseException e) {
            return null;
        }
        return new Long(thisDate.getTime());
    }

    public static Long dateTimeString2Long(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date thisDate = null;
        try {
            thisDate = format.parse(date);
        } catch (ParseException e) {
            return null;
        }
        return new Long(thisDate.getTime());
    }

    public static String long2DateString(Long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        if (time != null) {
            if (time.longValue() != 0) {
                return format.format(new Date(time.longValue()));
            }
        }
        return null;
    }

    public static String long2TimeString(Long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (time != null) {
            if (time.longValue() != 0) {
                return format.format(new Date(time.longValue()));
            }
        }
        return null;
    }

    public static Integer getDayOfMonth() {
        SimpleDateFormat sf = new SimpleDateFormat("dd");
        return Integer.valueOf(sf.format(new Date()));
    }

    public static Date string2Date(String s) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            return formatter.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String filterStr(String str, String filterStr) {
        if (str == null) return null;
        String newStr = "";
        char[] filterChars = filterStr.toCharArray();
        for (int i = 0; i < str.length(); i++) {
            char thisChar = str.charAt(i);
            if (filterStr.indexOf(thisChar) < 0) {
                newStr = newStr + thisChar;
            }
        }
        return newStr;
    }

    public static String filterStr(String str) {
        return filterStr(str, getFilterChars());
    }

    public static String getFilterChars() {
        return "'%+/\\" + '"';
    }
}
