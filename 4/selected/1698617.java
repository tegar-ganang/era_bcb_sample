package org.wwweeeportal.portal.channels;

import java.net.*;
import java.util.*;
import org.w3c.dom.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.xml.html.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;

/**
 * <p>
 * A {@link Channel} which renders a heading into the content of the {@link Page}.
 * </p>
 * 
 * <p>
 * By default, XHTML with the following general structure will be generated...
 * </p>
 * 
 * <p>
 * <code>
 * &lt;{@link #HEADING_ELEMENT_NAME_PROP h1}&gt;<br />
 * &#160;&#160;&lt;span&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;a href=&quot;/{@link org.wwweeeportal.portal.ContentManager.PageDefinition.Key#getPageURI(UriInfo, Map, String, boolean) link}_to_first_page&quot;&gt;&lt;span&gt;{@linkplain WWWeeePortal#NAME_PROP Portal Name}&lt;/span&gt;&lt;/a&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;span&gt; - &lt;/span&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;{@linkplain #PAGE_GROUP_ANCHOR_DISABLE_PROP a} {@link #PAGE_GROUP_ANCHOR_HREF_PROP href}=&quot;___&quot; {@link #PAGE_GROUP_ANCHOR_TITLE_PROP title}=&quot;___&quot; {@link #PAGE_GROUP_ANCHOR_REL_PROP rel}=&quot;___&quot;&gt;Current Page's {@linkplain Page#GROUP_TITLE_TEXT_PROP Group Title}&lt;/{@linkplain #PAGE_GROUP_ANCHOR_DISABLE_PROP a}&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;span&gt; - &lt;/span&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;a {@link #PAGE_TITLE_ANCHOR_HREF_PROP href}=&quot;/{@link org.wwweeeportal.portal.ContentManager.PageDefinition.Key#getPageURI(UriInfo, Map, String, boolean) link}_back_to_page_if_channel_maximized&quot; {@link #PAGE_TITLE_ANCHOR_TITLE_PROP title}=&quot;___&quot; {@link #PAGE_TITLE_ANCHOR_REL_PROP rel}=&quot;___&quot;&gt;&lt;span&gt;Current Page {@linkplain Page#TITLE_TEXT_PROP Title}&lt;/span&gt;&lt;/a&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;span&gt; - &lt;/span&gt;<br />
 * &#160;&#160;&#160;&#160;&lt;span&gt;Currently {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) Maximized} Channel {@linkplain Channel#TITLE_TEXT_PROP Title}&lt;/span&gt;<br />
 * &#160;&#160;&lt;/span&gt;<br />
 * &lt;/{@link #HEADING_ELEMENT_NAME_PROP h1}&gt;<br />
 * </code>
 * </p>
 * 
 * <p>
 * The markup also includes <code>id</code> and <code>class</code> attributes (not shown) to enable CSS styling of the
 * heading. This channel will use &quot;<code>header</code>&quot; as it's default
 * {@linkplain #getSectionType(Page.Request) section type}.
 * </p>
 * 
 * <p>
 * This channel's default {@linkplain #getMaximizationNormalPriority(Page.Request) maximization priority} will cause it
 * to be displayed even while others are {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel)
 * maximized}, and it's own maximization will be {@linkplain #isMaximizationDisabled(Page.Request) disabled} by default.
 * Note that if <em>no</em> channel is maximized, the related elements of the heading will not be shown, and the page
 * title anchor will not be a link (no &quot;<code>href</code>&quot; {@linkplain Attr attribute} will be added).
 * </p>
 * 
 * <h3 id="configuration">Configuration</h3>
 * <p>
 * In addition to those inherited from the {@link Channel} class, the following {@linkplain ConfigManager configuration
 * properties} are supported by this class:
 * </p>
 * <ul>
 * <li>{@link #HEADING_ELEMENT_NAME_PROP}</li>
 * <li>{@link #PAGE_GROUP_ANCHOR_DISABLE_PROP}</li>
 * <li>{@link #PAGE_GROUP_ANCHOR_HREF_PROP}</li>
 * <li>{@link #PAGE_GROUP_ANCHOR_TITLE_PROP}</li>
 * <li>{@link #PAGE_GROUP_ANCHOR_REL_PROP}</li>
 * <li>{@link #PAGE_TITLE_ANCHOR_HREF_PROP}</li>
 * <li>{@link #PAGE_TITLE_ANCHOR_TITLE_PROP}</li>
 * <li>{@link #PAGE_TITLE_ANCHOR_REL_PROP}</li>
 * </ul>
 */
