package org.maverickdbms.basic.term;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import org.maverickdbms.basic.Terminal;

public class PseudoTerminal implements Termios {

    private static final int[] DEFAULT_ATTRIBUTES = { ISTRIP, ICRNL, IMAXBEL, IXON, IXANY, OPOST, ONLCR, ECHO, ICANON, ISIG, IEXTEN, ECHOE, ECHOKE, ECHOCTL, CS7, PARENB };

    private String term;

    private int width;

    private int height;

    private int[] attributes = new int[NO_OF_MODES];

    private InputStream in;

    private OutputStream out;

    private Reader reader;

    private Writer writer;

    private Terminals terminals;

    private Terminal emulation;

    public PseudoTerminal(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        for (int i = 0; i < DEFAULT_ATTRIBUTES.length; i++) {
            attributes[DEFAULT_ATTRIBUTES[i]] = 1;
        }
    }

    public PseudoTerminal(Reader reader, Writer writer) {
        this.reader = reader;
        this.writer = writer;
        for (int i = 0; i < DEFAULT_ATTRIBUTES.length; i++) {
            attributes[DEFAULT_ATTRIBUTES[i]] = 1;
        }
    }

    public int getAttribute(int no) throws IOException {
        switch(no) {
            case VERASE:
                if (attributes[no] == 0) {
                    return getEmulation().getEscapeSequence(Terminal.TERM_BACKSPACEKEY).charAt(0);
                }
                break;
        }
        return attributes[no];
    }

    public Terminal getEmulation() throws IOException {
        if (emulation == null) {
            emulation = terminals.getTerminal(term);
        }
        return emulation;
    }

    public int getHeight() throws IOException {
        if (height == 0) {
            return getEmulation().getHeight();
        }
        return height;
    }

    public Reader getReader() {
        if (reader == null) {
            reader = new TermiosReader(this, in);
        }
        return reader;
    }

    public String getTerm() {
        return term;
    }

    public int getWidth() throws IOException {
        if (width == 0) {
            return getEmulation().getWidth();
        }
        return width;
    }

    public Writer getWriter() {
        if (writer == null) {
            writer = new TermiosWriter(this, out);
        }
        return writer;
    }

    public void setAttribute(int no, int value) {
        attributes[no] = value;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setTerm(String term) {
        if (this.term == null || !this.term.equals(term)) {
            emulation = null;
        }
        this.term = term;
    }

    public void setTerminals(Terminals terminals) {
        this.terminals = terminals;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
