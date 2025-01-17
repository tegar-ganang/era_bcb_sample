package xml.indent;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.util.Log;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.List;
import java.util.ArrayList;
import beauty.BeautyPlugin;

/**
 * EditPlugin implementation for the XML Indenter plugin.
 *
 * @author Robert McKinnon - robmckinnon@users.sourceforge.net
 */
public class XmlIndenterPlugin {

    private static final String XSL_TEXT_ELEMENT = "xsl:text";

    private static final String SVG_TEXT_ELEMENT = "text";

    private static final String SVG_TSPAN_ELEMENT = "tspan";

    private static final IndentingTransformerImpl TRANSFORMER = new IndentingTransformerImpl();

    public void start() {
        String modified = jEdit.getProperty("xmlindenter.preserve-whitespace-element.modified");
        boolean settingModified = (modified != null);
        if (!settingModified) {
            jEdit.setProperty("xmlindenter.preserve-whitespace-element.0", XSL_TEXT_ELEMENT);
            jEdit.setProperty("xmlindenter.preserve-whitespace-element.1", SVG_TEXT_ELEMENT);
            jEdit.setProperty("xmlindenter.preserve-whitespace-element.2", SVG_TSPAN_ELEMENT);
        }
    }

    /**
     * Displays a user-friendly error message to go with the supplied exception.
     */
    static void processException(Exception e, String message, Component component) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        Log.log(Log.DEBUG, Thread.currentThread(), writer.toString());
        String msg = MessageFormat.format(jEdit.getProperty("xmlindenter.message.error"), new Object[] { message, e.getMessage() });
        JOptionPane.showMessageDialog(component, msg.toString());
    }

    static void showMessageDialog(String property, Component component) {
        String message = jEdit.getProperty(property);
        JOptionPane.showMessageDialog(component, message);
    }

    public static void toggleSplitAttributes(View view) {
        boolean split = jEdit.getBooleanProperty("xmlindenter.splitAttributes", false);
        jEdit.setBooleanProperty("xmlindenter.splitAttributes", !split);
        BeautyPlugin.beautify(view.getBuffer(), view, true);
    }

    /**
     * Indents XML in current buffer.
     * @param view
     */
    public static void indentXml(View view) {
        Buffer buffer = view.getBuffer();
        boolean indentWithTabs = getIndentWithTabs(buffer);
        int indentAmount = getIndentAmount(indentWithTabs, buffer);
        buffer.writeLock();
        buffer.beginCompoundEdit();
        try {
            int caretPosition = view.getTextArea().getCaretPosition();
            String inputString = buffer.getText(0, buffer.getLength());
            String resultString = XmlIndenterPlugin.indent(inputString, indentAmount, indentWithTabs);
            buffer.remove(0, buffer.getLength());
            buffer.insert(0, resultString);
            if (caretPosition > resultString.length() - 1) {
                caretPosition = resultString.length() - 1;
            } else {
                char c = resultString.charAt(caretPosition);
                while (caretPosition < buffer.getLength() && !(c == '>' || c == '<')) {
                    caretPosition++;
                    c = resultString.charAt(caretPosition);
                }
                if (c == '>') {
                    caretPosition++;
                }
            }
            if (caretPosition < 0) {
                caretPosition = 0;
            } else if (caretPosition > buffer.getLength() - 1) {
                caretPosition = buffer.getLength() - 1;
            }
            view.getTextArea().setCaretPosition(caretPosition);
        } catch (Exception e) {
            Log.log(Log.ERROR, IndentingTransformerImpl.class, e);
            String message = jEdit.getProperty("xmlindenter.indent.message.failure");
            XmlIndenterPlugin.processException(e, message, view);
        } finally {
            if (buffer.insideCompoundEdit()) {
                buffer.endCompoundEdit();
            }
            buffer.writeUnlock();
        }
    }

    private static boolean getIndentWithTabs(Buffer buffer) {
        boolean tabSizeAppropriate = buffer.getTabSize() <= buffer.getIndentSize();
        return !buffer.getBooleanProperty("noTabs") && tabSizeAppropriate;
    }

    private static int getIndentAmount(boolean indentWithTabs, Buffer buffer) {
        if (indentWithTabs) {
            return buffer.getIndentSize() / buffer.getTabSize();
        } else {
            return buffer.getIndentSize();
        }
    }

    protected static String indent(String inputString, int indentAmount, boolean indentWithTabs) throws Exception {
        List preserveWhitespaceList = getEnumeratedProperty("xmlindenter.preserve-whitespace-element");
        StringWriter writer = new StringWriter();
        TRANSFORMER.indentXml(inputString, writer, indentAmount, indentWithTabs, preserveWhitespaceList);
        String resultString = writer.toString();
        return resultString;
    }

    public static List getEnumeratedProperty(String key) {
        List<String> values = new ArrayList<String>();
        int i = 0;
        String value;
        while ((value = jEdit.getProperty(key + "." + i)) != null) {
            values.add(value);
            i++;
        }
        return values;
    }
}
