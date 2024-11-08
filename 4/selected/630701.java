package org.swemas.rendering.backends.data.xml;

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
import org.swemas.data.xml.IXmlChannel;
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
 * @author ray
 * 
 */
public class SwXmlDataBackend extends SwFlowRenderingModule implements IXmlDataBackendChannel {

    public SwXmlDataBackend(IKernel kernel) throws InvocationTargetException {
        super(kernel);
        ns().setNamespace("dxml", "swemas/rendering/backends/data/xml");
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
                if (uri != null) {
                    String sp = parameters.get("ns_prefix");
                    String su = parameters.get("ns_uri");
                    String sl = parameters.get("ns_schema_location");
                    try {
                        IXmlChannel ixml = (IXmlChannel) kernel().getChannel(IXmlChannel.class);
                        if (sl != null && sp != null && su != null) {
                            ns().setNamespace(sp, su);
                            try {
                                NodeList ret = ixml.evaluate(ixml.open(uri, sl), qf, ns());
                                List<Node> rn = new ArrayList<Node>();
                                for (int i = 0; i < ret.getLength(); ++i) rn.add(ret.item(i));
                                ostream.put(anchor, rn);
                                oflow.put(stm, ostream);
                            } catch (XmlException e) {
                                try {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.ResourcesObtainingError.getCode())));
                                } catch (ModuleNotFoundException m) {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(new Locale("en", "US")), ErrorCode.ResourcesObtainingError.getCode())));
                                }
                                return oflow;
                            } catch (XPathExpressionException e) {
                                try {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.QueryError.getCode())));
                                } catch (ModuleNotFoundException m) {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(new Locale("en", "US")), ErrorCode.QueryError.getCode())));
                                }
                                return oflow;
                            }
                        } else if (sp != null && su != null) {
                            ns().setNamespace(sp, su);
                            try {
                                NodeList ret = ixml.evaluate(ixml.open(uri), qf, ns());
                                List<Node> rn = new ArrayList<Node>();
                                for (int i = 0; i < ret.getLength(); ++i) rn.add(ret.item(i));
                                ostream.put(anchor, rn);
                                oflow.put(stm, ostream);
                            } catch (XmlException e) {
                                try {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.ResourcesObtainingError.getCode())));
                                } catch (ModuleNotFoundException m) {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(new Locale("en", "US")), ErrorCode.ResourcesObtainingError.getCode())));
                                }
                                return oflow;
                            } catch (XPathExpressionException e) {
                                try {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.QueryError.getCode())));
                                } catch (ModuleNotFoundException m) {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(new Locale("en", "US")), ErrorCode.QueryError.getCode())));
                                }
                                return oflow;
                            }
                        } else {
                            try {
                                NodeList ret = ixml.evaluate(ixml.open(uri), qf);
                                List<Node> rn = new ArrayList<Node>();
                                for (int i = 0; i < ret.getLength(); ++i) rn.add(ret.item(i));
                                ostream.put(anchor, rn);
                                oflow.put(stm, ostream);
                            } catch (XmlException e) {
                                try {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.ResourcesObtainingError.getCode())));
                                } catch (ModuleNotFoundException m) {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(new Locale("en", "US")), ErrorCode.ResourcesObtainingError.getCode())));
                                }
                                return oflow;
                            } catch (XPathExpressionException e) {
                                try {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.QueryError.getCode())));
                                } catch (ModuleNotFoundException m) {
                                    IEventDispatchingChannel ed = (IEventDispatchingChannel) kernel().getChannel(IEventDispatchingChannel.class);
                                    ed.event(new RenderingEvent(new RenderingException(e, name(new Locale("en", "US")), ErrorCode.QueryError.getCode())));
                                }
                                return oflow;
                            }
                        }
                    } catch (ModuleNotFoundException e) {
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
                        return oflow;
                    }
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
                ixml().validate(request, kernel().getPath("/schema/xml_data_backend.xsd"));
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
                    Node doc = ixml().evaluate(request, "./dt:doc", ns()).item(0);
                    if (doc != null) {
                        ret.put("uri", ixml().evaluate(doc, "./dt:uri/text()", ns()).item(0).getNodeValue());
                        NodeList nsl = ixml().evaluate(doc, "./dt:namespace", ns());
                        if (nsl.getLength() > 0) {
                            Node nsnode = nsl.item(0);
                            ret.put("ns_prefix", ixml().evaluate(nsnode, "./dt:prefix/text()", ns()).item(0).getNodeValue());
                            ret.put("ns_uri", ixml().evaluate(nsnode, "./dt:uri/text()", ns()).item(0).getNodeValue());
                            NodeList loc = ixml().evaluate(nsnode, "./dt:location/text()", ns());
                            if (loc.getLength() > 0) ret.put("ns_schema_location", loc.item(0).getNodeValue());
                        }
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
