package br.unb.unbiquitous.ubiquitos.uos.integration;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ResourceBundle;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ChannelManager;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ConnectionManager;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ConnectionManagerListener;
import br.unb.unbiquitous.ubiquitos.network.exceptions.NetworkException;
import br.unb.unbiquitous.ubiquitos.network.model.NetworkDevice;
import br.unb.unbiquitous.ubiquitos.network.model.connection.ClientConnection;

public class IntegrationConnectionManager implements ConnectionManager {

    private IntegrationDevice device;

    private ConnectionManagerListener connectionListener;

    private static final CM channelMng = new CM();

    private static class CM implements ChannelManager {

        IntegrationDevice pc;

        PipedInputStream cellIn;

        PipedOutputStream cellOut;

        ClientConnection toPcConn;

        IntegrationConnectionManager pcManager;

        IntegrationDevice cell;

        PipedInputStream pcIn;

        PipedOutputStream pcOut;

        ClientConnection toCellConn;

        IntegrationConnectionManager cellManager;

        private void loadStreams() throws IOException {
            cellOut = new PipedOutputStream();
            pcOut = new PipedOutputStream();
            cellIn = new PipedInputStream(pcOut);
            pcIn = new PipedInputStream(cellOut);
        }

        public CM() {
            try {
                loadStreams();
                pc = new IntegrationDevice("my.pc");
                cell = new IntegrationDevice("my.cell");
                toPcConn = new ClientConnection(pc) {

                    public DataOutputStream getDataOutputStream() throws IOException {
                        return new DataOutputStream(cellOut);
                    }

                    public DataInputStream getDataInputStream() throws IOException {
                        return new DataInputStream(cellIn);
                    }

                    public void closeConnection() throws IOException {
                        loadStreams();
                    }
                };
                toCellConn = new ClientConnection(cell) {

                    public DataOutputStream getDataOutputStream() throws IOException {
                        return new DataOutputStream(pcOut);
                    }

                    public DataInputStream getDataInputStream() throws IOException {
                        return new DataInputStream(pcIn);
                    }

                    public void closeConnection() throws IOException {
                        System.out.println("Reload Stream");
                        loadStreams();
                    }
                };
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void tearDown() throws NetworkException, IOException {
        }

        public ClientConnection openPassiveConnection(String networkDeviceName) throws NetworkException, IOException {
            return null;
        }

        public ClientConnection openActiveConnection(String networkDeviceName) throws NetworkException, IOException {
            if (networkDeviceName.equals("my.pc")) {
                System.out.println(">>>> : Calling PC");
                pcManager.connectionListener.handleClientConnection(toCellConn);
                return toPcConn;
            } else if (networkDeviceName.equals("my.cell")) {
                System.out.println(">>>> : Calling Cell");
                cellManager.connectionListener.handleClientConnection(toPcConn);
                return toCellConn;
            }
            System.out.println(">>>> : Calling NoOne");
            return null;
        }

        public NetworkDevice getAvailableNetworkDevice() {
            return new IntegrationDevice("non.existant");
        }
    }

    @Override
    public void run() {
    }

    @Override
    public void setConnectionManagerListener(ConnectionManagerListener connectionManagerListener) {
        this.connectionListener = connectionManagerListener;
    }

    @Override
    public void setResourceBundle(ResourceBundle bundle) {
        String deviceName = bundle.getString("ubiquitos.uos.deviceName");
        if (deviceName.equals("my.pc")) {
            device = channelMng.pc;
            channelMng.pcManager = this;
        } else if (deviceName.equals("my.cell")) {
            device = channelMng.cell;
            channelMng.cellManager = this;
        } else {
            device = null;
        }
    }

    @Override
    public void tearDown() {
    }

    @Override
    public NetworkDevice getNetworkDevice() {
        return device;
    }

    @Override
    public ChannelManager getChannelManager() {
        return channelMng;
    }
}
