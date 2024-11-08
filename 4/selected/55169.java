package org.wwweeeportal.portal.contentstores;

import java.net.*;
import java.util.*;
import org.w3c.dom.*;
import javax.ws.rs.core.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.logging.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;

/**
 * Generate {@link org.wwweeeportal.portal.Page}s for Web Feeds from an OPML file.
 * 
 * @author <a href="http://www.hubick.com/">Chris Hubick</a>
 */
public class OPMLContentStore extends FeedContentStore {

    public static final String OPML_RESOURCE_PROP = "OPMLContentStore.OPMLSource";

    public static final String DEFAULT_OPML_RESOURCE = "/feeds-opml.xml";

    protected final List<Outline> pageOutlines = new ArrayList<Outline>();

    public OPMLContentStore(final ContentManager contentManager, final RSProperties properties) throws WWWeeePortal.Exception {
        super(contentManager, properties);
        return;
    }

    protected void createOutlines(final Outline parentOutline, final Node parentNode) throws WWWeeePortal.Exception {
        final List<Element> outlineElements = DOMUtil.getChildElements(parentNode, null, "outline");
        if (outlineElements == null) {
            return;
        }
        for (Element outlineElement : outlineElements) {
            final Outline outline = new Outline(parentOutline, outlineElement);
            if (outline.getFeedURL() != null) pageOutlines.add(outline);
            createOutlines(outline, outlineElement);
        }
        return;
    }

    @Override
    protected void initInternal(final LogAnnotation.Message logMessage) throws WWWeeePortal.Exception {
        super.initInternal(logMessage);
        final Document opmlDocument = ConfigManager.getConfigProp(properties, OPML_RESOURCE_PROP, null, null, new RSProperties.Entry<String>(OPML_RESOURCE_PROP, DEFAULT_OPML_RESOURCE, Locale.ROOT, null), getPortal().getMarkupManager().getConfigDocumentConverter(), null, false, false);
        final Element opmlElement = DOMUtil.getChildElement(opmlDocument, null, "opml");
        final Element opmlBodyElement = DOMUtil.getChildElement(opmlElement, null, "body");
        createOutlines(null, opmlBodyElement);
        return;
    }

    @Override
    protected void destroyInternal(final LogAnnotation.Message logMessage) {
        super.destroyInternal(logMessage);
        return;
    }

    protected static final Outline getPageOutline(final List<Outline> outlines, final ContentManager.PageDefinition.Key pageKey) {
        for (Outline outline : outlines) {
            if (!outline.getPageKey().equals(pageKey)) continue;
            return outline;
        }
        return null;
    }

    @Override
    protected FeedPageDescriptor getFeedPageDescriptor(final ContentManager.PageDefinition.Key pageKey) throws WWWeeePortal.Exception {
        return getPageOutline(pageOutlines, pageKey);
    }

    @Override
    protected List<FeedPageDescriptor> getFeedPageDescriptors(final SecurityContext securityContext) throws WWWeeePortal.Exception {
        if (pageOutlines.isEmpty()) return null;
        final List<FeedPageDescriptor> feedPageDescriptors = new ArrayList<FeedPageDescriptor>(pageOutlines.size());
        for (Outline outline : pageOutlines) {
            final RSAccessControl pageAccessControl = outline.getPageAccessControl();
            if ((pageAccessControl != null) && (!pageAccessControl.hasAccess(securityContext))) continue;
            feedPageDescriptors.add(outline);
        }
        return (!feedPageDescriptors.isEmpty()) ? feedPageDescriptors : null;
    }

    /**
   * Represents the Feed information from an OPML outline element.
   */
    protected class Outline implements FeedPageDescriptor {

        protected static final String DEFAULT_PAGE_GROUP_ID = "feeds";

        protected final Outline parent;

        protected final RSProperties feedProperties;

        protected final URL xmlUrl;

        protected final ContentManager.PageContentContainer contentContainer;

        protected final ContentManager.PageGroupDefinition pageGroupDefinition;

        protected final ContentManager.PageDefinition.Key pageDefinitionKey;

