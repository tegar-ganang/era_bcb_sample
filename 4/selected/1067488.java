package textlayout.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import textlayout.Element;
import textlayout.io.html.ElementReader;
import textlayout.io.html.Style;
import textlayout.io.html.style.DefaultStyle;

/**
 * Convenience methods to transform HTML to formatted plain text.
 */
public class Html2Text {

    private Style style;

    /**
	 * Constructor using a {@link DefaultStyle}.
	 */
    public Html2Text() {
        this(new DefaultStyle());
    }

    /**
	 * Constructor using the given style.
	 * 
	 * @param style
	 *            style
	 */
    public Html2Text(Style style) {
        this.style = style;
    }

    /**
	 * Transform the given HTML string into formatted plain text.
	 * 
	 * @param html
	 *            HTML string
	 * @return transformed text
	 * @throws IOException
	 */
    public String transform(String html) throws IOException {
        StringWriter writer = new StringWriter();
        transform(new StringReader(html), writer);
        return writer.toString();
    }

    /**
	 * Transform the HTML content of the given input stream.
	 * 
	 * @param inputStream
	 *            stream to read HTML from
	 * @param outputStream
	 *            stream to write text to
	 * @throws IOException 
	 */
    public void transform(InputStream inputStream, OutputStream outputStream) throws IOException {
        transform(new ElementReader(inputStream), new OutputStreamWriter(outputStream));
    }

    /**
	 * Transform the HTML content of the given input stream.
	 * 
	 * @param inputStream
	 *            stream to read HTML from
	 * @param outputStream
	 *            stream to write text to
	 * @param encoding
	 *            encoding of input stream content
	 * @throws IOException 
	 */
    public void transform(InputStream inputStream, OutputStream outputStream, String encoding) throws IOException {
        transform(new ElementReader(inputStream), new OutputStreamWriter(outputStream, encoding));
    }

    /**
	 * Transform the HTML content of the given reader.
	 * 
	 * @param reader
	 *            reader to read HTML from
	 * @param writer
	 *            writer to write text to
	 * @throws IOException
	 */
    public void transform(Reader reader, Writer writer) throws IOException {
        transform(new ElementReader(reader), writer);
    }

    private void transform(ElementReader reader, Writer writer) throws IOException {
        reader.setStyle(style);
        Element element = reader.read();
        new ElementWriter(writer).write(element);
    }
}
