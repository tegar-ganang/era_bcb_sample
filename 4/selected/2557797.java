package org.allcolor.alc.filesystem.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.allcolor.alc.filesystem.Directory;
import org.allcolor.alc.filesystem.File;
import org.allcolor.alc.filesystem.FileSystem;
import org.allcolor.alc.filesystem.FileSystemType;
import java.io.IOException;
import java.util.Hashtable;

/**
 * @author (Author)
 * @version $Revision$
  */
final class SshFileSystem extends FileSystem {

    /**
	 * Creates a new SshFileSystem object.
	 *
	 * @param label DOCUMENT ME!
	 * @param sftp DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    private SshFileSystem(final String label, final ChannelSftp sftp) throws IOException {
        super(label, FileSystemType.SSH, new SshDirectory("", sftp));
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param label DOCUMENT ME!
	 * @param param DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public static final FileSystem mount(final String label, final Object param) throws IOException {
        if (param == null) {
            throw new IOException("Invalid null mount parameter.");
        }
        final Object obj[] = SshFileSystem.getChannelSftp(param);
        final ChannelSftp sftp = (ChannelSftp) obj[0];
        final String chRoot = (String) obj[1];
        if (chRoot != null) {
            new SshFileSystem("chroot" + label + chRoot.replace('/', '_'), sftp);
            return FileSystem.mount(label, FileSystemType.CHROOT, "chroot" + label + chRoot.replace('/', '_') + ":" + chRoot);
        }
        return new SshFileSystem(label, sftp);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    @Override
    public final boolean isReadOnly() {
        return false;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    @Override
    public boolean umount() throws IOException {
        super.umount();
        final SshDirectory p = this.getRoot().cast();
        final ChannelSftp sftp = p.sftp;
        synchronized (p) {
            sftp.quit();
            try {
                sftp.getSession().disconnect();
            } catch (JSchException ignore) {
            }
        }
        return true;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param name DOCUMENT ME!
	 * @param parent DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    @Override
    protected final Directory createDirectoryObject(final String name, final Directory parent) throws IOException {
        final SshDirectory p = parent.cast();
        final ChannelSftp sftp = p.sftp;
        return new SshDirectory(name, this, p, sftp);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param name DOCUMENT ME!
	 * @param parent DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    @Override
    protected final File createFile(final String name, final Directory parent) throws IOException {
        return new SshFile(this, parent, name);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param host DOCUMENT ME!
	 * @param user DOCUMENT ME!
	 * @param password DOCUMENT ME!
	 * @param port DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws JSchException DOCUMENT ME!
	 */
    private static ChannelSftp connect(final String host, final String user, final String password, final int port) throws JSchException {
        final JSch jsch = new JSch();
        final Session session = jsch.getSession(user, host, port);
        session.setPassword(password);
        final Hashtable<String, String> table = new Hashtable<String, String>();
        table.put("StrictHostKeyChecking", "no");
        table.put("trust", "true");
        session.setConfig(table);
        session.connect();
        final Channel channel = session.openChannel("sftp");
        channel.connect();
        return (ChannelSftp) channel;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param oparam DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    private static Object[] getChannelSftp(final Object oparam) throws IOException {
        if (oparam instanceof ChannelSftp) {
            return new Object[] { oparam, null };
        }
        String param = (oparam instanceof String) ? (String) oparam : "";
        if (param.indexOf(':') == -1) {
            throw new IOException("Invalid ssh url : " + param);
        }
        final String host = param.substring(0, param.indexOf(':'));
        param = param.substring(param.indexOf(':') + 1);
        int port = 22;
        String user = null;
        try {
            port = Integer.parseInt(param.substring(0, param.indexOf(':')));
            param = param.substring(param.indexOf(':') + 1);
            user = param.substring(0, param.indexOf(':'));
            param = param.substring(param.indexOf(':') + 1);
        } catch (final Exception e) {
            if (param.indexOf(':') == -1) {
                throw new IOException("Invalid ssh url : " + param);
            }
            user = param.substring(0, param.indexOf(':'));
            param = param.substring(param.indexOf(':') + 1);
        }
        String password = param;
        String chroot = null;
        if (param.indexOf(':') != -1) {
            password = param.substring(0, param.indexOf(':'));
            param = param.substring(param.indexOf(':') + 1);
            chroot = param;
        }
        try {
            return new Object[] { SshFileSystem.connect(host, user, password, port), chroot };
        } catch (final JSchException e) {
            throw new IOException(e);
        }
    }
}
