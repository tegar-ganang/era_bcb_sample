package org.t2framework.commons.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.t2framework.commons.exception.IORuntimeException;
import org.t2framework.commons.exception.MalformedURLRuntimeException;

/**
 * <#if locale="en">
 * <p>
 * URL utility class.
 * 
 * </p>
 * <#else>
 * <p>
 * 
 * </p>
 * </#if>
 * 
 * @author shot
 * 
 */
public class URLUtil {

    protected static final Map<String, String> CANONICAL_PROTOCOLS = new HashMap<String, String>();

    protected static final String VALID_SCHEME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+.-";

    static {
        CANONICAL_PROTOCOLS.put("wsjar", "jar");
        CANONICAL_PROTOCOLS.put("vfszip", "zip");
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * no instantiation.
	 * </p>
	 * <#else>
	 * <p>
	 * 
	 * </p>
	 * </#if>
	 */
    private URLUtil() {
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * Get {@link URLConnection} from {@link URL}.
	 * 
	 * </p>
	 * <#else>
	 * <p>
	 * 
	 * </p>
	 * </#if>
	 * 
	 * @param url
	 * @return
	 * @throws IORuntimeException
	 */
    public static URLConnection openConnection(URL url) {
        try {
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection;
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * Get {@link InputStream} from {@link URL}.
	 * 
	 * </p>
	 * <#else>
	 * <p>
	 * 
	 * </p>
	 * </#if>
	 * 
	 * @param url
	 * @return
	 * @throws IORuntimeException
	 */
    public static InputStream openStream(URL url) {
        try {
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * Create {@link URL} from the string.
	 * 
	 * </p>
	 * <#else>
	 * <p>
	 * 
	 * </p>
	 * </#if>
	 * 
	 * @param spec
	 * @return
	 * @throws IORuntimeException
	 */
    public static URL create(String spec) {
        try {
            return new URL(spec);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * Create {@link URL} from context url and spec.
	 * 
	 * </p>
	 * <#else>
	 * <p>
	 * 
	 * </p>
	 * </#if>
	 * 
	 * @param context
	 * @param spec
	 * @return url
	 * @throws IORuntimeException
	 */
    public static URL create(URL context, String spec) {
        try {
            return new URL(context, spec);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * Encode string with the encode character.
	 * 
	 * </p>
	 * <#else>
	 * <p>
	 * 
	 * </p>
	 * </#if>
	 * 
	 * @param s
	 * @param enc
	 * @return
	 * @throws IORuntimeException
	 */
    public static String encode(final String s, final String enc) {
        try {
            return URLEncoder.encode(s, enc);
        } catch (final UnsupportedEncodingException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * Decode string with the encode character.
	 * 
	 * </p>
	 * <#else>
	 * <p>
	 * 
	 * </p>
	 * </#if>
	 * 
	 * @param s
	 * @param enc
	 * @return
	 * @throws IORuntimeException
	 */
    public static String decode(final String s, final String enc) {
        try {
            return URLDecoder.decode(s, enc);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * Convert protocol to canonical protocol.This method is used for
	 * application server dependent protocol like wsjar as jar for WebSphere.
	 * 
	 * </p>
	 * <#else>
	 * <p>
	 * 
	 * </p>
	 * </#if>
	 * 
	 * @param protocol
	 * @return
	 */
    public static String toCanonicalProtocol(final String protocol) {
        final String canonicalProtocol = CANONICAL_PROTOCOLS.get(protocol);
        if (canonicalProtocol != null) {
            return canonicalProtocol;
        }
        return protocol;
    }

    /**
	 * <#if locale="en">
	 * <p>
	 * Return the url is absolute url or not.
	 * 
	 * </p>
	 * <#else>
	 * <p>
	 * 
	 * </p>
	 * </#if>
	 * 
	 * @param url
	 * @return
	 */
    public static boolean isAbsoluteUrl(String url) {
        if (url == null) {
            return false;
        }
        int colonPos;
        if ((colonPos = url.indexOf(":")) == -1) {
            return false;
        }
        for (int i = 0; i < colonPos; i++) {
            if (VALID_SCHEME_CHARS.indexOf(url.charAt(i)) == -1) {
                return false;
            }
        }
        return true;
    }

    public static URL toUrl(File file) {
        Assertion.notNull(file);
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new MalformedURLRuntimeException(e);
        }
    }
}
