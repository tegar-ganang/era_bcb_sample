package org.wwweeeportal.portal.channels;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;
import javax.activation.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.springframework.core.convert.*;
import org.springframework.core.convert.converter.*;
import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.*;
import org.apache.http.conn.*;
import org.apache.http.impl.client.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.xml.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.xml.html.*;
import org.wwweeeportal.util.xml.sax.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.http.*;
import org.wwweeeportal.util.io.*;
import org.wwweeeportal.util.logging.*;
import org.wwweeeportal.util.net.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;

/**
 * <p>
 * A {@link Channel} which provides content proxied via an {@link HttpClient}.
 * </p>
 * 
 * <p>
 * {@linkplain #BASE_URI_PROP Provided} a {@link URL}, a <code>ProxyChannel</code> will provide access to a single
 * document or entire website at that location.
 * </p>
 * 
 * <p>
 * This channel is implemented in a completely <em>generic</em> fashion, and is capable of rendering <em>any</em> type
 * of XML content into a {@link Page}. As such, you will probably desire the enhanced functionality provided by the
 * {@linkplain org.wwweeeportal.portal.channelplugins.ProxyChannelHTMLSource HTML plugin}, in the likely case you will
 * be using the channel to proxy (X)HTML content.
 * </p>
 * 
 * <p>
 * Aside from actual retrieval of the content itself, a major function of this channel is to perform
 * {@linkplain #rewriteProxiedFileLink(Page.Request, URL, URI, boolean, boolean) link rewriting} within the proxied
 * documents.
 * </p>
 * 
 * <h3 id="configuration">Configuration</h3>
 * <p>
 * In addition to those inherited from the {@link Channel} class, the following {@linkplain ConfigManager configuration
 * properties} are supported by this class:
 * </p>
 * <ul>
 * <li>{@link #BASE_URI_PROP}</li>
 * <li>{@link #DEFAULT_PATH_PROP}</li>
 * <li>{@link #INLINE_CONTENT_DISABLE_PROP}</li>
 * <li>{@link #DEFAULT_PATH_RESTRICTION_ENABLE_PROP}</li>
 * <li>{@link #PARENT_FOLDERS_RESTRICTION_DISABLE_PROP}</li>
 * <li>{@link #LINK_REWRITING_HYPER_LINKS_TO_CHANNEL_DISABLE_PROP}</li>
 * <li>{@link #LINK_REWRITING_RESOURCE_LINKS_TO_CHANNEL_ENABLE_PROP}</li>
 * <li>{@link #CONNECT_TIMEOUT_PROP}</li>
 * <li>{@link #READ_TIMEOUT_PROP}</li>
 * <li>{@link #FOLLOW_REDIRECTS_ENABLE_PROP}</li>
 * </ul>
 */
public class ProxyChannel extends Channel {

    /**
   * <p>
   * The key to a <strong>required</strong> {@link RSProperties#RESULT_URI_CONVERTER URI} property defining the
   * {@linkplain #getProxiedBaseURI(Page.Request) base URI} of the document or site to be proxied.
   * </p>
   * 
   * <p>
   * If you wish to create a channel which will simply display a single document, proxied from a specified URL, then
   * this property can just contain the {@link URL} to that document (this value will essentially include the
   * {@linkplain #DEFAULT_PATH_PROP default path}, which is then not required).
   * </p>
   * 
   * <p>
   * If you wish to create a channel which will proxy an entire website, where users can navigate between the pages of
   * that site within the channel {@linkplain Channel#VIEW_MODE view}, then this property should contain the {@link URL}
   * of the top-most root <em>folder</em> which contains the documents for the entire site, and a
   * {@linkplain #DEFAULT_PATH_PROP default path} should then also be specified.
   * </p>
   * 
   * <p>
   * By default, this channel will proxy all documents within the leaf-most <em>folder</em> specified by this property,
   * though you can {@linkplain #DEFAULT_PATH_RESTRICTION_ENABLE_PROP restrict} it to just the document specified by
   * this URL (including the {@linkplain #DEFAULT_PATH_PROP default path}). Though discouraged, you may also
   * {@linkplain #PARENT_FOLDERS_RESTRICTION_DISABLE_PROP enable} proxying of documents on the server which are outside
   * of this folder.
   * </p>
   * 
   * <p>
   * Note that if you specify a value for this property which is not {@linkplain URI#isAbsolute() absolute}, it will be
   * {@linkplain ConfigManager#getContextResourceLocalHostURI(UriInfo, String, Map, String, boolean) resolved} within
   * the local portal context (relative to the root of the context). This is a very useful feature for creating a
   * self-contained portal context which includes all the documents it proxies.
   * </p>
   * 
   * @see #getProxiedBaseURI(Page.Request)
   * @see #DEFAULT_PATH_PROP
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String BASE_URI_PROP = "ProxyChannel.BaseURI";

    /**
   * <p>
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining a <strong>relative</strong> path
   * under the proxied {@linkplain #getProxiedBaseURI(Page.Request) base URI} to the document which should be displayed
   * within this channel by default (when {@linkplain Channel#VIEW_MODE viewed} as part of a
   * {@linkplain Page#doViewGetRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map) request} for the
   * {@link Page}, or when no {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) local path}
   * is specified).
   * </p>
   * 
   * <p>
   * This property is not required if the {@linkplain #getProxiedBaseURI(Page.Request) base URI} points directly to a
   * document (as opposed to a folder), as then that value will be interpreted as though it includes this path.
   * </p>
   * 
   * <p>
   * Note that this path is only really used during {@linkplain Channel#VIEW_MODE view mode} requests, except when the
   * {@linkplain #isDefaultPathRestrictionEnabled(Page.Request) default path} restriction is
   * {@linkplain #DEFAULT_PATH_RESTRICTION_ENABLE_PROP enabled}, in which case the
   * {@linkplain #getProxiedFileLocalURI(Page.Request, String, boolean) proxied file URI} will be validated against it.
   * </p>
   * 
   * @see #BASE_URI_PROP
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String DEFAULT_PATH_PROP = "ProxyChannel.DefaultPath";

    /**
   * <p>
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property indicating inline content should be
   * {@linkplain #isInlineContentDisabled(Page.Request) disabled}.
   * </p>
   * 
   * <p>
   * Instead of rendering the source content directly as part of the markup for the {@link Page}, the
   * {@linkplain Channel#VIEW_MODE view mode} for this channel will instead output an <code>&lt;object&gt;</code> tag
   * containing a reference to the content (works similarly to the non-standard <code>&lt;iframe&gt;</code> within
   * modern browsers).
   * </p>
   * 
   * <p>
   * The primary motivation behind this feature is the fact that (despite the best efforts of this implementation), it's
   * <em>impossible</em> for a portal to properly aggregate multiple channels containing arbitrarily complex webapps
   * from a dynamic list of third party sources onto a <em>single</em> page without breaking <em>something</em> (from
   * attempted AJAX requests to the source site having cross-site scripting problems, to certificate related problems,
   * etc, etc, etc). By using an <code>&lt;object&gt;</code> tag for the channel content, the majority of the problems
   * with such complex sites can be avoided, because the site will still be rendered directly from it's source location
   * by the browser, and the site will still <em>appear</em> as part of the portal page, and you can still use all the
   * features of the portal to organize content and control which clients see what.
   * </p>
   * 
   * <p>
   * It's also worth noting that this same behavior will be utilized to
   * {@linkplain #renderResourceReferenceView(Page.Request, Channel.ViewResponse, URL, MimeType) render} any non-markup
   * content encountered during {@linkplain Channel#VIEW_MODE view mode}.
   * </p>
   * 
   * @see #isInlineContentDisabled(Page.Request)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String INLINE_CONTENT_DISABLE_PROP = "ProxyChannel.InlineContent.Disable";

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property indicating that this channel should
   * only be {@linkplain #isDefaultPathRestrictionEnabled(Page.Request) enabled} to proxy the single document located at
   * the {@linkplain #DEFAULT_PATH_PROP default path} location, and <em>not</em> anything else within the specified
   * {@linkplain #getProxiedBaseURI(Page.Request) base URI} folder (as allowed by default).
   * 
   * @see #isDefaultPathRestrictionEnabled(Page.Request)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String DEFAULT_PATH_RESTRICTION_ENABLE_PROP = "ProxyChannel.DefaultPathRestriction.Enable";

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property indicating the default restriction
   * against the proxying of documents from the source server which reside outside the
   * {@linkplain #getProxiedBaseURI(Page.Request) base URI} should be
   * {@linkplain #isParentFoldersRestrictionDisabled(Page.Request) disabled}. This option should never really need to be
   * used, as the base URI should generally encompass all files required by a site.
   * 
   * @see #isParentFoldersRestrictionDisabled(Page.Request)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String PARENT_FOLDERS_RESTRICTION_DISABLE_PROP = "ProxyChannel.ParentFoldersRestriction.Disable";

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property which indicates that, during
   * {@linkplain #rewriteProxiedFileLink(Page.Request, URL, URI, boolean, boolean) link rewriting}, that any
   * <em>hyperlink</em> between this and another proxied site document should no longer be rewritten so that the client
   * browser {@linkplain Channel#VIEW_MODE views} the linked document within this portal channel (
   * {@linkplain #isLinkRewritingHyperLinksToChannelDisabled(Page.Request) default behaviour}), but rather so the client
   * is directed outside of the portal, to display the document directly from it's
   * {@linkplain #getProxiedBaseURI(Page.Request) origin/source location}.
   * 
   * @see #isLinkRewritingHyperLinksToChannelDisabled(Page.Request)
   * @see #rewriteProxiedFileLink(Page.Request, URL, URI, boolean, boolean)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String LINK_REWRITING_HYPER_LINKS_TO_CHANNEL_DISABLE_PROP = "ProxyChannel.LinkRewriting.HyperLinksToChannel.Disable";

    /**
   * <p>
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property which indicates that, during
   * {@linkplain #rewriteProxiedFileLink(Page.Request, URL, URI, boolean, boolean) link rewriting}, any relative link
   * from within the proxied document to an <em>external resource</em> should no longer be rewritten into an
   * {@linkplain URI#isAbsolute() absolute} link pointing directly back to the
   * {@linkplain #getProxiedBaseURI(Page.Request) origin/source location} (
   * {@linkplain #isLinkRewritingResourceLinksToChannelEnabled(Page.Request) default behaviour}), but rather so that the
   * client browser retrieves it as a {@linkplain Channel#RESOURCE_MODE resource} proxied via this channel.
   * </p>
   * 
   * <p>
   * This option can be useful if there is a firewall preventing clients from direct access to an internal server
   * hosting the source resources, but should be exercised with care, as WWWeee-Portal is designed primarily for
   * aggregation of markup, and doesn't provide the most sophisticated HTTP proxy implementation (ie, conditional
   * requests <em>are</em> supported, but limited byte-range type requests against large resources are <em>not</em>).
   * Note that this option may be used to force resource links to be rewritten pointing back through the portal, and
   * then the container configured to intercept those requests and provide a more sophisticated proxy implementation
   * (ie, Apache <code>mod_proxy</code> and <code>mod_cache</code>) if required.
   * </p>
   * 
   * @see #isLinkRewritingResourceLinksToChannelEnabled(Page.Request)
   * @see #rewriteProxiedFileLink(Page.Request, URL, URI, boolean, boolean)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String LINK_REWRITING_RESOURCE_LINKS_TO_CHANNEL_ENABLE_PROP = "ProxyChannel.LinkRewriting.ResourceLinksToChannel.Enable";

    /**
   * The key to an {@link RSProperties#RESULT_INTEGER_CONVERTER Integer} property (in milliseconds)
   * {@linkplain #getConnectTimeout(Page.Request) used} to {@linkplain HttpParams#setIntParameter(String, int) set} the
   * {@linkplain CoreConnectionPNames#CONNECTION_TIMEOUT connection timeout} {@linkplain HttpClient#getParams()
   * parameter} on any {@link HttpClient} created to proxy documents for this channel. If this property is not specified
   * then the {@linkplain #DEFAULT_CONNECT_TIMEOUT_MS default} will be used.
   * 
   * @see CoreConnectionPNames#CONNECTION_TIMEOUT
   * @see #DEFAULT_CONNECT_TIMEOUT_MS
   * @see #getConnectTimeout(Page.Request)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String CONNECT_TIMEOUT_PROP = "ProxyChannel.ConnectTimeout";

    /**
   * Unless an explicit value is {@linkplain #CONNECT_TIMEOUT_PROP specified}, this value will be
   * {@linkplain #getConnectTimeout(Page.Request) used} as the default value (in milliseconds) to
   * {@linkplain HttpParams#setIntParameter(String, int) set} the {@linkplain CoreConnectionPNames#CONNECTION_TIMEOUT
   * connection timeout} {@linkplain HttpClient#getParams() parameter} on any {@link HttpClient} created to proxy
   * documents for this channel.
   * 
   * @see #CONNECT_TIMEOUT_PROP
   * @see #getConnectTimeout(Page.Request)
   */
    public static final Integer DEFAULT_CONNECT_TIMEOUT_MS = Integer.valueOf(30000);

