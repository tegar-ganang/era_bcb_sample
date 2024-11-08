package edu.sdsc.rtdsm.dig.sites;

import java.util.*;
import java.io.*;
import java.net.*;
import edu.sdsc.rtdsm.framework.src.*;
import edu.sdsc.rtdsm.framework.util.*;
import edu.sdsc.rtdsm.dig.sites.lake.*;

public class SiteSourceListener {

    int port;

    ServerSocket server;

    SrcConfigParser parser;

    public SiteSourceListener(String configFile) {
        try {
            DTProperties dtp = DTProperties.getProperties(Constants.DEFAULT_PROP_FILE_NAME);
            String portStr = dtp.getProperty(Constants.SITE_LISTENER_PORT_TAG);
            port = Integer.parseInt(portStr);
            if (Constants.SITE_LISTENER_HEADER_LENGTH_SIZE > 4) {
                throw new IllegalStateException("The header length size cannot be " + "greater than 4 (the number of bytes in integer in Java)");
            }
            parser = new SrcConfigParser();
            parser.fileName = configFile;
            parser.parse();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("No \"rtdsm.properties\" file found " + "while trying to get Site Listener properties");
        }
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

    private void initializeSource(SensorMetaData smd, boolean runningInstanceToo) {
        if (smd.getSource() != null && !runningInstanceToo) {
            return;
        }
        SrcConfig srcConfig = parser.getSourceConfig(smd.getId());
        SiteSource src = new SiteSource(srcConfig);
        smd.setSrcConfig(srcConfig);
        smd.setSource(src);
        boolean connected = src.connect();
        Debugger.debug(Debugger.TRACE, "Source connected = " + connected);
        if (connected == false) {
            throw new IllegalStateException("Could not connect the source " + "\"" + smd.getId() + "\"");
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
                    SensorMetaData smd = handleGetMetaData(sis);
                    sendSmd(sock, smd);
                    initializeSource(smd, false);
                    break;
                case Constants.SITE_LISTENER_SEND_DATA_TYPE:
                    handleSendData(sis);
                    break;
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

    public void send(Socket sock, byte[] buf) {
        try {
            for (int i = 0; i < buf.length; i++) {
                Debugger.debug(Debugger.TRACE, "buf[" + i + "]=" + buf[i]);
            }
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
        short numChannels = (short) smd.getNumChannels();
        Vector datatypes = smd.getChannelDatatypes();
        byte[] idLenArr = Utility.shortToByteArray(len);
        byte[] numChannelsArr = Utility.shortToByteArray(numChannels);
        byte[] idArr = id.getBytes();
        byte[] finalBuff = new byte[idLenArr.length + numChannelsArr.length + idArr.length + idLenArr.length * datatypes.size()];
        int offset = 0;
        System.arraycopy(idLenArr, 0, finalBuff, offset, idLenArr.length);
        offset += idLenArr.length;
        System.arraycopy(idArr, 0, finalBuff, offset, idArr.length);
        offset += idArr.length;
        System.arraycopy(numChannelsArr, 0, finalBuff, offset, numChannelsArr.length);
        offset += numChannelsArr.length;
        for (int i = 0; i < datatypes.size(); i++) {
            byte[] dtArr = Utility.shortToByteArray((short) (((Integer) datatypes.elementAt(i)).intValue()));
            System.arraycopy(dtArr, 0, finalBuff, offset, dtArr.length);
            offset += dtArr.length;
        }
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

    private void handleSendData(StructureInputStream sis) {
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
            handleSensorSpecificData(smd, sis);
        } catch (EOFException eof) {
            eof.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void handleSensorSpecificData(SensorMetaData smd, StructureInputStream sis) {
        try {
            int dataLen = sis.readUnsignedShort();
            Debugger.debug(Debugger.TRACE, "Sis:dataLen=" + dataLen);
            Vector<Integer> datatypes = smd.getChannelDatatypes();
            for (int i = 0; i < smd.getNumChannels(); i++) {
                int dt = ((Integer) datatypes.elementAt(i)).intValue();
                switch(dt) {
                    case Constants.DATATYPE_DOUBLE:
                        double val[] = new double[1];
                        val[0] = sis.readDouble();
                        smd.getSource().insertData(i, (Object) val);
                        Debugger.debug(Debugger.TRACE, "Channel[" + i + "]=" + val[0]);
                        break;
                    default:
                        throw new IllegalStateException("Currently, support is " + "provided only to double data type");
                }
            }
            smd.getSource().flush();
        } catch (EOFException eof) {
            eof.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java stubs.SiteSourceListener <configFile>");
            return;
        }
        String configFile = args[0];
        SiteSourceListener listener = new SiteSourceListener(configFile);
        listener.connectAndListen();
    }
}
