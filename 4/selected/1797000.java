package edu.georgetown.nnj.data.layout;

import java.awt.Rectangle;

/**
 *
 * @author Kentaroh Takagaki
 * @version 0.4.0
 */
public final class NNJDataLayout464iiBinned extends NNJAbstractDataLayoutBinnned {

    public static final NNJDataLayout464iiBinned INSTANCE = new NNJDataLayout464iiBinned();

    @Override
    public NNJDataLayout464iiBinned getInstance() {
        return INSTANCE;
    }

    private int[] BIN_CHANNELS = new int[] { 8, 238, 20, 250, 25, 255, 31, 260, 46, 273, 52, 280, 59, 287, 294, 67, 74, 301, 83, 309, 316, 92, 318, 325, 101, 108, 334, 117, 343, 350, 127, 134, 359, 136, 143, 369, 151, 378, 385, 159, 166, 393, 174, 401, 181, 409, 416, 194, 424, 201, 429, 206, 435, 218, 448, 222, 453 };

    private int[] ORI_TO_BIN = new int[] { 8, 8, -1, -1, -1, -1, -1, 8, -1, 8, -1, -1, 20, 20, -1, 8, 8, 25, 25, 20, -1, -1, 31, 31, 25, -1, 25, 20, 20, -1, 31, -1, 31, 25, 25, 45, 45, 260, -1, -1, 31, 31, 52, 52, 45, -1, 45, -1, -1, 59, 59, 52, -1, 52, 45, 45, 67, 67, 59, -1, 59, 52, 52, 74, 74, 287, 67, -1, 67, 59, 59, 83, 83, 74, -1, 74, 287, -1, 67, 67, 92, 92, 83, -1, 83, 74, 74, 318, -1, 101, 101, 92, -1, 92, 83, 83, 108, 108, 318, -1, 101, -1, 101, 92, 92, 117, 117, 108, -1, 108, 318, -1, 101, 101, 127, 127, 117, -1, 117, 108, 108, 134, 134, -1, 136, 136, 127, -1, 127, 117, 117, 143, 143, 134, -1, 136, -1, 136, 127, 127, 151, 151, 143, -1, 143, 134, 136, 136, 159, 159, 151, -1, 151, 143, 143, 166, 166, -1, 159, -1, 159, 151, 151, 174, 174, 166, -1, 166, -1, 159, 159, 181, 181, 174, -1, 174, 166, 166, -1, -1, 181, -1, 181, 174, 174, 194, 194, -1, -1, 181, 181, 200, 200, 194, -1, 194, -1, 206, 206, 200, -1, 200, 194, 194, 435, 206, -1, 206, 200, 200, 218, 218, 435, 206, 206, 222, 222, 218, -1, 218, -1, 222, -1, 222, 218, 218, -1, 222, 222, -1, -1, -1, 238, 238, -1, -1, -1, 238, -1, 238, -1, -1, 250, 250, 20, 238, 238, 255, 255, 250, -1, 250, 260, 260, 255, -1, 255, 250, 250, -1, -1, 260, 255, 255, 273, 273, -1, -1, 260, 260, 280, 280, 273, -1, 273, -1, -1, 287, 287, 280, -1, 280, 273, 273, 294, 294, -1, -1, 287, 280, 280, 301, 301, 294, -1, 294, -1, 287, 309, 309, 301, -1, 301, 294, 294, 316, 316, 318, 309, -1, 309, 301, 301, 325, 325, 316, -1, 316, -1, 318, 309, 309, 334, 334, 325, -1, 325, 316, 316, -1, 318, 343, 343, 334, -1, 334, 325, 325, 350, 350, -1, -1, 343, -1, 343, 334, 334, 359, 359, 350, -1, 350, -1, 134, 343, 343, 369, 369, 359, -1, 359, 350, 350, -1, -1, 134, 378, 378, 369, -1, 369, 359, 359, 385, 385, -1, -1, 378, -1, 378, 369, 369, 393, 393, 385, -1, 385, -1, 378, 378, 401, 401, 393, -1, 393, 385, 385, -1, 409, 409, 401, -1, 401, 393, 393, 416, 416, -1, 409, -1, 409, 401, 401, 423, 423, 416, -1, 416, 409, 409, 429, 429, 423, -1, 423, 416, 416, 435, 429, -1, 429, 423, 423, -1, -1, -1, 435, 429, 429, 448, 448, -1, -1, 435, 435, 453, 453, 448, -1, 448, -1, -1, 453, -1, 453, 448, 448, -1, -1, -1, 453, 453, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

    public int[] getBinChannels(int binnedDetector) {
        return NNJDataLayout464ii.INSTANCE.getNeighbors(BIN_CHANNELS[binnedDetector], 1);
    }

    private static String NAME = "WuTech464iiBinned";

    @Override
    public String getName() {
        return NAME;
    }

    public static final int DETECTOR_COUNT = 57;

    public static final int CHANNEL_COUNT = 57 + 8;

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

    private final int[][] DET_TO_COORD_DATA = { { 7300000, 8167434 }, { 4500000, 8167434 }, { 5500000, 7821024 }, { 2700000, 7821024 }, { 6500000, 7474613 }, { 3700000, 7474613 }, { 7500000, 7128203 }, { 4700000, 7128203 }, { 5300000, 6781793 }, { 2900000, 6781793 }, { 6700000, 6435383 }, { 3900000, 6435383 }, { 7700000, 6088973 }, { 4900000, 6088973 }, { 2100000, 6088973 }, { 8700000, 5742563 }, { 5900000, 5742563 }, { 3100000, 5742563 }, { 6900000, 5396152 }, { 4100000, 5396152 }, { 1300000, 5396152 }, { 7900000, 5049742 }, { 5100000, 5049742 }, { 2300000, 5049742 }, { 8900000, 4703332 }, { 6100000, 4703332 }, { 3300000, 4703332 }, { 7100000, 4356922 }, { 4300000, 4356922 }, { 1500000, 4356922 }, { 8100000, 4010512 }, { 5300000, 4010512 }, { 2500000, 4010512 }, { 9100000, 3664102 }, { 6300000, 3664102 }, { 3500000, 3664102 }, { 7300000, 3317691 }, { 4500000, 3317691 }, { 1700000, 3317691 }, { 8300000, 2971281 }, { 5500000, 2971281 }, { 2700000, 2971281 }, { 6500000, 2624871 }, { 3700000, 2624871 }, { 7500000, 2278461 }, { 4700000, 2278461 }, { 1900000, 2278461 }, { 5700000, 1932051 }, { 2500000, 1932051 }, { 6300000, 1585641 }, { 3900000, 1585641 }, { 7700000, 1239230 }, { 4900000, 1239230 }, { 5900000, 892820 }, { 3100000, 892820 }, { 6900000, 546410 }, { 4100000, 546410 } };

    @Override
    public final int[] getDetectorCoordinates(int detector) {
        assert isValidDetector(detector) : "Invalid detector";
        return NNJDataLayout464ii.INSTANCE.getDetectorCoordinates(BIN_CHANNELS[detector]);
    }

    public final int getDetectorX(int detector) {
        assert isValidDetector(detector) : "Invalid detector";
        return NNJDataLayout464ii.INSTANCE.getDetectorX(BIN_CHANNELS[detector]);
    }

    public final int getDetectorY(int detector) {
        assert isValidDetector(detector) : "Invalid detector";
        return NNJDataLayout464ii.INSTANCE.getDetectorY(BIN_CHANNELS[detector]);
    }

    @Override
    public final int getCoordinateDetector(int x, int y) {
        int tempdet = NNJDataLayout464ii.INSTANCE.getCoordinateDetector(x, y);
        if (tempdet == -1) return -1; else return ORI_TO_BIN[tempdet];
    }

    @Override
    public int matrixToDetector(int matrix0, int matrix1) {
        throw new UnsupportedOperationException("Not supported for this format");
    }

    @Override
    public final int[] detectorToMatrix(int det) {
        throw new UnsupportedOperationException("Not supported for this format");
    }

    @Override
    public final int[][] frameToMatrix(int[] frame) {
        throw new UnsupportedOperationException("Not supported for this format");
    }

    @Override
    public final int[] matrixToFrame(int[][] matrix) {
        throw new UnsupportedOperationException("Not supported for this format");
    }

    private static final int[] DIRECTION_COUNT = { 0, 6, 12, 18 };

    @Override
    public final int getDirectionCount(int ring) {
        if (isValidNeighborRing(ring)) {
            return DIRECTION_COUNT[ring];
        }
        return -1;
    }

    private static final int MAX_RING = 3;

    @Override
    public final int getMaxRing() {
        return MAX_RING;
    }

    @Override
    public int getNeighbor(int det, int direction, int ring) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final int[] getNeighbors(int det, int ring) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getNeighborDirection(int det, int neighbor, int ring) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static int[][] getGetNeighborsData(int n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] getNeighborVector(int direction, int ring) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[][] getNeighborVectors(int ring) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] getNeighborVectorFlowExtension(int direction, int ring) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[][] getNeighborVectorsFlowExtension(int ring) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int[][] getNeighborCombinationsFlowExtension(int detector, int ring) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean[][] getIsEdgeData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public final boolean isEdge(int det, int ring) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
