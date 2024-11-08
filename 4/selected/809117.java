package com.armatiek.infofuze.xslt.functions.base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import net.iharder.Base64;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ArrayIterator;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.xpath.XPathEvaluator;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import com.armatiek.infofuze.config.Definitions;
import com.armatiek.infofuze.utils.XmlUtils;
import com.armatiek.infofuze.xslt.functions.ExtensionFunctionCall;

/**
 * 
 * 
 * @author Maarten Kroon
 */
public class Base64Decode extends ExtensionFunctionDefinition {

    private static final long serialVersionUID = 1L;

    private static final StructuredQName qName = new StructuredQName("", Definitions.INFOFUZE_NAMESPACE, "base64-decode");

    public StructuredQName getFunctionQName() {
        return qName;
    }

    public int getMinimumNumberOfArguments() {
        return 1;
    }

    public int getMaximumNumberOfArguments() {
        return 2;
    }

    public SequenceType[] getArgumentTypes() {
        return new SequenceType[] { SequenceType.SINGLE_STRING, SequenceType.OPTIONAL_BOOLEAN };
    }

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.OPTIONAL_NODE;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new Base64DecodeCall();
    }

    private static class Base64DecodeCall extends ExtensionFunctionCall {

        private static final long serialVersionUID = 1L;

        public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
            try {
                String encodedString = ((StringValue) arguments[0].next()).getStringValue();
                byte[] decodedBytes = Base64.decode(encodedString);
                if (arguments.length > 1 && ((BooleanValue) arguments[1].next()).getBooleanValue()) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(decodedBytes);
                    GZIPInputStream zis = new GZIPInputStream(bis);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IOUtils.copy(zis, baos);
                    decodedBytes = baos.toByteArray();
                }
                Document doc = XmlUtils.stringToDocument(new String(decodedBytes, "UTF-8"));
                Source source = new DOMSource(doc.getDocumentElement());
                XPathEvaluator evaluator = new XPathEvaluator(context.getConfiguration());
                NodeInfo[] infos = new NodeInfo[] { evaluator.setSource(source) };
                return new ArrayIterator(infos);
            } catch (Exception e) {
                throw new XPathException("Could not base64 decode string", e);
            }
        }
    }
}
