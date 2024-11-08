package de.cue4net.eventservice.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * StringUtils
 *
 * @author Thorsten Vogel
 * @author Keino Uelze - cue4net
 * @version $Id: StringUtils.java,v 1.6 2008-06-05 12:19:09 keino Exp $
 */
public class StringUtils {

    public static String doubleQuote(CharSequence text) {
        return quote(text, '"');
    }

    public static String singleQuote(CharSequence text) {
        return quote(text, '\'');
    }

    public static String quote(CharSequence text, char quoteChar) {
        StringBuilder builder = new StringBuilder();
        builder.append(quoteChar).append(text).append(quoteChar);
        return builder.toString();
    }

    /**
     * Numeric character reference to unicode function.
     *
     * @param str
     * @return
     */
    public static String ncrToUnicode(String str) {
        String ostr = new String();
        int i1 = 0;
        int i2 = 0;
        while (i2 < str.length()) {
            i1 = str.indexOf("&#", i2);
            if (i1 == -1) {
                ostr += str.substring(i2, str.length());
                break;
            }
            ostr += str.substring(i2, i1);
            i2 = str.indexOf(";", i1);
            if (i2 == -1) {
                ostr += str.substring(i1, str.length());
                break;
            }
            String tok = str.substring(i1 + 2, i2);
            try {
                int radix = 10;
                if (tok.trim().charAt(0) == 'x') {
                    radix = 16;
                    tok = tok.substring(1, tok.length());
                }
                ostr += (char) Integer.parseInt(tok, radix);
            } catch (NumberFormatException exp) {
                ostr += '?';
            }
            i2++;
        }
        return ostr;
    }

