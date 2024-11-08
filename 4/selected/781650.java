package ch.laoe.operation;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.MMArray;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.LProgressViewer;

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


Class:			AOToolkit
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	this toolkit contains common procedures,
					math and algorithms.

History:
Date:			Description:									Autor:
21.04.01		first draft										oli4

***********************************************************/
public class AOToolkit {

    /**
	 *	real to dB
	 */
    public static float todB(float r) {
        return (float) Math.max((8.685890 * Math.log(Math.abs(r))), -200);
    }

    /**
	 *	dB to real
	 */
    public static float fromdB(float dB) {
        return (float) (Math.exp(dB / 8.685890));
    }

    private static final double halfToneFactor = 1. / Math.log(Math.pow(2., 1. / 12.));

    /**
	 *	real to halftone
	 */
    public static float toHalfTone(float r) {
        return (float) (halfToneFactor * Math.log(Math.abs(r)));
    }

    /**
	 *	halftone to real
	 */
    public static float fromHalfTone(float halfTone) {
        return (float) (Math.exp(halfTone / halfToneFactor));
    }

    private static final double octaveFactor = 1. / Math.log(2);

    /**
	 *	real to octave
	 */
    public static float toOctave(float r) {
        return (float) (octaveFactor * Math.log(Math.abs(r)));
    }

    /**
	 *	octave to real
	 */
    public static float fromOctave(float octave) {
        return (float) (Math.exp(octave / octaveFactor));
    }

    /**
	 *	zeroth order interpolation
	 *	@param data seen as circular buffer when array out of bounds
	 */
    public static float interpolate0(MMArray data, float index) {
        return data.get(((int) index) % data.getLength());
    }

    /**
    * first order interpolation
    * @param data seen as circular buffer when array out of bounds
    */
    public static float interpolate1(MMArray data, float index) {
        int ip = ((int) index);
        float fp = index - ip;
        int l = data.getLength();
        return data.get(ip % l) * (1 - fp) + data.get((ip + 1) % l) * fp;
    }

    /**
    * first order interpolation
    * @param data seen as circular buffer when array out of bounds
    */
    public static float interpolate1(float data[], float index) {
        int ip = ((int) index);
        float fp = index - ip;
        int l = data.length;
        return data[ip % l] * (1 - fp) + data[(ip + 1) % l] * fp;
    }

    /**
	 *	second order interpolation 
	 *	@param data seen as circular buffer when array out of bounds
	 */
    public static float interpolate2(MMArray data, float index) {
        int ip = ((int) index);
        float fp = index - ip;
        int l = data.getLength();
        float d0 = data.get(ip % l);
        float d1 = data.get((ip + 1) % l);
        float d2 = data.get((ip + 2) % l);
        float a0 = d0;
        float a1 = d1 - d0;
        float a2 = (d2 - d1 - a1) / 2;
        return a0 + a1 * fp + a2 * fp * (fp - 1);
    }

    /**
	 *	third order interpolation 
	 *	@param data seen as circular buffer when array out of bounds
	 */
    public static float interpolate3(MMArray data, float index) {
        int ip = (int) index;
        float fp = index - ip;
        int l = data.getLength();
        float dm1 = data.get((ip - 1) % l);
        float d0 = data.get(ip % l);
        float d1 = data.get((ip + 1) % l);
        float d2 = data.get((ip + 2) % l);
        float a = (3 * (d0 - d1) - dm1 + d2) / 2;
        float b = 2 * d1 + dm1 - (5 * d0 + d2) / 2;
        float c = (d1 - dm1) / 2;
        return (((a * fp) + b) * fp + c) * fp + data.get(ip % l);
    }

    /**
	 *	zeroth order interpolation. the x-values must be ordered in rising manner.
	 *	x- and y-length must be equal. 
	 */
    public static float interpolate0(MMArray x, MMArray y, float index) {
        if (index < x.get(0)) {
            return y.get(0);
        }
        for (int i = 1; i < x.getLength(); i++) {
            if (x.get(i) > index) {
                return y.get(i - 1);
            }
        }
        return y.get(y.getLength() - 1);
    }

    /**
    * first order interpolation. the x-values must be ordered in rising manner.
    * x- and y-length must be equal. 
    */
    public static float interpolate1(MMArray x, MMArray y, float index) {
        float dx, dy;
        if (index < x.get(0)) {
            return y.get(0);
        }
        for (int i = 1; i < x.getLength(); i++) {
            if (x.get(i) > index) {
                dx = x.get(i) - x.get(i - 1);
                dy = y.get(i) - y.get(i - 1);
                return y.get(i - 1) + ((index - x.get(i - 1)) / dx * dy);
            }
        }
        return y.get(y.getLength() - 1);
    }

