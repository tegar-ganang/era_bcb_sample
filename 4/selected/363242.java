package fr.esrf.TangoApi;

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.TangoDs.Except;

/**
 *	Class Description:
 *	This class manage device connection for Tango attribute access.
 *
 * @author  verdier
 * @version  $Revision: 19130 $
 */
public class AttributeProxy implements ApiDefs, java.io.Serializable {

    private String full_attname = null;

    private String attname = null;

    private DeviceProxy dev;

    private int idl_version = -1;

    public AttributeProxy(String attname) throws DevFailed {
        if (attname.indexOf('/') < 0) {
            String alias = attname;
            attname = ApiUtil.get_db_obj().get_attribute_alias(alias);
        }
        String devname = attname.substring(0, attname.lastIndexOf("/", attname.length() - 1));
        full_attname = attname;
        this.attname = attname.substring(attname.lastIndexOf("/", attname.length() - 1) + 1);
        dev = DeviceProxyFactory.get(devname);
    }

    public void set_timeout_millis(int millis) throws DevFailed {
        dev.set_timeout_millis(millis);
    }

    public DeviceProxy getDeviceProxy() {
        return dev;
    }

    public int get_idl_version() throws DevFailed {
        if (idl_version < 0) idl_version = dev.get_idl_version();
        return idl_version;
    }

    public String fullName() {
        return full_attname;
    }

    public String name() {
        return attname;
    }

    public long ping() throws DevFailed {
        return dev.ping();
    }

    public DevState state() throws DevFailed {
        return dev.state();
    }

    public String status() throws DevFailed {
        return dev.status();
    }

    public DbAttribute get_property() throws DevFailed {
        return dev.get_attribute_property(attname);
    }

    public void put_property(DbDatum property) throws DevFailed {
        DbAttribute db_att = new DbAttribute(attname);
        db_att.add(property);
        dev.put_attribute_property(db_att);
    }

    public void put_property(DbDatum[] properties) throws DevFailed {
        DbAttribute db_att = new DbAttribute(attname);
        for (DbDatum property : properties) db_att.add(property);
        dev.put_attribute_property(db_att);
    }

    public void delete_property(String propname) throws DevFailed {
        dev.delete_attribute_property(attname, propname);
    }

    public void delete_property(String[] propnames) throws DevFailed {
        dev.delete_attribute_property(attname, propnames);
    }

    public AttributeInfo get_info() throws DevFailed {
        return dev.get_attribute_info(attname);
    }

    public AttributeInfoEx get_info_ex() throws DevFailed {
        return dev.get_attribute_info_ex(attname);
    }

    public void set_info(AttributeInfo[] attr) throws DevFailed {
        dev.set_attribute_info(attr);
    }

    public void set_info(AttributeInfoEx[] attr) throws DevFailed {
        dev.set_attribute_info(attr);
    }

    public DeviceAttribute read() throws DevFailed {
        return dev.read_attribute(attname);
    }

    public void write(DeviceAttribute devattr) throws DevFailed {
        dev.write_attribute(devattr);
    }

    public DeviceAttribute write_read_attribute(DeviceAttribute devattr) throws DevFailed {
        return dev.write_read_attribute(devattr);
    }

    public DeviceAttribute[] write_read_attribute(DeviceAttribute[] devattr) throws DevFailed {
        return dev.write_read_attribute(devattr);
    }

    public DeviceDataHistory[] history(int nb) throws DevFailed {
        return dev.attribute_history(attname, nb);
    }

    public DeviceDataHistory[] history() throws DevFailed {
        return dev.attribute_history(attname);
    }

    public void poll(int period) throws DevFailed {
        dev.poll_attribute(attname, period);
    }

    public int get_polling_period() throws DevFailed {
        return dev.get_attribute_polling_period(attname);
    }

    public void stop_poll() throws DevFailed {
        dev.stop_poll_attribute(attname);
    }

    public int read_asynch() throws DevFailed {
        return dev.read_attribute_asynch(attname);
    }

    public void read_asynch(CallBack cb) throws DevFailed {
        dev.read_attribute_asynch(attname, cb);
    }

    public DeviceAttribute[] read_reply(int id, int timeout) throws DevFailed, AsynReplyNotArrived {
        return dev.read_attribute_reply(id, timeout);
    }

    public DeviceAttribute[] read_reply(int id) throws DevFailed, AsynReplyNotArrived {
        return dev.read_attribute_reply(id);
    }

    public int write_asynch(DeviceAttribute attr) throws DevFailed {
        return dev.write_attribute_asynch(attr);
    }

    public int write_asynch(DeviceAttribute attr, boolean forget) throws DevFailed {
        return dev.write_attribute_asynch(attr, forget);
    }

    public void write_asynch(DeviceAttribute attr, CallBack cb) throws DevFailed {
        dev.write_attribute_asynch(attr, cb);
    }

    public void write_reply(int id) throws DevFailed, AsynReplyNotArrived {
        dev.write_attribute_reply(id);
    }

    public void write_reply(int id, int timeout) throws DevFailed, AsynReplyNotArrived {
        dev.write_attribute_reply(id, timeout);
    }

    public int subscribe_event(int event, CallBack callback, String[] filters) throws DevFailed {
        return dev.subscribe_event(attname, event, callback, filters);
    }

    public static void main(String args[]) {
        String attname = "tango/admin/corvus/hoststate";
        try {
            AttributeProxy att = new AttributeProxy(attname);
            att.ping();
            System.out.println(att.name() + " is alive");
            DbAttribute db_att = att.get_property();
            for (int i = 0; i < db_att.size(); i++) {
                DbDatum datum = db_att.datum(i);
                System.out.println(datum.name + " : " + datum.extractString());
            }
            DeviceAttribute da = att.read();
            System.out.println(att.name() + " : " + da.extractShort());
            System.out.println(att.name() + " state  : " + ApiUtil.stateName(att.state()));
            System.out.println(att.name() + " status : " + att.status());
        } catch (DevFailed e) {
            Except.print_exception(e);
        }
    }
}
