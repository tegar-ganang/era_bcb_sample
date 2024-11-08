package org.jsresources.apps.jam.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;
import org.jsresources.apps.jam.Debug;
import org.tritonus.share.sampled.FloatSampleBuffer;

/**
 */
public class SimpleWaveformDisplay extends JPanel {

    private float[] m_anDisplayData;

    private int m_nWidth;

    public SimpleWaveformDisplay(int nWidth, int nHeight) {
        m_nWidth = nWidth;
        Dimension dim = new Dimension(nWidth, nHeight);
        setMinimumSize(dim);
        setMaximumSize(dim);
        setPreferredSize(dim);
        m_anDisplayData = new float[nWidth];
        setForeground(Color.GREEN);
        setBackground(Color.BLACK);
    }

    /** Data are assumed to be 16 bit.
	 */
    public void setData(int[] anData) {
        int nSamplesPerPixel = anData.length / m_nWidth;
        for (int i = 0; i < m_nWidth; i++) {
            float nValue = 0.0f;
            for (int j = 0; j < nSamplesPerPixel; j++) {
                nValue += (float) (Math.abs(anData[i * nSamplesPerPixel + j]) / 65536.0f);
            }
            nValue /= nSamplesPerPixel;
            m_anDisplayData[i] = nValue;
        }
        repaint();
    }

    public void setData(FloatSampleBuffer fsb) {
        int length = 0;
        if (m_nWidth > 0 && fsb != null) {
            if (fsb.getChannelCount() > 0) {
                length = fsb.getSampleCount();
            }
        }
        if (length > 0) {
            int nSamplesPerPixel = length / m_nWidth;
            float data[] = fsb.getChannel(0);
            for (int i = 0; i < m_nWidth; i++) {
                float value = 0;
                for (int j = 0; j < nSamplesPerPixel; j++) {
                    value += Math.abs(data[i * nSamplesPerPixel + j]);
                }
                value /= nSamplesPerPixel;
                m_anDisplayData[i] = value;
            }
        } else {
            for (int i = 0; i < m_nWidth; i++) {
                m_anDisplayData[i] = 0.0f;
            }
        }
        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int nHeight = getHeight();
        for (int i = 0; i < m_nWidth; i++) {
            int value = (int) (m_anDisplayData[i] * nHeight);
            int y1 = (nHeight - 2 * value) / 2;
            int y2 = y1 + 2 * value;
            g.drawLine(i, y1, i, y2);
        }
    }
}
