import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;

/**
 * A base class for all MJPEG video sources.
 * 
 * @author Jason P. Hanley
 */
public abstract class MJPEGSource extends RBNBSource {

    private static final String DEFAULT_RBNB_CHANNEL = "video.jpg";

    private String rbnbChannelName = DEFAULT_RBNB_CHANNEL;

    private static final long RETRY_INTERVAL = 5000;

    private static final int HTTP_TIMEOUT = 10000;

    protected String hostName;

    /**
   * A variable to set what percentage of the archived frames are to be
   * cached by the rbnb server.
   */
    private static final double DEFAULT_CACHE_PERCENT = 10;

    private double rbnbCachePercent = DEFAULT_CACHE_PERCENT;

    private Thread timerThread;

    private boolean runit = false;

    String delimiter = new String();

    protected Options setBaseOptions(Options opt) {
        super.setBaseOptions(opt);
        opt.addOption("C", true, "RBNB source channel name *" + DEFAULT_RBNB_CHANNEL);
        opt.addOption("A", true, "Video camera host name (required)");
        opt.addOption("r", true, "length (in hours) to create the ring buffer for this source");
        opt.addOption("m", true, "percentage (%) of the ring buffer specified in -r to cache in memory *" + DEFAULT_CACHE_PERCENT);
        return opt;
    }

