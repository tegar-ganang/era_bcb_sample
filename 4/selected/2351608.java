package srcp.server;

import java.net.*;
import java.io.*;
import java.lang.*;
import srcp.common.*;
import java.util.*;
import srcp.common.exception.*;

/** Handle the input and output for a SRCP client session. Every client has its
 * own SessionThread instance. The sessions are managed by SRCPDaemon.
 * Command input is collected until the SRCP command delimiter "LF" is reached.
 * Depending on the working mode (handshake, command, info), the input is processed
 * This class also has method for writing SRCP compliant messages back to the
 * client.
 *
 * @author  osc
 * @version $Revision: 1221 $
 */
public class SessionThread extends java.lang.Thread implements Runnable {

    protected static int intConnectionCounter = 0;

    protected static Object objConnectionCounterMutex;

    protected Socket objCommSocket = null;

    protected OutputStream objOStream = null;

    protected OutputStreamWriter objOStreamWriter = null;

    protected InputStream objIStream = null;

    protected InputStreamReader objIStreamReader = null;

    protected int intConnectionId;

    protected boolean blnShouldTerminate = false;

    public static final int intModeHandshake = 1;

    public static final int intModeCommand = 2;

    public static final int intModeInfo = 3;

    protected int intServerMode;

    protected int intNextServerMode;

    protected char[] achrReceiveBuffer;

    protected byte[] abytReceiveBuffer;

    protected StringTokenizer objStringTok;

    protected SRCPDaemon objSRCPDaemon;

    protected Vector ThreadList;

    protected CommandDispatcher objCmdDisp;

    protected static final int intTrace = 0;

    public static final String strLineTerm = "\r\n";

    protected Eventlog objEventlog;

    public SRCPDaemon getSRCPDaemon() {
        return objSRCPDaemon;
    }

    /** set flag for terminating this session */
    public void doTerminate() {
        blnShouldTerminate = true;
    }

    /** Creates new PortThread */
    public SessionThread(Socket objSocket, SRCPDaemon objADaemon) {
        super();
        objSRCPDaemon = objADaemon;
        objEventlog = objADaemon.getEventlog();
        objCommSocket = objSocket;
        synchronized (objConnectionCounterMutex) {
            if (intConnectionCounter < 0) {
                intConnectionId = 0;
                objEventlog.printError("SessionThread.SessionThread: out of resources");
            } else {
                intConnectionId = ++intConnectionCounter;
            }
        }
        intServerMode = intModeHandshake;
        intNextServerMode = intModeCommand;
        objCmdDisp = objADaemon.getCmdDisp();
    }

    /** Thread working loop. produce welcome message, then do command handling.
	  * the command handling begins in handshake mode */
    public void run() {
        try {
            objOStream = objCommSocket.getOutputStream();
            objOStreamWriter = new OutputStreamWriter(objOStream);
            objIStream = objCommSocket.getInputStream();
            objIStreamReader = new InputStreamReader(objIStream);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            objEventlog.println("SessionThread.run: " + e.getMessage());
        }
        try {
            writeWelcome();
        } catch (SRCPException e) {
            try {
                writeException(e);
                objCommSocket.close();
            } catch (Exception ee) {
                objEventlog.println("SessionThread.run: " + ee.getMessage(), 1);
            }
            return;
        } catch (Exception ee) {
            objEventlog.println("SessionThread.run: " + ee.getMessage(), 1);
            return;
        }
        while (!blnShouldTerminate) {
            try {
                readCommand();
            } catch (SRCPException se) {
                try {
                    writeException(se);
                } catch (Exception see) {
                    blnShouldTerminate = true;
                }
            } catch (Exception e) {
                blnShouldTerminate = true;
            }
        }
        try {
            objCommSocket.close();
        } catch (Exception e) {
            objEventlog.printError("SessionThread.run: " + e.getMessage());
        }
        objSRCPDaemon.removeThread(this);
        objEventlog.println("session " + Integer.toString(intConnectionId) + " done");
    }

    /** called by the daemon to stop this thread. uses the
	  * "interrupt" method to get the thread out of a blocking read operation. */
    public void stopThread() {
        blnShouldTerminate = true;
        interrupt();
    }

