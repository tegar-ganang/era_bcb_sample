package cn.langhua.opencms.ofbiz.rmi.base.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.opencms.main.CmsLog;
import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * HttpUtil - Misc TTP Utility Functions
 */
public class UtilHttp {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(UtilHttp.class);

    public static final String module = UtilHttp.class.getName();

    public static final String MULTI_ROW_DELIMITER = "_o_";

    public static final String ROW_SUBMIT_PREFIX = "_rowSubmit_o_";

    public static final String COMPOSITE_DELIMITER = "_c_";

    public static final int MULTI_ROW_DELIMITER_LENGTH = MULTI_ROW_DELIMITER.length();

    public static final int ROW_SUBMIT_PREFIX_LENGTH = ROW_SUBMIT_PREFIX.length();

    public static final int COMPOSITE_DELIMITER_LENGTH = COMPOSITE_DELIMITER.length();

    /**
     * Create a map from an HttpServletRequest object
     * @return The resulting Map
     */
    public static Map getParameterMap(HttpServletRequest request) {
        Map paramMap = FastMap.newInstance();
        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            Object value = null;
            String[] paramArr = request.getParameterValues(name);
            if (paramArr != null) {
                if (paramArr.length > 1) {
                    value = Arrays.asList(paramArr);
                } else {
                    value = paramArr[0];
                }
            }
            paramMap.put(name, value);
        }
        String pathInfoStr = request.getPathInfo();
        if (pathInfoStr != null && pathInfoStr.length() > 0) {
            if (!pathInfoStr.endsWith("/")) pathInfoStr += "/";
            int current = pathInfoStr.indexOf('/');
            int last = current;
            while ((current = pathInfoStr.indexOf('/', last + 1)) != -1) {
                String element = pathInfoStr.substring(last + 1, current);
                last = current;
                if (element.charAt(0) == '~' && element.indexOf('=') > -1) {
                    String name = element.substring(1, element.indexOf('='));
                    String value = element.substring(element.indexOf('=') + 1);
                    Object curValue = paramMap.get(name);
                    if (curValue != null) {
                        List paramList = null;
                        if (curValue instanceof List) {
                            paramList = (List) curValue;
                            paramList.add(value);
                        } else {
                            String paramString = (String) curValue;
                            paramList = FastList.newInstance();
                            paramList.add(paramString);
                            paramList.add(value);
                        }
                        paramMap.put(name, paramList);
                    } else {
                        paramMap.put(name, value);
                    }
                }
            }
        }
        if (paramMap.size() == 0) {
            Map multiPartMap = (Map) request.getAttribute("multiPartMap");
            if (multiPartMap != null && multiPartMap.size() > 0) {
                paramMap.putAll(multiPartMap);
            }
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Made Request Parameter Map with [" + paramMap.size() + "] Entries");
            Iterator entryIter = paramMap.entrySet().iterator();
            while (entryIter.hasNext()) {
                Map.Entry entry = (Map.Entry) entryIter.next();
                LOG.info("Request Parameter Map Entry: [" + entry.getKey() + "] --> " + entry.getValue());
            }
        }
        return paramMap;
    }

    public static Map makeParamMapWithPrefix(HttpServletRequest request, String prefix, String suffix) {
        return makeParamMapWithPrefix(request, null, prefix, suffix);
    }

    public static Map makeParamMapWithPrefix(HttpServletRequest request, Map additionalFields, String prefix, String suffix) {
        Map paramMap = new HashMap();
        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = (String) parameterNames.nextElement();
            if (parameterName.startsWith(prefix)) {
                if (suffix != null && suffix.length() > 0) {
                    if (parameterName.endsWith(suffix)) {
                        String key = parameterName.substring(prefix.length(), parameterName.length() - (suffix.length()));
                        String value = request.getParameter(parameterName);
                        paramMap.put(key, value);
                    }
                } else {
                    String key = parameterName.substring(prefix.length());
                    String value = request.getParameter(parameterName);
                    paramMap.put(key, value);
                }
            }
        }
        if (additionalFields != null) {
            Iterator i = additionalFields.keySet().iterator();
            while (i.hasNext()) {
                String fieldName = (String) i.next();
                if (fieldName.startsWith(prefix)) {
                    if (suffix != null && suffix.length() > 0) {
                        if (fieldName.endsWith(suffix)) {
                            String key = fieldName.substring(prefix.length(), fieldName.length() - (suffix.length() - 1));
                            Object value = additionalFields.get(fieldName);
                            paramMap.put(key, value);
                            if (!(value instanceof String)) {
                                String nameKey = "_" + key + "_fileName";
                                Object nameVal = additionalFields.get("_" + fieldName + "_fileName");
                                if (nameVal != null) {
                                    paramMap.put(nameKey, nameVal);
                                }
                                String typeKey = "_" + key + "_contentType";
                                Object typeVal = additionalFields.get("_" + fieldName + "_contentType");
                                if (typeVal != null) {
                                    paramMap.put(typeKey, typeVal);
                                }
                                String sizeKey = "_" + key + "_size";
                                Object sizeVal = additionalFields.get("_" + fieldName + "_size");
                                if (sizeVal != null) {
                                    paramMap.put(sizeKey, sizeVal);
                                }
                            }
                        }
                    } else {
                        String key = fieldName.substring(prefix.length());
                        Object value = additionalFields.get(fieldName);
                        paramMap.put(key, value);
                        if (!(value instanceof String)) {
                            String nameKey = "_" + key + "_fileName";
                            Object nameVal = additionalFields.get("_" + fieldName + "_fileName");
                            if (nameVal != null) {
                                paramMap.put(nameKey, nameVal);
                            }
                            String typeKey = "_" + key + "_contentType";
                            Object typeVal = additionalFields.get("_" + fieldName + "_contentType");
                            if (typeVal != null) {
                                paramMap.put(typeKey, typeVal);
                            }
                            String sizeKey = "_" + key + "_size";
                            Object sizeVal = additionalFields.get("_" + fieldName + "_size");
                            if (sizeVal != null) {
                                paramMap.put(sizeKey, sizeVal);
                            }
                        }
                    }
                }
            }
        }
        return paramMap;
    }

    public static List makeParamListWithSuffix(HttpServletRequest request, String suffix, String prefix) {
        return makeParamListWithSuffix(request, null, suffix, prefix);
    }

    public static List makeParamListWithSuffix(HttpServletRequest request, Map additionalFields, String suffix, String prefix) {
        List paramList = new ArrayList();
        Enumeration parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String parameterName = (String) parameterNames.nextElement();
            if (parameterName.endsWith(suffix)) {
                if (prefix != null && prefix.length() > 0) {
                    if (parameterName.startsWith(prefix)) {
                        String value = request.getParameter(parameterName);
                        paramList.add(value);
                    }
                } else {
                    String value = request.getParameter(parameterName);
                    paramList.add(value);
                }
            }
        }
        if (additionalFields != null) {
            Iterator i = additionalFields.keySet().iterator();
            while (i.hasNext()) {
                String fieldName = (String) i.next();
                if (fieldName.endsWith(suffix)) {
                    if (prefix != null && prefix.length() > 0) {
                        if (fieldName.startsWith(prefix)) {
                            Object value = additionalFields.get(fieldName);
                            paramList.add(value);
                        }
                    } else {
                        Object value = additionalFields.get(fieldName);
                        paramList.add(value);
                    }
                }
            }
        }
        return paramList;
    }

    /**
     * Given a request, returns the application name or "root" if deployed on root
     * @param request An HttpServletRequest to get the name info from
     * @return String
     */
    public static String getApplicationName(HttpServletRequest request) {
        String appName = "root";
        if (request.getContextPath().length() > 1) {
            appName = request.getContextPath().substring(1);
        }
        return appName;
    }

    /**
     * Put request parameters in request object as attributes.
     * @param request
     */
    public static void parametersToAttributes(HttpServletRequest request) {
        java.util.Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            request.setAttribute(name, request.getParameter(name));
        }
    }

    public static StringBuffer getServerRootUrl(HttpServletRequest request) {
        StringBuffer requestUrl = new StringBuffer();
        requestUrl.append(request.getScheme());
        requestUrl.append("://" + request.getServerName());
        if (request.getServerPort() != 80 && request.getServerPort() != 443) requestUrl.append(":" + request.getServerPort());
        return requestUrl;
    }

    public static StringBuffer getFullRequestUrl(HttpServletRequest request) {
        StringBuffer requestUrl = UtilHttp.getServerRootUrl(request);
        requestUrl.append(request.getRequestURI());
        if (request.getQueryString() != null) {
            requestUrl.append("?" + request.getQueryString());
        }
        return requestUrl;
    }

    public static Locale getLocale(HttpServletRequest request, HttpSession session, Object appDefaultLocale) {
        Object localeObject = session != null ? session.getAttribute("locale") : null;
        if (localeObject == null) {
            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
            if (userLogin == null) {
                userLogin = (GenericValue) session.getAttribute("autoUserLogin");
            }
            if (userLogin != null) {
                localeObject = userLogin.getAllFields().get("lastLocale");
            }
        }
        if (localeObject == null && !UtilValidate.isEmpty(appDefaultLocale)) {
            localeObject = appDefaultLocale;
        }
        if (localeObject == null) {
            localeObject = request != null ? request.getLocale() : null;
        }
        return UtilMisc.ensureLocale(localeObject);
    }

    /**
     * Get the Locale object from a session variable; if not found use the browser's default
     * @param request HttpServletRequest object to use for lookup
     * @return Locale The current Locale to use
     */
    public static Locale getLocale(HttpServletRequest request) {
        if (request == null) return Locale.getDefault();
        return UtilHttp.getLocale(request, request.getSession(), null);
    }

    /**
     * Get the Locale object from a session variable; if not found use the system's default.
     * NOTE: This method is not recommended because it ignores the Locale from the browser not having the request object.
     * @param session HttpSession object to use for lookup
     * @return Locale The current Locale to use
     */
    public static Locale getLocale(HttpSession session) {
        if (session == null) return Locale.getDefault();
        return UtilHttp.getLocale(null, session, null);
    }

    public static void setLocale(HttpServletRequest request, String localeString) {
        UtilHttp.setLocale(request.getSession(), UtilMisc.parseLocale(localeString));
    }

    public static void setLocale(HttpSession session, Locale locale) {
        session.setAttribute("locale", locale);
    }

    public static void setLocaleIfNone(HttpSession session, String localeString) {
        if (UtilValidate.isNotEmpty(localeString) && session.getAttribute("locale") == null) {
            UtilHttp.setLocale(session, UtilMisc.parseLocale(localeString));
        }
    }

    /**
     * Get the currency string from the session.
     * @param session HttpSession object to use for lookup
     * @return String The ISO currency code
     */
    public static String getCurrencyUom(HttpSession session, String appDefaultCurrencyUom) {
        String iso = (String) session.getAttribute("currencyUom");
        if (iso == null) {
            Map userLogin = (Map) session.getAttribute("userLogin");
            if (userLogin == null) {
                userLogin = (Map) session.getAttribute("autoUserLogin");
            }
            if (userLogin != null) {
                iso = (String) userLogin.get("lastCurrencyUom");
            }
        }
        if (iso == null && !UtilValidate.isEmpty(appDefaultCurrencyUom)) {
            iso = appDefaultCurrencyUom;
        }
        if (iso == null) {
            try {
                iso = UtilProperties.getPropertyValue("general", "currency.uom.id.default", "USD");
            } catch (Exception e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Error getting the general:currency.uom.id.default value: ", e);
                }
            }
        }
        if (iso == null) {
            Currency cur = Currency.getInstance(getLocale(session));
            iso = cur.getCurrencyCode();
        }
        return iso;
    }

    /**
     * Get the currency string from the session.
     * @param request HttpServletRequest object to use for lookup
     * @return String The ISO currency code
     */
    public static String getCurrencyUom(HttpServletRequest request) {
        return getCurrencyUom(request.getSession(), null);
    }

    /** Simple event to set the users per-session currency uom value */
    public static void setCurrencyUom(HttpSession session, String currencyUom) {
        session.setAttribute("currencyUom", currencyUom);
    }

    public static void setCurrencyUomIfNone(HttpSession session, String currencyUom) {
        if (UtilValidate.isNotEmpty(currencyUom) && session.getAttribute("currencyUom") == null) {
            session.setAttribute("currencyUom", currencyUom);
        }
    }

    /** URL Encodes a Map of arguements */
    public static String urlEncodeArgs(Map args) {
        return urlEncodeArgs(args, true);
    }

    /** URL Encodes a Map of arguements */
    public static String urlEncodeArgs(Map args, boolean useExpandedEntites) {
        StringBuffer buf = new StringBuffer();
        if (args != null) {
            Iterator i = args.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry entry = (Map.Entry) i.next();
                String name = (String) entry.getKey();
                Object value = entry.getValue();
                String valueStr = null;
                if (name != null && value != null) {
                    if (value instanceof String) {
                        valueStr = (String) value;
                    } else {
                        valueStr = value.toString();
                    }
                    if (valueStr != null && valueStr.length() > 0) {
                        if (buf.length() > 0) {
                            if (useExpandedEntites) {
                                buf.append("&amp;");
                            } else {
                                buf.append("&");
                            }
                        }
                        try {
                            buf.append(URLEncoder.encode(name, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            if (LOG.isErrorEnabled()) {
                                LOG.error(e);
                            }
                        }
                        buf.append('=');
                        try {
                            buf.append(URLEncoder.encode(valueStr, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            if (LOG.isErrorEnabled()) {
                                LOG.error(e);
                            }
                        }
                    }
                }
            }
        }
        return buf.toString();
    }

    public static String encodeAmpersands(String htmlString) {
        StringBuffer htmlBuffer = new StringBuffer(htmlString);
        int ampLoc = -1;
        while ((ampLoc = htmlBuffer.indexOf("&", ampLoc + 1)) != -1) {
            int semiLoc = htmlBuffer.indexOf(";", ampLoc);
            if (semiLoc != -1) {
                int eqLoc = htmlBuffer.indexOf("=", ampLoc);
                int amp2Loc = htmlBuffer.indexOf("&", ampLoc + 1);
                if ((eqLoc == -1 || eqLoc > semiLoc) && (amp2Loc == -1 || amp2Loc > semiLoc)) {
                    continue;
                }
            }
            htmlBuffer.insert(ampLoc + 1, "amp;");
        }
        return htmlBuffer.toString();
    }

    public static String encodeBlanks(String htmlString) {
        return htmlString.replaceAll(" ", "%20");
    }

    public static String setResponseBrowserProxyNoCache(HttpServletRequest request, HttpServletResponse response) {
        setResponseBrowserProxyNoCache(response);
        return "success";
    }

    public static void setResponseBrowserProxyNoCache(HttpServletResponse response) {
        long nowMillis = System.currentTimeMillis();
        response.setDateHeader("Expires", nowMillis);
        response.setDateHeader("Last-Modified", nowMillis);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0, false");
        response.setHeader("Pragma", "no-cache");
    }

    public static String getContentTypeByFileName(String fileName) {
        FileNameMap mime = URLConnection.getFileNameMap();
        return mime.getContentTypeFor(fileName);
    }

    /**
     * Stream an array of bytes to the browser
     * This method will close the ServletOutputStream when finished
     *
     * @param response HttpServletResponse object to get OutputStream from
     * @param bytes Byte array of content to stream
     * @param contentType The content type to pass to the browser
     * @param fileName the fileName to tell the browser we are downloading
     * @throws IOException
     */
    public static void streamContentToBrowser(HttpServletResponse response, byte[] bytes, String contentType, String fileName) throws IOException {
        setResponseBrowserProxyNoCache(response);
        response.setContentLength(bytes.length);
        if (contentType != null) {
            response.setContentType(contentType);
        }
        if (fileName != null) {
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        }
        OutputStream out = response.getOutputStream();
        InputStream in = new ByteArrayInputStream(bytes);
        try {
            streamContent(out, in, bytes.length);
        } catch (IOException e) {
            in.close();
            out.close();
            throw e;
        }
        in.close();
        out.flush();
        out.close();
    }

    public static void streamContentToBrowser(HttpServletResponse response, byte[] bytes, String contentType) throws IOException {
        streamContentToBrowser(response, bytes, contentType, null);
    }

    /**
     * Streams content from InputStream to the ServletOutputStream
     * This method will close the ServletOutputStream when finished
     * This method does not close the InputSteam passed
     *
     * @param response HttpServletResponse object to get OutputStream from
     * @param in InputStream of the actual content
     * @param length Size (in bytes) of the content
     * @param contentType The content type to pass to the browser
     * @throws IOException
     */
    public static void streamContentToBrowser(HttpServletResponse response, InputStream in, int length, String contentType, String fileName) throws IOException {
        setResponseBrowserProxyNoCache(response);
        response.setContentLength(length);
        if (contentType != null) {
            response.setContentType(contentType);
        }
        if (fileName != null) {
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        }
        OutputStream out = response.getOutputStream();
        try {
            streamContent(out, in, length);
        } catch (IOException e) {
            out.close();
            throw e;
        }
        out.flush();
        out.close();
    }

    public static void streamContentToBrowser(HttpServletResponse response, InputStream in, int length, String contentType) throws IOException {
        streamContentToBrowser(response, in, length, contentType, null);
    }

    /**
     * Stream binary content from InputStream to OutputStream
     * This method does not close the streams passed
     *
     * @param out OutputStream content should go to
     * @param in InputStream of the actual content
     * @param length Size (in bytes) of the content
     * @throws IOException
     */
    public static void streamContent(OutputStream out, InputStream in, int length) throws IOException {
        int bufferSize = 512;
        if (out == null) {
            throw new IOException("Attempt to write to null output stream");
        }
        if (in == null) {
            throw new IOException("Attempt to read from null input stream");
        }
        if (length == 0) {
            throw new IOException("Attempt to write 0 bytes of content to output stream");
        }
        BufferedOutputStream bos = new BufferedOutputStream(out, bufferSize);
        BufferedInputStream bis = new BufferedInputStream(in, bufferSize);
        byte[] buffer = new byte[length];
        int read = 0;
        try {
            while ((read = bis.read(buffer, 0, buffer.length)) != -1) {
                bos.write(buffer, 0, read);
            }
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Problem reading/writing buffers", e);
            }
            bis.close();
            bos.close();
            throw e;
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (bos != null) {
                bos.flush();
                bos.close();
            }
        }
    }

    public static String stripViewParamsFromQueryString(String queryString) {
        Set paramNames = new HashSet();
        paramNames.add("VIEW_INDEX");
        paramNames.add("VIEW_SIZE");
        paramNames.add("viewIndex");
        paramNames.add("viewSize");
        return stripNamedParamsFromQueryString(queryString, paramNames);
    }

    public static String stripNamedParamsFromQueryString(String queryString, Collection paramNames) {
        String retStr = null;
        if (UtilValidate.isNotEmpty(queryString)) {
            StringTokenizer queryTokens = new StringTokenizer(queryString, "&");
            StringBuffer cleanQuery = new StringBuffer();
            while (queryTokens.hasMoreTokens()) {
                String token = queryTokens.nextToken();
                if (token.startsWith("amp;")) {
                    token = token.substring(4);
                }
                int equalsIndex = token.indexOf("=");
                String name = token;
                if (equalsIndex > 0) {
                    name = token.substring(0, equalsIndex);
                }
                if (!paramNames.contains(name)) {
                    cleanQuery.append(token);
                    if (queryTokens.hasMoreTokens()) {
                        cleanQuery.append("&");
                    }
                }
            }
            retStr = cleanQuery.toString();
        }
        return retStr;
    }

    /**
     * Given multi form data with the ${param}_o_N notation, creates a Collection 
     * of Maps for the submitted rows. Each Map contains the key/value pairs 
     * of a particular row. The keys will be stripped of the _o_N suffix. 
     * There is an additionaly key "row" for each Map that holds the 
     * index of the row.
     */
    public static Collection parseMultiFormData(Map parameters) {
        FastMap rows = new FastMap();
        Iterator keys = parameters.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (key == null || key.length() <= ROW_SUBMIT_PREFIX_LENGTH) continue;
            if (key.indexOf(MULTI_ROW_DELIMITER) <= 0) continue;
            if (!key.substring(0, ROW_SUBMIT_PREFIX_LENGTH).equals(ROW_SUBMIT_PREFIX)) continue;
            if (!parameters.get(key).equals("Y")) continue;
            Integer n = Integer.decode(key.substring(ROW_SUBMIT_PREFIX_LENGTH, key.length()));
            Map m = new FastMap();
            m.put("row", n);
            rows.put(n, m);
        }
        keys = parameters.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (key == null) continue;
            int index = key.indexOf(MULTI_ROW_DELIMITER);
            if (index <= 0) continue;
            if (key.length() > ROW_SUBMIT_PREFIX_LENGTH && key.substring(0, ROW_SUBMIT_PREFIX_LENGTH).equals(ROW_SUBMIT_PREFIX)) continue;
            Integer n = Integer.decode(key.substring(index + MULTI_ROW_DELIMITER_LENGTH, key.length()));
            Map map = (Map) rows.get(n);
            if (map == null) continue;
            String newKey = key.substring(0, index);
            map.put(newKey, parameters.get(key));
        }
        return rows.values();
    }

    /**
     * Returns a new map containing all the parameters from the input map except for the
     * multi form parameters (usually named according to the ${param}_o_N notation).
     */
    public static Map removeMultiFormParameters(Map parameters) {
        FastMap filteredParameters = new FastMap();
        Iterator keys = parameters.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (key != null && (key.indexOf(MULTI_ROW_DELIMITER) != -1 || key.indexOf("_useRowSubmit") != -1 || key.indexOf("_rowCount") != -1)) {
                continue;
            }
            filteredParameters.put(key, parameters.get(key));
        }
        return filteredParameters;
    }

    /**
     * Utility to make a composite parameter from the given prefix and suffix.
     * The prefix should be a regular paramter name such as meetingDate. The
     * suffix is the composite field, such as the hour of the meeting. The
     * result would be meetingDate_${COMPOSITE_DELIMITER}_hour.
     * 
     * @param prefix
     * @param suffix
     * @return
     */
    public static String makeCompositeParam(String prefix, String suffix) {
        return prefix + COMPOSITE_DELIMITER + suffix;
    }

    /**
     * Given the prefix of a composite parameter, recomposes a single Object from 
     * the composite according to compositeType. For example, consider the following
     * form widget field,
     * 
     *   <field name="meetingDate">
     *     <date-time type="timestamp" input-method="time-dropdown">
     *   </field>
     *     
     * The result in HTML is three input boxes to input the date, hour and minutes separately.
     * The parameter names are named meetingDate_c_date, meetingDate_c_hour, meetingDate_c_minutes.
     * Additionally, there will be a field named meetingDate_c_compositeType with a value of "Timestamp".
     * where _c_ is the COMPOSITE_DELIMITER. These parameters will then be recomposed into a Timestamp
     * object from the composite fields.
     * 
     * @param request
     * @param prefix
     * @return Composite object from data or nulll if not supported or a parsing error occured.
     */
    public static Object makeParamValueFromComposite(HttpServletRequest request, String prefix, Locale locale) {
        String compositeType = request.getParameter(makeCompositeParam(prefix, "compositeType"));
        if (compositeType == null || compositeType.length() == 0) return null;
        Map data = FastMap.newInstance();
        for (Enumeration names = request.getParameterNames(); names.hasMoreElements(); ) {
            String name = (String) names.nextElement();
            if (!name.startsWith(prefix + COMPOSITE_DELIMITER)) continue;
            String suffix = name.substring(name.indexOf(COMPOSITE_DELIMITER) + COMPOSITE_DELIMITER_LENGTH);
            Object value = request.getParameter(name);
            data.put(suffix, value);
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Creating composite type with parameter data: " + data.toString());
        }
        if ("Timestamp".equals(compositeType)) {
            String date = (String) data.get("date");
            String hour = (String) data.get("hour");
            String minutes = (String) data.get("minutes");
            String ampm = (String) data.get("ampm");
            if (date == null || date.length() < 10) return null;
            if (hour == null || hour.length() == 0) return null;
            if (minutes == null || minutes.length() == 0) return null;
            boolean isTwelveHour = ((ampm == null || ampm.length() == 0) ? false : true);
            try {
                int h = Integer.parseInt(hour);
                Timestamp timestamp = Timestamp.valueOf(date.substring(0, 10) + " 00:00:00.000");
                Calendar cal = Calendar.getInstance(locale);
                cal.setTime(timestamp);
                if (isTwelveHour) {
                    boolean isAM = ("AM".equals(ampm) ? true : false);
                    if (isAM && h == 12) h = 0;
                    if (!isAM && h < 12) h += 12;
                }
                cal.set(Calendar.HOUR_OF_DAY, h);
                cal.set(Calendar.MINUTE, Integer.parseInt(minutes));
                return new Timestamp(cal.getTimeInMillis());
            } catch (IllegalArgumentException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("User input for composite timestamp was invalid: ", e);
                }
                return null;
            }
        }
        return null;
    }

    /** Obtains the session ID from the request, or "unknown" if no session pressent. */
    public static String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession();
        return (session == null ? "unknown" : session.getId());
    }
}
