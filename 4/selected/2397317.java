package org.wwweeeportal.portal.channelplugins;

import java.util.*;
import org.w3c.dom.*;
import javax.ws.rs.core.*;
import net.sf.jsr107cache.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.logging.*;
import org.wwweeeportal.util.net.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;

/**
 * Stores the output of a {@link org.wwweeeportal.portal.Channel} for reuse.
 */
public class ChannelCache extends Channel.Plugin {

    /**
   * @see ConfigManager#getCache(Class, String, RSProperties, String, String, String)
   */
    public static final String GLOBAL_RESPONSE_JCACHE_PROPERTY_PROP = "Channel.Cache.GlobalResponse.JCache.Property.";

    /**
   * @see ConfigManager#getCache(Class, String, RSProperties, String, String, String)
   */
    public static final String GLOBAL_RESPONSE_JCACHE_NAME_PROP = "Channel.Cache.GlobalResponse.JCache.Name";

    /**
   * @see ConfigManager#getCache(Class, String, RSProperties, String, String, String)
   */
    public static final String GLOBAL_RESPONSE_JCACHE_PROPERTIES_CACHE_NAME_PROP_PROP = "Channel.Cache.GlobalResponse.JCache.Properties.CacheNameProp";

    public static final String CACHE_PER_PAGE_ENABLE_PROP = "Channel.Cache.PerPage.Enable";

    public static final String CACHE_PER_LANGUAGE_ENABLE_PROP = "Channel.Cache.PerLanguage.Enable";

    public static final String CACHE_PER_QUERY_ENABLE_PROP = "Channel.Cache.PerQuery.Enable";

    public static final String SESSION_STORAGE_ENABLE_PROP = "Channel.Cache.SessionStorage.Enable";

    protected final Cache globalResponseCache;

    public ChannelCache(final Channel channel, final ContentManager.ChannelPluginDefinition<?> definition) throws WWWeeePortal.Exception {
        channel.super(definition);
        globalResponseCache = ConfigManager.getCache(ChannelCache.class, "GlobalResponse", definition.getProperties(), GLOBAL_RESPONSE_JCACHE_PROPERTY_PROP, GLOBAL_RESPONSE_JCACHE_NAME_PROP, GLOBAL_RESPONSE_JCACHE_PROPERTIES_CACHE_NAME_PROP_PROP);
        return;
    }

    @Override
    protected void initInternal(final LogAnnotation.Message logMessage) throws WWWeeePortal.Exception {
        super.initInternal(logMessage);
        LogAnnotation.annotate(logMessage, "GlobalResponseCacheClass", globalResponseCache.getClass(), null, false);
        return;
    }

