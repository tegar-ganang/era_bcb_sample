package jreceiver.util.text;

import java.io.*;
import java.text.FieldPosition;
import java.util.*;
import jreceiver.util.HexDump;

/**
 * A mirror of the MessageFormat class for encoding byte arrays.
 * <p>
 * As with MessageFormat, arguments may be in any order.
 * <p>
 * Unlike MessageFormat, you may use more than ten (10) arguments
 * and may use each more than once in the format string.
 * <p>
 * String example:
 * <pre>
 * String pattern = "{0}{1,string}{2,string,pp}{3,string,zt}{4,pp,UTF-8}";
 * MessageFormatB mfb = new MessageFormatB(pattern);
 * Object[] objs = { "foo", "foo", "bar", "baz", "buzz" };
 * byte[] = mfb.format(objs);
 * </pre>
 * The above pattern will format all its arguments as Strings
 * using the StringFormatB() formatter and output as an array of
 * bytes.  All objects will be coerced to strings using their
 * toString() methods.
 * <P>
 * {0} stored in byte array as "foo" using the default encoding (3 bytes)
 * <P>
 * {1,string} same as {0}
 * <P>
 * {2,string,pp} "bar" is stored with default encoding and prefixed
 * with an unsigned length byte, up to 255.  (4 bytes)
 * <P>
 * {3,string,zt} "baz" is stored with default encoding and a zero-
 * terminator (4 bytes)
 * {4,string,zt,UTF-8} "buzz" both a length-prefix byte encoding using utf-8
 * <P>
 * Word example:
 * <pre>
 * String pattern = "{0,word}{1,word,be}{2,word,le}";
 * MessageFormatB mfb = new MessageFormatB(pattern);
 * Object[] objs = { new Integer(0xAABBCCDD),
 *                   new Integer(0xAABBCCDD),
 *                   new Integer(0xAABBCCDD) };
 * byte[] = mfb.format(objs);
 * </pre>
 * {0,word} encoded as a 4-byte word using the default big-endian (Mac) order (result = AA BB CC DD)
 * <P>
 * {1,word,be} same as {0}
 * <P>
 * {2,word,le} encoded as a 4-byte word using the little-endian (Intel) order (result = DD CC BB AA)
 * <P>
 * TODO: support specifying the default encoding in constructor or when specifying
 * a new pattern.
 * <p>
 *
 * @author Reed Esau
 * @version $Revision: 1.3 $ $Date: 2002/12/29 00:44:08 $
 */
public class MessageFormatB extends FormatB {

    /**
     * ctor
     *
     * @param pattern
     */
    public MessageFormatB(byte[] pattern) {
        applyPattern(pattern);
    }

    /**
     * A convenience function to use a pattern without having
     * to separately instantiate a MessageFormatB object.
     * <p>
     * This is appropriate for single use of a pattern.  If you
     * will be using a specific pattern more than once, you
     * should create and re-use a MessageFormatB object.
     *
     * @param pattern
     * @param arguments
     * @return
     */
    public static byte[] format(byte[] pattern, Object[] arguments) throws IOException {
        return new MessageFormatB(pattern).format(arguments);
    }

    /**
     * A convenience function to use a pattern without having
     * to separately instantiate a MessageFormatB object.
     * <p>
     * This is appropriate for single use of a pattern.  If you
     * will be using a specific pattern more than once, you
     * should create and re-use a MessageFormatB object.
     *
     * @param pattern <code>String</code> will be transformed to bytes
     *                  using the platform's default encoding.
     * @param arguments
     * @return
     */
    public static byte[] format(String pattern, Object[] arguments) throws IOException {
        return new MessageFormatB(pattern.getBytes()).format(arguments);
    }

