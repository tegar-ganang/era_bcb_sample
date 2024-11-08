package srcp.server;

import srcp.common.*;
import srcp.common.exception.*;
import java.util.*;
import java.lang.*;

public class DeviceGroupGL extends DeviceGroup {

    protected int intBusNr;

    protected ProcessorInterface objProcessor = null;

    protected String strBusNr;

    /** set identifier and bus number */
    public DeviceGroupGL(BusInterface objABus, int intABusNr) {
        super(objABus.getDaemon());
        strGroupIdentifier = Declare.strDevGL;
        objBus = objABus;
        intBusNr = intABusNr;
        strBusNr = Integer.toString(intABusNr);
    }

    /** get the processor object instance (is not available at construction time) */
    protected void getProcessor() {
        if (objProcessor != null) {
            return;
        }
        objProcessor = objBus.getDaemon().getProcessor();
    }

    /** SRCP syntax SET <bus> GL <addr> <> <> <> <> <>*/
    public void set(StringTokenizer objStrTok, SessionThread objThread, boolean blnCheck) throws Exception {
        String strNewCmd = "";
        String strAddr = null;
        String strDrvMode = null;
        String strVelo = null;
        String strVeloMax = null;
        String strFunc = null;
        int intDummy = -2;
        int intVelo = 0;
        int intVeloMax = 0;
        try {
            strAddr = objStrTok.nextToken().trim();
            strDrvMode = objStrTok.nextToken().trim();
            strVelo = objStrTok.nextToken().trim();
            strVeloMax = objStrTok.nextToken().trim();
        } catch (Exception e) {
            throw new ExcListToShort();
        }
        if (strAddr.equals(Declare.strAddrBroadcast)) {
        } else {
            intDummy = -2;
            try {
                intDummy = Integer.parseInt(strAddr);
            } catch (Exception e) {
            }
            if (intDummy < 0) {
                throw new ExcWrongValue();
            }
        }
        if (strDrvMode.equals(Declare.strDrvUnchanged)) {
        } else {
            intDummy = -2;
            try {
                intDummy = Integer.parseInt(strDrvMode);
            } catch (Exception e) {
            }
            if ((intDummy < 0) || (intDummy > 2)) {
                throw new ExcWrongValue();
            }
        }
        if (((strVelo.equals(Declare.strDrvUnchanged) && !strVeloMax.equals(Declare.strDrvUnchanged))) || ((strVeloMax.equals(Declare.strDrvUnchanged) && !strVelo.equals(Declare.strDrvUnchanged)))) {
            throw new ExcWrongValue();
        }
        if (!strVelo.equals(Declare.strDrvUnchanged)) {
            intVelo = -2;
            try {
                intVelo = Integer.parseInt(strVelo);
            } catch (Exception e) {
            }
            if (intVelo < 0) {
                throw new ExcWrongValue();
            }
        }
        if (!strVeloMax.equals(Declare.strDrvUnchanged)) {
            intVeloMax = -2;
            try {
                intVeloMax = Integer.parseInt(strVeloMax);
            } catch (Exception e) {
            }
            if (intVeloMax < 0) {
                throw new ExcWrongValue();
            }
        }
        if (intVelo > intVeloMax) {
            throw new ExcWrongValue();
        }
        strFunc = "";
        while (objStrTok.hasMoreElements()) {
            strFunc = strFunc + objStrTok.nextToken() + " ";
        }
        getProcessor();
        Operand objOperand = new Operand(strBusNr, Declare.strDevGL, strAddr, strDrvMode, strVelo, strVeloMax, strFunc, objSRCPDaemon.getTimestamp());
        if (!strAddr.equals(Declare.strAddrBroadcast)) {
            if (!objProcessor.isAvailable(objOperand)) {
                throw new ExcUnsupportedDevice();
            }
        }
        if (!blnCheck) {
            try {
                objProcessor.set(objOperand);
            } catch (Exception e) {
                throw new ExcProcessorDied();
            }
        }
        objThread.writeAck();
    }

