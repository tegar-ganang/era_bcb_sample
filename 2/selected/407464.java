package org.vardb.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.vardb.CConstants;
import org.vardb.CVardbException;

public final class CWebHelper {

    public static final String JSESSIONID = "JSESSIONID";

    private static final String SUCCESS = "success";

    private static final String MESSAGE = "message";

    private CWebHelper() {
    }

    public static String getServerName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    public static String joinParams(Map<String, Object> params, boolean escape) {
        StringBuilder buffer = new StringBuilder();
        String separator = "";
        for (String name : params.keySet()) {
            buffer.append(separator);
            buffer.append(name + "=" + params.get(name).toString());
            if (escape) separator = "&amp;"; else separator = "&";
        }
        return buffer.toString();
    }

    public static String getUrl(HttpServletRequest request) {
        StringBuilder buffer = new StringBuilder("");
        if (request.getRequestURL() != null) {
            buffer.append(request.getRequestURI());
            String qs = request.getQueryString();
            if (CStringHelper.hasContent(qs)) buffer.append("?" + qs);
        }
        return buffer.toString();
    }

    public static String getQueryString(HttpServletRequest request) {
        String qs = request.getQueryString();
        try {
            qs = URLDecoder.decode(qs, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new CVardbException(e);
        }
        return qs;
    }

    public static List<String> getHeaders(HttpServletRequest request) {
        List<String> headers = new ArrayList<String>();
        for (Enumeration<?> e = request.getHeaderNames(); e.hasMoreElements(); ) {
            String name = (String) e.nextElement();
            String value = (String) request.getHeader(name);
            headers.add("header " + name + "=" + value);
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getParameters(HttpServletRequest request, int maxlength) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        for (Enumeration<String> enumeration = request.getParameterNames(); enumeration.hasMoreElements(); ) {
            String name = (String) enumeration.nextElement();
            String value = (String) request.getParameter(name);
            if (value.length() > maxlength) value = CStringHelper.truncate(value, maxlength);
            params.put(name, value);
        }
        return params;
    }

    public static String getOriginalFilename(HttpServletRequest request, String name) {
        if (!(request instanceof MultipartHttpServletRequest)) throw new CVardbException("request is not an instance of MultipartHttpServletRequest");
        MultipartHttpServletRequest multipart = (MultipartHttpServletRequest) request;
        CommonsMultipartFile file = (CommonsMultipartFile) multipart.getFileMap().get(name);
        return file.getOriginalFilename();
    }

    public static boolean isLinkChecker(HttpServletRequest request) {
        String user_agent = request.getHeader("User-Agent");
        if (user_agent.indexOf("Xenu") != -1) return true;
        return false;
    }

    public static String getWebapp(HttpServletRequest request) {
        String webapp = request.getContextPath();
        System.out.println("webapp=" + webapp);
        return webapp;
    }

    public static Cookie setCookie(HttpServletRequest request, HttpServletResponse response, String name, String value, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(0);
        cookie.setPath(path);
        response.addCookie(cookie);
        displayCookie(cookie);
        return cookie;
    }

    public static String getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            displayCookie(cookie);
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }

    public static void displayCookie(Cookie cookie) {
        System.out.println("COOKIE----------------------------------------");
        System.out.println("cookie: " + cookie.getName());
        System.out.println("value: " + cookie.getValue());
        System.out.println("domain: " + cookie.getDomain());
        System.out.println("path: " + cookie.getPath());
    }

    public static void removeCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        response.addCookie(cookie);
    }

