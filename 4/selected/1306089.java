package org.wcb.autohome.factories;

import java.util.*;
import java.io.*;
import org.wcb.autohome.interfaces.IDeviceRemote;
import org.wcb.autohome.interfaces.IMacro;
import org.wcb.autohome.interfaces.X10DeviceConstants;
import org.wcb.autohome.implementations.X10Events;
import org.wcb.autohome.implementations.X10Module;
import org.wcb.autohome.implementations.X10MonitorEvent;

/**
 *  Project:   Alice X10 Home Automation
 *  Filename:  $Id: DeviceFactory.java,v 1.17 2004/02/25 21:38:36 wbogaardt Exp $
 *  Abstract:  Centralized backend for various components to send events to
 *             Serial port output. This mostly handles reading from a file and writting
 *             to a file the various user entries such as devices and macros for latter
 *             retrieval.
 *
 * $Log: DeviceFactory.java,v $
 * Revision 1.17  2004/02/25 21:38:36  wbogaardt
 * added javadocs and fixed formating for checkstyle report
 *
 * Revision 1.16  2004/02/03 21:02:27  wbogaardt
 * moved DeviceFactory away from rmi creation and simplified interface between gateway
 *
 * Revision 1.15  2004/01/17 08:15:46  wbogaardt
 * Have an initial working monitoring frame that actually shows the date and time an event takes place
 *
 * Revision 1.14  2004/01/17 07:21:17  wbogaardt
 * added serialization to run events and allow monitoring of these events to the file system to reload later
 *
 * Revision 1.13  2004/01/16 00:53:43  wbogaardt
 * Fixed a very obscure bug with the Macro Panel that it didn't added new
 * x10 devices to the drop down of available x10 device for the macro. Modified Macro triggers to change the events
 * to integer verses strings cleaner this way.
 *
 * Revision 1.12  2003/12/24 20:24:20  wbogaardt
 * Fixed display of console message on start up
 *
 * Revision 1.11  2003/12/20 21:32:39  wbogaardt
 * enabled new file menu option and added functionality
 *
 * Revision 1.10  2003/12/16 22:08:37  wbogaardt
 * refactored events daemon handeling
 *
 * Revision 1.9  2003/12/12 23:17:36  wbogaardt
 * javadoc comments refactored methods so they are more descriptive
 *
 * Revision 1.8  2003/12/09 23:11:42  wbogaardt
 *
 * C: ----------------------------------------------------------------------
 *
 * Revision 1.7  2003/12/08 21:57:24  wbogaardt
 * refactored out properties loading for old x10 format
 *
 * Revision 1.6  2003/12/08 21:35:18  wbogaardt
 * refactored out old methods of saving macros and modules
 *
 * Revision 1.5  2003/12/08 21:09:04  wbogaardt
 * refactored old method signatures improved error handeling on windows
 *
 * Revision 1.4  2003/12/06 00:41:53  wbogaardt
 * deprecated some method calls
 *
 * Revision 1.3  2003/10/10 21:39:07  wbogaardt
 * modified macro triggers to use calendar in stead of strings
 *
 * Revision 1.2  2003/10/10 18:39:12  wbogaardt
 * changed date time information from a string to a calendar object
 *
 *
 * @author wbogaardt
 * @version 1.0
 */
public class DeviceFactory implements IDeviceRemote, X10DeviceConstants {

    private static Vector[] FILE_SAVED_HASH = new Vector[5];

    /**
     * Default Consturctor
     */
    public DeviceFactory() {
        super();
    }

    /**
     * This new method takes in an Array of Vectors and assigns them
     * to the appropriate Vector Array so that other objects can access these
     * Vectors.
     * @param sfilename an array of Vectors which have serilized objects.
     */
    public void loadFile(String sfilename) {
        FILE_SAVED_HASH = new Vector[5];
        try {
            FileInputStream fin = new FileInputStream(sfilename);
            ObjectInputStream inStream = new ObjectInputStream(fin);
            System.out.println("Reading in: " + (String) inStream.readObject());
            FILE_SAVED_HASH[0] = (Vector) inStream.readObject();
            FILE_SAVED_HASH[1] = (Vector) inStream.readObject();
            FILE_SAVED_HASH[2] = (Vector) inStream.readObject();
            FILE_SAVED_HASH[3] = (Vector) inStream.readObject();
            FILE_SAVED_HASH[4] = (Vector) inStream.readObject();
        } catch (IOException ioe) {
            Vector vX10modules = new Vector();
            vX10modules.add(new X10Module());
            FILE_SAVED_HASH[0] = vX10modules;
            Vector vEvents = new Vector();
            vEvents.add(new X10Events());
            FILE_SAVED_HASH[1] = vEvents;
            FILE_SAVED_HASH[2] = new Vector();
            FILE_SAVED_HASH[3] = new Vector();
            Vector vMonitors = new Vector();
            vMonitors.add(new X10MonitorEvent());
            FILE_SAVED_HASH[4] = vMonitors;
        } catch (ClassNotFoundException cnfe) {
            Vector vX10modules = new Vector();
            vX10modules.add(new X10Module());
            FILE_SAVED_HASH[0] = vX10modules;
            Vector vEvents = new Vector();
            vEvents.add(new X10Events());
            FILE_SAVED_HASH[1] = vEvents;
            FILE_SAVED_HASH[2] = new Vector();
            FILE_SAVED_HASH[3] = new Vector();
            Vector vMonitors = new Vector();
            vMonitors.add(new X10MonitorEvent());
            FILE_SAVED_HASH[4] = vMonitors;
        }
    }

