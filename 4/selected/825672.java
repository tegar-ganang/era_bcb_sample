package snoozesoft.systray4j;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * <p>This class represents a platform specific icon file. The supported formats
 * are *.ico on win32, and *.xpm on KDE. Usually on KDE the dimension of the icon
 * is 24 x 24, and 16 x 16 or 32 x 32 on Windows.</p>
 * <p>Interested listeners are called whenever this icon is clicked.</p>
 */
public class SysTrayMenuIcon {

    private static String extension = ".unknown";

    static {
        if (SysTrayManager.isWindows) extension = ".ico"; else if (SysTrayManager.isLinux) extension = ".xpm";
    }

    private String actionCommand;

    protected File iconFile;

    private Vector listeners;

    /**
     * <p>Creates a new <code>SysTrayMenuIcon</code> object from
     * the file pointed to by the URL.</p>
     * <p>This constructor makes it possible to store the icon files
     * together with the application classes in a JAR file.</p>
     * <p>One possibility to get the URL of an icon is through the
     * getResource() method:<br><br><ul style="list-style-type:none">
     * <li><code>getClass().getResource( "duke" + SysTrayMenuIcon.getExtension() )
     * </code></li></ul><br>provided that the icon is in the same path where the
     * class is stored.</p>
     * @param icon The URL of the icon.
     */
    public SysTrayMenuIcon(URL icon) {
        this(icon, "");
    }

    /**
     * <p>Creates a new <code>SysTrayMenuIcon</code> object from
     * the file pointed to by the URL.</p>
     * <p>This constructor makes it possible to store the icon files
     * together with the application classes in a JAR file.</p>
     * <p>One possibility to get the URL of an icon is through the
     * getResource() method:<br><br><ul style="list-style-type:none">
     * <li><code>getClass().getResource( "duke" + SysTrayMenuIcon.getExtension() )
     * </code></li></ul><br>provided that the icon is in the same path where the
     * class is stored.</p>
     * @param icon The URL of the icon.
     * @param actionCommand The action command passed to the listeners.
     */
    public SysTrayMenuIcon(URL icon, String actionCommand) {
        if (icon != null) iconFile = createTempFile(icon); else iconFile = new File("foo", "bar");
        this.actionCommand = actionCommand;
        listeners = new Vector();
    }

    /**
     * This method returns the correct icon file extension for the
     * actual platform.
     * @return ".ico" ro ".xpm" depending on the platform.
     */
    public static String getExtension() {
        return extension;
    }

    /**
     * <p>Creates a new <code>SysTrayMenuIcon</code> object.</p>
     * <p>Since the file extension (ico/xpm) depends on the actual platform,
     * it is allowed, but not necessary to specify it.</p>
     * <p>Example:<br>&nbsp&nbsprealFileName: duke.ico<br>
     * &nbsp&nbsppassedFileName: duke or duke.ico</p>
     * @param iconFileName The associated file name.
     */
    public SysTrayMenuIcon(String iconFileName) {
        this(iconFileName, "");
    }

    /**
     * Creates a new <code>SysTrayMenuIcon</code> object.
     * <p>Since the file extension (ico/xpm) depends on the actual platform,
     * it is allowed, but not necessary to specify it.</p>
     * <p>Example:<br>&nbsp&nbsprealFileName: duke.ico<br>
     * &nbsp&nbsppassedFileName: duke or duke.ico</p>
     * @param iconFileName The associated file name.
     * @param actionCommand The action command passed to the listeners.
     */
    public SysTrayMenuIcon(String iconFileName, String actionCommand) {
        if (!iconFileName.endsWith(extension)) iconFileName += extension;
        iconFile = new File(iconFileName);
        this.actionCommand = actionCommand;
        listeners = new Vector();
    }

    /**
     * Getter for the action command.
     * @return The action command passed to the listeners.
     */
    public String getActionCommand() {
        return actionCommand;
    }

    /**
     * Setter for the action command.
     * @param actionCommand The action command passed to the listeners.
     */
    public void setActionCommand(String actionCommand) {
        this.actionCommand = actionCommand;
    }

    /**
     * Getter for the file name associated with this object.
     * @return The associated file name.
     */
    public String getName() {
        return iconFile.getName();
    }

    /**
     * Adds the specified listener to receive events from this icon.
     * @param listener The systray menu listener.
     */
    public void addSysTrayMenuListener(SysTrayMenuListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the specified listener so that it no longer receives events from this
     * menu item.
     * @param listener The systray menu listener.
     */
    public void removeSysTrayMenuListener(SysTrayMenuListener listener) {
        listeners.remove(listener);
    }

    protected void fireIconLeftClicked() {
        SysTrayMenuListener listener = null;
        for (int i = 0; i < listeners.size(); i++) {
            listener = (SysTrayMenuListener) listeners.elementAt(i);
            listener.iconLeftClicked(new SysTrayMenuEvent(this, actionCommand));
        }
    }

    protected void fireIconLeftDoubleClicked() {
        SysTrayMenuListener listener = null;
        for (int i = 0; i < listeners.size(); i++) {
            listener = (SysTrayMenuListener) listeners.elementAt(i);
            listener.iconLeftDoubleClicked(new SysTrayMenuEvent(this, actionCommand));
        }
    }

    private File createTempFile(URL icon) {
        File file = null;
        byte[] buffer = new byte[2048];
        try {
            InputStream in = icon.openStream();
            file = File.createTempFile("st4j_icon_", extension);
            file.deleteOnExit();
            FileOutputStream out = new FileOutputStream(file);
            int read = in.read(buffer);
            while (read != -1) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
