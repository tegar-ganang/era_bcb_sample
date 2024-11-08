package org.wwweeeportal.portal.channels;

import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.w3c.dom.*;
import javax.ws.rs.core.*;
import org.springframework.core.convert.converter.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.xml.html.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;

/**
 * <p>
 * A {@link Channel} which renders site navigation into the content of the {@link Page}, containing a list of links to
 * the
 * {@linkplain ContentManager#getPageDefinitions(Map, Map, Map, Map, Map, Map, boolean, UriInfo, SecurityContext, HttpHeaders, boolean, long, int)
 * pages} accessible to the client user.
 * </p>
 * 
 * <p>
 * By default, XHTML with the following general structure will be generated...
 * </p>
 * 
 * <p>
 * <code>
 * &lt;ol&gt;<br />
 * &#160;&#160;&lt;li&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;div&gt;&lt;a href=&quot;/{@link org.wwweeeportal.portal.ContentManager.PageDefinition.Key#getPageURI(UriInfo, Map, String, boolean) link}_to_page_1.1&quot;&gt;&lt;span&gt;{@linkplain Page#GROUP_TITLE_TEXT_PROP Page Group Title} 1&lt;/span&gt;&lt;/a&gt;&lt;/div&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;ol&gt;<br />
 * &#160;&#160;&#160;&#160;&#160;&#160;&lt;li&gt;&lt;div&gt;&lt;a href=&quot;/{@link org.wwweeeportal.portal.ContentManager.PageDefinition.Key#getPageURI(UriInfo, Map, String, boolean) link}_to_page_1.1&quot;&gt;&lt;span&gt;{@linkplain Page#TITLE_TEXT_PROP Page Title} 1.1&lt;/span&gt;&lt;/a&gt;&lt;/div&gt;&lt;/li&gt;<br />
 * &#160;&#160;&#160;&#160;&#160;&#160;&lt;li&gt;&lt;div&gt;&lt;a href=&quot;/{@link org.wwweeeportal.portal.ContentManager.PageDefinition.Key#getPageURI(UriInfo, Map, String, boolean) link}_to_page_1.2&quot;&gt;&lt;span&gt;{@linkplain Page#TITLE_TEXT_PROP Page Title} 1.2&lt;/span&gt;&lt;/a&gt;&lt;/div&gt;&lt;/li&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;/ol&gt;<br />
 * &#160;&#160;&lt;/li&gt;<br />
 * &#160;&#160;&lt;li&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;div&gt;&lt;a href=&quot;/{@link org.wwweeeportal.portal.ContentManager.PageDefinition.Key#getPageURI(UriInfo, Map, String, boolean) link}_to_page_2.1&quot;&gt;&lt;span&gt;{@linkplain Page#GROUP_TITLE_TEXT_PROP Page Group Title} 2&lt;/span&gt;&lt;/a&gt;&lt;/div&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;ol&gt;<br />
 * &#160;&#160;&#160;&#160;&#160;&#160;&lt;li&gt;&lt;div&gt;&lt;a href=&quot;/{@link org.wwweeeportal.portal.ContentManager.PageDefinition.Key#getPageURI(UriInfo, Map, String, boolean) link}_to_page_2.1&quot;&gt;&lt;span&gt;{@linkplain Page#TITLE_TEXT_PROP Page Title} 2.1&lt;/span&gt;&lt;/a&gt;&lt;/div&gt;&lt;/li&gt;<br />
 * &#160;&#160;&#160;&#160;&#160;&#160;&lt;li&gt;&lt;div&gt;&lt;a href=&quot;/{@link org.wwweeeportal.portal.ContentManager.PageDefinition.Key#getPageURI(UriInfo, Map, String, boolean) link}_to_page_2.2&quot;&gt;&lt;span&gt;{@linkplain Page#TITLE_TEXT_PROP Page Title} 2.2&lt;/span&gt;&lt;/a&gt;&lt;/div&gt;&lt;/li&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;/ol&gt;<br />
 * &#160;&#160;&lt;/li&gt;<br />
 * &lt;/ol&gt;<br />
 * </code>
 * </p>
 * 
 * <p>
 * The markup also includes <code>id</code> and <code>class</code> attributes (not shown) to enable CSS styling of the
 * navigation. This channel will use &quot;<code>nav</code>&quot; as it's default
 * {@linkplain #getSectionType(Page.Request) section type}. Note that no &quot;<code>href</code>&quot; {@linkplain Attr
 * attribute} will be added to the anchor generated for the current Page.
 * </p>
 * 
 * <p>
 * This channel's default {@linkplain #getMaximizationNormalPriority(Page.Request) maximization priority} will cause it
 * to be displayed even while others are {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel)
 * maximized}, and it's own maximization will be {@linkplain #isMaximizationDisabled(Page.Request) disabled} by default.
 * </p>
 * 
 * <h3 id="configuration">Configuration</h3>
 * <p>
 * In addition to those inherited from the {@link Channel} class, the following {@linkplain ConfigManager configuration
 * properties} are supported by this class:
 * </p>
 * <ul>
 * <li>{@link #PAGE_CHANNEL_DISPLAY_ALL_PAGES_ENABLE_PROP}</li>
 * <li>{@link #PAGE_CHANNEL_DISPLAY_CURRENT_PAGE_ENABLE_PROP}</li>
 * <li>{@link #GROUP_ANCHOR_DISABLE_PROP}</li>
 * <li>{@link #PAGE_META_PROP_PUBLISH_ENABLE_PROP}</li>
 * <li>{@link #CHANNEL_META_PROP_PUBLISH_ENABLE_PROP}</li>
 * <li>{@link #INCLUDE_PAGE_CONTAINER_WITH_PROP_PROP}</li>
 * <li>{@link #INCLUDE_PAGE_CONTAINER_WITHOUT_PROP_PROP}</li>
 * <li>{@link #INCLUDE_PAGE_GROUP_WITH_PROP_PROP}</li>
 * <li>{@link #INCLUDE_PAGE_GROUP_WITHOUT_PROP_PROP}</li>
 * <li>{@link #INCLUDE_PAGE_WITH_PROP_PROP}</li>
 * <li>{@link #INCLUDE_PAGE_WITHOUT_PROP_PROP}</li>
 * <li>{@link #INCLUDE_CHANNEL_GROUP_WITH_PROP_PROP}</li>
 * <li>{@link #INCLUDE_CHANNEL_GROUP_WITHOUT_PROP_PROP}</li>
 * <li>{@link #INCLUDE_CHANNEL_WITH_PROP_PROP}</li>
 * <li>{@link #INCLUDE_CHANNEL_WITHOUT_PROP_PROP}</li>
 * </ul>
 */
