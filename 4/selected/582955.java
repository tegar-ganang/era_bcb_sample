package srcp.server;

import srcp.common.*;
import srcp.common.exception.*;
import java.util.*;
import java.lang.*;

public class DeviceGroupGA extends DeviceGroup {

    protected int intBusNr;

    protected ProcessorInterface objProcessor = null;

    protected String strBusNr;

    public DeviceGroupGA(BusInterface objABus, int intABusNr) {
        super(objABus.getDaemon());
        strGroupIdentifier = Declare.strDevGA;
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

    /** SRCP syntax SET <bus> GA <addr> <port> <value> <delay> */
    public void set(StringTokenizer objStrTok, SessionThread objThread, boolean blnCheck) throws Exception {
        String strNewCmd = "";
        String strAddr = null;
        String strPort = null;
        String strValue = null;
        String strDelay = null;
        try {
            strAddr = objStrTok.nextToken().trim();
            strPort = objStrTok.nextToken().trim();
            strValue = objStrTok.nextToken().trim();
            strDelay = objStrTok.nextToken().trim();
        } catch (Exception e) {
            throw new ExcListToShort();
        }
        int intDummy = -2;
        try {
            intDummy = Integer.parseInt(strAddr);
        } catch (Exception e) {
        }
        if (intDummy <= 0) {
            throw new ExcWrongValue();
        }
        intDummy = -2;
        try {
            intDummy = Integer.parseInt(strPort);
        } catch (Exception e) {
        }
        if (intDummy < 0) {
            throw new ExcWrongValue();
        }
        intDummy = -2;
        try {
            intDummy = Integer.parseInt(strValue);
        } catch (Exception e) {
        }
        if (intDummy < 0) {
            throw new ExcWrongValue();
        }
        intDummy = -2;
        try {
            intDummy = Integer.parseInt(strDelay);
        } catch (Exception e) {
        }
        if ((intDummy < -1) || (intDummy == 0)) {
            throw new ExcWrongValue();
        }
        getProcessor();
        Operand objOperand = new Operand(strBusNr, Declare.strDevGA, strAddr, strPort, strValue, strDelay, objSRCPDaemon.getTimestamp());
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
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

    /** SRCP syntax: GET <bus> GA <addr> <port> */
    public void get(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strNewCmd = "";
        String strAddr = null;
        String strPort = null;
        String strValue = null;
        try {
            strAddr = objStrTok.nextToken();
            strPort = objStrTok.nextToken();
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
        intDummy = -2;
        try {
            intDummy = Integer.parseInt(strPort);
        } catch (Exception e) {
        }
        if (intDummy < 0) {
            throw new ExcWrongValue();
        }
        Operand objOperand = new Operand(strBusNr, Declare.strDevGA, strAddr, strPort, "", "", "");
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
        }
        objOperand = objProcessor.getOperand(objOperand);
        objThread.writeAck(objOperand.getTimestamp(), Declare.intInfoMin, Integer.toString(intBusNr) + " GA " + strAddr + " " + strPort + " " + objOperand.getValue());
    }

    public String info(String strAddr) throws Exception {
        getProcessor();
        return objProcessor.info(strBusNr, "GA", strAddr);
    }

    /** SRCP syntax: INIT <bus> GA <addr> <protocol> [<parameter>.. ] */
    public void init(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strAddr = null;
        String strProto = null;
        try {
            strAddr = objStrTok.nextToken();
            strProto = objStrTok.nextToken();
        } catch (Exception e) {
            throw new ExcListToShort();
        }
        int intAddr = Integer.parseInt(strAddr);
        if (strProto.equals(Declare.strProtocolMaerklinMotorola)) {
            if ((intAddr < 1) || (intAddr > 256)) {
                throw new ExcWrongValue();
            }
        } else {
            if (strProto.equals(Declare.strProtocolNmraDcc)) {
                if ((intAddr < 1) || (intAddr > 511)) {
                    throw new ExcWrongValue();
                }
            } else {
                if (strProto.equals(Declare.strProtocolByServer)) {
                    if (intAddr < 1) {
                        throw new ExcWrongValue();
                    }
                } else {
                    throw new ExcWrongValue();
                }
            }
        }
        getProcessor();
        String strValue = "";
        while (objStrTok.hasMoreElements()) {
            strValue = strValue + objStrTok.nextToken() + " ";
        }
        Operand objOperand = new Operand(strBusNr, Declare.strDevGA, strAddr, "", strProto + " " + strValue, "", objSRCPDaemon.getTimestamp());
        if (objProcessor.isAvailable(objOperand)) {
            throw new ExcForbidden();
        }
        objOperand.setInit();
        objProcessor.init(objOperand);
        objThread.writeAck();
    }

    /** SRCP syntax: TERM <bus> GA <addr> */
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
        Operand objOperand = new Operand(strBusNr, Declare.strDevGA, strAddr, "", "", "", objSRCPDaemon.getTimestamp());
        objOperand.setTerm();
        getProcessor();
        objProcessor.term(objOperand);
        objThread.writeAck();
    }

    /** SRCP syntax: RESET <bus> GA <addr> */
    public void reset(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strAddr = null;
        String strDummy = null;
        try {
            strAddr = objStrTok.nextToken();
            strDummy = objStrTok.nextToken();
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
        String strInfo = info(strAddr);
        Operand objOperand = new Operand(strBusNr, Declare.strDevGA, strAddr, "", "", "", objSRCPDaemon.getTimestamp());
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
        }
        objOperand.setTerm();
        objProcessor.term(objOperand);
        objOperand = new Operand(strBusNr, Declare.strDevGA, strAddr, "", strInfo, "", objSRCPDaemon.getTimestamp());
        objOperand.setInit();
        objProcessor.init(objOperand);
        objThread.writeAck();
    }
}
