package org.softvfc.daq.trace;

import java.awt.GridLayout;
import java.util.Enumeration;
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 *
 * @author Diego Schmaedech Martins (schmaedech@gmail.com)
 * @version 29/07/2010
 */
public class FormatControls extends JPanel {

    private Vector<ButtonGroup> groups = new Vector<ButtonGroup>();

    private JToggleButton linrB, ulawB, alawB, rate8B, rate11B, rate16B, rate22B, rate44B;

    private JToggleButton size8B, size16B, signB, unsignB, litB, bigB, monoB, sterB;

    private AudioFormat defaultAudioFormat;

    public FormatControls() {
        setLayout(new GridLayout(0, 1));
        JPanel p1 = new JPanel();
        ButtonGroup encodingGroup = new ButtonGroup();
        linrB = addToggleButton(p1, encodingGroup, "linear", true);
        ulawB = addToggleButton(p1, encodingGroup, "ulaw", false);
        alawB = addToggleButton(p1, encodingGroup, "alaw", false);
        add(p1);
        groups.addElement(encodingGroup);
        JPanel p2 = new JPanel();
        JPanel p2b = new JPanel();
        ButtonGroup sampleRateGroup = new ButtonGroup();
        rate8B = addToggleButton(p2, sampleRateGroup, "8000", false);
        rate11B = addToggleButton(p2, sampleRateGroup, "11025", false);
        rate16B = addToggleButton(p2b, sampleRateGroup, "16000", false);
        rate22B = addToggleButton(p2b, sampleRateGroup, "22050", false);
        rate44B = addToggleButton(p2b, sampleRateGroup, "44100", true);
        add(p2);
        add(p2b);
        groups.addElement(sampleRateGroup);
        JPanel p3 = new JPanel();
        ButtonGroup sampleSizeInBitsGroup = new ButtonGroup();
        size8B = addToggleButton(p3, sampleSizeInBitsGroup, "8", false);
        size16B = addToggleButton(p3, sampleSizeInBitsGroup, "16", true);
        add(p3);
        groups.addElement(sampleSizeInBitsGroup);
        JPanel p4 = new JPanel();
        ButtonGroup signGroup = new ButtonGroup();
        signB = addToggleButton(p4, signGroup, "signed", true);
        unsignB = addToggleButton(p4, signGroup, "unsigned", false);
        add(p4);
        groups.addElement(signGroup);
        JPanel p5 = new JPanel();
        ButtonGroup endianGroup = new ButtonGroup();
        litB = addToggleButton(p5, endianGroup, "little endian", true);
        bigB = addToggleButton(p5, endianGroup, "big endian", false);
        add(p5);
        groups.addElement(endianGroup);
        JPanel p6 = new JPanel();
        ButtonGroup channelsGroup = new ButtonGroup();
        monoB = addToggleButton(p6, channelsGroup, "mono", false);
        sterB = addToggleButton(p6, channelsGroup, "stereo", true);
        add(p6);
        groups.addElement(channelsGroup);
        defaultAudioFormat = getFormat();
    }

    private JToggleButton addToggleButton(JPanel p, ButtonGroup g, String name, boolean state) {
        JToggleButton b = new JToggleButton(name, state);
        p.add(b);
        g.add(b);
        return b;
    }

    public AudioFormat getFormat() {
        Vector<String> v = new Vector<String>(groups.size());
        for (int i = 0; i < groups.size(); i++) {
            ButtonGroup g = groups.get(i);
            for (Enumeration e = g.getElements(); e.hasMoreElements(); ) {
                AbstractButton b = (AbstractButton) e.nextElement();
                if (b.isSelected()) {
                    v.add(b.getText());
                    break;
                }
            }
        }
        AudioFormat.Encoding encoding = AudioFormat.Encoding.ULAW;
        String encString = v.get(0);
        float rate = Float.valueOf(v.get(1)).floatValue();
        int sampleSize = Integer.valueOf(v.get(2)).intValue();
        String signedString = v.get(3);
        boolean bigEndian = (v.get(4)).startsWith("big");
        int channels = (v.get(5)).equals("mono") ? 1 : 2;
        if (encString.equals("linear")) {
            if (signedString.equals("signed")) {
                encoding = AudioFormat.Encoding.PCM_SIGNED;
            } else {
                encoding = AudioFormat.Encoding.PCM_UNSIGNED;
            }
        } else if (encString.equals("alaw")) {
            encoding = AudioFormat.Encoding.ALAW;
        }
        return new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize / 8) * channels, rate, bigEndian);
    }

    public void setFormat(AudioFormat format) {
        AudioFormat.Encoding type = format.getEncoding();
        if (type == AudioFormat.Encoding.ULAW) {
            ulawB.doClick();
        } else if (type == AudioFormat.Encoding.ALAW) {
            alawB.doClick();
        } else if (type == AudioFormat.Encoding.PCM_SIGNED) {
            linrB.doClick();
            signB.doClick();
        } else if (type == AudioFormat.Encoding.PCM_UNSIGNED) {
            linrB.doClick();
            unsignB.doClick();
        }
        float rate = format.getFrameRate();
        if (rate == 8000) {
            rate8B.doClick();
        } else if (rate == 11025) {
            rate11B.doClick();
        } else if (rate == 16000) {
            rate16B.doClick();
        } else if (rate == 22050) {
            rate22B.doClick();
        } else if (rate == 44100) {
            rate44B.doClick();
        }
        switch(format.getSampleSizeInBits()) {
            case 8:
                size8B.doClick();
                break;
            case 16:
                size16B.doClick();
                break;
        }
        if (format.isBigEndian()) {
            bigB.doClick();
        } else {
            litB.doClick();
        }
        if (format.getChannels() == 1) {
            monoB.doClick();
        } else {
            sterB.doClick();
        }
    }

    /**
     * @return the defaultAudioFormat
     */
    public AudioFormat getDefaultAudioFormat() {
        return defaultAudioFormat;
    }

    /**
     * @param defaultAudioFormat the defaultAudioFormat to set
     */
    public void setDefaultAudioFormat(AudioFormat defaultAudioFormat) {
        this.defaultAudioFormat = defaultAudioFormat;
    }
}
