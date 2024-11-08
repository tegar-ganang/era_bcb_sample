package channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.StringTokenizer;

/**
 *
 * @author Michael Hanns
 *
 */
public class GrabChannelData {

    public static String[][] getChannelData(String ipStr) {
        try {
            InetAddress ip = InetAddress.getByName(ipStr);
            Socket socket = new Socket(ip, 724);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("GETSERVERS");
            String line = in.readLine();
            if (line == null) {
                return null;
            } else if (line.equals("0")) {
                return new String[0][8];
            } else if (isInteger(line)) {
                System.out.println(line);
                int numServers = Integer.parseInt(line);
                String[][] servers = new String[numServers][8];
                for (int x = 0; x < numServers; x++) {
                    String chanData = in.readLine();
                    if (chanData != null && isChannelDataValid(chanData)) {
                        System.out.println("Channel data valid.");
                        servers[x][0] = getServerIP(chanData);
                        servers[x][1] = getServerPort(chanData);
                        for (int y = 2; y < 8; y++) {
                            servers[x][y] = readQuotes(chanData, y);
                            System.out.println(servers[x][y]);
                        }
                    } else {
                        System.out.println("Channel data invalid.");
                        System.out.println(chanData);
                    }
                }
                System.out.println("Channel data reading complete.");
                return servers;
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getServerIP(String chanData) {
        return chanData.split(" ")[0];
    }

    private static String getServerPort(String chanData) {
        return chanData.split(" ")[1];
    }

    private static String readQuotes(String chanData, int quoteNo) {
        if (quoteNo > 8 || quoteNo < 1) {
            return "";
        }
        StringTokenizer tkns = new StringTokenizer(chanData);
        tkns.nextToken();
        tkns.nextToken();
        int currentQuote = 2;
        boolean openQuote = false;
        String elem = "";
        while (tkns.hasMoreTokens()) {
            String token = tkns.nextToken();
            if (currentQuote != quoteNo) {
                if (!openQuote && token.startsWith("`")) {
                    openQuote = true;
                }
                if (openQuote && token.endsWith("`")) {
                    currentQuote++;
                    openQuote = false;
                }
            } else if (currentQuote == quoteNo) {
                if (!openQuote) {
                    if (token.startsWith("`")) {
                        openQuote = true;
                        elem = token.substring(1);
                        if (token.endsWith("`")) {
                            openQuote = false;
                            elem = elem.substring(0, token.length() - 2);
                            return elem;
                        }
                    } else if (token.endsWith("`")) {
                        openQuote = false;
                        elem += " " + elem.substring(0, token.length() - 2);
                        return elem;
                    }
                } else if (openQuote) {
                    if (token.endsWith("`")) {
                        openQuote = false;
                        if (elem.length() > 1) {
                            elem += " " + token.substring(0, token.length() - 1);
                        }
                        return elem;
                    } else {
                        elem += " " + token;
                    }
                }
            }
        }
        return "";
    }

    private static boolean isChannelDataValid(String chanData) {
        int quoteCount = 0;
        for (int x = 0; x < chanData.length(); x++) {
            if (chanData.charAt(x) == '`') {
                quoteCount++;
            }
        }
        if (quoteCount == 12) {
            return true;
        }
        System.out.println("Quote count:" + quoteCount);
        return false;
    }

    public static String getDefaultChannelIP() {
        try {
            URL whatismyip = new URL("http://www.witna.co.uk/chanData/officialIP.html");
            InputStream stream = whatismyip.openStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(stream));
            String ip = in.readLine();
            stream.close();
            in.close();
            if (ip != null) {
                return ip;
            }
            return "Can't get master channel ip!";
        } catch (IOException e) {
            e.printStackTrace();
            return "Can't get master channel ip!";
        }
    }

    private static boolean isInteger(String intStr) {
        try {
            Integer.parseInt(intStr);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        getChannelData("127.0.0.1");
    }
}
