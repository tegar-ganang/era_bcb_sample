package fr.esrf.TangoApi;

import org.omg.CORBA.Any;
import org.omg.CORBA.Request;
import fr.esrf.Tango.AttributeValue;
import fr.esrf.Tango.DevError;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.webapi.IWebImpl;
import fr.esrf.webapi.WebServerClientUtil;

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
 * DeviceProxy dev = new DeviceProxy("sys/steppermotor/1"); <Br>
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
 * @version $Revision: 12929 $
 */
public class DeviceProxyDAOWebImpl extends ConnectionDAOWebImpl implements IDeviceProxyDAO, ApiDefs, IWebImpl {

    private Object[] classParam = null;

    public void init(DeviceProxy deviceProxy) {
        classParam = new Object[] {};
    }

    public void init(DeviceProxy deviceProxy, String devname) throws DevFailed {
        classParam = new Object[] { devname };
    }

    public void init(DeviceProxy deviceProxy, String devname, boolean check_access) throws DevFailed {
        classParam = new Object[] { devname, check_access };
    }

    public void init(DeviceProxy deviceProxy, String devname, String ior) {
        classParam = new Object[] { devname, ior };
    }

    public void init(DeviceProxy deviceProxy, String devname, String host, String port) throws DevFailed {
        classParam = new Object[] { devname, host, port };
    }

    public boolean use_db(DeviceProxy deviceProxy) {
        try {
            return (Boolean) WebServerClientUtil.getResponse(this, classParam, "use_db", new Object[] {}, new Class[] {});
        } catch (DevFailed e) {
            e.printStackTrace();
            return false;
        }
    }

    public Database get_db_obj(DeviceProxy deviceProxy) throws DevFailed {
        return (Database) WebServerClientUtil.getResponse(this, classParam, "get_db_obj", new Object[] {}, new Class[] {});
    }

