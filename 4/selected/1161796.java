package javaclient3;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javaclient3.structures.PlayerMsgHdr;
import javaclient3.structures.blobfinder.PlayerBlobfinderBlob;
import javaclient3.structures.blobfinder.PlayerBlobfinderColorConfig;
import javaclient3.structures.blobfinder.PlayerBlobfinderData;
import javaclient3.structures.blobfinder.PlayerBlobfinderImagerConfig;
import javaclient3.xdr.OncRpcException;
import javaclient3.xdr.XdrBufferDecodingStream;
import javaclient3.xdr.XdrBufferEncodingStream;

/**
 * The blobfinder interface provides access to devices that detect blobs in images.
 * @author Radu Bogdan Rusu, Maxim Batalin
 * @version
 * <ul>
 *      <li>v3.0 - Player 3.0 supported
 * </ul>
 */
public class BlobfinderInterface extends PlayerDevice {

    private static final boolean isDebugging = PlayerClient.isDebugging;

    private Logger logger = Logger.getLogger(BlobfinderInterface.class.getName());

    private PlayerBlobfinderData pbdata;

    private boolean readyPbdata = false;

    /**
     * Constructor for BlobfinderInterface.
     * @param pc a reference to the PlayerClient object
     */
    public BlobfinderInterface(PlayerClient pc) {
        super(pc);
    }

    /**
     * Read the list of detected blobs.
     */
    public synchronized void readData(PlayerMsgHdr header) {
        try {
            switch(header.getSubtype()) {
                case PLAYER_BLOBFINDER_DATA_BLOBS:
                    {
                        this.timestamp = header.getTimestamp();
                        byte[] buffer = new byte[16];
                        is.readFully(buffer, 0, 16);
                        pbdata = new PlayerBlobfinderData();
                        XdrBufferDecodingStream xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        pbdata.setWidth(xdr.xdrDecodeInt());
                        pbdata.setHeight(xdr.xdrDecodeInt());
                        int blobsCount = xdr.xdrDecodeInt();
                        xdr.endDecoding();
                        xdr.close();
                        buffer = new byte[blobsCount * 40];
                        is.readFully(buffer, 0, blobsCount * 40);
                        xdr = new XdrBufferDecodingStream(buffer);
                        xdr.beginDecoding();
                        PlayerBlobfinderBlob[] pbbs = new PlayerBlobfinderBlob[blobsCount];
                        for (int i = 0; i < blobsCount; i++) {
                            PlayerBlobfinderBlob ppb = new PlayerBlobfinderBlob();
                            ppb.setId(xdr.xdrDecodeInt());
                            ppb.setColor(xdr.xdrDecodeInt());
                            ppb.setArea(xdr.xdrDecodeInt());
                            ppb.setX(xdr.xdrDecodeInt());
                            ppb.setY(xdr.xdrDecodeInt());
                            ppb.setLeft(xdr.xdrDecodeInt());
                            ppb.setRight(xdr.xdrDecodeInt());
                            ppb.setTop(xdr.xdrDecodeInt());
                            ppb.setBottom(xdr.xdrDecodeInt());
                            ppb.setRange(xdr.xdrDecodeFloat());
                            pbbs[i] = ppb;
                        }
                        xdr.endDecoding();
                        xdr.close();
                        pbdata.setBlobs(pbbs);
                        readyPbdata = true;
                        break;
                    }
            }
        } catch (IOException e) {
            throw new PlayerException("[Blobfinder] : Error reading payload: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Blobfinder] : Error while XDR-decoding payload: " + e.toString(), e);
        }
    }

