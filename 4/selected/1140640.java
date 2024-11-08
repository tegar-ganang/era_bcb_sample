package de.pannenleiter.cocoon;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import org.w3c.dom.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.cocoon.store.*;
import org.apache.cocoon.parser.*;
import org.apache.cocoon.logger.*;
import org.apache.cocoon.processor.*;
import org.apache.cocoon.framework.*;
import org.apache.cocoon.transformer.*;
import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributeListImpl;
import org.apache.cocoon.Utils;
import org.apache.cocoon.Defaults;
import org.apache.xalan.xslt.*;
import org.apache.xalan.xpath.*;
import org.apache.xalan.xpath.xml.*;
import org.apache.xalan.xpath.xdom.XercesLiaison;
import org.apache.xalan.xslt.trace.*;
import org.apache.xml.serialize.*;
import de.pannenleiter.util.*;
import de.pannenleiter.servlet.LogRequestSink;
import de.pannenleiter.servlet.DBWrapper;
import de.opus5.servlet.HttpMultipartRequest;

/**
 * XalanProcessor -- a cocoon processor
 *
 *
 */
public class XalanProcessor implements Actor, Processor, Status, Defaults, Configurable {

    private static final Object rawReadWrite = new Object();

    private static final Object rawReadOnly = new Object();

    private Monitor monitor = new Monitor(10);

    private Parser parser;

    private Store store;

    private Logger logger;

    public void init(Director director) {
        this.parser = (Parser) director.getActor("parser");
        this.store = (Store) director.getActor("store");
        this.logger = (Logger) director.getActor("logger");
        LoggerProxy.init(logger);
    }

    public void init(Configurations conf) throws InitializationException {
        try {
            CocoonElement.init(conf, logger);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new InitializationException(ex.getMessage());
        }
    }

    public Document process(Document document, Dictionary parameters) throws Exception {
        HttpServletRequest request = (HttpServletRequest) parameters.get("request");
        ServletContext context = (ServletContext) parameters.get("context");
        String path = (String) parameters.get("path");
        String browser = (String) parameters.get("browser");
        try {
            Object resource = getResource(context, request, document, path, browser);
            Document result = this.parser.createEmptyDocument();
            if (resource == rawReadWrite) {
                return rawService(result, parameters, true);
            } else if (resource == rawReadOnly) {
                return rawService(result, parameters, false);
            } else {
                return transform(document, null, resource, resource.toString(), result, parameters);
            }
        } catch (PINotFoundException e) {
            return document;
        }
    }

    private Object getResource(ServletContext context, HttpServletRequest request, Document document, String path, String browser) throws ProcessorException {
        Object resource = null;
        Enumeration pis = Utils.getAllPIs(document, STYLESHEET_PI).elements();
        while (pis.hasMoreElements()) {
            Hashtable attributes = Utils.getPIPseudoAttributes((ProcessingInstruction) pis.nextElement());
            String type = (String) attributes.get("type");
            if ((type != null) && (type.equals("text/xsl"))) {
                String url = (String) attributes.get("href");
                if (url != null) {
                    Object local = null;
                    try {
                        if (url.charAt(0) == '/') {
                            local = new File(Utils.getRootpath(request, context) + url);
                        } else if (url.indexOf("://") < 0) {
                            local = new File(Utils.getBasepath(request, context) + url);
                        } else {
                            local = new URL(url);
                        }
                    } catch (MalformedURLException e) {
                        throw new ProcessorException("Could not associate stylesheet to document: " + url + " is a malformed URL.");
                    }
                    String media = (String) attributes.get("media");
                    if (media == null) {
                        resource = local;
                        if (browser == null) break;
                    } else if (browser != null) {
                        if (media.equals(browser)) {
                            resource = local;
                            break;
                        }
                    }
                }
            } else if ((type != null) && (type.equals("application/rmdms-raw"))) {
                Object access = attributes.get("access");
                if ("readwrite".equals(access)) {
                    resource = rawReadWrite;
                } else {
                    resource = rawReadOnly;
                }
            }
        }
        if (resource == null) {
            throw new ProcessorException("Could not associate stylesheet to document: " + " no matching stylesheet for: " + browser);
        } else {
            return resource;
        }
    }

    public boolean hasChanged(Object context) {
        return true;
    }

