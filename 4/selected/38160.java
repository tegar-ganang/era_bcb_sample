package de.jlab.communication;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.jlab.util.CommandUtils;

/**
 * Basic Class for all possible Connection Types to the C't Lab.
 * 
 * @author Volker Raum (C) 2007
 */
public abstract class BoardCommunication {

    protected BoardReceiver boardReceiver = null;

    protected Logger stdlog = Logger.getLogger(BoardCommunication.class.getName());

    protected ReceiverThread receiverThread = null;

    Map<Integer, List<NotifierInfo>> pendingWaits = new HashMap<Integer, List<NotifierInfo>>();

    boolean getStatusForCommands = false;

    boolean debugCommunication = false;

    boolean sendCheckSum = false;

    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

    DecimalFormat valueformat = new DecimalFormat("#0.000###", new DecimalFormatSymbols(new Locale("en")));

    String channelName = null;

    /**
    * if a called needs to wait for a reply on his query this method inits the Synchro.
    */
    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    /**
    * if a pending query was answered => this method is called and the waiting process continues.
    */
    public void notifyReceiver(int address, int subchannel, String reply) {
        int id = createPendingId(address, subchannel);
        List<NotifierInfo> pending = null;
        synchronized (pendingWaits) {
            pending = pendingWaits.get(id);
            pendingWaits.remove(id);
        }
        if (pending != null) {
            for (NotifierInfo currWait : pending) {
                currWait.notifiertime = System.currentTimeMillis();
                currWait.setResult(reply);
                synchronized (currWait) {
                    currWait.notify();
                }
                stdlog.finest("Wait notified " + id + " Pending Wait List length = " + pendingWaits.size());
            }
        }
    }

    private Integer createPendingId(int address, int subchannel) {
        return new Integer(10000 * address + subchannel);
    }

    /**
    * send a query for a given address/subchannel and wait for an answer in form of a double
    * 
    * @param address
    * @param subchannel
    * @return
    */
    public double queryDoubleValue(int address, int subchannel) {
        String command = address + ":" + subchannel + "?";
        command = getCommandPlusCheckSum(command);
        String reply = sendCommand(command, address, subchannel);
        return CommandUtils.getDoubleValueFromReply(reply);
    }

    /**
    * send a query for a given address/subchannel and wait for an answer in form of an integer
    * 
    * @param address
    * @param subchannel
    * @return
    */
    public int queryIntegerValue(int address, int subchannel) {
        String command = address + ":" + subchannel + "?";
        command = getCommandPlusCheckSum(command);
        String reply = sendCommand(command, address, subchannel);
        return CommandUtils.getIntegerValueFromReply(reply);
    }

    /**
    * send a query for a given address/subchannel and wait for an answer in form of a long
    * 
    * @param address
    * @param subchannel
    * @return
    */
    public long queryLongValue(int address, int subchannel) {
        String command = address + ":" + subchannel + "?";
        command = getCommandPlusCheckSum(command);
        String reply = sendCommand(command, address, subchannel);
        return CommandUtils.getLongValueFromReply(reply);
    }

    /**
    * send a query for a given address/subchannel and wait for an answer in form of a String
    * 
    * @param address
    * @param subchannel
    * @return
    */
    public String queryStringValue(int address, int subchannel) {
        String command = address + ":" + subchannel + "?";
        command = getCommandPlusCheckSum(command);
        String reply = sendCommand(command, address, subchannel);
        return reply;
    }

    /**
    * send a command to the lab and don not wait for the answer. Since no "!" is given there will be no answer. value is given as an double.
    * 
    * @param address
    * @param subchannel
    * @param value
    * @return
    */
    public void sendCommand(int address, int subchannel, double value) {
        String command = address + ":" + subchannel + "=" + valueformat.format(value) + (getStatusForCommands ? "!" : "");
        command = getCommandPlusCheckSum(command);
        sendCommand(command);
    }