    /**
    * first order interpolation. the x-values must be ordered in rising manner.
    * x- and y-length must be equal. 
    */
    public static float interpolate1(float x[], float y[], float index) {
        float dx, dy;
        if (index < x[0]) {
            return y[0];
        }
        for (int i = 1; i < x.length; i++) {
            if (x[i] > index) {
                dx = x[i] - x[i - 1];
                dy = y[i] - y[i - 1];
                return y[i - 1] + ((index - x[i - 1]) / dx * dy);
            }
        }
        return y[y.length - 1];
    }

    /**
	 *	second order interpolation. the x-values must be ordered in rising manner.
	 *	x- and y-length must be equal. 
	 */
    public static float interpolate2(MMArray x, MMArray y, float index) {
        int i = 0;
        for (i = x.getLength() - 1; i >= 0; i--) {
            if (x.get(i) < index) break;
        }
        if (i > x.getLength() - 3) {
            i = x.getLength() - 3;
        }
        if (i < 0) {
            i = 0;
        }
        float x0, x1, x2, y0, y1, y2, a10, a11, a20;
        x0 = x1 = x2 = y0 = y1 = y2 = a10 = a11 = a20 = 0;
        x0 = x.get(i);
        x1 = x.get(i + 1);
        x2 = x.get(i + 2);
        y0 = y.get(i);
        y1 = y.get(i + 1);
        y2 = y.get(i + 2);
        a10 = (y1 - y0) / (x1 - x0);
        a11 = (y2 - y1) / (x2 - x1);
        a20 = (a11 - a10) / (x2 - x0);
        return y0 + a10 * (index - x0) + a20 * (index - x0) * (index - x1);
    }

    /**
	 *	third order interpolation. the x-values must be ordered in rising manner.
	 *	x- and y-length must be equal.
	 */
    public static float interpolate3(MMArray x, MMArray y, float index) {
        int i = 0;
        for (i = x.getLength() - 1; i >= 0; i--) {
            if (x.get(i) < index) break;
        }
        i--;
        if (i > x.getLength() - 4) {
            i = x.getLength() - 4;
        }
        if (i < 0) {
            i = 0;
        }
        float x0, x1, x2, x3, y0, y1, y2, y3, a10, a11, a12, a20, a21, a30;
        x0 = x1 = x2 = x3 = y0 = y1 = y2 = y3 = a10 = a11 = a12 = a20 = a21 = a30 = 0;
        x0 = x.get(i);
        x1 = x.get(i + 1);
        x2 = x.get(i + 2);
        x3 = x.get(i + 3);
        y0 = y.get(i);
        y1 = y.get(i + 1);
        y2 = y.get(i + 2);
        y3 = y.get(i + 3);
        a10 = (y1 - y0) / (x1 - x0);
        a11 = (y2 - y1) / (x2 - x1);
        a12 = (y3 - y2) / (x3 - x2);
        a20 = (a11 - a10) / (x2 - x0);
        a21 = (a12 - a11) / (x3 - x1);
        a30 = (a21 - a20) / (x3 - x0);
        return y0 + a10 * (index - x0) + a20 * (index - x0) * (index - x1) + a30 * (index - x0) * (index - x1) * (index - x2);
    }

    public static AOSpline createSpline() {
        return new AOSpline();
    }

    /**
	 *	convolution
	 */
    public static float convolve(MMArray data, int index, MMArray kernel, int m) {
        float y = 0;
        for (int i = 0; i < m; i++) {
            y += data.get(index - i) * kernel.get(i);
        }
        return y;
    }

    /**
	 *	creates a lowpass filter kernel. WARNING: right-to-left processing
	 *	required.
	 */
    public static void setLowPassKernel(MMArray kernel, int m, float fc) {
        int m2 = m / 2;
        float summ = 0;
        for (int i = 0; i < m; i++) {
            if (i == m2) {
                kernel.set(i, 2 * (float) Math.PI * fc);
            } else {
                kernel.set(i, (float) Math.sin(2 * (float) Math.PI * fc * (i - m2)) / (i - m2));
            }
            kernel.set(i, (float) (kernel.get(i) * (0.42 - 0.5 * Math.cos(2 * Math.PI * i / m) + 0.08 * Math.cos(4 * Math.PI * i / m))));
            summ += kernel.get(i);
        }
        for (int i = 0; i < m; i++) {
            kernel.set(i, kernel.get(i) / summ);
        }
    }

    /**
	 *	creates a highpass filter kernel. WARNING: right-to-left processing
	 *	required. this kernel doesn't work correctly yet!!!
	 */
    public static void setHighPassKernel(MMArray kernel, int m, float fc) {
        int m2 = m / 2;
        float summ = 0;
        for (int i = 0; i < m; i++) {
            if (i != m2) {
                kernel.set(i, (float) Math.sin(2 * (float) Math.PI * fc * (i - m2)) / (i - m2));
                kernel.set(i, (float) (kernel.get(i) * (0.42 - 0.5 * Math.cos(2 * Math.PI * i / m) + 0.08 * Math.cos(4 * Math.PI * i / m))));
                summ += kernel.get(i);
            }
        }
        for (int i = 0; i < m; i++) {
            kernel.set(i, kernel.get(i) / -summ);
        }
        kernel.set(m2, 1);
    }

