package eu.kostia.filetypedetect;

import static eu.kostia.filetypedetect.Type.ByteOrderEnum.BIG_ENDIAN;
import static eu.kostia.filetypedetect.Type.ByteOrderEnum.LITTLE_ENDIAN;
import static eu.kostia.filetypedetect.Type.ByteOrderEnum.NATIVE;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * <p>
 * Detect the file type basing on the magic pattern database.
 * </p>
 * The format of the magic patter file is described in {@link http
 * ://man.he.net/?topic=magic&section=all}. <br />
 * In the most linux system the magic file is in <tt>/usr/share/file/magic</tt>.
 * 
 * @author Costantino Cerbo
 * 
 */
public class Magic {

    /**
	 * The operators for the test condition already sorted.
	 */
    private static final char[] OPERATORS = new char[] { '!', '&', '<', '=', '>', '^', '~' };

    private InputStream magicInputStream;

    private File magicfile;

    private transient int currentLevel = -1;

    /**
	 * The position where the processor currently is.
	 */
    private transient int currentPosition = 0;

    private transient FileChannel channel;

    private transient StringBuilder fileDescription;

    private transient Result result = null;

    private transient boolean lastTestSuccessful = false;

    public Magic(File magicfile) throws FileNotFoundException {
        this.magicfile = magicfile;
        this.magicInputStream = new BufferedInputStream(new FileInputStream(magicfile));
    }

    public Magic() {
        this.magicInputStream = getClass().getResourceAsStream("/META-INF/magic");
    }

    private void resetInstanceVariables() throws FileNotFoundException {
        this.currentLevel = -1;
        this.currentPosition = 0;
        this.channel = null;
        this.fileDescription = null;
        this.result = null;
        this.lastTestSuccessful = false;
        if (magicfile != null) {
            this.magicInputStream = new BufferedInputStream(new FileInputStream(magicfile));
        } else {
            this.magicInputStream = getClass().getResourceAsStream("/META-INF/magic");
        }
    }

    public Result detect(File file) throws IOException {
        Scanner sc0 = null;
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(file);
            channel = fstream.getChannel();
            if (channel.size() == 0L) {
                result = new Result();
                result.setDescription("empty");
                result.setMime("application/x-empty");
                return result;
            }
            sc0 = new Scanner(magicInputStream);
            while (sc0.hasNextLine()) {
                String line = sc0.nextLine();
                if (line.indexOf("BBx") != -1) {
                    debug("breakpoint");
                }
                if (line.startsWith(">>(0x3c.l) string PE")) {
                    debug("breakpoint");
                }
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                if (line.startsWith("!:")) {
                    if (line.startsWith("!:mime") && currentLevel >= 0 && lastTestSuccessful) {
                        String s = removeInlineComment(line);
                        result.setMime(s.substring("!:mime".length()).trim());
                    }
                } else if (line.startsWith(">")) {
                    int level = level(line);
                    if (currentLevel > 0 && level <= currentLevel || level == currentLevel + 1) {
                        processLine(line, level);
                    }
                } else {
                    if (currentLevel >= 0) {
                        if (fileDescription != null && !fileDescription.toString().trim().isEmpty()) {
                            break;
                        } else {
                            currentLevel = -1;
                            result = null;
                            currentPosition = 0;
                        }
                    }
                    processLine(line, 0);
                }
            }
            if (fileDescription != null && fileDescription.length() > 0) {
                result.setDescription(fileDescription.toString().trim().replace("\n", ""));
                if (result.getMime() == null) {
                    if (result.getDescription().indexOf("ASCII") != -1) {
                        result.setMime("text/plain");
                    }
                }
            }
            return result;
        } finally {
            if (fstream != null) {
                fstream.close();
            }
            if (channel != null) {
                channel.close();
            }
            if (sc0 != null) {
                sc0.close();
            }
            resetInstanceVariables();
        }
    }

    private String removeInlineComment(String line) {
        int end = line.indexOf('#') != -1 ? line.indexOf('#') : line.length();
        return line.substring(0, end).trim();
    }

    private void processLine(String line, int level) throws IOException {
        line = line.replace("\\ ", String.valueOf((char) 0x04));
        try {
            Scanner sc1 = new Scanner(line.substring(level));
            int offset = toInt(sc1.next());
            if (offset < 0) {
                return;
            }
            Type type = parseType(sc1.next());
            String test = sc1.next().replace(String.valueOf((char) 0x04), " ");
            test = convertString(test);
            Object value = null;
            if (type.isString()) {
                value = performStringTest(offset, type, test);
            } else if (type.isSearch()) {
                value = performSearchTest(offset, type, test);
            } else if (type.isRegex()) {
                value = performRegexTest(offset, type, test);
            } else {
                int len = type.getLenght();
                if (len > 0 && (offset + len) > channel.size()) {
                    return;
                }
                value = performByteTest(type, readByte(offset, len), test);
            }
            if (value != null) {
                lastTestSuccessful = true;
                if (currentLevel == -1) {
                    result = new Result();
                    fileDescription = new StringBuilder();
                    currentLevel = 0;
                }
                currentLevel = level;
                sc1.useDelimiter("\\Z");
                String message = sc1.hasNext() ? sc1.next().trim() : null;
                if (message != null) {
                    if (message.startsWith("\\b")) {
                        if (fileDescription.length() > 0) {
                            fileDescription.deleteCharAt(fileDescription.length() - 1);
                        }
                        message = message.substring("\\b".length());
                    }
                    fileDescription.append(format(message, value));
                    fileDescription.append(" ");
                }
            } else {
                lastTestSuccessful = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Line that caused the exception:\n" + line, e);
        }
    }

    private String format(String format, Object... args) {
        return Printf.format(format, args);
    }

    byte[] readByte(int offset, int len) throws IOException {
        try {
            ByteBuffer bb = channel.map(MapMode.READ_ONLY, offset, len);
            byte[] b = new byte[bb.limit()];
            bb.position(0);
            bb.get(b);
            return b;
        } catch (Exception e) {
            throw new IllegalArgumentException("offset: " + offset + ", lenght: " + len + ", file size: " + channel.size(), e);
        }
    }

    /**
	 * Returns an array of signed bytes
	 */
    int[] readUnsignedByte(int offset, int len) throws IOException {
        byte[] b = readByte(offset, len);
        int[] bu = new int[len];
        for (int i = 0; i < len; i++) {
            bu[i] = b[i] & 0xff;
        }
        return bu;
    }

    /**
	 * For string values, the string from the file must match the spec- ified
	 * string. The operators =, < and > (but not &) can be applied to strings.
	 * The length used for matching is that of the string argument in the magic
	 * file. This means that a line can match any non-empty string (usually used
	 * to then print the string), with >\0 (because all non-empty strings are
	 * greater than the empty string).
	 */
    private Object performStringTest(int offset, Type type, String test) throws IOException {
        String str = readString(offset);
        if ("x".equals(test)) {
            if (lastTestSuccessful) {
                currentPosition = offset + str.length();
                return returnLine(str);
            } else {
                return null;
            }
        }
        if (">".equals(test)) {
            if (!str.isEmpty() && lastTestSuccessful) {
                currentPosition = offset + str.length();
                return returnLine(str);
            } else {
                return null;
            }
        }
        if (test.isEmpty()) {
            currentPosition = offset;
            return "";
        }
        boolean flag_b = false;
        boolean flag_c = false;
        if (type.getName().indexOf('/') != -1) {
            String flags = type.getName().substring(type.getName().indexOf('/'));
            StringFlags stringFlags = parseStringFlags(flags);
            flag_b = stringFlags.flag_b;
            flag_c = stringFlags.flag_c;
        }
        if (flag_c) {
            str = str.toLowerCase();
            test = test.toLowerCase();
        }
        int adjust = 0;
        if (flag_b) {
            adjust = str.length();
            str = str.replaceAll(" ", "");
            adjust -= str.length();
            test = test.replaceAll(" ", "");
            adjust -= str.length() - test.length();
        }
        switch(test.charAt(0)) {
            case '>':
                test = test.substring(1);
                String str0 = str;
                if (str0.length() > test.length()) {
                    str0 = str0.substring(0, test.length() - 1);
                }
                if (str0.compareTo(test) > 0) {
                    currentPosition = offset + str.length();
                    return returnLine(str);
                } else {
                    return null;
                }
            case '<':
                test = test.substring(1);
                str0 = str;
                if (str0.length() > test.length()) {
                    str0 = str0.substring(0, test.length() - 1);
                }
                if (str0.compareTo(test) < 0) {
                    currentPosition = offset + str.length();
                    return returnLine(str);
                } else {
                    return null;
                }
            case '!':
                test = test.substring(1);
                if (test.startsWith("\\<")) {
                    test = test.substring(1);
                }
                if (!str.startsWith(test)) {
                    currentPosition = offset + test.length() + adjust;
                    return returnLine(str);
                } else {
                    return null;
                }
            case '=':
                test = test.substring(1);
            default:
                if (test.startsWith("\\<")) {
                    test = test.substring(1);
                }
                if (str.startsWith(test)) {
                    currentPosition = offset + test.length() + adjust;
                    return returnLine(str);
                } else {
                    return null;
                }
        }
    }

    private Object performRegexTest(int offset, Type type, String regex) throws IOException {
        String str = readString(offset);
        StringFlags stringFlags = new StringFlags();
        if (type.getName().indexOf('/') != -1) {
            String flags = type.getName().substring(type.getName().indexOf('/'));
            stringFlags = parseStringFlags(flags);
        }
        if (stringFlags.flag_c) {
            str = str.toLowerCase();
            regex = regex.toLowerCase();
        }
        Matcher matcher = Pattern.compile(regex).matcher(str);
        if (matcher.find()) {
            currentPosition = offset + (stringFlags.flag_s ? matcher.start() : matcher.end());
            return returnLine(str);
        } else {
            return null;
        }
    }

    private Object performSearchTest(int offset, Type type, String test) throws IOException {
        String str = readString(offset);
        if ("x".equals(test)) {
            if (lastTestSuccessful) {
                currentPosition = offset + str.length();
                return returnLine(str);
            } else {
                return null;
            }
        }
        if (">".equals(test)) {
            if (lastTestSuccessful) {
                currentPosition = offset + str.length();
                return !str.isEmpty() ? returnLine(str) : null;
            } else {
                return null;
            }
        }
        if (test.isEmpty()) {
            currentPosition = offset;
            return "";
        }
        int range = -1;
        String typeName = type.getName();
        Scanner sc = new Scanner(typeName).useDelimiter(Pattern.quote("/"));
        sc.next();
        StringFlags stringFlags = new StringFlags();
        while (sc.hasNext()) {
            String next = sc.next();
            if (areStringFlags(next)) {
                stringFlags = parseStringFlags(next);
            } else {
                range = toInt(next);
            }
        }
        if (range != -1 && str.length() < range) {
            int len = range + test.length();
            if (offset < channel.size()) {
                if (offset + len >= channel.size()) {
                    len = (int) (channel.size() - offset - 1);
                }
                str = readString(offset, len);
            }
        }
        switch(test.charAt(0)) {
            case '>':
                test = test.substring(1);
                String str0 = str;
                if (str0.length() > test.length()) {
                    str0 = str0.substring(0, test.length() - 1);
                }
                if (str0.compareTo(test) > 0) {
                    currentPosition = offset + str.length();
                    return returnLine(str);
                } else {
                    return null;
                }
            case '<':
                test = test.substring(1);
                str0 = str;
                if (str0.length() > test.length()) {
                    str0 = str0.substring(0, test.length() - 1);
                }
                if (str0.compareTo(test) < 0) {
                    currentPosition = offset + str.length();
                    return returnLine(str);
                } else {
                    return null;
                }
            case '=':
                test = test.substring(1);
            default:
                if (test.startsWith("\\<")) {
                    test = test.substring(1);
                }
                if (stringFlags.flag_c) {
                    str = str.toLowerCase();
                    test = test.toLowerCase();
                }
                if (stringFlags.flag_b) {
                    str = str.replaceAll(" ", "");
                    test = test.replaceAll(" ", "");
                }
        }
        if (str.indexOf(test) >= 0) {
            currentPosition = offset + str.indexOf(test) + test.length();
            return returnLine(str);
        } else {
            return null;
        }
    }

    /**
	 * A line is considered terminated when ends with 00 (NUL), 0A (LF \n) or,
	 * 0D (CR \r).
	 */
    private String returnLine(String str) {
        int end = 0;
        for (end = 0; end < str.length(); end++) {
            char ch = str.charAt(end);
            if (ch == 0x00 || ch == 0x0a || ch == 0x0d) {
                break;
            }
        }
        return str.substring(0, end);
    }

    private boolean areStringFlags(String flags) {
        if (flags.indexOf('B') != -1) {
            return true;
        }
        if (flags.indexOf('b') != -1) {
            return true;
        }
        if (flags.indexOf('c') != -1) {
            return true;
        }
        return false;
    }

    class StringFlags {

        boolean flag_B = false;

        boolean flag_b = false;

        boolean flag_c = false;

        boolean flag_s = false;
    }

    private StringFlags parseStringFlags(String flags) {
        StringFlags stringFlags = new StringFlags();
        for (int i = 0; i < flags.length(); i++) {
            switch(flags.charAt(i)) {
                case 'B':
                    stringFlags.flag_B = true;
                    break;
                case 'b':
                    stringFlags.flag_b = true;
                    break;
                case 'c':
                    stringFlags.flag_c = true;
                    break;
            }
        }
        return stringFlags;
    }

    /**
	 * Beginning from the given offset, it read a string (as in C, a string is
	 * sequence of chars terminated with 0). For performance the max string
	 * length is 255. A string is considered terminated, when ends with 00
	 * (NUL).
	 * 
	 * In this case the {@code #currentPosition} is set in {@code
	 * #performStringTest(int, Type, String)}. Normally, it's set in {@code
	 * #readByte(int, int)}.
	 */
    private String readString(int offset) throws IOException {
        int k = 255;
        int len = (offset + k) <= channel.size() ? k : (int) channel.size() - offset;
        if (len <= 0) {
            return "";
        }
        int[] bu = readUnsignedByte(offset, len);
        int n = 0;
        for (n = 0; n < bu.length; n++) {
            if (bu[n] == 0) {
                break;
            }
        }
        bu = Arrays.copyOf(bu, n);
        return new String(bu, 0, bu.length);
    }

    private String readString(int offset, int len) throws IOException {
        int[] bu = readUnsignedByte(offset, len);
        return new String(bu, 0, bu.length);
    }

    /**
	 * When the byte lenght is fixed...
	 */
    private Object performByteTest(Type type, byte[] b, String test) {
        Object value = null;
        boolean passed = false;
        if ("x".equals(test)) {
            if (lastTestSuccessful) {
                passed = true;
            } else {
                return null;
            }
        }
        if (type.isIntegerNumber()) {
            long actual = toIntegerNumber(type.getOrder(), b);
            if (type.getAnd() != null) {
                actual = actual & type.getAnd();
            }
            value = actual;
            if (!passed) {
                if (test.length() > 1 && Arrays.binarySearch(OPERATORS, test.charAt(0)) >= 0) {
                    long expected = toLong(test.substring(1));
                    char operator = test.charAt(0);
                    switch(operator) {
                        case '=':
                            passed = (actual == expected);
                            break;
                        case '!':
                            passed = (actual != expected);
                            break;
                        case '<':
                            passed = (actual < expected);
                            break;
                        case '>':
                            passed = (actual > expected);
                            break;
                        case '&':
                            passed = and(actual, expected);
                            break;
                        case '^':
                            passed = xor(actual, expected);
                            break;
                        case '~':
                            passed = not(actual, expected);
                            break;
                    }
                } else {
                    long expected = toLong(test);
                    passed = (actual == expected);
                }
            }
        } else if (type.isDecimalNumber()) {
            double actual = toDecimalNumber(type.getOrder(), b);
            value = actual;
            if (!passed) {
                if (test.length() > 1 && Arrays.binarySearch(OPERATORS, test.charAt(0)) > 0) {
                    double expected = toDouble(test.substring(1));
                    char operator = test.charAt(0);
                    switch(operator) {
                        case '=':
                            passed = (actual == expected);
                            break;
                        case '!':
                            passed = (actual != expected);
                            break;
                        case '<':
                            passed = (actual < expected);
                            break;
                        case '>':
                            passed = (actual > expected);
                            break;
                    }
                }
            } else {
                double expected = toDouble(test);
                passed = (actual == expected);
            }
        } else if (type.isDate()) {
            value = toDate(type.getOrder(), b);
            passed = true;
        }
        return passed ? value : null;
    }

    /**
	 * Return true if the actual value must have set all of the bits that are
	 * set in the expected value.
	 */
    private boolean and(long actual, long expected) {
        String a = ByteUtil.toBinaryString(actual);
        String e = ByteUtil.toBinaryString(expected);
        for (int i = 0; i < e.length(); i++) {
            if (e.charAt(i) == '1') {
                if (a.charAt(i) != '1') {
                    return false;
                }
            }
        }
        return true;
    }

    /**
	 * The actual value must have clear any of the bits that are set in the
	 * specified value.
	 */
    private boolean xor(long actual, long expected) {
        String a = ByteUtil.toBinaryString(actual);
        String e = ByteUtil.toBinaryString(expected);
        for (int i = 0; i < e.length(); i++) {
            int x = (e.charAt(i) == '1') ? 1 : 0;
            int y = (a.charAt(i) == '1') ? 1 : 0;
            if ((x ^ y) != 1) {
                return false;
            }
        }
        return true;
    }

    /**
	 * The value specified after is negated before tested
	 */
    private boolean not(long actual, long expected) {
        String a = ByteUtil.toBinaryString(actual);
        String e = ByteUtil.toBinaryString(expected);
        char[] r = new char[e.length()];
        for (int i = 0; i < r.length; i++) {
            r[i] = e.charAt(i) == '0' ? '1' : '0';
        }
        String er = String.valueOf(r);
        return a.equals(er);
    }

    /**
	 * Return the byte array as unsigned integer number.
	 */
    private long toIntegerNumber(ByteOrder order, byte[] b) {
        int len = b.length;
        switch(len) {
            case 1:
                return b[0];
            case 2:
                return ByteUtil.toUnsigned(ByteUtil.toShort(order, b));
            case 4:
                return ByteUtil.toUnsigned(ByteUtil.toInt(order, b));
            case 8:
                return ByteUtil.toLong(order, b);
            default:
                throw new IllegalArgumentException("Invalid length: " + len);
        }
    }

    /**
	 * Return the byte array as decimal number.
	 */
    private double toDecimalNumber(ByteOrder order, byte[] b) {
        int len = b.length;
        switch(len) {
            case 4:
                return ByteUtil.toFloat(order, b);
            case 8:
                return ByteUtil.toDouble(order, b);
            default:
                throw new IllegalArgumentException("Invalid length: " + len);
        }
    }

    /**
	 * Return the byte array as date.
	 */
    private Date toDate(ByteOrder order, byte[] b) {
        int len = b.length;
        switch(len) {
            case 4:
                return new Date(ByteUtil.toInt(order, b) * 1000L);
            case 8:
                return new Date(ByteUtil.toLong(order, b) * 1000L);
            default:
                throw new IllegalArgumentException("Invalid length: " + len);
        }
    }

    /**
	 * Returns the byte lenght for the given type.
	 */
    private Type parseType(String rawtype) {
        String type = null;
        Long and = null;
        boolean unsigned = false;
        if (rawtype.startsWith("u")) {
            unsigned = true;
            rawtype = rawtype.substring(1);
        }
        if (rawtype.indexOf('&') != -1) {
            type = rawtype.substring(0, rawtype.indexOf('&'));
            and = Long.parseLong(rawtype.substring(rawtype.indexOf('&') + 3), 16);
        } else {
            type = rawtype;
        }
        if ("byte".equals(type)) {
            return new Type("byte", 1, NATIVE, and, unsigned);
        } else if ("short".equals(type)) {
            return new Type("short", 2, NATIVE, and, unsigned);
        } else if ("long".equals(type)) {
            return new Type("long", 4, NATIVE, and, unsigned);
        } else if ("quad".equals(type)) {
            return new Type("quad", 8, NATIVE, and, unsigned);
        } else if ("float".equals(type)) {
            return new Type("float", 4, NATIVE, and, unsigned);
        } else if ("double".equals(type)) {
            return new Type("double", 8, NATIVE, and, unsigned);
        } else if ("date".equals(type)) {
            return new Type("date", 4, NATIVE, and, unsigned);
        } else if ("beshort".equals(type)) {
            return new Type("beshort", 2, BIG_ENDIAN, and, unsigned);
        } else if ("belong".equals(type)) {
            return new Type("belong", 4, BIG_ENDIAN, and, unsigned);
        } else if ("bequad".equals(type)) {
            return new Type("bequad", 8, BIG_ENDIAN, and, unsigned);
        } else if ("befloat".equals(type)) {
            return new Type("befloat", 4, BIG_ENDIAN, and, unsigned);
        } else if ("bedouble".equals(type)) {
            return new Type("bedouble", 8, BIG_ENDIAN, and, unsigned);
        } else if ("bedate".equals(type)) {
            return new Type("bedate", 4, BIG_ENDIAN, and, unsigned);
        } else if ("beqdate".equals(type)) {
            return new Type("beqdate", 8, BIG_ENDIAN, and, unsigned);
        } else if ("beldate".equals(type)) {
            return new Type("beldate", 4, BIG_ENDIAN, and, unsigned);
        } else if ("beqldate".equals(type)) {
            return new Type("beqldate", 8, BIG_ENDIAN, and, unsigned);
        } else if ("bestring16".equals(type)) {
            return new Type("bestring16", 2, BIG_ENDIAN, and, unsigned);
        } else if ("leshort".equals(type)) {
            return new Type("leshort", 2, LITTLE_ENDIAN, and, unsigned);
        } else if ("lelong".equals(type)) {
            return new Type("lelong", 4, LITTLE_ENDIAN, and, unsigned);
        } else if ("lequad".equals(type)) {
            return new Type("lequad", 8, LITTLE_ENDIAN, and, unsigned);
        } else if ("lefloat".equals(type)) {
            return new Type("lefloat", 4, LITTLE_ENDIAN, and, unsigned);
        } else if ("ledouble".equals(type)) {
            return new Type("ledouble", 8, LITTLE_ENDIAN, and, unsigned);
        } else if ("ledate".equals(type)) {
            return new Type("ledate", 4, LITTLE_ENDIAN, and, unsigned);
        } else if ("leqdate".equals(type)) {
            return new Type("leqdate", 8, LITTLE_ENDIAN, and, unsigned);
        } else if ("leldate".equals(type)) {
            return new Type("leldate", 8, LITTLE_ENDIAN, and, unsigned);
        } else if ("leqldate".equals(type)) {
            return new Type("leqldate", 8, LITTLE_ENDIAN, and, unsigned);
        } else if ("lestring16".equals(type)) {
            return new Type("lestring16", 2, LITTLE_ENDIAN, and, unsigned);
        } else if (type.startsWith("string")) {
            return new Type(type, -1, null, null, unsigned);
        } else if (type.startsWith("pstring")) {
            return new Type(type, -1, null, null, unsigned);
        } else if (type.startsWith("search")) {
            return new Type(type, -1, null, null, unsigned);
        } else if (type.startsWith("regex")) {
            return new Type(type, -1, null, null, unsigned);
        } else if ("default".equals(type)) {
            return new Type(type, -1, null, null, unsigned);
        }
        throw new IllegalArgumentException("Type '" + type + "' is unknown.");
    }

    String convertString(String str) {
        str = str.replace("\\ ", " ");
        str = str.replace("\\<", "\\\\<");
        if (str.startsWith("\\074")) {
            str = "\\\\<" + str.substring("".length());
        }
        str = str.replace("\"", "\\\"");
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            String cstr = (String) engine.eval("new java.lang.String(\"" + str + "\")");
            return cstr.replace("\0", "");
        } catch (ScriptException e) {
            throw new IllegalStateException("Cannot parse string '" + str + "'");
        }
    }

    /**
	 * Convert to int from decimal, octal and hexdecimal format. The result is
	 * unsigned.
	 * 
	 * @throws IOException
	 */
    int toInt(String value) throws IOException {
        if (value.startsWith("0x")) {
            return Integer.parseInt(value.substring(2), 16);
        } else if (value.startsWith("0") && (value.length() > 1)) {
            return Integer.parseInt(value.substring(1), 8);
        } else if (value.startsWith("&") && (value.length() > 1)) {
            return currentPosition + toInt(value.substring(1));
        } else if (value.startsWith("(") && value.endsWith(")")) {
            long result = -1;
            String indirect = value.substring(1, value.length() - 1);
            int idx = indirect.indexOf('.');
            if (idx < 0) {
                return toInt(indirect);
            } else {
                int offset = toInt(indirect.substring(0, idx));
                char t = indirect.charAt(idx + 1);
                result = getIndirectValue(offset, t);
                if (indirect.length() > idx + 2) {
                    char op = indirect.charAt(idx + 2);
                    long rightValue = toLong(indirect.substring(idx + 3));
                    result = doOperation(result, op, rightValue);
                }
            }
            return (int) result;
        }
        return Integer.parseInt(removeParenthesis(value));
    }

    private long doOperation(long left, char op, long right) {
        switch(op) {
            case '+':
                return left + right;
            case '-':
                return left - right;
            case '*':
                return left * right;
            case '/':
                return left / right;
            case '%':
                return left % right;
            case '&':
                return left & right;
            case '|':
                return left | right;
            case '^':
                return left ^ right;
            default:
                throw new IllegalArgumentException("Unknown type: '" + op + "'");
        }
    }

    private long getIndirectValue(int offset, char t) throws IOException {
        switch(t) {
            case 'b':
                return readByte(offset, 1)[0];
            case 'i':
                return -1;
            case 's':
                return ByteUtil.toShort(LITTLE_ENDIAN.toByteOrder(), readByte(offset, 2));
            case 'l':
                return ByteUtil.toInt(LITTLE_ENDIAN.toByteOrder(), readByte(offset, 4));
            case 'B':
                return readByte(offset, 1)[0];
            case 'I':
                return -1;
            case 'S':
                return ByteUtil.toShort(BIG_ENDIAN.toByteOrder(), readByte(offset, 2));
            case 'L':
                return ByteUtil.toInt(BIG_ENDIAN.toByteOrder(), readByte(offset, 4));
            case 'm':
                return -1;
            default:
                throw new IllegalArgumentException("Unknown type: '" + t + "'");
        }
    }

    /**
	 * Convert to long from decimal, octal and hexdecimal format. The result is
	 * unsigned.
	 */
    private long toLong(String rawvalue) {
        String value = rawvalue;
        if (rawvalue.toUpperCase().endsWith("L")) {
            value = rawvalue.substring(0, rawvalue.length() - 1);
        }
        if (value.startsWith("0x")) {
            return Long.parseLong(value.substring(2), 16);
        } else if (value.startsWith("0") && (value.length() > 1)) {
            return Long.parseLong(value.substring(1), 8);
        }
        return Long.parseLong(removeParenthesis(value));
    }

    private String removeParenthesis(String value) {
        if (value.startsWith("(") && value.endsWith(")")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
	 * Convert to double from decimal, octal and hexdecimal format. The result
	 * is unsigned.
	 */
    private double toDouble(String value) {
        return Double.parseDouble(removeParenthesis(value));
    }

    private int level(String line) {
        int level = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '>') {
                level++;
            } else {
                break;
            }
        }
        return level;
    }

    private void debug(String string) {
        if ("true".equalsIgnoreCase(System.getProperty("filetypedetect.debug"))) {
            System.out.println(string);
        }
    }
}
