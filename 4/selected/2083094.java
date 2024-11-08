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


Class:			AOHistogram
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	histogram analysis.

History:
Date:			Description:									Autor:
08.06.02		first draft										oli4

***********************************************************/
public class AOHistogram extends AOperation {

    public AOHistogram() {
        histogram = new MMArray(histogramLength, 0);
    }

    private static final int histogramLength = 0x7FFF;

    private MMArray histogram;

    public MMArray getHistogram() {
        return histogram;
    }

    public static int getHistogramLength() {
        return histogramLength;
    }

    /**
	*	performs the histogram of the given channel-selection.
	*	histogram-length is constant, begins at zero and ends at samplewidth
	*/
    public final void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        try {
            LProgressViewer.getInstance().entrySubProgress(0.7);
            for (int i = 0; i < l1; i++) {
                if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
                int h = Math.abs((int) (s1.get(o1 + i)));
                if (h >= histogram.getLength()) {
                    h = histogram.getLength() - 1;
                } else if (h < 0) {
                    h = 0;
                }
                if (Math.abs((int) (s1.get(o1 + i - 1))) < h && Math.abs((int) (s1.get(o1 + i + 1))) < h) {
                    histogram.set(h, histogram.get(h) + 1);
                }
            }
            LProgressViewer.getInstance().exitSubProgress();
        } catch (ArrayIndexOutOfBoundsException oob) {
            Debug.printStackTrace(5, oob);
        }
    }
}
