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


Class:			AOMeasure
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	general amplifier

History:
Date:			Description:									Autor:
16.05.01		first draft										oli4

***********************************************************/
public class AOMeasure extends AOperation {

    public AOMeasure(int sampleWidth) {
        super();
        clippedThreshold = 1 << (sampleWidth - 1);
    }

    private float min, max, mean, rms, stdDev;

    private float sum, sumOfSquares;

    private int samples, clippedSamples, clippedThreshold;

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getAbsoluteMax() {
        if (Math.abs(max) > Math.abs(min)) {
            return Math.abs(max);
        } else {
            return Math.abs(min);
        }
    }

    public float getMean() {
        return mean;
    }

    public float getRms() {
        return rms;
    }

    public float getStandardDeviation() {
        return stdDev;
    }

    public int getNumberOfClippedSamples() {
        return clippedSamples;
    }

    public void startOperation() {
        min = Float.MAX_VALUE;
        max = Float.MIN_VALUE;
        sum = 0;
        sumOfSquares = 0;
        samples = 0;
        clippedSamples = 0;
    }

    public void endOperation() {
        mean = sum / samples;
        rms = (float) Math.sqrt(sumOfSquares / samples);
        stdDev = (float) Math.sqrt((sumOfSquares - (sum * sum / samples)) / (samples - 1));
    }

    public void operate(AChannelSelection ch1) {
        MMArray s1 = ch1.getChannel().getSamples();
        int o1 = ch1.getOffset();
        int l1 = ch1.getLength();
        try {
            for (int i = o1; i < (o1 + l1); i++) {
                samples++;
                sum += s1.get(i);
                sumOfSquares += s1.get(i) * s1.get(i);
                if (s1.get(i) > max) {
                    max = s1.get(i);
                }
                if (s1.get(i) < min) {
                    min = s1.get(i);
                }
                if (s1.get(i) > clippedThreshold) {
                    clippedSamples++;
                }
            }
        } catch (ArrayIndexOutOfBoundsException oob) {
        }
    }
}