    /**
   * The key to an {@link RSProperties#RESULT_INTEGER_CONVERTER Integer} property (in milliseconds)
   * {@linkplain #getReadTimeout(Page.Request) used} to {@linkplain HttpParams#setIntParameter(String, int) set} the
   * {@linkplain CoreConnectionPNames#SO_TIMEOUT socket timeout} {@linkplain HttpClient#getParams() parameter} on any
   * {@link HttpClient} created to proxy documents for this channel. If this property is not specified then the
   * {@linkplain #DEFAULT_READ_TIMEOUT_MS default} will be used.
   * 
   * @see CoreConnectionPNames#SO_TIMEOUT
   * @see #DEFAULT_READ_TIMEOUT_MS
   * @see #getReadTimeout(Page.Request)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String READ_TIMEOUT_PROP = "ProxyChannel.ReadTimeout";

    /**
   * Unless an explicit value is {@linkplain #READ_TIMEOUT_PROP specified}, this value will be
   * {@linkplain #getReadTimeout(Page.Request) used} as the default value (in milliseconds) to
   * {@linkplain HttpParams#setIntParameter(String, int) set} the {@linkplain CoreConnectionPNames#SO_TIMEOUT socket
   * timeout} {@linkplain HttpClient#getParams() parameter} on any {@link HttpClient} created to proxy documents for
   * this channel.
   * 
   * @see #READ_TIMEOUT_PROP
   * @see #getReadTimeout(Page.Request)
   */
    public static final Integer DEFAULT_READ_TIMEOUT_MS = Integer.valueOf(30000);

    /**
   * <p>
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property
   * {@linkplain #isFollowRedirectsEnabled(Page.Request) used} to
   * {@linkplain HttpParams#setBooleanParameter(String, boolean) set} the automatic
   * {@linkplain ClientPNames#HANDLE_REDIRECTS redirect handling} {@linkplain HttpClient#getParams() parameter} on any
   * {@link HttpClient} created to proxy documents for this channel.
   * </p>
   * 
   * <p>
   * By default, any redirect URL returned by the proxied server will be
   * {@linkplain #rewriteProxiedFileLink(Page.Request, URL, URI, boolean, boolean) rewritten} and forwarded to the
   * client browser, as though it were returned as a link within a proxied document (redirects in
   * {@linkplain Channel#VIEW_MODE view mode} are treated as <em>hyperlinks</em>, redirects in
   * {@linkplain Channel#RESOURCE_MODE resource mode} as <em>external resource</em> links). It is important to note that
   * forwarding a redirect to the client is only possible within {@linkplain Channel#RESOURCE_MODE resource mode}, or
   * when the channel is being {@linkplain Channel#VIEW_MODE viewed} while
   * {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximized}, so any redirect returned by the
   * {@linkplain #DEFAULT_PATH_PROP default path} will generally result in a
   * {@link org.wwweeeportal.portal.ConfigManager.ConfigException ConfigException}.
   * </p>
   * 
   * <p>
   * Enabling this property will cause <em>this channel</em> to follow any redirections while loading proxied documents,
   * instead of rewriting and forwarding them to the client browser, and also allow for successful redirection by the
   * {@linkplain #DEFAULT_PATH_PROP default path}.
   * </p>
   * 
   * <p>
   * This behavior is disabled by default as a security precaution, as when enabled, there will <strong>no longer be
   * <em>any</em> restrictions</strong> (ie, {@linkplain #PARENT_FOLDERS_RESTRICTION_DISABLE_PROP parent folders} or
   * {@linkplain #DEFAULT_PATH_RESTRICTION_ENABLE_PROP default path}) imposed on the redirection target URL. You should
   * ensure you trust the application at the {@linkplain #BASE_URI_PROP base URL}, otherwise it could cause the portal
   * to load data from <em>anywhere</em> and return it to clients from within your domain, mitigating a client browser's
   * <a href="http://en.wikipedia.org/wiki/Same_origin_policy">same-origin policy</a> and possibly enabling <a
   * href="http://en.wikipedia.org/wiki/Cross-site_request_forgery">CSRF</a> type attacks against your users.
   * </p>
   * 
   * @see ClientPNames#HANDLE_REDIRECTS
   * @see #isFollowRedirectsEnabled(Page.Request)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String FOLLOW_REDIRECTS_ENABLE_PROP = "ProxyChannel.FollowRedirects.Enable";

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@linkplain #getProxiedBaseURI(Page.Request) proxied base URI} or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the
   * {@linkplain #BASE_URI_PROP provided} value.
   * 
   * @see #getProxiedBaseURI(Page.Request)
   * @see #BASE_URI_PROP
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, URI> PROXIED_BASE_URI_HOOK = new PluginHook<ProxyChannel, URI>(ProxyChannel.class, 1, URI.class, null);

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@linkplain #getProxiedFilePathDefault(Page.Request) proxied file default path} or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the
   * {@linkplain #DEFAULT_PATH_PROP provided} value.
   * 
   * @see #getProxiedFilePathDefault(Page.Request)
   * @see #DEFAULT_PATH_PROP
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, URI> PROXIED_FILE_PATH_DEFAULT_HOOK = new PluginHook<ProxyChannel, URI>(ProxyChannel.class, 2, URI.class, null);

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@linkplain #getProxiedFileLocalURI(Page.Request, String, boolean) proxied file local URI} or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the calculated value.
   * 
   * @see #getProxiedFileLocalURI(Page.Request, String, boolean)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, URI> PROXIED_FILE_LOCAL_URI_HOOK = new PluginHook<ProxyChannel, URI>(ProxyChannel.class, 3, URI.class, new Class<?>[] { URI.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@linkplain #getProxiedFileURL(Page.Request, String, boolean) proxied file URL} or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the calculated value.
   * 
   * @see #getProxiedFileURL(Page.Request, String, boolean)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, URL> PROXIED_FILE_URL_HOOK = new PluginHook<ProxyChannel, URL>(ProxyChannel.class, 4, URL.class, new Class<?>[] { String.class, Boolean.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@link #createProxyClientManager(Page.Request) ProxyClientManager} or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the created one.
   * 
   * @see #createProxyClientManager(Page.Request)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, ClientConnectionManager> PROXY_CLIENT_MANAGER_HOOK = new PluginHook<ProxyChannel, ClientConnectionManager>(ProxyChannel.class, 5, ClientConnectionManager.class, null);

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@link #createProxyClientParams(Page.Request, ClientConnectionManager) HttpParams} to the proxy client, or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the created ones.
   * 
   * @see #createProxyClientParams(Page.Request, ClientConnectionManager)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, HttpParams> PROXY_CLIENT_PARAMS_HOOK = new PluginHook<ProxyChannel, HttpParams>(ProxyChannel.class, 6, HttpParams.class, new Class<?>[] { ClientConnectionManager.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@link #createProxyClient(Page.Request) HttpClient} to {@linkplain #doProxyRequest(Page.Request, String) proxy}
   * content, or to {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the
   * created one.
   * 
   * @see #createProxyClient(Page.Request)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, HttpClient> PROXY_CLIENT_HOOK = new PluginHook<ProxyChannel, HttpClient>(ProxyChannel.class, 7, HttpClient.class, new Class<?>[] { ClientConnectionManager.class, HttpParams.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@link #createProxyRequest(Page.Request, String, HttpClient) HttpUriRequest} to
   * {@linkplain #doProxyRequest(Page.Request, String) proxy} content, or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the configured one.
   * 
   * @see #createProxyRequest(Page.Request, String, HttpClient)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, HttpUriRequest> PROXY_REQUEST_HOOK = new PluginHook<ProxyChannel, HttpUriRequest>(ProxyChannel.class, 8, HttpUriRequest.class, new Class<?>[] { String.class, URL.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@link #createProxyRequestObject(Page.Request, String, URL) HttpUriRequest} to
   * {@linkplain #doProxyRequest(Page.Request, String) proxy} content, or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the created one.
   * 
   * @see #createProxyRequestObject(Page.Request, String, URL)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, HttpUriRequest> PROXY_REQUEST_OBJ_HOOK = new PluginHook<ProxyChannel, HttpUriRequest>(ProxyChannel.class, 9, HttpUriRequest.class, new Class<?>[] { String.class, URL.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the {@link HttpRequest}
   * sent by the {@linkplain #createProxyClient(Page.Request) proxy client}.
   * 
   * @see #createProxyClient(Page.Request)
   * @see ProxyRequestInterceptor
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, HttpRequest> PROXY_REQUEST_INTERCEPTOR_HOOK = new PluginHook<ProxyChannel, HttpRequest>(ProxyChannel.class, 10, HttpRequest.class, new Class<?>[] { HttpClient.class, HttpContext.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the {@link HttpResponse}
   * received by the {@linkplain #createProxyClient(Page.Request) proxy client}.
   * 
   * @see #createProxyClient(Page.Request)
   * @see ProxyResponseInterceptor
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, HttpResponse> PROXY_RESPONSE_INTERCEPTOR_HOOK = new PluginHook<ProxyChannel, HttpResponse>(ProxyChannel.class, 11, HttpResponse.class, new Class<?>[] { HttpClient.class, HttpContext.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) perform} the
   * {@linkplain #doProxyRequest(Page.Request, String) proxy request} on it's own, or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the response.
   * 
   * @see #doProxyRequest(Page.Request, String)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, HttpResponse> PROXY_RESPONSE_HOOK = new PluginHook<ProxyChannel, HttpResponse>(ProxyChannel.class, 12, HttpResponse.class, new Class<?>[] { String.class, HttpClient.class, HttpContext.class, HttpUriRequest.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@linkplain #getProxyResponseHeader(Page.Request, HttpResponse, String, Converter) response header}, as if it had
   * {@linkplain HttpResponse#getHeaders(String) come} from the {@link #doProxyRequest(Page.Request, String) proxied}
   * response, or to {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the
   * returned value.
   * 
   * @see #getProxyResponseHeader(Page.Request, HttpResponse, String, Converter)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, Header> PROXY_RESPONSE_HEADER_HOOK = new PluginHook<ProxyChannel, Header>(ProxyChannel.class, 13, Header.class, new Class<?>[] { HttpResponse.class, String.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own {@link Boolean} value,
   * to {@linkplain #isRenderedUsingXMLView(Page.Request, HttpResponse, MimeType) indicate} the
   * {@linkplain #doProxyRequest(Page.Request, String) proxied} response should be
   * {@linkplain #renderXMLView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType) rendered} as XML, or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the calculated value.
   * 
   * @see #isRenderedUsingXMLView(Page.Request, HttpResponse, MimeType)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, Boolean> IS_RENDERED_USING_XML_VIEW_HOOK = new PluginHook<ProxyChannel, Boolean>(ProxyChannel.class, 14, Boolean.class, new Class<?>[] { HttpResponse.class, MimeType.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@link #createProxiedDocumentInputSource(Page.Request, HttpResponse, URL, MimeType) TypedInputSource}, from which
   * to {@linkplain MarkupManager#parseXMLDocument(InputSource, DefaultHandler2, boolean) parse} the
   * {@linkplain #doProxyRequest(Page.Request, String) proxied} content being
   * {@linkplain #renderXMLView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType) rendered}, or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the configured one.
   * 
   * @see #createProxiedDocumentInputSource(Page.Request, HttpResponse, URL, MimeType)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, TypedInputSource> PROXIED_DOC_INPUT_SOURCE_HOOK = new PluginHook<ProxyChannel, TypedInputSource>(ProxyChannel.class, 15, TypedInputSource.class, new Class<?>[] { HttpResponse.class, URL.class, MimeType.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own
   * {@link #createProxiedDocumentContentHandler(Page.Request, Channel.ViewResponse, URL, TypedInputSource)
   * DefaultHandler2}, to receive {@linkplain MarkupManager#parseXMLDocument(InputSource, DefaultHandler2, boolean)
   * parsing} events from the {@linkplain #doProxyRequest(Page.Request, String) proxied} content being
   * {@linkplain #renderXMLView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType) rendered}, or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the created one.
   * 
   * @see #createProxiedDocumentContentHandler(Page.Request, Channel.ViewResponse, URL, TypedInputSource)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, DefaultHandler2> PROXIED_DOC_CONTENT_HANDLER_HOOK = new PluginHook<ProxyChannel, DefaultHandler2>(ProxyChannel.class, 16, DefaultHandler2.class, new Class<?>[] { ViewResponse.class, URL.class, TypedInputSource.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own {@link Boolean} value,
   * indicating the {@linkplain #doProxyRequest(Page.Request, String) proxied}
   * {@linkplain #createProxiedDocumentInputSource(Page.Request, HttpResponse, URL, MimeType) input} should
   * <strong>not</strong> be {@linkplain MarkupManager#parseXMLDocument(InputSource, DefaultHandler2, boolean) parsed}
   * into the
   * {@linkplain #createProxiedDocumentContentHandler(Page.Request, Channel.ViewResponse, URL, TypedInputSource) content
   * handler} during {@linkplain #renderXMLView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType)
   * rendering} (ie, parsing was already handled by the plugin), or to
   * {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the calculated value.
   * 
   * @see #renderXMLView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, Boolean> PARSE_XML_HOOK = new PluginHook<ProxyChannel, Boolean>(ProxyChannel.class, 17, Boolean.class, new Class<?>[] { ViewResponse.class, TypedInputSource.class, DefaultHandler2.class });

    /**
   * A {@linkplain org.wwweeeportal.portal.Channel.PluginHook hook} which allows a
   * {@link org.wwweeeportal.portal.Channel.Plugin Plugin} to either
   * {@linkplain #pluginValueHook(Channel.PluginHook, Object[], Page.Request) provide} it's own {@link Boolean} value,
   * to {@linkplain #isRenderedUsingTextView(Page.Request, HttpResponse, MimeType) indicate} the
   * {@linkplain #doProxyRequest(Page.Request, String) proxied} response should be
   * {@linkplain #renderTextView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType) rendered} as text, or
   * to {@linkplain #pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the calculated value.
   * 
   * @see #isRenderedUsingTextView(Page.Request, HttpResponse, MimeType)
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<ProxyChannel, Boolean> IS_RENDERED_USING_TEXT_VIEW_HOOK = new PluginHook<ProxyChannel, Boolean>(ProxyChannel.class, 18, Boolean.class, new Class<?>[] { HttpResponse.class, MimeType.class });

    /**
   * A {@link Converter} which will {@linkplain RESTUtil#STRING_MEDIA_TYPE_CONVERTER parse} a {@link MediaType} from a
   * {@link Header}'s {@linkplain HTTPUtil#HEADER_VALUE_CONVERTER value}.
   * 
   * @see HTTPUtil#HEADER_VALUE_CONVERTER
   * @see RESTUtil#STRING_MEDIA_TYPE_CONVERTER
   */
    public static final Converter<Header, MediaType> HEADER_MEDIA_TYPE_CONVERTER = new ConverterChain<Header, String, MediaType>(HTTPUtil.HEADER_VALUE_CONVERTER, RESTUtil.STRING_MEDIA_TYPE_CONVERTER);

