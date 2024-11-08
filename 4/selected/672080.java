package org.jia.examples;

import org.apache.taglibs.standard.lang.support.ExpressionEvaluatorManager;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

/**
 * <p>Tag handler for &lt;escapeHtml&gt;
 *
 * @author Pierre Delisle
 * @version $Revision: 1.3 $ $Date: 2004/02/05 16:23:47 $
 */
public class EscapeHtmlTag extends BodyTagSupport {

    private String reader;

    private String writer;

    public EscapeHtmlTag() {
        super();
        init();
    }

    private void init() {
        reader = null;
        writer = null;
    }

    /**
     * Tag's 'reader' attribute
     */
    public void setReader(String reader) {
        this.reader = reader;
    }

    /**
     * Tag's 'writer' attribute
     */
    public void setWriter(String reader) {
        this.writer = writer;
    }

    public int doEndTag() throws JspException {
        Reader in;
        Writer out;
        if (reader == null) {
            String bcs = getBodyContent().getString().trim();
            if (bcs == null || bcs.equals("")) {
                throw new JspTagException("In &lt;escapeHtml&gt;, 'reader' " + "not specified and no non-whitespace content inside the tag.");
            }
            in = castToReader(bcs);
        } else {
            in = castToReader(eval("reader", reader, Object.class));
        }
        if (writer == null) {
            out = pageContext.getOut();
        } else {
            out = castToWriter(eval("writer", writer, Object.class));
        }
        transform(in, out);
        return EVAL_PAGE;
    }

    /**
     * Releases any resources we may have (or inherit)
     */
    public void release() {
        super.release();
        init();
    }

    /**
     * Transform
     */
    public void transform(Reader reader, Writer writer) throws JspException {
        int c;
        try {
            writer.write("<pre>");
            while ((c = reader.read()) != -1) {
                if (c == '<') {
                    writer.write("&lt;");
                } else if (c == '>') {
                    writer.write("&gt;");
                } else {
                    writer.write(c);
                }
            }
            writer.write("</pre>");
        } catch (IOException ex) {
            throw new JspException("EscapeHtml: " + "error copying chars", ex);
        }
    }

    /**
     * Evaluate elexprvalue
     */
    private Object eval(String attName, String attValue, Class clazz) throws JspException {
        Object obj = ExpressionEvaluatorManager.evaluate(attName, attValue, clazz, this, pageContext);
        if (obj == null) {
            throw new JspException("escapeHtml");
        } else {
            return obj;
        }
    }

    public static Reader castToReader(Object obj) throws JspException {
        if (obj instanceof InputStream) {
            return new InputStreamReader((InputStream) obj);
        } else if (obj instanceof Reader) {
            return (Reader) obj;
        } else if (obj instanceof String) {
            return new StringReader((String) obj);
        }
        throw new JspException("Invalid type '" + obj.getClass().getName() + "' for castToReader()");
    }

    public static Writer castToWriter(Object obj) throws JspException {
        if (obj instanceof OutputStream) {
            return new OutputStreamWriter((OutputStream) obj);
        } else if (obj instanceof Writer) {
            return (Writer) obj;
        }
        throw new JspException("Invalid type '" + obj.getClass().getName() + "' for castToWriter()");
    }
}
