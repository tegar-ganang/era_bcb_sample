package edu.georgetown.nnj.filterblocks.flow;

import de.ifn_magdeburg.kazukazuj.K;
import edu.georgetown.nnj.data.NNJDataSource;
import edu.georgetown.nnj.data.NNJDataWindow;
import edu.georgetown.nnj.data.layout.NNJDataLayout;
import edu.georgetown.nnj.data.layout.NNJDataLayoutNull;
import edu.georgetown.nnj.events.NNJChangeEvent;
import edu.georgetown.nnj.filterblocks.filters.NNJFilterPassthru;
import java.util.HashSet;

/**Provides a framework for pairwise flow detection. calculatePairwiseFlow(int, int) must be
 * implemented in children.
 *
 * @author Kenta
 * @version 0.4.0
 */
public abstract class NNJFilterPairwiseFlow extends NNJFilterPassthru {

    public final String filterName = "Pairwise flow calculator (" + this.getClass().getName() + ")";

    @Override
    public String getFilterName() {
        return filterName;
    }

    ;

    @Override
    public String getFilterStateDescription() {
        return NULL_STRING;
    }

    public NNJFilterPairwiseFlow(NNJDataSource source) {
        super(source);
    }

    public NNJFilterPairwiseFlow(NNJDataSource source, boolean filterActive) {
        super(source, filterActive);
    }

    protected NNJFilterPairwiseFlow() {
        super();
    }

    private NNJDataLayout layout = NNJDataLayoutNull.INSTANCE;

    @Override
    public void setNNJDataSourceImpl() {
        this.layout = getDataLayout();
    }

    private int maxShift = 16;

    /** How many frames maximum to shift when searching for correlations.*/
    public void setMaxShift(int val) {
        if (val <= 0) {
            throw new IllegalArgumentException("maxShift must be >0!");
        } else if (val != this.maxShift) {
            this.maxShift = val;
            flushBuffer();
        }
    }

    /** How many frames maximum to shift when searching for correlations.*/
    public int getMaxShift() {
        return this.maxShift;
    }

    private boolean calcInv = false;

    public void setCalculateInverse(boolean val) {
        if (val != this.calcInv) {
            this.calcInv = val;
            this.flushBuffer();
        }
    }

    /** How many frames maximum to shift when searching for correlations.*/
    public boolean getCalculateInverse() {
        return this.calcInv;
    }

    double[][][] buffer;

    public void flushBuffer() {
        buffer = new double[getDataLayout().getChannelCount()][][];
    }

    public final void flushBuffer(HashSet<Integer> changed) {
        if (calcInv == false) {
            for (int chChannel : changed) {
                buffer[chChannel] = null;
                if (chChannel < buffer.length - 2) {
                    for (int moreC = chChannel + 1; moreC < buffer.length; moreC++) {
                        if (buffer[moreC] != null) {
                            buffer[moreC][chChannel] = null;
                        }
                    }
                }
            }
        } else {
            for (int chChannel : changed) {
                buffer[chChannel] = null;
                for (int count = 0; count < buffer.length; count++) {
                    if (buffer[count] != null) {
                        buffer[count][chChannel] = null;
                    }
                }
            }
        }
    }

    private int currentFrame = -1;

    public void checkCurrentFrame(int newFrame) {
        if (currentFrame != newFrame) {
            currentFrame = newFrame;
            flushBuffer();
        }
    }

    private NNJDataWindow internalWindow = new NNJDataWindow(this.getDataWindow());

    public void checkCurrentWindow() {
        if (!internalWindow.equals(this.getDataWindow())) {
            flushBuffer();
        }
    }

    /** Returns the pairwise flow for detector 2 in relationship to detector1,
     * from buffer (will load buffer if not found).
     * Implemented so that if det2 < det1, then -getPairwiseFlow(det1, det2) is returned,
     * which assumes that locally, the delays are reciprocal.<p>
     *
     * @param frame frame at which to detect flow
     * @param det1 center detector
     * @param det2 detector to be compared to
     * @return
     */
    public double[] getPairwiseFlow(int frame, int det1, int det2) {
        checkCurrentFrame(frame);
        checkCurrentWindow();
        return getPairwiseFlowImpl(det1, det2);
    }

