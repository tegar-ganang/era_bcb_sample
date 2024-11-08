package edu.georgetown.nnm;

import edu.georgetown.nnj.data.layout.NNJDataLayout;
import edu.georgetown.nnj.data.layout.NNJDataLayout464ii;
import java.awt.Rectangle;

/**This is an adapter class for easy use of NounouJ, in particular for data based
 * on the WuTech 464ii array. To use this class in Mathematica, do the following
 * setup:<br>
 * <ul><li><code>Needs["JLink`"]</li>
 * <li><code>AddToClasspath["C:\\My Documents\\.....\\NounouJ.jar"];</li>
 * <li>LoadJavaClass["edu.georgetown.nnm.N464ii"];</code></li></ul>
 * To use this class in Matlab, do the following setup<br>
 * <ul><li><code>javaaddpath('C:\My Documents\.....\NounouJ.jar');</li>
 * <li>import('edu.georgetown.nnm.N464ii');</code></li></ul>
 * Once this setup is complete, the functions ("static methods" in Java terminology)
 * in this class can be used just like native Mathematica/Matlab functions. Just
 * prepend the class name "N464ii", so in order to get the spatial coordinates
 * of detector #100, in Mathematica, type:<br>
 * <code>N464ii`getDetectorCoordinates[99]</code><br>
 * and int Matlab, type:<br>
 * <code>N464ii.getDetectorCoordinates(99)</code><br>
 * Notice that the detector argument is 99 (100-1). This stems from the fact that Java
 * arrays (like arrays in most programming languages) are 0-based, so the first
 * array entry has an index of 0. In technical computing environments such
 * as Mathematica or Matlab, arrays are typically 1-based, and start with an
 * index of 1.<p>
 * 
 * @author Kenta Takagaki
 */
public class N464ii {

    private static NNJDataLayout layout = NNJDataLayout464ii.INSTANCE;

    public static NNMDataSource load(String fileName) {
        NNMDataSource tempret = new NNMDataSource(fileName);
        layout = tempret.getDataLayout();
        return tempret;
    }

    /**Reads the file data as a second order array of double values, scaled to the correct
     * units (ie for WuTech 464II, mV).
     * @param fileName
     */
    public static double[][] readFile(String fileName) {
        NNMDataSource tempret = new NNMDataSource(fileName);
        layout = tempret.getDataLayout();
        return tempret.readDataAbsolute();
    }

    public static double[] readFileTrace(String fileName, int channel) {
        NNMDataSource tempret = new NNMDataSource(fileName);
        layout = tempret.getDataLayout();
        return tempret.readDataTraceAbsolute(channel);
    }

    public static double[] readFileTrace(String fileName, int[] channels) {
        throw new UnsupportedOperationException("Just a stub, not programmed yet.");
    }

    /**The number of detector channels in the data array. In the data array,
     detector channels are laid out first. Therefore, detector data is contained
     in data[0...getDetectorCount()-1][]. Non-detector analog data is stored in
     data[getDetectorCount()...getChannelCount()-1][].*/
    public static int getDetectorCount() {
        return layout.getDetectorCount();
    }

    ;

    /**The total number of channels in the data array, which should
     * be the sum of the detectors and the analog channels.*/
    public static int getChannelCount() {
        return layout.getChannelCount();
    }

    ;

    /**Checks whether the input is a valid detector value.*/
    public static boolean isValidDetector(int detector) {
        return layout.isValidDetector(detector);
    }

    ;

    /**Checks whether the input is a valid channel value.*/
    public static boolean isValidChannel(int channel) {
        return layout.isValidChannel(channel);
    }

    ;

    /** The bounding rectangle of the detector field, given as {{x0, y0},{x1, y1}}.*/
    public static double[][] getDetectorField() {
        Rectangle r = layout.getDetectorField();
        return new double[][] { { r.getX(), r.getY() }, { r.getX() + r.getWidth(), r.getY() + r.getHeight() } };
    }

    ;

    /** X origin of the bounding rectangle of the detector field.*/
    public static int getDetectorFieldX() {
        return layout.getDetectorFieldX();
    }

    /** Y origin of the bounding rectangle of the detector field.*/
    public static int getDetectorFieldY() {
        return layout.getDetectorFieldY();
    }

