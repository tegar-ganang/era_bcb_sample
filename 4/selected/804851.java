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


Class:			AOLoopableMultiplicate
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	multiplicates a selection n times

History:
Date:			Description:									Autor:
01.02.02		first draft										oli4

***********************************************************/
public class AOLoopableMultiplicate extends AOperation {

    public AOLoopableMultiplicate(int n) {
        super();
        this.n = n;
    }

    private int n;

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(l1 * n + s1.getLength() - l1, 0);
        ch1.getChannel().markChange();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < l1; j++) {
                tmp.set(o1 + i * l1 + j, s1.get(o1 + j));
            }
        }
        tmp.copy(s1, 0, 0, o1);
        tmp.copy(s1, o1 + l1, o1 + l1 + l1 * (n - 1), s1.getLength() - o1 - l1);
        for (int i = 0; i < n; i++) {
            AOToolkit.applyZeroCross(tmp, o1 + l1 * (n + 1));
        }
        ch1.getChannel().setSamples(tmp);
    }
}