    /**
	 *	add a band of x to y. offset is considered in x only.
    */
    public static void addIirBandPass(MMArray x, MMArray y, int o, int l, float f, float q, float gain) {
        float theta = (float) (2. * Math.PI * f);
        float beta = (float) (0.5 * (1. - Math.tan(theta / (2. * q))) / (1. + Math.tan(theta / (2. * q))));
        float alpha = (float) ((0.5 - beta) / 2);
        float gamma = (float) ((0.5 + beta) * Math.cos(theta));
        float x0, x1, x2;
        x0 = x1 = x2 = 0;
        float y0, y1, y2;
        y0 = y1 = y2 = 0;
        for (int i = o; i < (o + l); i++) {
            x2 = x1;
            x1 = x0;
            x0 = x.get(i);
            y2 = y1;
            y1 = y0;
            y0 = 2 * (alpha * (x0 - x2) + gamma * y1 - beta * y2);
            y.set(i - o, y.get(i - o) + gain * y0);
        }
    }

    /**
	 *	processes high pass to data.
	 */
    public static void setIirHighPass(MMArray data, int o, int l, float dry, float wet, float freq) {
        float omega = (float) (2. * Math.PI * freq);
        float cs = (float) Math.cos(omega);
        float alpha = (float) (Math.sin(omega) / 2.);
        float a0 = 1 + alpha;
        float b0 = (1 + cs) / 2 / a0;
        float b1 = -(1 + cs) / a0;
        float b2 = b0;
        float a1 = -2 * cs / a0;
        float a2 = (1 - alpha) / a0;
        float x0, x1, x2;
        x0 = x1 = x2 = 0;
        float y0, y1, y2;
        y0 = y1 = y2 = 0;
        for (int i = o; i < (o + l); i++) {
            x2 = x1;
            x1 = x0;
            x0 = data.get(i);
            y2 = y1;
            y1 = y0;
            y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            data.set(i, data.get(i) * dry + y0 * wet);
        }
    }

    /**
	 *	processes low pass to data.
	 */
    public static void setIirLowPass(MMArray data, int o, int l, float dry, float wet, float freq) {
        float omega = (float) (2. * Math.PI * freq);
        float cs = (float) Math.cos(omega);
        float alpha = (float) (Math.sin(omega) / 2.);
        float a0 = 1 + alpha;
        float b0 = (1 - cs) / 2 / a0;
        float b1 = (1 - cs) / a0;
        float b2 = b0;
        float a1 = -2 * cs / a0;
        float a2 = (1 - alpha) / a0;
        float x0, x1, x2;
        x0 = x1 = x2 = 0;
        float y0, y1, y2;
        y0 = y1 = y2 = 0;
        for (int i = o; i < (o + l); i++) {
            x2 = x1;
            x1 = x0;
            x0 = data.get(i);
            y2 = y1;
            y1 = y0;
            y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            data.set(i, data.get(i) * dry + y0 * wet);
        }
    }

    /**
	 *	processes notch filter to data.
	 */
    public static void setIirNotch(MMArray data, int o, int l, float freq, float q, float gain) {
        float omega = (float) (2. * Math.PI * freq);
        float cs = (float) Math.cos(omega);
        float alpha = (float) (Math.sin(omega) / (2 * q));
        float a0 = 1 + alpha;
        float b0 = 1 / a0;
        float b1 = -2 * cs / a0;
        float b2 = b0;
        float a1 = -2 * cs / a0;
        float a2 = (1 - alpha) / a0;
        float x0, x1, x2;
        x0 = x1 = x2 = 0;
        float y0, y1, y2;
        y0 = y1 = y2 = 0;
        for (int i = o; i < (o + l); i++) {
            x2 = x1;
            x1 = x0;
            x0 = data.get(i);
            y2 = y1;
            y1 = y0;
            y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            data.set(i, y0 * gain);
        }
    }

    /**
	 *	performs complex FFT
	 */
    public static void complexFft(MMArray re, MMArray im) {
        baseFft(false, re, im);
    }

    /**
	 *	performs complex IFFT
	 */
    public static void complexIfft(MMArray re, MMArray im) {
        baseFft(true, re, im);
    }

