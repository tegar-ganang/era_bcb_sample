package org.darkimport.omeglespy_z_desktop;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author user
 * 
 */
public class LogViewerHelper {

    private static final Log log = LogFactory.getLog(LogViewerHelper.class);

    public static final String[] CLASS_NAMES = { "youmsg", "strangermsg" };

    public static final String BTN_LINK = "btn-link";

    private static final String CONVO_LINK = "convo-link";

    private static final Pattern SAVCON_REGEX = Pattern.compile("save-convo-(\\d+)");

    private static final String HTML_EXT = ".html";

    private static final JFileChooser fc;

    public static final Pattern url_regex = Pattern.compile("([a-z]{2,6}://)?(?:[a-z0-9\\-]+\\.)+[a-z]{2,6}(?::\\d+)?(/\\S*)?", Pattern.CASE_INSENSITIVE);

    static {
        JFileChooser jfc;
        try {
            jfc = new JFileChooser();
            jfc.addChoosableFileFilter(new FileFilter() {

                @Override
                public boolean accept(final File f) {
                    final String n = f.getName().toLowerCase();
                    return f.isDirectory() || n.endsWith(HTML_EXT);
                }

                @Override
                public String getDescription() {
                    return "HTML files (*.html)";
                }
            });
        } catch (final Exception ex) {
            jfc = null;
            log.warn("Could not create a file selector window", ex);
        }
        fc = jfc;
    }

    /**
	 * @param e
	 * @param conversations
	 * @throws IOException
	 */
    public static Boolean saveLog(final Element e, final String baseHtml, final Map<Integer, Map<Date, String>> conversations) throws IOException {
        final HTMLDocument.RunElement re = (HTMLDocument.RunElement) e;
        final AttributeSet atts = (AttributeSet) re.getAttributes().getAttribute(HTML.Tag.A);
        final String className = (String) atts.getAttribute(HTML.Attribute.CLASS);
        if (className != null && className.equals(BTN_LINK)) {
            final String id = (String) atts.getAttribute(HTML.Attribute.ID);
            Matcher m;
            if ((m = SAVCON_REGEX.matcher(id)).matches()) {
                final int ci = Integer.parseInt(m.group(1));
                final Map<Date, String> conversation = conversations.get(ci);
                final StringBuffer conversationBuffer = generateWholeConversation(conversation);
                final String ct = baseHtml.replace("<!--%s-->", conversationBuffer.toString());
                return guiWriteHtmlFile(ct, null);
            }
        }
        return null;
    }

    /**
	 * @param conversation
	 * @return
	 */
    public static StringBuffer generateWholeConversation(final Map<Date, String> conversation) {
        final List<Date> sortedTimestamps = new ArrayList<Date>(conversation.keySet());
        Collections.sort(sortedTimestamps);
        final StringBuffer conversationBuffer = new StringBuffer();
        for (final Date timestamp : sortedTimestamps) {
            conversationBuffer.append(conversation.get(timestamp));
        }
        return conversationBuffer;
    }

    public static boolean guiWriteHtmlFile(final String text, final Component p) throws IOException {
        if (fc.showSaveDialog(p) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(HTML_EXT)) {
                f = new File(f.getParent(), f.getName() + HTML_EXT);
            }
            if (!(f.exists() && !showWarning(p, "The file " + f.getName() + " already exists. " + "Are you sure you want to " + "overwrite it?"))) {
                final FileOutputStream fos = new FileOutputStream(f);
                fos.write(text.getBytes());
                return true;
            }
        }
        return false;
    }

    private static boolean showWarning(final Component p, final String msg) {
        final int result = JOptionPane.showConfirmDialog(p, msg, "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    public static void printLabelledMsg(final String className, final String from, String msg, final Element currentConversation, final HTMLDocument doc) {
        msg = StringEscapeUtils.escapeHtml(msg);
        final StringBuffer sb = new StringBuffer();
        final Matcher m = url_regex.matcher(msg);
        while (m.find()) {
            String rep;
            if (m.group(1) != null || m.group(2) != null) {
                final String proto = m.group(1) == null ? "http://" : "";
                rep = "<a href='" + proto + "$0' target='_blank' " + "class='" + CONVO_LINK + "'>$0</a>";
            } else {
                rep = m.group();
            }
            m.appendReplacement(sb, rep);
        }
        m.appendTail(sb);
        msg = sb.toString();
        printLogItem(currentConversation, doc, "<span class='" + className + "'>" + from + ":</span> " + msg);
    }

    private static void printLogItem(final Element e, final HTMLDocument doc, final String line) {
        final DateFormat timestamp = DateFormat.getTimeInstance(DateFormat.SHORT);
        final String htmlText = "<div class='logitem'>" + "<span class='timestamp'>[" + timestamp.format(new Date()) + "]</span>" + " " + line + "</div>";
        try {
            doc.insertBeforeEnd(e, htmlText);
        } catch (final Exception ex) {
            log.warn("An error occurred while adding html, " + htmlText + ", to the chat history.", ex);
        }
    }

    public static void printBlockedMsg(final int fromIndex, final String fromName, final String msg, final Element currentConversation, final HTMLDocument doc) {
        final String className = CLASS_NAMES[fromIndex] + "-blocked";
        final String fromLbl = "<s>&lt;&lt;" + fromName + "&gt;&gt;</s>";
        printLabelledMsg(className, fromLbl, msg, currentConversation, doc);
    }

    public static void printSecretMsg(final int fromIndex, final String fromName, final String msg, final Element currentConversation, final HTMLDocument doc) {
        final String className = CLASS_NAMES[fromIndex] + "-secret";
        printLabelledMsg(className, "{{from " + fromName + "}}", msg, currentConversation, doc);
    }

    public static void printStatusLog(final Element e, final HTMLDocument doc, final String sl) {
        printLogItem(e, doc, "<span class='statuslog'>" + sl + "</span>");
    }
}
