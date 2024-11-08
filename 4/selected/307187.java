package com.luzan.common.httprpc;

import com.luzan.common.httprpc.annotation.*;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlNs;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

/**
 * HttpRpcUtils
 *
 * @author Alexander Bondar
 */
public class HttpRpcUtils {

    private static final Logger logger = Logger.getLogger(HttpRpcUtils.class);

    public static final Pattern PATTERN_EMAIL = Pattern.compile("^((([a-z]|[0-9]|!|#|$|%|&|'|\\*|\\+|\\-|/|=|\\?|\\^|_|`|\\{|\\||\\}|~)+(\\.([a-z]|[0-9]|!|#|$|%|&|'|\\*|\\+|\\-|/|=|\\?|\\^|_|`|\\{|\\||\\}|~)+)*)@((((([a-z]|[0-9])([a-z]|[0-9]|\\-){0,61}([a-z]|[0-9])\\.))*([a-z]|[0-9])([a-z]|[0-9]|\\-){0,61}([a-z]|[0-9])\\.(af|ax|al|dz|as|ad|ao|ai|aq|ag|ar|am|aw|au|at|az|bs|bh|bd|bb|by|be|bz|bj|bm|bt|bo|ba|bw|bv|br|io|bn|bg|bf|bi|kh|cm|ca|cv|ky|cf|td|cl|cn|cx|cc|co|km|cg|cd|ck|cr|ci|hr|cu|cy|cz|dk|dj|dm|do|ec|eg|sv|gq|er|ee|et|fk|fo|fj|fi|fr|gf|pf|tf|ga|gm|ge|de|gh|gi|gr|gl|gd|gp|gu|gt| gg|gn|gw|gy|ht|hm|va|hn|hk|hu|is|in|id|ir|iq|ie|im|il|it|jm|jp|je|jo|kz|ke|ki|kp|kr|kw|kg|la|lv|lb|ls|lr|ly|li|lt|lu|mo|mk|mg|mw|my|mv|ml|mt|mh|mq|mr|mu|yt|mx|fm|md|mc|mn|ms|ma|mz|mm|na|nr|np|nl|an|nc|nz|ni|ne|ng|nu|nf|mp|no|om|pk|pw|ps|pa|pg|py|pe|ph|pn|pl|pt|pr|qa|re|ro|ru|rw|sh|kn|lc|pm|vc|ws|sm|st|sa|sn|cs|sc|sl|sg|sk|si|sb|so|za|gs|es|lk|sd|sr|sj|sz|se|ch|sy|tw|tj|tz|th|tl|tg|tk|to|tt|tn|tr|tm|tc|tv|ug|ua|ae|gb|us|um|uy|uz|vu|ve|vn|vg|vi|wf|eh|ye|zm|zw|com|edu|gov|int|mil|net|org|biz|info|name|pro|aero|coop|museum|arpa))|(((([0-9]){1,3}\\.){3}([0-9]){1,3}))|(\\[((([0-9]){1,3}\\.){3}([0-9]){1,3})\\])))$");

    public static final Pattern PATTERN_LOGIN = Pattern.compile("^([a-zA-Z](?:\\w[\\.\\_]?){3,14})([a-zA-Z0-9])$");

    public static final Pattern PATTERN_PASSWORD = Pattern.compile("^.{6,16}$");

    public static final int MEMORY_FILE_SIZE_THRESHOLD = 10 * 1024 * 1024;

    public static final String ATTR_JSON_OBJECT = "json.object";

    private static final String HTTP_HEADER_AUTHORIZATION = "Authorization";

    private static final String WSSE = "WSSE ";

    private static final String WWW_AUTH = "WWW-authenticate";

    private static final String WSSE_REALM = "WSSE  realm=\"Luzan\", profile=\"UsernameToken\"";

    private static HashMap<Class, Collection<String>> excludedProperties = new HashMap<Class, Collection<String>>();

    private static final String CACHE_PM_NAME = "HTTP_RPC_UTILS_CACHE_PM";

    private static final String CACHE_PCM_NAME = "HTTP_RPC_UTILS_CACHE_PCM";

    static {
        CacheManager.getInstance().addCache(new Cache(CACHE_PM_NAME, 1000, false, true, 0, 0));
        CacheManager.getInstance().addCache(new Cache(CACHE_PCM_NAME, 1000, false, true, 0, 0));
    }

