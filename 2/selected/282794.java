package net.ontopia.topicmaps.viz;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.ontopia.utils.StreamUtils;

/**
 * Parses menu configuration files.
 */
public class MenuFileParser {

    private String source;

    Map enabled;

    int lineIndex;

    boolean succeeded;

    /**
   * Create new from a given file.
   */
    public MenuFileParser(String fileString) {
        try {
            source = readFile(fileString);
            VizDebugUtils.debug(source);
        } catch (IOException e) {
            source = null;
            WarningBox warningBox = new WarningBox(Messages.getString("Viz.FileNotFoundColon") + fileString);
            warningBox.setVisible(true);
        }
    }

    public void warn(String warning) {
        String args[] = new String[1];
        args[0] = String.valueOf(lineIndex);
        WarningBox warningBox = new WarningBox(Messages.getString("Viz.ErrorInLine", args) + warning);
        warningBox.setVisible(true);
    }

    public ParsedMenuFile parse() {
        if (source == null) return null;
        enabled = new HashMap();
        lineIndex = 1;
        try {
            while (parseLine()) lineIndex++;
            return new ParsedMenuFile(enabled);
        } catch (MenuFileParseError e) {
            warn(e.getMessage());
            return new ParsedMenuFile(null);
        }
    }

    /**
   * @return true iff there are more lines to parse *after* the current line.
   */
    private boolean parseLine() throws MenuFileParseError {
        int index = source.indexOf('\n');
        if (index == -1) {
            if (source.length() > 0) parseLine(source);
            return false;
        }
        parseLine(source.substring(0, index));
        if (source.length() == index + 1) return false;
        source = source.substring(index + 1);
        return true;
    }

    private void parseLine(String startLine) throws MenuFileParseError {
        String line = startLine;
        VizDebugUtils.debug("Parsing line: " + line);
        int hashIndex = line.indexOf('#');
        if (hashIndex != -1) line = line.substring(0, hashIndex);
        line = line.trim();
        VizDebugUtils.debug("trimmed line: " + line);
        if (line.length() == 0) return;
        String args[] = new String[1];
        args[0] = startLine;
        int eqIndex = line.indexOf('=');
        if (eqIndex == -1) throw new MenuFileParseError(Messages.getString("Viz.MissingEqualsMessage", startLine));
        if (eqIndex == 0) throw new MenuFileParseError(Messages.getString("Viz.MissingNameMessage", startLine));
        if (eqIndex == line.length() - 1) throw new MenuFileParseError(Messages.getString("Viz.MissingOnOffMessage", startLine));
        String name = line.substring(0, eqIndex);
        VizDebugUtils.debug("name: " + name);
        validateName(name);
        String value = line.substring(eqIndex + 1);
        VizDebugUtils.debug("value: " + value);
        validateValue(value);
        enabled.put(name, value.equals("on") ? Boolean.TRUE : Boolean.FALSE);
    }

    private void validateName(String name) throws MenuFileParseError {
        if (!loweralpha(name.charAt(0))) throw new MenuFileParseError(Messages.getString("Viz.NameCannotStartPeriod"));
        if (!loweralpha(name.charAt(name.length() - 1))) throw new MenuFileParseError(Messages.getString("Viz.NameCannotEndPeriod"));
        int index = 0;
        while (index < name.length()) {
            char currentChar = name.charAt(index);
            if (!(loweralpha(currentChar) || currentChar == '.')) throw new MenuFileParseError(Messages.getString("Viz.NameOnlyAZ"));
            index++;
        }
    }

    private boolean loweralpha(char c) {
        return 'a' <= c && c <= 'z';
    }

    private void validateValue(String value) throws MenuFileParseError {
        if (!(value.equals("on") || value.equals("off"))) throw new MenuFileParseError("The value \"" + value + "\" is not on the form \"on\" or \"off\"");
    }

    private String readFile(String urlFileString) throws IOException {
        URL url = new URL(urlFileString);
        VizDebugUtils.debug("url: " + url.toString());
        InputStream stream = url.openStream();
        Reader reader = new InputStreamReader(stream, "iso-8859-1");
        StringWriter writer = new StringWriter();
        StreamUtils.transfer(reader, writer);
        stream.close();
        return writer.toString();
    }

    /**
   * Generates a box with a warning message and an Ok button.
   */
    private class WarningBox extends JFrame {

        public WarningBox(String warning) {
            JPanel warningPanel = new JPanel();
            warningPanel.setLayout(new BoxLayout(warningPanel, BoxLayout.Y_AXIS));
            warningPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Messages.getString("Viz.Warning")));
            JTextArea warningTextArea = new JTextArea(warning);
            warningTextArea.setLineWrap(true);
            warningTextArea.setWrapStyleWord(true);
            warningTextArea.setEditable(false);
            warningTextArea.setFont(warningTextArea.getFont().deriveFont(13f));
            warningTextArea.setBackground(warningPanel.getBackground());
            warningPanel.add(warningTextArea);
            ActionListener okListener = new ActionListener() {

                public void actionPerformed(ActionEvent action) {
                    setVisible(false);
                }
            };
            JButton okButton = new JButton(Messages.getString("Viz.OK"));
            okButton.addActionListener(okListener);
            warningPanel.add(okButton);
            getContentPane().add(warningPanel);
            setSize(400, 150);
        }
    }

    /**
   * Indicates a parse or syntax error in MenuFileParser.
   */
    public class MenuFileParseError extends Exception {

        String message;

        public MenuFileParseError(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
