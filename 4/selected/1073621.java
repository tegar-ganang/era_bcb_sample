package scpn.transfer;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author orestrepo
 * @date 17/06/2011
 */
public class RsyncHelper {

    public static final String RSYNC_HELPER_SCRIPT = "rsync_helper.sh";

    public static final String RSYNC_ASKPASS_SCRIPT = "sshaskpass.sh";

    public static class MyUserInfo implements UserInfo {

        private final String password;

        public MyUserInfo(String password) {
            this.password = password;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public boolean promptPassword(String string) {
            return true;
        }

        @Override
        public boolean promptPassphrase(String string) {
            return true;
        }

        @Override
        public boolean promptYesNo(String string) {
            return true;
        }

        @Override
        public void showMessage(String string) {
        }
    }

    public static Session connect(String host, String userName, String password) throws JSchException {
        JSch sch = new JSch();
        Session session = sch.getSession(userName, host);
        session.setUserInfo(new MyUserInfo(password));
        session.connect();
        return session;
    }

    /**
     * Rebuild the initial scripts in the host
     * @param session to connection with the host
     * @return string with the path directory that contains the initial scripts in the host
     */
    public static String sendScripts(Session session) {
        Channel channel = null;
        String tempDirectory = "";
        Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "Start sendScripts.");
        try {
            {
                channel = session.openChannel("exec");
                final String command = "mktemp -d /tmp/scipionXXXXXXXX";
                ((ChannelExec) channel).setCommand(command);
                InputStream in = channel.getInputStream();
                channel.connect();
                String[] result = inputStreamToString(in, channel);
                tempDirectory = result[1];
                tempDirectory = tempDirectory.replaceAll("\n", "");
                Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "status:" + result[0] + "-command:" + command + "-result:" + tempDirectory);
                IOUtils.closeQuietly(in);
                channel.disconnect();
            }
            {
                channel = session.openChannel("exec");
                final String command = "chmod 700 " + tempDirectory;
                ((ChannelExec) channel).setCommand(command);
                InputStream in = channel.getInputStream();
                channel.connect();
                String[] result = inputStreamToString(in, channel);
                Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "status:" + result[0] + "-command:" + command + "-result:" + result[1]);
                IOUtils.closeQuietly(in);
                channel.disconnect();
            }
            {
                InputStream rsyncHelperContentInput = Thread.currentThread().getContextClassLoader().getResourceAsStream("scripts/" + RSYNC_HELPER_SCRIPT);
                channel = session.openChannel("exec");
                final String command = "cat > " + tempDirectory + "/" + RSYNC_HELPER_SCRIPT;
                ((ChannelExec) channel).setCommand(command);
                OutputStream out = channel.getOutputStream();
                channel.connect();
                IOUtils.copy(rsyncHelperContentInput, out);
                IOUtils.closeQuietly(out);
                channel.disconnect();
            }
            {
                channel = session.openChannel("exec");
                final String command = "chmod 700 " + tempDirectory + "/" + RSYNC_HELPER_SCRIPT;
                ((ChannelExec) channel).setCommand(command);
                InputStream in = channel.getInputStream();
                channel.connect();
                String[] result = inputStreamToString(in, channel);
                Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "status:" + result[0] + "-command:" + command + "-result:" + result[1]);
                IOUtils.closeQuietly(in);
                channel.disconnect();
            }
            {
                InputStream askPassContentInput = Thread.currentThread().getContextClassLoader().getResourceAsStream("scripts/" + RSYNC_ASKPASS_SCRIPT);
                channel = session.openChannel("exec");
                final String command = "cat > " + tempDirectory + "/" + RSYNC_ASKPASS_SCRIPT;
                ((ChannelExec) channel).setCommand(command);
                OutputStream out = channel.getOutputStream();
                channel.connect();
                IOUtils.copy(askPassContentInput, out);
                IOUtils.closeQuietly(out);
                channel.disconnect();
            }
            {
                channel = session.openChannel("exec");
                final String command = "chmod 700 " + tempDirectory + "/" + RSYNC_ASKPASS_SCRIPT;
                ((ChannelExec) channel).setCommand(command);
                InputStream in = channel.getInputStream();
                channel.connect();
                String[] result = inputStreamToString(in, channel);
                Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "status:" + result[0] + "-command:" + command + "-result:" + result[1]);
                IOUtils.closeQuietly(in);
                channel.disconnect();
            }
        } catch (IOException ex) {
            Logger.getLogger(RsyncHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSchException ex) {
            Logger.getLogger(RsyncHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "End sendScripts.");
        return tempDirectory;
    }

    /**
     * Read the content of inputStream in a string
     * @param in is the input of channel, in the host is the output
     * @param channel in the session with the host
     * @return string with the content of inputstream
     */
    public static String[] inputStreamToString(InputStream in, Channel channel) {
        String[] result = new String[2];
        StringBuilder inputStreamContent = new StringBuilder();
        byte[] tmp = new byte[1024];
        while (true) {
            try {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    inputStreamContent.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    final int exitStatus = channel.getExitStatus();
                    result[0] = String.valueOf(exitStatus);
                    result[1] = inputStreamContent.toString();
                    return result;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            } catch (IOException ex) {
                Logger.getLogger(RsyncHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static String inputStreamToString(InputStream in, OutputStream out, Channel channel) {
        String result = "";
        byte[] tmp = new byte[1024];
        while (true) {
            try {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    out.write(tmp, 0, i);
                }
                if (channel.isClosed()) {
                    final int exitStatus = channel.getExitStatus();
                    result = String.valueOf(exitStatus);
                    return result;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            } catch (IOException ex) {
                Logger.getLogger(RsyncHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static boolean testHostConnection(Session session, String remoteUser, String remotePassword, String tempDirectoryScripts) {
        Channel channel = null;
        InputStream in = null;
        OutputStream out = null;
        Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "Start testHostConnection.");
        try {
            channel = session.openChannel("exec");
            final String command = tempDirectoryScripts + "/" + RSYNC_HELPER_SCRIPT + " testconnection " + remoteUser + " " + tempDirectoryScripts + "/" + RSYNC_ASKPASS_SCRIPT;
            ((ChannelExec) channel).setCommand(command);
            in = channel.getInputStream();
            out = channel.getOutputStream();
            channel.connect();
            out.write((remotePassword + "\n").getBytes());
            IOUtils.closeQuietly(out);
            String[] result = inputStreamToString(in, channel);
            Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "status:" + result[0] + "-command:" + command + "-result:" + result[1]);
            return Integer.parseInt(result[0]) == 0;
        } catch (IOException ex) {
            Logger.getLogger(RsyncHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSchException ex) {
            Logger.getLogger(RsyncHelper.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(RsyncHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (channel != null) {
                channel.disconnect();
            }
            Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "End testHostConnection.");
        }
        return false;
    }

    /**
     * When finish the transfer files delete the temporal directory
     * @param session session to connection with the host
     * @param directory the path directory that contains the initial scripts in the host
     */
    public static void deleteDirectory(Session session, String directory) {
        {
            InputStream in = null;
            Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "Start deleteDirectory.");
            try {
                Channel channel = null;
                channel = session.openChannel("exec");
                final String command = "rm -r " + directory;
                ((ChannelExec) channel).setCommand(command);
                in = channel.getInputStream();
                channel.connect();
                String[] result = inputStreamToString(in, channel);
                Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "status:" + result[0] + "-command:" + command + "-result:" + result[1]);
                IOUtils.closeQuietly(in);
                channel.disconnect();
            } catch (IOException ex) {
                Logger.getLogger(RsyncHelper.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JSchException ex) {
                Logger.getLogger(RsyncHelper.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    in.close();
                    Logger.getLogger(RsyncHelper.class.getName()).log(Level.INFO, "End deleteDirectory.");
                } catch (IOException ex) {
                    Logger.getLogger(RsyncHelper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
