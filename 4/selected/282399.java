package net.sf.refactorit.source.html;

import net.sf.refactorit.classmodel.CompilationUnit;
import net.sf.refactorit.options.GlobalOptions;
import net.sf.refactorit.source.format.FormatSettings;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;

public class HTMLSourceEditor {

    private String linebreak = System.getProperty("line.separator", "\n");

    /** The source to transform into HTML */
    private CompilationUnit compilationUnit;

    /** Should the source be numbered? */
    private boolean numbered;

    /** The base directory for storing the output */
    private String path;

    private HTMLSourceEditor(CompilationUnit compilationUnit, String path) {
        this.compilationUnit = compilationUnit;
        setPath(path);
    }

    private void parse() throws IOException {
        HTMLLinkIndexer indexer = getHTMLEntities();
        BufferedReader reader = null;
        BufferedWriter writer = null;
        InputStream data = null;
        StringBuffer input = new StringBuffer(512);
        try {
            data = this.compilationUnit.getSource().getInputStream();
            String encoding = GlobalOptions.getEncoding();
            reader = new BufferedReader(new InputStreamReader(data, encoding));
            writer = new BufferedWriter(new FileWriter(createFileName(this.compilationUnit.getSource().getRelativePath())));
            printHeader(writer, this.compilationUnit.getSource().getRelativePath());
            printContent(indexer, reader, writer, input);
            printFooter(writer);
        } catch (Exception e) {
            System.err.println("reader: " + reader);
            System.err.println("writer: " + writer);
            System.err.println("out: " + createFileName(this.compilationUnit.getSource().getRelativePath()));
            e.printStackTrace();
            throw new RuntimeException("The HTML printer crashed unexpectedly, check the console for a stack trace.");
        } finally {
            if (data != null) {
                data.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }

    private void printContent(final HTMLLinkIndexer indexer, final BufferedReader reader, final BufferedWriter writer, StringBuffer input) throws IOException {
        for (int line = 0; (input = readLine(reader, indexer.getLine(++line), input, line)) != null; ) {
            writer.write(input.toString(), 0, input.length());
        }
    }

    private StringBuffer readLine(BufferedReader reader, ArrayList content, StringBuffer buffer, int index) throws IOException {
        buffer.setLength(0);
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        buffer.append(line);
        modifier: {
            if (content == null) {
                encodeCharacters(buffer, 0, buffer.length());
                break modifier;
            }
            int update = 0;
            for (int pos = 0, max = (content != null ? content.size() : 0), fix = 0; pos < max; pos++) {
                HTMLEntity entity = (HTMLEntity) content.get(pos);
                if (update < (entity.getNode().getColumn() + fix)) {
                    fix += encodeBuffer(buffer, update, (entity.getNode().getColumn() + fix - 1));
                }
                String oldName = entity.getNode().getText();
                String newName = entity.toString();
                int start = (entity.getNode().getColumn() + fix - 1);
                int close = (entity.getNode().getColumn() + fix - 1) + oldName.length();
                buffer.replace(start, close, newName);
                update = start + newName.length();
                fix += (newName.length() - oldName.length());
            }
            if (update < buffer.length()) {
                encodeBuffer(buffer, update, buffer.length());
            }
        }
        buffer.insert(0, "<TD class=\"src\">");
        buffer.append("</TD></TR>");
        if ((this.numbered)) {
            buffer.insert(0, "<TD WIDTH=\"1\" class=\"line\">" + index + ":</TD>");
        }
        buffer.insert(0, "<TR>");
        buffer.append(linebreak);
        fixTabs(buffer);
        return buffer;
    }

    private void fixTabs(StringBuffer aBuffer) {
        final String tabReplacement = FormatSettings.getIndentString(FormatSettings.getTabSize());
        for (int i = 0; i < aBuffer.length(); ++i) {
            if (aBuffer.charAt(i) == 9) {
                aBuffer.replace(i, i + 1, tabReplacement);
            }
        }
    }

    /** @return the number of characters this buffer's length increased */
    private int encodeBuffer(StringBuffer buffer, int start, int close) {
        int before = buffer.length();
        return encodeCharacters(buffer, start, close - start).length() - before;
    }

    private void setNumbered(boolean numbered) {
        this.numbered = numbered;
    }

    private String createFileName(String resource) {
        return getPath() + HTMLLinkIndexer.createFileName(resource);
    }

    private HTMLLinkIndexer getHTMLEntities() {
        HTMLLinkIndexer result = new HTMLLinkIndexer();
        result.visit(this.compilationUnit);
        return result;
    }

    private void printHeader(Writer writer, String title) throws IOException {
        writer.write("<HTML>");
        writer.write("<HEAD>");
        writer.write("<TITLE>" + title + "</TITLE>");
        writer.write("<LINK rel=\"stylesheet\" href=\"source.css\" type=\"text/css\" />");
        writer.write("</HEAD>");
        writer.write("<BODY>");
        writer.write("<TABLE BORDER=\"0\" WIDTH=\"100%\" CELLPADDING=\"0\" CELLSPACING=\"0\">");
        writer.write(linebreak);
    }

    private void printFooter(Writer writer) throws IOException {
        writer.write(linebreak);
        writer.write("</TABLE>");
        writer.write("</BODY>");
        writer.write("</HTML>");
    }

    private String getPath() {
        return this.path;
    }

    private void setPath(String path) {
        this.path = (path.endsWith("/") ? path : path + "/");
    }

    public void setLinebreak(String linebreak) {
        if (linebreak != null) {
            this.linebreak = linebreak;
        }
    }

    static StringBuffer encodeCharacters(StringBuffer buffer, int offset, int length) {
        for (int pos = (offset + length) - 1, min = (offset - 1), code = -1; pos > min; pos--) {
            switch(buffer.charAt(pos)) {
                case '<':
                    code = 60;
                    break;
                case '>':
                    code = 62;
                    break;
                case '&':
                    code = 38;
                    break;
                case '\"':
                    code = 34;
                    break;
                default:
                    code = -1;
                    break;
            }
            if (code != -1) {
                buffer.replace(pos, pos + 1, "&#" + String.valueOf(code) + ";");
            }
        }
        return buffer;
    }

    /**
   * @return output fileName(full path)
   * @param destination destination
   * @param current current
   * @param showLineNumbers numbers
   * @throws IOException
   */
    public static String doEditing(final String destination, final CompilationUnit current, boolean showLineNumbers) throws IOException {
        return doEditing(destination, current, showLineNumbers, null);
    }

    /**
     * @return output fileName(full path)
     * @param destination destination
     * @param current current
     * @param showLineNumbers numbers
     * @throws IOException
     */
    public static String doEditing(final String destination, final CompilationUnit current, boolean showLineNumbers, String linebreak) throws IOException {
        HTMLSourceEditor editor = new HTMLSourceEditor(current, destination);
        editor.setNumbered(showLineNumbers);
        editor.setLinebreak(linebreak);
        editor.parse();
        return editor.createFileName(current.getSource().getRelativePath());
    }
}
