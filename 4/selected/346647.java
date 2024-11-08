package it.newinstance.jrainbow.ui.web;

import it.newinstance.jrainbow.converter.Converter;
import it.newinstance.jrainbow.converter.HTMLConverter;
import it.newinstance.jrainbow.converter.UnsupportedConverterException;
import it.newinstance.jrainbow.parser.Parser;
import it.newinstance.jrainbow.parser.UnsupportedParserException;
import it.newinstance.jrainbow.source.TaggedSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Luigi R. Viggiano
 * @version $Id: JRainbowServlet.java 163 2008-04-17 10:38:04Z luigi.viggiano $
 */
public class JRainbowServlet extends HttpServlet {

    private static final long serialVersionUID = -8807911162643391402L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        long before = System.currentTimeMillis();
        res.setContentType("text/html");
        String style = safeGetParameter(req, "style");
        String source = safeGetParameter(req, "source");
        String numbers = safeGetParameter(req, "numbers");
        String embedCss = safeGetParameter(req, "embed-css");
        String language = safeGetParameter(req, "language");
        Writer out = new OutputStreamWriter(res.getOutputStream());
        long elapsed = 0;
        header(language, style, embedCss, out);
        out.write("<pre class=\"" + language + "\">");
        Parser parser = null;
        HTMLConverter converter = null;
        try {
            converter = (HTMLConverter) Converter.Factory.newInstance(language, "html");
            parser = Parser.Factory.newInstance(language);
            if ("on".equals(numbers)) {
                StringWriter writer = new StringWriter();
                converter.setWriter(writer);
                elapsed = perform(parser, converter, source);
                number(writer.toString(), out);
            } else {
                converter.setWriter(out);
                elapsed = perform(parser, converter, source);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        out.write("</pre>");
        footer(parser, converter, before, elapsed, out);
        out.flush();
    }

    private void number(String source, Writer out) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(source));
        int lines = 0;
        while ((reader.readLine()) != null) lines++;
        int length = String.valueOf(lines).length();
        int currentLine = 0;
        reader = new BufferedReader(new StringReader(source));
        String line;
        while ((line = reader.readLine()) != null) {
            out.write("<span class=\"line_number\">");
            out.write(fixLength(++currentLine, length));
            out.write("</span>");
            out.write(line + "\n");
        }
    }

    private String fixLength(int i, int length) {
        String value = String.valueOf(i);
        while (value.length() < length) value = ' ' + value;
        return value;
    }

    private long perform(Parser parser, Converter converter, String source) throws IOException, UnsupportedParserException, UnsupportedConverterException {
        TaggedSource taggedSource = parser.parse(source);
        converter.convert(taggedSource);
        return taggedSource.getElapsed();
    }

    private void header(String language, String style, String embedCss, Writer out) throws IOException {
        out.write("<html><head>");
        String cssResourceName = "css/languages/" + language + "/" + style;
        if ("on".equals(embedCss)) {
            out.write("<style type=\"text/css\" media=\"screen\">");
            InputStream is = getServletContext().getResourceAsStream("/" + cssResourceName);
            int ch;
            while ((ch = is.read()) != -1) out.write(ch);
            out.write("</style>");
        } else {
            out.write("<link rel=\"stylesheet\" href=\"" + cssResourceName + "\" type=\"text/css\" />");
        }
        out.write("</head><body>");
        cutHere(out);
    }

    private void footer(Parser parser, HTMLConverter converter, long before, long elapsed, Writer out) throws IOException {
        cutHere(out);
        out.write("<!-- ");
        out.write("\n   parser class: " + parser.getClass().getName());
        out.write("\nconverter class: " + converter.getClass().getName());
        out.write("\n     parse time: " + elapsed + "ms.");
        out.write("\n     total time: " + (System.currentTimeMillis() - before) + "ms.");
        out.write("\n--></body></html>");
    }

    private void cutHere(Writer out) throws IOException {
        out.write("\n\n<!-- cut here -->\n\n");
    }

    private String safeGetParameter(HttpServletRequest req, String name) {
        return String.valueOf(req.getParameter(name));
    }
}
