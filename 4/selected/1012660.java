package ch.laoe.operation;

import ch.laoe.clip.AChannelSelection;
import ch.laoe.clip.MMArray;
import ch.laoe.ui.Debug;
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


Class:			AOSpectrum
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	spectrum analysis.

History:
Date:			Description:									Autor:
24.05.01		first draft										oli4

***********************************************************/
public class AOSpectrum extends AOperation {

    public AOSpectrum(int fftLength, int windowType) {
        this.convertBufferLength = fftLength;
        this.windowType = windowType;
        re = new MMArray(convertBufferLength, 0);
        im = new MMArray(convertBufferLength, 0);
        spectrum = new MMArray(convertBufferLength / 2, 0);
    }

    private int windowType;

    private int convertBufferLength;

    private MMArray re, im;

    private MMArray spectrum;

    public MMArray getSpectrum() {
        return spectrum;
    }

    private void operateTWindow() {
        AOToolkit.applyFFTWindow(windowType, re, re.getLength());
    }

    private void addToSpectrum() {
        for (int i = 0; i < spectrum.getLength(); i++) {
            spectrum.set(i, spectrum.get(i) + AOToolkit.cartesianToMagnitude(re.get(i), im.get(i)));
        }
    }

    private void reduceSpectrum(int n) {
        for (int i = 0; i < spectrum.getLength(); i++) {
            spectrum.set(i, spectrum.get(i) / n);
        }
    }

    /**
	*	performs a constant amplification
	*/
    public final void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        try {
            int bufferOperations = l1 / convertBufferLength + 1;
            LProgressViewer.getInstance().entrySubProgress(0.7);
            for (int i = 0; i < bufferOperations; i++) {
                if (LProgressViewer.getInstance().setProgress((i + 1) * 1.0 / bufferOperations)) return;
                for (int j = 0; j < convertBufferLength; j++) {
                    int jj = o1 + i * convertBufferLength + j;
                    if (jj < o1 + l1) {
                        re.set(j, s1.get(jj));
                    } else {
                        re.set(j, 0);
                    }
                    im.set(j, 0);
                }
                operateTWindow();
                AOToolkit.complexFft(re, im);
                addToSpectrum();
            }
            reduceSpectrum(bufferOperations);
            LProgressViewer.getInstance().exitSubProgress();
        } catch (ArrayIndexOutOfBoundsException oob) {
            Debug.printStackTrace(5, oob);
        }
    }
}
