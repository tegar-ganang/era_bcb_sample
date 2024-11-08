package org.wwweeeportal.portal.channelplugins;

import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;
import javax.activation.*;
import javax.xml.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import org.w3c.dom.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.xml.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.xml.transform.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;

/**
 * Applies XSLT Transformations to the output of a {@link org.wwweeeportal.portal.Channel}.
 */
public class ChannelTransformer extends Channel.Plugin {

    public static final String VIEW_TRANSFORM_BY_NUM_PROP = "Channel.Transformer.View.Transform.Num.";

    protected static final Pattern VIEW_TRANSFORM_BY_NUM_PATTERN = Pattern.compile("^" + Pattern.quote(VIEW_TRANSFORM_BY_NUM_PROP) + ".*");

    public static final String VIEW_TRANSFORM_BY_PATH_PROP = "Channel.Transformer.View.Transform.Path.";

    protected static final Pattern VIEW_TRANSFORM_BY_PATH_PATTERN = Pattern.compile("^" + Pattern.quote(VIEW_TRANSFORM_BY_PATH_PROP) + ".*");

    public ChannelTransformer(final Channel channel, final ContentManager.ChannelPluginDefinition<?> definition) throws WWWeeePortal.Exception {
        channel.super(definition);
        return;
    }

    protected List<Map.Entry<URL, Templates>> getTransformations(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        final ArrayList<Map.Entry<URL, Templates>> transformations = new ArrayList<Map.Entry<URL, Templates>>();
        CollectionUtil.addAll(transformations, ConfigManager.getConfigProps(definition.getProperties(), VIEW_TRANSFORM_BY_NUM_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), getPortal().getMarkupManager().getTransformByNumPropConverter(), true, true).values());
        CollectionUtil.addAll(transformations, CollectionUtil.values(ConfigManager.RegexPropKeyConverter.getMatchingValues(ConfigManager.getConfigProps(definition.getProperties(), VIEW_TRANSFORM_BY_PATH_PATTERN, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders(), getPortal().getMarkupManager().getTransformByPathPropConverter(), true, true), null, false, StringUtil.toString(pageRequest.getChannelLocalPath(getChannel()), null))));
        return (!transformations.isEmpty()) ? transformations : null;
    }

    protected void transformContent(final Page.Request pageRequest, final Channel.ViewResponse viewResponse) throws WWWeeePortal.Exception {
        final List<Map.Entry<URL, Templates>> transformations = getTransformations(pageRequest);
        if (transformations == null) return;
        final Element originalContentContainerElement = viewResponse.getContentContainerElement();
        final Element originalDocumentElement = DOMUtil.getChildElement(originalContentContainerElement, null, null);
        final TransformableDocument transformableDocument = new TransformableDocument(this);
        if (originalDocumentElement != null) {
            final Element transformSourceDocumentElement;
            synchronized (DOMUtil.getDocument(originalContentContainerElement)) {
                transformSourceDocumentElement = (Element) transformableDocument.getDocument().adoptNode(originalDocumentElement);
                if (originalDocumentElement.getNamespaceURI() != null) DOMUtil.createAttribute(XMLUtil.XMLNS_ATTRIBUTE_NS_URI, null, XMLConstants.XMLNS_ATTRIBUTE, originalDocumentElement.getNamespaceURI(), transformSourceDocumentElement);
            }
            transformableDocument.getDocument().appendChild(transformSourceDocumentElement);
        }
        final Document transformResultContentDocument = DOMUtil.newDocument();
        final DOMResult transformContentResult = new DOMResult(transformResultContentDocument);
        final Map<String, Object> transformProps = new HashMap<String, Object>();
        pageRequest.getMetaProps(transformProps);
        getPortal().getMetaProps(transformProps, pageRequest.getSecurityContext(), pageRequest.getHttpHeaders());
        pageRequest.getPage().getMetaProps(pageRequest, transformProps);
        getChannel().getMetaProps(pageRequest, transformProps);
        getMetaProps(pageRequest, transformProps);
        transformableDocument.setMediaType(ConversionUtil.invokeConverter(RESTUtil.MEDIA_TYPE_MIME_TYPE_CONVERTER, viewResponse.getContentType()));
        transformableDocument.setTransformations(transformations);
        transformableDocument.setTransformationParameters(transformProps);
        try {
            transformableDocument.transform(transformContentResult, viewResponse);
        } catch (Exception e) {
            throw new ConfigManager.ConfigException(e);
        }
        DOMUtil.appendChild(originalContentContainerElement, transformResultContentDocument);
        try {
            viewResponse.setContentType(ConversionUtil.invokeConverter(RESTUtil.MIME_TYPE_MEDIA_TYPE_CONVERTER, transformableDocument.getOutputMediaType()));
        } catch (MimeTypeParseException mtpe) {
            throw new ConfigManager.ConfigException("Transformation has invalid media type", mtpe);
        } catch (UnsupportedCharsetException uce) {
            throw new ConfigManager.ConfigException("Transformation has invalid encoding", uce);
        }
        return;
    }

    @Override
    protected <T> T pluginFilterHook(final Channel.PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest, final T data) throws WWWeeePortal.Exception {
        if (Channel.VIEW_RESPONSE_HOOK.equals(pluginHook)) {
            final Channel.ViewResponse viewResponse = Channel.VIEW_RESPONSE_HOOK.getResultClass().cast(data);
            transformContent(pageRequest, viewResponse);
        }
        return data;
    }
}