    public void sendCommand(int address, int subchannel, String value) {
        String command = address + ":" + subchannel + "=" + value + (getStatusForCommands ? "!" : "");
        command = getCommandPlusCheckSum(command);
        sendCommand(command);
    }

    String getCommandPlusCheckSum(String originalCommand) {
        String command = originalCommand;
        if (sendCheckSum) {
            byte[] bytes = originalCommand.getBytes();
            int check = 0;
            for (int i = 0; i < bytes.length; ++i) {
                check ^= bytes[i] & 0xff;
            }
            command += "$" + Integer.toHexString(check & 0xff);
        }
        command += "\r\n";
        return command;
    }

    /**
    * query a value but do not wait for the answer. (Mostly used by Value Watches).
    * 
    * @param address
    * @param subchannel
    */
    public void queryValueAsynchronously(int address, int subchannel) {
        String command = getCommandPlusCheckSum(address + ":" + subchannel + "?");
        sendCommand(command);
    }

    /**
    * send a command to the lab and don not wait for the answer. Since no "!" is given there will be no answer. value is given as an integer.
    * 
    * @param address
    * @param subchannel
    * @param value
    * @return
    */
    public void sendCommand(int address, int subchannel, int value) {
        String command = address + ":" + subchannel + "=" + value + (getStatusForCommands ? "!" : "");
        command = getCommandPlusCheckSum(command);
        sendCommand(command);
    }

    /**
    * send a command to the lab and don not wait for the answer. Since no "!" is given there will be no answer. value is given as an integer.
    * 
    * @param address
    * @param subchannel
    * @param value
    * @return
    */
    public void sendCommand(int address, int subchannel, long value) {
        String command = address + ":" + subchannel + "=" + value + (getStatusForCommands ? "!" : "");
        command = getCommandPlusCheckSum(command);
        sendCommand(command);
    }

    /**
    * send a command to the lab and don not wait for the answer. Since no "!" is given there will be no answer. value is given as an integer.
    * 
    * @param address
    * @param subchannel
    * @param value
    * @return
    */
    public void sendCommand(int address, String command) {
        command = address + ":" + command + (getStatusForCommands ? "!" : "");
        command = getCommandPlusCheckSum(command);
        sendCommand(command);
    }

    /**
    * obsolete ?
    * 
    * @param labReply
    */
    protected void decodeReply(String labReply) {
        if (debugCommunication) {
            String debugReply = labReply.replace("\n", "\\n");
            debugReply = debugReply.replace("\r", "\\r");
            System.err.print(sdf.format(System.currentTimeMillis()) + " IN <" + debugReply + ">\n");
        }
        boardReceiver.decodeLabReply(channelName, labReply);
    }

    public void setReceiver(BoardReceiver receiver) {
        this.boardReceiver = receiver;
    }

    protected void informCallerAboutResult(String labReply, int address, int subchannel) {
        this.notifyReceiver(address, subchannel, labReply);
    }

    public void stopReceiver() {
        receiverThread.setContinueToReceive(false);
    }

    public void startReceiverThread() {
        receiverThread = new ReceiverThread(this);
        receiverThread.start();
    }

    /**
    * Send a command and wait for the reply.
    */
    protected synchronized String sendCommand(String command, int address, int subchannel) {
        stdlog.finest("Send Command to Serial " + command);
        String replyFromLab = null;
        try {
            NotifierInfo newNotifier = null;
            Integer id = createPendingId(address, subchannel);
            newNotifier = new NotifierInfo(id);
            newNotifier.setTimestamp(System.currentTimeMillis());
            newNotifier.synctime = System.currentTimeMillis();
            synchronized (pendingWaits) {
                List<NotifierInfo> waitsForID = pendingWaits.get(id);
                if (waitsForID == null) {
                    waitsForID = new ArrayList<NotifierInfo>();
                    pendingWaits.put(id, waitsForID);
                }
                waitsForID.add(newNotifier);
                if (debugCommunication) {
                    String debugReply = command.replace("\n", "\\n");
                    debugReply = debugReply.replace("\r", "\\r");
                    System.err.println(sdf.format(System.currentTimeMillis()) + " OUT <" + debugReply + ">");
                }
                sendToLab(command.getBytes());
            }
            synchronized (newNotifier) {
                try {
                    newNotifier.wait(500);
                } catch (InterruptedException e) {
                }
            }
            replyFromLab = newNotifier.getResult();
        } catch (Exception ex) {
            stdlog.log(Level.SEVERE, "Error sending data : ", ex);
        }
        return replyFromLab;
    }

