package ch.laoe.operation;

import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.MMArray;
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


Class:			AOResample
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	resamples the whole channels which are (partly)
               selected. 

To do:			resample with interpolation

History:
Date:			Description:									Autor:
26.07.00		erster Entwurf									oli4
04.08.00		neuer Stil        							oli4
26.10.00		neuer Stil        							oli4
19.12.00		float audio samples							oli4
21.04.01		useage of toolkit								oli4
19.09.01		index-calculation double-precision         oli4

***********************************************************/
public class AOResample extends AOperation {

    public AOResample() {
        super();
        this.sampleRateFactor = 1.f;
        this.order = 2;
    }

    public AOResample(float sampleRateFactor, int order) {
        this();
        this.sampleRateFactor = sampleRateFactor;
        this.order = order;
    }

    public AOResample(int order) {
        this();
        this.order = order;
    }

    private double sampleRateFactor;

    private int order;

    /**
	*	performs constant resampling
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        int resampledLength = (int) ((float) l1 / sampleRateFactor);
        ch1.getChannel().markChange();
        MMArray tmp = new MMArray(s1.getLength() - l1 + resampledLength, 0);
        tmp.copy(s1, 0, 0, o1);
        double oldIndex = o1;
        LProgressViewer.getInstance().entrySubProgress(0.7);
        for (int i = o1; i < o1 + resampledLength; i++) {
            if (LProgressViewer.getInstance().setProgress((i + 1 - o1) * 1.0 / (int) resampledLength)) return;
            if (((int) oldIndex + 1) < s1.getLength()) {
                switch(order) {
                    case 0:
                        tmp.set(i, AOToolkit.interpolate0(s1, (float) oldIndex));
                        break;
                    case 1:
                        tmp.set(i, AOToolkit.interpolate1(s1, (float) oldIndex));
                        break;
                    case 2:
                        tmp.set(i, AOToolkit.interpolate2(s1, (float) oldIndex));
                        break;
                    case 3:
                        tmp.set(i, AOToolkit.interpolate3(s1, (float) oldIndex));
                        break;
                }
            } else break;
            oldIndex += sampleRateFactor;
        }
        LProgressViewer.getInstance().exitSubProgress();
        tmp.copy(s1, o1 + l1, o1 + resampledLength, tmp.getLength() - o1 - resampledLength);
        ch1.getChannel().setSamples(tmp);
    }

    /**
	*	performs variable resampling
	*/
    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray s2 = ch2.getChannel().getSamples();
        ch1.getChannel().markChange();
        double resampledLength = 0;
        for (int i = 0; i < l1; i++) {
            try {
                resampledLength += 1 / s2.get(o1 + i);
            } catch (ArrayIndexOutOfBoundsException aoobe) {
                resampledLength += 1;
            }
        }
        MMArray tmp = new MMArray(s1.getLength() - l1 + (int) resampledLength, 0);
        tmp.copy(s1, 0, 0, o1);
        double oldIndex = o1;
        LProgressViewer.getInstance().entrySubProgress(0.7);
        for (int i = o1; i < o1 + resampledLength; i++) {
            if (LProgressViewer.getInstance().setProgress((i + 1 - o1) * 1.0 / (int) resampledLength)) return;
            if (((int) oldIndex + 1) < s1.getLength()) {
                switch(order) {
                    case 0:
                        tmp.set(i, AOToolkit.interpolate0(s1, (float) oldIndex));
                        break;
                    case 1:
                        tmp.set(i, AOToolkit.interpolate1(s1, (float) oldIndex));
                        break;
                    case 2:
                        tmp.set(i, AOToolkit.interpolate2(s1, (float) oldIndex));
                        break;
                    case 3:
                        tmp.set(i, AOToolkit.interpolate3(s1, (float) oldIndex));
                        break;
                }
            } else break;
            try {
                if (s2.get((int) oldIndex) > 0) oldIndex += s2.get((int) oldIndex); else oldIndex += 1;
            } catch (ArrayIndexOutOfBoundsException aoobe) {
                oldIndex += 1;
            }
        }
        LProgressViewer.getInstance().exitSubProgress();
        tmp.copy(s1, o1 + l1, o1 + (int) resampledLength, tmp.getLength() - o1 - (int) resampledLength);
        ch1.getChannel().setSamples(tmp);
    }
}
