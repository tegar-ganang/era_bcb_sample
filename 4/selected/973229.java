package org.peaseplate.domain.locator;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Locale;
import org.peaseplate.TemplateLocatorException;
import org.peaseplate.Template;
import org.peaseplate.TemplateEngine;
import org.peaseplate.TemplateException;
import org.peaseplate.TemplateLocator;
import org.peaseplate.TemplateResolverException;
import org.peaseplate.domain.parser.TemplateParser;
import org.peaseplate.locator.AbstractURLBasedLocator;

/**
 * A template locator that is based on an URL
 * 
 * @author Manfred HANTSCHEL
 */
public class URLBasedTemplateLocator extends AbstractURLBasedLocator implements TemplateLocator {

    public URLBasedTemplateLocator(URL url, String name, Locale locale, String encoding) {
        super(url, name, locale, encoding);
    }

    /**
	 * @see org.peaseplate.TemplateLocator#load(org.peaseplate.TemplateEngine)
	 */
    public Template load(TemplateEngine engine) throws TemplateException {
        return new TemplateParser(engine, this).parse();
    }

    /**
	 * @see org.peaseplate.TemplateLocator#loadSource(org.peaseplate.TemplateEngine)
	 */
    public char[] loadSource(TemplateEngine engine) throws TemplateException {
        char[] source = null;
        try {
            CharArrayWriter writer = new CharArrayWriter();
            try {
                Reader reader = new InputStreamReader(getUrl().openStream(), getKey().getEncoding());
                try {
                    int length = 0;
                    char[] buffer = new char[4096];
                    while ((length = reader.read(buffer)) >= 0) writer.write(buffer, 0, length);
                } finally {
                    reader.close();
                }
                source = writer.toCharArray();
                updateMetadata();
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            throw new TemplateLocatorException("Could not load template from \"" + this + "\"");
        }
        return source;
    }

    /**
	 * @see org.peaseplate.TemplateLocator#resolve(org.peaseplate.TemplateEngine, java.lang.String, java.util.Locale, java.lang.String)
	 */
    public Template resolve(TemplateEngine engine, String name, Locale locale, String encoding) throws TemplateException {
        Template template = null;
        try {
            try {
                String ownName = getKey().getName();
                int index = ownName.lastIndexOf('/');
                String relativeName = ((index >= 0) ? ownName.substring(0, index + 1) : "") + name;
                template = engine.getTemplate(relativeName, locale, encoding);
            } finally {
                if (template == null) template = engine.getTemplate(name, locale, encoding);
            }
        } catch (TemplateException e) {
            throw new TemplateResolverException("Could ont load template \"" + name + "\"", e);
        }
        return template;
    }

    /**
     * @see org.peaseplate.TemplateLocator#highlight(int, int)
     */
    public String highlight(int line, int column) {
        String result = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getUrl().openStream(), getKey().getEncoding()));
            try {
                result = HighlightUtils.highlight(reader, line, column);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
        }
        return result;
    }
}