    /**
   * A {@link Converter} which will {@linkplain RESTUtil#STRING_ENTITY_TAG_CONVERTER parse} an {@link EntityTag} from a
   * {@link Header}'s {@linkplain HTTPUtil#HEADER_VALUE_CONVERTER value}.
   * 
   * @see HTTPUtil#HEADER_VALUE_CONVERTER
   * @see RESTUtil#STRING_ENTITY_TAG_CONVERTER
   */
    public static final Converter<Header, EntityTag> HEADER_ENTITY_TAG_CONVERTER = new ConverterChain<Header, String, EntityTag>(HTTPUtil.HEADER_VALUE_CONVERTER, RESTUtil.STRING_ENTITY_TAG_CONVERTER);

    /**
   * A {@link Converter} which will {@linkplain RESTUtil#STRING_CACHE_CONTROL_CONVERTER parse} a {@link CacheControl}
   * from a {@link Header}'s {@linkplain HTTPUtil#HEADER_VALUE_CONVERTER value}.
   * 
   * @see HTTPUtil#HEADER_VALUE_CONVERTER
   * @see RESTUtil#STRING_CACHE_CONTROL_CONVERTER
   */
    public static final Converter<Header, CacheControl> HEADER_CACHE_CONTROL_CONVERTER = new ConverterChain<Header, String, CacheControl>(HTTPUtil.HEADER_VALUE_CONVERTER, RESTUtil.STRING_CACHE_CONTROL_CONVERTER);

    /**
   * A constant used during
   * {@link #getProxyRequestUserAgentHeader(Page.Request, String, HttpClient, URL, HttpUriRequest) construction} of the
   * value for the &quot;User-Agent&quot; header to be {@link HttpUriRequest#setHeader(String, String) set} on the
   * {@link HttpUriRequest} being used to {@linkplain #createProxyRequest(Page.Request, String, HttpClient) proxy}
   * content for this channel.
   * 
   * @see #getProxyRequestUserAgentHeader(Page.Request, String, HttpClient, URL, HttpUriRequest)
   */
    public static final String USER_AGENT_HEADER = "WWWeeePortal/1.0";

    /**
   * The {@link MimeType} Object for the <code>"application/*"</code> mime type.
   * 
   * @see #getProxyRequestAcceptHeader(Page.Request, String, HttpClient, URL, HttpUriRequest)
   */
    protected static final MimeType APPLICATION_STAR_MIME_TYPE = IOUtil.newMimeType("application", "*");

    /**
   * The {@link MimeType} Object for the <code>"text/*"</code> mime type.
   * 
   * @see #getProxyRequestAcceptHeader(Page.Request, String, HttpClient, URL, HttpUriRequest)
   */
    protected static final MimeType TEXT_STAR_MIME_TYPE = IOUtil.newMimeType("text", "*");

    /**
   * Construct a new <code>ProxyChannel</code> instance.
   */
    public ProxyChannel(final WWWeeePortal portal, final ContentManager.ChannelDefinition<?, ? extends ProxyChannel> definition, final Page page) throws WWWeeePortal.Exception {
        super(portal, definition, page);
        return;
    }

