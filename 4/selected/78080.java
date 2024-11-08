package oxygen.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds different string utility methods.
 * provides various character utilities, primarily
 * related to converting between characters and bytes, escaping
 * unicode characters, etc. 
 *
 * the hexChars are the hexChars for converting a string to unicode hex.
 *
 * @author  Ugorji Dick-Nwoke ugorji.dick-nwoke@bea.com
 * @version 1.0, Mar 3, 2001
 */
public final class StringUtils {

    private static Pattern propertyReferencesReplacedPattern = Pattern.compile("\\$\\{.*?\\}");

    private static Pattern patternForRB = Pattern.compile("\\{\\d+\\}");

    /**
   * the hexChars are the hexChars for converting a string to unicode hex.
   */
    private static char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String LINE_SEP = System.getProperty("line.separator");

    /**
   * This takes a String and replaces all occurrences of old with neww
   */
    public static String replaceInString(String in, String old, String neww) {
        int b = 0;
        if (in == null || old == null || neww == null || old.length() == 0) {
            return in;
        }
        b = in.indexOf(old);
        int oldLength = old.length();
        StringBuffer mod = new StringBuffer(in.length());
        mod.append(in.substring(0, b));
        int e = in.indexOf(old, b + oldLength);
        while (e != -1) {
            mod.append(neww);
            mod.append(in.substring(b + oldLength, e));
            b = e;
            e = in.indexOf(old, b + oldLength);
        }
        mod.append(neww);
        if (b < in.length()) {
            mod.append(in.substring(b + oldLength));
        }
        return mod.toString();
    }

    /**
   * This takes a String and replaces all the single quotes with a
   * two single quotes ... 
   */
    public static String dbEscape(String in) {
        if (in == null) return "";
        return (replaceInString(in, "'", "''"));
    }

    /**
   * Separates a collection, using a blank space as the separator
   */
    public static String separate(Collection col) {
        return toString(col, " ");
    }

    /**
   * Separates a collection, using the specified sep as the separator
   */
    public static final String toString(Collection col, String sep) {
        StringBuffer sb = new StringBuffer();
        Iterator itr = col.iterator();
        String tmpstr = null;
        if (itr.hasNext()) {
            tmpstr = itr.next().toString();
            sb.append(tmpstr);
            while (itr.hasNext()) {
                tmpstr = itr.next().toString();
                sb.append(sep);
                sb.append(tmpstr);
            }
        }
        return sb.toString();
    }

    public static final List tokenize(String main, String sep) {
        return tokenize(main, sep, false);
    }

    public static final List tokenize(String main, String sep, boolean ignoreBlanks) {
        List array = new ArrayList();
        String tmp = null;
        if (main == null) return array;
        if (main.indexOf(sep) == -1) {
            tokenizeAddStr(array, main, ignoreBlanks);
            return array;
        }
        int pos = 0;
        int nextPos = 0;
        int sepLength = sep.length();
        int mainLength = main.length();
        boolean firstTime = true;
        boolean lastTime = false;
        while (!lastTime) {
            if (firstTime) {
                firstTime = false;
                nextPos = main.indexOf(sep);
                tokenizeAddStr(array, main.substring(0, nextPos), ignoreBlanks);
            } else {
                nextPos = main.indexOf(sep, pos + sepLength);
                if (nextPos == -1) {
                    nextPos = mainLength;
                    lastTime = true;
                    tokenizeAddStr(array, main.substring(pos + sepLength, nextPos), ignoreBlanks);
                } else {
                    tokenizeAddStr(array, main.substring(pos + sepLength, nextPos), ignoreBlanks);
                }
            }
            pos = nextPos;
        }
        return array;
    }

    private static final void tokenizeAddStr(List list, String str, boolean ignoreBlanks) {
        if (ignoreBlanks) {
            str = str.trim();
            if (str.length() > 0) list.add(str);
        } else {
            list.add(str);
        }
    }

    /**
   * Given a string, return only the first line.
   * If more than one line, add " ... " to the end of the first line, before returning it
   */
    public static String getFirstLine(String logMsg1) {
        if (logMsg1 != null) {
            int indexOfNewLine = logMsg1.indexOf("\r\n");
            if (indexOfNewLine == -1) {
                indexOfNewLine = logMsg1.indexOf("\n");
            }
            if (indexOfNewLine != -1) {
                logMsg1 = logMsg1.substring(0, indexOfNewLine);
                logMsg1 = logMsg1 + " ... ";
            }
            logMsg1 = replaceInString(logMsg1, "\r", "");
        }
        return logMsg1;
    }

