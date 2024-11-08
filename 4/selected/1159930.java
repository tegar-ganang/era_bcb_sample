package org.openiaml.iacleaner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.openiaml.iacleaner.inline.IACleanerStringReader;
import org.openiaml.iacleaner.inline.IACleanerStringWriter;
import org.openiaml.iacleaner.inline.InlineCleanerException;
import org.openiaml.iacleaner.inline.InlineStringReader;
import org.openiaml.iacleaner.inline.InlineStringWriter;

/**
 * <p>This cleaner implementation uses a much more efficient method of 
 * reading, parsing and writing the formatted output.</p> 
 * 
 * <p>Instead of keeping
 * the entire string in memory at once, two special buffered readers and
 * writers ({@link InlineStringReader} and {@link InlineStringWriter}) allow
 * the IACleaner to parse the string character-by-character in
 * real-time.</p>
 * 
 * <p>As a result, it is at least an order of magnitude faster than
 * {@link IARegexpCleaner}.</p>
 * 
 * @see InlineCssCleaner
 * @see InlineHtmlCleaner
 * @see InlineJavascriptCleaner
 * @see InlineHtmlCleaner
 * @author Jevon
 *
 */
public class IAInlineCleaner extends DefaultIACleaner implements IACleaner {

    private InlineHtmlCleaner html = new InlineHtmlCleaner(this);

    public InlineHtmlCleaner getHtmlCleaner() {
        return html;
    }

    private InlineXmlCleaner xml = new InlineXmlCleaner(this);

    public InlineXmlCleaner getXmlCleaner() {
        return xml;
    }

    private InlineCssCleaner css = new InlineCssCleaner(this);

    public InlineCssCleaner getCssCleaner() {
        return css;
    }

    private InlineJavascriptCleaner javascript = new InlineJavascriptCleaner(this);

    public InlineJavascriptCleaner getJavascriptCleaner() {
        return javascript;
    }

    private InlinePhpCleaner php = new InlinePhpCleaner(this);

    public InlinePhpCleaner getPhpCleaner() {
        return php;
    }

    public String cleanScript(String script, String extension) throws CleanerException {
        InlineStringReader reader = new IACleanerStringReader(script, this);
        return cleanScript(reader, extension);
    }

    public String cleanScript(InputStream stream, String extension) throws CleanerException {
        InlineStringReader reader = new IACleanerStringReader(new InputStreamReader(stream), this);
        return cleanScript(reader, extension);
    }

    private int wordWrapLength = -1;

    @Override
    public void setWordWrapLength(int chars) {
        wordWrapLength = chars;
    }

    /**
	 * Do the actual script read/write using our readers and writers.
	 * 
	 * @param reader
	 * @param writer
	 * @param extension the extension of the file; will be changed to lowercase for comparison
	 * @return the formatted script
	 * @throws CleanerException 
	 * @throws IOException 
	 */
    protected String cleanScript(InlineStringReader reader, String extension) throws CleanerException {
        InlineStringWriter writer = new IACleanerStringWriter(this);
        if (wordWrapLength != -1) {
            writer.setWordWrapLength(wordWrapLength);
        }
        extension = extension.toLowerCase();
        try {
            if (extension.equals("js")) {
                cleanHtmlJavascript(reader, writer, false);
            } else if (extension.equals("css")) {
                cleanHtmlCss(reader, writer, false);
            } else if (extension.equals("txt") || extension.equals("tex")) {
                writer.enableWordwrap(false);
                directCopy(reader, writer);
                writer.enableWordwrap(true);
            } else {
                cleanHtmlBlock(reader, writer);
            }
        } catch (IOException e) {
            throw new CleanerException(e);
        }
        {
            int c;
            try {
                while ((c = reader.read()) != -1) {
                    writer.write(c);
                }
            } catch (IOException e) {
                throw new CleanerException(e);
            }
        }
        return writer.getBuffer().toString();
    }

    /**
	 * Directly copy from the reader to the writer; this is useful for
	 * text files, for example.
	 * 
	 * @param reader
	 * @param writer
	 * @throws IOException 
	 */
    protected void directCopy(InlineStringReader reader, InlineStringWriter writer) throws IOException {
        int bufSize = 1024;
        char[] buf = new char[bufSize];
        while (true) {
            int read = reader.read(buf);
            if (read == -1) break;
            writer.write(buf, 0, read);
        }
    }

