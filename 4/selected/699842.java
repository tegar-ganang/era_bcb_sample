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


Class:			AOClickReduction
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	click reduction filter, applying zero-cross.

History:
Date:			Description:									Autor:
05.06.01		first draft										oli4

***********************************************************/
public class AOClickReduction extends AOperation {

    /**
	 *	@param	sense: limit of stepsize, at which smoothing is performed
	 *	@param	smooth: width of zero-cross
	 */
    public AOClickReduction(float sense, int smooth) {
        super();
        this.sense = sense;
        this.smooth = smooth;
    }

    private float sense;

    private int smooth;

    /**
	*	performs a constant amplification
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        try {
            MMArray d = new MMArray(l1, 0);
            d.copy(s1, o1, 0, d.getLength());
            AOToolkit.derivate(d, 0, d.getLength());
            AOToolkit.derivate(d, 0, d.getLength());
            AOToolkit.derivate(d, 0, d.getLength());
            AOToolkit.derivate(d, 0, d.getLength());
            float max = 0;
            for (int i = 0; i < d.getLength(); i++) {
                if (Math.abs(d.get(i)) > max) {
                    max = Math.abs(d.get(i));
                }
            }
            float minClickPeriod = 200;
            float oldX = 0;
            LProgressViewer.getInstance().entrySubProgress(0.7);
            for (int i = 0; i < d.getLength(); i++) {
                if (d.get(i) > max * sense) {
                    if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / d.getLength())) return;
                    if ((i - oldX) > minClickPeriod) {
                        AOToolkit.applyZeroCross(s1, i + o1, smooth);
                    }
                    oldX = i;
                }
            }
            LProgressViewer.getInstance().exitSubProgress();
        } catch (ArrayIndexOutOfBoundsException oob) {
            Debug.printStackTrace(5, oob);
        }
    }
}