    public static void removeCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        for (int i = 0; i < cookies.length; i++) {
            Cookie oldcookie = cookies[i];
            String name = oldcookie.getName();
            if (JSESSIONID.equals(name)) continue;
            Cookie cookie = new Cookie(name, null);
            response.addCookie(cookie);
        }
    }

    public static Map<String, String> getCookies(HttpServletRequest request) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return map;
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            map.put(cookie.getName(), cookie.getValue());
        }
        return map;
    }

    public static String createQueryString(Map<String, Object> params) {
        List<String> pairs = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            pairs.add(entry.getKey() + "=" + entry.getValue());
        }
        return CStringHelper.join(pairs, "&");
    }

    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    public static boolean isIE(HttpServletRequest request) {
        return isIE(getUserAgent(request));
    }

    public static boolean isIE(String browser) {
        return (browser.toLowerCase().indexOf("msie") != -1);
    }

    public static String getReferer(HttpServletRequest request) {
        return request.getHeader("referer");
    }

    public static String getIpaddress(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    public static String getSessionid(HttpServletRequest request) {
        String sessionid = "";
        HttpSession session = request.getSession();
        if (session != null) sessionid = session.getId();
        return sessionid;
    }

    public static OutputStream getOutputStream(HttpServletResponse response) {
        try {
            return response.getOutputStream();
        } catch (Exception e) {
            throw new CVardbException(e);
        }
    }

    public static String json(HttpServletResponse response, Object... args) {
        response.setContentType(CConstants.ContentType.JSON);
        String json = CStringHelper.toJson(args);
        return write(response, json);
    }

    protected String jsonSuccess(HttpServletResponse response) {
        return json(response, SUCCESS, true, MESSAGE, "success");
    }

    public static String jsonSuccess(HttpServletResponse response, Object... args) {
        if (args.length == 1) return json(response, SUCCESS, true, MESSAGE, args[0]);
        Map<String, Object> map = CStringHelper.createMap(args);
        map.put(SUCCESS, true);
        if (!map.containsKey(MESSAGE)) map.put(MESSAGE, "success");
        return json(response, map);
    }

    public static String jsonUploadSuccess(HttpServletResponse response, Object... args) {
        Map<String, Object> map = CStringHelper.createMap(args);
        map.put(SUCCESS, true);
        if (!map.containsKey(MESSAGE)) map.put(MESSAGE, "success");
        response.setContentType(CConstants.ContentType.HTML);
        String json = CStringHelper.toJson(map);
        return write(response, json);
    }

    public static String write(HttpServletResponse response, String str) {
        try {
            PrintWriter writer = response.getWriter();
            writer.print(str);
            writer.flush();
            return null;
        } catch (IOException e) {
            throw new CVardbException(e);
        }
    }

    public static void setFileDownload(HttpServletResponse response, String filename) {
        response.setContentType(CConstants.ContentType.TXT);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
    }

    public static String[] parseUrlParams(HttpServletRequest request, String regex) {
        String url = request.getServletPath();
        regex = regex.replaceAll("\\*", "([-a-zA-Z0-9.]*)");
        System.out.println("regex=" + regex);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        boolean result = matcher.find();
        if (!result) throw new CVardbException("url does not match pattern: " + url + "[" + regex + "]");
        String[] list = new String[matcher.groupCount()];
        for (int index = 0; index < list.length; index++) {
            list[index] = matcher.group(index + 1);
        }
        return list;
    }

    public static String redirect(HttpServletRequest request, HttpServletResponse response, String url) {
        try {
            if (url.substring(0, 1).equals("/")) url = CWebHelper.getWebapp(request) + url;
            Writer writer = response.getWriter();
            StringBuilder buffer = new StringBuilder();
            buffer.append("<html>\n");
            buffer.append("<head>\n");
            buffer.append("<meta http-equiv=\"refresh\" CONTENT=0;URL=" + url + ">\n");
            buffer.append("</head>\n");
            buffer.append("</html>\n");
            writer.write(buffer.toString());
            writer.flush();
            return null;
        } catch (IOException e) {
            throw new CVardbException(e);
        }
    }

    public static String parseIdentifier(HttpServletRequest request) {
        String url = request.getServletPath();
        int start = url.lastIndexOf('/') + 1;
        int end = url.lastIndexOf('.');
        String identifier = url.substring(start, end);
        System.out.println("identifier=" + identifier + " (" + url + ")");
        return identifier;
    }

    public static String parseIdentifier(HttpServletRequest request, String regex) {
        String params[] = CWebHelper.parseUrlParams(request, regex);
        return params[0];
    }

    public static String getFolder(HttpServletRequest request) {
        List<String> folders = getFolders(request, CConstants.BASEDIR_ATTRIBUTE, CConstants.FOLDER_ATTRIBUTE);
        return (folders.isEmpty()) ? "" : folders.get(0);
    }

    public static List<String> getFolders(HttpServletRequest request) {
        return getFolders(request, CConstants.BASEDIR_ATTRIBUTE, CConstants.FOLDERS_ATTRIBUTE);
    }

    public static List<String> getFolders(HttpServletRequest request, String basedir_attribute, String folder_attribute) {
        try {
            String baseDir = ServletRequestUtils.getStringParameter(request, basedir_attribute, "").trim();
            String str = ServletRequestUtils.getRequiredStringParameter(request, folder_attribute).trim();
            List<String> folders = new ArrayList<String>();
            for (String line : CStringHelper.splitLines(str)) {
                if (line.indexOf('#') == 0) continue;
                String path = baseDir + line;
                if (CFileHelper.isFolder(path)) folders.add(path);
            }
            return folders;
        } catch (ServletException e) {
            throw new CVardbException(e);
        }
    }

    public static List<String> getFilenames(HttpServletRequest request) {
        return getFilenames(request, CConstants.BASEDIR_ATTRIBUTE, CConstants.FOLDERS_ATTRIBUTE);
    }

    public static List<String> getFilenames(HttpServletRequest request, String basedir_attribute, String attribute) {
        try {
            String baseDir = ServletRequestUtils.getRequiredStringParameter(request, basedir_attribute).trim();
            String str = ServletRequestUtils.getRequiredStringParameter(request, attribute).trim();
            List<String> filenames = new ArrayList<String>();
            for (String line : CStringHelper.splitLines(str)) {
                line = line.trim();
                if (line.indexOf('#') == 0) continue;
                String path = baseDir + line;
                if (!CFileHelper.isFolder(path)) filenames.add(path);
            }
            return filenames;
        } catch (ServletException e) {
            throw new CVardbException(e);
        }
    }

    private static final String RSS_USER_AGENT = "Mozilla 5.0 (Windows; U; " + "Windows NT 5.1; en-US; rv:1.8.0.11) ";

    public static String readRss(String feed, int num) {
        InputStream stream = null;
        try {
            feed = appendParam(feed, "num", "" + num);
            System.out.println("feed=" + feed);
            URL url = new URL(feed);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", RSS_USER_AGENT);
            stream = connection.getInputStream();
            return CFileHelper.readInputStream(stream);
        } catch (Exception e) {
            throw new CVardbException(e);
        } finally {
            CFileHelper.closeStream(stream);
        }
    }

    public static String appendParam(String url, String name, String value) {
        if (url.indexOf("?") == -1) url += "?"; else url += "&";
        return url + name + "=" + value;
    }
}
