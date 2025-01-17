package com.amwebexpert.tags.workbook.model;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

/**
 * DataFormatter contains methods for formatting the value stored in an Cell.
 * This can be useful for reports and GUI presentations when you need to display
 * data exactly as it appears in Excel. Supported formats include currency, SSN,
 * percentages, decimals, dates, phone numbers, zip codes, etc.
 * <p>
 * Internally, formats will be implemented using subclasses of {@link Format}
 * such as {@link DecimalFormat} and {@link SimpleDateFormat}. Therefore the
 * formats used by this class must obey the same pattern rules as these Format
 * subclasses. This means that only legal number pattern characters ("0", "#",
 * ".", "," etc.) may appear in number formats. Other characters can be inserted
 * <em>before</em> or <em> after</em> the number pattern to form a prefix or
 * suffix.
 * </p>
 * <p>
 * For example the Excel pattern <code>"$#,##0.00 "USD"_);($#,##0.00 "USD")"
 * </code> will be correctly formatted as "$1,000.00 USD" or "($1,000.00 USD)".
 * However the pattern <code>"00-00-00"</code> is incorrectly formatted by
 * DecimalFormat as "000000--". For Excel formats that are not compatible with
 * DecimalFormat, you can provide your own custom {@link Format} implementation
 * via <code>DataFormatter.addFormat(String,Format)</code>. The following custom
 * formats are already provided by this class:
 * </p>
 * 
 * <pre>
 * &lt;ul&gt;&lt;li&gt;SSN &quot;000-00-0000&quot;&lt;/li&gt;
 *     &lt;li&gt;Phone Number &quot;(###) ###-####&quot;&lt;/li&gt;
 *     &lt;li&gt;Zip plus 4 &quot;00000-0000&quot;&lt;/li&gt;
 * &lt;/ul&gt;
 * </pre>
 * <p>
 * If the Excel format pattern cannot be parsed successfully, then a default
 * format will be used. The default number format will mimic the Excel General
 * format: "#" for whole numbers and "#.##########" for decimal numbers. You can
 * override the default format pattern with <code>
 * DataFormatter.setDefaultNumberFormat(Format)</code> . <b>Note:</b> the
 * default format will only be used when a Format cannot be created from the
 * cell's data format string.
 * 
 * @author James May (james dot may at fmr dot com)
 * 
 */
public class DataFormatterUtil {

    private static final String FILENAME_NUMBER_FMT = "workbook-tag-number-formats.properties";

    private static final String FILENAME_DATE_FMT = "workbook-tag-date-formats.properties";

    /** Pattern to find a number format: "0" or "#" */
    private static final Pattern numPattern = Pattern.compile("[0#]+");

    /** Pattern to find days of week as text "ddd...." */
    private static final Pattern daysAsText = Pattern.compile("([d]{3,})", Pattern.CASE_INSENSITIVE);

    /** Pattern to find "AM/PM" marker */
    private static final Pattern amPmPattern = Pattern.compile("((A|P)[M/P]*)", Pattern.CASE_INSENSITIVE);

    /** A regex to find patterns like [$$-1009] and [$?-452]. */
    private static final Pattern specialPatternGroup = Pattern.compile("(\\[\\$[^-\\]]*-[0-9A-Z]+\\])");

    /** <em>General</em> format for whole numbers. */
    private static final Format generalWholeNumFormat = new DecimalFormat("#");

    /** <em>General</em> format for decimal numbers. */
    private static final Format generalDecimalNumFormat = new DecimalFormat("#.##");

    /** A default format to use when a number pattern cannot be parsed. */
    private Format defaultNumFormat;

    /**
	 * A map to cache formats. Map<String,Format> formats
	 */
    private final Map formats;

    private boolean logWarnings;

