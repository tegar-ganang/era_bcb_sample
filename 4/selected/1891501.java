package rbnb;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import javax.imageio.ImageIO;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Source;

/**
 * GenericDataSource.java (  RBNB )
 * Created: Jan 17, 2010
 * @author Michael Nekrasov
 * @version 1.0
 * 
 * Description:	A generic way to create a source. Saves a lot of repeated
 * 				book keeping methods. Can be extended or instantiated to
 * 				to provide a way to store data into RBNB
 * 
 * Example1 (multiple sources):
 * 		GenericDataSource src = new GenericDataSource("MySource");
 * 		src.addChannel("count");
 * 		...
 * 		src.put("count", 42);
 * 		src.flush();
 * 		...
 * 		src.close();
 * 
 * Example2 (multiple channels):
 * 		GenericDataSource src = new GenericDataSource("MySource2");
 * 		src.addChannel("temp");
 * 		src.addChannel("gps", GenericDataSource.MIME_GPS);
 * 		...
 * 		src.put("temp", 32.1);
 * 		src.put("gps", new double[]{53.0,-12.0});
 * 		src.flush();
 * 		...
 * 		src.close();
 * 
 * Example3 (with time stamp):
 * 		byte[] img;
 *		...
 *		double time = System.currentTimeMillis()/1000;
 *		...
 *		GenericDataSource src = new GenericDataSource("MySource2");
 *		src.addChannel("temp");
 *		src.addChannel("img", GenericDataSource.MIME_JPG);
 *		...
 *		src.put("temp", 32.1, time);
 *		src.put("gps", img, time);
 *		src.flush();
 *		...
 *		src.close();
 *
 */
public class GenericDataSource {

    public static final String MIME_BINARY = "application/octet-stream";

    public static final String MIME_JPG = "image/jpeg";

    public static final String MIME_AUDIO = "audio/basic";

    public static final String MIME_TEXT = "text/plain";

    public static final String MIME_GPS = "application/x-gps";

    public static final String DEFAULT_ARCHIVEMODE = "append";

    public static final String DEFAULT_RBNB_SERVER = "localhost";

    public static final int DEFAULT_RBNB_PORT = 3333;

    public static final int DEFAULT_CACHESIZE = 10240;

    public static final int DEFAULT_ARCHIVESIZE = DEFAULT_CACHESIZE * 10;

    private Source source;

    private ChannelMap cmap;

    private HashMap<String, Integer> chRecorder;

    /**
	 * Create a new Generic Data Source with default server settings
	 * @param srcName		Name
	 */
    public GenericDataSource(String srcName) throws SAPIException {
        this(srcName, DEFAULT_RBNB_SERVER, DEFAULT_RBNB_PORT, DEFAULT_CACHESIZE, DEFAULT_ARCHIVESIZE);
    }

    /**
	 * Create a new Generic Data Source
	 * @param srcName		Name
	 * @param serverPath	Path to RBNB Server (default to localhost)
	 * @param port			Port of RBNB Server (default to 3333)
	 * @param cacheSize		Number of frames to store in memory
	 * @param archiveSize	Number of frames to store on disk
	 * @throws SAPIException
	 */
    public GenericDataSource(String srcName, String serverPath, int port, int cacheSize, int archiveSize) throws SAPIException {
        this(srcName, serverPath, port, cacheSize, archiveSize, DEFAULT_ARCHIVEMODE);
    }

    /**
	 * Create a new Generic Data Source
	 * @param srcName		Name
	 * @param serverPath	Path to RBNB Server (default to localhost)
	 * @param port			Port of RBNB Server (default to 3333)
	 * @param cacheSize		Number of frames to store in memory
	 * @param archiveSize	Number of frames to store on disk
	 * @param archiveMode	Mode to add data as (default to append)
	 * @throws SAPIException
	 */
    public GenericDataSource(String srcName, String serverPath, int port, int cacheSize, int archiveSize, String archiveMode) throws SAPIException {
        chRecorder = new HashMap<String, Integer>();
        cmap = new ChannelMap();
        source = new Source(cacheSize, archiveMode, archiveSize);
        source.OpenRBNBConnection(serverPath + ":" + port, srcName);
    }

    /**
	 * Gets the name of the Source as reflected on the RBNB server
	 * @return the name
	 */
    public String getName() {
        return source.GetClientName();
    }

    /**
	 * Gets a set of all defined channels
	 * @return
	 */
    public Set<String> getChannels() {
        return chRecorder.keySet();
    }

    /**
	 * Create a new channel for this source
	 * @param chName
	 * @throws SAPIException
	 */
    public void addChannel(String chName) throws SAPIException {
        addChannel(chName, MIME_BINARY);
    }

