package org.nees.jcamera;

import java.io.IOException;
import org.apache.log4j.Logger;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Source;

public class DataTurbine {

    static Logger log = Logger.getLogger(DataTurbine.class.getName());

    private static final String SERVER = "localhost:3333";

    private static final String CHANNEL_NAME = "video.jpg";

    private String server = SERVER;

    private String channelName = CHANNEL_NAME;

    private String name;

    private Source source;

    private int channelId = -1;

    private ChannelMap cmap;

    public DataTurbine(String name) {
        this.name = name;
    }

    public void open() {
        try {
            log.debug("Turbine server: " + server);
            log.debug("Opening turbine: " + name);
            source = new Source();
            source.OpenRBNBConnection(server, name);
        } catch (Exception e) {
            log.error("Error opening turbine source: " + e);
        }
        try {
            log.debug("Opening turbine channel");
            cmap = new ChannelMap();
            cmap.PutTimeAuto("timeofday");
        } catch (Exception e) {
            log.error("Error creating turbine channel: " + e);
        }
        try {
            log.debug("Adding turbine channel");
            channelId = cmap.Add(channelName);
        } catch (Exception e) {
            log.error("Error adding turbine channel: " + e);
        }
    }

    /**
     * Upload an image from a camera to data turbine.
     * @param camera
     * @throws IOException 
     * @throws SAPIException 
     */
    public void upload(Camera camera) throws SAPIException, IOException {
        post(camera.getRecentImageBuffer());
    }

    private void post(byte[] imageBuffer) throws SAPIException {
        cmap.PutMime(channelId, "image/jpeg");
        try {
            cmap.PutDataAsByteArray(channelId, imageBuffer);
            source.Flush(cmap);
        } catch (SAPIException e) {
            log.debug("Upload image; RBNB put failed ");
            throw e;
        }
    }

    /**
     * Close the data turbine.
     */
    public void close() {
        log.debug("Closing turbine");
        source.CloseRBNBConnection();
    }

    /**
     * Generaic bean method
     * @return Returns the channelName.
     */
    public String getChannelName() {
        return channelName;
    }

    /**
     * Generaic bean method
     * @param channelName The channelName to set.
     */
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    /**
     * Generaic bean method
     * @return Returns the server.
     */
    public String getServer() {
        return server;
    }

    /**
     * Generaic bean method
     * @param server The server to set.
     */
    public void setServer(String server) {
        this.server = server;
    }
}
