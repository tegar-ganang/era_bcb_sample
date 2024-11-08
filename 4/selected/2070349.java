package edu.ucsd.osdt.daq;

import java.net.*;
import java.io.*;
import java.lang.String.*;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ControlPort {

    private BufferedReader rd;

    private BufferedWriter wr;

    private boolean connected;

    static Log log = LogFactory.getLog(ControlPort.class.getName());

    public ControlPort(Socket socket) throws UnknownHostException, IOException {
        rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        connected = true;
        log.info("Created NWP Control Port");
    }

    public synchronized String readFromControl() throws IOException {
        String retval = "";
        retval = rd.readLine();
        log.debug("Read \"" + retval + "\" from daq control");
        return retval;
    }

    public synchronized void writeToControl(String commandToWrite) throws IOException {
        wr.write(commandToWrite + '\n');
        wr.flush();
        log.debug("Wrote \"" + commandToWrite + "\" to daq control");
    }

    public synchronized String writeReadControl(String commandToWrite) throws IOException {
        String retval = "";
        wr.write(commandToWrite + '\n');
        wr.flush();
        retval = rd.readLine();
        return retval;
    }

    public String[] getNwpUnits(String[] channelList) throws IOException {
        String unitCmd = "get-channel-info ";
        int listLengthAdjustment = 2;
        String[] retval = new String[channelList.length];
        log.debug("Requesting NWP unit list");
        String[] nwpResponse = null;
        for (int i = 0; i < channelList.length - listLengthAdjustment; i++) {
            nwpResponse = writeReadControl(unitCmd + channelList[i]).split(",");
            retval[i] = nwpResponse[6];
        }
        retval[retval.length - listLengthAdjustment] = "string";
        retval[retval.length - listLengthAdjustment + 1] = "string";
        return retval;
    }

    public void getSensorMetadata(String[] channelList, Hashtable<String, String> sensType, Hashtable<String, String> measType) throws IOException {
        String unitCmd = "get-channel-info ";
        int listLengthAdjustment = 2;
        String[] nwpResponse = null;
        for (int i = 0; i < channelList.length - listLengthAdjustment; i++) {
            nwpResponse = writeReadControl(unitCmd + channelList[i]).split(",");
            sensType.put(channelList[i], nwpResponse[2] + " " + nwpResponse[3]);
            measType.put(channelList[i], nwpResponse[5]);
        }
    }

    public String[] getUnits() throws IOException {
        String list_command = "list-units";
        String delimiter = ",\t";
        String raw_string = "";
        String[] result;
        Vector units = new Vector();
        if (!connected) {
            throw new IOException("NWP Not connected.");
        }
        try {
            log.debug("Requesting NWP unit list");
            raw_string = writeReadControl(list_command + '\n');
        } catch (IOException ioe) {
            log.error("communicating with daq control channel" + ioe);
            ioe.printStackTrace();
        }
        StringTokenizer tokens = new StringTokenizer(raw_string, delimiter);
        String tok;
        while (tokens.hasMoreTokens()) {
            tok = tokens.nextToken();
            while ((tok.length() > 1) && (tok.startsWith(" "))) tok = tok.substring(1);
            while ((tok.length() > 1) && (tok.endsWith(" "))) tok = tok.substring(0, tok.length() - 1);
            units.addElement(tok);
        }
        result = new String[units.size()];
        for (int i = 0; i < units.size(); i++) result[i] = (String) units.elementAt(i);
        return (result);
    }

    public String[] getChannels() throws IOException {
        String list_command = "list-channels";
        String delimiter = ",";
        String raw_list = "";
        String processed_list = "";
        String[] result = new String[0];
        Vector channels = new Vector();
        if (!connected) {
            throw new IOException("NWP Not connected.");
        }
        try {
            log.debug("Requesting NWP channel list");
            raw_list = writeReadControl(list_command + '\n');
            processed_list = raw_list.split(":")[2].trim();
        } catch (IOException ioe) {
            log.error("Communicating with daq control channel" + ioe);
            ioe.printStackTrace();
        }
        StringTokenizer tokens = new StringTokenizer(processed_list, delimiter);
        String tok;
        while (tokens.hasMoreTokens()) {
            tok = tokens.nextToken();
            channels.addElement(tok);
        }
        result = new String[channels.size()];
        for (int i = 0; i < channels.size(); i++) result[i] = (String) channels.elementAt(i);
        return (result);
    }

    private void subUnsub(String channel, boolean subscribe) throws IOException {
        String[] commands = { "open-port", "close-port" };
        String[] responses = { "Streaming", "Stopping" };
        String[] stdout = { "Subscribing to", "Unsubscribing from" };
        int cmd_idx;
        if (subscribe) cmd_idx = 0; else cmd_idx = 1;
        if (!connected) {
            throw new IOException("NWP not connected");
        }
        String response = "No Response";
        try {
            response = writeReadControl(commands[cmd_idx] + " " + channel + "\n");
        } catch (IOException ioe) {
            log.error("Communicating with daq control channel" + ioe);
            ioe.printStackTrace();
        }
        if (response != null && response.startsWith(responses[cmd_idx])) {
            return;
        } else {
            throw new IOException("Response Error on channel " + channel + " :" + response);
        }
    }

    public void subscribe(String channel) throws IOException {
        subUnsub(channel, true);
    }

    public void unsubscribe(String channel) throws IOException {
        subUnsub(channel, false);
    }
}
