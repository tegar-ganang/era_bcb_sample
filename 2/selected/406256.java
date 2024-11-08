package com.rapidminer.tools;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.lang.StringEscapeUtils;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.io.Encoding;
import com.rapidminer.tools.plugin.Plugin;

/**
 * Tools for RapidMiner.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class Tools {

    /** Units for sizes in bytes. */
    private static final String[] MEMORY_UNITS = { "b", "kB", "MB", "GB", "TB" };

    /** The line separator depending on the operating system. */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /** Number smaller than this value are considered as zero. */
    private static final double IS_ZERO = 1E-6;

    /** Number of post-comma digits needed to distinguish between display of numbers as integers or doubles. */
    private static final double IS_DISPLAY_ZERO = 1E-8;

    /** Used for formatting values in the {@link #formatTime(Date)} method. */
    private static final TimeFormat DURATION_TIME_FORMAT = new TimeFormat();

    /** Used for formatting values in the {@link #formatTime(Date)} method. */
    private static final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.LONG, Locale.getDefault());

    /** Used for formatting values in the {@link #formatDate(Date)} method. */
    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());

    /** Used for formatting values in the {@link #formatDateTime(Date)} method. */
    private static final DateFormat DATE_TIME_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG, Locale.getDefault());

    private static Locale FORMAT_LOCALE = Locale.US;

    /** Used for formatting values in the {@link #formatNumber(double)} method. */
    private static NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(FORMAT_LOCALE);

    /** Used for formatting values in the {@link #formatNumber(double)} method. */
    private static NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(FORMAT_LOCALE);

    /** Used for formatting values in the {@link #formatPercent(double)} method. */
    private static NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance(FORMAT_LOCALE);

    /** Used for determining the symbols used in decimal formats. */
    private static DecimalFormatSymbols FORMAT_SYMBOLS = new DecimalFormatSymbols(FORMAT_LOCALE);

    private static final LinkedList<ResourceSource> ALL_RESOURCE_SOURCES = new LinkedList<ResourceSource>();

    public static final String RESOURCE_PREFIX = "com/rapidminer/resources/";

    static {
        ALL_RESOURCE_SOURCES.add(new ResourceSource(Tools.class.getClassLoader()));
    }

    public static String[] availableTimeZoneNames;

    public static final int SYSTEM_TIME_ZONE = 0;

    static {
        String[] allTimeZoneNames = TimeZone.getAvailableIDs();
        Arrays.sort(allTimeZoneNames);
        availableTimeZoneNames = new String[allTimeZoneNames.length + 1];
        availableTimeZoneNames[SYSTEM_TIME_ZONE] = "SYSTEM";
        System.arraycopy(allTimeZoneNames, 0, availableTimeZoneNames, 1, allTimeZoneNames.length);
    }

    public static void setFormatLocale(Locale locale) {
        FORMAT_LOCALE = locale;
        NUMBER_FORMAT = NumberFormat.getInstance(locale);
        INTEGER_FORMAT = NumberFormat.getIntegerInstance(locale);
        PERCENT_FORMAT = NumberFormat.getPercentInstance(locale);
        FORMAT_SYMBOLS = new DecimalFormatSymbols(locale);
    }

    public static Locale getFormatLocale() {
        return FORMAT_LOCALE;
    }

    public static String[] getAllTimeZones() {
        return availableTimeZoneNames;
    }

    public static TimeZone getTimeZone(int index) {
        if (index == SYSTEM_TIME_ZONE) return TimeZone.getDefault(); else return TimeZone.getTimeZone(availableTimeZoneNames[index]);
    }

    public static TimeZone getPreferredTimeZone() {
        return getTimeZone(getPreferredTimeZoneIndex());
    }

    public static int getPreferredTimeZoneIndex() {
        String timeZoneString = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_TIME_ZONE);
        int preferredTimeZone = SYSTEM_TIME_ZONE;
        try {
            if (timeZoneString != null) preferredTimeZone = Integer.parseInt(timeZoneString);
        } catch (NumberFormatException e) {
            int index = 0;
            boolean found = false;
            for (String id : availableTimeZoneNames) {
                if (id.equals(timeZoneString)) {
                    found = true;
                    break;
                }
                index++;
            }
            if (found) preferredTimeZone = index;
        }
        return preferredTimeZone;
    }

    public static Calendar getPreferredCalendar() {
        return Calendar.getInstance(getPreferredTimeZone(), Locale.getDefault());
    }

    /**
     * Formats the value according to the given valueType. The value must be an object of type String for nominal
     * values, an object of type Date for date_time values or of type Double for numerical values.
     * 
     * @param value
     * @param valueType
     * @return value as string
     */
    public static String format(Object value, int valueType) {
        if (value == null) {
            return "?";
        }
        if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL)) {
            return (String) value;
        }
        if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NUMERICAL)) {
            return formatIntegerIfPossible((Double) value);
        }
        if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE)) {
            return formatDate((Date) value);
        }
        if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.TIME)) {
            return formatTime((Date) value);
        }
        if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.DATE_TIME)) {
            return formatDateTime((Date) value);
        }
        return "?";
    }

    /**
     * Returns a formatted string of the given number (percent format with two fraction digits).
     */
    public static String formatPercent(double value) {
        if (Double.isNaN(value)) return "?";
        String percentDigitsString = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_PERCENT);
        int percentDigits = 2;
        try {
            if (percentDigitsString != null) percentDigits = Integer.parseInt(percentDigitsString);
        } catch (NumberFormatException e) {
            LogService.getGlobal().log("Bad integer for property 'rapidminer.gui.fractiondigits.percent', using default number if digits (2).", LogService.WARNING);
        }
        PERCENT_FORMAT.setMaximumFractionDigits(percentDigits);
        PERCENT_FORMAT.setMinimumFractionDigits(percentDigits);
        return PERCENT_FORMAT.format(value);
    }

    /**
     * Returns a formatted string of the given number (number format with usually three fraction digits).
     */
    public static String formatNumber(double value) {
        if (Double.isNaN(value)) return "?";
        int numberDigits = 3;
        try {
            String numberDigitsString = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS);
            numberDigits = Integer.parseInt(numberDigitsString);
        } catch (NumberFormatException e) {
        }
        return formatNumber(value, numberDigits, false);
    }

    /**
     * Returns a formatted string of the given number (uses the property rapidminer.gui.fractiondigits.numbers if the
     * given number of digits is smaller than 0 (usually 3)).
     */
    public static String formatNumber(double value, int numberOfDigits) {
        return formatNumber(value, numberOfDigits, false);
    }

    /**
     * Returns a formatted string of the given number (uses the property rapidminer.gui.fractiondigits.numbers if the
     * given number of digits is smaller than 0 (usually 3)).
     */
    public static String formatNumber(double value, int numberOfDigits, boolean groupingCharacters) {
        if (Double.isNaN(value)) return "?";
        int numberDigits = numberOfDigits;
        if (numberDigits < 0) {
            try {
                String numberDigitsString = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS);
                numberDigits = Integer.parseInt(numberDigitsString);
            } catch (NumberFormatException e) {
                numberDigits = 3;
            }
        }
        NUMBER_FORMAT.setMaximumFractionDigits(numberDigits);
        NUMBER_FORMAT.setMinimumFractionDigits(numberDigits);
        NUMBER_FORMAT.setGroupingUsed(groupingCharacters);
        return NUMBER_FORMAT.format(value);
    }

    /**
     * Returns a number string with no fraction digits if possible. Otherwise the default number of digits will be
     * returned.
     */
    public static String formatIntegerIfPossible(double value) {
        int numberDigits = 3;
        try {
            String numberDigitsString = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_FRACTIONDIGITS_NUMBERS);
            numberDigits = Integer.parseInt(numberDigitsString);
        } catch (NumberFormatException e) {
        }
        return formatIntegerIfPossible(value, numberDigits, false);
    }

    /**
     * Returns a number string with no fraction digits if possible. Otherwise the given number of digits will be
     * returned.
     */
    public static String formatIntegerIfPossible(double value, int numberOfDigits) {
        return formatIntegerIfPossible(value, numberOfDigits, false);
    }

    /**
     * Returns a number string with no fraction digits if possible. Otherwise the given number of digits will be
     * returned.
     */
    public static String formatIntegerIfPossible(double value, int numberOfDigits, boolean groupingCharacter) {
        if (Double.isNaN(value)) return "?";
        if (Double.isInfinite(value)) {
            if (value < 0) return "-" + FORMAT_SYMBOLS.getInfinity(); else return FORMAT_SYMBOLS.getInfinity();
        }
        long longValue = Math.round(value);
        if (Math.abs(longValue - value) < IS_DISPLAY_ZERO) {
            INTEGER_FORMAT.setGroupingUsed(groupingCharacter);
            return INTEGER_FORMAT.format(longValue);
        } else {
            return formatNumber(value, numberOfDigits, groupingCharacter);
        }
    }

    /** Format date as a short time string. */
    public static String formatTime(Date date) {
        TIME_FORMAT.setTimeZone(getPreferredTimeZone());
        return TIME_FORMAT.format(date);
    }

    /** Format date as a short time string. */
    public static String formatDate(Date date) {
        DATE_FORMAT.setTimeZone(getPreferredTimeZone());
        return DATE_FORMAT.format(date);
    }

    /** Format date as a short time string. */
    public static String formatDateTime(Date date) {
        DATE_TIME_FORMAT.setTimeZone(getPreferredTimeZone());
        return DATE_TIME_FORMAT.format(date);
    }

    /** Format the given amount of milliseconds as a human readable string. */
    public static String formatDuration(long milliseconds) {
        return DURATION_TIME_FORMAT.format(milliseconds);
    }

    /** Returns the name for an ordinal number. */
    public static String ordinalNumber(int n) {
        if (n % 10 == 1 && n % 100 != 11) {
            return n + "st";
        }
        if (n % 10 == 2 && n % 100 != 12) {
            return n + "nd";
        }
        if (n % 10 == 3 && n % 100 != 13) {
            return n + "rd";
        }
        return n + "th";
    }

    /** Returns true if the difference between both numbers is smaller than IS_ZERO. */
    public static boolean isEqual(double d1, double d2) {
        if (Double.isNaN(d1) && Double.isNaN(d2)) return true;
        if (Double.isNaN(d1) || Double.isNaN(d2)) return false;
        return Math.abs(d1 - d2) < IS_ZERO;
    }

    /** Returns {@link #isEqual(double, double)} for d and 0. */
    public static boolean isZero(double d) {
        return isEqual(d, 0.0d);
    }

    /** Returns no {@link #isEqual(double, double)}. */
    public static boolean isNotEqual(double d1, double d2) {
        return !isEqual(d1, d2);
    }

    /** Returns true if the d1 is greater than d2 and they are not equal. */
    public static boolean isGreater(double d1, double d2) {
        return Double.compare(d1, d2) > 0 && isNotEqual(d1, d2) || Double.isNaN(d1) || Double.isNaN(d2);
    }

    /** Returns true if the d1 is greater than d1 or both are equal, or if one of the values is NaN */
    public static boolean isGreaterEqual(double d1, double d2) {
        return Double.compare(d1, d2) > 0 || isEqual(d1, d2) || Double.isNaN(d1) || Double.isNaN(d2);
    }

    /** Returns true if the d1 is less than d2 and they are not equal. */
    public static boolean isLess(double d1, double d2) {
        return !isGreaterEqual(d1, d2);
    }

    /** Returns true if the d1 is less than d1 or both are equal. */
    public static boolean isLessEqual(double d1, double d2) {
        return !isGreater(d1, d2) || Double.isNaN(d1) || Double.isNaN(d2);
    }

    /** Returns the correct line separator for the current operating system. */
    public static String getLineSeparator() {
        return LINE_SEPARATOR;
    }

    /**
     * Returns the correct line separator for the current operating system concatenated for the given number of times.
     */
    public static String getLineSeparators(int number) {
        if (number < 0) number = 0;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < number; i++) result.append(LINE_SEPARATOR);
        return result.toString();
    }

    /**
     * Replaces all possible line feed character combinations by &quot;\n&quot;. This might be important for GUI
     * purposes like tool tip texts which do not support carriage return combinations.
     */
    public static String transformAllLineSeparators(String text) {
        Pattern crlf = Pattern.compile("(\r\n|\r|\n|\n\r)");
        Matcher m = crlf.matcher(text);
        if (m.find()) {
            text = m.replaceAll("\n");
        }
        return text;
    }

    /**
     * Removes all possible line feed character combinations. This might be important for GUI purposes like tool tip
     * texts which do not support carriage return combinations.
     */
    public static String removeAllLineSeparators(String text) {
        Pattern crlf = Pattern.compile("(\r\n|\r|\n|\n\r)");
        Matcher m = crlf.matcher(text);
        if (m.find()) {
            text = m.replaceAll(" ");
        }
        return text;
    }

    /**
     * Returns the class name of the given class without the package information.
     * 
     * @deprecated Call c.getSimpleName() directly.
     */
    @Deprecated
    public static String classNameWOPackage(Class c) {
        return c.getSimpleName();
    }

    /**
     * Reads the output of the reader and delivers it as string.
     */
    public static String readOutput(BufferedReader in) throws IOException {
        StringBuffer output = new StringBuffer();
        String line = null;
        while ((line = in.readLine()) != null) {
            output.append(line);
            output.append(Tools.getLineSeparator());
        }
        return output.toString();
    }

    /**
     * Creates a file relative to the given parent if name is not an absolute file name. Returns null if name is null.
     */
    public static File getFile(File parent, String name) {
        if (name == null) return null;
        File file = new File(name);
        if (file.isAbsolute()) return file; else return new File(parent, name);
    }

    /**
     * This method checks if the given file is a Zip file containing one entry (in case of file extension .zip). If this
     * is the case, a reader based on a ZipInputStream for this entry is returned. Otherwise, this method checks if the
     * file has the extension .gz. If this applies, a gzipped stream reader is returned. Otherwise, this method just
     * returns a BufferedReader for the given file (file was not zipped at all).
     */
    public static BufferedReader getReader(File file, Charset encoding) throws IOException {
        if (file.getAbsolutePath().endsWith(".zip")) {
            ZipFile zipFile = new ZipFile(file);
            if (zipFile.size() == 0) {
                throw new IOException("Input of Zip file failed: the file archive does not contain any entries.");
            }
            if (zipFile.size() > 1) {
                throw new IOException("Input of Zip file failed: the file archive contains more than one entry.");
            }
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            InputStream zipIn = zipFile.getInputStream(entries.nextElement());
            return new BufferedReader(new InputStreamReader(zipIn, encoding));
        } else if (file.getAbsolutePath().endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), encoding));
        } else {
            return new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
        }
    }

    /**
     * This method tries to identify the encoding if a GUI is running and a process is defined. In this case, the
     * encoding is taken from the process. Otherwise, the method tries to identify the encoding via the property
     * {@link RapidMiner#PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING}. If this is not possible, this method just
     * returns the default system encoding.
     */
    public static Charset getDefaultEncoding() {
        Charset result = null;
        if (RapidMiner.getExecutionMode().hasMainFrame()) {
            MainFrame mainFrame = RapidMinerGUI.getMainFrame();
            if (mainFrame != null) {
                com.rapidminer.Process process = mainFrame.getProcess();
                if (process != null) {
                    Operator rootOperator = process.getRootOperator();
                    if (rootOperator != null) {
                        try {
                            result = Encoding.getEncoding(rootOperator);
                        } catch (UndefinedParameterError e) {
                            result = Charset.defaultCharset();
                        } catch (UserError e) {
                            result = Charset.defaultCharset();
                        }
                    }
                }
            }
        }
        if (result == null) {
            String encoding = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING);
            if (encoding != null && encoding.trim().length() > 0) {
                if (RapidMiner.SYSTEM_ENCODING_NAME.equals(encoding)) {
                    result = Charset.defaultCharset();
                } else {
                    result = Charset.forName(encoding);
                }
            }
        }
        if (result == null) {
            result = Charset.defaultCharset();
        }
        return result;
    }

    /** Returns the relative path of the first file resolved against the second. */
    public static String getRelativePath(File firstFile, File secondFile) throws IOException {
        String canonicalFirstPath = firstFile.getCanonicalPath();
        String canonicalSecondPath = secondFile.getCanonicalPath();
        int minLength = Math.min(canonicalFirstPath.length(), canonicalSecondPath.length());
        int index = 0;
        for (index = 0; index < minLength; index++) {
            if (canonicalFirstPath.charAt(index) != canonicalSecondPath.charAt(index)) {
                break;
            }
        }
        String relPath = canonicalFirstPath;
        int lastSeparatorIndex = canonicalFirstPath.substring(0, index).lastIndexOf(File.separator);
        if (lastSeparatorIndex != -1) {
            String absRest = canonicalSecondPath.substring(lastSeparatorIndex + 1);
            StringBuffer relPathBuffer = new StringBuffer();
            while (absRest.indexOf(File.separator) >= 0) {
                relPathBuffer.append(".." + File.separator);
                absRest = absRest.substring(absRest.indexOf(File.separator) + 1);
            }
            relPathBuffer.append(canonicalFirstPath.substring(lastSeparatorIndex + 1));
            relPath = relPathBuffer.toString();
        }
        return relPath;
    }

    /**
     * Waits for process to die and writes log messages. Terminates if exit value is not 0.
     */
    public static void waitForProcess(Operator operator, Process process, String name) throws OperatorException {
        try {
            LogService.getGlobal().log("Waiting for process '" + name + "' to die.", LogService.MINIMUM);
            int value = process.waitFor();
            if (value == 0) {
                LogService.getGlobal().log("Process '" + name + "' terminated successfully.", LogService.STATUS);
            } else {
                throw new UserError(operator, 306, new Object[] { name, value });
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for process '" + name + "' to die.", e);
        }
    }

    /**
     * @deprecated Use {@link MailUtilities#sendEmail(String,String,String)} instead
     */
    @Deprecated
    public static void sendEmail(String address, String subject, String content) {
        MailUtilities.sendEmail(address, subject, content);
    }

    /** Adds a new resource source. Might be used by plugins etc. */
    public static void addResourceSource(ResourceSource source) {
        ALL_RESOURCE_SOURCES.add(source);
    }

    /** Adds a new resource source before the others. Might be used by plugins etc. */
    public static void prependResourceSource(ResourceSource source) {
        ALL_RESOURCE_SOURCES.addFirst(source);
    }

    public static URL getResource(ClassLoader loader, String name) {
        return getResource(loader, RESOURCE_PREFIX, name);
    }

    public static URL getResource(ClassLoader loader, String prefix, String name) {
        return loader.getResource(prefix + name);
    }

    /**
     * Returns the desired resource. Tries first to find a resource in the core RapidMiner resources directory. If no
     * resource with the given name is found, it is tried to load with help of the ResourceSource which might have been
     * added by plugins. Please note that resource names are only allowed to use '/' as separator instead of
     * File.separator!
     */
    public static URL getResource(String name) {
        Iterator<ResourceSource> i = ALL_RESOURCE_SOURCES.iterator();
        while (i.hasNext()) {
            ResourceSource source = i.next();
            URL url = source.getResource(name);
            if (url != null) {
                return url;
            }
        }
        URL resourceURL = getResource(Plugin.getMajorClassLoader(), name);
        if (resourceURL != null) {
            return resourceURL;
        } else {
            return null;
        }
    }

    public static String readTextFile(InputStream in) throws IOException {
        return readTextFile(new InputStreamReader(in, "UTF-8"));
    }

    /** Reads a text file into a single string. */
    public static String readTextFile(File file) throws IOException {
        return readTextFile(new FileReader(file));
    }

    public static String readTextFile(Reader r) throws IOException {
        StringBuilder contents = new StringBuilder();
        BufferedReader reader = new BufferedReader(r);
        String line = "";
        try {
            while ((line = reader.readLine()) != null) {
                contents.append(line + Tools.getLineSeparator());
            }
        } finally {
            reader.close();
        }
        return contents.toString();
    }

    public static void writeTextFile(File file, String text) throws IOException {
        FileWriter out = new FileWriter(file);
        try {
            out.write(text);
        } finally {
            out.close();
        }
    }

    public static final String[] TRUE_STRINGS = { "true", "on", "yes", "y" };

    public static final String[] FALSE_STRINGS = { "false", "off", "no", "n" };

    public static boolean booleanValue(String string, boolean deflt) {
        if (string == null) return deflt;
        string = string.toLowerCase().trim();
        for (String element : TRUE_STRINGS) {
            if (element.equals(string)) {
                return true;
            }
        }
        for (String element : FALSE_STRINGS) {
            if (element.equals(string)) {
                return false;
            }
        }
        return deflt;
    }

    public static File findSourceFile(StackTraceElement e) {
        try {
            Class clazz = Class.forName(e.getClassName());
            while (clazz.getDeclaringClass() != null) clazz = clazz.getDeclaringClass();
            String filename = clazz.getName().replace('.', File.separatorChar);
            return FileSystemService.getSourceFile(filename + ".java");
        } catch (Throwable t) {
        }
        String filename = e.getClassName().replace('.', File.separatorChar);
        return FileSystemService.getSourceFile(filename + ".java");
    }

    public static Process launchFileEditor(File file, int line) throws IOException {
        String editor = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_EDITOR);
        if (editor == null) throw new IOException("Property 'rapidminer.tools.editor' undefined.");
        editor = editor.replaceAll("%f", file.getAbsolutePath());
        editor = editor.replaceAll("%l", line + "");
        return Runtime.getRuntime().exec(editor);
    }

    /** Replaces angle brackets by html entities. */
    public static String escapeXML(String string) {
        if (string == null) return "null";
        return StringEscapeUtils.escapeXml(string);
    }

    /**
     * This method will encode the given string by replacing all forbidden characters by the appropriate HTML entity.
     */
    public static String escapeHTML(String string) {
        return StringEscapeUtils.escapeHtml(string);
    }

    public static void findImplementationsInJar(JarFile jar, Class superClass, List<String> implementations) {
        findImplementationsInJar(Tools.class.getClassLoader(), jar, superClass, implementations);
    }

    public static void findImplementationsInJar(ClassLoader loader, JarFile jar, Class<?> superClass, List<String> implementations) {
        Enumeration<JarEntry> e = jar.entries();
        while (e.hasMoreElements()) {
            JarEntry entry = e.nextElement();
            String name = entry.getName();
            int dotClass = name.lastIndexOf(".class");
            if (dotClass < 0) continue;
            name = name.substring(0, dotClass);
            name = name.replaceAll("/", "\\.");
            try {
                Class<?> c = loader.loadClass(name);
                if (superClass.isAssignableFrom(c)) {
                    if (!java.lang.reflect.Modifier.isAbstract(c.getModifiers())) {
                        implementations.add(name);
                    }
                }
            } catch (Throwable t) {
            }
        }
    }

    /** TODO: Looks like this can be replaced by {@link Plugin#getMajorClassLoader()} */
    public static Class classForName(String className) throws ClassNotFoundException {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
        }
        try {
            return ClassLoader.getSystemClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
        }
        Iterator i = Plugin.getAllPlugins().iterator();
        while (i.hasNext()) {
            Plugin p = (Plugin) i.next();
            try {
                return p.getClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
            }
        }
        throw new ClassNotFoundException(className);
    }

    /**
     * Splits the given line according to the given separator pattern while only those separators will be regarded not
     * lying inside of a quoting string. Please note that quoting characters will not be regarded if they are escaped by
     * an escaping character. The usual double quote (&quot;) is used for quoting and can be escaped by a backslash,
     * i.e. \&quot;.
     */
    public static String[] quotedSplit(String line, Pattern separatorPattern) {
        return quotedSplit(line, separatorPattern, '"', '\\');
    }

    /**
     * Splits the given line according to the given separator pattern while only those separators will be regarded not
     * lying inside of a quoting string. Please note that quoting characters will not be regarded if they are escaped by
     * an escaping character.
     */
    public static String[] quotedSplit(String line, Pattern separatorPattern, char quotingChar, char escapeChar) {
        int[] quoteSplitIndices = new int[line.length()];
        char lastChar = '0';
        int lastSplitIndex = -1;
        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);
            if (currentChar == quotingChar) {
                boolean escaped = false;
                if (i != 0 && lastChar == escapeChar) {
                    escaped = true;
                }
                if (!escaped) {
                    quoteSplitIndices[++lastSplitIndex] = i;
                }
            }
            lastChar = currentChar;
        }
        List<String> quotedSplits = new LinkedList<String>();
        if (lastSplitIndex < 0) {
            line = line.replaceAll("\\\\\"", "\"");
            quotedSplits.add(line);
        } else {
            int start = 0;
            for (int i = 0; i <= lastSplitIndex; i++) {
                int end = quoteSplitIndices[i];
                String part = "";
                if (end > start) {
                    part = line.substring(start, end);
                }
                part = part.replaceAll("\\\\\"", "\"");
                quotedSplits.add(part);
                start = end + 1;
            }
            if (start < line.length()) {
                String part = line.substring(start);
                part = part.replaceAll("\\\\\"", "\"");
                quotedSplits.add(part);
            }
        }
        List<String> result = new LinkedList<String>();
        boolean isSplitPart = true;
        int index = 0;
        for (String part : quotedSplits) {
            if (index > 0 || part.trim().length() > 0) {
                if (isSplitPart) {
                    String[] separatedParts = separatorPattern.split(part, -1);
                    for (int s = 0; s < separatedParts.length; s++) {
                        String currentPart = separatedParts[s].trim();
                        if (currentPart.length() == 0) {
                            if (s == 0 && index == 0) {
                                result.add(currentPart);
                            } else if (s == separatedParts.length - 1 && index == quotedSplits.size() - 1) {
                                result.add(currentPart);
                            } else if (s > 0 && s < separatedParts.length - 1) {
                                result.add(currentPart);
                            }
                        } else {
                            result.add(currentPart);
                        }
                    }
                } else {
                    result.add(part);
                }
            }
            isSplitPart = !isSplitPart;
            index++;
        }
        String[] resultArray = new String[result.size()];
        result.toArray(resultArray);
        return resultArray;
    }

    /**
     * This method merges quoted splits, e.g. if a string line should be splitted by comma and commas inside of a quoted
     * string should not be used as splitting point.
     * 
     * @param line
     *            the original line
     * @param splittedTokens
     *            the tokens as they were originally splitted
     * @param quoteString
     *            the string which should be used as quote indicator, e.g. &quot; or '
     * @return the array of strings where the given quoteString was regarded
     * @throws IOException
     *             if an open quote was not ended
     * 
     * @deprecated Please use {@link #quotedSplit(String, Pattern, char, char)} or {@link #quotedSplit(String, Pattern)}
     *             instead
     */
    @Deprecated
    public static String[] mergeQuotedSplits(String line, String[] splittedTokens, String quoteString) throws IOException {
        int[] tokenStarts = new int[splittedTokens.length];
        int currentCounter = 0;
        int currentIndex = 0;
        for (String currentToken : splittedTokens) {
            tokenStarts[currentIndex] = line.indexOf(currentToken, currentCounter);
            currentCounter = tokenStarts[currentIndex] + currentToken.length() + 1;
            currentIndex++;
        }
        List<String> tokens = new LinkedList<String>();
        int start = -1;
        int end = -1;
        for (int i = 0; i < splittedTokens.length; i++) {
            if (splittedTokens[i].trim().startsWith(quoteString)) {
                start = i;
            }
            if (start >= 0) {
                StringBuffer current = new StringBuffer();
                while (end < 0 && i < splittedTokens.length) {
                    if (splittedTokens[i].endsWith(quoteString)) {
                        end = i;
                        break;
                    }
                    i++;
                }
                if (end < 0) throw new IOException("Error during reading: open quote \" is not ended!");
                String lastToken = null;
                for (int a = start; a <= end; a++) {
                    String nextToken = splittedTokens[a];
                    if (nextToken.length() == 0) continue;
                    if (a == start) {
                        nextToken = nextToken.substring(quoteString.length());
                    }
                    if (a == end) {
                        nextToken = nextToken.substring(0, nextToken.length() - quoteString.length());
                    }
                    if (lastToken != null) {
                        int lastIndex = tokenStarts[a - 1] + lastToken.length();
                        int thisIndex = tokenStarts[a];
                        if (lastIndex >= 0 && thisIndex >= lastIndex) {
                            String separator = line.substring(lastIndex, thisIndex);
                            current.append(separator);
                        }
                    }
                    current.append(nextToken);
                    lastToken = splittedTokens[a];
                }
                tokens.add(current.toString());
                start = -1;
                end = -1;
            } else {
                tokens.add(splittedTokens[i]);
            }
        }
        String[] quoted = new String[tokens.size()];
        tokens.toArray(quoted);
        return quoted;
    }

    /** Delivers the next token and skip empty lines. */
    public static void getFirstToken(StreamTokenizer tokenizer) throws IOException {
        while (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
        }
        ;
        if (tokenizer.ttype == '\'' || tokenizer.ttype == '"') {
            tokenizer.ttype = StreamTokenizer.TT_WORD;
        } else if (tokenizer.ttype == StreamTokenizer.TT_WORD && tokenizer.sval.equals("?")) {
            tokenizer.ttype = '?';
        }
    }

    /** Delivers the next token and checks if its the end of line. */
    public static void getLastToken(StreamTokenizer tokenizer, boolean endOfFileOk) throws IOException {
        if (tokenizer.nextToken() != StreamTokenizer.TT_EOL && (tokenizer.ttype != StreamTokenizer.TT_EOF || !endOfFileOk)) {
            throw new IOException("expected the end of the line " + tokenizer.lineno());
        }
    }

    /** Delivers the next token and checks for an unexpected end of line or file. */
    public static void getNextToken(StreamTokenizer tokenizer) throws IOException {
        if (tokenizer.nextToken() == StreamTokenizer.TT_EOL) {
            throw new IOException("unexpected end of line " + tokenizer.lineno());
        }
        if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
            throw new IOException("unexpected end of file in line " + tokenizer.lineno());
        } else if (tokenizer.ttype == '\'' || tokenizer.ttype == '"') {
            tokenizer.ttype = StreamTokenizer.TT_WORD;
        } else if (tokenizer.ttype == StreamTokenizer.TT_WORD && tokenizer.sval.equals("?")) {
            tokenizer.ttype = '?';
        }
    }

    /** Skips all tokens before next end of line (EOL). */
    public static void waitForEOL(StreamTokenizer tokenizer) throws IOException {
        while (tokenizer.nextToken() != StreamTokenizer.TT_EOL) {
        }
        ;
        tokenizer.pushBack();
    }

    /** Deletes the file. If it is a directory, deletes recursively. */
    public static boolean delete(File file) {
        if (file.isDirectory()) {
            boolean success = true;
            File[] files = file.listFiles();
            for (File child : files) {
                success &= delete(child);
            }
            boolean result = file.delete();
            if (!result) {
                LogService.getRoot().warning("Unable to delete file " + file);
                return false;
            }
            return success;
        } else {
            boolean result = file.delete();
            if (!result) {
                LogService.getRoot().warning("Unable to delete file " + file);
            }
            return result;
        }
    }

    public static void copy(File srcPath, File dstPath) throws IOException {
        if (srcPath.isDirectory()) {
            if (!dstPath.exists()) {
                boolean result = dstPath.mkdir();
                if (!result) throw new IOException("Unable to create directoy: " + dstPath);
            }
            String[] files = srcPath.list();
            for (String file : files) {
                copy(new File(srcPath, file), new File(dstPath, file));
            }
        } else {
            if (srcPath.exists()) {
                FileChannel in = null;
                FileChannel out = null;
                try {
                    in = new FileInputStream(srcPath).getChannel();
                    out = new FileOutputStream(dstPath).getChannel();
                    long size = in.size();
                    MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
                    out.write(buf);
                } finally {
                    if (in != null) in.close();
                    if (out != null) out.close();
                }
            }
        }
    }

    /** Returns a whitespace with length indent. */
    public static String indent(int indent) {
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < indent; i++) s.append(" ");
        return s.toString();
    }

    public static String formatBytes(long numberOfBytes) {
        if (numberOfBytes > 1024 * 1024) {
            long mBytes = numberOfBytes / (1024 * 1024);
            if (mBytes >= 100) {
                return mBytes + " MB";
            } else {
                long remainder = (numberOfBytes - mBytes * 1024 * 1024) / 1024;
                return mBytes + "." + Long.toString(remainder).charAt(0) + " MB";
            }
        } else if (numberOfBytes > 1024) {
            return numberOfBytes / 1024 + " kB";
        } else {
            return numberOfBytes + " bytes";
        }
    }

    /**
     * Copies the contents read from the input stream to the output stream in the current thread. Both streams will be
     * closed, even in case of a failure.
     * 
     * @param closeOutputStream
     */
    public static void copyStreamSynchronously(InputStream in, OutputStream out, boolean closeOutputStream) throws IOException {
        byte[] buffer = new byte[1024 * 20];
        try {
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            out.flush();
            if (closeOutputStream) {
                out.close();
            }
        } finally {
            if (closeOutputStream && out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    /** Esacapes quotes, newlines, and backslashes. */
    public static String escape(String unescaped) {
        StringBuilder result = new StringBuilder();
        for (char c : unescaped.toCharArray()) {
            switch(c) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                default:
                    result.append(c);
                    break;
            }
        }
        return result.toString();
    }

    /**
     * 
     * Returns the column name of the the n'th column like excel names it.
     * 
     * @param index
     *            the index of the column
     * 
     * @return
     */
    public static String getExcelColumnName(int index) {
        if (index < 0) {
            return "error";
        }
        final Character[] alphabet = new Character[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
        int quotient = index / 26;
        if (quotient > 0) {
            return getExcelColumnName(quotient - 1) + alphabet[index % 26].toString();
        } else {
            return alphabet[index % 26].toString();
        }
    }

    /**
     * Replace quote chars in-quote characters by escapeChar+quotingChar
     * 
     * Example: seperatorPatern = ',' , quotingChar = '"' , escapeCahr = '\\'
     * 
     * line = '"Charles says: "Some people never go crazy, What truly horrible lives they must live"", 1968, "US"'
     * return = '"Charles says: \"Some people never go crazy, What truly horrible lives they must live\"", "1968", "US"'
     */
    public static String escapeQuoteCharsInQuotes(String line, Pattern separatorPattern, char quotingChar, char escapeChar, boolean showWarning) {
        char lastChar = '0';
        boolean openedQuote = false;
        List<Integer> rememberQuotePosition = new LinkedList<Integer>();
        for (int i = 0; i < line.length(); i++) {
            if (lastChar == quotingChar) {
                if (openedQuote) {
                    boolean matches = Pattern.matches(separatorPattern.pattern() + ".*", line.substring(i));
                    if (matches) {
                        openedQuote = false;
                    } else {
                        rememberQuotePosition.add(i - 1);
                    }
                } else {
                    openedQuote = true;
                }
            }
            lastChar = line.charAt(i);
        }
        if (openedQuote && lastChar == quotingChar) {
            openedQuote = false;
        }
        if (showWarning && !rememberQuotePosition.isEmpty()) {
            StringBuilder positions = new StringBuilder();
            int j = 1;
            for (int i = 0; i < rememberQuotePosition.size(); i++) {
                if (j % 10 == 0) {
                    positions.append("\n");
                }
                positions.append(rememberQuotePosition.get(i));
                if (i + 1 < rememberQuotePosition.size()) positions.append(", ");
                j++;
            }
            String lineBeginning = line;
            if (line.length() > 20) {
                lineBeginning = line.substring(0, 20);
            }
            String warning = "While reading the line starting with \n\n\t" + lineBeginning + "   ...\n\n" + ",an unescaped quote character was substituted by an escaped quote at the position(s) " + positions.toString() + ". " + "In particular der character '" + Character.toString(lastChar) + "' was replaced by '" + Character.toString(escapeChar) + Character.toString(lastChar) + ".";
            LogService.getGlobal().logWarning(warning);
        }
        if (!rememberQuotePosition.isEmpty()) {
            String newLine = "";
            int pos = rememberQuotePosition.remove(0);
            int i = 0;
            for (Character c : line.toCharArray()) {
                if (i == pos) {
                    newLine += Character.toString(escapeChar) + c;
                    if (!rememberQuotePosition.isEmpty()) {
                        pos = rememberQuotePosition.remove(0);
                    }
                } else {
                    newLine += c;
                }
                i++;
            }
            line = newLine;
        }
        return line;
    }

    public static String unescape(String escaped) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < escaped.length(); index++) {
            char c = escaped.charAt(index);
            switch(c) {
                case '\\':
                    if (index < escaped.length() - 1) {
                        index++;
                        char next = escaped.charAt(index);
                        switch(next) {
                            case 'n':
                                result.append('\n');
                                break;
                            case '\\':
                                result.append('\\');
                                break;
                            case '"':
                                result.append('"');
                                break;
                            default:
                                result.append('\\').append(next);
                        }
                    } else {
                        result.append('\\');
                    }
                    break;
                default:
                    result.append(c);
                    break;
            }
        }
        return result.toString();
    }

    /** As {@link #toString(Collection, String)} with ", ". */
    public static String toString(Collection<?> collection) {
        return toString(collection, ", ");
    }

    /**
     * Returns a string containing the toString()-representation of the elements of collection, separated by the given
     * separator.
     */
    public static String toString(Collection<?> collection, String separator) {
        boolean first = true;
        StringBuilder b = new StringBuilder();
        for (Object o : collection) {
            if (first) {
                first = false;
            } else {
                b.append(separator);
            }
            b.append(o);
        }
        return b.toString();
    }

    public static String toString(Object[] collection) {
        return toString(collection, ", ");
    }

    public static String toString(Object[] collection, String separator) {
        if (collection == null) {
            return null;
        } else {
            return toString(Arrays.asList(collection), separator);
        }
    }

    public static String formatSizeInBytes(long bytes) {
        long result = bytes;
        long rest = 0;
        int unit = 0;
        while (result > 1024) {
            rest = result % 1024;
            result /= 1024;
            unit++;
            if (unit >= Tools.MEMORY_UNITS.length - 1) break;
        }
        if (result < 10 && unit > 0) {
            return result + "." + 10 * rest / 1024 + " " + Tools.MEMORY_UNITS[unit];
        } else {
            return result + " " + Tools.MEMORY_UNITS[unit];
        }
    }

    /**
     * This method will return a byte array containing the raw data from the given url. If any error occurs null will be
     * returned. Please keep in mind that in order to load the data, the data will be stored in memory twice.
     */
    public static byte[] readUrl(URL url) {
        BufferedInputStream in = null;
        try {
            class Part {

                byte[] partData;

                int len;
            }
            in = new BufferedInputStream(url.openStream());
            LinkedList<Part> parts = new LinkedList<Part>();
            int len = 1;
            while (len > 0) {
                byte[] data = new byte[1024];
                len = in.read(data);
                if (len > 0) {
                    Part part = new Part();
                    part.partData = data;
                    part.len = len;
                    parts.add(part);
                }
            }
            int length = 0;
            for (Part part : parts) length += part.len;
            byte[] result = new byte[length];
            int pos = 0;
            for (Part part : parts) {
                System.arraycopy(part.partData, 0, result, pos, part.len);
                pos += part.len;
            }
            return result;
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /** Prefixes every occurrence */
    public static String escape(String source, char escapeChar, char[] specialCharacters) {
        if (source == null) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (char c : source.toCharArray()) {
            if (c == escapeChar) {
                b.append(escapeChar);
            } else {
                for (char s : specialCharacters) {
                    if (c == s) {
                        b.append(escapeChar);
                        break;
                    }
                }
            }
            b.append(c);
        }
        return b.toString();
    }

    /** Splits the string at every split character unless escaped. */
    public static List<String> unescape(String source, char escapeChar, char[] specialCharacters, char splitCharacter) {
        return unescape(source, escapeChar, specialCharacters, splitCharacter, -1);
    }

    /**
     * Splits the string at every split character unless escaped. If the split limit is not -1, at most so many tokens
     * will be returned. No more escaping is performed in the last token!
     */
    public static List<String> unescape(String source, char escapeChar, char[] specialCharacters, char splitCharacter, int splitLimit) {
        List<String> result = new LinkedList<String>();
        StringBuilder b = new StringBuilder();
        boolean readEscape = false;
        int indexCount = -1;
        for (char c : source.toCharArray()) {
            indexCount++;
            if (readEscape) {
                boolean found = false;
                if (c == splitCharacter) {
                    found = true;
                    b.append(c);
                } else if (c == escapeChar) {
                    found = true;
                    b.append(c);
                } else {
                    for (char s : specialCharacters) {
                        if (s == c) {
                            found = true;
                            b.append(c);
                            break;
                        }
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("String '" + source + "' contains illegal escaped character '" + c + "'.");
                }
                readEscape = false;
            } else if (c == escapeChar) {
                readEscape = true;
            } else if (c == splitCharacter) {
                readEscape = false;
                result.add(b.toString());
                if (splitLimit != -1) {
                    if (result.size() == splitLimit - 1) {
                        result.add(source.substring(indexCount + 1));
                        return result;
                    }
                }
                b = new StringBuilder();
            } else {
                readEscape = false;
                b.append(c);
            }
        }
        result.add(b.toString());
        return result;
    }

    /** In contrast to o1.equals(o2), this method also works with p1==null. */
    public static boolean equals(Object o1, Object o2) {
        if (o1 != null) return o1.equals(o2);
        if (o1 == null && o2 == null) return true;
        return false;
    }
}
