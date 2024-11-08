package fr.esrf.TangoApi;

import fr.esrf.Tango.AttributeValue;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.Tango.factory.TangoFactory;
import fr.esrf.TangoDs.Except;
import fr.esrf.TangoApi.events.EventData;
import fr.esrf.TangoApi.events.EventQueue;
import fr.esrf.TangoApi.events.DbEventImportInfo;
import fr.esrf.TangoDs.TangoConst;
import org.omg.CORBA.Request;
import java.util.Vector;

/**
 * Class Description: This class manage device connection for Tango objects. It
 * is an api between user and IDL Device object.
 *
 * <Br>
 * <Br>
 * <Br>
 * <b> Usage example: </b> <Br>
 * <ul>
 * <i> String status; <Br>
 * DeviceProxy dev = ApiUtil.getDeviceProxy("sys/steppermotor/1"); <Br>
 * try { <Br>
 * <ul>
 * DeviceData data = dev.command_inout("DevStatus"); <Br>
 * status = data.extractString(); <Br>
 * </ul> } <Br>
 * catch (DevFailed e) { <Br>
 * <ul>
 * status = "Unknown status"; <Br>
 * Except.print_exception(e); <Br>
 * </ul> } <Br>
 * </ul>
 * </i>
 *
 * @author verdier
 * @version $Revision: 19130 $
 */
public class DeviceProxy extends Connection implements ApiDefs {

    private IDeviceProxyDAO deviceProxy = null;

    private static final boolean check_idl = false;

    /**
	 *	DbDevice object to make an agregat..
	 */
    private DbDevice db_dev;

    private String full_class_name;

    /**
	 *	Instance on administration device
	 */
    private DeviceProxy adm_dev = null;

    private String[] attnames_array = null;

    /**
	 *	Lock device counter for this object instance
	 */
    protected int proxy_lock_cnt = 0;

    /**
	 *	Event queue instance
	 */
    protected EventQueue event_queue;

    private DbEventImportInfo evt_import_info = null;

    public DeviceProxy() throws DevFailed {
        super();
        deviceProxy = TangoFactory.getSingleton().getDeviceProxyDAO();
        deviceProxy.init(this);
        DeviceProxyFactory.add(this);
    }

    public DeviceProxy(DbDevImportInfo info) throws DevFailed {
        super(info);
        deviceProxy = TangoFactory.getSingleton().getDeviceProxyDAO();
        deviceProxy.init(this, info.name);
        DeviceProxyFactory.add(this);
    }

    public DeviceProxy(String devname) throws DevFailed {
        super(devname);
        deviceProxy = TangoFactory.getSingleton().getDeviceProxyDAO();
        deviceProxy.init(this, devname);
        DeviceProxyFactory.add(this);
    }

    DeviceProxy(String devname, boolean check_access) throws DevFailed {
        super(devname, check_access);
        deviceProxy = TangoFactory.getSingleton().getDeviceProxyDAO();
        deviceProxy.init(this, devname, check_access);
        DeviceProxyFactory.add(this);
    }

    public DeviceProxy(String devname, String ior) throws DevFailed {
        super(devname, ior);
        deviceProxy = TangoFactory.getSingleton().getDeviceProxyDAO();
        deviceProxy.init(this, devname, ior);
        DeviceProxyFactory.add(this);
    }

    public DeviceProxy(String devname, String host, String port) throws DevFailed {
        super(devname, host, port);
        deviceProxy = TangoFactory.getSingleton().getDeviceProxyDAO();
        deviceProxy.init(this, devname, host, host);
        DeviceProxyFactory.add(this);
    }

    public boolean use_db() {
        return deviceProxy.use_db(this);
    }

    public Database get_db_obj() throws DevFailed {
        return deviceProxy.get_db_obj(this);
    }

    protected void import_admin_device(DbDevImportInfo info) throws DevFailed {
        deviceProxy.import_admin_device(this, info);
    }

