package edu.georgetown.nnj.data.formats;

import de.ifn_magdeburg.kazukazuj.K;
import edu.georgetown.nnj.data.layout.NNJDataLayout464ii;
import edu.georgetown.nnj.data.NNJAbstractDataSource;
import edu.georgetown.nnj.data.NNJDataFileReader;
import edu.georgetown.nnj.data.layout.NNJDataLayout;
import edu.georgetown.nnj.data.NNJDataMask;
import edu.georgetown.nnj.data.NNJDataWindow;
import de.ifn_magdeburg.kazukazuj.util.KKJArrays;
import edu.georgetown.nnj.data.NNJDataFileWriter;
import edu.georgetown.nnj.data.NNJDataSource;
import java.io.DataInputStream;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import static edu.georgetown.nnj.util.NNJUtilNumberConversion.byteArrayToIntArray16Rev;

/** Encapsulates the data for NounouJ. Internal representations
 *and calculations will utilize integers, since toInteger arithmetic is generally much
 *faster than floating-point arithmetic, and the data is not of exceedingly
 *high resolution or dynamic range to begin with. The internal representation
 *is scaled up by a certain factor (DATA_EXTRA_BITS) from the original int(16-bit)
 *values. This should be taken into account when implementing any operation
 *on these values-- the values are very large, and are therefore prone to
 *overflow, so they may need to be cast to long(64-bit).<p>
 *
 *All data sent along the data processing stream (via interface NNJDataSource)
 *is either sent by value or sent as a protective copy. Therefore, the data
 *within this class is impervious to outside modification by downstream members
 *of the processing stream.
 */
public final class NNJDataSourceBin464ii extends NNJAbstractDataSource implements NNJDataFileReader, NNJDataFileWriter {

    /** The raw data from a .da file is scaled up by multiplying the short (16-bit)
     * value with DATA_EXTRA_BITS into an (int) value.
     * Therefore, the values should fall between [-32768*DATA_EXTRA_BITS,
     * 32768*DATA_EXTRA_BITS], or equivalently [+/- DATA_SCALE].<p>
     *
     * When scaling, the resulting values fed to each drawing method are assumed to
     * fall between [+/-DATA_SCALE], and values larger than this may exceed the
     * window boundaries.<p>
     *
     *Besides the speed of toInteger arithmetic, this data format is used in order
     *to use the old (and faster) java.awt.Graphics functions, which
     *take only integers. The new Graphics2D functions taking doubles/floats was tested,
     *but it was much slower as of JDK 1.5 on a fast Wintel notebook
     *with dedicated graphics board.<p>
     *
     *This optimization may cause minor round of errors with repeated filtering,
     *etc., but also remember that the original data is only int16 to begin with.
     *This optimization may become unnecessary at some point in the future,
     *if Graphics2D functions are accelerated to draw floating point quickly.*/
    private int[][] data = { { 0, 0 }, { 0, 0 } };

    /**To be used in static read methods, to access layout information.*/
    private static NNJDataLayout464ii DATA_LAYOUT = NNJDataLayout464ii.INSTANCE;

    /**Remember to generate each time this class is constructed, or data changed.*/
    private NNJDataWindow dataWin;

    /**Remember to generate each time this class is constructed, or data changed.*/
    private NNJDataMask dataMask = new NNJDataMask();

    private boolean dataLoaded = false;

    private int frameCount;

    /**This constant provides the multiplication factor by which
     *original neuroplex data short (16-bit) is multiplied to give
     *internal representation in int (32-bit).
     */
    private static int DATA_EXTRA_BITS = 1024;

    /**The Neuroplex da file is scaled such that multiplying this value
     * to the original toInteger data values will give mV.*/
    private static double DA_SCALE_TO_MV = 0.305175781250000d;

    private static double ABS_GAIN = ((double) DATA_EXTRA_BITS) / DA_SCALE_TO_MV;

    private static String ABS_VAL_UNIT = "mV";

    private static double SAMPLING_RATE = 1600d;

    @Override
    public int getDataExtraBits() {
        return DATA_EXTRA_BITS;
    }

    @Override
    public double getAbsoluteGain() {
        return ABS_GAIN;
    }

    @Override
    public String getAbsoluteUnit() {
        return ABS_VAL_UNIT;
    }

    @Override
    public double getSamplingRate() {
        return SAMPLING_RATE;
    }

    @Override
    public int getTotalFrameCount() {
        return frameCount;
    }

    private NNJDataSourceBin464ii() {
    }

    public NNJDataSourceBin464ii(File file) throws IOException {
        loadFile(file);
    }

    /**Opens data at the path/filename "string".*/
    public NNJDataSourceBin464ii(String file) throws IOException {
        loadFile(file);
    }

    @Override
    public int readDataPointImpl(int ch, int fr) {
        if (dataLoaded) {
            return this.data[ch][fr];
        } else {
            throw new IllegalArgumentException("Data reqeusted before initialization.");
        }
    }

    @Override
    public int[] readDataTraceSegmentImpl(int ch, int start, int end, int decimationFactor) {
        if (dataLoaded) {
            return super.readDataTraceSegmentImpl(ch, start, end, decimationFactor);
        } else {
            throw new IllegalArgumentException("Data reqeusted before initialization.");
        }
    }

    @Override
    public int[] readDataTraceSegmentImpl(int ch, int start, int end) {
        if (dataLoaded) {
            return K.copy(this.data[ch], start, end);
        } else {
            throw new IllegalArgumentException("Data reqeusted before initialization.");
        }
    }

    @Override
    public NNJDataLayout getDataLayout() {
        return DATA_LAYOUT;
    }