        public Outline(final Outline parent, final Element outlineElement) throws WWWeeePortal.Exception {
            this.parent = parent;
            contentContainer = templateContentContainer.duplicate(null, null, null, false, null);
            feedProperties = new RSProperties(this, contentContainer.getProperties());
            final NamedNodeMap attributesMap = outlineElement.getAttributes();
            for (int i = 0; i < attributesMap.getLength(); i++) {
                final Node attr = attributesMap.item(i);
                final String attrName = attr.getLocalName();
                final String attrValue = attr.getNodeValue();
                if (attrValue == null) continue;
                feedProperties.setProp("Outline." + attrName, attrValue, Locale.ROOT, null);
            }
            final String xmlUrlAttr = feedProperties.getProp("Outline.xmlUrl", true, null, null, null, RSProperties.RESULT_STRING_CONVERTER, null, false, true);
            if (xmlUrlAttr != null) {
                if (xmlUrlAttr.charAt(0) == '/') {
                    xmlUrl = getPortal().getConfigManager().getResourceLocator().getResource(xmlUrlAttr, false);
                } else {
                    try {
                        xmlUrl = new URL(xmlUrlAttr);
                    } catch (MalformedURLException mue) {
                        throw new ConfigManager.ConfigException(mue);
                    }
                }
            } else {
                xmlUrl = null;
            }
            if (feedProperties.getProp("Outline.title", true, null, null, null, RSProperties.RESULT_STRING_CONVERTER, null, false, false) != null) {
                feedProperties.setProp(Page.TITLE_TEXT_PROP, feedProperties.getProp("Outline.title", true, null, null, null, RSProperties.RESULT_STRING_CONVERTER, null, false, false), Locale.ROOT, null);
            } else if (feedProperties.getProp("Outline.text", true, null, null, null, RSProperties.RESULT_STRING_CONVERTER, null, false, false) != null) {
                feedProperties.setProp(Page.TITLE_TEXT_PROP, feedProperties.getProp("Outline.text", true, null, null, null, RSProperties.RESULT_STRING_CONVERTER, null, false, false), Locale.ROOT, null);
            }
            if (parent != null) {
                if (parent.feedProperties.getProp("Outline.title", true, null, null, null, RSProperties.RESULT_STRING_CONVERTER, null, false, false) != null) {
                    feedProperties.setProp(Page.GROUP_TITLE_TEXT_PROP, parent.feedProperties.getProp("Outline.title", true, null, null, null, RSProperties.RESULT_STRING_CONVERTER, null, false, false), Locale.ROOT, null);
                } else if (parent.feedProperties.getProp("Outline.text", true, null, null, null, RSProperties.RESULT_STRING_CONVERTER, null, false, false) != null) {
                    feedProperties.setProp(Page.GROUP_TITLE_TEXT_PROP, parent.feedProperties.getProp("Outline.text", true, null, null, null, RSProperties.RESULT_STRING_CONVERTER, null, false, false), Locale.ROOT, null);
                }
            }
            final String pageGroupID = (parent != null) ? createID(StringUtil.toString(parent.feedProperties.getProp("Outline.text", true, null, null, null, RSProperties.RESULT_STRING_CONVERTER, null, false, false), null), DEFAULT_PAGE_GROUP_ID) : DEFAULT_PAGE_GROUP_ID;
            pageGroupDefinition = new ContentManager.PageGroupDefinition(contentContainer, feedProperties, pageGroupID, null);
            final String pageID = createID(feedProperties.getProp("Outline.text", true, null, null, null, RSProperties.RESULT_STRING_CONVERTER, null, false, false), null);
            pageDefinitionKey = new ContentManager.PageDefinition.Key(pageGroupDefinition.getKey().getOwnerID(), pageGroupID, pageID);
            return;
        }

        @Override
        public ContentManager.PageDefinition.Key getPageKey() {
            return pageDefinitionKey;
        }

        @Override
        public ContentManager.PageGroupDefinition getPageGroupDefinition() {
            return pageGroupDefinition;
        }

        @Override
        public RSProperties getPageProperties() {
            return feedProperties;
        }

        @Override
        public boolean createProxyChannels() {
            return false;
        }

        @Override
        public RSAccessControl getChannelAccessControl() {
            return null;
        }

        @Override
        public URL getFeedURL() {
            return xmlUrl;
        }

        @Override
        public RSAccessControl getPageAccessControl() {
            return null;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '#' + pageDefinitionKey + '[' + hashCode() + ']';
        }
    }
}
