package com.jujunie.service.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;

/**
 * This class writes an XML stream to the given Writer
 * @author julien
 */
public class XMLOutputStreamWriter {

    /** open tag char (&lt;) */
    private static final char OPEN_TAG = '<';

    /** close tag char (&gt;) */
    private static final char CLOSE_TAG = '>';

    /** Line separator */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /** The writer */
    private Writer writer = null;

    /** Stack of tags */
    private LinkedList<StackedElement> stack = null;

    /** Indent level */
    private int indentLevel = 0;

    /** Indentation flag */
    private boolean indentation = true;

    /**
     * Creates a new XMLOutputStreamWriter
     * @param writer the writer to write to
     */
    public XMLOutputStreamWriter(Writer writer) {
        this.writer = writer;
        this.stack = new LinkedList<StackedElement>();
    }

    /**
     * @return Returns the indentation.
     */
    public boolean isIndentation() {
        return indentation;
    }

    /**
     * @param indentation The indentation to set.
     */
    public void setIndentation(boolean indentation) {
        this.indentation = indentation;
    }

    /**
     * Open given a new element with the given name
     * @param elementName element name
     * @return this
     * @throws IOException on IO error
     */
    public XMLOutputStreamWriter openElement(String elementName) throws IOException {
        this.setHaveContentToCurrent();
        this.stack.addLast(new StackedElement(elementName));
        this.indent();
        this.writer.write(OPEN_TAG);
        this.writer.write(elementName.toCharArray());
        return this;
    }

    /**
     * Close the current element
     * @return this
     * @throws IOException on IO error
     */
    public XMLOutputStreamWriter closeElement() throws IOException {
        this.closeElement((StackedElement) this.stack.removeLast());
        return this;
    }

    /**
     * Close all remaining elements
     * @return this
     * @throws IOException on IO error
     */
    public XMLOutputStreamWriter closeAllElements() throws IOException {
        while (!this.stack.isEmpty()) {
            this.closeElement((StackedElement) this.stack.removeLast());
        }
        return this;
    }

    /**
     * Writes given text as current element content
     * @param text text to write
     * @return this
     * @throws IOException on IOError
     */
    public XMLOutputStreamWriter addCharacters(char[] value, int start, int length) throws IOException {
        this.setHaveContentToCurrent();
        this.writeContent(value, start, length);
        return this;
    }

    /**
     * Writes given text as current element content
     * @param text text to write
     * @return this
     * @throws IOException on I/O error
     */
    public XMLOutputStreamWriter addText(String text) throws IOException {
        char[] val = text.toCharArray();
        return this.addCharacters(val, 0, val.length);
    }

    /**
     * Add an attribute
     * @param attributeName attribute name
     * @param attributeValue attribute value
     * @return this
     * @throws IOException on IO error
     * @throws IllegalStateException if a content as already been set for this element or if there is no current element
     */
    public XMLOutputStreamWriter addAttribute(String attributeName, String attributeValue) throws IOException {
        if (this.isCurrentElementReady()) {
            if (this.isCurrentAcceptAttribute()) {
                this.writer.write(' ');
                this.writer.write(attributeName);
                this.writer.write("=\"");
                this.writeAttributeValue(attributeValue.toCharArray());
                this.writer.write('"');
            } else {
                throw new IllegalStateException("Cannot add attribute because content have " + "aready been writed for current element");
            }
        } else {
            throw new IllegalStateException("Cannot add attribute there is no current element ready");
        }
        return this;
    }

    /**
     * Close the given element
     * @param elt the element to close
     * @throws IOException on IO error
     */
    private void closeElement(StackedElement elt) throws IOException {
        if (elt != null) {
            if (elt.haveContent) {
                this.indentLevel--;
                this.writer.write(OPEN_TAG);
                this.writer.write('/');
                this.writer.write(elt.getName());
                this.writer.write(CLOSE_TAG);
            } else {
                this.writer.write('/');
                this.writer.write(CLOSE_TAG);
            }
            if (indentation) {
                this.writer.write(LINE_SEPARATOR);
            }
        }
    }

    private void indent() throws IOException {
        if (indentation) {
            for (int i = 0; i < this.indentLevel; i++) {
                this.writer.write(' ');
            }
        }
    }

    /**
     * Writes an attribute value
     * @param value the value to write
     * @throws IOException on IO error
     */
    private void writeAttributeValue(char[] value) throws IOException {
        for (int i = 0; i < value.length; i++) {
            switch(value[i]) {
                case '&':
                    this.writer.write("&amp;");
                    break;
                case '"':
                    this.writer.write("&quot;");
                    break;
                default:
                    this.writer.write(value[i]);
            }
        }
    }

    /**
     * Writes a content value
     * @param value the value to write
     * @param offset index
     * @param length
     * @throws IOException on IO error
     */
    private void writeContent(char[] value, int offset, int length) throws IOException {
        char c;
        for (int i = 0; i < length; i++) {
            c = value[offset + i];
            switch(c) {
                case '&':
                    this.writer.write("&amp;");
                    break;
                case OPEN_TAG:
                    this.writer.write("&lt;");
                    break;
                case CLOSE_TAG:
                    this.writer.write("&gt;");
                    break;
                default:
                    this.writer.write(c);
            }
        }
    }

    /**
     * Returns true if I have a current element ready
     * @return true if I have a current element ready
     */
    private boolean isCurrentElementReady() {
        return !(this.stack.isEmpty() || this.stack.getLast() == null);
    }

    /**
     * Return true if current element can accept attribute
     * @return true if current element can accept attribute
     */
    private boolean isCurrentAcceptAttribute() {
        return !((StackedElement) this.stack.getLast()).haveContent();
    }

    /**
     * Set the have content flag to the current element if any and close the tag.
     * @throws IOException on IO error
     */
    private void setHaveContentToCurrent() throws IOException {
        if (this.isCurrentElementReady()) {
            StackedElement elt = (StackedElement) this.stack.getLast();
            if (!elt.haveContent) {
                elt.setHaveContent();
                this.writer.write(CLOSE_TAG);
                this.indentLevel++;
            }
        }
    }

    /**
     * Represent a stacked element with is name, and a flag to indicate that
     * a content as been put. If a content as been put, no more attributes can be added
     * and it requires a closing tag.
     * @author julien
     */
    private class StackedElement {

        /** Name */
        private String name = null;

        /** Have content */
        private boolean haveContent = false;

        /**
         * Creates a new StackedElement
         * @param name the element name
         */
        public StackedElement(String name) {
            this.name = name;
        }

        /**
         * Returns true is there is content
         * @return true is there is content
         */
        public boolean haveContent() {
            return this.haveContent;
        }

        /**
         * Set the haveContent flag to true
         */
        public void setHaveContent() {
            this.haveContent = true;
        }

        /**
         * Name
         * @return name
         */
        public String getName() {
            return this.name;
        }
    }
}
