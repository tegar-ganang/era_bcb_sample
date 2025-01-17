package org.ofbiz.base.util.string;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.sql.Timestamp;
import javax.el.*;
import javolution.util.FastMap;
import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;

/** Implements Unified Expression Language functions.
 * <p>Built-in functions are divided into a number of
 * namespace prefixes:</p>
 * <table border="1">
 * <tr><td colspan="2"><b><code>date:</code> contains miscellaneous date/time functions</b></td></tr>
 * <tr><td><code>date:second(Timestamp, TimeZone, Locale)</code></td><td>Returns the second value of <code>Timestamp</code> (0 - 59).</td></tr>
 * <tr><td><code>date:minute(Timestamp, TimeZone, Locale)</code></td><td>Returns the minute value of <code>Timestamp</code> (0 - 59).</td></tr>
 * <tr><td><code>date:hour(Timestamp, TimeZone, Locale)</code></td><td>Returns the hour value of <code>Timestamp</code> (0 - 23).</td></tr>
 * <tr><td><code>date:dayOfMonth(Timestamp, TimeZone, Locale)</code></td><td>Returns the day of month value of <code>Timestamp</code> (1 - 31).</td></tr>
 * <tr><td><code>date:dayOfWeek(Timestamp, TimeZone, Locale)</code></td><td>Returns the day of week value of <code>Timestamp</code> (Sunday = 1, Saturday = 7).</td></tr>
 * <tr><td><code>date:dayOfYear(Timestamp, TimeZone, Locale)</code></td><td>Returns the day of year value of <code>Timestamp</code>.</td></tr>
 * <tr><td><code>date:week(Timestamp, TimeZone, Locale)</code></td><td>Returns the week value of <code>Timestamp</code>.</td></tr>
 * <tr><td><code>date:month(Timestamp, TimeZone, Locale)</code></td><td>Returns the month value of <code>Timestamp</code> (January = 0, December = 11).</td></tr>
 * <tr><td><code>date:year(Timestamp, TimeZone, Locale)</code></td><td>Returns the year value of <code>Timestamp</code>.</td></tr>
 * <tr><td><code>date:dayStart(Timestamp, TimeZone, Locale)</code></td><td>Returns <code>Timestamp</code> set to start of day.</td></tr>
 * <tr><td><code>date:dayEnd(Timestamp, TimeZone, Locale)</code></td><td>Returns <code>Timestamp</code> set to end of day.</td></tr>
 * <tr><td><code>date:weekStart(Timestamp, TimeZone, Locale)</code></td><td>Returns <code>Timestamp</code> set to start of week.</td></tr>
 * <tr><td><code>date:weekEnd(Timestamp, TimeZone, Locale)</code></td><td>Returns <code>Timestamp</code> set to end of week.</td></tr>
 * <tr><td><code>date:monthStart(Timestamp, TimeZone, Locale)</code></td><td>Returns <code>Timestamp</code> set to start of month.</td></tr>
 * <tr><td><code>date:monthEnd(Timestamp, TimeZone, Locale)</code></td><td>Returns <code>Timestamp</code> set to end of month.</td></tr>
 * <tr><td><code>date:yearStart(Timestamp, TimeZone, Locale)</code></td><td>Returns <code>Timestamp</code> set to start of year.</td></tr>
 * <tr><td><code>date:yearEnd(Timestamp, TimeZone, Locale)</code></td><td>Returns <code>Timestamp</code> set to end of year.</td></tr>
 * <tr><td><code>date:dateStr(Timestamp, TimeZone, Locale)</code></td><td>Returns <code>Timestamp</code> as a date <code>String</code> (yyyy-mm-dd).</td></tr>
 * <tr><td><code>date:dateTimeStr(Timestamp, TimeZone, Locale)</code></td><td>Returns <code>Timestamp</code> as a date-time <code>String</code> (yyyy-mm-dd hh:mm).</td></tr>
 * <tr><td><code>date:timeStr(Timestamp, TimeZone, Locale)</code></td><td>Returns <code>Timestamp</code> as a time <code>String</code> (hh:mm).</td></tr>
 * <tr><td><code>date:nowTimestamp()</code></td><td>Returns <code>Timestamp </code> for right now.</td></tr>
 * <tr><td colspan="2"><b><code>math:</code> maps to <code>java.lang.Math</code></b></td></tr>
 * <tr><td><code>math:absDouble(double)</code></td><td>Returns the absolute value of a <code>double</code> value.</td></tr>
 * <tr><td><code>math:absFloat(float)</code></td><td>Returns the absolute value of a <code>float</code> value.</td></tr>
 * <tr><td><code>math:absInt(int)</code></td><td>Returns the absolute value of an <code>int</code> value.</td></tr>
 * <tr><td><code>math:absLong(long)</code></td><td>Returns the absolute value of a <code>long</code> value.</td></tr>
 * <tr><td><code>math:acos(double)</code></td><td>Returns the arc cosine of an angle, in the range of 0.0 through <i>pi</i>.</td></tr>
 * <tr><td><code>math:asin(double)</code></td><td>Returns the arc sine of an angle, in the range of -<i>pi</i>/2 through <i>pi</i>/2.</td></tr>
 * <tr><td><code>math:atan(double)</code></td><td>Returns the arc tangent of an angle, in the range of -<i>pi</i>/2 through <i>pi</i>/2.</td></tr>
 * <tr><td><code>math:atan2(double, double)</code></td><td>Converts rectangular coordinates (<code>x</code>,&nbsp;<code>y</code>) to polar (r,&nbsp;<i>theta</i>).</td></tr>
 * <tr><td><code>math:cbrt(double)</code></td><td>Returns the cube root of a <code>double</code> value.</td></tr>
 * <tr><td><code>math:ceil(double)</code></td><td>Returns the smallest (closest to negative infinity) <code>double</code> value that is greater than or equal to the argument and is equal to a mathematical integer.</td></tr>
 * <tr><td><code>math:cos(double)</code></td><td>Returns the trigonometric cosine of an angle.</td></tr>
 * <tr><td><code>math:cosh(double)</code></td><td>Returns the hyperbolic cosine of a <code>double</code> value.</td></tr>
 * <tr><td><code>math:exp(double)</code></td><td>Returns Euler's number <i>e</i> raised to the power of a <code>double</code> value.</td></tr>
 * <tr><td><code>math:expm1(double)</code></td><td>Returns <i>e</i><sup>x</sup>&nbsp;-1.</td></tr>
 * <tr><td><code>math:floor(double)</code></td><td>Returns the largest (closest to positive infinity) <code>double</code> value that is less than or equal to the argument and is equal to a mathematical integer.</td></tr>
 * <tr><td><code>math:hypot(double, double)</code></td><td>Returns sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>) without intermediate overflow or underflow.</td></tr>
 * <tr><td><code>math:IEEEremainder(double, double)</code></td><td>Computes the remainder operation on two arguments as prescribed by the IEEE 754 standard.</td></tr>
 * <tr><td><code>math:log(double)</code></td><td>Returns the natural logarithm (base <i>e</i>) of a <code>double</code> value.</td></tr>
 * <tr><td><code>math:log10(double)</code></td><td>Returns the base 10 logarithm of a <code>double</code> value.</td></tr>
 * <tr><td><code>math:log1p(double)</code></td><td>Returns the natural logarithm of the sum of the argument and 1.</td></tr>
 * <tr><td><code>math:maxDouble(double, double)</code></td><td>Returns the greater of two <code>double</code> values.</td></tr>
 * <tr><td><code>math:maxFloat(float, float)</code></td><td>Returns the greater of two <code>float</code> values.</td></tr>
 * <tr><td><code>math:maxInt(int, int)</code></td><td>Returns the greater of two <code>int</code> values.</td></tr>
 * <tr><td><code>math:maxLong(long, long)</code></td><td>Returns the greater of two <code>long</code> values.</td></tr>
 * <tr><td><code>math:minDouble(double, double)</code></td><td>Returns the smaller of two <code>double</code> values.</td></tr>
 * <tr><td><code>math:minFloat(float, float)</code></td><td>Returns the smaller of two <code>float</code> values.</td></tr>
 * <tr><td><code>math:minInt(int, int)</code></td><td>Returns the smaller of two <code>int</code> values.</td></tr>
 * <tr><td><code>math:minLong(long, long)</code></td><td>Returns the smaller of two <code>long</code> values.</td></tr>
 * <tr><td><code>math:pow(double, double)</code></td><td>Returns the value of the first argument raised to the power of the second argument.</td></tr>
 * <tr><td><code>math:random()</code></td><td>Returns a <code>double</code> value with a positive sign, greater than or equal to <code>0.0</code> and less than <code>1.0</code>.</td></tr>
 * <tr><td><code>math:rint(double)</code></td><td>Returns the <code>double</code> value that is closest in value to the argument and is equal to a mathematical integer.</td></tr>
 * <tr><td><code>math:roundDouble(double)</code></td><td>Returns the closest <code>long</code> to the argument.</td></tr>
 * <tr><td><code>math:roundFloat(float)</code></td><td>Returns the closest <code>int</code> to the argument.</td></tr>
 * <tr><td><code>math:signumDouble(double)</code></td><td>Returns the signum function of the argument; zero if the argument is zero, 1.0 if the argument is greater than zero, -1.0 if the argument is less than zero.</td></tr>
 * <tr><td><code>math:signumFloat(float)</code></td><td>Returns the signum function of the argument; zero if the argument is zero, 1.0f if the argument is greater than zero, -1.0f if the argument is less than zero.</td></tr>
 * <tr><td><code>math:sin(double)</code></td><td>Returns the trigonometric sine of an angle.</td></tr>
 * <tr><td><code>math:sinh(double)</code></td><td>Returns the hyperbolic sine of a <code>double</code> value.</td></tr>
 * <tr><td><code>math:sqrt(double)</code></td><td>Returns the correctly rounded positive square root of a <code>double</code> value.</td></tr>
 * <tr><td><code>math:tan(double)</code></td><td>Returns the trigonometric tangent of an angle.</td></tr>
 * <tr><td><code>math:tanh(double)</code></td><td>Returns the hyperbolic tangent of a <code>double</code> value.</td></tr>
 * <tr><td><code>math:toDegrees(double)</code></td><td>Converts an angle measured in radians to an approximately equivalent angle measured in degrees.</td></tr>
 * <tr><td><code>math:toRadians(double)</code></td><td>Converts an angle measured in degrees to an approximately equivalent angle measured in radians.</td></tr>
 * <tr><td><code>math:ulpDouble(double)</code></td><td>Returns the size of an ulp (units in the last place) of the argument.</td></tr>
 * <tr><td><code>math:ulpFloat(float)</code></td><td>Returns the size of an ulp (units in the last place) of the argument.</td></tr>
 * <tr><td colspan="2"><b><code>str:</code> maps to <code>java.lang.String</code></b></td></tr>
 * <tr><td><code>str:endsWith(String, String)</code></td><td>Returns <code>true</code> if this string ends with the specified suffix.</td></tr>
 * <tr><td><code>str:indexOf(String, String)</code></td><td>Returns the index within this string of the first occurrence of the specified substring.</td></tr>
 * <tr><td><code>str:lastIndexOf(String, String)</code></td><td>Returns the index within this string of the last occurrence of the specified character.</td></tr>
 * <tr><td><code>str:length(String)</code></td><td>Returns the length of this string.</td></tr>
 * <tr><td><code>str:replace(String, String, String)</code></td><td>Replaces each substring of this string that matches the literal target sequence with the specified literal replacement sequence.</td></tr>
 * <tr><td><code>str:replaceAll(String, String, String)</code></td><td>Replaces each substring of this string that matches the given regular expression with the given replacement.</td></tr>
 * <tr><td><code>str:replaceFirst(String, String, String)</code></td><td>Replaces the first substring of this string that matches the given regular expression with the given replacement.</td></tr>
 * <tr><td><code>str:startsWith(String, String)</code></td><td>Returns <code>true</code> if this string starts with the specified prefix.</td></tr>
 * <tr><td><code>str:endstring(String, int)</code></td><td>Returns a new string that is a substring of this string. The substring begins with the character at the specified index and extends to the end of this string.</td></tr>
 * <tr><td><code>str:substring(String, int, int)</code></td><td>Returns a new string that is a substring of this string. The substring begins at the specified beginIndex and extends to the character at index endIndex - 1. Thus the length of the substring is endIndex-beginIndex.</td></tr>
 * <tr><td><code>str:toString(Object)</code></td><td>Converts <code>Object</code> to a <code>String</code> - bypassing localization.</td></tr>
 * <tr><td><code>str:trim(String)</code></td><td>Returns a copy of the string, with leading and trailing whitespace omitted.</td></tr>
 * <tr><td colspan="2"><b><code>sys:</code> maps to <code>java.lang.System</code></b></td></tr>
 * <tr><td><code>sys:getenv(String)</code></td><td>Gets the value of the specified environment variable.</td></tr>
 * <tr><td><code>sys:getProperty(String)</code></td><td>Gets the system property indicated by the specified key.</td></tr>
 * <tr><td colspan="2"><b><code>util:</code> contains miscellaneous utility functions</b></td></tr>
 * <tr><td><code>util:defaultLocale()</code></td><td>Returns the default <code>Locale</code>.</td></tr>
 * <tr><td><code>util:defaultTimeZone()</code></td><td>Returns the default <code>TimeZone</code>.</td></tr>
 * <tr><td><code>util:size(Object)</code></td><td>Returns the size of <code>Maps</code>,
 * <code>Collections</code>, and <code>Strings</code>. Invalid <code>Object</code> types return -1.</td></tr>
 * <tr><td><code>util:urlExists(String)</code></td><td>Returns <code>true</code> if the specified URL exists.</td></tr>
 * </table>
 */
