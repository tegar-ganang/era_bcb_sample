package org.wwweeeportal.portal;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;
import javax.activation.*;
import org.w3c.dom.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.springframework.core.convert.converter.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.logging.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.xml.html.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.util.ws.rs.annotation.*;

/**
 * <p>
 * An abstract base for creating runtime instance classes responsible for
 * {@linkplain #doViewRequest(Page.Request, Node) providing} an XML content fragment or resources to a {@link Page}
 * instance based on configuration from a {@linkplain ContentManager.ChannelDefinition channel definition}.
 * </p>
 * 
 * <p>
 * Each <code>Channel</code> on a {@link Page} is {@linkplain ContentManager.ChannelSpecification specified} via a
 * unique {@linkplain ContentManager.ChannelSpecification.Key#getChannelID() identifier} as part of it's
 * {@linkplain ContentManager.ChannelSpecification#getKey() key}. If a client request to
 * {@linkplain Page#doViewGetRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map) view} an entire
 * {@link Page} is received, it will result in that {@link Page} making it's own calls to
 * {@linkplain #doViewRequest(Page.Request, Node) view} each <code>Channel</code> on the {@link Page}. However, a
 * request may also {@linkplain Page#getChannel(String) target} a specific <code>Channel</code> as a sub-resource of the
 * {@link Page}, in which case the JAX-RS framework will then route the request directly to this class, either as a
 * {@linkplain #VIEW_MODE view} request (
 * {@link #doViewGetRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map) GET} or
 * {@link #doViewPostRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map, DataSource) POST}) or a
 * {@linkplain #RESOURCE_MODE resource} request (
 * {@link #doResourceGetRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map) GET} or
 * {@link #doResourcePostRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map, DataSource) POST}). Any
 * request {@linkplain UriInfo#getPath() path} beyond that required to identify the <code>Channel</code> is considered
 * part of the {@linkplain Page.Request#getChannelLocalPath(Channel) channel local path}.
 * </p>
 * 
 * <p>
 * By default, the markup for each <code>Channel</code> is actually comprised of three nested markup elements. First is
 * an outer {@linkplain Channel.ViewResponse#getResponseRootElement() root element}, appended to the
 * {@linkplain ContentManager.ChannelGroupDefinition channel group} element provided by the {@link Page} (see there for
 * markup details), which acts as the container for the entire <code>Channel</code>. And, although that root element
 * will likely be configured to contain additional <code>Channel</code> header and/or footer
 * {@linkplain org.wwweeeportal.portal.channelplugins.AbstractChannelAugmentation augmentations}, by default it simply
 * contains a single intermediate &quot;channel body&quot; <code>&lt;div&gt;</code> element. And, finally, the
 * <code>Channel</code> body contains an inner &quot;{@linkplain Channel.ViewResponse#getContentContainerElement()
 * content container}&quot; <code>&lt;div&gt;</code>, to which the actual <code>Channel</code> content is added. While
 * the body <code>&lt;div&gt;</code> is intended for visual/thematic styling, the additional &quot;content
 * container&quot; <code>&lt;div&gt;</code> is intended to add extra flexibility in handling overflow/scrolling of the
 * <code>Channel</code> content (ie, you can control the margin/padding between the body <code>&lt;div&gt;</code> border
 * and a scrollbar created by content overflow).
 * </p>
 * 
 * <h3 id="configuration">Configuration</h3>
 * <p>
 * The following {@linkplain ConfigManager configuration properties} are supported by this class:
 * </p>
 * <ul>
 * <li>{@link #TITLE_TEXT_PROP}</li>
 * <li>{@link #LINK_BY_NUM_PROP}</li>
 * <li>{@link #LINK_BY_PATH_PROP}</li>
 * <li>{@link #SCRIPT_BY_NUM_PROP}</li>
 * <li>{@link #SCRIPT_BY_PATH_PROP}</li>
 * <li>{@link #META_BY_NUM_PROP}</li>
 * <li>{@link #META_BY_PATH_PROP}</li>
 * <li>{@link #CLASS_BY_NUM_PROP}</li>
 * <li>{@link #CACHE_CONTROL_CLIENT_DIRECTIVES_DISABLE_PROP}</li>
 * <li>{@link #CACHE_CONTROL_DEFAULT_PROP}</li>
 * <li>{@link #MAXIMIZATION_DISABLE_PROP}</li>
 * <li>{@link #MAXIMIZATION_PRIORITY_NORMAL_PROP}</li>
 * <li>{@link #MAXIMIZATION_PRIORITY_MAXIMIZED_PROP}</li>
 * <li>{@link #SECTION_TYPE_PROP}</li>
 * </ul>
 * 
 * @see Page
 */
public abstract class Channel {

    /**
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the <code>Channel</code>
   * {@linkplain #getTitleText(Page.Request) title}.
   * 
   * @see #getTitleText(Page.Request)
   * @see Page#TITLE_TEXT_PROP
   * @see org.wwweeeportal.portal.channelplugins.ChannelTitle
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String TITLE_TEXT_PROP = "Channel.Title.Text";

    /**
   * The prefix defining an ordered set of numbered keys to
   * {@linkplain MarkupManager#PROP_RESULT_LINK_ELEMENT_CONVERTER query encoded} property values, each specifying a
   * <code>&lt;link&gt;</code> element to be added, in order, to the {@link Page} <code>&lt;html&gt;&lt;head&gt;</code>.
   * Any non-{@linkplain URI#isAbsolute() absolute} URI attributes will be
   * {@linkplain MarkupManager#resolveMetaLinkElementContextResources(Element, UriInfo) resolved}.
   * 
   * @see #getMetaLinkElements(Page.Request)
   * @see #LINK_BY_PATH_PROP
   * @see MarkupManager#PROP_RESULT_LINK_ELEMENT_CONVERTER
   * @see Page#LINK_BY_NUM_PROP
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String LINK_BY_NUM_PROP = "Channel.Link.Num.";

    /**
   * @see #LINK_BY_NUM_PROP
   * @see #getMetaLinkElements(Page.Request)
   */
    protected static final Pattern LINK_BY_NUM_PATTERN = Pattern.compile("^" + Pattern.quote(LINK_BY_NUM_PROP) + ".*");

