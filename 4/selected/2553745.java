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
public class AOSpectrogramFilter extends AOperation implements AOSpectrogramBlockProcessing.Processor {

    public AOSpectrogramFilter(int filterMode, float amplification, float threshold, int blockLength, int windowType, float overlap) {
        super();
        this.filterMode = filterMode;
        this.amplification = amplification;
        this.threshold = threshold;
        this.blockLength = blockLength;
        this.windowType = windowType;
        this.overlap = overlap;
    }

    private int filterMode;

    private float amplification;

    private float threshold;

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

    private static final int MAGNITUDE = 0;

    private static final int MAGNITUDE_ABOVE_THRESHOLD = 1;

    private static final int MAGNITUDE_BELOW_THRESHOLD = 2;

    private static String filterModeItems[] = { GLanguage.translate("magnitude"), GLanguage.translate("aboveThreshold"), GLanguage.translate("belowThreshold") };

    public static final String[] getFilterModeItems() {
        return filterModeItems;
    }

    public boolean process(MMArray re, MMArray im, int x, int length, AChannel2DSelection chs) {
        switch(filterMode) {
            case MAGNITUDE:
                return performMagnitude(re, im, x, length, chs);
            case MAGNITUDE_ABOVE_THRESHOLD:
                return performMagnitudeAboveThreshold(re, im, x, length, chs);
            case MAGNITUDE_BELOW_THRESHOLD:
                return performMagnitudeBelowThreshold(re, im, x, length, chs);
            default:
                return false;
        }
    }

    /**
    * performs magnitude modification, returns true if modified
    */
    private boolean performMagnitude(MMArray re, MMArray im, int x, int length, AChannel2DSelection chs) {
        boolean changed = false;
        float sr = chs.getChannel().getParentClip().getSampleRate();
        for (int j = 0; j < length; j++) {
            if (chs.getArea().isSelected(x, (float) j * sr / length / 2)) {
                re.set(j, re.get(j) * amplification);
                im.set(j, im.get(j) * amplification);
                changed = true;
            }
        }
        return changed;
    }

    /**
    * performs magnitude modification, but only if above thershold, returns true if modified
    */
    private boolean performMagnitudeAboveThreshold(MMArray re, MMArray im, int x, int length, AChannel2DSelection chs) {
        boolean changed = false;
        float sr = chs.getChannel().getParentClip().getSampleRate();
        for (int j = 0; j < length; j++) {
            if (chs.getArea().isSelected(x, (float) j * sr / length / 2)) {
                if (AOToolkit.cartesianToMagnitude(re.get(j), im.get(j)) > threshold) {
                    re.set(j, re.get(j) * amplification);
                    im.set(j, im.get(j) * amplification);
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
    * performs magnitude modification, but only if below thershold, returns true if modified
    */
    private boolean performMagnitudeBelowThreshold(MMArray re, MMArray im, int x, int length, AChannel2DSelection chs) {
        boolean changed = false;
        float sr = chs.getChannel().getParentClip().getSampleRate();
        for (int j = 0; j < length; j++) {
            if (chs.getArea().isSelected(x, (float) j * sr / length / 2)) {
                if (AOToolkit.cartesianToMagnitude(re.get(j), im.get(j)) < threshold) {
                    re.set(j, re.get(j) * amplification);
                    im.set(j, im.get(j) * amplification);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