    protected boolean setBaseArgs(CommandLine cmd) {
        if (!super.setBaseArgs(cmd)) {
            return false;
        }
        if (cmd.hasOption('C')) {
            String a = cmd.getOptionValue('C');
            if (a != null) {
                rbnbChannelName = a;
            }
        }
        if (cmd.hasOption('m')) {
            if (!cmd.hasOption('r')) {
                writeMessage("The -m parameter is only used by this program in " + "conjunction with the -r parameter");
            } else {
                String a = cmd.getOptionValue('m');
                if (a != null) {
                    try {
                        rbnbCachePercent = Double.parseDouble(a);
                    } catch (NumberFormatException nf) {
                        writeMessage("Please ensure to enter a numeric value for -m option. " + a + " is not valid!");
                        return false;
                    }
                }
            }
        }
        if (cmd.hasOption('r')) {
            if (cmd.hasOption('Z')) {
                writeMessage("Note: Value specified from -Z will be " + "used to create the ring buffer, rather than the " + "archive value calculated from the -r parameter.");
            } else {
                String a = cmd.getOptionValue('r');
                if (a != null) {
                    try {
                        double rbnbTime = Double.parseDouble(a);
                        archiveSize = RBNBUtilities.getFrameCountFromTime(rbnbTime, getFPS());
                    } catch (NumberFormatException nfe) {
                        writeMessage("Please ensure to enter a numeric value for -r option. " + a + " is not valid!");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
   * Get the name of the RBNB channel for this MJPEG video.
   * 
   * @return  the name of the channel
   */
    public String getRBNBChannelName() {
        return rbnbChannelName;
    }

    /**
   * Get the host name of the MJPEG video server.
   * 
   * @return  the host name of the MJPEG video server
   */
    public String getHostName() {
        return hostName;
    }

    /**
   * Get the frames per second of the MJPEG video.
   * 
   * @return  the frames per second of video
   */
    protected abstract int getFPS();

    /**
   * Get the URL to the MJPEG video feed.
   * 
   * @return  the URL to the MJPEG video feed
   */
    protected abstract URL getMJPEGURL();

    /**
   * 
   *
   */
    protected URLConnection getCameraConnection() {
        URL cameraURL = getMJPEGURL();
        if (cameraURL == null) {
            System.err.println("Camera URL is null.");
            disconnect();
            return null;
        }
        URLConnection cameraConnection;
        try {
            cameraConnection = cameraURL.openConnection();
            cameraConnection.setReadTimeout(HTTP_TIMEOUT);
            cameraConnection.connect();
        } catch (IOException e) {
            writeMessage("Error: Failed to connect to video host with " + cameraURL);
            disconnect();
            return null;
        }
        return cameraConnection;
    }

    private void startThread() {
        Runnable r = new Runnable() {

            public void run() {
                runWork();
            }
        };
        runit = true;
        timerThread = new Thread(r, "Timer");
        timerThread.start();
        writeMessage("Started thread.");
    }

    private void stopThread() {
        runit = false;
        timerThread.interrupt();
        writeMessage("Stopped thread.");
    }

    private void runWork() {
        boolean retry = true;
        while (retry) {
            if (connect()) {
                retry = !execute();
            }
            disconnect();
            if (retry) {
                try {
                    Thread.sleep(RETRY_INTERVAL);
                } catch (Exception e) {
                }
                writeMessage("Some problem. Retrying!");
            }
        }
        writeMessage("Done!");
        stop();
    }

    private boolean execute() {
        if (!isConnected()) return false;
        ChannelMap cmap = new ChannelMap();
        int channelId;
        try {
            channelId = cmap.Add(rbnbChannelName);
        } catch (SAPIException e) {
            writeMessage("Error: Failed to add video channel to channel map; name = " + rbnbChannelName);
            disconnect();
            return false;
        }
        cmap.PutTimeAuto("timeofday");
        cmap.PutMime(channelId, "image/jpeg");
        URLConnection cameraConnection = getCameraConnection();
        if (cameraConnection == null) {
            writeMessage("Failed to get camera connection.");
            disconnect();
            return false;
        }
        String contentType = cameraConnection.getHeaderField("Content-Type");
        if (contentType == null) {
            writeMessage("Failed to find content type in stream.");
            disconnect();
            return false;
        }
        writeMessage("contentType :" + contentType);
        String[] fields = contentType.split(";");
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim();
            if (fields[i].toLowerCase().startsWith("boundary=")) {
                delimiter = fields[i].substring(9);
                break;
            }
        }
        if (delimiter.length() == 0) {
            writeMessage("Error: Failed to find delimiter.");
            disconnect();
            return false;
        }
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(cameraConnection.getInputStream());
        } catch (IOException e) {
            writeMessage("Error: Failed to get data stream from D-Link host.");
            disconnect();
            return false;
        }
        int imageLength = 0;
        boolean failed = false;
        long previousTimeStamp = -1;
        double averageFPS = -1;
        long images = 0;
        long timeStamp;
        double fps;
        while (true) {
            try {
                imageLength = getContentLength(dis);
            } catch (IOException eio) {
                writeMessage("Error: Failed to read header data: " + eio.getMessage());
                failed = true;
                break;
            }
            if (imageLength > 0) {
                try {
                    loadImage(dis, cmap, channelId, imageLength);
                } catch (IOException io) {
                    writeMessage("Error: Failed to load image: " + io.getMessage());
                    failed = true;
                    break;
                } catch (SAPIException es) {
                    writeMessage("Error: Failed to load data: " + es.getMessage());
                    failed = true;
                    break;
                }
                images++;
                timeStamp = System.currentTimeMillis();
                if (previousTimeStamp != -1 && timeStamp > previousTimeStamp) {
                    fps = 1000d / (timeStamp - previousTimeStamp);
                    if (averageFPS == -1) {
                        averageFPS = fps;
                    } else {
                        averageFPS = 0.95 * averageFPS + 0.05 * fps;
                    }
                    long roundedAverageFPS = Math.round(averageFPS);
                    if (images % roundedAverageFPS == 0) {
                        writeProgressMessage("Average frames per second: " + roundedAverageFPS);
                    }
                }
                previousTimeStamp = timeStamp;
            }
        }
        try {
            dis.close();
        } catch (IOException e) {
            writeMessage("Error: Failed to close connect to D-Link host.");
        }
        return !failed;
    }

    private int getContentLength(DataInputStream dis) throws IOException {
        int contentLength = 0;
        boolean done = false;
        char ch;
        String line;
        StringBuffer inputLine = new StringBuffer();
        boolean gotHeader = false;
        while (!done) {
            ch = (char) dis.readByte();
            if (ch == '\r') {
                dis.readByte();
                line = inputLine.toString().trim();
                if (line.equals(delimiter)) {
                    gotHeader = true;
                } else if (line.toLowerCase().startsWith("content-length")) {
                    contentLength = Integer.parseInt(inputLine.substring(16));
                } else if (gotHeader && line.length() == 0) {
                    done = true;
                }
                inputLine = new StringBuffer();
            } else {
                inputLine.append(ch);
            }
        }
        return contentLength;
    }

    private void loadImage(DataInputStream dis, ChannelMap map, int chanId, int imgLength) throws IOException, SAPIException {
        byte[] imageData = new byte[imgLength];
        dis.readFully(imageData);
        map.PutDataAsByteArray(chanId, imageData);
        getSource().Flush(map, true);
    }

    public boolean isRunning() {
        return (isConnected() && runit);
    }

    public boolean start() {
        if (isRunning()) {
            return false;
        }
        if (isConnected()) {
            disconnect();
        }
        connect();
        if (!isConnected()) {
            return false;
        }
        startThread();
        return true;
    }

    public boolean stop() {
        if (!isRunning()) {
            return false;
        }
        stopThread();
        disconnect();
        return true;
    }
}
