package org.xmlsh.commands.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.xmlsh.core.InputPort;
import org.xmlsh.core.Options;
import org.xmlsh.core.Options.OptionValue;
import org.xmlsh.core.OutputPort;
import org.xmlsh.core.XCommand;
import org.xmlsh.core.XValue;
import org.xmlsh.sh.shell.SerializeOpts;
import org.xmlsh.util.Util;

public class xsplit extends XCommand {

    private XMLOutputFactory mOutputFactory = XMLOutputFactory.newInstance();

    private XMLEventFactory mEventFactory = XMLEventFactory.newInstance();

    private XMLEvent mRootNode = null;

    private String mPrefix = "x";

    private int mSeq = 0;

    private String mSuffix = "";

    private String mExt = ".xml";

    private File mOutputDir = null;

    private boolean mNoRoot = false;

    private int mNumChildren = 1;

    private boolean mNoDTD = false;

    private boolean mNoPI = false;

    private XValue mList = null;

    private List<XMLEvent> mHeader = new ArrayList<XMLEvent>();

    public int run(List<XValue> args) throws Exception {
        Options opts = new Options("c=children:,w=wrap:,n,p=prefix:,e=ext:,s=suffix:,n=nowrap,o=output:,nopi,nodtd,l=list:", SerializeOpts.getOptionDefs());
        opts.parse(args);
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
        if (opts.hasOpt("o")) mOutputDir = getFile(opts.getOptValue("o"));
        mNoDTD = opts.hasOpt("nodtd");
        mNoPI = opts.hasOpt("nopi");
        mList = opts.getOptValue("list");
        if (opts.hasOpt("n")) {
            mNumChildren = 1;
            mNoRoot = true;
        }
        List<XValue> xvargs = opts.getRemainingArgs();
        if (xvargs.size() > 1) {
            usage();
            return 1;
        }
        InputPort in = xvargs.size() == 1 ? getInput(xvargs.get(0)) : getStdin();
        PrintWriter listWriter = null;
        OutputPort listOut = null;
        try {
            SerializeOpts serializeOpts = getSerializeOpts(opts);
            InputStream is = in.asInputStream(serializeOpts);
            if (mList != null) listWriter = (listOut = getOutput(mList, false)).asPrintWriter(serializeOpts);
            split(in.getSystemId(), is, listWriter);
            is.close();
        } catch (Exception e) {
            printErr("Exception splitting input", e);
        } finally {
            in.release();
            if (listWriter != null) listWriter.close();
            if (listOut != null) listOut.release();
        }
        return 0;
    }

    private void split(String systemId, InputStream is, PrintWriter listWriter) throws XMLStreamException, IOException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.valueOf(true));
        XMLEventReader xmlreader = inputFactory.createXMLEventReader(systemId, is);
        List<Namespace> ns = null;
        while (xmlreader.hasNext()) {
            XMLEvent e = xmlreader.nextEvent();
            if (e.getEventType() != XMLStreamConstants.START_ELEMENT) {
                if (mNoDTD && e.getEventType() == XMLStreamConstants.DTD) continue;
                if (mNoPI && e.getEventType() == XMLStreamConstants.PROCESSING_INSTRUCTION) continue;
                mHeader.add(e);
                continue;
            }
            if (mNoRoot) {
                StartElement se = e.asStartElement();
                Iterator<?> nsi = se.getNamespaces();
                ns = new ArrayList<Namespace>();
                while (nsi.hasNext()) ns.add((Namespace) nsi.next());
            }
            if (!mNoRoot) {
                if (mRootNode == null) mHeader.add(e); else mHeader.add(mRootNode);
            }
            break;
        }
        while (xmlreader.hasNext()) {
            XMLEvent e = xmlreader.nextEvent();
            int type = e.getEventType();
            if (type == XMLStreamConstants.START_ELEMENT) {
                write(xmlreader, e, ns, listWriter);
            } else if (type == XMLStreamConstants.END_ELEMENT || type == XMLStreamConstants.END_DOCUMENT) {
            } else {
                if (type == XMLStreamConstants.CHARACTERS && e.asCharacters().isWhiteSpace()) continue;
                printErr("Skipping XML node: " + e.toString());
            }
        }
        xmlreader.close();
    }

    private void write(XMLEventReader xmlreader, XMLEvent first, List<Namespace> ns, PrintWriter listWriter) throws XMLStreamException, IOException {
        File fout = nextFile();
        OutputStream fo = new FileOutputStream(fout);
        XMLEventWriter w = mOutputFactory.createXMLEventWriter(fo);
        for (XMLEvent e : mHeader) w.add(e);
        w.add(first);
        if (ns != null) for (Namespace n : ns) w.add(n);
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
        w.add(mEventFactory.createEndDocument());
        w.flush();
        w.close();
        fo.close();
        if (listWriter != null) {
            listWriter.println(fout.getName());
            listWriter.flush();
        }
    }

    private File nextFile() throws IOException {
        File f = getEnv().getShell().getFile(mOutputDir, mPrefix + mSeq++ + mSuffix + mExt);
        return f;
    }
}