public class SiteNavigationChannel extends Channel {

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property indicating that an ordered list of the
   * {@linkplain ContentManager#getChannelDefinitions(ContentManager.PageDefinition, Map, Map, Map, Map, Map, Map, boolean, SecurityContext, HttpHeaders)
   * channels} on each page <em>should</em> also be included within the generated navigation.
   * 
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String PAGE_CHANNEL_DISPLAY_ALL_PAGES_ENABLE_PROP = "SiteNavigationChannel.Page.ChannelDisplay.AllPages.Enable";

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property indicating that an ordered list of the
   * {@linkplain ContentManager#getChannelDefinitions(ContentManager.PageDefinition, Map, Map, Map, Map, Map, Map, boolean, SecurityContext, HttpHeaders)
   * channels} on the <em>current</em> page <em>should</em> also be included within the generated navigation.
   * 
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String PAGE_CHANNEL_DISPLAY_CURRENT_PAGE_ENABLE_PROP = "SiteNavigationChannel.Page.ChannelDisplay.CurrentPage.Enable";

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property indicating that the anchor (
   * <code>&lt;a&gt;</code>) {@link Element} generated for each {@linkplain Page#GROUP_TITLE_TEXT_PROP Page Group Title}
   * should <em>not</em> include an &quot;<code>href</code>&quot; {@linkplain Attr attribute} making it a link to the
   * first page within that group.
   * 
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String GROUP_ANCHOR_DISABLE_PROP = "SiteNavigationChannel.PageGroup.Anchor.Disable";

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property <em>enabling</em> the
   * {@linkplain MarkupManager#createMetaPropsPublishElement(Node, Map) publishing} of
   * {@linkplain org.wwweeeportal.portal.ContentManager.PageDefinition#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
   * meta-props} within the generated navigation for each page.
   * 
   * @see org.wwweeeportal.portal.ContentManager.PageDefinition#getMetaProps(Page.Request, Map, SecurityContext,
   * HttpHeaders, int)
   * @see MarkupManager#createMetaPropsPublishElement(Node, Map)
   * @see #createSiteNavPageMetaPropsElement(Page.Request, ContentManager.PageDefinition, boolean, Element)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String PAGE_META_PROP_PUBLISH_ENABLE_PROP = "SiteNavigationChannel.Page.Meta.PropPublish.Enable";

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property <em>enabling</em> the
   * {@linkplain MarkupManager#createMetaPropsPublishElement(Node, Map) publishing} of
   * {@linkplain org.wwweeeportal.portal.ContentManager.ChannelDefinition#getMetaProps(Page.Request, Map, SecurityContext, HttpHeaders, int)
   * meta-props} within the generated navigation for each channel.
   * 
   * @see org.wwweeeportal.portal.ContentManager.ChannelDefinition#getMetaProps(Page.Request, Map, SecurityContext,
   * HttpHeaders, int)
   * @see MarkupManager#createMetaPropsPublishElement(Node, Map)
   * @see #createSiteNavChannelMetaPropsElement(Page.Request, ContentManager.PageDefinition,
   * ContentManager.ChannelDefinition, boolean, Element)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String CHANNEL_META_PROP_PUBLISH_ENABLE_PROP = "SiteNavigationChannel.Channel.Meta.PropPublish.Enable";

    /**
   * The prefix defining a set of keys to {@link RSProperties#RESULT_STRING_CONVERTER String} property values, where the
   * remaining portion of each matching key is combined with it's value to form a
   * {@linkplain ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER properties filter} which will be used to
   * {@linkplain ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders) restrict} the
   * {@linkplain org.wwweeeportal.portal.ContentManager.PageContentContainer page containers} included in the generated
   * navigation to those <em>with</em> the specified properties.
   * 
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    public static final String INCLUDE_PAGE_CONTAINER_WITH_PROP_PROP = "SiteNavigationChannel.Include.PageContainer.WithProp.";

    /**
   * @see #INCLUDE_PAGE_CONTAINER_WITH_PROP_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   */
    protected static final Pattern INCLUDE_PAGE_CONTAINER_WITH_PROP_PATTERN = Pattern.compile("^" + Pattern.quote(INCLUDE_PAGE_CONTAINER_WITH_PROP_PROP) + ".*");

    /**
   * @see #INCLUDE_PAGE_CONTAINER_WITH_PROP_PROP
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    protected static final Converter<Map<String, String>, Map<String, String>> INCLUDE_PAGE_CONTAINER_WITH_PROP_MAP_CONVERTER = new MapConverter<String, String, String, String>(new StringUtil.SubstringConverter(INCLUDE_PAGE_CONTAINER_WITH_PROP_PROP.length(), -1), null, StringUtil.STRING_NO_OP_CONVERTER, null);

    /**
   * The prefix defining a set of keys to {@link RSProperties#RESULT_STRING_CONVERTER String} property values, where the
   * remaining portion of each matching key is combined with it's value to form a
   * {@linkplain ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER properties filter} which will be used to
   * {@linkplain ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders) restrict} the
   * {@linkplain org.wwweeeportal.portal.ContentManager.PageContentContainer page containers} included in the generated
   * navigation to those <em>without</em> the specified properties.
   * 
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    public static final String INCLUDE_PAGE_CONTAINER_WITHOUT_PROP_PROP = "SiteNavigationChannel.Include.PageContainer.WithoutProp.";

    /**
   * @see #INCLUDE_PAGE_CONTAINER_WITHOUT_PROP_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   */
    protected static final Pattern INCLUDE_PAGE_CONTAINER_WITHOUT_PROP_PATTERN = Pattern.compile("^" + Pattern.quote(INCLUDE_PAGE_CONTAINER_WITHOUT_PROP_PROP) + ".*");

    /**
   * @see #INCLUDE_PAGE_CONTAINER_WITHOUT_PROP_PROP
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    protected static final Converter<Map<String, String>, Map<String, String>> INCLUDE_PAGE_CONTAINER_WITHOUT_PROP_MAP_CONVERTER = new MapConverter<String, String, String, String>(new StringUtil.SubstringConverter(INCLUDE_PAGE_CONTAINER_WITHOUT_PROP_PROP.length(), -1), null, StringUtil.STRING_NO_OP_CONVERTER, null);

    /**
   * The prefix defining a set of keys to {@link RSProperties#RESULT_STRING_CONVERTER String} property values, where the
   * remaining portion of each matching key is combined with it's value to form a
   * {@linkplain ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER properties filter} which will be used to
   * {@linkplain ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders) restrict} the
   * {@linkplain org.wwweeeportal.portal.ContentManager.PageGroupDefinition page groups} included in the generated
   * navigation to those <em>with</em> the specified properties.
   * 
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    public static final String INCLUDE_PAGE_GROUP_WITH_PROP_PROP = "SiteNavigationChannel.Include.PageGroup.WithProp.";

    /**
   * @see #INCLUDE_PAGE_GROUP_WITH_PROP_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   */
    protected static final Pattern INCLUDE_PAGE_GROUP_WITH_PROP_PATTERN = Pattern.compile("^" + Pattern.quote(INCLUDE_PAGE_GROUP_WITH_PROP_PROP) + ".*");

