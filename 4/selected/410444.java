package org.wwweeeportal.portal.channelplugins;

import java.util.*;
import org.w3c.dom.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.xml.html.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;

/**
 * <p>
 * An abstract base for creating {@link org.wwweeeportal.portal.Channel.Plugin Plugin} classes which generate header or
 * footer content under the channel {@linkplain org.wwweeeportal.portal.Channel.ViewResponse#getResponseRootElement()
 * root}, separate from the regular channel body
 * {@linkplain org.wwweeeportal.portal.Channel.ViewResponse#getContentContainerElement() content}.
 * </p>
 * 
 * <p>
 * This class implements a {@linkplain Channel#FINAL_VIEW_RESPONSE_HOOK filter} to create
 * {@linkplain #createAugmentationContent(Page.Request, Channel.ViewResponse, Element) content} within an
 * {@linkplain HTMLUtil#HTML_NS_URI HTML}5 {@linkplain #SECTION_TYPE_PROP section type} based
 * {@linkplain #createAugmentationElement(Page.Request, Channel.ViewResponse) augmentation element}, either prepended
 * (default) or {@linkplain #APPEND_ENABLE_PROP appended} as a
 * {@linkplain DOMUtil#getChildElement(Node, URI, String, URI, String, String) child} of the channel
 * {@linkplain org.wwweeeportal.portal.Channel.ViewResponse#getResponseRootElement() root} element. Any existing child
 * Element with the same {@linkplain #NAME_PROP name} and {@linkplain #SECTION_TYPE_PROP type} will be <em>reused</em>
 * as the {@linkplain #createAugmentationElement(Page.Request, Channel.ViewResponse) augmentation element} for this
 * instance, having the resulting {@linkplain #createAugmentationContent(Page.Request, Channel.ViewResponse, Element)
 * content} appended to it.
 * </p>
 * 
 * <h3 id="configuration">Configuration</h3>
 * <p>
 * The following {@linkplain ConfigManager configuration properties} are supported by this class:
 * </p>
 * <ul>
 * <li>{@link #NAME_PROP}</li>
 * <li>{@link #SECTION_TYPE_PROP}</li>
 * <li>{@link #APPEND_ENABLE_PROP}</li>
 * </ul>
 */
public abstract class AbstractChannelAugmentation extends Channel.Plugin {

    /**
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the name of this augmentation.
   * If this property isn't specified, the default value <code>&quot;header&quot;</code> will be used. The value of this
   * property is used primarily to construct the <code>id</code> attribute on the
   * {@linkplain #createAugmentationElement(Page.Request, Channel.ViewResponse) augmentation element}, which is also
   * used to locate an existing augmentation element for reuse by subsequently created augmentations with the same name
   * and {@linkplain #SECTION_TYPE_PROP type}.
   * 
   * @see #getAugmentationName(Page.Request)
   * @see #createAugmentationElement(Page.Request, Channel.ViewResponse)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String NAME_PROP = "Channel.Augmentation.Name";

    /**
   * The key to a {@link RSProperties#RESULT_STRING_CONVERTER String} property defining the
   * {@linkplain Element#getLocalName() local name} for the {@linkplain HTMLUtil#HTML_NS_URI HTML}5
   * {@linkplain #getSectionType(Page.Request) section type} {@link Element} to be used for the
   * {@linkplain #createAugmentationElement(Page.Request, Channel.ViewResponse) augmentation element}.
   * 
   * @see #getSectionType(Page.Request)
   * @see #createAugmentationElement(Page.Request, Channel.ViewResponse)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String SECTION_TYPE_PROP = "Channel.Augmentation.Section.Type";

    /**
   * The key to a {@link RSProperties#RESULT_BOOLEAN_CONVERTER Boolean} property indicating that if an
   * {@linkplain #createAugmentationElement(Page.Request, Channel.ViewResponse) augmentation element} is
   * {@linkplain #createAugmentationElement(Page.Request, Channel.ViewResponse) created}, it should be
   * {@linkplain Element#appendChild(Node) appended} to the channel
   * {@linkplain org.wwweeeportal.portal.Channel.ViewResponse#getResponseRootElement() root} instead of being
   * {@linkplain Element#insertBefore(Node, Node) inserted} preceding it's {@linkplain Element#getFirstChild() first
   * child}. If you enable this property you will also likely want to override the default value of
   * <code>&quot;header&quot;</code> used as the augmentation {@linkplain #NAME_PROP name} and
   * {@linkplain #SECTION_TYPE_PROP type}.
   * 
   * @see #isAppendEnabled(Page.Request)
   * @see #createAugmentationElement(Page.Request, Channel.ViewResponse)
   * @category WWWEEE_PORTAL_CONFIG_PROP
   */
    public static final String APPEND_ENABLE_PROP = "Channel.Augmentation.Append.Enable";