    protected void import_admin_device(String origin) throws DevFailed {
        deviceProxy.import_admin_device(this, origin);
    }

    public String name() {
        return deviceProxy.name(this);
    }

    public String status() throws DevFailed {
        return deviceProxy.status(this);
    }

    public String status(boolean src) throws DevFailed {
        return deviceProxy.status(this, src);
    }

    public DevState state() throws DevFailed {
        return deviceProxy.state(this);
    }

    public DevState state(boolean src) throws DevFailed {
        return deviceProxy.state(this, src);
    }

    public CommandInfo command_query(String cmdname) throws DevFailed {
        return deviceProxy.command_query(this, cmdname);
    }

    public String get_class() throws DevFailed {
        return deviceProxy.get_class(this);
    }

    public String[] get_class_inheritance() throws DevFailed {
        return deviceProxy.get_class_inheritance(this);
    }

    public void put_alias(String aliasname) throws DevFailed {
        deviceProxy.put_alias(this, aliasname);
    }

    public String get_alias() throws DevFailed {
        return deviceProxy.get_alias(this);
    }

    public DeviceInfo get_info() throws DevFailed {
        return deviceProxy.get_info(this);
    }

    public DbDevImportInfo import_device() throws DevFailed {
        return deviceProxy.import_device(this);
    }

    public void export_device(DbDevExportInfo devinfo) throws DevFailed {
        deviceProxy.export_device(this, devinfo);
    }

    public void unexport_device() throws DevFailed {
        deviceProxy.unexport_device(this);
    }

    public void add_device(DbDevInfo devinfo) throws DevFailed {
        deviceProxy.add_device(this, devinfo);
    }

    public void delete_device() throws DevFailed {
        deviceProxy.delete_device(this);
    }

    public String[] get_property_list(String wildcard) throws DevFailed {
        return deviceProxy.get_property_list(this, wildcard);
    }

    public DbDatum[] get_property(String[] propnames) throws DevFailed {
        return deviceProxy.get_property(this, propnames);
    }

    public DbDatum get_property(String propname) throws DevFailed {
        return deviceProxy.get_property(this, propname);
    }

    public DbDatum[] get_property(DbDatum[] properties) throws DevFailed {
        return deviceProxy.get_property(this, properties);
    }

    public void put_property(DbDatum prop) throws DevFailed {
        deviceProxy.put_property(this, prop);
    }

    public void put_property(DbDatum[] properties) throws DevFailed {
        deviceProxy.put_property(this, properties);
    }

    public void delete_property(String[] propnames) throws DevFailed {
        deviceProxy.delete_property(this, propnames);
    }

    public void delete_property(String propname) throws DevFailed {
        deviceProxy.delete_property(this, propname);
    }

    public void delete_property(DbDatum[] properties) throws DevFailed {
        deviceProxy.delete_property(this, properties);
    }

    public String[] get_attribute_list() throws DevFailed {
        return deviceProxy.get_attribute_list(this);
    }

    public void put_attribute_property(DbAttribute[] attr) throws DevFailed {
        deviceProxy.put_attribute_property(this, attr);
    }

    public void put_attribute_property(DbAttribute attr) throws DevFailed {
        deviceProxy.put_attribute_property(this, attr);
    }

    public void delete_attribute_property(String attname, String[] propnames) throws DevFailed {
        deviceProxy.delete_attribute_property(this, attname, propnames);
    }

    public void delete_attribute_property(String attname, String propname) throws DevFailed {
        deviceProxy.delete_attribute_property(this, attname, propname);
    }

    public void delete_attribute_property(DbAttribute attr) throws DevFailed {
        deviceProxy.delete_attribute_property(this, attr);
    }

    public void delete_attribute_property(DbAttribute[] attr) throws DevFailed {
        deviceProxy.delete_attribute_property(this, attr);
    }

    public DbAttribute[] get_attribute_property(String[] attnames) throws DevFailed {
        return deviceProxy.get_attribute_property(this, attnames);
    }

