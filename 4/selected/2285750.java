package channel.master;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;

/**
 *
 * @author Michael Hanns
 *
 */
class MasterChannelItem {

    private Socket socket;

    private String channelName;

    private int currentServers;

    private String motd;

    public MasterChannelItem(Socket s, String channelName, String motd) {
        this.socket = s;
        this.channelName = channelName;
        this.currentServers = 0;
        this.motd = "";
    }

    public String getChannelName() {
        return channelName;
    }

    public void setMOTD(String motd) {
        this.motd = motd;
    }

    public String getMOTD() {
        return motd;
    }

    public String getIP() {
        if (socket.getInetAddress().getHostAddress().equals("127.0.0.1")) {
            try {
                URL whatismyip = new URL("http://automation.whatismyip.com/n09230945.asp");
                InputStream stream = whatismyip.openStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(stream));
                String ip = in.readLine();
                stream.close();
                in.close();
                if (ip != null && ip.length() > 0) {
                    return ip;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return socket.getInetAddress().getHostAddress();
    }

    public boolean isSameIP(String ip) {
        if (socket.getInetAddress().getHostAddress().equals(ip)) {
            return true;
        }
        return false;
    }

    public int getServerCount() {
        return currentServers;
    }

    public void setServerCount(int x) {
        currentServers = x;
    }

    public boolean isConnected() {
        return socket.isConnected();
    }
}