    /**
   * Construct a new <code>AbstractChannelAugmentation</code> instance.
   */
    protected AbstractChannelAugmentation(final Channel channel, final ContentManager.ChannelPluginDefinition<?> definition) throws WWWeeePortal.Exception {
        channel.super(definition);
        return;
    }

    /**
   * @see #NAME_PROP
   */
    protected String getAugmentationName(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        return getConfigProp(NAME_PROP, pageRequest, "header", false);
    }

    /**
   * @see #SECTION_TYPE_PROP
   */
    protected String getSectionType(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        return getConfigProp(SECTION_TYPE_PROP, pageRequest, "header", false);
    }

    /**
   * @see #APPEND_ENABLE_PROP
   */
    protected boolean isAppendEnabled(final Page.Request pageRequest) throws ConfigManager.ConfigException {
        return getConfigProp(APPEND_ENABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    /**
   * Get the {@link Element} from under the channel
   * {@linkplain org.wwweeeportal.portal.Channel.ViewResponse#getResponseRootElement() root} which is to host the
   * {@linkplain #createAugmentationContent(Page.Request, Channel.ViewResponse, Element) content} for this augmentation,
   * creating it if necessary.
   * 
   * @see #getAugmentationName(Page.Request)
   * @see #getSectionType(Page.Request)
   * @see #isAppendEnabled(Page.Request)
   * @see #createAugmentation(Page.Request, Channel.ViewResponse)
   */
    protected Element createAugmentationElement(final Page.Request pageRequest, final Channel.ViewResponse viewResponse) throws WWWeeePortal.Exception {
        final String augmentationName = getAugmentationName(pageRequest);
        final String augmentationID = ConversionUtil.invokeConverter(MarkupManager.MARKUP_ENCODE_CONVERTER, Arrays.asList(getPortal().getPortalID(), "channel", augmentationName, getChannel().getDefinition().getID()));
        final String sectionType = getSectionType(pageRequest);
        final boolean isHTML5Supported = getPortal().isHTML5Supported(pageRequest.getSecurityContext(), pageRequest.getHttpHeaders());
        final String augmentationType = isHTML5Supported ? sectionType : "div";
        final boolean isAppendEnabled = isAppendEnabled(pageRequest);
        final Element responseRootElement = viewResponse.getResponseRootElement();
        final Element existingAugmentationElement = DOMUtil.getChildElement(responseRootElement, HTMLUtil.HTML_NS_URI, augmentationType, null, "id", augmentationID);
        if (existingAugmentationElement != null) return existingAugmentationElement;
        final Element augmentationElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, HTMLUtil.HTML_NS_PREFIX, augmentationType, responseRootElement, DOMUtil.getDocument(responseRootElement), false, false);
        getChannel().setIDAndClassAttrs(augmentationElement, Arrays.asList(augmentationName), isHTML5Supported ? null : new String[][] { new String[] { sectionType } }, null);
        synchronized (DOMUtil.getDocument(responseRootElement)) {
            if (isAppendEnabled) {
                responseRootElement.appendChild(augmentationElement);
            } else {
                responseRootElement.insertBefore(augmentationElement, responseRootElement.getFirstChild());
            }
        }
        return augmentationElement;
    }

    /**
   * Render the content for this augmentation into the supplied <code>augmentationElement</code>.
   * 
   * @see #createAugmentation(Page.Request, Channel.ViewResponse)
   */
    protected abstract void createAugmentationContent(final Page.Request pageRequest, final Channel.ViewResponse viewResponse, final Element augmentationElement) throws WWWeeePortal.Exception;

    /**
   * Create the {@linkplain #createAugmentationElement(Page.Request, Channel.ViewResponse) element} for this
   * augmentation and render it's {@linkplain #createAugmentationContent(Page.Request, Channel.ViewResponse, Element)
   * content}.
   */
    protected void createAugmentation(final Page.Request pageRequest, final Channel.ViewResponse viewResponse) throws WWWeeePortal.Exception {
        final Element augmentationElement = createAugmentationElement(pageRequest, viewResponse);
        if (augmentationElement == null) return;
        createAugmentationContent(pageRequest, viewResponse, augmentationElement);
        return;
    }

    @Override
    protected <T> T pluginFilterHook(final Channel.PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest, final T data) throws WWWeeePortal.Exception {
        if (Channel.FINAL_VIEW_RESPONSE_HOOK.equals(pluginHook)) {
            final Channel.ViewResponse viewResponse = Channel.FINAL_VIEW_RESPONSE_HOOK.getResultClass().cast(data);
            createAugmentation(pageRequest, viewResponse);
        }
        return data;
    }
}