    @SuppressWarnings({ "unchecked" })
    public static Map<String, Class<?>> getPropertyClassesMap(Class<?> bean) throws IntrospectionException {
        final Cache cache = CacheManager.getInstance().getCache(CACHE_PCM_NAME);
        if (cache.isKeyInCache(bean)) return (Map<String, Class<?>>) cache.get(bean).getValue();
        Class stopClass = Object.class;
        if (bean.isAssignableFrom(HttpRpcException.class)) stopClass = Exception.class;
        if (bean.isInterface()) stopClass = null;
        final Map<String, Class<?>> paramMap = new HashMap<String, Class<?>>();
        for (PropertyDescriptor p : Introspector.getBeanInfo(bean, stopClass).getPropertyDescriptors()) {
            final Method m = p.getReadMethod();
            final String propName;
            if (m == null) continue;
            if (m.isAnnotationPresent(HttpHiddenField.class)) continue; else if (m.isAnnotationPresent(HttpParameter.class)) {
                final HttpParameter ann = m.getAnnotation(HttpParameter.class);
                propName = ann.name();
            } else propName = p.getName();
            if (Collection.class.isAssignableFrom(m.getReturnType())) {
                final Type returnType = m.getGenericReturnType();
                if (returnType instanceof ParameterizedType) paramMap.put(propName, (Class) ((ParameterizedType) returnType).getActualTypeArguments()[0]);
            } else paramMap.put(propName, p.getPropertyType());
        }
        cache.put(new Element(bean, paramMap));
        return paramMap;
    }

    @SuppressWarnings({ "unchecked" })
    public static Map<String, PropertyDescriptor> getPropertiesMap(Class bean) throws IntrospectionException {
        final Cache cache = CacheManager.getInstance().getCache(CACHE_PM_NAME);
        if (cache.isKeyInCache(bean)) return (Map<String, PropertyDescriptor>) cache.get(bean).getValue();
        Class stopClass = Object.class;
        if (bean.isAssignableFrom(HttpRpcException.class)) stopClass = Exception.class;
        if (bean.isInterface()) stopClass = null;
        final Map<String, PropertyDescriptor> paramMap = new HashMap<String, PropertyDescriptor>();
        for (PropertyDescriptor p : Introspector.getBeanInfo(bean, stopClass).getPropertyDescriptors()) {
            final Method m = p.getReadMethod();
            if (m == null) continue;
            if (m.isAnnotationPresent(HttpHiddenField.class)) continue; else if (m.isAnnotationPresent(HttpParameter.class)) {
                final HttpParameter ann = m.getAnnotation(HttpParameter.class);
                paramMap.put(ann.name(), p);
            } else paramMap.put(p.getName(), p);
        }
        cache.put(new Element(bean, paramMap));
        return paramMap;
    }