    /**
	 * Constructor
	 */
    public DataFormatterUtil(boolean logWarnings) {
        formats = new HashMap();
        Format zipFormat = ZipPlusFourFormat.instance;
        addFormat("00000\\-0000", zipFormat);
        addFormat("00000-0000", zipFormat);
        Format phoneFormat = PhoneFormat.instance;
        addFormat("[<=9999999]###\\-####;\\(###\\)\\ ###\\-####", phoneFormat);
        addFormat("[<=9999999]###-####;(###) ###-####", phoneFormat);
        addFormat("###\\-####;\\(###\\)\\ ###\\-####", phoneFormat);
        addFormat("###-####;(###) ###-####", phoneFormat);
        Format ssnFormat = SSNFormat.instance;
        addFormat("000\\-00\\-0000", ssnFormat);
        addFormat("000-00-0000", ssnFormat);
        Format fiscalFormat = new DecimalFormat("#,##0.00$;(#,##0.00)$");
        addFormat("_-* #,##0 $_-;_-* #,##0 $-;_-* \"-\" $_-;_-@_-", fiscalFormat);
        addFormat("_-* #,##0 _$_-;_-* #,##0 _$-;_-* \"-\" _$_-;_-@_-", fiscalFormat);
        addFormat("_-* #,##0.00 $_-;_-* #,##0.00 $-;_-* \"-\"?? $_-;_-@_-", fiscalFormat);
        addFormat("_-* #,##0.00 _$_-;_-* #,##0.00 _$-;_-* \"-\"?? _$_-;_-@_-", fiscalFormat);
        addFormat("_ * #,##0.00_) _$_ ;_ * (#,##0.00) _$_ ;_ * \"-\"??_) _$_ ;_ @_ ", fiscalFormat);
        addFormat("_ * #,##0.00_) $_ ;_ * (#,##0.00) $_ ;_ * \"-\"??_) $_ ;_ @_ ", fiscalFormat);
        addFormat("_ * #,##0_) _$_ ;_ * (#,##0) _$_ ;_ * \"-\"_) _$_ ;_ @_ ", fiscalFormat);
        addFormat("_ * #,##0_) $_ ;_ * (#,##0) $_ ;_ * \"-\"_) $_ ;_ @_ ", fiscalFormat);
        Format fiscalFormat2 = new DecimalFormat("$#,##0.00;($#,##0.00)");
        addFormat("_($* #,##0_);_($* (#,##0);_($* \"-\"_);_(@_)", fiscalFormat2);
        addFormat("_($* #,##0.00_);_($* (#,##0.00);_($* \"-\"??_);_(@_)", fiscalFormat2);
        addFormat("_($*#,##0.00_);_($*(#,##0.00);_($*\"-\"??_);_(@_)", fiscalFormat2);
        Format decFormat = new DecimalFormat("#,##0.00;(#,##0.00)");
        addFormat("_(* #,##0_);_(* (#,##0);_(* \"-\"_);_(@_)", decFormat);
        addFormat("_(* #,##0.00_);_(* (#,##0.00);_(* \"-\"??_);_(@_)", decFormat);
        loadNumberFormats();
        loadDateFormats();
        this.logWarnings = logWarnings;
    }

    private void loadNumberFormats() {
        String fileToLocate = "/" + FILENAME_NUMBER_FMT;
        URL url = getClass().getClassLoader().getResource(fileToLocate);
        if (url == null) {
            return;
        }
        List<String> lines;
        try {
            lines = IOUtils.readLines(url.openStream());
        } catch (IOException e) {
            throw new ConfigurationException("Problem loading file " + fileToLocate, e);
        }
        for (String line : lines) {
            if (line.startsWith("#") || StringUtils.isBlank(line)) {
                continue;
            }
            String[] parts = StringUtils.split(line, "=");
            addFormat(parts[0], new DecimalFormat(parts[1]));
        }
    }

    private void loadDateFormats() {
        String fileToLocate = "/" + FILENAME_DATE_FMT;
        URL url = getClass().getClassLoader().getResource(fileToLocate);
        if (url == null) {
            return;
        }
        List<String> lines;
        try {
            lines = IOUtils.readLines(url.openStream());
        } catch (IOException e) {
            throw new ConfigurationException("Problem loading file " + fileToLocate, e);
        }
        for (String line : lines) {
            if (line.startsWith("#") || StringUtils.isBlank(line)) {
                continue;
            }
            String[] parts = StringUtils.split(line, "=");
            addFormat(parts[0], new SimpleDateFormat(parts[1]));
        }
    }