    /**
   * Takes a string, replaces all the "\r" with blanks,
   * Then tokenizes based on "\n", and trims each line
   * Then puts all together
   */
    public static String trimLines(String str1) {
        if (str1 == null) return "";
        str1 = replaceInString(str1, "\r", "");
        StringBuffer buf = new StringBuffer();
        int indx = 0, lastIndx = 0;
        String s1 = null;
        while ((indx = str1.indexOf("\n", lastIndx)) != -1) {
            s1 = str1.substring(lastIndx, indx).trim();
            buf.append(s1);
            lastIndx = indx + 1;
        }
        s1 = str1.substring(lastIndx).trim();
        buf.append(s1);
        return buf.toString();
    }

    /** 
   * Takes a string, and returns a CVS (comma-value-seperated) version of the string
   */
    public static String toCSVString(String str1) {
        if (str1 == null) return str1;
        int len = str1.length();
        StringBuffer sb = new StringBuffer(len + 10);
        sb.append('"');
        for (int i = 0; i < len; i++) {
            char c = str1.charAt(i);
            switch(c) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '"':
                    sb.append("\"\"");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /** Split into CSV */
    public static int csvSplit(String line, List list) {
        int num = 0;
        char[] arr = line.toCharArray();
        if (arr.length == 0) return num;
        StringBuffer fld = null;
        int j = 0;
        int i = 0;
        while (i < arr.length) {
            if (arr[i] == '"') {
                i++;
                fld = new StringBuffer();
                while (i < arr.length) {
                    if (arr[i] != '"') {
                        fld.append(arr[i]);
                        i++;
                    } else {
                        if (i + 1 >= arr.length) {
                            i = i + 2;
                            break;
                        }
                        if (arr[i + 1] == '"') {
                            fld.append("\"");
                            i = i + 2;
                        } else {
                            i++;
                            break;
                        }
                    }
                }
            } else {
                fld = new StringBuffer();
                while (i < arr.length) {
                    if (arr[i] == ',') {
                        i++;
                        break;
                    }
                    fld.append(arr[i]);
                    i++;
                }
            }
            list.add(fld.toString());
            num++;
        }
        return num;
    }

    /**
   * Convert codepoint values > 0x7F to Unicode escape sequences.
   * This is used when you don't know the codeset that you will be
   * converting to.  This is important for i18n test handling.
   */
    public static String unicodeEscapeNonASCIIChars(String s) {
        StringBuffer sb = new StringBuffer(s);
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c > 0x7f) {
                i = escapeChar(sb, i);
            }
        }
        return sb.toString();
    }

    /**
   * converts a character in a string buffer into its unicode escaped form.
   */
    public static int escapeChar(StringBuffer sb, int offset) {
        char c = sb.charAt(offset);
        sb.setCharAt(offset++, '\\');
        char[] ci = new char[5];
        ci[0] = 'u';
        ci[1] = hexChars[(c >> 12) & 0xf];
        ci[2] = hexChars[(c >> 8) & 0xf];
        ci[3] = hexChars[(c >> 4) & 0xf];
        ci[4] = hexChars[(c) & 0xf];
        sb.insert(offset, ci);
        return offset + 4;
    }

    public static String replacePatternInString(String s, String patternstr, String replacement) throws Exception {
        Pattern p = Pattern.compile(patternstr, Pattern.DOTALL);
        Matcher m = p.matcher(s);
        String s2 = m.replaceAll(replacement);
        return s2;
    }