    /**
    * Send a command and do not wait for the reply.
    */
    public synchronized void sendCommand(String command) {
        stdlog.finest("Send Command to Serial " + command);
        if (debugCommunication) {
            String debugReply = command.replace("\n", "\\n");
            debugReply = debugReply.replace("\r", "\\r");
            System.err.println(sdf.format(System.currentTimeMillis()) + " OUT <" + debugReply + ">");
        }
        try {
            sendToLab(command.getBytes());
        } catch (Exception ex) {
            stdlog.log(Level.SEVERE, "Error sending data : " + ex.toString());
        }
    }

    public String connectAndSendSingleCommand(String singleCommand) throws Exception {
        StringBuilder builder = new StringBuilder();
        try {
            connect();
            if (debugCommunication) System.err.println(sdf.format(System.currentTimeMillis()) + " OUT " + singleCommand);
            sendToLab(singleCommand.getBytes());
            boolean responseComplete = false;
            while (!responseComplete) {
                byte[] buffer = new byte[1024];
                int readBytes = readFromLab(buffer);
                String receivedSequence = new String(buffer, 0, readBytes);
                builder.append(receivedSequence);
                if (receivedSequence.contains("\n")) {
                    responseComplete = true;
                } else {
                    Thread.sleep(100);
                }
            }
            disconnect();
        } catch (Exception e) {
            stdlog.log(Level.SEVERE, "Error in Sending single Command", e);
        }
        return builder.toString();
    }

    /**
    * Read bytes from the Lab. This method must be blocking until data is received
    * 
    * @param copy received bytes into this array
    * @return the number of received bytes
    */
    protected abstract int readFromLab(byte[] buffer);

    /**
    * send bytes to the lab
    * 
    * @param buffer the bytes to transfer
    */
    protected abstract void sendToLab(byte[] buffer);

    /**
    * establish a connection with the lab
    * 
    * @throws Exception
    */
    public abstract void connect() throws Exception;

    /**
    * disconnect from the lab.
    */
    public abstract void disconnect();

    /**
    * initialize the connection interface with necessary parameters (hostname, port etc.) This does NOT cause a connect !!
    * 
    * @param parameters
    */
    public abstract void initByParameters(HashMap<String, String> parameters);

    public boolean isCommandConfirmation() {
        return getStatusForCommands;
    }

    public void setCommandConfirmation(boolean getStatusForCommands) {
        this.getStatusForCommands = getStatusForCommands;
    }

    public boolean isCommandProtocol() {
        return debugCommunication;
    }

    public void setCommandProtocol(boolean debugCommunication) {
        this.debugCommunication = debugCommunication;
    }

    public boolean isSendCheckSum() {
        return sendCheckSum;
    }

    public void setSendCheckSum(boolean sendCheckSum) {
        this.sendCheckSum = sendCheckSum;
    }
}

class NotifierInfo {

    public int resultId;

    public String result;

    long timestamp;

    public long synctime;

    public long notifiertime;

    private static long counter = 0;

    public long count;

    public int getResultId() {
        return resultId;
    }

    public void setResultId(int resultId) {
        this.resultId = resultId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public NotifierInfo(int resultId) {
        super();
        this.resultId = resultId;
        count = incCounter();
    }

    synchronized long incCounter() {
        counter++;
        return counter;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

class Incoming {

    public long timestamp;

    public int addressId;

    public long counter;

    public Incoming(long timestamp, int addressId, long counter) {
        super();
        this.timestamp = timestamp;
        this.addressId = addressId;
        this.counter = counter;
    }
}
