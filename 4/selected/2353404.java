package org.hoydaa.codesnippet.util;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import javax.xml.transform.TransformerException;
import org.hoydaa.codesnippet.core.HTMLWriter;
import org.hoydaa.codesnippet.core.JavaCharStream;
import org.hoydaa.codesnippet.core.TokenManager;
import org.hoydaa.codesnippet.core.filter.doc.LineNumberFilter;
import org.hoydaa.codesnippet.core.filter.docfrag.SpanFilter;

public class SnippetGenerator {

    private SnippetConfig config;

    public SnippetGenerator(SnippetConfig config) {
        this.config = config;
    }

    public String generate(String code) {
        StringWriter stringWriter = new StringWriter();
        generate(stringWriter, new StringReader(code));
        return stringWriter.toString();
    }

    public void generate(Writer writer, Reader reader) {
        HTMLWriter htmlWriter = null;
        try {
            htmlWriter = getHTMLWriter(reader);
        } catch (Exception e) {
        }
        htmlWriter.addDocumentFragmentFilter(new SpanFilter());
        try {
            htmlWriter.write(writer);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    public HTMLWriter getHTMLWriter(Reader reader) throws Exception {
        Class clazz = Class.forName("org.hoydaa.codesnippet." + config.getLanguage().getName().toLowerCase() + "." + config.getLanguage().getName() + "ParserTokenManager");
        Constructor constructor = clazz.getConstructor(JavaCharStream.class);
        TokenManager tokenManager = (TokenManager) constructor.newInstance(new JavaCharStream(reader));
        HTMLWriter htmlWriter = new HTMLWriter(tokenManager);
        if (config.getConfig(SnippetConfig.LINE_NUMBERS) == "true") {
            LineNumberFilter lineNumberFilter = new LineNumberFilter();
            if (config.getConfig(SnippetConfig.LINE_NUMBERS_NAMED_ANCHORS) == "true") lineNumberFilter.setNamedAnchorsEnabled(true);
            if (config.getConfig(SnippetConfig.LINE_NUMBERS_LINKS) == "true") lineNumberFilter.setLineNumberLinksEnabled(true);
            if (config.getConfig(SnippetConfig.LINE_NUMBERS_PREFIX) != null) lineNumberFilter.setLineNumberPrefix(config.getConfig(SnippetConfig.LINE_NUMBERS_PREFIX));
            lineNumberFilter.setLeftTdClass("left");
            lineNumberFilter.setRightTdClass("right");
            htmlWriter.addDocumentFilter(lineNumberFilter);
        }
        return htmlWriter;
    }
}