    /** create a well-formated header (timestamp, message code, message type) */
    public String getAckHeader(int intCode) {
        String strBuffer = objSRCPDaemon.getTimestamp() + " " + Integer.toString(intCode) + " ";
        if ((intCode >= Declare.intInfoMin) && (intCode <= Declare.intInfoMax)) {
            strBuffer += "INFO ";
        }
        if ((intCode >= Declare.intAckMin) && (intCode <= Declare.intAckMax)) {
            strBuffer += "OK ";
        }
        if ((intCode >= Declare.intCmdErrMin) && (intCode <= Declare.intCmdErrMax)) {
            strBuffer += "ERROR ";
        }
        if ((intCode >= Declare.intSrvErrMin) && (intCode <= Declare.intSrvErrMax)) {
            strBuffer += "ERROR ";
        }
        return strBuffer;
    }

    /** create a well-formated header (timestamp, message code, message type) */
    public String getAckHeader(String strTimestamp, int intCode) {
        String strBuffer = strTimestamp + " " + Integer.toString(intCode) + " ";
        if ((intCode >= Declare.intInfoMin) && (intCode <= Declare.intInfoMax)) {
            strBuffer += "INFO ";
        }
        if ((intCode >= Declare.intAckMin) && (intCode <= Declare.intAckMax)) {
            strBuffer += "OK ";
        }
        if ((intCode >= Declare.intCmdErrMin) && (intCode <= Declare.intCmdErrMax)) {
            strBuffer += "ERROR ";
        }
        if ((intCode >= Declare.intSrvErrMin) && (intCode <= Declare.intSrvErrMax)) {
            strBuffer += "ERROR ";
        }
        return strBuffer;
    }

    /** write a well-formatted acknowledge to the output socket */
    public void writeAck(int intCode, String strMessage) throws Exception {
        try {
            String strBuffer = getAckHeader(intCode);
            strBuffer += strMessage + strLineTerm;
            char achrBuffer[] = strBuffer.toCharArray();
            objOStreamWriter.write(achrBuffer);
            objOStreamWriter.flush();
        } catch (Exception e) {
            objEventlog.printError("SessionThread.writeAck: " + e.getMessage());
            throw e;
        }
    }

    /** write the default OK acknowledge to the socket */
    public void writeAck() throws Exception {
        writeAck(200, "");
    }

    /** write a well-formatted acknowledge to the output socket */
    public void writeAck(String strTimestamp, int intCode, String strMessage) throws Exception {
        try {
            String strBuffer = getAckHeader(strTimestamp, intCode);
            strBuffer += strMessage + strLineTerm;
            char achrBuffer[] = strBuffer.toCharArray();
            objOStreamWriter.write(achrBuffer);
            objOStreamWriter.flush();
        } catch (Exception e) {
            objEventlog.printError("SessionThread.writeAck: " + e.getMessage());
            throw e;
        }
    }

    /** write the server welcome message to the socket */
    protected void writeWelcome() throws Exception {
        if (intConnectionId == 0) {
            throw new ExcOutOfResources();
        }
        objEventlog.println("session " + Integer.toString(intConnectionId) + " welcome " + objCommSocket.getInetAddress().getHostAddress());
        String strBuffer;
        strBuffer = "SERVER " + objSRCPDaemon.getProcessor().getProcessorName() + "; ";
        strBuffer += "SRCP " + Declare.strSRCPVersion;
        strBuffer += "; HOST " + objCommSocket.getLocalAddress().getHostName() + ";" + strLineTerm;
        try {
            char achrBuffer[] = strBuffer.toCharArray();
            objOStreamWriter.write(achrBuffer);
            objOStreamWriter.flush();
        } catch (Exception e) {
            objEventlog.printError("SessionThread.writeWelcome: " + e.getMessage());
        }
    }