    /**
   * @see #INCLUDE_PAGE_GROUP_WITH_PROP_PROP
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    protected static final Converter<Map<String, String>, Map<String, String>> INCLUDE_PAGE_GROUP_WITH_PROP_MAP_CONVERTER = new MapConverter<String, String, String, String>(new StringUtil.SubstringConverter(INCLUDE_PAGE_GROUP_WITH_PROP_PROP.length(), -1), null, StringUtil.STRING_NO_OP_CONVERTER, null);

    /**
   * The prefix defining a set of keys to {@link RSProperties#RESULT_STRING_CONVERTER String} property values, where the
   * remaining portion of each matching key is combined with it's value to form a
   * {@linkplain ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER properties filter} which will be used to
   * {@linkplain ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders) restrict} the
   * {@linkplain org.wwweeeportal.portal.ContentManager.PageGroupDefinition page groups} included in the generated
   * navigation to those <em>without</em> the specified properties.
   * 
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    public static final String INCLUDE_PAGE_GROUP_WITHOUT_PROP_PROP = "SiteNavigationChannel.Include.PageGroup.WithoutProp.";

    /**
   * @see #INCLUDE_PAGE_GROUP_WITHOUT_PROP_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   */
    protected static final Pattern INCLUDE_PAGE_GROUP_WITHOUT_PROP_PATTERN = Pattern.compile("^" + Pattern.quote(INCLUDE_PAGE_GROUP_WITHOUT_PROP_PROP) + ".*");

    /**
   * @see #INCLUDE_PAGE_GROUP_WITHOUT_PROP_PROP
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    protected static final Converter<Map<String, String>, Map<String, String>> INCLUDE_PAGE_GROUP_WITHOUT_PROP_MAP_CONVERTER = new MapConverter<String, String, String, String>(new StringUtil.SubstringConverter(INCLUDE_PAGE_GROUP_WITHOUT_PROP_PROP.length(), -1), null, StringUtil.STRING_NO_OP_CONVERTER, null);

    /**
   * The prefix defining a set of keys to {@link RSProperties#RESULT_STRING_CONVERTER String} property values, where the
   * remaining portion of each matching key is combined with it's value to form a
   * {@linkplain ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER properties filter} which will be used to
   * {@linkplain ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders) restrict} the
   * {@linkplain org.wwweeeportal.portal.ContentManager.PageDefinition pages} included in the generated navigation to
   * those <em>with</em> the specified properties.
   * 
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    public static final String INCLUDE_PAGE_WITH_PROP_PROP = "SiteNavigationChannel.Include.Page.WithProp.";

    /**
   * @see #INCLUDE_PAGE_WITH_PROP_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   */
    protected static final Pattern INCLUDE_PAGE_WITH_PROP_PATTERN = Pattern.compile("^" + Pattern.quote(INCLUDE_PAGE_WITH_PROP_PROP) + ".*");

    /**
   * @see #INCLUDE_PAGE_WITH_PROP_PROP
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    protected static final Converter<Map<String, String>, Map<String, String>> INCLUDE_PAGE_WITH_PROP_MAP_CONVERTER = new MapConverter<String, String, String, String>(new StringUtil.SubstringConverter(INCLUDE_PAGE_WITH_PROP_PROP.length(), -1), null, StringUtil.STRING_NO_OP_CONVERTER, null);

    /**
   * The prefix defining a set of keys to {@link RSProperties#RESULT_STRING_CONVERTER String} property values, where the
   * remaining portion of each matching key is combined with it's value to form a
   * {@linkplain ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER properties filter} which will be used to
   * {@linkplain ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders) restrict} the
   * {@linkplain org.wwweeeportal.portal.ContentManager.PageDefinition pages} included in the generated navigation to
   * those <em>without</em> the specified properties.
   * 
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    public static final String INCLUDE_PAGE_WITHOUT_PROP_PROP = "SiteNavigationChannel.Include.Page.WithoutProp.";

    /**
   * @see #INCLUDE_PAGE_WITHOUT_PROP_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   */
    protected static final Pattern INCLUDE_PAGE_WITHOUT_PROP_PATTERN = Pattern.compile("^" + Pattern.quote(INCLUDE_PAGE_WITHOUT_PROP_PROP) + ".*");

    /**
   * @see #INCLUDE_PAGE_WITHOUT_PROP_PROP
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    protected static final Converter<Map<String, String>, Map<String, String>> INCLUDE_PAGE_WITHOUT_PROP_MAP_CONVERTER = new MapConverter<String, String, String, String>(new StringUtil.SubstringConverter(INCLUDE_PAGE_WITHOUT_PROP_PROP.length(), -1), null, StringUtil.STRING_NO_OP_CONVERTER, null);

    /**
   * The prefix defining a set of keys to {@link RSProperties#RESULT_STRING_CONVERTER String} property values, where the
   * remaining portion of each matching key is combined with it's value to form a
   * {@linkplain ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER properties filter} which will be used to
   * {@linkplain ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders) restrict} the
   * {@linkplain org.wwweeeportal.portal.ContentManager.ChannelGroupDefinition channel groups} included in the generated
   * navigation to those <em>with</em> the specified properties.
   * 
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    public static final String INCLUDE_CHANNEL_GROUP_WITH_PROP_PROP = "SiteNavigationChannel.Include.ChannelGroup.WithProp.";

    /**
   * @see #INCLUDE_CHANNEL_GROUP_WITH_PROP_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   */
    protected static final Pattern INCLUDE_CHANNEL_GROUP_WITH_PROP_PATTERN = Pattern.compile("^" + Pattern.quote(INCLUDE_CHANNEL_GROUP_WITH_PROP_PROP) + ".*");

    /**
   * @see #INCLUDE_CHANNEL_GROUP_WITH_PROP_PROP
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    protected static final Converter<Map<String, String>, Map<Pattern, Pattern>> INCLUDE_CHANNEL_GROUP_WITH_PROP_MAP_CONVERTER = new ConverterChain<Map<String, String>, Map<String, String>, Map<Pattern, Pattern>>(new MapConverter<String, String, String, String>(new StringUtil.SubstringConverter(INCLUDE_CHANNEL_GROUP_WITH_PROP_PROP.length(), -1), null, StringUtil.STRING_NO_OP_CONVERTER, null), ConfigManager.MATCHING_PROP_MAP_COMPILATION_CONVERTER);

    /**
   * The prefix defining a set of keys to {@link RSProperties#RESULT_STRING_CONVERTER String} property values, where the
   * remaining portion of each matching key is combined with it's value to form a
   * {@linkplain ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER properties filter} which will be used to
   * {@linkplain ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders) restrict} the
   * {@linkplain org.wwweeeportal.portal.ContentManager.ChannelGroupDefinition channel groups} included in the generated
   * navigation to those <em>without</em> the specified properties.
   * 
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    public static final String INCLUDE_CHANNEL_GROUP_WITHOUT_PROP_PROP = "SiteNavigationChannel.Include.ChannelGroup.WithoutProp.";

    /**
   * @see #INCLUDE_CHANNEL_GROUP_WITHOUT_PROP_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   */
    protected static final Pattern INCLUDE_CHANNEL_GROUP_WITHOUT_PROP_PATTERN = Pattern.compile("^" + Pattern.quote(INCLUDE_CHANNEL_GROUP_WITHOUT_PROP_PROP) + ".*");

