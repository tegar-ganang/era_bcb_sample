package javaclient3.structures.blobfinder;

import javaclient3.structures.*;

/**
 * Request/reply: Set tracking color.
 * For some sensors (ie CMUcam), simple blob tracking tracks only one color.
 * To set the tracking color, send a PLAYER_BLOBFINDER_REQ_SET_COLOR request
 * with the format below, including the RGB color ranges (max and min).
 * Values of -1 will cause the track color to be automatically set to the
 * current window color. This is useful for setting the track color by
 * holding the tracking object in front of the lens. Null response.
 * <br>
 * For devices that can track multiple colors, channel attribute indicates
 * which color channel we are defining with this structure.
 * Single channel devices will ignore this field.
 * @author Radu Bogdan Rusu
 * @version
 * <ul>
 *      <li>v3.0 - Player 3.0 supported
 * </ul>
 */
public class PlayerBlobfinderColorConfig implements PlayerConstants {

    private int channel;

    private int rmin;

    private int rmax;

    private int gmin;

    private int gmax;

    private int bmin;

    private int bmax;

    /**
     * @return  Color channel defined on this structure
     */
    public synchronized int getChannel() {
        return this.channel;
    }

    /**
     * @param newChannel  Color channel defined on this structure
     */
    public synchronized void setChannel(int newChannel) {
        this.channel = newChannel;
    }

    /**
     * @return  Red minimum value (0-255)
     */
    public synchronized int getRmin() {
        return this.rmin;
    }

    /**
     * @param newRmin  Red minimum value (0-255)
     */
    public synchronized void setRmin(int newRmin) {
        this.rmin = newRmin;
    }

    /**
     * @return  Red maximum value (0-255)
     */
    public synchronized int getRmax() {
        return this.rmax;
    }

    /**
     * @param newRmax   Red maximum value (0-255)
     */
    public synchronized void setRmax(int newRmax) {
        this.rmax = newRmax;
    }

    /**
     * @return  Green minimum (0-255)
     */
    public synchronized int getGmin() {
        return this.gmin;
    }

    /**
     * @param newGmin  Green minimum value (0-255)
     */
    public synchronized void setGmin(int newGmin) {
        this.gmin = newGmin;
    }

    /**
     * @return  Green maximum value (0-255)
     */
    public synchronized int getGmax() {
        return this.gmax;
    }

    /**
     * @param newGmax  Green maximum value (0-255)
     */
    public synchronized void setGmax(int newGmax) {
        this.gmax = newGmax;
    }

    /**
     * @return  Blue minimum value (0-255)
     */
    public synchronized int getBmin() {
        return this.bmin;
    }

    /**
     * @param newBmin  Blue minimum value (0-255)
     */
    public synchronized void setBmin(int newBmin) {
        this.bmin = newBmin;
    }

    /**
     * @return  Blue maximum value (0-255)
     */
    public synchronized int getBmax() {
        return this.bmax;
    }

    /**
     * @param newBmax  Blue maximum value (0-255)
     */
    public synchronized void setBmax(int newBmax) {
        this.bmax = newBmax;
    }
}