    /**
   * Is inline content {@linkplain #INLINE_CONTENT_DISABLE_PROP disabled} for this channel?
   * 
   * @see #INLINE_CONTENT_DISABLE_PROP
   */
    public boolean isInlineContentDisabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(INLINE_CONTENT_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    /**
   * Is the default-path restriction {@linkplain #DEFAULT_PATH_RESTRICTION_ENABLE_PROP enabled} for this channel?
   * 
   * @see #DEFAULT_PATH_RESTRICTION_ENABLE_PROP
   */
    public boolean isDefaultPathRestrictionEnabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(DEFAULT_PATH_RESTRICTION_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    /**
   * Is the parent-folders restriction {@linkplain #PARENT_FOLDERS_RESTRICTION_DISABLE_PROP disabled} for this channel?
   * 
   * @see #PARENT_FOLDERS_RESTRICTION_DISABLE_PROP
   */
    public boolean isParentFoldersRestrictionDisabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(PARENT_FOLDERS_RESTRICTION_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    /**
   * Is the rewriting of hyperlinks within proxied documents back to this channel
   * {@linkplain #LINK_REWRITING_HYPER_LINKS_TO_CHANNEL_DISABLE_PROP disabled}?
   * 
   * @see #LINK_REWRITING_HYPER_LINKS_TO_CHANNEL_DISABLE_PROP
   */
    public boolean isLinkRewritingHyperLinksToChannelDisabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(LINK_REWRITING_HYPER_LINKS_TO_CHANNEL_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    /**
   * Is the rewriting of external resource links within proxied documents back to this channel
   * {@linkplain #LINK_REWRITING_RESOURCE_LINKS_TO_CHANNEL_ENABLE_PROP enabled}?
   * 
   * @see #LINK_REWRITING_RESOURCE_LINKS_TO_CHANNEL_ENABLE_PROP
   */
    public boolean isLinkRewritingResourceLinksToChannelEnabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(LINK_REWRITING_RESOURCE_LINKS_TO_CHANNEL_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    /**
   * Get the value (in milliseconds) used to {@linkplain HttpParams#setIntParameter(String, int) set} the
   * {@linkplain CoreConnectionPNames#CONNECTION_TIMEOUT connection timeout} {@linkplain HttpClient#getParams()
   * parameter} on any {@link HttpClient} created to proxy documents for this channel. If an explicit value is not
   * {@linkplain #CONNECT_TIMEOUT_PROP specified} then the {@linkplain #DEFAULT_CONNECT_TIMEOUT_MS default} will be
   * used.
   * 
   * @see CoreConnectionPNames#CONNECTION_TIMEOUT
   * @see #CONNECT_TIMEOUT_PROP
   * @see #DEFAULT_CONNECT_TIMEOUT_MS
   */
    public int getConnectTimeout(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(CONNECT_TIMEOUT_PROP, pageRequest, RSProperties.RESULT_INTEGER_CONVERTER, DEFAULT_CONNECT_TIMEOUT_MS, false, false).intValue();
    }

    /**
   * Get the value (in milliseconds) used to {@linkplain HttpParams#setIntParameter(String, int) set} the
   * {@linkplain CoreConnectionPNames#SO_TIMEOUT socket timeout} {@linkplain HttpClient#getParams() parameter} on any
   * {@link HttpClient} created to proxy documents for this channel. If an explicit value is not
   * {@linkplain #READ_TIMEOUT_PROP specified} then the {@linkplain #DEFAULT_READ_TIMEOUT_MS default} will be used.
   * 
   * @see CoreConnectionPNames#SO_TIMEOUT
   * @see #READ_TIMEOUT_PROP
   * @see #DEFAULT_READ_TIMEOUT_MS
   */
    public int getReadTimeout(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(READ_TIMEOUT_PROP, pageRequest, RSProperties.RESULT_INTEGER_CONVERTER, DEFAULT_READ_TIMEOUT_MS, false, false).intValue();
    }

    /**
   * Is the automatic handling of redirects by this channel's proxy client {@linkplain #FOLLOW_REDIRECTS_ENABLE_PROP
   * enabled}?
   * 
   * @see #FOLLOW_REDIRECTS_ENABLE_PROP
   */
    public boolean isFollowRedirectsEnabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(FOLLOW_REDIRECTS_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    /**
   * <p>
   * Is the supplied {@linkplain File#getPath() path} a relative path, which, if
   * {@linkplain java.net.URI#resolve(String) resolved} against some other path/folder, would point to a location
   * <em>within</em> that path's folder.
   * </p>
   * 
   * <ul>
   * <li><code>"/hello/"</code> returns <code>false</code>.</li>
   * <li><code>"./"</code> returns <code>true</code>.</li>
   * <li><code>"./hello.txt"</code> returns <code>true</code>.</li>
   * <li><code>"hello/"</code> returns <code>true</code>.</li>
   * <li><code>"hello/../"</code> returns <code>true</code>.</li>
   * <li><code>"hello/world/../"</code> returns <code>true</code>.</li>
   * <li><code>"hello/world/../../"</code> returns <code>true</code>.</li>
   * <li><code>"hello/world/../.././"</code> returns <code>true</code>.</li>
   * <li><code>"hello/world/../../../"</code> returns <code>false</code>.</li>
   * <li><code>".."</code> returns <code>false</code>.</li>
   * <li><code>"../"</code> returns <code>false</code>.</li>
   * </ul>
   * 
   * @param path The {@linkplain File#getPath() path} to some file or directory.
   * @return <code>true</code> if the supplied <code>path</code> is a relative subpath.
   * @throws IllegalArgumentException If <code>path</code> is <code>null</code> or {@linkplain String#isEmpty() empty}.
   */
    protected static final boolean isRelativeSubPath(final String path) throws IllegalArgumentException {
        if (path == null) throw new IllegalArgumentException("null path");
        if (path.isEmpty()) throw new IllegalArgumentException("emtpy path");
        if (path.charAt(0) == '/') return false;
        int depth = 0;
        final StringBuffer currentComponent = new StringBuffer(path.length());
        for (int i = 0; i < path.length(); i++) {
            final char c = path.charAt(i);
            if (c != '/') {
                currentComponent.append(c);
            } else {
                final String currentComponentString = currentComponent.toString();
                if (currentComponentString.equals("..")) {
                    depth--;
                    if (depth < 0) return false;
                } else if ((currentComponentString.length() > 0) && (!currentComponentString.equals("."))) {
                    depth++;
                }
                currentComponent.setLength(0);
            }
        }
        if ((currentComponent.length() == 2) && (currentComponent.toString().equals("..")) && (depth <= 0)) return false;
        return true;
    }

    /**
   * <p>
   * Do the supplied {@link URL}'s both point to the same {@linkplain URL#getHost() host} and {@linkplain URL#getPort()
   * port}?
   * </p>
   * 
   * <p>
   * If either of the supplied URL's don't specify a {@linkplain URL#getPort() port}, the
   * {@linkplain URL#getDefaultPort() default} will be used. If there is no default port, the comparison will fail
   * unless both have no default.
   * </p>
   * 
   * @param url1 The first URL.
   * @param url2 The second URL.
   * @return <code>true</code> if the URL's point to the same host and port.
   * @throws IllegalArgumentException If <code>url1</code> or <code>url2</code> is <code>null</code>.
   */
    protected static final boolean equalHostAndPort(final URL url1, final URL url2) throws IllegalArgumentException {
        if (url1 == null) throw new IllegalArgumentException("null url1");
        if (url2 == null) throw new IllegalArgumentException("null url2");
        if (!StringUtil.equal(StringUtil.mkNull(url1.getHost(), false), StringUtil.mkNull(url2.getHost(), false), false)) return false;
        final int url1Port = (url1.getPort() >= 0) ? url1.getPort() : url1.getDefaultPort();
        final int url2Port = (url2.getPort() >= 0) ? url2.getPort() : url2.getDefaultPort();
        if (url1Port != url2Port) return false;
        return true;
    }

    /**
   * Get the {@linkplain #BASE_URI_PROP base URI} of the document or site to be
   * {@linkplain #getProxiedFileURL(Page.Request, String, boolean) proxied}.
   * 
   * @see #BASE_URI_PROP
   * @see #getProxiedFileURL(Page.Request, String, boolean)
   * @see #PROXIED_BASE_URI_HOOK
   */
    public URI getProxiedBaseURI(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        URI baseURI = pluginValueHook(PROXIED_BASE_URI_HOOK, null, pageRequest);
        if (baseURI == null) {
            baseURI = getConfigProp(BASE_URI_PROP, pageRequest, RSProperties.RESULT_URI_CONVERTER, null, true, false);
        }
        baseURI = pluginFilterHook(PROXIED_BASE_URI_HOOK, null, pageRequest, baseURI);
        return baseURI;
    }

    /**
   * Get the <em>relative</em> {@linkplain #DEFAULT_PATH_PROP path} (within the
   * {@linkplain #getProxiedBaseURI(Page.Request) proxied base URI}) to the document which should be displayed within
   * this channel by default.
   * 
   * @see #DEFAULT_PATH_PROP
   * @see #getProxiedFileLocalURI(Page.Request, String, boolean)
   * @see #PROXIED_FILE_PATH_DEFAULT_HOOK
   */
    protected URI getProxiedFilePathDefault(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        URI defaultPath = pluginValueHook(PROXIED_FILE_PATH_DEFAULT_HOOK, null, pageRequest);
        if (defaultPath == null) {
            defaultPath = getConfigProp(DEFAULT_PATH_PROP, pageRequest, RSProperties.RESULT_URI_CONVERTER, null, true, true);
            if ((defaultPath != null) && ((defaultPath.isAbsolute()) || (defaultPath.getPath() == null) || (defaultPath.getPath().startsWith("/")))) {
                throw new ConfigManager.ConfigException("The '" + DEFAULT_PATH_PROP + "' property is not a relative path: '" + defaultPath.toString() + '\'', null);
            }
        }
        defaultPath = pluginFilterHook(PROXIED_FILE_PATH_DEFAULT_HOOK, null, pageRequest, defaultPath);
        return defaultPath;
    }

    /**
   * Perform any desired modifications to a link which is <em>not</em> being
   * {@linkplain #rewriteProxiedFileLink(Page.Request, URL, URI, boolean, boolean) rewritten} to point back through this
   * channel.
   * 
   * @see #rewriteProxiedFileLink(Page.Request, URL, URI, boolean, boolean)
   */
    protected static final URI rewriteProxiedFileLinkOutsideChannel(final Page.Request pageRequest, final URL proxiedFileURL, final URI linkURI, final boolean hyperLink, final boolean absoluteURLRequired, final URL resolvedLinkURL) throws IllegalArgumentException, WWWeeePortal.Exception {
        if ((linkURI != null) && (linkURI.isAbsolute())) {
            return linkURI;
        }
        try {
            if ((!absoluteURLRequired) && (equalHostAndPort(proxiedFileURL, pageRequest.getBaseURL()))) {
                final StringBuffer sb = new StringBuffer();
                sb.append(resolvedLinkURL.getPath());
                if (resolvedLinkURL.getQuery() != null) {
                    sb.append('?');
                    sb.append(resolvedLinkURL.getQuery());
                }
                if (resolvedLinkURL.getRef() != null) {
                    sb.append('#');
                    sb.append(resolvedLinkURL.getRef());
                }
                return new URI(sb.toString());
            }
            return resolvedLinkURL.toURI();
        } catch (URISyntaxException urise) {
            throw new ContentManager.ContentException("Error constructing resolved link URI", urise);
        }
    }

    /**
   * <p>
   * Rewrite a <code>linkURI</code> associated with a <code>proxiedFileURL</code>, if required.
   * </p>
   * 
   * <p>
   * Technically, there are two types of links within a document. First, a link within a document can be to an
   * <em>external resource</em>, which is loaded automatically by the browser to augment that document (ie a link to an
   * image, style sheet, script, etc). Or, second, a link within a document can be a <em>hyperlink</em>, which, when
   * activated by the user, will cause the browser to stop displaying that document and navigate to displaying the
   * linked document instead.
   * </p>
   * 
   * <p>
   * If the portal is configured to display a website to clients through this <code>ProxyChannel</code>, it is generally
   * expected that if the client navigates a hyperlink from one document to another within the proxied site, that the
   * linked document would also be rendered within the channel ({@linkplain Channel#VIEW_MODE view mode}), and that any
   * external resource links will continue to resolve correctly. Link rewriting is required for each of these two
   * scenarios to work.
   * </p>
   * 
   * <p>
   * To continue rendering within {@linkplain Channel#VIEW_MODE view mode} while navigating between website documents,
   * any hyperlink from within a proxied document to another document within the
   * {@linkplain #getProxiedBaseURI(Page.Request) proxied site} will, by default, be rewritten to point back through
   * this channel (alternatively, hyperlinks may {@linkplain #isLinkRewritingHyperLinksToChannelDisabled(Page.Request)
   * optionally} be resolved into an {@linkplain URI#isAbsolute() absolute} link pointing directly back to their
   * {@linkplain #getProxiedBaseURI(Page.Request) origin/source location} instead).
   * </p>
   * 
   * <p>
   * If this channel were to blindly return unmodified source HTML from a proxied document for aggregation into a
   * {@link Page}, any relative link would break when it was incorrectly resolved relative to the
   * {@link org.wwweeeportal.portal.ContentManager.PageDefinition.Key#getPageURI(UriInfo, Map, String, boolean) URL} of
   * that page, instead of relative to the {@linkplain #BASE_URI_PROP base URL} of the document providing it. To avoid
   * this, any relative link to an external resource from within a proxied document will, by default, be resolved into
   * an {@linkplain URI#isAbsolute() absolute} link pointing directly back to the
   * {@linkplain #getProxiedBaseURI(Page.Request) origin/source location} for that resource (alternatively, resource
   * links may {@linkplain #isLinkRewritingResourceLinksToChannelEnabled(Page.Request) optionally} be rewritten to point
   * back through this channel using {@linkplain Channel#RESOURCE_MODE resource mode} instead).
   * </p>
   * 
   * <p>
   * For link rewriting to work, the <code>ProxyChannel</code> obviously needs to know which attributes of a proxied
   * document constitute <em>links</em>. But since the implementation is generic, and doesn't actually understand any
   * particular dialect of markup language on it's own, <em>including HTML</em>, you will likely want to configure this
   * channel alongside a plugin which does, such as the
   * {@linkplain org.wwweeeportal.portal.channelplugins.ProxyChannelHTMLSource HTML plugin}.
   * </p>
   * 
   * @see #isLinkRewritingHyperLinksToChannelDisabled(Page.Request)
   * @see #isLinkRewritingResourceLinksToChannelEnabled(Page.Request)
   * @see org.wwweeeportal.portal.channelplugins.ProxyChannelHTMLSource
   */
    public URI rewriteProxiedFileLink(final Page.Request pageRequest, final URL proxiedFileURL, final URI linkURI, final boolean hyperLink, final boolean absoluteURLRequired) throws IllegalArgumentException, WWWeeePortal.Exception {
        if (proxiedFileURL == null) throw new IllegalArgumentException("null proxiedFileURL");
        if ((linkURI != null) && (linkURI.isOpaque())) {
            return linkURI;
        }
        final URL resolvedLinkURL;
        try {
            if (linkURI == null) {
                resolvedLinkURL = proxiedFileURL;
            } else if (linkURI.isAbsolute()) {
                resolvedLinkURL = linkURI.toURL();
            } else {
                resolvedLinkURL = new URL(proxiedFileURL, linkURI.toString());
            }
        } catch (MalformedURLException mue) {
            throw new ContentManager.ContentException("Error resolving proxied link URL", mue);
        }
        if (((hyperLink) && (isLinkRewritingHyperLinksToChannelDisabled(pageRequest))) || ((!hyperLink) && (!isLinkRewritingResourceLinksToChannelEnabled(pageRequest)))) {
            return rewriteProxiedFileLinkOutsideChannel(pageRequest, proxiedFileURL, linkURI, hyperLink, absoluteURLRequired, resolvedLinkURL);
        }
        if ((linkURI != null) && (linkURI.isAbsolute()) && (!equalHostAndPort(resolvedLinkURL, proxiedFileURL))) {
            return rewriteProxiedFileLinkOutsideChannel(pageRequest, proxiedFileURL, linkURI, hyperLink, absoluteURLRequired, resolvedLinkURL);
        }
        final String resolvedLinkPath = StringUtil.toString(StringUtil.mkNull(resolvedLinkURL.getPath(), false), "/");
        final URI baseURI = getProxiedBaseURI(pageRequest);
        final URI resolvedBaseURI;
        if (baseURI.isAbsolute()) {
            resolvedBaseURI = baseURI;
        } else {
            resolvedBaseURI = ConfigManager.getContextResourceLocalHostURI(pageRequest.getUriInfo(), baseURI.getPath(), ConversionUtil.invokeConverter(NetUtil.URI_QUERY_PARAMS_CONVERTER, baseURI), baseURI.getFragment(), true);
        }
        final String baseURIPath = resolvedBaseURI.getPath();
        final String baseURIFolder;
        if ((baseURIPath.length() == 1) || (baseURIPath.charAt(baseURIPath.length() - 1) == '/')) {
            baseURIFolder = baseURIPath;
        } else {
            final int lastSlashIndex = baseURIPath.lastIndexOf('/');
            baseURIFolder = (lastSlashIndex > 0) ? baseURIPath.substring(0, lastSlashIndex + 1) : String.valueOf('/');
        }
        if (!resolvedLinkPath.startsWith(baseURIFolder)) {
            return rewriteProxiedFileLinkOutsideChannel(pageRequest, proxiedFileURL, linkURI, hyperLink, absoluteURLRequired, resolvedLinkURL);
        }
        final String linkChannelLocalPath = StringUtil.mkNull(resolvedLinkPath.substring(baseURIFolder.length()), false);
        final String channelMode = ((hyperLink) && (!isMaximizationDisabled(pageRequest))) ? VIEW_MODE : RESOURCE_MODE;
        final ContentManager.ChannelSpecification<?> channelSpecification = pageRequest.getChannelSpecification(this);
        return channelSpecification.getKey().getChannelURI(pageRequest.getUriInfo(), channelMode, linkChannelLocalPath, ConversionUtil.invokeConverter(NetUtil.URI_QUERY_PARAMS_CONVERTER, linkURI), (linkURI != null) ? linkURI.getFragment() : null, absoluteURLRequired);
    }

    /**
   * Combine the {@linkplain org.wwweeeportal.portal.Page.Request#getChannelLocalPath(Channel) channel local path} (or
   * the {@linkplain #getProxiedFilePathDefault(Page.Request) default path} if that's <code>null</code>) and
   * {@linkplain UriInfo#getQueryParameters() query parameters} from the client <code>pageRequest</code>, to construct a
   * URI containing a <em>relative</em> path and query params, which can later be resolved against the
   * {@linkplain #getProxiedBaseURI(Page.Request) base URI} to create the
   * {@linkplain #getProxiedFileURL(Page.Request, String, boolean) proxied file URL}. This method will also validate the
   * request against any {@linkplain #isParentFoldersRestrictionDisabled(Page.Request) parent folder} or
   * {@linkplain #isDefaultPathRestrictionEnabled(Page.Request) default path} restrictions.
   * 
   * @see #getProxiedFileURL(Page.Request, String, boolean)
   * @see #PROXIED_FILE_LOCAL_URI_HOOK
   */
    protected URI getProxiedFileLocalURI(final Page.Request pageRequest, final String mode, final boolean validate) throws WWWeeePortal.Exception, WebApplicationException {
        final URI channelLocalPath = pageRequest.getChannelLocalPath(this);
        final Object[] context = new Object[] { channelLocalPath };
        URI proxiedFileLocalURI = pluginValueHook(PROXIED_FILE_LOCAL_URI_HOOK, context, pageRequest);
        if (proxiedFileLocalURI == null) {
            final URI proxiedFilePath;
            if (channelLocalPath != null) {
                if ((validate) && (isDefaultPathRestrictionEnabled(pageRequest))) {
                    final URI proxiedFilePathDefault = getProxiedFilePathDefault(pageRequest);
                    if (!channelLocalPath.equals(proxiedFilePathDefault)) {
                        throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).entity("Request outside default path").type("text/plain").build());
                    }
                }
                if ((validate) && (!isParentFoldersRestrictionDisabled(pageRequest))) {
                    if (!isRelativeSubPath(channelLocalPath.getPath())) {
                        throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).entity("Request outside base URL folder").type("text/plain").build());
                    }
                }
                proxiedFilePath = channelLocalPath;
            } else if (VIEW_MODE.equals(mode)) {
                proxiedFilePath = getProxiedFilePathDefault(pageRequest);
            } else {
                proxiedFilePath = null;
            }
            final Map<String, List<String>> reqParameters = (pageRequest.isMaximized(this)) ? pageRequest.getUriInfo().getQueryParameters() : null;
            if ((validate) && (isDefaultPathRestrictionEnabled(pageRequest)) && (reqParameters != null) && (!reqParameters.isEmpty())) {
                throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).entity("Request outside default path").type("text/plain").build());
            }
            if ((proxiedFilePath == null) && ((reqParameters == null) || (reqParameters.isEmpty()))) return null;
            final StringBuffer proxiedFileLocalURIBuffer = (proxiedFilePath != null) ? new StringBuffer(proxiedFilePath.toString()) : new StringBuffer();
            if ((reqParameters != null) && (!reqParameters.isEmpty())) {
                for (String reqParamName : reqParameters.keySet()) {
                    final List<String> reqParamValues = reqParameters.get(reqParamName);
                    if (reqParamValues == null) continue;
                    final String reqParamNameEncoded = ConversionUtil.invokeConverter(NetUtil.URL_ENCODE_CONVERTER, reqParamName);
                    for (String reqParamValue : reqParamValues) {
                        if (proxiedFileLocalURIBuffer.indexOf("?") >= 0) {
                            proxiedFileLocalURIBuffer.append('&');
                        } else {
                            proxiedFileLocalURIBuffer.append('?');
                        }
                        proxiedFileLocalURIBuffer.append(reqParamNameEncoded);
                        proxiedFileLocalURIBuffer.append('=');
                        proxiedFileLocalURIBuffer.append(ConversionUtil.invokeConverter(NetUtil.URL_ENCODE_CONVERTER, reqParamValue));
                    }
                }
            }
            try {
                proxiedFileLocalURI = new URI(proxiedFileLocalURIBuffer.toString());
            } catch (URISyntaxException urise) {
                throw new WWWeeePortal.SoftwareException(urise);
            }
        }
        proxiedFileLocalURI = pluginFilterHook(PROXIED_FILE_LOCAL_URI_HOOK, context, pageRequest, proxiedFileLocalURI);
        return proxiedFileLocalURI;
    }

