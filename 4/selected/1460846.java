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


Class:			AOClippReparing
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	repairs clipping, tries to reproduce original
					curve, but without performing normalization.

History:
Date:			Description:									Autor:
08.07.02		first draft										oli4

***********************************************************/
public class AOClippReparing extends AOperation {

    /**
	 *	@param	maxWidth: maximum clipp width which will be repaired
	 *	@param	minDerivation: minimum derivation which identifies a clipping
	 *				border, unit in [2^samplewidth], 0.1 .. 1
	 */
    public AOClippReparing(int maxWidth, float minDerivationFactor) {
        super();
        this.maxWidth = maxWidth;
        this.minDerivationFactor = minDerivationFactor;
    }

    private float minDerivationFactor;

    private int maxWidth;

    /**
	*	performs a constant amplification
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        int sampleWidth = 1 << ch1.getChannel().getParentClip().getSampleWidth();
        int minDerivation = (int) (minDerivationFactor * sampleWidth);
        try {
            LProgressViewer.getInstance().entrySubProgress(0.7);
            for (int i = 1; i < l1; i++) {
                if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
                float dBegin = s1.get(o1 + i) - s1.get(o1 + i - 1);
                float dEnd;
                if (Math.abs(dBegin) > minDerivation) {
                    for (int j = 1; j < maxWidth; j++) {
                        dEnd = s1.get(o1 + i + j) - s1.get(o1 + i + j - 1);
                        if (Math.abs(dEnd) > minDerivation) {
                            if ((dBegin > 0) && (dEnd < 0)) {
                                for (int k = 0; k < j; k++) {
                                    s1.set(o1 + i + k, s1.get(o1 + i + k) - sampleWidth);
                                }
                            } else if ((dBegin < 0) && (dEnd > 0)) {
                                for (int k = 0; k < j; k++) {
                                    s1.set(o1 + i + k, s1.get(o1 + i + k) + sampleWidth);
                                }
                            }
                            i += j + 1;
                            break;
                        }
                    }
                }
            }
            LProgressViewer.getInstance().exitSubProgress();
        } catch (ArrayIndexOutOfBoundsException oob) {
            Debug.printStackTrace(5, oob);
        }
    }
}
