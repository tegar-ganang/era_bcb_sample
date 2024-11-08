package editor.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import editor.common.FileUtil;
import editor.common.Node;
import editor.common.SourceDetail;
import editor.config.ConfigManager;

public class FTPSource implements ISource {

    private FTPClient client = null;

    private File workingDirectory = null;

    private Map<String, String> localPathMap = null;

    int localFileNameCounter = 0;

    private SourceDetail sourceDetail = null;

    public FTPSource(SourceDetail sourceDetail) throws SourceException {
        this.sourceDetail = sourceDetail;
        localPathMap = new HashMap<String, String>();
        client = new FTPClient();
        try {
            client.connect(sourceDetail.getHost());
            client.login(sourceDetail.getUser(), sourceDetail.getPassword());
            workingDirectory = new File(ConfigManager.getGUIConfig().getWorkingDirectoryName() + File.separator + sourceDetail.getName());
            workingDirectory.mkdir();
        } catch (Exception e) {
            throw new SourceException(e);
        }
    }

    public SourceDetail getSourceDetail() {
        return sourceDetail;
    }

    public Node[] getRoots() throws SourceException {
        Node node = new Node();
        node.setAlias("/");
        node.setAbsolutePath("/");
        node.setLeaf(false);
        Node[] roots = { node };
        return roots;
    }

    public Node[] getChildern(Node parent) throws SourceException {
        List<Node> list = new ArrayList<Node>();
        FTPListParseEngine engine;
        try {
            engine = client.initiateListParsing(parent.getAbsolutePath());
            while (engine.hasNext()) {
                FTPFile[] fileNames = engine.getNext(25);
                if (fileNames != null) {
                    for (FTPFile myfile : fileNames) {
                        Node myNode = new Node();
                        myNode.setAlias(myfile.getName());
                        myNode.setLeaf(!myfile.isDirectory());
                        if (!myfile.isDirectory()) myNode.setAbsolutePath(parent.getAbsolutePath() + myfile.getName()); else myNode.setAbsolutePath(parent.getAbsolutePath() + myfile.getName() + "/");
                        myNode.setSize(myfile.getSize());
                        myNode.setDateModifies(new Date(myfile.getTimestamp().getTimeInMillis()));
                        myNode.setOwner(myfile.getUser());
                        myNode.setPermissionsAsString(getPermissionAsString(myfile));
                        list.add(myNode);
                    }
                }
            }
        } catch (IOException e) {
            throw new SourceException(e);
        }
        Node[] nodes = new Node[list.size()];
        return list.toArray(nodes);
    }

    private String getPermissionAsString(FTPFile file) {
        StringBuffer permissions = new StringBuffer();
        if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION)) permissions.append("r");
        if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION)) permissions.append("w");
        if (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION)) permissions.append("x");
        permissions.append(" - ");
        if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION)) permissions.append("r");
        if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION)) permissions.append("w");
        if (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION)) permissions.append("x");
        permissions.append(" - ");
        if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION)) permissions.append("r");
        if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION)) permissions.append("w");
        if (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION)) permissions.append("x");
        return permissions.toString();
    }

    public boolean isAlive() {
        return client.isConnected();
    }

    public void close() {
        try {
            if (client != null) client.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (workingDirectory != null) FileUtil.deleteDirectory(workingDirectory);
    }

    public void createFile(String location, String fileName) throws SourceException {
        File file = new File(workingDirectory.getAbsolutePath() + File.separator + fileName);
        FileInputStream fileInputStream = null;
        try {
            if (!file.exists()) file.createNewFile();
            fileInputStream = new FileInputStream(file);
            client.storeFile(location + fileName, fileInputStream);
        } catch (IOException e) {
            new SourceException(e);
        } finally {
            try {
                if (fileInputStream != null) fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void createFolder(String location, String fileName) throws SourceException {
        try {
            client.makeDirectory(location + fileName);
        } catch (IOException e) {
            new SourceException(e);
        }
    }

    public void deleteFile(String filePath) throws SourceException {
        try {
            client.deleteFile(filePath);
        } catch (IOException e) {
            new SourceException(e);
        }
    }

    public void deleteDirectory(String dirPath) throws SourceException {
        try {
            client.removeDirectory(dirPath);
        } catch (IOException e) {
            new SourceException(e);
        }
    }

    public void rename(String location, String oldName, String newName) throws SourceException {
        try {
            client.rename(location + "/" + oldName, location + "/" + newName);
        } catch (IOException e) {
            new SourceException(e);
        }
    }

    public String getFileContentAsString(String absolutePath) throws SourceException {
        String fileContent = null;
        try {
            String localPath = null;
            if (localPathMap.containsKey(absolutePath)) {
                localPath = localPathMap.get(absolutePath);
            } else {
                localPath = workingDirectory.getAbsolutePath() + File.separator + "File" + localFileNameCounter;
                localPathMap.put(absolutePath, localPath);
                localFileNameCounter++;
            }
            retrieveFile(absolutePath, localPath);
            fileContent = FileUtil.getFileContentAsString(localPath);
        } catch (Exception e) {
            new SourceException(e);
        }
        return fileContent;
    }

    public void save(String location, String content) throws SourceException {
        String localPath = localPathMap.get(location);
        FileUtil.save(localPath, content);
        FileInputStream in = null;
        try {
            in = new FileInputStream(localPath);
            client.storeFile(location, in);
        } catch (Exception e) {
            new SourceException(e);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void retrieveFile(String remotePath, String localPath) throws Exception {
        FileOutputStream out = new FileOutputStream(localPath);
        client.retrieveFile(remotePath, out);
        out.flush();
        out.close();
    }
}