    /**
   * @see #INCLUDE_CHANNEL_GROUP_WITHOUT_PROP_PROP
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    protected static final Converter<Map<String, String>, Map<Pattern, Pattern>> INCLUDE_CHANNEL_GROUP_WITHOUT_PROP_MAP_CONVERTER = new ConverterChain<Map<String, String>, Map<String, String>, Map<Pattern, Pattern>>(new MapConverter<String, String, String, String>(new StringUtil.SubstringConverter(INCLUDE_CHANNEL_GROUP_WITHOUT_PROP_PROP.length(), -1), null, StringUtil.STRING_NO_OP_CONVERTER, null), ConfigManager.MATCHING_PROP_MAP_COMPILATION_CONVERTER);

    /**
   * The prefix defining a set of keys to {@link RSProperties#RESULT_STRING_CONVERTER String} property values, where the
   * remaining portion of each matching key is combined with it's value to form a
   * {@linkplain ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER properties filter} which will be used to
   * {@linkplain ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders) restrict} the
   * {@linkplain org.wwweeeportal.portal.ContentManager.ChannelSpecification channel specifications} <em>and</em>
   * {@linkplain org.wwweeeportal.portal.ContentManager.ChannelDefinition channel definitions} included in the generated
   * navigation to those <em>with</em> the specified properties.
   * 
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    public static final String INCLUDE_CHANNEL_WITH_PROP_PROP = "SiteNavigationChannel.Include.Channel.WithProp.";

    /**
   * @see #INCLUDE_CHANNEL_WITH_PROP_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   */
    protected static final Pattern INCLUDE_CHANNEL_WITH_PROP_PATTERN = Pattern.compile("^" + Pattern.quote(INCLUDE_CHANNEL_WITH_PROP_PROP) + ".*");

    /**
   * @see #INCLUDE_CHANNEL_WITH_PROP_PROP
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    protected static final Converter<Map<String, String>, Map<Pattern, Pattern>> INCLUDE_CHANNEL_WITH_PROP_MAP_CONVERTER = new ConverterChain<Map<String, String>, Map<String, String>, Map<Pattern, Pattern>>(new MapConverter<String, String, String, String>(new StringUtil.SubstringConverter(INCLUDE_CHANNEL_WITH_PROP_PROP.length(), -1), null, StringUtil.STRING_NO_OP_CONVERTER, null), ConfigManager.MATCHING_PROP_MAP_COMPILATION_CONVERTER);

    /**
   * The prefix defining a set of keys to {@link RSProperties#RESULT_STRING_CONVERTER String} property values, where the
   * remaining portion of each matching key is combined with it's value to form a
   * {@linkplain ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER properties filter} which will be used to
   * {@linkplain ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders) restrict} the
   * {@linkplain org.wwweeeportal.portal.ContentManager.ChannelSpecification channel specifications} <em>and</em>
   * {@linkplain org.wwweeeportal.portal.ContentManager.ChannelDefinition channel definitions} included in the generated
   * navigation to those <em>without</em> the specified properties.
   * 
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    public static final String INCLUDE_CHANNEL_WITHOUT_PROP_PROP = "SiteNavigationChannel.Include.Channel.WithoutProp.";

    /**
   * @see #INCLUDE_CHANNEL_WITHOUT_PROP_PROP
   * @see ConfigManager#getConfigProps(RSProperties, Pattern, SecurityContext, HttpHeaders, Converter, boolean, boolean)
   */
    protected static final Pattern INCLUDE_CHANNEL_WITHOUT_PROP_PATTERN = Pattern.compile("^" + Pattern.quote(INCLUDE_CHANNEL_WITHOUT_PROP_PROP) + ".*");

    /**
   * @see #INCLUDE_CHANNEL_WITHOUT_PROP_PROP
   * @see ConfigManager#MATCHING_PROP_MAP_COMPILATION_CONVERTER
   * @see ConfigManager#isPropsMatch(RSProperties, Map, Map, boolean, SecurityContext, HttpHeaders)
   */
    protected static final Converter<Map<String, String>, Map<Pattern, Pattern>> INCLUDE_CHANNEL_WITHOUT_PROP_MAP_CONVERTER = new ConverterChain<Map<String, String>, Map<String, String>, Map<Pattern, Pattern>>(new MapConverter<String, String, String, String>(new StringUtil.SubstringConverter(INCLUDE_CHANNEL_WITHOUT_PROP_PROP.length(), -1), null, StringUtil.STRING_NO_OP_CONVERTER, null), ConfigManager.MATCHING_PROP_MAP_COMPILATION_CONVERTER);

    /**
   * Construct a new <code>SiteNavigationChannel</code> instance.
   */
    public SiteNavigationChannel(final WWWeeePortal portal, final ContentManager.ChannelDefinition<?, ? extends SiteNavigationChannel> definition, final Page page) throws WWWeeePortal.Exception {
        super(portal, definition, page);
        return;
    }

    @Override
    public boolean isMaximizationDisabled(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        if (getConfigProp(MAXIMIZATION_DISABLE_PROP, pageRequest, null, true) == null) return true;
        return super.isMaximizationDisabled(pageRequest);
    }

    @Override
    public int getMaximizationNormalPriority(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        if (getConfigProp(MAXIMIZATION_PRIORITY_NORMAL_PROP, pageRequest, null, true) == null) return MAXIMIZATION_PRIORITY_DEFAULT.intValue() * (MAXIMIZATION_PRIORITY_MULTIPLIER + 30);
        return super.getMaximizationNormalPriority(pageRequest);
    }

    @Override
    public String getSectionType(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        if (getConfigProp(SECTION_TYPE_PROP, pageRequest, null, true) == null) return "nav";
        return super.getSectionType(pageRequest);
    }

