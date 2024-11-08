package org.jsresources.apps.jam.audio;

import java.awt.Component;
import javax.swing.JList;
import javax.swing.DefaultListCellRenderer;
import javax.swing.UIManager;
import javax.sound.sampled.AudioFormat;
import org.jsresources.apps.jam.Debug;

public class AudioFormatListCellRenderer extends DefaultListCellRenderer {

    public AudioFormatListCellRenderer() {
        super();
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        setComponentOrientation(list.getComponentOrientation());
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        setIcon(null);
        AudioFormat format = (AudioFormat) value;
        String text = "" + ((int) format.getSampleRate()) + " Hz, " + format.getSampleSizeInBits() + " bit " + (format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED ? "signed, " : "unsigned, ") + (format.getChannels() == 2 ? "stereo" : "mono");
        setText(text);
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setBorder((cellHasFocus) ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
        return this;
    }
}
