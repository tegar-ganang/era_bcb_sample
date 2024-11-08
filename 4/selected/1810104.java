package channel.master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import channel.Channel;
import colour.Colour;
import console.IOBuffer;

public class MasterChannelImp extends Thread implements Channel {

    private double version;

    private ServerSocket ss;

    private int port = 725;

    private MasterChannelServer listener;

    private ArrayList<MasterChannelItem> sockets;

    private boolean running;

    private IOBuffer terminal;

    private String motd;

    private String adminName;

    private String adminPass;

    public MasterChannelImp(String adminName, String adminPass, IOBuffer t) {
        terminal = t;
        version = 0.7;
        sockets = new ArrayList<MasterChannelItem>();
        motd = "";
        this.adminName = adminName;
        this.adminPass = adminPass;
    }

    @Override
    public void run() {
        running = true;
        try {
            terminal.writeTo("\nStarting master channel...\n");
            ss = new ServerSocket(port);
            terminal.writeTo(Colour.colourise("\nWITNA Channel v" + version, Colour.cyan));
            terminal.writeTo(Colour.colourise("\n-----", Colour.white));
            terminal.writeTo(Colour.colourise("\n-", Colour.white) + Colour.colourise("Channel     : ", Colour.cyan) + Colour.colourise("MASTER", Colour.yellow));
            terminal.writeTo(Colour.colourise("\n-", Colour.white) + Colour.colourise("Global ip   : ", Colour.cyan));
            String globalip = getGlobalIP();
            String localip = getLocalIP();
            terminal.writeTo(Colour.colourise(globalip + ":" + port, Colour.yellow));
            terminal.writeTo(Colour.colourise("\n-", Colour.white) + Colour.colourise("Local  ip   : ", Colour.cyan) + Colour.colourise(localip + ":" + port, Colour.yellow));
            String status = getNetStatus(globalip, localip);
            terminal.writeTo(Colour.colourise("\n-", Colour.white) + Colour.colourise("Connection  : ", Colour.cyan) + Colour.colourise(status, Colour.yellow) + Colour.colourise("\n", Colour.grey));
            listener = new MasterChannelServer(ss, sockets, motd);
            listener.start();
            int cycles = 0;
            while (running) {
                for (int x = 0; x < sockets.size(); x++) {
                    if (!sockets.get(x).isConnected()) {
                        sockets.remove(x);
                    }
                }
                boolean incorrectPass = false;
                int pass = 0;
                while (!incorrectPass && !UpdateChannelList.update(adminName, adminPass, getChannelString(), globalip)) {
                    Thread.sleep(5000);
                    terminal.writeTo("\nConnection error: trying again..");
                    if (pass == 3) {
                        incorrectPass = true;
                        terminal.writeTo("\nConnection failed three times in a row." + " Username and/or password might be bad.");
                    }
                }
                if (!incorrectPass) {
                    terminal.writeTo("\nChannel listing updated.");
                }
                Thread.sleep(25000);
                cycles++;
                if (cycles == 15) {
                    cycles = 0;
                    String newip = getGlobalIP();
                    if (!newip.equals(globalip)) {
                        globalip = newip;
                        terminal.writeTo("\nGlobal IP has changed. Your new IP address is " + globalip + ".");
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processLocalCommand(String input) {
        if (input.equalsIgnoreCase("DISCONNECT") || input.equalsIgnoreCase("QUIT")) {
            quit();
        } else if (input.toUpperCase().startsWith("MOTD")) {
            if (input.length() == 4) {
                terminal.writeTo("\nSet the Message of the Day to what?");
            } else if (input.length() == 5 && input.charAt(4) == ' ') {
                terminal.writeTo("\nSet the Message of the Day to what?");
            } else if (input.length() > 5 && input.charAt(4) == ' ') {
                setMOTD(input.substring(5));
            }
        } else {
            terminal.writeTo("\nMaster channel command not recognised.");
        }
    }

    private void setMOTD(String motd) {
        this.motd = motd;
        if (listener != null) {
            listener.updateMOTD(motd);
        }
        terminal.writeTo("\nMessage of the Day set to: " + motd);
    }

    public void quit() {
        try {
            ss.close();
            ss = null;
            listener.interrupt();
            listener.quit();
            running = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getChannelString() throws IOException {
        String channels = String.valueOf(System.currentTimeMillis()).concat("\n".concat(String.valueOf(sockets.size())).concat("\n"));
        for (int x = 0; x < sockets.size(); x++) {
            channels = channels.concat(sockets.get(x).getIP().concat(" `".concat(sockets.get(x).getChannelName().concat("`".concat(" `".concat(sockets.get(x).getMOTD().concat("`".concat(" `".concat(String.valueOf(sockets.get(x).getServerCount()).concat("`".concat("\n")))))))))));
        }
        return channels;
    }

    private String getGlobalIP() {
        try {
            URL whatismyip = new URL("http://automation.whatismyip.com/n09230945.asp");
            InputStream stream = whatismyip.openStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            String ip = in.readLine();
            stream.close();
            in.close();
            if (ip != null && ip.length() > 0) {
                return ip;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    private String getLocalIP() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    private String getNetStatus(String globalip, String localip) {
        if (!globalip.equals("127.0.0.1")) {
            return "ONLINE";
        } else {
            if (!localip.equals("127.0.0.1")) {
                return "NETWORK";
            } else {
                return "OFFLINE";
            }
        }
    }
}
