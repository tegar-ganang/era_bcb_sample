package srcp.server;

import srcp.common.*;
import srcp.common.exception.*;
import java.util.*;

public class DeviceGroupDescription extends DeviceGroup {

    /** constructor set the identifier, stores the reference to the
	  * parent bus */
    public DeviceGroupDescription(BusInterface objABus) {
        super(objABus.getDaemon());
        strGroupIdentifier = Declare.strDevDescription;
        objBus = objABus;
    }

    /** handle the get description info for bus and device groups */
    public void get(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strDevGrp = null;
        String strDevAddr = null;
        try {
            strDevGrp = objStrTok.nextToken().trim();
        } catch (Exception e) {
            getBus(objThread);
            return;
        }
        if (strDevGrp.length() < 1) {
            getBus(objThread);
            return;
        }
        try {
            strDevAddr = objStrTok.nextToken().trim();
        } catch (Exception e) {
            strDevAddr = "";
        }
        if (objStrTok.hasMoreElements()) {
            throw new ExcListToLong();
        }
        if ((strDevGrp.equals(Declare.strDevPower) || (strDevGrp.equals(Declare.strDevFB)))) {
            if (strDevAddr.length() > 0) {
                throw new ExcListToLong();
            }
            strDevAddr = "1";
        } else {
            if (strDevAddr.length() < 1) {
                throw new ExcListToShort();
            }
        }
        int intAddr = -2;
        try {
            intAddr = Integer.parseInt(strDevAddr);
        } catch (Exception e) {
        }
        if (intAddr < 1) {
            throw new ExcWrongValue();
        }
        getInit(objThread, strDevGrp, strDevAddr);
    }

    /** return the bus description info */
    protected void getBus(SessionThread objThread) throws Exception {
        String strOutBuffer = "";
        String strIdBuffer = "";
        int intCount = objBus.getGroupCount();
        int intIndex;
        strOutBuffer = Integer.toString(objBus.getId()) + " " + Declare.strDevDescription + " ";
        for (intIndex = 0; intIndex < intCount; intIndex++) {
            strIdBuffer = objBus.getGroup(intIndex).getIdentifier();
            if (!strIdBuffer.equals(Declare.strDevDescription)) {
                strOutBuffer += strIdBuffer + " ";
            }
        }
        objThread.writeAck(Declare.intInfoMin, strOutBuffer);
    }

    /** return the device init info */
    protected void getInit(SessionThread objThread, String strDevGrp, String strDevAddr) throws Exception {
        DeviceGroupInterface objDgi = objBus.getGroup(strDevGrp);
        String strOutBuffer = Integer.toString(objBus.getId()) + " " + Declare.strDevDescription + " " + strDevGrp + " ";
        strOutBuffer += objDgi.info(strDevAddr);
        objThread.writeAck(Declare.intInfoMin, strOutBuffer);
    }
}

;
