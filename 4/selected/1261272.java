package ch.laoe.operation;

import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.MMArray;

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


Class:			AOSegmentGenerator
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	generate linear segments, works without
					selections!!!

History:
Date:			Description:									Autor:
26.07.00		erster Entwurf									oli4
03.08.00		neuer Stil        							oli4
17.03.01		first use in a plugin						oli4
27.04.01		based on Toolkit								oli4
12.08.01		single-point mode								oli4
01.12.01		add envelope operation						oli4
21.01.02		selection-dependent and -independent mode		oli4

***********************************************************/
public class AOSegmentGenerator extends AOperation {

    /**
	 *	interpolation order
	 */
    public static final int SINGLE_POINTS = -1;

    public static final int ORDER_0 = 0;

    public static final int ORDER_1 = 1;

    public static final int ORDER_2 = 2;

    public static final int ORDER_3 = 3;

    public static final int SPLINE = 4;

    private int order;

    /**
	 *	operation on samples
	 */
    public static final int REPLACE_OPERATION = 0;

    public static final int ENVELOPE_OPERATION = 1;

    private int operation;

    /**
   *	x,y:		point definitions of the segments
   *	order: 	interpolation order
   */
    public AOSegmentGenerator(MMArray x, MMArray y, int order, int operation, boolean selectionIndependent) {
        super();
        this.x = x;
        this.y = y;
        this.order = order;
        this.operation = operation;
        this.selectionIndependent = selectionIndependent;
    }

    private MMArray x;

    private MMArray y;

    private boolean selectionIndependent;

    /**
	*	performs the segments generation
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int xStart;
        int xEnd;
        MMArray tmp;
        if (selectionIndependent) {
            xStart = (int) x.get(0);
            xEnd = (int) x.get(x.getLength() - 1);
        } else {
            xStart = ch1.getOffset();
            xEnd = ch1.getLength() + ch1.getOffset();
        }
        tmp = new MMArray(xEnd - xStart, 0);
        ch1.getChannel().markChange();
        switch(order) {
            case SINGLE_POINTS:
                for (int i = 0; i < x.getLength(); i++) {
                    if (ch1.getChannel().getSamples().isInRange((int) x.get(i))) {
                        if (selectionIndependent) {
                            s1.set((int) x.get(i), y.get(i));
                        } else {
                            int ii = (int) x.get(i);
                            if (ch1.isSelected(ii)) {
                                s1.set(ii, ch1.mixIntensity(ii, s1.get(ii), y.get(i)));
                            }
                        }
                    }
                }
                break;
            case ORDER_0:
                for (int i = xStart; i < xEnd; i++) {
                    if (ch1.getChannel().getSamples().isInRange(i)) {
                        tmp.set(i - xStart, AOToolkit.interpolate0(x, y, (float) i));
                    }
                }
                break;
            case ORDER_1:
                for (int i = xStart; i < xEnd; i++) {
                    if (ch1.getChannel().getSamples().isInRange(i)) {
                        tmp.set(i - xStart, AOToolkit.interpolate1(x, y, (float) i));
                    }
                }
                break;
            case ORDER_2:
                for (int i = xStart; i < xEnd; i++) {
                    if (ch1.getChannel().getSamples().isInRange(i)) {
                        tmp.set(i - xStart, AOToolkit.interpolate2(x, y, (float) i));
                    }
                }
                break;
            case ORDER_3:
                for (int i = xStart; i < xEnd; i++) {
                    if (ch1.getChannel().getSamples().isInRange(i)) {
                        tmp.set(i - xStart, AOToolkit.interpolate3(x, y, (float) i));
                    }
                }
                break;
            case SPLINE:
                AOSpline spline = AOToolkit.createSpline();
                spline.load(x, y);
                for (int i = xStart; i < xEnd; i++) {
                    if (ch1.getChannel().getSamples().isInRange(i)) {
                        tmp.set(i - xStart, spline.getResult((float) i));
                    }
                }
                break;
        }
        switch(order) {
            case ORDER_0:
            case ORDER_1:
            case ORDER_2:
            case ORDER_3:
            case SPLINE:
                switch(operation) {
                    case REPLACE_OPERATION:
                        for (int i = 0; i < tmp.getLength(); i++) {
                            if (ch1.getChannel().getSamples().isInRange(i + xStart)) {
                                if (selectionIndependent) {
                                    s1.set(i + xStart, tmp.get(i));
                                } else {
                                    int ii = i + xStart;
                                    s1.set(ii, ch1.mixIntensity(ii, s1.get(ii), tmp.get(i)));
                                }
                            }
                        }
                        break;
                    case ENVELOPE_OPERATION:
                        float factor = AOToolkit.max(s1, xStart, xEnd - xStart);
                        for (int i = 0; i < tmp.getLength(); i++) {
                            if (ch1.getChannel().getSamples().isInRange(i + xStart)) {
                                if (selectionIndependent) {
                                    s1.set(i + xStart, s1.get(i + xStart) * tmp.get(i) / factor);
                                } else {
                                    int ii = i + xStart;
                                    float x = s1.get(ii) * tmp.get(i) / factor;
                                    s1.set(ii, ch1.mixIntensity(ii, s1.get(ii), x));
                                }
                            }
                        }
                        break;
                }
                break;
        }
    }
}
