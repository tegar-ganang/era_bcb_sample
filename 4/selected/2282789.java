package SkolController;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import SushUtil.*;
import SkolUtil.*;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author sushi
 */
public class Main {

    public static int paketLength = 65507;

    public static boolean connected = false;

    public static int triggerLevel = 50;

    private static byte triggerLevelHighByte = 0;

    private static byte triggerLevelLowByte = 0;

    public static double[] yValuesCh0 = new double[] {};

    public static double[] yValuesCh1 = new double[] {};

    public static double[] xCoordinates = null;

    public static ControllerGui myGui;

    public static boolean checkUndersampling = false;

    public static int ResolutionMultiplier = 1;

    public static int AdcSamplesPerFrameValue = 0;

    public static int AdcBitValue = 0;

    public static Boolean showSerialDataStream = false;

    public static Boolean timingAnalysis = false;

    public static Boolean AdcCounterReceivedInChannel1 = false;

    public static double OffsetXCh0, OffsetYCh0 = 0;

    public static double OffsetXCh1, OffsetYCh1 = 0;

    public static int OffsetXmax = 0;

    public static Object OffsetXLockCh0 = new Object();

    public static Object OffsetXLockCh1 = new Object();

    public static Object OffsetYLockCh0 = new Object();

    public static Object OffsetYLockCh1 = new Object();

    public static double positionXOffsetMultiplier = 0.2;

    public static double positionYOffsetMultiplier = .5;

    public static ArrayList<Byte> bytesToWaitFor = new ArrayList<Byte>();

    private static communicationThread communicator = null;

    private static MySqlConnection myMySqlThread = null;

    private static Boolean keepAquisitionRunning = false;

    private static FileWriterThread myFileWriterThread = null;

    private static TimeDividerHelper myTimeDividerHelper;

    private static FileReaderThread myFileReaderThread;

    private static Integer timeDividerIntegerCh0 = -1;

    private static Boolean triggerAtPosEdge = true;

    private static UnderSamplingCheckerThread myUnderSamplingCheckerThread;

    private static String settingsFile = "skol.xml";

    private static int timeDividerIntegerCh1;

    private static boolean triggerOn;

    public static int tmpCnt = 0;

