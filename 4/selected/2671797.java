package agorum.blender.yadra.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Hashtable;
import agorum.commons.file.FileUtil;

/**
 * 
 * @author oliver.schulze
 */
public class ComUtils {

    private Config config = null;

    private Socket gSocket = null;

    private InputStream gInputStream = null;

    private OutputStream gOutputStream = null;

    /** Creates a new instance of ComUtils */
    public ComUtils(Config config) {
        this.config = config;
    }

    /**
	 * get a file on remote side
	 */
    public boolean retrFile(String fullPath, String toFile) throws Exception {
        try {
            ComBean comBean = new ComBean();
            comBean.setCommand("retrFile");
            Hashtable params = new Hashtable();
            params.put("fullPath", fullPath);
            comBean.setParameters(params);
            ComBean answer = sendCommand(comBean);
            if (answer.getErrorCode() == 0) {
                FileOutputStream fos = new FileOutputStream(toFile);
                int amnt = 1460;
                byte[] buffer = new byte[amnt];
                while (true) {
                    int read = answer.getInputStream().read(buffer, 0, amnt);
                    if (read >= 0) {
                        fos.write(buffer, 0, read);
                    } else {
                        break;
                    }
                }
                fos.close();
                return true;
            } else {
                return false;
            }
        } finally {
            cleanUpConnection();
        }
    }

    /**
	 * Retreive file serverside (send file to client)
	 */
    public void retrFileServer(InputStream is, OutputStream os, ComBean comBean) throws Exception {
        String fullPath = getFullPath((String) comBean.getParameters().get("fullPath"));
        FileInputStream fis = new FileInputStream(fullPath);
        ObjectOutputStream oos = new ObjectOutputStream(os);
        ComBean answer = new ComBean();
        answer.setErrorCode(0);
        answer.setVersionNumber(ComBean.version);
        oos.writeObject(answer);
        int amnt = 1460;
        byte[] buffer = new byte[amnt];
        while (true) {
            int read = fis.read(buffer, 0, amnt);
            if (read >= 0) {
                os.write(buffer, 0, read);
            } else {
                break;
            }
        }
        fis.close();
    }

    /**
	 * 
	 */
    private String getFullPath(String path) throws Exception {
        String sl = "";
        if (!path.startsWith("/")) {
            sl = "/";
        }
        path = config.getWorkPath() + sl + path;
        return path;
    }

    /**
	 * clean up the connection
	 */
    private void cleanUpConnection() {
        try {
            gInputStream.close();
        } catch (Exception e) {
        }
        try {
            gOutputStream.close();
        } catch (Exception e) {
        }
        try {
            gSocket.close();
        } catch (Exception e) {
        }
    }

