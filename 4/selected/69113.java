package ch.laoe.clip;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import ch.laoe.operation.AOToolkit;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.LProgressViewer;
import ch.laoe.ui.GToolkit;

/***********************************************************

This file is part of LAoE.

LAoE is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation; either version 2 of the License,
or (at your option) any later version.

LAoE is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LAoE; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


Class:			AChannelPlotterSpectrogram
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	channel view, x-axis = time, y-axis = frequency
					color-darkness = magnitude, spectrogramm-view.



History:
Date:			Description:									Autor:
07.03.02		first draft										oli4

***********************************************************/
public class AChannelPlotterSpectrogram extends AChannelPlotter {

    /**
	* constructor
	*/
    public AChannelPlotterSpectrogram(AModel m, AChannelPlotter p) {
        super(m, p);
    }

    public double getAutoscaleXOffset() {
        return 0;
    }

    public double getAutoscaleXLength() {
        return getChannelModel().getSampleLength();
    }

    public float getAutoscaleYOffset(int xOffset, int xLength) {
        return 0;
    }

    public float getAutoscaleYLength(int xOffset, int xLength) {
        return ((AClip) getChannelModel().getParent().getParent()).getSampleRate() / 2;
    }

    protected float getValidYOffset() {
        return 0;
    }

    protected float getValidYLength() {
        return ((AClip) getChannelModel().getParent().getParent()).getSampleRate() / 2;
    }

    private static int fftLength = 1024;

    public static void setFftLength(int l) {
        fftLength = l;
    }

    public static int getFftLength() {
        return fftLength;
    }

    private static int windowType = AOToolkit.GAUSSIAN_WINDOW;

    public static void setWindowType(int w) {
        windowType = w;
    }

    public static int getWindowType() {
        return windowType;
    }

    private MMArray re = null, im = null;

    private String oldId = "";

    private int oldWindowType = 0;

    private int oldFftLength = 0;

    private MMArray cacheMag = null;

    private float magMin, magMax, magRange, magMean;

    private int gy[], sy[];

    /**
    * cache is reloaded when the clip ID has changed only. 
    */
    private final void reloadCache() {
        AChannel ch = getChannelModel();
        if (!oldId.equals(ch.getChangeId()) || oldWindowType != windowType || oldFftLength != fftLength) {
            int k = 0;
            LProgressViewer.getInstance().entrySubProgress(GLanguage.translate("reloadCache") + " " + model.getName());
            LProgressViewer.getInstance().entrySubProgress(0.9);
            oldId = ch.getChangeId();
            oldWindowType = windowType;
            oldFftLength = fftLength;
            Debug.println(7, "reload cache of channel spectrogram plotter name=" + ch.getName() + " id=" + ch.getChangeId());
            MMArray s = getChannelModel().getSamples();
            int bl = s.getLength() / fftLength;
            if (cacheMag == null) {
                cacheMag = new MMArray(s.getLength() / 2, 0);
            }
            if (cacheMag.getLength() != s.getLength() / 2) {
                cacheMag.setLength(s.getLength() / 2);
            }
            magMin = Float.MAX_VALUE;
            magMax = Float.MIN_VALUE;
            if (re == null) {
                re = new MMArray(fftLength, 0);
            }
            if (re.getLength() != fftLength) {
                re.setLength(fftLength);
            }
            if (im == null) {
                im = new MMArray(fftLength, 0);
            }
            if (im.getLength() != fftLength) {
                im.setLength(fftLength);
            }
            for (int i = 0; i < bl + 1; i++) {
                LProgressViewer.getInstance().setProgress((i + 1) / bl + 1);
                int ii = i * fftLength / 2;
                re.copy(s, ii * 2, 0, fftLength);
                im.clear();
                AOToolkit.applyFFTWindow(windowType, re, fftLength);
                AOToolkit.complexFft(re, im);
                for (int j = 0; j < fftLength / 2; j++) {
                    float m = AOToolkit.cartesianToMagnitude(re.get(j), im.get(j));
                    cacheMag.set(ii + j, m);
                    magMin = Math.min(magMin, m);
                    magMax = Math.max(magMax, m);
                    magMean += m;
                    k++;
                }
            }
            magRange = Math.max(magMax - magMin, 0.0000001f);
            magMean /= k;
            magMean = (magMean - magMin) / magRange;
            LProgressViewer.getInstance().exitSubProgress();
            LProgressViewer.getInstance().exitSubProgress();
        }
    }

    public void paintSamples(Graphics2D g2d, Color color, float colorGamma) {
        try {
            int cnt = 0;
            long t0 = System.currentTimeMillis();
            reloadCache();
            AChannel ch = getChannelModel();
            AClip clip = (AClip) ch.getParent().getParent();
            int sampleWidth = 1 << (clip.getSampleWidth() - 1);
            float samplerate = clip.getSampleRate();
            int fftLengthHalf = fftLength / 2;
            float samplerateHalf = samplerate / 2;
            final int gResolution = 2;
            int dgx = Math.max(gResolution, sampleToGraphX(fftLength) - sampleToGraphX(0));
            int dgy = Math.max(gResolution, sampleToGraphY(samplerate / fftLength) - sampleToGraphY(0));
            int rx = rectangle.width / dgx;
            int ry = rectangle.height / dgy;
            g2d.setClip(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            g2d.setColor(color);
            if (gy == null || gy.length != ry) {
                gy = new int[ry];
            }
            if (sy == null || sy.length != ry) {
                sy = new int[ry];
            }
            for (int j = 0; j < ry; j++) {
                gy[j] = rectangle.y + j * dgy;
                sy[j] = (int) graphToSampleY(gy[j]);
            }
            for (int i = 0; i < rx; i++) {
                int gx = rectangle.x + i * dgx;
                int sx = (int) graphToSampleX(gx);
                if (sx < 0) {
                    continue;
                }
                if (sx > ch.getSampleLength()) {
                    continue;
                }
                for (int j = 0; j < ry; j++) {
                    int syj = sy[j];
                    int gyj = gy[j];
                    if (syj < 0) {
                        break;
                    }
                    if (syj > samplerateHalf) {
                        continue;
                    }
                    float mag = cacheMag.get((int) (sx / fftLength * fftLengthHalf + syj * fftLengthHalf / samplerateHalf));
                    mag = (mag - magMin) / magRange;
                    float g = (float) Math.exp(Math.log(mag) * colorGamma);
                    float s = (float) (1.0 / (1.0 + Math.exp((magMean * colorGamma - mag) * 1000 / colorGamma)));
                    s = s * g;
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, s));
                    g2d.fillRect(gx, gyj, dgx, dgy);
                }
            }
            System.out.println("paint spectrogram time = " + (System.currentTimeMillis() - t0) + "ms cnt=" + cnt + " fftLength=" + fftLength);
        } catch (Exception e) {
            Debug.printStackTrace(5, e);
        }
    }
}
