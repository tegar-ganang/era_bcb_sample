package ch.laoe.operation;

import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.MMArray;
import ch.laoe.ui.Debug;
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


Class:			AOSpline
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	FFT block processing utility

History:
Date:			Description:									Autor:
20-10-2010	first draft										oli4

***********************************************************/
public class AOFftBlockProcessing {

    public interface Processor {

        /**
       * implementation of the f-domain block operation. the implementation may modify the block. the modified block will be read
       * back after this call. 
       * @param re real part of the f-domain block. 
       * @param im imaginary part of the f-domain block. 
       * @param x the x position of the block begin
       * @param length length of the real and imaginary part (corresponds to the half of the block length in time domain) 
       */
        public void process(MMArray re, MMArray im, int x, int length);
    }

    /**
    * create a block processing environment, that will use the processor for frequency-domain block manipulation. 
    * @param p the processor
    */
    public AOFftBlockProcessing(Processor p) {
        processor = p;
    }

    private Processor processor;

    private int fftWindowType = AOToolkit.BARTLETT_WINDOW;

    private int fftBlockLength = 512;

    private float overlapFactor = 0.5f;

    private boolean rmsAdaptionEnabled = false;

    private boolean zeroCrossEnabled = false;

    /**
    * set the windowing function to be applied for each block before transforming to f-domain
    * @param wt
    */
    public void setFftWindowType(int wt) {
        fftWindowType = wt;
    }

    /**
    * set the length of the block in time domain. 
    * @param bl
    */
    public void setFftBlockLength(int bl) {
        fftBlockLength = bl;
    }

    /**
    * set the block overlapping between the neighbour blocks. 0 means no overlap, 0.5 means 50% overlapping. must be smaller than 1
    * @param o
    */
    public void setOverlapFactor(float o) {
        overlapFactor = o;
    }

    /**
    * when enabled, the amplitude is auto-adjusted after the block-processing has finished. this tries to keep the origin RMS amplitude. 
    * @param e
    */
    public void setRmsAdaptionEnabled(boolean e) {
        rmsAdaptionEnabled = e;
    }

    /**
    * when enabled, zero-cross smoothing is applied between each block. try to avoid using this, by setting reasonable overlap and window type. 
    * @param e
    */
    public void setZeroCrossEnabled(boolean e) {
        zeroCrossEnabled = e;
    }

    private MMArray re, im, tmp;

    public final void operate(AChannelSelection chs) {
        MMArray s = chs.getChannel().getSamples();
        int o = chs.getOffset();
        int l = chs.getLength();
        float oldRms = 0;
        if (rmsAdaptionEnabled) {
            oldRms = AOToolkit.rmsAverage(s, o, l);
        }
        chs.getChannel().markChange();
        if (re == null) {
            re = new MMArray(fftBlockLength, 0);
        }
        if (re.getLength() != fftBlockLength) {
            re.setLength(fftBlockLength);
        }
        if (im == null) {
            im = new MMArray(fftBlockLength, 0);
        }
        if (im.getLength() != fftBlockLength) {
            im.setLength(fftBlockLength);
        }
        if (tmp == null) {
            tmp = new MMArray(fftBlockLength, 0);
        }
        if (tmp.getLength() != l) {
            tmp.setLength(l);
        }
        try {
            LProgressViewer.getInstance().entrySubProgress(0.7);
            int bufferOperations = (int) (l / fftBlockLength / (1f - overlapFactor) + 1);
            for (int i = -1; i < bufferOperations + 1; i++) {
                if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / bufferOperations)) return;
                int ii = (int) (o + i * fftBlockLength * (1f - overlapFactor));
                for (int j = 0; j < fftBlockLength; j++) {
                    int jj = ii + j;
                    if ((jj >= 0) && (jj < o + l)) {
                        re.set(j, s.get(jj));
                    } else {
                        re.set(j, 0);
                    }
                    im.set(j, 0);
                }
                AOToolkit.applyFFTWindow(fftWindowType, re, fftBlockLength);
                AOToolkit.complexFft(re, im);
                processor.process(re, im, ii, fftBlockLength / 2);
                AOToolkit.complexIfft(re, im);
                for (int j = 0; j < fftBlockLength; j++) {
                    int jjj = ii - o + j;
                    if ((jjj >= 0) && (jjj < l)) {
                        tmp.set(jjj, tmp.get(jjj) + re.get(j));
                    }
                }
                if (zeroCrossEnabled) {
                    AOToolkit.applyZeroCross(tmp, ii - o);
                }
            }
            for (int i = 0; i < l; i++) {
                s.set(o + i, chs.mixIntensity(o + i, s.get(o + i), tmp.get(i)));
            }
            if (rmsAdaptionEnabled) {
                float newRms = AOToolkit.rmsAverage(s, o, l);
                AOToolkit.multiply(s, o, l, (float) (oldRms / newRms));
            }
            if (zeroCrossEnabled) {
                AOToolkit.applyZeroCross(s, o);
                AOToolkit.applyZeroCross(s, o + l);
            }
            LProgressViewer.getInstance().exitSubProgress();
        } catch (Exception e) {
            Debug.printStackTrace(5, e);
        }
    }
}
