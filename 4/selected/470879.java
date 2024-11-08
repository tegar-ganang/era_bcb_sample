package org.rdv.rbnb;

import java.io.File;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rdv.data.DataChannel;
import org.rdv.data.DataFileReader;
import org.rdv.data.NumericDataSample;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Source;

/**
 * A class to import data into data turbine.
 * 
 * @author  Jason P. Hanley
 */
public class RBNBImport {

    /** the logger for this class */
    protected static Log log = org.rdv.LogFactory.getLog(RBNBImport.class.getName());

    /** the RBNB host name to connect too */
    private String rbnbHostName;

    /** the RBNB port number to connect too */
    private int rbnbPortNumber;

    /** the RBNB source */
    private Source source;

    /** the RBNB channel map */
    private ChannelMap cmap;

    /** the channel indexes for the channel map */
    private int[] cindex;

    /** the progress listener for the import */
    private ProgressListener listener;

    /** the thread doing the import */
    private Thread importThread;

    /** flag to indicate if the import has been canceled */
    private boolean canceled;

    /** the number of rows imported */
    private long rowsImported;

    /** the number of samples in the data file */
    private int samples;

    private static int SAMPLES_PER_FLUSH = 50;

    /**
	 * Initialize the class with the RBNB server to import data too.
	 * 
	 * @param rbnbHostName    the host name of the RBNB server
	 * @param rbnbPortNumber  the port number of the RBNB server
	 */
    public RBNBImport(String rbnbHostName, int rbnbPortNumber) {
        this.rbnbHostName = rbnbHostName;
        this.rbnbPortNumber = rbnbPortNumber;
    }

    /**
	 * Start the import of the data file to the RBNB server specified in the
	 * constructor using the source name supplied.
	 * <p>
	 * This methods will spawn the import in another thread and return before it
	 * is completed. 
	 * 
	 * @param sourceName  the source name for the RBNB server
	 * @param dataFile    the file containing the data to import
	 */
    public void startImport(String sourceName, File dataFile) {
        ProgressListener dummyListener = new ProgressListener() {

            public void postCompletion() {
            }

            public void postError(String errorMessage) {
            }

            public void postProgress(double progress) {
            }
        };
        startImport(sourceName, dataFile, dummyListener);
    }

    /**
	 * Start the import of the data file to the RBNB server specified in the
	 * constructor using the source name supplied. The status of the import will
	 * be posted too the listener.
	 * <p>
	 * This methods will spawn the import in another thread and return before it
	 * is completed.
	 * 
	 * @param sourceName  the source name for the RBNB server
	 * @param dataFile    the file containing the data to import
	 * @param listener    the listener to post status too
	 */
    public void startImport(final String sourceName, final File dataFile, ProgressListener listener) {
        this.listener = listener;
        importThread = new Thread() {

            public void run() {
                importData(sourceName, dataFile);
            }
        };
        importThread.start();
    }

    /**
	 * Import specified data file into RBNB server with the specified source name.
	 * If the listener is not null, status ofthe import will be posted.
	 * 
	 * @param sourceName  the source name for the data channels
	 * @param dataFile    the file containing the data channels
	 * @param listener    the listener to post status too, can be null
	 */
    private void importData(String sourceName, File dataFile) {
        if (dataFile == null) {
            listener.postError("Data file not specified.");
            return;
        }
        DataFileReader reader;
        try {
            reader = new DataFileReader(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
            listener.postError("Problem reading data file header.");
            return;
        }
        List<DataChannel> channels = reader.getChannels();
        cindex = new int[channels.size()];
        samples = Integer.parseInt(reader.getProperty("samples"));
        int archiveSize = (int) Math.ceil((double) samples / SAMPLES_PER_FLUSH);
        source = new Source(1, "create", archiveSize);
        cmap = new ChannelMap();
        try {
            for (int i = 0; i < channels.size(); i++) {
                DataChannel channel = channels.get(i);
                cindex[i] = cmap.Add(channel.getName());
                cmap.PutMime(cindex[i], "application/octet-stream");
                if (channel.getUnit() != null) {
                    cmap.PutUserInfo(cindex[i], "units=" + channel.getUnit());
                }
            }
            source.OpenRBNBConnection(rbnbHostName + ":" + rbnbPortNumber, sourceName);
            source.Register(cmap);
        } catch (SAPIException e) {
            e.printStackTrace();
            listener.postError("Unable to connect to the server.");
            return;
        }
        boolean error = false;
        try {
            NumericDataSample sample;
            while ((sample = reader.readSample()) != null) {
                postDataSamples(sample.getTimestamp(), sample.getValues());
            }
            source.Flush(cmap);
        } catch (Exception e) {
            e.printStackTrace();
            error = true;
        }
        if (error) {
            source.CloseRBNBConnection();
            if (canceled) {
                listener.postError("The import was canceled.");
            } else {
                listener.postError("Problem importing data file.");
            }
        } else {
            source.Detach();
            listener.postCompletion();
        }
        canceled = false;
    }

    /**
   * Callback for the data file reader to process the data samples.
   * 
   * @param timestamp       the timestamp for the data
   * @param values          the data values
   * @throws SAPIException  if there is an error communicating with the server
   */
    public void postDataSamples(double timestamp, Number[] values) throws SAPIException {
        cmap.PutTime(timestamp, 0);
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                continue;
            }
            double[] value = { values[i].doubleValue() };
            cmap.PutDataAsFloat64(cindex[i], value);
        }
        double progress = ++rowsImported / (double) samples;
        if (progress > 1) {
            progress = 1;
        }
        listener.postProgress(progress);
        if (rowsImported % SAMPLES_PER_FLUSH == 0) {
            source.Flush(cmap, true);
        }
    }

    /**
   * Stop the import of the data.
   */
    public void cancelImport() {
        if (importThread == null) {
            return;
        }
        canceled = true;
        importThread.interrupt();
    }
}