public class UelFunctions {

    public static final String module = UelFunctions.class.getName();

    protected static final FunctionMapper functionMapper = new Functions();

    /** Returns a <code>FunctionMapper</code> instance.
     * @return <code>FunctionMapper</code> instance
     */
    public static FunctionMapper getFunctionMapper() {
        return functionMapper;
    }

    protected static class Functions extends FunctionMapper {

        protected final Map<String, Method> functionMap = FastMap.newInstance();

        public Functions() {
            try {
                this.functionMap.put("date:second", UtilDateTime.class.getMethod("getSecond", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:minute", UtilDateTime.class.getMethod("getMinute", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:hour", UtilDateTime.class.getMethod("getHour", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:dayOfMonth", UtilDateTime.class.getMethod("getDayOfMonth", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:dayOfWeek", UtilDateTime.class.getMethod("getDayOfWeek", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:dayOfYear", UtilDateTime.class.getMethod("getDayOfYear", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:week", UtilDateTime.class.getMethod("getWeek", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:month", UtilDateTime.class.getMethod("getMonth", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:year", UtilDateTime.class.getMethod("getYear", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:dayStart", UtilDateTime.class.getMethod("getDayStart", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:dayEnd", UtilDateTime.class.getMethod("getDayEnd", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:weekStart", UtilDateTime.class.getMethod("getWeekStart", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:weekEnd", UtilDateTime.class.getMethod("getWeekEnd", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:monthStart", UtilDateTime.class.getMethod("getMonthStart", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:monthEnd", UtilDateTime.class.getMethod("getMonthEnd", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:yearStart", UtilDateTime.class.getMethod("getYearStart", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:yearEnd", UtilDateTime.class.getMethod("getYearEnd", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:dateStr", UelFunctions.class.getMethod("dateString", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:dateTimeStr", UelFunctions.class.getMethod("dateTimeString", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:timeStr", UelFunctions.class.getMethod("timeString", Timestamp.class, TimeZone.class, Locale.class));
                this.functionMap.put("date:nowTimestamp", UtilDateTime.class.getMethod("nowTimestamp"));
                this.functionMap.put("math:absDouble", Math.class.getMethod("abs", double.class));
                this.functionMap.put("math:absFloat", Math.class.getMethod("abs", float.class));
                this.functionMap.put("math:absInt", Math.class.getMethod("abs", int.class));
                this.functionMap.put("math:absLong", Math.class.getMethod("abs", long.class));
                this.functionMap.put("math:acos", Math.class.getMethod("abs", double.class));
                this.functionMap.put("math:asin", Math.class.getMethod("asin", double.class));
                this.functionMap.put("math:atan", Math.class.getMethod("atan", double.class));
                this.functionMap.put("math:atan2", Math.class.getMethod("max", double.class, double.class));
                this.functionMap.put("math:cbrt", Math.class.getMethod("cbrt", double.class));
                this.functionMap.put("math:ceil", Math.class.getMethod("ceil", double.class));
                this.functionMap.put("math:cos", Math.class.getMethod("cos", double.class));
                this.functionMap.put("math:cosh", Math.class.getMethod("cosh", double.class));
                this.functionMap.put("math:exp", Math.class.getMethod("exp", double.class));
                this.functionMap.put("math:expm1", Math.class.getMethod("expm1", double.class));
                this.functionMap.put("math:floor", Math.class.getMethod("floor", double.class));
                this.functionMap.put("math:hypot", Math.class.getMethod("hypot", double.class, double.class));
                this.functionMap.put("math:IEEEremainder", Math.class.getMethod("IEEEremainder", double.class, double.class));
                this.functionMap.put("math:log", Math.class.getMethod("log", double.class));
                this.functionMap.put("math:log10", Math.class.getMethod("log10", double.class));
                this.functionMap.put("math:log1p", Math.class.getMethod("log1p", double.class));
                this.functionMap.put("math:maxDouble", Math.class.getMethod("max", double.class, double.class));
                this.functionMap.put("math:maxFloat", Math.class.getMethod("max", float.class, float.class));
                this.functionMap.put("math:maxInt", Math.class.getMethod("max", int.class, int.class));
                this.functionMap.put("math:maxLong", Math.class.getMethod("max", long.class, long.class));
                this.functionMap.put("math:minDouble", Math.class.getMethod("min", double.class, double.class));
                this.functionMap.put("math:minFloat", Math.class.getMethod("min", float.class, float.class));
                this.functionMap.put("math:minInt", Math.class.getMethod("min", int.class, int.class));
                this.functionMap.put("math:minLong", Math.class.getMethod("min", long.class, long.class));
                this.functionMap.put("math:pow", Math.class.getMethod("pow", double.class, double.class));
                this.functionMap.put("math:random", Math.class.getMethod("random"));
                this.functionMap.put("math:rint", Math.class.getMethod("rint", double.class));
                this.functionMap.put("math:roundDouble", Math.class.getMethod("round", double.class));
                this.functionMap.put("math:roundFloat", Math.class.getMethod("round", float.class));
                this.functionMap.put("math:signumDouble", Math.class.getMethod("signum", double.class));
                this.functionMap.put("math:signumFloat", Math.class.getMethod("signum", float.class));
                this.functionMap.put("math:sin", Math.class.getMethod("sin", double.class));
                this.functionMap.put("math:sinh", Math.class.getMethod("sinh", double.class));
                this.functionMap.put("math:sqrt", Math.class.getMethod("sqrt", double.class));
                this.functionMap.put("math:tan", Math.class.getMethod("tan", double.class));
                this.functionMap.put("math:tanh", Math.class.getMethod("tanh", double.class));
                this.functionMap.put("math:toDegrees", Math.class.getMethod("toDegrees", double.class));
                this.functionMap.put("math:toRadians", Math.class.getMethod("toRadians", double.class));
                this.functionMap.put("math:ulpDouble", Math.class.getMethod("ulp", double.class));
                this.functionMap.put("math:ulpFloat", Math.class.getMethod("ulp", float.class));
                this.functionMap.put("str:endsWith", UelFunctions.class.getMethod("endsWith", String.class, String.class));
                this.functionMap.put("str:indexOf", UelFunctions.class.getMethod("indexOf", String.class, String.class));
                this.functionMap.put("str:lastIndexOf", UelFunctions.class.getMethod("lastIndexOf", String.class, String.class));
                this.functionMap.put("str:length", UelFunctions.class.getMethod("length", String.class));
                this.functionMap.put("str:replace", UelFunctions.class.getMethod("replace", String.class, String.class, String.class));
                this.functionMap.put("str:replaceAll", UelFunctions.class.getMethod("replaceAll", String.class, String.class, String.class));
                this.functionMap.put("str:replaceFirst", UelFunctions.class.getMethod("replaceFirst", String.class, String.class, String.class));
                this.functionMap.put("str:startsWith", UelFunctions.class.getMethod("startsWith", String.class, String.class));
                this.functionMap.put("str:endstring", UelFunctions.class.getMethod("endString", String.class, int.class));
                this.functionMap.put("str:substring", UelFunctions.class.getMethod("subString", String.class, int.class, int.class));
                this.functionMap.put("str:toString", UelFunctions.class.getMethod("toString", Object.class));
                this.functionMap.put("str:trim", UelFunctions.class.getMethod("trim", String.class));
                this.functionMap.put("sys:getenv", UelFunctions.class.getMethod("sysGetEnv", String.class));
                this.functionMap.put("sys:getProperty", UelFunctions.class.getMethod("sysGetProp", String.class));
                this.functionMap.put("util:size", UelFunctions.class.getMethod("getSize", Object.class));
                this.functionMap.put("util:defaultLocale", Locale.class.getMethod("getDefault"));
                this.functionMap.put("util:defaultTimeZone", TimeZone.class.getMethod("getDefault"));
                this.functionMap.put("util:urlExists", UelFunctions.class.getMethod("urlExists", String.class));
            } catch (Exception e) {
                Debug.logWarning("Error while initializing UelFunctions.Functions instance: " + e, module);
            }
            Debug.logVerbose("UelFunctions.Functions loaded " + this.functionMap.size() + " functions", module);
        }

        public void setFunction(String prefix, String localName, Method method) {
            synchronized (this) {
                functionMap.put(prefix + ":" + localName, method);
            }
        }

        public Method resolveFunction(String prefix, String localName) {
            return functionMap.get(prefix + ":" + localName);
        }
    }

    public static String dateString(Timestamp stamp, TimeZone timeZone, Locale locale) {
        DateFormat dateFormat = UtilDateTime.toDateFormat(UtilDateTime.DATE_FORMAT, timeZone, locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(stamp);
    }

    public static String dateTimeString(Timestamp stamp, TimeZone timeZone, Locale locale) {
        DateFormat dateFormat = UtilDateTime.toDateTimeFormat("yyyy-MM-dd HH:mm", timeZone, locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(stamp);
    }

    public static String timeString(Timestamp stamp, TimeZone timeZone, Locale locale) {
        DateFormat dateFormat = UtilDateTime.toTimeFormat(UtilDateTime.TIME_FORMAT, timeZone, locale);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(stamp);
    }

    @SuppressWarnings("unchecked")
    public static int getSize(Object obj) {
        try {
            Map map = (Map) obj;
            return map.size();
        } catch (Exception e) {
        }
        try {
            Collection coll = (Collection) obj;
            return coll.size();
        } catch (Exception e) {
        }
        try {
            String str = (String) obj;
            return str.length();
        } catch (Exception e) {
        }
        return -1;
    }

    public static boolean endsWith(String str1, String str2) {
        try {
            return str1.endsWith(str2);
        } catch (Exception e) {
        }
        return false;
    }

    public static int indexOf(String str1, String str2) {
        try {
            return str1.indexOf(str2);
        } catch (Exception e) {
        }
        return -1;
    }

    public static int lastIndexOf(String str1, String str2) {
        try {
            return str1.lastIndexOf(str2);
        } catch (Exception e) {
        }
        return -1;
    }

    public static int length(String str1) {
        try {
            return str1.length();
        } catch (Exception e) {
        }
        return -1;
    }

    public static String replace(String str1, String str2, String str3) {
        try {
            return str1.replace(str2, str3);
        } catch (Exception e) {
        }
        return null;
    }

    public static String replaceAll(String str1, String str2, String str3) {
        try {
            return str1.replaceAll(str2, str3);
        } catch (Exception e) {
        }
        return null;
    }

    public static String replaceFirst(String str1, String str2, String str3) {
        try {
            return str1.replaceFirst(str2, str3);
        } catch (Exception e) {
        }
        return null;
    }

    public static boolean startsWith(String str1, String str2) {
        try {
            return str1.startsWith(str2);
        } catch (Exception e) {
        }
        return false;
    }

    public static String endString(String str, int index) {
        try {
            return str.substring(index);
        } catch (Exception e) {
        }
        return null;
    }

    public static String subString(String str, int beginIndex, int endIndex) {
        try {
            return str.substring(beginIndex, endIndex);
        } catch (Exception e) {
        }
        return null;
    }

    public static String trim(String str) {
        try {
            return str.trim();
        } catch (Exception e) {
        }
        return null;
    }

    public static String toString(Object obj) {
        return obj.toString();
    }

    public static String sysGetEnv(String str) {
        try {
            return System.getenv(str);
        } catch (Exception e) {
        }
        return null;
    }

    public static String sysGetProp(String str) {
        try {
            return System.getProperty(str);
        } catch (Exception e) {
        }
        return null;
    }

    public static boolean urlExists(String str) {
        boolean result = false;
        try {
            URL url = FlexibleLocation.resolveLocation(str);
            if (url != null) {
                InputStream is = url.openStream();
                result = true;
                is.close();
            }
        } catch (Exception e) {
        }
        return result;
    }
}