    /**
	 *	performs complex FFT/IFFT
	 */
    private static void baseFft(boolean isInvers, MMArray re, MMArray im) {
        int n = re.getLength();
        float scale = (float) Math.sqrt(1.0f / n);
        int sign;
        if (isInvers) {
            sign = -1;
        } else {
            sign = 1;
        }
        int i, j;
        for (i = j = 0; i < n; ++i) {
            if (j >= i) {
                float tempr = re.get(j) * scale;
                float tempi = im.get(j) * scale;
                re.set(j, re.get(i) * scale);
                im.set(j, im.get(i) * scale);
                re.set(i, tempr);
                im.set(i, tempi);
            }
            int m = n / 2;
            while (m >= 1 && j >= m) {
                j -= m;
                m /= 2;
            }
            j += m;
        }
        int mmax, istep;
        for (mmax = 1, istep = 2 * mmax; mmax < n; mmax = istep, istep = 2 * mmax) {
            float delta = sign * ((float) Math.PI) / mmax;
            for (int m = 0; m < mmax; ++m) {
                float w = m * delta;
                float wr = (float) Math.cos(w);
                float wi = (float) Math.sin(w);
                for (i = m; i < n; i += istep) {
                    j = i + mmax;
                    float rej = re.get(j);
                    float imj = im.get(j);
                    float tr = wr * rej - wi * imj;
                    float ti = wr * imj + wi * rej;
                    float rei = re.get(i);
                    float imi = im.get(i);
                    re.set(j, rei - tr);
                    im.set(j, imi - ti);
                    re.set(i, rei + tr);
                    im.set(i, imi + ti);
                }
            }
            mmax = istep;
        }
        if (isInvers) {
        } else {
            int n2 = n / 2;
            for (int k = 0; k < n2; ++k) {
                re.set(k, re.get(k) * 2);
                im.set(k, im.get(k) * 2);
            }
            for (int k = n2; k < n; ++k) {
                re.set(k, 0);
                im.set(k, 0);
            }
        }
    }

    /**
	 *	performs power spectrum FFT
	 */
    public static void powerFft(MMArray tRe, MMArray pRe) {
        throw new NotImplementedException();
    }

    public static final int RECTANGULAR_WINDOW = 0;

    public static final int HANNING_WINDOW = 1;

    public static final int HAMMING_WINDOW = 2;

    public static final int GAUSSIAN_WINDOW = 3;

    public static final int BLACKMAN_WINDOW = 4;

    public static final int FLATTOP_WINDOW = 5;

    public static final int BARTLETT_WINDOW = 6;

    public static final String[] getFFTWindowNames() {
        return new String[] { GLanguage.translate("rectangular"), GLanguage.translate("hanning"), GLanguage.translate("hamming"), GLanguage.translate("gaussian"), GLanguage.translate("blackman"), GLanguage.translate("flattop"), GLanguage.translate("bartlett") };
    }

    public static void applyFFTWindow(int windowType, MMArray data, int width) {
        switch(windowType) {
            case RECTANGULAR_WINDOW:
                applyRectangularWindow(data, width);
                break;
            case HANNING_WINDOW:
                applyHanningWindow(data, width);
                break;
            case HAMMING_WINDOW:
                applyHammingWindow(data, width);
                break;
            case GAUSSIAN_WINDOW:
                applyGaussianWindow(data, width);
                break;
            case BLACKMAN_WINDOW:
                applyBlackmanWindow(data, width);
                break;
            case FLATTOP_WINDOW:
                applyFlattopWindow(data, width);
                break;
            case BARTLETT_WINDOW:
                applyBartlettWindow(data, width);
                break;
        }
    }

    /**
	 *	applies a window to the given data
	 */
    private static void applyRectangularWindow(MMArray data, int width) {
        for (int i = 0; i < data.getLength(); i++) {
            if (i < width) {
            } else {
                data.set(i, 0);
            }
        }
    }

    /**
    * applies a window to the given data
    */
    private static void applyHanningWindow(MMArray data, int width) {
        for (int i = 0; i < data.getLength(); i++) {
            if (i < width) {
                data.set(i, (float) (data.get(i) * 0.5 * (1 - Math.cos(2 * Math.PI * i / width))));
            } else {
                data.set(i, 0);
            }
        }
    }

    /**
    * applies a window to the given data
    */
    private static void applyHammingWindow(MMArray data, int width) {
        for (int i = 0; i < data.getLength(); i++) {
            if (i < width) {
                data.set(i, (float) (data.get(i) * (0.54 - 0.46 * Math.cos(2 * Math.PI * i / width))));
            } else {
                data.set(i, 0);
            }
        }
    }

    /**
    * applies a window to the given data
    */
    private static void applyGaussianWindow(MMArray data, int width) {
        for (int i = 0; i < data.getLength(); i++) {
            if (i < width) {
                data.set(i, (float) (data.get(i) * Math.exp(-0.5 * Math.pow((i - width / 2) / (0.4 * width / 2), 2))));
            } else {
                data.set(i, 0);
            }
        }
    }

