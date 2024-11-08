package org.wwweeeportal.portal.channelplugins;

import java.util.*;
import java.util.regex.*;
import javax.activation.*;
import javax.ws.rs.core.*;
import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.protocol.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.springframework.core.convert.converter.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.http.*;
import org.wwweeeportal.util.io.*;
import org.wwweeeportal.portal.*;
import org.wwweeeportal.portal.channels.*;

/**
 * Modifies the response given to a {@link org.wwweeeportal.portal.channels.ProxyChannel} from it's proxied
 * URL/document.
 */
public class ProxyChannelResponseAttributes extends Channel.Plugin {

    /**
   * <dl>
   * <dt>
   * WWWeee.ProxyChannel.ResponseAttributes.Header.Override.Content-Type.Path.^/$=application/xhtml+xml;charset=UTF-8</dt>
   * <dd>This would mean that for any channel local path matching the '^/$' pattern, it will override the 'Content-Type'
   * header with the value 'application/xhtml+xml;charset=UTF-8'.</dd>
   * <dt>WWWeee.ProxyChannel.ResponseAttributes.Header.Override.Content-Language.Path.^/$=fr</dt>
   * <dd>This would mean that for any channel local path matching the '^/$' pattern, it will override the
   * 'Content-Language' header with the value 'fr'.</dd>
   * <dt>WWWeee.ProxyChannel.ResponseAttributes.Header.Override.Content-Type.Type.^text/html=application/xhtml+xml</dt>
   * <dd>This would mean that for any channel response Content-Type matching the '^text/html' pattern, it will override
   * the 'Content-Type' header with the value 'application/xhtml+xml'.</dd>
   * </dl>
   */
    public static final String HEADER_OVERRIDE_BY_PATH_PROP = "ProxyChannel.ResponseAttributes.Header.Override.Path.";

