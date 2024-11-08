package editor.source;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.apache.commons.net.ftp.FTPClient;
import editor.common.ClipBord;
import editor.common.FileUtil;
import editor.common.IConstants;
import editor.common.Node;
import editor.common.SourceDetail;
import editor.config.ConfigManager;

public class SourceUtil implements IConstants {

    public static void doPaste(boolean isBinary) throws SourceException {
        ISource source = ClipBord.getSource();
        ISource target = ClipBord.getTarget();
        if (ClipBord.getTargetNode().isLeaf()) ClipBord.setTargetNode((Node) ClipBord.getTargetNode().getParent());
        try {
            if (ConfigManager.getGUIConfig().getLabel(FTP).equalsIgnoreCase(target.getSourceDetail().getProtocol()) && ConfigManager.getGUIConfig().getLabel(FTP).equalsIgnoreCase(source.getSourceDetail().getProtocol())) {
                String tempOutDirName = ConfigManager.getGUIConfig().getWorkingDirectoryName() + File.separator + "local";
                retriveRemote(source, ClipBord.getSourceArr(), tempOutDirName, isBinary);
                storeRemote(tempOutDirName, target.getSourceDetail(), ClipBord.getTargetNode().getAbsolutePath(), isBinary);
                FileUtil.delete(tempOutDirName);
            } else if (!ConfigManager.getGUIConfig().getLabel(FTP).equalsIgnoreCase(target.getSourceDetail().getProtocol()) && !ConfigManager.getGUIConfig().getLabel(FTP).equalsIgnoreCase(source.getSourceDetail().getProtocol())) {
                for (Node node : ClipBord.getSourceArr()) {
                    FileUtil.copy(node.getAbsolutePath(), ClipBord.getTargetNode().getAbsolutePath() + File.separator + node.getAlias());
                }
            } else if (!ConfigManager.getGUIConfig().getLabel(FTP).equalsIgnoreCase(target.getSourceDetail().getProtocol()) && ConfigManager.getGUIConfig().getLabel(FTP).equalsIgnoreCase(source.getSourceDetail().getProtocol())) {
                retriveRemote(source, ClipBord.getSourceArr(), ClipBord.getTargetNode().getAbsolutePath(), isBinary);
            } else if (ConfigManager.getGUIConfig().getLabel(FTP).equalsIgnoreCase(target.getSourceDetail().getProtocol()) && !ConfigManager.getGUIConfig().getLabel(FTP).equalsIgnoreCase(source.getSourceDetail().getProtocol())) {
                storeRemote(ClipBord.getSourceArr(), target.getSourceDetail(), ClipBord.getTargetNode().getAbsolutePath(), isBinary);
            }
        } catch (Exception e) {
            throw new SourceException(e);
        }
    }

    public static void retriveRemote(ISource source, Node[] nodes, String outDirName, boolean isBinary) throws Exception {
        FTPClient client = new FTPClient();
        client.connect(source.getSourceDetail().getHost());
        client.login(source.getSourceDetail().getUser(), source.getSourceDetail().getPassword());
        if (isBinary) client.setFileType(FTPClient.BINARY_FILE_TYPE);
        FileOutputStream out = null;
        for (Node node : nodes) {
            if (!node.isLeaf()) {
                Node[] childern = source.getChildern(node);
                File dir = new File(outDirName + File.separator + node.getAlias());
                dir.mkdir();
                retriveRemote(source, childern, outDirName + File.separator + node.getAlias(), isBinary);
            } else {
                out = new FileOutputStream(outDirName + File.separator + node.getAlias());
                client.retrieveFile(node.getAbsolutePath(), out);
                out.flush();
                out.close();
            }
        }
        client.disconnect();
    }

    public static void storeRemote(String sourceLocation, SourceDetail targetSourceDetail, String targetlocation, boolean isBinary) throws Exception {
        FTPClient client = new FTPClient();
        client.connect(targetSourceDetail.getHost());
        client.login(targetSourceDetail.getUser(), targetSourceDetail.getPassword());
        if (isBinary) client.setFileType(FTPClient.BINARY_FILE_TYPE);
        File file = new File(sourceLocation);
        if (file.isDirectory()) {
            client.makeDirectory(targetlocation);
            FileInputStream in = null;
            for (File myFile : file.listFiles()) {
                if (myFile.isDirectory()) {
                    storeRemote(myFile.getAbsolutePath(), targetSourceDetail, targetlocation + "/" + myFile.getName(), isBinary);
                } else {
                    in = new FileInputStream(myFile.getAbsolutePath());
                    if (!targetlocation.endsWith("/")) client.storeFile(targetlocation + "/" + myFile.getName(), in); else client.storeFile(targetlocation + myFile.getName(), in);
                    in.close();
                }
            }
        } else {
            FileInputStream in = new FileInputStream(sourceLocation);
            client.storeFile(targetlocation, in);
            in.close();
        }
        client.disconnect();
    }

    public static void storeRemote(Node[] sourceLocations, SourceDetail targetSourceDetail, String targetlocation, boolean isBinary) throws Exception {
        for (Node node : sourceLocations) {
            storeRemote(node.getAbsolutePath(), targetSourceDetail, targetlocation + node.getAlias(), isBinary);
        }
    }
}
