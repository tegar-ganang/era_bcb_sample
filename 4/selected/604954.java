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


Class:			AOSweepResample
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	resamples the whole channels which are (partly)
               selected, in sweep mode (linear resampling
 				   factor variation).


History:
Date:			Description:									Autor:
29.05.01		first draft										oli4
01.08.01		first stable version                   oli4
19.09.01		index-calculation double-precision         oli4

***********************************************************/
public class AOSweepResample extends AOperation {

    public AOSweepResample(float beginFactor, float endFactor, int order) {
        super();
        this.beginFactor = beginFactor;
        this.endFactor = endFactor;
        this.order = order;
    }

    private float beginFactor, endFactor;

    private int order;

    /**
	*	performs variable resampling
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        double resampledLength = 0;
        for (int i = 0; i < l1; i++) {
            double f = (double) i / (double) l1;
            resampledLength += (1 / beginFactor * (1 - f)) + (1 / endFactor * f);
        }
        MMArray tmp = new MMArray(s1.getLength() - l1 + (int) resampledLength, 0);
        tmp.copy(s1, 0, 0, o1);
        double oldIndex = o1;
        int end = o1 + (int) resampledLength;
        LProgressViewer.getInstance().entrySubProgress(0.7);
        for (int i = o1; i < end; i++) {
            if (LProgressViewer.getInstance().setProgress((i + 1 - o1) * 1.0 / (end - o1))) return;
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
            } else {
                break;
            }
            double f = (double) (i - o1) / resampledLength;
            oldIndex += 1 / (1 / beginFactor * (1 - f) + 1 / endFactor * f);
        }
        LProgressViewer.getInstance().exitSubProgress();
        tmp.copy(s1, o1 + l1, o1 + (int) resampledLength, tmp.getLength() - o1 - (int) resampledLength);
        ch1.getChannel().setSamples(tmp);
    }
}