    protected static final Pattern HEADER_OVERRIDE_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(HEADER_OVERRIDE_BY_PATH_PROP) + ".*");

    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>> HEADER_OVERRIDE_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<String>, Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>>(RSProperties.RESULT_ENTRY_STRING_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<String>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<String>()));

    public static final String HEADER_OVERRIDE_BY_TYPE_PROP = "ProxyChannel.ResponseAttributes.Header.Override.Type.";

    protected static final Pattern HEADER_OVERRIDE_BY_TYPE_PATTERN = Pattern.compile("^" + Pattern.quote(HEADER_OVERRIDE_BY_TYPE_PROP) + ".*");

    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>> HEADER_OVERRIDE_BY_TYPE_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<String>, Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>>(RSProperties.RESULT_ENTRY_STRING_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<String>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<String>()));

    public static final String HEADER_ETAG_GENERATE_MISSING_PROP = "ProxyChannel.ResponseAttributes.Header.ETag.GenerateMissing";

    public static final String COOKIES_SESSION_STORAGE_ENABLE_PROP = "ProxyChannel.ResponseAttributes.Cookies.SessionStorage.Enable";

    public ProxyChannelResponseAttributes(final Channel channel, final ContentManager.ChannelPluginDefinition<?> definition) throws WWWeeePortal.Exception {
        channel.super(definition);
        if (!(channel instanceof ProxyChannel)) {
            throw new ConfigManager.ConfigException(getClass().getSimpleName() + " only works with ProxyChannel", null);
        }
        return;
    }

    protected ProxyChannel getProxyChannel() {
        return (ProxyChannel) getChannel();
    }

    protected boolean isCookiesSessionStorageEnabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(COOKIES_SESSION_STORAGE_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected Header getHeaderOverrideByPath(final String headerName, final Page.Request pageRequest) throws WWWeeePortal.Exception {
        final Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>> headerOverride = CollectionUtil.first(CollectionUtil.keySet(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), HEADER_OVERRIDE_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), HEADER_OVERRIDE_BY_PATH_CONVERTER, true, true), headerName, false, StringUtil.toString(pageRequest.getChannelLocalPath(getChannel()), null))), null);
        return (headerOverride != null) ? new BasicHeader(headerOverride.getValue().getKey(), headerOverride.getKey().getValue()) : null;
    }

    protected Header getHeaderOverrideByType(final String headerName, final MimeType contentType, final Page.Request pageRequest) throws WWWeeePortal.Exception {
        final Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>> headerOverride = CollectionUtil.first(CollectionUtil.keySet(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), HEADER_OVERRIDE_BY_TYPE_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), HEADER_OVERRIDE_BY_TYPE_CONVERTER, true, true), headerName, false, ConversionUtil.invokeConverter(IOUtil.MIME_TYPE_BASE_TYPE_CONVERTER, contentType))), null);
        return (headerOverride != null) ? new BasicHeader(headerOverride.getValue().getKey(), headerOverride.getKey().getValue()) : null;
    }

    protected EntityTag generateMissingEntityTag(final Page.Request pageRequest, final HttpResponse proxyResponse) throws WWWeeePortal.Exception {
        if (!getConfigProp(HEADER_ETAG_GENERATE_MISSING_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue()) return null;
        final CacheControl cacheControl = getProxyChannel().getProxyResponseHeader(pageRequest, proxyResponse, "Cache-Control", ProxyChannel.HEADER_CACHE_CONTROL_CONVERTER);
        final Long contentLength = getProxyChannel().getProxyResponseHeader(pageRequest, proxyResponse, "Content-Length", HTTPUtil.HEADER_LONG_CONVERTER);
        final Date lastModified = getProxyChannel().getProxyResponseHeader(pageRequest, proxyResponse, "Last-Modified", HTTPUtil.HEADER_DATE_CONVERTER);
        return Page.createEntityTag(pageRequest, cacheControl, null, contentLength, lastModified, false);
    }

    protected void addRequestCookiesFromSession(final Page.Request pageRequest, final HttpClient proxyClient) throws WWWeeePortal.Exception {
        if ((!isCookiesSessionStorageEnabled(pageRequest)) || (!(proxyClient instanceof DefaultHttpClient))) return;
        @SuppressWarnings("unchecked") final List<org.apache.http.cookie.Cookie> userCookies = (List<org.apache.http.cookie.Cookie>) pageRequest.getSessionAttributes().get(createClientAttributesKey(pageRequest, "Cookies", false, null));
        if (userCookies != null) {
            for (org.apache.http.cookie.Cookie userCookie : userCookies) {
                ((DefaultHttpClient) proxyClient).getCookieStore().addCookie(userCookie);
            }
        }
        return;
    }

    protected void storeResponseCookiesInSession(final Page.Request pageRequest, final HttpClient proxyClient, final HttpContext proxyContext) throws WWWeeePortal.Exception {
        if (!isCookiesSessionStorageEnabled(pageRequest)) return;
        CookieStore cookieStore = (CookieStore) proxyContext.getAttribute(ClientContext.COOKIE_STORE);
        if ((cookieStore == null) && (proxyClient instanceof DefaultHttpClient)) cookieStore = ((DefaultHttpClient) proxyClient).getCookieStore();
        List<org.apache.http.cookie.Cookie> proxiedCookies = (cookieStore != null) ? cookieStore.getCookies() : null;
        if ((proxiedCookies != null) && (proxiedCookies.isEmpty())) proxiedCookies = null;
        pageRequest.getSessionAttributes().put(createClientAttributesKey(pageRequest, "Cookies", false, null), proxiedCookies);
        return;
    }

    @Override
    protected <T> T pluginValueHook(final Channel.PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest) throws WWWeeePortal.Exception {
        if (ProxyChannel.PROXY_RESPONSE_HEADER_HOOK.equals(pluginHook)) {
            final HttpResponse proxyResponse = (HttpResponse) context[0];
            final String headerName = (String) context[1];
            Header newHeader = getHeaderOverrideByPath(headerName, pageRequest);
            if (newHeader != null) return pluginHook.getResultClass().cast(newHeader);
            final MimeType contentType = ConversionUtil.invokeConverter(HTTPUtil.HEADER_MIME_TYPE_CONVERTER, (proxyResponse.getEntity() != null) ? proxyResponse.getEntity().getContentType() : null);
            newHeader = getHeaderOverrideByType(headerName, contentType, pageRequest);
            if (newHeader != null) return pluginHook.getResultClass().cast(newHeader);
            if ("ETag".equalsIgnoreCase(headerName)) {
                if (!proxyResponse.containsHeader("ETag")) {
                    final EntityTag entityTag = generateMissingEntityTag(pageRequest, proxyResponse);
                    if (entityTag != null) return pluginHook.getResultClass().cast(new BasicHeader("ETag", entityTag.toString()));
                }
            }
        }
        return null;
    }

    @Override
    protected <T> T pluginFilterHook(final Channel.PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest, final T data) throws WWWeeePortal.Exception {
        if (ProxyChannel.PROXY_CLIENT_HOOK.equals(pluginHook)) {
            final HttpClient proxyClient = ProxyChannel.PROXY_CLIENT_HOOK.getResultClass().cast(data);
            addRequestCookiesFromSession(pageRequest, proxyClient);
        } else if (ProxyChannel.PROXY_RESPONSE_HOOK.equals(pluginHook)) {
            final HttpClient proxyClient = (HttpClient) context[1];
            final HttpContext proxyContext = (HttpContext) context[2];
            storeResponseCookiesInSession(pageRequest, proxyClient, proxyContext);
        }
        return data;
    }
}