    /**
	 * 
	 * @param chName
	 * @param mimeType
	 * @throws SAPIException
	 */
    public void addChannel(String chName, String mimeType) throws SAPIException {
        int ch = cmap.Add(chName);
        cmap.PutMime(ch, mimeType);
        chRecorder.put(chName, ch);
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public int put(String chName, int data) throws SAPIException {
        put(chName, new int[] { data });
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public int put(String chName, int data, double timestamp) throws SAPIException {
        put(chName, new int[] { data }, timestamp);
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public int[] put(String chName, int[] data) throws SAPIException {
        return put(chName, data, System.currentTimeMillis() / 1000);
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public int[] put(String chName, int[] data, double timestamp) throws SAPIException {
        putTime(data.length, timestamp);
        cmap.PutDataAsInt32(getCh(chName), data);
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public float put(String chName, float data) throws SAPIException {
        put(chName, new float[] { data });
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public float put(String chName, float data, double timestamp) throws SAPIException {
        put(chName, new float[] { data }, timestamp);
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public float[] put(String chName, float[] data) throws SAPIException {
        put(chName, data, System.currentTimeMillis() / 1000);
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public float[] put(String chName, float[] data, double timestamp) throws SAPIException {
        putTime(data.length, timestamp);
        cmap.PutDataAsFloat32(getCh(chName), data);
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public double put(String chName, double data) throws SAPIException {
        put(chName, new double[] { data });
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public double put(String chName, double data, double timestamp) throws SAPIException {
        put(chName, new double[] { data }, timestamp);
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public double[] put(String chName, double[] data) throws SAPIException {
        put(chName, data, System.currentTimeMillis() / 1000);
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public double[] put(String chName, double[] data, double timestamp) throws SAPIException {
        putTime(data.length, timestamp);
        cmap.PutDataAsFloat64(getCh(chName), data);
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public String put(String chName, String data) throws SAPIException {
        put(chName, data, System.currentTimeMillis() / 1000);
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public String put(String chName, String data, double timestamp) throws SAPIException {
        putTime(1, timestamp);
        cmap.PutDataAsString(getCh(chName), data);
        return data;
    }

    /** @see put(String chName, byte[] data, double timestamp)*/
    public byte[] put(String chName, byte[] data) throws SAPIException {
        put(chName, data, System.currentTimeMillis() / 1000);
        return data;
    }

    /**
	 * Put byte array into RBNB Server
	 * @param chName		channel to add data to
	 * @param data			data to add
	 * @param timestamp		timestamp to add data as (optional, def to sys time)
	 * @return				data added
	 * @throws SAPIException
	 */
    public byte[] put(String chName, byte[] data, double timestamp) throws SAPIException {
        putTime(1, timestamp);
        cmap.PutDataAsByteArray(getCh(chName), data);
        return data;
    }

    /** @see putput(String chName, BufferedImage img)*/
    public BufferedImage put(String chName, BufferedImage img) throws SAPIException, IOException {
        put(chName, img, System.currentTimeMillis() / 1000);
        return img;
    }

    /**
	 * Put Image into RBNB Server
	 * It will be added as a jpg
	 * @param chName		channel to add data to
	 * @param img			img to add
	 * @param timestamp		timestamp to add data as (optional, def to sys time)
	 * @return				data added
	 * @throws SAPIException
	 */
    public BufferedImage put(String chName, BufferedImage img, double timestamp) throws SAPIException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        put(chName, out.toByteArray(), timestamp);
        return img;
    }

    /** 
	 * Commit data to RBNB Server
	 * @throws SAPIException
	 */
    public void flush() throws SAPIException {
        source.Flush(cmap);
    }

    /**
	 * Commit data to RBNB Server
	 * @param doSynch	wait for confirmation
	 * @throws SAPIException
	 */
    public void flush(boolean doSynch) throws SAPIException {
        source.Flush(cmap, doSynch);
    }

    /**
	 * Close connection to RBNB server (will try to flush all remaining data)
	 */
    public void close() {
        try {
            flush(true);
        } catch (Exception e) {
        }
        source.Detach();
        source.CloseRBNBConnection();
    }

    /**
	 * Sets the timestamp of block of data
	 * @param sizeOfData (3 of array entries) about to be added
	 * @param timestamp  to set
	 */
    private void putTime(int sizeOfData, double timestamp) {
        double[] time = new double[sizeOfData];
        for (int i = 0; i < sizeOfData; i++) time[i] = timestamp;
        cmap.PutTimes(time);
    }

    /**
	 * Retrieves the ID of a channel
	 * @param name of channel
	 * @return the id
	 */
    private int getCh(String name) {
        Integer i = chRecorder.get(name);
        if (i == null) throw new IllegalStateException("Channel '" + name + "' not set");
        return i;
    }
}