    public void import_admin_device(DeviceProxy deviceProxy, String origin) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "import_admin_device", new Object[] { origin }, new Class[] { String.class });
    }

    public String name(DeviceProxy deviceProxy) {
        try {
            return (String) WebServerClientUtil.getResponse(this, classParam, "name", new Object[] {}, new Class[] {});
        } catch (DevFailed e) {
            e.printStackTrace();
            return null;
        }
    }

    public String status(DeviceProxy deviceProxy) throws DevFailed {
        return (String) WebServerClientUtil.getResponse(this, classParam, "status", new Object[] {}, new Class[] {});
    }

    public String status(DeviceProxy deviceProxy, boolean src) throws DevFailed {
        return (String) WebServerClientUtil.getResponse(this, classParam, "status", new Object[] { src }, new Class[] { boolean.class });
    }

    public DevState state(DeviceProxy deviceProxy) throws DevFailed {
        return (DevState) WebServerClientUtil.getResponse(this, classParam, "state", new Object[] {}, new Class[] {});
    }

    public DevState state(DeviceProxy deviceProxy, boolean src) throws DevFailed {
        return (DevState) WebServerClientUtil.getResponse(this, classParam, "state", new Object[] { src }, new Class[] { boolean.class });
    }

    public CommandInfo command_query(DeviceProxy deviceProxy, String cmdname) throws DevFailed {
        return (CommandInfo) WebServerClientUtil.getResponse(this, classParam, "command_query", new Object[] { cmdname }, new Class[] { String.class });
    }

    public String get_class(DeviceProxy deviceProxy) throws DevFailed {
        return (String) WebServerClientUtil.getResponse(this, classParam, "get_class", new Object[] {}, new Class[] {});
    }

    public String[] get_class_inheritance(DeviceProxy deviceProxy) throws DevFailed {
        return (String[]) WebServerClientUtil.getResponse(this, classParam, "get_class_inheritance", new Object[] {}, new Class[] {});
    }

    public void put_alias(DeviceProxy deviceProxy, String aliasname) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "put_alias", new Object[] { aliasname }, new Class[] { String.class });
    }

    public String get_alias(DeviceProxy deviceProxy) throws DevFailed {
        return (String) WebServerClientUtil.getResponse(this, classParam, "get_alias", new Object[] {}, new Class[] {});
    }

    public DeviceInfo get_info(DeviceProxy deviceProxy) throws DevFailed {
        return (DeviceInfo) WebServerClientUtil.getResponse(this, classParam, "get_info", new Object[] {}, new Class[] {});
    }

    public DbDevImportInfo import_device(DeviceProxy deviceProxy) throws DevFailed {
        return (DbDevImportInfo) WebServerClientUtil.getResponse(this, classParam, "import_device", new Object[] {}, new Class[] {});
    }

    public void export_device(DeviceProxy deviceProxy, DbDevExportInfo devinfo) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "export_device", new Object[] { devinfo }, new Class[] { DbDevExportInfo.class });
    }

    public void unexport_device(DeviceProxy deviceProxy) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "unexport_device", new Object[] {}, new Class[] {});
    }

    public void add_device(DeviceProxy deviceProxy, DbDevInfo devinfo) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "add_device", new Object[] { devinfo }, new Class[] { DbDevInfo.class });
    }

    public void delete_device(DeviceProxy deviceProxy) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "delete_device", new Object[] {}, new Class[] {});
    }

    public String[] get_property_list(DeviceProxy deviceProxy, String wildcard) throws DevFailed {
        return (String[]) WebServerClientUtil.getResponse(this, classParam, "get_property_list", new Object[] { wildcard }, new Class[] { String.class });
    }

    public DbDatum[] get_property(DeviceProxy deviceProxy, String[] propnames) throws DevFailed {
        return (DbDatum[]) WebServerClientUtil.getResponse(this, classParam, "get_property", new Object[] { propnames }, new Class[] { String[].class });
    }

    public DbDatum get_property(DeviceProxy deviceProxy, String propname) throws DevFailed {
        return (DbDatum) WebServerClientUtil.getResponse(this, classParam, "get_property", new Object[] { propname }, new Class[] { String.class });
    }

    public DbDatum[] get_property(DeviceProxy deviceProxy, DbDatum[] properties) throws DevFailed {
        return (DbDatum[]) WebServerClientUtil.getResponse(this, classParam, "get_property", new Object[] { properties }, new Class[] { DbDatum[].class });
    }

    public void put_property(DeviceProxy deviceProxy, DbDatum prop) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "put_property", new Object[] { prop }, new Class[] { DbDatum.class });
    }

    public void put_property(DeviceProxy deviceProxy, DbDatum[] properties) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "put_property", new Object[] { properties }, new Class[] { DbDatum[].class });
    }

    public void delete_property(DeviceProxy deviceProxy, String[] propnames) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "delete_property", new Object[] { propnames }, new Class[] { String[].class });
    }

    public void delete_property(DeviceProxy deviceProxy, String propname) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "delete_property", new Object[] { propname }, new Class[] { String.class });
    }

    public void delete_property(DeviceProxy deviceProxy, DbDatum[] properties) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "delete_property", new Object[] { properties }, new Class[] { DbDatum[].class });
    }

    public String[] get_attribute_list(DeviceProxy deviceProxy) throws DevFailed {
        return (String[]) WebServerClientUtil.getResponse(this, classParam, "get_attribute_list", new Object[] {}, new Class[] {});
    }

    public void put_attribute_property(DeviceProxy deviceProxy, DbAttribute[] attr) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "put_attribute_property", new Object[] { attr }, new Class[] { DbAttribute[].class });
    }

    public void put_attribute_property(DeviceProxy deviceProxy, DbAttribute attr) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "put_attribute_property", new Object[] { attr }, new Class[] { DbAttribute.class });
    }

    public void delete_attribute_property(DeviceProxy deviceProxy, String attname, String[] propnames) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "delete_attribute_property", new Object[] { attname, propnames }, new Class[] { String.class, String[].class });
    }

    public void delete_attribute_property(DeviceProxy deviceProxy, String attname, String propname) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "delete_attribute_property", new Object[] { attname, propname }, new Class[] { String.class, String.class });
    }

    public void delete_attribute_property(DeviceProxy deviceProxy, DbAttribute attr) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "delete_attribute_property", new Object[] { attr }, new Class[] { DbAttribute.class });
    }

    public void delete_attribute_property(DeviceProxy deviceProxy, DbAttribute[] attr) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "delete_attribute_property", new Object[] { attr }, new Class[] { DbAttribute[].class });
    }

    public DbAttribute[] get_attribute_property(DeviceProxy deviceProxy, String[] attnames) throws DevFailed {
        return (DbAttribute[]) WebServerClientUtil.getResponse(this, classParam, "get_attribute_property", new Object[] { attnames }, new Class[] { String[].class });
    }

    public DbAttribute get_attribute_property(DeviceProxy deviceProxy, String attname) throws DevFailed {
        return (DbAttribute) WebServerClientUtil.getResponse(this, classParam, "get_attribute_property", new Object[] { attname }, new Class[] { String.class });
    }

    public void delete_attribute(DeviceProxy deviceProxy, String attname) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "delete_attribute", new Object[] { attname }, new Class[] { String.class });
    }

    public AttributeInfo[] get_attribute_info(DeviceProxy deviceProxy, String[] attnames) throws DevFailed {
        return (AttributeInfo[]) WebServerClientUtil.getResponse(this, classParam, "get_attribute_info", new Object[] { attnames }, new Class[] { String[].class });
    }

    public AttributeInfoEx[] get_attribute_info_ex(DeviceProxy deviceProxy, String[] attnames) throws DevFailed {
        return (AttributeInfoEx[]) WebServerClientUtil.getResponse(this, classParam, "get_attribute_info_ex", new Object[] { attnames }, new Class[] { String[].class });
    }

    public AttributeInfo[] get_attribute_config(DeviceProxy deviceProxy, String[] attnames) throws DevFailed {
        return (AttributeInfo[]) WebServerClientUtil.getResponse(this, classParam, "get_attribute_config", new Object[] { attnames }, new Class[] { String[].class });
    }

    public AttributeInfo get_attribute_info(DeviceProxy deviceProxy, String attname) throws DevFailed {
        return (AttributeInfo) WebServerClientUtil.getResponse(this, classParam, "get_attribute_info", new Object[] { attname }, new Class[] { String.class });
    }

    public AttributeInfoEx get_attribute_info_ex(DeviceProxy deviceProxy, String attname) throws DevFailed {
        return (AttributeInfoEx) WebServerClientUtil.getResponse(this, classParam, "get_attribute_info_ex", new Object[] { attname }, new Class[] { String.class });
    }

    public AttributeInfo get_attribute_config(DeviceProxy deviceProxy, String attname) throws DevFailed {
        return (AttributeInfo) WebServerClientUtil.getResponse(this, classParam, "get_attribute_config", new Object[] { attname }, new Class[] { String.class });
    }

    public AttributeInfo[] get_attribute_info(DeviceProxy deviceProxy) throws DevFailed {
        return (AttributeInfo[]) WebServerClientUtil.getResponse(this, classParam, "get_attribute_info", new Object[] {}, new Class[] {});
    }

    public AttributeInfoEx[] get_attribute_info_ex(DeviceProxy deviceProxy) throws DevFailed {
        return (AttributeInfoEx[]) WebServerClientUtil.getResponse(this, classParam, "get_attribute_info_ex", new Object[] {}, new Class[] {});
    }

    public AttributeInfo[] get_attribute_config(DeviceProxy deviceProxy) throws DevFailed {
        return (AttributeInfo[]) WebServerClientUtil.getResponse(this, classParam, "get_attribute_config", new Object[] {}, new Class[] {});
    }

    public void set_attribute_info(DeviceProxy deviceProxy, AttributeInfo[] attr) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "set_attribute_info", new Object[] { attr }, new Class[] { AttributeInfo[].class });
    }

    public void set_attribute_info(DeviceProxy deviceProxy, AttributeInfoEx[] attr) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "set_attribute_info", new Object[] { attr }, new Class[] { AttributeInfoEx[].class });
    }

    public void set_attribute_config(DeviceProxy deviceProxy, AttributeInfo[] attr) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "set_attribute_config", new Object[] { attr }, new Class[] { AttributeInfo[].class });
    }

    public DeviceAttribute read_attribute(DeviceProxy deviceProxy, String attname) throws DevFailed {
        return (DeviceAttribute) WebServerClientUtil.getResponse(this, classParam, "read_attribute", new Object[] { attname }, new Class[] { String.class });
    }

    public AttributeValue read_attribute_value(DeviceProxy deviceProxy, String attname) throws DevFailed {
        return (AttributeValue) WebServerClientUtil.getResponse(this, classParam, "read_attribute_value", new Object[] { attname }, new Class[] { String.class });
    }

    public DeviceAttribute[] read_attribute(DeviceProxy deviceProxy, String[] attnames) throws DevFailed {
        return (DeviceAttribute[]) WebServerClientUtil.getResponse(this, classParam, "read_attribute", new Object[] { attnames }, new Class[] { String[].class });
    }

    public void write_attribute(DeviceProxy deviceProxy, DeviceAttribute devattr) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "write_attribute", new Object[] { devattr }, new Class[] { DeviceAttribute.class });
    }

    public void write_attribute(DeviceProxy deviceProxy, DeviceAttribute[] devattr) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "write_attribute", new Object[] { devattr }, new Class[] { DeviceAttribute[].class });
    }

    public DeviceAttribute[] write_read_attribute(DeviceProxy arg0, DeviceAttribute[] devattr) throws DevFailed {
        return (DeviceAttribute[]) WebServerClientUtil.getResponse(this, classParam, "write_read_attribute", new Object[] { devattr }, new Class[] { DeviceAttribute[].class });
    }

    public DeviceProxy get_adm_dev(DeviceProxy deviceProxy) throws DevFailed {
        return (DeviceProxy) WebServerClientUtil.getResponse(this, classParam, "get_adm_dev", new Object[] {}, new Class[] {});
    }

    public void poll_command(DeviceProxy deviceProxy, String cmdname, int period) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "poll_command", new Object[] { cmdname, period }, new Class[] { String.class, int.class });
    }

    public void poll_attribute(DeviceProxy deviceProxy, String attname, int period) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "poll_attribute", new Object[] { attname, period }, new Class[] { String.class, int.class });
    }

    public void stop_poll_command(DeviceProxy deviceProxy, String cmdname) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "stop_poll_command", new Object[] { cmdname }, new Class[] { String.class });
    }

    public void stop_poll_attribute(DeviceProxy deviceProxy, String attname) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "stop_poll_attribute", new Object[] { attname }, new Class[] { String.class });
    }

    public String[] polling_status(DeviceProxy deviceProxy) throws DevFailed {
        return (String[]) WebServerClientUtil.getResponse(this, classParam, "polling_status", new Object[] {}, new Class[] {});
    }

    public DeviceDataHistory[] command_history(DeviceProxy deviceProxy, String cmdname, int nb) throws DevFailed {
        return (DeviceDataHistory[]) WebServerClientUtil.getResponse(this, classParam, "command_history", new Object[] { cmdname, nb }, new Class[] { String.class, int.class });
    }

    public DeviceDataHistory[] attribute_history(DeviceProxy deviceProxy, String attname, int nb) throws DevFailed {
        return (DeviceDataHistory[]) WebServerClientUtil.getResponse(this, classParam, "attribute_history", new Object[] { attname, nb }, new Class[] { String.class, int.class });
    }

    public DeviceDataHistory[] command_history(DeviceProxy deviceProxy, String cmdname) throws DevFailed {
        return (DeviceDataHistory[]) WebServerClientUtil.getResponse(this, classParam, "command_history", new Object[] { cmdname }, new Class[] { String.class });
    }

    public DeviceDataHistory[] attribute_history(DeviceProxy deviceProxy, String attname) throws DevFailed {
        return (DeviceDataHistory[]) WebServerClientUtil.getResponse(this, classParam, "attribute_history", new Object[] { attname }, new Class[] { String.class });
    }

    public int get_attribute_polling_period(DeviceProxy arg0, String attname) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "get_attribute_polling_period", new Object[] { attname }, new Class[] { String.class });
    }

    public int get_command_polling_period(DeviceProxy arg0, String cmdname) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "get_command_polling_period", new Object[] { cmdname }, new Class[] { String.class });
    }

    public int command_inout_asynch(DeviceProxy deviceProxy, String cmdname, DeviceData data_in) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "command_inout_asynch", new Object[] { cmdname, data_in }, new Class[] { String.class, DeviceData.class });
    }

    public int command_inout_asynch(DeviceProxy deviceProxy, String cmdname) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "command_inout_asynch", new Object[] { cmdname }, new Class[] { String.class });
    }

    public int command_inout_asynch(DeviceProxy deviceProxy, String cmdname, boolean forget) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "command_inout_asynch", new Object[] { cmdname, forget }, new Class[] { String.class, boolean.class });
    }

    public int command_inout_asynch(DeviceProxy deviceProxy, String cmdname, DeviceData data_in, boolean forget) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "command_inout_asynch", new Object[] { cmdname, data_in, forget }, new Class[] { String.class, DeviceData.class, boolean.class });
    }

    public void command_inout_asynch(DeviceProxy deviceProxy, String cmdname, DeviceData argin, CallBack cb) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "command_inout_asynch", new Object[] { cmdname, argin, cb }, new Class[] { String.class, DeviceData.class, CallBack.class });
    }

    public void command_inout_asynch(DeviceProxy deviceProxy, String cmdname, CallBack cb) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "command_inout_asynch", new Object[] { cmdname, cb }, new Class[] { String.class, CallBack.class });
    }

    public DeviceData command_inout_reply(DeviceProxy deviceProxy, int id, int timeout) throws DevFailed, AsynReplyNotArrived {
        return (DeviceData) WebServerClientUtil.getResponse(this, classParam, "command_inout_reply", new Object[] { id, timeout }, new Class[] { int.class, int.class });
    }

    public DeviceData command_inout_reply(DeviceProxy deviceProxy, AsyncCallObject aco, int timeout) throws DevFailed, AsynReplyNotArrived {
        DeviceData argout = null;
        int ms_to_sleep = 50;
        AsynReplyNotArrived except = null;
        long t0 = System.currentTimeMillis();
        long t1 = t0;
        while (((t1 - t0) < timeout || timeout == 0) && argout == null) {
            try {
                argout = command_inout_reply(deviceProxy, aco);
            } catch (AsynReplyNotArrived na) {
                except = na;
                this.sleep(ms_to_sleep);
                t1 = System.currentTimeMillis();
            } catch (DevFailed e) {
                throw e;
            }
        }
        if (argout == null && except != null) throw except;
        return argout;
    }

    private synchronized void sleep(long ms) {
        try {
            wait(ms);
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }

    public DeviceData command_inout_reply(DeviceProxy deviceProxy, int id) throws DevFailed, AsynReplyNotArrived {
        return (DeviceData) WebServerClientUtil.getResponse(this, classParam, "command_inout_reply", new Object[] { id }, new Class[] { int.class });
    }

    public DeviceData command_inout_reply(DeviceProxy deviceProxy, AsyncCallObject aco) throws DevFailed, AsynReplyNotArrived {
        DeviceData data = null;
        check_asynch_reply(deviceProxy, aco.request, aco.id, "command_inout");
        Any any = aco.request.return_value().extract_any();
        data = new DeviceData();
        data.any = any;
        ApiUtil.remove_async_request(aco.id);
        return data;
    }

    public int read_attribute_asynch(DeviceProxy deviceProxy, String attname) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "read_attribute_asynch", new Object[] { attname }, new Class[] { String.class });
    }

    public int read_attribute_asynch(DeviceProxy deviceProxy, String[] attnames) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "read_attribute_asynch", new Object[] { attnames }, new Class[] { String[].class });
    }

    public String get_asynch_idl_cmd(DeviceProxy deviceProxy, Request request, String idl_cmd) {
        try {
            return (String) WebServerClientUtil.getResponse(this, classParam, "get_asynch_idl_cmd", new Object[] { request, idl_cmd }, new Class[] { Request.class, String.class });
        } catch (DevFailed e) {
            e.printStackTrace();
            return null;
        }
    }

    public void check_asynch_reply(DeviceProxy deviceProxy, Request request, int id, String idl_cmd) throws DevFailed, AsynReplyNotArrived {
        WebServerClientUtil.getResponse(this, classParam, "check_asynch_reply", new Object[] { request, id, idl_cmd }, new Class[] { Request.class, int.class, String.class });
    }

    public DeviceAttribute[] read_attribute_reply(DeviceProxy deviceProxy, int id, int timeout) throws DevFailed, AsynReplyNotArrived {
        return (DeviceAttribute[]) WebServerClientUtil.getResponse(this, classParam, "read_attribute_reply", new Object[] { id, timeout }, new Class[] { int.class, int.class });
    }

    public DeviceAttribute[] read_attribute_reply(DeviceProxy deviceProxy, int id) throws DevFailed, AsynReplyNotArrived {
        return (DeviceAttribute[]) WebServerClientUtil.getResponse(this, classParam, "read_attribute_reply", new Object[] { id }, new Class[] { int.class });
    }

    public void read_attribute_asynch(DeviceProxy deviceProxy, String attname, CallBack cb) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "read_attribute_asynch", new Object[] { attname, cb }, new Class[] { String.class, CallBack.class });
    }

    public void read_attribute_asynch(DeviceProxy deviceProxy, String[] attnames, CallBack cb) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "read_attribute_asynch", new Object[] { attnames, cb }, new Class[] { String[].class, CallBack.class });
    }

    public int write_attribute_asynch(DeviceProxy deviceProxy, DeviceAttribute attr) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "write_attribute_asynch", new Object[] { attr }, new Class[] { DeviceAttribute.class });
    }

    public int write_attribute_asynch(DeviceProxy deviceProxy, DeviceAttribute attr, boolean forget) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "write_attribute_asynch", new Object[] { attr, forget }, new Class[] { DeviceAttribute.class, boolean.class });
    }

    public int write_attribute_asynch(DeviceProxy deviceProxy, DeviceAttribute[] attribs) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "write_attribute_asynch", new Object[] { attribs }, new Class[] { DeviceAttribute[].class });
    }

    public int write_attribute_asynch(DeviceProxy deviceProxy, DeviceAttribute[] attribs, boolean forget) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "write_attribute_asynch", new Object[] { attribs, forget }, new Class[] { DeviceAttribute[].class, boolean.class });
    }

    public void write_attribute_reply(DeviceProxy deviceProxy, int id) throws DevFailed, AsynReplyNotArrived {
        WebServerClientUtil.getResponse(this, classParam, "write_attribute_reply", new Object[] { id }, new Class[] { int.class });
    }

    public void write_attribute_reply(DeviceProxy deviceProxy, int id, int timeout) throws DevFailed, AsynReplyNotArrived {
        WebServerClientUtil.getResponse(this, classParam, "write_attribute_reply", new Object[] { id, timeout }, new Class[] { int.class, int.class });
    }

    public void write_attribute_asynch(DeviceProxy deviceProxy, DeviceAttribute attr, CallBack cb) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "write_attribute_asynch", new Object[] { attr, cb }, new Class[] { DeviceAttribute.class, CallBack.class });
    }

    public void write_attribute_asynch(DeviceProxy deviceProxy, DeviceAttribute[] attribs, CallBack cb) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "write_attribute_asynch", new Object[] { attribs, cb }, new Class[] { DeviceAttribute[].class, CallBack.class });
    }

    public int pending_asynch_call(DeviceProxy deviceProxy, int reply_model) {
        try {
            return (Integer) WebServerClientUtil.getResponse(this, classParam, "pending_asynch_call", new Object[] { reply_model }, new Class[] { int.class });
        } catch (DevFailed e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void get_asynch_replies(DeviceProxy deviceProxy) {
        try {
            WebServerClientUtil.getResponse(this, classParam, "get_asynch_replies", new Object[] {}, new Class[] {});
        } catch (DevFailed e) {
            e.printStackTrace();
        }
    }

    public void get_asynch_replies(DeviceProxy deviceProxy, int timeout) {
        try {
            WebServerClientUtil.getResponse(this, classParam, "get_asynch_replies", new Object[] { timeout }, new Class[] { int.class });
        } catch (DevFailed e) {
            e.printStackTrace();
        }
    }

    public void add_logging_target(DeviceProxy deviceProxy, String target_type, String target_name) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "add_logging_target", new Object[] { target_type, target_name }, new Class[] { String.class, String.class });
    }

    public void add_logging_target(DeviceProxy deviceProxy, String target) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "add_logging_target", new Object[] { target }, new Class[] { String.class });
    }

    public void remove_logging_target(DeviceProxy deviceProxy, String target_type, String target_name) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "remove_logging_target", new Object[] { target_type, target_name }, new Class[] { String.class, String.class });
    }

    public String[] get_logging_target(DeviceProxy deviceProxy) throws DevFailed {
        return (String[]) WebServerClientUtil.getResponse(this, classParam, "get_logging_target", new Object[] {}, new Class[] {});
    }

    public int get_logging_level(DeviceProxy deviceProxy) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "get_logging_level", new Object[] {}, new Class[] {});
    }

    public void set_logging_level(DeviceProxy deviceProxy, int level) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "set_logging_level", new Object[] { level }, new Class[] { int.class });
    }

    public void lock(DeviceProxy deviceProxy, int validity) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "lock", new Object[] { validity }, new Class[] { int.class });
    }

    public int unlock(DeviceProxy deviceProxy) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "unlock", new Object[] {}, new Class[] {});
    }

    public boolean isLocked(DeviceProxy deviceProxy) throws DevFailed {
        return (Boolean) WebServerClientUtil.getResponse(this, classParam, "isLocked", new Object[] {}, new Class[] {});
    }

    public boolean isLockedByMe(DeviceProxy deviceProxy) throws DevFailed {
        return (Boolean) WebServerClientUtil.getResponse(this, classParam, "isLockedByMe", new Object[] {}, new Class[] {});
    }

    public String getLockerStatus(DeviceProxy deviceProxy) throws DevFailed {
        return (String) WebServerClientUtil.getResponse(this, classParam, "getLockerStatus", new Object[] {}, new Class[] {});
    }

    public String[] dev_inform(DeviceProxy deviceProxy) throws DevFailed {
        return (String[]) WebServerClientUtil.getResponse(this, classParam, "dev_inform", new Object[] {}, new Class[] {});
    }

    public void set_rpc_protocol(DeviceProxy deviceProxy, int mode) throws DevFailed {
        WebServerClientUtil.getResponse(this, classParam, "set_rpc_protocol", new Object[] { mode }, new Class[] { int.class });
    }

    public int get_rpc_protocol(DeviceProxy deviceProxy) throws DevFailed {
        return (Integer) WebServerClientUtil.getResponse(this, classParam, "get_rpc_protocol", new Object[] {}, new Class[] {});
    }

    public void main(String args[]) {
    }

    public int subscribe_event(DeviceProxy deviceProxy, String attr_name, int event, CallBack callback, String[] filters) throws DevFailed {
        System.out.println("First version of TANGO WEB No event manage");
        DevError error = new DevError();
        throw new DevFailed(new DevError[] { error });
    }

    public void unsubscribe_event(DeviceProxy deviceProxy, int event_id) throws DevFailed {
    }

    public Object[] getClassParam() {
        return classParam;
    }

    public void setClassParam(Object[] classParam) {
        this.classParam = classParam;
    }

    public int subscribe_event(DeviceProxy deviceProxy, String attr_name, int event, CallBack callback, String[] filters, boolean stateless) throws DevFailed {
        System.out.println("First version of TANGO WEB No event manage");
        DevError error = new DevError();
        throw new DevFailed(new DevError[] { error });
    }

    public int subscribe_event(DeviceProxy arg0, String arg1, int arg2, int arg3, String[] arg4, boolean arg5) throws DevFailed {
        System.out.println("First version of TANGO WEB No event manage");
        DevError error = new DevError();
        throw new DevFailed(new DevError[] { error });
    }
}