    /**
     * Unicode to Numeric character reference.
     *
     * @param str
     * @param escapeAscii
     * @return NCR string.
     */
    public static String unicodeToNcr(String str, boolean escapeAscii) {
        String ostr = new String();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (!escapeAscii && ((ch >= 0x0020) && (ch <= 0x007e))) ostr += ch; else {
                ostr += "\\u";
                String hex = Integer.toHexString(str.charAt(i) & 0xFFFF);
                if (hex.length() == 2) ostr += "00";
                ostr += hex.toUpperCase(Locale.getDefault());
            }
        }
        return (ostr);
    }

    /**
     * Returns <code>true</code> if the given path is a file.
     *
     * @param path
     * @return <code>True</code> if the given path is a file.
     */
    public static final boolean isFile(String path) {
        boolean r = false;
        try {
            r = new File(path).isFile();
        } catch (Exception ignored) {
        }
        return r;
    }

    /**
     * Returns <code>true</code> if the given path is a directory.
     *
     * @param path
     * @return <code>True</code> if the given path is a directory.
     */
    public static final boolean isDirectory(String path) {
        boolean r = false;
        try {
            r = new File(path).isDirectory();
        } catch (Exception ignored) {
        }
        return r;
    }

    /**
     * Returns the generated system id from the given path as
     * <code>String</code>.
     *
     * @param filepath
     * @return The generated system id from the given path as
     *         <code>String</code>.
     * @throws java.net.MalformedURLException
     */
    public static final String generateSystemId(String filepath) throws java.net.MalformedURLException {
        String r = null;
        try {
            r = (new java.io.File(filepath).toURI().toURL().toExternalForm());
        } catch (Exception e) {
        }
        return r;
    }

    /**
     * Padding-Zeroes (hey, never forget to save some for later :)
     */
    private static final String[] ZEROS = { "", "0", "00", "000", "0000", "00000", "000000", "0000000", "00000000", "000000000", "0000000000" };

    /**
     * Returns a padded string for <code>int</code>s.
     *
     * @param value An <code>int</code>.
     * @param length Must be 1 <= value <= 10
     * @return Returns a padded string for <code>int</code>s.
     */
    public static final String padInt(int value, int length) {
        String str = Integer.toString(value);
        if ((length > 10) | (str.length() > length)) {
            return str;
        }
        StringBuffer strbf = new StringBuffer(ZEROS[length - str.length()]);
        return strbf.append(str).toString();
    }

    /**
     * Converts the contents of a file into a CharSequence suitable for use by
     * the regex package.
     *
     * @param filename The file to read (UTF8 format).
     * @return CharSequence from the file
     * @throws IOException
     */
    public static CharSequence fromFile(String filename) throws IOException {
        FileInputStream fis = new FileInputStream(filename);
        FileChannel fc = fis.getChannel();
        ByteBuffer bbuf = fc.map(FileChannel.MapMode.READ_ONLY, 0, (int) fc.size());
        CharBuffer cbuf = Charset.forName("UTF8").newDecoder().decode(bbuf);
        fc.close();
        return cbuf;
    }

    /**
     * Copies src file to dst file.
     *
     * @param src
     * @param dst
     * @throws IOException
     */
    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /**
     * Returns a nice string.
     *
     * @param s
     * @return
     */
    public static final String nicestr(String s) {
        String str = null;
        if (s != null) {
            str = new String(s).trim();
            str = str.substring(0, 1).toUpperCase() + str.substring(1, str.length()).toLowerCase();
        }
        return str;
    }

    /**
     * Returns the newline char as <code>String</code>.
     *
     * @return Returns the newline char as <code>String</code>.
     */
    public static final String nl() {
        return Constants.FS;
    }

    /**
     * Returns the current time in milliseconds as
     *
     * <pre>
     * String
     * </pre> .
     *
     * @return Returns the current time in milliseconds as
     *
     * <pre>
     * String
     * </pre> .
     */
    public static final long now() {
        return Calendar.getInstance().getTimeInMillis();
    }

    /**
     * @param title
     * @return
     */
    public static String toUrlForm(String title) {
        String result = null;
        title = title.trim();
        title = title.replaceAll("Ö", "Oe").replaceAll("ö", "oe");
        title = title.replaceAll("Ü", "Ue").replaceAll("ü", "ue");
        title = title.replaceAll("Ä", "Ae").replaceAll("ä", "ae");
        title = title.replaceAll("ß", "ss");
        title = title.replaceAll("#", "").replaceAll("& ", "");
        title = title.replaceAll("&", "").replaceAll("&", "");
        title = title.replaceAll("<", "").replaceAll(">", "");
        title = title.replaceAll("[.]", "").replaceAll("!", "");
        title = title.replaceAll(",", "").replaceAll("[?]", "");
        title = title.replaceAll(" ", "-");
        try {
            result = URLEncoder.encode(title.toLowerCase(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }

    /**
     * Replace characters having special meaning <em>inside</em> HTML tags
     * with their escaped equivalents, using character entities such as
     * <tt>'&amp;'</tt>.
     *
     * <P>
     * The escaped characters are :
     * <ul>
     * <li> <
     * <li> >
     * <li> "
     * <li> '
     * <li> \
     * <li> &
     * </ul>
     *
     * <P>
     * This method ensures that arbitrary text appearing inside a tag does not
     * "confuse" the tag. For example, <tt>HREF='Blah.do?Page=1&Sort=ASC'</tt>
     * does not comply with strict HTML because of the ampersand, and should
     * be changed to <tt>HREF='Blah.do?Page=1&amp;Sort=ASC'</tt>. This is
     * commonly seen in building query strings. (In JSTL, the c:url tag
     * performs this task automatically.)
     */
    public static String escapeHTML(String inputString) {
        final StringBuffer result = new StringBuffer();
        final StringCharacterIterator iterator;
        iterator = new StringCharacterIterator(inputString);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            if (character == '<') {
                result.append("&lt;");
            } else if (character == '>') {
                result.append("&gt;");
            } else if (character == '\"') {
                result.append("&quot;");
            } else if (character == '\'') {
                result.append("&#039;");
            } else if (character == '\\') {
                result.append("&#092;");
            } else if (character == '&') {
                result.append("&amp;");
            } else {
                result.append(character);
            }
            character = iterator.next();
        }
        return result.toString();
    }

    public static void displayObjectsList(List<?> list) {
        Iterator<?> iter = list.iterator();
        if (!iter.hasNext()) {
            System.out.println("No objects to display.");
            return;
        }
        while (iter.hasNext()) {
            Object obj = (Object) iter.next();
            if (obj instanceof Object[]) {
                Object[] arr = (Object[]) obj;
                for (int i = 0; i < arr.length; i++) {
                    System.out.println(arr[i]);
                }
            } else {
                System.out.println(obj);
            }
        }
    }

    /**
     * Converts "fieldNameTest" to "Field Name Test"
     **/
    public static String camelCaseToNice(String s) {
        StringBuffer buf = new StringBuffer();
        char[] ch = s.toCharArray();
        for (int i = 0; i < ch.length; ++i) {
            if (Character.isUpperCase(ch[i]) && i != 0) {
                buf.append(' ');
                buf.append(Character.toUpperCase(ch[i]));
            } else {
                buf.append(ch[i]);
            }
        }
        return upperize(buf.toString());
    }

    public static String lowerize(final String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    public static String upperize(final String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * TODO Sheep, etc...?
     *
     * @param s
     * @return
     */
    public static String pluralize(final String s) {
        if (s.endsWith("y")) {
            return s.substring(0, s.length() - 1) + "ies";
        } else if (s.endsWith("s") || s.endsWith("x") || s.endsWith("h")) {
            return s + "es";
        } else {
            return s + "s";
        }
    }

    /**
     *
     * @param stringToCut
     * @param amountLetters
     * @param ending
     * @return a String which is cut after amountLetters and ends with ending
     */
    public static String cutAfterAmountOfLettersNice(String stringToCut, int amountLetters, String ending) {
        return (stringToCut.length() > amountLetters) ? stringToCut.substring(0, amountLetters).concat(ending) : stringToCut;
    }

    /**
     * 
     * @param stringToBeInjected
     * @param amountLetters
     * @param injectChar
     * @return a String in which after amountLetters a character has been injected in 
     * case it was not already at that position already
     */
    public static String injectCharAfterAmountOfLettersNice(String stringToBeInjected, int amountLetters, char injectChar) {
        final StringBuffer result = new StringBuffer();
        final StringCharacterIterator iterator;
        iterator = new StringCharacterIterator(stringToBeInjected);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            if ((iterator.getIndex() % amountLetters) == 0 && character != injectChar) result.append(injectChar);
            result.append(character);
            character = iterator.next();
        }
        return result.toString();
    }
}
