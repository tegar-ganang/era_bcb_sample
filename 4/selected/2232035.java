package edu.georgetown.nnj.data.layout;

import de.ifn_magdeburg.kazukazuj.util.KKJArrays;
import static edu.georgetown.nnj.data.layout.NNJDataLayout464iiConstants.*;
import static edu.georgetown.nnj.data.layout.NNJDataLayout464iiConstantsB.*;
import static edu.georgetown.nnj.data.layout.NNJDataLayout464iiConstantsC.*;
import static edu.georgetown.nnj.data.layout.NNJDataLayout464iiConstantsD.*;
import static edu.georgetown.nnj.data.layout.NNJDataLayout464iiConstantsE.*;
import java.awt.Rectangle;
import java.util.Arrays;

/**
 *
 * @author Kentaroh Takagaki
 * @version 0.4.0
 */
public final class NNJDataLayout464ii extends NNJAbstractDataLayout {

    public static final NNJDataLayout464ii INSTANCE = new NNJDataLayout464ii();

    @Override
    public NNJDataLayout464ii getInstance() {
        return INSTANCE;
    }

    public NNJDataLayout464ii() {
    }

    private static String NAME = "WuTech464ii";

    @Override
    public String getName() {
        return NAME;
    }

    public static final int DETECTOR_COUNT = 464;

    public static final int CHANNEL_COUNT = 472;

    @Override
    public final int getDetectorCount() {
        return DETECTOR_COUNT;
    }

    @Override
    public final int getChannelCount() {
        return CHANNEL_COUNT;
    }

    private static final int DET_FIELD_X = 0;

    private static final int DET_FIELD_Y = 0;

    private static final int DET_FIELD_WIDTH = 10000000;

    private static final int DET_FIELD_HEIGHT = 8660254;

    private static final Rectangle DET_FIELD_RECT = new Rectangle(DET_FIELD_X, DET_FIELD_Y, DET_FIELD_WIDTH, DET_FIELD_HEIGHT);

    /**The scaled size of each detector (circular).*/
    private static final int DET_RADIUS = 200000;

    @Override
    public final Rectangle getDetectorField() {
        return DET_FIELD_RECT;
    }

    @Override
    public final int getDetectorFieldX() {
        return DET_FIELD_X;
    }

    @Override
    public final int getDetectorFieldY() {
        return DET_FIELD_Y;
    }

    @Override
    public final int getDetectorFieldWidth() {
        return DET_FIELD_WIDTH;
    }

    @Override
    public final double getDetectorFieldAspectRatio() {
        return (double) DET_FIELD_HEIGHT / (double) DET_FIELD_WIDTH;
    }

    @Override
    public final int getDetectorFieldHeight() {
        return DET_FIELD_HEIGHT;
    }

    @Override
    public final int getDetectorRadius() {
        return DET_RADIUS;
    }

    @Override
    public final int[] getDetectorCoordinates(int detector) {
        assert isValidDetector(detector) : "Invalid detector";
        return DET_TO_COORD_DATA[detector];
    }

    @Override
    public final int getDetectorX(int detector) {
        assert isValidDetector(detector) : "Invalid detector";
        return DET_TO_COORD_DATA[detector][0];
    }

    @Override
    public final int getDetectorY(int detector) {
        assert isValidDetector(detector) : "Invalid detector";
        return DET_TO_COORD_DATA[detector][1];
    }

    @Override
    public final int getCoordinateDetector(int x, int y) {
        double dx = (double) x;
        double dy = (double) y;
        int arrX = (int) Math.round((-69d - 2d * Math.sqrt(3d) + 3 * dx / 100000d + Math.sqrt(3d) * dy / 100000d) / 12d);
        int arrY = (int) Math.round((-1d + Math.sqrt(3d) + dy / 200000d) / Math.sqrt(3d));
        int ret;
        if (arrX < 1 || 25 < arrX || arrY < 1 || 25 < arrY) {
            ret = -1;
        } else {
            ret = matrixToDetector(arrX, arrY);
        }
        return (ret);
    }

    @Override
    public final int matrixToDetector(int x, int y) {
        return matrixToDetectorStaticImpl(x, y);
    }

    public static final int matrixToDetectorStaticImpl(int[] mat) {
        return matrixToDetectorStaticImpl(mat[0], mat[1]);
    }

    public static final int matrixToDetectorStaticImpl(int x, int y) {
        if (1 <= x && x <= 25 && 1 <= y && y <= 25) {
            return (MAT_TO_DET_DATA[x - 1][y - 1]);
        } else return -1;
    }

    @Override
    public final int[] detectorToMatrix(int det) {
        if (isValidDetector(det)) {
            return DET_TO_MAT_DATA[det];
        } else {
            return null;
        }
    }

