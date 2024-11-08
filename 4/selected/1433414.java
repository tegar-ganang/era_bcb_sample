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


Class:			AONarrowWide
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	general amplifier

History:
Date:			Description:									Autor:
02.06.01		first draft										oli4
01.09.01		perform widening in steps to avoid 
				resampling effect								oli4
02.12.2001	modify ch1/2 separatly						oli4
29.12.2001	increase variable widening quality by
				not vary the dephasing but the wet/dry
				part												oli4

***********************************************************/
public class AONarrowWide extends AOperation {

    public AONarrowWide(boolean modifyCh1, boolean modifyCh2) {
        super();
        this.modifyCh1 = modifyCh1;
        this.modifyCh2 = modifyCh2;
    }

    /**
	 *	@param wide constant narrow/widening factor
	 */
    public AONarrowWide(boolean modifyCh1, boolean modifyCh2, float wide) {
        this(modifyCh1, modifyCh2);
        this.wide = wide;
    }

    private float wide;

    protected boolean modifyCh1, modifyCh2;

    /**
	 *	@param wet narrow intensity 0..1 (1=narrowest)
	 */
    protected void narrowing(AChannelSelection ch1, AChannelSelection ch2, int index, float wet) {
        float f = 1.f - (wet / 2);
        float fc = 1.f - f;
        float s1 = ch1.getChannel().getSamples().get(index);
        float s2 = ch2.getChannel().getSamples().get(index);
        if (modifyCh1) {
            float m = s1 * f + s2 * fc;
            ch1.getChannel().getSamples().set(index, ch1.mixIntensity(index, s1, m));
        }
        if (modifyCh2) {
            float m = s2 * f + s1 * fc;
            ch2.getChannel().getSamples().set(index, ch2.mixIntensity(index, s2, m));
        }
    }

    /**
	 *	@param wet wide intensity 0..1 (1=widest)
	 */
    protected void widening(AChannelSelection ch1, AChannelSelection ch2, int index, float wet) {
        int d = 1000;
        float f = 1.f - wet;
        float fc = 1.f - f;
        MMArray s1 = ch1.getChannel().getSamples();
        MMArray s2 = ch2.getChannel().getSamples();
        try {
            if (modifyCh1) {
                float m = s1.get(index) * f + s1.get(index + d) * fc;
                s1.set(index, ch1.mixIntensity(index, s1.get(index), m));
            } else if (modifyCh2) {
                float m = s2.get(index) * f + s2.get(index + d) * fc;
                s2.set(index, ch2.mixIntensity(index, s2.get(index), m));
            }
        } catch (Exception e) {
        }
    }

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
            for (int i = 0; i < l1; i++) {
                if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
                if (wide < 1) {
                    narrowing(ch1, ch2, o1 + i, 1 - wide);
                } else {
                    widening(ch1, ch2, o1 + i, wide - 1);
                }
            }
            LProgressViewer.getInstance().exitSubProgress();
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }

    /**
	*	performs a variable widening on channel1 and channel2, taking into account
	*	only selection-range of the first channel. 0=narrowest, 1=neutral, 2=widest
	*/
    public void operate(AChannelSelection channel1, AChannelSelection channel2, AChannelSelection param) {
        MMArray p = param.getChannel().getSamples();
        int o1 = channel1.getOffset();
        int l1 = channel1.getLength();
        if (modifyCh1) {
            channel1.getChannel().markChange();
        }
        if (modifyCh2) {
            channel2.getChannel().markChange();
        }
        try {
            LProgressViewer.getInstance().entrySubProgress(0.7);
            for (int i = 0; i < l1; i++) {
                if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
                float wide = p.get(i + o1);
                if (wide < 1) {
                    narrowing(channel1, channel2, o1 + i, 1 - wide);
                } else {
                    widening(channel1, channel2, o1 + i, wide - 1);
                }
            }
            LProgressViewer.getInstance().exitSubProgress();
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }
}