    /**
     * 'compile' the specified pattern
     *
     * Parse pattern, locating arguments and creating formatters for each
     *
     * A real single quote is represented by ''.  For example:
     * "O''Malley" translates to O'Malley"
     *
     * A curly brace can be escaped in quotes.  For example:
     * "foo'}'baz'{'bar" translates to "foo}baz{bar"
     *
     * @param pattern
     */
    public void applyPattern(byte[] pattern) {
        if (pattern == null || pattern.length < 1) throw new IllegalArgumentException("a pattern must be specified");
        HashMap formats = new HashMap();
        List fields = new LinkedList();
        final int NIL = '\0';
        final int CURLY_BRACE_OPEN = '{';
        final int CURLY_BRACE_CLOSE = '}';
        final int SINGLE_QUOTE = '\'';
        final int UNDEF = -1;
        int curly_start = UNDEF;
        int quote_start = UNDEF;
        int pos = 0;
        int end = pos + pattern.length - 1;
        do {
            switch(pattern[pos]) {
                case CURLY_BRACE_OPEN:
                    if (quote_start == UNDEF) {
                        if (curly_start != UNDEF) throw new IllegalArgumentException("nesting curly braces not supported yet, pos=" + pos);
                        curly_start = pos;
                    }
                    break;
                case CURLY_BRACE_CLOSE:
                    if (quote_start == UNDEF) {
                        if (curly_start == UNDEF) throw new IllegalArgumentException("missing opening curly brace, pos=" + pos);
                        createFormatter(pattern, curly_start, pos, formats, fields);
                        curly_start = UNDEF;
                    }
                    break;
                case SINGLE_QUOTE:
                    if (curly_start != UNDEF) {
                    } else if (quote_start == UNDEF) {
                        quote_start = pos;
                    } else {
                        int diff = pos - quote_start;
                        if (diff > 1) {
                            for (int i = quote_start; i < pos - 1; i++) pattern[i] = pattern[i + 1];
                            pos--;
                            for (int i = pos; i < end - 1; i++) pattern[i] = pattern[i + 2];
                            pos--;
                            end -= 2;
                        } else {
                            for (int i = pos; i < end; i++) pattern[i] = pattern[i + 1];
                            pos--;
                            end--;
                        }
                        quote_start = UNDEF;
                    }
                    break;
            }
        } while (++pos <= end);
        if (curly_start != UNDEF) throw new IllegalArgumentException("unmatched braces in the pattern");
        if (quote_start != UNDEF) throw new IllegalArgumentException("unmatched single quotes in the pattern");
        clean_pattern = new byte[end + 1];
        System.arraycopy(pattern, 0, clean_pattern, 0, clean_pattern.length);
        int max_index = -1;
        Iterator it = formats.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            if (key.intValue() > max_index) max_index = key.intValue();
        }
        if (max_index >= 0) {
            xformats = new FormatB[max_index + 1];
            it = formats.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Integer key = (Integer) entry.getKey();
                xformats[key.intValue()] = (FormatB) formats.get(key);
            }
            int index = 0;
            xfields = new FieldPosition[fields.size()];
            it = fields.iterator();
            while (it.hasNext()) xfields[index++] = (FieldPosition) it.next();
        }
    }

    /**
     * apply pattern, encoded into bytes using the platform's default encoding
     */
    public void applyPattern(String pattern) {
        applyPattern(pattern.getBytes());
    }

    /**
     * Assign the formats to be used.  Should be keyed by Integer() arg_nos
     */
    public void setFormats(FormatB[] xformats) {
        this.xformats = xformats;
    }

    /**
     * Assign a specific format to be used for the specified arg_no.
     */
    public void setFormat(int arg_no, FormatB format) {
        xformats[arg_no] = format;
    }

    /**
     * Obtain the formats to be used, keyed by Integer() arg_nos
     */
    public FormatB[] getFormats() {
        return xformats;
    }

    /**
     * format an array of objects into a byte output stream
     */
    public ByteArrayOutputStream format(Object obj, ByteArrayOutputStream toAppendTo, FieldPosition ignore) throws IOException {
        return format((Object[]) obj, toAppendTo, ignore);
    }

    /**
     * format an array of objects into a byte output stream
     */
    public ByteArrayOutputStream format(Object[] objs, ByteArrayOutputStream toAppendTo, FieldPosition ignore) throws IOException {
        FieldPosition field = null;
        int pos = 0;
        for (int i = 0; i < xfields.length; i++) {
            field = xfields[i];
            int begin = field.getBeginIndex();
            if (begin > pos) toAppendTo.write(clean_pattern, pos, begin - pos);
            int arg_no = field.getField();
            FormatB fmtr = xformats[arg_no];
            toAppendTo.write(fmtr.format(objs[arg_no]));
            pos = field.getEndIndex() + 1;
        }
        int last_field_pos = (field != null) ? field.getEndIndex() : -1;
        int remaining = clean_pattern.length - last_field_pos - 1;
        if (remaining > 0) toAppendTo.write(clean_pattern, last_field_pos + 1, remaining);
        return toAppendTo;
    }

    /**
    * assign options to formatter
    */
    public void setOption(String option) {
    }

    /**
     * Render this format object as a string.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("MessageFormatB:");
        if (clean_pattern != null) {
            buf.append(" pattern=[");
            buf.append(HexDump.dumpHexData("MessageFormatB", clean_pattern));
            buf.append(']');
        }
        if (xformats != null) {
            buf.append("\n\t formats=[");
            for (int i = 0; i < xformats.length; i++) {
                if (xformats[i] == null) continue;
                buf.append("\n\t\t{");
                buf.append(i);
                buf.append("}=");
                buf.append(xformats[i].toString());
                buf.append(" ");
            }
            buf.append(']');
        }
        return buf.toString();
    }

    /**
    */
    private void createFormatter(byte[] pattern, int start, int finish, Map formats, List fields) {
        String tmp = new String(pattern, start, finish - start + 1);
        String cleaned = tmp.substring(1, tmp.length() - 1);
        int arg_no;
        StringTokenizer st = new StringTokenizer(cleaned, ",");
        if (st.hasMoreTokens()) {
            String str_arg_no = (String) st.nextToken();
            try {
                arg_no = Integer.parseInt(str_arg_no);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid or missing arg_no, " + str_arg_no);
            }
        } else throw new IllegalArgumentException("invalid format element");
        FieldPosition field = new FieldPosition(arg_no);
        field.setBeginIndex(start);
        field.setEndIndex(finish);
        fields.add(field);
        if (formats.containsKey(new Integer(arg_no))) return;
        String name = null;
        if (st.hasMoreTokens()) {
            name = ((String) st.nextToken()).trim();
        }
        List options = new LinkedList();
        while (st.hasMoreTokens()) options.add(((String) st.nextToken()).trim());
        FormatB fmt = null;
        if (name != null && name.equalsIgnoreCase(WordFormatB.ID)) fmt = new WordFormatB(); else fmt = new StringFormatB();
        Iterator it = options.iterator();
        while (it.hasNext()) fmt.setOption((String) it.next());
        formats.put(new Integer(arg_no), fmt);
    }

    /**
     * The pattern which has had its quotes unescaped.
     */
    private byte[] clean_pattern = null;

    /**
     * the list of formatters
     */
    private FormatB[] xformats = null;

    /**
     * a sequential list of positions in the pattern string for
     * each argument
     */
    private FieldPosition[] xfields = null;
}
