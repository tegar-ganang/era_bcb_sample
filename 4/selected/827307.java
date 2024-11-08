package org.swemas.rendering.common;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
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

/**
 * @author Alexey Chernov
 * 
 */
public class SwPlainRenderer extends SwFlowRenderingModule implements IPlainRenderingChannel {

    /**
	 * @param kernel
	 * @throws InvocationTargetException
	 */
    public SwPlainRenderer(IKernel kernel) throws InvocationTargetException {
        super(kernel);
        ns().setNamespace("rp", "swemas/addons/rendering/plain");
    }

    @Override
    public Map<String, Map<String, List<Node>>> render(Map<String, Map<String, List<Node>>> iflow, Map<String, String> params, Node init, HttpServletRequest request) {
        String stm = "";
        if (params.containsKey("stream")) stm = params.get("stream");
        try {
            Map<String, List<Node>> istream;
            if (iflow.containsKey(stm)) istream = iflow.get(stm); else istream = new HashMap<String, List<Node>>();
            Map<String, List<Node>> ostream = new HashMap<String, List<Node>>();
            if (!params.containsKey("qname")) {
                try {
                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                    ed.event(new RenderingEvent(new RenderingException(null, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.NotEnoughInformationError.getCode())));
                } catch (ModuleNotFoundException m) {
                    try {
                        IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                        ed.event(new RenderingEvent(new RenderingException(null, name(new Locale("en", "US")), ErrorCode.NotEnoughInformationError.getCode())));
                    } catch (ModuleNotFoundException m1) {
                    }
                }
                iflow.remove(stm);
                return new HashMap<String, Map<String, List<Node>>>(iflow);
            }
            String qname = params.get("qname");
            String ns = "";
            if (params.containsKey("namespace")) ns = params.get("namespace");
            List<Node> nodes = new ArrayList<Node>();
            Document doc;
            if (init != null) doc = init.getOwnerDocument(); else doc = ixml().create();
            boolean injective = Boolean.parseBoolean(params.get("injective"));
            if (injective) {
                List<Map<String, Node>> parts = populateNodes(istream);
                for (int i = 0; i < parts.size(); ++i) {
                    Node tnode;
                    if (ns.equals("")) tnode = doc.createElement(qname); else tnode = doc.createElementNS(ns, qname);
                    Map<String, Node> map = parts.get(i);
                    if (map.containsKey("")) appendChild(tnode, map.get(""));
                    nodes.add(tnode);
                }
            } else {
                Node tnode;
                if (ns.equals("")) tnode = doc.createElement(qname); else tnode = doc.createElementNS(ns, qname);
                if (istream.containsKey("")) {
                    List<Node> chnodes = istream.get("");
                    for (Node c : chnodes) appendChild(tnode, c);
                }
                nodes.add(tnode);
            }
            String anchor = "";
            if (params.containsKey("anchor")) anchor = params.get("anchor");
            ostream.put(anchor, nodes);
            Map<String, Map<String, List<Node>>> oflow = new HashMap<String, Map<String, List<Node>>>();
            oflow.put(stm, ostream);
            for (String stmname : iflow.keySet()) if (!stmname.equals(stm)) oflow.put(stmname, iflow.get(stmname));
            return oflow;
        } catch (XmlException e) {
            try {
                IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                ed.event(new RenderingEvent(new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.InternalError.getCode())));
            } catch (ModuleNotFoundException m) {
                try {
                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                    ed.event(new RenderingEvent(new RenderingException(e, name(new Locale("en", "US")), ErrorCode.InternalError.getCode())));
                } catch (ModuleNotFoundException m1) {
                }
            }
            iflow.remove(stm);
            return new HashMap<String, Map<String, List<Node>>>(iflow);
        } catch (DOMException e) {
            try {
                IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                ed.event(new RenderingEvent(new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.ResultComposingError.getCode())));
            } catch (ModuleNotFoundException m) {
                try {
                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                    ed.event(new RenderingEvent(new RenderingException(e, name(new Locale("en", "US")), ErrorCode.ResultComposingError.getCode())));
                } catch (ModuleNotFoundException m1) {
                }
            }
            iflow.remove(stm);
            return new HashMap<String, Map<String, List<Node>>>(iflow);
        }
    }

    protected Map<String, String> parseInit(Node init) throws RenderingException {
        return new HashMap<String, String>();
    }
}
