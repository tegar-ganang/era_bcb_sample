package org.softvfc.daq.trace;

import java.awt.*;
import java.awt.geom.Line2D;
import javax.swing.*;
import java.util.Vector;
import javax.sound.sampled.*;
import java.text.*;
import java.util.Date;
import java.util.Iterator;
import javax.swing.table.DefaultTableModel;
import org.softvfc.statistics.math.StdStats;

/**
 *
 * @author Diego Schmaedech Martins (schmaedech@gmail.com)
 * @version 29/07/2010
 */
public class SamplingGraph extends JPanel implements Runnable {

    private StdStats stats = new StdStats();

    private Thread thread;

    AudioInputStream audioInputStream;

    private DefaultTableModel samplingLeftTableModel;

    private DefaultTableModel samplingRightTableModel;

    final int bufSize = 16384;

    private Vector<Double> XLeftLimWave = new Vector<Double>();

    private Vector<Double> YLeftLimWave = new Vector<Double>();

    private Vector<Double> XRightLimWave = new Vector<Double>();

    private Vector<Double> YRightLimWave = new Vector<Double>();

    private Vector<Line2D> leftLine = new Vector<Line2D>();

    private Vector<Line2D> rightLine = new Vector<Line2D>();

    public double leftFrequencyMillisenconds = 0;

    public double leftBatimento = 0;

    public double leftHistoryFrequencyMillisenconds = 0;

    public double rightFrequencyMillisenconds = 0;

    public double rightBatimento = 0;

    public double rightHistoryFrequencyMillisenconds = 0;

    private Vector<Double> leftFreqCard = new Vector<Double>();

    private Vector<Double> leftBufFreqCard = new Vector<Double>();

    private Vector<Double> leftHistoryFreqCard = new Vector<Double>();

    private Vector<Double> rightFreqCard = new Vector<Double>();

    private Vector<Double> rightBufFreqCard = new Vector<Double>();

    private Vector<Double> rightHistoryFreqCard = new Vector<Double>();

    private final int BAT = 60000;

    private double leftBC = 0;

    private double rightBC = 0;

    private int limitmax = 0, limitmin = 0;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss");

    private int redLimit = 10;

    private int countLeft0s = 0;

    private int countRight0s = 0;

    private double duration;

    public SamplingGraph() {
        setBackground(new Color(0, 0, 0));
        initLeftTableModel();
        initRightTableModel();
    }

