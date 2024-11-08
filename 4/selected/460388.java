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


Class:			AOMove
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	move a selection to another place

History:
Date:			Description:									Autor:
13.04.01		first draft										oli4

***********************************************************/
public class AOMove extends AOperation {

    public AOMove(int newIndex) {
        super();
        this.newIndex = newIndex;
    }

    private int newIndex;

    /**
	*	performs the move
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        MMArray tmp1, tmp2;
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        if ((newIndex >= o1) && (newIndex < o1 + l1)) {
            return;
        }
        int ni = newIndex;
        if (ni < 0) {
            ni = 0;
        } else if (ni > o1 + l1) {
            ni -= l1;
        }
        tmp1 = new MMArray(s1.getLength() - l1, 0);
        tmp1.copy(s1, 0, 0, o1);
        tmp1.copy(s1, o1 + l1, o1, s1.getLength() - o1 - l1);
        tmp2 = new MMArray(s1.getLength(), 0);
        tmp2.copy(tmp1, 0, 0, ni);
        tmp2.copy(s1, o1, ni, l1);
        tmp2.copy(tmp1, ni, ni + l1, tmp1.getLength() - ni);
        AOToolkit.applyZeroCross(tmp2, ni);
        AOToolkit.applyZeroCross(tmp2, ni + l1);
        tmp2.cleanup();
        ch1.getChannel().setSamples(tmp2);
    }
}
