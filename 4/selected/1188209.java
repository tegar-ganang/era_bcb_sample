package engine;

import javax.vecmath.Point3d;

/**
 * The Instrument class stores a single instrument within an orchestra or band.
 * Instruments are transient objects and exist only for calculations for
 * the distortion with seats in mind.
 * 
 * @author Jonathan Sun
 */
public abstract class Instrument {

    /** The Instrument name */
    protected String iName;

    /** The channel number of the instrument */
    protected int channelNumber;

    /** The unadjusted volume of the channel of the instrument */
    protected int channelVolume;

    /** The relative location of the instrument */
    protected Point3d iLocation;

    /**
	 * Default constructor
	 */
    public Instrument() {
    }

    /**
	 * Constructs a new Instrument
	 * @param name the name of the instrument
	 * @param channelNum the temporary MIDI channel index of this instrument
	 * @param channelVol the temporary MIDI balanced channel volume
	 * @param location the relative location in space of the instrument
	 */
    public Instrument(String name, int channelNum, int channelVol, Point3d location) {
        iName = name;
        channelNumber = channelNum;
        channelVolume = channelVol;
        iLocation = location;
    }

    /**
	 * @return the relative adjusted volume for a seat section
	 * @param s the SeatSection to calculate the relative volume for
	 */
    public abstract double getAdjustedVolume(SeatSection s);

    /**
	 * Returns the name of the instrument
	 * @return the name of the instrument
	 */
    public String getName() {
        return iName;
    }

    /**
	 * Returns the MIDI channel number of the instrument
	 * @return the MIDI channel number of the instrument
	 */
    public int getChannelNumber() {
        return channelNumber;
    }

    /**
	 * Returns the MIDI balanced channel volume
	 * @return the MIDI balanced channel volume
	 */
    public int getChannelVolume() {
        return channelVolume;
    }

    /**
	 * Returns the location of the instrument
	 * @return the location of the instrument
	 */
    public Point3d getLocation() {
        return iLocation;
    }

    /**
	 * Returns a string representation of the instrument
	 * @return a string representation of the instrument
	 */
    public String toString() {
        return "instrument " + iName + iLocation.x + "," + iLocation.y + "," + iLocation.z;
    }
}
