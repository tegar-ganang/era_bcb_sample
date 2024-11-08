package net.sf.mustang.xbean;

import java.util.Hashtable;
import java.util.Vector;
import net.sf.mustang.K;
import net.sf.mustang.Mustang;
import net.sf.mustang.xbean.driver.XBeanDriver;
import net.sf.mustang.log.KLog;
import net.sf.mustang.service.Request;

public class XTransaction {

    private static KLog log = Mustang.getLog(XTransaction.class);

    private static final String DRIVER_PACKAGE = "net.sf.mustang.xbean.driver.";

    private boolean closed = false;

    private boolean rollBacked = false;

    Vector<XBeanDriver> drivers = new Vector();

    Hashtable<String, XBeanDriver> driversMap = new Hashtable();

    public XBeanDriver getDriver(Request request, DriverInfo driverInfo) throws Exception {
        String driverKey = driverInfo.getClassName();
        if (driverInfo.getChannel() != null) driverKey = driverKey.concat(K.COLON).concat(driverInfo.getChannel());
        XBeanDriver retVal = driversMap.get(driverKey);
        if (retVal == null) {
            try {
                retVal = (XBeanDriver) Class.forName(driverInfo.getClassName()).newInstance();
            } catch (ClassNotFoundException notFound) {
                retVal = (XBeanDriver) Class.forName(DRIVER_PACKAGE.concat(driverInfo.getClassName())).newInstance();
            }
            if (request != null) retVal.open(request, driverInfo.getChannel()); else retVal.open(driverInfo.getChannel());
            driversMap.put(driverKey, retVal);
            drivers.addElement(retVal);
            if (log.isInfo()) log.info("Instatiated XBeanDriver '" + driverKey + "' (" + retVal + ")");
        } else {
            if (log.isInfo()) log.info("Reusing XBeanDriver '" + driverKey + "' (" + retVal + ")");
        }
        return retVal;
    }

    public void undo() {
        if (closed || rollBacked) return;
        for (int i = drivers.size() - 1; i >= 0; i--) {
            drivers.get(i).undo();
            if (log.isInfo()) log.info("XBeanDriver.undo (" + drivers.get(i) + ")");
        }
        rollBacked = true;
    }

    public void close() {
        if (closed) return;
        for (int i = drivers.size() - 1; i >= 0; i--) {
            drivers.get(i).close();
            if (log.isInfo()) log.info("XBeanDriver.close (" + drivers.get(i) + ")");
        }
        closed = true;
    }

    public void commit() throws Exception {
        if (closed || rollBacked) return;
        for (int i = drivers.size() - 1; i >= 0; i--) {
            drivers.get(i).commit();
            if (log.isInfo()) log.info("XBeanDriver.commit (" + drivers.get(i) + ")");
        }
    }
}
