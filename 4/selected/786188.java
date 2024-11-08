package srcp.server;

import srcp.common.*;
import srcp.common.exception.*;
import java.util.*;

public class DeviceGroupSession extends DeviceGroup {

    public DeviceGroupSession(BusInterface objABus) {
        super(objABus.getDaemon());
        strGroupIdentifier = Declare.strDevSession;
        objBus = objABus;
    }

    /** SRCP syntax: TERM 0 SESSION [<sessionid>] */
    public void term(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strID = null;
        try {
            strID = objStrTok.nextToken();
        } catch (Exception e) {
            objThread.writeAck();
            objThread.doTerminate();
            return;
        }
        if (objStrTok.hasMoreElements()) {
            throw new ExcListToLong();
        }
        int intSearchId = 0;
        int intID = 0;
        SRCPDaemon objDaemon = objThread.getSRCPDaemon();
        try {
            intID = Integer.parseInt(strID);
            while (objDaemon.getSessionThread(intSearchId).getConnectionId() != intID) {
                intSearchId++;
            }
            objDaemon.getSessionThread(intSearchId).stopThread();
            objThread.writeAck();
        } catch (Exception e) {
            throw new ExcWrongValue();
        }
        ;
    }

    /** return the session info;
	  * SRCP syntax: GET 0 SESSION <sessionid> */
    public void get(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strID;
        SRCPDaemon objDaemon = objThread.getSRCPDaemon();
        int intID;
        String strOutBuffer = null;
        String strRetBuffer;
        try {
            strID = objStrTok.nextToken();
        } catch (Exception e) {
            throw new ExcListToShort();
        }
        if (objStrTok.hasMoreElements()) {
            throw new ExcListToLong();
        }
        int intSearchId = 0;
        try {
            intID = Integer.parseInt(strID);
            while (objDaemon.getSessionThread(intSearchId).getConnectionId() != intID) {
                intSearchId++;
            }
            strOutBuffer = objDaemon.getSessionThread(intSearchId).getInfo();
            objThread.writeAck(Declare.intInfoMin, strOutBuffer);
        } catch (Exception e) {
            throw new ExcWrongValue();
        }
    }
}

;
