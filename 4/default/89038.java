import java.io.*;
import java.net.*;
import java.util.Random;

/**The GnubertServer listens for connection requests from other Gnubert clients and spawns server threads to handle those connections.
  */
public class GnubertServer extends Thread {

    private GnubertProperties propertyList;

    private int numStrings;

    String commandStrings[];

    public int maxIncoming = 5;

    public static int currentIncoming;

    HostCatcher hostCatcher = null;

    GnubertStatus currentStatus = null;

    public GnubertServer(String strings[], int nstrings, GnubertProperties _propertyList, HostCatcher hosts, GnubertStatus _currentStatus) {
        propertyList = _propertyList;
        commandStrings = strings;
        numStrings = nstrings;
        hostCatcher = hosts;
        currentStatus = _currentStatus;
    }

    public void run() {
        ServerSocket mainServer = null;
        Socket incoming = null;
        try {
            mainServer = new ServerSocket(propertyList.getPortNumber());
        } catch (IOException e) {
            System.out.println(e);
        }
        while (true) {
            try {
                incoming = mainServer.accept();
                if (currentIncoming < maxIncoming) {
                    currentIncoming++;
                    IncomingConnectionHandler connection = new IncomingConnectionHandler(incoming);
                    connection.start();
                } else {
                    DataOutputStream outStream = new DataOutputStream(incoming.getOutputStream());
                    outStream.writeBytes("Sorry, maximum incoming connections (" + maxIncoming + ") already reached!\n");
                    outStream = null;
                    incoming.close();
                }
            } catch (Exception ei) {
                ei.printStackTrace();
            }
        }
    }

    class IncomingConnectionHandler extends Thread {

        protected BufferedReader inStream;

        protected DataOutputStream outStream;

        protected Socket incomingSocket;

        protected boolean _dirty = true;

        public IncomingConnectionHandler(Socket i) {
            incomingSocket = i;
            try {
                inStream = new BufferedReader(new InputStreamReader(incomingSocket.getInputStream()));
                outStream = new DataOutputStream(incomingSocket.getOutputStream());
            } catch (Exception ei) {
                cleanup();
            }
        }

        public void run() {
            int port;
            try {
                String command = inStream.readLine();
                if (command.equals("get hosts")) {
                    sendHosts();
                } else if (command.equals("get status")) {
                    outStream.writeBytes(currentStatus.toString() + "\n");
                } else if (command.startsWith("GET /index.html")) {
                    webRequest();
                } else if (command.equals("get operator")) {
                    operator();
                } else {
                    try {
                        port = Integer.valueOf(command).intValue();
                        persistentConnection(port);
                    } catch (NumberFormatException e) {
                        outStream.writeBytes("I don't understand: " + command + "\n");
                    }
                }
            } catch (Exception ei) {
            }
            cleanup();
        }

        /** function which provides a connection to other Gnubert clients
      */
        protected void persistentConnection(int port) {
            Host iHost;
            byte ipInBytes[];
            String ipAddress = incomingSocket.getInetAddress().getHostAddress();
            iHost = (new Host(ipAddress, port));
            hostCatcher.addHost(iHost);
            currentStatus.acceptedClient(iHost);
            try {
                outStream.writeBytes("OK\n");
            } catch (java.io.IOException E) {
                cleanup();
            }
            String command = null;
            try {
                command = inStream.readLine();
                while (!command.equals("goodbye")) {
                    if (command.equals("get string")) {
                        outStream.writeBytes(getCmdString() + "\n");
                    } else {
                        outStream.writeBytes("I don't understand: " + command + "\n");
                    }
                    command = inStream.readLine();
                }
            } catch (Exception e) {
            }
            currentStatus.lostClient(iHost);
        }

        /**handles requests for operator access
      */
        protected void operator() {
            String command;
            try {
                outStream.writeBytes("Password: ");
                command = inStream.readLine();
                while (!command.equals("goodbye")) {
                    command = inStream.readLine();
                }
            } catch (java.io.IOException E) {
                cleanup();
            }
        }

        /**Function which chooses a command string and returns it.
    */
        String getCmdString() {
            Random rand = new Random();
            int index = (int) (rand.nextDouble() * numStrings);
            return commandStrings[index];
        }

        /**Sends webpage which will eventually contain the control applet to the client.
    */
        void webRequest() {
            String[] indexHtml = { "<html>", "  <title>Gnubert Distributed P2P Evolution Client Control Page</title>", "..<body bgcolor=#1D7AAD>", "    <center><H1>Gnubert Distributed Evolution Client Control Page</H1></center>", "    <p> &nbsp &nbsp This page will eventually contain an embedded Java applet which displays progress,", "    connection information and serves as a control panel.  It will be runnable as an application", "    and maybe using a screen saver wrapper as well.</p>", "    <pre>" + currentStatus.toString() + "</pre>", "  </body>", "</html>" };
            try {
                for (int counter = 0; counter < indexHtml.length; counter++) {
                    outStream.writeBytes(indexHtml[counter] + "\n");
                }
            } catch (Exception e) {
            }
        }

        /**sends the list of hosts from the host catcher to the client
      */
        protected void sendHosts() {
            try {
                outStream.writeBytes("Host List\n");
                outStream.writeBytes(hostCatcher.getHosts().toString() + '\n');
            } catch (Exception e) {
            }
        }

        /**Closes data streams, the socket, and decrements the open connection counter
      */
        protected void cleanup() {
            _dirty = false;
            try {
                inStream.close();
                outStream.close();
                incomingSocket.close();
            } catch (IOException ic) {
                ic.printStackTrace();
            }
            currentIncoming--;
        }

        protected void finalize() {
            if (_dirty) {
                cleanup();
            }
        }
    }
}