public class PageHeadingChannel extends Channel {

    /**
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the
   * {@linkplain Element#getLocalName() local name} of the {@linkplain HTMLUtil#HTML_NS_URI HTML} {@link Element} to be
   * created under the channel {@linkplain org.wwweeeportal.portal.Channel.ViewResponse#getContentContainerElement()
   * container} to house the generated heading content.
   * 
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String HEADING_ELEMENT_NAME_PROP = "PageHeadingChannel.Heading.ElementName";

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property indicating an anchor (
   * <code>&lt;a&gt;</code>) {@link Element} containing the current {@linkplain Page#GROUP_TITLE_TEXT_PROP Page Group
   * Title} should not be generated as part of this heading.
   * 
   * @see Page#GROUP_TITLE_TEXT_PROP
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String PAGE_GROUP_ANCHOR_DISABLE_PROP = "PageHeadingChannel.PageGroup.Anchor.Disable";

    /**
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the value of an &quot;
   * <code>href</code>&quot; {@linkplain Attr attribute} to be included on the {@linkplain Page#GROUP_TITLE_TEXT_PROP
   * Page Group Title} anchor (<code>&lt;a&gt;</code>) {@link Element} within the generated heading.
   * 
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String PAGE_GROUP_ANCHOR_HREF_PROP = "PageHeadingChannel.PageGroup.Anchor.Href";

    /**
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the value of a &quot;
   * <code>title</code>&quot; {@linkplain Attr attribute} to be included on the {@linkplain Page#GROUP_TITLE_TEXT_PROP
   * Page Group Title} anchor (<code>&lt;a&gt;</code>) {@link Element} within the generated heading.
   * 
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String PAGE_GROUP_ANCHOR_TITLE_PROP = "PageHeadingChannel.PageGroup.Anchor.Title";

    /**
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the value of a &quot;
   * <code>rel</code>&quot; {@linkplain Attr attribute} to be included on the {@linkplain Page#GROUP_TITLE_TEXT_PROP
   * Page Group Title} anchor (<code>&lt;a&gt;</code>) {@link Element} within the generated heading.
   * 
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String PAGE_GROUP_ANCHOR_REL_PROP = "PageHeadingChannel.PageGroup.Anchor.Rel";

    /**
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the value of an &quot;
   * <code>href</code>&quot; {@linkplain Attr attribute} to be included on the {@linkplain Page#TITLE_TEXT_PROP Page
   * Title} anchor (<code>&lt;a&gt;</code>) {@link Element} within the generated heading when no channel is
   * {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximized} (in which case the
   * {@linkplain org.wwweeeportal.portal.ContentManager.PageDefinition.Key#getPageURI(UriInfo, Map, String, boolean)
   * Page URI} will be used).
   * 
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String PAGE_TITLE_ANCHOR_HREF_PROP = "PageHeadingChannel.PageTitle.Anchor.Href";

    /**
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the value of a &quot;
   * <code>title</code>&quot; {@linkplain Attr attribute} to be included on the {@linkplain Page#TITLE_TEXT_PROP Page
   * Title} anchor (<code>&lt;a&gt;</code>) {@link Element} within the generated heading when no channel is
   * {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximized}.
   * 
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String PAGE_TITLE_ANCHOR_TITLE_PROP = "PageHeadingChannel.PageTitle.Anchor.Title";

    /**
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the value of a &quot;
   * <code>rel</code>&quot; {@linkplain Attr attribute} to be included on the {@linkplain Page#TITLE_TEXT_PROP Page
   * Title} anchor (<code>&lt;a&gt;</code>) {@link Element} within the generated heading when no channel is
   * {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximized} (in which case &quot;
   * <code>up</code>&quot; will be used).
   * 
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String PAGE_TITLE_ANCHOR_REL_PROP = "PageHeadingChannel.PageTitle.Anchor.Rel";

    /**
   * Construct a new <code>PageHeadingChannel</code> instance.
   */
    public PageHeadingChannel(final WWWeeePortal portal, final ContentManager.ChannelDefinition<?, ? extends PageHeadingChannel> definition, final Page page) throws WWWeeePortal.Exception {
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
        if (getConfigProp(SECTION_TYPE_PROP, pageRequest, null, true) == null) return "header";
        return super.getSectionType(pageRequest);
    }

