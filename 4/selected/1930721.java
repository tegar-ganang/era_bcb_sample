package net.sourceforge.musole.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JPanel;

/**
 * Render a WaveForm.
 */
public class SamplingGraph extends JPanel {

    private static final long serialVersionUID = 9221077930968301698L;

    private AudioInputStream audioInputStream;

    private Vector lines;

    private double seconds;

    private double duration;

    private String fileName;

    private Font font12 = new Font("serif", Font.PLAIN, 12);

    Color jfcBlue = new Color(204, 204, 255);

    Color pink = new Color(255, 175, 175);

    public SamplingGraph(File file) {
        lines = new Vector();
        this.fileName = file.getName();
        try {
            audioInputStream = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        setBackground(new Color(20, 20, 20));
        long sizeInBytes = file.length();
        setPreferredSize(new Dimension((int) (sizeInBytes / 44100) * 100, SamplePanel.HEIGHT - 15));
        createWaveForm(null);
        repaint();
        revalidate();
    }

    public void createWaveForm(byte[] audioBytes) {
        lines.removeAllElements();
        AudioFormat format = audioInputStream.getFormat();
        if (audioBytes == null) {
            try {
                audioBytes = new byte[(int) (audioInputStream.getFrameLength() * format.getFrameSize())];
                audioInputStream.read(audioBytes);
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        }
        Dimension d = getPreferredSize();
        int w = d.width;
        int h = d.height - 15;
        int[] audioData = null;
        if (format.getSampleSizeInBits() == 16) {
            int nlengthInSamples = audioBytes.length / 2;
            audioData = new int[nlengthInSamples];
            if (format.isBigEndian()) {
                for (int i = 0; i < nlengthInSamples; i++) {
                    int MSB = (int) audioBytes[2 * i];
                    int LSB = (int) audioBytes[2 * i + 1];
                    audioData[i] = MSB << 8 | (255 & LSB);
                }
            } else {
                for (int i = 0; i < nlengthInSamples; i++) {
                    int LSB = (int) audioBytes[2 * i];
                    int MSB = (int) audioBytes[2 * i + 1];
                    audioData[i] = MSB << 8 | (255 & LSB);
                }
            }
        } else if (format.getSampleSizeInBits() == 8) {
            int nlengthInSamples = audioBytes.length;
            audioData = new int[nlengthInSamples];
            if (format.getEncoding().toString().startsWith("PCM_SIGN")) {
                for (int i = 0; i < audioBytes.length; i++) {
                    audioData[i] = audioBytes[i];
                }
            } else {
                for (int i = 0; i < audioBytes.length; i++) {
                    audioData[i] = audioBytes[i] - 128;
                }
            }
        }
        int frames_per_pixel = audioBytes.length / format.getFrameSize() / w;
        byte my_byte = 0;
        double y_last = 0;
        int numChannels = format.getChannels();
        for (double x = 0; x < w && audioData != null; x++) {
            int idx = (int) (frames_per_pixel * numChannels * x);
            if (format.getSampleSizeInBits() == 8) {
                my_byte = (byte) audioData[idx];
            } else {
                my_byte = (byte) (128 * audioData[idx] / 32768);
            }
            double y_new = (double) (h * (128 - my_byte) / 256);
            lines.add(new Line2D.Double(x, y_last, x, y_new));
            y_last = y_new;
        }
        repaint();
    }

    public void paint(Graphics g) {
        Dimension d = getSize();
        int w = d.width;
        int h = d.height;
        int INFOPAD = 15;
        Graphics2D g2 = (Graphics2D) g;
        g2.setBackground(getBackground());
        g2.clearRect(0, 0, w, h);
        g2.setColor(Color.white);
        g2.fillRect(0, h - INFOPAD, w, INFOPAD);
        g2.setColor(Color.black);
        g2.setFont(font12);
        g2.drawString(fileName, 3, h - 4);
        if (audioInputStream != null) {
            g2.setColor(jfcBlue);
            for (int i = 1; i < lines.size(); i++) {
                g2.draw((Line2D) lines.get(i));
            }
        }
    }
}