    private void initLeftTableModel() {
        setSamplingLeftTableModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] { "Timestamp", "Left RR Interval" }) {

            Class[] types = new Class[] { java.lang.String.class, java.lang.String.class };

            boolean[] canEdit = new boolean[] { false, false };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
    }

    private void initRightTableModel() {
        setSamplingRightTableModel(new javax.swing.table.DefaultTableModel(new Object[][] {}, new String[] { "Timestamp", "Right RR Interval" }) {

            Class[] types = new Class[] { java.lang.String.class, java.lang.String.class };

            boolean[] canEdit = new boolean[] { false, false };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
    }

    public void createWaveForm(byte[] audioBytes, AudioInputStream audioInputStream, double duration) {
        this.audioInputStream = audioInputStream;
        this.duration = duration;
        leftLine.removeAllElements();
        rightLine.removeAllElements();
        XLeftLimWave.add(0.0);
        YLeftLimWave.add(0.0);
        XRightLimWave.add(0.0);
        YRightLimWave.add(0.0);
        AudioFormat format = audioInputStream.getFormat();
        if (audioBytes == null) {
            try {
                audioBytes = new byte[(int) (audioInputStream.getFrameLength() * format.getFrameSize())];
                audioInputStream.read(audioBytes);
            } catch (Exception ex) {
                return;
            }
        }
        Dimension d = getSize();
        int w = d.width;
        int h = d.height;
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
        int frames_per_pixel = 0;
        try {
            frames_per_pixel = audioBytes.length / format.getFrameSize() / w;
        } catch (Exception ex) {
            ex.toString();
            return;
        }
        byte my_left_byte = 0;
        byte my_right_byte = 0;
        double y_left_last = 0;
        double y_right_last = 0;
        int numChannels = format.getChannels();
        for (double x = 0; x < w && audioData != null; x++) {
            int idx = (int) (frames_per_pixel * numChannels * x);
            if (format.getSampleSizeInBits() == 8) {
                my_left_byte = (byte) audioData[idx + 1];
                my_right_byte = (byte) audioData[idx];
            } else {
                my_left_byte = (byte) (128 * audioData[idx + 1] / format.getSampleRate());
                my_right_byte = (byte) (128 * audioData[idx] / format.getSampleRate());
            }
            double y_left_new = (double) (h * (128 - my_left_byte) / (256));
            double y_right_new = (double) (h * (128 - my_right_byte) / (256));
            leftLine.add(new Line2D.Double(x, y_left_last, x, y_left_new));
            rightLine.add(new Line2D.Double(x, y_right_last, x, y_right_new));
            y_left_last = y_left_new;
            y_right_last = y_right_new;
            if (y_left_new > limitmax || y_left_new < limitmin) {
                XLeftLimWave.add((x / w) * duration);
                YLeftLimWave.add(y_left_new);
            }
            if (y_right_new > limitmax || y_right_new < limitmin) {
                XRightLimWave.add((x / w) * duration);
                YRightLimWave.add(y_right_new);
            }
        }
        analiseLeftWaveForm(XLeftLimWave, YLeftLimWave);
        XLeftLimWave.removeAllElements();
        YLeftLimWave.removeAllElements();
        analiseRightWaveForm(XRightLimWave, YRightLimWave);
        XRightLimWave.removeAllElements();
        YRightLimWave.removeAllElements();
        repaint();
    }

    @SuppressWarnings("static-access")
    public void analiseLeftWaveForm(Vector tempo, Vector amplitude) {
        Vector<String> samplingLeftLine = new Vector<String>();
        double outArray[] = new double[bufSize * 2];
        double txtArray[] = new double[leftHistoryFreqCard.size()];
        Date date = new Date();
        int interact = 0;
        for (Iterator ite = tempo.iterator(); ite.hasNext(); ) {
            double output = (Double) ite.next();
            outArray[interact++] = output;
        }
        interact = 0;
        for (Iterator ite = leftHistoryFreqCard.iterator(); ite.hasNext(); ) {
            double output = (Double) ite.next();
            txtArray[interact++] = output;
        }
        if (stats.max(outArray) < 1) {
            countLeft0s++;
        } else {
            try {
                this.leftFrequencyMillisenconds = (countLeft0s * duration + stats.max(outArray) + leftBufFreqCard.lastElement());
                this.leftBatimento = BAT / leftFrequencyMillisenconds;
                this.leftHistoryFrequencyMillisenconds = leftHistoryFreqCard.lastElement();
                if (Math.abs(Math.round(leftFrequencyMillisenconds)) < 300) {
                    leftFrequencyMillisenconds = Math.abs(Math.round((leftFrequencyMillisenconds + leftHistoryFrequencyMillisenconds)));
                } else {
                    if (Math.abs(Math.round(leftFrequencyMillisenconds - leftHistoryFrequencyMillisenconds)) > 300) {
                        if (leftFrequencyMillisenconds > leftHistoryFrequencyMillisenconds) {
                            leftFrequencyMillisenconds = Math.abs(Math.round((leftFrequencyMillisenconds + leftHistoryFrequencyMillisenconds) / 3));
                            setLeftBC(BAT / Math.abs(Math.round(leftFrequencyMillisenconds)));
                            samplingLeftLine.add(sdf.format(date));
                            samplingLeftLine.add(String.valueOf(Math.abs(Math.round(leftFrequencyMillisenconds))));
                        } else {
                            setLeftBC(BAT / Math.abs(Math.round(leftFrequencyMillisenconds)));
                            samplingLeftLine.add(sdf.format(date));
                            samplingLeftLine.add(String.valueOf(Math.abs(Math.round(leftFrequencyMillisenconds))));
                        }
                    } else {
                        setLeftBC(Math.abs(BAT / Math.round(leftHistoryFrequencyMillisenconds)));
                        samplingLeftLine.add(sdf.format(date));
                        samplingLeftLine.add(String.valueOf(Math.abs(Math.round(leftHistoryFrequencyMillisenconds))));
                    }
                }
                if (samplingLeftLine.size() == 2) {
                    getSamplingLeftTableModel().addRow(samplingLeftLine);
                }
            } catch (Exception ex) {
            }
            countLeft0s = 0;
            leftHistoryFreqCard.add(leftFrequencyMillisenconds);
            this.leftFrequencyMillisenconds = 0;
            leftFreqCard.add(stats.max(outArray));
            leftBufFreqCard.add(duration - stats.max(outArray));
        }
    }

    @SuppressWarnings("static-access")
    public void analiseRightWaveForm(Vector tempo, Vector amplitude) {
        Vector<String> samplingRightLine = new Vector<String>();
        double outArray[] = new double[bufSize * 2];
        double txtArray[] = new double[rightHistoryFreqCard.size()];
        Date date = new Date();
        int interact = 0;
        for (Iterator ite = tempo.iterator(); ite.hasNext(); ) {
            double output = (Double) ite.next();
            outArray[interact++] = output;
        }
        interact = 0;
        for (Iterator ite = rightHistoryFreqCard.iterator(); ite.hasNext(); ) {
            double output = (Double) ite.next();
            txtArray[interact++] = output;
        }
        if (stats.max(outArray) < 1) {
            countRight0s++;
        } else {
            try {
                this.rightFrequencyMillisenconds = (countRight0s * duration + stats.max(outArray) + rightBufFreqCard.lastElement());
                this.rightBatimento = BAT / rightFrequencyMillisenconds;
                this.rightHistoryFrequencyMillisenconds = rightHistoryFreqCard.lastElement();
                if (Math.abs(Math.round(rightFrequencyMillisenconds)) < 300) {
                    rightFrequencyMillisenconds = Math.abs(Math.round((rightFrequencyMillisenconds + rightHistoryFrequencyMillisenconds)));
                } else {
                    if (Math.abs(Math.round(rightFrequencyMillisenconds - rightHistoryFrequencyMillisenconds)) > 300) {
                        if (rightFrequencyMillisenconds > rightHistoryFrequencyMillisenconds) {
                            rightFrequencyMillisenconds = Math.abs(Math.round((rightFrequencyMillisenconds + rightHistoryFrequencyMillisenconds) / 3));
                            setRightBC(BAT / Math.abs(Math.round(rightFrequencyMillisenconds)));
                            samplingRightLine.add(sdf.format(date));
                            samplingRightLine.add(String.valueOf(Math.abs(Math.round(rightFrequencyMillisenconds))));
                        } else {
                            setRightBC(BAT / Math.abs(Math.round(rightFrequencyMillisenconds)));
                            samplingRightLine.add(sdf.format(date));
                            samplingRightLine.add(String.valueOf(Math.abs(Math.round(rightFrequencyMillisenconds))));
                        }
                    } else {
                        setRightBC(BAT / Math.abs(Math.round(rightHistoryFrequencyMillisenconds)));
                        samplingRightLine.add(sdf.format(date));
                        samplingRightLine.add(String.valueOf(Math.abs(Math.round(rightHistoryFrequencyMillisenconds))));
                    }
                }
                if (samplingRightLine.size() == 2) {
                    getSamplingRightTableModel().addRow(samplingRightLine);
                }
            } catch (Exception ex) {
            }
            countRight0s = 0;
            rightHistoryFreqCard.add(rightFrequencyMillisenconds);
            this.rightFrequencyMillisenconds = 0;
            rightFreqCard.add(stats.max(outArray));
            rightBufFreqCard.add(duration - stats.max(outArray));
        }
    }

    @Override
    public void paint(Graphics g) {
        Dimension d = getSize();
        int w = d.width;
        int h = d.height;
        limitmax = ((h) / 2) + ((h) / getRedLimit());
        limitmin = ((h) / 2) - ((h) / getRedLimit());
        Graphics2D g2 = (Graphics2D) g;
        g2.setBackground(getBackground());
        g2.clearRect(0, 0, w, h);
        if (audioInputStream != null) {
            try {
                for (int i = 1; i < leftLine.size(); i++) {
                    g2.setColor(Color.GREEN);
                    g2.draw(leftLine.get(i));
                }
                for (int i = 1; i < rightLine.size(); i++) {
                    g2.setColor(Color.BLUE);
                    g2.draw(rightLine.get(i));
                }
                g2.setColor(Color.red);
                g2.setStroke(new BasicStroke(2));
                g2.draw(new Line2D.Double(0, limitmin, w, limitmin));
                g2.setStroke(new BasicStroke(2));
                g2.draw(new Line2D.Double(0, limitmax, w, limitmax));
            } catch (Exception ex) {
            }
        }
    }

    public void start() {
        thread = new Thread(this);
        thread.setName("samplinggraph");
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }
        thread = null;
    }

    @SuppressWarnings("static-access")
    @Override
    public void run() {
        repaint();
    }

    public int getRedLimit() {
        return redLimit;
    }

    public void setRedLimit(int redLimit) {
        this.redLimit = redLimit;
        this.repaint();
    }

    /**
     * @return the leftBC
     */
    public double getLeftBC() {
        return leftBC;
    }

    /**
     * @param leftBC the leftBC to set
     */
    public void setLeftBC(double leftBC) {
        this.leftBC = leftBC;
    }

    /**
     * @return the rightBC
     */
    public double getRightBC() {
        return rightBC;
    }

    /**
     * @param rightBC the rightBC to set
     */
    public void setRightBC(double rightBC) {
        this.rightBC = rightBC;
    }

    /**
     * @return the samplingLeftTableModel
     */
    public DefaultTableModel getSamplingLeftTableModel() {
        return samplingLeftTableModel;
    }

    /**
     * @param samplingLeftTableModel the samplingLeftTableModel to set
     */
    public void setSamplingLeftTableModel(DefaultTableModel samplingLeftTableModel) {
        this.samplingLeftTableModel = samplingLeftTableModel;
    }

    /**
     * @return the samplingRightTableModel
     */
    public DefaultTableModel getSamplingRightTableModel() {
        return samplingRightTableModel;
    }

    /**
     * @param samplingRightTableModel the samplingRightTableModel to set
     */
    public void setSamplingRightTableModel(DefaultTableModel samplingRightTableModel) {
        this.samplingRightTableModel = samplingRightTableModel;
    }
}
