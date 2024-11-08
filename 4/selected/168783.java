package fr.esrf.TangoApi;

import fr.esrf.TangoApi.events.EventConsumerUtil;
import org.omg.CORBA.Any;
import org.omg.CORBA.NVList;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Request;
import org.omg.CORBA.TypeCode;
import fr.esrf.Tango.AttributeConfig;
import fr.esrf.Tango.AttributeConfig_2;
import fr.esrf.Tango.AttributeConfig_3;
import fr.esrf.Tango.AttributeValue;
import fr.esrf.Tango.AttributeValueListHelper;
import fr.esrf.Tango.AttributeValueList_3Helper;
import fr.esrf.Tango.AttributeValueList_4Helper;
import fr.esrf.Tango.AttributeValue_3;
import fr.esrf.Tango.AttributeValue_4;
import fr.esrf.Tango.ClntIdent;
import fr.esrf.Tango.ClntIdentHelper;
import fr.esrf.Tango.DevAttrHistory;
import fr.esrf.Tango.DevAttrHistory_3;
import fr.esrf.Tango.DevAttrHistory_4;
import fr.esrf.Tango.DevCmdHistory;
import fr.esrf.Tango.DevCmdHistory_4;
import fr.esrf.Tango.DevCmdInfo;
import fr.esrf.Tango.DevCmdInfo_2;
import fr.esrf.Tango.DevError;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevFailedHelper;
import fr.esrf.Tango.DevSource;
import fr.esrf.Tango.DevSourceHelper;
import fr.esrf.Tango.DevState;
import fr.esrf.Tango.DevVarLongStringArray;
import fr.esrf.Tango.DevVarStringArrayHelper;
import fr.esrf.Tango.MultiDevFailed;
import fr.esrf.Tango.MultiDevFailedHelper;
import fr.esrf.TangoDs.Except;
import fr.esrf.TangoDs.NamedDevFailed;
import fr.esrf.TangoDs.NamedDevFailedList;
import fr.esrf.TangoDs.TangoConst;

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
 * DeviceProxy dev = DeviceProxyFactory.get("sys/steppermotor/1"); <Br>
 * try { <Br>
 * <ul>
 * DeviceData data = dev.command_inout("DevStatus"); <Br>
 * status = data.extractString(); <Br>
 * </ul>
 * } <Br>
 * catch (DevFailed e) { <Br>
 * <ul>
 * status = "Unknown status"; <Br>
 * Except.print_exception(e); <Br>
 * </ul>
 * } <Br>
 * </ul></i>
 * 
 * @author verdier
 * @version $Revision: 19124 $
 */
@SuppressWarnings({ "ThrowInsideCatchBlockWhichIgnoresCaughtException", "NestedTryStatement" })
public class DeviceProxyDAODefaultImpl extends ConnectionDAODefaultImpl implements ApiDefs, IDeviceProxyDAO {

    public DeviceProxyDAODefaultImpl() {
    }

    public void init(final DeviceProxy deviceProxy) throws DevFailed {
    }

    public void init(final DeviceProxy deviceProxy, final String devname) throws DevFailed {
        deviceProxy.setFull_class_name("DeviceProxy(" + name(deviceProxy) + ")");
    }

    public void init(final DeviceProxy deviceProxy, final String devname, final boolean check_access) throws DevFailed {
        deviceProxy.setFull_class_name("DeviceProxy(" + name(deviceProxy) + ")");
    }

    public void init(final DeviceProxy deviceProxy, final String devname, final String ior) throws DevFailed {
        deviceProxy.setFull_class_name("DeviceProxy(" + name(deviceProxy) + ")");
    }

    public void init(final DeviceProxy deviceProxy, final String devname, final String host, final String port) throws DevFailed {
        deviceProxy.setFull_class_name("DeviceProxy(" + name(deviceProxy) + ")");
    }

    public boolean use_db(final DeviceProxy deviceProxy) {
        return deviceProxy.url.use_db;
    }

    private void checkIfUseDb(final DeviceProxy deviceProxy, final String origin) throws DevFailed {
        if (!deviceProxy.url.use_db) {
            Except.throw_non_db_exception("Api_NonDatabaseDevice", "Device " + name(deviceProxy) + " do not use database", "DeviceProxy(" + name(deviceProxy) + ")." + origin);
        }
    }

    public Database get_db_obj(final DeviceProxy deviceProxy) throws DevFailed {
        checkIfUseDb(deviceProxy, "get_db_obj()");
        return ApiUtil.get_db_obj(deviceProxy.url.host, deviceProxy.url.strport);
    }

    public void import_admin_device(final DeviceProxy deviceProxy, final DbDevImportInfo info) throws DevFailed {
        checkIfTango(deviceProxy, "import_admin_device()");
        if (deviceProxy.getAdm_dev() == null) {
            if (DeviceProxyFactory.exists(info.name)) {
                deviceProxy.setAdm_dev(DeviceProxyFactory.get(info.name));
            } else {
                deviceProxy.setAdm_dev(new DeviceProxy(info));
            }
        }
    }

    public void import_admin_device(final DeviceProxy deviceProxy, final String origin) throws DevFailed {
        checkIfTango(deviceProxy, origin);
        build_connection(deviceProxy);
        if (deviceProxy.getAdm_dev() == null) {
            deviceProxy.setAdm_dev(DeviceProxyFactory.get(adm_name(deviceProxy)));
        }
    }

    public String name(final DeviceProxy deviceProxy) {
        return get_name(deviceProxy);
    }

    public String status(final DeviceProxy deviceProxy) throws DevFailed {
        return status(deviceProxy, ApiDefs.FROM_ATTR);
    }

    public String status(final DeviceProxy deviceProxy, final boolean src) throws DevFailed {
        build_connection(deviceProxy);
        if (deviceProxy.url.protocol == TANGO) {
            String status = "Unknown";
            final int retries = deviceProxy.transparent_reconnection ? 2 : 1;
            boolean done = false;
            for (int i = 0; i < retries && !done; i++) {
                try {
                    if (src == ApiDefs.FROM_ATTR) {
                        status = deviceProxy.device.status();
                    } else {
                        final DeviceData argout = deviceProxy.command_inout("Status");
                        status = argout.extractString();
                    }
                    done = true;
                } catch (final Exception e) {
                    manageExceptionReconnection(deviceProxy, retries, i, e, this.getClass() + ".status");
                }
            }
            return status;
        } else {
            return command_inout(deviceProxy, "DevStatus").extractString();
        }
    }

    public DevState state(final DeviceProxy deviceProxy) throws DevFailed {
        return state(deviceProxy, ApiDefs.FROM_ATTR);
    }

    public DevState state(final DeviceProxy deviceProxy, final boolean src) throws DevFailed {
        build_connection(deviceProxy);
        if (deviceProxy.url.protocol == TANGO) {
            DevState state = DevState.UNKNOWN;
            final int retries = deviceProxy.transparent_reconnection ? 2 : 1;
            boolean done = false;
            for (int i = 0; i < retries && !done; i++) {
                try {
                    if (src == ApiDefs.FROM_ATTR) {
                        state = deviceProxy.device.state();
                    } else {
                        final DeviceData argout = deviceProxy.command_inout("State");
                        state = argout.extractDevState();
                    }
                    done = true;
                } catch (final Exception e) {
                    manageExceptionReconnection(deviceProxy, retries, i, e, this.getClass() + ".state");
                }
            }
            return state;
        } else {
            final DeviceData argout = command_inout(deviceProxy, "DevState");
            final short state = argout.extractShort();
            return T2Ttypes.tangoState(state);
        }
    }

    public CommandInfo command_query(final DeviceProxy deviceProxy, final String cmdname) throws DevFailed {
        build_connection(deviceProxy);
        CommandInfo info = null;
        if (deviceProxy.url.protocol == TANGO) {
            final int retries = deviceProxy.transparent_reconnection ? 2 : 1;
            for (int i = 0; i < retries; i++) {
                try {
                    if (deviceProxy.device_2 != null) {
                        final DevCmdInfo_2 tmp = deviceProxy.device_2.command_query_2(cmdname);
                        info = new CommandInfo(tmp);
                    } else {
                        final DevCmdInfo tmp = deviceProxy.device.command_query(cmdname);
                        info = new CommandInfo(tmp);
                    }
                } catch (final Exception e) {
                    manageExceptionReconnection(deviceProxy, retries, i, e, this.getClass() + ".command_query");
                }
            }
        } else {
            info = deviceProxy.taco_device.commandQuery(cmdname);
        }
        return info;
    }

