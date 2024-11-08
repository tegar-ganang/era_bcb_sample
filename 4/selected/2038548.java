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


Class:			AOFade
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	fade in / out

History:
Date:			Description:									Autor:
26.04.01		first draft										oli4
29.12.2001	add fill-zeroes option						oli4
10.07.2002	selectable low-factor (not just zero)	oli4

***********************************************************/
public class AOFade extends AOperation {

    /**
	 *	@param mode fade-mode, IN, OUT, CROSS
	 *	@param order order of shape of ramp
	 *	@param fillZeroes set all samples to zero on low-side of fade-selection
	 */
    public AOFade(int mode, int order, float lowFactor, boolean continueLow) {
        super();
        this.mode = mode;
        this.order = order;
        this.lowFactor = lowFactor;
        this.variableFactor = 1 - lowFactor;
        this.continueLow = continueLow;
    }

    private int mode;

    public static final int IN = 1;

    public static final int OUT = 2;

    public static final int CROSS = 3;

    private int order;

    public static final int SQUARE_ROOT = -2;

    public static final int LINEAR = 1;

    public static final int SQUARE = 2;

    public static final int CUBIC = 3;

    private boolean continueLow;

    private float lowFactor, variableFactor;

    private float performOrder(float a) {
        switch(order) {
            case LINEAR:
                return a;
            case SQUARE:
                return a * a;
            case CUBIC:
                return a * a * a;
            case SQUARE_ROOT:
                return (float) Math.pow(a, 0.5);
            default:
                return a;
        }
    }

    /**
	*	performs a constant amplification
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        float a;
        switch(mode) {
            case IN:
                try {
                    LProgressViewer.getInstance().entrySubProgress(0.7);
                    for (int i = 0; i < l1; i++) {
                        if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
                        a = (float) i / (float) l1;
                        a = performOrder(a);
                        s1.set(o1 + i, s1.get(o1 + i) * (a * variableFactor + lowFactor));
                    }
                    LProgressViewer.getInstance().exitSubProgress();
                    if (continueLow) {
                        for (int i = 0; i < o1; i++) {
                            s1.set(i, s1.get(i) * lowFactor);
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException oob) {
                }
                break;
            case OUT:
                try {
                    LProgressViewer.getInstance().entrySubProgress(0.7);
                    for (int i = 0; i < l1; i++) {
                        if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
                        a = (float) (l1 - i) / (float) l1;
                        a = performOrder(a);
                        s1.set(o1 + i, s1.get(o1 + i) * (a * variableFactor + lowFactor));
                    }
                    LProgressViewer.getInstance().exitSubProgress();
                    if (continueLow) {
                        for (int i = o1 + l1; i < s1.getLength(); i++) {
                            s1.set(i, s1.get(i) * lowFactor);
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException oob) {
                }
                break;
            case CROSS:
                int lh = l1 / 2;
                float b;
                MMArray tmp = new MMArray(s1.getLength() - lh, 0);
                try {
                    LProgressViewer.getInstance().entrySubProgress(0.7);
                    for (int i = 0; i < lh; i++) {
                        if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / lh)) return;
                        a = (float) i / (float) lh;
                        a = performOrder(a);
                        b = (float) (lh - i) / (float) lh;
                        b = performOrder(b);
                        s1.set(o1 + i, b * s1.get(o1 + i) + a * s1.get(o1 + lh + i));
                    }
                    LProgressViewer.getInstance().exitSubProgress();
                    tmp.copy(s1, 0, 0, o1 + lh);
                    tmp.copy(s1, o1 + l1, o1 + l1 - lh, s1.getLength() - o1 - l1);
                } catch (ArrayIndexOutOfBoundsException oob) {
                }
                ch1.getChannel().setSamples(tmp);
                break;
        }
    }
}