    /**
	 * get a file on remote side
	 */
    public boolean sendFile(String fromFile, String fullPath) throws Exception {
        try {
            ComBean comBean = new ComBean();
            comBean.setCommand("sendFile");
            Hashtable params = new Hashtable();
            params.put("fullPath", fullPath);
            comBean.setParameters(params);
            File f = new File(fromFile);
            if (f.exists()) {
                OutputStream os = sendCommandOS(comBean);
                if (os != null) {
                    FileInputStream fis = new FileInputStream(fromFile);
                    int amnt = 1460;
                    byte[] buffer = new byte[amnt];
                    while (true) {
                        int read = fis.read(buffer, 0, amnt);
                        if (read >= 0) {
                            os.write(buffer, 0, read);
                        } else {
                            break;
                        }
                    }
                    fis.close();
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } finally {
            cleanUpConnection();
        }
    }

    /**
	 * Send file serverside (receive file from client)
	 */
    public void sendFileServer(InputStream is, OutputStream os, ComBean comBean) throws Exception {
        String fullPath = getFullPath((String) comBean.getParameters().get("fullPath"));
        File f = new File(fullPath);
        mkDirs(f);
        FileOutputStream fos = new FileOutputStream(fullPath);
        int amnt = 1460;
        byte[] buffer = new byte[amnt];
        while (true) {
            int read = is.read(buffer, 0, amnt);
            if (read >= 0) {
                fos.write(buffer, 0, read);
            } else {
                break;
            }
        }
        fos.close();
    }

    /**
	 * get file info
	 */
    public ComBean doPing(String remoteAddress, int remotePort, String passPhrase) throws Exception {
        try {
            ComBean comBean = new ComBean();
            comBean.setCommand("ping");
            ComBean answer = sendCommandTest(comBean, remoteAddress, remotePort, passPhrase);
            return answer;
        } finally {
            cleanUpConnection();
        }
    }

    /**
	 * get file info
	 */
    public FileInfo getFileInfo(String fullPath) throws Exception {
        try {
            ComBean comBean = new ComBean();
            comBean.setCommand("getFileInfo");
            Hashtable params = new Hashtable();
            params.put("fullPath", fullPath);
            comBean.setParameters(params);
            ComBean answer = sendCommand(comBean);
            if (answer.getErrorCode() == 0) {
                FileInfo fileInfo = (FileInfo) answer.getParameters().get("fileInfo");
                return fileInfo;
            } else {
                return null;
            }
        } finally {
            cleanUpConnection();
        }
    }

    /**
	 * Send file serverside (receive file from client)
	 */
    public void getFileInfoServer(InputStream is, OutputStream os, ComBean comBean) throws Exception {
        String fullPath = getFullPath((String) comBean.getParameters().get("fullPath"));
        ObjectOutputStream oos = new ObjectOutputStream(os);
        ComBean answer = new ComBean();
        File f = new File(fullPath);
        if (f.exists()) {
            answer.setErrorCode(0);
            FileInfo fileInfo = getFileInfo(f);
            Hashtable params = new Hashtable();
            params.put("fileInfo", fileInfo);
            answer.setParameters(params);
        } else {
            answer.setErrorCode(1);
        }
        answer.setVersionNumber(ComBean.version);
        oos.writeObject(answer);
    }

    /**
	 * set file info
	 */
    public void setFileInfo(String fullPath, FileInfo fileInfo) throws Exception {
        try {
            ComBean comBean = new ComBean();
            comBean.setCommand("setFileInfo");
            Hashtable params = new Hashtable();
            params.put("fullPath", fullPath);
            params.put("fileInfo", fileInfo);
            comBean.setParameters(params);
            ComBean answer = sendCommand(comBean);
        } finally {
            cleanUpConnection();
        }
    }

    /**
	 * Send file serverside (receive file from client)
	 */
    public void setFileInfoServer(InputStream is, OutputStream os, ComBean comBean) throws Exception {
        String fullPath = getFullPath((String) comBean.getParameters().get("fullPath"));
        ObjectOutputStream oos = new ObjectOutputStream(os);
        ComBean answer = new ComBean();
        File f = new File(fullPath);
        FileInfo fileInfo = (FileInfo) comBean.getParameters().get("fileInfo");
        if (f.exists()) {
            answer.setErrorCode(0);
            setFileInfo(f, fileInfo);
        } else {
            answer.setErrorCode(1);
        }
        answer.setVersionNumber(ComBean.version);
        oos.writeObject(answer);
    }

    /**
	 * list a directory
	 */
    public FileInfo[] listFiles(String fullPath) throws Exception {
        try {
            ComBean comBean = new ComBean();
            comBean.setCommand("listFiles");
            Hashtable params = new Hashtable();
            params.put("fullPath", fullPath);
            comBean.setParameters(params);
            ComBean answer = sendCommand(comBean);
            if (answer.getErrorCode() == 0) {
                FileInfo[] fileInfos = (FileInfo[]) answer.getParameters().get("fileInfos");
                return fileInfos;
            } else {
                return null;
            }
        } finally {
            cleanUpConnection();
        }
    }

    /**
	 * Send file serverside (receive file from client)
	 */
    public void listFilesServer(InputStream is, OutputStream os, ComBean comBean) throws Exception {
        String fullPath = getFullPath((String) comBean.getParameters().get("fullPath"));
        ObjectOutputStream oos = new ObjectOutputStream(os);
        ComBean answer = new ComBean();
        File f = new File(fullPath);
        if (f.exists() && f.isDirectory()) {
            answer.setErrorCode(0);
            File[] files = f.listFiles();
            FileInfo[] fileInfos = null;
            if (files != null) {
                int len = files.length;
                fileInfos = new FileInfo[len];
                for (int i = 0; i < len; i++) {
                    fileInfos[i] = getFileInfo(files[i]);
                }
            }
            Hashtable params = new Hashtable();
            if (fileInfos != null) {
                params.put("fileInfos", fileInfos);
            }
            answer.setParameters(params);
        } else {
            answer.setErrorCode(1);
        }
        answer.setVersionNumber(ComBean.version);
        oos.writeObject(answer);
    }

    /**
	 * get Info from a file
	 */
    private FileInfo getFileInfo(File file) throws Exception {
        FileInfo fileInfo = null;
        if (file.exists()) {
            fileInfo = new FileInfo();
            fileInfo.setName(file.getName());
            fileInfo.setIsDirectory(file.isDirectory());
            fileInfo.setDate(new Date(file.lastModified()));
            fileInfo.setSize(file.length());
        }
        return fileInfo;
    }

    /**
	 * set Info to a file
	 */
    private void setFileInfo(File file, FileInfo fileInfo) throws Exception {
        if (file.exists()) {
            file.setLastModified(fileInfo.getDate().getTime());
        }
    }

    /**
	 * move a file
	 */
    public boolean moveFile(String from, String to) throws Exception {
        try {
            ComBean comBean = new ComBean();
            comBean.setCommand("moveFile");
            Hashtable params = new Hashtable();
            params.put("from", from);
            params.put("to", to);
            comBean.setParameters(params);
            ComBean answer = sendCommand(comBean);
            if (answer.getErrorCode() == 0) {
                return true;
            } else {
                return false;
            }
        } finally {
            cleanUpConnection();
        }
    }

    /**
	 * Send file serverside (receive file from client)
	 */
    public void moveFileServer(InputStream is, OutputStream os, ComBean comBean) throws Exception {
        String from = getFullPath((String) comBean.getParameters().get("from"));
        String to = getFullPath((String) comBean.getParameters().get("to"));
        ObjectOutputStream oos = new ObjectOutputStream(os);
        ComBean answer = new ComBean();
        File fFrom = new File(from);
        File fTo = new File(to);
        if (fFrom.exists() && !fTo.exists()) {
            mkDirs(fTo);
            answer.setErrorCode(0);
            fFrom.renameTo(fTo);
        } else {
            answer.setErrorCode(1);
        }
        answer.setVersionNumber(ComBean.version);
        oos.writeObject(answer);
    }

    /**
	 * delete a fullPath on remote
	 */
    public boolean delete(String fullPath) throws Exception {
        try {
            ComBean comBean = new ComBean();
            comBean.setCommand("delete");
            Hashtable params = new Hashtable();
            params.put("fullPath", fullPath);
            comBean.setParameters(params);
            ComBean answer = sendCommand(comBean);
            if (answer.getErrorCode() == 0) {
                return true;
            } else {
                return false;
            }
        } finally {
            cleanUpConnection();
        }
    }

    /**
	 * Send file serverside (receive file from client)
	 */
    public void deleteServer(InputStream is, OutputStream os, ComBean comBean) throws Exception {
        String fullPath = getFullPath((String) comBean.getParameters().get("fullPath"));
        ObjectOutputStream oos = new ObjectOutputStream(os);
        ComBean answer = new ComBean();
        File f = new File(fullPath);
        if (f.exists()) {
            answer.setErrorCode(0);
            FileUtil fileUtil = new FileUtil();
            fileUtil.delete(f);
        } else {
            answer.setErrorCode(1);
        }
        answer.setVersionNumber(ComBean.version);
        oos.writeObject(answer);
    }

    /**
	 * create not existing directories
	 */
    private void mkDirs(File file) throws Exception {
        file.getParentFile().mkdirs();
    }

    /**
	 * send a command to remote
	 */
    private ComBean sendCommand(ComBean command) throws Exception {
        InetAddress ia = InetAddress.getByName(config.getPuplicIPAdress());
        gSocket = new Socket(ia, config.getRemotePort());
        gSocket.setSoTimeout(10000);
        gSocket.setTcpNoDelay(true);
        CryptOutputStreamWrapper cos = new CryptOutputStreamWrapper(gSocket.getOutputStream(), config);
        gOutputStream = cos;
        ObjectOutputStream oos = new ObjectOutputStream(cos);
        command.setVersionNumber(ComBean.version);
        oos.writeObject(command);
        CryptInputStreamWrapper cis = new CryptInputStreamWrapper(gSocket.getInputStream(), config);
        gInputStream = cis;
        ObjectInputStream ois = new ObjectInputStream(cis);
        ComBean answer = (ComBean) ois.readObject();
        checkAnswer(answer, "");
        answer.setInputStream(cis);
        return answer;
    }

    /**
	 * check the answer
	 * 
	 * @param answer
	 * @throws Exception
	 */
    private void checkAnswer(ComBean answer, String ra) throws Exception {
        if (!answer.getVersionNumber().equals(ComBean.version)) {
            throw new Exception("Wrong Versionnumber on remote: " + answer.getVersionNumber() + ", expected: " + ComBean.version + ", from: " + ra);
        }
    }

    /**
	 * send a command to remote
	 */
    private ComBean sendCommandTest(ComBean command, String remoteAddress, int remotePort, String passPhrase) throws Exception {
        ComBean answer = new ComBean();
        try {
            InetAddress ia = InetAddress.getByName(remoteAddress);
            gSocket = new Socket(ia, remotePort);
            gSocket.setTcpNoDelay(true);
            gSocket.setSoTimeout(5000);
        } catch (Exception e) {
            answer.setErrorCode(1);
            return answer;
        }
        try {
            CryptOutputStreamWrapper cos = new CryptOutputStreamWrapper(gSocket.getOutputStream(), passPhrase);
            gOutputStream = cos;
            ObjectOutputStream oos = new ObjectOutputStream(cos);
            command.setVersionNumber(ComBean.version);
            oos.writeObject(command);
            CryptInputStreamWrapper cis = new CryptInputStreamWrapper(gSocket.getInputStream(), passPhrase);
            gInputStream = cis;
            ObjectInputStream ois = new ObjectInputStream(cis);
            answer = (ComBean) ois.readObject();
            checkAnswer(answer, "");
            answer.setInputStream(cis);
            return answer;
        } catch (Exception e) {
            answer.setErrorCode(2);
            return answer;
        }
    }

    /**
	 * send a command to remote
	 */
    private OutputStream sendCommandOS(ComBean command) throws Exception {
        InetAddress ia = InetAddress.getByName(config.getRemoteAddress());
        gSocket = new Socket(ia, config.getRemotePort());
        gSocket.setSoTimeout(10000);
        gSocket.setTcpNoDelay(true);
        CryptOutputStreamWrapper cos = new CryptOutputStreamWrapper(gSocket.getOutputStream(), config);
        gOutputStream = cos;
        ObjectOutputStream oos = new ObjectOutputStream(cos);
        command.setVersionNumber(ComBean.version);
        oos.writeObject(command);
        return cos;
    }

    /**
	 * receive a command and handle it
	 */
    public void receiveCommand(Socket socket, InputStream is, OutputStream os) throws Exception {
        String remote = socket.getRemoteSocketAddress().toString();
        ObjectInputStream ois = new ObjectInputStream(is);
        ComBean comBean = null;
        try {
            comBean = (ComBean) ois.readObject();
            checkAnswer(comBean, remote);
        } catch (Exception e) {
            System.err.println("Error from server: " + remote);
            throw e;
        }
        try {
            String command = comBean.getCommand();
            if (command.equals("retrFile")) {
                retrFileServer(is, os, comBean);
            } else if (command.equals("sendFile")) {
                sendFileServer(is, os, comBean);
            } else if (command.equals("getFileInfo")) {
                getFileInfoServer(is, os, comBean);
            } else if (command.equals("setFileInfo")) {
                setFileInfoServer(is, os, comBean);
            } else if (command.equals("listFiles")) {
                listFilesServer(is, os, comBean);
            } else if (command.equals("moveFile")) {
                moveFileServer(is, os, comBean);
            } else if (command.equals("delete")) {
                deleteServer(is, os, comBean);
            } else if (command.equals("ping")) {
                ObjectOutputStream oos = new ObjectOutputStream(os);
                ComBean answer = new ComBean();
                answer.setErrorCode(0);
                answer.setVersionNumber(ComBean.version);
                oos.writeObject(answer);
            } else {
                System.err.println("unhandled command: " + command);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            ObjectOutputStream oos = new ObjectOutputStream(os);
            ComBean answer = new ComBean();
            answer.setErrorCode(10);
            answer.setErrorMessage("" + e.getMessage());
            answer.setVersionNumber(ComBean.version);
            oos.writeObject(answer);
        } finally {
            try {
                os.flush();
                os.close();
            } catch (Exception e) {
            }
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }
}
