package br.com.visualmidia.core.server;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.util.Scanner;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.apache.log4j.Logger;
import br.com.visualmidia.business.FileDescriptor;
import br.com.visualmidia.business.FileDescriptorMD5;
import br.com.visualmidia.core.Constants;
import br.com.visualmidia.core.ServerAdress;
import br.com.visualmidia.system.GDSystem;
import br.com.visualmidia.ui.Server;

public class Communicate {

    private Socket socket;

    private Socket fileTransferSocket;

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private static final Logger logger = Logger.getLogger(Communicate.class);

    public Communicate(Socket socket) {
        this.socket = socket;
    }

    public String sendAndReceive(String message) {
        send(message);
        return receive();
    }

    public String receiveAndSend(String message) {
        String response = receive();
        send(message);
        return response;
    }

    public void send(String message) {
        try {
            OutputStream outputstream = socket.getOutputStream();
            OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream);
            BufferedWriter bufferedwriter = new BufferedWriter(outputstreamwriter);
            bufferedwriter.write(message + '\n');
            bufferedwriter.write("cya\n");
            bufferedwriter.flush();
        } catch (IOException e) {
            logger.error("Send Message:", e);
        }
    }

    public String receive() {
        StringBuffer message = new StringBuffer();
        try {
            InputStream inputstream = socket.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            String string = null;
            while (!(string = bufferedreader.readLine()).equals("cya")) {
                message.append(string);
                logger.info(socket.getInetAddress() + ": " + string);
                System.out.flush();
            }
        } catch (IOException e) {
            logger.error("Receive Message:", e);
        }
        Scanner scanner = new Scanner(message.toString());
        scanner.useDelimiter(",");
        return (scanner.hasNext() ? scanner.next() : "");
    }

    public void sendFile(File file) {
        try {
            fileTransferSocket = GetFileTransferSocket.getInstance().getSocket();
            InputStream fileIn = new FileInputStream(file);
            byte[] block = new byte[1024];
            OutputStream dataOut = fileTransferSocket.getOutputStream();
            int sizeRead = 0;
            while ((sizeRead = fileIn.read(block)) >= 0) {
                dataOut.write(block, 0, sizeRead);
            }
            dataOut.flush();
            dataOut.close();
            fileIn.close();
            fileTransferSocket.close();
        } catch (IOException e) {
            try {
                fileTransferSocket.close();
                socket.close();
            } catch (IOException e1) {
                logger.error("File Transfer Socket:", e);
            }
            logger.error("Send File:", e);
        }
    }

    public void sendFile(Socket fileTransferSocket, File file) {
        try {
            InputStream fileIn = new FileInputStream(file);
            byte[] block = new byte[1024];
            OutputStream dataOut = fileTransferSocket.getOutputStream();
            int sizeRead = 0;
            while ((sizeRead = fileIn.read(block)) >= 0) {
                dataOut.write(block, 0, sizeRead);
            }
            dataOut.flush();
            dataOut.close();
            fileIn.close();
            fileTransferSocket.close();
        } catch (IOException e) {
            try {
                fileTransferSocket.close();
                socket.close();
            } catch (IOException e1) {
                logger.error("File Transfer Socket:", e);
            }
            logger.error("Send File:", e);
        }
    }

    public boolean receiveFile(FileDescriptor fileDescriptor) {
        try {
            byte[] block = new byte[1024];
            int sizeRead = 0;
            int totalRead = 0;
            File dir = new File(Constants.DOWNLOAD_DIR + fileDescriptor.getLocation());
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(Constants.DOWNLOAD_DIR + fileDescriptor.getLocation() + fileDescriptor.getName());
            if (!file.exists()) {
                file.createNewFile();
            }
            SSLSocket sslsocket = getFileTransferConectionConnectMode(ServerAdress.getServerAdress());
            OutputStream fileOut = new FileOutputStream(file);
            InputStream dataIn = sslsocket.getInputStream();
            while ((sizeRead = dataIn.read(block)) >= 0) {
                totalRead += sizeRead;
                fileOut.write(block, 0, sizeRead);
                propertyChangeSupport.firePropertyChange("fileByte", 0, totalRead);
            }
            fileOut.close();
            dataIn.close();
            sslsocket.close();
            if (fileDescriptor.getName().contains(".snapshot")) {
                try {
                    File fileData = new File(Constants.DOWNLOAD_DIR + fileDescriptor.getLocation() + fileDescriptor.getName());
                    File dirData = new File(Constants.PREVAYLER_DATA_DIRETORY + Constants.FILE_SEPARATOR);
                    File destino = new File(dirData, fileData.getName());
                    boolean success = fileData.renameTo(destino);
                    if (!success) {
                        deleteDir(Constants.DOWNLOAD_DIR);
                        return false;
                    }
                    deleteDir(Constants.DOWNLOAD_DIR);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (Server.isServerOpen()) {
                    FileChannel inFileChannel = new FileInputStream(file).getChannel();
                    File dirServer = new File(Constants.DOWNLOAD_DIR + fileDescriptor.getLocation());
                    if (!dirServer.exists()) {
                        dirServer.mkdirs();
                    }
                    File fileServer = new File(Constants.DOWNLOAD_DIR + fileDescriptor.getName());
                    if (!fileServer.exists()) {
                        fileServer.createNewFile();
                    }
                    inFileChannel.transferTo(0, inFileChannel.size(), new FileOutputStream(fileServer).getChannel());
                    inFileChannel.close();
                }
            }
            if (totalRead == fileDescriptor.getSize()) {
                return true;
            }
        } catch (Exception e) {
            logger.error("Receive File:", e);
        }
        return false;
    }

    public boolean receiveFile(SSLSocket sslsocket, FileDescriptor fileDescriptor, String location) {
        try {
            byte[] block = new byte[1024];
            int sizeRead = 0;
            int totalRead = 0;
            File dir = new File(location + fileDescriptor.getLocation());
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(location + fileDescriptor.getLocation() + fileDescriptor.getName());
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStream fileOut = new FileOutputStream(file);
            InputStream dataIn = sslsocket.getInputStream();
            while ((sizeRead = dataIn.read(block)) >= 0) {
                totalRead += sizeRead;
                fileOut.write(block, 0, sizeRead);
                propertyChangeSupport.firePropertyChange("fileByte", 0, totalRead);
            }
            fileOut.close();
            dataIn.close();
            sslsocket.close();
            if (totalRead == fileDescriptor.getSize()) {
                return true;
            }
        } catch (Exception e) {
            logger.error("Receive File:", e);
        }
        return false;
    }

    public boolean receiveFile(SSLSocket sslsocket, FileDescriptorMD5 fileDescriptorMD5, String location) {
        try {
            byte[] block = new byte[1024];
            int sizeRead = 0;
            int totalRead = 0;
            File dir = new File(location + fileDescriptorMD5.getLocation());
            if (!dir.exists()) {
                dir.mkdirs();
            }
            System.out.println("Localiza��o " + location + fileDescriptorMD5.getLocation() + fileDescriptorMD5.getName() + "  size " + fileDescriptorMD5.getSize());
            File file = new File(location + fileDescriptorMD5.getLocation() + fileDescriptorMD5.getName());
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStream fileOut = new FileOutputStream(file);
            InputStream dataIn = sslsocket.getInputStream();
            while ((sizeRead = dataIn.read(block)) >= 0) {
                totalRead += sizeRead;
                fileOut.write(block, 0, sizeRead);
                propertyChangeSupport.firePropertyChange("fileByte", 0, totalRead);
            }
            fileOut.close();
            dataIn.close();
            sslsocket.close();
            if (totalRead == fileDescriptorMD5.getSize()) {
                return true;
            }
        } catch (Exception e) {
            logger.error("Receive File:", e);
        }
        return false;
    }

    public Socket getFileTransferConectionWaitMode() throws UnknownHostException, IOException {
        return GetFileTransferSocket.getInstance().getSocket();
    }

    public SSLSocket getFileTransferConectionConnectMode(String ip) throws UnknownHostException, IOException {
        SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(ip, 9999);
        return sslsocket;
    }

    public void receivePersonPhoto() {
        try {
            byte[] block = new byte[1024];
            int sizeRead = 0;
            int totalRead = 0;
            File dir = new File(Constants.TEMP_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(Constants.TEMP_DIR + "personPhoto" + ".jpg");
            if (!file.exists()) {
                file.createNewFile();
            }
            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(GDSystem.getServerIp(), 9999);
            OutputStream fileOut = new FileOutputStream(file);
            InputStream dataIn = sslsocket.getInputStream();
            while ((sizeRead = dataIn.read(block)) >= 0) {
                totalRead += sizeRead;
                fileOut.write(block, 0, sizeRead);
            }
            fileOut.close();
            dataIn.close();
            sslsocket.close();
        } catch (Exception e) {
            logger.error("Receive photo:", e);
        }
    }

    public void receiveLogo() {
        try {
            byte[] block = new byte[1024];
            int sizeRead = 0;
            int totalRead = 0;
            File dir = new File(Constants.PHOTO_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(Constants.PHOTO_DIR + "corporateLogo" + ".jpg");
            if (!file.exists()) {
                file.createNewFile();
            }
            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(GDSystem.getServerIp(), 9999);
            OutputStream fileOut = new FileOutputStream(file);
            InputStream dataIn = sslsocket.getInputStream();
            while ((sizeRead = dataIn.read(block)) >= 0) {
                totalRead += sizeRead;
                fileOut.write(block, 0, sizeRead);
            }
            fileOut.close();
            dataIn.close();
            sslsocket.close();
        } catch (Exception e) {
            logger.error("Receive photo:", e);
        }
    }

    public boolean deletePersonPhoto(String idPerson) {
        try {
            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslsocket = (SSLSocket) sslsocketfactory.createSocket(GDSystem.getServerIp(), 9999);
            File file = new File(Constants.PHOTO_DIR + idPerson + ".jpg");
            FileChannel fileIn = new FileInputStream(file).getChannel();
            OutputStream dataout = sslsocket.getOutputStream();
            if (file.exists()) {
                return file.delete();
            } else {
                return false;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void sendObject(Object arg) {
        try {
            ObjectOutputStream req = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            req.writeObject(arg);
            req.flush();
        } catch (IOException e) {
            logger.error("Send Object:", e);
        }
    }

    public Object receiveObject() {
        try {
            return new ObjectInputStream(new BufferedInputStream(socket.getInputStream())).readObject();
        } catch (Exception e) {
            logger.error("Receive Object:", e);
        }
        return null;
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    public static boolean deleteDir(String strFile) {
        File fDir = new File(strFile);
        String[] strChildren = null;
        boolean bRet = false;
        if (fDir.isDirectory()) {
            strChildren = fDir.list();
            for (int i = 0; i < strChildren.length; i++) {
                bRet = deleteDir(new File(fDir, strChildren[i]).getAbsolutePath());
                if (!bRet) {
                    return false;
                }
            }
        }
        return fDir.delete();
    }
}
