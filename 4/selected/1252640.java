package edu.georgetown.nnj.data.layout;

import de.ifn_magdeburg.kazukazuj.util.KKJArrays;
import java.util.Arrays;

/**This abstract class fills in some implementation for NNJDataLayout.
 *
 * @author Kentaroh Takagaki
 */
public abstract class NNJAbstractDataLayout implements NNJDataLayout {

    protected NNJAbstractDataLayout() {
    }

    @Override
    public boolean isCompatible(NNJDataLayout layout) {
        return layout.getClass() == this.getClass();
    }

    @Override
    public String toString() {
        return this.getClass().toString() + "... (detector count)=" + this.getDetectorCount() + ",  (channel count)=" + this.getChannelCount() + "... (direction count 1)=" + this.getDirectionCount(1) + ",  (neighbor rings)=" + this.getMaxRing();
    }

    @Override
    public final boolean isValidDetector(int detector) {
        return (0 <= detector && detector < this.getDetectorCount());
    }

    @Override
    public final boolean isValidChannel(int channel) {
        return (0 <= channel && channel < this.getChannelCount());
    }

    @Override
    public int getCoordinateDetector(int[] coordinates) {
        assert coordinates.length == 2 : "Coordinates must be an array of length 2.";
        return getCoordinateDetector(coordinates[0], coordinates[1]);
    }

    @Override
    public final int getNeighborDirection(int det, int neighbor) {
        return getNeighborDirection(det, neighbor, 1);
    }

    @Override
    public final int getNeighbor(int det, int direction) {
        return getNeighbor(det, direction, 1);
    }

    @Override
    public final int[] getNeighbors(int det) {
        return getNeighbors(det, 1);
    }

    @Override
    public final int[] getNeighborsInclusive(int det, int n) {
        int[][] tempret = new int[n + 1][];
        tempret[0] = new int[] { det };
        for (int k = 1; k <= n; k++) {
            tempret[k] = getNeighbors(det, n);
        }
        return KKJArrays.flatten(tempret);
    }

    @Override
    public final boolean isEdge(int det) {
        return isEdge(det, 1);
    }

    @Override
    public boolean isEdge(int det, int ring) {
        if (isValidNeighborRing(ring)) {
            if (this.isValidDetector(det)) {
                int[] neighbors = getNeighbors(det, ring);
                if (Arrays.binarySearch(neighbors, -1) < 0) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public final int getOppositeDirection(int direction, int n) {
        int dc = this.getDirectionCount(n);
        return (direction + dc / 2) % dc;
    }

    @Override
    public final boolean isValidDirection(int direction) {
        return isValidDirection(direction, 1);
    }

    @Override
    public final boolean isValidDirection(int direction, int ring) {
        return 0 <= direction && direction < getDirectionCount(ring);
    }

    @Override
    public final boolean isValidNeighborRing(int ring) {
        return 0 < ring && ring <= this.getMaxRing();
    }
}