    public DbAttribute get_attribute_property(String attname) throws DevFailed {
        return deviceProxy.get_attribute_property(this, attname);
    }

    public void delete_attribute(String attname) throws DevFailed {
        deviceProxy.delete_attribute(this, attname);
    }

    public AttributeInfo[] get_attribute_info(String[] attnames) throws DevFailed {
        return deviceProxy.get_attribute_info(this, attnames);
    }

    public AttributeInfoEx[] get_attribute_info_ex(String[] attnames) throws DevFailed {
        return deviceProxy.get_attribute_info_ex(this, attnames);
    }

    public AttributeInfo[] get_attribute_config(String[] attnames) throws DevFailed {
        return deviceProxy.get_attribute_info(this, attnames);
    }

    public AttributeInfo get_attribute_info(String attname) throws DevFailed {
        return deviceProxy.get_attribute_info(this, attname);
    }

    public AttributeInfoEx get_attribute_info_ex(String attname) throws DevFailed {
        return deviceProxy.get_attribute_info_ex(this, attname);
    }

    public AttributeInfo get_attribute_config(String attname) throws DevFailed {
        return deviceProxy.get_attribute_info(this, attname);
    }

    public AttributeInfo[] get_attribute_info() throws DevFailed {
        return deviceProxy.get_attribute_info(this);
    }

    public AttributeInfoEx[] get_attribute_info_ex() throws DevFailed {
        return deviceProxy.get_attribute_info_ex(this);
    }

    public AttributeInfo[] get_attribute_config() throws DevFailed {
        return deviceProxy.get_attribute_info(this);
    }

    public void set_attribute_info(AttributeInfo[] attr) throws DevFailed {
        deviceProxy.set_attribute_info(this, attr);
    }

    public void set_attribute_info(AttributeInfoEx[] attr) throws DevFailed {
        deviceProxy.set_attribute_info(this, attr);
    }

    public void set_attribute_config(AttributeInfo[] attr) throws DevFailed {
        deviceProxy.set_attribute_info(this, attr);
    }

    public DeviceAttribute read_attribute(String attname) throws DevFailed {
        return deviceProxy.read_attribute(this, attname);
    }

    public AttributeValue read_attribute_value(String attname) throws DevFailed {
        return deviceProxy.read_attribute_value(this, attname);
    }

    public DeviceAttribute[] read_attribute(String[] attnames) throws DevFailed {
        checkDuplication(attnames, "DeviceProxy.read_attribute()");
        return deviceProxy.read_attribute(this, attnames);
    }

    public void write_attribute(DeviceAttribute devattr) throws DevFailed {
        deviceProxy.write_attribute(this, devattr);
    }

    public void write_attribute(DeviceAttribute[] devattr) throws DevFailed {
        deviceProxy.write_attribute(this, devattr);
    }

    public DeviceAttribute write_read_attribute(DeviceAttribute devattr) throws DevFailed {
        return deviceProxy.write_read_attribute(this, new DeviceAttribute[] { devattr })[0];
    }

    public DeviceAttribute[] write_read_attribute(DeviceAttribute[] devattr) throws DevFailed {
        return deviceProxy.write_read_attribute(this, devattr);
    }

    public DeviceProxy get_adm_dev() throws DevFailed {
        return deviceProxy.get_adm_dev(this);
    }

    public void poll_command(String cmdname, int period) throws DevFailed {
        deviceProxy.poll_command(this, cmdname, period);
    }

    public void poll_attribute(String attname, int period) throws DevFailed {
        deviceProxy.poll_attribute(this, attname, period);
    }

    public void stop_poll_command(String cmdname) throws DevFailed {
        deviceProxy.stop_poll_command(this, cmdname);
    }

    public void stop_poll_attribute(String attname) throws DevFailed {
        deviceProxy.stop_poll_attribute(this, attname);
    }

    public String[] polling_status() throws DevFailed {
        return deviceProxy.polling_status(this);
    }

