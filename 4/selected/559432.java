package justsftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public class SFtpFileHandler implements FtpFileHandler {

    public void putFile(File lFile, RemoteFile rFolder) {
        try {
            Channel channel = getChannel(rFolder);
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            sftpChannel.put(lFile.getAbsolutePath(), rFolder.getCompletePath());
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }

    public void removeFile(RemoteFile rFile) {
        try {
            Channel channel = getChannel(rFile);
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            sftpChannel.rm(rFile.getCompletePath());
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }

    public void removeFolder(RemoteFile rFolder) {
        try {
            Channel channel = getChannel(rFolder);
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            sftpChannel.rmdir(rFolder.getCompletePath());
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }

    public void populateChildren(RemoteFile remoteFolder) {
        List<RemoteFile> files = new ArrayList<RemoteFile>();
        if (remoteFolder == null || remoteFolder.getConnection() == null) {
            return;
        }
        try {
            Channel channel = getChannel(remoteFolder);
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            Vector<Object> objs = sftpChannel.ls(remoteFolder.getCompletePath());
            for (Object obj : objs) {
                if (obj instanceof ChannelSftp.LsEntry) {
                    ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) obj;
                    if (!(".".equals(entry.getFilename()) || "..".equals(entry.getFilename()))) {
                        RemoteFile rf = new RemoteFile();
                        rf.setCompletePath(sftpChannel.pwd() + "/" + entry.getFilename());
                        rf.setName(entry.getFilename());
                        com.jcraft.jsch.SftpATTRS attrs = entry.getAttrs();
                        if (attrs.isLink()) {
                            rf.setCompletePath(sftpChannel.readlink(rf.getCompletePath()));
                            SftpATTRS lnAttr = sftpChannel.lstat(rf.getCompletePath());
                            if (lnAttr.isDir()) {
                                rf.setFile(false);
                            } else {
                                rf.setFile(true);
                            }
                        } else {
                            if (attrs.isDir()) {
                                rf.setFile(false);
                            } else {
                                rf.setFile(true);
                            }
                        }
                        rf.setParent(remoteFolder);
                        rf.setConnection(remoteFolder.getConnection());
                        rf.setChannel(sftpChannel);
                        files.add(rf);
                    }
                }
            }
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        }
        Collections.sort(files);
        remoteFolder.setChildren(files);
    }

    private Channel getChannel(RemoteFile file) throws JSchException {
        if (file.getChannel() == null || file.getChannel().isClosed()) {
            Connection connection = file.getConnection();
            Channel channel = getChannel(connection);
            file.setChannel(channel);
        } else if (!file.getChannel().isConnected()) {
            file.getChannel().connect();
        }
        return file.getChannel();
    }

    public Channel getChannel(Connection connection) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(connection.getUserName(), connection.getHost(), connection.getPort());
        session.setPassword(connection.getPassword());
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        Channel channel = session.openChannel(connection.getProtocol());
        channel.connect();
        return channel;
    }

    public void getFile(Connection connection, RemoteFile rFile, String lFile) {
        try {
            Channel channel = getChannel(rFile);
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            FileOutputStream fos = new FileOutputStream(lFile);
            sftpChannel.get(rFile.getCompletePath(), fos);
            fos.flush();
            fos.close();
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
