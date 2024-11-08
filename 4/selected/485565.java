package org.swemas.rendering.composing;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathExpressionException;
import org.swemas.rendering.ErrorCode;
import org.swemas.rendering.IFlowRenderingChannel;
import org.swemas.rendering.RenderingException;
import org.swemas.core.ModuleNotFoundException;
import org.swemas.core.dispatcher.IHttpProcessingChannel;
import org.swemas.core.kernel.IKernel;
import org.swemas.core.messaging.ILocaleProvidingChannel;
import org.swemas.data.xml.XmlException;
import org.swemas.data.xml.XmlUsingModule;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Alexey Chernov
 * 
 */
public abstract class SwXmlComposingModule extends XmlUsingModule implements IHttpProcessingChannel, IComposingChannel {

    /**
	 * @param kernel
	 */
    public SwXmlComposingModule(IKernel kernel) throws InvocationTargetException {
        super(kernel);
        ns().setNamespace("tp", "swemas/topology");
        ns().setNamespace("tpl", "swemas/template");
    }

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response) throws RenderingException {
        Node doc = render(null, request).get(0);
        try {
            ixml().save(doc, response.getOutputStream());
        } catch (XmlException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InternalError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InternalError.getCode());
            }
        } catch (IOException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InternalError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InternalError.getCode());
            }
        }
    }

    protected Map<String, Map<String, List<Node>>> parseElement(Node node, HttpServletRequest request) throws RenderingException {
        try {
            String cname = parseChannelName(node);
            Map<String, Map<String, List<Node>>> iflow = new HashMap<String, Map<String, List<Node>>>();
            NodeList childs = ixml().evaluate(node, "./tp:content/tp:element", ns());
            for (int i = 0; i < childs.getLength(); ++i) {
                Map<String, Map<String, List<Node>>> oflow = parseElement(childs.item(i), request);
                for (String flowname : oflow.keySet()) if (iflow.containsKey(flowname)) {
                    Map<String, List<Node>> ostream = oflow.get(flowname);
                    Map<String, List<Node>> istream = iflow.get(flowname);
                    for (String anchname : ostream.keySet()) if (istream.containsKey(anchname)) istream.get(anchname).addAll(ostream.get(anchname)); else istream.put(anchname, ostream.get(anchname));
                } else iflow.put(flowname, oflow.get(flowname));
            }
            Node init = ixml().evaluate(node, "./tp:init", ns()).item(0);
            Map<String, String> params = new HashMap<String, String>();
            if (node.getAttributes().getNamedItem("qname") != null) params.put("qname", node.getAttributes().getNamedItem("qname").getNodeValue());
            if (node.getAttributes().getNamedItem("anchor") != null) params.put("anchor", node.getAttributes().getNamedItem("anchor").getNodeValue());
            if (node.getAttributes().getNamedItem("namespace") != null) params.put("namespace", node.getAttributes().getNamedItem("namespace").getNodeValue());
            if (node.getAttributes().getNamedItem("stream") != null) params.put("stream", node.getAttributes().getNamedItem("stream").getNodeValue());
            IFlowRenderingChannel module = (IFlowRenderingChannel) kernel().getChannel(cname);
            return module.render(iflow, params, init, request);
        } catch (XPathExpressionException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.QueryError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.QueryError.getCode());
            }
        } catch (ModuleNotFoundException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.CallError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.CallError.getCode());
            }
        }
    }

    protected String parseChannelName(Node node) {
        return node.getAttributes().getNamedItem("module").getNodeValue();
    }

    protected Map<String, String> parseInit(Node init) throws RenderingException {
        Map<String, String> params = new HashMap<String, String>();
        try {
            if (init != null) {
                NodeList pms = ixml().evaluate(init, "./parameter", ns());
                for (int i = 0; i < pms.getLength(); ++i) {
                    String name = ixml().evaluate(pms.item(i), "./@name", ns()).item(0).getNodeValue();
                    String value = ixml().evaluate(pms.item(i), "./text()", ns()).item(0).getNodeValue();
                    params.put(name, value);
                }
                return params;
            } else {
                try {
                    throw new RenderingException(null, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InitializationError.getCode());
                } catch (ModuleNotFoundException m) {
                    throw new RenderingException(null, name(new Locale("en", "US")), ErrorCode.InitializationError.getCode());
                }
            }
        } catch (XPathExpressionException e) {
            try {
                throw new RenderingException(null, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InitializationError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(null, name(new Locale("en", "US")), ErrorCode.InitializationError.getCode());
            }
        }
    }
}
