package org.wwweeeportal.portal.channelplugins;

import java.util.*;
import org.w3c.dom.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.xml.html.*;
import org.wwweeeportal.portal.*;

/**
 * <p>
 * A {@link org.wwweeeportal.portal.Channel.Plugin Plugin} providing a channel {@linkplain AbstractChannelControl
 * control} {@linkplain AbstractChannelAugmentation augmentation} rendering user interface markup to &quot;
 * {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximize}&quot; or &quot;restore&quot; the
 * channel {@linkplain Channel#VIEW_MODE view}.
 * </p>
 * 
 * <p>
 * By default, XHTML with the following general structure will be generated...
 * </p>
 * 
 * <p>
 * <code>
 * &lt;{@link AbstractChannelControl#createControlsElement(Page.Request, Channel.ViewResponse, Element) ol}&gt;<br />
 * &#160;&#160;&lt;{@link AbstractChannelControl#createControlElement(Page.Request, Channel.ViewResponse, Element) li}&gt;&lt;{@link #createMaximizeControlElement(Page.Request, Channel.ViewResponse, Element) a} href=&quot;/{@link org.wwweeeportal.portal.ContentManager.ChannelSpecification.Key#getChannelURI(UriInfo, String, String, Map, String, boolean) link}_from_page_to_maximized_channel_view&quot; title=&quot;Maximize&quot;&gt;&lt;span&gt;{@link WWWeeePortal#getPortalResourceBundle(HttpHeaders) Maximize}&lt;/span&gt;&lt;/a&gt;&lt;/{@link AbstractChannelControl#createControlElement(Page.Request, Channel.ViewResponse, Element) li}&gt;<br />
 * &lt;/{@link AbstractChannelControl#createControlsElement(Page.Request, Channel.ViewResponse, Element) ol}&gt;<br />
 * </code>
 * </p>
 * 
 * <p>
 * Or, if the channel is {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximized}...
 * </p>
 * 
 * <p>
 * <code>
 * &lt;{@link AbstractChannelControl#createControlsElement(Page.Request, Channel.ViewResponse, Element) ol}&gt;<br />
 * &#160;&#160;&lt;{@link AbstractChannelControl#createControlElement(Page.Request, Channel.ViewResponse, Element) li}&gt;&lt;{@link #createRestoreControlElement(Page.Request, Channel.ViewResponse, Element) a} href=&quot;/{@link org.wwweeeportal.portal.ContentManager.PageDefinition.Key#getPageURI(UriInfo, Map, String, boolean) link}_from_maximized_channel_to_page&quot; rel=&quot;up&quot; title=&quot;Restore&quot;&gt;&lt;span&gt;{@link WWWeeePortal#getPortalResourceBundle(HttpHeaders) Restore}&lt;/span&gt;&lt;/a&gt;&lt;/{@link AbstractChannelControl#createControlElement(Page.Request, Channel.ViewResponse, Element) li}&gt;<br />
 * &lt;/{@link AbstractChannelControl#createControlsElement(Page.Request, Channel.ViewResponse, Element) ol}&gt;<br />
 * </code>
 * </p>
 * 
 * <p>
 * The markup also includes <code>id</code> and <code>class</code> attributes (not shown) to enable CSS styling of the
 * control.
 * </p>
 * 
 * <h3 id="configuration">Configuration</h3>
 * <p>
 * This class provides no {@linkplain ConfigManager configuration properties} in addition to those inherited from the
 * {@link AbstractChannelControl}.
 * </p>
 */
public class ChannelMaximizationControl extends AbstractChannelControl {

    /**
   * Construct a new <code>ChannelMaximizationControl</code> instance.
   */
    public ChannelMaximizationControl(final Channel channel, final ContentManager.ChannelPluginDefinition<?> definition) throws WWWeeePortal.Exception {
        super(channel, definition);
        return;
    }

    @Override
    protected String getControlName(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        return "maximization";
    }

