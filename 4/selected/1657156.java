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


Class:			AOChorusFlange
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	chorus and flange effect.

History:
Date:			Description:									Autor:
29.07.00		erster Entwurf: bin nicht sicher, ob
            der Algorithmus stimmt...					oli4
03.08.00		neuer Stil        							oli4
11.05.01		array-based again...							oli4

***********************************************************/
public class AOChorusFlange extends AOperation {

    public AOChorusFlange(float dry, float wet, float feedback, boolean negFeedback, int baseDelay, int modulationDelay, int modulationPeriod, int modulationShape) {
        super();
        this.dry = dry;
        this.wet = wet;
        this.feedback = feedback;
        this.negFeedback = negFeedback;
        this.baseDelay = baseDelay;
        this.modulationDelay = modulationDelay;
        this.modulationPeriod = modulationPeriod;
        this.modulationShape = modulationShape;
    }

    private float wet, dry, feedback, baseDelay, modulationDelay, modulationPeriod;

    private int modulationShape;

    private boolean negFeedback;

    public static final int SINUS = 1;

    public static final int TRIANGLE = 2;

    public static final int SAW = 3;

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        ch1.getChannel().markChange();
        AOFifo delayFifo = new AOFifo((int) (baseDelay + modulationDelay + 10));
        LProgressViewer.getInstance().entrySubProgress(0.7);
        for (int i = o1; i < (o1 + l1); i++) {
            if (LProgressViewer.getInstance().setProgress((i + 1 - o1) * 1.0 / l1)) return;
            float iMod = baseDelay;
            switch(modulationShape) {
                case SINUS:
                    iMod += (float) Math.sin((float) (i % (int) modulationPeriod) / modulationPeriod * 2 * Math.PI) * modulationDelay;
                    break;
                case TRIANGLE:
                    iMod += (Math.abs(((float) (i % (int) modulationPeriod) / modulationPeriod * 4) - 2) - 1) * modulationDelay;
                    break;
                case SAW:
                    iMod += (((float) (i % (int) modulationPeriod) / modulationPeriod * 2) - 1) * modulationDelay;
                    break;
            }
            float s = s1.get(i);
            if ((int) iMod < delayFifo.getActualSize() - 1) {
                delayFifo.put(s + (delayFifo.pickFromHead(iMod - 1) * feedback));
                if (negFeedback) {
                    s = (s * dry) - (delayFifo.pickFromHead(iMod) * wet);
                } else {
                    s = (s * dry) + (delayFifo.pickFromHead(iMod) * wet);
                }
            } else {
                delayFifo.put(s);
                s = s * dry;
            }
            s1.set(i, ch1.mixIntensity(i, s1.get(i), s));
        }
        AOToolkit.applyZeroCross(s1, o1);
        AOToolkit.applyZeroCross(s1, o1 + l1);
        LProgressViewer.getInstance().exitSubProgress();
    }
}
