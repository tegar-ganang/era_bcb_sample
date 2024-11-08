package org.wwweeeportal.portal.channelplugins;

import java.util.*;
import org.w3c.dom.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.xml.html.*;
import org.wwweeeportal.portal.*;

/**
 * <p>
 * An abstract base for creating a {@linkplain AbstractChannelAugmentation channel augmentation}
 * {@link org.wwweeeportal.portal.Channel.Plugin Plugin}, which adds some form of user interface &quot;control&quot; for
 * interacting with the <em>channel</em> (as opposed to it's content) to an ordered list of &quot;controls&quot;.
 * </p>
 * 
 * <p>
 * This class maintains the default <code>&quot;header&quot;</code> value for the augmentation
 * {@linkplain AbstractChannelAugmentation#NAME_PROP name} and
 * {@linkplain AbstractChannelAugmentation#SECTION_TYPE_PROP type}, with the augmentation
 * {@linkplain #createAugmentationContent(Page.Request, Channel.ViewResponse, Element) content} being an
 * {@linkplain #createControlsElement(Page.Request, Channel.ViewResponse, Element) ordered list} (
 * <code>&lt;ol&gt;</code>) element, to which each control will append an
 * {@linkplain #createControlElement(Page.Request, Channel.ViewResponse, Element) item} (<code>&lt;li&gt;</code>)
 * containing it's own {@linkplain #createControlContent(Page.Request, Channel.ViewResponse, Element) content}.
 * </p>
 * 
 * <h3 id="configuration">Configuration</h3>
 * <p>
 * This class provides no {@linkplain ConfigManager configuration properties} in addition to those inherited from the
 * {@link AbstractChannelAugmentation}.
 * </p>
 */
public abstract class AbstractChannelControl extends AbstractChannelAugmentation {

    /**
   * Construct a new <code>AbstractChannelControl</code> instance.
   */
    protected AbstractChannelControl(final Channel channel, final ContentManager.ChannelPluginDefinition<?> definition) throws WWWeeePortal.Exception {
        super(channel, definition);
        return;
    }

    /**
   * Get the ordered list (<code>&lt;ol&gt;</code>) of controls from under the <code>augmentationElement</code>,
   * creating it if necessary.
   * 
   * @see #createAugmentationContent(Page.Request, Channel.ViewResponse, Element)
   */
    protected Element createControlsElement(final Page.Request pageRequest, final Channel.ViewResponse viewResponse, final Element augmentationElement) throws WWWeeePortal.Exception {
        final String channelControlsID = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(getPortal().getPortalID(), "channel", "controls", getChannel().getDefinition().getID()));
        final Element existingChannelControlsElement = DOMUtil.getChildElement(augmentationElement, HTMLUtil.HTML_NS_URI, "ol", null, "id", channelControlsID);
        if (existingChannelControlsElement != null) return existingChannelControlsElement;
        final Element controlsElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "ol", augmentationElement);
        getChannel().setIDAndClassAttrs(controlsElement, Arrays.asList("controls"), null, null);
        return controlsElement;
    }

    /**
   * Get the name of this channel control.
   * 
   * @see #createControlElement(Page.Request, Channel.ViewResponse, Element)
   */
    protected abstract String getControlName(final Page.Request pageRequest) throws ConfigManager.ConfigException;

    /**
   * Create the list item (<code>&lt;li&gt;</code>) to contain this control's
   * {@linkplain #createControlContent(Page.Request, Channel.ViewResponse, Element) content}.
   * 
   * @see #createAugmentationContent(Page.Request, Channel.ViewResponse, Element)
   */
    protected Element createControlElement(final Page.Request pageRequest, final Channel.ViewResponse viewResponse, final Element controlsElement) throws WWWeeePortal.Exception {
        final Element controlElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, "li", controlsElement);
        getChannel().setIDAndClassAttrs(controlElement, Arrays.asList("controls", getControlName(pageRequest)), new String[][] { new String[] { getPortal().getPortalID(), "channel", "controls", "control" } }, null);
        return controlElement;
    }

    /**
   * Render the content for this control into the supplied <code>controlElement</code>.
   * 
   * @see #createAugmentationContent(Page.Request, Channel.ViewResponse, Element)
   */
    protected abstract void createControlContent(final Page.Request pageRequest, final Channel.ViewResponse viewResponse, final Element controlElement) throws WWWeeePortal.Exception;

    @Override
    protected void createAugmentationContent(final Page.Request pageRequest, final Channel.ViewResponse viewResponse, final Element augmentationElement) throws WWWeeePortal.Exception {
        final Element controlsElement = createControlsElement(pageRequest, viewResponse, augmentationElement);
        if (controlsElement == null) return;
        final Element controlElement = createControlElement(pageRequest, viewResponse, controlsElement);
        if (controlElement == null) return;
        createControlContent(pageRequest, viewResponse, controlElement);
        return;
    }
}
