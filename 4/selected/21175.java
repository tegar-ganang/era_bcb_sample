package org.tanso.fountain.util.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import sun.net.TelnetInputStream;
import sun.net.TelnetOutputStream;
import sun.net.ftp.FtpClient;

public class FTPClientSun {

    private FtpClient ftpClient;

    /**
	 * Connect to a FTP Server with a specified port number
	 * 
	 * @param server
	 *            IP Address for the FTP Server.
	 * @param port
	 *            Port number for the FTP Server.
	 * @param user
	 *            User name
	 * @param password
	 *            Password
	 * @return true if connect success
	 */
    public boolean connectToServer(String server, int port, String user, String password) {
        return this.connectToServer(server, port, user, password, null);
    }

    /**
	 * Connect to a FTP Server on port 21
	 * 
	 * @param server
	 *            IP Address for the FTP Server.
	 * @param user
	 *            User name
	 * @param password
	 *            Password
	 * @return true if connect success
	 */
    public boolean connectToServer(String server, String user, String password) {
        return this.connectToServer(server, 21, user, password, null);
    }

    /**
	 * 
	 * Connect to the server with specified port and path.
	 * 
	 * @param server
	 *            IP Address for the FTP Server.
	 * @param port
	 *            Port number for the FTP Server.
	 * @param user
	 *            User name
	 * @param password
	 *            Password
	 * @param path
	 *            Server path. if null or empty, no path change will perform
	 * 
	 */
    public boolean connectToServer(String server, int port, String user, String password, String path) {
        ftpClient = new FtpClient();
        try {
            ftpClient.openServer(server);
            ftpClient.login(user, password);
            if (null != path && path.length() != 0) {
                ftpClient.cd(path);
            }
            ftpClient.binary();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * 
	 * Upload a file.
	 * 
	 * @param upFile
	 *            The file to be uploaded
	 * @param newName
	 *            �ϴ��ļ�����ļ���������
	 * @return true: if succeeded. else false
	 * 
	 */
    public boolean upload(File upFile, String newName) throws IOException {
        InputStream in = new FileInputStream(upFile);
        TelnetOutputStream os = null;
        try {
            os = ftpClient.put(newName);
            byte[] bytes = new byte[1024];
            int c;
            while ((c = in.read(bytes)) != -1) {
                os.write(bytes, 0, c);
            }
        } catch (IOException e) {
            return false;
        } finally {
            if (in != null) {
                in.close();
            }
            if (os != null) {
                os.close();
            }
        }
        return true;
    }

    /**
	 * 
	 * Get the file list with the current server path
	 * 
	 * @return File List. If error occurs, null will be returned.
	 * 
	 */
    public List<String> getFileList() {
        List<String> list = new ArrayList<String>();
        TelnetInputStream in;
        try {
            in = ftpClient.nameList(".");
            BufferedReader bf = new BufferedReader(new InputStreamReader(in));
            String l = null;
            while ((l = bf.readLine()) != null) {
                if (!l.equals(".") && !l.equals("..")) list.add(l);
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * 
	 * Download a file with its input stream.
	 * 
	 * @param fileName
	 *            The file name to be download.
	 * 
	 * @return File stream. Or null if download failed.
	 * 
	 */
    public InputStream getFile(String fileName) {
        TelnetInputStream in = null;
        try {
            in = ftpClient.get(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return in;
    }

    /**
	 * Store a file with its input stream. Close the stream when finish.
	 * 
	 * @param is
	 *            InputStream for the file.
	 * @param fileName
	 *            The name to be stored.
	 * @return true if success. else false.
	 */
    public boolean storeFile(InputStream is, String fileName) {
        if (null == is) {
            return false;
        }
        FileOutputStream fos = null;
        File outPutFile = new File(fileName);
        try {
            fos = new FileOutputStream(outPutFile);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            outPutFile.delete();
            return false;
        }
        byte[] buffer = new byte[1024];
        int readBytes = 0;
        try {
            while ((readBytes = is.read(buffer)) >= 0) {
                fos.write(buffer, 0, readBytes);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            outPutFile.delete();
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * 
	 * Change to the specified directory.
	 * 
	 * @param path
	 *            Server directory.
	 * 
	 * @throws IOException
	 * 
	 */
    public void cdPath(String path) throws IOException {
        ftpClient.cd(path);
    }

    /**
	 * Close the FTP Client
	 * 
	 * @throws IOException
	 */
    public void closeFTPClient() throws IOException {
        if (ftpClient != null) {
            ftpClient.closeServer();
        }
    }

    /**
	 * @param args
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException {
        String server = "127.0.0.1";
        String user = "tanso";
        String passWord = "appapp";
        FTPClientSun ftp = new FTPClientSun();
        ftp.connectToServer(server, user, passWord);
        System.out.println("Download: " + ftp.storeFile(ftp.getFile("ubuntushare/transportservice.jar"), "ts.jar"));
        System.out.println("Upload: " + ftp.upload(new File("simpleapp.war"), "simpleapp.war"));
        List<String> s = ftp.getFileList();
        for (String i : s) System.out.println(i);
        ftp.closeFTPClient();
    }
}