    private static String versionString;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        myGui = new ControllerGui();
        myTimeDividerHelper = new TimeDividerHelper(settingsFile);
        for (int i = 0; i < myTimeDividerHelper.itemCount; i++) {
            myGui.jComboBox_TimeTividerCh0.addItem(myTimeDividerHelper.getItem(i));
            myGui.jComboBox_TimeTividerCh1.addItem(myTimeDividerHelper.getItem(i));
        }
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                myGui.setVisible(true);
            }
        });
        logger.jtextArea = myGui.jTextAreaMsgOutput;
        logger.jtextArea2 = myGui.jTextArea_debugOutDeveloper;
        readXmlParamFile();
        myUnderSamplingCheckerThread = new UnderSamplingCheckerThread();
        myUnderSamplingCheckerThread.setName("myUnderSamplingCheckerThread");
        myUnderSamplingCheckerThread.start();
        AdcSamplesPerFrameValue = Main.myGui.getAdcSamplesPerFrameValue();
        AdcBitValue = Main.myGui.getAdcBitValue();
        OffsetXmax = myGui.getOffsetXmaxRange();
        logger.logInfo("Skol '" + versionString + "' up and running.");
    }

    /**
     * is called by CommunicationThread with received Message.
     * This Function dispatches the Message.
     * @param myMessage
     */
    public static void dispatchReceivedMessage(message myMessage) {
        switch(myMessage.opCode) {
            case opCodes.dataAquCh0:
                receivedDataStream(0, myMessage);
                break;
            case opCodes.dataAquCh1:
                receivedDataStream(1, myMessage);
                break;
            case opCodes.singleVoltValueCh0:
                receivedSingleVoltValue(0, myMessage);
                break;
            case opCodes.singleVoltValueCh1:
                receivedSingleVoltValue(1, myMessage);
                break;
            case opCodes.getTimeDividerCh0:
                if (hexUtil.byteToUnsignedInt(myMessage.dataByte[0]) == timeDividerIntegerCh0) logger.logInfo("Set TimeDivider to " + myTimeDividerHelper.getItem(timeDividerIntegerCh0) + " succcess! (CH0)"); else logger.logError("Set TimeDivider to " + myTimeDividerHelper.getItem(timeDividerIntegerCh0) + " ERROR! (CH0)");
                bytesToWaitFor.clear();
                break;
            case opCodes.getTimeDividerCh1:
                if (hexUtil.byteToUnsignedInt(myMessage.dataByte[0]) == timeDividerIntegerCh1) logger.logInfo("Set TimeDivider to " + myTimeDividerHelper.getItem(timeDividerIntegerCh1) + " succcess! (CH1)"); else logger.logError("Set TimeDivider to " + myTimeDividerHelper.getItem(timeDividerIntegerCh1) + " ERROR! (CH1)");
                bytesToWaitFor.clear();
                break;
            case opCodes.getTriggerMode:
                if (myMessage.dataByte[0] == constants.TriggerOff && (triggerOn == false)) logger.logInfo("Set Trigger Off succcess!"); else if (myMessage.dataByte[0] == constants.TriggerNegEdge && (triggerAtPosEdge == false)) logger.logInfo("Set TriggerEdge to NegEdge succcess!"); else if (myMessage.dataByte[0] == constants.TriggerPosEdge && (triggerAtPosEdge == true)) logger.logInfo("Set TriggerEdge to PosEdge succcess!"); else logger.logInfo("Set TriggerMode ERROR! dataByte" + hexUtil.byteToHexString(myMessage.dataByte[0]));
                bytesToWaitFor.clear();
                break;
            case opCodes.getTriggerLevel:
                if (myMessage.dataByte[1] == triggerLevelHighByte && myMessage.dataByte[0] == triggerLevelLowByte) logger.logInfo("Set TriggerLevel succcess!"); else logger.logError("Set TriggerLevel ERROR!");
                bytesToWaitFor.clear();
                break;
            case opCodes.getSamplesPerFrameMode:
                if (myMessage.dataByte[0] == constants.SamplesPerFrameMode[myGui.jComboBox_Resolution.getSelectedIndex()]) logger.logInfo("Set Resolution succcess!"); else logger.logError("Set Resolution ERROR!");
                bytesToWaitFor.clear();
                break;
            default:
                logger.logDebug("Main: Wrong opCode in Message '" + myMessage.rawMessage.toString() + "'  - Message dropped.");
        }
    }

    /**
     * Starts All Communicaton related stuff
     * Here one can change if other communication e.g. RS232 should be used
     */
    public static boolean communicationThreadStart(int myType) {
        if (communicator != null) {
            logger.logDebug("(communicator != null) in startCommunicationThread");
            return false;
        }
        if (myType == communicationType.udp) {
            communicator = new udpThread();
            communicator.setName("CommunicationThread_UDP");
            ((udpThread) communicator).init(myGui.getLocalPort(), myGui.getRemotePort(), myGui.getRemoteHost());
        } else if (myType == communicationType.serial) {
            communicator = new rs232Thread();
            communicator.setName("CommunicationThread_RS232");
            ((rs232Thread) communicator).init(myGui.getSerialPortName());
        } else {
            logger.logError("Unknown communication Type in startCommunication Thread");
        }
        if (communicator.keepRunning) {
            communicator.start();
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                logger.logError("communicationThreadStart InterruptedException" + ex);
            }
            sendDeviceRawDataByte(opCodes.RESET);
            return true;
        } else {
            communicator = null;
            return false;
        }
    }

    /**
     * stops Communication
     * Releases all object ports locks sockets which were required
     */
    public static void communicationThreadStop() {
        communicator.keepRunning = false;
        try {
            communicator.interrupt();
            communicator.join();
        } catch (InterruptedException ex) {
            logger.logError(" InterruptedException stopCommunicationThread", ex);
        }
        communicator = null;
    }

    /**
     * Helper Funciton
     */
    public static void doAquisition() {
        keepAquisitionRunning = true;
        try {
            message m = new message(opCodes.dataAquCh0, new byte[] {}, (byte) 1);
            communicator.sendMessage(m.rawMessage);
        } catch (Exception ex) {
            logger.logError(" generating msg in startAquisition", ex);
        }
    }

    /**
     * Helper Funciton
     */
    public static void stopAquisition() {
        keepAquisitionRunning = false;
    }

    static void generateNewHardwareTimePerDivSettingsInLoggingOutput() {
        new TimeDividerHelper(myGui.getSystemClockMhzValue(), myGui.getAdcCyclesForSingleDAQValue(), myGui.getAdcSamplesPerFrameValue());
    }

    static void setDeviceSamplesPerFrameMode(String comboBoxText) {
        if (keepAquisitionRunning == true) {
            logger.logError("Stop Aquisition First ! ");
            return;
        }
        try {
            ResolutionMultiplier = Integer.valueOf(comboBoxText);
            byte tmpByte = constants.SamplesPerFrameMode[myGui.jComboBox_Resolution.getSelectedIndex()];
            message m = null;
            m = new message(opCodes.setSamplesPerFrameMode, new byte[] { tmpByte }, (byte) 1);
            communicator.sendMessage(m.rawMessage);
            setExpectedOpCodeValue(opCodes.getSamplesPerFrameMode);
            logger.logInfo("Try to set TimeDivider to " + comboBoxText);
            logger.logInfo("TimePerDiv from all Channels needs now to be multiplied with this Factor!");
        } catch (Exception ex) {
            logger.logError("Error setting TimeDivider", ex);
        }
    }

    static void setDeviceTimeDivider(String comboBoxText, int channel) {
        if (keepAquisitionRunning == true) {
            logger.logError("Stop Aquisition First ! ");
            return;
        }
        try {
            int tmpTimeDividerInteger = myTimeDividerHelper.getDividerIndex(comboBoxText);
            if (hexUtil.howMuchBytesAreNeededToRepresentTheHighestIntegerInThisArray(new int[] { tmpTimeDividerInteger }) != 1) {
                logger.logError("timeDividerInteger is too big in setDeviceTimedivider " + tmpTimeDividerInteger);
                return;
            }
            byte tmpByte = hexUtil.intToByteArray(tmpTimeDividerInteger, 1)[0];
            message m = null;
            if (channel == 0) {
                timeDividerIntegerCh0 = tmpTimeDividerInteger;
                m = new message(opCodes.setTimeDividerCh0, new byte[] { tmpByte }, (byte) 1);
                communicator.sendMessage(m.rawMessage);
                setExpectedOpCodeValue(opCodes.getTimeDividerCh0);
            } else if (channel == 1) {
                timeDividerIntegerCh1 = tmpTimeDividerInteger;
                m = new message(opCodes.setTimeDividerCh1, new byte[] { tmpByte }, (byte) 1);
                communicator.sendMessage(m.rawMessage);
                setExpectedOpCodeValue(opCodes.getTimeDividerCh1);
            } else {
                logger.logError("Wrong Channel in Main.setDeviceTimeDivider" + channel);
            }
            logger.logInfo("Try to set TimeDivider to " + comboBoxText);
        } catch (Exception ex) {
            logger.logError("Error setting TimeDivider", ex);
        }
    }

    public static void setDeviceTriggerLevel(int newLevel) {
        if (keepAquisitionRunning == true) {
            logger.logError("Stop Aquisition First ! ");
            return;
        }
        if (newLevel != triggerLevel) {
            logger.logInfo("Try to set TriggerLevel to :" + newLevel + " Percent.");
            triggerLevel = newLevel;
            int maxAdcLevel = myGui.getAdcBitValue();
            maxAdcLevel = (int) java.lang.Math.pow(2, maxAdcLevel);
            maxAdcLevel = maxAdcLevel - 1;
            double newValAsDouble = maxAdcLevel * (double) ((double) newLevel / (double) 100);
            int newValAsInt = (int) newValAsDouble;
            if (newValAsInt > maxAdcLevel) {
                logger.logDebug("maxAdcLevel Correction manually " + newValAsInt);
                newValAsInt = maxAdcLevel;
            }
            byte[] newSettings = hexUtil.intToByteArray(newValAsInt, 2);
            triggerLevelHighByte = newSettings[1];
            triggerLevelLowByte = newSettings[0];
            try {
                message setTriggerLevelHighByte = new message(opCodes.setTriggerLevelHighByte, new byte[] { newSettings[0] }, (byte) 1);
                communicator.sendMessage(setTriggerLevelHighByte.rawMessage);
                message setTriggerLevelLowByte = new message(opCodes.setTriggerLevelLowByte, new byte[] { newSettings[1] }, (byte) 1);
                communicator.sendMessage(setTriggerLevelLowByte.rawMessage);
                setExpectedOpCodeValue(opCodes.getTriggerLevel);
            } catch (Exception ex) {
                logger.logError("Error sending setTriggerLevel...", ex);
            }
        }
    }

    public static void setDeviceTriggerPosEdge() {
        if (keepAquisitionRunning == true) {
            logger.logError("Stop Aquisition First ! ");
            return;
        }
        message msg;
        try {
            msg = new message(opCodes.setTriggerMode, new byte[] { constants.TriggerPosEdge }, (byte) 1);
            communicator.sendMessage(msg.rawMessage);
            logger.logInfo("Try to set Trigger to PosEdge");
            setExpectedOpCodeValue(opCodes.getTriggerMode);
            triggerAtPosEdge = true;
            triggerOn = true;
        } catch (Exception ex) {
            logger.logError("SetTriggerPosEdge Failed : ", ex);
        }
    }

    public static void setDeviceTriggerNegEdge() {
        if (keepAquisitionRunning == true) {
            logger.logError("Stop Aquisition First ! ");
            return;
        }
        message msg;
        try {
            msg = new message(opCodes.setTriggerMode, new byte[] { constants.TriggerNegEdge }, (byte) 1);
            communicator.sendMessage(msg.rawMessage);
            logger.logInfo("Try to set Trigger to NegEdge");
            setExpectedOpCodeValue(opCodes.getTriggerMode);
            triggerAtPosEdge = false;
            triggerOn = true;
        } catch (Exception ex) {
            logger.logError("SetTriggerNegEdge Failed : ", ex);
        }
    }

    public static void setDeviceTriggerOff() {
        if (keepAquisitionRunning == true) {
            logger.logError("Stop Aquisition First ! ");
            return;
        }
        message msg;
        try {
            msg = new message(opCodes.setTriggerMode, new byte[] { constants.TriggerOff }, (byte) 1);
            communicator.sendMessage(msg.rawMessage);
            logger.logInfo("Try to set Trigger off");
            setExpectedOpCodeValue(opCodes.getTriggerMode);
            triggerAtPosEdge = false;
            triggerOn = false;
        } catch (Exception ex) {
            logger.logError("SetTriggerNegEdge Failed : ", ex);
        }
    }

    static void setExpectedOpCodeValue(byte opCode) {
        int sleep = 0;
        try {
            if (!bytesToWaitFor.isEmpty()) {
                logger.logError("Did not receive Answer for old Request (setDevice_TestValue)");
                for (int i = 0; i < bytesToWaitFor.size(); i++) {
                    logger.logDebug("(setDevice_TestValue) Value " + i + " :" + hexUtil.byteToHexString(bytesToWaitFor.get(i)));
                }
                bytesToWaitFor.clear();
            }
            bytesToWaitFor.add(opCode);
            message m2 = new message(opCode, new byte[] { (byte) 0 }, (byte) 1);
            communicator.sendMessage(m2.rawMessage);
        } catch (messageException ex) {
            logger.logError("Could not send setDevice_TestValue Message", ex);
        }
    }

    static void getDeviceSingleVolt_ch0() {
        try {
            message m = new message(opCodes.singleVoltValueCh0, new byte[] {}, (byte) 1);
            communicator.sendMessage(m.rawMessage);
        } catch (Exception ex) {
            logger.logError("singleDataAqCh0", ex);
        }
    }

    static void getDeviceSingleFrameCh0withTrigger() {
        try {
            message m = new message(opCodes.dataAquCh0, new byte[] {}, (byte) 1);
            communicator.sendMessage(m.rawMessage);
        } catch (Exception ex) {
            logger.logError(" generating msg in singleFrameCh0withTrigger", ex);
        }
    }

    static void communicationFileReaderReadLine(Integer selectedVal) {
        if (myFileReaderThread == null) {
            logger.logDebug("No FileReader running");
            return;
        }
        myFileReaderThread.readLine(selectedVal);
    }

    static void communicationStartFileReaderThread(String text) {
        myFileReaderThread = new FileReaderThread(text);
        myFileReaderThread.setName("FileReaderThread");
        myFileReaderThread.start();
        myGui.jComboBoxFileReaderData.setEnabled(true);
        for (long i = 0; i < myFileReaderThread.numLines; i++) {
            myGui.jComboBoxFileReaderData.addItem(String.valueOf(i));
        }
        connected = true;
    }

    static void communicationStopFileReader() {
        try {
            myFileReaderThread.keepRunning = false;
            myFileReaderThread.interrupt();
            myFileReaderThread.join();
        } catch (InterruptedException ex) {
            logger.logError("Error Joining myFileReaderThread ", ex);
        }
        myGui.jComboBoxFileReaderData.removeAllItems();
        myFileReaderThread = null;
        connected = false;
        myGui.jComboBoxFileReaderData.setEnabled(false);
    }

    static void fileWriterStart(String filename) {
        myFileWriterThread = new FileWriterThread(filename);
        myFileWriterThread.setName("FileWriterThread");
        myFileWriterThread.start();
    }

    static void fileWriterStop() {
        myFileWriterThread.keepRunning = false;
        myFileWriterThread.interrupt();
        try {
            myFileWriterThread.join();
        } catch (InterruptedException ex) {
            logger.logError("Error Joining FileWriterThread ", ex);
        }
        myFileWriterThread = null;
    }

    static void communicationStartSqlServer(String dbHost, String dbPort, String dbName, String dbUser, String dbPass) {
        myMySqlThread = new MySqlConnection(dbHost, dbPort, dbName, dbUser, dbPass);
    }

    /**
     * Is called by the dispatcher in Main.dispatchReceivedMessage
     * udates dataInt for gui and notifys gui listener for update
     * @param dataInt pure raw dataInt
     */
    private static void receivedDataStream(int channel, message msg) {
        int[] data = msg.dataInt;
        if (myFileWriterThread != null) myFileWriterThread.writeToFile(hexUtil.byteToHexString(msg.rawMessage));
        if (channel == 0 && !myGui.jCheckBox_channel0DisplayOrNot.isSelected()) return;
        if (channel == 1 && !myGui.jCheckBox_channel1DisplayOrNot.isSelected()) {
            if (keepAquisitionRunning == true) doAquisition();
            return;
        }
        double dataLenMultiplier = (double) (data.length - 1) / (double) 10;
        if (xCoordinates == null) {
            xCoordinates = new double[data.length];
            for (int i = 0; i < data.length; i++) xCoordinates[i] = ((double) i / dataLenMultiplier) - 5.0;
        }
        if (xCoordinates.length != data.length) {
            logger.logInfo("Data with new Resolution received");
            xCoordinates = null;
            xCoordinates = new double[data.length];
            for (int i = 0; i < data.length; i++) xCoordinates[i] = ((double) i / dataLenMultiplier) - 5.0;
        }
        double xOffsetMultiplier = (double) AdcSamplesPerFrameValue / OffsetXmax;
        if (channel == 0) {
            int positionDiff = 0;
            synchronized (Main.OffsetXLockCh0) {
                positionDiff = (int) (OffsetXCh0 * xOffsetMultiplier * positionXOffsetMultiplier);
            }
            synchronized (Main.yValuesCh0) {
                if (yValuesCh0.length == 0) {
                    yValuesCh0 = null;
                    yValuesCh0 = new double[data.length];
                }
                if (yValuesCh0.length != data.length) {
                    yValuesCh0 = null;
                    yValuesCh0 = new double[data.length];
                }
                int position = 0;
                for (int i = 0; i < data.length; i++) {
                    yValuesCh0[i] = 0;
                }
                for (int i = 0; i < data.length; i++) {
                    position = i + positionDiff;
                    if (position >= 0 && position < data.length) yValuesCh0[position] = ((double) data[i] / (double) 2048);
                }
            }
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    myGui.updateDataCh0();
                }
            });
        } else if (channel == 1) {
            int positionDiff = 0;
            synchronized (Main.OffsetXLockCh1) {
                positionDiff = (int) (OffsetXCh1 * xOffsetMultiplier * positionXOffsetMultiplier);
            }
            synchronized (Main.yValuesCh1) {
                if (yValuesCh1.length == 0) {
                    yValuesCh1 = null;
                    yValuesCh1 = new double[data.length];
                }
                if (yValuesCh1.length != data.length) {
                    yValuesCh1 = null;
                    yValuesCh1 = new double[data.length];
                }
                int position = 0;
                for (int i = 0; i < data.length; i++) {
                    yValuesCh1[i] = 0;
                }
                for (int i = 0; i < data.length; i++) {
                    position = i + positionDiff;
                    if (position >= 0 && position < data.length) yValuesCh1[position] = ((double) data[i] / (double) 2048);
                }
                if (AdcCounterReceivedInChannel1) {
                    for (int j = 0; j < data.length; j++) logger.logDebug("AdcCounterReceivedInChannel1 " + j + "\t" + data[j] + " Cycles for single Aquisition");
                }
            }
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    myGui.updateDataCh1();
                }
            });
        }
        if (checkUndersampling) {
            synchronized (myUnderSamplingCheckerThread.dataArray) {
                if (myUnderSamplingCheckerThread.dataArray.length != data.length) myUnderSamplingCheckerThread.dataArray = new int[data.length];
                for (int i = 0; i < data.length; i++) myUnderSamplingCheckerThread.dataArray[i] = data[i];
                myUnderSamplingCheckerThread.channel = channel;
            }
            synchronized (myUnderSamplingCheckerThread) {
                myUnderSamplingCheckerThread.notify();
            }
        }
        if (channel == 1 && keepAquisitionRunning == true) doAquisition();
    }

    /**
     * @return    A HashSet containing the CommPortIdentifier for all serial ports that are not currently being used.
     */
    public static HashSet<CommPortIdentifier> getAvailableSerialPorts() {
        HashSet<CommPortIdentifier> h = new HashSet<CommPortIdentifier>();
        Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
        while (thePorts.hasMoreElements()) {
            CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
            switch(com.getPortType()) {
                case CommPortIdentifier.PORT_SERIAL:
                    try {
                        CommPort thePort = com.open("CommUtil", 50);
                        thePort.close();
                        h.add(com);
                    } catch (PortInUseException e) {
                        logger.logDebug("Port, " + com.getName() + ", is in use.");
                    } catch (Exception e) {
                        logger.logDebug("Failed to open port " + com.getName());
                        e.printStackTrace();
                    }
            }
        }
        return h;
    }

    public static void sendDeviceRawDataByte(byte[] byteArray) {
        communicator.sendMessage(byteArray);
    }

    private static void receivedSingleVoltValue(int channel, message myMessage) {
        if (myMessage.dataByte.length != 2) {
            logger.logError("SingleDataAquCh" + channel + ", DataByte length != 2 : " + hexUtil.byteToHexString(myMessage.rawMessage));
            return;
        }
        int start = myGui.getAdcLowestVoltageValue();
        int end = myGui.getAdcHighestVoltagetValue();
        int diff = end - start;
        int maxAdcLevel = myGui.getAdcBitValue();
        maxAdcLevel = (int) java.lang.Math.pow(2, maxAdcLevel);
        maxAdcLevel = maxAdcLevel - 1;
        int currentVal = 0;
        try {
            currentVal = hexUtil.byteToUnsignedInt(myMessage.dataByte);
            double divisor = (double) maxAdcLevel / (double) currentVal;
            double toAdd = (double) diff / divisor;
            int toAddInteger = (int) toAdd;
            int result = start + toAddInteger;
            logger.logInfo("singleDataAqCh" + channel + " : " + result + "mV  " + hexUtil.byteToHexString(myMessage.rawMessage));
        } catch (Exception ex) {
            logger.logError("SingleDataAquCh0", ex);
        }
    }

    private static void readXmlParamFile() {
        try {
            Properties properties = new Properties();
            FileInputStream fis = new FileInputStream(settingsFile);
            properties.loadFromXML(fis);
            properties.list(System.out);
            myGui.setSerialPortName(properties.getProperty("defaultComPort"));
            versionString = properties.getProperty("PackageVersion");
            int itemCount = Integer.valueOf(properties.getProperty("defaultSamplesPerFrameModeEntrys"));
            for (int i = 0; i < itemCount; i++) {
                String propertyName;
                if (i < 10) propertyName = "defaultSamplesPerFrameMode0" + i; else propertyName = "defaultSamplesPerFrameMode" + i;
                myGui.jComboBox_Resolution.addItem(properties.getProperty(propertyName));
            }
            fis.close();
        } catch (IOException ex) {
            logger.logError("Error Reading XML Config File: '" + settingsFile + "'", ex);
        }
    }

    public static void debugTimingCheck(String description) {
        if (Main.timingAnalysis) {
            logger.logDebug("Timing: " + description + "\t" + TimeUtil.timingStringNow());
        }
    }

    public static class communicationType {

        public static final int serial = 0;

        public static final int udp = 1;
    }
}
