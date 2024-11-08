package org.zodiak.document;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.Text;
import nu.xom.XPathContext;

/**
 * This class provides an interface to the content portion of an OpenDocument
 * file.
 *
 * @author Steven R. Farley
 */
public class OpenDocumentContent {

    private File file;

    private static final String TEXT_NS = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";

    private static final String TABLE_NS = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";

    /**
   * Creates a new OpenDocumentContent object.
   *
   * @see OpenDocumentFile#getContent()
   * @param file the temporary content.xml file.
   */
    OpenDocumentContent(File file) {
        this.file = file;
    }

    /**
   * Returns a list of strings that match the regular expression pattern.
   * The strings are the whole values of the matching <text:p> elements.
   * The results are case-sensitive.
   * 
   * @param rePattern the pattern to match
   * @return the list of matching text elements
   * @throws IOException
   */
    public List<String> findText(String rePattern) throws IOException {
        return findText(rePattern, false);
    }

    /**
   * Returns a list of strings that match the regular expression pattern.
   * The strings are the whole values of the matching <text:p> elements.
   * 
   * @param rePattern the pattern to match
   * @param caseInsensitive true if character case should be ignored
   * @return the list of matching text elements
   * @throws IOException
   */
    public List<String> findText(String rePattern, boolean caseInsensitive) throws IOException {
        int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
        Pattern pattern = Pattern.compile(rePattern, flags);
        List<String> matches = new ArrayList<String>();
        Reader reader = new BufferedReader(new FileReader(file));
        try {
            Builder builder = new Builder(false);
            Document document = builder.build(reader);
            XPathContext context = new XPathContext("text", TEXT_NS);
            Nodes textNodes = document.query("//text:p", context);
            int count = textNodes.size();
            for (int i = 0; i < count; i++) {
                String text = textNodes.get(i).getValue();
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    matches.add(text);
                }
            }
            return matches;
        } catch (ParsingException e) {
            throw new RuntimeException("Unable to parse content file.", e);
        } finally {
            if (reader != null) reader.close();
        }
    }

    /**
   * Replaces all text that matches the regular expression 'pattern', up to
   * 'max' times.  'values' is an array of strings that will be used as
   * replacements. If the number of matches exceeds the length of values
   * then the matching segments will be removed up to 'max' times.
   *    
   * @param rePattern
   * @param replacements
   * @param max
   * @param group
   * @return
   */
    public int replaceText(String rePattern, String[] replacements, int max, int group) throws IOException {
        Pattern pattern = Pattern.compile(rePattern);
        int replaced = 0;
        Reader reader = new BufferedReader(new FileReader(file));
        try {
            Builder builder = new Builder(false);
            Document document = builder.build(reader);
            XPathContext context = new XPathContext("text", TEXT_NS);
            Nodes nodes = document.query("//text:p", context);
            boolean done = false;
            int count = nodes.size();
            for (int i = 0; i < count; i++) {
                List<Text> textNodes = findTextNodes(nodes.get(i));
                for (Text textNode : textNodes) {
                    String value = textNode.getValue();
                    Matcher matcher = pattern.matcher(value);
                    if (matcher.find()) {
                        int start = matcher.start(group);
                        int end = matcher.end(group);
                        String replacement;
                        if (replaced < replacements.length) {
                            replacement = String.format("%s%s%s", value.substring(0, start), replacements[replaced], value.substring(end));
                        } else {
                            replacement = String.format("%s%s", value.substring(0, start), value.substring(end));
                        }
                        textNode.setValue(replacement);
                        replaced += 1;
                        if (max > 0 && replaced == max) {
                            done = true;
                            break;
                        }
                    }
                }
                if (done) break;
            }
            reader.close();
            reader = null;
            save(document);
            return replaced;
        } catch (ParsingException e) {
            throw new RuntimeException("Unable to parse content file.", e);
        } finally {
            if (reader != null) reader.close();
        }
    }

    private void save(Document document) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        try {
            Serializer serializer = new Serializer(out);
            serializer.write(document);
            serializer.flush();
        } finally {
            if (out != null) out.close();
        }
    }

    private static List<Text> findTextNodes(Node node) {
        List<Text> textNodes = new ArrayList<Text>();
        Text text = null;
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            Node child = node.getChild(i);
            if (child instanceof Text) {
                textNodes.add((Text) child);
            } else {
                textNodes.addAll(findTextNodes(child));
            }
            if (text != null) {
                break;
            }
        }
        return textNodes;
    }

    private static void copy(Reader reader, Writer writer) throws IOException {
        int c;
        while ((c = reader.read()) != -1) writer.write(c);
    }
}