    public static String replaceRBStyle(String s, String[] r) {
        Matcher m = patternForRB.matcher(s);
        String s2 = s;
        if (m.find()) {
            m = m.reset();
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String ss = m.group();
                int idx = Integer.parseInt(ss.substring(1, ss.length() - 1));
                if (idx >= 0 && idx < r.length) {
                    m.appendReplacement(sb, r[idx]);
                }
            }
            m.appendTail(sb);
            s2 = sb.toString();
        }
        return s2;
    }

    public static boolean isFileMatch(File f, String regex) throws Exception {
        long flength = f.length();
        if (flength >= (long) Integer.MAX_VALUE) {
            throw new Exception("We cannot handle files with length >= Integer.MAX_VALUE (" + Integer.MAX_VALUE + ")");
        }
        char[] buf = new char[(int) flength];
        FileReader fr = new FileReader(f);
        fr.read(buf, 0, buf.length);
        fr.close();
        CharBuffer cbuf = CharBuffer.wrap(buf);
        Pattern p = Pattern.compile(regex, Pattern.DOTALL);
        Matcher m = p.matcher(cbuf);
        return m.matches();
    }

    public static String toString(Throwable thr) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        thr.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }

    public static String[] split(String stringrep, String sep) {
        List list = new ArrayList();
        int indexOfSep = 0;
        int currpos = 0;
        int seplen = sep.length();
        int stringreplen = stringrep.length();
        while ((indexOfSep = stringrep.indexOf(sep, currpos)) != -1) {
            if (currpos != indexOfSep) {
                list.add(stringrep.substring(currpos, indexOfSep));
            }
            currpos = indexOfSep + seplen;
        }
        if (currpos != stringreplen) {
            list.add(stringrep.substring(currpos, stringreplen));
        }
        return (String[]) list.toArray(new String[0]);
    }

    public static List tokens(String stringrep, String separatorChars, boolean ignoreBlanks, boolean trim) {
        List l = new ArrayList();
        StringTokenizer stz = new StringTokenizer(stringrep, separatorChars);
        String s = null;
        while (stz.hasMoreTokens()) {
            s = stz.nextToken();
            if (trim) {
                s = s.trim();
            }
            if (!isBlank(s)) {
                l.add(s);
            }
        }
        return l;
    }

    public static String getFormattedTimePeriod(long time) {
        long hrs = time / 3600000;
        int timeleft = (int) (time % 3600000);
        int mins = (timeleft / 60000);
        timeleft = (timeleft % 60000);
        int secs = (timeleft / 1000);
        StringBuffer buf = new StringBuffer();
        if (hrs > 0) {
            buf.append(hrs).append("h, ");
        }
        if (mins > 0) {
            buf.append(mins).append("m, ");
        }
        if (secs > 0) {
            buf.append(secs).append("s");
        }
        return buf.toString();
    }

    public static String propsToString(Properties p) throws Exception {
        return propsToString(p, null);
    }

    public static String propsToString(Properties p, String header) throws Exception {
        ByteArrayOutputStream logstw = null;
        String logmsg = null;
        if (p != null) {
            try {
                logstw = new ByteArrayOutputStream();
                p.store(logstw, header);
                logmsg = logstw.toString();
            } finally {
                CloseUtils.close(logstw);
            }
        }
        return logmsg;
    }

    public static Properties stringToProps(String s) throws Exception {
        Properties p = new Properties();
        stringToProps(s, p);
        return p;
    }

    public static void stringToProps(String s, Properties p) throws Exception {
        ByteArrayInputStream bais = null;
        try {
            if (s != null) {
                bais = new ByteArrayInputStream(s.getBytes());
                p.load(bais);
            }
        } finally {
            CloseUtils.close(bais);
        }
    }

    /**
   * Note that the Reader is closed after it is read
   * @param fr
   * @return
   * @throws Exception
   */
    public static String readerToString(Reader fr) throws Exception {
        try {
            StringWriter bos = new StringWriter();
            char[] buffer = new char[1024];
            int readCount = 0;
            while ((readCount = fr.read(buffer)) > 0) {
                bos.write(buffer, 0, readCount);
            }
            return bos.toString();
        } finally {
            CloseUtils.close(fr);
        }
    }

    public static int getMaxKeyLength(Collection c) {
        int maxlen = -1;
        for (Iterator itr = c.iterator(); itr.hasNext(); ) {
            Object s = itr.next();
            maxlen = Math.max(maxlen, String.valueOf(s).length());
        }
        return maxlen;
    }

    public static String padToLength(Object s, int len) {
        return padToLength(s, ' ', len);
    }

    public static String padToLength(Object s, char padchar, int len) {
        StringBuffer buf = new StringBuffer(len);
        buf.append(s);
        for (int i = String.valueOf(s).length(); i < len; i++) {
            buf.append(padchar);
        }
        return buf.toString();
    }

    /**
   * Took this method almost verbatim from 
   * http://jakarta.apache.org/commons/digester/xref/org/apache/commons/digester/SimpleRegexMatcher.html
   * Fixed a bug with it.
   */
    private static boolean match(String basePattern, String regexPattern, int baseAt, int regexAt) {
        char regexCurrent = '\0';
        if (regexAt >= regexPattern.length()) {
            if (baseAt >= basePattern.length()) {
                return true;
            }
            return false;
        } else {
            regexCurrent = regexPattern.charAt(regexAt);
            if (baseAt >= basePattern.length() && regexCurrent != '*') {
                return false;
            }
        }
        regexCurrent = regexPattern.charAt(regexAt);
        switch(regexCurrent) {
            case '*':
                if (++regexAt >= regexPattern.length()) {
                    return true;
                }
                char nextRegex = regexPattern.charAt(regexAt);
                int nextMatch = basePattern.indexOf(nextRegex, baseAt);
                while (nextMatch != -1) {
                    if (match(basePattern, regexPattern, nextMatch, regexAt)) {
                        return true;
                    }
                    nextMatch = basePattern.indexOf(nextRegex, nextMatch + 1);
                }
                return false;
            case '?':
                return match(basePattern, regexPattern, ++baseAt, ++regexAt);
            default:
                if (regexCurrent == basePattern.charAt(baseAt)) {
                    return match(basePattern, regexPattern, ++baseAt, ++regexAt);
                }
                return false;
        }
    }

    public static boolean matchSimpleRegex(String s, String regex) {
        return match(s, regex, 0, 0);
    }

    public static String nonNullString(Object obj, String nullReplacement) {
        String summhelp = null;
        if (obj != null) {
            summhelp = obj.toString();
        }
        if (summhelp == null) {
            summhelp = nullReplacement;
        }
        return summhelp;
    }

    public static String nonNullString(Object obj) {
        return nonNullString(obj, "");
    }

    public static String replacePropertyReferencesInString(String s, Properties props) {
        if (s == null || s.indexOf("${") < 0) {
            return s;
        }
        Matcher matcher = propertyReferencesReplacedPattern.matcher(s);
        StringBuffer sbuf = new StringBuffer();
        int currpos = 0;
        while (matcher.find()) {
            String key0 = matcher.group();
            int matchstartpos = matcher.start();
            String key1 = key0.substring(2, key0.length() - 1);
            key1 = props.getProperty(key1);
            if (key1 != null) {
                sbuf.append(s.substring(currpos, matchstartpos)).append(key1);
                currpos = matcher.end();
            }
        }
        sbuf.append(s.substring(currpos));
        if (currpos > 0) {
            s = sbuf.toString();
            return replacePropertyReferencesInString(s, props);
        } else {
            return s;
        }
    }

    public static boolean isBlank(String s) {
        return (s == null || s.trim().length() == 0);
    }

    public static String trim(String s, char c) {
        return trim(s, c, true, true);
    }

    public static String trim(String s, char c, boolean trimFront, boolean trimEnd) {
        int slen = s.length();
        if (slen == 0 || (!trimFront && !trimEnd)) {
            return s;
        }
        int beginIndx = 0;
        int endIndx = slen - 1;
        if (trimFront) {
            while (s.charAt(beginIndx) == c) {
                beginIndx++;
                if (beginIndx > endIndx) {
                    break;
                }
            }
        }
        if (trimEnd && (endIndx > beginIndx)) {
            while (s.charAt(endIndx) == c) {
                endIndx--;
                if (endIndx <= beginIndx) {
                    break;
                }
            }
        }
        return s.substring(beginIndx, endIndx + 1);
    }

    public static String getSingleValue(Object o) {
        String s = null;
        if (o instanceof List) {
            List col = (List) o;
            if (col.size() > 0) {
                s = col.get(0).toString();
            }
        } else if (o instanceof String[]) {
            String[] sa = (String[]) o;
            if (sa.length > 0) {
                s = sa[0];
            }
        }
        return s;
    }

    public static String[] toArray(String s) {
        String[] sa = new String[0];
        if (s != null) {
            sa = new String[] { s };
        }
        return sa;
    }

    /**
   * Strips the sepChar from beginning and end of a String
   * Also replaces \ with the sepChar (to account for Windows)
   */
    public static String getCleanPathName(String s, char sepChar) {
        if (s != null) {
            if (sepChar != '\\') {
                s = s.replace('\\', sepChar);
            }
            s = trim(s, sepChar);
        }
        return s;
    }

    public static String decodeURLEncodedPercentHexHex(String s) {
        try {
            if (s.indexOf('%') == -1) {
                return s;
            }
            StringBuffer sb = new StringBuffer();
            byte[] bytes = new byte[1];
            int numChars = s.length();
            for (int i = 0; i < numChars; i++) {
                char c = s.charAt(i);
                if (c == '%') {
                    bytes[0] = (byte) Integer.parseInt(s.substring(i + 1, i + 3), 16);
                    sb.append(new String(bytes, 0, 1, "UTF-8"));
                    i += 2;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } catch (UnsupportedEncodingException usee) {
            throw new RuntimeException(usee);
        }
    }

    public static String getFileNameExtension(String fname, String defaultExt) {
        String ext0 = defaultExt;
        int lastIndexOfDot = fname.lastIndexOf('.');
        if (lastIndexOfDot != -1) {
            ext0 = fname.substring(lastIndexOfDot);
        }
        if (ext0 != null) {
            ext0 = ext0.trim();
        }
        return ext0;
    }

    /** Main method for testing */
    public static void main(String[] args) {
        Properties p = new Properties();
        p.put("abc", "def");
        p.put("oxygen.wiki.webroot.path", "c:/a/b/c/${abc}");
        String s = "${def}thanks${abc}ok${def}thanks${abc}ok";
        s = "${oxygen.wiki.webroot.path}/WEB-INF/oxywiki/config";
        System.out.println(s);
        System.out.println(replacePropertyReferencesInString(s, p));
        System.out.println("The+string+%C3%BC%40foo-%20-+-bar");
        System.out.println(decodeURLEncodedPercentHexHex("The+string+%C3%BC%40foo-%20-+-bar"));
    }

    /**
   * escapes <xmp> < > & " </xmp> from the string
   * @param s
   * @return
   */
    public static String toHTMLEscape(String s, boolean changeNewLine, boolean changeWhiteSpace) {
        if (s == null) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        int slen = s.length();
        for (int i = 0; i < slen; i++) {
            char c = s.charAt(i);
            if (c == '<') {
                buf.append("&lt;");
            } else if (c == '>') {
                buf.append("&gt;");
            } else if (c == '&') {
                buf.append("&amp;");
            } else if (c == '\"') {
                buf.append("&quot;");
            } else if (c == '\n' && changeNewLine) {
                buf.append("<br/>\n");
            } else if (c == ' ' && changeWhiteSpace) {
                buf.append("&nbsp;");
            } else if (c == '\t' && changeWhiteSpace) {
                for (int j = 0; j < 8; j++) {
                    buf.append("&nbsp;");
                }
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    public static String mapToStringWithValuesInBrackets(Map m) {
        StringBuffer buf = new StringBuffer();
        if (m.size() == 0) {
            return "";
        }
        Iterator itr = m.entrySet().iterator();
        while (true) {
            Map.Entry me = (Map.Entry) itr.next();
            buf.append(me.getKey()).append("(").append(me.getValue()).append(")");
            if (itr.hasNext()) {
                buf.append(", ");
            } else {
                break;
            }
        }
        return buf.toString();
    }

    public static void encode(String s, StringBuffer buf) {
        if (s != null && s.length() > 0) {
            int slen = s.length();
            for (int i = 0; i < slen; i++) {
                char c = s.charAt(i);
                int ci = (int) c;
                if (ci == '*' || ci == '\'' || ci == ',' || ci == '\\' || ci == '^' || ci == '-' || ci == '_' || ci == '~' || ci == ':' || ci == '=' || ci == '`' || ci == '|' || ci == '[' || ci == ']' || ci == '<' || ci == '>' || ci == '{' || ci == '}') {
                    buf.append("&#").append(ci).append(";");
                } else {
                    buf.append(c);
                }
            }
        }
    }
}
