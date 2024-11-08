package org.tolk.ipico.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.tolk.ApplicationContextFactory;
import org.tolk.io.DataSource;
import org.tolk.io.impl.SerialDataSource;
import org.tolk.io.impl.TcpIpClientDataSource;
import org.tolk.io.impl.TcpIpServerDataSource;
import org.tolk.ipico.process.node.impl.TtoFilter;
import org.tolk.ipico.util.IpicoUtil;
import org.tolk.process.node.DeviceInterrogatorNode;
import org.tolk.util.SerialPortUtil;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import org.tolk.ipico.process.node.impl.IpicoMoxaDiscovery;

;

/**
 * Interface implemented by DataSources to enable it to respond to read events and keep on listening for events. Once a read event
 * occurs the read data should be passed forward to next Process Nodes for the DataSource.
 * 
 * @author Johan Roets
 * @author Werner van Rensburg
 */
public class DemoDataSource extends DataSource implements Runnable, DisposableBean, InitializingBean {

    protected boolean listenOnStartup = false;

    protected boolean listening = false;

    protected Thread thread;

    private List<DeviceInterrogatorNode> deviceInterrogatorNodes = new ArrayList<DeviceInterrogatorNode>();

    private IpicoMoxaDiscovery MoxaDscv;

    TcpIpClientDataSource TcpClient;

    SerialDataSource serialRdr;

    TcpIpServerDataSource TcpServer;

    TtoFilter ttoFilter;

    private final IpicoUtil ipicoUtil = new IpicoUtil();

    private int listCounter = 0;

    private List<String> portsList = new ArrayList<String>();

    String rdrReply = "";

    String connectInfo = "NO READER AVAILABLE";

    String readerType = "";

    String readerMode = "";

    String[] writeData = new String[15];

    int portNum = 0;

    int errorCounter = 0;

    int PageNum = 1;

    boolean isConnected = false;

    boolean dateSet = false;

    boolean setTCPReader = false;

    boolean netWorkScanned = false;

    /**
     * see {@link DataSource#write(String)}
     */
    @Override
    public void write(String str) {
        TranslateCommand(str);
    }