    /**
	 * Return a Format for the given cell if one exists, otherwise try to create
	 * one. This method will return <code>null</code> if the any of the
	 * following is true:
	 * <ul>
	 * <li>the cell's style is null</li>
	 * <li>the style's data format string is null or empty</li>
	 * <li>the format string cannot be recognized as either a number or date</li>
	 * </ul>
	 * 
	 * @param cell
	 *            The cell to retrieve a Format for
	 * @return A Format for the format String
	 */
    public Format getFormat(Cell cell) {
        if (cell.getCellStyle() == null) {
            return null;
        }
        int formatIndex = cell.getCellStyle().getDataFormat();
        String formatStr = cell.getCellStyle().getDataFormatString();
        if (formatStr == null || formatStr.trim().length() == 0) {
            return null;
        }
        return getFormat(cell.getNumericCellValue(), formatIndex, formatStr);
    }

    private Format getFormat(double cellValue, int formatIndex, String formatStr) {
        Format format = (Format) formats.get(formatStr);
        if (format != null) {
            return format;
        }
        if (formatStr.equalsIgnoreCase("General") || formatStr.equals("@")) {
            if (DataFormatterUtil.isWholeNumber(cellValue)) {
                return generalWholeNumFormat;
            }
            return generalDecimalNumFormat;
        }
        if (formatStr.equalsIgnoreCase("[=1]\"�\";\"\"")) {
            return new CheckBoxFormat();
        }
        format = createFormat(cellValue, formatIndex, formatStr);
        formats.put(formatStr, format);
        return format;
    }

    /**
	 * Create and return a Format based on the format string from a cell's
	 * style. If the pattern cannot be parsed, return a default pattern.
	 * 
	 * @param cell
	 *            The Excel cell
	 * @return A Format representing the excel format. May return null.
	 */
    public Format createFormat(Cell cell) {
        int formatIndex = cell.getCellStyle().getDataFormat();
        String formatStr = cell.getCellStyle().getDataFormatString();
        return createFormat(cell.getNumericCellValue(), formatIndex, formatStr);
    }

    private Format createFormat(double cellValue, int formatIndex, String sFormat) {
        String formatStr = sFormat.replaceAll("\\[[a-zA-Z]*\\]", "");
        Matcher m = specialPatternGroup.matcher(formatStr);
        while (m.find()) {
            String match = m.group();
            String symbol = match.substring(match.indexOf('$') + 1, match.indexOf('-'));
            if (symbol.indexOf('$') > -1) {
                StringBuffer sb = new StringBuffer();
                sb.append(symbol.substring(0, symbol.indexOf('$')));
                sb.append('\\');
                sb.append(symbol.substring(symbol.indexOf('$'), symbol.length()));
                symbol = sb.toString();
            }
            formatStr = m.replaceAll(symbol);
            m = specialPatternGroup.matcher(formatStr);
        }
        if (formatStr == null || formatStr.trim().length() == 0) {
            return getDefaultFormat(cellValue);
        }
        if (DateUtil.isADateFormat(formatIndex, formatStr) && DateUtil.isValidExcelDate(cellValue)) {
            return createDateFormat(formatStr, cellValue);
        }
        if (numPattern.matcher(formatStr).find()) {
            return createNumberFormat(formatStr, cellValue);
        }
        return null;
    }

