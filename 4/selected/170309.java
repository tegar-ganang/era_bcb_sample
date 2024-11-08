package org.wwweeeportal.portal.channelplugins;

import java.io.*;
import java.net.*;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.*;
import org.jasig.cas.client.util.*;
import org.jasig.cas.client.authentication.*;
import org.jasig.cas.client.validation.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;
import org.wwweeeportal.portal.channels.*;

/**
 * <p>
 * When a ProxyChannel request results in a redirect to the CAS Login server, run the request again with the inclusion
 * of a CAS proxy ticket.
 * </p>
 * 
 * <p>
 * Note that using {@link ProxyChannel#FOLLOW_REDIRECTS_ENABLE_PROP} will break this plugin, instead causing the CAS
 * login page to be displayed in the Channel. This plugin will automatically handle the redirect generated if the CAS
 * Validation Filter wrapping the proxied file has been set to redirectAfterValidation=true.
 * </p>
 */
public class ProxyChannelCASClient extends Channel.Plugin {

    public static final String CAS_SERVER_LOGIN_URL_PROP = "ProxyChannel.CASClient.CASServerLoginURL";

    protected final URL casServerLoginURL;

    public ProxyChannelCASClient(final Channel channel, final ContentManager.ChannelPluginDefinition<?> definition) throws WWWeeePortal.Exception {
        channel.super(definition);
        if (!(channel instanceof ProxyChannel)) {
            throw new ConfigManager.ConfigException(getClass().getSimpleName() + " only works with ProxyChannel", null);
        }
        casServerLoginURL = getConfigProp(CAS_SERVER_LOGIN_URL_PROP, null, RSProperties.RESULT_URL_CONVERTER, null, false, false);
        return;
    }

    protected ProxyChannel getProxyChannel() {
        return (ProxyChannel) getChannel();
    }

    protected boolean getReceivedCASLoginRedirectAttr(final Page.Request pageRequest) {
        return Boolean.TRUE.equals(pageRequest.getAttributes().get(createClientAttributesKey(pageRequest, "ReceivedCASLoginRedirect", true, null)));
    }

    protected void setReceivedCASLoginRedirectAttr(final Page.Request pageRequest) {
        pageRequest.getAttributes().put(createClientAttributesKey(pageRequest, "ReceivedCASLoginRedirect", true, null), Boolean.TRUE);
        return;
    }

    protected boolean getAddedCASProxyTicketAttr(final Page.Request pageRequest) {
        return Boolean.TRUE.equals(pageRequest.getAttributes().get(createClientAttributesKey(pageRequest, "AddedCASProxyTicket", true, null)));
    }

    protected void setAddedCASProxyTicketAttr(final Page.Request pageRequest) {
        pageRequest.getAttributes().put(createClientAttributesKey(pageRequest, "AddedCASProxyTicket", true, null), Boolean.TRUE);
        return;
    }

    /**
   * Is this {@link HttpResponse} a redirect to the CAS server, indicating a proxy ticket is needed?
   */
    protected boolean isCASLoginRedirect(final HttpResponse proxyResponse) throws WWWeeePortal.Exception {
        return (proxyResponse != null) && (proxyResponse.getStatusLine().getStatusCode() == 302) && (proxyResponse.containsHeader("Location")) && (proxyResponse.getFirstHeader("Location").getValue().startsWith(casServerLoginURL.toString()));
    }

    protected String getProxyTicket(final Page.Request pageRequest, final URL proxiedFileURL) throws WWWeeePortal.Exception {
        Assertion assertion = (Assertion) pageRequest.getAttributes().get(AbstractCasFilter.CONST_CAS_ASSERTION);
        if (assertion == null) assertion = (Assertion) pageRequest.getSessionAttributes().get(AbstractCasFilter.CONST_CAS_ASSERTION);
        if (assertion == null) return null;
        final AttributePrincipal principal = assertion.getPrincipal();
        if (principal == null) return null;
        return principal.getProxyTicketFor(proxiedFileURL.toString());
    }

    protected URL addProxyTicketParameter(final Page.Request pageRequest, final URL proxiedFileURL) throws WWWeeePortal.Exception {
        final String proxyTicket = getProxyTicket(pageRequest, proxiedFileURL);
        if (proxyTicket == null) {
            return proxiedFileURL;
        }
        final StringBuffer newProxiedFileURL = new StringBuffer(proxiedFileURL.toString());
        if (proxiedFileURL.getQuery() != null) {
            newProxiedFileURL.append('&');
        } else {
            newProxiedFileURL.append('?');
        }
        newProxiedFileURL.append("ticket=");
        newProxiedFileURL.append(proxyTicket);
        try {
            return new URL(newProxiedFileURL.toString());
        } catch (MalformedURLException mue) {
            throw new ConfigManager.ConfigException(mue);
        }
    }

    @Override
    protected <T> T pluginFilterHook(final Channel.PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest, final T data) throws WWWeeePortal.Exception {
        if (ProxyChannel.PROXIED_FILE_URL_HOOK.equals(pluginHook)) {
            if (getReceivedCASLoginRedirectAttr(pageRequest) && (!getAddedCASProxyTicketAttr(pageRequest))) {
                final URL oldProxiedFileURL = ProxyChannel.PROXIED_FILE_URL_HOOK.getResultClass().cast(data);
                final URL newProxiedFileURL = addProxyTicketParameter(pageRequest, oldProxiedFileURL);
                if (newProxiedFileURL != oldProxiedFileURL) {
                    setAddedCASProxyTicketAttr(pageRequest);
                    return pluginHook.getResultClass().cast(newProxiedFileURL);
                }
            }
        } else if (ProxyChannel.PROXY_REQUEST_INTERCEPTOR_HOOK.equals(pluginHook)) {
            if (getAddedCASProxyTicketAttr(pageRequest)) {
                final HttpUriRequest proxyRequest = (HttpUriRequest) ProxyChannel.PROXY_REQUEST_INTERCEPTOR_HOOK.getResultClass().cast(data);
                ((ClientParamsStack) proxyRequest.getParams()).getRequestParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
            }
        } else if (ProxyChannel.PROXY_RESPONSE_HOOK.equals(pluginHook)) {
            final HttpResponse proxyResponse = ProxyChannel.PROXY_RESPONSE_HOOK.getResultClass().cast(data);
            if ((isCASLoginRedirect(proxyResponse)) && (!getReceivedCASLoginRedirectAttr(pageRequest))) {
                setReceivedCASLoginRedirectAttr(pageRequest);
                try {
                    EntityUtils.consume(proxyResponse.getEntity());
                } catch (IOException ioe) {
                }
                return null;
            }
        }
        return data;
    }
}