    /** SRCP syntax: GET <bus> GL <addr> <port> */
    public void get(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strNewCmd = "";
        String strAddr = null;
        try {
            strAddr = objStrTok.nextToken();
        } catch (Exception e) {
            throw new ExcListToShort();
        }
        getProcessor();
        int intDummy = -2;
        try {
            intDummy = Integer.parseInt(strAddr);
        } catch (Exception e) {
        }
        if (intDummy <= 0) {
            throw new ExcWrongValue();
        }
        Operand objOperand = new Operand(strBusNr, Declare.strDevGL, strAddr, "", "", "", "");
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
        }
        objOperand = objProcessor.getOperand(objOperand);
        objThread.writeAck(objOperand.getTimestamp(), Declare.intInfoMin, Integer.toString(intBusNr) + " GL " + strAddr + " " + objOperand.getPort() + " " + objOperand.getValue() + " " + objOperand.getDelay() + " " + objOperand.getFunc());
    }

    public String info(String strAddr) throws Exception {
        getProcessor();
        return objProcessor.info(strBusNr, Declare.strDevGL, strAddr);
    }

    /** SRCP syntax: INIT <bus> GL <addr> <protocol> [<parameter>.. ] */
    public void init(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strAddr = null;
        String strProto = null;
        try {
            strAddr = objStrTok.nextToken();
            strProto = objStrTok.nextToken();
        } catch (Exception e) {
            throw new ExcListToShort();
        }
        int intAddr = -2;
        try {
            intAddr = Integer.parseInt(strAddr);
        } catch (Exception e) {
        }
        if (intAddr < 1) {
            throw new ExcWrongValue();
        }
        if (!strProto.equals(Declare.strProtocolByServer)) {
            throw new ExcUnsupportedProtocol();
        }
        getProcessor();
        String strValue = "";
        while (objStrTok.hasMoreElements()) {
            strValue = strValue + objStrTok.nextToken() + " ";
        }
        Operand objOperand = new Operand(strBusNr, Declare.strDevGL, strAddr, "", strProto + " " + strValue, "", objSRCPDaemon.getTimestamp());
        if (objProcessor.isAvailable(objOperand)) {
            throw new ExcForbidden();
        }
        objOperand.setInit();
        objProcessor.init(objOperand);
        objThread.writeAck();
    }

    /** SRCP syntax: TERM <bus> GL <addr> */
    public void term(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strAddr = null;
        try {
            strAddr = objStrTok.nextToken();
        } catch (Exception e) {
            throw new ExcListToShort();
        }
        int intAddr = Integer.parseInt(strAddr);
        if (intAddr < 1) {
            throw new ExcWrongValue();
        }
        getProcessor();
        Operand objOperand = new Operand(strBusNr, Declare.strDevGL, strAddr, "", "", "", objSRCPDaemon.getTimestamp());
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
        }
        objOperand.setTerm();
        getProcessor();
        objProcessor.term(objOperand);
        objThread.writeAck();
    }

    /** SRCP syntax: RESET <bus> GL <addr> */
    public void reset(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strAddr = null;
        try {
            strAddr = objStrTok.nextToken();
        } catch (Exception e) {
            throw new ExcListToShort();
        }
        int intAddr = -2;
        try {
            intAddr = Integer.parseInt(strAddr);
        } catch (Exception e) {
        }
        if (intAddr < 1) {
            throw new ExcWrongValue();
        }
        getProcessor();
        Operand objOperand = new Operand(strBusNr, Declare.strDevGL, strAddr, "", "", "", objSRCPDaemon.getTimestamp());
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
        }
        String strInfo = info(strAddr);
        objOperand.setTerm();
        objProcessor.term(objOperand);
        objOperand = new Operand(strBusNr, Declare.strDevGL, strAddr, "", strInfo, "", objSRCPDaemon.getTimestamp());
        objOperand.setInit();
        objProcessor.init(objOperand);
        objThread.writeAck();
    }
}