    public DeviceDataHistory[] command_history(String cmdname, int nb) throws DevFailed {
        return deviceProxy.command_history(this, cmdname, nb);
    }

    public DeviceDataHistory[] attribute_history(String attname, int nb) throws DevFailed {
        return deviceProxy.attribute_history(this, attname, nb);
    }

    public DeviceDataHistory[] command_history(String cmdname) throws DevFailed {
        return deviceProxy.command_history(this, cmdname);
    }

    public DeviceDataHistory[] attribute_history(String attname) throws DevFailed {
        return deviceProxy.attribute_history(this, attname);
    }

    public int get_attribute_polling_period(String attname) throws DevFailed {
        return deviceProxy.get_attribute_polling_period(this, attname);
    }

    public int get_command_polling_period(DeviceProxy deviceProxy, String cmdname) throws DevFailed {
        return deviceProxy.get_command_polling_period(this, cmdname);
    }

    public int command_inout_asynch(String cmdname, DeviceData data_in) throws DevFailed {
        return deviceProxy.command_inout_asynch(this, cmdname, data_in);
    }

    public int command_inout_asynch(String cmdname) throws DevFailed {
        return deviceProxy.command_inout_asynch(this, cmdname);
    }

    public int command_inout_asynch(String cmdname, boolean forget) throws DevFailed {
        return deviceProxy.command_inout_asynch(this, cmdname, forget);
    }

    public int command_inout_asynch(String cmdname, DeviceData data_in, boolean forget) throws DevFailed {
        return deviceProxy.command_inout_asynch(this, cmdname, data_in, forget);
    }

    public void command_inout_asynch(String cmdname, DeviceData argin, CallBack cb) throws DevFailed {
        deviceProxy.command_inout_asynch(this, cmdname, argin, cb);
    }

    public void command_inout_asynch(String cmdname, CallBack cb) throws DevFailed {
        deviceProxy.command_inout_asynch(this, cmdname, cb);
    }

    public DeviceData command_inout_reply(int id, int timeout) throws DevFailed, AsynReplyNotArrived {
        return deviceProxy.command_inout_reply(this, id, timeout);
    }

    DeviceData command_inout_reply(AsyncCallObject aco, int timeout) throws DevFailed, AsynReplyNotArrived {
        return deviceProxy.command_inout_reply(this, aco, timeout);
    }

    public DeviceData command_inout_reply(int id) throws DevFailed, AsynReplyNotArrived {
        return deviceProxy.command_inout_reply(this, id);
    }

    DeviceData command_inout_reply(AsyncCallObject aco) throws DevFailed, AsynReplyNotArrived {
        return deviceProxy.command_inout_reply(this, aco);
    }

    public int read_attribute_asynch(String attname) throws DevFailed {
        return deviceProxy.read_attribute_asynch(this, attname);
    }

    public int read_attribute_asynch(String[] attnames) throws DevFailed {
        checkDuplication(attnames, "DeviceProxy.read_attribute_asynch()");
        return deviceProxy.read_attribute_asynch(this, attnames);
    }

    protected String get_asynch_idl_cmd(Request request, String idl_cmd) {
        return deviceProxy.get_asynch_idl_cmd(this, request, idl_cmd);
    }

    protected void check_asynch_reply(Request request, int id, String idl_cmd) throws DevFailed, AsynReplyNotArrived {
        deviceProxy.check_asynch_reply(this, request, id, idl_cmd);
    }

    public DeviceAttribute[] read_attribute_reply(int id, int timeout) throws DevFailed, AsynReplyNotArrived {
        return deviceProxy.read_attribute_reply(this, id, timeout);
    }

    public DeviceAttribute[] read_attribute_reply(int id) throws DevFailed, AsynReplyNotArrived {
        return deviceProxy.read_attribute_reply(this, id);
    }

    public void read_attribute_asynch(String attname, CallBack cb) throws DevFailed {
        deviceProxy.read_attribute_asynch(this, attname, cb);
    }

