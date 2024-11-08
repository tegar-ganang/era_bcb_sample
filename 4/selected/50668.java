package org.hoydaa.codesnippet.core;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import javax.xml.transform.TransformerException;
import org.hoydaa.codesnippet.core.filter.doc.DocumentFilterFactory;
import org.hoydaa.codesnippet.core.filter.docfrag.DocumentFragmentFilterFactory;
import org.hoydaa.codesnippet.core.filter.token.TokenFilterFactory;

/**
 * 
 * @author Utku Utkan
 */
public class CodeSnippetGenerator {

    private Configuration config;

    public CodeSnippetGenerator() {
    }

    public CodeSnippetGenerator(Configuration config) {
        init(config);
    }

    public void init(Configuration config) {
        this.config = config;
    }

    public void generate(Reader reader, Writer writer) throws TransformerException {
        if (config == null) {
            throw new IllegalStateException("CodeSnippetGenerator has not been initialized.");
        }
        HTMLGenerator htmlGenerator = createHTMLGenerator(config, reader);
        htmlGenerator.write(writer);
    }

    private HTMLGenerator createHTMLGenerator(Configuration config, Reader reader) {
        if (config.getTokenManager() == null) {
            throw new IllegalArgumentException(TokenManager.class + " property is mandatory.");
        }
        TokenManager tokenManager = null;
        try {
            Class _class = Class.forName(config.getTokenManager());
            Constructor constructor = _class.getConstructor(JavaCharStream.class);
            tokenManager = (TokenManager) constructor.newInstance(new JavaCharStream(reader));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        TokenKindGenerator tokenKindGenerator = new TokenKindGenerator(tokenManager.getClass());
        HTMLGenerator htmlGenerator = new HTMLGenerator(tokenManager);
        addTokenFilters(htmlGenerator, config, tokenKindGenerator);
        addDocumentFragmentFilters(htmlGenerator, config, tokenKindGenerator);
        addDocumentFilters(htmlGenerator, config, tokenKindGenerator);
        return htmlGenerator;
    }

    private void addTokenFilters(HTMLGenerator htmlGenerator, Configuration config, TokenKindGenerator tokenKindGenerator) {
        if (config.getTokenFilters() == null) {
            return;
        }
        for (String filter : config.getTokenFilters()) {
            if (config.getFilterFactory(filter) == null) {
                throw new IllegalArgumentException(filter + ".factory property is mandatory.");
            }
            TokenFilterFactory filterFactory = null;
            try {
                filterFactory = (TokenFilterFactory) Class.forName(config.getFilterFactory(filter)).newInstance();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
            filterFactory.setFilterName(filter);
            filterFactory.setTokenKindGenerator(tokenKindGenerator);
            htmlGenerator.addTokenFilter(filterFactory.createFilter(config.getFilterProperties(filter)));
        }
    }

    private void addDocumentFragmentFilters(HTMLGenerator htmlGenerator, Configuration config, TokenKindGenerator tokenKindGenerator) {
        if (config.getDocumentFragmentFilters() == null) {
            return;
        }
        for (String filter : config.getDocumentFragmentFilters()) {
            if (config.getFilterFactory(filter) == null) {
                throw new IllegalArgumentException(filter + ".factory property is mandatory.");
            }
            DocumentFragmentFilterFactory filterFactory = null;
            try {
                filterFactory = (DocumentFragmentFilterFactory) Class.forName(config.getFilterFactory(filter)).newInstance();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
            filterFactory.setFilterName(filter);
            filterFactory.setTokenKindGenerator(tokenKindGenerator);
            htmlGenerator.addDocumentFragmentFilter(filterFactory.createFilter(config.getFilterProperties(filter)));
        }
    }

    private void addDocumentFilters(HTMLGenerator htmlGenerator, Configuration config, TokenKindGenerator tokenKindGenerator) {
        if (config.getDocumentFilters() == null) {
            return;
        }
        for (String filter : config.getDocumentFilters()) {
            if (config.getFilterFactory(filter) == null) {
                throw new IllegalArgumentException(filter + ".factory property is mandatory.");
            }
            DocumentFilterFactory filterFactory = null;
            try {
                filterFactory = (DocumentFilterFactory) Class.forName(config.getFilterFactory(filter)).newInstance();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
            filterFactory.setFilterName(filter);
            filterFactory.setTokenKindGenerator(tokenKindGenerator);
            htmlGenerator.addDocumentFilter(filterFactory.createFilter(config.getFilterProperties(filter)));
        }
    }
}
