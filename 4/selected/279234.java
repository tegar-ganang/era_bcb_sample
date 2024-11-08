package org.xmlsh.commands.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import net.sf.saxon.s9api.SaxonApiException;
import org.xmlsh.core.CoreException;
import org.xmlsh.core.InvalidArgumentException;
import org.xmlsh.core.Options;
import org.xmlsh.core.UnexpectedException;
import org.xmlsh.core.XCommand;
import org.xmlsh.core.XValue;
import org.xmlsh.sh.shell.SerializeOpts;
import org.xmlsh.util.StringPair;

public class xproperties extends XCommand {

    public int run(List<XValue> args) throws Exception {
        Options opts = new Options("in:,inxml:,text,xml,d=delete:+,v=var:+,a=add:+,c=comment:", SerializeOpts.getOptionDefs());
        opts.parse(args);
        SerializeOpts serializeOpts = getSerializeOpts(opts);
        XValue optIn = opts.getOptValue("in");
        XValue optInXml = opts.getOptValue("inxml");
        boolean bOutText = opts.hasOpt("text");
        if (optIn != null && optInXml != null) {
            usage("Only one of -in and -inxml allowed");
            return -1;
        }
        String comment = opts.getOptString("c", null);
        Properties props = new Properties();
        if (optInXml != null) props.loadFromXML(getInput(optInXml).asInputStream(serializeOpts)); else if (optIn != null) props.load(getInput(optIn).asInputStream(serializeOpts));
        if (opts.hasOpt("d")) for (XValue d : opts.getOpt("d").getValues()) props.remove(d.toString());
        List<String> printVars = null;
        if (opts.hasOpt("v")) {
            printVars = new ArrayList<String>();
            for (XValue var : opts.getOpt("v").getValues()) printVars.add(var.toString());
        }
        if (opts.hasOpt("a")) {
            for (XValue add : opts.getOpt("a").getValues()) {
                StringPair pair = new StringPair(add.toString(), '=');
                props.setProperty(pair.getLeft(), pair.getRight());
            }
        }
        if (printVars != null) writeVars(props, printVars, serializeOpts); else if (!bOutText) writeXML(props, comment); else writeText(props, comment, serializeOpts);
        return 0;
    }

    private void writeVars(Properties props, List<String> vars, SerializeOpts serializeOpts) throws UnsupportedEncodingException, IOException, InvalidArgumentException {
        PrintWriter out = getStdout().asPrintWriter(serializeOpts);
        for (String var : vars) out.println(props.getProperty(var, ""));
        out.flush();
    }

    private void writeText(Properties props, String comment, SerializeOpts serializeOpts) throws IOException {
        props.store(getEnv().getStdout().asOutputStream(serializeOpts), comment);
    }

    private void writeXML(Properties props, String comment) throws IOException, CoreException, SaxonApiException, XMLStreamException {
        SerializeOpts serializeOpts = getSerializeOpts();
        ByteArrayOutputStream oss = new ByteArrayOutputStream();
        props.storeToXML(oss, comment, serializeOpts.getOutputXmlEncoding());
        ByteArrayInputStream iss = new ByteArrayInputStream(oss.toByteArray());
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.valueOf(false));
        XMLEventReader reader = factory.createXMLEventReader(null, iss);
        XMLEventWriter writer = getStdout().asXMLEventWriter(serializeOpts);
        writer.add(reader);
        reader.close();
        writer.close();
    }
}