    /**
     * Get a command from the write function and act according to command
     */
    public void TranslateCommand(String str) {
        if (setTCPReader == true) {
            this.TcpClient.write("ab000911ffff6161aa000d0a0038\r\n");
            this.setTCPReader = false;
        }
        if (str.contains("ab000e0a")) {
            this.TcpServer.write(str);
            return;
        }
        if (this.ipicoUtil.isValidTag(str)) {
            this.rdrReply = "ValidTag";
            String messages = getTagFormat(str);
            if (this.readerMode == "Write" & messages.contains("DATA")) {
                if (messages.contains(this.writeData[this.PageNum])) {
                    if (PageNum < 14) {
                        this.PageNum++;
                        TcpServer.write(messages);
                        setWriteData(this.writeData[this.PageNum]);
                    } else {
                        this.PageNum = 1;
                    }
                } else {
                    TcpServer.write(messages);
                    setWriteData(this.writeData[this.PageNum]);
                }
            } else {
                TcpServer.write(messages);
            }
        } else if (this.ipicoUtil.isValidReply(str)) {
            if (str.contains("ab00002022")) {
                this.TcpServer.write("Reader mode set to: " + this.readerMode);
            } else if (str.contains("ab00002123")) {
                this.TcpServer.write("Data set Writing data to page: " + this.PageNum);
                writeData(this.PageNum);
            } else {
                this.TcpServer.write(str);
                this.rdrReply = str;
            }
        } else if (str.length() > 2) {
            if (str.contains("stopTolk")) {
                System.exit(0);
            } else if (str.substring(0, 3).contains("ip")) {
                connectReader(str);
            } else if (str.contains("ReadMode")) {
                this.fwdMessageToAllNextNodes("ab000320030f858b\r\n");
                this.readerMode = "Read";
            } else if (str.contains("TTOMode")) {
                this.fwdMessageToAllNextNodes("ab000320040fc5b7\r\n");
                this.readerMode = "TTO";
            } else if (str.contains("WDATA")) {
                while (str.contains("WDATA")) {
                    int index = str.indexOf("WDATA");
                    int pNum = Integer.parseInt(str.substring(index + 5, index + 7));
                    int nextIndex;
                    if (str.substring(index + 5, str.length()).contains("WDATA")) {
                        nextIndex = str.substring(index + 5, str.length()).indexOf("WDATA") + 5;
                    } else {
                        nextIndex = str.length() - 1;
                    }
                    if (nextIndex - index > 20) {
                        this.writeData[pNum] = str.substring(index + 7, nextIndex);
                        this.TcpServer.write(Integer.toString(pNum));
                        this.TcpServer.write(this.writeData[pNum]);
                    }
                    str = str.substring(index + nextIndex, str.length());
                }
                this.PageNum = 1;
                setWriteData(this.writeData[this.PageNum]);
                this.readerMode = "Write";
            } else if (str.contains("ScanNetworkReaders")) {
                try {
                    MoxaDscv.scanForReaders();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                this.readerType = getRdrType(str);
                TcpServer.write("READER TYPE:" + this.readerType);
                this.fwdMessageToAllNextNodes("ab00000a51\r\n");
                setDate();
            }
        }
    }

    public void writeData(int page) {
        String cmd = "ab00032022" + Integer.toString(page, 16).toUpperCase() + "165";
        String lrc = this.ipicoUtil.generateIpxLrcString(cmd);
        this.fwdMessageToAllNextNodes(cmd + lrc + "\r\n");
    }

    public void setWriteData(String data) {
        while (this.writeData[this.PageNum] == null & this.PageNum < 14) {
            this.PageNum++;
        }
        if (this.writeData[this.PageNum] != null) {
            if (writeData[this.PageNum].length() >= 15) {
                String cmd = "ab000821" + this.writeData[this.PageNum];
                String lrc = this.ipicoUtil.generateIpxLrcString(cmd);
                this.fwdMessageToAllNextNodes(cmd + lrc + "\r\n");
                this.TcpServer.write("Command: " + cmd);
            }
        }
    }

    public String getTagFormat(String str) {
        String tagStr = "";
        if (!this.ipicoUtil.isTtoTagId(str)) {
            if (isDataPage(str)) {
                int pageNumber = getDataPageNumber(str);
                if (pageNumber < 10) {
                    tagStr = "DATA0" + Integer.toString(pageNumber) + this.ipicoUtil.getUID(str) + getData(str);
                } else {
                    tagStr = "DATA" + Integer.toString(pageNumber) + this.ipicoUtil.getUID(str) + getData(str);
                }
            } else {
                int pageNumber = this.ipicoUtil.getTtoPageNumber(str);
                if (pageNumber < 10) {
                    tagStr = "TTO0" + Integer.toString(pageNumber) + this.ipicoUtil.getTtoData(str);
                } else {
                    tagStr = "TTO" + Integer.toString(pageNumber) + this.ipicoUtil.getTtoData(str);
                }
            }
        } else {
            tagStr = "UID" + this.ipicoUtil.getUID(str);
        }
        return tagStr;
    }

    public boolean isDataPage(String str) {
        if (str.charAt(0) == 'a') {
            if (str.charAt(1) == 'd') {
                return true;
            }
        }
        return false;
    }

    public String connectReader(String str) {
        this.TcpClient = (TcpIpClientDataSource) ApplicationContextFactory.getBean("EthernetRdr");
        String[] strAray = str.split("\n");
        if (strAray[0].contains("ip")) {
            strAray = strAray[0].split(" ");
            strAray = strAray[1].split(":");
            if (isValidIp(strAray[0])) {
                this.TcpClient.setIpAddress(strAray[0]);
                this.TcpClient.setPort(Integer.parseInt(strAray[1]));
                try {
                    Thread.sleep(1000);
                    this.TcpServer.write("Slept for 1 sec");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.setTCPReader = true;
                this.TcpClient.write("ab000911ffff6161aa000d0a0038\r\n");
                return "IP set";
            }
        }
        return "IP not set";
    }

    public static boolean isValidIp(final String ip) {
        return ip.matches("^[\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3}\\.[\\d]{1,3}$");
    }

    public String getData(String str) {
        String data = null;
        try {
            data = str.substring(24, 40);
        } catch (IndexOutOfBoundsException e) {
        }
        return data;
    }

    public int getDataPageNumber(String str) {
        int index = 0;
        try {
            index = Integer.parseInt(str.substring(21, 22), 16);
        } catch (IndexOutOfBoundsException e) {
        }
        return index;
    }

    public void setDate() {
        String dateStr;
        String[] days = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        dateStr = sdf.format(cal.getTime());
        String time = cal.getTime().toString();
        String day = "01";
        for (int k = 1; k < 7; k++) {
            if (time.contains(days[k])) {
                day = "0" + Integer.toString(k);
            }
        }
        String command;
        command = "ab000701" + dateStr.substring(2, 4) + dateStr.substring(5, 7) + dateStr.substring(8, 10) + day + dateStr.substring(11, 13) + dateStr.substring(14, 16) + dateStr.substring(17, 19);
        command += this.ipicoUtil.generateIpxLrcString(command) + "\r\n";
        this.fwdMessageToAllNextNodes(command);
        this.TcpServer.write(command);
    }

    /**
     * Starts listening for read events and respond to the read event.
     */
    public void startListening() {
        this.listening = true;
        this.thread = new Thread(this);
        this.thread.start();
    }

    /**
     * Stops listening and responding to read events.
     */
    public void stopListening() {
        this.listening = false;
    }

    /**
     * Specifies whether this DataSource should immediately start listening and responding to read events once the bean has been
     * successfully started.
     * 
     * @param listenOnStartup
     *            true if the bean should start listening on startup.
     */
    public void setListeningOnStartup(boolean listenOnStartup) {
        this.listenOnStartup = listenOnStartup;
        this.listening = listenOnStartup;
    }

    /**
     * @return true if this bean is setup to listen on startup.
     */
    public boolean getListeningOnStartup() {
        return this.listenOnStartup;
    }

    /**
     * see {@link Runnable#run()}
     */
    public void run() {
        while (this.listening) {
            try {
                String str = read();
                if (str != null) {
                    if (this.ipicoUtil.isValidTag(str)) {
                    } else if (this.ipicoUtil.isValidReply(str)) {
                        this.rdrReply = str;
                        this.TcpServer.write(str);
                    } else if (str.length() > 2) {
                        this.readerType = this.getRdrType(str);
                    }
                }
                Thread.sleep(1000);
                findSerialReader();
                if (!this.netWorkScanned) {
                    this.MoxaDscv = (IpicoMoxaDiscovery) ApplicationContextFactory.getBean("MoxaDiscovery");
                    MoxaDscv.scanForReaders();
                    this.netWorkScanned = true;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Connects to a serial reader at a specific port and baud rate. Returns whether the connection was made,
     * successfully.   
     * @return True if successful else return false. 
     * @author gerhardw
     */
    public Boolean connectToSerialReader(SerialDataSource serialReader, String port, int baud) {
        serialReader.connectDataSource(port, baud);
        return true;
    }

    /**
     * Detects a serial reader when plugged into the USB port, by going through the serial list of the computer
     * and trying to connect to each port at a baud of 9600 sending a command and waiting for a reply form the
     * decoder. 
     * @author gerhardw
     */
    public void findSerialReader() {
        if (!this.isConnected) {
            if (this.rdrReply.contains("ab000902") || this.rdrReply.contains("ab000011")) {
                this.rdrReply = "";
                this.fwdMessageToAllNextNodes("ab00012a0fea\r\n");
                this.fwdMessageToAllNextNodes("ab00000222\r\n");
                this.connectInfo = "READER AVAILABLE ON " + this.portsList.get(this.portNum);
                this.TcpServer.write(this.connectInfo);
                this.fwdMessageToAllNextNodes("ab0000372a\r\n");
                this.isConnected = true;
            } else {
                if (this.portsList.size() == this.listCounter) {
                    if (this.serialRdr != null) {
                        this.serialRdr.disconnectDataSource();
                        this.portsList = null;
                    }
                    this.serialRdr = (SerialDataSource) ApplicationContextFactory.getBean("SerialRdr");
                    this.TcpServer = (TcpIpServerDataSource) ApplicationContextFactory.getBean("tcpipLocalPort");
                    this.portsList = new ArrayList<String>();
                    this.portsList = SerialPortUtil.enumerateSerialPorts();
                    this.listCounter = 0;
                } else {
                    this.serialRdr.connectDataSource(this.portsList.get(this.listCounter), 9600);
                    if (serialRdr.hasSerialObj()) {
                        serialRdr.startListening();
                        this.serialRdr.write("ab000911ffff6161aa000d0a0038\r\n");
                        this.portNum = this.listCounter;
                    }
                    this.listCounter++;
                }
            }
        }
    }

    /**
     * see {@link DisposableBean#destroy()}
     */
    public void destroy() throws Exception {
        stopListening();
    }

    /**
     * see {@link InitializingBean#afterPropertiesSet()}
     */
    public void afterPropertiesSet() throws Exception {
        if (this.listenOnStartup) {
            startListening();
        }
    }

    /**
     * Forwards a message to all next nodes, if there are any.
     * 
     * @param message
     */
    @Override
    public void fwdMessageToAllNextNodes(String message) {
        if (this.deviceInterrogatorNodes != null) {
            for (DeviceInterrogatorNode deviceInterrogatorNode : this.deviceInterrogatorNodes) {
                deviceInterrogatorNode.setDataSourceMessage(message);
            }
        }
        super.fwdMessageToAllNextNodes(message);
    }

    /**
     * @return the deviceInterrogatorNodes
     */
    public List<DeviceInterrogatorNode> getDeviceInterrogatorNodes() {
        return this.deviceInterrogatorNodes;
    }

    /**
     * @param deviceInterrogatorNodes
     *            the deviceInterrogatorNodes to set
     */
    public void setDeviceInterrogatorNodes(List<DeviceInterrogatorNode> deviceInterrogatorNodes) {
        this.deviceInterrogatorNodes = deviceInterrogatorNodes;
    }

    private String getRdrType(String str) {
        String readerString = new String();
        if (str.contains("Handheld")) {
            readerString = "HAND HELD READER";
        } else if (str.contains("RR")) {
            if (str.contains("HF")) {
                readerString = "HF REGISTRATION READER";
            } else if (str.contains("DF")) {
                readerString = "DF REGISTRATION READER";
            }
        } else if (str.contains("ACR")) {
            if (str.contains("HF")) {
                readerString = "HF ACCESS CONTROL READER";
            } else if (str.contains("DF")) {
                readerString = "DF ACCESS CONTROL READER";
            }
        } else {
            readerString = "UNKNOWN READER";
        }
        return readerString;
    }
}
