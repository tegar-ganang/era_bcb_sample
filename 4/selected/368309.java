package net.sourceforge.sandirc.gui.text;

import jerklib.Channel;
import jerklib.Session;
import net.sourceforge.sandirc.gui.IRCWindow.Type;
import net.sourceforge.sandirc.gui.text.syntax.ColorLexer;
import net.sourceforge.sandirc.gui.text.syntax.IrcLexer;
import net.sourceforge.sandirc.gui.text.syntax.SimpleStyle;
import net.sourceforge.sandirc.gui.text.syntax.StyleProfile;
import net.sourceforge.sandirc.gui.text.syntax.tokens.Token;
import net.sourceforge.sandirc.utils.DynamicIntArray;
import java.awt.Font;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;

/**
 * @author mohadib
 */
public class IRCDocument extends PlainDocument {

    private final Session session;

    private final Channel channel;

    private final Type type;

    private final DateFormat df = new SimpleDateFormat("hh:mm");

    private JTextArea area;

    private StyleProfile styleProfile;

    private DynamicIntArray endTokens = new DynamicIntArray(500);

    private Segment seg = new Segment();

    private final IrcLexer lexer = new IrcLexer();

    private ColorLexer cl = new ColorLexer();

    private String nick;

    private Map<Integer, Map<Integer, SimpleStyle>> colorMap = new LinkedHashMap<Integer, Map<Integer, SimpleStyle>>();

    public IRCDocument(Session session, Channel channel, String nick, Type type, JTextArea area, StyleProfile styleProfile) {
        this.area = area;
        this.session = session;
        this.channel = channel;
        this.type = type;
        this.styleProfile = styleProfile;
        this.nick = nick;
        area.setFont(new Font("verdana", Font.PLAIN, 12));
        area.setDocument(this);
        area.setWrapStyleWord(true);
    }

