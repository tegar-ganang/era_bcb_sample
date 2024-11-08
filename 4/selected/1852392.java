package edu.sdsc.rtdsm.dig.sites;

import java.util.*;
import java.io.*;
import java.net.*;
import edu.sdsc.rtdsm.framework.src.*;
import edu.sdsc.rtdsm.framework.util.*;
import edu.sdsc.rtdsm.dig.sites.lake.*;
import edu.sdsc.rtdsm.drivers.turbine.util.*;
import edu.sdsc.rtdsm.framework.feedback.SrcFeedbackListener;

public class LakeSource {

    int port;

    ServerSocket server;

    LakeSrcConfigParser parser;

    Hashtable<String, String> feedbackHash = new Hashtable<String, String>();

    Vector<String> controlChannelVec = new Vector<String>();

    Vector<Integer> controlChannelDatatypes = new Vector<Integer>();

    public LakeSource(String configFile) {
        try {
            DTProperties dtp = DTProperties.getProperties(Constants.DEFAULT_PROP_FILE_NAME);
            String portStr = dtp.getProperty(Constants.SITE_LISTENER_PORT_TAG);
            port = Integer.parseInt(portStr);
            if (Constants.SITE_LISTENER_HEADER_LENGTH_SIZE > 4) {
                throw new IllegalStateException("The header length size cannot be " + "greater than 4 (the number of bytes in integer in Java)");
            }
            if (configFile != null) {
                parser = new LakeSrcConfigParser(configFile);
                parser.parse();
            } else {
                parser = new LakeSrcConfigParser();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("No \"rtdsm.properties\" file found " + "while trying to get Site Listener properties");
        }
        controlChannelVec.addElement(Constants.LAKE_CONTROL_SOURCE_CHANNEL_NAME);
        controlChannelDatatypes.addElement(Constants.DATATYPE_STRING_OBJ);
    }

    public void connect() {
        try {
            server = new ServerSocket(port);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Socket Error: Could not connect to " + "port:" + port);
        }
    }

    public void accept() {
        Socket acceptSock = null;
        while (true) {
            System.out.println("Waiting...");
            try {
                acceptSock = server.accept();
                System.out.println("Accepted from " + acceptSock.getInetAddress());
            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw new IllegalStateException("Socket Error: Could not \"accept\" at " + "port:" + port);
            }
            byte[] buffer = readFromSock(acceptSock);
            boolean terminateConn = processPacket(acceptSock, buffer);
            try {
                acceptSock.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw new IllegalStateException("Accept socket could not be closed");
            }
            if (terminateConn) {
                closeConnection();
                break;
            }
        }
    }

    private void closeConnection() {
        try {
            server.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Socket Error: Could not close " + "serverSocket");
        }
    }

    private void connectAndListen() {
        connect();
        accept();
    }

    private int readBlocking(int numBytes, BufferedInputStream in, byte[] buffer, int offset, int numTries) {
        int numBytesRead = 0;
        int count = 0;
        Debugger.debug(Debugger.TRACE, "Reading " + numBytes + ". Storing from " + offset);
        try {
            while (numBytesRead < numBytes) {
                System.out.println("Blocking read: Number of bytes available = " + in.available());
                if (in.available() >= numBytes) {
                    numBytesRead += in.read(buffer, offset, numBytes);
                } else {
                    Debugger.debug(Debugger.TRACE, "Sleep Waiting... " + numBytesRead + "/" + numBytes);
                    Thread.sleep(100);
                    count++;
                }
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            throw new IllegalStateException("Read Error: Unable to wait till the data is available " + "input stream");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Socket Error: Could not read the " + "input stream");
        }
        return numBytesRead;
    }

    public byte[] readFromSock(Socket sock) {
        byte[] buffer = new byte[Constants.SITE_LISTENER_SEND_DATA_KEY_LENGTH_SIZE];
        try {
            BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
            int numBytesRead = 0;
            numBytesRead += readBlocking(Constants.SITE_LISTENER_SEND_DATA_KEY_LENGTH_SIZE, in, buffer, 0, 2);
            int len = Utility.readIntoInt(buffer, 0, Constants.SITE_LISTENER_HEADER_LENGTH_SIZE);
            Debugger.debug(Debugger.TRACE, "Packet len=" + len);
            byte[] tbuffer = new byte[len];
            for (int i = 0; i < Constants.SITE_LISTENER_SEND_DATA_KEY_LENGTH_SIZE; i++) {
                tbuffer[i] = buffer[i];
            }
            buffer = tbuffer;
            numBytesRead += readBlocking(len - numBytesRead, in, buffer, numBytesRead, 2);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Socket Error: Could not read the " + "input stream");
        }
        return buffer;
    }

    private void initializeSource(SensorMetaData smd, boolean overwriteRunningInstance) {
        if (smd.getSource() != null && !overwriteRunningInstance) {
            return;
        }
        System.out.println("smd=" + smd);
        System.out.println("smd.id=" + smd.getId() + "=");
        SrcConfig srcConfig = parser.getSourceConfig(smd.getId());
        configureSrcConfig(smd, srcConfig);
        if (smd.getSource() != null) {
            smd.getSource().disconnect();
        }
        SrcFeedbackListener fbListener = new LakeSourceFeedbackHandler(this, srcConfig);
        SiteSource src = new SiteSource(srcConfig, fbListener);
        smd.setSrcConfig(srcConfig);
        smd.setSource(src);
        boolean connected = src.connect();
        Debugger.debug(Debugger.TRACE, "Source connected = " + connected);
        if (connected == false) {
            throw new IllegalStateException("Could not connect the source " + "\"" + smd.getId() + "\"");
        }
        broadcastOnControlChannel(smd);
    }

    public void broadcastOnControlChannel(SensorMetaData smd) {
        TurbineSrcConfig controlSrcConfig = new TurbineSrcConfig(Constants.LAKE_CONTROL_SOURCE_NAME);
        TurbineSrcConfig currSrcConfig = (TurbineSrcConfig) parser.getSourceConfig(smd.getId());
        TurbineServer currServer = currSrcConfig.getServer();
        TurbineServer controlServer = new TurbineServer(currServer.getServerAddr(), currServer.getUsername(), currServer.getPassword());
        controlSrcConfig.setServer(controlServer);
        controlSrcConfig.setChannelInfo(controlChannelVec, controlChannelDatatypes);
        Debugger.debug(Debugger.TRACE, "Control Information");
        Debugger.debug(Debugger.TRACE, "====================");
        controlSrcConfig.printSrcConfig();
        Debugger.debug(Debugger.TRACE, "====================");
        SiteSource src = new SiteSource(controlSrcConfig, null);
        Vector<Integer> chnlIndex = src.getChannelIndicies();
        src.connect();
        Debugger.debug(Debugger.TRACE, "====================");
        String serverAddr = currServer.getServerAddr();
        String portStr = "";
        int indexOfPort = serverAddr.indexOf(":");
        if (indexOfPort != -1) {
            portStr = serverAddr.substring(indexOfPort, serverAddr.length());
            serverAddr = serverAddr.substring(0, indexOfPort);
        }
        if ("127.0.0.1".equals(serverAddr) || "localhost".equals(serverAddr)) {
            serverAddr = Utility.getIPAddress(serverAddr);
            System.err.println("WARNING: WARNING: WARNING: Server address is local " + "address. IP address being sent (" + serverAddr + ") to sink may be " + "a loopback address. " + "In such a case, sink WILL not work if it is not on the same machine " + "as the server");
        }
        Debugger.debug(Debugger.TRACE, "PortStr= " + portStr);
        Debugger.debug(Debugger.TRACE, "ServerAddr= " + serverAddr);
        String username = currServer.getUsername();
        String password = currServer.getPassword();
        if ("".equals(username)) {
            username = Constants.NONEMPTY_DUMMY_USER_NAME_OR_PASSWORD;
        }
        if ("".equals(password)) {
            password = Constants.NONEMPTY_DUMMY_USER_NAME_OR_PASSWORD;
        }
        String message = Constants.LAKE_CONTROL_LOOKUP_PREFIX + smd.getId() + Constants.LAKE_CONTROL_SEPARATOR + serverAddr + portStr + Constants.LAKE_CONTROL_SEPARATOR + username + Constants.LAKE_CONTROL_SEPARATOR + password + Constants.LAKE_CONTROL_SEPARATOR + smd.getWebServiceString();
        src.insertData(chnlIndex.elementAt(0).intValue(), (Object) message);
        src.flush();
        src.disconnect();
    }

    private void configureSrcConfig(SensorMetaData smd, SrcConfig srcConfig) {
        if (srcConfig instanceof TurbineSrcConfig) {
            ((TurbineSrcConfig) srcConfig).resetChannelVecs(smd.getChannels(), smd.getChannelDatatypes());
        } else {
            throw new IllegalArgumentException("Currently only data turbine is " + "supported");
        }
    }

    private boolean processPacket(Socket sock, byte[] buffer) {
        int len = 0;
        int type = 0;
        int offset = 0;
        try {
            StructureInputStream sis = new StructureInputStream(buffer);
            len = sis.readUnsignedShort();
            Debugger.debug(Debugger.TRACE, "len=" + len + "byteArr.length= " + buffer.length);
            type = (int) (sis.readByte());
            Debugger.debug(Debugger.TRACE, "type=" + type);
            switch(type) {
                case Constants.SITE_LISTENER_GET_META_DATA_TYPE:
                    {
                        SensorMetaData smd = handleGetMetaData(sis);
                        parser.addConfig(smd, true);
                        sendSmd(sock, smd);
                        initializeSource(smd, true);
                        break;
                    }
                case Constants.SITE_LISTENER_SEND_DATA_TYPE:
                    {
                        SensorMetaData smd = handleSendData(sis);
                        ackAndCheckForFeedback(sock, smd);
                        break;
                    }
                default:
                    throw new IllegalStateException("Illegal Type of packet: Only " + "SEND_DATA and GET_META_DATA are supported currently.");
            }
        } catch (EOFException eof) {
            eof.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return false;
    }

    public synchronized void ackAndCheckForFeedback(Socket sock, SensorMetaData smd) {
        short ack = (short) ((smd == null) ? 0 : 1);
        byte[] ackArr = Utility.shortToByteArray(ack);
        String feedbackMsg = null;
        if (smd != null) {
            Debugger.debug(Debugger.TRACE, "SensorId=" + smd.getId());
            if (feedbackHash.containsKey(smd.getId())) {
                feedbackMsg = feedbackHash.get(smd.getId());
                feedbackHash.remove(smd.getId());
            }
        }
        short pktSize = (short) ackArr.length;
        byte[] feedbackMsgSizeArr = null;
        byte[] feedbackArr = null;
        short feedbackMsgSize = 0;
        if (feedbackMsg != null) {
            feedbackMsgSize = (short) feedbackMsg.length();
            feedbackMsgSizeArr = Utility.shortToByteArray(feedbackMsgSize);
            feedbackArr = feedbackMsg.getBytes();
            pktSize += feedbackMsgSizeArr.length + feedbackArr.length;
        }
        byte[] pktSizeArr = Utility.shortToByteArray(pktSize);
        byte[] finalBuff = new byte[pktSizeArr.length + pktSize];
        int offset = 0;
        System.arraycopy(pktSizeArr, 0, finalBuff, offset, pktSizeArr.length);
        offset += pktSizeArr.length;
        System.arraycopy(ackArr, 0, finalBuff, offset, ackArr.length);
        offset += ackArr.length;
        if (feedbackMsg != null) {
            System.arraycopy(feedbackMsgSizeArr, 0, finalBuff, offset, feedbackMsgSizeArr.length);
            offset += feedbackMsgSizeArr.length;
            System.arraycopy(feedbackArr, 0, finalBuff, offset, feedbackArr.length);
            offset += feedbackArr.length;
        }
        send(sock, finalBuff);
    }

    public void send(Socket sock, byte[] buf) {
        try {
            DataOutputStream os = new DataOutputStream(sock.getOutputStream());
            os.write(buf, 0, buf.length);
            os.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Error sending: Could not send via " + "the given socket");
        }
    }

    public void sendSmd(Socket sock, SensorMetaData smd) {
        String id = smd.getId();
        short len = (short) id.length();
        short numChannels = (short) (smd.getNumChannels() - 1);
        Vector datatypes = smd.getChannelDatatypes();
        byte[] idLenArr = Utility.shortToByteArray(len);
        byte[] numChannelsArr = Utility.shortToByteArray(numChannels);
        byte[] idArr = id.getBytes();
        String webSerStr = smd.getWebServiceString();
        short webSerStrLen = (short) webSerStr.length();
        byte[] webSerStrLenArr = Utility.shortToByteArray(webSerStrLen);
        byte[] webSerStrArr = webSerStr.getBytes();
        byte[] finalBuff = new byte[idLenArr.length + numChannelsArr.length + idArr.length + idLenArr.length * datatypes.size() + webSerStrLenArr.length + webSerStrArr.length];
        int offset = 0;
        System.arraycopy(idLenArr, 0, finalBuff, offset, idLenArr.length);
        offset += idLenArr.length;
        System.arraycopy(idArr, 0, finalBuff, offset, idArr.length);
        offset += idArr.length;
        System.arraycopy(numChannelsArr, 0, finalBuff, offset, numChannelsArr.length);
        offset += numChannelsArr.length;
        for (int i = 1; i < datatypes.size(); i++) {
            byte[] dtArr = Utility.shortToByteArray((short) (((Integer) datatypes.elementAt(i)).intValue()));
            System.arraycopy(dtArr, 0, finalBuff, offset, dtArr.length);
            offset += dtArr.length;
        }
        System.arraycopy(webSerStrLenArr, 0, finalBuff, offset, webSerStrLenArr.length);
        offset += webSerStrLenArr.length;
        System.arraycopy(webSerStrArr, 0, finalBuff, offset, webSerStrArr.length);
        offset += webSerStrArr.length;
        send(sock, finalBuff);
    }

    public void sendString(Socket sock, String str) {
        int length = str.length();
        byte[] intBuff = Utility.intToByteArray(length);
        byte[] buf = str.getBytes();
        byte[] buffer = new byte[intBuff.length + buf.length];
        System.arraycopy(intBuff, 0, buffer, 0, intBuff.length);
        System.arraycopy(buf, 0, buffer, intBuff.length, buf.length);
        send(sock, buffer);
    }

    private SensorMetaData handleGetMetaData(StructureInputStream sis) {
        String retVal = null;
        SensorMetaData smd = null;
        try {
            int offset = 0;
            int idLen = sis.readUnsignedShort();
            offset += Constants.SITE_LISTENER_GET_META_DATA_ID_LENGTH_SIZE;
            Debugger.debug(Debugger.TRACE, "Sis:idLen=" + idLen);
            String id = sis.getStringValue(offset, idLen);
            offset += idLen;
            Debugger.debug(Debugger.TRACE, "Sis:id=" + id);
            SiteMetaDataRequester mdr = new SiteMetaDataRequester(id);
            smd = mdr.call();
        } catch (EOFException eof) {
            eof.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return smd;
    }

    private SensorMetaData handleSendData(StructureInputStream sis) {
        try {
            int offset = 0;
            int keyLen = sis.readUnsignedShort();
            offset += Constants.SITE_LISTENER_SEND_DATA_KEY_LENGTH_SIZE;
            Debugger.debug(Debugger.TRACE, "Sis:keyLen=" + keyLen);
            String key = sis.getStringValue(offset, keyLen);
            offset += keyLen;
            Debugger.debug(Debugger.TRACE, "Sis:key=" + key);
            SensorMetaData smd = SensorMetaDataManager.getInstance().getSensorMetaData(key);
            if (smd == null) {
                throw new IllegalArgumentException("There is no sensor registered" + "with the id \"" + key + "\"");
            }
            boolean success = handleSensorSpecificData(smd, sis);
            return ((success) ? smd : null);
        } catch (EOFException eof) {
            eof.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    public boolean handleSensorSpecificData(SensorMetaData smd, StructureInputStream sis) {
        try {
            int dataLen = sis.readUnsignedShort();
            Debugger.debug(Debugger.TRACE, "Sis:dataLen=" + dataLen);
            long ts = sis.readLong();
            Debugger.debug(Debugger.TRACE, "Sis:time=" + ts);
            Vector<Integer> datatypes = smd.getChannelDatatypes();
            for (int i = 0; i < smd.getNumChannels(); i++) {
                int dt = ((Integer) datatypes.elementAt(i)).intValue();
                switch(dt) {
                    case Constants.DATATYPE_DOUBLE:
                        double val[] = new double[1];
                        if (i != 0) {
                            val[0] = sis.readDouble();
                        } else {
                            val[0] = (double) ts;
                        }
                        smd.getSource().insertData(i, (Object) val);
                        Debugger.debug(Debugger.TRACE, "Channel[" + i + "]=" + val[0]);
                        break;
                    default:
                        throw new IllegalStateException("Currently, support is " + "provided only to double data type");
                }
            }
            smd.getSource().flush();
            return true;
        } catch (EOFException eof) {
            eof.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java stubs.LakeSource [configFile]");
            return;
        }
        String configFile = null;
        if (args.length == 1) {
            configFile = args[0];
        }
        LakeSource listener = new LakeSource(configFile);
        listener.connectAndListen();
    }

    public synchronized void appendFeedbackMsg(String srcName, Date time, String feedbackMsg) {
        StringBuffer currVal = new StringBuffer();
        if (feedbackHash.containsKey(srcName)) {
            currVal = currVal.append(feedbackHash.get(srcName));
            currVal = currVal.append(Constants.LAKE_FEEDBACK_SEPARATOR);
        }
        currVal.append("" + time.getTime());
        currVal.append(Constants.LAKE_FEEDBACK_FIELD_SEPARATOR);
        currVal.append(feedbackMsg);
        feedbackHash.put(srcName, currVal.toString());
        Debugger.debug(Debugger.TRACE, "Feedback: " + srcName + " Message: ");
        Debugger.debug(Debugger.TRACE, "\t" + feedbackHash.get(srcName));
    }
}
