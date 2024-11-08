package org.mcisb.beacon.experiment;

/**
 * 
 *
 * @author Daniel Jameson
 */
public class Track {

    /**
	 * 
	 */
    private String name = "";

    /**
	 * 
	 */
    private String id = "";

    /**
	 * 
	 */
    private String channel = "";

    /**
	 * 
	 */
    public Track() {
    }

    /**
	 * 
	 *
	 * @param n
	 * @param i
	 * @param p
	 */
    public Track(String n, String i, String p) {
        name = n;
        id = i;
        channel = p;
    }

    /**
	 * 
	 *
	 * @return String
	 */
    public String getName() {
        return name;
    }

    /**
	 * 
	 *
	 * @return String
	 */
    public String getId() {
        return id;
    }

    /**
	 * 
	 *
	 * @return String
	 */
    public String getChannel() {
        return channel;
    }

    /**
	 * 
	 *
	 * @param s
	 */
    public void setName(String s) {
        name = s;
    }

    /**
	 * 
	 *
	 * @param s
	 */
    public void setID(String s) {
        id = s;
    }

    /**
	 * 
	 *
	 * @param s
	 */
    public void setChannel(String s) {
        channel = s;
    }
}