    @Override
    public final int[][] frameToMatrix(int[] frame) {
        assert frame != null : "Cannot convert a null frame.";
        int tempMax = frame.length;
        assert tempMax < this.getDetectorCount() : "Invalid frame size.";
        if (tempMax >= this.getChannelCount()) {
            tempMax = this.getChannelCount();
        } else if (tempMax >= this.getDetectorCount()) {
            tempMax = this.getDetectorCount();
        }
        int[][] tempret = new int[25][25];
        for (int ch = 0; ch < tempMax; ch++) {
            int[] tempMXY = DET_TO_MAT_DATA[ch];
            tempret[tempMXY[0] - 1][tempMXY[1] - 1] = frame[ch];
        }
        return tempret;
    }

    @Override
    public final int[] matrixToFrame(int[][] matrix) {
        assert matrix.length == 25 && matrix[0].length == 25 : "Incorrect matrix dimensions.";
        int[] tempret = new int[this.getChannelCount()];
        for (int ch = 0; ch < tempret.length; ch++) {
            int[] tempMXY = DET_TO_MAT_DATA[ch];
            tempret[ch] = matrix[tempMXY[0] - 1][tempMXY[1] - 1];
        }
        return tempret;
    }

    private static final int[] DIRECTION_COUNT = { 0, 6, 12, 18, 24 };

    @Override
    public final int getDirectionCount(int ring) {
        if (isValidNeighborRing(ring)) {
            return DIRECTION_COUNT[ring];
        }
        return -1;
    }

    private static final int MAX_RING = 4;

    @Override
    public final int getMaxRing() {
        return MAX_RING;
    }

    @Override
    public int getNeighbor(int det, int direction, int ring) {
        if (isValidDetector(det) && isValidDirection(direction, ring) && isValidNeighborRing(ring)) {
            return getNeighborImpl(det, direction, ring);
        } else {
            return -1;
        }
    }

    public final int getNeighborImpl(int det, int direction, int ring) {
        return GET_NEIGHBORS_DATA[ring][det][direction];
    }

    @Override
    public final int[] getNeighbors(int det, int ring) {
        if (isValidDetector(det) && isValidNeighborRing(ring)) {
            return GET_NEIGHBORS_DATA[ring][det].clone();
        } else {
            return null;
        }
    }

    @Override
    public int getNeighborDirection(int det, int neighbor, int ring) {
        if (isValidDetector(det) && isValidNeighborRing(ring)) {
            for (int dir = 0; dir < this.getDirectionCount(ring); dir++) {
                if (GET_NEIGHBORS_DATA[ring][det][dir] == neighbor) {
                    return dir;
                }
            }
        }
        return -1;
    }

    public static int[][][] GET_NEIGHBORS_DATA = GET_NEIGHBORS_DATA_1;

    static {
        GET_NEIGHBORS_DATA[3] = GET_NEIGHBORS_DATA_3;
        GET_NEIGHBORS_DATA[4] = new int[464][1];
        KKJArrays.overWriteArray(GET_NEIGHBORS_DATA[4], GET_NEIGHBORS_DATA_41, 0);
        KKJArrays.overWriteArray(GET_NEIGHBORS_DATA[4], GET_NEIGHBORS_DATA_42, 232);
    }

    public static int[][] getGetNeighborsData(int n) {
        return GET_NEIGHBORS_DATA[n];
    }

    @Override
    public double[] getNeighborVector(int direction, int ring) {
        if (isValidDirection(direction) && isValidNeighborRing(ring)) {
            return NEIGHBOR_VECTORS[ring][direction].clone();
        }
        return null;
    }

    @Override
    public double[][] getNeighborVectors(int ring) {
        if (isValidNeighborRing(ring)) {
            return Arrays.copyOf(NEIGHBOR_VECTORS[ring], DIRECTION_COUNT[ring]);
        }
        return null;
    }

    @Override
    public double[] getNeighborVectorFlowExtension(int direction, int ring) {
        if (isValidNeighborRing(ring) && 0 <= direction && direction < DIRECTION_COUNT[ring] * 2) {
            return NEIGHBOR_VECTORS[ring][direction].clone();
        }
        return null;
    }

    @Override
    public double[][] getNeighborVectorsFlowExtension(int ring) {
        if (isValidNeighborRing(ring)) {
            return NEIGHBOR_VECTORS[ring].clone();
        }
        return null;
    }

    @Override
    public int[][] getNeighborCombinationsFlowExtension(int detector, int ring) {
        int dc = DIRECTION_COUNT[ring];
        int[][] tempret = new int[dc * 2][2];
        for (int k = 0; k < dc; k++) {
            tempret[k][0] = detector;
            tempret[k][1] = getNeighborImpl(detector, k, ring);
        }
        for (int k = 0; k < dc; k++) {
            tempret[k + dc][0] = getNeighborImpl(detector, k, ring);
            tempret[k + dc][1] = getNeighborImpl(detector, (k + ring) % dc, ring);
        }
        return tempret;
    }

    public boolean[][] getIsEdgeData() {
        return ISEDGE_DATA;
    }

    @Override
    public final boolean isEdge(int det, int ring) {
        if (this.isValidNeighborRing(ring) && this.isValidDetector(det)) {
            return ISEDGE_DATA[ring][det];
        } else {
            return true;
        }
    }
}