    @Override
    public void doViewRequestImpl(final Page.Request pageRequest, final ViewResponse viewResponse) throws WWWeeePortal.Exception {
        final StringBuffer contentSignature = new StringBuffer();
        final Element contentContainerElement = viewResponse.getContentContainerElement();
        final Element pageHeadingElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, getConfigProp(HEADING_ELEMENT_NAME_PROP, pageRequest, "h1", false), contentContainerElement);
        setIDAndClassAttrs(pageHeadingElement, Arrays.asList("heading"), null, null);
        final Element pageHeadingTextElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", pageHeadingElement);
        setIDAndClassAttrs(pageHeadingTextElement, Arrays.asList("heading", "text"), null, null);
        final String[][] extraPageHeadingClasses = new String[][] { new String[] { portal.getPortalID(), "channel", definition.getID() } };
        final String[][] extraPageHeadingDividerClasses = new String[][] { new String[] { portal.getPortalID(), "channel", definition.getID(), "divider" } };
        final String[][] extraPageHeadingTextClasses = new String[][] { new String[] { portal.getPortalID(), "channel", definition.getID(), "text" } };
        final Element portalNameAnchorElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "a", pageHeadingTextElement);
        setIDAndClassAttrs(portalNameAnchorElement, Arrays.asList("heading", "portal", "anchor"), extraPageHeadingClasses, null);
        final Element portalNameAnchorTextElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", portalNameAnchorElement);
        setIDAndClassAttrs(portalNameAnchorTextElement, Arrays.asList("heading", "portal", "anchor", "text"), extraPageHeadingTextClasses, null);
        final String portalName = portal.getName(pageRequest.getSecurityContext(), pageRequest.getHttpHeaders());
        DOMUtil.appendText(portalNameAnchorTextElement, portalName);
        contentSignature.append(portalName);
        final ContentManager.PageDefinition<?> defaultPageDef = CollectionUtil.first(portal.getContentManager().getPageDefinitions(null, null, null, null, null, null, true, pageRequest.getUriInfo(), pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), false, 0, 1), null);
        if ((defaultPageDef != null) && ((!defaultPageDef.getKey().equals(pageRequest.getPage().getKey())) || (pageRequest.getMaximizedChannelKey() != null))) {
            final URI defaultPageURI = defaultPageDef.getKey().getPageURI(pageRequest.getUriInfo(), null, null, false);
            DOMUtil.createAttribute(null, null, "href", defaultPageURI.toString(), portalNameAnchorElement);
            DOMUtil.createAttribute(null, null, "rel", "first", portalNameAnchorElement);
        }
        if (!getConfigProp(PAGE_GROUP_ANCHOR_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue()) {
            final Element pageGroupDividerElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", pageHeadingTextElement);
            setIDAndClassAttrs(pageGroupDividerElement, Arrays.asList("heading", "group", "divider"), extraPageHeadingDividerClasses, null);
            DOMUtil.appendText(pageGroupDividerElement, " - ");
            contentSignature.append(" - ");
            final Element pageGroupAnchorElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "a", pageHeadingTextElement);
            setIDAndClassAttrs(pageGroupAnchorElement, Arrays.asList("heading", "group", "anchor"), extraPageHeadingClasses, null);
            final String pageGroupTitle = pageRequest.getPage().getGroupTitleText(pageRequest);
            DOMUtil.appendText(pageGroupAnchorElement, pageGroupTitle);
            contentSignature.append(pageGroupTitle);
            final String pageGroupAnchorHrefProp = getConfigProp(PAGE_GROUP_ANCHOR_HREF_PROP, pageRequest, null, true);
            if (pageGroupAnchorHrefProp != null) {
                DOMUtil.createAttribute(null, null, "href", pageGroupAnchorHrefProp, pageGroupAnchorElement);
                final String pageGroupAnchorTitleProp = getConfigProp(PAGE_GROUP_ANCHOR_TITLE_PROP, pageRequest, null, true);
                if (pageGroupAnchorTitleProp != null) {
                    DOMUtil.createAttribute(null, null, "title", pageGroupAnchorTitleProp, pageGroupAnchorElement);
                }
                final String pageGroupAnchorRelProp = getConfigProp(PAGE_GROUP_ANCHOR_REL_PROP, pageRequest, null, true);
                if (pageGroupAnchorRelProp != null) {
                    DOMUtil.createAttribute(null, null, "rel", pageGroupAnchorRelProp, pageGroupAnchorElement);
                }
            }
        }
        final Element pageTitleDividerElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", pageHeadingTextElement);
        setIDAndClassAttrs(pageTitleDividerElement, Arrays.asList("heading", "page", "divider"), extraPageHeadingDividerClasses, null);
        DOMUtil.appendText(pageTitleDividerElement, " - ");
        contentSignature.append(" - ");
        final Element pageTitleAnchorElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "a", pageHeadingTextElement);
        setIDAndClassAttrs(pageTitleAnchorElement, Arrays.asList("heading", "page", "anchor"), extraPageHeadingClasses, null);
        final Element pageTitleAnchorTextElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", pageTitleAnchorElement);
        setIDAndClassAttrs(pageTitleAnchorTextElement, Arrays.asList("heading", "page", "anchor", "text"), extraPageHeadingTextClasses, null);
        final String pageTitle = pageRequest.getPage().getTitleText(pageRequest);
        DOMUtil.appendText(pageTitleAnchorTextElement, pageTitle);
        contentSignature.append(pageTitle);
        if (pageRequest.getMaximizedChannelKey() != null) {
            final URI pageURI = pageRequest.getPage().getKey().getPageURI(pageRequest.getUriInfo(), null, null, false);
            DOMUtil.createAttribute(null, null, "href", pageURI.toString(), pageTitleAnchorElement);
            DOMUtil.createAttribute(null, null, "rel", "up", pageTitleAnchorElement);
        } else {
            final String pageTitleAnchorHrefProp = getConfigProp(PAGE_TITLE_ANCHOR_HREF_PROP, pageRequest, null, true);
            if (pageTitleAnchorHrefProp != null) {
                DOMUtil.createAttribute(null, null, "href", pageTitleAnchorHrefProp, pageTitleAnchorElement);
                final String pageTitleAnchorTitleProp = getConfigProp(PAGE_TITLE_ANCHOR_TITLE_PROP, pageRequest, null, true);
                if (pageTitleAnchorTitleProp != null) DOMUtil.createAttribute(null, null, "title", pageTitleAnchorTitleProp, pageTitleAnchorElement);
                final String pageTitleAnchorRelProp = getConfigProp(PAGE_TITLE_ANCHOR_REL_PROP, pageRequest, null, true);
                if (pageTitleAnchorRelProp != null) DOMUtil.createAttribute(null, null, "rel", pageTitleAnchorRelProp, pageTitleAnchorElement);
            }
        }
        if (pageRequest.getMaximizedChannelKey() != null) {
            final Element channelTitleDividerElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", pageHeadingTextElement);
            setIDAndClassAttrs(channelTitleDividerElement, Arrays.asList("heading", "channel", "divider"), extraPageHeadingDividerClasses, null);
            DOMUtil.appendText(channelTitleDividerElement, " - ");
            contentSignature.append(" - ");
            final Element channelTitleElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", pageHeadingTextElement);
            setIDAndClassAttrs(channelTitleElement, Arrays.asList("heading", "channel", "text"), extraPageHeadingTextClasses, null);
            String channelTitle = null;
            if (!pageRequest.isMaximized(this)) {
                try {
                    final ViewResponse maximizedChannelResponse = Page.getChannelViewResponse(pageRequest.getChannelViewTasks(), pageRequest.getMaximizedChannelKey());
                    if (maximizedChannelResponse != null) {
                        if (!MiscUtil.equal(viewResponse.getLocale(), maximizedChannelResponse.getLocale())) {
                            DOMUtil.setXMLLangAttr(channelTitleElement, maximizedChannelResponse.getLocale());
                        }
                        channelTitle = maximizedChannelResponse.getTitle();
                    }
                } catch (WebApplicationException wae) {
                } catch (WWWeeePortal.Exception wpe) {
                }
            }
            if (channelTitle == null) channelTitle = pageRequest.getPage().getChannel(pageRequest.getMaximizedChannelKey()).getTitleText(pageRequest);
            DOMUtil.appendText(channelTitleElement, channelTitle);
            contentSignature.append(channelTitle);
        }
        final EntityTag entityTag = Page.createEntityTag(pageRequest, viewResponse.getCacheControl(), contentSignature, null, viewResponse.getLastModified(), false);
        if (entityTag != null) viewResponse.setEntityTag(entityTag);
        return;
    }
}
