package mt_tcp.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import mt_tcp.server.MyLogger;
import mt_tcp.server.TransferLengthInfo;

/**
 * 多线程接收端
 * @author hcy
 */
public class MClientMain {

    /**
     * 默认的服务器主机名为localhost,端口为1234
     */
    public MClientMain() {
        this("localhost", 1234);
    }

    /**
     * 设定服务器主机名和端口号
     * 默认一个线程传送数据
     * @param hostname
     * @param port
     */
    public MClientMain(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        this.numberOfThreads = 1;
        logger = MyLogger.getInstance();
        transferLengthInfo = new TransferLengthInfo();
    }

    public int connect() throws UnknownHostException, IOException {
        logger.info("[MClientMain] TSF: Creating socket,input stream and output stream.");
        createSocket();
        buffer = new byte[BUFFERSIZE];
        logger.info("[MClientMain] Requesting file length.");
        getFileLengthFromServer();
        if (fileLength == -1) {
            return FILE_NOT_EXIST;
        }
        logger.info("[MClientMain] Receiving data...");
        long beginTime = System.currentTimeMillis();
        receiveDataFromServer();
        if (done) {
            long endTime = System.currentTimeMillis();
            logger.info("[MClientMain] Total time：" + (endTime - beginTime) / 1000 + "s.");
        }
        return RECEIVE_SUCCESS;
    }

