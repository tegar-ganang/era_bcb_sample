package org.xmlsh.commands.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import javanet.staxutils.XMLStreamUtils;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.xmlsh.core.Options;
import org.xmlsh.core.XCommand;
import org.xmlsh.core.XValue;
import org.xmlsh.sh.shell.SerializeOpts;
import org.xmlsh.util.Util;

public class xaddbase extends XCommand {

    private XMLEventFactory mFactory = XMLEventFactory.newInstance();

    private static QName mBaseQName = new QName("http://www.w3.org/XML/1998/namespace", "base", "xml");

    @Override
    public int run(List<XValue> args) throws Exception {
        boolean opt_all = false;
        boolean opt_relative = false;
        Options opts = new Options("a=all,r=relative", SerializeOpts.getOptionDefs());
        opts.parse(args);
        SerializeOpts serializeOpts = getSerializeOpts(opts);
        XMLEventReader reader = getStdin().asXMLEventReader(serializeOpts);
        XMLEventWriter writer = getStdout().asXMLEventWriter(serializeOpts);
        opt_all = opts.hasOpt("all");
        opt_relative = opts.hasOpt("relative");
        add_base(reader, writer, opt_all, opt_relative, getStdin().getSystemId());
        return 0;
    }

    private XMLEvent add_base_attr(StartElement start, String base) {
        Attribute attr = mFactory.createAttribute(mBaseQName, base);
        return XMLStreamUtils.mergeAttributes(start, Collections.singletonList(attr).iterator(), mFactory);
    }

    private String resolve(String parentBaseURI, String baseURI, boolean opt_relative) throws URISyntaxException {
        if (!opt_relative) return baseURI;
        if (Util.isBlank(parentBaseURI)) return baseURI;
        URI u = new URI(parentBaseURI);
        String path = u.getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0) path = path.substring(0, slash);
        URI parent_URI = new URI(u.getScheme(), u.getUserInfo(), u.getHost(), u.getPort(), path, u.getQuery(), u.getFragment());
        URI base_URI = new URI(baseURI);
        URI relative = parent_URI.relativize(base_URI);
        return relative.toString();
    }

    private void add_base(XMLEventReader reader, XMLEventWriter writer, boolean opt_all, boolean opt_relative, String parentURI) throws URISyntaxException, XMLStreamException {
        Stack<String> mURIs = new Stack<String>();
        mURIs.push(parentURI);
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.getEventType() == XMLEvent.START_ELEMENT) {
                boolean addBase = false;
                StartElement start = event.asStartElement();
                String baseURI = getBaseURI(start);
                parentURI = mURIs.lastElement();
                boolean bIsRoot = mURIs.size() == 1;
                if (bIsRoot) {
                    addBase = true;
                } else if (opt_all) addBase = true; else if (baseURI.equals(parentURI)) addBase = false; else if (!Util.isEqual(parentURI, baseURI)) addBase = true;
                if (addBase) {
                    String uri = resolve(parentURI, baseURI, bIsRoot ? false : opt_relative);
                    event = add_base_attr(start, uri);
                } else event = removeBase(start);
                mURIs.push(baseURI);
            } else if (event.getEventType() == XMLEvent.END_ELEMENT) mURIs.pop();
            writer.add(event);
        }
    }

    private String getBaseURI(StartElement start) {
        Attribute baseAttr = start.getAttributeByName(mBaseQName);
        if (baseAttr != null) return baseAttr.getValue();
        String systemID = start.getLocation().getSystemId();
        if (Util.isBlank(systemID)) return "";
        return systemID;
    }

    @SuppressWarnings("unchecked")
    private XMLEvent removeBase(StartElement start) {
        if (start.getAttributeByName(mBaseQName) != null) {
            Iterator iter = start.getAttributes();
            while (iter.hasNext()) {
                Attribute a = (Attribute) iter.next();
                if (a.getName().equals(mBaseQName)) iter.remove();
            }
        }
        return start;
    }
}
