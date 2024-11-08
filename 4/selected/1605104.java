package SnapManager;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DispLevel;
import fr.esrf.TangoApi.DbDatum;
import fr.esrf.TangoDs.Attr;
import fr.esrf.TangoDs.Command;
import fr.esrf.TangoDs.DeviceClass;
import fr.esrf.TangoDs.DeviceImpl;
import fr.esrf.TangoDs.TangoConst;
import fr.esrf.TangoDs.TemplCommandInOut;
import fr.esrf.TangoDs.Util;

public class SnapManagerClass extends DeviceClass implements TangoConst {

    /**
     * Computer identifier on wich is settled the database HDB. The identifier
     * can be the computer name or its IP address. <br>
     * <b>Default value : </b> localhost.
     */
    String dbHost;

    /**
     * Database name.<br>
     * <b>Default value : </b> hdb
     */
    String dbName;

    /**
     * Database schema name.<br>
     * <b>Default value : </b> snap
     */
    String dbSchema;

    /**
     * SnapManagerClass class instance (it is a singleton).
     */
    private static SnapManagerClass _instance = null;

    public static SnapManagerClass instance() {
        if (_instance == null) {
            System.err.println("SnapManagerClass is not initialised !!!");
            System.err.println("Exiting");
            System.exit(-1);
        }
        return _instance;
    }

    public static SnapManagerClass init(final String class_name) throws DevFailed {
        if (_instance == null) {
            _instance = new SnapManagerClass(class_name);
        }
        return _instance;
    }

    protected SnapManagerClass(final String name) throws DevFailed {
        super(name);
        Util.out2.println("Entering SnapManagerClass constructor");
        write_class_property();
        get_class_property();
        Util.out2.println("Leaving SnapManagerClass constructor");
    }

    @Override
    public void command_factory() {
        command_list.addElement(new CreateNewContextClass("CreateNewContext", Tango_DEVVAR_STRINGARRAY, Tango_DEV_LONG, "All the informations usefull to create a context ,Snapshot pattern).", "The new assigned context ID", DispLevel.OPERATOR));
        command_list.addElement(new SetEquipmentsWithSnapshotClass("SetEquipmentsWithSnapshot", Tango_DEVVAR_STRINGARRAY, Tango_DEV_VOID, "The snapshot from which equipments are set.", "", DispLevel.OPERATOR));
        command_list.addElement(new SetEquipmentsWithCommandClass("SetEquipmentsWithCommand", Tango_DEVVAR_STRINGARRAY, Tango_DEV_STRING, "The command name,  STORED_READ_VALUE || STORED_WRITE_VALUE ,\n and the snapshot ID from which equipments are set.", "", DispLevel.OPERATOR));
        command_list.addElement(new SetEquipmentsClass("SetEquipments", Tango_DEVVAR_STRINGARRAY, Tango_DEV_VOID, "* First Case: Setpoint is  done on all the snapshot attributes:\n" + "  - argin[0]= the snap identifier\n" + "  - argin[1]=STORED_READ_VALUE (Setpoint with theirs read values) or STORED_WRITE_VALUE (Setpoint with theirs write values)\n\n" + "* Second Case: Setpoint is done on a set of the snapshot attributes:\n " + "  - argin[0]= the snap identifier\n" + "  - argin[1]=the number of attributes.\n" + " Let us note index the last index used (for example, at this point,index = 2).\n" + "  - argin[index]=NEW_VALUE or STORED_READ_VALUE or STORED_WRITE_VALUE\n" + "  - argin[index+1]= the attribut name\n" + "  - argin[index+2]= the value to set when NEW_VALUE is requested", "", DispLevel.OPERATOR));
        command_list.addElement(new UpdateSnapCommentClass("UpdateSnapComment", Tango_DEVVAR_LONGSTRINGARRAY, Tango_DEV_VOID, "1) snapshot identifier 2) The new comment", "", DispLevel.OPERATOR));
        command_list.addElement(new LaunchSnapShotCmd("LaunchSnapShot", Tango_DEV_SHORT, Tango_DEV_VOID, "The snapshot associated context's identifier", "", DispLevel.OPERATOR));
        command_list.addElement(new TemplCommandInOut("GetSnapShotResult", "getSnapShotResult", "The snapshot associated context's identifier", "The new snapshot identifier", DispLevel.OPERATOR));
        command_list.addElement(new TemplCommandInOut("GetSnapShotComment", "getSnapComment", "The snapshot id", "The comment", DispLevel.OPERATOR));
        for (int i = 0; i < command_list.size(); i++) {
            final Command cmd = (Command) command_list.elementAt(i);
        }
    }

    @Override
    public void attribute_factory(final Vector att_list) throws DevFailed {
        final Attr version = new Attr("version", Tango_DEV_STRING, AttrWriteType.READ);
        att_list.addElement(version);
    }

    @Override
    public void device_factory(final String[] devlist) throws DevFailed {
        String device_version = "unkown";
        try {
            device_version = ResourceBundle.getBundle("application").getString("project.version");
        } catch (final MissingResourceException e) {
        }
        for (int i = 0; i < devlist.length; i++) {
            device_list.addElement(new SnapManager(this, devlist[i], device_version));
            if (Util._UseDb == true) {
                export_device((DeviceImpl) device_list.elementAt(i));
            } else {
                export_device((DeviceImpl) device_list.elementAt(i), devlist[i]);
            }
        }
    }

    public DbDatum get_class_property(final String name) throws DevFailed {
        final DbDatum[] classProps = get_db_class().get_property(new String[] { name });
        return classProps[0];
    }

    public void get_class_property() throws DevFailed {
    }

    private void write_class_property() throws DevFailed {
        if (Util._UseDb == false) {
            return;
        }
        final DbDatum[] data = new DbDatum[2];
        data[0] = new DbDatum("ProjectTitle");
        data[0].insert("Tango Device Server");
        data[1] = new DbDatum("Description");
        data[1].insert("This DServer provides the connections points and methods to the SnapShot service.");
        get_db_class().put_property(data);
    }
}