    /**
	 * Clean up HTML code.
	 * 
	 * @see InlineHtmlCleaner#cleanHtmlBlock(InlineStringReader, InlineStringWriter)
	 * @param reader
	 * @param writer
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    protected void cleanHtmlBlock(InlineStringReader reader, InlineStringWriter writer) throws IOException, CleanerException {
        getHtmlCleaner().cleanHtmlBlock(reader, writer);
    }

    /**
	 * Clean up XML code. This does not change any content, except for
	 * special tags (PHP, etc).
	 * 
	 * @see InlineXmlCleaner#cleanXmlBlock(InlineStringReader, InlineStringWriter)
	 * @param reader
	 * @param writer
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    protected void cleanXmlBlock(InlineStringReader reader, InlineStringWriter writer) throws IOException, CleanerException {
        getXmlCleaner().cleanXmlBlock(reader, writer);
    }

    /**
	 * We need to read in a PHP script and output it as appropriate.
	 * Reader starts with "&lt;?php'. 
	 * 
	 * @see InlinePhpCleaner#cleanPhpBlock(InlineStringReader, InlineStringWriter)
	 * @param reader
	 * @param writer
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    protected void cleanPhpBlock(InlineStringReader reader, InlineStringWriter writer) throws IOException, CleanerException {
        getPhpCleaner().cleanPhpBlock(reader, writer);
    }

    /**
	 * Try switching to PHP mode from the current point. 
	 * Returns true if the switch was successful, in which case
	 * the outside loop should issue 'continue;' to resume parsing.
	 * 
	 * @param reader
	 * @param writer
	 * @param cur
	 * @return
	 * @throws CleanerException 
	 * @throws IOException 
	 */
    public boolean didSwitchToPhpMode(InlineStringReader reader, InlineStringWriter writer, int cur) throws IOException, CleanerException {
        if (cur == '<' && reader.readAhead(4).equals("?php")) {
            boolean oldIndent = writer.getIndentEnabled();
            writer.enableIndent(true);
            reader.unread('<');
            cleanPhpBlock(reader, writer);
            writer.enableIndent(oldIndent);
            return true;
        }
        return false;
    }

    /**
	 * We are in HTML and we have just processed a tag
	 * '&lt;style ...&gt;'. We need to parse the inline CSS until
	 * we are about to hit '&lt;/style&gt;'.
	 * 
	 * @see InlineCssCleaner#cleanHtmlCss(InlineStringReader, InlineStringWriter, boolean)
	 * @param reader
	 * @param writer
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    protected void cleanHtmlCss(InlineStringReader reader, InlineStringWriter writer, boolean withinHtml) throws IOException, CleanerException {
        getCssCleaner().cleanHtmlCss(reader, writer, withinHtml);
    }

    /**
	 * We are in HTML and we have just processed a tag
	 * '&lt;script ...&gt;'. We need to parse the inline Javascript until
	 * we are about to hit '&lt;/script&gt;'.
	 * 
	 * @see InlineJavascriptCleaner#cleanJavascriptBlock(InlineStringReader, InlineStringWriter, boolean)
	 * @param reader
	 * @param writer
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    protected void cleanHtmlJavascript(InlineStringReader reader, InlineStringWriter writer, boolean withinHtml) throws IOException, CleanerException {
        getJavascriptCleaner().cleanJavascriptBlock(reader, writer, withinHtml);
    }

    /**
	 * Same as {@link #readAheadUntilEndHtmlTag(org.openiaml.iacleaner.IAInlineCleaner.InlineStringReader)},
	 * except this skips over a '<' in front.
	 * 
	 * @param reader
	 * @param writer
	 * @return
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    protected String readAheadUntilEndHtmlTagWithOpenBrace(InlineStringReader reader, InlineStringWriter writer) throws IOException, CleanerException {
        int oldChar = reader.getLastChar();
        int cur = reader.read();
        String nextTag = readAheadUntilEndHtmlTag(reader);
        reader.unread(cur);
        reader.setLastChar(oldChar);
        return nextTag;
    }

    /**
	 * We want to read ahead, and see what the next HTML tag is.
	 * 
	 * Read ahead until we find something outside [A-Za-z0-9_\-/!], ignoring
	 * leading whitespace.
	 * 
	 * Will only read up to 512 characters into the stream.
	 * 
	 * Return the text found, or throws an exception.
	 * 
	 * @return
	 * @throws IOException 
	 * @throws CleanerException 
	 */
    protected String readAheadUntilEndHtmlTag(InlineStringReader reader) throws IOException, CleanerException {
        int oldLast = reader.getLastChar();
        char[] buffer = new char[512];
        char[] retBuffer = new char[512];
        int i = -1;
        int j = -1;
        int cur;
        while ((cur = reader.read()) != -1) {
            i++;
            buffer[i] = (char) cur;
            if (Character.isWhitespace(cur) && j == -1) {
                continue;
            }
            j++;
            retBuffer[j] = (char) cur;
            if (!(Character.isLetterOrDigit(cur) || cur == '_' || cur == '-' || cur == '/' || cur == '!')) {
                reader.unread(buffer, 0, i + 1);
                reader.setLastChar(oldLast);
                return new String(retBuffer, 0, j);
            }
        }
        if (i > 0) {
            throw new InlineCleanerException("Could not read until end of HTML tag; never found end tag. Buffer = " + String.valueOf(buffer).substring(0, i - 1), reader);
        } else {
            throw new InlineCleanerException("Could not read until end of HTML tag; never found end tag. Buffer is empty", reader);
        }
    }

    public String cleanScript(String script) throws CleanerException {
        return cleanScript(script, "php");
    }
}