    /**
   * Construct the final {@linkplain URI#isAbsolute() absolute} {@link URL} for the
   * {@linkplain #createProxyRequest(Page.Request, String, HttpClient) proxied} file by resolving the relative
   * {@linkplain #getProxiedFileLocalURI(Page.Request, String, boolean) proxied file local URI} against the
   * {@linkplain #getProxiedBaseURI(Page.Request) base URI}, and if the result isn't absolute, against the
   * {@linkplain ConfigManager#getContextResourceLocalHostURI(UriInfo, String, Map, String, boolean) local host context}
   * .
   * 
   * @see #createProxyRequest(Page.Request, String, HttpClient)
   * @see #PROXIED_FILE_URL_HOOK
   */
    protected URL getProxiedFileURL(final Page.Request pageRequest, final String mode, final boolean validate) throws WWWeeePortal.Exception, WebApplicationException {
        final Object[] context = new Object[] { mode, Boolean.valueOf(validate) };
        URL proxiedFileURL = pluginValueHook(PROXIED_FILE_URL_HOOK, context, pageRequest);
        if (proxiedFileURL == null) {
            try {
                final URI proxiedFileLocalURI = getProxiedFileLocalURI(pageRequest, mode, validate);
                final URI baseURI = getProxiedBaseURI(pageRequest);
                if (proxiedFileLocalURI != null) {
                    final URI proxiedFileURI = baseURI.resolve(proxiedFileLocalURI);
                    if (proxiedFileURI.isAbsolute()) {
                        proxiedFileURL = proxiedFileURI.toURL();
                    } else {
                        proxiedFileURL = ConfigManager.getContextResourceLocalHostURI(pageRequest.getUriInfo(), proxiedFileURI.getPath(), ConversionUtil.invokeConverter(NetUtil.URI_QUERY_PARAMS_CONVERTER, proxiedFileURI), proxiedFileURI.getFragment(), true).toURL();
                    }
                } else {
                    if (baseURI.isAbsolute()) {
                        proxiedFileURL = baseURI.toURL();
                    } else {
                        proxiedFileURL = ConfigManager.getContextResourceLocalHostURI(pageRequest.getUriInfo(), baseURI.getPath(), ConversionUtil.invokeConverter(NetUtil.URI_QUERY_PARAMS_CONVERTER, baseURI), baseURI.getFragment(), true).toURL();
                    }
                }
            } catch (MalformedURLException mue) {
                throw new WWWeeePortal.SoftwareException(mue);
            }
        }
        proxiedFileURL = pluginFilterHook(PROXIED_FILE_URL_HOOK, context, pageRequest, proxiedFileURL);
        return proxiedFileURL;
    }

    /**
   * Construct a custom {@link ClientConnectionManager} (if one is required) to manage the {@link HttpClient} being
   * {@linkplain #createProxyClient(Page.Request) created} to {@linkplain #doProxyRequest(Page.Request, String) proxy}
   * content while fulfilling the specified <code>pageRequest</code>. The default implementation returns
   * <code>null</code>, which will cause the default implementation to be constructed by
   * {@link DefaultHttpClient#createClientConnectionManager()}.
   * 
   * @see #createProxyClient(Page.Request)
   * @see #PROXY_CLIENT_MANAGER_HOOK
   */
    protected ClientConnectionManager createProxyClientManager(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        ClientConnectionManager proxyClientManager = pluginValueHook(PROXY_CLIENT_MANAGER_HOOK, null, pageRequest);
        proxyClientManager = pluginFilterHook(PROXY_CLIENT_MANAGER_HOOK, null, pageRequest, proxyClientManager);
        return proxyClientManager;
    }