    /**
   * Generate the user interface {@linkplain #createControlContent(Page.Request, Channel.ViewResponse, Element) content}
   * allowing the user to {@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximize} the channel.
   * 
   * @see #createControlContent(Page.Request, Channel.ViewResponse, Element)
   */
    protected Element createMaximizeControlElement(final Page.Request pageRequest, final Channel.ViewResponse viewResponse, final Element controlElement) throws WWWeeePortal.Exception {
        final ResourceBundle wwweeeResourceBundle = WWWeeePortal.getPortalResourceBundle(pageRequest.getHttpHeaders());
        final Element maxElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "a", controlElement);
        getChannel().setIDAndClassAttrs(maxElement, Arrays.asList("controls", "maximize"), new String[][] { new String[] { getPortal().getPortalID(), "channel", "controls", "maximization" } }, null);
        if (!MiscUtil.equal(wwweeeResourceBundle.getLocale(), viewResponse.getLocale())) DOMUtil.setXMLLangAttr(maxElement, wwweeeResourceBundle.getLocale());
        final String maxElementTitle = wwweeeResourceBundle.getString("channel_control_maximize");
        DOMUtil.createAttribute(null, null, "title", maxElementTitle, maxElement);
        final ContentManager.ChannelSpecification<?> channelSpecification = pageRequest.getChannelSpecification(getChannel());
        final String maxElementHref = channelSpecification.getKey().getChannelURI(pageRequest.getUriInfo(), Channel.VIEW_MODE, null, null, null, false).toString();
        DOMUtil.createAttribute(null, null, "href", maxElementHref, maxElement);
        final Element maxTextElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", maxElement);
        getChannel().setIDAndClassAttrs(maxTextElement, Arrays.asList("controls", "maximize", "text"), null, null);
        DOMUtil.appendText(maxTextElement, maxElementTitle);
        return maxElement;
    }

    /**
   * Generate the user interface {@linkplain #createControlContent(Page.Request, Channel.ViewResponse, Element) content}
   * allowing the user to un-{@linkplain org.wwweeeportal.portal.Page.Request#isMaximized(Channel) maximize} (restore)
   * the channel.
   * 
   * @see #createControlContent(Page.Request, Channel.ViewResponse, Element)
   */
    protected Element createRestoreControlElement(final Page.Request pageRequest, final Channel.ViewResponse viewResponse, final Element controlElement) throws WWWeeePortal.Exception {
        final ResourceBundle wwweeeResourceBundle = WWWeeePortal.getPortalResourceBundle(pageRequest.getHttpHeaders());
        final Element unmaxElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "a", controlElement);
        getChannel().setIDAndClassAttrs(unmaxElement, Arrays.asList("controls", "restore"), new String[][] { new String[] { getPortal().getPortalID(), "channel", "controls", "maximization" } }, null);
        if (!MiscUtil.equal(wwweeeResourceBundle.getLocale(), viewResponse.getLocale())) DOMUtil.setXMLLangAttr(unmaxElement, wwweeeResourceBundle.getLocale());
        final String unmaxElementTitle = wwweeeResourceBundle.getString("channel_control_restore");
        DOMUtil.createAttribute(null, null, "title", unmaxElementTitle, unmaxElement);
        final String unmaxElementHref = pageRequest.getPage().getKey().getPageURI(pageRequest.getUriInfo(), null, null, false).toString();
        DOMUtil.createAttribute(null, null, "href", unmaxElementHref, unmaxElement);
        DOMUtil.createAttribute(null, null, "rel", "up", unmaxElement);
        final Element unmaxTextElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "span", unmaxElement);
        getChannel().setIDAndClassAttrs(unmaxTextElement, Arrays.asList("controls", "restore", "text"), null, null);
        DOMUtil.appendText(unmaxTextElement, unmaxElementTitle);
        return unmaxElement;
    }

    @Override
    protected void createControlContent(final Page.Request pageRequest, final Channel.ViewResponse viewResponse, final Element controlElement) throws WWWeeePortal.Exception {
        if (pageRequest.isMaximized(getChannel())) {
            createRestoreControlElement(pageRequest, viewResponse, controlElement);
        } else {
            createMaximizeControlElement(pageRequest, viewResponse, controlElement);
        }
        return;
    }
}