    /**
     * Saves all of the module information to the user selected
     * save to file this method is usually called from a
     * save or save as command. This also writes the information
     * to the diskdrive.
     * @param sfileName Full path and file name
     * @return true if file saved succesfully
     */
    public boolean saveFile(String sfileName) {
        try {
            FileOutputStream output = new FileOutputStream(sfileName);
            ObjectOutputStream outStream = new ObjectOutputStream(output);
            outStream.writeObject("FILENAME: " + sfileName);
            outStream.writeObject(this.readInX10Devices());
            outStream.writeObject(this.readInX10Events());
            outStream.writeObject(this.loadMacroTriggers());
            outStream.writeObject(this.readInX10Macros());
            outStream.writeObject(this.readInAliceMonitors());
            outStream.flush();
            outStream.close();
        } catch (IOException err) {
            System.err.println("Unable to write file :" + err);
            return false;
        }
        return true;
    }

    /**
     * Create a new file such as when the user select
     * File->new
     */
    public void createNewFile() {
        Vector vModules = new Vector();
        vModules.add(new X10Module());
        FILE_SAVED_HASH[0] = vModules;
        Vector vEvents = new Vector();
        vEvents.add(new X10Events());
        FILE_SAVED_HASH[1] = vEvents;
        FILE_SAVED_HASH[2] = new Vector();
        FILE_SAVED_HASH[3] = new Vector();
        Vector vMonitors = new Vector();
        vMonitors.add(new X10MonitorEvent());
        FILE_SAVED_HASH[4] = vMonitors;
    }

    /**
     * Returns a list of X10Devices saved in the file.
     * @return Vector of IX10Module
     */
    public Vector readInX10Devices() {
        return FILE_SAVED_HASH[0];
    }

    /**
     * Save the vector of IX10Module into a properties
     * Hash table key for quick access and storage;
     * @param items Vector of IX10Module which are serialized.
      */
    public void writeX10Devices(Vector items) {
        FILE_SAVED_HASH[0] = items;
    }

    /**
     * Returns a vector of IX10Events which are specific to the
     * Alice environment.
     * @return Vector of IX10Events
     */
    public Vector readInX10Events() {
        if (FILE_SAVED_HASH[1] != null) {
            return FILE_SAVED_HASH[1];
        }
        Vector vEvents = new Vector();
        vEvents.add(new X10Events());
        return vEvents;
    }

    /**
     * This is used to store the alice monitoring events.
     * @param vList list of events
     */
    public void writeAliceMonitors(Vector vList) {
        FILE_SAVED_HASH[4] = vList;
    }

    /**
     * Save the vector list of IX10Events into a
     * Hash table key for quick access and storage.
     * @param items Vector of IX10Events, which are serialized.
     */
    public void writeX10Events(Vector items) {
        FILE_SAVED_HASH[1] = items;
    }

    /**
     * Reads from the hash the serialized objects as
     * a Vector of IX10Macro.
     * @return  Vector of IX10Macr serialized objects default value is an empyt Vector.
     */
    public Vector readInX10Macros() {
        if (FILE_SAVED_HASH[3] != null) {
            return FILE_SAVED_HASH[3];
        }
        return new Vector();
    }

    /**
     * Loads the alice monitors and returns them as a vector to
     * the application
     * @return  Vector of IX10MonitorEvents, which also have IRunEvents arrays
     */
    public Vector readInAliceMonitors() {
        if (FILE_SAVED_HASH[4] != null) {
            return FILE_SAVED_HASH[4];
        }
        Vector vMonitors = new Vector();
        vMonitors.add(new X10MonitorEvent());
        return vMonitors;
    }

    /**
     * Writes the IMacro vector to the array for serialization storage.
     * @param vec Vector of IMacros to be serialized
     */
    public void writeMacros(Vector vec) {
        FILE_SAVED_HASH[3] = vec;
    }

    /**
     * Load the Macro item triggers.  Thes are events that
     * are handled by the CM11A X10 interface. These triggers
     * are light A1 being turned on or appliance B2 turned off.
     * Trigger are stored as
     * Trigger.1=
     * @return vector of IMacroTrigger objects.
     */
    public Vector loadMacroTriggers() {
        return FILE_SAVED_HASH[2];
    }

    /**
     * Save the Macro item triggers.  Thes are events that
     * are handled by the CM11A X10 interface. These triggers
     * are light A1 being turned on or appliance B2 turned off.
     * @param items vector of IMacroTrigger objects to be serialized.
     */
    public void saveMacroTriggers(Vector items) {
        FILE_SAVED_HASH[2] = items;
    }

    /**
     *Moved saving macro information in the hash table
     *@param macro saves a serialized object into the hash.
     */
    public void saveMacro(IMacro macro) {
        int count = FILE_SAVED_HASH[3].size();
        boolean saved = false;
        IMacro currMacro;
        for (int i = 0; i < count; i++) {
            currMacro = (IMacro) FILE_SAVED_HASH[3].elementAt(i);
            if (macro.getMacroName().equalsIgnoreCase(currMacro.getMacroName())) {
                FILE_SAVED_HASH[3].setElementAt(macro, i);
                saved = true;
            }
        }
        if (!saved) {
            FILE_SAVED_HASH[3].addElement(macro);
        }
    }

    /**
     *Moved saving macro information in the hash table
     *@param macro allows removal of IMacro from hash.
     */
    public void deleteMacro(IMacro macro) {
        int count = FILE_SAVED_HASH[3].size();
        IMacro currMacro;
        for (int i = 0; i < count; i++) {
            currMacro = (IMacro) FILE_SAVED_HASH[3].elementAt(i);
            if (macro.getMacroName().equalsIgnoreCase(currMacro.getMacroName())) {
                FILE_SAVED_HASH[3].removeElementAt(i);
            }
        }
    }
}
