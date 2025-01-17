package org.jivesoftware.spark.util;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to peform common String manipulation algorithms.
 */
public class StringUtils {

    private static final char[] QUOTE_ENCODE = "&quot;".toCharArray();

    private static final char[] AMP_ENCODE = "&amp;".toCharArray();

    private static final char[] LT_ENCODE = "&lt;".toCharArray();

    private static final char[] GT_ENCODE = "&gt;".toCharArray();

    private static Pattern basicAddressPattern;

    private static Pattern validUserPattern;

    private static Pattern domainPattern;

    private static Pattern ipDomainPattern;

    private static Pattern tldPattern;

    static {
        String basicAddress = "^([\\w\\.-]+)@([\\w\\.-]+)$";
        String specialChars = "\\(\\)><@,;:\\\\\\\"\\.\\[\\]";
        String validChars = "[^ \f\n\r\t" + specialChars + "]";
        String atom = validChars + "+";
        String quotedUser = "(\"[^\"]+\")";
        String word = "(" + atom + "|" + quotedUser + ")";
        String validUser = "^" + word + "(\\." + word + ")*$";
        String domain = "^" + atom + "(\\." + atom + ")+$";
        String ipDomain = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";
        String knownTLDs = "^\\.(com|net|org|edu|int|mil|gov|arpa|biz|aero|name|coop|info|pro|museum)$";
        basicAddressPattern = Pattern.compile(basicAddress, Pattern.CASE_INSENSITIVE);
        validUserPattern = Pattern.compile(validUser, Pattern.CASE_INSENSITIVE);
        domainPattern = Pattern.compile(domain, Pattern.CASE_INSENSITIVE);
        ipDomainPattern = Pattern.compile(ipDomain, Pattern.CASE_INSENSITIVE);
        tldPattern = Pattern.compile(knownTLDs, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Replaces all instances of oldString with newString in string.
     *
     * @param string    the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     * @return a String will all instances of oldString replaced by newString
     */
    public static String replace(String string, String oldString, String newString) {
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
     * <p/>
     * added feature that matches of newString in oldString ignore case.
     *
     * @param line      the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     * @return a String will all instances of oldString replaced by newString
     */
    public static String replaceIgnoreCase(String line, String oldString, String newString) {
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
     * <p/>
     * added feature that matches of newString in oldString ignore case.
     * <p/>
     * The count paramater is set to the number of replaces performed.
     *
     * @param line      the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     * @param count     a value that will be updated with the number of replaces
     *                  <p/>
     *                  performed.
     * @return a String will all instances of oldString replaced by newString
     */
    public static String replaceIgnoreCase(String line, String oldString, String newString, int[] count) {
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

    /**
     * Replaces all instances of oldString with newString in line.
     * <p/>
     * The count Integer is updated with number of replaces.
     *
     * @param line      the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     * @param count     Number of replaces.
     * @return a String will all instances of oldString replaced by newString
     */
    public static String replace(String line, String oldString, String newString, int[] count) {
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
     * <p/>
     * the tag body intact.
     *
     * @param in the text to be converted.
     * @return the input string with all tags removed.
     */
    public static String stripTags(String in) {
        if (in == null) {
            return null;
        }
        return stripTags(in, false);
    }

    /**
     * This method takes a string and strips out all tags while still leaving
     * <p/>
     * the tag body intact.
     *
     * @param in the text to be converted.
     * @param stripBRTag Remove BR tags.
     * @return the input string with all tags removed.
     */
    public static String stripTags(String in, boolean stripBRTag) {
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
            } else if (ch == '<') {
                if (!stripBRTag && i + 3 < len && input[i + 1] == 'b' && input[i + 2] == 'r' && input[i + 3] == '>') {
                    i += 3;
                    continue;
                }
                if (i > last) {
                    if (last > 0) {
                        out.append(" ");
                    }
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
     * <p/>
     * &lt;table&gt;, etc) and converts the '&lt'' and '&gt;' characters to
     * <p/>
     * their HTML escape sequences.
     *
     * @param in the text to be converted.
     * @return the input string with the characters '&lt;' and '&gt;' replaced
     *         <p/>
     *         with their HTML escape sequences.
     */
    public static String escapeHTMLTags(String in) {
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
            } else if (ch == '"') {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                last = i + 1;
                out.append(QUOTE_ENCODE);
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
     * <p/>
     * String of hexadecimal numbers. This method is synchronized to avoid
     * <p/>
     * excessive MessageDigest object creation. If calling this method becomes
     * <p/>
     * a bottleneck in your code, you may wish to maintain a pool of
     * <p/>
     * MessageDigest objects instead of using this method.
     * <p/>
     * <p/>
     * <p/>
     * A hash is a one-way function -- that is, given an
     * <p/>
     * input, an output is easily computed. However, given the output, the
     * <p/>
     * input is almost impossible to compute. This is useful for passwords
     * <p/>
     * since we can store the hash and a hacker will then have a very hard time
     * <p/>
     * determining the original password.
     * <p/>
     * <p/>
     * <p/>
     * In Jive, every time a user logs in, we simply
     * <p/>
     * take their plain text password, compute the hash, and compare the
     * <p/>
     * generated hash to the stored hash. Since it is almost impossible that
     * <p/>
     * two passwords will generate the same hash, we know if the user gave us
     * <p/>
     * the correct password or not. The only negative to this system is that
     * <p/>
     * password recovery is basically impossible. Therefore, a reset password
     * <p/>
     * method is used instead.
     *
     * @param data the String to compute the hash of.
     * @return a hashed version of the passed-in String
     */
    public static synchronized String hash(String data) {
        if (digest == null) {
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException nsae) {
            }
        }
        try {
            digest.update(data.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
        }
        return encodeHex(digest.digest());
    }

    public static synchronized String hash(byte[] data) {
        if (digest == null) {
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException nsae) {
            }
        }
        digest.update(data);
        return encodeHex(digest.digest());
    }

    /**
     * Turns an array of bytes into a String representing each byte as an
     * <p/>
     * unsigned hex number.
     * <p/>
     * <p/>
     * <p/>
     * Method by Santeri Paavolainen, Helsinki Finland 1996<br>
     * <p/>
     * (c) Santeri Paavolainen, Helsinki Finland 1996<br>
     * <p/>
     * Distributed under LGPL.
     *
     * @param bytes an array of bytes to convert to a hex-string
     * @return generated hex string
     */
    public static String encodeHex(byte[] bytes) {
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
     * <p/>
     * to "reverse" the toHex(byte[]) method.
     *
     * @param hex a hex encoded String to transform into a byte array.
     * @return a byte array representing the hex String[
     */
    public static byte[] decodeHex(String hex) {
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
     * <p/>
     * that the hexidecimal chars are lower case as appropriate.
     *
     * @param ch a hexedicmal character (0-f)
     * @return the byte value of the character (0x00-0x0F)
     */
    private static byte hexCharToByte(char ch) {
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

    /**
     * The method below is under the following license
     * <p/>
     * <p/>
     * <p/>
     * ====================================================================
     * <p/>
     * <p/>
     * <p/>
     * The Apache Software License, Version 1.1
     * <p/>
     * <p/>
     * <p/>
     * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
     * <p/>
     * reserved.
     * <p/>
     * <p/>
     * <p/>
     * Redistribution and use in source and binary forms, with or without
     * <p/>
     * modification, are permitted provided that the following conditions
     * <p/>
     * are met:
     * <p/>
     * <p/>
     * <p/>
     * 1. Redistributions of source code must retain the above copyright
     * <p/>
     * notice, this list of conditions and the following disclaimer.
     * <p/>
     * <p/>
     * <p/>
     * 2. Redistributions in binary form must reproduce the above copyright
     * <p/>
     * notice, this list of conditions and the following disclaimer in
     * <p/>
     * the documentation and/or other materials provided with the
     * <p/>
     * distribution.
     * <p/>
     * <p/>
     * <p/>
     * 3. The end-user documentation included with the redistribution, if
     * <p/>
     * any, must include the following acknowlegement:
     * <p/>
     * "This product includes software developed by the
     * <p/>
     * Apache Software Foundation (http://www.apache.org/)."
     * <p/>
     * Alternately, this acknowlegement may appear in the software itself,
     * <p/>
     * if and wherever such third-party acknowlegements normally appear.
     * <p/>
     * <p/>
     * <p/>
     * 4. The names "The Jakarta Project", "Commons", and "Apache Software
     * <p/>
     * Foundation" must not be used to endorse or promote products derived
     * <p/>
     * from this software without prior written permission. For written
     * <p/>
     * permission, please contact apache@apache.org.
     * <p/>
     * <p/>
     * <p/>
     * 5. Products derived from this software may not be called "Apache"
     * <p/>
     * nor may "Apache" appear in their names without prior written
     * <p/>
     * permission of the Apache Group.
     * <p/>
     * <p/>
     * <p/>
     * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
     * <p/>
     * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
     * <p/>
     * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
     * <p/>
     * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
     * <p/>
     * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
     * <p/>
     * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
     * <p/>
     * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
     * <p/>
     * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
     * <p/>
     * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
     * <p/>
     * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
     * <p/>
     * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
     * <p/>
     * SUCH DAMAGE.
     * <p/>
     * ====================================================================
     * <p/>
     * <p/>
     * <p/>
     * This software consists of voluntary contributions made by many
     * <p/>
     * individuals on behalf of the Apache Software Foundation.  For more
     * <p/>
     * information on the Apache Software Foundation, please see
     * <p/>
     * <http://www.apache.org/>.
     */
    private static final BitSet allowed_query = new BitSet(256);

    static {
        for (int i = '0'; i <= '9'; i++) {
            allowed_query.set(i);
        }
        for (int i = 'a'; i <= 'z'; i++) {
            allowed_query.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            allowed_query.set(i);
        }
        allowed_query.set('-');
        allowed_query.set('_');
        allowed_query.set('.');
        allowed_query.set('!');
        allowed_query.set('~');
        allowed_query.set('*');
        allowed_query.set('\'');
        allowed_query.set('(');
        allowed_query.set(')');
    }

    /**
     * Encodes URI string. This is a replacement for the java.net.URLEncode#encode(String, String)
     * <p/>
     * class which is broken under JDK 1.3.
     * <p/>
     * <p/>
     *
     * @param original the original character sequence
     * @param charset  the protocol charset
     * @return URI character sequence
     * @throws UnsupportedEncodingException unsupported character encoding
     */
    public static String URLEncode(String original, String charset) throws UnsupportedEncodingException {
        if (original == null) {
            return null;
        }
        byte[] octets;
        try {
            octets = original.getBytes(charset);
        } catch (UnsupportedEncodingException error) {
            throw new UnsupportedEncodingException();
        }
        StringBuffer buf = new StringBuffer(octets.length);
        for (byte octet : octets) {
            char c = (char) octet;
            if (allowed_query.get(c)) {
                buf.append(c);
            } else {
                buf.append('%');
                char hexadecimal = Character.forDigit((octet >> 4) & 0xF, 16);
                buf.append(Character.toUpperCase(hexadecimal));
                hexadecimal = Character.forDigit(octet & 0xF, 16);
                buf.append(Character.toUpperCase(hexadecimal));
            }
        }
        return buf.toString();
    }

    private static final int fillchar = '=';

    private static final String cvt = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789+/";

    /**
     * Converts a line of text into an array of lower case words using a
     * <p/>
     * BreakIterator.wordInstance(). <p>
     * <p/>
     * <p/>
     * <p/>
     * This method is under the Jive Open Source Software License and was
     * <p/>
     * written by Mark Imbriaco.
     *
     * @param text a String of text to convert into an array of words
     * @return text broken up into an array of words.
     */
    public static String[] toLowerCaseWordArray(String text) {
        if (text == null || text.length() == 0) {
            return new String[0];
        }
        ArrayList<String> wordList = new ArrayList<String>();
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
        return wordList.toArray(new String[wordList.size()]);
    }

    /**
     * Pseudo-random number generator object for use with randomString().
     * <p/>
     * The Random class is not considered to be cryptographically secure, so
     * <p/>
     * only use these random Strings for low to medium security applications.
     */
    private static Random randGen = new Random();

    /**
     * Array of numbers and letters of mixed case. Numbers appear in the list
     * <p/>
     * twice so that there is a more equal chance that a number will be picked.
     * <p/>
     * We can use the array to get a random number or letter by picking a random
     * <p/>
     * array index.
     */
    private static char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyz" + "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();

    /**
     * Returns a random String of numbers and letters (lower and upper case)
     * <p/>
     * of the specified length. The method uses the Random class that is
     * <p/>
     * built-in to Java which is suitable for low to medium grade security uses.
     * <p/>
     * This means that the output is only pseudo random, i.e., each number is
     * <p/>
     * mathematically generated so is not truly random.<p>
     * <p/>
     * <p/>
     * <p/>
     * The specified length must be at least one. If not, the method will return
     * <p/>
     * null.
     *
     * @param length the desired length of the random String to return.
     * @return a random String of numbers and letters of the specified length.
     */
    public static String randomString(int length) {
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
     * <p/>
     * at the specified index in the argument or before. However, if there is a
     * <p/>
     * newline character before <code>length</code>, the String will be chopped
     * <p/>
     * there. If no newline or whitespace is found in <code>string</code> up to
     * <p/>
     * the index <code>length</code>, the String will chopped at <code>length</code>.
     * <p/>
     * <p/>
     * <p/>
     * For example, chopAtWord("This is a nice String", 10, -1) will return
     * <p/>
     * "This is a" which is the first word boundary less than or equal to 10
     * <p/>
     * characters into the original String.
     *
     * @param string    the String to chop.
     * @param length    the index in <code>string</code> to start looking for a
     *                  <p/>
     *                  whitespace boundary at.
     * @param minLength the minimum length the word should be chopped at. This is helpful
     *                  <p/>
     *                  for words with no natural boundaries, ie: "thisisareallylonglonglongword".
     *                  <p/>
     *                  This must be smaller than length and can be -1 if no minLength is wanted
     * @return a substring of <code>string</code> whose length is less than or
     *         <p/>
     *         equal to <code>length</code>, and that is chopped at whitespace.
     */
    public static String chopAtWord(String string, int length, int minLength) {
        if (length < 2) {
            throw new IllegalArgumentException("Length specified (" + length + ") must be > 2");
        } else if (minLength >= length) {
            throw new IllegalArgumentException("minLength must be smaller than length");
        }
        int sLength = (string == null) ? -1 : string.length();
        if (sLength < 1) {
            return string;
        } else if (minLength != -1 && sLength < minLength) {
            return string;
        } else if (minLength == -1 && sLength < length) {
            return string;
        }
        if (string == null) return null;
        char[] charArray = string.toCharArray();
        if (sLength > length) {
            sLength = length;
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
            for (int i = sLength - 1; i > 0; i--) {
                if (charArray[i] == ' ') {
                    return string.substring(0, i).trim();
                }
            }
        } else if (minLength != -1 && sLength > minLength) {
            for (int i = 0; i < minLength; i++) {
                if (charArray[i] == ' ') {
                    return string;
                }
            }
        }
        if (minLength > -1 && minLength <= string.length()) {
            return string.substring(0, minLength);
        }
        return string.substring(0, length);
    }

    /**
     * Intelligently chops a String at a word boundary (whitespace) that occurs
     * <p/>
     * at the specified index in the argument or before. However, if there is a
     * <p/>
     * newline character before <code>length</code>, the String will be chopped
     * <p/>
     * there. If no newline or whitespace is found in <code>string</code> up to
     * <p/>
     * the index <code>length</code>, the String will chopped at <code>length</code>.
     * <p/>
     * <p/>
     * <p/>
     * For example, chopAtWord("This is a nice String", 10) will return
     * <p/>
     * "This is a" which is the first word boundary less than or equal to 10
     * <p/>
     * characters into the original String.
     *
     * @param string the String to chop.
     * @param length the index in <code>string</code> to start looking for a
     *               <p/>
     *               whitespace boundary at.
     * @return a substring of <code>string</code> whose length is less than or
     *         <p/>
     *         equal to <code>length</code>, and that is chopped at whitespace.
     */
    public static String chopAtWord(String string, int length) {
        return chopAtWord(string, length, -1);
    }

    /**
     * Returns a substring of the given string which represents the words around the given word.
     * <p/>
     * For example, passing in "This is a quick test a test", "{a,test}" and 5 would return a string
     * <p/>
     * of "This is a quick" - that's 5 characters (or to the end of the word, whichever
     * <p/>
     * is greater) on either side of "a". Also, since {a,test} is passed in a "a" is found
     * <p/>
     * first in the string, we base the substring off of the position of "a". The wordList is
     * <p/>
     * really just a list of strings to try - the first one found is used.<p>
     * <p/>
     * <p/>
     * <p/>
     * Note: The wordList passed in should be lowercase.
     *
     * @param input    The string to parse.
     * @param wordList The words to look for - the first one found in the string is used.
     * @param numChars The number of characters on either side to include in the chop.
     * @return a substring of the given string matching the criteria, otherwise "".
     */
    public static String chopAtWordsAround(String input, String[] wordList, int numChars) {
        if (input == null || "".equals(input.trim()) || wordList == null || wordList.length == 0 || numChars == 0) {
            return "";
        }
        String lc = input.toLowerCase();
        for (String aWordList : wordList) {
            int pos = lc.indexOf(aWordList);
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
     * <p/>
     * are split apart at the earliest wordbreak or at maxLength, whichever is
     * <p/>
     * sooner. If the width specified is less than 5 or greater than the input
     * <p/>
     * Strings length the string will be returned as is.
     * <p/>
     * <p/>
     * <p/>
     * Please note that this method can be lossy - trailing spaces on wrapped
     * <p/>
     * lines may be trimmed.
     *
     * @param input the String to reformat.
     * @param width the maximum length of any one line.
     * @param locale of the string to be wrapped.
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
     * Highlights words in a string. Words matching ignores case. The actual
     * <p/>
     * higlighting method is specified with the start and end higlight tags.
     * <p/>
     * Those might be beginning and ending HTML bold tags, or anything else.<p>
     * <p/>
     * <p/>
     * <p/>
     * This method is under the Jive Open Source Software License and was
     * <p/>
     * written by Mark Imbriaco.
     *
     * @param string         the String to highlight words in.
     * @param words          an array of words that should be highlighted in the string.
     * @param startHighlight the tag that should be inserted to start highlighting.
     * @param endHighlight   the tag that should be inserted to end highlighting.
     * @return a new String with the specified words highlighted.
     */
    public static String highlightWords(String string, String[] words, String startHighlight, String endHighlight) {
        if (string == null || words == null || startHighlight == null || endHighlight == null) {
            return null;
        }
        StringBuffer regexp = new StringBuffer();
        regexp.append("(?i)\\b(");
        for (int x = 0; x < words.length; x++) {
            words[x] = words[x].replaceAll("([\\$\\?\\|\\/\\.])", "\\\\$1");
            regexp.append(words[x]);
            if (x != words.length - 1) {
                regexp.append("|");
            }
        }
        regexp.append(")");
        return string.replaceAll(regexp.toString(), startHighlight + "$1" + endHighlight);
    }

    /**
     * Escapes all necessary characters in the String so that it can be used
     * <p/>
     * in an XML doc.
     *
     * @param string the string to escape.
     * @return the string with appropriate characters escaped.
     */
    public static String escapeForXML(String string) {
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
     * <p/>
     * characters.
     *
     * @param string the string to unescape.
     * @return the string with appropriate characters unescaped.
     */
    public static String unescapeFromXML(String string) {
        string = replace(string, "&lt;", "<");
        string = replace(string, "&gt;", ">");
        string = replace(string, "&quot;", "\"");
        return replace(string, "&amp;", "&");
    }

    private static final char[] zeroArray = "0000000000000000000000000000000000000000000000000000000000000000".toCharArray();

    /**
     * Pads the supplied String with 0's to the specified length and returns
     * <p/>
     * the result as a new String. For example, if the initial String is
     * <p/>
     * "9999" and the desired length is 8, the result would be "00009999".
     * <p/>
     * This type of padding is useful for creating numerical values that need
     * <p/>
     * to be stored and sorted as character data. Note: the current
     * <p/>
     * implementation of this method allows for a maximum <tt>length</tt> of
     * <p/>
     * 64.
     *
     * @param string the original String to pad.
     * @param length the desired length of the new padded String.
     * @return a new String padded with the required number of 0's.
     */
    public static String zeroPadString(String string, int length) {
        if (string == null || string.length() > length) {
            return string;
        }
        StringBuffer buf = new StringBuffer(length);
        buf.append(zeroArray, 0, length - string.length()).append(string);
        return buf.toString();
    }

    /**
     * Formats a Date as a String. Depending on how Dates are defined in the database
     * <p/>
     * (character data or numberic), the format return will either be a fifteen character long String
     * <p/>
     * made up of the Date's padded millisecond value, or will simply be the Date's millesecond value.
     *
     * @param date Date to convert to milliseconds.
     * @return a Date encoded as a String.
     */
    public static String dateToMillis(Date date) {
        return Long.toString(date.getTime());
    }

    /**
     * Validate an email address. This isn't 100% perfect but should handle just about everything
     * <p/>
     * that is in common use.
     *
     * @param addr the email address to validate
     * @return true if the address is valid, false otherwise
     */
    public static boolean isValidEmailAddress(String addr) {
        if (addr == null) {
            return false;
        }
        addr = addr.trim();
        if (addr.length() == 0) {
            return false;
        }
        Matcher matcher = basicAddressPattern.matcher(addr);
        if (!matcher.matches()) {
            return false;
        }
        String userPart = matcher.group(1);
        String domainPart = matcher.group(2);
        matcher = validUserPattern.matcher(userPart);
        if (!matcher.matches()) {
            return false;
        }
        matcher = ipDomainPattern.matcher(domainPart);
        if (matcher.matches()) {
            for (int i = 1; i < 5; i++) {
                String num = matcher.group(i);
                if (num == null) {
                    return false;
                }
                if (Integer.parseInt(num) > 254) {
                    return false;
                }
            }
            return true;
        }
        matcher = domainPattern.matcher(domainPart);
        if (matcher.matches()) {
            String tld = matcher.group(matcher.groupCount());
            matcher = tldPattern.matcher(tld);
            if (tld.length() != 3 && !matcher.matches()) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public static String keyStroke2String(KeyStroke key) {
        StringBuffer s = new StringBuffer(50);
        int m = key.getModifiers();
        if ((m & (InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK)) != 0) {
            s.append("shift ");
        }
        if ((m & (InputEvent.CTRL_DOWN_MASK | InputEvent.CTRL_MASK)) != 0) {
            s.append("ctrl ");
        }
        if ((m & (InputEvent.META_DOWN_MASK | InputEvent.META_MASK)) != 0) {
            s.append("meta ");
        }
        if ((m & (InputEvent.ALT_DOWN_MASK | InputEvent.ALT_MASK)) != 0) {
            s.append("alt ");
        }
        if ((m & (InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON1_MASK)) != 0) {
            s.append("button1 ");
        }
        if ((m & (InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON2_MASK)) != 0) {
            s.append("button2 ");
        }
        if ((m & (InputEvent.BUTTON3_DOWN_MASK | InputEvent.BUTTON3_MASK)) != 0) {
            s.append("button3 ");
        }
        switch(key.getKeyEventType()) {
            case KeyEvent.KEY_TYPED:
                s.append("typed ");
                s.append(key.getKeyChar()).append(" ");
                break;
            case KeyEvent.KEY_PRESSED:
                s.append("pressed ");
                s.append(getKeyText(key.getKeyCode())).append(" ");
                break;
            case KeyEvent.KEY_RELEASED:
                s.append("released ");
                s.append(getKeyText(key.getKeyCode())).append(" ");
                break;
            default:
                s.append("unknown-event-type ");
                break;
        }
        return s.toString();
    }

    public static String getKeyText(int keyCode) {
        if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9 || keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
            return String.valueOf((char) keyCode);
        }
        switch(keyCode) {
            case KeyEvent.VK_COMMA:
                return "COMMA";
            case KeyEvent.VK_PERIOD:
                return "PERIOD";
            case KeyEvent.VK_SLASH:
                return "SLASH";
            case KeyEvent.VK_SEMICOLON:
                return "SEMICOLON";
            case KeyEvent.VK_EQUALS:
                return "EQUALS";
            case KeyEvent.VK_OPEN_BRACKET:
                return "OPEN_BRACKET";
            case KeyEvent.VK_BACK_SLASH:
                return "BACK_SLASH";
            case KeyEvent.VK_CLOSE_BRACKET:
                return "CLOSE_BRACKET";
            case KeyEvent.VK_ENTER:
                return "ENTER";
            case KeyEvent.VK_BACK_SPACE:
                return "BACK_SPACE";
            case KeyEvent.VK_TAB:
                return "TAB";
            case KeyEvent.VK_CANCEL:
                return "CANCEL";
            case KeyEvent.VK_CLEAR:
                return "CLEAR";
            case KeyEvent.VK_SHIFT:
                return "SHIFT";
            case KeyEvent.VK_CONTROL:
                return "CONTROL";
            case KeyEvent.VK_ALT:
                return "ALT";
            case KeyEvent.VK_PAUSE:
                return "PAUSE";
            case KeyEvent.VK_CAPS_LOCK:
                return "CAPS_LOCK";
            case KeyEvent.VK_ESCAPE:
                return "ESCAPE";
            case KeyEvent.VK_SPACE:
                return "SPACE";
            case KeyEvent.VK_PAGE_UP:
                return "PAGE_UP";
            case KeyEvent.VK_PAGE_DOWN:
                return "PAGE_DOWN";
            case KeyEvent.VK_END:
                return "END";
            case KeyEvent.VK_HOME:
                return "HOME";
            case KeyEvent.VK_LEFT:
                return "LEFT";
            case KeyEvent.VK_UP:
                return "UP";
            case KeyEvent.VK_RIGHT:
                return "RIGHT";
            case KeyEvent.VK_DOWN:
                return "DOWN";
            case KeyEvent.VK_MULTIPLY:
                return "MULTIPLY";
            case KeyEvent.VK_ADD:
                return "ADD";
            case KeyEvent.VK_SEPARATOR:
                return "SEPARATOR";
            case KeyEvent.VK_SUBTRACT:
                return "SUBTRACT";
            case KeyEvent.VK_DECIMAL:
                return "DECIMAL";
            case KeyEvent.VK_DIVIDE:
                return "DIVIDE";
            case KeyEvent.VK_DELETE:
                return "DELETE";
            case KeyEvent.VK_NUM_LOCK:
                return "NUM_LOCK";
            case KeyEvent.VK_SCROLL_LOCK:
                return "SCROLL_LOCK";
            case KeyEvent.VK_F1:
                return "F1";
            case KeyEvent.VK_F2:
                return "F2";
            case KeyEvent.VK_F3:
                return "F3";
            case KeyEvent.VK_F4:
                return "F4";
            case KeyEvent.VK_F5:
                return "F5";
            case KeyEvent.VK_F6:
                return "F6";
            case KeyEvent.VK_F7:
                return "F7";
            case KeyEvent.VK_F8:
                return "F8";
            case KeyEvent.VK_F9:
                return "F9";
            case KeyEvent.VK_F10:
                return "F10";
            case KeyEvent.VK_F11:
                return "F11";
            case KeyEvent.VK_F12:
                return "F12";
            case KeyEvent.VK_F13:
                return "F13";
            case KeyEvent.VK_F14:
                return "F14";
            case KeyEvent.VK_F15:
                return "F15";
            case KeyEvent.VK_F16:
                return "F16";
            case KeyEvent.VK_F17:
                return "F17";
            case KeyEvent.VK_F18:
                return "F18";
            case KeyEvent.VK_F19:
                return "F19";
            case KeyEvent.VK_F20:
                return "F20";
            case KeyEvent.VK_F21:
                return "F21";
            case KeyEvent.VK_F22:
                return "F22";
            case KeyEvent.VK_F23:
                return "F23";
            case KeyEvent.VK_F24:
                return "F24";
            case KeyEvent.VK_PRINTSCREEN:
                return "PRINTSCREEN";
            case KeyEvent.VK_INSERT:
                return "INSERT";
            case KeyEvent.VK_HELP:
                return "HELP";
            case KeyEvent.VK_META:
                return "META";
            case KeyEvent.VK_BACK_QUOTE:
                return "BACK_QUOTE";
            case KeyEvent.VK_QUOTE:
                return "QUOTE";
            case KeyEvent.VK_KP_UP:
                return "KP_UP";
            case KeyEvent.VK_KP_DOWN:
                return "KP_DOWN";
            case KeyEvent.VK_KP_LEFT:
                return "KP_LEFT";
            case KeyEvent.VK_KP_RIGHT:
                return "KP_RIGHT";
            case KeyEvent.VK_DEAD_GRAVE:
                return "DEAD_GRAVE";
            case KeyEvent.VK_DEAD_ACUTE:
                return "DEAD_ACUTE";
            case KeyEvent.VK_DEAD_CIRCUMFLEX:
                return "DEAD_CIRCUMFLEX";
            case KeyEvent.VK_DEAD_TILDE:
                return "DEAD_TILDE";
            case KeyEvent.VK_DEAD_MACRON:
                return "DEAD_MACRON";
            case KeyEvent.VK_DEAD_BREVE:
                return "DEAD_BREVE";
            case KeyEvent.VK_DEAD_ABOVEDOT:
                return "DEAD_ABOVEDOT";
            case KeyEvent.VK_DEAD_DIAERESIS:
                return "DEAD_DIAERESIS";
            case KeyEvent.VK_DEAD_ABOVERING:
                return "DEAD_ABOVERING";
            case KeyEvent.VK_DEAD_DOUBLEACUTE:
                return "DEAD_DOUBLEACUTE";
            case KeyEvent.VK_DEAD_CARON:
                return "DEAD_CARON";
            case KeyEvent.VK_DEAD_CEDILLA:
                return "DEAD_CEDILLA";
            case KeyEvent.VK_DEAD_OGONEK:
                return "DEAD_OGONEK";
            case KeyEvent.VK_DEAD_IOTA:
                return "DEAD_IOTA";
            case KeyEvent.VK_DEAD_VOICED_SOUND:
                return "DEAD_VOICED_SOUND";
            case KeyEvent.VK_DEAD_SEMIVOICED_SOUND:
                return "DEAD_SEMIVOICED_SOUND";
            case KeyEvent.VK_AMPERSAND:
                return "AMPERSAND";
            case KeyEvent.VK_ASTERISK:
                return "ASTERISK";
            case KeyEvent.VK_QUOTEDBL:
                return "QUOTEDBL";
            case KeyEvent.VK_LESS:
                return "LESS";
            case KeyEvent.VK_GREATER:
                return "GREATER";
            case KeyEvent.VK_BRACELEFT:
                return "BRACELEFT";
            case KeyEvent.VK_BRACERIGHT:
                return "BRACERIGHT";
            case KeyEvent.VK_AT:
                return "AT";
            case KeyEvent.VK_COLON:
                return "COLON";
            case KeyEvent.VK_CIRCUMFLEX:
                return "CIRCUMFLEX";
            case KeyEvent.VK_DOLLAR:
                return "DOLLAR";
            case KeyEvent.VK_EURO_SIGN:
                return "EURO_SIGN";
            case KeyEvent.VK_EXCLAMATION_MARK:
                return "EXCLAMATION_MARK";
            case KeyEvent.VK_INVERTED_EXCLAMATION_MARK:
                return "INVERTED_EXCLAMATION_MARK";
            case KeyEvent.VK_LEFT_PARENTHESIS:
                return "LEFT_PARENTHESIS";
            case KeyEvent.VK_NUMBER_SIGN:
                return "NUMBER_SIGN";
            case KeyEvent.VK_MINUS:
                return "MINUS";
            case KeyEvent.VK_PLUS:
                return "PLUS";
            case KeyEvent.VK_RIGHT_PARENTHESIS:
                return "RIGHT_PARENTHESIS";
            case KeyEvent.VK_UNDERSCORE:
                return "UNDERSCORE";
            case KeyEvent.VK_FINAL:
                return "FINAL";
            case KeyEvent.VK_CONVERT:
                return "CONVERT";
            case KeyEvent.VK_NONCONVERT:
                return "NONCONVERT";
            case KeyEvent.VK_ACCEPT:
                return "ACCEPT";
            case KeyEvent.VK_MODECHANGE:
                return "MODECHANGE";
            case KeyEvent.VK_KANA:
                return "KANA";
            case KeyEvent.VK_KANJI:
                return "KANJI";
            case KeyEvent.VK_ALPHANUMERIC:
                return "ALPHANUMERIC";
            case KeyEvent.VK_KATAKANA:
                return "KATAKANA";
            case KeyEvent.VK_HIRAGANA:
                return "HIRAGANA";
            case KeyEvent.VK_FULL_WIDTH:
                return "FULL_WIDTH";
            case KeyEvent.VK_HALF_WIDTH:
                return "HALF_WIDTH";
            case KeyEvent.VK_ROMAN_CHARACTERS:
                return "ROMAN_CHARACTERS";
            case KeyEvent.VK_ALL_CANDIDATES:
                return "ALL_CANDIDATES";
            case KeyEvent.VK_PREVIOUS_CANDIDATE:
                return "PREVIOUS_CANDIDATE";
            case KeyEvent.VK_CODE_INPUT:
                return "CODE_INPUT";
            case KeyEvent.VK_JAPANESE_KATAKANA:
                return "JAPANESE_KATAKANA";
            case KeyEvent.VK_JAPANESE_HIRAGANA:
                return "JAPANESE_HIRAGANA";
            case KeyEvent.VK_JAPANESE_ROMAN:
                return "JAPANESE_ROMAN";
            case KeyEvent.VK_KANA_LOCK:
                return "KANA_LOCK";
            case KeyEvent.VK_INPUT_METHOD_ON_OFF:
                return "INPUT_METHOD_ON_OFF";
            case KeyEvent.VK_AGAIN:
                return "AGAIN";
            case KeyEvent.VK_UNDO:
                return "UNDO";
            case KeyEvent.VK_COPY:
                return "COPY";
            case KeyEvent.VK_PASTE:
                return "PASTE";
            case KeyEvent.VK_CUT:
                return "CUT";
            case KeyEvent.VK_FIND:
                return "FIND";
            case KeyEvent.VK_PROPS:
                return "PROPS";
            case KeyEvent.VK_STOP:
                return "STOP";
            case KeyEvent.VK_COMPOSE:
                return "COMPOSE";
            case KeyEvent.VK_ALT_GRAPH:
                return "ALT_GRAPH";
        }
        if (keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9) {
            char c = (char) (keyCode - KeyEvent.VK_NUMPAD0 + '0');
            return "NUMPAD" + c;
        }
        return "unknown(0x" + Integer.toString(keyCode, 16) + ")";
    }

    public static String makeFirstWordCaptial(String word) {
        if (word.length() < 2) {
            return word;
        }
        String firstWord = word.substring(0, 1);
        String restOfWord = word.substring(1);
        return firstWord.toUpperCase() + restOfWord;
    }
}
