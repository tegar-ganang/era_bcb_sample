package channel.master;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class MasterChannelInterface extends Thread {

    private Socket socket;

    private ArrayList<MasterChannelItem> sockets;

    private String motd;

    public MasterChannelInterface(Socket socket, ArrayList<MasterChannelItem> sockets, String motd) {
        this.socket = socket;
        this.sockets = sockets;
        this.motd = motd;
    }

    @Override
    public void run() {
        takeInput();
    }

    private void takeInput() {
        try {
            System.out.println(socket.getInetAddress() + ":" + socket.getPort());
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
            while (true) {
                String requestMessageLine = inFromClient.readLine();
                System.out.println(requestMessageLine);
                if (requestMessageLine != null) {
                    if (requestMessageLine.equals("GETCHANNELS")) {
                        getChannels(outToClient);
                    } else if (requestMessageLine.startsWith("ADDCHANNEL ")) {
                        addChannel(requestMessageLine.substring(11), outToClient);
                    } else if (requestMessageLine.startsWith("SETSERVERS ")) {
                        setServerCount(requestMessageLine.substring(11));
                    } else if (requestMessageLine.startsWith("SETMOTD ")) {
                        setServerMOTD(requestMessageLine.substring(8));
                    } else if (requestMessageLine.startsWith("GETMOTD")) {
                        getMOTD(outToClient);
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getChannels(DataOutputStream outToClient) throws IOException {
        String servers = String.valueOf(sockets.size()).concat("\r\n");
        for (int x = 0; x < sockets.size(); x++) {
            servers = servers.concat(sockets.get(x).getIP().concat(" `".concat(sockets.get(x).getChannelName().concat("`".concat(" `".concat(sockets.get(x).getMOTD().concat("`".concat(" `".concat(String.valueOf(sockets.get(x).getServerCount()).concat("`".concat("\r\n")))))))))));
        }
        outToClient.writeBytes(servers);
    }

    private void getMOTD(DataOutputStream outToClient) throws IOException {
        outToClient.writeBytes(motd);
    }

    private void addChannel(String channelDets, DataOutputStream outToClient) throws IOException {
        boolean readingField = false;
        String field = "";
        ArrayList<String> fields = new ArrayList<String>();
        for (int x = 0; x < channelDets.length(); x++) {
            if (channelDets.charAt(x) == '`') {
                if (readingField) {
                    readingField = false;
                    fields.add(field);
                    field = "";
                } else {
                    readingField = true;
                }
            } else {
                if (readingField) {
                    field = field + channelDets.charAt(x);
                }
            }
        }
        if (fields.size() == 3) {
            boolean alreadyExists = false;
            for (int x = 0; x < sockets.size() & !alreadyExists; x++) {
                if (sockets.get(x).getIP().equals(socket.getInetAddress().getHostAddress())) {
                    alreadyExists = true;
                }
            }
            if (!alreadyExists) {
                try {
                    MasterChannelItem item = new MasterChannelItem(socket, fields.get(0), fields.get(1));
                    item.setServerCount(Integer.parseInt(fields.get(2)));
                    sockets.add(item);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    MasterChannelItem item = new MasterChannelItem(socket, fields.get(0), fields.get(1));
                    sockets.add(item);
                }
            }
        }
        outToClient.writeBytes(true + "\r\n");
    }

    private void setServerCount(String num) {
        boolean match = false;
        for (int x = 0; x < sockets.size() && !match; x++) {
            if (sockets.get(x).isSameIP(socket.getInetAddress().toString().substring(1))) {
                match = true;
                try {
                    int servers = Integer.parseInt(num);
                    System.out.println("Servers: " + servers);
                    sockets.get(x).setServerCount(servers);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setServerMOTD(String motd) {
        boolean match = false;
        for (int x = 0; x < sockets.size() && !match; x++) {
            System.out.println(sockets.get(x).getIP());
            System.out.println(socket.getInetAddress().toString().substring(1));
            if (sockets.get(x).isSameIP(socket.getInetAddress().toString().substring(1))) {
                match = true;
                try {
                    System.out.println("MOTD set: " + motd);
                    sockets.get(x).setMOTD(motd);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
