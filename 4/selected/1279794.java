package org.wwweeeportal.portal.channelplugins;

import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.activation.*;
import org.w3c.dom.*;
import javax.xml.namespace.*;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import javax.ws.rs.core.*;
import org.springframework.core.convert.*;
import org.apache.http.client.methods.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.collection.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.http.*;
import org.wwweeeportal.util.net.*;
import org.wwweeeportal.util.xml.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.xml.html.*;
import org.wwweeeportal.util.xml.sax.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;
import org.wwweeeportal.portal.channels.*;

/**
 * Integrates a proxied XHTML document for a {@link org.wwweeeportal.portal.channels.ProxyChannel} into the portal.
 */
public class ProxyChannelHTMLSource extends Channel.Plugin {

    public static final String BODY_UNWRAPPING_DISABLE_PROP = "ProxyChannel.HTMLSource.BodyUnwrapping.Disable";

    public static final String LINK_REWRITING_DISABLE_PROP = "ProxyChannel.HTMLSource.LinkRewriting.Disable";

    public static final String TITLE_SETTING_DISABLE_PROP = "ProxyChannel.HTMLSource.TitleSetting.Disable";

    /**
   * <p>
   * If not specified, the value for this property defaults to <em><code>1</code></em>, meaning any
   * <code>&lt;h1&gt;</code> will become an <code>&lt;h2&gt;</code>, an <code>&lt;h2&gt;</code> will become an
   * <code>&lt;h3&gt;</code>, and so on. This default was chosen because every page is <em>supposed</em> to start with
   * an <code>&lt;h1&gt;</code> at the root, with each subsection being an <code>&lt;h2&gt;</code>, and so on down the
   * hierarchy. When the portal aggregates several of these pages together, each of them is essentially moving one step
   * down that hierarchy away from the root, with the portal then responsible for providing the root
   * <code>&lt;h1&gt;</code> for that aggregated page (ie, {@link PageHeadingChannel}).
   * </p>
   * 
   * <p>
   * Note that if you are utilizing the {@link ChannelTitle} plugin in it's default configuration to provide an
   * <code>&lt;h2&gt;</code> for each channel, then you may want to consider increasing this value to
   * <em><code>2</code></em>.
   * </p>
   * 
   * <p>
   * Also keep in mind that you are able to set this property globally within the root
   * {@link WWWeeePortal#getProperties() portal properties}, where it will automatically apply to <em>all</em> instances
   * of this plugin by default, but can still be overridden on a per-instance basis.
   * </p>
   */
    public static final String HEADING_ADJUSTMENT_AMOUNT_PROP = "ProxyChannel.HTMLSource.HeadingAdjustment.Amount";

    public static final String CSS_LINK_PROXY_DISABLE_PROP = "ProxyChannel.HTMLSource.CSSLinkProxy.Disable";

    public static final String JAVASCRIPT_LINK_PROXY_DISABLE_PROP = "ProxyChannel.HTMLSource.JavascriptLinkProxy.Disable";

    public static final String KEYWORDS_PROXY_DISABLE_PROP = "ProxyChannel.HTMLSource.KeywordsProxy.Disable";

    protected static final Collection<CaptureHandler.AttrTest> REL_STYLESHEET_TESTS = Collections.singletonList(new CaptureHandler.AttrTest(null, "rel", "stylesheet", false));

    protected static final Collection<CaptureHandler.AttrTest> TYPE_JAVASCRIPT_TESTS = Collections.singletonList(new CaptureHandler.AttrTest(null, "type", "text/javascript", false));

    protected static final Collection<CaptureHandler.AttrTest> NAME_KEYWORDS_TESTS = Collections.singletonList(new CaptureHandler.AttrTest(null, "name", "keywords", false));

    public ProxyChannelHTMLSource(final Channel channel, final ContentManager.ChannelPluginDefinition<?> definition) throws WWWeeePortal.Exception {
        channel.super(definition);
        if (!(channel instanceof ProxyChannel)) {
            throw new ConfigManager.ConfigException(getClass().getSimpleName() + " only works with ProxyChannel", null);
        }
        return;
    }

    protected ProxyChannel getProxyChannel() {
        return (ProxyChannel) getChannel();
    }

    protected boolean isBodyUnwrappingDisabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(BODY_UNWRAPPING_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected boolean isLinkRewritingDisabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(LINK_REWRITING_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected boolean isTitleSettingDisabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(TITLE_SETTING_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected boolean isCSSLinkProxyDisabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(CSS_LINK_PROXY_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected boolean isJavascriptLinkProxyDisabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(JAVASCRIPT_LINK_PROXY_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected boolean isKeywordsProxyDisabled(final Page.Request pageRequest) throws WWWeeePortal.Exception {
        return getConfigProp(KEYWORDS_PROXY_DISABLE_PROP, pageRequest, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
    }

    protected DefaultHandler2 createUnwrapBodyContentHandler(final Page.Request pageRequest, final DefaultHandler2 defaultHandler) throws WWWeeePortal.Exception {
        if (isBodyUnwrappingDisabled(pageRequest)) {
            return defaultHandler;
        }
        final Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("body", "div");
        final ElementReplacementContentHandler bodyDivContentHandler = new ElementReplacementContentHandler(defaultHandler, HTMLUtil.HTML_NS_URI, replacements);
        return new SubDocumentContentHandler(bodyDivContentHandler, HTMLUtil.HTML_NS_URI, "body");
    }

    protected DefaultHandler2 createLinkRewriteContentHandler(final Page.Request pageRequest, final DefaultHandler2 defaultHandler, final URL proxiedFileURL) throws WWWeeePortal.Exception {
        if (isLinkRewritingDisabled(pageRequest)) {
            return defaultHandler;
        }
        return new HTMLLinkRewritingContentHandler(getProxyChannel(), pageRequest, defaultHandler, proxiedFileURL);
    }

    protected DefaultHandler2 createTitleSettingContentHandler(final Page.Request pageRequest, final DefaultHandler2 defaultHandler) throws WWWeeePortal.Exception {
        if ((isTitleSettingDisabled(pageRequest)) || (getProxyChannel().isInlineContentDisabled(pageRequest))) {
            return defaultHandler;
        }
        final CaptureHandler.ElementText titleSettingCaptureHandler = new CaptureHandler.ElementText(defaultHandler, HTMLUtil.HTML_NS_URI, "title", null);
        pageRequest.getAttributes().put(createClientAttributesKey(pageRequest, "TitleSettingCaptureHandler", true, null), titleSettingCaptureHandler);
        return titleSettingCaptureHandler;
    }

    protected void setTitleFromContentHandler(final Page.Request pageRequest, final Channel.ViewResponse viewResponse) throws WWWeeePortal.Exception {
        if ((isTitleSettingDisabled(pageRequest)) || (getProxyChannel().isInlineContentDisabled(pageRequest))) {
            return;
        }
        final String titleSettingCaptureHandlerRequestKey = createClientAttributesKey(pageRequest, "TitleSettingCaptureHandler", true, null);
        final CaptureHandler.ElementText titleSettingCaptureHandler = (CaptureHandler.ElementText) pageRequest.getAttributes().get(titleSettingCaptureHandlerRequestKey);
        if (titleSettingCaptureHandler == null) {
            return;
        }
        pageRequest.getAttributes().remove(titleSettingCaptureHandlerRequestKey);
        final String title = CollectionUtil.first(titleSettingCaptureHandler.getCapturedValues(), null);
        if (title != null) {
            viewResponse.setTitle(title);
        }
        return;
    }

    protected DefaultHandler2 createHeadingAdjustContentHandler(final Page.Request pageRequest, final DefaultHandler2 defaultHandler) throws WWWeeePortal.Exception {
        final Integer headingAdjustmentAmount = getConfigProp(HEADING_ADJUSTMENT_AMOUNT_PROP, pageRequest, RSProperties.RESULT_INTEGER_CONVERTER, Integer.valueOf(1), false, true);
        if ((headingAdjustmentAmount == null) || (headingAdjustmentAmount.intValue() == 0)) {
            return defaultHandler;
        }
        final Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("h1", "h" + String.valueOf(Math.min(6, 1 + headingAdjustmentAmount.intValue())));
        replacements.put("h2", "h" + String.valueOf(Math.min(6, 2 + headingAdjustmentAmount.intValue())));
        replacements.put("h3", "h" + String.valueOf(Math.min(6, 3 + headingAdjustmentAmount.intValue())));
        replacements.put("h4", "h" + String.valueOf(Math.min(6, 4 + headingAdjustmentAmount.intValue())));
        replacements.put("h5", "h" + String.valueOf(Math.min(6, 5 + headingAdjustmentAmount.intValue())));
        return new ElementReplacementContentHandler(defaultHandler, HTMLUtil.HTML_NS_URI, replacements);
    }

    protected DefaultHandler2 createCSSLinkProxyContentHandler(final Page.Request pageRequest, final DefaultHandler2 defaultHandler) throws WWWeeePortal.Exception {
        if ((isCSSLinkProxyDisabled(pageRequest)) || (getProxyChannel().isInlineContentDisabled(pageRequest))) {
            return defaultHandler;
        }
        final CaptureHandler.AttributeValue cssLinkProxyCaptureHandler = new CaptureHandler.AttributeValue(defaultHandler, HTMLUtil.HTML_NS_URI, "link", REL_STYLESHEET_TESTS, null, "href");
        pageRequest.getAttributes().put(createClientAttributesKey(pageRequest, "CSSLinkProxyCaptureHandler", true, null), cssLinkProxyCaptureHandler);
        return cssLinkProxyCaptureHandler;
    }

    protected void proxyCSSLinksFromContentHandler(final Page.Request pageRequest, final Channel.ViewResponse viewResponse) throws WWWeeePortal.Exception {
        if ((isCSSLinkProxyDisabled(pageRequest)) || (getProxyChannel().isInlineContentDisabled(pageRequest))) {
            return;
        }
        final String cssLinkProxyCaptureHandlerRequestKey = createClientAttributesKey(pageRequest, "CSSLinkProxyCaptureHandler", true, null);
        final CaptureHandler.AttributeValue cssLinkProxyCaptureHandler = (CaptureHandler.AttributeValue) pageRequest.getAttributes().get(cssLinkProxyCaptureHandlerRequestKey);
        if (cssLinkProxyCaptureHandler == null) {
            return;
        }
        pageRequest.getAttributes().remove(cssLinkProxyCaptureHandlerRequestKey);
        final String portalName = getPortal().getName(pageRequest.getSecurityContext(), pageRequest.getHttpHeaders());
        final List<String> cssLinks = cssLinkProxyCaptureHandler.getCapturedValues();
        if (cssLinks != null) {
            final Document cssLinksDocument = DOMUtil.newDocument();
            for (String cssLinkString : cssLinks) {
                cssLinkString = StringUtil.mkNull(cssLinkString, true);
                if (cssLinkString == null) {
                    continue;
                }
                final Element cssLinkElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, null, "link", null, cssLinksDocument, false, false);
                cssLinkElement.setAttributeNS(null, "href", cssLinkString);
                cssLinkElement.setAttributeNS(null, "rel", "stylesheet");
                cssLinkElement.setAttributeNS(null, "type", "text/css");
                cssLinkElement.setAttributeNS(null, "title", portalName);
                viewResponse.addMetaElement(cssLinkElement);
            }
        }
        return;
    }

    protected DefaultHandler2 createJavascriptLinkProxyContentHandler(final Page.Request pageRequest, final DefaultHandler2 defaultHandler) throws WWWeeePortal.Exception {
        if ((isJavascriptLinkProxyDisabled(pageRequest)) || (getProxyChannel().isInlineContentDisabled(pageRequest))) {
            return defaultHandler;
        }
        final CaptureHandler.AttributeValue javascriptLinkProxyCaptureHandler = new CaptureHandler.AttributeValue(defaultHandler, HTMLUtil.HTML_NS_URI, "script", TYPE_JAVASCRIPT_TESTS, null, "src");
        pageRequest.getAttributes().put(createClientAttributesKey(pageRequest, "JavascriptLinkProxyCaptureHandler", true, null), javascriptLinkProxyCaptureHandler);
        return javascriptLinkProxyCaptureHandler;
    }

    protected void proxyJavascriptLinksFromContentHandler(final Page.Request pageRequest, final Channel.ViewResponse viewResponse) throws WWWeeePortal.Exception {
        if ((isJavascriptLinkProxyDisabled(pageRequest)) || (getProxyChannel().isInlineContentDisabled(pageRequest))) {
            return;
        }
        final String javascriptLinkProxyCaptureHandlerRequestKey = createClientAttributesKey(pageRequest, "JavascriptLinkProxyCaptureHandler", true, null);
        final CaptureHandler.AttributeValue javascriptLinkProxyCaptureHandler = (CaptureHandler.AttributeValue) pageRequest.getAttributes().get(javascriptLinkProxyCaptureHandlerRequestKey);
        if (javascriptLinkProxyCaptureHandler == null) {
            return;
        }
        pageRequest.getAttributes().remove(javascriptLinkProxyCaptureHandlerRequestKey);
        final List<String> javascriptLinks = javascriptLinkProxyCaptureHandler.getCapturedValues();
        if (javascriptLinks != null) {
            final Document javascriptLinksDocument = DOMUtil.newDocument();
            for (String javascriptLinkString : javascriptLinks) {
                javascriptLinkString = StringUtil.mkNull(javascriptLinkString, true);
                if (javascriptLinkString == null) {
                    continue;
                }
                final Element javascriptLinkElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, null, "script", null, javascriptLinksDocument, false, false);
                javascriptLinkElement.setAttributeNS(null, "src", javascriptLinkString);
                javascriptLinkElement.setAttributeNS(null, "type", "text/javascript");
                viewResponse.addMetaElement(javascriptLinkElement);
            }
        }
        return;
    }

    protected DefaultHandler2 createKeywordsProxyContentHandler(final Page.Request pageRequest, final DefaultHandler2 defaultHandler) throws WWWeeePortal.Exception {
        if ((isKeywordsProxyDisabled(pageRequest)) || (getProxyChannel().isInlineContentDisabled(pageRequest))) {
            return defaultHandler;
        }
        final CaptureHandler.AttributeValue keywordsProxyCaptureHandler = new CaptureHandler.AttributeValue(defaultHandler, HTMLUtil.HTML_NS_URI, "meta", NAME_KEYWORDS_TESTS, null, "content");
        pageRequest.getAttributes().put(createClientAttributesKey(pageRequest, "KeywordsProxyCaptureHandler", true, null), keywordsProxyCaptureHandler);
        return keywordsProxyCaptureHandler;
    }

    protected void proxyKeywordsFromContentHandler(final Page.Request pageRequest, final Channel.ViewResponse viewResponse) throws WWWeeePortal.Exception {
        if ((isKeywordsProxyDisabled(pageRequest)) || (getProxyChannel().isInlineContentDisabled(pageRequest))) {
            return;
        }
        final String keywordsProxyCaptureHandlerRequestKey = createClientAttributesKey(pageRequest, "KeywordsProxyCaptureHandler", true, null);
        final CaptureHandler.AttributeValue keywordsProxyCaptureHandler = (CaptureHandler.AttributeValue) pageRequest.getAttributes().get(keywordsProxyCaptureHandlerRequestKey);
        if (keywordsProxyCaptureHandler == null) {
            return;
        }
        pageRequest.getAttributes().remove(keywordsProxyCaptureHandlerRequestKey);
        final List<String> keywords = keywordsProxyCaptureHandler.getCapturedValues();
        if (keywords != null) {
            final Document keywordsDocument = DOMUtil.newDocument();
            for (String keyword : keywords) {
                keyword = StringUtil.mkNull(keyword, true);
                if (keyword == null) {
                    continue;
                }
                final Element metaElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, null, "meta", null, keywordsDocument, false, false);
                metaElement.setAttributeNS(null, "name", "keywords");
                metaElement.setAttributeNS(null, "content", keyword);
                viewResponse.addMetaElement(metaElement);
            }
        }
        return;
    }

    protected void parseHTML(final Channel.ViewResponse viewResponse, final TypedInputSource proxiedDocumentInputSource, final DefaultHandler2 contentHandler) throws WWWeeePortal.Exception {
        MarkupManager.parseHTMLDocument(proxiedDocumentInputSource.getInputSource(), contentHandler, false);
        viewResponse.setContentType(MediaType.APPLICATION_XHTML_XML_TYPE);
        return;
    }

    @Override
    protected <T> T pluginValueHook(final Channel.PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest) throws WWWeeePortal.Exception {
        if (ProxyChannel.PARSE_XML_HOOK.equals(pluginHook)) {
            final TypedInputSource proxiedDocumentInputSource = (TypedInputSource) context[1];
            if (!XMLUtil.isXML(proxiedDocumentInputSource.getContentType()) && (HTMLUtil.isHTML(proxiedDocumentInputSource.getContentType()))) {
                final Channel.ViewResponse viewResponse = (Channel.ViewResponse) context[0];
                final DefaultHandler2 contentHandler = (DefaultHandler2) context[2];
                parseHTML(viewResponse, proxiedDocumentInputSource, contentHandler);
                return pluginHook.getResultClass().cast(Boolean.TRUE);
            }
        }
        return null;
    }

    @Override
    protected <T> T pluginFilterHook(final Channel.PluginHook<?, T> pluginHook, final Object[] context, final Page.Request pageRequest, final T data) throws WWWeeePortal.Exception {
        if (ProxyChannel.PROXY_REQUEST_HOOK.equals(pluginHook)) {
            final String mode = (String) context[0];
            if (Channel.VIEW_MODE.equals(mode)) {
                final HttpUriRequest proxyRequest = ProxyChannel.PROXY_REQUEST_HOOK.getResultClass().cast(data);
                final String accept = (proxyRequest.containsHeader("Accept") ? HTTPUtil.consolidateHeaders(proxyRequest.getHeaders("Accept")).getValue() + ',' : "") + HTMLUtil.TEXT_HTML_MIME_TYPE + ";q=0.8";
                proxyRequest.removeHeaders("Accept");
                proxyRequest.setHeader("Accept", accept);
            }
        } else if (ProxyChannel.IS_RENDERED_USING_XML_VIEW_HOOK.equals(pluginHook)) {
            final Boolean useXMLView = ProxyChannel.IS_RENDERED_USING_XML_VIEW_HOOK.getResultClass().cast(data);
            final MimeType contentMimeType = (MimeType) context[1];
            if ((!Boolean.TRUE.equals(useXMLView)) && HTMLUtil.isHTML(contentMimeType)) {
                return pluginHook.getResultClass().cast(Boolean.TRUE);
            }
        } else if (ProxyChannel.PROXIED_DOC_CONTENT_HANDLER_HOOK.equals(pluginHook)) {
            final URL proxiedFileURL = (URL) context[1];
            final TypedInputSource proxiedDocumentInputSource = (TypedInputSource) context[2];
            if (HTMLUtil.isHTML(proxiedDocumentInputSource.getContentType())) {
                DefaultHandler2 handler = ProxyChannel.PROXIED_DOC_CONTENT_HANDLER_HOOK.getResultClass().cast(data);
                handler = createUnwrapBodyContentHandler(pageRequest, handler);
                handler = createCSSLinkProxyContentHandler(pageRequest, handler);
                handler = createJavascriptLinkProxyContentHandler(pageRequest, handler);
                handler = createKeywordsProxyContentHandler(pageRequest, handler);
                handler = createTitleSettingContentHandler(pageRequest, handler);
                handler = createHeadingAdjustContentHandler(pageRequest, handler);
                handler = createLinkRewriteContentHandler(pageRequest, handler, proxiedFileURL);
                return pluginHook.getResultClass().cast(handler);
            }
        } else if (Channel.VIEW_RESPONSE_HOOK.equals(pluginHook)) {
            final Channel.ViewResponse viewResponse = Channel.VIEW_RESPONSE_HOOK.getResultClass().cast(data);
            if (HTMLUtil.isHTML(ConversionUtil.invokeConverter(RESTUtil.MEDIA_TYPE_MIME_TYPE_CONVERTER, viewResponse.getContentType()))) {
                setTitleFromContentHandler(pageRequest, viewResponse);
                proxyCSSLinksFromContentHandler(pageRequest, viewResponse);
                proxyJavascriptLinksFromContentHandler(pageRequest, viewResponse);
                proxyKeywordsFromContentHandler(pageRequest, viewResponse);
            }
        }
        return data;
    }

    public static class SubDocumentContentHandler extends ChainedHandler {

        protected final URI subDocNSURI;

        protected final String subDocLocalName;

        protected final Stack<Locale> ignoredElementLocales = new Stack<Locale>();

        public SubDocumentContentHandler(final DefaultHandler2 defaultHandler, final URI subDocNSURI, final String subDocLocalName) {
            super(defaultHandler);
            this.subDocNSURI = subDocNSURI;
            this.subDocLocalName = subDocLocalName;
            ignore = true;
            return;
        }

        protected static final Locale getLastLocale(final Stack<Locale> locales) {
            for (int i = locales.size() - 1; i >= 0; i--) {
                if (locales.get(i) != null) return locales.get(i);
            }
            return null;
        }

        @Override
        public void startElement(final String namespaceURI, final String localName, final String qName, Attributes attrs) throws SAXException {
            if ((ignore) && (subDocNSURI.toString().equals(namespaceURI)) && (subDocLocalName.equals(localName))) {
                ignore = false;
                final Locale elementLocale = SAXUtil.getXMLLangAttr(attrs, null);
                if (elementLocale == null) {
                    final Locale lastIgnoredLocale = getLastLocale(ignoredElementLocales);
                    if (lastIgnoredLocale != null) {
                        try {
                            attrs = SAXUtil.setXMLLangAttr(attrs, lastIgnoredLocale);
                        } catch (ConversionException ce) {
                            throw new SAXException(ce);
                        }
                    }
                }
            }
            if (ignore) ignoredElementLocales.push(SAXUtil.getXMLLangAttr(attrs, null));
            super.startElement(namespaceURI, localName, qName, attrs);
            return;
        }

        @Override
        public void endElement(final String namespaceURI, final String localName, final String qName) throws SAXException {
            if (ignore) ignoredElementLocales.pop();
            super.endElement(namespaceURI, localName, qName);
            if ((!ignore) && (subDocNSURI.toString().equals(namespaceURI)) && (subDocLocalName.equals(localName))) {
                ignore = true;
            }
            return;
        }
    }

    /**
   * Rewrite links from the document to either point through the {@link org.wwweeeportal.portal.channels.ProxyChannel}
   * or directly back to the origin server.
   */
    public abstract static class LinkRewritingContentHandler extends ChainedHandler {

        protected final ProxyChannel proxyChannel;

        protected final Page.Request pageRequest;

        protected final URL proxiedDocumentURL;

        public LinkRewritingContentHandler(final ProxyChannel proxyChannel, final Page.Request pageRequest, final DefaultHandler2 defaultHandler, final URL proxiedDocumentURL) throws WWWeeePortal.Exception {
            super(defaultHandler);
            this.proxyChannel = proxyChannel;
            this.pageRequest = pageRequest;
            this.proxiedDocumentURL = proxiedDocumentURL;
            return;
        }

        protected String rewriteLinkValue(final String linkValue, final boolean hyperLink) throws Exception {
            final URI originalLinkURI = ConversionUtil.invokeConverter(NetUtil.STRING_URI_CONVERTER, StringUtil.mkNull(linkValue, true));
            final URI rewrittenLinkURI = proxyChannel.rewriteProxiedFileLink(pageRequest, proxiedDocumentURL, originalLinkURI, hyperLink, false);
            return (originalLinkURI != rewrittenLinkURI) ? rewrittenLinkURI.toString() : linkValue;
        }

        /**
     * Is the specified attribute a link?
     * 
     * @return The {@link LinkSpec} if the attribute is a link, else <code>null</code>.
     */
        protected abstract LinkSpec isLinkAttribute(final URI namespaceURI, final String localName, final String qName, final Attributes attributes, final int attrIndex) throws Exception;

        @Override
        public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes attributes) throws SAXException {
            Attributes newAttributes = attributes;
            try {
                if (attributes != null) {
                    final URI nsURI = ConversionUtil.invokeConverter(NetUtil.STRING_URI_CONVERTER, namespaceURI);
                    for (int i = 0; i < attributes.getLength(); i++) {
                        final String attrValue = attributes.getValue(i);
                        if (attrValue == null) continue;
                        final LinkSpec linkSpec = isLinkAttribute(nsURI, localName, qName, attributes, i);
                        if (linkSpec == null) continue;
                        final String rewrittenAttrValue;
                        if (linkSpec.getMultivaluedSeparator() != null) {
                            final String[] links = attrValue.split(Pattern.quote(linkSpec.getMultivaluedSeparator()));
                            final StringBuffer rewrittenAttrValueBuffer = new StringBuffer();
                            boolean rewroteALink = false;
                            for (int s = 0; s < links.length; s++) {
                                final String rewrittenLink = rewriteLinkValue(links[s], linkSpec.isHyperLink());
                                if (rewrittenLink != links[s]) rewroteALink = true;
                                if (s > 0) rewrittenAttrValueBuffer.append(linkSpec.getMultivaluedSeparator());
                                rewrittenAttrValueBuffer.append(rewrittenLink);
                            }
                            rewrittenAttrValue = rewroteALink ? rewrittenAttrValueBuffer.toString() : attrValue;
                        } else {
                            rewrittenAttrValue = rewriteLinkValue(attrValue, linkSpec.isHyperLink());
                        }
                        if (rewrittenAttrValue == attrValue) continue;
                        final String attrNSURIString = attributes.getURI(i);
                        final QName attrQName = XMLUtil.parseQName(attrNSURIString, attributes.getQName(i));
                        newAttributes = SAXUtil.setAttribute(newAttributes, attrNSURIString, attrQName.getPrefix(), attributes.getLocalName(i), rewrittenAttrValue);
                    }
                }
            } catch (Exception e) {
                final SAXException saxe = new SAXException(e.getClass().getName() + ": " + e.getMessage(), e);
                if (saxe.getCause() == null) saxe.initCause(e);
                throw saxe;
            }
            super.startElement(namespaceURI, localName, qName, newAttributes);
            return;
        }

        public static LinkSpec isXLinkAttribute(final URI namespaceURI, final String localName, final String qName, final Attributes attributes, final int attrIndex) throws Exception {
            if (!XMLUtil.XLINK_NS_URI.equals(attributes.getURI(attrIndex))) return null;
            final String attrLocalName = attributes.getLocalName(attrIndex);
            if (("href".equals(attrLocalName)) && ("simple".equals(attributes.getValue(XMLUtil.XLINK_NS_URI.toString(), "type")))) {
                return new LinkSpec(true, null);
            }
            return null;
        }

        protected static class LinkSpec {

            protected final boolean hyperLink;

            protected final String multivaluedSeparator;

            public LinkSpec(final boolean hyperLink, final String multivaluedSeparator) {
                this.hyperLink = hyperLink;
                this.multivaluedSeparator = multivaluedSeparator;
                return;
            }

            public boolean isHyperLink() {
                return hyperLink;
            }

            public String getMultivaluedSeparator() {
                return multivaluedSeparator;
            }
        }
    }

    /**
   * Rewrite HTML links.
   */
    public static class HTMLLinkRewritingContentHandler extends LinkRewritingContentHandler {

        public HTMLLinkRewritingContentHandler(final ProxyChannel proxyChannel, final Page.Request pageRequest, final DefaultHandler2 defaultHandler, final URL proxiedDocumentURL) throws WWWeeePortal.Exception {
            super(proxyChannel, pageRequest, defaultHandler, proxiedDocumentURL);
            return;
        }

        protected static boolean isHTMLHyperLinkRel(final String relAttr, final boolean isLinkElement) throws WWWeeePortal.Exception {
            if (relAttr == null) return false;
            final List<String> linkTypes = Arrays.asList(relAttr.toLowerCase().split(" "));
            if ((linkTypes.contains("alternate")) && ((!isLinkElement) || (!linkTypes.contains("stylesheet")))) return true;
            if (linkTypes.contains("archives")) return true;
            if (linkTypes.contains("author")) return true;
            if ((!isLinkElement) && (linkTypes.contains("bookmark"))) return true;
            if ((!isLinkElement) && (linkTypes.contains("external"))) return true;
            if (linkTypes.contains("first")) return true;
            if (linkTypes.contains("help")) return true;
            if (linkTypes.contains("index")) return true;
            if (linkTypes.contains("last")) return true;
            if (linkTypes.contains("license")) return true;
            if (linkTypes.contains("next")) return true;
            if (linkTypes.contains("prev")) return true;
            if (linkTypes.contains("search")) return true;
            if (linkTypes.contains("sidebar")) return true;
            if (linkTypes.contains("tag")) return true;
            if (linkTypes.contains("up")) return true;
            if (linkTypes.contains("start")) return true;
            if (linkTypes.contains("contents")) return true;
            if (linkTypes.contains("glossary")) return true;
            if (linkTypes.contains("copyright")) return true;
            if (linkTypes.contains("chapter")) return true;
            if (linkTypes.contains("section")) return true;
            if (linkTypes.contains("subsection")) return true;
            if (linkTypes.contains("appendix")) return true;
            if (linkTypes.contains("top")) return true;
            return false;
        }

        public static LinkSpec isHTMLLinkAttribute(final URI namespaceURI, final String localName, final String qName, final Attributes attributes, final int attrIndex) throws Exception {
            if ((!HTMLUtil.HTML_NS_URI.equals(namespaceURI)) || (!"".equals(attributes.getURI(attrIndex)))) return null;
            final String attrLocalName = attributes.getLocalName(attrIndex);
            if ("a".equals(localName)) {
                if ("href".equals(attrLocalName)) return new LinkSpec(true, null);
                if ("ping".equals(attrLocalName)) return new LinkSpec(false, " ");
            } else if ("img".equals(localName)) {
                if ("src".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("usemap".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("longdesc".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("script".equals(localName)) {
                if ("src".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("link".equals(localName)) {
                if ("href".equals(attrLocalName)) return new LinkSpec(isHTMLHyperLinkRel(attributes.getValue("", "rel"), true), null);
            } else if ("form".equals(localName)) {
                if ("action".equals(attrLocalName)) return new LinkSpec(true, null);
            } else if ("input".equals(localName)) {
                if ("src".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("usemap".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("area".equals(localName)) {
                if ("href".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("ping".equals(attrLocalName)) return new LinkSpec(false, " ");
            } else if ("video".equals(localName)) {
                if ("src".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("poster".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("audio".equals(localName)) {
                if ("src".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("source".equals(localName)) {
                if ("src".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("track".equals(localName)) {
                if ("src".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("object".equals(localName)) {
                if ("data".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("usemap".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("codebase".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("param".equals(localName)) {
                if (("value".equals(attrLocalName)) && ("ref".equalsIgnoreCase(attributes.getValue("", "valuetype")))) return new LinkSpec(false, null);
            } else if ("embed".equals(localName)) {
                if ("src".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("pluginurl".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("pluginspage".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("href".equals(attrLocalName)) return new LinkSpec(true, null);
            } else if ("html".equals(localName)) {
                if ("manifest".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("body".equals(localName)) {
                if ("background".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("applet".equals(localName)) {
                if ("codebase".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("archive".equals(attrLocalName)) return new LinkSpec(false, ",");
            } else if ("q".equals(localName)) {
                if ("cite".equals(attrLocalName)) return new LinkSpec(true, null);
            } else if ("blockquote".equals(localName)) {
                if ("cite".equals(attrLocalName)) return new LinkSpec(true, null);
            } else if ("ins".equals(localName)) {
                if ("cite".equals(attrLocalName)) return new LinkSpec(true, null);
            } else if ("del".equals(localName)) {
                if ("cite".equals(attrLocalName)) return new LinkSpec(true, null);
            } else if ("iframe".equals(localName)) {
                if ("src".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("frame".equals(localName)) {
                if ("src".equals(attrLocalName)) return new LinkSpec(false, null);
                if ("longdesc".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("head".equals(localName)) {
                if ("profile".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("base".equals(localName)) {
                if ("href".equals(attrLocalName)) return new LinkSpec(false, null);
            } else if ("map".equals(localName)) {
                if ("href".equals(attrLocalName)) return new LinkSpec(true, null);
            } else if ("command".equals(localName)) {
                if ("icon".equals(attrLocalName)) return new LinkSpec(false, null);
            }
            return null;
        }

        @Override
        protected LinkSpec isLinkAttribute(final URI namespaceURI, final String localName, final String qName, final Attributes attributes, final int attrIndex) throws Exception {
            final LinkSpec htmlLinkSpec = isHTMLLinkAttribute(namespaceURI, localName, qName, attributes, attrIndex);
            if (htmlLinkSpec != null) return htmlLinkSpec;
            final LinkSpec xLinkSpec = isXLinkAttribute(namespaceURI, localName, qName, attributes, attrIndex);
            if (xLinkSpec != null) return xLinkSpec;
            return null;
        }
    }

    public static class ElementReplacementContentHandler extends ChainedHandler {

        protected final URI nsURI;

        protected final Map<String, String> replacements;

        public ElementReplacementContentHandler(final DefaultHandler2 defaultHandler, final URI nsURI, final Map<String, String> replacements) {
            super(defaultHandler);
            this.nsURI = nsURI;
            this.replacements = replacements;
            return;
        }

        @Override
        public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes attributes) throws SAXException {
            if (!nsURI.toString().equals(namespaceURI)) {
                super.startElement(namespaceURI, localName, qName, attributes);
                return;
            }
            final String replacementLocalName = replacements.get(localName);
            if (replacementLocalName == null) {
                super.startElement(namespaceURI, localName, qName, attributes);
                return;
            }
            final QName originalQName = XMLUtil.parseQName(namespaceURI, qName);
            final QName replacementQName = new QName(originalQName.getNamespaceURI(), replacementLocalName, originalQName.getPrefix());
            super.startElement(namespaceURI, replacementLocalName, ConversionUtil.invokeConverter(XMLUtil.QNAME_STRING_CONVERTER, replacementQName), attributes);
            return;
        }
    }
}
