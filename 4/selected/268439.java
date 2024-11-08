package de.peathal.util;

/**
 *
 * @author peter karich
 */
public class DirProperties {

    /**
     * This variable specifies the folder to read and write to the
     * root of /usr/share/appName/appName.jar
     */
    public static final String DEV = "developer";

    /**
     * This variable specifies the folder to read and write to the
     * /usr/share/appName/appName/
     */
    public static final String ADMIN = "admin";

    /**
     * This variable specifies the folder to read and write to the
     * /home/user/.appName/
     */
    public static final String USER = "user";

    String location;

    boolean write;

    boolean read;

    public DirProperties(String location, boolean write, boolean read) {
        if (location.equals(USER)) {
        } else if (location.equals(DEV)) {
        } else if (location.equals(ADMIN)) {
        } else throw new UnsupportedOperationException("location should be one of the variables: DEV, ADMIN or USER");
        this.write = write;
        this.read = read;
    }

    public String getLocation() {
        return location;
    }

    public boolean isReadable() {
        return read;
    }

    public boolean isWriteable() {
        return write;
    }
}