    /**
   * The prefix defining a set of {@linkplain ConfigManager.RegexPropKeyConverter regexp keys} to
   * {@linkplain MarkupManager#PROP_RESULT_ENTRY_LINK_ELEMENT_CONVERTER query encoded} property values, each specifying
   * a <code>&lt;link&gt;</code> element to be added to the {@link Page} <code>&lt;html&gt;&lt;head&gt;</code> whenever
   * the {@linkplain Page.Request#getChannelLocalPath(Channel) channel local path} matches that regexp. Any non-
   * {@linkplain URI#isAbsolute() absolute} URI attributes will be
   * {@linkplain MarkupManager#resolveMetaLinkElementContextResources(Element, UriInfo) resolved}.
   * 
   * @see #getMetaLinkElements(Page.Request)
   * @see #LINK_BY_NUM_PROP
   * @see ConfigManager.RegexPropKeyConverter
   * @see MarkupManager#PROP_RESULT_ENTRY_LINK_ELEMENT_CONVERTER
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String LINK_BY_PATH_PROP = "Channel.Link.Path.";

    /**
   * @see #LINK_BY_PATH_PROP
   * @see #getMetaLinkElements(Page.Request)
   */
    protected static final Pattern LINK_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(LINK_BY_PATH_PROP) + ".*");

    /**
   * @see #LINK_BY_PATH_PROP
   * @see #getMetaLinkElements(Page.Request)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<Element>, Map.Entry<String, Pattern>>> LINK_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<Element>, Map.Entry<RSProperties.Entry<Element>, Map.Entry<String, Pattern>>>(MarkupManager.PROP_RESULT_ENTRY_LINK_ELEMENT_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<Element>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<Element>()));

    /**
   * The prefix defining an ordered set of numbered keys to
   * {@linkplain MarkupManager#PROP_RESULT_SCRIPT_ELEMENT_CONVERTER query encoded} property values, each specifying a
   * <code>&lt;script&gt;</code> element to be added, in order, to the {@link Page}
   * <code>&lt;html&gt;&lt;head&gt;</code>. Any non-{@linkplain URI#isAbsolute() absolute} URI attributes will be
   * {@linkplain MarkupManager#resolveMetaScriptElementContextResources(Element, UriInfo) resolved}.
   * 
   * @see #getMetaScriptElements(Page.Request)
   * @see #SCRIPT_BY_PATH_PROP
   * @see MarkupManager#PROP_RESULT_SCRIPT_ELEMENT_CONVERTER
   * @see Page#SCRIPT_BY_NUM_PROP
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String SCRIPT_BY_NUM_PROP = "Channel.Script.Num.";

    /**
   * @see #SCRIPT_BY_NUM_PROP
   * @see #getMetaScriptElements(Page.Request)
   */
    protected static final Pattern SCRIPT_BY_NUM_PATTERN = Pattern.compile("^" + Pattern.quote(SCRIPT_BY_NUM_PROP) + ".*");

    /**
   * The prefix defining a set of {@linkplain ConfigManager.RegexPropKeyConverter regexp keys} to
   * {@linkplain MarkupManager#PROP_RESULT_ENTRY_SCRIPT_ELEMENT_CONVERTER query encoded} property values, each
   * specifying a <code>&lt;script&gt;</code> element to be added to the {@link Page}
   * <code>&lt;html&gt;&lt;head&gt;</code> whenever the {@linkplain Page.Request#getChannelLocalPath(Channel) channel
   * local path} matches that regexp. Any non- {@linkplain URI#isAbsolute() absolute} URI attributes will be
   * {@linkplain MarkupManager#resolveMetaScriptElementContextResources(Element, UriInfo) resolved}.
   * 
   * @see #getMetaScriptElements(Page.Request)
   * @see #SCRIPT_BY_NUM_PROP
   * @see ConfigManager.RegexPropKeyConverter
   * @see MarkupManager#PROP_RESULT_ENTRY_SCRIPT_ELEMENT_CONVERTER
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String SCRIPT_BY_PATH_PROP = "Channel.Script.Path.";

    /**
   * @see #SCRIPT_BY_PATH_PROP
   * @see #getMetaScriptElements(Page.Request)
   */
    protected static final Pattern SCRIPT_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(SCRIPT_BY_PATH_PROP) + ".*");

    /**
   * @see #SCRIPT_BY_PATH_PROP
   * @see #getMetaScriptElements(Page.Request)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<Element>, Map.Entry<String, Pattern>>> SCRIPT_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<Element>, Map.Entry<RSProperties.Entry<Element>, Map.Entry<String, Pattern>>>(MarkupManager.PROP_RESULT_ENTRY_SCRIPT_ELEMENT_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<Element>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<Element>()));

    /**
   * The prefix defining an ordered set of numbered keys to
   * {@linkplain MarkupManager#PROP_RESULT_META_ELEMENT_CONVERTER query encoded} property values, each specifying a
   * <code>&lt;meta&gt;</code> element to be added, in order, to the {@link Page} <code>&lt;html&gt;&lt;head&gt;</code>.
   * Any non-{@linkplain URI#isAbsolute() absolute} URI attributes will be
   * {@linkplain MarkupManager#resolveMetaMetaElementContextResources(Element, UriInfo) resolved}.
   * 
   * @see #getMetaMetaElements(Page.Request)
   * @see #META_BY_PATH_PROP
   * @see MarkupManager#PROP_RESULT_META_ELEMENT_CONVERTER
   * @see Page#META_BY_NUM_PROP
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String META_BY_NUM_PROP = "Channel.Meta.Num.";

    /**
   * @see #META_BY_NUM_PROP
   * @see #getMetaMetaElements(Page.Request)
   */
    protected static final Pattern META_BY_NUM_PATTERN = Pattern.compile("^" + Pattern.quote(META_BY_NUM_PROP) + ".*");

    /**
   * The prefix defining a set of {@linkplain ConfigManager.RegexPropKeyConverter regexp keys} to
   * {@linkplain MarkupManager#PROP_RESULT_ENTRY_META_ELEMENT_CONVERTER query encoded} property values, each specifying
   * a <code>&lt;meta&gt;</code> element to be added to the {@link Page} <code>&lt;html&gt;&lt;head&gt;</code> whenever
   * the {@linkplain Page.Request#getChannelLocalPath(Channel) channel local path} matches that regexp. Any non-
   * {@linkplain URI#isAbsolute() absolute} URI attributes will be
   * {@linkplain MarkupManager#resolveMetaMetaElementContextResources(Element, UriInfo) resolved}.
   * 
   * @see #getMetaMetaElements(Page.Request)
   * @see #META_BY_NUM_PROP
   * @see ConfigManager.RegexPropKeyConverter
   * @see MarkupManager#PROP_RESULT_ENTRY_META_ELEMENT_CONVERTER
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String META_BY_PATH_PROP = "Channel.Meta.Path.";

    /**
   * @see #META_BY_PATH_PROP
   * @see #getMetaMetaElements(Page.Request)
   */
    protected static final Pattern META_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(META_BY_PATH_PROP) + ".*");

    /**
   * @see #META_BY_PATH_PROP
   * @see #getMetaMetaElements(Page.Request)
   */
    protected static final Converter<RSProperties.Result, Map.Entry<RSProperties.Entry<Element>, Map.Entry<String, Pattern>>> META_BY_PATH_CONVERTER = new ConverterChain<RSProperties.Result, RSProperties.Entry<Element>, Map.Entry<RSProperties.Entry<Element>, Map.Entry<String, Pattern>>>(MarkupManager.PROP_RESULT_ENTRY_META_ELEMENT_CONVERTER, new SourceTargetMapEntryConverterWrapper<RSProperties.Entry<Element>, Map.Entry<String, Pattern>>(new ConfigManager.RegexPropKeyConverter<Element>()));

    /**
   * The prefix defining an ordered set of numbered keys to {@link RSProperties#RESULT_STRING_CONVERTER String} property
   * values to be added to the <code>class</code> attribute on the <code>Channel</code>
   * {@link ViewResponse#getResponseRootElement() root} Element.
   * 
   * @see Page#CLASS_BY_NUM_PROP
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String CLASS_BY_NUM_PROP = "Channel.Class.Num.";

    /**
   * @see #CLASS_BY_NUM_PROP
   */
    protected static final Pattern CLASS_BY_NUM_PATTERN = Pattern.compile("^" + Pattern.quote(CLASS_BY_NUM_PROP) + ".*");

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property disabling any evaluation of the
   * {@link CacheControl} request header provided by the client.
   * 
   * @see #isCacheControlClientDirectivesDisabled(Page.Request)
   * @see org.wwweeeportal.portal.channelplugins.ChannelCache
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String CACHE_CONTROL_CLIENT_DIRECTIVES_DISABLE_PROP = "Channel.CacheControl.ClientDirectives.Disable";

    /**
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the default value of the
   * {@link CacheControl} response header to be used for the <code>Channel</code>.
   * 
   * @see #getCacheControlDefault()
   * @see Page#CACHE_CONTROL_DEFAULT_PROP
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String CACHE_CONTROL_DEFAULT_PROP = "Channel.CacheControl.Default";

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property disabling the ability for the
   * <code>Channel</code> to be viewed {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximized}.
   * 
   * @see #isMaximizationDisabled(Page.Request)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String MAXIMIZATION_DISABLE_PROP = "Channel.Maximization.Disable";

    /**
   * The key to an {@link RSProperties#RESULT_INTEGER_CONVERTER Integer} property defining an explicit
   * {@linkplain #getMaximizationNormalPriority(Page.Request) viewing priority} value for this <code>Channel</code> when
   * <em>not</em> {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximized}. If this property is
   * not specified then the {@linkplain #MAXIMIZATION_PRIORITY_DEFAULT default} will be used.
   * 
   * @see #getMaximizationNormalPriority(Page.Request)
   * @see #MAXIMIZATION_PRIORITY_DEFAULT
   * @see #MAXIMIZATION_PRIORITY_MAXIMIZED_PROP
   * @see org.wwweeeportal.portal.Page.Request#isMaximized(Channel)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String MAXIMIZATION_PRIORITY_NORMAL_PROP = "Channel.Maximization.Priority.Normal";

    /**
   * Unless an explicit value is {@linkplain #MAXIMIZATION_PRIORITY_NORMAL_PROP specified}, this value will be used as
   * the default {@linkplain #getMaximizationNormalPriority(Page.Request) viewing priority} for this
   * <code>Channel</code> when <em>not</em> {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel)
   * maximized}.
   * 
   * @see #getMaximizationNormalPriority(Page.Request)
   * @see org.wwweeeportal.portal.Page.Request#isMaximized(Channel)
   * @see #MAXIMIZATION_PRIORITY_NORMAL_PROP
   */
    public static final Integer MAXIMIZATION_PRIORITY_DEFAULT = Integer.valueOf(1000);

    /**
   * The key to an {@link RSProperties#RESULT_INTEGER_CONVERTER Integer} property defining an explicit
   * {@linkplain #getMaximizationMaximizedPriority(Page.Request) viewing priority} value for this <code>Channel</code>
   * when <em>{@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximized}</em>.
   * 
   * @see #getMaximizationMaximizedPriority(Page.Request)
   * @see #MAXIMIZATION_PRIORITY_NORMAL_PROP
   * @see #MAXIMIZATION_PRIORITY_DEFAULT
   * @see #MAXIMIZATION_PRIORITY_MULTIPLIER
   * @see org.wwweeeportal.portal.Page.Request#isMaximized(Channel)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String MAXIMIZATION_PRIORITY_MAXIMIZED_PROP = "Channel.Maximization.Priority.Maximized";

    /**
   * Unless an explicit value is {@linkplain #MAXIMIZATION_PRIORITY_MAXIMIZED_PROP specified}, then the
   * {@linkplain #getMaximizationMaximizedPriority(Page.Request) maximized priority} will be calculated as the
   * {@linkplain #getMaximizationNormalPriority(Page.Request) normal priority} multiplied by this value.
   * 
   * @see #getMaximizationMaximizedPriority(Page.Request)
   * @see #MAXIMIZATION_PRIORITY_MAXIMIZED_PROP
   */
    public static final int MAXIMIZATION_PRIORITY_MULTIPLIER = 100;

    /**
   * <p>
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the
   * {@linkplain Element#getLocalName() local name} for the {@linkplain HTMLUtil#HTML_NS_URI HTML}5
   * {@linkplain #getSectionType(Page.Request) section type} {@link Element} to be used for this <code>Channel</code>.
   * </p>
   * 
   * <p>
   * If this property is not specified then the {@linkplain #SECTION_TYPE_DEFAULT default} will be used.
   * </p>
   * 
   * @see #getSectionType(Page.Request)
   * @see #SECTION_TYPE_DEFAULT
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String SECTION_TYPE_PROP = "Channel.Section.Type";

    /**
   * Unless an explicit value is {@linkplain #SECTION_TYPE_PROP specified}, this value will be used as the default
   * {@linkplain #getSectionType(Page.Request) section type} for this <code>Channel</code>.
   * 
   * @see #getSectionType(Page.Request)
   * @see #SECTION_TYPE_PROP
   */
    public static final String SECTION_TYPE_DEFAULT = "article";

    /**
   * View mode is the method by which the <code>Channel</code> renders content as part of a {@link Page}. Since a
   * {@link Page} is an XML document, obviously only XML based content can be returned as part of that {@link Page} via
   * this mode. This differs from {@linkplain #RESOURCE_MODE resource mode}, where content is returned as a
   * &quot;regular&quot; standalone file, which is capable of accommodating any type of content (XML, binary, etc).
   * 
   * @see #RESOURCE_MODE
   * @see #doViewGetRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map)
   * @see #doViewPostRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map, DataSource)
   */
    public static final String VIEW_MODE = "view";

    /**
   * Resource mode is the method by which the <code>Channel</code> provides content as a standalone file. This mode is
   * capable of returning any type of content (binary, XML, etc). This differs from {@linkplain #VIEW_MODE view mode},
   * where content is rendered as part of a {@link Page}, and thus required to be XML.
   * 
   * @see #VIEW_MODE
   * @see #doResourceGetRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map)
   * @see #doResourcePostRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map, DataSource)
   */
    public static final String RESOURCE_MODE = "resources";

    /**
   * A {@linkplain PluginHook hook} which allows a {@link Plugin} to either
   * {@linkplain #pluginValueHook(PluginHook, Object[], Page.Request) provide} it's own {@linkplain ViewResponse
   * response} (ie, from a {@linkplain org.wwweeeportal.portal.channelplugins.ChannelCache cache}), instead of the
   * <code>Channel</code> retrieving one {@linkplain #doViewRequestImpl(Page.Request, ViewResponse) normally}, or to
   * {@linkplain #pluginFilterHook(PluginHook, Object[], Page.Request, Object) filter} (ie,
   * {@linkplain org.wwweeeportal.portal.channelplugins.ChannelTransformer transform}) the normally provided value.
   * 
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<Channel, ViewResponse> VIEW_RESPONSE_HOOK = new PluginHook<Channel, ViewResponse>(Channel.class, -1, ViewResponse.class, new Class<?>[] { ViewResponse.class });

    /**
   * A {@linkplain PluginHook hook} which allows a {@link Plugin} once last chance to
   * {@linkplain #pluginFilterHook(PluginHook, Object[], Page.Request, Object) filter} (ie,
   * {@linkplain org.wwweeeportal.portal.channelplugins.AbstractChannelAugmentation augment}) a
   * {@linkplain #doViewRequestImpl(Page.Request, ViewResponse) view} response before it's
   * {@linkplain #finalizeViewResponse(Page.Request, ViewResponse) finalized} and returned to the client.
   * 
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<Channel, ViewResponse> FINAL_VIEW_RESPONSE_HOOK = new PluginHook<Channel, ViewResponse>(Channel.class, -2, ViewResponse.class, new Class<?>[] { ViewResponse.class });

    /**
   * A {@linkplain PluginHook hook} which allows a {@link Plugin} an opportunity to
   * {@linkplain #pluginFilterHook(PluginHook, Object[], Page.Request, Object) filter} away an
   * {@linkplain WWWeeePortal.OperationalException operational exception} which occurred while generating the
   * {@linkplain #doViewRequestImpl(Page.Request, ViewResponse) view} (ie, if the response can still be rendered from a
   * {@linkplain org.wwweeeportal.portal.channelplugins.ChannelCache cache}).
   * 
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<Channel, WWWeeePortal.OperationalException> VIEW_EXCEPTION_HOOK = new PluginHook<Channel, WWWeeePortal.OperationalException>(Channel.class, -3, WWWeeePortal.OperationalException.class, new Class<?>[] { ViewResponse.class });

    /**
   * A {@linkplain PluginHook hook} which allows a {@link Plugin} to either
   * {@linkplain #pluginValueHook(PluginHook, Object[], Page.Request) provide} it's own {@link Response}, instead of the
   * <code>Channel</code> retrieving one {@linkplain #doResourceRequestImpl(Page.Request) normally}, or to
   * {@linkplain #pluginFilterHook(PluginHook, Object[], Page.Request, Object) filter} the normally provided value.
   * 
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<Channel, Response> RESOURCE_RESPONSE_HOOK = new PluginHook<Channel, Response>(Channel.class, -4, Response.class, null);

    /**
   * A {@linkplain PluginHook hook} which allows a {@link Plugin} once last chance to
   * {@linkplain #pluginFilterHook(PluginHook, Object[], Page.Request, Object) filter} a
   * {@linkplain #doResourceRequestImpl(Page.Request) resource} response before it's returned to the client.
   * 
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<Channel, Response> FINAL_RESOURCE_RESPONSE_HOOK = new PluginHook<Channel, Response>(Channel.class, -5, Response.class, null);

    /**
   * A {@linkplain PluginHook hook} which allows a {@link Plugin} an opportunity to
   * {@linkplain #pluginValueHook(PluginHook, Object[], Page.Request) provide} an alternate {@link Response} if an
   * {@linkplain WWWeeePortal.OperationalException operational exception} occurred while
   * {@linkplain #doResourceRequestImpl(Page.Request) retrieving} a resource.
   * 
   * @category WWWEEE_PORTAL_CHANNEL_PLUGIN_HOOK
   */
    public static final PluginHook<Channel, Response> RESOURCE_EXCEPTION_HOOK = new PluginHook<Channel, Response>(Channel.class, -6, Response.class, new Class<?>[] { WWWeeePortal.OperationalException.class });

    /**
   * The {@link WWWeeePortal} instance hosting this <code>Channel</code>.
   * 
   * @see #getPortal()
   */
    protected final WWWeeePortal portal;

    /**
   * The {@linkplain ContentManager.ChannelDefinition definition} for this <code>Channel</code>.
   * 
   * @see #getDefinition()
   */
    protected final ContentManager.ChannelDefinition<?, ?> definition;

    /**
   * The {@link Page} instance to which this <code>Channel</code> belongs (if
   * <em>{@linkplain ContentManager.LocalChannelDefinition local}</em>, else <code>null</code>).
   * 
   * @see #getPage()
   */
    protected final Page page;

    /**
   * The {@link Plugin} instances in effect for this <code>Channel</code>.
   */
    protected final List<Plugin> plugins;

    /**
   * Construct a new <code>Channel</code> instance.
   * 
   * @param portal The {@link #getPortal() WWWeeePortal} instance hosting this <code>Channel</code>.
   * @param definition The {@linkplain #getDefinition() definition} for this <code>Channel</code>.
   * @param page The {@link Page} instance to which this
   * <em>{@linkplain ContentManager.LocalChannelDefinition local}</em> <code>Channel</code> belongs, or
   * <code>null</code> if it's {@linkplain ContentManager.GlobalChannelDefinition global}.
   */
    protected Channel(final WWWeeePortal portal, final ContentManager.ChannelDefinition<?, ?> definition, final Page page) throws WWWeeePortal.Exception {
        if (portal == null) throw new IllegalArgumentException("null portal");
        if (definition == null) throw new IllegalArgumentException("null definition");
        if (!definition.isInitialized()) throw new IllegalStateException("Attempt to instantiate Channel from uninitialized definition");
        if ((page == null) && (!(definition instanceof ContentManager.GlobalChannelDefinition<?>))) throw new IllegalArgumentException("null page");
        this.portal = portal;
        this.definition = definition;
        this.page = page;
        plugins = portal.getContentManager().constructPlugins(this);
        return;
    }

    /**
   * Get the {@link WWWeeePortal} instance hosting this <code>Channel</code>.
   */
    public final WWWeeePortal getPortal() {
        return portal;
    }

    /**
   * Get the {@linkplain ContentManager.ChannelDefinition definition} for this <code>Channel</code>.
   */
    public final ContentManager.ChannelDefinition<?, ?> getDefinition() {
        return definition;
    }

    /**
   * <p>
   * Get the {@link Page} instance to which this <em>{@linkplain ContentManager.LocalChannelDefinition local}</em>
   * <code>Channel</code> belongs.
   * </p>
   * 
   * <p>
   * <strong>WARNING</strong>: This method <em>only</em> works for
   * <em>{@linkplain ContentManager.LocalChannelDefinition local}</em> channels, and only exists for special
   * circumstances - most code likely wants to be calling {@link Page.Request#getPage()}, which will <em>always</em>
   * return the {@link Page} instance targeted by that particular request.
   * </p>
   * 
   * @return The {@link Page} instance, if this is a {@linkplain ContentManager.LocalChannelDefinition local}
   * <code>Channel</code>, or <code>null</code> if it's {@linkplain ContentManager.GlobalChannelDefinition global}.
   * @see Page.Request#getPage()
   */
    public final Page getPage() {
        return page;
    }

    /**
   * {@linkplain ConfigManager#getConfigProp(RSProperties, String, SecurityContext, HttpHeaders, RSProperties.Entry, Converter, Object, boolean, boolean)
   * Get} the {@linkplain ContentManager.ChannelDefinition#getProperties() channel property} matching the specified
   * criteria.
   * 
   * @see ConfigManager#getConfigProp(RSProperties, String, SecurityContext, HttpHeaders, RSProperties.Entry, Converter,
   * Object, boolean, boolean)
   */
    public final <V> V getConfigProp(final String key, final Page.Request pageRequest, final Converter<RSProperties.Result, V> resultConverter, final V defaultConvertedValue, final boolean cacheValue, final boolean optional) throws IllegalArgumentException, ConfigManager.ConfigException {
        return ConfigManager.getConfigProp(definition.getProperties(), key, (pageRequest != null) ? pageRequest.getSecurityContext() : null, (pageRequest != null) ? pageRequest.getHttpHeaders() : null, null, resultConverter, defaultConvertedValue, cacheValue, optional);
    }

    /**
   * {@linkplain #getConfigProp(String, Page.Request, Converter, Object, boolean, boolean) Get} the
   * {@linkplain ContentManager.ChannelDefinition#getProperties() channel property} matching the specified criteria as a
   * {@link RSProperties#RESULT_STRING_CONVERTER String}.
   * 
   * @see #getConfigProp(String, Page.Request, Converter, Object, boolean, boolean)
   */
    public final String getConfigProp(final String key, final Page.Request pageRequest, final String defaultConvertedValue, final boolean optional) throws IllegalArgumentException, ConfigManager.ConfigException {
        return getConfigProp(key, pageRequest, RSProperties.RESULT_STRING_CONVERTER, defaultConvertedValue, false, optional);
    }

    /**
   * <p>
   * Get the title text String for this <code>Channel</code>.
   * </p>
   * 
   * <p>
   * When this <code>Channel</code> is {@linkplain Page.Request#isMaximized(Channel) maximized}, this value will
   * normally be displayed as part of each generated {@link Page}
   * <code>&lt;html&gt;&lt;head&gt;&lt;{@link Page#createHeadTitleElement(Page.Request, Element) title}&gt;</code>
   * element, and also possibly within the {@link Page} content as part of any configured
   * {@linkplain org.wwweeeportal.portal.channels.PageHeadingChannel page heading} or
   * {@link org.wwweeeportal.portal.channelplugins.ChannelTitle channel title}. This value is also used to
   * {@linkplain ViewResponse#setTitle(String) initialize} the {@link ViewResponse#getTitle() title} of each
   * {@link ViewResponse}.
   * </p>
   * 
   * <p>
   * If no explicit value {@link #TITLE_TEXT_PROP specified} then the
   * {@linkplain ContentManager.ChannelDefinition#getID() channel identifier} will be used.
   * </p>
   * 
   * @see #TITLE_TEXT_PROP
   */
    public String getTitleText(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        return getConfigProp(TITLE_TEXT_PROP, pageRequest, definition.getID(), false);
    }

    /**
   * @see #CACHE_CONTROL_CLIENT_DIRECTIVES_DISABLE_PROP
   */
    public boolean isCacheControlClientDirectivesDisabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(CACHE_CONTROL_CLIENT_DIRECTIVES_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    /**
   * @see #CACHE_CONTROL_DEFAULT_PROP
   */
    public CacheControl getCacheControlDefault() throws WWWeeePortal.Exception {
        return getConfigProp(CACHE_CONTROL_DEFAULT_PROP, null, RSProperties.RESULT_CACHE_CONTROL_CONVERTER, null, true, true);
    }

    /**
   * @see #MAXIMIZATION_DISABLE_PROP
   */
    public boolean isMaximizationDisabled(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        return getConfigProp(MAXIMIZATION_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    /**
   * <p>
   * Get the viewing priority value for this <code>Channel</code> when <em>not</em>
   * {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximized}.
   * </p>
   * 
   * <p>
   * If an explicit value is not {@linkplain #MAXIMIZATION_PRIORITY_NORMAL_PROP specified} then the
   * {@linkplain #MAXIMIZATION_PRIORITY_DEFAULT default} will be used.
   * </p>
   * 
   * <p>
   * When a <code>Channel</code> on a page is maximized, in order for any other <code>Channel</code>'s to also remain
   * viewable, their normal priority (this value) must be equal or greater than the
   * {@linkplain #getMaximizationMaximizedPriority(Page.Request) priority of the maximized channel}.
   * </p>
   * 
   * @see #MAXIMIZATION_PRIORITY_NORMAL_PROP
   * @see #MAXIMIZATION_PRIORITY_DEFAULT
   * @see #getMaximizationMaximizedPriority(Page.Request)
   * @see org.wwweeeportal.portal.Page.Request#isMaximized(Channel)
   */
    public int getMaximizationNormalPriority(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        return getConfigProp(MAXIMIZATION_PRIORITY_NORMAL_PROP, pageRequest, RSProperties.RESULT_INTEGER_CONVERTER, MAXIMIZATION_PRIORITY_DEFAULT, false, false).intValue();
    }

    /**
   * <p>
   * Get the viewing priority value for this <code>Channel</code> when
   * <em>{@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximized}</em>.
   * </p>
   * 
   * <p>
   * If an explicit value is not {@linkplain #MAXIMIZATION_PRIORITY_MAXIMIZED_PROP specified} then the value will be
   * calculated as the {@linkplain #getMaximizationNormalPriority(Page.Request) normal priority} with a
   * {@linkplain #MAXIMIZATION_PRIORITY_MULTIPLIER multiplier} applied.
   * </p>
   * 
   * <p>
   * When a <code>Channel</code> on a page is maximized, in order for any other <code>Channel</code>'s to also remain
   * viewable, their {@linkplain #getMaximizationNormalPriority(Page.Request) normal priority} must be equal or greater
   * than the priority of the maximized <code>Channel</code> (this value).
   * </p>
   * 
   * @see #MAXIMIZATION_PRIORITY_MAXIMIZED_PROP
   * @see #MAXIMIZATION_PRIORITY_MULTIPLIER
   * @see #getMaximizationNormalPriority(Page.Request)
   * @see org.wwweeeportal.portal.Page.Request#isMaximized(Channel)
   */
    public int getMaximizationMaximizedPriority(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        final Integer maximizationMaximizedPriority = getConfigProp(MAXIMIZATION_PRIORITY_MAXIMIZED_PROP, pageRequest, RSProperties.RESULT_INTEGER_CONVERTER, null, false, true);
        return (maximizationMaximizedPriority != null) ? maximizationMaximizedPriority.intValue() : getMaximizationNormalPriority(pageRequest) * MAXIMIZATION_PRIORITY_MULTIPLIER;
    }

    /**
   * <p>
   * Get the {@linkplain Element#getLocalName() local name} for the {@linkplain HTMLUtil#HTML_NS_URI HTML}5 section type
   * {@link Element} to be created as the {@linkplain ViewResponse#getResponseRootElement() root element} for this
   * {@link Channel} (see there for a more detailed markup description).
   * </p>
   * 
   * <p>
   * If an explicit value is not {@linkplain #SECTION_TYPE_PROP specified} then the {@linkplain #SECTION_TYPE_DEFAULT
   * default} will be used.
   * </p>
   * 
   * @see #SECTION_TYPE_PROP
   * @see #SECTION_TYPE_DEFAULT
   * @see ViewResponse#getResponseRootElement()
   * @see Channel
   */
    public String getSectionType(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        return getConfigProp(SECTION_TYPE_PROP, pageRequest, SECTION_TYPE_DEFAULT, false);
    }

    /**
   * {@linkplain Logger#getLogger(String) Get} the {@link Logger} for this <code>Channel</code> instance.
   * 
   * @return The {@link Logger} for this <code>Channel</code> instance.
   * @see Logger#getLogger(String)
   */
    protected final Logger getLogger() {
        return Logger.getLogger(getClass().getName());
    }

    /**
   * Put this <code>Channel</code> into a state where it is prepared to accept client requests.
   * 
   * @param logMessage A {@linkplain org.wwweeeportal.util.logging.LogAnnotation.Message message} which will be
   * {@linkplain LogAnnotation#log(Logger, LogAnnotation.Message, Class, Throwable) logged} ({@link Level#FINER}) after
   * initialization, which can be
   * {@linkplain LogAnnotation#annotate(LogAnnotation.Message, String, Object, Level, boolean) annotated} with any
   * informational notices the implementation wishes to have recorded in that log message.
   * @see #init()
   */
    protected void initInternal(final LogAnnotation.Message logMessage) throws WWWeeePortal.Exception {
        for (Plugin plugin : CollectionUtil.mkNotNull(plugins)) {
            plugin.init();
        }
        return;
    }

    /**
   * <p>
   * Put this <code>Channel</code> into a state where it is prepared to accept client requests.
   * </p>
   * 
   * <p>
   * This method must be called prior to any client requests being routed to the <code>Channel</code>, and will
   * {@linkplain Channel.Plugin#init() initialize} each of it's plugin instances. This method follows the same semantics
   * as {@link ContentManager#init()}.
   * </p>
   * 
   * @see Channel.Plugin#init()
   * @see #destroy()
   * @see ContentManager#init()
   */
    public final void init() throws WWWeeePortal.Exception {
        final LogAnnotation.Message logMessage = new LogAnnotation.MessageImpl(Level.FINER, "Initialized Channel: " + toString());
        try {
            initInternal(logMessage);
        } catch (WWWeeePortal.Exception wpe) {
            destroyInternal(wpe);
            throw wpe;
        }
        LogAnnotation.log(getLogger(), logMessage, getClass(), null);
        return;
    }

    /**
   * Take this <code>Channel</code> out of active service.
   * 
   * @param logMessage A {@linkplain org.wwweeeportal.util.logging.LogAnnotation.Message message} which will be
   * {@linkplain LogAnnotation#log(Logger, LogAnnotation.Message, Class, Throwable) logged} ({@link Level#FINER}) after
   * destruction, which can be
   * {@linkplain LogAnnotation#annotate(LogAnnotation.Message, String, Object, Level, boolean) annotated} with any
   * informational notices the implementation wishes to have recorded in that log message.
   * @see #destroy()
   */
    protected void destroyInternal(final LogAnnotation.Message logMessage) {
        for (Plugin plugin : CollectionUtil.mkNotNull(plugins)) {
            if (plugin == null) continue;
            try {
                plugin.destroy();
            } catch (Exception e) {
            }
        }
        return;
    }

    /**
   * <p>
   * Take this <code>Channel</code> out of active service.
   * </p>
   * 
   * <p>
   * This method will {@linkplain Channel.Plugin#destroy() destroy} each of it's plugin instances. This method follows
   * the same semantics as {@link ContentManager#destroy()}.
   * </p>
   * 
   * @see Channel.Plugin#destroy()
   * @see #init()
   * @see ContentManager#destroy()
   */
    public final void destroy() {
        final LogAnnotation.Message logMessage = new LogAnnotation.MessageImpl(Level.FINER, "Destroyed Channel: " + toString());
        destroyInternal(logMessage);
        LogAnnotation.log(getLogger(), logMessage, getClass(), null);
        return;
    }

    /**
   * <p>
   * Populate the supplied <code>metaProps</code> Map with the set of meta-information about this <code>Channel</code>.
   * </p>
   * 
   * <p>
   * This method will include information from this <code>Channel</code> itself, but also from it's definition. If this
   * is a {@linkplain ContentManager.LocalChannelDefinition local channel}, then the
   * {@linkplain ContentManager.LocalChannelDefinition#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
   * definition},
   * {@linkplain ContentManager.ChannelSpecification#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
   * channel specification}, and
   * {@linkplain ContentManager.ChannelGroupDefinition#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
   * channel group} properties will be included. If this is a {@linkplain ContentManager.GlobalChannelDefinition global
   * channel}, then the
   * {@linkplain ContentManager.GlobalChannelDefinition#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
   * definition} and
   * {@linkplain ContentManager.GlobalChannelContentContainer#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
   * container} properties will be included.
   * </p>
   * 
   * <p>
   * The following properties are provided for the <code>Channel</code> itself:
   * </p>
   * <dl>
   * <dt><code>WWWeee.Request.ChannelLocalPath</code></dt>
   * <dd>A {@link java.net.URI} containing the {@linkplain Page.Request#getChannelLocalPath(Channel) channel local path}
   * .</dd>
   * <dt><code>WWWeee.Channel.Maximized</code></dt>
   * <dd>A {@link Boolean} telling if the <code>Channel</code> is {@linkplain Page.Request#isMaximized(Channel)
   * maximized}.</dd>
   * <dt><code>WWWeee.Channel</code></dt>
   * <dd>The {@link Channel} instance.</dd>
   * </dl>
   * 
   * @see ContentManager.LocalChannelDefinition#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
   * @see ContentManager.ChannelSpecification#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
   * @see ContentManager.ChannelGroupDefinition#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
   * @see ContentManager.GlobalChannelDefinition#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
   * @see ContentManager.GlobalChannelContentContainer#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders,
   * int)
   */
    public Map<String, Object> getMetaProps(final Page.Request pageRequest, final Map<String, Object> metaProps) throws IllegalArgumentException, ConfigManager.ConfigException {
        if (metaProps == null) throw new IllegalArgumentException("null metaProps");
        definition.getMetaProps(pageRequest, metaProps, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), (definition instanceof ContentManager.LocalChannelDefinition) ? 2 : 1);
        metaProps.put("WWWeee.Request.ChannelLocalPath", pageRequest.getChannelLocalPath(this));
        metaProps.put("WWWeee.Channel.Maximized", Boolean.valueOf(pageRequest.isMaximized(this)));
        metaProps.put("WWWeee.Channel", this);
        return metaProps;
    }

    /**
   * @see #LINK_BY_NUM_PROP
   * @see #LINK_BY_NUM_PATTERN
   * @see #LINK_BY_PATH_PROP
   * @see #LINK_BY_PATH_PATTERN
   * @see #LINK_BY_PATH_CONVERTER
   * @see MarkupManager#PROP_RESULT_LINK_ELEMENT_CONVERTER
   * @see MarkupManager#PROP_RESULT_ENTRY_LINK_ELEMENT_CONVERTER
   * @see ConfigManager.RegexPropKeyConverter
   * @see Page#getMetaLinkElements(Page.Request)
   * @see ViewResponse#addMetaElement(Element)
   */
    protected Collection<Element> getMetaLinkElements(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        final ArrayList<Element> metaLinkElements = new ArrayList<Element>();
        CollectionUtil.addAll(metaLinkElements, ConfigManager.getConfigProps(definition.getProperties(), LINK_BY_NUM_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), MarkupManager.PROP_RESULT_LINK_ELEMENT_CONVERTER, true, true).values());
        CollectionUtil.addAll(metaLinkElements, CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), LINK_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), LINK_BY_PATH_CONVERTER, true, true), null, false, StringUtil.toString(pageRequest.getChannelLocalPath(this), null))));
        return (!metaLinkElements.isEmpty()) ? metaLinkElements : null;
    }

    /**
   * @see #SCRIPT_BY_NUM_PROP
   * @see #SCRIPT_BY_NUM_PATTERN
   * @see #SCRIPT_BY_PATH_PROP
   * @see #SCRIPT_BY_PATH_PATTERN
   * @see #SCRIPT_BY_PATH_CONVERTER
   * @see MarkupManager#PROP_RESULT_SCRIPT_ELEMENT_CONVERTER
   * @see MarkupManager#PROP_RESULT_ENTRY_SCRIPT_ELEMENT_CONVERTER
   * @see ConfigManager.RegexPropKeyConverter
   * @see Page#getMetaScriptElements(Page.Request)
   * @see ViewResponse#addMetaElement(Element)
   */
    protected Collection<Element> getMetaScriptElements(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        final ArrayList<Element> metaScriptElements = new ArrayList<Element>();
        CollectionUtil.addAll(metaScriptElements, ConfigManager.getConfigProps(definition.getProperties(), SCRIPT_BY_NUM_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), MarkupManager.PROP_RESULT_SCRIPT_ELEMENT_CONVERTER, true, true).values());
        CollectionUtil.addAll(metaScriptElements, CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), SCRIPT_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), SCRIPT_BY_PATH_CONVERTER, true, true), null, false, StringUtil.toString(pageRequest.getChannelLocalPath(this), null))));
        return (!metaScriptElements.isEmpty()) ? metaScriptElements : null;
    }

    /**
   * @see #META_BY_NUM_PROP
   * @see #META_BY_NUM_PATTERN
   * @see #META_BY_PATH_PROP
   * @see #META_BY_PATH_PATTERN
   * @see #META_BY_PATH_CONVERTER
   * @see MarkupManager#PROP_RESULT_META_ELEMENT_CONVERTER
   * @see MarkupManager#PROP_RESULT_ENTRY_META_ELEMENT_CONVERTER
   * @see ConfigManager.RegexPropKeyConverter
   * @see Page#getMetaMetaElements(Page.Request)
   * @see ViewResponse#addMetaElement(Element)
   */
    protected Collection<Element> getMetaMetaElements(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        final ArrayList<Element> metaMetaElements = new ArrayList<Element>();
        CollectionUtil.addAll(metaMetaElements, ConfigManager.getConfigProps(definition.getProperties(), META_BY_NUM_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), MarkupManager.PROP_RESULT_META_ELEMENT_CONVERTER, true, true).values());
        CollectionUtil.addAll(metaMetaElements, CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), META_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), META_BY_PATH_CONVERTER, true, true), null, false, StringUtil.toString(pageRequest.getChannelLocalPath(this), null))));
        return (!metaMetaElements.isEmpty()) ? metaMetaElements : null;
    }

    /**
   * Set a variety of useful ID and Class attributes on the given element. Only use this if the given set of
   * <code>channelIDComponents</code> occurs once in the <code>Channel</code>!
   */
    public final void setIDAndClassAttrs(final Element element, final List<String> channelIDComponents, final String[][] extraMarkupEncodedClasses, final Collection<String> extraClasses) throws ConfigManager.ConfigException {
        final String portalID = portal.getPortalID();
        final ArrayList<String> idComponents = new ArrayList<String>();
        idComponents.add(portalID);
        idComponents.add("channel");
        if (channelIDComponents != null) idComponents.addAll(channelIDComponents);
        idComponents.add(definition.getID());
        final String idValue = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, idComponents);
        DOMUtil.createAttribute(null, null, "id", idValue, element);
        final StringBuffer classBuffer = new StringBuffer();
        final ArrayList<String> coreClassComponents = new ArrayList<String>();
        coreClassComponents.add(portalID);
        coreClassComponents.add("channel");
        if (channelIDComponents != null) coreClassComponents.addAll(channelIDComponents);
        classBuffer.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, coreClassComponents));
        if (extraMarkupEncodedClasses != null) {
            for (String[] extraClassComponents : extraMarkupEncodedClasses) {
                if (extraClassComponents == null) continue;
                classBuffer.append(' ');
                classBuffer.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(extraClassComponents)));
            }
        }
        if (extraClasses != null) {
            for (String extraClass : extraClasses) {
                classBuffer.append(' ');
                classBuffer.append(extraClass);
            }
        }
        final Attr existingClass = DOMUtil.getAttr(element, null, "class", null);
        if (existingClass != null) {
            classBuffer.append(' ');
            classBuffer.append(existingClass.getValue());
        }
        DOMUtil.createAttribute(null, null, "class", classBuffer.toString(), element);
        return;
    }

    /**
   * Create an {@link Element} containing the content to be returned in response to an
   * {@linkplain WWWeeePortal.Exception exception} occurring during a request to
   * {@linkplain #doViewRequest(Page.Request, Node) view} this <code>Channel</code>.
   */
    protected Element createExceptionElement(final Page.Request pageRequest, final ViewResponse viewResponse, final WWWeeePortal.Exception wpe) throws ConfigManager.ConfigException {
        final ResourceBundle wwweeeResourceBundle = WWWeeePortal.getPortalResourceBundle(pageRequest.getHttpHeaders());
        final Element responseContentContainerElement = viewResponse.getContentContainerElement();
        DOMUtil.removeChildren(responseContentContainerElement);
        final Element exceptionElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "div", responseContentContainerElement);
        setIDAndClassAttrs(exceptionElement, Arrays.asList("exception"), null, null);
        final Element exceptionHeadingElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "h3", exceptionElement);
        setIDAndClassAttrs(exceptionHeadingElement, Arrays.asList("exception", "heading"), null, null);
        DOMUtil.appendText(exceptionHeadingElement, wwweeeResourceBundle.getString(wpe.getHeadingResourceKey()));
        final Element exceptionUserMessageElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "div", exceptionElement);
        setIDAndClassAttrs(exceptionHeadingElement, Arrays.asList("exception", "message"), null, null);
        DOMUtil.appendText(exceptionUserMessageElement, wwweeeResourceBundle.getString(wpe.getUserMessageResourceKey()));
        return exceptionElement;
    }

    /**
   * Create {@linkplain ViewResponse#getResponseRootElement() root} and
   * {@linkplain ViewResponse#getContentContainerElement() content container} Elements under the specified
   * <code>responseContainerNode</code>, and use them to construct the {@link ViewResponse} to be returned for the
   * supplied <code>pageRequest</code> to {@linkplain #doViewRequest(Page.Request, Node) view} this <code>Channel</code>
   * .
   */
    protected ViewResponse createViewResponse(final Page.Request pageRequest, final Node responseContainerNode) throws WWWeeePortal.Exception {
        final String sectionType = getSectionType(pageRequest);
        final boolean isHTML5Supported = portal.isHTML5Supported(pageRequest.getSecurityContext(), pageRequest.getHttpHeaders());
        final Element responseRootElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, ((isHTML5Supported) && (sectionType != null)) ? sectionType : "div", responseContainerNode, DOMUtil.getDocument(responseContainerNode), true, false);
        final String[][] extraRootMarkupEncodedClasses = new String[][] { (pageRequest.isMaximized(this)) ? new String[] { portal.getPortalID(), "channel", "maximized" } : null, ((isHTML5Supported) || (sectionType == null)) ? new String[] { sectionType } : null };
        final Collection<String> extraRootClasses = ConfigManager.getConfigProps(definition.getProperties(), CLASS_BY_NUM_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RSProperties.RESULT_STRING_CONVERTER, false, false).values();
        setIDAndClassAttrs(responseRootElement, null, extraRootMarkupEncodedClasses, extraRootClasses);
        final Element bodyElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "div", responseRootElement);
        setIDAndClassAttrs(bodyElement, Arrays.asList("body"), null, null);
        final Element contentContainerElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "div", bodyElement);
        setIDAndClassAttrs(contentContainerElement, Arrays.asList("content"), null, null);
        final ViewResponse viewResponse = new ViewResponse(new GregorianCalendar(DateUtil.UTC_TIME_ZONE).getTime(), responseContainerNode, responseRootElement, contentContainerElement);
        viewResponse.setCacheControl(getCacheControlDefault());
        viewResponse.setTitle(getTitleText(pageRequest));
        for (Element metaLinkElement : CollectionUtil.mkNotNull(getMetaLinkElements(pageRequest))) {
            viewResponse.addMetaElement(MarkupManager.resolveMetaLinkElementContextResources(metaLinkElement, pageRequest.getUriInfo()));
        }
        for (Element metaScriptElement : CollectionUtil.mkNotNull(getMetaScriptElements(pageRequest))) {
            viewResponse.addMetaElement(MarkupManager.resolveMetaScriptElementContextResources(metaScriptElement, pageRequest.getUriInfo()));
        }
        for (Element metaMetaElement : CollectionUtil.mkNotNull(getMetaMetaElements(pageRequest))) {
            viewResponse.addMetaElement(MarkupManager.resolveMetaMetaElementContextResources(metaMetaElement, pageRequest.getUriInfo()));
        }
        return viewResponse;
    }

    /**
   * Perform any {@linkplain #FINAL_VIEW_RESPONSE_HOOK final} post-
   * {@linkplain #doViewRequestImpl(Page.Request, ViewResponse) rendering} modifications on the
   * <code>viewResponse</code> resulting from the supplied <code>pageRequest</code> to
   * {@linkplain #doViewRequest(Page.Request, Node) view} this <code>Channel</code>.
   */
    protected void finalizeViewResponse(final Page.Request pageRequest, final ViewResponse viewResponse) throws WWWeeePortal.Exception {
        DOMUtil.setXMLLangAttr(viewResponse.getResponseRootElement(), viewResponse.getLocale());
        return;
    }

    /**
   * <p>
   * Render the incoming <code>pageRequest</code> into the provided <code>viewResponse</code> container.
   * </p>
   * 
   * <p>
   * Any thrown exceptions will be trapped and
   * {@linkplain #createExceptionElement(Page.Request, ViewResponse, WWWeeePortal.Exception) rendered} into the
   * response. The exception to this rule is the {@link WebApplicationException}, which will be passed through if this
   * <code>Channel</code> is {@linkplain Page.Request#isMaximized(Channel) maximized}.
   * </p>
   * 
   * @see #doViewRequest(Page.Request, Node)
   */
    protected abstract void doViewRequestImpl(final Page.Request pageRequest, final ViewResponse viewResponse) throws WWWeeePortal.Exception, WebApplicationException;

    /**
   * <p>
   * Perform a request to {@linkplain #VIEW_MODE view} this <code>Channel</code> instance.
   * </p>
   * 
   * <p>
   * Most functionality within this class is to support the implementation of this method. This method will
   * {@linkplain #createViewResponse(Page.Request, Node) create} the <code>Channel</code> response container,
   * {@linkplain #doViewRequestImpl(Page.Request, ViewResponse) render} the <code>Channel</code> into it,
   * {@linkplain #finalizeViewResponse(Page.Request, ViewResponse) finalize} the result document, and return it to the
   * <code>Page</code>.
   * </p>
   * 
   * @see #VIEW_MODE
   * @see Page#doViewRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map, DataSource,
   * ContentManager.ChannelSpecification.Key)
   * @see #doResourceRequest(Page.Request)
   * @see #VIEW_RESPONSE_HOOK
   * @see #FINAL_VIEW_RESPONSE_HOOK
   * @see #VIEW_EXCEPTION_HOOK
   */
    final ViewResponse doViewRequest(final Page.Request pageRequest, final Node responseContainerNode) throws WWWeeePortal.Exception, WebApplicationException {
        try {
            if ((page != null) && (pageRequest.getPage() != page)) throw new IllegalArgumentException("Attempt to have local Channel handle request for mismatched Page instance");
            final ViewResponse viewResponse = createViewResponse(pageRequest, responseContainerNode);
            final Object[] viewResponseHookContext = new Object[] { viewResponse };
            try {
                try {
                    if ((pageRequest.isMaximized(this)) && (isMaximizationDisabled(pageRequest))) {
                        throw new WebApplicationException(Response.Status.FORBIDDEN);
                    }
                    final ViewResponse viewHandled = pluginValueHook(VIEW_RESPONSE_HOOK, viewResponseHookContext, pageRequest);
                    if (viewHandled == null) {
                        doViewRequestImpl(pageRequest, viewResponse);
                        pluginFilterHook(VIEW_RESPONSE_HOOK, viewResponseHookContext, pageRequest, viewResponse);
                    }
                } catch (WebApplicationException wae) {
                    if (pageRequest.isMaximized(this)) {
                        throw wae;
                    }
                    throw new WWWeeePortal.SoftwareException(wae);
                } catch (WWWeeePortal.OperationalException wpoe) {
                    wpoe = pluginFilterHook(VIEW_EXCEPTION_HOOK, viewResponseHookContext, pageRequest, wpoe);
                    if (wpoe != null) {
                        throw wpoe;
                    }
                } catch (WWWeeePortal.Exception wpe) {
                    throw wpe;
                } catch (Exception e) {
                    throw new WWWeeePortal.SoftwareException(e);
                }
            } catch (WWWeeePortal.Exception wpe) {
                createExceptionElement(pageRequest, viewResponse, LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", pageRequest.getSecurityContext(), null, false), "UriInfo", pageRequest.getUriInfo(), null, false), "Channel", this, null, false), getClass(), wpe));
            }
            pluginFilterHook(FINAL_VIEW_RESPONSE_HOOK, viewResponseHookContext, pageRequest, viewResponse);
            finalizeViewResponse(pageRequest, viewResponse);
            return viewResponse;
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", pageRequest.getSecurityContext(), null, false), "UriInfo", pageRequest.getUriInfo(), null, false), "Channel", this, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", pageRequest.getSecurityContext(), null, false), "UriInfo", pageRequest.getUriInfo(), null, false), "Channel", this, null, false), getClass(), e);
        }
    }

    /**
   * Perform a {@link GET} request to {@linkplain #VIEW_MODE view} this <code>Channel</code> instance.
   * 
   * @see Page#doViewRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map, DataSource,
   * ContentManager.ChannelSpecification.Key)
   * @see #doViewRequest(Page.Request, Node)
   */
    @GET
    @Path(VIEW_MODE + "{ChannelLocalPath:.*}")
    @Produces("application/xhtml+xml,*/*")
    public final Response doViewGetRequest(@Context final Request rsRequest, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders, @RequestAttributes final Map<String, Object> requestAttributes, @SessionAttributes final Map<String, Object> sessionAttributes) throws WWWeeePortal.Exception, WebApplicationException {
        final Page page = (Page) uriInfo.getMatchedResources().get(2);
        return page.doViewRequest(rsRequest, uriInfo, securityContext, httpHeaders, requestAttributes, sessionAttributes, null, ContentManager.ChannelSpecification.getKey(page.getDefinition().getChannelSpecification(definition.getID())));
    }

    /**
   * Perform a {@link POST} request to {@linkplain #VIEW_MODE view} this <code>Channel</code> instance.
   * 
   * @see Page#doViewRequest(Request, UriInfo, SecurityContext, HttpHeaders, Map, Map, DataSource,
   * ContentManager.ChannelSpecification.Key)
   * @see #doViewRequest(Page.Request, Node)
   */
    @POST
    @Path(VIEW_MODE + "{ChannelLocalPath:.*}")
    @Produces("application/xhtml+xml,*/*")
    public final Response doViewPostRequest(@Context final Request rsRequest, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders, @RequestAttributes final Map<String, Object> requestAttributes, @SessionAttributes final Map<String, Object> sessionAttributes, final DataSource entity) throws WWWeeePortal.Exception, WebApplicationException {
        final Page page = (Page) uriInfo.getMatchedResources().get(2);
        return page.doViewRequest(rsRequest, uriInfo, securityContext, httpHeaders, requestAttributes, sessionAttributes, entity, ContentManager.ChannelSpecification.getKey(page.getDefinition().getChannelSpecification(definition.getID())));
    }

    /**
   * Perform a request to retrieve a {@linkplain #RESOURCE_MODE resource} from this <code>Channel</code>.
   * 
   * @see #doResourceRequest(Page.Request)
   */
    protected Response doResourceRequestImpl(final Page.Request pageRequest) throws WWWeeePortal.Exception, WebApplicationException {
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    /**
   * <p>
   * Perform a request to retrieve a {@linkplain #RESOURCE_MODE resource} from this <code>Channel</code>.
   * </p>
   * 
   * @see #RESOURCE_MODE
   * @see #doViewRequest(Page.Request, Node)
   * @see #RESOURCE_RESPONSE_HOOK
   * @see #FINAL_RESOURCE_RESPONSE_HOOK
   * @see #RESOURCE_EXCEPTION_HOOK
   */
    final Response doResourceRequest(final Page.Request pageRequest) throws WWWeeePortal.Exception, WebApplicationException {
        pageRequest.getPage().activeRequestLock.readLock().lock();
        try {
            if (!pageRequest.getPage().initialized) throw new IllegalStateException("Attempt to have uninitialized Page handle a request");
            if ((page != null) && (pageRequest.getPage() != page)) throw new IllegalArgumentException("Attempt to have local Channel handle request for mismatched Page instance");
            final Response noAccessResponse = pageRequest.checkAccess();
            if (noAccessResponse != null) {
                return noAccessResponse;
            } else if ((definition.getAccessControl() != null) && (!definition.getAccessControl().hasAccess(pageRequest.getSecurityContext()))) {
                return pageRequest.createNoAccessResponse(definition.getProperties());
            }
            Response resourceResponse = null;
            try {
                resourceResponse = pluginValueHook(RESOURCE_RESPONSE_HOOK, null, pageRequest);
                if (resourceResponse == null) {
                    resourceResponse = doResourceRequestImpl(pageRequest);
                    pluginFilterHook(RESOURCE_RESPONSE_HOOK, null, pageRequest, resourceResponse);
                }
            } catch (WebApplicationException wae) {
                throw wae;
            } catch (WWWeeePortal.OperationalException wpoe) {
                final Response exceptionResponse = pluginValueHook(RESOURCE_EXCEPTION_HOOK, new Object[] { wpoe }, pageRequest);
                if (exceptionResponse == null) {
                    throw wpoe;
                }
                resourceResponse = exceptionResponse;
            } catch (WWWeeePortal.Exception wpe) {
                throw wpe;
            } catch (Exception e) {
                throw new WWWeeePortal.SoftwareException(e);
            }
            pluginFilterHook(FINAL_RESOURCE_RESPONSE_HOOK, null, pageRequest, resourceResponse);
            return resourceResponse;
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (WWWeeePortal.Exception wpe) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(LogAnnotation.annotate(wpe, "SecurityContext", pageRequest.getSecurityContext(), null, false), "UriInfo", pageRequest.getUriInfo(), null, false), "Channel", this, null, false), getClass(), wpe);
        } catch (Exception e) {
            throw LogAnnotation.log(getLogger(), LogAnnotation.annotate(LogAnnotation.annotate(LogAnnotation.annotate(new WWWeeePortal.SoftwareException(e), "SecurityContext", pageRequest.getSecurityContext(), null, false), "UriInfo", pageRequest.getUriInfo(), null, false), "Channel", this, null, false), getClass(), e);
        } finally {
            pageRequest.getPage().activeRequestLock.readLock().unlock();
        }
    }

    /**
   * Perform a {@link GET} request to retrieve a {@linkplain #RESOURCE_MODE resource} from this <code>Channel</code>.
   * 
   * @see #doResourceRequest(Page.Request)
   */
    @GET
    @Path(RESOURCE_MODE + "{ChannelLocalPath:.*}")
    public final Response doResourceGetRequest(@Context final Request rsRequest, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders, @RequestAttributes final Map<String, Object> requestAttributes, @SessionAttributes final Map<String, Object> sessionAttributes) throws WWWeeePortal.Exception, WebApplicationException {
        final Page page = (Page) uriInfo.getMatchedResources().get(2);
        return doResourceRequest(page.new Request(rsRequest, uriInfo, securityContext, httpHeaders, requestAttributes, sessionAttributes, null, page, ContentManager.ChannelSpecification.getKey(page.getDefinition().getChannelSpecification(definition.getID())), null));
    }

    /**
   * Perform a {@link POST} request to retrieve a {@linkplain #RESOURCE_MODE resource} from this <code>Channel</code>.
   * 
   * @see #doResourceRequest(Page.Request)
   */
    @POST
    @Path(RESOURCE_MODE + "{ChannelLocalPath:.*}")
    public final Response doResourcePostRequest(@Context final Request rsRequest, @Context final UriInfo uriInfo, @Context final SecurityContext securityContext, @Context final HttpHeaders httpHeaders, @RequestAttributes final Map<String, Object> requestAttributes, @SessionAttributes final Map<String, Object> sessionAttributes, final DataSource entity) throws WWWeeePortal.Exception, WebApplicationException {
        final Page page = (Page) uriInfo.getMatchedResources().get(2);
        return doResourceRequest(page.new Request(rsRequest, uriInfo, securityContext, httpHeaders, requestAttributes, sessionAttributes, entity, page, ContentManager.ChannelSpecification.getKey(page.getDefinition().getChannelSpecification(definition.getID())), null));
    }

    @Override
    public final boolean equals(final Object object) {
        if (object == null) return false;
        if (object == this) return true;
        if (!(object instanceof Channel)) return false;
        return definition.equals(((Channel) object).getDefinition());
    }

    @Override
    public final int hashCode() {
        return definition.hashCode();
    }

    @Override
    public final String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append('@');
        sb.append(Integer.toHexString(System.identityHashCode(this)).toUpperCase());
        sb.append('(');
        sb.append(definition);
        sb.append(')');
        return sb.toString();
    }

    /**
   * <p>
   * Create an opportunity for a {@link Channel.Plugin Plugin} to intercept regular <code>Channel</code> operation and
   * provide a value for the specified {@linkplain PluginHook hook}.
   * </p>
   * 
   * <p>
   * Each defined plugin will be called in order, and the first provided value will be returned.
   * </p>
   * 
   * @see PluginHook
   * @see Plugin#pluginValueHook(PluginHook, Object[], Page.Request)
   */
    protected final <T> T pluginValueHook(final PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest) throws WWWeeePortal.Exception {
        pluginHook.verifyContext(context);
        for (Plugin plugin : CollectionUtil.mkNotNull(plugins)) {
            if ((plugin.getDefinition().getAccessControl() != null) && (!plugin.getDefinition().getAccessControl().hasAccess(pageRequest.getSecurityContext()))) continue;
            final T result = plugin.pluginValueHook(pluginHook, context, pageRequest);
            if (result != null) {
                return pluginHook.getResultClass().cast(result);
            }
        }
        return null;
    }

    /**
   * <p>
   * Create an opportunity for a {@link Channel.Plugin Plugin} to intercept regular <code>Channel</code> operation and
   * filter a value for the specified {@linkplain PluginHook hook}.
   * </p>
   * 
   * <p>
   * Each defined plugin will be called in order, and the value returned from a previous plugin will be provided to the
   * next.
   * </p>
   * 
   * @see PluginHook
   * @see Plugin#pluginFilterHook(PluginHook, Object[], Page.Request, Object)
   */
    protected final <T> T pluginFilterHook(final PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest, final T data) throws WWWeeePortal.Exception {
        pluginHook.verifyContext(context);
        T result = data;
        for (Plugin plugin : CollectionUtil.mkNotNull(plugins)) {
            if ((plugin.getDefinition().getAccessControl() != null) && (!plugin.getDefinition().getAccessControl().hasAccess(pageRequest.getSecurityContext()))) continue;
            result = plugin.pluginFilterHook(pluginHook, context, pageRequest, result);
        }
        return pluginHook.getResultClass().cast(result);
    }

    /**
   * <p>
   * Encapsulates the info set for a response to a {@linkplain Channel#doViewRequest(Page.Request, Node) view request}.
   * </p>
   * 
   * <p>
   * This class is <code>static</code>, {@link Serializable}, and completely independent of the {@link Page.Request}, so
   * that it may be stored and cached.
   * </p>
   */
    public static final class ViewResponse implements Serializable {

        /**
     * @see Serializable
     */
        private static final long serialVersionUID = 1L;

        /**
     * @see #getDate()
     */
        protected final Date date;

        /**
     * @see #getResponseContainerNode()
     */
        protected final Node responseContainerNode;

        /**
     * @see #getResponseRootElement()
     */
        protected final Element responseRootElement;

        /**
     * @see #getContentContainerElement()
     */
        protected final Element contentContainerElement;

        /**
     * @see #getContentType()
     */
        protected transient MediaType contentType = null;

        /**
     * @see #getLastModified()
     */
        protected Date lastModified = null;

        /**
     * @see #getExpires()
     */
        protected Date expires = null;

        /**
     * @see #getCacheControl()
     */
        protected transient CacheControl cacheControl = null;

        /**
     * @see #getEntityTag()
     */
        protected transient EntityTag entityTag = null;

        /**
     * @see #getLocale()
     */
        protected Locale locale = null;

        /**
     * @see #getTitle()
     */
        protected String title;

        /**
     * @see #getMetaElements()
     */
        protected List<Element> metaElements;

        /**
     * Construct a new <code>Channel.ViewResponse</code> instance.
     */
        public ViewResponse(final Date date, final Node responseContainerNode, final Element responseRootElement, final Element contentContainerElement) throws IllegalArgumentException {
            if (date == null) throw new IllegalArgumentException("null date");
            if (responseContainerNode == null) throw new IllegalArgumentException("null responseContainerNode");
            this.date = date;
            this.responseContainerNode = responseContainerNode;
            this.responseRootElement = responseRootElement;
            this.contentContainerElement = contentContainerElement;
            return;
        }

        /**
     * Get the date/time/stamp this <code>Response</code> was made.
     * 
     * @return A {@link Date} representing the date/time/stamp this <code>Response</code> was made.
     */
        public Date getDate() {
            return date;
        }

        /**
     * <p>
     * Get the container for the <code>Channel</code>'s {@linkplain #getResponseRootElement() root element}.
     * </p>
     * 
     * <p>
     * This element is <em>not</em> part of the <code>Channel</code> itself (it's normally the <code>&lt;div&gt;</code>
     * element associated with the {@linkplain ContentManager.ChannelGroupDefinition channel group}), and is the value
     * which was provided by the {@link Page} (see there for a more detailed markup description) when calling
     * {@link Channel#doViewRequest(Page.Request, Node)}.
     * </p>
     * 
     * @see Page
     * @see Channel
     */
        public Node getResponseContainerNode() {
            return responseContainerNode;
        }

        /**
     * <p>
     * Get the outer root element for this <code>Channel</code>.
     * </p>
     * 
     * <p>
     * This element defines the top-most {@linkplain Channel#getSectionType(Page.Request) section} of the {@link Page}
     * which is specific to containing <em>this</em> <code>Channel</code> and it's content, and will likely be
     * configured to contain additional <code>Channel</code> header and/or footer
     * {@linkplain org.wwweeeportal.portal.channelplugins.AbstractChannelAugmentation augmentations}, but by default
     * will simply contain a single intermediate <code>Channel</code> &quot;body&quot; <code>&lt;div&gt;</code> element,
     * which, in turn, will host the inner {@linkplain Channel.ViewResponse#getContentContainerElement() content
     * container} Element for this {@link Channel} (see there for a more detailed markup description).
     * </p>
     * 
     * @see Channel#getSectionType(Page.Request)
     * @see Channel
     */
        public Element getResponseRootElement() {
            return responseRootElement;
        }

        /**
     * Get the element within the {@linkplain #getResponseRootElement() response} which will act as the immediate
     * {@linkplain Node#getParentNode() parent} to any content generated by this {@link Channel} (see there for markup
     * details).
     * 
     * @see #getResponseRootElement()
     * @see Channel
     */
        public Element getContentContainerElement() {
            return contentContainerElement;
        }

        /**
     * Get the {@link MediaType} for the {@linkplain javax.ws.rs.core.Response.ResponseBuilder#type(MediaType) type} of
     * content within this <code>Channel.ViewResponse</code>.
     * 
     * @see #setContentType(MediaType)
     */
        public synchronized MediaType getContentType() {
            return contentType;
        }

        /**
     * Set the {@link MediaType} for the {@linkplain javax.ws.rs.core.Response.ResponseBuilder#type(MediaType) type} of
     * content within this <code>Channel.ViewResponse</code>.
     * 
     * @see #getContentType()
     */
        public synchronized void setContentType(final MediaType contentType) {
            this.contentType = contentType;
            return;
        }

        /**
     * Get the {@link Date} the content within this <code>Channel.ViewResponse</code> was
     * {@linkplain javax.ws.rs.core.Response.ResponseBuilder#lastModified(Date) last modified}.
     * 
     * @see #setLastModified(Date)
     */
        public synchronized Date getLastModified() {
            return lastModified;
        }

        /**
     * Set the {@link Date} the content within this <code>Channel.ViewResponse</code> was
     * {@linkplain javax.ws.rs.core.Response.ResponseBuilder#lastModified(Date) last modified}.
     * 
     * @see #getLastModified()
     */
        public synchronized void setLastModified(final Date lastModified) {
            this.lastModified = lastModified;
            return;
        }

        /**
     * Get the {@link Date} the content within this <code>Channel.ViewResponse</code>
     * {@linkplain javax.ws.rs.core.Response.ResponseBuilder#expires(Date) expires}.
     * 
     * @see #setExpires(Date)
     */
        public synchronized Date getExpires() {
            return expires;
        }

        /**
     * Set the {@link Date} the content within this <code>Channel.ViewResponse</code>
     * {@linkplain javax.ws.rs.core.Response.ResponseBuilder#expires(Date) expires}.
     * 
     * @see #getExpires()
     */
        public synchronized void setExpires(final Date expires) {
            this.expires = expires;
            return;
        }

        /**
     * Get the {@link CacheControl} object for the
     * {@linkplain javax.ws.rs.core.Response.ResponseBuilder#cacheControl(CacheControl) cache-control} of the content
     * within this <code>Channel.ViewResponse</code>.
     * 
     * @see #setCacheControl(CacheControl)
     */
        public synchronized CacheControl getCacheControl() {
            return cacheControl;
        }

        /**
     * Set the {@link CacheControl} object for the
     * {@linkplain javax.ws.rs.core.Response.ResponseBuilder#cacheControl(CacheControl) cache-control} of the content
     * within this <code>Channel.ViewResponse</code>.
     * 
     * @see #getCacheControl()
     * @see Channel#getCacheControlDefault()
     */
        public synchronized void setCacheControl(final CacheControl cacheControl) {
            this.cacheControl = cacheControl;
            return;
        }

        /**
     * Get the {@link EntityTag} object used to {@linkplain javax.ws.rs.core.Response.ResponseBuilder#tag(EntityTag)
     * tag} the content within this <code>Channel.ViewResponse</code>.
     * 
     * @see #setEntityTag(EntityTag)
     */
        public synchronized EntityTag getEntityTag() {
            return entityTag;
        }

        /**
     * Set the {@link EntityTag} object used to {@linkplain javax.ws.rs.core.Response.ResponseBuilder#tag(EntityTag)
     * tag} the content within this <code>Channel.ViewResponse</code>.
     * 
     * @see #getEntityTag()
     */
        public synchronized void setEntityTag(final EntityTag entityTag) {
            this.entityTag = entityTag;
            return;
        }

        /**
     * Get the {@link Locale} of the content within this <code>Channel.ViewResponse</code>.
     * 
     * @see #setLocale(Locale)
     */
        public synchronized Locale getLocale() {
            return locale;
        }

        /**
     * Set the {@link Locale} of the content within this <code>Channel.ViewResponse</code>.
     * 
     * @see #getLocale()
     */
        public synchronized void setLocale(final Locale locale) {
            this.locale = locale;
            return;
        }

        /**
     * Get the title of the content within this <code>Channel.ViewResponse</code>.
     * 
     * @see #setTitle(String)
     */
        public synchronized String getTitle() {
            return title;
        }

        /**
     * Set the title of the content within this <code>Channel.ViewResponse</code>.
     * 
     * @see #getTitle()
     * @see Channel#getTitleText(Page.Request)
     */
        public synchronized void setTitle(final String title) {
            this.title = title;
            return;
        }

        /**
     * Get the meta {@link Element}'s (&quot;<code>link</code>&quot;, &quot;<code>script</code>&quot;, &quot;
     * <code>meta</code>&quot;) to be added to the {@link Page} for this <code>Channel.ViewResponse</code>.
     * 
     * @see #addMetaElement(Element)
     */
        public synchronized Iterable<Element> getMetaElements() {
            return metaElements;
        }

        /**
     * Add a meta {@link Element} (&quot;<code>link</code>&quot;, &quot;<code>script</code>&quot;, &quot;
     * <code>meta</code>&quot;) to the {@link Page} for this <code>Channel.ViewResponse</code>.
     * 
     * @see #getMetaElements()
     * @see Channel#getMetaLinkElements(Page.Request)
     * @see Channel#getMetaScriptElements(Page.Request)
     * @see Channel#getMetaMetaElements(Page.Request)
     */
        public synchronized void addMetaElement(final Element metaElement) {
            if (this.metaElements == null) this.metaElements = new ArrayList<Element>(5);
            this.metaElements.add(metaElement);
            return;
        }

        /**
     * @see Serializable
     */
        private void writeObject(final ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeObject(ConversionUtil.invokeConverter(RESTUtil.MEDIA_TYPE_MIME_TYPE_CONVERTER, contentType));
            out.writeObject(ConversionUtil.invokeConverter(StringUtil.OBJECT_STRING_CONVERTER, cacheControl));
            out.writeObject(ConversionUtil.invokeConverter(StringUtil.OBJECT_STRING_CONVERTER, entityTag));
            return;
        }

        /**
     * @see Serializable
     */
        private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            contentType = ConversionUtil.invokeConverter(RESTUtil.MIME_TYPE_MEDIA_TYPE_CONVERTER, (MimeType) in.readObject());
            cacheControl = ConversionUtil.invokeConverter(RESTUtil.STRING_CACHE_CONTROL_CONVERTER, (String) in.readObject());
            entityTag = ConversionUtil.invokeConverter(RESTUtil.STRING_ENTITY_TAG_CONVERTER, (String) in.readObject());
            return;
        }
    }

    /**
   * Used to create a constant which acts like a label for defining an opportunity for a {@link Channel.Plugin Plugin}
   * to intercept regular <code>Channel</code> operation.
   * 
   * @param <C> The {@link Channel} class against which this hook applies.
   * @param <T> The type of value {@linkplain Channel#pluginValueHook(PluginHook, Object[], Page.Request) provided} /
   * {@linkplain Channel#pluginFilterHook(PluginHook, Object[], Page.Request, Object) filtered} by this hook.
   */
    public static final class PluginHook<C extends Channel, T> {

        /**
     * A lookup table used to verify that no two hooks are ever defined within any single <code>Channel</code> class
     * which both share the same ordinal value.
     */
        private static final Set<Map.Entry<Class<?>, Integer>> HOOK_IDS = new HashSet<Map.Entry<Class<?>, Integer>>();

        /**
     * A unique identifier for this hook based on the combination of <code>Channel</code> class and an ordinal value.
     */
        private final Map.Entry<Class<?>, Integer> id;

        /**
     * The type of value provided or filtered by this hook.
     */
        private final Class<T> resultClass;

        /**
     * The types for an array of objects to be provided by the <code>Channel</code> when calling the plugin for this
     * hook.
     */
        private final Class<?>[] contextClasses;

        /**
     * Define a new <code>Channel.PluginHook</code>.
     * 
     * @param channelClass The {@link Channel} class against which this hook applies.
     * @param ordinal A unique index within the <code>Channel</code> implementation which is used to identify this hook.
     * @param resultClass The type of value provided or filtered by this hook.
     * @param contextClasses Define the types for an array of objects to be provided by the <code>Channel</code> when
     * calling the plugin for this hook, in order to communicate to the plugin the set of useful and pertinent values in
     * context at the time of the call.
     */
        public PluginHook(final Class<C> channelClass, final int ordinal, final Class<T> resultClass, final Class<?>[] contextClasses) throws IllegalArgumentException {
            if (channelClass == null) throw new IllegalArgumentException("null channelClass");
            if ((ordinal < 0) && (!Channel.class.equals(channelClass))) throw new IllegalArgumentException("negative ordinals are reserved for Channel use");
            if (resultClass == null) throw new IllegalArgumentException("null resultClass");
            id = new AbstractMap.SimpleImmutableEntry<Class<?>, Integer>(channelClass, Integer.valueOf(ordinal));
            synchronized (HOOK_IDS) {
                if (HOOK_IDS.contains(id)) throw new IllegalArgumentException("duplicate hook");
                HOOK_IDS.add(id);
            }
            this.resultClass = resultClass;
            this.contextClasses = contextClasses;
            return;
        }

        /**
     * Get the type of value {@linkplain Channel#pluginValueHook(PluginHook, Object[], Page.Request) provided} /
     * {@linkplain Channel#pluginFilterHook(PluginHook, Object[], Page.Request, Object) filtered} by this hook.
     */
        public Class<T> getResultClass() {
            return resultClass;
        }

        /**
     * Verify that context values being provided in a call to the plugin for this hook are valid.
     */
        protected void verifyContext(final Object[] context) throws WWWeeePortal.Exception {
            if (contextClasses == null) {
                if (context != null) throw new WWWeeePortal.SoftwareException(new IllegalArgumentException("Attempt to provide unrequired plugin context"));
                return;
            }
            if (context == null) throw new WWWeeePortal.SoftwareException(new IllegalArgumentException("Defined plugin context not provided"));
            if (contextClasses.length != context.length) throw new WWWeeePortal.SoftwareException(new IllegalArgumentException("Provided plugin context not of defined length"));
            for (int i = 0; i < context.length; i++) {
                if ((context[i] != null) && (!contextClasses[i].isInstance(context[i]))) {
                    throw new WWWeeePortal.SoftwareException(new IllegalArgumentException("Plugin context entry " + (i + 1) + " is not of defined type"));
                }
            }
            return;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof PluginHook<?, ?>)) return false;
            final PluginHook<?, ?> otherHook = (PluginHook<?, ?>) obj;
            return id.equals(otherHook.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '-' + id.toString();
        }
    }

    /**
   * <p>
   * An abstract base for creating runtime instance classes responsible for {@linkplain PluginHook intercepting} regular
   * {@link Channel channel} operation to
   * {@linkplain Channel.Plugin#pluginValueHook(PluginHook, Object[], Page.Request) provide} or
   * {@linkplain Channel.Plugin#pluginFilterHook(PluginHook, Object[], Page.Request, Object) filter} values based on
   * configuration from a {@linkplain ContentManager.ChannelPluginDefinition channel plugin definition}.
   * </p>
   */
    public abstract class Plugin {

        /**
     * The {@linkplain ContentManager.ChannelPluginDefinition definition} for this <code>Channel.Plugin</code> instance.
     */
        protected final ContentManager.ChannelPluginDefinition<?> definition;

        /**
     * Construct a new <code>Channel.Plugin</code> instance.
     */
        protected Plugin(final ContentManager.ChannelPluginDefinition<?> definition) throws WWWeeePortal.Exception {
            if (definition == null) throw new IllegalArgumentException("null definition");
            if (!definition.isInitialized()) throw new IllegalStateException("Attempt to instantiate Channel Plugin from uninitialized definition");
            this.definition = definition;
            return;
        }

        /**
     * Get the {@link WWWeeePortal} instance hosting this <code>Channel.Plugin</code> instance.
     */
        public WWWeeePortal getPortal() {
            return Channel.this.getPortal();
        }

        /**
     * Get the {@link Channel} instance hosting this <code>Channel.Plugin</code> instance.
     */
        public Channel getChannel() {
            return Channel.this;
        }

        /**
     * Get the {@linkplain ContentManager.ChannelPluginDefinition definition} for this <code>Channel.Plugin</code>
     * instance.
     */
        public final ContentManager.ChannelPluginDefinition<?> getDefinition() {
            return definition;
        }

        /**
     * {@linkplain ConfigManager#getConfigProp(RSProperties, String, SecurityContext, HttpHeaders, RSProperties.Entry, Converter, Object, boolean, boolean)
     * Get} the {@linkplain ContentManager.ChannelPluginDefinition#getProperties() plugin property} matching the
     * specified criteria.
     * 
     * @see ConfigManager#getConfigProp(RSProperties, String, SecurityContext, HttpHeaders, RSProperties.Entry,
     * Converter, Object, boolean, boolean)
     */
        public <V> V getConfigProp(final String key, final Page.Request pageRequest, final Converter<RSProperties.Result, V> resultConverter, final V defaultConvertedValue, final boolean cacheValue, final boolean optional) throws IllegalArgumentException, ConfigManager.ConfigException {
            return ConfigManager.getConfigProp(definition.getProperties(), key, (pageRequest != null) ? pageRequest.getSecurityContext() : null, (pageRequest != null) ? pageRequest.getHttpHeaders() : null, null, resultConverter, defaultConvertedValue, cacheValue, optional);
        }

        /**
     * {@linkplain #getConfigProp(String, Page.Request, Converter, Object, boolean, boolean) Get} the
     * {@linkplain ContentManager.ChannelPluginDefinition#getProperties() plugin property} matching the specified
     * criteria as a {@link RSProperties#RESULT_STRING_CONVERTER String}.
     * 
     * @see #getConfigProp(String, Page.Request, Converter, Object, boolean, boolean)
     */
        public String getConfigProp(final String key, final Page.Request pageRequest, final String defaultConvertedValue, final boolean optional) throws IllegalArgumentException, ConfigManager.ConfigException {
            return getConfigProp(key, pageRequest, RSProperties.RESULT_STRING_CONVERTER, defaultConvertedValue, false, optional);
        }

        /**
     * <p>
     * Populate the supplied <code>metaProps</code> Map with the set of meta-information about this plugin.
     * </p>
     * 
     * <p>
     * This method will include information from this plugin itself, but also from it's
     * {@linkplain ContentManager.ChannelPluginDefinition#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
     * definition}.
     * </p>
     * 
     * <p>
     * The following properties are provided for the plugin itself:
     * </p>
     * <dl>
     * <dt><code>WWWeee.Channel.Plugin</code></dt>
     * <dd>The {@link Plugin} instance.</dd>
     * </dl>
     * 
     * @see ContentManager.ChannelPluginDefinition#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
     */
        public Map<String, Object> getMetaProps(final Page.Request pageRequest, final Map<String, Object> metaProps) throws IllegalArgumentException, ConfigManager.ConfigException {
            if (metaProps == null) throw new IllegalArgumentException("null metaProps");
            definition.getMetaProps(pageRequest, metaProps, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), 0);
            metaProps.put("WWWeee.Channel.Plugin", this);
            return metaProps;
        }

        /**
     * {@linkplain Logger#getLogger(String) Get} the {@link Logger} for this plugin instance.
     * 
     * @return The {@link Logger} for this plugin instance.
     * @see Logger#getLogger(String)
     */
        protected Logger getLogger() {
            return Logger.getLogger(getClass().getName());
        }

        /**
     * <p>
     * Create a key for storing a value in the client {@linkplain Page.Request#getAttributes() attributes}.
     * </p>
     * 
     * <p>
     * This utility method helps portal plugins create attribute keys which all use a consistent naming mechanism,
     * clearly identify the source of each key, and prevents collisions between plugin instances.
     * </p>
     * 
     * @param pageRequest The {@link Page.Request} whose {@linkplain Page.Request#getAttributes() attributes} the value
     * for the generated key will be stored in.
     * @param name The unique name for this attribute within the code for this Plugin class.
     * @param perPage If this plugin is hosted within a {@linkplain ContentManager.GlobalChannelDefinition global}
     * <code>Channel</code> instance, potentially shared across many {@link Page} instances, does the plugin storing a
     * value with this key want to store a separate value for each {@link Page} hosting the <code>Channel</code>, or one
     * common global value shared everywhere this <code>Channel</code>+<code>Plugin</code> is used? This argument has no
     * effect (always <code>true</code>) for a plugin hosted within a {@linkplain ContentManager.LocalChannelDefinition
     * local} <code>Channel</code>.
     * @param qualifiers If the <code>name</code>'d plugin code needs to store multiple values, this list of qualifiers
     * will be encoded into the key in order to differentiate between those values.
     * @return The key to be used for storing/retreiving the <code>name</code>'d attribute.
     */
        protected String createClientAttributesKey(final Page.Request pageRequest, final String name, final boolean perPage, final List<String> qualifiers) {
            final ArrayList<String> keyComponents = new ArrayList<String>(8 + ((qualifiers != null) ? qualifiers.size() : 0));
            keyComponents.add(getPortal().getPortalID());
            keyComponents.add(getChannel().getClass().getSimpleName());
            keyComponents.add(getClass().getSimpleName());
            keyComponents.add(name);
            keyComponents.add((perPage) ? pageRequest.getPage().getKey().getOwnerID() : definition.getContentContainer().getOwnerID());
            if ((perPage) || (page != null)) keyComponents.add((perPage) ? pageRequest.getPage().getKey().getPageGroupID() : page.getKey().getPageGroupID());
            if ((perPage) || (page != null)) keyComponents.add((perPage) ? pageRequest.getPage().getKey().getPageID() : page.getKey().getPageID());
            keyComponents.add(definition.getParentDefinition().getID());
            if (qualifiers != null) keyComponents.addAll(qualifiers);
            return ConversionUtil.invokeConverter(StringUtil.COMPONENTS_TO_DASH_DELIMITED_STRING_CONVERTER, keyComponents);
        }

        /**
     * Put this plugin into an operational state.
     * 
     * @param logMessage A {@linkplain org.wwweeeportal.util.logging.LogAnnotation.Message message} which will be
     * {@linkplain LogAnnotation#log(Logger, LogAnnotation.Message, Class, Throwable) logged} ({@link Level#FINER})
     * after initialization, which can be
     * {@linkplain LogAnnotation#annotate(LogAnnotation.Message, String, Object, Level, boolean) annotated} with any
     * informational notices the implementation wishes to have recorded in that log message.
     * @see #init()
     */
        protected void initInternal(final LogAnnotation.Message logMessage) throws WWWeeePortal.Exception {
            return;
        }

        /**
     * <p>
     * Put this <code>Plugin</code> into an operational state.
     * </p>
     * 
     * <p>
     * This method must be called prior to any requests being routed to the <code>Channel</code>. This method follows
     * the same semantics as {@link ContentManager#init()}.
     * </p>
     * 
     * @see Channel#init()
     * @see #destroy()
     * @see ContentManager#init()
     */
        protected final void init() throws WWWeeePortal.Exception {
            final LogAnnotation.Message logMessage = new LogAnnotation.MessageImpl(Level.FINER, "Initialized Channel Plugin: " + toString());
            try {
                initInternal(logMessage);
            } catch (WWWeeePortal.Exception wpe) {
                destroyInternal(wpe);
                throw wpe;
            }
            LogAnnotation.log(getLogger(), logMessage, getClass(), null);
            return;
        }

        /**
     * Take this <code>Plugin</code> out of active service.
     * 
     * @param logMessage A {@linkplain org.wwweeeportal.util.logging.LogAnnotation.Message message} which will be
     * {@linkplain LogAnnotation#log(Logger, LogAnnotation.Message, Class, Throwable) logged} ({@link Level#FINER})
     * after destruction, which can be
     * {@linkplain LogAnnotation#annotate(LogAnnotation.Message, String, Object, Level, boolean) annotated} with any
     * informational notices the implementation wishes to have recorded in that log message.
     * @see #destroy()
     */
        protected void destroyInternal(final LogAnnotation.Message logMessage) {
            return;
        }

        /**
     * <p>
     * Take this <code>Plugin</code> out of active service.
     * </p>
     * 
     * <p>
     * This method follows the same semantics as {@link ContentManager#destroy()}.
     * </p>
     * 
     * @see Channel#destroy()
     * @see #init()
     * @see ContentManager#destroy()
     */
        protected final void destroy() {
            final LogAnnotation.Message logMessage = new LogAnnotation.MessageImpl(Level.FINER, "Destroyed Channel Plugin: " + toString());
            destroyInternal(logMessage);
            LogAnnotation.log(getLogger(), logMessage, getClass(), null);
            return;
        }

        /**
     * @see Channel#pluginValueHook(PluginHook, Object[], Page.Request)
     */
        protected <T> T pluginValueHook(final PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest) throws WWWeeePortal.Exception {
            return null;
        }

        /**
     * @see Channel#pluginFilterHook(PluginHook, Object[], Page.Request, Object)
     */
        protected <T> T pluginFilterHook(final PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest, final T data) throws WWWeeePortal.Exception {
            return data;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(getClass().getSimpleName());
            sb.append('@');
            sb.append(Integer.toHexString(System.identityHashCode(this)).toUpperCase());
            sb.append('(');
            sb.append(definition);
            sb.append(')');
            return sb.toString();
        }
    }
}
