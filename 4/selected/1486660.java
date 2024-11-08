package org.swemas.rendering.backends.data.db;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.xml.xpath.XPathExpressionException;
import org.swemas.core.ModuleNotFoundException;
import org.swemas.core.event.IEventDispatchingChannel;
import org.swemas.core.kernel.IKernel;
import org.swemas.core.messaging.ILocaleProvidingChannel;
import org.swemas.data.xml.XmlException;
import org.swemas.rendering.ErrorCode;
import org.swemas.rendering.RenderingEvent;
import org.swemas.rendering.RenderingException;
import org.swemas.rendering.SwFlowRenderingModule;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Alexey Chernov
 * 
 */
public class SwDbBackend extends SwFlowRenderingModule implements IDbBackendChannel {

    public SwDbBackend(IKernel kernel) throws InvocationTargetException {
        super(kernel);
        ns().setNamespace("ddb", "swemas/rendering/backends/data/db");
    }

    @Override
    public Map<String, Map<String, List<Node>>> render(Map<String, Map<String, List<Node>>> iflow, Map<String, String> params, Node init, HttpServletRequest request) throws RenderingException {
        Map<String, Map<String, List<Node>>> oflow = new HashMap<String, Map<String, List<Node>>>();
        Map<String, List<Node>> ostream = new HashMap<String, List<Node>>();
        String anchor = "";
        if (params.containsKey("anchor")) anchor = params.get("anchor");
        String stm = "";
        if (params.containsKey("stream")) stm = params.get("stream");
        try {
            Map<String, String> parameters = parseInit(init);
            String text = parameters.get("value");
            if (text != null) {
                if (parameters.containsKey("formatted") && parameters.get("formatted").equals("true") && request != null) text = String.format(text, request.getParameterMap().values().toArray());
                List<Node> ret = new ArrayList<Node>();
                Document doc;
                if (init != null) doc = init.getOwnerDocument(); else doc = ixml().create();
                if (parameters.containsKey("type") && parameters.get("type").equals("CDATA")) ret.add(doc.createCDATASection(text)); else ret.add(doc.createTextNode(text));
                ostream.put(anchor, ret);
                oflow.put(stm, ostream);
            } else {
                String qf = parameters.get("query");
                if (parameters.containsKey("formatted") && parameters.get("formatted").equals("true") && request != null) String.format(qf, request.getParameterMap().values());
                String uri = parameters.get("uri");
                if (uri == null) {
                }
            }
            return oflow;
        } catch (XmlException e) {
            try {
                IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                ed.event(new RenderingEvent(new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.ResourcesObtainingError.getCode())));
            } catch (ModuleNotFoundException m) {
                try {
                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                    ed.event(new RenderingEvent(new RenderingException(e, name(new Locale("en", "US")), ErrorCode.ResourcesObtainingError.getCode())));
                } catch (ModuleNotFoundException e1) {
                }
            }
            return oflow;
        } catch (IllegalFormatException ma) {
            try {
                IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                ed.event(new RenderingEvent(new RenderingException(ma, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.ResourcesObtainingError.getCode())));
            } catch (ModuleNotFoundException m) {
                try {
                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                    ed.event(new RenderingEvent(new RenderingException(ma, name(new Locale("en", "US")), ErrorCode.ResourcesObtainingError.getCode())));
                } catch (ModuleNotFoundException e) {
                }
            }
            return oflow;
        } catch (RenderingException e) {
            try {
                IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                ed.event(new RenderingEvent(e));
            } catch (ModuleNotFoundException m) {
                try {
                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                    ed.event(new RenderingEvent(e));
                } catch (ModuleNotFoundException m1) {
                }
            }
            return oflow;
        }
    }

    @Override
    protected Map<String, String> parseInit(Node init) throws RenderingException {
        Map<String, String> ret = new HashMap<String, String>();
        try {
            if (init != null) {
                Node request = ixml().evaluate(init, "./dt:request", ns()).item(0);
                ixml().validate(request, kernel().getPath("/schema/db_data_backend.xsd"));
                boolean formatted = false;
                if (request.getAttributes().getNamedItem("formatted") != null) formatted = Boolean.parseBoolean(request.getAttributes().getNamedItem("formatted").getNodeValue());
                ret.put("formatted", Boolean.toString(formatted));
                String type = "text";
                if (request.getAttributes().getNamedItem("type") != null) type = request.getAttributes().getNamedItem("type").getNodeValue();
                ret.put("type", type);
                NodeList vl = ixml().evaluate(request, "./dt:value/text()", ns());
                if (vl.getLength() > 0) ret.put("value", vl.item(0).getNodeValue()); else {
                    NodeList ql = ixml().evaluate(request, "./dt:query/text()", ns());
                    ret.put("query", ql.item(0).getNodeValue());
                    Node server = ixml().evaluate(request, "./dt:server", ns()).item(0);
                    if (server != null) {
                        ret.put("server", ixml().evaluate(server, "./dt:host/text()", ns()).item(0).getNodeValue());
                        ret.put("port", ixml().evaluate(server, "./dt:port/text()", ns()).item(0).getNodeValue());
                        ret.put("login", ixml().evaluate(server, "./dt:login/text()", ns()).item(0).getNodeValue());
                        ret.put("pass", ixml().evaluate(server, "./dt:pass/text()", ns()).item(0).getNodeValue());
                    }
                }
                return ret;
            } else {
                try {
                    throw new RenderingException(null, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InitializationError.getCode());
                } catch (ModuleNotFoundException m) {
                    throw new RenderingException(null, name(new Locale("en", "US")), ErrorCode.InitializationError.getCode());
                }
            }
        } catch (DOMException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InternalError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InternalError.getCode());
            }
        } catch (XmlException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InitializationError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InitializationError.getCode());
            }
        } catch (XPathExpressionException e) {
            try {
                throw new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InitializationError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InitializationError.getCode());
            }
        }
    }
}
