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


Class:			AOLoopable
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	makes a selection loopable, different modes are
				possible:
            		- 

History:
Date:			Description:									Autor:
14.05.01		first draft										oli4
21.06.01		add loopcount								oli4
15.09.01		completely redefined							oli4

***********************************************************/
public class AOLoopable extends AOperation {

    public AOLoopable(int order) {
        super();
        this.order = order;
    }

    private int order;

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(l1, 0);
        ch1.getChannel().markChange();
        tmp.copy(s1, o1, 0, l1);
        float oldRms = AOToolkit.rmsAverage(tmp, 0, tmp.getLength());
        AChannelSelection ch2 = new AChannelSelection(ch1.getChannel(), 0, o1);
        ch2.operateChannel(new AOFade(AOFade.IN, order, 0, false));
        AChannelSelection ch3 = new AChannelSelection(ch1.getChannel(), o1 + l1, s1.getLength() - o1 - l1);
        ch3.operateChannel(new AOFade(AOFade.OUT, order, 0, false));
        for (int i = 0; i < l1; i++) {
            if (o1 - l1 + i >= 0) {
                tmp.set(i, tmp.get(i) + s1.get(o1 - l1 + i));
            }
        }
        for (int i = 0; i < l1; i++) {
            if (o1 + l1 + i < s1.getLength()) {
                tmp.set(i, tmp.get(i) + s1.get(o1 + l1 + i));
            }
        }
        float newRms = AOToolkit.rmsAverage(tmp, 0, tmp.getLength());
        AOToolkit.multiply(tmp, 0, tmp.getLength(), (float) (oldRms / newRms));
        ch1.getChannel().setSamples(tmp);
    }
}
