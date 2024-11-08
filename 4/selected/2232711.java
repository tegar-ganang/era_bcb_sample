package it.aton.proj.dem.commons.util.xml;

import java.util.Stack;

/**
 * Makes writing XML much much easier.
 */
public class XMLBuilder {

    private StringBuilder buffer;

    private Stack<String> stack;

    private StringBuilder attrs;

    private boolean empty;

    private boolean closed;

    private String openTagInTheSameLine;

    private boolean isImplicitRoot = false;

    public XMLBuilder() {
        this.buffer = new StringBuilder();
        this.closed = true;
        this.stack = new Stack<String>();
    }

    public XMLBuilder(String root) {
        this();
        writeElement(root);
        isImplicitRoot = true;
    }

    /**
	 * Begin to output an element.
	 * 
	 * @param String
	 *            name of element.
	 */
    public XMLBuilder writeElement(String name) {
        closeOpeningTag(true);
        this.closed = false;
        writeIndent(stack.size());
        this.buffer.append('<');
        this.buffer.append(name);
        stack.add(name);
        this.openTagInTheSameLine = name;
        this.empty = true;
        return this;
    }

    private void writeIndent(int num) {
        for (int i = 0; i < num; i++) this.buffer.append("  ");
    }

    /**
	 * Output body text. Any xml characters are escaped.
	 */
    public XMLBuilder writeText(String text) {
        closeOpeningTag(false);
        this.empty = false;
        this.buffer.append(XMLUtils.XMLEscape(text));
        return this;
    }

    private void closeOpeningTag(boolean writeReturn) {
        if (!this.closed) {
            writeAttributes();
            this.closed = true;
            this.buffer.append('>');
            if (writeReturn) nextLine();
        }
    }

    private void nextLine() {
        this.buffer.append("\r\n");
        this.openTagInTheSameLine = null;
    }

    private void writeAttributes() {
        if (this.attrs != null) {
            this.buffer.append(this.attrs.toString());
            this.attrs.setLength(0);
            this.empty = false;
        }
    }

    /**
	 * Write an attribute out for the current entity. Any xml characters in the
	 * value are escaped. Currently it does not actually throw the exception,
	 * but the api is set that way for future changes.
	 * 
	 * @param String
	 *            name of attribute.
	 * @param String
	 *            value of attribute.
	 */
    public XMLBuilder writeAttribute(String attr, String value) {
        if (this.attrs == null) {
            this.attrs = new StringBuilder();
        }
        this.attrs.append(" ");
        this.attrs.append(attr);
        this.attrs.append("=\"");
        this.attrs.append(XMLUtils.XMLEscape(value));
        this.attrs.append("\"");
        return this;
    }

    /**
	 * End the current element. This will throw an exception if it is called
	 * when there is not a currently open element.
	 */
    public XMLBuilder endElement() {
        if (this.stack.empty()) return this;
        String name = (String) this.stack.pop();
        if (name != null) {
            if (this.empty) {
                writeAttributes();
                this.buffer.append("/>");
            } else {
                if (!name.equals(openTagInTheSameLine)) writeIndent(stack.size());
                this.buffer.append("</");
                this.buffer.append(name);
                this.buffer.append('>');
            }
            this.empty = false;
            this.closed = true;
            nextLine();
        }
        return this;
    }

    public String toString() {
        if (this.isImplicitRoot) endElement();
        if (!this.stack.empty()) {
            throw new IllegalStateException("Tags are not all closed. " + "Possibly, " + this.stack.pop() + " is unclosed. ");
        }
        return this.buffer.toString();
    }

    /**
	 * Write xml, without escaping characters
	 */
    public XMLBuilder writeXML(String xml) {
        closeOpeningTag(true);
        this.empty = false;
        writeIndent(stack.size());
        this.buffer.append(xml);
        nextLine();
        return this;
    }

    /**
	 * Write xml, without escaping characters, identing each row
	 */
    public XMLBuilder writeXMLIndented(String xml) {
        closeOpeningTag(true);
        this.empty = false;
        String[] sXml = xml.split("[\\n]");
        for (int i = 0; i < sXml.length; i++) {
            writeIndent(stack.size());
            this.buffer.append(sXml[i] + "\n");
        }
        return this;
    }
}
