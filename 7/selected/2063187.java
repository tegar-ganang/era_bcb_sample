package com.wilko.jslinked;

import java.*;
import java.util.Enumeration;
import java.util.Vector;
import java.io.*;
import java.lang.*;
import javax.comm.CommPort;
import javax.comm.CommPortIdentifier;
import javax.comm.SerialPort;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.*;

public class slinke implements SerialPortEventListener, Runnable {

    private SerialPort sp;

    private OutputStream os;

    private InputStream is;

    int baud;

    private Vector messages;

    private slinkeRawMsg[] recvMsgs;

    private int msgLen;

    private int fragLen;

    private int currentPort;

    private boolean inMessage;

    private boolean inFrag;

    private int sysMsg;

    private Byte version;

    private Byte[] serialNumber;

    private int samplePeriod;

    private int carrierFreq;

    private boolean versionRead;

    private boolean serialRead;

    private boolean sampleRateRead;

    private boolean carrierFreqRead;

    private char hexchar[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private byte[] buffer;

    public slinke(String portname) {
        try {
            CommPortIdentifier port = CommPortIdentifier.getPortIdentifier(portname);
            Initialise(port);
        } catch (NoSuchPortException e) {
            System.out.println("Unable to locate port: " + portname);
        }
    }

    public slinke(String portname, int br) {
        try {
            CommPortIdentifier port = CommPortIdentifier.getPortIdentifier(portname);
            Initialise(port, br);
        } catch (NoSuchPortException e) {
            System.out.println("Unable to locate port: " + portname);
        }
    }

    public slinke(CommPortIdentifier port) {
        Initialise(port);
    }

    public slinke(CommPortIdentifier port, int br) {
        Initialise(port, br);
    }

    private void Initialise(CommPortIdentifier port) {
        Initialise(port, 19200);
    }

    private void Initialise(CommPortIdentifier port, int br) {
        baud = br;
        messages = new Vector(10);
        recvMsgs = new slinkeRawMsg[8];
        buffer = new byte[1000];
        msgLen = 0;
        fragLen = 0;
        currentPort = 0;
        inMessage = false;
        inFrag = false;
        sysMsg = 0;
        System.out.println("Opening Port " + port.getName());
        try {
            sp = (SerialPort) port.open("slinke", 10);
            os = sp.getOutputStream();
            is = sp.getInputStream();
            sp.setSerialPortParams(baud, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            sp.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_OUT);
            sp.enableReceiveTimeout(5000);
            sp.addEventListener(this);
            sp.notifyOnDataAvailable(true);
        } catch (Exception e) {
            System.out.println("Caught exception while setting up serial port: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void serialEvent(SerialPortEvent ev) {
        if (sp == null) {
            return;
        }
        int et = ev.getEventType();
        switch(et) {
            case SerialPortEvent.DATA_AVAILABLE:
                receiveData();
                break;
        }
    }

    private void receiveData() {
        int bytes;
        try {
            bytes = is.read(buffer);
            for (int i = 0; i < bytes; i++) {
                if (sysMsg == 1) {
                    sysMsg = 2;
                    inFrag = true;
                    fragLen = 0;
                    switch(buffer[i]) {
                        case 1:
                            msgLen = 2;
                            break;
                        case 4:
                            msgLen = 3;
                            break;
                        case 6:
                            msgLen = 3;
                            break;
                        case 11:
                            msgLen = 2;
                            break;
                        case 12:
                            msgLen = 9;
                            break;
                        default:
                            System.out.println("Unknown response");
                            break;
                    }
                }
                if (inFrag) {
                    recvMsgs[currentPort].addDataToMsg(buffer[i]);
                    fragLen++;
                    if (fragLen == msgLen) {
                        inFrag = false;
                        if (sysMsg == 2) {
                            sysMsg = 0;
                            ProcessSystem(recvMsgs[currentPort]);
                            inMessage = false;
                        }
                    }
                } else {
                    currentPort = ((buffer[i] & 0xe0) >> 5);
                    msgLen = (buffer[i] & 0x1f);
                    fragLen = 0;
                    if (msgLen == 0) {
                        messages.addElement(recvMsgs[currentPort]);
                        inMessage = false;
                        inFrag = false;
                    } else if (msgLen == 31) {
                        sysMsg = 1;
                        recvMsgs[currentPort] = new slinkeRawMsg();
                        recvMsgs[currentPort].setPortNo(currentPort);
                    } else {
                        inFrag = true;
                        if (inMessage == false) {
                            recvMsgs[currentPort] = new slinkeRawMsg();
                            recvMsgs[currentPort].setPortNo(currentPort);
                            if (recvMsgs[currentPort].isIRMsg()) {
                                recvMsgs[currentPort].setSamplePeriod(samplePeriod);
                            } else {
                                recvMsgs[currentPort].setSamplePeriod(0);
                            }
                            inMessage = true;
                        }
                    }
                }
            }
        } catch (IOException e) {
        }
    }

    public void close() {
        sp.removeEventListener();
        sp.close();
        sp = null;
    }

    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    wait(1000);
                }
            } catch (InterruptedException e) {
            }
        }
    }

    public boolean messageAvailable() {
        return ((messages.size() > 0));
    }

    public slinkeRawMsg getMessage() {
        slinkeRawMsg msg = null;
        if (messages.size() > 0) {
            msg = (slinkeRawMsg) messages.firstElement();
            messages.removeElementAt(0);
        }
        return (msg);
    }

    private String hexval(byte b) {
        String ret = new String("");
        int nyb;
        nyb = b & 0xf0;
        nyb = nyb >> 4;
        ret = ret + hexchar[nyb];
        nyb = b & 0x0f;
        ret = ret + hexchar[nyb];
        return (ret);
    }

    private void sendData(byte[] cmd) {
        for (int i = 0; i < cmd.length; i++) {
            try {
                os.write(cmd[i]);
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void RequestVersion() {
        byte[] cmdmsg = { (byte) 0xff, (byte) 0x0b, (byte) 0xe0 };
        sendData(cmdmsg);
        versionRead = false;
    }

    private void RequestSampleRate() {
        byte[] cmdmsg = { (byte) 0x9f, (byte) 0x05, (byte) 0x80 };
        sendData(cmdmsg);
        sampleRateRead = false;
    }

    private void RequestCarrierFreq() {
        byte[] cmdmsg = { (byte) 0x9f, (byte) 0x07, (byte) 0x80 };
        sendData(cmdmsg);
        carrierFreqRead = false;
    }

    private void SetSampleRate(int rate) {
        byte[] cmdmsg = { (byte) 0x9f, (byte) 0x04, (byte) 0x00, (byte) 0x00 };
        rate = rate * 5;
        cmdmsg[2] = (byte) ((rate / 256) & 0xff);
        cmdmsg[3] = (byte) ((rate % 256) & 0xff);
        sendData(cmdmsg);
        sampleRateRead = false;
    }

    private void SetCarrierPeriod(int prescale, int period) {
        byte[] cmdmsg = { (byte) 0x9f, (byte) 0x06, (byte) 0x00, (byte) 0x00 };
        cmdmsg[2] = (byte) (prescale & 0xff);
        cmdmsg[3] = (byte) (period & 0x0ff);
        sendData(cmdmsg);
        carrierFreqRead = false;
    }

    private void RequestSerial() {
        byte[] cmdmsg = { (byte) 0xff, (byte) 0x0c, (byte) 0xe0 };
        sendData(cmdmsg);
        serialRead = false;
    }

    public int getVersion() {
        if (versionRead) return (version.intValue()); else return (-1);
    }

    public int getCarrierFreq() {
        if (carrierFreqRead) {
            return (carrierFreq);
        } else {
            return (-1);
        }
    }

    public int getSamplePeriod() {
        if (sampleRateRead) {
            return (samplePeriod);
        } else {
            return (-1);
        }
    }

    public String getSerialNumber() {
        String retstr = new String("");
        if (serialRead) {
            for (int i = 0; i < 8; i++) {
                int temp = serialNumber[i].intValue();
                String tmp = Integer.toHexString(temp & 0x00ff);
                if (tmp.length() == 1) tmp = "0" + tmp;
                retstr = retstr + tmp;
            }
        }
        return (retstr);
    }

    private void ProcessSystem(slinkeRawMsg m) {
        Byte[] msg = m.getMsgData();
        switch(msg[0].intValue()) {
            case 4:
                samplePeriod = ((msg[2].intValue() & 0xff) + ((msg[1].intValue() & 0xff) * 256)) / 5;
                sampleRateRead = true;
                break;
            case 6:
                carrierFreq = 1000 / (((1 << (msg[1].intValue() & 0xff)) * ((msg[2].intValue() & 0xff) + 1)) / 5);
                carrierFreqRead = true;
                break;
            case 11:
                version = msg[1];
                versionRead = true;
                ;
                break;
            case 12:
                int i;
                serialNumber = new Byte[8];
                for (i = 0; i < 8; i++) {
                    serialNumber[i] = msg[i + 1];
                }
                serialRead = true;
                break;
            default:
                break;
        }
    }

    public Thread startBG() {
        Thread t;
        t = new Thread(this);
        t.start();
        RequestSerial();
        RequestVersion();
        SetSampleRate(150);
        SetCarrierPeriod(0, 124);
        while (!versionRead) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }
        return (t);
    }
}