    /**
	 *	applies a window to the given data
	 */
    private static void applyBlackmanWindow(MMArray data, int width) {
        for (int i = 0; i < data.getLength(); i++) {
            if (i < width) {
                double a = 2 * Math.PI * i / width;
                data.set(i, data.get(i) * (float) (0.42 - 0.5 * Math.cos(a) + 0.08 * Math.cos(2 * a)));
            } else {
                data.set(i, 0);
            }
        }
    }

    /**
    * applies a window to the given data
    */
    private static void applyFlattopWindow(MMArray data, int width) {
        for (int i = 0; i < data.getLength(); i++) {
            if (i < width) {
                double a = 2 * Math.PI * i / width;
                data.set(i, data.get(i) * (float) (1 - 1.93 * Math.cos(a) + 1.29 * Math.cos(2 * a) - 0.388 * Math.cos(3 * a) + 0.032 * Math.cos(4 * a)));
            } else {
                data.set(i, 0);
            }
        }
    }

    /**
    * applies a window to the given data
    */
    private static void applyBartlettWindow(MMArray data, int width) {
        for (int i = 0; i < data.getLength(); i++) {
            if (i < width) {
                data.set(i, data.get(i) * (float) (2.0 / width * (width / 2.0 - Math.abs(i - width / 2))));
            } else {
                data.set(i, 0);
            }
        }
    }

    /**
    * performs autocorrelation
    */
    public void autoCorrelation(MMArray source, MMArray result) {
        throw new NotImplementedException();
    }

    /**
	 *	 average
	 */
    public static float average(MMArray s, int o, int l) {
        float a = 0;
        for (int i = 0; i < l; i++) {
            a += s.get(o + i);
        }
        a /= l;
        return a;
    }

    public static float average(float s[], int o, int l) {
        float a = 0;
        for (int i = 0; i < l; i++) {
            a += s[o + i];
        }
        a /= l;
        return a;
    }

    /**
	 *	calculates and returns the RMS average of the given sample range, without modifying s. 
	 *
	 */
    public static float rmsAverage(MMArray s, int o, int l) {
        float a = 0;
        for (int i = 0; i < l; i++) {
            a += s.get(o + i) * s.get(o + i);
        }
        a = (float) Math.sqrt(a / l);
        return a;
    }

    public static float rmsAverage(float s[], int o, int l) {
        float a = 0;
        for (int i = 0; i < l; i++) {
            a += s[o + i] * s[o + i];
        }
        a = (float) Math.sqrt(a / l);
        return a;
    }

    /**
	 *	moving average
	 */
    public static float movingAverage(float average, float newData, float weight) {
        return ((average * weight) + newData) / (weight + 1);
    }

    /**
	 *	moving RMS average
	 */
    public static float movingRmsAverage(float average, float newData, float weight) {
        return (float) Math.sqrt(((average * average * weight) + (newData * newData)) / (weight + 1));
    }

    /**
	 *	smooth using mean algorithm
	 */
    public static void smooth(MMArray s, int offset, int length, float weight) {
        float av = s.get(offset);
        for (int i = offset; i < offset + length; i++) {
            av = AOToolkit.movingAverage(av, s.get(i), weight);
            s.set(i, av);
        }
        av = s.get(offset + length - 1);
        for (int i = offset + length - 1; i >= offset; i--) {
            av = AOToolkit.movingAverage(av, s.get(i), weight);
            s.set(i, av);
        }
    }

    /**
	 *	smooth using RMS algorithm
	 */
    public static void smoothRms(MMArray s, int offset, int length, float weight) {
        float av = s.get(offset);
        for (int i = offset; i < offset + length; i++) {
            av = AOToolkit.movingRmsAverage(av, s.get(i), weight);
            s.set(i, av);
        }
        av = s.get(offset + length - 1);
        for (int i = offset + length - 1; i >= offset; i--) {
            av = AOToolkit.movingRmsAverage(av, s.get(i), weight);
            s.set(i, av);
        }
    }

    /**
	 *	return the index of the most positive peak
	 */
    public static int getPositivePeakIndex(MMArray s, int offset, int length) {
        float p = Float.MIN_VALUE;
        int index = 0;
        for (int i = 0; i < length; i++) {
            if (s.get(offset + i) > p) {
                p = s.get(offset + i);
                index = offset + i;
            }
        }
        return index;
    }

