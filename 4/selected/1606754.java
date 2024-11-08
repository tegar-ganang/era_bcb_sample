package ch.laoe.operation;

import ch.laoe.clip.AChannelSelection;
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


Class:			AONarrowWideSweep
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.4

Desctription:	general amplifier

History:
Date:			Description:									Autor:
30.04.01		first draft										oli4

***********************************************************/
public class AONarrowWideSweep extends AONarrowWide {

    public AONarrowWideSweep(boolean modifyCh1, boolean modifyCh2, float wideBegin, float wideEnd, boolean continueBefore, boolean continueAfter) {
        super(modifyCh1, modifyCh2);
        this.wideBegin = wideBegin;
        this.wideEnd = wideEnd;
        this.continueBefore = continueBefore;
        this.continueAfter = continueAfter;
    }

    private boolean continueBefore, continueAfter;

    private float wideBegin, wideEnd;

    /**
	*	performs a constant widening
	*/
    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        if (modifyCh1) {
            ch1.getChannel().markChange();
        }
        if (modifyCh2) {
            ch2.getChannel().markChange();
        }
        try {
            LProgressViewer.getInstance().entrySubProgress(0.7);
            if (continueBefore) {
                for (int i = 0; i < o1; i++) {
                    float wide = wideBegin;
                    if (wide < 1) {
                        narrowing(ch1, ch2, i, 1 - wide);
                    } else {
                        widening(ch1, ch2, i, wide - 1);
                    }
                }
            }
            for (int i = 0; i < l1; i++) {
                if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
                float wide = wideBegin + ((wideEnd - wideBegin) * i / l1);
                if (wide < 1) {
                    narrowing(ch1, ch2, o1 + i, 1 - wide);
                } else {
                    widening(ch1, ch2, o1 + i, wide - 1);
                }
            }
            if (continueAfter) {
                int length = ch1.getChannel().getSamples().getLength();
                for (int i = o1 + l1; i < length; i++) {
                    float wide = wideEnd;
                    if (wide < 1) {
                        narrowing(ch1, ch2, i, 1 - wide);
                    } else {
                        widening(ch1, ch2, i, wide - 1);
                    }
                }
            }
            LProgressViewer.getInstance().exitSubProgress();
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }
}