    private Format createDateFormat(String pFormatStr, double cellValue) {
        String formatStr = pFormatStr;
        formatStr = formatStr.replaceAll("\\\\-", "-");
        formatStr = formatStr.replaceAll("\\\\,", ",");
        formatStr = formatStr.replaceAll("\\\\ ", " ");
        formatStr = formatStr.replaceAll(";@", "");
        boolean hasAmPm = false;
        Matcher amPmMatcher = amPmPattern.matcher(formatStr);
        while (amPmMatcher.find()) {
            formatStr = amPmMatcher.replaceAll("@");
            hasAmPm = true;
            amPmMatcher = amPmPattern.matcher(formatStr);
        }
        formatStr = formatStr.replaceAll("@", "a");
        Matcher dateMatcher = daysAsText.matcher(formatStr);
        if (dateMatcher.find()) {
            String match = dateMatcher.group(0);
            formatStr = dateMatcher.replaceAll(match.toUpperCase().replaceAll("D", "E"));
        }
        StringBuffer sb = new StringBuffer();
        char[] chars = formatStr.toCharArray();
        boolean mIsMonth = true;
        List ms = new ArrayList();
        for (int j = 0; j < chars.length; j++) {
            char c = chars[j];
            if (c == 'h' || c == 'H') {
                mIsMonth = false;
                if (hasAmPm) {
                    sb.append('h');
                } else {
                    sb.append('H');
                }
            } else if (c == 'm') {
                if (mIsMonth) {
                    sb.append('M');
                    ms.add(new Integer(sb.length() - 1));
                } else {
                    sb.append('m');
                }
            } else if (c == 's' || c == 'S') {
                sb.append('s');
                for (int i = 0; i < ms.size(); i++) {
                    int index = ((Integer) ms.get(i)).intValue();
                    if (sb.charAt(index) == 'M') {
                        sb.replace(index, index + 1, "m");
                    }
                }
                mIsMonth = true;
                ms.clear();
            } else if (Character.isLetter(c)) {
                mIsMonth = true;
                ms.clear();
                if (c == 'y' || c == 'Y') {
                    sb.append('y');
                } else if (c == 'd' || c == 'D') {
                    sb.append('d');
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        formatStr = sb.toString();
        try {
            return new SimpleDateFormat(formatStr);
        } catch (IllegalArgumentException iae) {
            if (logWarnings) {
                System.err.println("Format not found : " + formatStr + "\n\tConsider using " + FILENAME_DATE_FMT + " for corresponding Excel date formats with Java SimpleDateFormat");
            }
            return getDefaultFormat(cellValue);
        }
    }

    private Format createNumberFormat(String formatStr, double cellValue) {
        StringBuffer sb = new StringBuffer(formatStr);
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '(') {
                int idx = sb.indexOf(")", i);
                if (idx > -1 && sb.charAt(idx - 1) == '_') {
                    sb.deleteCharAt(idx);
                    sb.deleteCharAt(idx - 1);
                    sb.deleteCharAt(i);
                    i--;
                }
            } else if (c == ')' && i > 0 && sb.charAt(i - 1) == '_') {
                sb.deleteCharAt(i);
                sb.deleteCharAt(i - 1);
                i--;
            } else if (c == '\\' || c == '"') {
                sb.deleteCharAt(i);
                i--;
            } else if (c == '+' && i > 0 && sb.charAt(i - 1) == 'E') {
                sb.deleteCharAt(i);
                i--;
            }
        }
        String fmt = sb.toString();
        fmt = StringUtils.remove(fmt, "_-");
        try {
            return new DecimalFormat(fmt);
        } catch (IllegalArgumentException iae) {
            if (logWarnings) {
                System.err.println("Format not found : " + formatStr + "\n\tConsider using " + FILENAME_NUMBER_FMT + " for corresponding Excel number formats with Java DecimalFormat: " + iae.getMessage());
            }
            return getDefaultFormat(cellValue);
        }
    }

    /**
	 * Return true if the double value represents a whole number
	 * 
	 * @param d
	 *            the double value to check
	 * @return <code>true</code> if d is a whole number
	 */
    private static boolean isWholeNumber(double d) {
        return d == Math.floor(d);
    }

    /**
	 * Returns a default format for a cell.
	 * 
	 * @param cell
	 *            The cell
	 * @return a default format
	 */
    public Format getDefaultFormat(Cell cell) {
        return getDefaultFormat(cell.getNumericCellValue());
    }

    private Format getDefaultFormat(double cellValue) {
        if (defaultNumFormat != null) {
            return defaultNumFormat;
        }
        if (isWholeNumber(cellValue)) {
            return generalWholeNumFormat;
        }
        return generalDecimalNumFormat;
    }

    /**
	 * Returns the formatted value of an Excel date as a <tt>String</tt> based
	 * on the cell's <code>DataFormat</code>. i.e. "Thursday, January 02, 2003"
	 * , "01/02/2003" , "02-Jan" , etc.
	 * 
	 * @param cell
	 *            The cell
	 * @return a formatted date string
	 */
    private String getFormattedDateString(Cell cell) {
        Format dateFormat = getFormat(cell);
        Date d = cell.getDateCellValue();
        if (dateFormat != null) {
            return dateFormat.format(d);
        }
        return d.toString();
    }

    /**
	 * Returns the formatted value of an Excel number as a <tt>String</tt> based
	 * on the cell's <code>DataFormat</code>. Supported formats include
	 * currency, percents, decimals, phone number, SSN, etc.: "61.54%",
	 * "$100.00", "(800) 555-1234".
	 * 
	 * @param cell
	 *            The cell
	 * @return a formatted number string
	 */
    private String getFormattedNumberString(Cell cell) {
        Format numberFormat = getFormat(cell);
        double d = cell.getNumericCellValue();
        if (numberFormat == null) {
            return String.valueOf(d);
        }
        return numberFormat.format(new Double(d));
    }

    /**
	 * Formats the given raw cell value, based on the supplied format index and
	 * string, according to excel style rules.
	 * 
	 * @see #formatCellValue(Cell)
	 */
    public String formatRawCellContents(double value, int formatIndex, String formatString) {
        if (DateUtil.isADateFormat(formatIndex, formatString) && DateUtil.isValidExcelDate(value)) {
            Format dateFormat = getFormat(value, formatIndex, formatString);
            Date d = DateUtil.getJavaDate(value);
            if (dateFormat == null) {
                return d.toString();
            }
            return dateFormat.format(d);
        }
        Format numberFormat = getFormat(value, formatIndex, formatString);
        if (numberFormat == null) {
            return String.valueOf(value);
        }
        return numberFormat.format(new Double(value));
    }

    /**
	 * <p>
	 * Returns the formatted value of a cell as a <tt>String</tt> regardless of
	 * the cell type. If the Excel format pattern cannot be parsed then the cell
	 * value will be formatted using a default format.
	 * </p>
	 * <p>
	 * When passed a null or blank cell, this method will return an empty String
	 * (""). Formulas in formula type cells will not be evaluated.
	 * </p>
	 * 
	 * @param cell
	 *            The cell
	 * @return the formatted cell value as a String
	 */
    public String formatCellValue(Cell cell) {
        return formatCellValue(cell, null);
    }

    /**
	 * <p>
	 * Returns the formatted value of a cell as a <tt>String</tt> regardless of
	 * the cell type. If the Excel format pattern cannot be parsed then the cell
	 * value will be formatted using a default format.
	 * </p>
	 * <p>
	 * When passed a null or blank cell, this method will return an empty String
	 * (""). Formula cells will be evaluated using the given
	 * {@link FormulaEvaluator} if the evaluator is non-null. If the evaluator
	 * is null, then the formula String will be returned. The caller is
	 * responsible for setting the currentRow on the evaluator
	 * </p>
	 * 
	 * @param cell
	 *            The cell (can be null)
	 * @param evaluator
	 *            The FormulaEvaluator (can be null)
	 * @return a string value of the cell
	 */
    public String formatCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        int cellType = cell.getCellType();
        if (cellType == Cell.CELL_TYPE_FORMULA) {
            if (evaluator == null) {
                return cell.getCellFormula();
            }
            cellType = evaluator.evaluateFormulaCell(cell);
        }
        switch(cellType) {
            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return getFormattedDateString(cell);
                }
                return getFormattedNumberString(cell);
            case Cell.CELL_TYPE_STRING:
                return cell.getRichStringCellValue().getString();
            case Cell.CELL_TYPE_BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_BLANK:
                return "";
        }
        throw new RuntimeException("Unexpected celltype (" + cellType + ")");
    }

    /**
	 * <p>
	 * Sets a default number format to be used when the Excel format cannot be
	 * parsed successfully. <b>Note:</b> This is a fall back for when an error
	 * occurs while parsing an Excel number format pattern. This will not affect
	 * cells with the <em>General</em> format.
	 * </p>
	 * <p>
	 * The value that will be passed to the Format's format method (specified by
	 * <code>java.text.Format#format</code>) will be a double value from a
	 * numeric cell. Therefore the code in the format method should expect a
	 * <code>Number</code> value.
	 * </p>
	 * 
	 * @param format
	 *            A Format instance to be used as a default
	 * @see java.text.Format#format
	 */
    public void setDefaultNumberFormat(Format format) {
        Iterator itr = formats.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry entry = (Map.Entry) itr.next();
            if (entry.getValue() == generalDecimalNumFormat || entry.getValue() == generalWholeNumFormat) {
                entry.setValue(format);
            }
        }
        defaultNumFormat = format;
    }

    /**
	 * Adds a new format to the available formats.
	 * <p>
	 * The value that will be passed to the Format's format method (specified by
	 * <code>java.text.Format#format</code>) will be a double value from a
	 * numeric cell. Therefore the code in the format method should expect a
	 * <code>Number</code> value.
	 * </p>
	 * 
	 * @param excelFormatStr
	 *            The data format string
	 * @param format
	 *            A Format instance
	 */
    public void addFormat(String excelFormatStr, Format format) {
        formats.put(excelFormatStr, format);
    }

    static DecimalFormat createIntegerOnlyFormat(String fmt) {
        DecimalFormat result = new DecimalFormat(fmt);
        result.setParseIntegerOnly(true);
        return result;
    }

    /**
	 * Format class for Excel's SSN format. This class mimics Excel's built-in
	 * SSN formatting.
	 * 
	 * @author James May
	 */
    private static final class SSNFormat extends Format {

        public static final Format instance = new SSNFormat();

        private static final DecimalFormat df = createIntegerOnlyFormat("000000000");

        private SSNFormat() {
        }

        /** Format a number as an SSN */
        public static String format(Number num) {
            String result = df.format(num);
            StringBuffer sb = new StringBuffer();
            sb.append(result.substring(0, 3)).append('-');
            sb.append(result.substring(3, 5)).append('-');
            sb.append(result.substring(5, 9));
            return sb.toString();
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(format((Number) obj));
        }

        public Object parseObject(String source, ParsePosition pos) {
            return df.parseObject(source, pos);
        }
    }

    /**
	 * Format class for Excel Zip + 4 format. This class mimics Excel's built-in
	 * formatting for Zip + 4.
	 * 
	 * @author James May
	 */
    private static final class ZipPlusFourFormat extends Format {

        public static final Format instance = new ZipPlusFourFormat();

        private static final DecimalFormat df = createIntegerOnlyFormat("000000000");

        private ZipPlusFourFormat() {
        }

        /** Format a number as Zip + 4 */
        public static String format(Number num) {
            String result = df.format(num);
            StringBuffer sb = new StringBuffer();
            sb.append(result.substring(0, 5)).append('-');
            sb.append(result.substring(5, 9));
            return sb.toString();
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(format((Number) obj));
        }

        public Object parseObject(String source, ParsePosition pos) {
            return df.parseObject(source, pos);
        }
    }

    /**
	 * Format class for Excel phone number format. This class mimics Excel's
	 * built-in phone number formatting.
	 * 
	 * @author James May
	 */
    private static final class PhoneFormat extends Format {

        public static final Format instance = new PhoneFormat();

        private static final DecimalFormat df = createIntegerOnlyFormat("##########");

        private PhoneFormat() {
        }

        /** Format a number as a phone number */
        public static String format(Number num) {
            String result = df.format(num);
            StringBuffer sb = new StringBuffer();
            String seg1, seg2, seg3;
            int len = result.length();
            if (len <= 4) {
                return result;
            }
            seg3 = result.substring(len - 4, len);
            seg2 = result.substring(Math.max(0, len - 7), len - 4);
            seg1 = result.substring(Math.max(0, len - 10), Math.max(0, len - 7));
            if (seg1 != null && seg1.trim().length() > 0) {
                sb.append('(').append(seg1).append(") ");
            }
            if (seg2 != null && seg2.trim().length() > 0) {
                sb.append(seg2).append('-');
            }
            sb.append(seg3);
            return sb.toString();
        }

        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(format((Number) obj));
        }

        public Object parseObject(String source, ParsePosition pos) {
            return df.parseObject(source, pos);
        }
    }
}
