package org.wwweeeportal.portal.channelplugins;

import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.apache.http.client.methods.*;
import org.springframework.core.convert.converter.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.net.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;
import org.wwweeeportal.portal.channels.*;

/**
 * Modifies the request made from a {@link org.wwweeeportal.portal.channels.ProxyChannel} to it's proxied URL/document.
 */
public class ProxyChannelRequestAttributes extends Channel.Plugin {

    public static final String REFERER_USE_BASE_URI_PROP = "ProxyChannel.RequestAttributes.Referer.UseBaseURI";

    public static final String PARAMETER_ADD_BY_PATH_PROP = "ProxyChannel.RequestAttributes.Parameter.Add.Path.";

    protected static final Pattern PARAMETER_ADD_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(PARAMETER_ADD_BY_PATH_PROP) + ".*");

    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>> PARAMETER_ADD_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<String>, Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>>(RSProperties.RESULT_ENTRY_STRING_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<String>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<String>()));

    public static final String HEADER_ADD_BY_PATH_PROP = "ProxyChannel.RequestAttributes.Header.Add.Path.";

    public static final String HEADER_ADD_REPLACE_EXISTING_PROP = "ProxyChannel.RequestAttributes.Header.Add.ReplaceExisting";

    protected static final Pattern HEADER_ADD_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(HEADER_ADD_BY_PATH_PROP) + ".*");

    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>> HEADER_ADD_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<String>, Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>>(RSProperties.RESULT_ENTRY_STRING_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<String>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<String>()));

    public ProxyChannelRequestAttributes(final Channel channel, final ContentManager.ChannelPluginDefinition<?> definition) throws WWWeeePortal.Exception {
        channel.super(definition);
        if (!(channel instanceof ProxyChannel)) {
            throw new ConfigManager.ConfigException(getClass().getSimpleName() + " only works with ProxyChannel", null);
        }
        return;
    }

    protected ProxyChannel getProxyChannel() {
        return (ProxyChannel) getChannel();
    }

    protected boolean getSetRefererToBaseURI(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(REFERER_USE_BASE_URI_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected boolean getHeaderAddReplaceExisting(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(HEADER_ADD_REPLACE_EXISTING_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected Set<Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>> getParamAdditionPropsByPath(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return CollectionUtil.keySet(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), PARAMETER_ADD_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), PARAMETER_ADD_BY_PATH_CONVERTER, true, true), null, false, StringUtil.toString(pageRequest.getChannelLocalPath(getChannel()), null)));
    }

    protected Set<Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>> getHeaderAdditionPropsByPath(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return CollectionUtil.keySet(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), HEADER_ADD_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), HEADER_ADD_BY_PATH_CONVERTER, true, true), null, false, StringUtil.toString(pageRequest.getChannelLocalPath(getChannel()), null)));
    }

    protected Map<String, Object> getMetaProps(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        final Map<String, Object> metaProps = new HashMap<String, Object>();
        pageRequest.getMetaProps(metaProps);
        getPortal().getMetaProps(metaProps, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders());
        pageRequest.getPage().getMetaProps(pageRequest, metaProps);
        getChannel().getMetaProps(pageRequest, metaProps);
        return metaProps;
    }

    protected URL addRequestParameters(final Page.Request pageRequest, final URL proxiedFileURL) throws WWWeeePortal.Exception {
        final Set<Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>> paramAdditionPropsByPath = getParamAdditionPropsByPath(pageRequest);
        if (paramAdditionPropsByPath == null) {
            return null;
        }
        final Map<String, Object> metaProps = getMetaProps(pageRequest);
        final StringBuffer newProxiedFileURL = new StringBuffer(proxiedFileURL.toString());
        boolean ampNeeded = false;
        if (proxiedFileURL.getQuery() != null) {
            ampNeeded = true;
        } else {
            newProxiedFileURL.append('?');
        }
        for (final Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>> paramAdditionProp : paramAdditionPropsByPath) {
            final String parameterName = paramAdditionProp.getValue().getKey();
            final String metaPropName = paramAdditionProp.getKey().getValue();
            final Object metaPropValue = metaProps.get(metaPropName);
            if (ampNeeded) newProxiedFileURL.append('&');
            newProxiedFileURL.append(ConversionUtil.invokeConverter(NetUtil.URL_ENCODE_CONVERTER, parameterName));
            newProxiedFileURL.append('=');
            if (metaPropValue != null) {
                newProxiedFileURL.append(ConversionUtil.invokeConverter(NetUtil.URL_ENCODE_CONVERTER, StringUtil.toString(metaPropValue, null)));
            }
            ampNeeded = true;
        }
        try {
            return new URL(newProxiedFileURL.toString());
        } catch (MalformedURLException mue) {
            throw new ConfigManager.ConfigException(mue);
        }
    }

    protected void addRequestHeaders(final Page.Request pageRequest, final HttpUriRequest proxyRequest) throws WWWeeePortal.Exception {
        final Set<Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>>> headerAdditionPropsByPath = getHeaderAdditionPropsByPath(pageRequest);
        if (headerAdditionPropsByPath == null) {
            return;
        }
        final boolean replaceExistingHeaders = getHeaderAddReplaceExisting(pageRequest);
        final Map<String, Object> metaProps = getMetaProps(pageRequest);
        for (final Map.Entry<RSProperties.Entry<String>, Map.Entry<String, Pattern>> headerAdditionProp : headerAdditionPropsByPath) {
            final String headerName = headerAdditionProp.getValue().getKey();
            final String metaPropName = headerAdditionProp.getKey().getValue();
            final String metaPropValue = StringUtil.toString(metaProps.get(metaPropName), "");
            if (proxyRequest != null) {
                if (replaceExistingHeaders) {
                    proxyRequest.removeHeaders(headerName);
                    proxyRequest.addHeader(headerName, metaPropValue);
                } else {
                    proxyRequest.addHeader(headerName, metaPropValue);
                }
            }
        }
        return;
    }

    @Override
    protected <T> T pluginFilterHook(final Channel.PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest, final T data) throws WWWeeePortal.Exception {
        if (ProxyChannel.PROXIED_FILE_URL_HOOK.equals(pluginHook)) {
            URL proxiedFileURL = ProxyChannel.PROXIED_FILE_URL_HOOK.getResultClass().cast(data);
            proxiedFileURL = addRequestParameters(pageRequest, proxiedFileURL);
            if (proxiedFileURL != null) {
                return pluginHook.getResultClass().cast(proxiedFileURL);
            }
        } else if (ProxyChannel.PROXY_REQUEST_HOOK.equals(pluginHook)) {
            final HttpUriRequest proxyRequest = ProxyChannel.PROXY_REQUEST_HOOK.getResultClass().cast(data);
            addRequestHeaders(pageRequest, proxyRequest);
            if (getSetRefererToBaseURI(pageRequest)) {
                proxyRequest.setHeader("Referer", getProxyChannel().getProxiedBaseURI(pageRequest).toString());
            }
        }
        return data;
    }
}
