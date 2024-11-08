package com.google.code.b0rx0r.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;
import org.tritonus.share.sampled.FloatSampleBuffer;

/**
 */
public class SimpleWaveformDisplay extends JPanel {

    private float[][] m_anDisplayData;

    private int m_nWidth;

    public static void calculate(FloatSampleBuffer fsb, float[][] buffer, int start, int length) {
        int width = buffer[0].length;
        if (length > 0) {
            int nSamplesPerPixel = length / width;
            for (int c = 0; c < fsb.getChannelCount(); c++) {
                float data[] = fsb.getChannel(c);
                for (int i = 0; i < width; i++) {
                    float value = 0;
                    for (int j = 0; j < nSamplesPerPixel; j++) {
                        value += Math.abs(data[start + i * nSamplesPerPixel + j]);
                    }
                    value /= nSamplesPerPixel;
                    buffer[c][i] = value;
                }
            }
        } else {
            for (int c = 0; c < fsb.getChannelCount(); c++) {
                for (int i = 0; i < width; i++) {
                    buffer[c][i] = 0.0f;
                }
            }
        }
    }

    public static void calculate(FloatSampleBuffer fsb, float[][] buffer) {
        int width = buffer.length;
        int length = 0;
        if (width > 0 && fsb != null) {
            if (fsb.getChannelCount() > 0) {
                length = fsb.getSampleCount();
            }
        }
        calculate(fsb, buffer, 0, length);
    }

    public static float[][] calculate(FloatSampleBuffer fsb, int width, int height) {
        float[][] retVal = new float[fsb.getChannelCount()][width];
        calculate(fsb, retVal);
        return retVal;
    }

    public SimpleWaveformDisplay(int nWidth, int nHeight) {
        m_nWidth = nWidth;
        Dimension dim = new Dimension(nWidth, nHeight);
        setMinimumSize(dim);
        setMaximumSize(dim);
        setPreferredSize(dim);
        m_anDisplayData = new float[1][nWidth];
    }

    public void setData(FloatSampleBuffer fsb, int start, int length) {
        m_anDisplayData = new float[fsb.getChannelCount()][m_nWidth];
        calculate(fsb, m_anDisplayData, start, length);
        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int nHeight = getHeight() / m_anDisplayData.length;
        for (int c = 0; c < m_anDisplayData.length; c++) {
            for (int i = 0; i < m_nWidth; i++) {
                int value = (int) (m_anDisplayData[c][i] * nHeight);
                int yOffset = nHeight * c;
                int y1 = yOffset + ((nHeight - 2 * value) / 2);
                int y2 = y1 + 2 * value;
                g.drawLine(i, y1, i, y2);
            }
        }
    }

    public void setWidth(int width) {
        m_nWidth = width;
    }
}
