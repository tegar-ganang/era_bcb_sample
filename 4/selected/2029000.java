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


Class:			AOInsertReplace
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	cuts the original selection and replaces it with
the new selection. 

History:
Date:			Description:									Autor:
27.04.2003	first draft										oli4

***********************************************************/
public class AOInsertReplace extends AOperation {

    public AOInsertReplace() {
        super();
        insertedLength = 0;
    }

    /**
   *  inserts 0's ("length" times) 
   */
    public AOInsertReplace(int length) {
        super();
        insertedLength = length;
    }

    private int insertedLength;

    /**
	*	performs a 0-samples insertion
	*/
    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(s1.getLength() - l1 + insertedLength, 0);
        ch1.getChannel().markChange();
        tmp.copy(s1, 0, 0, o1);
        for (int i = o1; i < o1 + insertedLength; i++) {
            tmp.set(i, 0);
        }
        tmp.copy(s1, o1 + l1, o1 + insertedLength, tmp.getLength() - o1 - insertedLength);
        AOToolkit.applyZeroCross(tmp, o1);
        AOToolkit.applyZeroCross(tmp, o1 + insertedLength);
        ch1.getChannel().setSamples(tmp);
    }

    /**
	*	performs an insertion of channel2 to channel1
	*/
    public void operate(AChannelSelection ch1, AChannelSelection ch2) {
        MMArray s1 = ch1.getChannel().getSamples();
        MMArray s2 = ch2.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        int o2 = ch2.getOffset();
        int l2 = ch2.getLength();
        ch1.getChannel().markChange();
        MMArray tmp = new MMArray(s1.getLength() - l1 + l2, 0);
        tmp.copy(s1, 0, 0, o1);
        tmp.copy(s2, o2, o1, l2);
        tmp.copy(s1, o1 + l1, o1 + l2, tmp.getLength() - o1 - l2);
        AOToolkit.applyZeroCross(tmp, o1);
        AOToolkit.applyZeroCross(tmp, o1 + l2);
        ch1.getChannel().setSamples(tmp);
    }
}
