package srcp.server;

import srcp.common.*;
import srcp.common.exception.*;
import java.util.*;
import java.lang.*;

public class DeviceGroupFB extends DeviceGroup {

    protected int intBusNr;

    protected ProcessorInterface objProcessor = null;

    protected String strBusNr;

    public DeviceGroupFB(BusInterface objABus, int intABusNr) {
        super(objABus.getDaemon());
        strGroupIdentifier = Declare.strDevFB;
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

    public void get(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strNewCmd = "";
        String strAddr = null;
        String strValue = null;
        try {
            strAddr = objStrTok.nextToken();
        } catch (Exception e) {
            throw new ExcListToShort();
        }
        if (objStrTok.hasMoreElements()) {
            throw new ExcListToLong();
        }
        getProcessor();
        int intAddr = -2;
        try {
            intAddr = Integer.parseInt(strAddr);
        } catch (Exception e) {
        }
        if (intAddr <= 0) {
            throw new ExcWrongValue();
        }
        Operand objOperand = new Operand(strBusNr, Declare.strDevFB, strAddr, "", "", "", "");
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
        }
        strValue = objProcessor.get(objOperand);
        objThread.writeAck(Declare.intInfoMin, Integer.toString(intBusNr) + " FB " + strAddr + " " + strValue);
    }

    public String info(String strAddr) throws Exception {
        getProcessor();
        return objProcessor.info(strBusNr, Declare.strDevFB, strAddr);
    }

    /** SRCP syntax: INIT <bus> FB [<parameter>.. ] */
    public void init(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        getProcessor();
        String strValue = "";
        while (objStrTok.hasMoreElements()) {
            strValue = strValue + objStrTok.nextToken() + " ";
        }
        Operand objOperand = new Operand(strBusNr, Declare.strDevFB, "1", "", strValue, "", objSRCPDaemon.getTimestamp());
        if (objProcessor.isAvailable(objOperand)) {
            throw new ExcForbidden();
        }
        objOperand.setInit();
        objProcessor.init(objOperand);
        objThread.writeAck();
    }

    /** SRCP Syntax: TERM <bus> FB */
    public void term(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        if (objStrTok.hasMoreElements()) {
            throw new ExcListToLong();
        }
        getProcessor();
        Operand objOperand = new Operand(strBusNr, Declare.strDevFB, "1", "", "", "", objSRCPDaemon.getTimestamp());
        objOperand.setTerm();
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
        }
        objProcessor.term(objOperand);
        objThread.writeAck();
    }

    /** SRCP syntax: RESET <bus> FB */
    public void reset(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        if (objStrTok.hasMoreElements()) {
            throw new ExcListToLong();
        }
        String strInfo = info("0");
        Operand objOperand = new Operand(strBusNr, Declare.strDevFB, "1", "", "", "", objSRCPDaemon.getTimestamp());
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
        }
        objOperand.setTerm();
        objProcessor.term(objOperand);
        objOperand = new Operand(strBusNr, Declare.strDevFB, "1", "", strInfo, "", objSRCPDaemon.getTimestamp());
        objOperand.setInit();
        objProcessor.init(objOperand);
        objThread.writeAck();
    }

    /** SRCP syntax: WAIT <bus> FB <addr> <value> <timeout>*/
    public void wait(StringTokenizer objStrTok, SessionThread objThread) throws Exception {
        String strAddr = null;
        String strValue = null;
        String strTimeout = null;
        try {
            strAddr = objStrTok.nextToken();
            strValue = objStrTok.nextToken();
            strTimeout = objStrTok.nextToken();
        } catch (Exception e) {
            throw new ExcListToShort();
        }
        if (objStrTok.hasMoreElements()) {
            throw new ExcListToLong();
        }
        if ((!strValue.equals("1")) && (!strValue.equals("0"))) {
            throw new ExcWrongValue();
        }
        int intDummy = -2;
        try {
            intDummy = Integer.parseInt(strTimeout);
        } catch (Exception e) {
        }
        if (intDummy <= 0) {
            throw new ExcWrongValue();
        }
        Operand objOperand = new Operand(strBusNr, Declare.strDevFB, strAddr, "", strValue, strTimeout, objSRCPDaemon.getTimestamp());
        getProcessor();
        if (!objProcessor.isAvailable(objOperand)) {
            throw new ExcUnsupportedDevice();
        }
        objProcessor.wait(objOperand);
        objThread.writeAck();
    }
}

;