    public void read_attribute_asynch(String[] attnames, CallBack cb) throws DevFailed {
        deviceProxy.read_attribute_asynch(this, attnames, cb);
    }

    public int write_attribute_asynch(DeviceAttribute attr) throws DevFailed {
        return deviceProxy.write_attribute_asynch(this, attr);
    }

    public int write_attribute_asynch(DeviceAttribute attr, boolean forget) throws DevFailed {
        return deviceProxy.write_attribute_asynch(this, attr, forget);
    }

    public int write_attribute_asynch(DeviceAttribute[] attribs) throws DevFailed {
        return deviceProxy.write_attribute_asynch(this, attribs);
    }

    public int write_attribute_asynch(DeviceAttribute[] attribs, boolean forget) throws DevFailed {
        return deviceProxy.write_attribute_asynch(this, attribs, forget);
    }

    public void write_attribute_reply(int id) throws DevFailed, AsynReplyNotArrived {
        deviceProxy.write_attribute_reply(this, id);
    }

    public void write_attribute_reply(int id, int timeout) throws DevFailed, AsynReplyNotArrived {
        deviceProxy.write_attribute_reply(this, id, timeout);
    }

    public void write_attribute_asynch(DeviceAttribute attr, CallBack cb) throws DevFailed {
        deviceProxy.write_attribute_asynch(this, attr, cb);
    }

    public void write_attribute_asynch(DeviceAttribute[] attribs, CallBack cb) throws DevFailed {
        deviceProxy.write_attribute_asynch(this, attribs, cb);
    }

    public int pending_asynch_call(int reply_model) {
        return deviceProxy.pending_asynch_call(this, reply_model);
    }

    public void get_asynch_replies() {
        deviceProxy.get_asynch_replies(this);
    }

    public void get_asynch_replies(int timeout) {
        deviceProxy.get_asynch_replies(this, timeout);
    }

    public void add_logging_target(String target_type, String target_name) throws DevFailed {
        deviceProxy.add_logging_target(this, target_type + "::" + target_name);
    }

    public void add_logging_target(String target) throws DevFailed {
        deviceProxy.add_logging_target(this, target);
    }

    public void remove_logging_target(String target_type, String target_name) throws DevFailed {
        deviceProxy.remove_logging_target(this, target_type, target_name);
    }

    public String[] get_logging_target() throws DevFailed {
        return deviceProxy.get_logging_target(this);
    }

    public int get_logging_level() throws DevFailed {
        return deviceProxy.get_logging_level(this);
    }

    public void set_logging_level(int level) throws DevFailed {
        deviceProxy.set_logging_level(this, level);
    }

    public void lock() throws DevFailed {
        this.lock(TangoConst.DEFAULT_LOCK_VALIDITY);
    }

    public void lock(int validity) throws DevFailed {
        deviceProxy.lock(this, validity);
        proxy_lock_cnt++;
    }

    public int unlock() throws DevFailed {
        int n = deviceProxy.unlock(this);
        proxy_lock_cnt--;
        return n;
    }

    public boolean isLocked() throws DevFailed {
        return deviceProxy.isLocked(this);
    }

    public boolean isLockedByMe() throws DevFailed {
        return deviceProxy.isLockedByMe(this);
    }

    public String getLockerStatus() throws DevFailed {
        return deviceProxy.getLockerStatus(this);
    }

    public LockerInfo getLockerInfo() throws DevFailed {
        return deviceProxy.getLockerInfo(this);
    }

    public String[] dev_inform() throws DevFailed {
        return deviceProxy.dev_inform(this);
    }

    public void set_rpc_protocol(int mode) throws DevFailed {
        deviceProxy.set_rpc_protocol(this, mode);
    }

    public int get_rpc_protocol() throws DevFailed {
        return deviceProxy.get_rpc_protocol(this);
    }

