package org.omegat.filters2.html;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;
import org.omegat.filters2.LBuffer;
import org.omegat.filters2.AbstractFilter;
import org.omegat.filters2.Instance;
import org.omegat.filters2.TranslationException;
import org.omegat.util.AntiCRReader;
import org.omegat.util.EncodingAwareReader;
import org.omegat.util.OConsts;
import org.omegat.util.OStrings;
import org.omegat.util.StaticUtils;
import org.omegat.util.UTF8Writer;

public class HTMLFilter extends AbstractFilter {

    public HTMLFilter() {
        m_tagList = new LinkedList();
        m_preNT = new LinkedList();
        m_fdList = new LinkedList();
        m_postNT = new LinkedList();
    }

    /**
     * Convert simplified formatting tags to full originals.
     * Version 2.
     *
     * @author Maxym Mykhalchuk
     */
    private String formatString(String text) {
        StringBuffer res = new StringBuffer(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<') {
                try {
                    int tagend = text.indexOf('>', i);
                    if (tagend > i + 2) {
                        String inside = text.substring(i + 1, tagend);
                        boolean close = false;
                        int shortcutStart = 0;
                        int shortcutEnd = inside.length() - 1;
                        if (inside.charAt(0) == '/') {
                            shortcutStart = 1;
                            close = true;
                        } else if (inside.charAt(shortcutEnd) == '/') {
                            shortcutEnd = inside.length() - 2;
                            close = true;
                        }
                        char shortcut = inside.charAt(shortcutStart);
                        String tagNumberString = inside.substring(shortcutStart + 1, shortcutEnd + 1);
                        int tagNumber = Integer.parseInt(tagNumberString);
                        HTMLTag tag = (HTMLTag) m_tagList.get(tagNumber);
                        if (tag.shortcut() == shortcut) {
                            if (!close) {
                                res.append('<');
                                res.append(tag.verbatum().string());
                                res.append('>');
                            } else {
                                if (tag.name().startsWith("/")) res.append("<"); else res.append("</");
                                res.append(tag.name());
                                res.append('>');
                            }
                            i = tagend;
                            continue;
                        }
                    }
                } catch (NumberFormatException nfe) {
                } catch (StringIndexOutOfBoundsException sioob) {
                } catch (IndexOutOfBoundsException iobe) {
                } catch (Exception e) {
                    StaticUtils.log("Exception: " + e);
                }
            }
            res.append(convertToEntity(c));
        }
        return res.toString();
    }

    public void reset() {
        m_tagList.clear();
        m_preNT.clear();
        m_postNT.clear();
        m_fdList.clear();
        m_ws = false;
        m_pre = false;
        m_hasText = false;
        nextChar_line = 1;
        nextChar_pos = 1;
        pushedChars = new Stack();
    }

    /** Stack where you can push characters "back" the stream. */
    private Stack pushedChars = new Stack();

    /** The number of lines we processed. */
    private int nextChar_line = 1;

    /** The number of characters in a current line we processed. */
    private int nextChar_pos = 1;

    /** 
     * Pushs one character "back" the active stream. 
     * You can push arbitrary many characters there.
     */
    protected void pushNextChar(char c) {
        pushedChars.push(new Character(c));
    }

    /** 
     * Returns the next character on the stream.
     * If the characters were previously pushed "back", returns the pushed chars.
     */
    private int getNextChar(BufferedReader infile) throws IOException {
        if (!pushedChars.empty()) {
            int res = ((Character) pushedChars.pop()).charValue();
            return res;
        } else {
            int nextchar = infile.read();
            if (nextchar == '\n') {
                nextChar_line++;
                nextChar_pos = 1;
            } else {
                nextChar_pos++;
            }
            return nextchar;
        }
    }

    private char getEscChar(BufferedReader reader, FormatData fd) throws IOException, TranslationException {
        char c = 0;
        int i;
        int ctr = 0;
        int numeric = 0;
        int val = 0;
        LBuffer buf = new LBuffer(8);
        while ((i = getNextChar(reader)) >= 0) {
            c = (char) i;
            fd.appendOrig(c);
            if (ctr == 0) {
                if (c == 10 || c == 13 || c == 0 || c == ' ') {
                    fd.appendDisplay('&');
                    c = ' ';
                    break;
                }
                if (c == '#') {
                    numeric = 1;
                    ctr++;
                    continue;
                }
            } else if (ctr == 1 && numeric > 0) {
                if (c == 'x' || c == 'X') {
                    numeric = 2;
                    ctr++;
                    continue;
                }
            }
            if (numeric > 0) {
                if (c == ';') {
                    c = (char) val;
                    break;
                }
                if (numeric == 1) {
                    if (c >= '0' && c <= '9') val = val * 10 + (c - '0'); else {
                        pushNextChar(c);
                        c = (char) val;
                        break;
                    }
                } else if (numeric == 2) {
                    if (c > 'Z') c -= 'a' - 'A';
                    if (c >= '0' && c <= '9') val = val * 16 + (c - '0'); else if (c >= 'A' && c <= 'F') val = val * 16 + (c - 'A'); else {
                        pushNextChar(c);
                        c = (char) val;
                        break;
                    }
                }
                ctr++;
                if (ctr > 10) throw new TranslationException(OStrings.getString("HTMLFILTER_ERROR_ILLEGAL_ENTITY") + buf.string());
                continue;
            } else if (c == '&' || c == ' ' || c == 10 || c == 13 || ctr > 10) {
                String pushing = buf.string();
                for (int n = pushing.length() - 1; n >= 0; n--) pushNextChar(pushing.charAt(n));
                pushNextChar(c);
                c = '&';
                break;
            }
            if (c == ';') {
                c = convertToChar(buf.string());
                break;
            }
            buf.append(c);
            ctr++;
        }
        if (c == 0) throw new IOException(MessageFormat.format(OStrings.getString("HFH_ERROR_UNRECOGNIZED_ESCAPE_CHAR"), new Object[] { "" + nextChar_line }) + "\n " + buf.string());
        return c;
    }

    private void handleTag(BufferedReader reader, BufferedWriter writer) throws IOException, TranslationException {
        HTMLTag tag;
        FormatData fd;
        tag = identTag(reader);
        fd = new FormatData(tag.close());
        fd.setOrig(tag.verbatum());
        if (tag.type() == HTMLTag.TAG_FORMAT) {
            if (m_hasText) {
                m_fdList.add(fd);
            } else {
                m_preNT.add(fd);
            }
            HTMLTag cand;
            if (tag.close()) {
                ListIterator it;
                it = m_tagList.listIterator(m_tagList.size());
                boolean foundPartner = false;
                while (it.hasPrevious()) {
                    cand = (HTMLTag) it.previous();
                    if (tag.willPartner(cand)) {
                        cand.setPartner();
                        tag.setPartner();
                        fd.setTagData(cand.shortcut(), cand.num());
                        foundPartner = true;
                        break;
                    }
                }
                if (!foundPartner) {
                    tag.setNum(m_tagList.size());
                    m_tagList.add(tag);
                    fd.setTagData(tag.shortcut(), tag.num());
                }
            } else {
                tag.setNum(m_tagList.size());
                m_tagList.add(tag);
                fd.setTagData(tag.shortcut(), tag.num());
            }
            fd.finalize();
        } else {
            m_postNT.add(fd);
            fd.finalize();
            writeEntry(writer);
        }
        if (tag.isPreTag()) {
            m_pre = !tag.close();
        }
    }

    private void writeEntry(BufferedWriter writer) throws IOException {
        ListIterator it;
        FormatData fd;
        LBuffer buf;
        compressOutputData();
        if (m_fdList.size() == 0 && writer == null) {
            m_preNT.clear();
            m_postNT.clear();
            m_fdList.clear();
            m_tagList.clear();
            m_ws = false;
            m_hasText = false;
            return;
        }
        if (writer != null) {
            it = m_preNT.listIterator();
            while (it.hasNext()) {
                fd = (FormatData) it.next();
                buf = fd.getOrig();
                writer.write(buf.getBuf(), 0, buf.size());
            }
        }
        if (m_fdList.size() > 0) {
            it = m_fdList.listIterator();
            LBuffer out = new LBuffer(256);
            while (it.hasNext()) {
                fd = (FormatData) it.next();
                out.append(fd.getDisplay());
            }
            writer.write(formatString(processEntry(out.string())));
        }
        if (writer != null) {
            it = m_postNT.listIterator(m_postNT.size());
            while (it.hasPrevious()) {
                fd = (FormatData) it.previous();
                buf = fd.getOrig();
                writer.write(buf.getBuf(), 0, buf.size());
            }
        }
        m_preNT.clear();
        m_postNT.clear();
        m_fdList.clear();
        m_tagList.clear();
        m_ws = false;
        m_hasText = false;
    }

    private void compressOutputData() {
        boolean change = true;
        FormatData fd_head;
        FormatData fd_tail;
        ListIterator it;
        int ctr;
        int len;
        FormatData fd;
        while (change) {
            if (m_fdList.size() == 0) break;
            fd_head = (FormatData) m_fdList.getFirst();
            fd_tail = (FormatData) m_fdList.getLast();
            if (fd_head.isWhiteSpace()) {
                m_preNT.add(fd_head);
                m_fdList.removeFirst();
                continue;
            }
            if (fd_tail.isWhiteSpace()) {
                m_postNT.add(fd_tail);
                m_fdList.removeLast();
                continue;
            }
            if (m_fdList.size() == 1) {
                if (fd_head.isTag()) {
                    m_preNT.add(fd_head);
                    m_fdList.clear();
                }
                break;
            }
            if (fd_tail.isTag() && !fd_tail.isCloseTag()) {
                m_postNT.add(fd_tail);
                m_fdList.removeLast();
                continue;
            }
            if (fd_head.isTag() && fd_head.isCloseTag()) {
                m_preNT.add(fd_head);
                m_fdList.removeFirst();
                continue;
            }
            if (fd_tail.isTag() && fd_head.isTag() && fd_tail.tagData() == fd_head.tagData()) {
                if (m_fdList.size() == 1) break;
                m_postNT.add(fd_tail);
                m_preNT.add(fd_head);
                m_fdList.removeFirst();
                m_fdList.removeLast();
                continue;
            }
            if (fd_head.isTag()) {
                it = m_fdList.listIterator();
                ctr = 0;
                len = m_fdList.size();
                while (it.hasNext()) {
                    fd = (FormatData) it.next();
                    if (ctr > 0) {
                        if (fd_head.tagData() == fd.tagData()) break;
                    }
                    ctr++;
                }
                if (ctr < len) {
                    m_preNT.add(fd_head);
                    m_fdList.removeFirst();
                    continue;
                }
            }
            if (m_fdList.size() == 1) {
                if (fd_head.isTag()) {
                    m_preNT.add(fd_head);
                    m_fdList.clear();
                }
                break;
            }
            if (fd_tail.isTag()) {
                it = m_fdList.listIterator();
                ctr = 0;
                len = m_fdList.size();
                while (it.hasNext()) {
                    fd = (FormatData) it.next();
                    if (ctr < len) {
                        if (fd_tail.tagData() == fd.tagData()) break;
                    }
                    ctr++;
                }
                if (ctr < len) {
                    m_postNT.add(fd_tail);
                    m_fdList.removeLast();
                    continue;
                }
            }
            change = false;
        }
    }

    private LinkedList m_tagList;

    private LinkedList m_preNT;

    private LinkedList m_fdList;

    private LinkedList m_postNT;

    private boolean m_ws;

    private boolean m_pre;

    private boolean m_hasText;

    public Instance[] getDefaultInstances() {
        return new Instance[] { new Instance("*.html", ENCODING_AUTO, "UTF-8"), new Instance("*.htm", ENCODING_AUTO, "UTF-8", "${nameOnly}.html") };
    }

    public String getFileFormatName() {
        return OStrings.getString("HTMLFILTER_FILTER_NAME");
    }

    public boolean isSourceEncodingVariable() {
        return true;
    }

    public boolean isTargetEncodingVariable() {
        return false;
    }

    public void processFile(BufferedReader reader, BufferedWriter writer) throws IOException, TranslationException {
        reset();
        char c;
        int i;
        FormatData fd = null;
        try {
            while ((i = getNextChar(reader)) >= 0) {
                c = (char) i;
                try {
                    if (c == '<') {
                        fd = null;
                        reader.mark(OConsts.READ_AHEAD_LIMIT);
                        handleTag(reader, writer);
                        continue;
                    }
                } catch (TranslationException e) {
                    reader.reset();
                }
                if (c == '\n' || c == 9 || c == ' ') {
                    if (!m_ws || fd == null) {
                        fd = new FormatData();
                        if (m_hasText) m_fdList.add(fd); else m_preNT.add(fd);
                    }
                    fd.appendOrig(c);
                    if (m_ws && !m_pre) continue;
                    m_ws = true;
                    if (m_pre) fd.appendDisplay(c); else fd.appendDisplay(' ');
                } else {
                    if (!m_hasText || m_ws || fd == null) {
                        fd = new FormatData();
                        m_fdList.add(fd);
                    }
                    fd.appendOrig(c);
                    if (c == '&') {
                        c = getEscChar(reader, fd);
                    }
                    if (c == 160 && !m_hasText) {
                        fd.appendDisplay(c);
                        m_ws = true;
                        m_hasText = false;
                    } else {
                        fd.appendDisplay(c);
                        fd.setHasText();
                        m_hasText = true;
                        m_ws = false;
                    }
                }
            }
            writeEntry(writer);
            writer.flush();
        } catch (IOException ioe) {
            String str = OStrings.FH_ERROR_WRITING_FILE;
            throw new IOException(str + ": \n" + ioe);
        }
    }

    /**
     * Customized version of creating input reader for HTML files,
     * aware of encoding by using <code>EncodingAwareReader</code> class.
     *
     * @see org.omegat.util.EncodingAwareReader
     */
    public BufferedReader createReader(File infile, String encoding) throws UnsupportedEncodingException, IOException {
        return new BufferedReader(new AntiCRReader(new EncodingAwareReader(infile.getAbsolutePath(), EncodingAwareReader.ST_HTML)));
    }

    /**
     * Customized version of creating an output stream for HTML files,
     * always UTF-8 and appending charset meta with UTF-8
     * by using <code>UTF8Writer</code> class.
     *
     * @see org.omegat.util.UTF8Writer
     */
    public BufferedWriter createWriter(File outfile, String encoding) throws UnsupportedEncodingException, IOException {
        return new BufferedWriter(new UTF8Writer(outfile.getAbsolutePath(), UTF8Writer.ST_HTML));
    }

    private HTMLTag identTag(BufferedReader reader) throws IOException, TranslationException {
        char c;
        int ctr = 0;
        int state = STATE_START;
        HTMLTag tag = new HTMLTag();
        int charType;
        HTMLTagAttr tagAttr = null;
        int i;
        int excl = 0;
        while (true) {
            ctr++;
            i = getNextChar(reader);
            if (i < 0) break;
            c = (char) i;
            if (ctr == 1 && c == '/') {
                tag.setClose();
                continue;
            }
            if (ctr == 1 && c == '!') {
                excl = 1;
                tag.verbatumAppend(c);
            }
            if (ctr == 1) {
                if (c == '/') {
                    tag.setClose();
                    continue;
                } else if (c == '!') {
                    excl = 1;
                    continue;
                }
            } else if (ctr == 2 && excl == 1) {
                if (c == '-') excl = 2;
            }
            if (excl > 1) {
                if (c == '-') excl++; else if (c == '>' && excl >= 4) break; else excl = 2;
                tag.verbatumAppend(c);
                continue;
            }
            switch(c) {
                case 9:
                case 10:
                case 13:
                case ' ':
                    charType = TYPE_WS;
                    break;
                case '=':
                    charType = TYPE_EQUAL;
                    break;
                case '\'':
                    charType = TYPE_QUOTE_SINGLE;
                    break;
                case '"':
                    charType = TYPE_QUOTE_DOUBLE;
                    break;
                case '>':
                    charType = TYPE_CLOSE;
                    break;
                case 0:
                    throw new TranslationException(MessageFormat.format(OStrings.getString("HP_ERROR_NULL"), new Object[] { "" + nextChar_line }));
                default:
                    charType = TYPE_NON_IDENT;
                    break;
            }
            switch(state) {
                case STATE_START:
                    switch(charType) {
                        case TYPE_NON_IDENT:
                            tag.nameAppend(c);
                            state = STATE_TOKEN;
                            break;
                        default:
                            throw new TranslationException(MessageFormat.format(OStrings.getString("HP_ERROR_BAD_TAG_START"), new Object[] { "" + nextChar_line }));
                    }
                    break;
                case STATE_TOKEN:
                    switch(charType) {
                        case TYPE_CLOSE:
                            state = STATE_CLOSE;
                            break;
                        case TYPE_WS:
                            if (excl > 0) state = STATE_RECORD; else state = STATE_WS;
                            break;
                        case TYPE_NON_IDENT:
                            tag.nameAppend(c);
                            break;
                        default:
                            throw new TranslationException(MessageFormat.format(OStrings.getString("HP_ERROR_BAD_TAG"), new Object[] { "" + nextChar_line }));
                    }
                    break;
                case STATE_RECORD:
                    switch(charType) {
                        case TYPE_QUOTE_SINGLE:
                            state = STATE_RECORD_QUOTE_SINGLE;
                            break;
                        case TYPE_QUOTE_DOUBLE:
                            state = STATE_RECORD_QUOTE_DOUBLE;
                            break;
                        case TYPE_CLOSE:
                            state = STATE_CLOSE;
                            break;
                    }
                    break;
                case STATE_RECORD_QUOTE_SINGLE:
                    switch(charType) {
                        case TYPE_QUOTE_SINGLE:
                            state = STATE_RECORD;
                            break;
                    }
                    break;
                case STATE_RECORD_QUOTE_DOUBLE:
                    switch(charType) {
                        case TYPE_QUOTE_DOUBLE:
                            state = STATE_RECORD;
                            break;
                    }
                    break;
                case STATE_WS:
                    switch(charType) {
                        case TYPE_WS:
                            break;
                        case TYPE_CLOSE:
                            state = STATE_CLOSE;
                            break;
                        case TYPE_NON_IDENT:
                            tagAttr = new HTMLTagAttr();
                            tagAttr.attrAppend(c);
                            tag.addAttr(tagAttr);
                            state = STATE_ATTR;
                            break;
                        default:
                            throw new TranslationException(MessageFormat.format(OStrings.getString("HP_ERROR_UNEXPECTED_CHAR"), new Object[] { tag.verbatum().string(), "" + nextChar_line }));
                    }
                    break;
                case STATE_ATTR_WS:
                    switch(charType) {
                        case TYPE_WS:
                            break;
                        case TYPE_CLOSE:
                            state = STATE_CLOSE;
                            break;
                        case TYPE_NON_IDENT:
                            tagAttr = new HTMLTagAttr();
                            tagAttr.attrAppend(c);
                            tag.addAttr(tagAttr);
                            state = STATE_ATTR;
                            break;
                        case TYPE_EQUAL:
                            state = STATE_EQUAL;
                            break;
                        default:
                            throw new TranslationException(MessageFormat.format(OStrings.getString("HP_ERROR_UNEXPECTED_CHAR"), new Object[] { tag.verbatum().string(), "" + nextChar_line }));
                    }
                    break;
                case STATE_EQUAL_WS:
                    switch(charType) {
                        case TYPE_WS:
                            break;
                        case TYPE_NON_IDENT:
                            state = STATE_VAL;
                            tagAttr.valAppend(c);
                            break;
                        case TYPE_QUOTE_SINGLE:
                            state = STATE_VAL_QUOTE_SINGLE;
                            break;
                        case TYPE_QUOTE_DOUBLE:
                            state = STATE_VAL_QUOTE_DOUBLE;
                            break;
                        default:
                            throw new TranslationException(MessageFormat.format(OStrings.getString("HP_ERROR_UNEXPECTED_CHAR"), new Object[] { tag.verbatum().string(), "" + nextChar_line }));
                    }
                    break;
                case STATE_ATTR:
                    switch(charType) {
                        case TYPE_NON_IDENT:
                            tagAttr.attrAppend(c);
                            break;
                        case TYPE_WS:
                            state = STATE_ATTR_WS;
                            break;
                        case TYPE_EQUAL:
                            state = STATE_EQUAL;
                            break;
                        case TYPE_CLOSE:
                            state = STATE_CLOSE;
                            break;
                        default:
                            throw new TranslationException(MessageFormat.format(OStrings.getString("HP_ERROR_UNEXPECTED_CHAR"), new Object[] { tag.verbatum().string(), "" + nextChar_line }));
                    }
                    break;
                case STATE_EQUAL:
                    switch(charType) {
                        case TYPE_WS:
                            state = STATE_EQUAL_WS;
                            break;
                        case TYPE_NON_IDENT:
                            state = STATE_VAL;
                            tagAttr.valAppend(c);
                            break;
                        case TYPE_QUOTE_SINGLE:
                            state = STATE_VAL_QUOTE_SINGLE;
                            break;
                        case TYPE_QUOTE_DOUBLE:
                            state = STATE_VAL_QUOTE_DOUBLE;
                            break;
                        default:
                            throw new TranslationException(MessageFormat.format(OStrings.getString("HP_ERROR_UNEXPECTED_CHAR"), new Object[] { tag.verbatum().string(), "" + nextChar_line }));
                    }
                    break;
                case STATE_VAL:
                    switch(charType) {
                        case TYPE_NON_IDENT:
                            tagAttr.valAppend(c);
                            break;
                        case TYPE_WS:
                            state = STATE_WS;
                            break;
                        case TYPE_CLOSE:
                            state = STATE_CLOSE;
                            break;
                        default:
                            throw new TranslationException(MessageFormat.format(OStrings.getString("HP_ERROR_UNEXPECTED_CHAR"), new Object[] { tag.verbatum().string(), "" + nextChar_line }));
                    }
                    break;
                case STATE_VAL_QUOTE_SINGLE:
                    switch(charType) {
                        case TYPE_QUOTE_SINGLE:
                            state = STATE_VAL_QUOTE_CLOSE;
                            break;
                        default:
                            tagAttr.valAppend(c);
                    }
                    break;
                case STATE_VAL_QUOTE_DOUBLE:
                    switch(charType) {
                        case TYPE_QUOTE_DOUBLE:
                            state = STATE_VAL_QUOTE_CLOSE;
                            break;
                        default:
                            tagAttr.valAppend(c);
                    }
                    break;
                case STATE_VAL_QUOTE_CLOSE:
                    switch(charType) {
                        case TYPE_CLOSE:
                            state = STATE_CLOSE;
                            break;
                        case TYPE_WS:
                            state = STATE_WS;
                            break;
                        case TYPE_NON_IDENT:
                            tagAttr = new HTMLTagAttr();
                            tagAttr.attrAppend(c);
                            tag.addAttr(tagAttr);
                            state = STATE_ATTR;
                            break;
                        default:
                            throw new TranslationException(MessageFormat.format(OStrings.getString("HP_ERROR_UNEXPECTED_CHAR"), new Object[] { tag.verbatum().string(), "" + nextChar_line }));
                    }
                    break;
                default:
                    throw new TranslationException(MessageFormat.format(OStrings.getString("HP_ERROR_UNKNOWN_STATE"), new Object[] { "" + nextChar_line }));
            }
            if (state == STATE_CLOSE) break; else tag.verbatumAppend(c);
        }
        tag.finalize();
        return tag;
    }

    private static final int TYPE_WS = 1;

    private static final int TYPE_NON_IDENT = 2;

    private static final int TYPE_EQUAL = 3;

    private static final int TYPE_CLOSE = 4;

    private static final int TYPE_QUOTE_SINGLE = 5;

    private static final int TYPE_QUOTE_DOUBLE = 6;

    private static final int STATE_START = 0;

    private static final int STATE_TOKEN = 1;

    private static final int STATE_WS = 2;

    private static final int STATE_ATTR = 3;

    private static final int STATE_ATTR_WS = 5;

    private static final int STATE_EQUAL = 6;

    private static final int STATE_EQUAL_WS = 7;

    private static final int STATE_VAL_QUOTE_SINGLE = 10;

    private static final int STATE_VAL_QUOTE_DOUBLE = 11;

    private static final int STATE_VAL_QUOTE_CLOSE = 15;

    private static final int STATE_VAL = 20;

    private static final int STATE_RECORD = 30;

    private static final int STATE_RECORD_QUOTE_SINGLE = 31;

    private static final int STATE_RECORD_QUOTE_DOUBLE = 32;

    private static final int STATE_CLOSE = 40;

    private static HashMap m_charMap;

    private static HashMap m_escMap;

    static {
        m_escMap = new HashMap(512);
        m_charMap = new HashMap(512);
        addMapEntry('\'', "apos");
        addMapEntry('"', "quot");
        addMapEntry('&', "amp");
        addMapEntry((char) 60, "lt");
        addMapEntry((char) 62, "gt");
        addMapEntry((char) 160, "nbsp");
        addMapEntry((char) 161, "iexcl");
        addMapEntry((char) 162, "cent");
        addMapEntry((char) 163, "pound");
        addMapEntry((char) 164, "curren");
        addMapEntry((char) 165, "yen");
        addMapEntry((char) 166, "brvbar");
        addMapEntry((char) 167, "sect");
        addMapEntry((char) 168, "uml");
        addMapEntry((char) 169, "copy");
        addMapEntry((char) 170, "ordf");
        addMapEntry((char) 171, "laquo");
        addMapEntry((char) 172, "not");
        addMapEntry((char) 173, "shy");
        addMapEntry((char) 174, "reg");
        addMapEntry((char) 175, "macr");
        addMapEntry((char) 176, "deg");
        addMapEntry((char) 177, "plusmn");
        addMapEntry((char) 178, "sup2");
        addMapEntry((char) 179, "sup3");
        addMapEntry((char) 180, "acute");
        addMapEntry((char) 181, "micro");
        addMapEntry((char) 182, "para");
        addMapEntry((char) 183, "middot");
        addMapEntry((char) 184, "cedil");
        addMapEntry((char) 185, "sup1");
        addMapEntry((char) 186, "ordm");
        addMapEntry((char) 187, "raquo");
        addMapEntry((char) 188, "frac14");
        addMapEntry((char) 189, "frac12");
        addMapEntry((char) 190, "frac34");
        addMapEntry((char) 191, "iquest");
        addMapEntry((char) 192, "Agrave");
        addMapEntry((char) 193, "Aacute");
        addMapEntry((char) 194, "Acirc");
        addMapEntry((char) 195, "Atilde");
        addMapEntry((char) 196, "Auml");
        addMapEntry((char) 197, "Aring");
        addMapEntry((char) 198, "AElig");
        addMapEntry((char) 199, "Ccedil");
        addMapEntry((char) 200, "Egrave");
        addMapEntry((char) 201, "Eacute");
        addMapEntry((char) 202, "Ecirc");
        addMapEntry((char) 203, "Euml");
        addMapEntry((char) 204, "Igrave");
        addMapEntry((char) 205, "Iacute");
        addMapEntry((char) 206, "Icirc");
        addMapEntry((char) 207, "Iuml");
        addMapEntry((char) 208, "ETH");
        addMapEntry((char) 209, "Ntilde");
        addMapEntry((char) 210, "Ograve");
        addMapEntry((char) 211, "Oacute");
        addMapEntry((char) 212, "Ocirc");
        addMapEntry((char) 213, "Otilde");
        addMapEntry((char) 214, "Ouml");
        addMapEntry((char) 215, "times");
        addMapEntry((char) 216, "Oslash");
        addMapEntry((char) 217, "Ugrave");
        addMapEntry((char) 218, "Uacute");
        addMapEntry((char) 219, "Ucirc");
        addMapEntry((char) 220, "Uuml");
        addMapEntry((char) 221, "Yacute");
        addMapEntry((char) 222, "THORN");
        addMapEntry((char) 223, "szlig");
        addMapEntry((char) 224, "agrave");
        addMapEntry((char) 225, "aacute");
        addMapEntry((char) 226, "acirc");
        addMapEntry((char) 227, "atilde");
        addMapEntry((char) 228, "auml");
        addMapEntry((char) 229, "aring");
        addMapEntry((char) 230, "aelig");
        addMapEntry((char) 231, "ccedil");
        addMapEntry((char) 232, "egrave");
        addMapEntry((char) 233, "eacute");
        addMapEntry((char) 234, "ecirc");
        addMapEntry((char) 235, "euml");
        addMapEntry((char) 236, "igrave");
        addMapEntry((char) 237, "iacute");
        addMapEntry((char) 238, "icirc");
        addMapEntry((char) 239, "iuml");
        addMapEntry((char) 240, "eth");
        addMapEntry((char) 241, "ntilde");
        addMapEntry((char) 242, "ograve");
        addMapEntry((char) 243, "oacute");
        addMapEntry((char) 244, "ocirc");
        addMapEntry((char) 245, "otilde");
        addMapEntry((char) 246, "ouml");
        addMapEntry((char) 247, "divide");
        addMapEntry((char) 248, "oslash");
        addMapEntry((char) 249, "ugrave");
        addMapEntry((char) 250, "uacute");
        addMapEntry((char) 251, "ucirc");
        addMapEntry((char) 252, "uuml");
        addMapEntry((char) 253, "yacute");
        addMapEntry((char) 254, "thorn");
        addMapEntry((char) 255, "yuml");
        addMapEntry((char) 338, "OElig");
        addMapEntry((char) 339, "oelig");
        addMapEntry((char) 352, "Scaron");
        addMapEntry((char) 353, "scaron");
        addMapEntry((char) 376, "Yuml");
        addMapEntry((char) 402, "fnof");
        addMapEntry((char) 710, "circ");
        addMapEntry((char) 732, "tilde");
        addMapEntry((char) 913, "Alpha");
        addMapEntry((char) 914, "Beta");
        addMapEntry((char) 915, "Gamma");
        addMapEntry((char) 916, "Delta");
        addMapEntry((char) 917, "Epsilon");
        addMapEntry((char) 918, "Zeta");
        addMapEntry((char) 919, "Eta");
        addMapEntry((char) 920, "Theta");
        addMapEntry((char) 921, "Iota");
        addMapEntry((char) 922, "Kappa");
        addMapEntry((char) 923, "Lambda");
        addMapEntry((char) 924, "Mu");
        addMapEntry((char) 925, "Nu");
        addMapEntry((char) 926, "Xi");
        addMapEntry((char) 927, "Omicron");
        addMapEntry((char) 928, "Pi");
        addMapEntry((char) 929, "Rho");
        addMapEntry((char) 931, "Sigma");
        addMapEntry((char) 932, "Tau");
        addMapEntry((char) 933, "Upsilon");
        addMapEntry((char) 934, "Phi");
        addMapEntry((char) 935, "Chi");
        addMapEntry((char) 936, "Psi");
        addMapEntry((char) 937, "Omega");
        addMapEntry((char) 945, "alpha");
        addMapEntry((char) 946, "beta");
        addMapEntry((char) 947, "gamma");
        addMapEntry((char) 948, "delta");
        addMapEntry((char) 949, "epsilon");
        addMapEntry((char) 950, "zeta");
        addMapEntry((char) 951, "eta");
        addMapEntry((char) 952, "theta");
        addMapEntry((char) 953, "iota");
        addMapEntry((char) 954, "kappa");
        addMapEntry((char) 955, "lambda");
        addMapEntry((char) 956, "mu");
        addMapEntry((char) 957, "nu");
        addMapEntry((char) 958, "xi");
        addMapEntry((char) 959, "omicron");
        addMapEntry((char) 960, "pi");
        addMapEntry((char) 961, "rho");
        addMapEntry((char) 962, "sigmaf");
        addMapEntry((char) 963, "sigma");
        addMapEntry((char) 964, "tau");
        addMapEntry((char) 965, "upsilon");
        addMapEntry((char) 966, "phi");
        addMapEntry((char) 967, "chi");
        addMapEntry((char) 968, "psi");
        addMapEntry((char) 969, "omega");
        addMapEntry((char) 977, "thetasym");
        addMapEntry((char) 978, "upsih");
        addMapEntry((char) 982, "piv");
        addMapEntry((char) 8194, "ensp");
        addMapEntry((char) 8195, "emsp");
        addMapEntry((char) 8201, "thinsp");
        addMapEntry((char) 8204, "zwnj");
        addMapEntry((char) 8205, "zwj");
        addMapEntry((char) 8206, "lrm");
        addMapEntry((char) 8207, "rlm");
        addMapEntry((char) 8211, "ndash");
        addMapEntry((char) 8212, "mdash");
        addMapEntry((char) 8216, "lsquo");
        addMapEntry((char) 8217, "rsquo");
        addMapEntry((char) 8218, "sbquo");
        addMapEntry((char) 8220, "ldquo");
        addMapEntry((char) 8221, "rdquo");
        addMapEntry((char) 8222, "bdquo");
        addMapEntry((char) 8224, "dagger");
        addMapEntry((char) 8225, "Dagger");
        addMapEntry((char) 8226, "bull");
        addMapEntry((char) 8230, "hellip");
        addMapEntry((char) 8240, "permil");
        addMapEntry((char) 8242, "prime");
        addMapEntry((char) 8243, "Prime");
        addMapEntry((char) 8249, "lsaquo");
        addMapEntry((char) 8250, "rsaquo");
        addMapEntry((char) 8254, "oline");
        addMapEntry((char) 8260, "frasl");
        addMapEntry((char) 8364, "euro");
        addMapEntry((char) 8465, "image");
        addMapEntry((char) 8472, "weierp");
        addMapEntry((char) 8476, "real");
        addMapEntry((char) 8482, "trade");
        addMapEntry((char) 8501, "alefsym");
        addMapEntry((char) 8592, "larr");
        addMapEntry((char) 8593, "uarr");
        addMapEntry((char) 8594, "rarr");
        addMapEntry((char) 8595, "darr");
        addMapEntry((char) 8596, "harr");
        addMapEntry((char) 8629, "crarr");
        addMapEntry((char) 8656, "lArr");
        addMapEntry((char) 8657, "uArr");
        addMapEntry((char) 8658, "rArr");
        addMapEntry((char) 8659, "dArr");
        addMapEntry((char) 8660, "hArr");
        addMapEntry((char) 8704, "forall");
        addMapEntry((char) 8706, "part");
        addMapEntry((char) 8707, "exist");
        addMapEntry((char) 8709, "empty");
        addMapEntry((char) 8711, "nabla");
        addMapEntry((char) 8712, "isin");
        addMapEntry((char) 8713, "notin");
        addMapEntry((char) 8715, "ni");
        addMapEntry((char) 8719, "prod");
        addMapEntry((char) 8721, "sum");
        addMapEntry((char) 8722, "minus");
        addMapEntry((char) 8727, "lowast");
        addMapEntry((char) 8730, "radic");
        addMapEntry((char) 8733, "prop");
        addMapEntry((char) 8734, "infin");
        addMapEntry((char) 8736, "ang");
        addMapEntry((char) 8743, "and");
        addMapEntry((char) 8744, "or");
        addMapEntry((char) 8745, "cap");
        addMapEntry((char) 8746, "cup");
        addMapEntry((char) 8747, "int");
        addMapEntry((char) 8756, "there4");
        addMapEntry((char) 8764, "sim");
        addMapEntry((char) 8773, "cong");
        addMapEntry((char) 8776, "asymp");
        addMapEntry((char) 8800, "ne");
        addMapEntry((char) 8801, "equiv");
        addMapEntry((char) 8804, "le");
        addMapEntry((char) 8805, "ge");
        addMapEntry((char) 8834, "sub");
        addMapEntry((char) 8835, "sup");
        addMapEntry((char) 8836, "nsub");
        addMapEntry((char) 8838, "sube");
        addMapEntry((char) 8839, "supe");
        addMapEntry((char) 8853, "oplus");
        addMapEntry((char) 8855, "otimes");
        addMapEntry((char) 8869, "perp");
        addMapEntry((char) 8901, "sdot");
        addMapEntry((char) 8968, "lceil");
        addMapEntry((char) 8969, "rceil");
        addMapEntry((char) 8970, "lfloor");
        addMapEntry((char) 8971, "rfloor");
        addMapEntry((char) 9001, "lang");
        addMapEntry((char) 9002, "rang");
        addMapEntry((char) 9674, "loz");
        addMapEntry((char) 9824, "spades");
        addMapEntry((char) 9827, "clubs");
        addMapEntry((char) 9829, "hearts");
        addMapEntry((char) 9830, "diams");
    }

    private static void addMapEntry(char val, String name) {
        m_escMap.put(name, new Character(val));
        m_charMap.put(new Character(val), name);
    }

    /**
     * Converts XML entity to plaintext character.
     * If the entity cannot be converted, returns 0.
     */
    public static char convertToChar(String tok) {
        Character c = (Character) m_escMap.get(tok);
        if (c == null) {
            try {
                Integer i = new Integer(tok);
                return (char) i.intValue();
            } catch (NumberFormatException e) {
                StaticUtils.log("\nUnconvertable Entity: " + tok);
                return 0;
            }
        } else return c.charValue();
    }

    /**
     * Converts plaintext symbol to XML entity.
     */
    public static String convertToEntity(char c) {
        String s = (String) m_charMap.get(new Character(c));
        if (s != null) {
            return "&" + s + ";";
        } else return "" + c;
    }

    /**
     * Converts plaintext string into valid HTML/XML,
     * replacing all "special" symbols with corresponding entities.
     */
    public static String convertToEntities(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) sb.append(convertToEntity(s.charAt(i)));
        return sb.toString();
    }
}
