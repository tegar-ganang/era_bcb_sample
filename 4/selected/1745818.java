package SnapExtractor;

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
import fr.esrf.TangoDs.Util;

public class SnapExtractorClass extends DeviceClass implements TangoConst {

    /**
     * SnapExtractorClass class instance (it is a singleton).
     */
    private static SnapExtractorClass _instance = null;

    public static SnapExtractorClass instance() {
        if (_instance == null) {
            System.err.println("SnapExtractorClass is not initialised !!!");
            System.err.println("Exiting");
            System.exit(-1);
        }
        return _instance;
    }

    public static SnapExtractorClass init(String class_name) throws DevFailed {
        if (_instance == null) {
            _instance = new SnapExtractorClass(class_name);
        }
        return _instance;
    }

    protected SnapExtractorClass(String name) throws DevFailed {
        super(name);
        Util.out2.println("Entering SnapExtractorClass constructor");
        write_class_property();
        get_class_property();
        Util.out2.println("Leaving SnapExtractorClass constructor");
    }

    @Override
    public void command_factory() {
        command_list.addElement(new RemoveAllDynAttrClass("RemoveAllDynAttr", Tango_DEV_VOID, Tango_DEV_VOID, "", "", DispLevel.EXPERT));
        command_list.addElement(new RemoveDynAttrsClass("RemoveDynAttrs", Tango_DEVVAR_STRINGARRAY, Tango_DEV_VOID, "", "", DispLevel.OPERATOR));
        command_list.addElement(new GetSnapClass("GetSnap", Tango_DEV_LONG, Tango_DEVVAR_STRINGARRAY, "snapID", "[attrRealName, dynAttrNameW,dynAttrNameR]*n", DispLevel.OPERATOR));
        command_list.addElement(new GetSnapValueClass("GetSnapValue", Tango_DEVVAR_STRINGARRAY, Tango_DEVVAR_STRINGARRAY, "snapID and attribute name", "Attribute's Read value and Write value", DispLevel.OPERATOR));
        command_list.addElement(new RemoveDynAttrClass("RemoveDynAttr", Tango_DEV_STRING, Tango_DEV_VOID, "", "", DispLevel.OPERATOR));
        command_list.addElement(new GetSnapsForContextClass("GetSnapsForContext", Tango_DEV_LONG, Tango_DEVVAR_LONGSTRINGARRAY, "", "", DispLevel.OPERATOR));
        command_list.addElement(new GetSnapIDClass("GetSnapID", Tango_DEVVAR_STRINGARRAY, Tango_DEVVAR_LONGARRAY, "ctx_id, criterion: \nSyntax: ctx_id, \"id_snap > | < | = | <= | >= nbr\",\n" + " \"time < | > | >= | <=  yyyy-mm-dd hh:mm:ss | dd-mm-yyyy hh:mm:ss\"," + "\n \"comment starts | ends | contains string\",\n first | last", "list of snapshot_id", DispLevel.OPERATOR));
        command_list.addElement(new GetSnapValuesClass("GetSnapValues", Tango_DEVVAR_STRINGARRAY, Tango_DEVVAR_STRINGARRAY, "snapID, true for read values or false for write values ,attribute names", "Attribute's Read value and Write value", DispLevel.OPERATOR));
        for (int i = 0; i < command_list.size(); i++) {
            Command cmd = (Command) command_list.elementAt(i);
        }
    }

    @Override
    public void attribute_factory(Vector att_list) throws DevFailed {
        Attr version = new Attr("version", Tango_DEV_STRING, AttrWriteType.READ);
        att_list.addElement(version);
    }

    @Override
    public void device_factory(String[] devlist) throws DevFailed {
        String device_version = ResourceBundle.getBundle("application").getString("project.version");
        for (int i = 0; i < devlist.length; i++) {
            device_list.addElement(new SnapExtractor(this, devlist[i], device_version));
            if (Util._UseDb == true) {
                export_device(((DeviceImpl) device_list.elementAt(i)));
            } else {
                export_device(((DeviceImpl) device_list.elementAt(i)), devlist[i]);
            }
        }
    }

    public DbDatum get_class_property(String name) throws DevFailed {
        DbDatum[] classProps = get_db_class().get_property(new String[] { name });
        return classProps[0];
    }

    public void get_class_property() throws DevFailed {
        if (Util._UseDb == false) {
            return;
        }
    }

    private void write_class_property() throws DevFailed {
        if (Util._UseDb == false) {
            return;
        }
        DbDatum[] data = new DbDatum[2];
        data[0] = new DbDatum("ProjectTitle");
        data[0].insert("Tango Device Server");
        data[1] = new DbDatum("Description");
        data[1].insert("SnapExtractor");
        get_db_class().put_property(data);
    }
}
