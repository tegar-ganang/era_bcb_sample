package org.swemas.rendering.composing.xhtml;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.*;
import javax.xml.xpath.XPathExpressionException;
import org.swemas.core.ModuleNotFoundException;
import org.swemas.core.dispatcher.IHttpProcessingChannel;
import org.swemas.core.event.IEventDispatchingChannel;
import org.swemas.core.kernel.IKernel;
import org.swemas.core.messaging.ILocaleProvidingChannel;
import org.swemas.data.xml.*;
import org.swemas.rendering.ErrorCode;
import org.swemas.rendering.RenderingEvent;
import org.swemas.rendering.RenderingException;
import org.w3c.dom.*;

/**
 * @author Alexey Chernov
 * 
 */
public class SwXhtmlComposer extends org.swemas.rendering.composing.SwXmlComposingModule implements IXhtmlComposingChannel, IHttpProcessingChannel {

    public SwXhtmlComposer(IKernel kernel) throws InvocationTargetException {
        super(kernel);
        ns().setNamespace("tp", "swemas/topology");
        ns().setNamespace("xhtml", "http://www.w3.org/1999/xhtml");
    }

    @Override
    public List<Node> render(Node node, HttpServletRequest request) throws RenderingException {
        try {
            String sc = "";
            sc = "xr_template_div_injective.xml";
            Document tpg = ixml().open(kernel().getPath("/site/pages/" + sc), kernel().getPath("/schema/topology.xsd"));
            Node root = ixml().evaluate(tpg, "/tp:element", ns()).item(0);
            if (root != null) {
                Map<String, Map<String, List<Node>>> flow = parseElement(root, request);
                Document xhtml = ixml().create();
                DOMConfiguration config = xhtml.getDomConfig();
                config.setParameter("namespaces", Boolean.TRUE);
                config.setParameter("namespace-declarations", Boolean.TRUE);
                Node html = xhtml.createElementNS(ns().getNamespaceURI("xhtml"), "html");
                Node head = xhtml.createElementNS(ns().getNamespaceURI("xhtml"), "head");
                Node body = xhtml.createElementNS(ns().getNamespaceURI("xhtml"), "body");
                appendChild(xhtml, html);
                appendChild(html, head);
                appendChild(html, body);
                Map<String, List<Node>> doctypestream;
                if (flow.containsKey("doctype")) doctypestream = flow.get("doctype"); else doctypestream = new HashMap<String, List<Node>>();
                Map<String, List<Node>> headstream;
                if (flow.containsKey("head")) headstream = flow.get("head"); else headstream = new HashMap<String, List<Node>>();
                Map<String, List<Node>> bodystream;
                if (flow.containsKey("body")) bodystream = flow.get("body"); else bodystream = new HashMap<String, List<Node>>();
                for (List<Node> list : doctypestream.values()) for (Node n : list) appendChild(xhtml, n);
                boolean title = false;
                for (List<Node> list : headstream.values()) for (Node n : list) if (n.getLocalName() != "title" || n.getNamespaceURI() != ns().getNamespaceURI("xhtml")) {
                    if (!ixml().contains(head.getChildNodes(), n)) appendChild(head, n);
                } else if (!title) {
                    if (!ixml().contains(head.getChildNodes(), n)) appendChild(head, n);
                    title = true;
                }
                for (List<Node> list : bodystream.values()) for (Node n : list) appendChild(body, n);
                try {
                    stripeAnchors(xhtml);
                    ixml().validate(xhtml, kernel().getPath("/schema/xhtml1-strict.xsd"));
                    xhtml.normalizeDocument();
                } catch (XmlException e) {
                    try {
                        IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                        ed.event(new RenderingEvent(new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.ResultValidationError.getCode())));
                    } catch (ModuleNotFoundException m) {
                        try {
                            IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                            ed.event(new RenderingEvent(new RenderingException(e, name(new Locale("en", "US")), ErrorCode.ResultValidationError.getCode())));
                        } catch (ModuleNotFoundException m1) {
                        }
                    }
                    return new ArrayList<Node>();
                }
                List<Node> out = new ArrayList<Node>();
                out.add(xhtml);
                return out;
            } else {
                try {
                    throw new RenderingException(null, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.ResourcesObtainingError.getCode());
                } catch (ModuleNotFoundException m) {
                    throw new RenderingException(null, name(new Locale("en", "US")), ErrorCode.ResourcesObtainingError.getCode());
                }
            }
        } catch (XmlException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InternalError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InternalError.getCode());
            }
        } catch (XPathExpressionException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InternalError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InternalError.getCode());
            }
        } catch (DOMException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InternalError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InternalError.getCode());
            }
        }
    }

    private void stripeAnchors(Node node) throws RenderingException {
        try {
            NodeList anchors = ixml().evaluate(node, "//xhtml:a[@class=\"swemas_content\"]", ns());
            for (int i = 0; i < anchors.getLength(); ++i) {
                Node it = anchors.item(i);
                it.getParentNode().removeChild(it);
            }
        } catch (XPathExpressionException e) {
            try {
                throw new RenderingException(null, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InternalError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(null, name(new Locale("en", "US")), ErrorCode.InternalError.getCode());
            }
        }
    }
}