    /**
     * 关闭输入输出流和socket。
     */
    public void close() {
        try {
            if (inFromServer != null) {
                inFromServer.close();
            }
            if (outToServer != null) {
                outToServer.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logger.info("[MClientMain] Close error..");
            e.printStackTrace();
        }
    }

    /**
     * 接收数据并写入文件
     *
     * @param writeToFileName
     *            写入的文件的文件名
     * @throws IOException
     * @throws FileNotFoundException
     */
    private int receiveDataFromServer() {
        File file = new File(writeToFileName);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e1) {
        }
        if (fileLength > 10240) {
            long perLength = fileLength / (long) numberOfThreads;
            threads = new Thread[numberOfThreads + 1];
            long tempstart = 0, remainLen = fileLength;
            int i = 0;
            while (i < numberOfThreads && remainLen > perLength) {
                logger.info("[MClientMain] Create Thread:" + (i + 1));
                logger.info("[MClientMain] Begin: " + tempstart + "   Length: " + perLength);
                threads[i] = new Thread(new TransferThreadClient.Builder().setHostname(hostname).setPort(port).setRequiredFileName(requiredFileName).setStartPos(tempstart).setTransferLength(perLength).setWriteToFileName(writeToFileName).setTransferLengthInfo(transferLengthInfo).build());
                threads[i].start();
                tempstart += perLength;
                remainLen -= perLength;
                ++i;
            }
            if (remainLen > 0) {
                logger.info("[MClientMain] Create Thread:" + (i + 1));
                logger.info("[MClientMain] Begin: " + tempstart + "   Length: " + remainLen);
                threads[i] = new Thread(new TransferThreadClient.Builder().setHostname(hostname).setPort(port).setRequiredFileName(requiredFileName).setStartPos(tempstart).setTransferLength(remainLen).setWriteToFileName(writeToFileName).setTransferLengthInfo(transferLengthInfo).build());
                threads[i].start();
            }
        } else {
            new Thread(new TransferThreadClient.Builder().setHostname(hostname).setPort(port).setRequiredFileName(requiredFileName).setStartPos(0).setTransferLength(fileLength).setWriteToFileName(writeToFileName).setTransferLengthInfo(transferLengthInfo).build()).start();
        }
        Thread acks = new Thread(new Acks());
        acks.start();
        try {
            acks.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return TRANSFER_SUCCESS;
    }

    /**
     * 创建socket 创建输入和输出流
     *
     * @throws UnknownHostException
     * @throws IOException
     * @throws SocketException
     */
    private void createSocket() throws UnknownHostException, IOException, SocketException {
        logger.info("[MClientMain] Creating socket connecion.");
        clientSocket = new Socket(hostname, port);
        logger.info("[MClientMain] Success create.");
        clientSocket.setSoTimeout(0);
        outToServer = new BufferedOutputStream(clientSocket.getOutputStream());
        inFromServer = new BufferedInputStream(clientSocket.getInputStream());
    }

    /**
     * 返回向服务器请求的文件的文件名
     *
     * @return the fileName
     */
    public String getFileName() {
        return requiredFileName;
    }

    /**
     * 设置请求的文件的文件名
     *
     * @param fileName
     *            the fileName to set
     */
    public void setFileName(String fileName) {
        this.requiredFileName = fileName;
    }

    /**
      * 从服务器读取文件的长度
      * 当读取的文件长度为-1时，表示文件不存在
      * @throws IOException
      */
    private void getFileLengthFromServer() throws IOException {
        String info = "GetFileLength";
        outToServer.write(info.getBytes(), 0, info.getBytes().length);
        outToServer.flush();
        inFromServer.read(buffer);
        outToServer.write(requiredFileName.getBytes(), 0, requiredFileName.getBytes().length);
        outToServer.flush();
        int lenLen = -1;
        lenLen = inFromServer.read(buffer);
        fileLength = Long.parseLong(new String(buffer, 0, lenLen));
        System.out.printf("[MClientMain] The length of the file：%d bytes\n", fileLength);
        outToServer.write("Got length".getBytes());
        outToServer.flush();
        logger.info("[MClientMain] Get length over.");
    }

    /**
      * 轮询所有传送线程，检查是否都已经传送完毕
      * @author hcy
      *
      */
    private class Acks implements Runnable {

        @Override
        public void run() {
            boolean allStop = false;
            while (!allStop) {
                allStop = true;
                for (int i = 0; i < threads.length; ++i) {
                    if (threads[i] != null && threads[i].isAlive()) {
                        allStop = false;
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            done = true;
        }
    }

    /**
      * 返回所连接的服务器的主机名
      *
      * @return
      */
    public String getHostname() {
        return hostname;
    }

    /**
      * 设置所连接的服务器主机名
      *
      * @param hostname
      */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
      * 返回所连接的服务器端口号
      *
      * @return
      */
    public int getPort() {
        return port;
    }

    /**
      * 设置所连接的服务器端口号
      *
      * @param port
      */
    public void setPort(int port) {
        this.port = port;
    }

    /**
      * @return the numberOfThreads
      */
    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    /**
      * @param numberOfThreads
      *            the numberOfThreads to set
      */
    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    /**
      * 获得传送的文件的长度。
      * @return
      */
    public long getFileLength() {
        return fileLength;
    }

    /**
      *
      * @return
      */
    public String getWriteToFileName() {
        return this.writeToFileName;
    }

    /**
      *
      * @param name
      */
    public void setWriteToFileName(String name) {
        this.writeToFileName = name;
    }

    /**
      * 返回当前传送长度信息类
      * @return
      */
    public TransferLengthInfo getTransferLengthInfo() {
        return transferLengthInfo;
    }

    private static final int BUFFERSIZE = 1024;

    private String hostname = null;

    private int port;

    private byte[] buffer = null;

    private BufferedOutputStream outToServer = null;

    private BufferedInputStream inFromServer = null;

    private Socket clientSocket = null;

    private long fileLength = 0;

    private String requiredFileName = null;

    private int numberOfThreads = 1;

    private volatile Thread[] threads = null;

    private boolean done = false;

    private String writeToFileName = null;

    public static final int RECEIVE_SUCCESS = 0;

    public static final int FILE_NOT_EXIST = -1;

    public static final int CREATE_SOCKET_ERROT = -2;

    public static final int TRANSFER_ERROR = -3;

    public static final int TRANSFER_SUCCESS = 1;

    private MyLogger logger = null;

    private TransferLengthInfo transferLengthInfo = null;
}
