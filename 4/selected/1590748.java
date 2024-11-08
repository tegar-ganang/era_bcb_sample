package dsp.soundinput;

import javax.swing.JDialog;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

/**
 * @author canti
 *  
 */
public class SoundInputDialog extends JDialog {

    private float sampleRate;

    private int sampleSize;

    private int channels;

    private boolean bigEndian;

    private boolean signed;

    private boolean okeyed;

    private JComboBox sampleRateComboBox, sampleSizeComboBox, channelNumberComboBox;

    private JButton ok, cancel;

    private JCheckBox signedCheckBox, bigEndianCheckBox;

    JPanel backPanel;

    public SoundInputDialog(Dialog parent) throws HeadlessException {
        super(parent, "SoundInputFactory", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initialize();
        setLocation(150, 150);
        setTitle("Input from SoundCard");
    }

    private void initialize() {
        okeyed = false;
        backPanel = new JPanel();
        backPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        String[] sampleRates = { "8000", "11025", "22050", "44100" };
        sampleRateComboBox = new JComboBox(sampleRates);
        String[] sampleSizes = { "8", "16" };
        sampleSizeComboBox = new JComboBox(sampleSizes);
        sampleSizeComboBox.addActionListener(new SetSampleSizeAction());
        String[] channels = { "1", "2" };
        channelNumberComboBox = new JComboBox(channels);
        channelNumberComboBox.setSelectedIndex(0);
        signedCheckBox = new JCheckBox("");
        signedCheckBox.setSelected(true);
        bigEndianCheckBox = new JCheckBox("");
        bigEndianCheckBox.setSelected(true);
        bigEndianCheckBox.setEnabled(false);
        ok = new JButton(new OKAction());
        cancel = new JButton(new CancelAction());
        backPanel.setLayout(new GridLayout(1, 2));
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new GridLayout(6, 3));
        ((GridLayout) rightPanel.getLayout()).setVgap(5);
        ((GridLayout) rightPanel.getLayout()).setHgap(5);
        rightPanel.add(new JLabel("SampleRate:"));
        rightPanel.add(sampleRateComboBox);
        rightPanel.add(new JLabel("SampleSize (bits):"));
        rightPanel.add(sampleSizeComboBox);
        rightPanel.add(new JLabel("Channel number:"));
        rightPanel.add(channelNumberComboBox);
        rightPanel.add(new JLabel("Signed/Unsigned:"));
        rightPanel.add(signedCheckBox);
        rightPanel.add(new JLabel("Big/Little Endian:"));
        rightPanel.add(bigEndianCheckBox);
        rightPanel.add(ok);
        rightPanel.add(cancel);
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(new JLabel(createImageIcon("images/microfon.JPG", "Sound Input/Output")), BorderLayout.CENTER);
        backPanel.add(leftPanel);
        backPanel.add(rightPanel);
        getContentPane().add(backPanel);
        pack();
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected static ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = SoundInputDialog.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    /**
     * @author canti, 22.11.2004
     *  
     */
    private class CancelAction extends AbstractAction {

        public CancelAction() {
            super("Cancel");
        }

        public void actionPerformed(ActionEvent arg0) {
            dispose();
        }
    }

    /**
     * @author canti, 22.11.2004
     *  
     */
    private class OKAction extends AbstractAction {

        public OKAction() {
            super("OK");
        }

        public void actionPerformed(ActionEvent arg0) {
            sampleRate = Float.parseFloat((String) sampleRateComboBox.getSelectedItem());
            sampleSize = Integer.parseInt((String) sampleSizeComboBox.getSelectedItem());
            channels = Integer.parseInt((String) channelNumberComboBox.getSelectedItem());
            signed = signedCheckBox.isSelected();
            bigEndian = bigEndianCheckBox.isSelected();
            okeyed = true;
            dispose();
        }
    }

    private class SetSampleSizeAction extends AbstractAction {

        public void actionPerformed(ActionEvent arg0) {
            bigEndianCheckBox.setEnabled(Integer.parseInt((String) sampleSizeComboBox.getSelectedItem()) > 8);
        }
    }

    public boolean isOK() {
        return okeyed;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    public int getChannels() {
        return channels;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public boolean isSigned() {
        return signed;
    }
}