    /**
   * @see #CHANNEL_META_PROP_PUBLISH_ENABLE_PROP
   */
    protected Element createSiteNavChannelMetaPropsElement(final Page.Request pageRequest, final ContentManager.PageDefinition<?> page, final ContentManager.ChannelDefinition<?, ?> channel, final boolean channelIsReqChannel, final Element parentElement) throws WWWeeePortal.Exception {
        if (!getConfigProp(CHANNEL_META_PROP_PUBLISH_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue()) return null;
        final String portalID = portal.getPortalID();
        final String channelID = definition.getID();
        final Map<String, Object> publishMetaProps = channel.getMetaProps(pageRequest, new HashMap<String, Object>(), pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), 0);
        final Element channelPropertiesElement = MarkupManager.createMetaPropsPublishElement(parentElement, publishMetaProps);
        final String channelPropertiesIDAttr = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "properties", page.getContentContainer().getOwnerID(), page.getParentDefinition().getID(), page.getID(), channel.getID()));
        DOMUtil.createAttribute(null, null, "id", channelPropertiesIDAttr, channelPropertiesElement);
        final StringBuffer channelPropertiesClassAttr = new StringBuffer();
        channelPropertiesClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "properties")));
        channelPropertiesClassAttr.append(' ');
        channelPropertiesClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "internal")));
        if (channelIsReqChannel) {
            channelPropertiesClassAttr.append(' ');
            channelPropertiesClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "properties", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", channelPropertiesClassAttr.toString(), channelPropertiesElement);
        return channelPropertiesElement;
    }

    /**
   * Generate the navigation anchor for the specified <code>channel</code>.
   */
    protected Element createSiteNavChannelAnchorElement(final Page.Request pageRequest, final ContentManager.PageDefinition<?> page, final ContentManager.ChannelDefinition<?, ?> channel, final boolean channelIsReqChannel, final Element parentElement, final StringBuffer contentSignature) throws WWWeeePortal.Exception {
        final String portalID = portal.getPortalID();
        final String channelID = definition.getID();
        final String channelTitle = ConfigManager.getConfigProp(channel.getProperties(), TITLE_TEXT_PROP, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), null, RSProperties.RESULT_STRING_CONVERTER, channel.getID(), false, false);
        final Element channelAnchorElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "a", parentElement);
        final String channelAnchorIDAttr = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "anchor", page.getContentContainer().getOwnerID(), page.getParentDefinition().getID(), page.getID(), channel.getID()));
        DOMUtil.createAttribute(null, null, "id", channelAnchorIDAttr, channelAnchorElement);
        final StringBuffer channelAnchorClassAttr = new StringBuffer();
        channelAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor")));
        channelAnchorClassAttr.append(' ');
        channelAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "anchor")));
        if (channelIsReqChannel) {
            channelAnchorClassAttr.append(' ');
            channelAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor", "current")));
            channelAnchorClassAttr.append(' ');
            channelAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "anchor", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", channelAnchorClassAttr.toString(), channelAnchorElement);
        final boolean channelMaximizationDisabled = ConfigManager.getConfigProp(channel.getProperties(), MAXIMIZATION_DISABLE_PROP, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), null, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
        if ((!channelIsReqChannel) && (!channelMaximizationDisabled)) {
            final ContentManager.ChannelSpecification<?> channelSpecification = page.getChannelSpecification(channel.getID());
            final URI channelHrefURI = channelSpecification.getKey().getChannelURI(pageRequest.getUriInfo(), VIEW_MODE, null, null, null, false);
            DOMUtil.createAttribute(null, null, "href", channelHrefURI.toString(), channelAnchorElement);
        }
        final Element channelAnchorTitleElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", channelAnchorElement);
        final String channelAnchorTitleIDAttr = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "anchor", "text", page.getContentContainer().getOwnerID(), page.getParentDefinition().getID(), page.getID(), channel.getID()));
        DOMUtil.createAttribute(null, null, "id", channelAnchorTitleIDAttr, channelAnchorTitleElement);
        final StringBuffer channelAnchorTitleClassAttr = new StringBuffer();
        channelAnchorTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor", "text")));
        channelAnchorTitleClassAttr.append(' ');
        channelAnchorTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "anchor", "text")));
        if (channelIsReqChannel) {
            channelAnchorTitleClassAttr.append(' ');
            channelAnchorTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor", "text", "current")));
            channelAnchorTitleClassAttr.append(' ');
            channelAnchorTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "anchor", "text", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", channelAnchorTitleClassAttr.toString(), channelAnchorTitleElement);
        DOMUtil.appendText(channelAnchorTitleElement, channelTitle);
        contentSignature.append('[');
        contentSignature.append(page.getParentDefinition().getID());
        contentSignature.append('.');
        contentSignature.append(page.getID());
        contentSignature.append('.');
        contentSignature.append(channel.getID());
        contentSignature.append("='");
        contentSignature.append(channelTitle);
        contentSignature.append("']");
        return channelAnchorElement;
    }

    /**
   * Generate the navigation for the specified <code>channel</code>.
   * 
   * @see #PAGE_CHANNEL_DISPLAY_ALL_PAGES_ENABLE_PROP
   * @see #PAGE_CHANNEL_DISPLAY_CURRENT_PAGE_ENABLE_PROP
   * @see #INCLUDE_CHANNEL_GROUP_WITH_PROP_MAP_CONVERTER
   * @see #INCLUDE_CHANNEL_GROUP_WITHOUT_PROP_MAP_CONVERTER
   * @see #INCLUDE_CHANNEL_WITH_PROP_MAP_CONVERTER
   * @see #INCLUDE_CHANNEL_WITHOUT_PROP_MAP_CONVERTER
   */
    protected Element createSiteNavChannelElement(final Page.Request pageRequest, final ContentManager.PageDefinition<?> page, final ContentManager.ChannelDefinition<?, ?> channel, final boolean channelIsReqChannel, final Element parentElement, final StringBuffer contentSignature) throws WWWeeePortal.Exception {
        final String portalID = portal.getPortalID();
        final String channelID = definition.getID();
        final Element channelElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "li", parentElement);
        final String channelIDAttr = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", page.getContentContainer().getOwnerID(), page.getParentDefinition().getID(), page.getID()));
        DOMUtil.createAttribute(null, null, "id", channelIDAttr, channelElement);
        final StringBuffer channelClassAttr = new StringBuffer();
        channelClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel")));
        if (channelIsReqChannel) {
            channelClassAttr.append(' ');
            channelClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", channelClassAttr.toString(), channelElement);
        final Element channelTitleElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "div", channelElement);
        final String channelTitleIDAttr = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "title", page.getContentContainer().getOwnerID(), page.getParentDefinition().getID(), page.getID()));
        DOMUtil.createAttribute(null, null, "id", channelTitleIDAttr, channelTitleElement);
        final StringBuffer channelTitleClassAttr = new StringBuffer();
        channelTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "title")));
        channelTitleClassAttr.append(' ');
        channelTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "title")));
        if (channelIsReqChannel) {
            channelTitleClassAttr.append(' ');
            channelTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "title", "current")));
            channelTitleClassAttr.append(' ');
            channelTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "channel", "title", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", channelTitleClassAttr.toString(), channelTitleElement);
        createSiteNavChannelAnchorElement(pageRequest, page, channel, channelIsReqChannel, channelTitleElement, contentSignature);
        createSiteNavChannelMetaPropsElement(pageRequest, page, channel, channelIsReqChannel, channelElement);
        return channelElement;
    }

    /**
   * @see #PAGE_META_PROP_PUBLISH_ENABLE_PROP
   */
    protected Element createSiteNavPageMetaPropsElement(final Page.Request pageRequest, final ContentManager.PageDefinition<?> page, final boolean pageIsReqPage, final Element parentElement) throws WWWeeePortal.Exception {
        if (!getConfigProp(PAGE_META_PROP_PUBLISH_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue()) return null;
        final String portalID = portal.getPortalID();
        final String channelID = definition.getID();
        final Map<String, Object> publishMetaProps = page.getMetaProps(pageRequest, new HashMap<String, Object>(), pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), 0);
        final Element pagePropertiesElement = MarkupManager.createMetaPropsPublishElement(parentElement, publishMetaProps);
        final String pagePropertiesIDAttr = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "properties", page.getContentContainer().getOwnerID(), page.getParentDefinition().getID(), page.getID()));
        DOMUtil.createAttribute(null, null, "id", pagePropertiesIDAttr, pagePropertiesElement);
        final StringBuffer pagePropertiesClassAttr = new StringBuffer();
        pagePropertiesClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "properties")));
        pagePropertiesClassAttr.append(' ');
        pagePropertiesClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "internal")));
        if (pageIsReqPage) {
            pagePropertiesClassAttr.append(' ');
            pagePropertiesClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "properties", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", pagePropertiesClassAttr.toString(), pagePropertiesElement);
        return pagePropertiesElement;
    }

    /**
   * Generate the navigation anchor for the specified <code>page</code>.
   */
    protected Element createSiteNavPageAnchorElement(final Page.Request pageRequest, final ContentManager.PageDefinition<?> page, final boolean pageIsReqPage, final Element parentElement, final StringBuffer contentSignature) throws WWWeeePortal.Exception {
        final String portalID = portal.getPortalID();
        final String channelID = definition.getID();
        final String pageTitle;
        if (pageIsReqPage) {
            pageTitle = pageRequest.getPage().getTitleText(pageRequest);
        } else {
            pageTitle = ConfigManager.getConfigProp(page.getProperties(), Page.TITLE_TEXT_PROP, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), null, RSProperties.RESULT_STRING_CONVERTER, page.getID(), false, false);
        }
        final Element pageAnchorElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "a", parentElement);
        final String pageAnchorIDAttr = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "anchor", page.getContentContainer().getOwnerID(), page.getParentDefinition().getID(), page.getID()));
        DOMUtil.createAttribute(null, null, "id", pageAnchorIDAttr, pageAnchorElement);
        final StringBuffer pageAnchorClassAttr = new StringBuffer();
        pageAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor")));
        pageAnchorClassAttr.append(' ');
        pageAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "anchor")));
        if (pageIsReqPage) {
            pageAnchorClassAttr.append(' ');
            pageAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor", "current")));
            pageAnchorClassAttr.append(' ');
            pageAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "anchor", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", pageAnchorClassAttr.toString(), pageAnchorElement);
        if ((pageRequest.getMaximizedChannelKey() != null) || (!pageIsReqPage)) {
            final URI pageHrefURI = page.getKey().getPageURI(pageRequest.getUriInfo(), null, null, false);
            DOMUtil.createAttribute(null, null, "href", pageHrefURI.toString(), pageAnchorElement);
        }
        final Element pageAnchorTitleElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", pageAnchorElement);
        final String pageAnchorTitleIDAttr = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "anchor", "text", page.getContentContainer().getOwnerID(), page.getParentDefinition().getID(), page.getID()));
        DOMUtil.createAttribute(null, null, "id", pageAnchorTitleIDAttr, pageAnchorTitleElement);
        final StringBuffer pageAnchorTitleClassAttr = new StringBuffer();
        pageAnchorTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor", "text")));
        pageAnchorTitleClassAttr.append(' ');
        pageAnchorTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "anchor", "text")));
        if (pageIsReqPage) {
            pageAnchorTitleClassAttr.append(' ');
            pageAnchorTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor", "text", "current")));
            pageAnchorTitleClassAttr.append(' ');
            pageAnchorTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "anchor", "text", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", pageAnchorTitleClassAttr.toString(), pageAnchorTitleElement);
        DOMUtil.appendText(pageAnchorTitleElement, pageTitle);
        contentSignature.append('[');
        contentSignature.append(page.getParentDefinition().getID());
        contentSignature.append('.');
        contentSignature.append(page.getID());
        contentSignature.append("='");
        contentSignature.append(pageTitle);
        contentSignature.append("']");
        return pageAnchorElement;
    }

    /**
   * Generate the navigation for the specified <code>page</code>.
   * 
   * @see #INCLUDE_PAGE_CONTAINER_WITH_PROP_MAP_CONVERTER
   * @see #INCLUDE_PAGE_CONTAINER_WITHOUT_PROP_MAP_CONVERTER
   * @see #INCLUDE_PAGE_GROUP_WITH_PROP_MAP_CONVERTER
   * @see #INCLUDE_PAGE_GROUP_WITHOUT_PROP_MAP_CONVERTER
   * @see #INCLUDE_PAGE_WITH_PROP_MAP_CONVERTER
   * @see #INCLUDE_PAGE_WITHOUT_PROP_MAP_CONVERTER
   */
    protected Element createSiteNavPageElement(final Page.Request pageRequest, ContentManager.PageDefinition<?> page, final boolean pageIsReqPage, final Element parentElement, final StringBuffer contentSignature, final boolean channelDisplayEnabled) throws WWWeeePortal.Exception {
        final String portalID = portal.getPortalID();
        final String channelID = definition.getID();
        final Element pageElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "li", parentElement);
        final String pageIDAttr = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", page.getContentContainer().getOwnerID(), page.getParentDefinition().getID(), page.getID()));
        DOMUtil.createAttribute(null, null, "id", pageIDAttr, pageElement);
        final StringBuffer pageClassAttr = new StringBuffer();
        pageClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page")));
        if (pageIsReqPage) {
            pageClassAttr.append(' ');
            pageClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", pageClassAttr.toString(), pageElement);
        final Element pageTitleElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "div", pageElement);
        final String pageTitleIDAttr = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "title", page.getContentContainer().getOwnerID(), page.getParentDefinition().getID(), page.getID()));
        DOMUtil.createAttribute(null, null, "id", pageTitleIDAttr, pageTitleElement);
        final StringBuffer pageTitleClassAttr = new StringBuffer();
        pageTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "title")));
        pageTitleClassAttr.append(' ');
        pageTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "title")));
        if (pageIsReqPage) {
            pageTitleClassAttr.append(' ');
            pageTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "title", "current")));
            pageTitleClassAttr.append(' ');
            pageTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "title", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", pageTitleClassAttr.toString(), pageTitleElement);
        createSiteNavPageAnchorElement(pageRequest, page, pageIsReqPage, pageTitleElement, contentSignature);
        createSiteNavPageMetaPropsElement(pageRequest, page, pageIsReqPage, pageElement);
        if (!channelDisplayEnabled) return pageElement;
        final Map<Pattern, Pattern> withMatchingChannelGroupProps = ConversionUtil.invokeConverter(INCLUDE_CHANNEL_GROUP_WITH_PROP_MAP_CONVERTER, ConfigManager.getConfigProps(definition.getProperties(), INCLUDE_CHANNEL_GROUP_WITH_PROP_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RSProperties.RESULT_STRING_CONVERTER, false, false));
        final Map<Pattern, Pattern> withoutMatchingChannelGroupProps = ConversionUtil.invokeConverter(INCLUDE_CHANNEL_GROUP_WITHOUT_PROP_MAP_CONVERTER, ConfigManager.getConfigProps(definition.getProperties(), INCLUDE_CHANNEL_GROUP_WITHOUT_PROP_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RSProperties.RESULT_STRING_CONVERTER, false, false));
        final Map<Pattern, Pattern> withMatchingChannelProps = ConversionUtil.invokeConverter(INCLUDE_CHANNEL_WITH_PROP_MAP_CONVERTER, ConfigManager.getConfigProps(definition.getProperties(), INCLUDE_CHANNEL_WITH_PROP_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RSProperties.RESULT_STRING_CONVERTER, false, false));
        final Map<Pattern, Pattern> withoutMatchingChannelProps = ConversionUtil.invokeConverter(INCLUDE_CHANNEL_WITHOUT_PROP_MAP_CONVERTER, ConfigManager.getConfigProps(definition.getProperties(), INCLUDE_CHANNEL_WITHOUT_PROP_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RSProperties.RESULT_STRING_CONVERTER, false, false));
        final List<ContentManager.ChannelDefinition<?, ?>> channelDefinitions = portal.getContentManager().getChannelDefinitions(page, withMatchingChannelGroupProps, withoutMatchingChannelGroupProps, withMatchingChannelProps, withoutMatchingChannelProps, withMatchingChannelProps, withoutMatchingChannelProps, true, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders());
        if (channelDefinitions == null) return pageElement;
        final Element channelsElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "ol", pageElement);
        final StringBuffer channelsClassAttr = new StringBuffer();
        channelsClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "channels")));
        channelsClassAttr.append(' ');
        channelsClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "page", "channels", page.getParentDefinition().getID(), page.getID())));
        DOMUtil.createAttribute(null, null, "class", channelsClassAttr.toString(), channelsElement);
        for (ContentManager.ChannelGroupDefinition channelGroupDefinition : CollectionUtil.mkNotNull(page.getMatchingChildDefinitions(null, withMatchingChannelGroupProps, withoutMatchingChannelGroupProps, true, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), -1, false, true))) {
            for (ContentManager.ChannelSpecification<?> channelSpecification : CollectionUtil.mkNotNull(channelGroupDefinition.getMatchingChildDefinitions(null, withMatchingChannelProps, withoutMatchingChannelProps, true, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), -1, false, true))) {
                final boolean channelIsReqChannel;
                final ContentManager.ChannelDefinition<?, ?> channelDefinition;
                if ((pageIsReqPage) && (pageRequest.getMaximizedChannelKey() != null) && (pageRequest.getMaximizedChannelKey().equals(channelSpecification.getKey()))) {
                    channelIsReqChannel = true;
                    channelDefinition = pageRequest.getPage().getChannel(pageRequest.getMaximizedChannelKey()).getDefinition();
                } else {
                    channelIsReqChannel = false;
                    channelDefinition = ContentManager.AbstractContentDefinition.getContentDefinition(channelDefinitions, channelSpecification.getID());
                }
                if (channelDefinition == null) continue;
                createSiteNavChannelElement(pageRequest, page, channelDefinition, channelIsReqChannel, channelsElement, contentSignature);
            }
        }
        return pageElement;
    }

    /**
   * Generate the navigation for the specified <code>pageGroup</code>.
   * 
   * @see #GROUP_ANCHOR_DISABLE_PROP
   */
    protected Element createSiteNavPageGroupElement(final Page.Request pageRequest, final Element parentElement, final ContentManager.PageGroupDefinition pageGroup, final ContentManager.PageDefinition<?> firstPageInGroup) throws WWWeeePortal.Exception {
        final String portalID = portal.getPortalID();
        final String ownerID = pageGroup.getContentContainer().getOwnerID();
        final String pageGroupID = pageGroup.getID();
        final String channelID = definition.getID();
        final boolean reqPageInPageGroup = pageRequest.getPage().getKey().getPageGroupID().equals(pageGroupID);
        final String pageGroupTitle;
        if (reqPageInPageGroup) {
            pageGroupTitle = pageRequest.getPage().getGroupTitleText(pageRequest);
        } else {
            pageGroupTitle = ConfigManager.getConfigProp(pageGroup.getProperties(), Page.GROUP_TITLE_TEXT_PROP, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), null, RSProperties.RESULT_STRING_CONVERTER, pageGroupID, false, false);
        }
        final Element pageGroupElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "li", parentElement);
        final StringBuffer pageGroupClassAttr = new StringBuffer();
        pageGroupClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group")));
        pageGroupClassAttr.append(' ');
        pageGroupClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", ownerID, pageGroupID)));
        if (reqPageInPageGroup) {
            pageGroupClassAttr.append(' ');
            pageGroupClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", pageGroupClassAttr.toString(), pageGroupElement);
        final Element pageGroupTitleElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "div", pageGroupElement);
        final StringBuffer currentPageGroupTitleClassAttr = new StringBuffer();
        currentPageGroupTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "title")));
        currentPageGroupTitleClassAttr.append(' ');
        currentPageGroupTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "title")));
        currentPageGroupTitleClassAttr.append(' ');
        currentPageGroupTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "title", ownerID, pageGroupID)));
        if (reqPageInPageGroup) {
            currentPageGroupTitleClassAttr.append(' ');
            currentPageGroupTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "title", "current")));
            currentPageGroupTitleClassAttr.append(' ');
            currentPageGroupTitleClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "title", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", currentPageGroupTitleClassAttr.toString(), pageGroupTitleElement);
        final Element pageGroupAnchorElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "a", pageGroupTitleElement);
        final StringBuffer currentPageGroupAnchorClassAttr = new StringBuffer();
        currentPageGroupAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor")));
        currentPageGroupAnchorClassAttr.append(' ');
        currentPageGroupAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "anchor")));
        currentPageGroupAnchorClassAttr.append(' ');
        currentPageGroupAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "anchor", ownerID, pageGroupID)));
        if (reqPageInPageGroup) {
            currentPageGroupAnchorClassAttr.append(' ');
            currentPageGroupAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor", "current")));
            currentPageGroupAnchorClassAttr.append(' ');
            currentPageGroupAnchorClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "anchor", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", currentPageGroupAnchorClassAttr.toString(), pageGroupAnchorElement);
        if ((!reqPageInPageGroup) && (!getConfigProp(GROUP_ANCHOR_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue())) {
            final URI pageHrefURI = firstPageInGroup.getKey().getPageURI(pageRequest.getUriInfo(), null, null, false);
            DOMUtil.createAttribute(null, null, "href", pageHrefURI.toString(), pageGroupAnchorElement);
        }
        final Element pageGroupAnchorTextElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", pageGroupAnchorElement);
        final StringBuffer currentPageGroupAnchorTextClassAttr = new StringBuffer();
        currentPageGroupAnchorTextClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor", "text")));
        currentPageGroupAnchorTextClassAttr.append(' ');
        currentPageGroupAnchorTextClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "anchor", "text")));
        currentPageGroupAnchorTextClassAttr.append(' ');
        currentPageGroupAnchorTextClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "anchor", "text", ownerID, pageGroupID)));
        if (reqPageInPageGroup) {
            currentPageGroupAnchorTextClassAttr.append(' ');
            currentPageGroupAnchorTextClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "anchor", "text", "current")));
            currentPageGroupAnchorTextClassAttr.append(' ');
            currentPageGroupAnchorTextClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "anchor", "text", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", currentPageGroupAnchorTextClassAttr.toString(), pageGroupAnchorTextElement);
        DOMUtil.appendText(pageGroupAnchorTextElement, pageGroupTitle);
        final Element pageGroupPagesElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "ol", pageGroupElement);
        final StringBuffer pageGroupPagesClassAttr = new StringBuffer();
        pageGroupPagesClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "pages")));
        pageGroupPagesClassAttr.append(' ');
        pageGroupPagesClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "pages", ownerID, pageGroupID)));
        if (reqPageInPageGroup) {
            pageGroupPagesClassAttr.append(' ');
            pageGroupPagesClassAttr.append(ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(portalID, "channel", channelID, "group", "pages", "current")));
        }
        DOMUtil.createAttribute(null, null, "class", pageGroupPagesClassAttr.toString(), pageGroupPagesElement);
        return pageGroupPagesElement;
    }

    @Override
    public void doViewRequestImpl(final Page.Request pageRequest, final ViewResponse viewResponse) throws WWWeeePortal.Exception {
        final Page reqPage = pageRequest.getPage();
        final ContentManager.PageDefinition<?> reqPageDef = reqPage.getDefinition();
        final boolean displayChannelsOnAllPages = getConfigProp(PAGE_CHANNEL_DISPLAY_ALL_PAGES_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
        final boolean displayChannelsOnCurrentPage = getConfigProp(PAGE_CHANNEL_DISPLAY_CURRENT_PAGE_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
        final StringBuffer contentSignature = new StringBuffer();
        final Map<String, String> withMatchingPageContainerProps = ConversionUtil.invokeConverter(INCLUDE_PAGE_CONTAINER_WITH_PROP_MAP_CONVERTER, ConfigManager.getConfigProps(definition.getProperties(), INCLUDE_PAGE_CONTAINER_WITH_PROP_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RSProperties.RESULT_STRING_CONVERTER, false, false));
        final Map<String, String> withoutMatchingPageContainerProps = ConversionUtil.invokeConverter(INCLUDE_PAGE_CONTAINER_WITHOUT_PROP_MAP_CONVERTER, ConfigManager.getConfigProps(definition.getProperties(), INCLUDE_PAGE_CONTAINER_WITHOUT_PROP_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RSProperties.RESULT_STRING_CONVERTER, false, false));
        final Map<String, String> withMatchingPageGroupProps = ConversionUtil.invokeConverter(INCLUDE_PAGE_GROUP_WITH_PROP_MAP_CONVERTER, ConfigManager.getConfigProps(definition.getProperties(), INCLUDE_PAGE_GROUP_WITH_PROP_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RSProperties.RESULT_STRING_CONVERTER, false, false));
        final Map<String, String> withoutMatchingPageGroupProps = ConversionUtil.invokeConverter(INCLUDE_PAGE_GROUP_WITHOUT_PROP_MAP_CONVERTER, ConfigManager.getConfigProps(definition.getProperties(), INCLUDE_PAGE_GROUP_WITHOUT_PROP_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RSProperties.RESULT_STRING_CONVERTER, false, false));
        final Map<String, String> withMatchingPageProps = ConversionUtil.invokeConverter(INCLUDE_PAGE_WITH_PROP_MAP_CONVERTER, ConfigManager.getConfigProps(definition.getProperties(), INCLUDE_PAGE_WITH_PROP_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RSProperties.RESULT_STRING_CONVERTER, false, false));
        final Map<String, String> withoutMatchingPageProps = ConversionUtil.invokeConverter(INCLUDE_PAGE_WITHOUT_PROP_MAP_CONVERTER, ConfigManager.getConfigProps(definition.getProperties(), INCLUDE_PAGE_WITHOUT_PROP_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), RSProperties.RESULT_STRING_CONVERTER, false, false));
        List<ContentManager.PageDefinition<?>> pageDefinitions = portal.getContentManager().getPageDefinitions(withMatchingPageContainerProps, withoutMatchingPageContainerProps, withMatchingPageGroupProps, withoutMatchingPageGroupProps, withMatchingPageProps, withoutMatchingPageProps, true, pageRequest.getUriInfo(), pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), displayChannelsOnAllPages, 0, -1);
        if ((pageDefinitions == null) || (ContentManager.PageDefinition.getPageDefinitionIndex(pageDefinitions, reqPage.getKey()) < 0)) {
            if (pageDefinitions == null) pageDefinitions = new ArrayList<ContentManager.PageDefinition<?>>(1);
            pageDefinitions.add(reqPageDef);
        }
        final Element pagesElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "ol", viewResponse.getContentContainerElement());
        setIDAndClassAttrs(pagesElement, Arrays.asList("nav", "pages"), null, null);
        ContentManager.PageGroupDefinition lastLoopPageGroup = null;
        Element pageGroupPagesElement = null;
        for (ContentManager.PageDefinition<?> loopPage : CollectionUtil.mkNotNull(pageDefinitions)) {
            final boolean pageIsReqPage = loopPage.getKey().equals(reqPageDef.getKey());
            final ContentManager.PageGroupDefinition loopPageGroup = loopPage.getParentDefinition();
            if ((lastLoopPageGroup == null) || (!loopPageGroup.getKey().equals(lastLoopPageGroup.getKey()))) {
                pageGroupPagesElement = createSiteNavPageGroupElement(pageRequest, pagesElement, loopPageGroup, loopPage);
            }
            createSiteNavPageElement(pageRequest, (pageIsReqPage) ? reqPageDef : loopPage, pageIsReqPage, pageGroupPagesElement, contentSignature, displayChannelsOnAllPages || (displayChannelsOnCurrentPage && pageIsReqPage));
            lastLoopPageGroup = loopPageGroup;
        }
        final EntityTag entityTag = Page.createEntityTag(pageRequest, viewResponse.getCacheControl(), contentSignature, null, viewResponse.getLastModified(), false);
        if (entityTag != null) viewResponse.setEntityTag(entityTag);
        return;
    }
}