    public static void main(String args[]) {
        IDeviceProxyDAO deviceProxyDAO = TangoFactory.getSingleton().getDeviceProxyDAO();
        deviceProxyDAO.main(args);
    }

    public int subscribe_event(String attr_name, int event, CallBack callback, String[] filters) throws DevFailed {
        return deviceProxy.subscribe_event(this, attr_name, event, callback, filters, false);
    }

    public int subscribe_event(String attr_name, int event, CallBack callback, String[] filters, boolean stateless) throws DevFailed {
        return deviceProxy.subscribe_event(this, attr_name, event, callback, filters, stateless);
    }

    public int subscribe_event(String attr_name, int event, int max_size, String[] filters, boolean stateless) throws DevFailed {
        return deviceProxy.subscribe_event(this, attr_name, event, max_size, filters, stateless);
    }

    public void setEventQueue(EventQueue eq) {
        event_queue = eq;
    }

    public EventQueue getEventQueue() {
        return event_queue;
    }

    public int get_event_queue_size() {
        return event_queue.size();
    }

    public int get_event_queue_size(int event_type) {
        return event_queue.size(event_type);
    }

    public EventData get_next_event() throws DevFailed {
        return event_queue.getNextEvent();
    }

    public EventData get_next_event(int event_type) throws DevFailed {
        return event_queue.getNextEvent(event_type);
    }

    public synchronized long get_last_event_date() throws DevFailed {
        return event_queue.getLastEventDate();
    }

    public EventData[] get_events() {
        return event_queue.getEvents();
    }

    public EventData[] get_events(int event_type) {
        return event_queue.getEvents(event_type);
    }

    public void unsubscribe_event(int event_id) throws DevFailed {
        deviceProxy.unsubscribe_event(this, event_id);
    }

    public IDeviceProxyDAO getDeviceProxy() {
        return deviceProxy;
    }

    public void setDeviceProxy(IDeviceProxyDAO deviceProxy) {
        this.deviceProxy = deviceProxy;
    }

    public DeviceProxy getAdm_dev() {
        return this.adm_dev;
    }

    public void setAdm_dev(DeviceProxy adm_dev) {
        this.adm_dev = adm_dev;
    }

    public String[] getAttnames_array() {
        return attnames_array;
    }

    public void setAttnames_array(String[] attnames_array) {
        this.attnames_array = attnames_array;
    }

    public DbDevice getDb_dev() {
        return db_dev;
    }

    public void setDb_dev(DbDevice db_dev) {
        this.db_dev = db_dev;
    }

    public String getFull_class_name() {
        return full_class_name;
    }

    public void setFull_class_name(String full_class_name) {
        this.full_class_name = full_class_name;
    }

    public static boolean isCheck_idl() {
        return check_idl;
    }

    public DbEventImportInfo get_evt_import_info() {
        return evt_import_info;
    }

    public void set_evt_import_info(DbEventImportInfo info) {
        evt_import_info = info;
    }

    public static void checkDuplication(String[] list, String orig) throws DevFailed {
        Vector<String> dupli = new Vector<String>();
        for (int i = 0; i < list.length; i++) {
            String str = list[i];
            for (int j = i + 1; j < list.length; j++) {
                if (list[j].equalsIgnoreCase(str)) if (dupli.indexOf(str) < 0) dupli.add(str);
            }
        }
        if (dupli.size() > 0) {
            String message = "Several times the same attribute in required attribute list: ";
            for (int i = 0; i < dupli.size(); i++) {
                message += dupli.get(i);
                if (i < dupli.size() - 1) {
                    message += ", ";
                }
            }
            Except.throw_exception("", message, orig);
        }
    }

    int factory_instance_counter = 1;

    protected void finalize() {
        if (proxy_lock_cnt > 0) {
            try {
                unlock();
            } catch (DevFailed e) {
            }
            System.out.println("======== DeviceProxy " + get_name() + " object deleted.=======");
        }
        try {
            super.finalize();
        } catch (Throwable e) {
        }
    }
}