    /**
	 *	return the index of the most negative peak
	 */
    public static int getNegativePeakIndex(MMArray s, int offset, int length) {
        float p = Float.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < length; i++) {
            if (s.get(offset + i) < p) {
                p = s.get(offset + i);
                index = offset + i;
            }
        }
        return index;
    }

    /**
	 * return the nearest positive peak index from center
    * or -1 if no peak found.
    *	the search ends after reaching length in both directions.
	 */
    public static int getNearestPositivePeakIndex(MMArray s, int center, int length) {
        if (center < 0) {
            center = 0;
        } else if (center >= s.getLength()) {
            center = s.getLength() - 1;
        }
        float peakValue = Float.MIN_VALUE;
        int peakIndex = center;
        int x;
        for (int i = 0; i < length; i++) {
            x = center + i;
            if (x < s.getLength()) {
                if (s.get(x) > peakValue) {
                    peakValue = s.get(x);
                    peakIndex = x;
                }
            }
            x = center - i;
            if (x >= 0) {
                if (s.get(x) > peakValue) {
                    peakValue = s.get(x);
                    peakIndex = x;
                }
            }
        }
        if ((s.get(peakIndex + 1) <= s.get(peakIndex)) && (s.get(peakIndex - 1) <= s.get(peakIndex))) {
            return peakIndex;
        } else {
            return center;
        }
    }

    /**
	 * return the nearest negative peak index from center 
    * or -1 if no peak found.
    *	the search ends after reaching length in both directions.
	 */
    public static int getNearestNegativePeakIndex(MMArray s, int center, int length) {
        if (center < 0) {
            center = 0;
        } else if (center >= s.getLength()) {
            center = s.getLength() - 1;
        }
        float peakValue = Float.MAX_VALUE;
        int peakIndex = center;
        int x;
        for (int i = 0; i < length; i++) {
            x = center + i;
            if (x < s.getLength()) {
                if (s.get(x) < peakValue) {
                    peakValue = s.get(x);
                    peakIndex = x;
                }
            }
            x = center - i;
            if (x >= 0) {
                if (s.get(x) < peakValue) {
                    peakValue = s.get(x);
                    peakIndex = x;
                }
            }
        }
        if ((s.get(peakIndex + 1) >= s.get(peakIndex)) && (s.get(peakIndex - 1) >= s.get(peakIndex))) {
            return peakIndex;
        } else {
            return center;
        }
    }

    /**
	 * return the nearest zero-cross index from center or -1 if no zero-cross found.
    *	the search ends after reaching length in both directions.
	 */
    public static int getNearestZeroCrossIndex(MMArray s, int center, int length) {
        if (center < 0) {
            center = 0;
        } else if (center >= s.getLength()) {
            center = s.getLength() - 1;
        }
        boolean sign = s.get(center) > 0;
        int x;
        for (int i = 0; i < length; i++) {
            x = center + i;
            if (x < s.getLength()) {
                if (sign != (s.get(x) > 0)) {
                    return x;
                }
            }
            x = center - i;
            if (x >= 0) {
                if (sign != (s.get(x) > 0)) {
                    return x;
                }
            }
        }
        return -1;
    }

    /**
	 * return the index of the begin of the next noise-area on right side,
    *	or -1 if no noise found. noise is defined by threshold and minimum
    *	length.
	 */
    public static int getNextUpperNoiseIndex(MMArray s, int offset, int length, float silenceThreshold, int minWidth) {
        for (int i = 1; i < length; i++) {
            if (Math.abs(s.get(offset + i)) > silenceThreshold) {
                return offset + i;
            }
        }
        return -1;
    }

    /**
	 * return the index of the begin of the next noise-area on left side,
    *	or -1 if no noise found. noise is defined by threshold and minimum
    *	length.
	 */
    public static int getNextLowerNoiseIndex(MMArray s, int offset, int length, float silenceThreshold, int minWidth) {
        for (int i = 1; i < length; i++) {
            if (Math.abs(s.get(offset - i)) > silenceThreshold) {
                return offset - i;
            }
        }
        return -1;
    }

    /**
	 * return the index of the begin of the next silence-area on right side,
    *	or -1 if no silence found. silence is defined by threshold and minimum
    *	length.
	 */
    public static int getNextUpperSilenceIndex(MMArray s, int offset, int length, float silenceThreshold, int minWidth) {
        int widthCounter = 0;
        for (int i = 1; i < length; i++) {
            if (Math.abs(s.get(offset + i)) <= silenceThreshold) {
                widthCounter++;
            } else {
                widthCounter = 0;
            }
            if (widthCounter >= minWidth) {
                return offset + i - widthCounter;
            }
        }
        return -1;
    }

    /**
	 * return the index of the begin of the next silence-area on left side,
    *	or -1 if no silence found. silence
    +++++++++++++++++ is defined by threshold and minimum
    *	length.
	 */
    public static int getNextLowerSilenceIndex(MMArray s, int offset, int length, float silenceThreshold, int minWidth) {
        int widthCounter = 0;
        for (int i = 1; i < length; i++) {
            if (Math.abs(s.get(offset - i)) <= silenceThreshold) {
                widthCounter++;
            } else {
                widthCounter = 0;
            }
            if (widthCounter >= minWidth) {
                return offset - i + widthCounter;
            }
        }
        return -1;
    }

    public static AChannelSelection expandSilence(AChannelSelection s, float silenceThreshold, int minimumWidth) {
        int low = s.getOffset();
        int high = low + s.getLength();
        AChannel ch = s.getChannel();
        low = AOToolkit.getNextLowerNoiseIndex(ch.getSamples(), low, low, silenceThreshold, minimumWidth);
        high = AOToolkit.getNextUpperNoiseIndex(ch.getSamples(), high, ch.getSamples().getLength() - high, silenceThreshold, minimumWidth);
        if (low == -1) {
            low = 0;
        }
        if (high == -1) {
            high = ch.getSamples().getLength() - 1;
        }
        if (low < high) {
            return new AChannelSelection(ch, low, high - low);
        }
        return new AChannelSelection(ch, 0, 0);
    }

    public static AChannelSelection expandNoise(AChannelSelection s, float silenceThreshold, int minimumWidth) {
        int low = s.getOffset();
        int high = low + s.getLength();
        AChannel ch = s.getChannel();
        low = AOToolkit.getNextLowerSilenceIndex(ch.getSamples(), low, low, silenceThreshold, minimumWidth);
        high = AOToolkit.getNextUpperSilenceIndex(ch.getSamples(), high, ch.getSamples().getLength() - high, silenceThreshold, minimumWidth);
        if (low == -1) {
            low = 0;
        }
        if (high == -1) {
            high = ch.getSamples().getLength() - 1;
        }
        if (low < high) {
            return new AChannelSelection(ch, low, high - low);
        }
        return new AChannelSelection(ch, 0, 0);
    }

    public static AChannelSelection reduceSilence(AChannelSelection s, float silenceThreshold, int minimumWidth) {
        int low = s.getOffset();
        int high = low + s.getLength();
        AChannel ch = s.getChannel();
        low = AOToolkit.getNextUpperSilenceIndex(ch.getSamples(), low, ch.getSamples().getLength() - low, silenceThreshold, minimumWidth);
        high = AOToolkit.getNextLowerSilenceIndex(ch.getSamples(), high, high, silenceThreshold, minimumWidth);
        if (low == -1) {
            low = 0;
        }
        if (high == -1) {
            high = ch.getSamples().getLength() - 1;
        }
        if (low < high) {
            return new AChannelSelection(ch, low, high - low);
        }
        return new AChannelSelection(ch, 0, 0);
    }

    public static AChannelSelection reduceNoise(AChannelSelection s, float silenceThreshold, int minimumWidth) {
        int low = s.getOffset();
        int high = low + s.getLength();
        AChannel ch = s.getChannel();
        low = AOToolkit.getNextUpperNoiseIndex(ch.getSamples(), low, ch.getSamples().getLength() - low, silenceThreshold, minimumWidth);
        high = AOToolkit.getNextLowerNoiseIndex(ch.getSamples(), high, high, silenceThreshold, minimumWidth);
        if (low == -1) {
            low = 0;
        }
        if (high == -1) {
            high = ch.getSamples().getLength() - 1;
        }
        if (low < high) {
            return new AChannelSelection(ch, low, high - low);
        }
        return new AChannelSelection(ch, 0, 0);
    }

    /**
	 *	coordinate trnsform: cartesian to polar magnitude
	 */
    public static float cartesianToMagnitude(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
	 *	coordinate trnsform: cartesian to polar phase 
	 */
    public static float cartesianToPhase(float x, float y) {
        return (float) (Math.atan2(y, x));
    }

    /**
	 *	coordinate trnsform: polar to cartesian x 
	 */
    public static float polarToX(float mag, float phas) {
        return (float) (mag * Math.cos(phas));
    }

    /**
	 *	coordinate trnsform: polar to cartesian y 
	 */
    public static float polarToY(float mag, float phas) {
        return (float) (mag * Math.sin(phas));
    }

    /**
	 *	applies a zero-cross boundary: filters clipping, which occur on editing
 	 *	(copy, paste, cut...) and some effects, when the phase of the signal 
	 *	makes a step. works in circular mode (end-begin)
	 *	@param sample data samples of a channel
	 *	@param crossIndex points on the middle of the crosspoint
	 *	@param crossWidth number of affected points
	 */
    public static void applyZeroCross(MMArray sample, int crossIndex, int crossWidth) {
        try {
            float halfStep = (sample.get(crossIndex) - sample.get(crossIndex - 1)) / 2;
            halfStep -= sample.get(crossIndex - 1) - sample.get(crossIndex - 2);
            int halfCrossWidth = crossWidth / 2;
            for (int i = 1; i < halfCrossWidth; i++) {
                int ii = crossIndex - i;
                sample.set(ii, sample.get(ii) + halfStep * (1f - ((float) i) / halfCrossWidth));
            }
            for (int i = 0; i < halfCrossWidth; i++) {
                int ii = crossIndex + i;
                sample.set(ii, sample.get(ii) - halfStep * (1f - ((float) i) / halfCrossWidth));
            }
        } catch (Exception e) {
        }
    }

    public static int zeroCrossWidth = 30;

    /**
	 *	global setting of zerocross-width
	 */
    public static void setZeroCrossWidth(int zc) {
        zeroCrossWidth = zc;
    }

    /**
	 *	global setting of zerocross-width
	 */
    public static int getZeroCrossWidth() {
        return zeroCrossWidth;
    }

    /**
	 *	same as above, but with default width and global enable
	 */
    public static void applyZeroCross(MMArray sample, int crossIndex) {
        applyZeroCross(sample, crossIndex, zeroCrossWidth);
    }

    /**
	 * constant addition
	 */
    public static void add(MMArray s, int o, int l, float value) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, s.get(o + i) + value);
        }
    }

    /**
	 * variable addition
	 */
    public static void add(MMArray s, MMArray value, int o, int l) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, s.get(o + i) + value.get(o + i));
        }
    }

    /**
	 * constant subtraction
	 */
    public static void subtract(MMArray s, int o, int l, float value) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, s.get(o + i) - value);
        }
    }

    /**
	 * variable subtraction
	 */
    public static void subtract(MMArray s, MMArray value, int o, int l) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, s.get(o + i) - value.get(o + i));
        }
    }

    /**
	 * constant multiplication
	 */
    public static void multiply(MMArray s, int o, int l, float factor) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, s.get(o + i) * factor);
        }
    }

    /**
	 * variable multiplication
	 */
    public static void multiply(MMArray s, MMArray factor, int o, int l) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, s.get(o + i) * factor.get(o + i));
        }
    }

    /**
	 * constant division
	 */
    public static void divide(MMArray s, int o, int l, float divisor) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, s.get(o + i) / divisor);
        }
    }

    /**
	 * variable division
	 */
    public static void divide(MMArray s, MMArray divisor, int o, int l) {
        for (int i = 0; i < l; i++) {
            if (divisor.get(o + i) != 0) {
                s.set(o + i, s.get(o + i) / divisor.get(o + i));
            }
        }
    }

    /**
	 * first order derivation
	 */
    public static void derivate(MMArray s, int o, int l) {
        float sOld = s.get(o);
        float d = 0;
        for (int i = 0; i < l; i++) {
            d = s.get(o + i) - sOld;
            sOld = s.get(o + i);
            s.set(o + i, d);
        }
    }

    /**
	 * first order integral
	 */
    public static void integrate(MMArray s, int o, int l) {
        float summ = 0;
        for (int i = 0; i < l; i++) {
            summ += s.get(o + i);
            s.set(o + i, summ);
        }
    }

    /**
	 * invers 1 / x
	 */
    public static void invers(MMArray s, int o, int l) {
        for (int i = 0; i < l; i++) {
            if (s.get(o + i) != 0) {
                s.set(o + i, 1.f / s.get(o + i));
            }
        }
    }

    /**
	 * neg +/-
	 */
    public static void neg(MMArray s, int o, int l) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, -s.get(o + i));
        }
    }

    /**
	 * power, (works with negative numbers, keeps sign)
	 */
    public static void pow(MMArray s, int o, int l, float exponent) {
        for (int i = 0; i < l; i++) {
            float si = s.get(o + i);
            if (si >= 0) {
                s.set(o + i, (float) Math.pow(s.get(o + i), exponent));
            } else {
                s.set(o + i, -(float) Math.pow(-s.get(o + i), exponent));
            }
        }
    }

    /**
	 * square root
	 */
    public static void sqrt(MMArray s, int o, int l) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, (float) Math.sqrt(s.get(o + i)));
        }
    }

    /**
	 * exponential
	 */
    public static void exp(MMArray s, int o, int l) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, (float) Math.exp(s.get(o + i)));
        }
    }

    /**
	 * logarithm
	 */
    public static void log(MMArray s, int o, int l) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, (float) Math.log(s.get(o + i)));
        }
    }

    /**
	 * to decibel
	 */
    public static void todB(MMArray s, int o, int l) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, todB(s.get(o + i)));
        }
    }

    /**
	 * from decibel
	 */
    public static void fromdB(MMArray s, int o, int l) {
        for (int i = 0; i < l; i++) {
            s.set(o + i, fromdB(s.get(o + i)));
        }
    }

    /**
	 * returns max absolut value
	 */
    public static float max(MMArray s, int o, int l) {
        float max = 0;
        for (int i = 0; i < l; i++) {
            if (Math.abs(s.get(o + i)) > max) {
                max = Math.abs(s.get(o + i));
            }
        }
        return max;
    }
}
