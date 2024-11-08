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


Class:			AORemoveMono
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	removes the mono-part of a stereo layer,
					and writes the result to channel1

History:
Date:			Description:									Autor:
11.07.01		first draft										oli4

***********************************************************/
public class AORemoveMono extends AOperation {

    /**
	 *	@param pan pan, value range 1..2, usually 1.5 (middle), but may
	 *	vary if mono source is not centered.
	 */
    public AORemoveMono(float pan) {
        super();
        this.pan = pan;
        l = Math.max(1, 2 * pan - 2);
        r = Math.max(1, 4 - 2 * pan);
    }

    private float pan;

    private float l;

    private float r;

    public void operate(AChannelSelection channel1, AChannelSelection channel2) {
        MMArray s1 = channel1.getChannel().getSamples();
        MMArray s2 = channel2.getChannel().getSamples();
        int o1 = channel1.getOffset();
        int l1 = channel1.getLength();
        float ch1, ch2;
        channel1.getChannel().markChange();
        try {
            LProgressViewer.getInstance().entrySubProgress(0.7);
            for (int i = 0; i < l1; i++) {
                if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / l1)) return;
                ch1 = s1.get(i + o1);
                ch2 = s2.get(i + o1);
                ch1 = ch1 * r - ch2 * l;
                s1.set(i + o1, channel1.mixIntensity(i + o1, s1.get(i + o1), ch1));
            }
            LProgressViewer.getInstance().exitSubProgress();
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }
}
