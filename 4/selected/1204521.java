package wand.channelControl;

import java.awt.Color;
import javax.swing.Scrollable;
import java.awt.GridLayout;
import java.awt.Insets;
import wand.ChannelFrame;
import wand.genericChannel.*;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.Rectangle;
import java.awt.Dimension;

public class TextInput extends JPanel implements Scrollable {

    JTextArea textPunchField = new JTextArea("", 5, 11);

    Insets inset = new Insets(5, 5, 5, 5);

    private static final int STRING_LINES = 20;

    String[] s;

    private int channelID;

    private GenericChannel channel;

    public void setChannelID(int chID) {
        channelID = chID;
        channel = ChannelFrame.channelGridPanel.channels[channelID];
        loadChannelsTextIntoInputPanel();
    }

    public int getChannelID() {
        return channelID;
    }

    public void loadChannelsTextIntoInputPanel() {
        String string = "";
        String[] array = channel.getStringArray();
        String line;
        for (int i = 0; i < array.length; i++) {
            line = array[i];
            if (line != null) string = string + array[i] + "\n";
        }
        setTextAreaInput(string);
    }

    public TextInput() {
        setBackground(Color.gray);
        setLayout(new GridLayout(1, 1));
        textPunchField.setMargin(inset);
        textPunchField.setLineWrap(true);
        add(textPunchField);
        textPunchField.getDocument().addDocumentListener(new MyDocumentListener());
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        int i = 6;
        return i;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        int i = 6;
        return i;
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    class MyDocumentListener implements DocumentListener {

        public void insertUpdate(DocumentEvent e) {
            updateInputString();
        }

        public void removeUpdate(DocumentEvent e) {
            updateInputString();
        }

        public void changedUpdate(DocumentEvent e) {
            updateInputString();
        }
    }

    public void updateInputString() {
        s = textPunchField.getText().split("\n");
        channel.setStringArray(s);
        channel.setText(s);
        channel.getChannelDisplay().repaint();
        channel.getOutputPreviewPanel().repaint();
        channel.getOutputDisplayPanel().repaint();
    }

    public void setTextAreaInput(String text) {
        textPunchField.setText(text);
        updateInputString();
    }
}
