package dsp.voc;

import dsp.voc.VocInputOutputFilterDialog;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.lang.*;
import java.math.*;
import javax.*;

/**
 * @author Sir Costy
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class VocInputDialog extends JDialog {

    private JLabel FormatLabel, FileLabel, label, Properties;

    private long sampleRate;

    private int sampleSize;

    private int channels;

    private File ReadFile;

    private DataInputStream myInputStream;

    private boolean FileChoosed;

    private JLabel SampleRateLabel, SampleSizeLabel, ChannelLabel;

    private JTextField FileName, SampleRateValue, SampleSizeValue;

    private JComboBox ChanelNumberComboBox;

    private JButton browse, ok, cancel;

    private boolean okeyed;

    public VocInputDialog(Dialog parent) throws HeadlessException {
        super(parent, "VOC INPUT DIALOG", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        okeyed = false;
        FileChoosed = false;
        Container pane = getContentPane();
        JPanel UPpanel = new JPanel();
        UPpanel.setLayout(new GridBagLayout());
        UPpanel.setBorder(new TitledBorder(new EtchedBorder(), "File Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        FileLabel = new JLabel("Voc File:  ");
        UPpanel.add(FileLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        label = new JLabel("                                       ");
        UPpanel.add(label, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        FileName = new JTextField();
        FileName.setText("No selected file");
        FileName.setEnabled(false);
        UPpanel.add(FileName, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        FormatLabel = new JLabel("Voc Properties:    ");
        FormatLabel.setFont(new Font("gigi", 0, 23));
        UPpanel.add(FormatLabel, gbc);
        gbc.gridx = 1;
        Properties = new JLabel("");
        Properties.setFont(new Font("gigi", 0, 14));
        UPpanel.add(Properties, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        SampleRateLabel = new JLabel("Sample Rate found:");
        UPpanel.add(SampleRateLabel, gbc);
        gbc.gridx = 1;
        SampleRateValue = new JTextField("0");
        SampleRateValue.setEnabled(false);
        UPpanel.add(SampleRateValue, gbc);
        gbc.gridy = 4;
        gbc.gridx = 0;
        SampleSizeLabel = new JLabel("Sample Size found:");
        UPpanel.add(SampleSizeLabel, gbc);
        gbc.gridx = 1;
        SampleSizeValue = new JTextField("0");
        SampleSizeValue.setEnabled(false);
        UPpanel.add(SampleSizeValue, gbc);
        gbc.gridy = 5;
        gbc.gridx = 0;
        ChannelLabel = new JLabel("Pick the channel:");
        UPpanel.add(ChannelLabel, gbc);
        gbc.gridx = 1;
        String[] channels = { "CH:" };
        ChanelNumberComboBox = new JComboBox(channels);
        ChanelNumberComboBox.setEnabled(false);
        ChanelNumberComboBox.setSelectedIndex(0);
        UPpanel.add(ChanelNumberComboBox, gbc);
        gbc.gridy = 6;
        gbc.gridx = 0;
        JLabel Extra = new JLabel("");
        UPpanel.add(Extra, gbc);
        ok = new JButton(new OKAction());
        cancel = new JButton(new CancelAction());
        browse = new JButton(new BrowseAction());
        JPanel buttonPane = new JPanel();
        buttonPane.add(ok);
        buttonPane.add(cancel);
        buttonPane.add(browse);
        pane.add(UPpanel);
        pane.add(buttonPane, BorderLayout.SOUTH);
        browse.setAction(new BrowseAction());
        setLocation(300, 70);
        setResizable(false);
        doPack();
    }

    private class BrowseAction extends AbstractAction {

        public BrowseAction() {
            super("Browse");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (ReadHeader() != false) FileChoosed = true; else {
                System.out.println("Folder selected or bad extension file!");
                FileChoosed = false;
            }
        }
    }

    public boolean ReadHeader() {
        JFileChooser chooser = new JFileChooser();
        VocInputOutputFilterDialog filter = new VocInputOutputFilterDialog();
        filter.addExtension("voc");
        filter.setDescription("Creative Voice File");
        chooser.setFileFilter(filter);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int returnVal = chooser.showOpenDialog(new Frame("Open Voc File"));
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            System.out.println("You chose to open this file: " + chooser.getSelectedFile().getName());
            FileName.setText(chooser.getSelectedFile().getName());
            ReadFile = chooser.getSelectedFile();
            if (StartReadHeader()) {
                doPack();
                return true;
            }
        } else {
            SampleRateValue.setText("0");
            SampleSizeValue.setText("0");
            ChanelNumberComboBox.removeAllItems();
            ChanelNumberComboBox.addItem("CH:");
            ChanelNumberComboBox.setEnabled(false);
            FileName.setText("No selected file");
            Properties.setText("");
            doPack();
            return false;
        }
        return false;
    }

    public boolean StartReadHeader() {
        try {
            int timeConstant = 0;
            byte bType;
            myInputStream = new DataInputStream(new FileInputStream(ReadFile));
            myInputStream.skipBytes(20);
            byte[] a = new byte[2];
            myInputStream.read(a);
            byte[] aa = { a[1], a[0] };
            myInputStream.skipBytes((new BigInteger(1, aa)).intValue() - 22);
            bType = (byte) myInputStream.readUnsignedByte();
            switch(bType) {
                case 1:
                    {
                        myInputStream.skipBytes(3);
                        timeConstant = myInputStream.readUnsignedByte();
                        System.out.println("time ct " + timeConstant);
                        sampleSize = 8;
                        channels = 1;
                        sampleRate = 1000000 / (256 - (timeConstant % 256));
                        SampleRateValue.setText(String.valueOf(sampleRate));
                        SampleSizeValue.setText(String.valueOf(sampleSize));
                        ChanelNumberComboBox.removeAllItems();
                        ChanelNumberComboBox.addItem("CH: 1");
                        ChanelNumberComboBox.setEnabled(true);
                        Properties.setText("Mono, 8 Bits/sample");
                        break;
                    }
                case 8:
                    {
                        myInputStream.skipBytes(3);
                        byte[] b = new byte[2];
                        myInputStream.read(b);
                        byte[] bb = { b[1], b[0] };
                        BigInteger Bi = (new BigInteger(1, bb)).shiftRight(8);
                        timeConstant = Bi.intValue();
                        myInputStream.skipBytes(1);
                        channels = myInputStream.readUnsignedByte();
                        sampleSize = 8;
                        channels++;
                        sampleRate = 500000 / (256 - (timeConstant % 256));
                        SampleRateValue.setText(String.valueOf(sampleRate));
                        SampleSizeValue.setText(String.valueOf(sampleSize));
                        ChanelNumberComboBox.removeAllItems();
                        ChanelNumberComboBox.addItem("CH: 1");
                        ChanelNumberComboBox.addItem("CH: 2");
                        ChanelNumberComboBox.addItem("CH: Mux");
                        ChanelNumberComboBox.setEnabled(true);
                        Properties.setText("Stereo, 8 Biti/sample");
                        break;
                    }
                case 9:
                    {
                        myInputStream.skip(3);
                        byte[] b = new byte[4];
                        myInputStream.read(b);
                        byte[] bb = { b[3], b[2], b[1], b[0] };
                        sampleRate = (new BigInteger(1, bb)).intValue();
                        sampleSize = myInputStream.read();
                        channels = myInputStream.read();
                        ChanelNumberComboBox.removeAllItems();
                        ChanelNumberComboBox.addItem("CH: 1");
                        Properties.setText("Mono, 16 Biti/sample");
                        if (channels == 2) {
                            sampleRate /= 2;
                            ChanelNumberComboBox.addItem("CH: 2");
                            ChanelNumberComboBox.addItem("CH: Mux");
                            Properties.setText("Stereo, 16 Biti/sample");
                        }
                        ChanelNumberComboBox.setEnabled(true);
                        SampleRateValue.setText(String.valueOf(sampleRate));
                        SampleSizeValue.setText(String.valueOf(sampleSize));
                        break;
                    }
            }
        } catch (Exception e) {
            System.out.println("Unable to read file! File Bad or Corupted");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private class CancelAction extends AbstractAction {

        public CancelAction() {
            super("Cancel");
        }

        public void actionPerformed(ActionEvent arg0) {
            dispose();
        }
    }

    private class OKAction extends AbstractAction {

        public OKAction() {
            super("OK");
        }

        public void actionPerformed(ActionEvent arg0) {
            if (FileChoosed) okeyed = true;
            dispose();
        }
    }

    public boolean isOK() {
        return okeyed;
    }

    public long getSampleRate() {
        return sampleRate;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public int getSel_Channel() {
        return ChanelNumberComboBox.getSelectedIndex();
    }

    public int getChannels() {
        return channels;
    }

    public File getFile() {
        return ReadFile;
    }

    public void doPack() {
        pack();
    }
}
