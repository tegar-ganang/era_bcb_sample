package com.skruk.elvis.rmi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * Description of the Class
 *
 * @author     skruk
 * @created    4 luty 2004
 */
public class FileReaderServerSocket extends Thread {

    /** Description of the Field */
    public static File RMI_DIR = null;

    /** DOCUMENT ME! */
    private static final int BUFFER_SIZE = 10240;

    /** Description of the Field */
    private ServerSocket serverSocket = null;

    /** Description of the Field */
    private int port = 0;

    /**
	 * Constructor for the FileReaderServerSocket object
	 *
	 * @param  port  Description of the Parameter
	 */
    public FileReaderServerSocket(int port) {
        synchronized (FileReaderServerSocket.class) {
            if (RMI_DIR == null) {
                RMI_DIR = new File(com.skruk.elvis.beans.ContextKeeper.getInstallDir() + "/WEB-INF/rmi-upload/");
            }
        }
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            try {
                serverSocket = new ServerSocket(0);
            } catch (IOException ex) {
                System.err.println("[ERROR] FileReaderServerSocket error: " + ex);
            }
        } finally {
            this.port = serverSocket.getLocalPort();
            System.out.println("[DEBUG] FileReaderServerSocket listening on port " + port);
        }
        this.setDaemon(true);
    }

    /** Main processing method for the FileReaderServerSocket object */
    public void run() {
        while (true) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("[ERROR] Problem accepting connection: " + e);
                continue;
            }
            String filename = null;
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                InputStream in = clientSocket.getInputStream();
                byte[] baUploadId = new byte[10];
                in.read(baUploadId);
                String uploadId = new String(baUploadId, 0, 10);
                UploadSession usSession = UploadSession.get(uploadId);
                System.out.println("[DEBUG] Got uploadId: >" + new String(baUploadId) + "< of size " + baUploadId.length + " > session = " + usSession);
                int namelength = in.read();
                byte[] name = new byte[namelength];
                in.read(name);
                filename = new String(name, 0, namelength);
                int lenlength = in.read();
                byte[] leng = new byte[lenlength];
                in.read(leng);
                long size = Long.valueOf(new String(leng)).longValue();
                byte[] buffer = new byte[BUFFER_SIZE];
                int len = 0;
                long count = 0;
                if ((usSession != null) && "marc21.xml".equals(filename)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream((int) size);
                    System.out.print("[DEBUG] Reading MARC-XML file with size " + size);
                    if (size > 0) {
                        while ((len = in.read(buffer, 0, BUFFER_SIZE)) > 0) {
                            baos.write(buffer, 0, len);
                            count += len;
                            if (count >= size) {
                                break;
                            }
                        }
                    }
                    System.out.println(" done");
                    out.println("ACK");
                    baos.flush();
                    String xmlmarc = new String(baos.toString("UTF-8"));
                    usSession.setXmlMarc21(xmlmarc);
                    baos.close();
                } else if ((usSession != null) && "structure.xml".equals(filename)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream((int) size);
                    System.out.print("[DEBUG] Reading structure XML file with size " + size);
                    if (size > 0) {
                        while ((len = in.read(buffer, 0, BUFFER_SIZE)) > 0) {
                            baos.write(buffer, 0, len);
                            count += len;
                            if (count >= size) {
                                break;
                            }
                        }
                    }
                    System.out.println(" done");
                    out.println("ACK");
                    baos.flush();
                    String xmlresource = new String(baos.toString("UTF-8"));
                    usSession.setXmlResource(xmlresource);
                    baos.close();
                } else if (usSession != null) {
                    String prefix = (filename.lastIndexOf('.') < 3) ? ((filename.length() < 3) ? "tmp" : filename) : filename.substring(0, filename.lastIndexOf('.') + 1);
                    String postfix = ((filename.lastIndexOf('.') < 0) || (filename.lastIndexOf('.') >= (filename.length() - 3))) ? null : filename.substring(filename.lastIndexOf('.'));
                    File file = File.createTempFile(prefix, postfix, RMI_DIR);
                    FileOutputStream fos = new FileOutputStream(file);
                    FileChannel fch = fos.getChannel();
                    ReadableByteChannel rbc = Channels.newChannel(in);
                    FileLock flock = fch.lock();
                    long pos = 0;
                    long cnt = 0;
                    System.out.print("[DEBUG] Reading file " + filename + " to file " + file.getName() + " with size " + size);
                    while ((cnt = fch.transferFrom(rbc, pos, size)) > 0) {
                        pos += cnt;
                        if (pos >= size) {
                            break;
                        }
                    }
                    flock.release();
                    System.out.println(" done");
                    out.println(file.getName());
                    fos.flush();
                    fos.close();
                    usSession.addFile(file);
                } else {
                    while ((len = in.read(buffer, 0, 1024)) > 0) {
                        count += len;
                        if (count >= size) {
                            break;
                        }
                    }
                    out.println("ERROR");
                }
                out.close();
                in.close();
                clientSocket.close();
            } catch (IOException ex) {
                System.out.println(RMI_DIR.getAbsolutePath());
                ex.printStackTrace();
            }
        }
    }

    /** Description of the Method */
    protected void finalize() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
	 * Gets the port attribute of the FileReaderServerSocket object
	 *
	 * @return    The port value
	 */
    public int getPort() {
        return this.port;
    }

    /** Description of the Method */
    public void close() {
        try {
            serverSocket.close();
        } catch (Exception ex) {
        }
        serverSocket = null;
    }
}