    public String getStatus() {
        return "rm -d ms Xalan Processor";
    }

    protected Document transform(Document in, String inBase, Object sheet, String sheetBase, Document out, Dictionary parameters) throws Exception {
        HttpServletRequest request = (HttpServletRequest) parameters.get("request");
        HttpServletResponse response = (HttpServletResponse) parameters.get("response");
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
            request = new HttpMultipartRequest(request, "/usr/tmp", "pl-upload-");
        }
        XSLTInputSource i = new XSLTInputSource(in);
        XSLTInputSource s = new XSLTInputSource(sheetBase);
        XSLTResultTarget o = new XSLTResultTarget(out);
        String key = Utils.encode(request);
        StylesheetRoot root;
        try {
            root = (StylesheetRoot) this.store.get(sheet);
            if ((root != null) && (!this.monitor.hasChanged(key))) {
                logger.log(this, "reuse stylesheet " + root, Logger.DEBUG);
            } else {
                if (root != null) logger.log(this, "stylesheet " + root + " has changed", Logger.DEBUG);
                logger.log(this, "create stylesheet " + root, Logger.DEBUG);
                WatchDriver styleprocessor = new WatchDriver(key);
                styleprocessor.watching = true;
                root = styleprocessor.processStylesheet(s);
                styleprocessor.watching = false;
                this.store.hold(sheet, root);
            }
        } catch (Exception e) {
            this.monitor.invalidate(request);
            LogRequestSink trace = new LogRequestSink();
            Trace.getInstance().setCurrent(trace);
            trace.setRequest(request);
            trace.setException(e);
            trace.close();
            throw new ProcessorException("Could not associate stylesheet to document: " + " error reading " + sheetBase + ": " + e);
        }
        XSLTProcessor processor = XSLTProcessorFactory.getProcessor(new XercesLiaison());
        Document doc = new XercesLiaison().getDOMFactory();
        DocumentFragment resultFragment = new ResultTreeFrag(doc, ((XSLTEngineImpl) processor).getExecContext());
        Element ps = doc.createElement("parameter");
        resultFragment.appendChild(ps);
        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String vals[] = request.getParameterValues(name);
            for (int v = 0; v < vals.length; v++) {
                Element p = doc.createElement(name);
                ps.appendChild(p);
                Text t = doc.createTextNode(vals[v]);
                p.appendChild(t);
            }
        }
        processor.setStylesheetParam("request", new XNodeSet(resultFragment));
        processor.setStylesheetParam("servlet-request", new org.apache.xalan.xpath.XObject(request));
        processor.setStylesheetParam("servlet-response", new org.apache.xalan.xpath.XObject(response));
        LogRequestSink trace = null;
        PlTraceListener traceListener = null;
        try {
            String suppress = request.getParameter("suppresstrace");
            if (Trace.getInstance().getTrace() && !"yes".equals(suppress)) {
                trace = new LogRequestSink();
                Trace.getInstance().setCurrent(trace);
                trace.setRequest(request);
            }
            if (trace != null && Trace.getInstance().getTracePoint("template")) {
                traceListener = new PlTraceListener();
                root.addTraceListener(traceListener);
            }
            root.process(processor, i, o);
            if (de.pannenleiter.xmlapache.Store.exHack != null) {
                Exception ex = de.pannenleiter.xmlapache.Store.exHack;
                de.pannenleiter.xmlapache.Store.exHack = null;
                throw ex;
            }
        } catch (Exception ex) {
            if (trace == null) {
                trace = new LogRequestSink();
                Trace.getInstance().setCurrent(trace);
                trace.setRequest(request);
            }
            trace.setException(ex);
            throw ex;
        } finally {
            if (traceListener != null) {
                traceListener.closeLogs(0);
                root.removeTraceListener(traceListener);
            }
            if (trace != null) {
                finalizeTrace(trace, out);
            }
        }
        return out;
    }

    protected Document rawService(Document out, Dictionary parameters, boolean readwrite) throws Exception {
        HttpServletRequest request = (HttpServletRequest) parameters.get("request");
        HttpServletResponse response = (HttpServletResponse) parameters.get("response");
        LogRequestSink trace = null;
        try {
            String suppress = request.getParameter("suppresstrace");
            if (Trace.getInstance().getTrace() && !"yes".equals(suppress)) {
                trace = new LogRequestSink();
                Trace.getInstance().setCurrent(trace);
                trace.setRequest(request);
            }
            String todo = request.getParameter("todo");
            ProcessingInstruction pi = out.createProcessingInstruction("cocoon-format", "type=\"text/xml\"");
            out.appendChild(pi);
            FormatterToDOM builder = new FormatterToDOM(out);
            if ("fetch".equals(todo)) {
                DBWrapper.fetch(builder, request.getParameter("db"), request.getParameter("document"), request.getParameter("pattern"), request.getParameter("fetch-childs"), request.getParameter("fetch-attributes"), request.getParameter("flags"), trace);
            } else if ("archive".equals(todo)) {
                int id = 0;
                String tmp = request.getParameter("id");
                if (tmp != null) {
                    id = Integer.parseInt(tmp);
                }
                DBWrapper.fetchArchive(builder, request.getParameter("db"), request.getParameter("document"), id, request.getParameter("flags"), trace);
            } else if ("write".equals(todo) && readwrite) {
                int pos = 0;
                String[] owner = null;
                if (request.getParameter("owner") != null) {
                    owner = new String[1];
                    owner[0] = request.getParameter("owner");
                }
                String tmp = request.getParameter("position");
                if (tmp != null) {
                    pos = Integer.parseInt(tmp);
                }
                String[] id = DBWrapper.write(request.getParameter("db"), request.getParameter("document"), request.getParameter("xml"), owner, pos, trace);
                AttributeListImpl atts = new AttributeListImpl();
                atts.addAttribute("plid", "CDATA", id[0]);
                builder.startElement("ok", atts);
                builder.endElement("ok");
            } else if ("remove".equals(todo) && readwrite) {
                String[] id = null;
                if (request.getParameter("id") != null) {
                    id = new String[1];
                    id[0] = request.getParameter("id");
                }
                int version = DBWrapper.remove(request.getParameter("db"), request.getParameter("document"), id, trace);
                AttributeListImpl atts = new AttributeListImpl();
                atts.addAttribute("plversion", "CDATA", String.valueOf(version));
                builder.startElement("ok", atts);
                builder.endElement("ok");
            } else {
                throw new Exception("illegal request: " + todo + " write=" + readwrite);
            }
        } catch (Exception ex) {
            if (trace == null) {
                trace = new LogRequestSink();
                Trace.getInstance().setCurrent(trace);
                trace.setRequest(request);
            }
            trace.setException(ex);
            throw ex;
        } finally {
            if (trace != null) {
                finalizeTrace(trace, out);
            }
        }
        return out;
    }

    protected void finalizeTrace(LogRequestSink trace, Document out) {
        try {
            StringWriter sink = new StringWriter();
            OutputFormat of = new OutputFormat("html", "ISO-8859-1", true);
            of.setLineWidth(0);
            HTMLSerializer s = new HTMLSerializer(sink, of);
            s.serialize(out);
            trace.setResponse(sink.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        trace.close();
    }

    class WatchDriver extends XSLTEngineImpl {

        boolean watching;

        String key;

        public void addResource(URL url) throws SAXException {
            try {
                String name = url.toString();
                if (name.startsWith("file:")) {
                    monitor.watch(key, new File(name.substring(5)));
                } else {
                    monitor.watch(key, url);
                }
            } catch (Exception ex) {
                throw new SAXException(ex);
            }
        }

        public URL getURLFromString(String urlString, String base) throws SAXException {
            URL tmp = super.getURLFromString(urlString, base);
            logger.log(this, "watch resource " + tmp, Logger.DEBUG);
            if (watching) addResource(tmp);
            return tmp;
        }

        public WatchDriver(String key) throws SAXException {
            super();
            this.key = key;
            watching = false;
        }
    }
}

class PlProblemListener extends ProblemListenerDefault {

    private ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    public PrintWriter myOut = null;

    public PlProblemListener() {
        myOut = new PrintWriter(new BufferedOutputStream(outStream), true);
        setDiagnosticsOutput(myOut);
    }

    public String getMessage() {
        return outStream.toString();
    }
}

class PlTraceListener implements TraceListener {

    Stack openLogs = new Stack();

    Stack openApplies = new Stack();

    public String str(Node node) {
        if (node instanceof Document) {
            return "/";
        }
        if (node instanceof DocumentFragment) {
            return "(/)";
        }
        if (node instanceof Element) {
            StringBuffer buf = new StringBuffer();
            buf.append('<');
            buf.append(node.getNodeName());
            NamedNodeMap atts = node.getAttributes();
            for (int i = 0; i < atts.getLength(); i++) {
                Node at = atts.item(i);
                buf.append(' ');
                buf.append(at.getNodeName());
                buf.append("=\"");
                buf.append(at.getNodeValue());
                buf.append('"');
            }
            buf.append('>');
            return buf.toString();
        }
        return "[" + node.getClass().getName() + "]";
    }

    public void trace(TracerEvent ev) {
        if (openLogs.size() > 0) {
            boolean done = false;
            Node lookup = ev.m_styleNode.getParentNode();
            while (!done && lookup instanceof ElemTemplateElement) {
                if (lookup instanceof ElemTemplate) {
                    if (openLogs.peek() == lookup) {
                        done = true;
                    } else {
                        closeLogs(openLogs.lastIndexOf(lookup) + 1);
                    }
                } else {
                    lookup = lookup.getParentNode();
                }
            }
        }
        if (ev.m_styleNode instanceof ElemTemplate) {
            LogSink log = Trace.getInstance().getCurrent();
            if (log != null) {
                ElemTemplate template = (ElemTemplate) ev.m_styleNode;
                StringBuffer buf = new StringBuffer();
                buf.append("<xsl:template");
                if (template.m_matchPattern != null) {
                    buf.append(" match=\"");
                    buf.append(template.m_matchPattern.getPatternString());
                    buf.append("\"");
                }
                if (template.m_name != null) {
                    buf.append(" name=\"");
                    buf.append(template.m_name);
                    buf.append("\"");
                }
                if (template.m_mode != null) {
                    buf.append(" mode=\"");
                    buf.append(template.m_mode);
                    buf.append("\"");
                }
                if (template.m_priority != XPath.MATCH_SCORE_NONE) {
                    buf.append(" priority=\"");
                    buf.append(String.valueOf(template.m_priority));
                    buf.append("\"");
                }
                buf.append("> line: ");
                buf.append(String.valueOf(template.m_lineNumber));
                LogSink myLog = log.createLogSink("template");
                myLog.append("template", buf.toString());
                myLog.append("node", str(ev.m_sourceNode));
                openLogs.push(myLog);
                openLogs.push(template);
            }
        } else if (ev.m_styleNode instanceof ElemApplyTemplates) {
            LogSink log = Trace.getInstance().getCurrent();
            if (log != null) {
                ElemApplyTemplates template = (ElemApplyTemplates) ev.m_styleNode;
                StringBuffer buf = new StringBuffer();
                buf.append("<xsl:apply-templates");
                if (template.m_selectPattern != null) {
                    buf.append(" select=\"");
                    buf.append(template.m_selectPattern.getPatternString());
                    buf.append("\"");
                }
                if (template.m_mode != null) {
                    buf.append(" mode=\"");
                    buf.append(template.m_mode);
                    buf.append("\"");
                }
                buf.append("> line: ");
                buf.append(String.valueOf(template.m_lineNumber));
                log.append("apply-templates", buf.toString());
            }
        }
    }

    public void closeLogs(int newSize) {
        while (openLogs.size() > newSize) {
            Object template = openLogs.pop();
            LogSink sink = (LogSink) openLogs.pop();
            sink.close();
        }
    }

    public void selected(SelectionEvent ev) {
    }

    public void generated(GenerateEvent ev) {
    }
}

class LoggerProxy extends de.apache.cocoon.Logger {

    Logger real;

    static void init(Logger real) {
        instance = new LoggerProxy(real);
    }

    public void log(String message, int level) {
        real.log(message, level);
    }

    public void log(Exception exception, String message, int level) {
        real.log(exception, message, level);
    }

    public void log(Object originator, String message, int level) {
        real.log(originator, message, level);
    }

    public void log(Object originator, Exception exception, String message, int level) {
        real.log(originator, exception, message, level);
    }

    protected void log(String message) {
    }

    protected void log(Exception exception, String message) {
    }

    LoggerProxy(Logger real) {
        this.real = real;
    }
}