    public static Collection<String> getExcludedProperties(Object bean) throws IntrospectionException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return getExcludedProperties(bean, excludedProperties);
    }

    private static Collection<String> getExcludedProperties(Object bean, HashMap<Class, Collection<String>> checked) throws IntrospectionException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (checked.containsKey(bean.getClass())) return checked.get(bean.getClass());
        Collection<String> excludes = new Vector<String>();
        checked.put(bean.getClass(), excludes);
        for (PropertyDescriptor p : PropertyUtils.getPropertyDescriptors(bean)) {
            final Method m = p.getReadMethod();
            if (m == null) continue;
            final String packageName = (!p.getPropertyType().equals(Object.class) && p.getPropertyType().getPackage() != null) ? p.getPropertyType().getPackage().getName() : "";
            if (m.isAnnotationPresent(HttpHiddenField.class)) excludes.add(p.getName()); else if ((packageName.indexOf("java.") < 0) && (packageName.indexOf("javax.") < 0) && !p.getPropertyType().isPrimitive() && !checked.containsKey(p.getPropertyType())) {
                final Object _bean = PropertyUtils.getProperty(bean, p.getName());
                if (_bean != null) excludes.addAll(getExcludedProperties(_bean, checked));
            }
        }
        return excludes;
    }

    public static Map<String, Method> getMethodsMap(Class bean) throws IntrospectionException {
        final Map<String, Method> methodMap = new HashMap<String, Method>();
        final Pattern ptrn = Pattern.compile("(get|post|put|delete|head)(\\p{Upper}\\w*)");
        for (MethodDescriptor p : Introspector.getBeanInfo(bean, Object.class).getMethodDescriptors()) {
            final Method m = p.getMethod();
            if (m == null || m.getDeclaringClass() == Object.class) continue;
            String prefix = getActionPrefix(m).toLowerCase();
            if (m.isAnnotationPresent(HttpAction.class)) {
                final HttpAction ann = m.getAnnotation(HttpAction.class);
                for (HttpAction.Method method : ann.method()) methodMap.put(String.valueOf(method).toLowerCase() + prefix + ann.name().toLowerCase(), m);
            } else if (Modifier.isPublic(m.getModifiers())) {
                HttpAction.Method[] actionMethods = getActionMethod(m);
                if (actionMethods == null) {
                    final Matcher mr = ptrn.matcher(m.getName());
                    if (mr.matches()) methodMap.put(p.getName().toLowerCase(), m); else methodMap.put(String.valueOf(HttpAction.Method.post).toLowerCase() + prefix + m.getName().toLowerCase(), m);
                } else {
                    for (HttpAction.Method method : actionMethods) {
                        methodMap.put(String.valueOf(method).toLowerCase() + prefix + m.getName().toLowerCase(), m);
                    }
                }
            }
        }
        return methodMap;
    }

    public static Map<String, Method> getActionsMap(Class<?> bean) throws IntrospectionException {
        final Map<String, Method> methodMap = new HashMap<String, Method>();
        final Pattern ptrn = Pattern.compile("(get|post|put|delete|head)(\\p{Upper}\\w*)");
        for (MethodDescriptor p : Introspector.getBeanInfo(bean, Object.class).getMethodDescriptors()) {
            final Method m = p.getMethod();
            if (m == null || m.getDeclaringClass() == Object.class) continue;
            String prefix = getActionPrefix(m).toLowerCase();
            if (m.isAnnotationPresent(HttpAction.class)) {
                final HttpAction ann = m.getAnnotation(HttpAction.class);
                methodMap.put(prefix + ann.name().toLowerCase(), m);
            } else if (Modifier.isPublic(m.getModifiers())) {
                if (getActionMethod(m) != null) methodMap.put(prefix + m.getName().toLowerCase(), m); else {
                    final Matcher mr = ptrn.matcher(m.getName());
                    if (mr.matches()) methodMap.put(mr.group(2).toLowerCase(), m); else methodMap.put(prefix + m.getName().toLowerCase(), m);
                }
            }
        }
        return methodMap;
    }

    private static HttpAction.Method[] getActionMethod(Method method) {
        if (method.isAnnotationPresent(HttpAction.class)) return method.getAnnotation(HttpAction.class).method();
        Class<?> clazz = method.getDeclaringClass();
        if (clazz.isAnnotationPresent(HttpActionMethod.class)) return clazz.getAnnotation(HttpActionMethod.class).value();
        return null;
    }

    private static String getActionPrefix(Method method) {
        String prefix = null;
        if (method.isAnnotationPresent(HttpAction.class)) {
            prefix = method.getAnnotation(HttpAction.class).prefix();
            if ("##default".equals(prefix)) prefix = null;
        }
        if (prefix == null) {
            Class<?> clazz = method.getDeclaringClass();
            if (clazz.isAnnotationPresent(HttpActionPrefix.class)) prefix = clazz.getAnnotation(HttpActionPrefix.class).value();
        }
        if (prefix == null || prefix.length() == 0) return "";
        return prefix + ".";
    }

    public static UserProfileImpl authenticate(HttpServletRequest request, HttpServletResponse response, boolean noneAllowed, UserAccessorImpl userAccessor) throws IOException, FileUploadException, DecoderException {
        if (userAccessor == null) {
            logger.error("UserAccessorImpl is null");
            if (!noneAllowed) response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
        String authToken = null;
        String header = request.getHeader(HTTP_HEADER_AUTHORIZATION);
        if (header != null) {
            if (header.indexOf(WSSE) >= 0) authToken = request.getHeader("X-WSSE");
        } else if (request.getParameterMap().containsKey("auth")) {
            authToken = request.getParameter("auth");
            if (!StringUtils.isEmpty(authToken)) authToken = new String(Base64.decodeBase64(authToken.getBytes()));
        } else if (request.getAttribute("auth") != null) {
            Object authObj = request.getAttribute("auth");
            if (String.class.isAssignableFrom(authObj.getClass())) {
                authToken = (String) authObj;
                if (!StringUtils.isEmpty(authToken)) authToken = new String(Base64.decodeBase64(authToken.getBytes()));
            }
        }
        if (!StringUtils.isEmpty(authToken)) {
            WSSEToken auth = new WSSEToken(authToken);
            UserProfileImpl user = auth.authenticate(userAccessor);
            if (user == null) {
                logger.error("Unable to authenticate user. X-WSSE " + authToken);
                if (!noneAllowed) response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
            return user;
        } else {
            response.setHeader(WWW_AUTH, WSSE_REALM);
            if (!noneAllowed) response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
        return null;
    }

    public static boolean readFully(InputStream in, byte buf[]) throws IOException {
        return readFully(in, buf, 0, buf.length);
    }

    public static boolean readFully(InputStream in, byte buf[], int pos, int len) throws IOException {
        int got_total = 0;
        while (got_total < len) {
            int got = in.read(buf, pos + got_total, len - got_total);
            if (got == -1) {
                if (got_total == 0) return false;
                throw new EOFException("readFully: end of file; expected " + len + " bytes, got only " + got_total);
            }
            got_total += got;
        }
        return true;
    }

    /**
     * Read the input stream and dump it all into a big byte array
     *
     * @param is input stream
     * @return byte[]
     * @throws java.io.IOException IO Error
     */
    public static byte[] slurpInputStream(InputStream is) throws IOException {
        final int chunkSize = 2048;
        byte[] buf = new byte[chunkSize];
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(chunkSize);
        int count;
        while ((count = is.read(buf)) != -1) byteStream.write(buf, 0, count);
        return byteStream.toByteArray();
    }

    public static boolean isFile(FileItem item) {
        Object value = null;
        if (!item.isFormField()) {
            if (logger.isDebugEnabled()) logger.debug("Extract file: " + item.getName() + " size: " + item.getSize());
            try {
                value = item.getInputStream();
            } catch (IOException e) {
                logger.error("error:", e);
            }
        }
        return (value != null);
    }

    public static Class<?> getClass(Type t) {
        if (t instanceof ParameterizedType) return (Class<?>) ((ParameterizedType) t).getRawType(); else return (Class<?>) t;
    }

    public static int proxy(java.net.URI uri, HttpServletRequest req, HttpServletResponse res) throws IOException {
        final HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(uri.getHost());
        HttpMethodBase httpMethod = null;
        if (HttpRpcServer.METHOD_GET.equalsIgnoreCase(req.getMethod())) {
            httpMethod = new GetMethod(uri.toString());
            httpMethod.setFollowRedirects(true);
        } else if (HttpRpcServer.METHOD_POST.equalsIgnoreCase(req.getMethod())) {
            httpMethod = new PostMethod(uri.toString());
            final Enumeration parameterNames = req.getParameterNames();
            if (parameterNames != null) while (parameterNames.hasMoreElements()) {
                final String parameterName = (String) parameterNames.nextElement();
                for (String parameterValue : req.getParameterValues(parameterName)) ((PostMethod) httpMethod).addParameter(parameterName, parameterValue);
            }
            ((PostMethod) httpMethod).setRequestEntity(new InputStreamRequestEntity(req.getInputStream()));
        }
        if (httpMethod == null) throw new IllegalArgumentException("Unsupported http request method");
        final int responseCode;
        final Enumeration headers = req.getHeaderNames();
        if (headers != null) while (headers.hasMoreElements()) {
            final String headerName = (String) headers.nextElement();
            final Enumeration headerValues = req.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                httpMethod.setRequestHeader(headerName, (String) headerValues.nextElement());
            }
        }
        final HttpState httpState = new HttpState();
        if (req.getCookies() != null) for (Cookie cookie : req.getCookies()) {
            String host = req.getHeader("Host");
            if (StringUtils.isEmpty(cookie.getDomain())) cookie.setDomain(StringUtils.isEmpty(host) ? req.getServerName() + ":" + req.getServerPort() : host);
            if (StringUtils.isEmpty(cookie.getPath())) cookie.setPath("/");
            httpState.addCookie(new org.apache.commons.httpclient.Cookie(cookie.getDomain(), cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getMaxAge(), cookie.getSecure()));
        }
        httpMethod.setQueryString(req.getQueryString());
        responseCode = (new HttpClient()).executeMethod(hostConfig, httpMethod, httpState);
        if (responseCode < 0) {
            httpMethod.releaseConnection();
            return responseCode;
        }
        if (httpMethod.getResponseHeaders() != null) for (Header header : httpMethod.getResponseHeaders()) res.setHeader(header.getName(), header.getValue());
        final InputStream in = httpMethod.getResponseBodyAsStream();
        final OutputStream out = res.getOutputStream();
        IOUtils.copy(in, out);
        out.flush();
        out.close();
        in.close();
        httpMethod.releaseConnection();
        return responseCode;
    }

    public static NamespacePrefixMapper getNamespacePrefixMapper(Class clazz) {
        if (clazz.getPackage().isAnnotationPresent(XmlSchema.class)) {
            final Map<String, String> prefixMap = new HashMap<String, String>();
            XmlSchema schema = clazz.getPackage().getAnnotation(XmlSchema.class);
            for (XmlNs ns : schema.xmlns()) {
                prefixMap.put(ns.namespaceURI(), ns.prefix());
            }
            return new NamespacePrefixMapper() {

                public String getPreferredPrefix(String namespaceUri, String suggestion, boolean b) {
                    if (prefixMap.containsKey(namespaceUri)) return prefixMap.get(namespaceUri);
                    return suggestion;
                }
            };
        }
        return null;
    }
}
