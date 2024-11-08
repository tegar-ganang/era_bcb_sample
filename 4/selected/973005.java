package edu.georgetown.nnj.filterblocks.flow;

import de.ifn_magdeburg.kazukazuj.K;
import de.ifn_magdeburg.kazukazuj.Kc;
import edu.georgetown.nnj.data.NNJDataSource;
import edu.georgetown.nnj.events.NNJChangeEvent;
import java.util.HashSet;

/**
 *
 * @author Kenta
 */
public class NNJFilterPFHilbertPhase extends NNJFilterPairwiseFlow {

    public NNJFilterPFHilbertPhase(NNJDataSource source) {
        super(source);
    }

    public NNJFilterPFHilbertPhase(NNJDataSource source, boolean filterActive) {
        super(source, filterActive);
    }

    protected NNJFilterPFHilbertPhase() {
        super();
    }

    @Override
    public void setNNJDataSourceImpl() {
        hilbPhBuff = new double[this.getDataLayout().getChannelCount()][];
        super.setNNJDataSourceImpl();
    }

    private double[][] hilbPhBuff;

    private void checkHilbertPhaseBuffer(int det) {
        if (hilbPhBuff[det] == null) {
            hilbPhBuff[det] = K.arg(K.hilbert(getNNJDataSource().readDataTraceSegmentAbsolute(det, 0, getDataWindow().getMaximum())));
        }
    }

    public double[] getHilbertPhase(int det) {
        checkHilbertPhaseBuffer(det);
        return K.copy(hilbPhBuff[det]);
    }

    @Override
    public double[] calculatePairwiseFlow(int fr, int det1, int det2) {
        if (fr < this.getMaxShift() || fr + this.getMaxShift() >= this.getTotalFrameCount()) {
            throw new IllegalArgumentException("The frame " + fr + " is out of range (given that getMaxShift()=" + getMaxShift() + ").");
        }
        checkHilbertPhaseBuffer(det1);
        checkHilbertPhaseBuffer(det2);
        return new double[] { calculatePairwiseFlowImpl(fr, hilbPhBuff[det1], hilbPhBuff[det2]), 1d };
    }

    public double calculatePairwiseFlowImpl(int fr, double[] trace1, double[] trace2) {
        double oriDet1 = trace1[fr];
        double oriDifference = Kc.circularSubtract(oriDet1, trace2[fr]);
        double tempret = 0d;
        if (oriDifference == 0) {
            return 0d;
        } else if (oriDifference > 0) {
            double currDet2Pd, currDet2Md;
            double prevDet2Pd = trace2[fr];
            double prevDet2Md = trace2[fr];
            for (int d = 1; d <= this.getMaxShift(); d++) {
                currDet2Pd = trace2[fr + d];
                currDet2Md = trace2[fr - d];
                if (Kc.circularSubtract(oriDet1, currDet2Pd) == 0) {
                    tempret = (double) d;
                    break;
                } else if (Kc.circularSubtract(oriDet1, currDet2Pd) < 0) {
                    tempret = (double) (d - 1) + Kc.circularSubtract(oriDet1, prevDet2Pd) / Kc.circularSubtract(currDet2Pd, prevDet2Pd);
                    break;
                } else if (Kc.circularSubtract(oriDet1, currDet2Md) == 0) {
                    tempret = -(double) d;
                    break;
                } else if (Kc.circularSubtract(oriDet1, currDet2Md) < 0) {
                    tempret = -(double) (d - 1) - Kc.circularSubtract(oriDet1, prevDet2Md) / Kc.circularSubtract(currDet2Md, prevDet2Md);
                    break;
                }
                prevDet2Pd = currDet2Pd;
                prevDet2Md = currDet2Md;
            }
            return tempret;
        } else {
            double currDet2Pd, currDet2Md;
            double prevDet2Pd = trace2[fr];
            double prevDet2Md = trace2[fr];
            for (int d = 1; d <= this.getMaxShift(); d++) {
                currDet2Pd = trace2[fr + d];
                currDet2Md = trace2[fr - d];
                if (Kc.circularSubtract(oriDet1, currDet2Pd) == 0) {
                    tempret = (double) d;
                    break;
                } else if (Kc.circularSubtract(oriDet1, currDet2Pd) > 0) {
                    tempret = (double) (d - 1) + Kc.circularSubtract(oriDet1, prevDet2Pd) / Kc.circularSubtract(currDet2Pd, prevDet2Pd);
                    break;
                } else if (Kc.circularSubtract(oriDet1, currDet2Md) == 0) {
                    tempret = -(double) d;
                    break;
                } else if (Kc.circularSubtract(oriDet1, currDet2Md) > 0) {
                    tempret = -(double) (d - 1) - Kc.circularSubtract(oriDet1, prevDet2Md) / Kc.circularSubtract(currDet2Md, prevDet2Md);
                    break;
                }
                prevDet2Pd = currDet2Pd;
                prevDet2Md = currDet2Md;
            }
            return tempret;
        }
    }

    @Override
    public void stateChangedNNJImplAllChannels(NNJChangeEvent evt) {
        super.stateChangedNNJImplAllChannels(evt);
        hilbPhBuff = new double[this.getDataLayout().getChannelCount()][];
    }

    @Override
    public void stateChangedNNJImplSomeChannels(NNJChangeEvent evt) {
        super.stateChangedNNJImplSomeChannels(evt);
        HashSet<Integer> changed = evt.getChangedChannels();
        for (int det : changed) {
            hilbPhBuff[det] = null;
        }
    }
}