    /**
     * Request/reply: Set tracking color.
     * <br><br>
     * For some sensors (ie CMUcam), simple blob tracking tracks only one
     * color. To set the tracking color, send a PLAYER_BLOBFINDER_REQ_SET_COLOR
     * request with the format below, including the RGB color ranges (max
     * and min). Values of -1 will cause the track color to be automatically
     * set to the current window color. This is useful for setting the track
     * color by holding the tracking object in front of the lens. Null response.
     * @param pbcc a PlayerBlobfinderColorConfig structure holding the required
     * data
     */
    public void setTrackingColor(PlayerBlobfinderColorConfig pbcc) {
        try {
            sendHeader(PLAYER_MSGTYPE_REQ, PLAYER_BLOBFINDER_REQ_SET_COLOR, 28);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(28);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(pbcc.getChannel());
            xdr.xdrEncodeInt(pbcc.getRmin());
            xdr.xdrEncodeInt(pbcc.getRmax());
            xdr.xdrEncodeInt(pbcc.getGmin());
            xdr.xdrEncodeInt(pbcc.getGmax());
            xdr.xdrEncodeInt(pbcc.getBmin());
            xdr.xdrEncodeInt(pbcc.getBmax());
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Blobfinder] : Couldn't request " + "PLAYER_BLOBFINDER_REQ_SET_COLOR: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Blobfinder] : Error while XDR-encoding SET_COLOR request: " + e.toString(), e);
        }
    }

    /**
     * Configuration request: Set imager params.
     * <br><br>
     * Imaging sensors that do blob tracking generally have some sorts of
     * image quality parameters that you can tweak. The following ones are
     * implemented here:
     * <ul>
     *          <li>brightness (0-255)
     *          <li>contrast (0-255)
     *          <li>auto gain (0=off, 1=on)
     *          <li>color mode (0=RGB/AutoWhiteBalance Off,
     *          1=RGB/AutoWhiteBalance On, 2=YCrCB/AWB Off, 3=YCrCb/AWB On)
     * </ul>
     * To set the params, send a PLAYER_BLOBFINDER_REQ_SET_IMAGER_PARAMS
     * request with the format below. Any values set to -1 will be left
     * unchanged. Null response.
     * @param pbic a PlayerBlobfinderImagerConfig structure holding the
     * required data
     */
    public void setImagerParams(PlayerBlobfinderImagerConfig pbic) {
        try {
            sendHeader(PLAYER_MSGTYPE_REQ, PLAYER_BLOBFINDER_REQ_SET_IMAGER_PARAMS, 24);
            XdrBufferEncodingStream xdr = new XdrBufferEncodingStream(24);
            xdr.beginEncoding(null, 0);
            xdr.xdrEncodeInt(pbic.getBrightness());
            xdr.xdrEncodeInt(pbic.getContrast());
            xdr.xdrEncodeInt(pbic.getColormode());
            xdr.xdrEncodeInt(pbic.getAutogain());
            xdr.endEncoding();
            os.write(xdr.getXdrData(), 0, xdr.getXdrLength());
            xdr.close();
            os.flush();
        } catch (IOException e) {
            throw new PlayerException("[Blobfinder] : Couldn't request " + "PLAYER_BLOBFINDER_REQ_SET_IMAGER_PARAMS: " + e.toString(), e);
        } catch (OncRpcException e) {
            throw new PlayerException("[Blobfinder] : Error while XDR-encoding SET_IMAGER_PARAMS " + "request: " + e.toString(), e);
        }
    }

    /**
     * Handle acknowledgement response messages.
     * @param header Player header
     */
    protected void handleResponse(PlayerMsgHdr header) {
        switch(header.getSubtype()) {
            case PLAYER_BLOBFINDER_REQ_SET_COLOR:
                {
                    break;
                }
            case PLAYER_BLOBFINDER_REQ_SET_IMAGER_PARAMS:
                {
                    break;
                }
            default:
                {
                    if (isDebugging) logger.log(Level.FINEST, "[Blobfinder][Debug] : " + "Unexpected response " + header.getSubtype() + " of size = " + header.getSize());
                    break;
                }
        }
    }

    /**
     * Get the Blobfinder data.
     * @return an object of type PlayerBlobfinderData containing the requested data
     */
    public PlayerBlobfinderData getData() {
        return this.pbdata;
    }

    /**
     * Check if data is available.
     * @return true if ready, false if not ready
     */
    public boolean isDataReady() {
        if (readyPbdata) {
            readyPbdata = false;
            return true;
        }
        return false;
    }
}
