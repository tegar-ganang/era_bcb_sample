package com.frinika.audio.analysis.gui;

import com.frinika.audio.io.LimitedAudioReader;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Observable;
import com.frinika.audio.analysis.SpectrogramDataListener;
import uk.org.toot.audio.core.AudioBuffer;

public class WaveImage extends Observable implements SpectrogramDataListener {

    private static final long serialVersionUID = 1L;

    int nChannel;

    private Image thumbNailImage = null;

    private LimitedAudioReader gin = null;

    private ThumbNailRunnable thumbNailRunnable;

    private double frameToScreen;

    int height = 100;

    Dimension rect;

    private float Fs;

    public WaveImage(LimitedAudioReader gin) {
        this.gin = gin;
        Fs = (float) gin.getSampleRate();
    }

    public Dimension getPreferredSize() {
        if (rect == null) return new Dimension(1000, height); else return rect;
    }

    private void reconstructThumbNail(Dimension rect) {
        if (thumbNailRunnable == null) {
            thumbNailRunnable = new ThumbNailRunnable();
            Thread t = new Thread(thumbNailRunnable);
            t.setPriority(Thread.MIN_PRIORITY);
            thumbNailRunnable.setThread(t);
            t.start();
        }
        thumbNailRunnable.reconstruct(rect);
    }

    class ThumbNailRunnable implements Runnable {

        Thread runThread;

        Graphics2D gg;

        public void reconstruct(Dimension rect1) {
            rect = (Dimension) rect1.clone();
            if (runThread.isInterrupted()) {
                System.out.println(" Thunmb nail thread already interupted");
                return;
            }
            runThread.interrupt();
        }

        public void setThread(Thread t) {
            runThread = t;
        }

        synchronized boolean buildThumbNail() {
            if (rect.getWidth() == 0) return true;
            thumbNailImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_BYTE_BINARY);
            gg = (Graphics2D) thumbNailImage.getGraphics();
            long nSamp = gin.getEnvelopedLengthInFrames();
            frameToScreen = rect.width / (double) nSamp;
            try {
                gin.seekEnvelopeStart(false);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            int nChannel = gin.getFormat().getChannels();
            long nFrame = gin.getEnvelopedLengthInFrames();
            int chunkSize = 1024;
            int nRead = 0;
            float valMax = 0;
            float valMin = 0;
            double scale = rect.height / 2.0;
            gg.setColor(Color.white);
            int midY = rect.height / 2;
            int pix = 0;
            int ii = 0;
            AudioBuffer buff = new AudioBuffer("WaveThumbnail", nChannel, chunkSize, Fs);
            System.out.println(" chunkSize,nFrame =" + chunkSize + " " + nFrame);
            while (nRead < nFrame) {
                if (runThread.isInterrupted()) {
                    System.out.println("Interupted .....");
                    return false;
                }
                int nn = chunkSize;
                buff.makeSilence();
                gin.processAudio(buff);
                nRead += chunkSize;
                float sampleLA[] = buff.getChannel(0);
                float sampleRA[];
                if (nChannel == 2) sampleRA = buff.getChannel(1); else sampleRA = buff.getChannel(0);
                assert (buff.getSampleCount() == chunkSize);
                for (int i = 0; i < chunkSize; i++, ii++) {
                    float sampleL = sampleLA[i];
                    float sampleR = sampleRA[i];
                    valMin = Math.min(Math.min(valMin, sampleL), sampleR);
                    valMax = Math.max(Math.max(valMax, sampleL), sampleR);
                    int pixNow = (int) (ii * frameToScreen);
                    if (pixNow > pix) {
                        gg.drawLine(pix, (int) (midY + valMin * scale), pix, (int) (midY + valMax * scale));
                        pix = pixNow;
                        valMax = valMin = 0;
                    }
                }
            }
            setChanged();
            notifyObservers();
            System.out.println(" BUILD DONE" + rect);
            return true;
        }

        public Image getImage() {
            return thumbNailImage;
        }

        public synchronized void run() {
            assert (runThread == Thread.currentThread());
            while (true) {
                try {
                    wait();
                } catch (InterruptedException e1) {
                    while (!buildThumbNail()) Thread.interrupted();
                }
            }
        }
    }

    public void notifySizeChange(Dimension d) {
        d.height = height;
        reconstructThumbNail(d);
    }

    public void drawImage(Graphics2D g, int i, int j) {
        if (thumbNailImage == null) return;
        g.setColor(Color.RED);
        g.fillRect(i, j, thumbNailImage.getWidth(null), thumbNailImage.getHeight(null));
        g.setXORMode(Color.WHITE);
        g.drawImage(thumbNailImage, i, j, null);
        g.setPaintMode();
    }

    public int getWidth() {
        if (rect == null) return 100;
        return rect.width;
    }

    public int getHeight() {
        return height;
    }

    public int frameToScreen(long frame) {
        return (int) (frameToScreen * frame);
    }

    public int screenToFrame(int p) {
        return (int) (p / frameToScreen);
    }

    public void notifyMoreDataReady() {
    }
}