    public Session getSession() {
        return session;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getNick() {
        return nick;
    }

    public Type getType() {
        return type;
    }

    public static String formatNick(String nick) {
        return "< " + nick + " > ";
    }

    public void insertDefault(String data) {
        cl = new ColorLexer();
        Map<Integer, SimpleStyle> sMap = cl.getFormatTokens(new Segment(data.toCharArray(), 0, data.length()));
        for (Iterator<Integer> it = sMap.keySet().iterator(); it.hasNext(); ) {
            Integer lineOffset = it.next();
            SimpleStyle style = sMap.get(lineOffset);
            String front = data.substring(0, lineOffset);
            front += data.substring(lineOffset + style.len);
            data = front;
        }
        colorMap.put(getDefaultRootElement().getElementCount() - 1, sMap);
        try {
            insertString(getLength(), data + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, SimpleStyle> getFormattingForLine(int lineNumb) {
        return colorMap.get(lineNumb);
    }

    public void insertMsg(String nick, String data) {
        if (data.startsWith("ACTION")) {
            try {
                data = data.substring(7, data.length() - 1);
                insertString(getLength(), "* " + nick + data + "\n", null);
                return;
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        if (channel != null) {
            List<String> modes = channel.getUsersModes(nick);
            Map<String, String> nickPrefixMap = session.getServerInformation().getNickPrefixMap();
            for (String prefix : nickPrefixMap.keySet()) {
                String mode = nickPrefixMap.get(prefix);
                if (modes.contains(mode)) {
                    nick = prefix + nick;
                }
            }
        }
        String msg = df.format(new Date()) + " " + formatNick(nick) + data + "\n";
        cl = new ColorLexer();
        Map<Integer, SimpleStyle> sMap = cl.getFormatTokens(new Segment(msg.toCharArray(), 0, msg.length()));
        for (Iterator<Integer> it = sMap.keySet().iterator(); it.hasNext(); ) {
            Integer lineOffset = it.next();
            SimpleStyle style = sMap.get(lineOffset);
            String front = msg.substring(0, lineOffset);
            front += msg.substring(lineOffset + style.len);
            msg = front;
        }
        colorMap.put(getDefaultRootElement().getElementCount() - 1, sMap);
        try {
            insertString(getLength(), msg, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public Token getTokenForOffset(int offset) {
        Element root = getDefaultRootElement();
        int index = root.getElementIndex(offset);
        return lexer.getTokenForOffset(index, endTokens.get(Math.max(0, index - 1)), offset, this);
    }

    protected void fireInsertUpdate(DocumentEvent e) {
        Element lineMap = getDefaultRootElement();
        DocumentEvent.ElementChange change = e.getChange(lineMap);
        Element[] added = (change == null) ? null : change.getChildrenAdded();
        int numLines = lineMap.getElementCount();
        int line = lineMap.getElementIndex(e.getOffset());
        int previousLine = line - 1;
        int previousTokenType = ((previousLine > -1) ? endTokens.get(previousLine) : Token.NULL);
        if ((added != null) && (added.length > 0)) {
            Element[] removed = change.getChildrenRemoved();
            int numRemoved = (removed != null) ? removed.length : 0;
            int endBefore = (line + added.length) - numRemoved;
            for (int i = line; i < endBefore; i++) {
                setSharedSegment(i);
                int tokenType = lexer.getLastTokenTypeOnLine(seg, previousTokenType, this);
                endTokens.add(i, tokenType);
                previousTokenType = tokenType;
            }
            updateLastTokensBelow(endBefore, numLines, previousTokenType);
        } else {
            updateLastTokensBelow(line, numLines, previousTokenType);
        }
        super.fireInsertUpdate(e);
    }

    protected void fireRemoveUpdate(DocumentEvent chng) {
        Element lineMap = getDefaultRootElement();
        int numLines = lineMap.getElementCount();
        DocumentEvent.ElementChange change = chng.getChange(lineMap);
        Element[] removed = (change == null) ? null : change.getChildrenRemoved();
        if ((removed != null) && (removed.length > 0)) {
            int line = change.getIndex();
            int previousLine = line - 1;
            int previousTokenType = ((previousLine > -1) ? endTokens.get(previousLine) : Token.NULL);
            Element[] added = change.getChildrenAdded();
            int numAdded = (added == null) ? 0 : added.length;
            int endBefore = (line + removed.length) - numAdded;
            endTokens.removeRange(line, endBefore);
            updateLastTokensBelow(line, numLines, previousTokenType);
        } else {
            int line = lineMap.getElementIndex(chng.getOffset());
            if (line >= endTokens.getSize()) {
                return;
            }
            int previousLine = line - 1;
            int previousTokenType = ((previousLine > -1) ? endTokens.get(previousLine) : Token.NULL);
            updateLastTokensBelow(line, numLines, previousTokenType);
        }
        super.fireRemoveUpdate(chng);
    }

    /**
     * Makes our private <code>Segment s</code> point to the text in our
     * document referenced by the specified element. Note that <code>line</code>
     * MUST be a valid line number in the document.
     *
     * @param line
     *          The line number you want to get.
     */
    private final void setSharedSegment(int line) {
        Element map = getDefaultRootElement();
        int numLines = map.getElementCount();
        Element element = map.getElement(line);
        if (element == null) {
            throw new InternalError("Invalid line number: " + line);
        }
        int startOffset = element.getStartOffset();
        int endOffset = ((line == (numLines - 1)) ? (element.getEndOffset() - 1) : (element.getEndOffset() - 1));
        try {
            getText(startOffset, endOffset - startOffset, seg);
        } catch (BadLocationException ble) {
            throw new InternalError("Text range not in document: " + startOffset + "-" + endOffset);
        }
    }

    private int updateLastTokensBelow(int line, int numLines, int previousTokenType) {
        int firstLine = line;
        int end = numLines - 1;
        while (line < end) {
            setSharedSegment(line);
            int oldTokenType = endTokens.get(line);
            int newTokenType = lexer.getLastTokenTypeOnLine(seg, previousTokenType, this);
            if (oldTokenType == newTokenType) {
                damageRange(firstLine, line);
                return line;
            }
            endTokens.set(line, newTokenType);
            previousTokenType = newTokenType;
            line++;
        }
        if (line > firstLine) {
            damageRange(firstLine, line);
        }
        return line;
    }

    private void damageRange(int firstLine, int lastLine) {
        Element f = getDefaultRootElement().getElement(firstLine);
        Element e = getDefaultRootElement().getElement(lastLine);
        area.getUI().damageRange(area, f.getStartOffset(), e.getEndOffset());
    }

    public final List<Token> getTokenListForLine(int line) {
        Element map = getDefaultRootElement();
        Element elem = map.getElement(line);
        int startOffset = elem.getStartOffset();
        int endOffset = elem.getEndOffset() - 1;
        try {
            getText(startOffset, endOffset - startOffset, seg);
        } catch (BadLocationException ble) {
            ble.printStackTrace();
            return null;
        }
        int initialTokenType = Token.NULL;
        if (line > 0) {
            initialTokenType = endTokens.get(line - 1);
        }
        return lexer.getTokens(seg, initialTokenType, this);
    }

    public SimpleStyle getStyleForType(int type) {
        return styleProfile.get(type);
    }
}
