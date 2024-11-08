package fr.jfhelie.wiki.renderer;

import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import org.wikimodel.common.IWikiUri;
import org.wikimodel.common.WikiParserException;
import org.wikimodel.format.IWikiWriter;
import org.wikimodel.format.WikiWriter;
import org.wikimodel.format.html.WikiDocumentHtmlFormatter;
import org.wikimodel.format.html.WikiInlineHtmlFormatter;
import org.wikimodel.format.source.WikiParser;
import org.wikimodel.parser.IWikiParser;
import org.wikimodel.wem.IWikiDocumentListener;
import org.wikimodel.wem.IWikiInlineListener;
import fr.jfhelie.wiki.exception.WikiException;

/**
 * Format wiki content into HTML content. Use Wiki Model
 * (http://wikimodel.sourceforge.net) api to parse and format wiki content.
 * 
 * @author jeff
 * 
 */
public class HtmlRenderer implements IRenderer {

    private String imgLink = "attach?src=";

    private String hrefLink = "?";

    public HtmlRenderer() {
    }

    public HtmlRenderer(String imgLink, String hrefLink) {
        this.imgLink = imgLink;
        this.hrefLink = hrefLink;
    }

    public void format(Reader reader, Writer writer) {
        try {
            format(reader, writer, false);
        } catch (WikiParserException e) {
            throw new WikiException("Html formating failed", e);
        }
    }

    public void format(String content, Writer writer) {
        try {
            format(new StringReader(content), writer, false);
        } catch (WikiParserException e) {
            throw new WikiException("Html formating failed", e);
        }
    }

    /**
	 * Reads the source page from the given reader and writes out the html page
	 * into the writer.
	 * 
	 * @param reader
	 *            the input stream with the source wiki page
	 * @param writer
	 *            the output stream where the html page will be written
	 * @param formatDocInfo
	 *            if this flag is <code>true</code> then the information about
	 *            all sections/subdocumens will be added in the resulting html
	 *            document.
	 * @throws WikiParserException
	 */
    protected void format(Reader reader, Writer writer, final boolean formatDocInfo) throws WikiParserException {
        IWikiUri docUri = null;
        IWikiWriter wikiWriter = new WikiWriter(writer);
        IWikiInlineListener inlineListener = new WikiInlineHtmlFormatter(wikiWriter) {

            public void onReference(IWikiUri uri) {
                HtmlRenderer.this.formatLink(fWriter, uri);
            }
        };
        IWikiDocumentListener documentListener = new WikiDocumentHtmlFormatter(wikiWriter) {

            public void beginDocument(IWikiUri uri) {
                if (formatDocInfo) super.beginDocument(uri);
            }

            public void beginSection(IWikiUri uri) {
                if (formatDocInfo) super.beginSection(uri);
            }

            public void endDocument(IWikiUri uri) {
                if (formatDocInfo) super.endDocument(uri);
            }

            public void endSection(IWikiUri uri) {
                if (formatDocInfo) super.endDocument(uri);
            }

            protected void formatLink(IWikiUri uri) {
                HtmlRenderer.this.formatLink(fWriter, uri);
            }
        };
        IWikiParser parser = new WikiParser();
        parser.parse(reader, docUri, inlineListener, documentListener);
    }

    /**
	 * Formats the given reference and writes out the result in the given output
	 * stream. This method can be overloaded in subclasses to define a specific
	 * formatting for different references.
	 * 
	 * @param writer
	 *            the output stream used to write the formatted reference
	 * @param reference
	 *            the reference to format
	 */
    protected void formatLink(IWikiWriter writer, IWikiUri reference) {
        String link = reference.getEscapedLink();
        String label = reference.getEscapedLabel();
        if (link.endsWith(".jpg") || link.endsWith(".gif") || link.endsWith(".png")) {
            String imgSrc = "";
            if (link.startsWith("http")) imgSrc = reference.getHttpEncodedLink(); else imgSrc = imgLink + reference.getHttpEncodedLink();
            if (label == null || "".equals(label)) writer.print("<img src='" + imgSrc + "'/>"); else writer.print("<img src='" + imgSrc + "' alt='" + label + "'/>");
        } else {
            String encodedLink = "";
            if (label == null || "".equals(label)) label = link;
            if (link.startsWith("http")) encodedLink = reference.getHttpEncodedLink(); else encodedLink = hrefLink + reference.getHttpEncodedLink();
            writer.print("<a href='" + encodedLink + "'>" + label + "</a>");
        }
    }

    public String getHrefLink() {
        return hrefLink;
    }

    public void setHrefLink(String hrefLink) {
        this.hrefLink = hrefLink;
    }

    public String getImgLink() {
        return imgLink;
    }

    public void setImgLink(String imgLink) {
        this.imgLink = imgLink;
    }
}