    /**
   * Construct and configure the {@link HttpParams} to be used for {@linkplain #createProxyClient(Page.Request)
   * creating} any {@link HttpClient} to {@linkplain #doProxyRequest(Page.Request, String) proxy} content while
   * fulfilling the specified <code>pageRequest</code>.
   * 
   * @see #createProxyClient(Page.Request)
   * @see #PROXY_CLIENT_PARAMS_HOOK
   */
    protected HttpParams createProxyClientParams(final Page.Request pageRequest, final ClientConnectionManager proxyClientManager) throws WWWeeePortal.Exception {
        final Object[] context = new Object[] { proxyClientManager };
        HttpParams proxyClientParams = pluginValueHook(PROXY_CLIENT_PARAMS_HOOK, context, pageRequest);
        if (proxyClientParams == null) {
            proxyClientParams = new SyncBasicHttpParams();
            DefaultHttpClient.setDefaultHttpParams(proxyClientParams);
            proxyClientParams.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, getConnectTimeout(pageRequest));
            proxyClientParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, getReadTimeout(pageRequest));
            proxyClientParams.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, isFollowRedirectsEnabled(pageRequest));
            proxyClientParams.setBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, false);
        }
        proxyClientParams = pluginFilterHook(PROXY_CLIENT_PARAMS_HOOK, context, pageRequest, proxyClientParams);
        return proxyClientParams;
    }

    /**
   * Construct and configure the {@link HttpClient} to be used to {@linkplain #doProxyRequest(Page.Request, String)
   * proxy} content while fulfilling the specified <code>pageRequest</code>. This method will also configure
   * {@linkplain ProxyRequestInterceptor request} and {@linkplain ProxyResponseInterceptor response} interceptors on the
   * created client.
   * 
   * @see #doProxyRequest(Page.Request, String)
   * @see #PROXY_CLIENT_HOOK
   * @see ProxyRequestInterceptor
   * @see ProxyResponseInterceptor
   */
    protected HttpClient createProxyClient(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        final ClientConnectionManager proxyClientManager = createProxyClientManager(pageRequest);
        final HttpParams httpParams = createProxyClientParams(pageRequest, proxyClientManager);
        final Object[] context = new Object[] { proxyClientManager, httpParams };
        HttpClient proxyClient = pluginValueHook(PROXY_CLIENT_HOOK, context, pageRequest);
        if (proxyClient == null) {
            proxyClient = new DefaultHttpClient(proxyClientManager, httpParams);
            ((AbstractHttpClient) proxyClient).addRequestInterceptor(new ProxyRequestInterceptor(pageRequest, proxyClient));
            ((AbstractHttpClient) proxyClient).addResponseInterceptor(new ProxyResponseInterceptor(pageRequest, proxyClient));
        }
        proxyClient = pluginFilterHook(PROXY_CLIENT_HOOK, context, pageRequest, proxyClient);
        return proxyClient;
    }

    /**
   * Construct the {@link HttpUriRequest} implementation appropriate to
   * {@linkplain #createProxyRequest(Page.Request, String, HttpClient) proxy} the specified <code>pageRequest</code> to
   * the <code>proxiedFileURL</code>.
   * 
   * @see #createProxyRequest(Page.Request, String, HttpClient)
   * @see #PROXY_REQUEST_OBJ_HOOK
   */
    protected HttpUriRequest createProxyRequestObject(final Page.Request pageRequest, final String mode, final URL proxiedFileURL) throws WWWeeePortal.Exception, WebApplicationException {
        final Object[] context = new Object[] { mode, proxiedFileURL };
        HttpUriRequest proxyRequest = pluginValueHook(PROXY_REQUEST_OBJ_HOOK, context, pageRequest);
        if (proxyRequest == null) {
            final URI proxiedFileURI;
            try {
                proxiedFileURI = proxiedFileURL.toURI();
            } catch (URISyntaxException urise) {
                throw new WWWeeePortal.SoftwareException(urise);
            }
            final String method = pageRequest.getRSRequest().getMethod();
            if ((!pageRequest.isMaximized(this)) || (method.equalsIgnoreCase("GET"))) {
                proxyRequest = new HttpGet(proxiedFileURI);
            } else if (method.equalsIgnoreCase("HEAD")) {
                proxyRequest = new HttpHead(proxiedFileURI);
            } else if (method.equalsIgnoreCase("POST")) {
                proxyRequest = new HttpPost(proxiedFileURI);
            } else if (method.equalsIgnoreCase("PUT")) {
                proxyRequest = new HttpPut(proxiedFileURI);
            } else if (method.equalsIgnoreCase("DELETE")) {
                proxyRequest = new HttpDelete(proxiedFileURI);
            } else {
                throw new WebApplicationException(Response.status(RESTUtil.Response.Status.METHOD_NOT_ALLOWED).build());
            }
        }
        proxyRequest = pluginFilterHook(PROXY_REQUEST_OBJ_HOOK, context, pageRequest, proxyRequest);
        return proxyRequest;
    }

    /**
   * Construct the value for the &quot;User-Agent&quot; header to be {@link HttpUriRequest#setHeader(String, String)
   * set} on the {@link HttpUriRequest} being used to {@linkplain #createProxyRequest(Page.Request, String, HttpClient)
   * proxy} content to the <code>proxiedFileURL</code>. This method starts with the
   * {@linkplain javax.ws.rs.core.HttpHeaders#USER_AGENT USER_AGENT} header provided by the client, appends the
   * WWWeee-Portal {@link #USER_AGENT_HEADER}, followed by the {@linkplain CoreProtocolPNames#USER_AGENT USER_AGENT} for
   * the {@link HttpClient} being used.
   * 
   * @see #USER_AGENT_HEADER
   * @see #createProxyRequest(Page.Request, String, HttpClient)
   */
    protected String getProxyRequestUserAgentHeader(final Page.Request pageRequest, final String mode, final HttpClient proxyClient, final URL proxiedFileURL, final HttpUriRequest proxyRequest) throws WWWeeePortal.Exception, WebApplicationException {
        final StringBuffer value = new StringBuffer();
        final String clientRequestHeader = CollectionUtil.first(pageRequest.getHttpHeaders().getRequestHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT), null);
        if (clientRequestHeader != null) value.append(clientRequestHeader);
        if (value.length() > 0) value.append(' ');
        value.append(USER_AGENT_HEADER);
        final String proxyRequestUserAgent = (String) proxyClient.getParams().getParameter(CoreProtocolPNames.USER_AGENT);
        if (proxyRequestUserAgent != null) {
            value.append(' ');
            value.append(proxyRequestUserAgent);
        }
        return value.toString();
    }

    /**
   * Construct the value for the &quot;Accept-Language&quot; header to be
   * {@link HttpUriRequest#setHeader(String, String) set} on the {@link HttpUriRequest} being used to
   * {@linkplain #createProxyRequest(Page.Request, String, HttpClient) proxy} content to the <code>proxiedFileURL</code>
   * . This method basically just fowards the {@link javax.ws.rs.core.HttpHeaders#getAcceptableLanguages() acceptable}
   * to the client.
   * 
   * @see javax.ws.rs.core.HttpHeaders#getAcceptableLanguages()
   * @see #createProxyRequest(Page.Request, String, HttpClient)
   */
    protected String getProxyRequestAcceptLanguageHeader(final Page.Request pageRequest, final String mode, final HttpClient proxyClient, final URL proxiedFileURL, final HttpUriRequest proxyRequest) throws WWWeeePortal.Exception, WebApplicationException {
        final List<Locale> acceptableLanguages = pageRequest.getHttpHeaders().getAcceptableLanguages();
        if ((acceptableLanguages == null) || (acceptableLanguages.isEmpty())) return null;
        final StringBuffer acceptLanguage = new StringBuffer();
        for (Locale locale : acceptableLanguages) {
            if (acceptLanguage.length() > 0) acceptLanguage.append(',');
            acceptLanguage.append(locale.getLanguage());
            if (!locale.getCountry().isEmpty()) {
                acceptLanguage.append('-');
                acceptLanguage.append(locale.getCountry());
            }
        }
        return acceptLanguage.toString();
    }

    /**
   * Construct the value for the &quot;Accept&quot; header to be {@link HttpUriRequest#setHeader(String, String) set} on
   * the {@link HttpUriRequest} being used to {@linkplain #createProxyRequest(Page.Request, String, HttpClient) proxy}
   * content to the <code>proxiedFileURL</code>.
   * 
   * @see #createProxyRequest(Page.Request, String, HttpClient)
   */
    protected String getProxyRequestAcceptHeader(final Page.Request pageRequest, final String mode, final HttpClient proxyClient, final URL proxiedFileURL, final HttpUriRequest proxyRequest) throws WWWeeePortal.Exception, WebApplicationException {
        if (VIEW_MODE.equals(mode)) {
            final StringBuffer accept = new StringBuffer();
            accept.append(HTMLUtil.APPLICATION_XHTML_XML_MIME_TYPE);
            accept.append(',');
            accept.append(XMLUtil.APPLICATION_XML_MIME_TYPE);
            accept.append(',');
            accept.append(XMLUtil.TEXT_XML_MIME_TYPE);
            accept.append(";q=0.9,");
            accept.append(APPLICATION_STAR_MIME_TYPE);
            accept.append(";q=0.5,");
            accept.append(TEXT_STAR_MIME_TYPE);
            accept.append(";q=0.4");
            return accept.toString();
        }
        return CollectionUtil.first(pageRequest.getHttpHeaders().getRequestHeader("Accept"), null);
    }

    /**
   * Construct the value for the &quot;Authorization&quot; header to be {@link HttpUriRequest#setHeader(String, String)
   * set} on the {@link HttpUriRequest} being used to {@linkplain #createProxyRequest(Page.Request, String, HttpClient)
   * proxy} content to the <code>proxiedFileURL</code>.
   * 
   * @see #createProxyRequest(Page.Request, String, HttpClient)
   */
    protected String getProxyRequestAuthorizationHeader(final Page.Request pageRequest, final String mode, final HttpClient proxyClient, final URL proxiedFileURL, final HttpUriRequest proxyRequest) throws WWWeeePortal.Exception, WebApplicationException {
        if ((!proxyRequest.containsHeader("Authorization")) && (CollectionUtil.first(pageRequest.getHttpHeaders().getRequestHeader("Authorization"), null) != null)) {
            return CollectionUtil.first(pageRequest.getHttpHeaders().getRequestHeader("Authorization"), null);
        }
        return null;
    }

    /**
   * Construct the {@link HttpUriRequest} that will be used to {@linkplain #doProxyRequest(Page.Request, String) proxy}
   * content while fulfilling the specified <code>pageRequest</code>. This method will
   * {@linkplain #getProxiedFileURL(Page.Request, String, boolean) calculate} the proxied file URL,
   * {@linkplain #createProxyRequestObject(Page.Request, String, URL) create} the appropriate type of request object,
   * set it's {@linkplain HttpUriRequest#setHeader(String, String) headers}, and, if this channel is
   * {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximized}, set any
   * {@linkplain HttpEntityEnclosingRequest#setEntity(HttpEntity) entity} that was
   * {@linkplain org.wwweeeportal.portal.Page.Request#getEntity() provided} by the <code>pageRequest</code>.
   * 
   * @see #doProxyRequest(Page.Request, String)
   * @see #PROXY_REQUEST_HOOK
   */
    protected HttpUriRequest createProxyRequest(final Page.Request pageRequest, final String mode, final HttpClient proxyClient) throws WWWeeePortal.Exception, WebApplicationException {
        final URL proxiedFileURL = getProxiedFileURL(pageRequest, mode, true);
        final Object[] context = new Object[] { mode, proxiedFileURL };
        HttpUriRequest proxyRequest = pluginValueHook(PROXY_REQUEST_HOOK, context, pageRequest);
        if (proxyRequest == null) {
            proxyRequest = createProxyRequestObject(pageRequest, mode, proxiedFileURL);
            final String userAgent = getProxyRequestUserAgentHeader(pageRequest, mode, proxyClient, proxiedFileURL, proxyRequest);
            if (userAgent != null) proxyRequest.setHeader("User-Agent", userAgent);
            final String acceptLanguage = getProxyRequestAcceptLanguageHeader(pageRequest, mode, proxyClient, proxiedFileURL, proxyRequest);
            if (acceptLanguage != null) proxyRequest.setHeader("Accept-Language", acceptLanguage);
            final String accept = getProxyRequestAcceptHeader(pageRequest, mode, proxyClient, proxiedFileURL, proxyRequest);
            if (accept != null) proxyRequest.setHeader("Accept", accept);
            final String authorization = getProxyRequestAuthorizationHeader(pageRequest, mode, proxyClient, proxiedFileURL, proxyRequest);
            if (authorization != null) proxyRequest.setHeader("Authorization", authorization);
            if (RESOURCE_MODE.equals(mode)) {
                final String ifMatch = CollectionUtil.first(pageRequest.getHttpHeaders().getRequestHeader("If-Match"), null);
                if (ifMatch != null) proxyRequest.setHeader("If-Match", ifMatch);
                final String ifModifiedSince = CollectionUtil.first(pageRequest.getHttpHeaders().getRequestHeader("If-Modified-Since"), null);
                if (ifModifiedSince != null) proxyRequest.setHeader("If-Modified-Since", ifModifiedSince);
                final String ifNoneMatch = CollectionUtil.first(pageRequest.getHttpHeaders().getRequestHeader("If-None-Match"), null);
                if (ifNoneMatch != null) proxyRequest.setHeader("If-None-Match", ifNoneMatch);
                final String ifUnmodifiedSince = CollectionUtil.first(pageRequest.getHttpHeaders().getRequestHeader("If-Unmodified-Since"), null);
                if (ifUnmodifiedSince != null) proxyRequest.setHeader("If-Unmodified-Since", ifUnmodifiedSince);
            }
            if (!isCacheControlClientDirectivesDisabled(pageRequest)) {
                final CacheControl cacheControl = ConversionUtil.invokeConverter(RESTUtil.HTTP_HEADERS_CACHE_CONTROL_CONVERTER, pageRequest.getHttpHeaders());
                if (cacheControl != null) proxyRequest.setHeader("Cache-Control", cacheControl.toString());
            }
            if (pageRequest.isMaximized(this)) {
                final MediaType contentType = pageRequest.getHttpHeaders().getMediaType();
                if (contentType != null) proxyRequest.setHeader("Content-Type", contentType.toString());
                if ((proxyRequest instanceof HttpEntityEnclosingRequest) && (pageRequest.getEntity() != null)) {
                    try {
                        final Long contentLength = ConversionUtil.invokeConverter(StringUtil.STRING_LONG_CONVERTER, CollectionUtil.first(pageRequest.getHttpHeaders().getRequestHeader("Content-Length"), null));
                        final HttpEntity httpEntity = new InputStreamEntity(pageRequest.getEntity().getInputStream(), (contentLength != null) ? contentLength.longValue() : -1);
                        ((HttpEntityEnclosingRequest) proxyRequest).setEntity(httpEntity);
                    } catch (IOException ioe) {
                        throw new WWWeeePortal.OperationalException(ioe);
                    } catch (IllegalArgumentException iae) {
                        throw new WebApplicationException(iae, Response.Status.BAD_REQUEST);
                    }
                }
            }
        }
        proxyRequest = pluginFilterHook(PROXY_REQUEST_HOOK, context, pageRequest, proxyRequest);
        return proxyRequest;
    }

    /**
   * Construct a {@link Reader} for the {@linkplain HttpResponse#getEntity() entity} within the supplied
   * <code>proxyResponse</code>. This method will attempt to use the given <code>responseContentType</code> to
   * {@linkplain IOUtil#getCharsetParameter(MimeType) determine} the correct {@link Charset} to use for
   * {@linkplain InputStreamReader#InputStreamReader(InputStream, Charset) decoding} the
   * {@linkplain HttpEntity#getContent() byte stream}.
   */
    protected Reader createProxiedFileReader(final HttpResponse proxyResponse, final MimeType responseContentType) throws WWWeeePortal.Exception {
        if (proxyResponse.getEntity() == null) return null;
        final Charset charset;
        try {
            charset = (responseContentType != null) ? IOUtil.getCharsetParameter(responseContentType) : null;
        } catch (UnsupportedCharsetException uce) {
            throw new ContentManager.ContentException("Proxied file has unsupported charset", uce);
        }
        try {
            return (charset != null) ? new InputStreamReader(proxyResponse.getEntity().getContent(), charset) : new InputStreamReader(proxyResponse.getEntity().getContent());
        } catch (IOException ioe) {
            throw new WWWeeePortal.OperationalException(ioe);
        }
    }

    /**
   * Examine the {@linkplain HttpResponse#getStatusLine() status} {@link StatusLine#getStatusCode() code} from the
   * response to the {@linkplain #doProxyRequest(Page.Request, String) proxy request} and throw an exception if
   * something went wrong.
   * 
   * @see #doProxyRequest(Page.Request, String)
   */
    protected HttpContext validateProxyResponse(final HttpContext proxyContext, final Page.Request pageRequest, final String mode) throws WWWeeePortal.Exception, WebApplicationException {
        final HttpResponse proxyResponse = (HttpResponse) proxyContext.getAttribute(ExecutionContext.HTTP_RESPONSE);
        final int responseCode = proxyResponse.getStatusLine().getStatusCode();
        if (responseCode == Response.Status.OK.getStatusCode()) return proxyContext;
        try {
            if (responseCode == Response.Status.NOT_MODIFIED.getStatusCode()) {
                throw new WebApplicationException(Response.Status.NOT_MODIFIED);
            } else if (((responseCode >= Response.Status.MOVED_PERMANENTLY.getStatusCode()) && (responseCode <= Response.Status.SEE_OTHER.getStatusCode())) || (responseCode == Response.Status.TEMPORARY_REDIRECT.getStatusCode())) {
                if (pageRequest.isMaximized(this)) {
                    final String location = ConversionUtil.invokeConverter(HTTPUtil.HEADER_VALUE_CONVERTER, proxyResponse.getLastHeader("Location"));
                    final URI locationURI;
                    try {
                        locationURI = new URI(location);
                    } catch (URISyntaxException urise) {
                        throw new ContentManager.ContentException("Error parsing 'Location' header: " + location, urise);
                    }
                    final URL proxiedFileURL;
                    try {
                        proxiedFileURL = HTTPUtil.getRequestTargetURL(proxyContext);
                    } catch (URISyntaxException urise) {
                        throw new ContentManager.ContentException("Error parsing proxied file URL", urise);
                    } catch (MalformedURLException mue) {
                        throw new ContentManager.ContentException("Error parsing proxied file URL", mue);
                    }
                    final URI fixedLocation;
                    try {
                        fixedLocation = rewriteProxiedFileLink(pageRequest, proxiedFileURL, locationURI, VIEW_MODE.equals(mode), true);
                    } catch (ContentManager.ContentException wpce) {
                        throw new ContentManager.ContentException("Error rewriting 'Location' header", wpce);
                    }
                    throw new WebApplicationException(Response.status(RESTUtil.Response.Status.fromStatusCode(responseCode)).location(fixedLocation).build());
                }
            } else if (responseCode == Response.Status.UNAUTHORIZED.getStatusCode()) {
                if (pageRequest.isMaximized(this)) {
                    throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", ConversionUtil.invokeConverter(HTTPUtil.HEADER_VALUE_CONVERTER, proxyResponse.getLastHeader("WWW-Authenticate"))).build());
                }
            } else if ((responseCode == Response.Status.NOT_FOUND.getStatusCode()) || (responseCode == Response.Status.GONE.getStatusCode())) {
                final URI channelLocalPath = pageRequest.getChannelLocalPath(this);
                if (channelLocalPath != null) {
                    throw new WebApplicationException(Response.Status.fromStatusCode(responseCode));
                }
            } else if (responseCode == RESTUtil.Response.Status.METHOD_NOT_ALLOWED.getStatusCode()) {
                final URI channelLocalPath = pageRequest.getChannelLocalPath(this);
                if (channelLocalPath != null) {
                    throw new WebApplicationException(Response.status(RESTUtil.Response.Status.METHOD_NOT_ALLOWED).header("Allow", ConversionUtil.invokeConverter(HTTPUtil.HEADER_VALUE_CONVERTER, proxyResponse.getLastHeader("Allow"))).build());
                }
            } else if (responseCode == RESTUtil.Response.Status.REQUEST_TIMEOUT.getStatusCode()) {
                throw new WWWeeePortal.OperationalException(new WebApplicationException(responseCode));
            } else if (responseCode == Response.Status.BAD_REQUEST.getStatusCode()) {
                throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
            } else if ((responseCode >= 400) && (responseCode < 500)) {
                if (pageRequest.isMaximized(this)) {
                    throw new WebApplicationException(Response.status(RESTUtil.Response.Status.fromStatusCode(responseCode)).build());
                }
            } else if ((responseCode >= RESTUtil.Response.Status.BAD_GATEWAY.getStatusCode()) && (responseCode <= RESTUtil.Response.Status.GATEWAY_TIMEOUT.getStatusCode())) {
                throw new WWWeeePortal.OperationalException(new WebApplicationException(responseCode));
            }
            final String codePhrase = (RESTUtil.Response.Status.fromStatusCode(responseCode) != null) ? " (" + RESTUtil.Response.Status.fromStatusCode(responseCode).getReasonPhrase() + ")" : "";
            final String responsePhrase = (proxyResponse.getStatusLine().getReasonPhrase() != null) ? ": " + proxyResponse.getStatusLine().getReasonPhrase() : "";
            final ConfigManager.ConfigException configurationException = new ConfigManager.ConfigException("The proxied file server returned code '" + responseCode + "'" + codePhrase + responsePhrase, null);
            if (getLogger().isLoggable(Level.FINE)) {
                try {
                    final Reader reader = createProxiedFileReader(proxyResponse, getProxyResponseHeader(pageRequest, proxyResponse, "Content-Type", HTTPUtil.HEADER_MIME_TYPE_CONVERTER));
                    final String responseContent = ConversionUtil.invokeConverter(IOUtil.READER_STRING_CONVERTER, reader);
                    LogAnnotation.annotate(configurationException, "ProxiedFileResponseContent", responseContent, Level.FINE, false);
                } catch (Exception e) {
                }
            }
            throw configurationException;
        } catch (WWWeeePortal.Exception wpe) {
            try {
                LogAnnotation.annotate(wpe, "ProxiedFileURL", HTTPUtil.getRequestTargetURL(proxyContext), null, false);
            } catch (Exception e) {
            }
            LogAnnotation.annotate(wpe, "ProxiedFileResponseCode", Integer.valueOf(responseCode), null, false);
            LogAnnotation.annotate(wpe, "ProxiedFileResponseCodeReasonPhrase", RESTUtil.Response.Status.fromStatusCode(responseCode), null, false);
            LogAnnotation.annotate(wpe, "ProxiedFileResponseReasonPhrase", proxyResponse.getStatusLine().getReasonPhrase(), null, false);
            throw wpe;
        }
    }

    /**
   * {@linkplain #createProxyClient(Page.Request) Create} an {@link HttpClient} and use it to
   * {@linkplain HttpClient#execute(HttpUriRequest, HttpContext) execute} a
   * {@linkplain #createProxyRequest(Page.Request, String, HttpClient) proxy request} to retrieve the content to be
   * returned/rendered in response to the supplied <code>pageRequest</code>.
   * 
   * @see #PROXY_RESPONSE_HOOK
   */
    protected HttpContext doProxyRequest(final Page.Request pageRequest, final String mode) throws WWWeeePortal.Exception, WebApplicationException {
        final HttpClient proxyClient = createProxyClient(pageRequest);
        final HttpContext proxyContext = new BasicHttpContext();
        try {
            HttpResponse proxyResponse = null;
            while (proxyResponse == null) {
                final HttpUriRequest proxyRequest = createProxyRequest(pageRequest, mode, proxyClient);
                try {
                    final Object[] context = new Object[] { mode, proxyClient, proxyContext, proxyRequest };
                    proxyResponse = pluginValueHook(PROXY_RESPONSE_HOOK, context, pageRequest);
                    if (proxyResponse == null) {
                        try {
                            proxyResponse = proxyClient.execute(proxyRequest, proxyContext);
                        } catch (UnknownHostException uhe) {
                            throw new ConfigManager.ConfigException(uhe);
                        } catch (IOException ioe) {
                            throw new WWWeeePortal.OperationalException(ioe);
                        }
                    }
                    proxyResponse = pluginFilterHook(PROXY_RESPONSE_HOOK, context, pageRequest, proxyResponse);
                } catch (WWWeeePortal.Exception wpe) {
                    LogAnnotation.annotate(wpe, "ProxyRequest", proxyRequest, null, false);
                    LogAnnotation.annotate(wpe, "ProxyResponse", proxyResponse, null, false);
                    LogAnnotation.annotate(wpe, "ProxiedFileURL", proxyRequest.getURI(), null, false);
                    throw wpe;
                }
            }
            return validateProxyResponse(proxyContext, pageRequest, mode);
        } catch (WWWeeePortal.Exception wpe) {
            LogAnnotation.annotate(wpe, "ProxyContext", proxyContext, null, false);
            try {
                LogAnnotation.annotate(wpe, "ProxiedFileURL", HTTPUtil.getRequestTargetURL(proxyContext), null, false);
            } catch (Exception e) {
            }
            throw wpe;
        }
    }

    /**
   * {@linkplain HttpResponse#getHeaders(String) Retrieve} the value of the named header (multiple values will be
   * {@linkplain HTTPUtil#consolidateHeaders(Header[]) consolidated}), using the supplied <code>converter</code> to
   * handle the data type.
   * 
   * @see #PROXY_RESPONSE_HEADER_HOOK
   */
    public <V> V getProxyResponseHeader(final Page.Request pageRequest, final HttpResponse proxyResponse, final String headerName, final Converter<Header, V> converter) throws WWWeeePortal.Exception {
        final Object[] context = new Object[] { proxyResponse, headerName };
        Header header = pluginValueHook(PROXY_RESPONSE_HEADER_HOOK, context, pageRequest);
        if (header == null) {
            header = HTTPUtil.consolidateHeaders(proxyResponse.getHeaders(headerName));
        }
        if (header == null) header = new BasicHeader(headerName, null);
        header = pluginFilterHook(PROXY_RESPONSE_HEADER_HOOK, context, pageRequest, header);
        try {
            return ConversionUtil.invokeConverter(converter, header);
        } catch (ConversionException ce) {
            throw new ContentManager.ContentException("Proxy response has invalid '" + headerName + "' header", ce);
        }
    }

    /**
   * Should the content contained within the supplied <code>proxyResponse</code> having the supplied
   * <code>contentMimeType</code> be
   * {@linkplain #renderXMLView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType) rendered using the XML
   * view}? By default, this method will return <code>true</code> for any {@linkplain XMLUtil#isXML(MimeType) XML}
   * MimeType.
   * 
   * @see #renderXMLView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType)
   * @see #IS_RENDERED_USING_XML_VIEW_HOOK
   */
    protected boolean isRenderedUsingXMLView(final Page.Request pageRequest, final HttpResponse proxyResponse, final MimeType contentMimeType) throws WWWeeePortal.Exception {
        final Object[] context = new Object[] { proxyResponse, contentMimeType };
        Boolean isRenderedUsingXMLView = pluginValueHook(IS_RENDERED_USING_XML_VIEW_HOOK, context, pageRequest);
        if (isRenderedUsingXMLView == null) {
            isRenderedUsingXMLView = Boolean.valueOf(XMLUtil.isXML(contentMimeType));
        }
        return Boolean.TRUE.equals(pluginFilterHook(IS_RENDERED_USING_XML_VIEW_HOOK, context, pageRequest, isRenderedUsingXMLView));
    }

    /**
   * Create a {@link #createProxiedFileReader(HttpResponse, MimeType) Reader} for the <code>proxyResponse</code> and
   * wrap it in a {@link TypedInputSource}.
   * 
   * @see #renderXMLView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType)
   * @see #PROXIED_DOC_INPUT_SOURCE_HOOK
   */
    protected TypedInputSource createProxiedDocumentInputSource(final Page.Request pageRequest, final HttpResponse proxyResponse, final URL proxiedFileURL, final MimeType responseContentType) throws WWWeeePortal.Exception {
        final Object[] context = new Object[] { proxyResponse, proxiedFileURL, responseContentType };
        TypedInputSource proxiedDocumentInputSource = pluginValueHook(PROXIED_DOC_INPUT_SOURCE_HOOK, context, pageRequest);
        if (proxiedDocumentInputSource == null) {
            final Reader proxiedFileReader = createProxiedFileReader(proxyResponse, responseContentType);
            proxiedDocumentInputSource = new TypedInputSource(new InputSource(proxiedFileReader), responseContentType);
        }
        proxiedDocumentInputSource = pluginFilterHook(PROXIED_DOC_INPUT_SOURCE_HOOK, context, pageRequest, proxiedDocumentInputSource);
        return proxiedDocumentInputSource;
    }

    /**
   * Create a {@linkplain DOMBuildingHandler} to add the parsed XML to the channel
   * {@linkplain org.wwweeeportal.portal.Channel.ViewResponse#getContentContainerElement() content}.
   * 
   * @see #renderXMLView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType)
   * @see #PROXIED_DOC_CONTENT_HANDLER_HOOK
   */
    protected DefaultHandler2 createProxiedDocumentContentHandler(final Page.Request pageRequest, final ViewResponse viewResponse, final URL proxiedFileURL, final TypedInputSource proxiedDocumentInputSource) throws WWWeeePortal.Exception {
        final Object[] context = new Object[] { viewResponse, proxiedFileURL, proxiedDocumentInputSource };
        DefaultHandler2 contentHandler = pluginValueHook(PROXIED_DOC_CONTENT_HANDLER_HOOK, context, pageRequest);
        if (contentHandler == null) {
            contentHandler = new DOMBuildingHandler((DefaultHandler2) null, viewResponse.getContentContainerElement());
        }
        contentHandler = pluginFilterHook(PROXIED_DOC_CONTENT_HANDLER_HOOK, context, pageRequest, contentHandler);
        return contentHandler;
    }

    /**
   * Create an {@linkplain #createProxiedDocumentInputSource(Page.Request, HttpResponse, URL, MimeType) input source},
   * {@linkplain MarkupManager#parseXMLDocument(InputSource, DefaultHandler2, boolean) feed} it to an XML parser, and
   * {@linkplain #createProxiedDocumentContentHandler(Page.Request, Channel.ViewResponse, URL, TypedInputSource) handle}
   * the results.
   * 
   * @see #isRenderedUsingXMLView(Page.Request, HttpResponse, MimeType)
   * @see #createProxiedDocumentInputSource(Page.Request, HttpResponse, URL, MimeType)
   * @see MarkupManager#parseXMLDocument(InputSource, DefaultHandler2, boolean)
   * @see #createProxiedDocumentContentHandler(Page.Request, Channel.ViewResponse, URL, TypedInputSource)
   * @see #PARSE_XML_HOOK
   */
    protected void renderXMLView(final Page.Request pageRequest, final ViewResponse viewResponse, final HttpResponse proxyResponse, final URL proxiedFileURL, final MimeType contentMimeType) throws WWWeeePortal.Exception {
        final TypedInputSource proxiedDocumentInputSource = createProxiedDocumentInputSource(pageRequest, proxyResponse, proxiedFileURL, contentMimeType);
        if (proxiedDocumentInputSource == null) return;
        final DefaultHandler2 contentHandler = createProxiedDocumentContentHandler(pageRequest, viewResponse, proxiedFileURL, proxiedDocumentInputSource);
        final Object[] context = new Object[] { viewResponse, proxiedDocumentInputSource, contentHandler };
        Boolean parsedXML = pluginValueHook(PARSE_XML_HOOK, context, pageRequest);
        if (!Boolean.TRUE.equals(parsedXML)) {
            MarkupManager.parseXMLDocument(proxiedDocumentInputSource.getInputSource(), contentHandler, false);
        }
        pluginFilterHook(PARSE_XML_HOOK, context, pageRequest, Boolean.TRUE);
        final Locale contentLocale = MarkupManager.getXMLLangAttr(CollectionUtil.first(DOMUtil.getChildElements(viewResponse.getContentContainerElement(), null, null), null), null, false);
        if (contentLocale != null) viewResponse.setLocale(contentLocale);
        return;
    }

    /**
   * Should the content contained within the supplied <code>proxyResponse</code> having the supplied
   * <code>contentMimeType</code> be
   * {@linkplain #renderTextView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType) rendered using the
   * Text view}?
   * 
   * @see #renderTextView(Page.Request, Channel.ViewResponse, HttpResponse, URL, MimeType)
   * @see #IS_RENDERED_USING_TEXT_VIEW_HOOK
   */
    protected boolean isRenderedUsingTextView(final Page.Request pageRequest, final HttpResponse proxyResponse, final MimeType contentMimeType) throws WWWeeePortal.Exception {
        final Object[] context = new Object[] { proxyResponse, contentMimeType };
        Boolean isRenderedUsingTextView = pluginValueHook(IS_RENDERED_USING_TEXT_VIEW_HOOK, context, pageRequest);
        if (isRenderedUsingTextView == null) {
            isRenderedUsingTextView = Boolean.valueOf((contentMimeType != null) && (IOUtil.TEXT_PLAIN_MIME_TYPE.getBaseType().equals(contentMimeType.getBaseType())));
        }
        return Boolean.TRUE.equals(pluginFilterHook(IS_RENDERED_USING_TEXT_VIEW_HOOK, context, pageRequest, isRenderedUsingTextView));
    }

    /**
   * {@link #createProxiedFileReader(HttpResponse, MimeType) Read} the <code>proxyResponse</code> into a
   * {@link IOUtil#READER_STRING_CONVERTER String} and add it to the channel
   * {@link org.wwweeeportal.portal.Channel.ViewResponse#getContentContainerElement() content} within a
   * <code>&lt;pre&gt;</code> element.
   * 
   * @see #isRenderedUsingTextView(Page.Request, HttpResponse, MimeType)
   * @see #createProxiedFileReader(HttpResponse, MimeType)
   */
    protected void renderTextView(final Page.Request pageRequest, final ViewResponse viewResponse, final HttpResponse proxyResponse, final URL proxiedFileURL, final MimeType contentMimeType) throws WWWeeePortal.Exception {
        final Reader proxiedDocumentReader = createProxiedFileReader(proxyResponse, contentMimeType);
        final String textString;
        try {
            textString = ConversionUtil.invokeConverter(IOUtil.READER_STRING_CONVERTER, proxiedDocumentReader);
        } catch (ConversionException ce) {
            throw new WWWeeePortal.OperationalException(ce);
        }
        final Element channelContentContainerElement = viewResponse.getContentContainerElement();
        final Element preElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "pre", channelContentContainerElement);
        setIDAndClassAttrs(preElement, Arrays.asList("proxy", "text"), null, null);
        DOMUtil.createAttribute(XMLUtil.XML_NS_URI, "xml", "space", "preserve", preElement);
        DOMUtil.appendText(preElement, textString);
        return;
    }

    /**
   * Add an <code>&lt;object&gt;</code> element to the channel
   * {@link org.wwweeeportal.portal.Channel.ViewResponse#getContentContainerElement() content} creating an external
   * resource reference to the content at the given <code>proxiedFileURL</code> (link
   * {@linkplain #rewriteProxiedFileLink(Page.Request, URL, URI, boolean, boolean) rewritten} if necessary).
   * 
   * @see #INLINE_CONTENT_DISABLE_PROP
   */
    protected void renderResourceReferenceView(final Page.Request pageRequest, final ViewResponse viewResponse, final URL proxiedFileURL, final MimeType contentType) throws WWWeeePortal.Exception, WebApplicationException {
        final Element contentContainerElement = viewResponse.getContentContainerElement();
        final Document contentContainerDocument = DOMUtil.getDocument(contentContainerElement);
        final Element objectElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "object", contentContainerElement, contentContainerDocument, true, true);
        final String[][] extraObjectClasses;
        if (contentType != null) {
            final String contentTypeString = contentType.toString();
            DOMUtil.createAttribute(null, null, "type", contentTypeString, objectElement);
            final StringBuffer safeTypeClass = new StringBuffer();
            for (int i = 0; i < contentTypeString.length(); i++) {
                char c = contentTypeString.charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    safeTypeClass.append(c);
                } else {
                    safeTypeClass.append('_');
                }
            }
            extraObjectClasses = new String[][] { new String[] { portal.getPortalID(), "channel", definition.getID(), "resource", "type", safeTypeClass.toString() } };
        } else {
            extraObjectClasses = null;
        }
        setIDAndClassAttrs(objectElement, Arrays.asList("proxy", "resource"), extraObjectClasses, null);
        final URI resourceURI = rewriteProxiedFileLink(pageRequest, proxiedFileURL, null, false, false);
        DOMUtil.createAttribute(null, null, "data", resourceURI.toString(), objectElement);
        return;
    }

    /**
   * This method will create a new {@link CacheControl} by intelligently merging the values from the
   * <code>newCacheControl</code> into the <code>defaultCacheControl</code> values, removing a default 'public'
   * directive if the input specifies a 'private' one, etc.
   */
    protected static final CacheControl mergeCacheControl(final CacheControl defaultCacheControl, final CacheControl newCacheControl) {
        if ((defaultCacheControl == null) || (defaultCacheControl.equals(newCacheControl))) return newCacheControl;
        final CacheControl mergedCacheControl = ConversionUtil.invokeConverter(RESTUtil.CACHE_CONTROL_CLONE_CONVERTER, defaultCacheControl);
        if (newCacheControl == null) return mergedCacheControl;
        if (newCacheControl.getCacheExtension().containsKey("public")) {
            mergedCacheControl.getCacheExtension().put("public", null);
            mergedCacheControl.setPrivate(false);
            mergedCacheControl.getPrivateFields().clear();
            mergedCacheControl.setNoStore(false);
            mergedCacheControl.setNoCache(false);
            mergedCacheControl.getNoCacheFields().clear();
        }
        if (newCacheControl.isPrivate()) {
            mergedCacheControl.setPrivate(true);
            mergedCacheControl.getPrivateFields().clear();
            mergedCacheControl.getPrivateFields().addAll(newCacheControl.getPrivateFields());
            mergedCacheControl.getCacheExtension().remove("public");
        }
        if (newCacheControl.isNoCache()) {
            mergedCacheControl.setNoCache(true);
            mergedCacheControl.getNoCacheFields().clear();
            mergedCacheControl.getNoCacheFields().addAll(newCacheControl.getPrivateFields());
            mergedCacheControl.getCacheExtension().remove("public");
        }
        if (newCacheControl.isNoStore()) {
            mergedCacheControl.setNoStore(true);
            mergedCacheControl.getCacheExtension().remove("public");
        }
        if (newCacheControl.isNoTransform()) {
            mergedCacheControl.setNoTransform(true);
        }
        if (newCacheControl.isMustRevalidate()) {
            mergedCacheControl.setMustRevalidate(true);
        }
        if (newCacheControl.isProxyRevalidate()) {
            mergedCacheControl.setProxyRevalidate(true);
        }
        if (newCacheControl.getMaxAge() >= 0) {
            mergedCacheControl.setMaxAge(newCacheControl.getMaxAge());
        }
        if (newCacheControl.getSMaxAge() >= 0) {
            mergedCacheControl.setSMaxAge(newCacheControl.getSMaxAge());
        }
        return mergedCacheControl;
    }

    @Override
    protected void doViewRequestImpl(final Page.Request pageRequest, final ViewResponse viewResponse) throws WWWeeePortal.Exception, WebApplicationException {
        if (isInlineContentDisabled(pageRequest)) {
            renderResourceReferenceView(pageRequest, viewResponse, getProxiedFileURL(pageRequest, VIEW_MODE, true), HTMLUtil.APPLICATION_XHTML_XML_MIME_TYPE);
            return;
        }
        final HttpContext proxyContext = doProxyRequest(pageRequest, VIEW_MODE);
        final HttpResponse proxyResponse = (HttpResponse) proxyContext.getAttribute(ExecutionContext.HTTP_RESPONSE);
        final URL proxiedFileURL;
        try {
            proxiedFileURL = HTTPUtil.getRequestTargetURL(proxyContext);
        } catch (IllegalArgumentException iae) {
            throw new ContentManager.ContentException("Error parsing proxied file URL", iae);
        } catch (URISyntaxException urise) {
            throw new ContentManager.ContentException("Error parsing proxied file URL", urise);
        } catch (MalformedURLException mue) {
            throw new ContentManager.ContentException("Error parsing proxied file URL", mue);
        }
        try {
            final MimeType contentMimeType = getProxyResponseHeader(pageRequest, proxyResponse, "Content-Type", HTTPUtil.HEADER_MIME_TYPE_CONVERTER);
            viewResponse.setLastModified(getProxyResponseHeader(pageRequest, proxyResponse, "Last-Modified", HTTPUtil.HEADER_DATE_CONVERTER));
            viewResponse.setContentType(ConversionUtil.invokeConverter(RESTUtil.MIME_TYPE_MEDIA_TYPE_CONVERTER, contentMimeType));
            viewResponse.setCacheControl(mergeCacheControl(viewResponse.getCacheControl(), getProxyResponseHeader(pageRequest, proxyResponse, "Cache-Control", HEADER_CACHE_CONTROL_CONVERTER)));
            viewResponse.setExpires(getProxyResponseHeader(pageRequest, proxyResponse, "Expires", HTTPUtil.HEADER_DATE_CONVERTER));
            viewResponse.setEntityTag(getProxyResponseHeader(pageRequest, proxyResponse, "ETag", HEADER_ENTITY_TAG_CONVERTER));
            viewResponse.setLocale(CollectionUtil.first(getProxyResponseHeader(pageRequest, proxyResponse, "Content-Language", HTTPUtil.HEADER_LOCALE_LIST_CONVERTER), null));
            if (isRenderedUsingXMLView(pageRequest, proxyResponse, contentMimeType)) {
                renderXMLView(pageRequest, viewResponse, proxyResponse, proxiedFileURL, contentMimeType);
            } else if (isRenderedUsingTextView(pageRequest, proxyResponse, contentMimeType)) {
                renderTextView(pageRequest, viewResponse, proxyResponse, proxiedFileURL, contentMimeType);
            } else {
                renderResourceReferenceView(pageRequest, viewResponse, proxiedFileURL, contentMimeType);
            }
        } catch (WWWeeePortal.Exception wpe) {
            LogAnnotation.annotate(wpe, "ProxyContext", proxyContext, null, false);
            LogAnnotation.annotate(wpe, "ProxyResponse", proxyResponse, null, false);
            LogAnnotation.annotate(wpe, "ProxiedFileURL", proxiedFileURL, null, false);
            throw wpe;
        }
        return;
    }

    @Override
    protected Response doResourceRequestImpl(final Page.Request pageRequest) throws WWWeeePortal.Exception, WebApplicationException {
        final HttpContext proxyContext = doProxyRequest(pageRequest, RESOURCE_MODE);
        final HttpResponse proxyResponse = (HttpResponse) proxyContext.getAttribute(ExecutionContext.HTTP_RESPONSE);
        try {
            final Response.ResponseBuilder responseBuilder = Response.ok();
            responseBuilder.lastModified(getProxyResponseHeader(pageRequest, proxyResponse, "Last-Modified", HTTPUtil.HEADER_DATE_CONVERTER));
            final MimeType contentMimeType = getProxyResponseHeader(pageRequest, proxyResponse, "Content-Type", HTTPUtil.HEADER_MIME_TYPE_CONVERTER);
            responseBuilder.type(ConversionUtil.invokeConverter(RESTUtil.MIME_TYPE_MEDIA_TYPE_CONVERTER, contentMimeType));
            responseBuilder.cacheControl(mergeCacheControl(getCacheControlDefault(), getProxyResponseHeader(pageRequest, proxyResponse, "Cache-Control", HEADER_CACHE_CONTROL_CONVERTER)));
            responseBuilder.expires(getProxyResponseHeader(pageRequest, proxyResponse, "Expires", HTTPUtil.HEADER_DATE_CONVERTER));
            responseBuilder.tag(getProxyResponseHeader(pageRequest, proxyResponse, "ETag", HEADER_ENTITY_TAG_CONVERTER));
            responseBuilder.language(CollectionUtil.first(getProxyResponseHeader(pageRequest, proxyResponse, "Content-Language", HTTPUtil.HEADER_LOCALE_LIST_CONVERTER), null));
            final HttpEntity proxyResponseEntity = proxyResponse.getEntity();
            final Long contentLength = (proxyResponseEntity != null) ? Long.valueOf(proxyResponseEntity.getContentLength()) : null;
            responseBuilder.header("Content-Length", contentLength);
            if (proxyResponseEntity != null) {
                responseBuilder.entity(new HttpEntityDataSource(null, proxyResponseEntity));
            }
            return responseBuilder.build();
        } catch (WWWeeePortal.Exception wpe) {
            LogAnnotation.annotate(wpe, "ProxyContext", proxyContext, null, false);
            LogAnnotation.annotate(wpe, "ProxyResponse", proxyResponse, null, false);
            try {
                LogAnnotation.annotate(wpe, "ProxiedFileURL", HTTPUtil.getRequestTargetURL(proxyContext), null, false);
            } catch (Exception e) {
            }
            throw wpe;
        }
    }

    /**
   * An {@link HttpRequestInterceptor} which is {@link AbstractHttpClient#addRequestInterceptor(HttpRequestInterceptor)
   * added} to all {@link HttpClient}'s {@linkplain ProxyChannel#createProxyClient(Page.Request) created} to
   * {@linkplain ProxyChannel#doProxyRequest(Page.Request, String) proxy} content for this channel, which, when
   * {@linkplain #process(HttpRequest, HttpContext) processed}, allows {@link org.wwweeeportal.portal.Channel.Plugin
   * plugin's} a {@linkplain ProxyChannel#PROXY_REQUEST_INTERCEPTOR_HOOK hook} they can use to
   * {@linkplain ProxyChannel#pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the
   * {@link HttpRequest}.
   * 
   * @see ProxyChannel#PROXY_REQUEST_INTERCEPTOR_HOOK
   */
    public final class ProxyRequestInterceptor implements HttpRequestInterceptor {

        /**
     * The {@link org.wwweeeportal.portal.Page.Request} behind the proxy {@link HttpRequest} to be intercepted.
     */
        protected final Page.Request pageRequest;

        /**
     * The {@link HttpClient} performing the proxy {@link HttpRequest} to be intercepted.
     */
        protected final HttpClient proxyClient;

        /**
     * Construct a new <code>ProxyRequestInterceptor</code>.
     */
        public ProxyRequestInterceptor(final Page.Request pageRequest, final HttpClient proxyClient) throws IllegalArgumentException {
            if (pageRequest == null) throw new IllegalArgumentException("null pageRequest");
            if (proxyClient == null) throw new IllegalArgumentException("null proxyClient");
            this.pageRequest = pageRequest;
            this.proxyClient = proxyClient;
            return;
        }

        @Override
        @SuppressWarnings("synthetic-access")
        public void process(final HttpRequest proxyRequest, final HttpContext proxyContext) throws HttpException, IOException {
            try {
                pluginFilterHook(PROXY_REQUEST_INTERCEPTOR_HOOK, new Object[] { proxyClient, proxyContext }, pageRequest, proxyRequest);
            } catch (WWWeeePortal.Exception wpe) {
                final HttpException httpException = new HttpException();
                httpException.initCause(wpe);
                throw httpException;
            }
            return;
        }
    }

    /**
   * An {@link HttpResponseInterceptor} which is
   * {@link AbstractHttpClient#addResponseInterceptor(HttpResponseInterceptor) added} to all {@link HttpClient}'s
   * {@linkplain ProxyChannel#createProxyClient(Page.Request) created} to
   * {@linkplain ProxyChannel#doProxyRequest(Page.Request, String) proxy} content for this channel, which, when
   * {@linkplain #process(HttpResponse, HttpContext) processed}, allows {@link org.wwweeeportal.portal.Channel.Plugin
   * plugin's} a {@linkplain ProxyChannel#PROXY_RESPONSE_INTERCEPTOR_HOOK hook} they can use to
   * {@linkplain ProxyChannel#pluginFilterHook(Channel.PluginHook, Object[], Page.Request, Object) filter} the
   * {@link HttpResponse}.
   * 
   * @see ProxyChannel#PROXY_RESPONSE_INTERCEPTOR_HOOK
   */
    public final class ProxyResponseInterceptor implements HttpResponseInterceptor {

        /**
     * The {@link org.wwweeeportal.portal.Page.Request} behind the proxy {@link HttpResponse} to be intercepted.
     */
        protected final Page.Request pageRequest;

        /**
     * The {@link HttpClient} performing the proxy {@link HttpResponse} to be intercepted.
     */
        protected final HttpClient proxyClient;

        /**
     * Construct a new <code>ProxyResponseInterceptor</code>.
     */
        public ProxyResponseInterceptor(final Page.Request pageRequest, final HttpClient proxyClient) throws IllegalArgumentException {
            if (pageRequest == null) throw new IllegalArgumentException("null pageRequest");
            if (proxyClient == null) throw new IllegalArgumentException("null proxyClient");
            this.pageRequest = pageRequest;
            this.proxyClient = proxyClient;
            return;
        }

        @Override
        @SuppressWarnings("synthetic-access")
        public void process(final HttpResponse proxyResponse, final HttpContext proxyContext) throws HttpException, IOException {
            try {
                pluginFilterHook(PROXY_RESPONSE_INTERCEPTOR_HOOK, new Object[] { proxyClient, proxyContext }, pageRequest, proxyResponse);
            } catch (WWWeeePortal.Exception wpe) {
                final HttpException httpException = new HttpException();
                httpException.initCause(wpe);
                throw httpException;
            }
            return;
        }
    }
}
