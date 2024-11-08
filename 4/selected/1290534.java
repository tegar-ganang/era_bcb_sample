package srcp.server;

import srcp.common.*;
import srcp.common.exception.*;
import java.util.*;

public class DeviceGroupServer extends DeviceGroup {

    /** build a "SERVER" device group for BUS 0 */
    public DeviceGroupServer(BusInterface objABus) {
        super(objABus.getDaemon());
        strGroupIdentifier = Declare.strDevServer;
        objBus = objABus;
    }

    /** terminate the whole SRCP server including all sessions */
    public void term(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        if (objStrTok.hasMoreElements()) {
            throw new ExcListToLong();
        }
        objThread.writeAck();
        objThread.getSRCPDaemon().stopServer();
    }

    /** return server state */
    public void get(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        if (objStrTok.hasMoreElements()) {
            throw new ExcListToLong();
        }
        String strState = "";
        switch(objThread.getSRCPDaemon().getServerState()) {
            case SRCPDaemon.intStateRunning:
                strState = Declare.strStateRunning;
                break;
            case SRCPDaemon.intStateTerminating:
                strState = Declare.strStateTerminating;
                break;
            case SRCPDaemon.intStateResetting:
                strState = Declare.strStateResetting;
                break;
        }
        objThread.writeAck(Declare.intInfoMin, strGroupIdentifier + " " + strState);
    }

    /** perform a reset on the whole SRCP server */
    public void reset(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        if (objStrTok.hasMoreElements()) {
            throw new ExcListToLong();
        }
        objThread.getSRCPDaemon().setState(SRCPDaemon.intStateResetting);
        objThread.writeAck();
    }
}

;