    /** wait for command data to arrive on the socket. dependig on the session mode
	  * the command is processed (handshake, command, info)
	  * Input data is written to a char array and converted to string later
	  * @deprecated fails with fragmented input data
	  * @see readCommand()
	  */
    protected void readCommandChar() throws java.lang.Exception {
        String strReceiveBuffer = "";
        int intByteCount;
        boolean blnLineComplete = false;
        while (!blnLineComplete) {
            achrReceiveBuffer = new char[Declare.intMaxLineLength + 24];
            intByteCount = objIStreamReader.read(achrReceiveBuffer);
            if (intByteCount < 1) {
                throw new IOException();
            }
            achrReceiveBuffer[intByteCount] = 0;
            if (achrReceiveBuffer[intByteCount - 1] == Declare.chrLineFeed) {
                blnLineComplete = true;
            } else {
                int intParse;
                for (intParse = 0; intParse < intByteCount - 1; intParse++) {
                    if (achrReceiveBuffer[intParse] == Declare.chrLineFeed) {
                        blnLineComplete = true;
                        achrReceiveBuffer[intParse + 1] = 0;
                        break;
                    }
                }
            }
            strReceiveBuffer += new String(achrReceiveBuffer);
            objEventlog.println("SessionThread buffer: '" + strReceiveBuffer + "'", 2);
        }
        objEventlog.println("SessionThread trimmed: '" + strReceiveBuffer.trim() + "'", 2);
        objStringTok = new StringTokenizer(strReceiveBuffer.trim());
        switch(intServerMode) {
            case intModeHandshake:
                processHandshake();
                break;
            case intModeCommand:
                objCmdDisp.doCommand(objStringTok, this);
                break;
            case intModeInfo:
                break;
        }
    }

    /** wait for command data to arrive on the socket. dependig on the session mode
	  * the command is processed (handshake, command, info)
	  * Input data is written to a byte array an converted to string later
	  * @deprecated fails with fragmented input data,
	  * @see readCommand()
	  */
    protected void readCommandByte() throws java.lang.Exception {
        String strReceiveBuffer = "";
        int intByteCount;
        boolean blnLineComplete = false;
        while (!blnLineComplete) {
            abytReceiveBuffer = new byte[Declare.intMaxLineLength + 24];
            intByteCount = objIStream.read(abytReceiveBuffer);
            if (intByteCount < 1) {
                throw new IOException();
            }
            abytReceiveBuffer[intByteCount] = 0;
            if (abytReceiveBuffer[intByteCount - 1] == Declare.chrLineFeed) {
                blnLineComplete = true;
            } else {
                int intParse;
                for (intParse = 0; intParse < intByteCount - 1; intParse++) {
                    if (abytReceiveBuffer[intParse] == Declare.chrLineFeed) {
                        blnLineComplete = true;
                        abytReceiveBuffer[intParse + 1] = 0;
                        break;
                    }
                }
            }
            strReceiveBuffer += new String(abytReceiveBuffer);
            if (intTrace > 0) {
                System.out.println("SessionThread buffer: '" + strReceiveBuffer + "'");
            }
        }
        if (intTrace > 0) {
            System.out.println("SessionThread trimmed: '" + strReceiveBuffer.trim() + "'");
        }
        objStringTok = new StringTokenizer(strReceiveBuffer.trim());
        switch(intServerMode) {
            case intModeHandshake:
                processHandshake();
                break;
            case intModeCommand:
                objCmdDisp.doCommand(objStringTok, this);
                break;
            case intModeInfo:
                break;
        }
    }

    /** wait for command data to arrive on the socket. dependig on the session mode
	  * the command is processed (handshake, command, info)
	  * Input data is read byte by byte and converted to string byte by byte
	  */
    protected void readCommand() throws java.lang.Exception {
        String strReceiveBuffer = "";
        int intByteCount;
        boolean blnLineComplete = false;
        int intInput;
        while (!blnLineComplete) {
            intInput = objIStream.read();
            if (intTrace > 2) {
                System.out.println("in: " + Integer.toString(intInput));
            }
            if (intInput < 0) {
                throw new IOException();
            }
            if (intInput == Declare.chrLineFeed) {
                blnLineComplete = true;
            }
            if (intInput > 0) {
                abytReceiveBuffer = new byte[1];
                abytReceiveBuffer[0] = (byte) intInput;
                strReceiveBuffer += new String(abytReceiveBuffer);
                if (intTrace > 1) {
                    System.out.println("SessionThread buffer: '" + strReceiveBuffer + "'");
                }
            }
        }
        if (intTrace > 0) {
            System.out.println("SessionThread trimmed: '" + strReceiveBuffer.trim() + "'");
        }
        objStringTok = new StringTokenizer(strReceiveBuffer.trim());
        switch(intServerMode) {
            case intModeHandshake:
                processHandshake();
                break;
            case intModeCommand:
                objCmdDisp.doCommand(objStringTok, this);
                break;
            case intModeInfo:
                break;
        }
    }

