package org.nees.daq;

import java.net.*;
import java.io.*;
import java.lang.String.*;
import java.util.StringTokenizer;
import java.util.Vector;

public class ControlPort {

    private BufferedReader rd;

    private BufferedWriter wr;

    private boolean connected;

    public ControlPort(Socket socket) throws UnknownHostException, IOException {
        rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        connected = true;
        System.out.println("Created DAQ Control Port");
    }

    public String[] getUnits() throws IOException {
        String list_command = "list-units";
        String delimiter = ",\t";
        String raw_string = "";
        String[] result;
        Vector units = new Vector();
        if (!connected) {
            throw new IOException("DAQ Not connected.");
        }
        System.out.println("Requesting DAQ unit list");
        wr.write(list_command + '\n');
        wr.flush();
        raw_string = rd.readLine();
        System.out.println("For units, got: >>" + raw_string + "<<");
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

    public String[] getUpperbounds() throws IOException {
        String list_command = "list-upperbounds";
        String delimiter = ",\t";
        String raw_string = "";
        String[] result;
        Vector ubs = new Vector();
        if (!connected) {
            throw new IOException("DAQ Not connected.");
        }
        System.out.println("Requesting DAQ upperbound list");
        wr.write(list_command + '\n');
        wr.flush();
        raw_string = rd.readLine();
        System.out.println("For upperbounds, got: >>" + raw_string + "<<");
        StringTokenizer tokens = new StringTokenizer(raw_string, delimiter);
        String tok;
        while (tokens.hasMoreTokens()) {
            tok = tokens.nextToken();
            while ((tok.length() > 1) && (tok.startsWith(" "))) tok = tok.substring(1);
            while ((tok.length() > 1) && (tok.endsWith(" "))) tok = tok.substring(0, tok.length() - 1);
            try {
                Float f = Float.valueOf(tok.trim());
                ubs.addElement(tok);
            } catch (NumberFormatException nfe) {
                System.out.println("Invalid upperbound value.");
            }
        }
        result = new String[ubs.size()];
        for (int i = 0; i < ubs.size(); i++) result[i] = (String) ubs.elementAt(i);
        return (result);
    }

    public String[] getLowerbounds() throws IOException {
        String list_command = "list-lowerbounds";
        String delimiter = ",\t";
        String raw_string = "";
        String[] result;
        Vector lbs = new Vector();
        if (!connected) {
            throw new IOException("DAQ Not connected.");
        }
        System.out.println("Requesting DAQ lowerbound list");
        wr.write(list_command + '\n');
        wr.flush();
        raw_string = rd.readLine();
        System.out.println("For lowerbounds, got: >>" + raw_string + "<<");
        StringTokenizer tokens = new StringTokenizer(raw_string, delimiter);
        String tok;
        while (tokens.hasMoreTokens()) {
            tok = tokens.nextToken();
            while ((tok.length() > 1) && (tok.startsWith(" "))) tok = tok.substring(1);
            while ((tok.length() > 1) && (tok.endsWith(" "))) tok = tok.substring(0, tok.length() - 1);
            try {
                Float f = Float.valueOf(tok.trim());
                lbs.addElement(tok);
            } catch (NumberFormatException nfe) {
                System.out.println("Invalid lowerbound value.");
            }
        }
        result = new String[lbs.size()];
        for (int i = 0; i < lbs.size(); i++) result[i] = (String) lbs.elementAt(i);
        return (result);
    }

    public String[] getChannels() throws IOException {
        String list_command = "list-channels";
        String delimiter = ",";
        String raw_list = "";
        String[] result = new String[0];
        Vector channels = new Vector();
        if (!connected) {
            throw new IOException("DAQ Not connected.");
        }
        System.out.println("Requesting DAQ channel list");
        wr.write(list_command + '\n');
        wr.flush();
        raw_list = rd.readLine();
        System.out.println("For Channels, got: " + raw_list);
        StringTokenizer tokens = new StringTokenizer(raw_list, delimiter);
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
            throw new IOException("DAQ not connected");
        }
        wr.write(commands[cmd_idx] + " " + channel + "\n");
        wr.flush();
        String response = rd.readLine();
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
