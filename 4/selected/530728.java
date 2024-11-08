package ch.laoe.operation;

import ch.laoe.clip.AChannel2DSelection;
import ch.laoe.clip.AClip;
import ch.laoe.clip.MMArray;
import ch.laoe.ui.GLanguage;

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


Class:			AOSpectrogramFilter
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	general amplifier

History:
Date:			Description:									Autor:
10.03.02		first draft										oli4

***********************************************************/
public class AOSpectrogramOrthogonalFilter extends AOperation implements AOSpectrogramBlockProcessing.Processor {

    public AOSpectrogramOrthogonalFilter(int filterMode, float amplification, float decay, int blockLength, int windowType, float overlap) {
        super();
        this.filterMode = filterMode;
        this.amplification = amplification;
        this.decay = decay;
        this.blockLength = blockLength;
        this.windowType = windowType;
        this.overlap = overlap;
    }

    private int filterMode;

    private float amplification;

    private float decay;

    private int blockLength;

    private int windowType;

    private float overlap;

    public void operate(AChannel2DSelection chs) {
        AOSpectrogramBlockProcessing processing = new AOSpectrogramBlockProcessing(this);
        processing.setFftBlockLength(blockLength);
        processing.setFftWindowType(windowType);
        processing.setOverlapFactor(overlap);
        processing.setRmsAdaptionEnabled(false);
        processing.setZeroCrossEnabled(false);
        processing.operate(chs);
    }

    public static final int VERTICAL = 0;

    public static final int HORIZONTAL = 1;

    private static String filterModeItems[] = { GLanguage.translate("vertical"), GLanguage.translate("horizontal") };

    public static final String[] getModeItems() {
        return filterModeItems;
    }

    public boolean process(MMArray re, MMArray im, int x, int length, AChannel2DSelection chs) {
        switch(filterMode) {
            case VERTICAL:
                return performVertical(amplification, re, im, x, length, chs);
            case HORIZONTAL:
                return performHorizontal(amplification, re, im, x, length, chs);
            default:
                return false;
        }
    }

    private float avv;

    private boolean performVertical(float a, MMArray re, MMArray im, int x, int length, AChannel2DSelection chs) {
        boolean changed = false;
        float sr = chs.getChannel().getParentClip().getSampleRate();
        float sw = 1 << (chs.getChannel().getParentClip().getSampleWidth() - 1);
        float xm[] = new float[length];
        for (int i = 0; i < length; i++) {
            xm[i] = AOToolkit.cartesianToMagnitude(re.get(i), im.get(i));
        }
        float av = AOToolkit.average(xm, 0, length) / sw;
        if (Math.abs(av) > Math.abs(avv)) {
            avv = av;
        } else {
            avv = Math.max(av, av * decay);
        }
        System.out.println("avv=" + avv);
        float f = (float) Math.pow(avv, a);
        for (int j = 0; j < length; j++) {
            if (chs.getArea().isSelected(x, (float) j * sr / length / 2)) {
                {
                    re.set(j, re.get(j) * f);
                    im.set(j, im.get(j) * f);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private float avh[];

    public void startOperation() {
        avh = null;
        avv = 0;
    }

    private boolean performHorizontal(float a, MMArray re, MMArray im, int x, int length, AChannel2DSelection chs) {
        boolean changed = false;
        float sr = chs.getChannel().getParentClip().getSampleRate();
        float sw = 1 << (chs.getChannel().getParentClip().getSampleWidth() - 1);
        if (avh == null) {
            avh = new float[length];
        }
        for (int i = 0; i < length; i++) {
            avh[i] = AOToolkit.movingAverage(avh[i], AOToolkit.cartesianToMagnitude(re.get(i), im.get(i)) / sw, 100);
        }
        for (int j = 0; j < length; j++) {
            if (chs.getArea().isSelected(x, (float) j * sr / length / 2)) {
                {
                    float f = (float) Math.pow(avh[j], a);
                    re.set(j, re.get(j) * f);
                    im.set(j, im.get(j) * f);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
