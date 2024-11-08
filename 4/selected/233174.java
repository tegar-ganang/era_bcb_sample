package org.xmlsh.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.xmlsh.core.Options;
import org.xmlsh.core.XCommand;
import org.xmlsh.core.XValue;
import org.xmlsh.core.Options.OptionValue;
import org.xmlsh.util.Util;

public class xsplit extends XCommand {

    private XMLOutputFactory mOutputFactory = XMLOutputFactory.newInstance();

    private XMLEventFactory mEventFactory = XMLEventFactory.newInstance();

    private XMLEvent mRootNode = null;

    private String mPrefix = "x";

    private int mSeq = 0;

    private String mSuffix = "";

    private String mExt = ".xml";

    private boolean mNoRoot = false;

    private int mNumChildren = 1;

    private List<XMLEvent> mHeader = new ArrayList<XMLEvent>();

    public int run(List<XValue> args) throws Exception {
        Options opts = new Options("c:,w:,n,p:,e:,s:", args);
        opts.parse();
        OptionValue ow = opts.getOpt("w");
        XValue wrapper = null;
        if (ow != null) {
            wrapper = ow.getValue();
            mRootNode = mEventFactory.createStartElement(new QName(null, wrapper.toString()), null, null);
        }
        mNumChildren = Util.parseInt(opts.getOptString("c", "1"), 1);
        mExt = opts.getOptString("e", mExt);
        mSuffix = opts.getOptString("s", mSuffix);
        mPrefix = opts.getOptString("p", mPrefix);
        if (opts.hasOpt("n")) {
            mNoRoot = true;
            mNumChildren = 1;
        }
        List<XValue> xvargs = opts.getRemainingArgs();
        if (xvargs.size() > 1) {
            usage();
            return 1;
        }
        InputStream is = xvargs.size() == 1 ? new FileInputStream(getFile(xvargs.get(0))) : getStdin();
        split(is);
        return 0;
    }

    private void split(InputStream is) throws XMLStreamException, IOException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.valueOf(true));
        XMLEventReader xmlreader = inputFactory.createXMLEventReader(is);
        while (xmlreader.hasNext()) {
            XMLEvent e = xmlreader.nextEvent();
            if (e.getEventType() != XMLStreamConstants.START_ELEMENT) {
                mHeader.add(e);
                continue;
            }
            if (mRootNode == null) mHeader.add(e); else mHeader.add(mRootNode);
            break;
        }
        while (xmlreader.hasNext()) {
            XMLEvent e = xmlreader.nextEvent();
            int type = e.getEventType();
            if (type == XMLStreamConstants.START_ELEMENT) {
                write(xmlreader, e);
            } else if (type == XMLStreamConstants.END_ELEMENT || type == XMLStreamConstants.END_DOCUMENT) {
            } else {
                printErr("Skipping XML node: " + e.toString());
            }
        }
        xmlreader.close();
    }

    private void write(XMLEventReader xmlreader, XMLEvent first) throws XMLStreamException, IOException {
        File fout = nextFile();
        OutputStream fo = new FileOutputStream(fout);
        XMLEventWriter w = mOutputFactory.createXMLEventWriter(fo);
        for (XMLEvent e : mHeader) w.add(e);
        w.add(first);
        int depth = 0;
        int nchild = 0;
        while (xmlreader.hasNext()) {
            XMLEvent e = xmlreader.nextEvent();
            w.add(e);
            if (e.getEventType() == XMLStreamConstants.START_ELEMENT) depth++; else if (e.getEventType() == XMLStreamConstants.END_ELEMENT) {
                if (depth-- <= 0) {
                    if (++nchild == mNumChildren) break;
                }
            }
        }
        w.add(mEventFactory.createEndElement(first.asStartElement().getName(), null));
        w.add(mEventFactory.createEndDocument());
        w.close();
        fo.close();
    }

    private File nextFile() throws IOException {
        File f = getFile(mPrefix + mSeq++ + mSuffix + mExt);
        return f;
    }

    private int usage() {
        printErr("Usage: xsplit [-w wrap] [-c children] [-n]  [-p prefix] [file]");
        return 1;
    }
}