    /** process the handshake phase of a session. prepares to propagate to
	  * command (default) or to info after the client issues "GO" */
    protected void processHandshake() throws java.lang.Exception {
        String strCmd = objStringTok.nextToken().trim();
        if (intTrace > 0) {
            System.out.println("SessionThread cmd: '" + strCmd + "'");
        }
        if (strCmd.equals(Declare.strCmdSet)) {
            String strSubject = null;
            String strAttr = null;
            String strPara = null;
            try {
                strSubject = objStringTok.nextToken().trim();
                strAttr = objStringTok.nextToken().trim();
                strPara = objStringTok.nextToken().trim();
            } catch (Exception e) {
            }
            if (intTrace > 0) {
                System.out.println("SessionThread subject: '" + strSubject + "'");
            }
            if (strSubject.equals("PROTOCOL")) {
                if ((strAttr.equals("SRCP")) && (strPara.equals(Declare.strSRCPVersion))) {
                    writeAck(201, "PROTOCOL SRCP");
                } else {
                    throw new ExcUnsupportedProtocol();
                }
                return;
            }
            if (strSubject.equals("CONNECTIONMODE")) {
                if ((strAttr.equals("SRCP")) && (strPara.equals("COMMAND"))) {
                    writeAck(202, "CONNECTIONMODE");
                    intNextServerMode = intModeCommand;
                    return;
                }
                if ((strAttr.equals("SRCP")) && (strPara.equals("INFO"))) {
                    writeAck(202, "CONNECTIONMODE");
                    intNextServerMode = intModeInfo;
                    return;
                }
                throw new ExcUnsupportedConnectionMode();
            }
        }
        if (intTrace > 0) {
            System.out.println("SessionThread before go");
        }
        if (strCmd.equals(Declare.strCmdGo)) {
            String strSubject;
            try {
                strSubject = objStringTok.nextToken();
            } catch (Exception e) {
            }
            ;
            writeAck(200, "GO " + Integer.toString(intConnectionId));
            intServerMode = intNextServerMode;
            if (intServerMode == intModeInfo) {
                objEventlog.println("session " + Integer.toString(intConnectionId) + " go info");
                processInfo();
            }
            if (intServerMode == intModeCommand) {
                objEventlog.println("session " + Integer.toString(intConnectionId) + " go command");
            }
            return;
        }
        if (intTrace > 0) {
            System.out.println("SessionThread after go");
        }
        throw new ExcUnsufficientData();
    }

    /** switch to info mode */
    protected void processInfo() throws java.lang.Exception {
        objSRCPDaemon.getInfoDistributor().connectSession(objOStream);
    }

    /** return any SRCP exception object via socket to the client */
    protected void writeException(SRCPException e) throws Exception {
        writeAck(e.getErrorNumber(), e.getErrorMessage());
    }

    /** return session info string (for GET 0 SESSION X) */
    public String getInfo() {
        String strDummy = "0 SESSION " + Integer.toString(intConnectionId);
        switch(intServerMode) {
            case intModeInfo:
                strDummy = strDummy + " INFOMODE ";
                break;
            case intModeCommand:
                strDummy = strDummy + " COMMANDMODE ";
                break;
            case intModeHandshake:
                strDummy = strDummy + " HANDSHAKEMODE ";
                break;
        }
        strDummy = strDummy + objCommSocket.getInetAddress().getHostName() + " (";
        strDummy = strDummy + objCommSocket.getInetAddress().getHostAddress() + ")";
        return strDummy;
    }

    public int getConnectionId() {
        return intConnectionId;
    }

    static {
        objConnectionCounterMutex = new Object();
    }
}