    protected boolean isCachePerPageEnabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(CACHE_PER_PAGE_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected boolean isCachePerLanguageEnabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(CACHE_PER_LANGUAGE_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected boolean isCachePerQueryEnabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(CACHE_PER_QUERY_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected boolean isSessionStorageEnabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(SESSION_STORAGE_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected static final String getEffectiveRequestMethod(final Page.Request pageRequest) {
        final String method = pageRequest.getRSRequest().getMethod();
        return method.equalsIgnoreCase("HEAD") ? "GET" : method;
    }

    protected String createCachedResponseClientAttributesKey(final Page.Request pageRequest, final String mode) throws WWWeeePortal.Exception {
        final List<String> qualifiers = new ArrayList<String>(5);
        qualifiers.add(getEffectiveRequestMethod(pageRequest));
        qualifiers.add(StringUtil.toString((isCachePerLanguageEnabled(pageRequest)) ? CollectionUtil.toString(pageRequest.getHttpHeaders().getAcceptableLanguages(), null, null, ",") : null, ""));
        qualifiers.add(mode);
        qualifiers.add(StringUtil.toString(pageRequest.getChannelLocalPath(getChannel()), ""));
        qualifiers.add(StringUtil.toString(((isCachePerQueryEnabled(pageRequest)) && (pageRequest.isMaximized(getChannel()))) ? ConversionUtil.invokeConverter(NetUtil.QUERY_PARAMS_URL_ENCODE_CONVERTER, new TreeMap<String, List<String>>(pageRequest.getUriInfo().getQueryParameters())) : null, ""));
        return createClientAttributesKey(pageRequest, "CachedResponse", isCachePerPageEnabled(pageRequest), qualifiers);
    }

    protected Channel.ViewResponse getCachedResponse(final Page.Request pageRequest, final String mode) throws WWWeeePortal.Exception {
        final String cachedResponseKey = createCachedResponseClientAttributesKey(pageRequest, mode);
        if (isSessionStorageEnabled(pageRequest)) {
            final Channel.ViewResponse sessionCachedResponse = (Channel.ViewResponse) pageRequest.getSessionAttributes().get(cachedResponseKey);
            if (sessionCachedResponse != null) {
                return sessionCachedResponse;
            }
        }
        return (Channel.ViewResponse) globalResponseCache.get(cachedResponseKey);
    }

    protected Channel.ViewResponse getCachedResponseIfValid(final Page.Request pageRequest, final String mode) throws WWWeeePortal.Exception {
        if (!getChannel().isCacheControlClientDirectivesDisabled(pageRequest)) {
            final CacheControl cacheControl = ConversionUtil.invokeConverter(RESTUtil.HTTP_HEADERS_CACHE_CONTROL_CONVERTER, pageRequest.getHttpHeaders());
            if (cacheControl != null) {
                if (cacheControl.isNoStore()) return null;
                if (cacheControl.isNoCache()) return null;
            }
        }
        final Channel.ViewResponse cachedResponse = getCachedResponse(pageRequest, mode);
        if (cachedResponse == null) return null;
        if ((cachedResponse.getCacheControl() != null) && (cachedResponse.getCacheControl().getMaxAge() >= 0) && (System.currentTimeMillis() > cachedResponse.getDate().getTime() + (1000L * cachedResponse.getCacheControl().getMaxAge()))) return null;
        if ((cachedResponse.getExpires() != null) && (System.currentTimeMillis() > cachedResponse.getExpires().getTime())) return null;
        return cachedResponse;
    }

    protected Channel.ViewResponse getCachedResponseOnException(final Page.Request pageRequest, final String mode, final WWWeeePortal.OperationalException wpoe) throws WWWeeePortal.Exception {
        if ((pageRequest.isMaximized(getChannel())) && (pageRequest.getEntity() != null)) return null;
        return getCachedResponse(pageRequest, mode);
    }

    protected boolean isResponseCacheable(final Page.Request pageRequest, final Channel.ViewResponse viewResponse) throws WWWeeePortal.Exception {
        if (!"GET".equalsIgnoreCase(getEffectiveRequestMethod(pageRequest))) return false;
        final CacheControl cacheControl = viewResponse.getCacheControl();
        if ((cacheControl != null) && ((cacheControl.isNoStore()) || (cacheControl.isNoCache()) || (cacheControl.getMaxAge() == 0))) return false;
        if ((cacheControl != null) && (cacheControl.isPrivate()) && (!isSessionStorageEnabled(pageRequest))) return false;
        return true;
    }

    protected Channel.ViewResponse cacheResponse(final Page.Request pageRequest, final String mode, final Channel.ViewResponse viewResponse) throws WWWeeePortal.Exception {
        if (!isResponseCacheable(pageRequest, viewResponse)) return null;
        final Document cachedDocument = DOMUtil.newDocument();
        final Element contentContainerElement = viewResponse.getContentContainerElement();
        final Document contentContainerDocument = DOMUtil.getDocument(contentContainerElement);
        final Element cachedContentContainerElement;
        synchronized (contentContainerDocument) {
            cachedContentContainerElement = (Element) cachedDocument.importNode(contentContainerElement, true);
        }
        cachedDocument.appendChild(cachedContentContainerElement);
        final Channel.ViewResponse cachedViewResponse = new Channel.ViewResponse(viewResponse.getDate(), cachedDocument, cachedContentContainerElement, cachedContentContainerElement);
        cachedViewResponse.setTitle(viewResponse.getTitle());
        final Iterable<Element> metaElements = viewResponse.getMetaElements();
        if (metaElements != null) {
            for (Element metaElement : metaElements) {
                cachedViewResponse.addMetaElement(metaElement);
            }
        }
        cachedViewResponse.setContentType(viewResponse.getContentType());
        cachedViewResponse.setLocale(viewResponse.getLocale());
        cachedViewResponse.setLastModified(viewResponse.getLastModified());
        cachedViewResponse.setExpires(viewResponse.getExpires());
        cachedViewResponse.setCacheControl(viewResponse.getCacheControl());
        cachedViewResponse.setEntityTag(viewResponse.getEntityTag());
        final String cachedResponseKey = createCachedResponseClientAttributesKey(pageRequest, mode);
        final CacheControl cacheControl = viewResponse.getCacheControl();
        if ((cacheControl != null) && (cacheControl.isPrivate())) {
            pageRequest.getSessionAttributes().put(cachedResponseKey, cachedViewResponse);
        } else {
            globalResponseCache.put(cachedResponseKey, cachedViewResponse);
        }
        return cachedViewResponse;
    }

    protected void respondFromCache(final Channel.ViewResponse cachedViewResponse, final Channel.ViewResponse viewResponse) throws WWWeeePortal.Exception {
        viewResponse.setContentType(cachedViewResponse.getContentType());
        viewResponse.setLocale(cachedViewResponse.getLocale());
        viewResponse.setLastModified(cachedViewResponse.getLastModified());
        viewResponse.setExpires(cachedViewResponse.getExpires());
        viewResponse.setCacheControl(cachedViewResponse.getCacheControl());
        viewResponse.setEntityTag(cachedViewResponse.getEntityTag());
        DOMUtil.copyChildren(cachedViewResponse.getContentContainerElement(), viewResponse.getContentContainerElement());
        viewResponse.setTitle(cachedViewResponse.getTitle());
        if (cachedViewResponse.getMetaElements() != null) {
            for (Element metaElement : cachedViewResponse.getMetaElements()) {
                boolean duplicate = false;
                for (Element responseMetaElement : CollectionUtil.mkNotNull(viewResponse.getMetaElements())) {
                    if (metaElement.isEqualNode(responseMetaElement)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) viewResponse.addMetaElement(metaElement);
            }
        }
        return;
    }

    @Override
    protected <T> T pluginValueHook(final Channel.PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest) throws WWWeeePortal.Exception {
        if (Channel.VIEW_RESPONSE_HOOK.equals(pluginHook)) {
            final Channel.ViewResponse viewResponse = (Channel.ViewResponse) context[0];
            final Channel.ViewResponse cachedViewResponse = getCachedResponseIfValid(pageRequest, Channel.VIEW_MODE);
            if (cachedViewResponse != null) {
                respondFromCache(cachedViewResponse, viewResponse);
                return pluginHook.getResultClass().cast(viewResponse);
            }
        }
        return null;
    }

    @Override
    protected <T> T pluginFilterHook(final Channel.PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest, final T data) throws WWWeeePortal.Exception {
        if (Channel.VIEW_RESPONSE_HOOK.equals(pluginHook)) {
            final Channel.ViewResponse viewResponse = Channel.VIEW_RESPONSE_HOOK.getResultClass().cast(data);
            cacheResponse(pageRequest, Channel.VIEW_MODE, viewResponse);
        } else if (Channel.VIEW_EXCEPTION_HOOK.equals(pluginHook)) {
            final Channel.ViewResponse viewResponse = (Channel.ViewResponse) context[0];
            final WWWeeePortal.OperationalException wpoe = Channel.VIEW_EXCEPTION_HOOK.getResultClass().cast(data);
            final Channel.ViewResponse cachedViewResponse = getCachedResponseOnException(pageRequest, Channel.VIEW_MODE, wpoe);
            if (cachedViewResponse != null) {
                respondFromCache(cachedViewResponse, viewResponse);
                return null;
            }
        }
        return data;
    }
}
