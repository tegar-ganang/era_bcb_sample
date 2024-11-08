package org.jmule.util;

import java.io.*;
import java.util.Stack;

/**
 * 
 *
 * this class provides simple creating of xml files out of Key/Value pairs.
 * Synopsis:<p>
 * xw = new XMLWriter(&quot;preferences.xml&quot;);<br>
 * xw.startDocument(&quot;&lt;?xml version=\&quot;1.0\&quot; encoding=\&quot;UTF-8\&quot;?&gt;&quot;);<br>
 * xw.startElement(&quot;preferences&quot;);<br>
 * xw.addField(&quot;usernick&quot;, &quot;KurtCobain&quot;);<br>
 * xw.endElement();<br>
 * xw.endDocument();<br>
 * <p>
 * ==
 * <pre>
 *  &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
 *  &lt;preferences&gt;
 *      &lt;userNick&gt; KurtCobain &lt;/userNick&gt;
 *  &lt;/preferences&gt;
 * </pre>
 * @author casper
 * @version $Revision: 1.1.1.1 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/04/22 21:46:01 $
 */
public class XMLWriter {

    public static final String ENCODING = "UTF-8";

    /**
	 * 
	 * @param fileName
	 */
    public XMLWriter(String fileName) {
        this.file = new File(fileName);
    }

    /**
	 * 
	 * @param file
	 */
    public XMLWriter(File file) {
        this.file = file;
    }

    /** starts a new XML document with a default header */
    public void startDocument() throws IOException {
        startDocument("<?xml version=\"1.0\" encoding=\"" + ENCODING + "\"?>");
    }

    /** starts a new XML document
	 * @param header the String to be used as header for the file, e.g. 
	 * &quot;&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&quot;
	 * */
    private void startDocument(String header) throws IOException {
        fileStream = new FileOutputStream(file);
        outStream = new BufferedOutputStream(fileStream);
        elementStack = new Stack();
        documentStarted = true;
        outStream.write((header + "\n").getBytes("UTF-8"));
    }

    /** starts a new tag
	 * startElement("test"); will result in &lt;test&gt;
	 * element is pushed onto a stack. Use endElement() to close in right order
	 * @param element the tag to start.
	 */
    public void startElement(String element) throws IOException {
        if (!documentStarted) {
            return;
        }
        for (int i = 1; i <= elementStack.size(); i++) {
            write("\t");
        }
        elementStack.push(element);
        writeln("<" + element + ">");
    }

    /** Adds <tag> value </tag>
	 */
    public void addField(String tag, String value) throws IOException {
        if (!documentStarted) {
            return;
        }
        for (int i = 1; i <= elementStack.size(); i++) {
            write("\t");
        }
        writeln("<" + tag + "> " + value + " </" + tag + ">");
    }

    /** Adds <tag />
	 */
    public void addTag(String tag) throws IOException {
        if (!documentStarted) {
            return;
        }
        for (int i = 1; i <= elementStack.size(); i++) {
            write("\t");
        }
        writeln("<" + tag + " />");
    }

    /**
	 * ends the last element started with startElement()
	 */
    public void endElement() throws IOException {
        if (!documentStarted) {
            return;
        }
        for (int i = 1; i < elementStack.size(); i++) {
            write("\t");
        }
        String lastElement = (String) elementStack.pop();
        writeln("</" + lastElement + ">");
    }

    /**
	 * ends this document and closes the file.
	 */
    public void endDocument() throws IOException {
        if (!documentStarted) {
            return;
        }
        fileStream.getChannel().force(true);
        outStream.close();
    }

    protected BufferedOutputStream outStream;

    protected FileOutputStream fileStream;

    protected File file;

    protected boolean documentStarted = false;

    protected Stack elementStack;

    /**
	 * Returns documentStarted.
	 * @return boolean
	 */
    public boolean isDocumentStarted() {
        return documentStarted;
    }

    /**
	 * Sets documentStarted.
	 * @param documentStarted The documentStarted to set
	 */
    public void setDocumentStarted(boolean documentStarted) {
        this.documentStarted = documentStarted;
    }

    /**
 	* writeln, appends a line to the outBuffer
    * should only be called internally
    * Attention: doesn't check whether outStream exists!
 	* @param outLine the output line as string
 	*/
    private void writeln(String outLine) throws IOException {
        write(outLine);
        write("\n");
    }

    /**
    * write, appends a string to the outBuffer should only be called internally
    * Attention: doesn't check whether outStream exists!
    * @param output the output as string
    */
    private void write(String output) throws IOException {
        outStream.write(output.getBytes(ENCODING));
    }

    public static void main(String argv[]) {
        System.out.println("XMLWriter test class. look for XMLTest.xml.");
        XMLWriter xw = new XMLWriter("XMLTest.xml");
        try {
            xw.startDocument("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            xw.startElement("preferences");
            xw.addField("usernick", "KurtCobain");
            xw.addField("age", "2384038");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("age", "2384038");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("age", "2384038");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("age", "2384038");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("age", "2384038");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("age", "2384038");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.addField("test", "234234234324");
            xw.endElement();
            xw.endDocument();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    /**
    * Holds some escaped chars for xml.<p>
    *<table border=1 padding="1">
    *<thead><tr><th align="center">j</th><th align="center"><b>ESCAPETABLE[0][j]</b></th><th align="center"><b>ESCAPETABLE[1][j]</b></th></tr></thead>
    *<tr><td align="center">0</td><td align="center">&lt;</td><td align="center"><code>&amp;lt;</code></td></tr>
    *<tr><td align="center">1</td><td align="center">&gt;</td><td align="center"><code>&amp;gt;</code></td></tr>
    *<tr><td align="center">2</td><td align="center">&amp;</td><td align="center"><code>&amp;amp;</code></td></tr>
    *<tr><td align="center">3</td><td align="center">&quot;</td><td align="center"><code>&amp;quot;</code></td></tr>
    *<tr><td align="center">4</td><td align="center">'</td><td align="center"><code>&amp;apos;</code></td></tr>
    *</table>
    */
    public static final String[][] ESCAPETABLE = { { "<", ">", "&", "\"", "'" }, { "&lt;", "&gt;", "&amp;", "&quot;", "&apos;" } };

    /** Escapes chars contained in {@link org.jmule.util.XMLWriter#ESCAPETABLE ESCAPETABLE} and message.
    * @param message a String want to write in xml but possibly contains one or more special chars.
    * @return a String equate to message, but possibly with some chars escaped.
    */
    public static String encodeSequence(String message) {
        StringBuffer sb = new StringBuffer(message);
        int i = 0;
        while (i < sb.length()) {
            boolean escape = false;
            char ch = sb.charAt(i);
            int j = 0;
            while (j < ESCAPETABLE[0].length) {
                escape = ch == ESCAPETABLE[0][j].charAt(0);
                if (escape) {
                    break;
                } else {
                    j++;
                }
            }
            if (escape) {
                sb.insert(i, ESCAPETABLE[1][j]);
                i += ESCAPETABLE[1][j].length();
                sb.deleteCharAt(i);
            } else {
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Returns the outStream.
     * @return BufferedOutputStream
     */
    public BufferedOutputStream getOutputStream() {
        return outStream;
    }

    /**
     * Returns the encoding used by the writer.
     * @return String name of encoding
     */
    public String getEncoding() {
        return ENCODING;
    }
}