    ;

    /** Width of the bounding rectangle of the detector field.*/
    public static int getDetectorFieldWidth() {
        return layout.getDetectorFieldWidth();
    }

    ;

    /** Height of the bounding rectangle of the detector field.*/
    public static int getDetectorFieldHeight() {
        return layout.getDetectorFieldHeight();
    }

    ;

    /** Width/height of the bounding rectangle of the detector field.*/
    public static double getDetectorFieldAspectRatio() {
        return layout.getDetectorFieldAspectRatio();
    }

    ;

    /** Center coordinate of chosen detector (or channel,
     *  if it is designated a part of the field for display.*/
    public static int[] getDetectorCoordinates(int detector) {
        return layout.getDetectorCoordinates(detector);
    }

    ;

    /** X coordinate of chosen detector (or channel,
     *  if it is designated a part of the field for display.*/
    public static int getDetectorX(int detector) {
        return layout.getDetectorX(detector);
    }

    ;

    /** Y coordinate of chosen detector (or channel,
     *  if it is designated a part of the field for display.*/
    public static int getDetectorY(int detector) {
        return layout.getDetectorY(detector);
    }

    ;

    /** Detector which covers the chosen coordinates.*/
    public static int getCoordinateDetector(int[] coordinates) {
        return layout.getCoordinateDetector(coordinates);
    }

    ;

    /** Detector which covers the chosen coordinates.*/
    public static int getCoordinateDetector(int x, int y) {
        return layout.getCoordinateDetector(x, y);
    }

    ;

    /** Geometric radius of detectors.*/
    public static int getDetectorRadius() {
        return layout.getDetectorRadius();
    }

    ;

    /**Takes a single frame worth of data (in 1d) and converts it into matrix form,
     * (in 2d) which is suitable for filtering, etc. For square-packed data,
     * this is relatively straight forward, but for hexagonally-packed data,
     * it requires tilting of the matrix.
     * @see edu.georgetown.nnj.data.layout.NNJDataLayout464ii */
    public static int[][] frameToMatrix(int[] frame) {
        return layout.frameToMatrix(frame);
    }

    ;

    /**Takes a matrix form of data (in 2-d), which is suitable for filtering, etc,
     * and converts it back into the data form.
     * @see edu.georgetown.nnj.data.layout.NNJDataLayout464ii
     */
    public static int[] matrixToFrame(int[][] matrix) {
        return layout.matrixToFrame(matrix);
    }

    ;

    /** Takes matrix indices for a detector, and convertis it to a detector number.
     * @return A return value of -1 means that the matrix coordinates were oob (0 means detector "#1").
     */
    public static int matrixToDetector(int matrix0, int matrix1) {
        return layout.matrixToDetector(matrix0, matrix1);
    }

    ;

    /** Takes a detector number, and returns the matrix coordinate.
     * @return A return value of null means that the detector was invalid.
     */
    public static int[] detectorToMatrix(int detector) {
        return layout.detectorToMatrix(detector);
    }

    ;

    /**Takes the frame of data as a list, and pops up a window with a frame plot.
     * By default, this assumes that the data is scaled in the range [0, 1].
     */
    public static void displayFrame(double[] frameData) {
        throw new UnsupportedOperationException("Just a stub, not programmed yet.");
    }

    /**Takes the segment of data as a matrix, and pops up a window with a movie plot.
     * By default, the data will be rescaled variably for each detector trace.
     *
     * @param traceChannel which channel to use for the trace display
     */
    public static void displayMovie(double[][] data, int traceChannel) {
        displayMovie(data, new int[] { traceChannel });
    }

    /**Takes the segment of data as a matrix, and pops up a window with a movie plot.
     * By default, the data will be rescaled variably for each detector trace.
     * 
     * @param traceChannels which channels to use for the trace display
     */
    public static void displayMovie(double[][] data, int[] traceChannels) {
        throw new UnsupportedOperationException("Just a stub, not programmed yet.");
    }

    /**Takes the segment of data as a matrix, and pops up a window with a page plot.
     * By default, the data will be rescaled variably.
     */
    public static void displayPage(double[][] data, int traceChannel) {
        throw new UnsupportedOperationException("Just a stub, not programmed yet.");
    }
}
