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


Class:			AOReverse
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	reverse or mirrors.

History:
Date:			Description:									Autor:
28.10.00		erster Entwurf									oli4
19.12.00		float audio samples							oli4
26.01.01		array-based again...							oli4
01.12.01		change classname from AOMirror to AOReverse	oli4

***********************************************************/
public class AOReverse extends AOperation {

    public AOReverse(int mode) {
        super();
        this.mode = mode;
    }

    private int mode;

    public static final int MIRROR_LEFT_SIDE = 1;

    public static final int MIRROR_RIGHT_SIDE = 2;

    public static final int REVERSE = 3;

    public void operate(AChannelSelection ch1) {
        ch1.getChannel().markChange();
        switch(mode) {
            case MIRROR_LEFT_SIDE:
                mirrorLeft(ch1);
                break;
            case MIRROR_RIGHT_SIDE:
                mirrorRight(ch1);
                break;
            case REVERSE:
                reverse(ch1);
                break;
        }
    }

    private void mirrorLeft(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(s1.getLength() + l1, 0);
        try {
            tmp.copy(s1, 0, 0, o1);
            for (int i = 0; i < l1; i++) tmp.set(i + o1, s1.get(o1 + l1 - i - 1));
            tmp.copy(s1, o1, o1 + l1, s1.getLength() - o1);
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
        ch1.getChannel().setSamples(tmp);
    }

    private void mirrorRight(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(s1.getLength() + l1, 0);
        try {
            tmp.copy(s1, 0, 0, o1 + l1);
            for (int i = 0; i < l1; i++) tmp.set(i + o1 + l1 - 1, s1.get(o1 + l1 - i - 1));
            tmp.copy(s1, o1 + l1, o1 + l1 + l1, s1.getLength() - o1 - l1);
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
        ch1.getChannel().setSamples(tmp);
    }

    private void reverse(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        MMArray tmp = new MMArray(l1, 0);
        try {
            tmp.copy(s1, o1, 0, l1);
            for (int i = 0; i < l1; i++) s1.set(i + o1, tmp.get(l1 - i - 1));
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }
}