    public String get_class(final DeviceProxy deviceProxy) throws DevFailed {
        checkIfUseDb(deviceProxy, "get_class()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "get_class");
        final String classname = super.get_class_name(deviceProxy);
        if (classname != null) {
            return classname;
        } else {
            return deviceProxy.getDb_dev().get_class();
        }
    }

    public String[] get_class_inheritance(final DeviceProxy deviceProxy) throws DevFailed {
        checkIfUseDb(deviceProxy, "get_class_inheritance()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "get_class_inheritance");
        return deviceProxy.getDb_dev().get_class_inheritance();
    }

    public void put_alias(final DeviceProxy deviceProxy, final String aliasname) throws DevFailed {
        checkIfUseDb(deviceProxy, "put_alias()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "put_alias");
        deviceProxy.getDb_dev().put_alias(aliasname);
    }

    public String get_alias(final DeviceProxy deviceProxy) throws DevFailed {
        checkIfUseDb(deviceProxy, "get_alias()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "get_alias");
        return deviceProxy.getDb_dev().get_alias();
    }

    public DeviceInfo get_info(final DeviceProxy deviceProxy) throws DevFailed {
        checkIfUseDb(deviceProxy, "get_info()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        if (deviceProxy.url.protocol == TANGO) {
            return deviceProxy.getDb_dev().get_info();
        } else {
            return null;
        }
    }

    public DbDevImportInfo import_device(final DeviceProxy deviceProxy) throws DevFailed {
        checkIfUseDb(deviceProxy, "import_device()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        if (deviceProxy.url.protocol == TANGO) {
            return deviceProxy.getDb_dev().import_device();
        } else {
            return new DbDevImportInfo(dev_inform(deviceProxy));
        }
    }

    public void export_device(final DeviceProxy deviceProxy, final DbDevExportInfo devinfo) throws DevFailed {
        checkIfTango(deviceProxy, "export_device");
        checkIfUseDb(deviceProxy, "export_device()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        deviceProxy.getDb_dev().export_device(devinfo);
    }

    public void unexport_device(final DeviceProxy deviceProxy) throws DevFailed {
        checkIfTango(deviceProxy, "unexport_device");
        checkIfUseDb(deviceProxy, "unexport_device()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        deviceProxy.getDb_dev().unexport_device();
    }

    public void add_device(final DeviceProxy deviceProxy, final DbDevInfo devinfo) throws DevFailed {
        checkIfTango(deviceProxy, "add_device");
        checkIfUseDb(deviceProxy, "add_device()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        deviceProxy.getDb_dev().add_device(devinfo);
    }

    public void delete_device(final DeviceProxy deviceProxy) throws DevFailed {
        checkIfTango(deviceProxy, "delete_device");
        checkIfUseDb(deviceProxy, "delete_device()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        deviceProxy.getDb_dev().delete_device();
    }

    public String[] get_property_list(final DeviceProxy deviceProxy, final String wildcard) throws DevFailed {
        checkIfTango(deviceProxy, "get_property_list");
        checkIfUseDb(deviceProxy, "get_property_list()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        return deviceProxy.getDb_dev().get_property_list(wildcard);
    }

    public DbDatum[] get_property(final DeviceProxy deviceProxy, final String[] propnames) throws DevFailed {
        if (deviceProxy.url.protocol == TANGO) {
            checkIfUseDb(deviceProxy, "get_property()");
            if (deviceProxy.getDb_dev() == null) {
                deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
            }
            return deviceProxy.getDb_dev().get_property(propnames);
        } else {
            if (deviceProxy.taco_device == null && deviceProxy.devname != null) {
                build_connection(deviceProxy);
            }
            return deviceProxy.taco_device.get_property(propnames);
        }
    }

    public DbDatum get_property(final DeviceProxy deviceProxy, final String propname) throws DevFailed {
        if (deviceProxy.url.protocol == TANGO) {
            checkIfUseDb(deviceProxy, "get_property()");
            if (deviceProxy.getDb_dev() == null) {
                deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
            }
            return deviceProxy.getDb_dev().get_property(propname);
        } else {
            final String[] propnames = { propname };
            return deviceProxy.taco_device.get_property(propnames)[0];
        }
    }

    public DbDatum[] get_property(final DeviceProxy deviceProxy, final DbDatum[] properties) throws DevFailed {
        checkIfUseDb(deviceProxy, "get_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "get_property");
        return deviceProxy.getDb_dev().get_property(properties);
    }

    public void put_property(final DeviceProxy deviceProxy, final DbDatum prop) throws DevFailed {
        checkIfUseDb(deviceProxy, "put_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "put_property");
        final DbDatum[] properties = new DbDatum[1];
        properties[0] = prop;
        put_property(deviceProxy, properties);
    }

    public void put_property(final DeviceProxy deviceProxy, final DbDatum[] properties) throws DevFailed {
        checkIfUseDb(deviceProxy, "put_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "put_property");
        deviceProxy.getDb_dev().put_property(properties);
    }

    public void delete_property(final DeviceProxy deviceProxy, final String[] propnames) throws DevFailed {
        checkIfUseDb(deviceProxy, "delete_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "delete_property");
        deviceProxy.getDb_dev().delete_property(propnames);
    }

    public void delete_property(final DeviceProxy deviceProxy, final String propname) throws DevFailed {
        checkIfUseDb(deviceProxy, "delete_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "delete_property");
        deviceProxy.getDb_dev().delete_property(propname);
    }

    public void delete_property(final DeviceProxy deviceProxy, final DbDatum[] properties) throws DevFailed {
        checkIfUseDb(deviceProxy, "delete_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "delete_property");
        deviceProxy.getDb_dev().delete_property(properties);
    }

    public String[] get_attribute_list(final DeviceProxy deviceProxy) throws DevFailed {
        build_connection(deviceProxy);
        if (deviceProxy.url.protocol == TANGO) {
            final String[] wildcard = new String[1];
            if (deviceProxy.device_3 != null) {
                wildcard[0] = TangoConst.Tango_AllAttr_3;
            } else {
                wildcard[0] = TangoConst.Tango_AllAttr;
            }
            final AttributeInfo[] ac = get_attribute_info(deviceProxy, wildcard);
            final String[] result = new String[ac.length];
            for (int i = 0; i < ac.length; i++) {
                result[i] = ac[i].name;
            }
            return result;
        } else {
            return deviceProxy.taco_device.get_attribute_list();
        }
    }

    public void put_attribute_property(final DeviceProxy deviceProxy, final DbAttribute[] attr) throws DevFailed {
        checkIfUseDb(deviceProxy, "put_attribute_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "put_attribute_property");
        deviceProxy.getDb_dev().put_attribute_property(attr);
    }

    public void put_attribute_property(final DeviceProxy deviceProxy, final DbAttribute attr) throws DevFailed {
        checkIfUseDb(deviceProxy, "put_attribute_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "put_attribute_property");
        deviceProxy.getDb_dev().put_attribute_property(attr);
    }

    public void delete_attribute_property(final DeviceProxy deviceProxy, final String attname, final String[] propnames) throws DevFailed {
        checkIfUseDb(deviceProxy, "delete_attribute_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "delete_attribute_property");
        deviceProxy.getDb_dev().delete_attribute_property(attname, propnames);
    }

    public void delete_attribute_property(final DeviceProxy deviceProxy, final String attname, final String propname) throws DevFailed {
        checkIfUseDb(deviceProxy, "delete_attribute_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "delete_attribute_property");
        deviceProxy.getDb_dev().delete_attribute_property(attname, propname);
    }

    public void delete_attribute_property(final DeviceProxy deviceProxy, final DbAttribute attr) throws DevFailed {
        checkIfUseDb(deviceProxy, "delete_attribute_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "delete_attribute_property");
        deviceProxy.getDb_dev().delete_attribute_property(attr);
    }

    public void delete_attribute_property(final DeviceProxy deviceProxy, final DbAttribute[] attr) throws DevFailed {
        checkIfUseDb(deviceProxy, "delete_attribute_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "delete_attribute_property");
        deviceProxy.getDb_dev().delete_attribute_property(attr);
    }

    public DbAttribute[] get_attribute_property(final DeviceProxy deviceProxy, final String[] attnames) throws DevFailed {
        checkIfUseDb(deviceProxy, "get_attribute_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "get_attribute_property");
        return deviceProxy.getDb_dev().get_attribute_property(attnames);
    }

    public DbAttribute get_attribute_property(final DeviceProxy deviceProxy, final String attname) throws DevFailed {
        checkIfUseDb(deviceProxy, "get_attribute_property()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "get_attribute_property");
        return deviceProxy.getDb_dev().get_attribute_property(attname);
    }

    public void delete_attribute(final DeviceProxy deviceProxy, final String attname) throws DevFailed {
        checkIfUseDb(deviceProxy, "delete_attribute()");
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "delete_attribute");
        deviceProxy.getDb_dev().delete_attribute(attname);
    }

    public AttributeInfo[] get_attribute_info(final DeviceProxy deviceProxy, final String[] attnames) throws DevFailed {
        build_connection(deviceProxy);
        try {
            AttributeInfo[] result;
            AttributeConfig[] ac = new AttributeConfig[0];
            AttributeConfig_2[] ac_2 = null;
            if (deviceProxy.url.protocol == TANGO) {
                if (deviceProxy.device_2 != null) {
                    ac_2 = deviceProxy.device_2.get_attribute_config_2(attnames);
                } else {
                    ac = deviceProxy.device.get_attribute_config(attnames);
                }
            } else {
                ac = deviceProxy.taco_device.get_attribute_config(attnames);
            }
            final int size = ac_2 != null ? ac_2.length : ac.length;
            result = new AttributeInfo[size];
            for (int i = 0; i < size; i++) {
                if (ac_2 != null) {
                    result[i] = new AttributeInfo(ac_2[i]);
                } else {
                    result[i] = new AttributeInfo(ac[i]);
                }
            }
            return result;
        } catch (final DevFailed e) {
            throw e;
        } catch (final Exception e) {
            ApiUtilDAODefaultImpl.removePendingRepliesOfDevice(deviceProxy);
            throw_dev_failed(deviceProxy, e, "get_attribute_config", true);
            return null;
        }
    }

    public AttributeInfoEx[] get_attribute_info_ex(final DeviceProxy deviceProxy, final String[] attnames) throws DevFailed {
        build_connection(deviceProxy);
        AttributeInfoEx[] result = null;
        boolean done = false;
        final int retries = deviceProxy.transparent_reconnection ? 2 : 1;
        for (int i = 0; i < retries && !done; i++) {
            try {
                AttributeConfig_3[] ac_3 = null;
                AttributeConfig_2[] ac_2 = null;
                AttributeConfig[] ac = null;
                if (deviceProxy.url.protocol == TANGO) {
                    if (deviceProxy.device_3 != null) {
                        ac_3 = deviceProxy.device_3.get_attribute_config_3(attnames);
                    } else if (deviceProxy.device_2 != null) {
                        ac_2 = deviceProxy.device_2.get_attribute_config_2(attnames);
                    } else {
                        Except.throw_non_supported_exception("TangoApi_IDL_NOT_SUPPORTED", "Not supported by the IDL version used by device", deviceProxy.getFull_class_name() + ".get_attribute_info_ex()");
                    }
                } else {
                    ac = deviceProxy.taco_device.get_attribute_config(attnames);
                }
                int size;
                if (ac_3 != null) {
                    size = ac_3.length;
                } else if (ac_2 != null) {
                    size = ac_2.length;
                } else {
                    size = ac.length;
                }
                result = new AttributeInfoEx[size];
                for (int n = 0; n < size; n++) {
                    if (ac_3 != null) {
                        result[n] = new AttributeInfoEx(ac_3[n]);
                    } else if (ac_2 != null) {
                        result[n] = new AttributeInfoEx(ac_2[n]);
                    } else if (ac != null) {
                        result[n] = new AttributeInfoEx(ac[n]);
                    }
                }
                done = true;
            } catch (final DevFailed e) {
                throw e;
            } catch (final Exception e) {
                manageExceptionReconnection(deviceProxy, retries, i, e, this.getClass() + ".get_attribute_config_ex");
            }
        }
        return result;
    }

    @Deprecated
    public AttributeInfo[] get_attribute_config(final DeviceProxy deviceProxy, final String[] attnames) throws DevFailed {
        return get_attribute_info(deviceProxy, attnames);
    }

    public AttributeInfo get_attribute_info(final DeviceProxy deviceProxy, final String attname) throws DevFailed {
        final String[] attnames = ApiUtil.toStringArray(attname);
        final AttributeInfo[] ac = get_attribute_info(deviceProxy, attnames);
        return ac[0];
    }

    public AttributeInfoEx get_attribute_info_ex(final DeviceProxy deviceProxy, final String attname) throws DevFailed {
        final String[] attnames = ApiUtil.toStringArray(attname);
        final AttributeInfoEx[] ac = get_attribute_info_ex(deviceProxy, attnames);
        return ac[0];
    }

    @Deprecated
    public AttributeInfo get_attribute_config(final DeviceProxy deviceProxy, final String attname) throws DevFailed {
        return get_attribute_info(deviceProxy, attname);
    }

    public AttributeInfo[] get_attribute_info(final DeviceProxy deviceProxy) throws DevFailed {
        build_connection(deviceProxy);
        final String[] attnames = new String[1];
        if (deviceProxy.device_3 != null) {
            attnames[0] = TangoConst.Tango_AllAttr_3;
        } else {
            attnames[0] = TangoConst.Tango_AllAttr;
        }
        return get_attribute_info(deviceProxy, attnames);
    }

    public AttributeInfoEx[] get_attribute_info_ex(final DeviceProxy deviceProxy) throws DevFailed {
        build_connection(deviceProxy);
        final String[] attnames = new String[1];
        if (deviceProxy.device_3 != null) {
            attnames[0] = TangoConst.Tango_AllAttr_3;
        } else {
            attnames[0] = TangoConst.Tango_AllAttr;
        }
        return get_attribute_info_ex(deviceProxy, attnames);
    }

    @Deprecated
    public AttributeInfo[] get_attribute_config(final DeviceProxy deviceProxy) throws DevFailed {
        return get_attribute_info(deviceProxy);
    }

    public void set_attribute_info(final DeviceProxy deviceProxy, final AttributeInfo[] attr) throws DevFailed {
        checkIfTango(deviceProxy, "set_attribute_config");
        build_connection(deviceProxy);
        try {
            final AttributeConfig[] config = new AttributeConfig[attr.length];
            for (int i = 0; i < attr.length; i++) {
                config[i] = attr[i].get_attribute_config_obj();
            }
            deviceProxy.device.set_attribute_config(config);
        } catch (final DevFailed e) {
            throw e;
        } catch (final Exception e) {
            ApiUtilDAODefaultImpl.removePendingRepliesOfDevice(deviceProxy);
            throw_dev_failed(deviceProxy, e, "set_attribute_info", true);
        }
    }

    public void set_attribute_info(final DeviceProxy deviceProxy, final AttributeInfoEx[] attr) throws DevFailed {
        checkIfTango(deviceProxy, "set_attribute_config");
        build_connection(deviceProxy);
        if (deviceProxy.access == TangoConst.ACCESS_READ) {
            throwNotAuthorizedException(deviceProxy.devname + ".set_attribute_info()", "DeviceProxy.set_attribute_info()");
        }
        try {
            if (deviceProxy.device_4 != null) {
                final AttributeConfig_3[] config = new AttributeConfig_3[attr.length];
                for (int i = 0; i < attr.length; i++) {
                    config[i] = attr[i].get_attribute_config_obj_3();
                }
                deviceProxy.device_4.set_attribute_config_4(config, DevLockManager.getInstance().getClntIdent());
            } else if (deviceProxy.device_3 != null) {
                final AttributeConfig_3[] config = new AttributeConfig_3[attr.length];
                for (int i = 0; i < attr.length; i++) {
                    config[i] = attr[i].get_attribute_config_obj_3();
                }
                deviceProxy.device_3.set_attribute_config_3(config);
            } else {
                final AttributeConfig[] config = new AttributeConfig[attr.length];
                for (int i = 0; i < attr.length; i++) {
                    config[i] = attr[i].get_attribute_config_obj();
                }
                deviceProxy.device.set_attribute_config(config);
            }
        } catch (final DevFailed e) {
            throw e;
        } catch (final Exception e) {
            ApiUtilDAODefaultImpl.removePendingRepliesOfDevice(deviceProxy);
            throw_dev_failed(deviceProxy, e, "set_attribute_info", true);
        }
    }

    @Deprecated
    public void set_attribute_config(final DeviceProxy deviceProxy, final AttributeInfo[] attr) throws DevFailed {
        set_attribute_info(deviceProxy, attr);
    }

    public DeviceAttribute read_attribute(final DeviceProxy deviceProxy, final String attname) throws DevFailed {
        final String[] names = ApiUtil.toStringArray(attname);
        final DeviceAttribute[] attval = read_attribute(deviceProxy, names);
        return attval[0];
    }

    public AttributeValue read_attribute_value(final DeviceProxy deviceProxy, final String attname) throws DevFailed {
        checkIfTango(deviceProxy, "read_attribute_value");
        build_connection(deviceProxy);
        AttributeValue[] attrval;
        if (deviceProxy.getAttnames_array() == null) {
            deviceProxy.setAttnames_array(new String[1]);
        }
        deviceProxy.getAttnames_array()[0] = attname;
        try {
            if (deviceProxy.device_2 != null) {
                attrval = deviceProxy.device_2.read_attributes_2(deviceProxy.getAttnames_array(), deviceProxy.dev_src);
            } else {
                attrval = deviceProxy.device.read_attributes(deviceProxy.getAttnames_array());
            }
            return attrval[0];
        } catch (final DevFailed e) {
            Except.throw_connection_failed(e, "TangoApi_CANNOT_READ_ATTRIBUTE", "Cannot read attribute:   " + attname, deviceProxy.getFull_class_name() + ".read_attribute()");
            return null;
        } catch (final Exception e) {
            ApiUtilDAODefaultImpl.removePendingRepliesOfDevice(deviceProxy);
            throw_dev_failed(deviceProxy, e, "device.read_attributes()", false);
            return null;
        }
    }

    public DeviceAttribute[] read_attribute(final DeviceProxy deviceProxy, final String[] attnames) throws DevFailed {
        DeviceAttribute[] attr;
        build_connection(deviceProxy);
        AttributeValue[] attrval = new AttributeValue[0];
        AttributeValue_3[] attrval_3 = new AttributeValue_3[0];
        AttributeValue_4[] attrval_4 = new AttributeValue_4[0];
        if (deviceProxy.url.protocol == TANGO) {
            boolean done = false;
            final int retries = deviceProxy.transparent_reconnection ? 2 : 1;
            for (int i = 0; i < retries && !done; i++) {
                try {
                    if (deviceProxy.device_4 != null) {
                        attrval_4 = deviceProxy.device_4.read_attributes_4(attnames, deviceProxy.dev_src, DevLockManager.getInstance().getClntIdent());
                    } else if (deviceProxy.device_3 != null) {
                        attrval_3 = deviceProxy.device_3.read_attributes_3(attnames, deviceProxy.dev_src);
                    } else if (deviceProxy.device_2 != null) {
                        attrval = deviceProxy.device_2.read_attributes_2(attnames, deviceProxy.dev_src);
                    } else {
                        attrval = deviceProxy.device.read_attributes(attnames);
                    }
                    done = true;
                } catch (final DevFailed e) {
                    final StringBuffer sb = new StringBuffer(attnames[0]);
                    for (int j = 1; j < attnames.length; j++) {
                        sb.append(", ").append(attnames[j]);
                    }
                    Except.throw_connection_failed(e, "TangoApi_CANNOT_READ_ATTRIBUTE", "Cannot read attribute(s):   " + sb.toString(), deviceProxy.getFull_class_name() + ".read_attribute()");
                } catch (final Exception e) {
                    manageExceptionReconnection(deviceProxy, retries, i, e, this.getClass() + ".read_attribute");
                }
            }
            if (deviceProxy.device_4 != null) {
                attr = new DeviceAttribute[attrval_4.length];
                for (int i = 0; i < attrval_4.length; i++) {
                    attr[i] = new DeviceAttribute(attrval_4[i]);
                }
            } else if (deviceProxy.device_3 != null) {
                attr = new DeviceAttribute[attrval_3.length];
                for (int i = 0; i < attrval_3.length; i++) {
                    attr[i] = new DeviceAttribute(attrval_3[i]);
                }
            } else {
                attr = new DeviceAttribute[attrval.length];
                for (int i = 0; i < attrval.length; i++) {
                    attr[i] = new DeviceAttribute(attrval[i]);
                }
            }
        } else {
            attr = deviceProxy.taco_device.read_attribute(attnames);
        }
        return attr;
    }

    public void write_attribute(final DeviceProxy deviceProxy, final DeviceAttribute devattr) throws DevFailed {
        checkIfTango(deviceProxy, "write_attribute");
        try {
            final DeviceAttribute[] array = { devattr };
            write_attribute(deviceProxy, array);
        } catch (final NamedDevFailedList e) {
            final NamedDevFailed namedDF = e.elementAt(0);
            throw new DevFailed(namedDF.err_stack);
        } catch (final Exception e) {
            throw_dev_failed(deviceProxy, e, "device.write_attributes()", false);
        }
    }

    public void write_attribute(final DeviceProxy deviceProxy, final DeviceAttribute[] devattr) throws DevFailed {
        checkIfTango(deviceProxy, "write_attribute");
        build_connection(deviceProxy);
        if (deviceProxy.access == TangoConst.ACCESS_READ) {
            ping(deviceProxy);
            throwNotAuthorizedException(deviceProxy.devname + ".write_attribute()", "DeviceProxy.write_attribute()");
        }
        AttributeValue_4[] attrval_4 = new AttributeValue_4[0];
        AttributeValue[] attrval = new AttributeValue[0];
        if (deviceProxy.device_4 != null) {
            attrval_4 = new AttributeValue_4[devattr.length];
            for (int i = 0; i < devattr.length; i++) {
                attrval_4[i] = devattr[i].getAttributeValueObject_4();
            }
        } else {
            attrval = new AttributeValue[devattr.length];
            for (int i = 0; i < devattr.length; i++) {
                attrval[i] = devattr[i].getAttributeValueObject_2();
            }
        }
        boolean done = false;
        final int retries = deviceProxy.transparent_reconnection ? 2 : 1;
        for (int i = 0; i < retries && !done; i++) {
            try {
                if (deviceProxy.device_4 != null) {
                    deviceProxy.device_4.write_attributes_4(attrval_4, DevLockManager.getInstance().getClntIdent());
                } else if (deviceProxy.device_3 != null) {
                    deviceProxy.device_3.write_attributes_3(attrval);
                } else {
                    deviceProxy.device.write_attributes(attrval);
                }
                done = true;
            } catch (final DevFailed e) {
                throw e;
            } catch (final MultiDevFailed e) {
                throw new NamedDevFailedList(e, name(deviceProxy), "DeviceProxy.write_attribute", "MultiDevFailed");
            } catch (final Exception e) {
                manageExceptionReconnection(deviceProxy, retries, i, e, this.getClass() + ".DeviceProxy.write_attribute");
            }
        }
    }

    public DeviceAttribute[] write_read_attribute(final DeviceProxy deviceProxy, final DeviceAttribute[] devattr) throws DevFailed {
        checkIfTango(deviceProxy, "write_read_attribute");
        build_connection(deviceProxy);
        if (deviceProxy.access == TangoConst.ACCESS_READ) {
            ping(deviceProxy);
            throwNotAuthorizedException(deviceProxy.devname + ".write_read_attribute()", "DeviceProxy.write_read_attribute()");
        }
        AttributeValue_4[] in_attrval_4 = new AttributeValue_4[0];
        AttributeValue_4[] out_attrval_4 = new AttributeValue_4[0];
        if (deviceProxy.device_4 != null) {
            in_attrval_4 = new AttributeValue_4[devattr.length];
            for (int i = 0; i < devattr.length; i++) {
                in_attrval_4[i] = devattr[i].getAttributeValueObject_4();
            }
        } else {
            Except.throw_connection_failed("TangoApi_READ_ONLY_MODE", "Cannot execute write_read_attribute(), " + deviceProxy.devname + " is not a device_4Impl or above", "DeviceProxy.write_read_attribute()");
        }
        boolean done = false;
        final int retries = deviceProxy.transparent_reconnection ? 2 : 1;
        for (int i = 0; i < retries && !done; i++) {
            try {
                if (deviceProxy.device_4 != null) {
                    out_attrval_4 = deviceProxy.device_4.write_read_attributes_4(in_attrval_4, DevLockManager.getInstance().getClntIdent());
                }
                done = true;
            } catch (final DevFailed e) {
                throw e;
            } catch (final MultiDevFailed e) {
                throw new NamedDevFailedList(e, name(deviceProxy), "DeviceProxy.write_read_attribute", "MultiDevFailed");
            } catch (final Exception e) {
                manageExceptionReconnection(deviceProxy, retries, i, e, this.getClass() + ".write_read_attribute");
            }
        }
        final DeviceAttribute[] attr = new DeviceAttribute[out_attrval_4.length];
        if (deviceProxy.device_4 != null) {
            for (int i = 0; i < out_attrval_4.length; i++) {
                attr[i] = new DeviceAttribute(out_attrval_4[i]);
            }
        }
        return attr;
    }

    public DeviceProxy get_adm_dev(final DeviceProxy deviceProxy) throws DevFailed {
        if (deviceProxy.getAdm_dev() == null) {
            import_admin_device(deviceProxy, "get_adm_dev");
        }
        return deviceProxy.getAdm_dev();
    }

    private void poll_object(final DeviceProxy deviceProxy, final String objname, final String objtype, final int period) throws DevFailed {
        final DevVarLongStringArray lsa = new DevVarLongStringArray();
        lsa.lvalue = new int[1];
        lsa.svalue = new String[3];
        lsa.svalue[0] = deviceProxy.devname;
        lsa.svalue[1] = objtype;
        lsa.svalue[2] = objname.toLowerCase();
        lsa.lvalue[0] = period;
        if (deviceProxy.getAdm_dev() == null) {
            import_admin_device(deviceProxy, "poll_object");
        }
        if (DeviceProxy.isCheck_idl() && deviceProxy.getAdm_dev().get_idl_version() < 2) {
            Except.throw_non_supported_exception("TangoApi_IDL_NOT_SUPPORTED", "Not supported by the IDL version used by device", deviceProxy.getFull_class_name() + ".poll_object()");
        }
        final DeviceData argin = new DeviceData();
        argin.insert(lsa);
        try {
            deviceProxy.getAdm_dev().command_inout("AddObjPolling", argin);
        } catch (final DevFailed e) {
            for (final DevError error : e.errors) {
                if (error.reason.equals("API_AlreadyPolled")) {
                    deviceProxy.getAdm_dev().command_inout("UpdObjPollingPeriod", argin);
                    return;
                }
            }
            Except.throw_communication_failed(e, "TangoApi_CANNOT_POLL_OBJECT", "Cannot poll object " + objname, deviceProxy.getFull_class_name() + ".poll_object()");
        }
    }

    public void poll_command(final DeviceProxy deviceProxy, final String cmdname, final int period) throws DevFailed {
        poll_object(deviceProxy, cmdname, "command", period);
    }

    public void poll_attribute(final DeviceProxy deviceProxy, final String attname, final int period) throws DevFailed {
        poll_object(deviceProxy, attname, "attribute", period);
    }

    private void remove_poll_object(final DeviceProxy deviceProxy, final String objname, final String objtype) throws DevFailed {
        if (deviceProxy.getAdm_dev() == null) {
            import_admin_device(deviceProxy, "remove_poll_object");
        }
        if (DeviceProxy.isCheck_idl() && deviceProxy.getAdm_dev().get_idl_version() < 2) {
            Except.throw_non_supported_exception("TangoApi_IDL_NOT_SUPPORTED", "Not supported by the IDL version used by device", deviceProxy.getFull_class_name() + ".remove_poll_object()");
        }
        final DeviceData argin = new DeviceData();
        final String[] params = new String[3];
        params[0] = deviceProxy.devname;
        params[1] = objtype;
        params[2] = objname.toLowerCase();
        argin.insert(params);
        deviceProxy.getAdm_dev().command_inout("RemObjPolling", argin);
    }

    public void stop_poll_command(final DeviceProxy deviceProxy, final String cmdname) throws DevFailed {
        remove_poll_object(deviceProxy, cmdname, "command");
    }

    public void stop_poll_attribute(final DeviceProxy deviceProxy, final String attname) throws DevFailed {
        remove_poll_object(deviceProxy, attname, "attribute");
    }

    public String[] polling_status(final DeviceProxy deviceProxy) throws DevFailed {
        if (deviceProxy.getAdm_dev() == null) {
            import_admin_device(deviceProxy, "polling_status");
        }
        if (DeviceProxy.isCheck_idl() && deviceProxy.getAdm_dev().get_idl_version() < 2) {
            Except.throw_non_supported_exception("TangoApi_IDL_NOT_SUPPORTED", "Not supported by the IDL version used by device", deviceProxy.getFull_class_name() + ".polling_status()");
        }
        final DeviceData argin = new DeviceData();
        argin.insert(deviceProxy.devname);
        final DeviceData argout = deviceProxy.getAdm_dev().command_inout("DevPollStatus", argin);
        return argout.extractStringArray();
    }

    public DeviceDataHistory[] command_history(final DeviceProxy deviceProxy, final String cmdname, final int nb) throws DevFailed {
        checkIfTango(deviceProxy, "command_history");
        build_connection(deviceProxy);
        if (DeviceProxy.isCheck_idl() && get_idl_version(deviceProxy) < 2) {
            Except.throw_non_supported_exception("TangoApi_IDL_NOT_SUPPORTED", "Not supported by the IDL version used by device", deviceProxy.getFull_class_name() + ".command_history()");
        }
        DeviceDataHistory[] histo = new DeviceDataHistory[0];
        try {
            if (deviceProxy.device_4 != null) {
                final DevCmdHistory_4 cmd_histo = deviceProxy.device_4.command_inout_history_4(cmdname, nb);
                histo = ConversionUtil.histo4ToDeviceDataHistoryArray(cmdname, cmd_histo);
            } else {
                final DevCmdHistory[] cmd_histo = deviceProxy.device_2.command_inout_history_2(cmdname, nb);
                histo = new DeviceDataHistory[cmd_histo.length];
                for (int i = 0; i < cmd_histo.length; i++) {
                    histo[i] = new DeviceDataHistory(cmdname, cmd_histo[i]);
                }
            }
        } catch (final DevFailed e) {
            throw e;
        } catch (final Exception e) {
            throw_dev_failed(deviceProxy, e, "command_inout_history()", false);
        }
        return histo;
    }

    public DeviceDataHistory[] attribute_history(final DeviceProxy deviceProxy, final String attname, final int nb) throws DevFailed {
        checkIfTango(deviceProxy, "attribute_history");
        build_connection(deviceProxy);
        if (DeviceProxy.isCheck_idl() && get_idl_version(deviceProxy) < 2) {
            Except.throw_non_supported_exception("TangoApi_IDL_NOT_SUPPORTED", "Not supported by the IDL version used by device", deviceProxy.getFull_class_name() + ".attribute_history()");
        }
        DeviceDataHistory[] histo = new DeviceDataHistory[0];
        try {
            if (deviceProxy.device_4 != null) {
                final DevAttrHistory_4 att_histo = deviceProxy.device_4.read_attribute_history_4(attname, nb);
                histo = ConversionUtil.histo4ToDeviceDataHistoryArray(att_histo);
            } else if (deviceProxy.device_3 != null) {
                final DevAttrHistory_3[] att_histo = deviceProxy.device_3.read_attribute_history_3(attname, nb);
                histo = new DeviceDataHistory[att_histo.length];
                for (int i = 0; i < att_histo.length; i++) {
                    histo[i] = new DeviceDataHistory(att_histo[i]);
                }
            } else if (deviceProxy.device_2 != null) {
                final DevAttrHistory[] att_histo = deviceProxy.device_2.read_attribute_history_2(attname, nb);
                histo = new DeviceDataHistory[att_histo.length];
                for (int i = 0; i < att_histo.length; i++) {
                    histo[i] = new DeviceDataHistory(att_histo[i]);
                }
            }
        } catch (final DevFailed e) {
            throw e;
        } catch (final Exception e) {
            throw_dev_failed(deviceProxy, e, "read_attribute_history()", false);
        }
        return histo;
    }

    public DeviceDataHistory[] command_history(final DeviceProxy deviceProxy, final String cmdname) throws DevFailed {
        int hist_depth = 10;
        final DbDatum data = get_property(deviceProxy, "poll_ring_depth");
        if (!data.is_empty()) {
            hist_depth = data.extractLong();
        }
        return command_history(deviceProxy, cmdname, hist_depth);
    }

    public DeviceDataHistory[] attribute_history(final DeviceProxy deviceProxy, final String attname) throws DevFailed {
        int hist_depth = 10;
        final DbDatum data = get_property(deviceProxy, "poll_ring_depth");
        if (!data.is_empty()) {
            hist_depth = data.extractLong();
        }
        return attribute_history(deviceProxy, attname, hist_depth);
    }

    public int get_attribute_polling_period(final DeviceProxy deviceProxy, final String attname) throws DevFailed {
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "get_attribute_polling_period");
        return deviceProxy.getDb_dev().get_attribute_polling_period(attname);
    }

    public int get_command_polling_period(final DeviceProxy deviceProxy, final String cmdname) throws DevFailed {
        if (deviceProxy.getDb_dev() == null) {
            deviceProxy.setDb_dev(new DbDevice(deviceProxy.devname, deviceProxy.url.host, deviceProxy.url.strport));
        }
        checkIfTango(deviceProxy, "get_attribute_polling_period");
        return deviceProxy.getDb_dev().get_attribute_polling_period(cmdname);
    }

    public int command_inout_asynch(final DeviceProxy deviceProxy, final String cmdname, final DeviceData data_in) throws DevFailed {
        return command_inout_asynch(deviceProxy, cmdname, data_in, false);
    }

    public int command_inout_asynch(final DeviceProxy deviceProxy, final String cmdname) throws DevFailed {
        return command_inout_asynch(deviceProxy, cmdname, new DeviceData(), false);
    }

    public int command_inout_asynch(final DeviceProxy deviceProxy, final String cmdname, final boolean forget) throws DevFailed {
        return command_inout_asynch(deviceProxy, cmdname, new DeviceData(), forget);
    }

    private void setRequestArgsForCmd(final Request request, final String name, final DeviceData data_in, final DevSource src, final ClntIdent ident) throws DevFailed {
        final ORB orb = ApiUtil.get_orb();
        request.add_in_arg().insert_string(name);
        request.add_in_arg().insert_any(data_in.extractAny());
        if (src != null) {
            final Any any = request.add_in_arg();
            DevSourceHelper.insert(any, src);
        }
        if (ident != null) {
            final Any any = request.add_in_arg();
            ClntIdentHelper.insert(any, ident);
        }
        request.set_return_type(orb.get_primitive_tc(org.omg.CORBA.TCKind.tk_any));
        request.exceptions().add(DevFailedHelper.type());
    }

    public int command_inout_asynch(final DeviceProxy deviceProxy, final String cmdname, final DeviceData data_in, final boolean forget) throws DevFailed {
        checkIfTango(deviceProxy, "command_inout_asynch");
        build_connection(deviceProxy);
        if (deviceProxy.access == TangoConst.ACCESS_READ) {
            final Database db = ApiUtil.get_db_obj(deviceProxy.url.host, deviceProxy.url.strport);
            if (!db.isCommandAllowed(deviceProxy.get_class_name(), cmdname)) {
                if (db.access_devfailed != null) {
                    throw db.access_devfailed;
                }
                ping(deviceProxy);
                System.out.println(deviceProxy.devname + "." + cmdname + "  -> TangoApi_READ_ONLY_MODE");
                throwNotAuthorizedException(deviceProxy.devname + ".command_inout_asynch(" + cmdname + ")", "Connection.command_inout_asynch()");
            }
        }
        Request request;
        if (deviceProxy.device_4 != null) {
            request = deviceProxy.device_4._request("command_inout_4");
            setRequestArgsForCmd(request, cmdname, data_in, get_source(deviceProxy), DevLockManager.getInstance().getClntIdent());
        } else if (deviceProxy.device_2 != null) {
            request = deviceProxy.device_2._request("command_inout_2");
            setRequestArgsForCmd(request, cmdname, data_in, get_source(deviceProxy), null);
        } else {
            request = deviceProxy.device._request("command_inout");
            setRequestArgsForCmd(request, cmdname, data_in, null, null);
        }
        int id = 0;
        boolean done = false;
        final int retries = deviceProxy.transparent_reconnection ? 2 : 1;
        for (int i = 0; i < retries && !done; i++) {
            try {
                if (forget) {
                    request.send_oneway();
                } else {
                    request.send_deferred();
                    final String[] names = new String[1];
                    names[0] = cmdname;
                    id = ApiUtil.put_async_request(new AsyncCallObject(request, deviceProxy, CMD, names));
                }
                done = true;
            } catch (final Exception e) {
                manageExceptionReconnection(deviceProxy, retries, i, e, this.getClass() + ".command_inout");
            }
        }
        return id;
    }

    public void command_inout_asynch(final DeviceProxy deviceProxy, final String cmdname, final DeviceData argin, final CallBack cb) throws DevFailed {
        final int id = command_inout_asynch(deviceProxy, cmdname, argin, false);
        ApiUtil.set_async_reply_model(id, CALLBACK);
        ApiUtil.set_async_reply_cb(id, cb);
        if (ApiUtil.get_asynch_cb_sub_model() == PUSH_CALLBACK) {
            final AsyncCallObject aco = ApiUtil.get_async_object(id);
            new CallbackThread(aco).start();
        }
    }

    public void command_inout_asynch(final DeviceProxy deviceProxy, final String cmdname, final CallBack cb) throws DevFailed {
        command_inout_asynch(deviceProxy, cmdname, new DeviceData(), cb);
    }

    public DeviceData command_inout_reply(final DeviceProxy deviceProxy, final int id, final int timeout) throws DevFailed, AsynReplyNotArrived {
        return command_inout_reply(deviceProxy, ApiUtil.get_async_object(id), timeout);
    }

    public DeviceData command_inout_reply(final DeviceProxy deviceProxy, final AsyncCallObject aco, final int timeout) throws DevFailed, AsynReplyNotArrived {
        DeviceData argout = null;
        final int ms_to_sleep = 50;
        AsynReplyNotArrived except = null;
        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while ((t1 - t0 < timeout || timeout == 0) && argout == null) {
            try {
                argout = command_inout_reply(deviceProxy, aco);
            } catch (final AsynReplyNotArrived na) {
                except = na;
                try {
                    Thread.sleep(ms_to_sleep);
                } catch (final InterruptedException e) {
                }
                t1 = System.currentTimeMillis();
            } catch (final DevFailed e) {
                throw e;
            }
        }
        if (argout == null) {
            ApiUtil.remove_async_request(aco.id);
            throw except;
        }
        return argout;
    }

    public DeviceData command_inout_reply(final DeviceProxy deviceProxy, final int id) throws DevFailed, AsynReplyNotArrived {
        return command_inout_reply(deviceProxy, ApiUtil.get_async_object(id));
    }

    public DeviceData command_inout_reply(final DeviceProxy deviceProxy, final AsyncCallObject aco) throws DevFailed, AsynReplyNotArrived {
        DeviceData data;
        if (deviceProxy.device_4 != null) {
            check_asynch_reply(deviceProxy, aco.request, aco.id, "command_inout_4");
        } else if (deviceProxy.device_2 != null) {
            check_asynch_reply(deviceProxy, aco.request, aco.id, "command_inout_2");
        } else {
            check_asynch_reply(deviceProxy, aco.request, aco.id, "command_inout");
        }
        final Any any = aco.request.return_value().extract_any();
        data = new DeviceData();
        data.insert(any);
        ApiUtil.remove_async_request(aco.id);
        return data;
    }

    private void setRequestArgsForReadAttr(final Request request, final String[] names, final DevSource src, final ClntIdent ident, final TypeCode return_type) {
        Any any;
        any = request.add_in_arg();
        DevVarStringArrayHelper.insert(any, names);
        if (src != null) {
            any = request.add_in_arg();
            DevSourceHelper.insert(any, src);
        }
        if (ident != null) {
            any = request.add_in_arg();
            ClntIdentHelper.insert(any, ident);
        }
        request.set_return_type(return_type);
        request.exceptions().add(DevFailedHelper.type());
    }

    public int read_attribute_asynch(final DeviceProxy deviceProxy, final String attname) throws DevFailed {
        final String[] attnames = new String[1];
        attnames[0] = attname;
        return read_attribute_asynch(deviceProxy, attnames);
    }

    public int read_attribute_asynch(final DeviceProxy deviceProxy, final String[] attnames) throws DevFailed {
        checkIfTango(deviceProxy, "read_attributes_asynch");
        build_connection(deviceProxy);
        Request request;
        if (deviceProxy.device_4 != null) {
            request = deviceProxy.device_4._request("read_attributes_4");
            setRequestArgsForReadAttr(request, attnames, get_source(deviceProxy), DevLockManager.getInstance().getClntIdent(), AttributeValueList_4Helper.type());
            request.exceptions().add(MultiDevFailedHelper.type());
        } else if (deviceProxy.device_3 != null) {
            request = deviceProxy.device_3._request("read_attributes_3");
            setRequestArgsForReadAttr(request, attnames, get_source(deviceProxy), null, AttributeValueList_3Helper.type());
        } else if (deviceProxy.device_2 != null) {
            request = deviceProxy.device_2._request("read_attributes_2");
            setRequestArgsForReadAttr(request, attnames, get_source(deviceProxy), null, AttributeValueListHelper.type());
        } else {
            request = deviceProxy.device._request("read_attributes");
            setRequestArgsForReadAttr(request, attnames, null, null, AttributeValueListHelper.type());
        }
        request.send_deferred();
        return ApiUtil.put_async_request(new AsyncCallObject(request, deviceProxy, ATT_R, attnames));
    }

    public String get_asynch_idl_cmd(final DeviceProxy deviceProxy, final Request request, final String idl_cmd) {
        final NVList args = request.arguments();
        final StringBuffer sb = new StringBuffer();
        try {
            if (idl_cmd.equals("command_inout")) {
                return args.item(0).value().extract_string();
            } else {
                final String[] s_array = DevVarStringArrayHelper.extract(args.item(0).value());
                for (int i = 0; i < s_array.length; i++) {
                    sb.append(s_array[i]);
                    if (i < s_array.length - 1) {
                        sb.append(", ");
                    }
                }
            }
        } catch (final org.omg.CORBA.Bounds e) {
            return "";
        } catch (final Exception e) {
            return "";
        }
        return sb.toString();
    }

    public void check_asynch_reply(final DeviceProxy deviceProxy, final Request request, final int id, final String idl_cmd) throws DevFailed, AsynReplyNotArrived {
        if (request == null) {
            Except.throw_connection_failed("TangoApi_CommandFailed", "Asynchronous call id not found", deviceProxy.getFull_class_name() + "." + idl_cmd + "_reply()");
        } else {
            if (!request.operation().equals(idl_cmd)) {
                Except.throw_connection_failed("TangoApi_CommandFailed", "Asynchronous call id not for " + idl_cmd, deviceProxy.getFull_class_name() + "." + idl_cmd + "_reply()");
            }
            if (!request.poll_response()) {
                Except.throw_asyn_reply_not_arrived("API_AsynReplyNotArrived", "Device " + deviceProxy.devname + ": reply for asynchronous call (id = " + id + ") is not yet arrived", deviceProxy.getFull_class_name() + "." + idl_cmd + "_reply()");
            } else {
                final Exception except = request.env().exception();
                if (except != null) {
                    ApiUtil.remove_async_request(id);
                    if (except instanceof org.omg.CORBA.UnknownUserException) {
                        final Any any = ((org.omg.CORBA.UnknownUserException) except).except;
                        MultiDevFailed ex = null;
                        try {
                            ex = MultiDevFailedHelper.extract(any);
                        } catch (final Exception e) {
                            final DevFailed df = DevFailedHelper.extract(any);
                            Except.throw_connection_failed(df, "TangoApi_CommandFailed", "Asynchronous command failed", deviceProxy.getFull_class_name() + "." + idl_cmd + "_reply(" + get_asynch_idl_cmd(deviceProxy, request, idl_cmd) + ")");
                        }
                        Except.throw_connection_failed(new DevFailed(ex.errors[0].err_list), "TangoApi_CommandFailed", "Asynchronous command failed", deviceProxy.getFull_class_name() + "." + idl_cmd + "_reply(" + get_asynch_idl_cmd(deviceProxy, request, idl_cmd) + ")");
                    } else {
                        except.printStackTrace();
                        System.out.println(deviceProxy.getFull_class_name() + "." + idl_cmd + "_reply(" + get_asynch_idl_cmd(deviceProxy, request, idl_cmd) + ")");
                        throw_dev_failed(deviceProxy, except, deviceProxy.getFull_class_name() + "." + idl_cmd + "_reply(" + get_asynch_idl_cmd(deviceProxy, request, idl_cmd) + ")", false);
                    }
                }
            }
        }
    }

    public DeviceAttribute[] read_attribute_reply(final DeviceProxy deviceProxy, final int id, final int timeout) throws DevFailed, AsynReplyNotArrived {
        DeviceAttribute[] argout = null;
        final int ms_to_sleep = 50;
        AsynReplyNotArrived except = null;
        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while ((t1 - t0 < timeout || timeout == 0) && argout == null) {
            try {
                argout = read_attribute_reply(deviceProxy, id);
            } catch (final AsynReplyNotArrived na) {
                except = na;
                try {
                    Thread.sleep(ms_to_sleep);
                } catch (final InterruptedException e) {
                }
                t1 = System.currentTimeMillis();
            } catch (final DevFailed e) {
                throw e;
            }
        }
        if (argout == null) {
            ApiUtil.remove_async_request(id);
            throw except;
        }
        return argout;
    }

    public DeviceAttribute[] read_attribute_reply(final DeviceProxy deviceProxy, final int id) throws DevFailed, AsynReplyNotArrived {
        DeviceAttribute[] data;
        AttributeValue[] attrval;
        AttributeValue_3[] attrval_3;
        AttributeValue_4[] attrval_4;
        final Request request = ApiUtil.get_async_request(id);
        final Any any = request.return_value();
        if (deviceProxy.device_4 != null) {
            check_asynch_reply(deviceProxy, request, id, "read_attributes_4");
            attrval_4 = AttributeValueList_4Helper.extract(any);
            data = new DeviceAttribute[attrval_4.length];
            for (int i = 0; i < attrval_4.length; i++) {
                data[i] = new DeviceAttribute(attrval_4[i]);
            }
        } else if (deviceProxy.device_3 != null) {
            check_asynch_reply(deviceProxy, request, id, "read_attributes_3");
            attrval_3 = AttributeValueList_3Helper.extract(any);
            data = new DeviceAttribute[attrval_3.length];
            for (int i = 0; i < attrval_3.length; i++) {
                data[i] = new DeviceAttribute(attrval_3[i]);
            }
        } else if (deviceProxy.device_2 != null) {
            check_asynch_reply(deviceProxy, request, id, "read_attributes_2");
            attrval = AttributeValueListHelper.extract(any);
            data = new DeviceAttribute[attrval.length];
            for (int i = 0; i < attrval.length; i++) {
                data[i] = new DeviceAttribute(attrval[i]);
            }
        } else {
            check_asynch_reply(deviceProxy, request, id, "read_attributes");
            attrval = AttributeValueListHelper.extract(any);
            data = new DeviceAttribute[attrval.length];
            for (int i = 0; i < attrval.length; i++) {
                data[i] = new DeviceAttribute(attrval[i]);
            }
        }
        ApiUtil.remove_async_request(id);
        return data;
    }

    public void read_attribute_asynch(final DeviceProxy deviceProxy, final String attname, final CallBack cb) throws DevFailed {
        final String[] attnames = new String[1];
        attnames[0] = attname;
        read_attribute_asynch(deviceProxy, attnames, cb);
    }

    public void read_attribute_asynch(final DeviceProxy deviceProxy, final String[] attnames, final CallBack cb) throws DevFailed {
        final int id = read_attribute_asynch(deviceProxy, attnames);
        ApiUtil.set_async_reply_model(id, CALLBACK);
        ApiUtil.set_async_reply_cb(id, cb);
        if (ApiUtil.get_asynch_cb_sub_model() == PUSH_CALLBACK) {
            final AsyncCallObject aco = ApiUtil.get_async_object(id);
            new CallbackThread(aco).start();
        }
    }

    public int write_attribute_asynch(final DeviceProxy deviceProxy, final DeviceAttribute attr) throws DevFailed {
        return write_attribute_asynch(deviceProxy, attr, false);
    }

    public int write_attribute_asynch(final DeviceProxy deviceProxy, final DeviceAttribute attr, final boolean forget) throws DevFailed {
        final DeviceAttribute[] attribs = new DeviceAttribute[1];
        attribs[0] = attr;
        return write_attribute_asynch(deviceProxy, attribs);
    }

    public int write_attribute_asynch(final DeviceProxy deviceProxy, final DeviceAttribute[] attribs) throws DevFailed {
        return write_attribute_asynch(deviceProxy, attribs, false);
    }

    public int write_attribute_asynch(final DeviceProxy deviceProxy, final DeviceAttribute[] attribs, final boolean forget) throws DevFailed {
        build_connection(deviceProxy);
        if (deviceProxy.access == TangoConst.ACCESS_READ) {
            ping(deviceProxy);
            throwNotAuthorizedException(deviceProxy.devname + ".write_attribute_asynch()", "DeviceProxy.write_attribute_asynch()");
        }
        Request request;
        final String[] attnames = new String[attribs.length];
        Any any;
        if (deviceProxy.device_4 != null) {
            final AttributeValue_4[] attrval_4 = new AttributeValue_4[attribs.length];
            for (int i = 0; i < attribs.length; i++) {
                attrval_4[i] = attribs[i].getAttributeValueObject_4();
                attrval_4[i].err_list = new DevError[0];
                attnames[i] = attrval_4[i].name;
            }
            request = deviceProxy.device_4._request("write_attributes_4");
            any = request.add_in_arg();
            AttributeValueList_4Helper.insert(any, attrval_4);
            any = request.add_in_arg();
            ClntIdentHelper.insert(any, DevLockManager.getInstance().getClntIdent());
            request.exceptions().add(MultiDevFailedHelper.type());
        } else {
            final AttributeValue[] attrval = new AttributeValue[attribs.length];
            for (int i = 0; i < attribs.length; i++) {
                attrval[i] = attribs[i].getAttributeValueObject_2();
                attnames[i] = attrval[i].name;
            }
            if (deviceProxy.device_3 != null) {
                request = deviceProxy.device_3._request("write_attributes_3");
            } else if (deviceProxy.device_2 != null) {
                request = deviceProxy.device_2._request("write_attributes");
            } else {
                request = deviceProxy.device._request("write_attributes");
            }
            any = request.add_in_arg();
            AttributeValueListHelper.insert(any, attrval);
        }
        request.exceptions().add(DevFailedHelper.type());
        int id = 0;
        boolean done = false;
        final int retries = deviceProxy.transparent_reconnection ? 2 : 1;
        for (int i = 0; i < retries && !done; i++) {
            try {
                if (forget) {
                    request.send_oneway();
                } else {
                    request.send_deferred();
                    id = ApiUtil.put_async_request(new AsyncCallObject(request, deviceProxy, ATT_W, attnames));
                }
                done = true;
            } catch (final Exception e) {
                manageExceptionReconnection(deviceProxy, retries, i, e, this.getClass() + ".write_attribute_asynch");
            }
        }
        return id;
    }

    public void write_attribute_reply(final DeviceProxy deviceProxy, final int id) throws DevFailed, AsynReplyNotArrived {
        final Request request = ApiUtil.get_async_request(id);
        if (deviceProxy.device_4 != null) {
            check_asynch_reply(deviceProxy, request, id, "write_attributes_4");
        } else if (deviceProxy.device_3 != null) {
            check_asynch_reply(deviceProxy, request, id, "write_attributes_3");
        } else if (deviceProxy.device_2 != null) {
            check_asynch_reply(deviceProxy, request, id, "write_attributes");
        } else {
            check_asynch_reply(deviceProxy, request, id, "write_attributes");
        }
    }

    public void write_attribute_reply(final DeviceProxy deviceProxy, final int id, final int timeout) throws DevFailed, AsynReplyNotArrived {
        final int ms_to_sleep = 50;
        AsynReplyNotArrived except = null;
        final long t0 = System.currentTimeMillis();
        final long t1 = t0;
        boolean done = false;
        while ((t1 - t0 < timeout || timeout == 0) && !done) {
            try {
                write_attribute_reply(deviceProxy, id);
                done = true;
            } catch (final AsynReplyNotArrived na) {
                except = na;
                try {
                    Thread.sleep(ms_to_sleep);
                } catch (final InterruptedException e) {
                }
            } catch (final DevFailed e) {
                throw e;
            }
        }
        if (!done) {
            throw except;
        }
    }

    public void write_attribute_asynch(final DeviceProxy deviceProxy, final DeviceAttribute attr, final CallBack cb) throws DevFailed {
        final DeviceAttribute[] attribs = new DeviceAttribute[1];
        attribs[0] = attr;
        write_attribute_asynch(deviceProxy, attribs, cb);
    }

    public void write_attribute_asynch(final DeviceProxy deviceProxy, final DeviceAttribute[] attribs, final CallBack cb) throws DevFailed {
        final int id = write_attribute_asynch(deviceProxy, attribs);
        ApiUtil.set_async_reply_model(id, CALLBACK);
        ApiUtil.set_async_reply_cb(id, cb);
        if (ApiUtil.get_asynch_cb_sub_model() == PUSH_CALLBACK) {
            final AsyncCallObject aco = ApiUtil.get_async_object(id);
            new CallbackThread(aco).start();
        }
    }

    public int pending_asynch_call(final DeviceProxy deviceProxy, final int reply_model) {
        return ApiUtil.pending_asynch_call(deviceProxy, reply_model);
    }

    public void get_asynch_replies(final DeviceProxy deviceProxy) {
        ApiUtil.get_asynch_replies(deviceProxy);
    }

    public void get_asynch_replies(final DeviceProxy deviceProxy, final int timeout) {
        ApiUtil.get_asynch_replies(deviceProxy, timeout);
    }

    public void add_logging_target(final DeviceProxy deviceProxy, final String target) throws DevFailed {
        if (deviceProxy.getAdm_dev() == null) {
            import_admin_device(deviceProxy, "add_logging_target");
        }
        final String[] str = new String[2];
        str[0] = get_name(deviceProxy);
        str[1] = target;
        final DeviceData argin = new DeviceData();
        argin.insert(str);
        deviceProxy.getAdm_dev().command_inout("AddLoggingTarget", argin);
    }

    public void remove_logging_target(final DeviceProxy deviceProxy, final String target_type, final String target_name) throws DevFailed {
        if (deviceProxy.getAdm_dev() == null) {
            import_admin_device(deviceProxy, "remove_logging_target");
        }
        final String[] target = new String[2];
        target[0] = get_name(deviceProxy);
        target[1] = target_type + "::" + target_name;
        final DeviceData argin = new DeviceData();
        argin.insert(target);
        deviceProxy.getAdm_dev().command_inout("RemoveLoggingTarget", argin);
    }

    public String[] get_logging_target(final DeviceProxy deviceProxy) throws DevFailed {
        if (deviceProxy.getAdm_dev() == null) {
            import_admin_device(deviceProxy, "get_logging_target");
        }
        final DeviceData argin = new DeviceData();
        argin.insert(get_name(deviceProxy));
        final DeviceData argout = deviceProxy.getAdm_dev().command_inout("GetLoggingTarget", argin);
        return argout.extractStringArray();
    }

    public int get_logging_level(final DeviceProxy deviceProxy) throws DevFailed {
        if (deviceProxy.getAdm_dev() == null) {
            import_admin_device(deviceProxy, "get_logging_level");
        }
        final String[] target = new String[1];
        target[0] = get_name(deviceProxy);
        final DeviceData argin = new DeviceData();
        argin.insert(target);
        final DeviceData argout = deviceProxy.getAdm_dev().command_inout("GetLoggingLevel", argin);
        final DevVarLongStringArray lsa = argout.extractLongStringArray();
        return lsa.lvalue[0];
    }

    public void set_logging_level(final DeviceProxy deviceProxy, final int level) throws DevFailed {
        if (deviceProxy.getAdm_dev() == null) {
            import_admin_device(deviceProxy, "set_logging_level");
        }
        final DevVarLongStringArray lsa = new DevVarLongStringArray();
        lsa.lvalue = new int[1];
        lsa.svalue = new String[1];
        lsa.lvalue[0] = level;
        lsa.svalue[0] = get_name(deviceProxy);
        final DeviceData argin = new DeviceData();
        argin.insert(lsa);
        deviceProxy.getAdm_dev().command_inout("SetLoggingLevel", argin);
    }

    public void lock(final DeviceProxy deviceProxy, final int validity) throws DevFailed {
        DevLockManager.getInstance().lock(deviceProxy, validity);
    }

    public int unlock(final DeviceProxy deviceProxy) throws DevFailed {
        return DevLockManager.getInstance().unlock(deviceProxy);
    }

    public boolean isLocked(final DeviceProxy deviceProxy) throws DevFailed {
        return DevLockManager.getInstance().isLocked(deviceProxy);
    }

    public boolean isLockedByMe(final DeviceProxy deviceProxy) throws DevFailed {
        return DevLockManager.getInstance().isLockedByMe(deviceProxy);
    }

    public String getLockerStatus(final DeviceProxy deviceProxy) throws DevFailed {
        return DevLockManager.getInstance().getLockerStatus(deviceProxy);
    }

    public LockerInfo getLockerInfo(final DeviceProxy deviceProxy) throws DevFailed {
        return DevLockManager.getInstance().getLockerInfo(deviceProxy);
    }

    public String[] dev_inform(final DeviceProxy deviceProxy) throws DevFailed {
        checkIfTaco(deviceProxy, "dev_inform");
        if (deviceProxy.taco_device == null && deviceProxy.devname != null) {
            build_connection(deviceProxy);
        }
        return deviceProxy.taco_device.dev_inform();
    }

    public void set_rpc_protocol(final DeviceProxy deviceProxy, final int mode) throws DevFailed {
        checkIfTaco(deviceProxy, "dev_rpc_protocol");
        build_connection(deviceProxy);
        deviceProxy.taco_device.set_rpc_protocol(mode);
    }

    public int get_rpc_protocol(final DeviceProxy deviceProxy) throws DevFailed {
        checkIfTaco(deviceProxy, "get_rpc_protocol");
        build_connection(deviceProxy);
        return deviceProxy.taco_device.get_rpc_protocol();
    }

    public int subscribe_event(final DeviceProxy deviceProxy, final String attributeName, final int event, final CallBack callback, final String[] filters, final boolean stateless) throws DevFailed {
        int id = 0;
        try {
            id = EventConsumerUtil.getInstance().subscribe_event(deviceProxy, attributeName.toLowerCase(), event, callback, filters, stateless);
        } catch (final DevFailed e) {
            throw e;
        } catch (final Exception e) {
            e.printStackTrace();
            Except.throw_communication_failed(e.toString(), "Subscribe event on " + name(deviceProxy) + "/" + attributeName + " Failed !", "DeviceProxy.subscribe_event()");
        }
        return id;
    }

    public int subscribe_event(final DeviceProxy deviceProxy, final String attributeName, final int event, final int max_size, final String[] filters, final boolean stateless) throws DevFailed {
        int id = 0;
        try {
            id = EventConsumerUtil.getInstance().subscribe_event(deviceProxy, attributeName.toLowerCase(), event, max_size, filters, stateless);
        } catch (final DevFailed e) {
            throw e;
        } catch (final Exception e) {
            Except.throw_communication_failed(e.toString(), "Subscribe event on " + name(deviceProxy) + "/" + attributeName + " Failed !", "DeviceProxy.subscribe_event()");
        }
        return id;
    }

    public void unsubscribe_event(final DeviceProxy deviceProxy, final int event_id) throws DevFailed {
        try {
            EventConsumerUtil.getInstance().unsubscribe_event(event_id);
        } catch (final DevFailed e) {
            throw e;
        } catch (final Exception e) {
            Except.throw_communication_failed(e.toString(), "Unsubsrcibe event on event ID " + event_id + " Failed !", "DeviceProxy.unsubscribe_event()");
        }
    }

    public void main(final String args[]) {
        String devname = null;
        String cmdname = null;
        try {
            cmdname = args[0];
            devname = args[1];
        } catch (final Exception e) {
            if (cmdname == null) {
                System.out.println("Usage :");
                System.out.println("fr.esrf.TangoApi.DeviceProxy  cmdname devname");
                System.out.println("	- cmdname : command name (ping, state, status, unexport...)");
                System.out.println("	- devname : device name to send command.");
            } else {
                System.out.println("Device name ?");
            }
            System.exit(0);
        }
        try {
            String[] devnames;
            DeviceProxy[] dev;
            if (devname.indexOf("*") < 0) {
                devnames = new String[1];
                devnames[0] = devname;
            } else {
                devnames = ApiUtil.get_db_obj().getDevices(devname);
            }
            dev = new DeviceProxy[devnames.length];
            for (int i = 0; i < devnames.length; i++) {
                dev[i] = new DeviceProxy(devnames[i]);
            }
            if (cmdname.equals("ping")) {
                while (true) {
                    for (int i = 0; i < dev.length; i++) {
                        try {
                            final long t = dev[i].ping();
                            System.out.println(devnames[i] + " is alive  (" + t / 1000 + " ms)");
                        } catch (final DevFailed e) {
                            System.out.println(devnames[i] + "  " + e.errors[0].desc);
                        }
                    }
                    if (dev.length > 1) {
                        System.out.println();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                    }
                }
            } else if (cmdname.equals("status")) {
                for (int i = 0; i < dev.length; i++) {
                    try {
                        System.out.println(devnames[i] + " - " + dev[i].status());
                    } catch (final DevFailed e) {
                        System.out.println(devnames[i] + "  " + e.errors[0].desc);
                    }
                }
            } else if (cmdname.equals("state")) {
                for (int i = 0; i < dev.length; i++) {
                    try {
                        System.out.println(devnames[i] + " is " + ApiUtil.stateName(dev[i].state()));
                    } catch (final DevFailed e) {
                        System.out.println(devnames[i] + "  " + e.errors[0].desc);
                    }
                }
            } else if (cmdname.equals("unexport")) {
                for (int i = 0; i < dev.length; i++) {
                    try {
                        dev[i].unexport_device();
                        System.out.println(devnames[i] + " unexported !");
                    } catch (final DevFailed e) {
                        System.out.println(devnames[i] + "  " + e.errors[0].desc);
                    }
                }
            } else {
                System.out.println(cmdname + " ?   Unknow command !");
            }
        } catch (final DevFailed e) {
            Except.print_exception(e);
        }
    }
}
