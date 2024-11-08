package ddbserver.common;

import ddbserver.DDBServerView;
import ddbserver.connections.SiteManager;
import ddbserver.connections.SocketConnector;
import ddbserver.constant.Constant;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 *
 * @author Roar QiXiao
 */
public class Site {

    String ip;

    int port;

    String name;

    SocketConnector socketConnector;

    boolean connected = false;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public Site(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public Site(String ipport) throws Exception {
        String[] sp = ipport.trim().split("[:\\s]");
        this.ip = sp[0];
        this.port = Integer.parseInt(sp[1]);
    }

    public Site(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public String getIP() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIP(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int connect() {
        try {
            newSession();
            send("client");
            send("echo");
            name = read();
            send(DDBServerView.getInstance().getSite());
            connected = true;
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            connected = false;
        }
        return 1;
    }

    public int connect(String purpose) {
        try {
            newSession(purpose);
            connected = true;
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            connected = false;
        }
        return 1;
    }

    public int send(String text) {
        return socketConnector.send(text);
    }

    public void sendObject(Object object) {
        SQLResult sqlResult = (SQLResult) object;
        newSession(Constant.DDB_SOCKET_STRING);
        send("client");
        send("object");
        send(sqlResult.getSqlType());
        if (sqlResult.getSqlType().equals(Constant.REMOTE_COMMAND_NONRESULT)) {
            send(sqlResult.getSql());
            return;
        } else if (sqlResult.getSqlType().equals(Constant.REMOTE_COMMAND_RESULTE)) {
            try {
                Integer objectPort = Integer.parseInt(read());
                SocketConnector objectSender = new SocketConnector(ip, objectPort, Constant.DDB_SOCKET_OBJECT);
                objectSender.sendObject(object);
                objectSender.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public int sendFile(File file) {
        return socketConnector.sendFile(file);
    }

    public String read() {
        return socketConnector.read();
    }

    @Override
    public String toString() {
        return (ip + ":" + port + " " + name + " " + connected).trim();
    }

    public void disconnect() {
        try {
            newSession();
            send("client");
            send("disconnect");
            if (socketConnector != null) {
                socketConnector.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        socketConnector = null;
        setConnected(false);
    }

    public void newSession() {
        try {
            socketConnector = new SocketConnector(ip, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void newSession(String purpose) {
        try {
            socketConnector = new SocketConnector(ip, port, purpose);
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
    }

    public void sendObject(String key, boolean flag) {
        newSession(Constant.DDB_SOCKET_STRING);
        send("client");
        send("SQLResult");
        send(key);
        send(SiteManager.getInstance().getMyname());
        if (flag) {
            read();
        }
    }

    public void sendFile(String fileName, String fileType) {
        newSession(Constant.DDB_SOCKET_STRING);
        send("client");
        send("file");
        send(name);
        send(fileType);
        send(fileName);
        try {
            Integer filePort = Integer.parseInt(read());
            SocketConnector fileSender = new SocketConnector(ip, filePort, Constant.DDB_SOCKET_FILE);
            fileSender.sendFile(new File(fileName));
            fileSender.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public Object reciveObject(String objectKey) {
        Object retval = null;
        newSession(Constant.DDB_SOCKET_STRING);
        send("client");
        send("objectSQLResult");
        send(objectKey);
        try {
            Integer objectPort = Integer.parseInt(read());
            SocketConnector objectReciver = new SocketConnector(ip, objectPort, Constant.DDB_SOCKET_OBJECT);
            retval = objectReciver.receiveObject();
            objectReciver.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return retval;
    }

    public Object reciveObject(int objectPort) {
        Object retval = null;
        try {
            Socket socket = new Socket(ip, objectPort);
            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInput = new ObjectInputStream(socket.getInputStream());
            retval = objectInput.readObject();
            try {
                objectOutput.close();
                objectInput.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return retval;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void reciveFile(int filePort, String fileName) {
        byte[] buffer = new byte[2048];
        try {
            Socket socket = new Socket(ip, filePort);
            DataInputStream dataInput = new DataInputStream(socket.getInputStream());
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            DataOutputStream fio = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            while (true) {
                int read = 0;
                if (dataInput != null) {
                    read = dataInput.read(buffer);
                }
                if (read == -1) {
                    break;
                }
                fio.write(buffer, 0, read);
            }
            fio.close();
            dataInput.close();
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
    }
}
