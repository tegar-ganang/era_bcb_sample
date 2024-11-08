package com.oneandone.sushi.fs.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.oneandone.sushi.fs.OnShutdown;
import com.oneandone.sushi.fs.Root;
import com.oneandone.sushi.io.MultiOutputStream;
import com.oneandone.sushi.util.ExitCode;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class SshRoot implements Root<SshNode>, UserInfo, Runnable {

    private final SshFilesystem filesystem;

    private final String user;

    private final Credentials credentials;

    private final String host;

    private final Session session;

    private ChannelSftp channelFtp;

    public SshRoot(SshFilesystem filesystem, String host, String user, Credentials credentials, int timeout) throws JSchException {
        if (credentials == null) {
            throw new IllegalArgumentException();
        }
        this.filesystem = filesystem;
        this.user = user;
        this.host = host;
        this.credentials = credentials;
        this.session = login(filesystem.getJSch(), host);
        this.session.connect(timeout);
        this.channelFtp = null;
        OnShutdown.get().onShutdown(this);
    }

    public SshFilesystem getFilesystem() {
        return filesystem;
    }

    public String getId() {
        return "//" + session.getUserName() + "@" + session.getHost() + "/";
    }

    public SshNode node(String path, String encodedQuery) {
        if (encodedQuery != null) {
            throw new IllegalArgumentException(encodedQuery);
        }
        return new SshNode(this, path);
    }

    @Override
    public boolean equals(Object obj) {
        SshRoot root;
        if (obj instanceof SshRoot) {
            root = (SshRoot) obj;
            return getId().equals(root.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return "SshNode host=" + host + ", user=" + user;
    }

    public ChannelSftp getChannelFtp() throws JSchException {
        if (channelFtp == null) {
            channelFtp = (ChannelSftp) session.openChannel("sftp");
            channelFtp.connect();
        }
        return channelFtp;
    }

    public ChannelExec createChannelExec() throws JSchException {
        return (ChannelExec) session.openChannel("exec");
    }

    public void close() {
        session.disconnect();
    }

    public Process start(boolean tty, String... command) throws JSchException {
        return start(tty, MultiOutputStream.createNullStream(), command);
    }

    public Process start(boolean tty, OutputStream out, String... command) throws JSchException {
        return Process.start(this, tty, out, command);
    }

    public String exec(String... command) throws JSchException, ExitCode {
        return exec(true, command);
    }

    public String exec(boolean tty, String... command) throws JSchException, ExitCode {
        ByteArrayOutputStream out;
        out = new ByteArrayOutputStream();
        try {
            start(tty, out, command).waitFor();
        } catch (ExitCode e) {
            throw new ExitCode(e.call, e.code, filesystem.getWorld().getSettings().string(out));
        }
        return filesystem.getWorld().getSettings().string(out);
    }

    public String getUser() {
        return user;
    }

    public Session login(JSch jsch, String host) throws JSchException {
        Session session;
        jsch.addIdentity(credentials.loadIdentity(jsch), null);
        session = jsch.getSession(user, host);
        session.setUserInfo(this);
        return session;
    }

    public String getHost() {
        return host;
    }

    public String getPassphrase(String message) {
        throw new IllegalStateException(message);
    }

    public String getPassword() {
        throw new IllegalStateException();
    }

    public boolean prompt(String str) {
        throw new IllegalStateException(str);
    }

    public String getPassphrase() {
        return credentials.passphrase;
    }

    public boolean promptPassphrase(String prompt) {
        return true;
    }

    public boolean promptPassword(String prompt) {
        return false;
    }

    public boolean promptYesNo(String message) {
        return true;
    }

    public void showMessage(String message) {
        System.out.println("showMessage " + message);
    }

    public void run() {
        close();
    }
}
