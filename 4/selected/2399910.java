package com.google.code.configprocessor.processing.xml;

import java.io.*;
import java.util.*;
import javax.xml.namespace.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import com.google.code.configprocessor.*;
import com.google.code.configprocessor.expression.*;
import com.google.code.configprocessor.io.*;
import com.google.code.configprocessor.processing.*;
import com.google.code.configprocessor.util.*;

public class XmlActionProcessor implements ActionProcessor {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private String encoding;

    private int lineWidth;

    private int indentSize;

    private FileResolver fileResolver;

    private ExpressionResolver expressionResolver;

    private NamespaceContext namespaceContext;

    private List<ParserFeature> parserFeatures;

    private boolean failOnMissingXpath;

    public XmlActionProcessor(String encoding, int lineWidth, int indentSize, FileResolver fileResolver, ExpressionResolver expressionResolver, Map<String, String> contextMappings, List<ParserFeature> parserFeatures, boolean failOnMissingXpath) {
        this.encoding = encoding;
        this.lineWidth = lineWidth;
        this.indentSize = indentSize;
        this.fileResolver = fileResolver;
        this.expressionResolver = expressionResolver;
        this.namespaceContext = new MapBasedNamespaceContext(contextMappings);
        this.parserFeatures = parserFeatures;
        this.failOnMissingXpath = failOnMissingXpath;
    }

    public void process(Reader input, Writer output, Action action) throws ParsingException, IOException {
        try {
            Document document = XmlHelper.parse(input, parserFeatures);
            if (action != null) {
                XmlActionProcessingAdvisor advisor = getAdvisorFor(action, action);
                advisor.process(document);
            }
            XmlHelper.write(output, document, encoding, lineWidth, indentSize);
        } catch (SAXException e) {
            throw new ParsingException(e);
        } catch (ParserConfigurationException e) {
            throw new ParsingException(e);
        }
    }

    protected XmlActionProcessingAdvisor getAdvisorFor(Action rootAction, Action action) throws ParsingException, IOException {
        if (action instanceof AddAction) {
            AddAction addAction = (AddAction) action;
            String fileName = addAction.getFile();
            String fileContent = null;
            if (fileName != null) {
                fileContent = getProcessedFile(fileName, addAction.getNestedAction());
                if (fileContent == null) {
                    throw new ParsingException(String.format("Processing file \"%s\" yielded null content.", addAction.getFile()));
                }
            }
            return new XmlAddActionProcessingAdvisor((AddAction) action, fileContent, expressionResolver, namespaceContext, parserFeatures, failOnMissingXpath);
        } else if (action instanceof ModifyAction) {
            return new XmlModifyActionProcessingAdvisor((ModifyAction) action, expressionResolver, namespaceContext, parserFeatures, failOnMissingXpath);
        } else if (action instanceof RemoveAction) {
            return new XmlRemoveActionProcessingAdvisor((RemoveAction) action, expressionResolver, namespaceContext, parserFeatures, failOnMissingXpath);
        } else if (action instanceof NestedAction) {
            List<XmlActionProcessingAdvisor> advisors = new ArrayList<XmlActionProcessingAdvisor>();
            NestedAction nestedAction = (NestedAction) action;
            for (Action nested : nestedAction.getActions()) {
                advisors.add(getAdvisorFor(rootAction, nested));
            }
            return new NestedXmlActionProcessingAdvisor(advisors, nestedAction);
        }
        throw new IllegalArgumentException("Unknown action: " + action);
    }

    protected String getProcessedFile(String name, Action action) throws ParsingException, IOException {
        File file = fileResolver.resolve(name);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), encoding);
        StringWriter writer = new StringWriter();
        try {
            process(reader, writer, action);
            return writer.toString();
        } finally {
            IOUtils.close(reader, null);
        }
    }
}
