package ddbadmin.admin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 *
 * @author Roar
 */
public class SocketConnector {

    Socket socket;

    BufferedReader br;

    BufferedWriter bw;

    ObjectInputStream objectInput;

    ObjectOutputStream objectOutput;

    DataInputStream dataInput;

    DataOutputStream dataOutput;

    public SocketConnector(String host, int port) throws Exception {
        socket = new Socket(InetAddress.getByName(host), port);
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public SocketConnector(String host, int port, String purpose) {
        try {
            socket = new Socket(InetAddress.getByName(host), port);
            if (purpose.equals(Constant.DDB_SOCKET_FILE)) {
                dataInput = new DataInputStream(socket.getInputStream());
                dataOutput = new DataOutputStream(socket.getOutputStream());
            } else if (purpose.equals(Constant.DDB_SOCKET_OBJECT)) {
                objectOutput = new ObjectOutputStream(socket.getOutputStream());
                objectInput = new ObjectInputStream(socket.getInputStream());
            } else if (purpose.equals(Constant.DDB_SOCKET_STRING)) {
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            }
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
    }

    public SocketConnector(Socket socket) {
        this.socket = socket;
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
    }

    public SocketConnector(Socket socket, String purpose) {
        this.socket = socket;
        try {
            if (purpose.equals(Constant.DDB_SOCKET_FILE)) {
                dataInput = new DataInputStream(socket.getInputStream());
                dataOutput = new DataOutputStream(socket.getOutputStream());
            } else if (purpose.equals(Constant.DDB_SOCKET_OBJECT)) {
                objectOutput = new ObjectOutputStream(socket.getOutputStream());
                objectInput = new ObjectInputStream(socket.getInputStream());
            }
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
    }

    public String read() {
        try {
            return br.readLine();
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
        return null;
    }

    public Object receiveObject() {
        try {
            return objectInput.readObject();
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
        return null;
    }

    public int receiveFile(String fileName) {
        byte[] buffer = new byte[2048];
        try {
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
            return 1;
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
            return 0;
        }
    }

    public int send(String str) {
        try {
            bw.write(str + "\r\n");
            bw.flush();
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
            return 1;
        }
        return 0;
    }

    public int sendObject(Object object) {
        try {
            objectOutput.writeObject(object);
            objectOutput.flush();
            return 1;
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
        return 0;
    }

    public int sendFile(File file) {
        byte[] buffer = new byte[2048];
        try {
            DataInputStream fis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            while (true) {
                int read = 0;
                if (fis != null) {
                    read = fis.read(buffer);
                }
                if (read == -1) {
                    break;
                }
                dataOutput.write(buffer, 0, read);
            }
            dataOutput.close();
            fis.close();
            return 1;
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
            return 0;
        }
    }

    public String getIP() {
        return socket.getInetAddress().toString();
    }

    public int getPort() {
        return socket.getPort();
    }

    @Override
    public String toString() {
        return getIP() + ":" + getPort();
    }

    public void close() throws Exception {
        if (br != null) {
            br.close();
        }
        if (bw != null) {
            bw.close();
        }
        socket.close();
    }
}
