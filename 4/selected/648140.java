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


Class:			AOCanon
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	folds the selection in two (or  more), 
				like a canon-song 

History:
Date:			Description:									Autor:
15.09.01		first draft										oli4

***********************************************************/
public class AOCanon extends AOperation {

    public AOCanon(int voices) {
        super();
        this.voices = voices;
    }

    private int voices;

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        int l2 = l1 / voices;
        MMArray tmp = new MMArray(s1.getLength() - l1 + l2, 0);
        ch1.getChannel().markChange();
        float oldRms = AOToolkit.rmsAverage(s1, o1, l1);
        tmp.copy(s1, 0, 0, o1);
        for (int i = o1; i < l1; i++) {
            tmp.set(o1 + (i % l2), tmp.get(o1 + (i % l2)) + s1.get(i));
        }
        tmp.copy(s1, o1 + l1, o1 + l2, s1.getLength() - o1 - l1);
        float newRms = AOToolkit.rmsAverage(tmp, o1, l2);
        AOToolkit.multiply(tmp, 0, tmp.getLength(), (float) (oldRms / newRms));
        AOToolkit.applyZeroCross(tmp, o1);
        AOToolkit.applyZeroCross(tmp, o1 + l2);
        ch1.getChannel().setSamples(tmp);
    }
}