    /**Gets an array of pairwise flows.
     * @see getPairwiseFlow(int, int, int)
     * @param dets int[number of pairs][2] should give a list of pairs to return
     * @return list of pairwise flows for the speicified detector pairs
     */
    public double[][] getPairwiseFlows(int frame, int[][] dets) {
        checkCurrentFrame(frame);
        checkCurrentWindow();
        double[][] tempret = new double[dets.length][];
        for (int k = 0; k < tempret.length; k++) {
            tempret[k] = getPairwiseFlowImpl(dets[k][0], dets[k][1]);
        }
        return tempret;
    }

    private double[] getPairwiseFlowImpl(int det1, int det2) {
        if (det1 == det2) {
            throw new IllegalArgumentException("det1 and det2 cannot be equal!");
        }
        if (this.calcInv) {
            if (buffer[det1] == null) {
                buffer[det1] = new double[this.getDataLayout().getChannelCount()][];
                double[] tempret = calculatePairwiseFlow(this.currentFrame, det1, det2);
                buffer[det1][det2] = tempret;
                return tempret;
            } else if (buffer[det1][det2] == null) {
                double[] tempret = calculatePairwiseFlow(this.currentFrame, det1, det2);
                buffer[det1][det2] = tempret;
                return tempret;
            } else {
                return buffer[det1][det2];
            }
        } else {
            boolean invert = false;
            int dMore, dLess;
            if (det1 > det2) {
                dMore = det1;
                dLess = det2;
            } else {
                dMore = det2;
                dLess = det1;
                invert = true;
            }
            if (buffer[dMore] == null) {
                buffer[dMore] = new double[dMore][];
                buffer[dMore][dLess] = calculatePairwiseFlow(this.currentFrame, dMore, dLess);
            } else if (buffer[dMore][dLess] == null) {
                buffer[dMore][dLess] = calculatePairwiseFlow(this.currentFrame, dMore, dLess);
            }
            if (invert) {
                return new double[] { -buffer[dMore][dLess][0], buffer[dMore][dLess][1] };
            } else {
                return buffer[dMore][dLess];
            }
        }
    }

    /**The core method to be implemented by each implementing child.*/
    public abstract double[] calculatePairwiseFlow(int frame, int det1, int det2);

    protected static boolean argumentChecks(int[] data1, int[] data2, int frame, int window, int maxShift) {
        int windowH = window / 2;
        if (window < 1) {
            throw new RuntimeException("window cannot be <1, value was: " + Integer.toString(window));
        }
        if (frame - windowH - maxShift < 0 || K.min(data1.length, data2.length) - 1 < frame + windowH + maxShift) {
            throw new RuntimeException("cannot calculate for frame: " + Integer.toString(frame) + " with window: " + Integer.toString(window) + ", data lengths are: " + Integer.toString(data1.length) + " & " + Integer.toString(data2.length));
        }
        return true;
    }

    protected static boolean argumentChecks(int[] data1, int[] data2, int[] frames, int window, int maxShift) {
        int windowH = window / 2;
        if (window < 1) {
            throw new RuntimeException("window cannot be <1, value was: " + Integer.toString(window));
        }
        int maxCount = K.min(data1.length, data2.length) - 1;
        for (int k = 0; k < frames.length; k++) {
            if (frames[k] - windowH - maxShift < 0 || maxCount < frames[k] + windowH + maxShift) {
                throw new RuntimeException("cannot calculate for frame: " + Integer.toString(frames[k]) + " with window: " + Integer.toString(window) + ", data lengths are: " + Integer.toString(data1.length) + " & " + Integer.toString(data2.length));
            }
        }
        return true;
    }

    @Override
    public void stateChangedNNJImplCommonPre(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplCommonPost(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplAllChannels(NNJChangeEvent evt) {
        flushBuffer();
    }

    @Override
    public void stateChangedNNJImplSomeChannels(NNJChangeEvent evt) {
        flushBuffer(evt.getChangedChannels());
    }

    @Override
    public void stateChangedNNJImplLayout(NNJChangeEvent evt) {
        flushBuffer();
    }

    @Override
    public void stateChangedNNJImplWindow(NNJChangeEvent evt) {
    }

    @Override
    public void stateChangedNNJImplMask(NNJChangeEvent evt) {
    }
}
