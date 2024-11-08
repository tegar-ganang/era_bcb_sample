package com.panopset.op;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import com.panopset.Strings;
import com.panopset.TextScrambler;
import com.panopset.Util;
import com.panopset.sf.AppProps;
import com.panopset.sf.PrompterSwing;

/**
 * FTP Site.
 *
 * @author Karl Dinwiddie
 */
public final class FtpSite {

    /**
     * Number of params is 4.
     */
    private static final int NUM_PARAMS = 4;

    /**
     * Use this constructor if a cancel button is not needed.
     */
    public FtpSite() {
    }

    /**
     * Entry point.
     *
     * @param s
     *            <pre>
     * s[0] = ftp url
     * s[1] = remote directory
     * s[2] = user id
     * s[3] = local directory
     * </pre>
     * @throws Exception
     *             Exception.
     */
    public static void main(final String... s) throws Exception {
        int i = 0;
        final String ftpURL = s[i++];
        final String remoteDir = s[i++];
        final String ftpUser = s[i++];
        final String localDir = s[i++];
        final String p = ftpURL + remoteDir + localDir;
        if (s == null || s.length != NUM_PARAMS) {
            printUsage();
            return;
        }
        AppProps state = new AppProps("com_panopset_sites");
        String ftpPwd = state.get(ftpUser);
        if (Strings.isPopulated(ftpPwd)) {
            ftpPwd = TextScrambler.unscramble(ftpPwd, p);
        } else {
            ftpPwd = new PrompterSwing("Please enter password for " + ftpUser).get();
            state.put(ftpUser, TextScrambler.scramble(ftpPwd, p));
        }
        new FtpSite().putFullDirectory(ftpURL, remoteDir, ftpUser, ftpPwd, localDir);
    }

    /**
     * Put a full directory up to an FTP site.
     *
     * @param ftpURL
     *            FTP url.
     * @param remoteDir
     *            Remote directory.
     * @param userId
     *            User ID.
     * @param pwd
     *            Password.
     * @param localDir
     *            Local directory, not created remotely. Use same name as
     *            remoteDir if you want it created.
     * @throws Exception
     *             Exception.
     */
    public void putFullDirectory(final String ftpURL, final String remoteDir, final String userId, final String pwd, final String localDir) throws Exception {
        if (!Strings.isPopulated(ftpURL)) {
            Util.dspmsg("Need an FTP url.");
            return;
        }
        if (!Strings.isPopulated(remoteDir)) {
            Util.dspmsg("Need a remote directory.");
            return;
        }
        if (!Strings.isPopulated(userId)) {
            Util.dspmsg("Need a user ID.");
            return;
        }
        if (!Strings.isPopulated(pwd)) {
            Util.dspmsg("Need a password.");
            return;
        }
        if (!Strings.isPopulated(localDir)) {
            Util.dspmsg("Need a local directory.");
            return;
        }
        FTPClient c = new FTPClient();
        c.connect(ftpURL);
        int replyCode = c.getReplyCode();
        if (!FTPReply.isPositiveCompletion(replyCode)) {
            Util.dspmsg("Could not connect, code: " + replyCode);
            c.disconnect();
            return;
        }
        if (!c.login(userId, pwd)) {
            Util.dspmsg("Could not log on, userId: " + userId + " pwd: " + pwd);
            return;
        }
        StringTokenizer st = new StringTokenizer(remoteDir, "/");
        while (st.hasMoreElements()) {
            if (!chgDir(c, st.nextToken())) {
                return;
            }
        }
        c.setFileType(FTP.BINARY_FILE_TYPE);
        File file = new File(localDir);
        if (file.isDirectory()) {
            FOR: for (File f : file.listFiles()) {
                if (!put(c, f)) {
                    break FOR;
                }
            }
        } else {
            put(c, file);
        }
        c.logout();
        c.disconnect();
    }

    /**
     *
     * @param client
     *            FTPClient
     * @param file
     *            File to put, recursive if directory.
     * @return true if successful.
     * @throws IOException
     *             IOException.
     */
    public boolean put(final FTPClient client, final File file) throws IOException {
        if (file.isDirectory()) {
            chgDir(client, file.getName());
            for (File f : file.listFiles()) {
                if (!put(client, f)) {
                    return false;
                }
            }
            Util.dspmsg("CWD ..");
            client.changeWorkingDirectory("..");
        } else {
            Util.dspmsg("Sending " + file.getName());
            client.enterLocalPassiveMode();
            FileInputStream is = new FileInputStream(file);
            client.storeFile(file.getName(), is);
            is.close();
        }
        return true;
    }

    /**
     * Change directory, create it if it does not exist.
     *
     * @param c
     *            FTP client.
     * @param dir
     *            Directory name.
     * @return true if successful.
     * @throws IOException
     *             IOException.
     */
    private boolean chgDir(final FTPClient c, final String dir) throws IOException {
        if (Strings.isPopulated(dir)) {
            Util.dspmsg("CWD " + dir);
            if (!c.changeWorkingDirectory(dir)) {
                c.makeDirectory(dir);
                if (!c.changeWorkingDirectory(dir)) {
                    Util.dspmsg("Could not create " + dir + ", exiting.");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Print usage.
     */
    private static void printUsage() {
        System.out.println("java FtpSite <ftp url>" + " <remote directory>" + " <user id>" + " <local directory>");
    }
}
