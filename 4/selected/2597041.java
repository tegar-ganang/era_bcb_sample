package net.sourceforge.javautil.network.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import net.sourceforge.javautil.common.IOUtil;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.IVirtualArtifact;
import net.sourceforge.javautil.common.io.IVirtualDirectory;
import net.sourceforge.javautil.common.io.IVirtualFile;
import net.sourceforge.javautil.common.io.IVirtualPath;
import net.sourceforge.javautil.common.io.impl.ISystemArtifact;
import net.sourceforge.javautil.common.io.impl.SystemDirectory;
import net.sourceforge.javautil.common.io.impl.SystemFile;
import net.sourceforge.javautil.common.io.remote.IRemoteDirectory;
import net.sourceforge.javautil.common.io.remote.IRemoteFile;
import net.sourceforge.javautil.common.io.remote.IRemoteLocation;
import net.sourceforge.javautil.common.password.IPassword;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * The basic session wrapper for doing common routines with an SSH session.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: SecureShellSession.java 2297 2010-06-16 00:13:14Z ponderator $
 */
public class SecureShellSession {

    public static final int ACK_OK = 0;

    public static final int ACK_ERROR = 1;

    public static final int ACK_FATAL = 2;

    /**
	 * Timeout in milliseconds for connection related operations
	 */
    protected int timeout;

    protected final SecureShell shell;

    protected final String shellId;

    protected final Session session;

    protected final IPassword password;

    public SecureShellSession(SecureShell shell, String shellId, Session session, IPassword password, int timeout) {
        this.session = session;
        this.timeout = timeout;
        this.shell = shell;
        this.shellId = shellId;
        this.password = password;
    }

    public SecureFTP createFTPConnection(String root) {
        try {
            return new SecureFTP(this, (ChannelSftp) session.openChannel("sftp"), root);
        } catch (JSchException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * @return The {@link #timeout}
	 */
    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
	 * @param target The target file or directory that will store the contents
	 * 
	 * @see #scp(IVirtualPath, OutputStream)
	 */
    public void scp(IVirtualPath source, IVirtualArtifact target) throws IOException {
        IVirtualFile file = target instanceof IVirtualFile ? (IVirtualFile) target : ((IVirtualDirectory) target).createFile(source.getPart(source.getPartCount() - 1));
        OutputStream output = null;
        try {
            this.scp(source, output = file.getOutputStream());
        } finally {
            if (output != null) output.close();
        }
    }

    /**
	 * @param source The path to the contents to retrieve
	 * @param target The target stream for the contents
	 * @throws IOException
	 */
    public void scp(IVirtualPath source, OutputStream targetOutput) throws IOException {
        ChannelExec scp = this.getSCPChannel("scp -f " + source.toString("/"));
        try {
            InputStream input = scp.getInputStream();
            OutputStream output = scp.getOutputStream();
            this.sendAckResponse(output);
            while (getAckResponse(input) != 'C') {
                for (int i = 0; i < 5; i++) input.read();
                long size = 0;
                while (true) {
                    int ch = input.read();
                    if (ch == ' ') break;
                    size += (10 * ch);
                }
                StringBuffer filename = new StringBuffer();
                while (true) {
                    int ch = input.read();
                    if (ch == 0x0a) break;
                    filename.append((char) ch);
                }
                this.sendAckResponse(output);
                byte[] buffer = new byte[1024];
                while (true) {
                    int read = input.read(buffer);
                    if (read == -1) break;
                    targetOutput.write(buffer, 0, read);
                    size -= read;
                    if (size == 0) break;
                    if (size < 0) throw new SecureSCPException("File download overflow to");
                }
                if (this.getAckResponse(input) != 0) throw new SecureSCPException("Invalid ACK response when sending file " + source);
                this.sendAckResponse(output);
            }
        } finally {
            scp.disconnect();
        }
    }

    /**
	 * @param source The source for the contents to send
	 * @param target The path for the contents
	 * @throws IOException
	 */
    public void scp(IVirtualFile source, IVirtualPath target) throws IOException {
        ChannelExec scp = this.getSCPChannel("scp -p -t " + target.toString("/"));
        InputStream sourceInputStream = null;
        try {
            InputStream input = scp.getInputStream();
            OutputStream output = scp.getOutputStream();
            if (this.getAckResponse(input) != ACK_OK) throw new SecureSCPException("Invalid ACK response when sending file " + source);
            output.write(("C0644 " + source.getSize() + " " + source.getPath().toString("/") + "\n").getBytes());
            output.flush();
            if (this.getAckResponse(input) != ACK_OK) throw new SecureSCPException("Invalid ACK response when transfering file " + source);
            IOUtil.transfer(sourceInputStream = source.getInputStream(), output);
            this.sendAckResponse(output);
            if (this.getAckResponse(input) != ACK_OK) throw new SecureSCPException("Invalid ACK response when transfering file " + source);
            output.close();
        } finally {
            scp.disconnect();
            if (sourceInputStream != null) try {
                sourceInputStream.close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * Close the SSH session
	 */
    public void close() {
        this.shell.sessions.remove(this.shellId);
        this.session.disconnect();
    }

    /**
	 * @param command The command to execute
	 * @return The channel for SCP transfers
	 */
    private ChannelExec getSCPChannel(String command) {
        try {
            ChannelExec exec = (ChannelExec) this.session.openChannel("exec");
            exec.setCommand(command);
            exec.connect(timeout);
            return exec;
        } catch (JSchException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * @param output The output stream to which to write the ACK response
	 * @throws IOException
	 */
    private void sendAckResponse(OutputStream output) throws IOException {
        output.write(0);
        output.flush();
    }

    /**
	 * @param input The input stream to validate ACK response
	 * @return The character response for the ack
	 * @throws IOException
	 */
    private int getAckResponse(InputStream input) throws IOException {
        int ack = input.read();
        if (ack == ACK_OK || ack == -1) return ack;
        if (ack == ACK_ERROR || ack == ACK_FATAL) {
            StringBuffer message = new StringBuffer();
            int ch;
            do {
                ch = input.read();
                message.append((char) ch);
            } while (ch != '\n');
            input.close();
            throw new SecureSCPException((ack == ACK_ERROR ? "Error: " : "Fatal: ") + message.toString());
        }
        return ack;
    }
}