    @Override
    public NNJDataWindow getDataWindow() {
        if (this.dataWin == null) {
            throw new NullPointerException("DataWindow requested before initialization!");
        }
        return this.dataWin;
    }

    @Override
    public NNJDataMask getDataMask() {
        if (this.dataMask == null) {
            throw new NullPointerException("DataMask requested before initialization!");
        }
        return this.dataMask;
    }

    public static double[][] readFile(File file) {
        return readFile(file, samplingRateDialog());
    }

    /** Convenience method for rapid I/O without returning object. */
    public static double[][] readFile(File file, double samplingRate) {
        SAMPLING_RATE = samplingRate;
        NNJDataSourceBin464ii data;
        double[][] tempret = null;
        try {
            data = new NNJDataSourceBin464ii(file);
            tempret = K.toDouble(data.readData());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        tempret = K.multiply(tempret, DA_SCALE_TO_MV);
        return tempret;
    }

    public static double[][] readFile(String fileStr) {
        return readFile(fileStr, samplingRateDialog());
    }

    /** Convenience method for rapid I/O without returning object. */
    public static double[][] readFile(String fileStr, double samplingRate) {
        SAMPLING_RATE = samplingRate;
        double[][] tempret = null;
        File file = new File(fileStr);
        tempret = readFile(file);
        return tempret;
    }

    /** Convenience method for rapid I/O of a single trace from muliple files. */
    public static double[] readFileTrace(File file, int channel) {
        NNJDataSourceBin464ii data = null;
        double[] tempret = null;
        try {
            data = new NNJDataSourceBin464ii(file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        tempret = K.toDouble(data.readDataTrace(channel));
        tempret = K.multiply(tempret, DA_SCALE_TO_MV);
        return tempret;
    }

    /** Convenience method for rapid I/O of a single trace from muliple files. */
    public static double[] readFileTrace(String fileStr, int channel) {
        double[] tempret = null;
        File file = new File(fileStr);
        tempret = readFileTrace(file, channel);
        return tempret;
    }

    public static int[] readFileTraceImpl(File file, int det) {
        FileInputStream fin;
        try {
            fin = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            throw new IllegalArgumentException("File not found!");
        }
        DataInputStream din = new DataInputStream(fin);
        ArrayList<Integer> tempret = new ArrayList<Integer>();
        int tempCh = DATA_LAYOUT.getChannelCount();
        try {
            din.skip(2 * det);
        } catch (IOException ex) {
            Logger.getLogger(NNJDataSourceBin464ii.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            while (din.available() != 0) {
                tempret.add(new Integer(din.readShort()));
                din.skip(2 * tempCh);
            }
        } catch (IOException ex) {
            Logger.getLogger(NNJDataSourceBin464ii.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            din.close();
        } catch (IOException ex) {
            Logger.getLogger(NNJDataSourceBin464ii.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            fin.close();
        } catch (IOException ex) {
            Logger.getLogger(NNJDataSourceBin464ii.class.getName()).log(Level.SEVERE, null, ex);
        }
        return K.toInteger(tempret.toArray());
    }

    public static double samplingRateDialog() {
        double samplingRate = 0d;
        boolean again = true;
        while (again == true) {
            JFrame frame = new JFrame("NounouJ");
            String s = (String) JOptionPane.showInputDialog(frame, "Select sampling rate for your \".bin\" data:", JOptionPane.QUESTION_MESSAGE);
            if ((s != null) && (s.length() > 0)) {
                try {
                    samplingRate = Double.parseDouble(s);
                    if (samplingRate <= 0) {
                        again = true;
                    } else {
                        again = false;
                    }
                } catch (NumberFormatException ex) {
                    again = true;
                }
            } else {
                again = true;
            }
        }
        return samplingRate;
    }

    /**Loads the specified file into the data object*/
    @Override
    public void loadFile(String file) throws IOException {
        File tempFile = new File(file);
        loadFile(tempFile);
    }

    /**Loads the specified file into the data object*/
    @Override
    public void loadFile(File file) throws IOException {
        this.dataWin = new NNJDataWindow(this);
        this.dataMask = new NNJDataMask();
    }

    /**@param detToRead setting this to -1 gives the default initialization action.
     * Setting to -2 skips certain unnecessary initialization steps,
     * for calls from the static convenience methods. Setting to a valid detector
     * value skips the same steps as -2, and additionally, only reads the trace
     * for detector det.
     */
    private void openDA(File file, int detToRead) throws IOException {
    }

    @Override
    public boolean supportsFileType(String extension) {
        return (extension.equalsIgnoreCase(".bin") || extension.equalsIgnoreCase("bin"));
    }

    @Override
    public void writeData(int[][] newData) {
        if (this.data.length != newData.length || this.data[0].length != newData[0].length) {
            throw new IllegalArgumentException("Input array must be equal in dimensions to original!");
        } else {
            this.data = newData;
        }
    }

    @Override
    public void writeFile(String fileName, NNJDataSource source) throws IOException {
        File file = new File(fileName);
        FileOutputStream fout = new FileOutputStream(file);
        BufferedOutputStream bout = new BufferedOutputStream(fout);
        DataOutputStream dout = new DataOutputStream(bout);
        for (int frame = 0; frame < this.getTotalFrameCount(); frame++) {
            for (int det = 0; det < this.getDataLayout().getChannelCount(); det++) dout.writeShort(Short.reverseBytes((short) ((double) source.readDataPoint(det, frame) / (double) source.getDataExtraBits())));
        }
        dout.close();
        bout.close();
        fout.close();
    }
}
