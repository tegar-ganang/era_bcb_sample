package de.ui.sushi.fs.ssh;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import de.ui.sushi.fs.DeleteException;
import de.ui.sushi.fs.ExistsException;
import de.ui.sushi.fs.GetLastModifiedException;
import de.ui.sushi.fs.LengthException;
import de.ui.sushi.fs.LinkException;
import de.ui.sushi.fs.ListException;
import de.ui.sushi.fs.MkdirException;
import de.ui.sushi.fs.MoveException;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.ReadLinkException;
import de.ui.sushi.fs.SetLastModifiedException;
import de.ui.sushi.fs.file.FileNode;
import de.ui.sushi.io.CheckedByteArrayOutputStream;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public class SshNode extends Node {

    private final SshRoot root;

    private final ChannelSftp channel;

    private final String slashPath;

    public SshNode(SshRoot root, String path) throws JSchException {
        this(root, root.getChannelFtp(), path);
    }

    public SshNode(SshRoot root, ChannelSftp channel, String path) {
        if (root == null) {
            throw new IllegalArgumentException();
        }
        this.root = root;
        this.channel = channel;
        this.slashPath = "/" + path;
    }

    @Override
    public SshRoot getRoot() {
        return root;
    }

    public ChannelSftp getChannel() {
        return channel;
    }

    @Override
    public long length() throws LengthException {
        try {
            SftpATTRS attrs = channel.stat(slashPath);
            if (attrs.isDir()) {
                throw new LengthException(this, new IOException("file expected"));
            }
            return attrs.getSize();
        } catch (SftpException e) {
            throw new LengthException(this, e);
        }
    }

    @Override
    public String getPath() {
        return slashPath.substring(1);
    }

    @Override
    public List<SshNode> list() throws ListException {
        List<SshNode> nodes;
        ChannelSftp.LsEntry entry;
        String name;
        boolean dir;
        try {
            nodes = new ArrayList<SshNode>();
            dir = false;
            for (Object obj : channel.ls(slashPath)) {
                try {
                    entry = (ChannelSftp.LsEntry) obj;
                    name = entry.getFilename();
                    if (".".equals(name) || "..".equals(name)) {
                        dir = true;
                    } else {
                        nodes.add((SshNode) join(name));
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("illegal name: " + obj, e);
                }
            }
            if (!dir && nodes.size() == 1) {
                return null;
            } else {
                return nodes;
            }
        } catch (SftpException e) {
            throw new ListException(this, e);
        }
    }

    @Override
    public SshNode delete() throws DeleteException {
        SftpATTRS stat;
        try {
            stat = channel.lstat(slashPath);
            if (stat.isDir()) {
                for (Node child : list()) {
                    child.delete();
                }
                channel.rmdir(slashPath);
            } else {
                channel.rm(slashPath);
            }
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE || e.id == ChannelSftp.SSH_FX_FAILURE) {
                throw new DeleteException(this, new FileNotFoundException());
            }
            throw new DeleteException(this, e);
        } catch (ListException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    @Override
    public Node move(Node destNode) throws MoveException {
        SshNode dest;
        if (!(destNode instanceof SshNode)) {
            throw new MoveException(this, destNode, "target has is different node type");
        }
        dest = (SshNode) destNode;
        try {
            channel.rename(slashPath, dest.slashPath);
        } catch (SftpException e) {
            throw new MoveException(this, dest, "ssh failure", e);
        }
        return dest;
    }

    @Override
    public Node mkdir() throws MkdirException {
        try {
            channel.mkdir(slashPath);
            return this;
        } catch (SftpException e) {
            throw new MkdirException(this, e);
        }
    }

    private boolean noSuchFile(SftpException e) throws ExistsException {
        if (e.id == 2) {
            return false;
        }
        throw new ExistsException(this, e);
    }

    @Override
    public boolean exists() throws ExistsException {
        try {
            channel.lstat(slashPath);
            return true;
        } catch (SftpException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public boolean isFile() throws ExistsException {
        try {
            return !channel.stat(slashPath).isDir();
        } catch (SftpException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public boolean isDirectory() throws ExistsException {
        try {
            return channel.stat(slashPath).isDir();
        } catch (SftpException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public boolean isLink() throws ExistsException {
        try {
            return channel.lstat(slashPath).isLink();
        } catch (SftpException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public void mklink(String target) throws LinkException {
        try {
            checkNotExists();
            getParent().checkDirectory();
            channel.symlink(target, slashPath);
        } catch (SftpException e) {
            throw new LinkException(this, e);
        } catch (IOException e) {
            throw new LinkException(this, e);
        }
    }

    @Override
    public String readLink() throws ReadLinkException {
        try {
            return channel.readlink(slashPath);
        } catch (SftpException e) {
            throw new ReadLinkException(this, e);
        }
    }

    @Override
    public long getLastModified() throws GetLastModifiedException {
        try {
            return 1000L * channel.stat(slashPath).getMTime();
        } catch (SftpException e) {
            throw new GetLastModifiedException(this, e);
        }
    }

    @Override
    public void setLastModified(long millis) throws SetLastModifiedException {
        try {
            channel.setMtime(slashPath, (int) (millis / 1000));
        } catch (SftpException e) {
            throw new SetLastModifiedException(this, e);
        }
    }

    @Override
    public int getMode() throws IOException {
        try {
            return channel.stat(slashPath).getPermissions() & 0777;
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setMode(int mode) throws IOException {
        SftpATTRS stat;
        try {
            stat = channel.stat(slashPath);
            stat.setPERMISSIONS(mode);
            channel.setStat(slashPath, stat);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int getUid() throws IOException {
        try {
            return channel.stat(slashPath).getUId();
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setUid(int uid) throws IOException {
        String str;
        SftpATTRS stat;
        try {
            if (isDirectory()) {
                str = getRoot().exec("chown", Integer.toString(uid), slashPath);
                if (str.length() > 0) {
                    throw new IOException("chown failed:" + str);
                }
            } else {
                stat = channel.stat(slashPath);
                stat.setUIDGID(uid, stat.getGId());
                channel.setStat(slashPath, stat);
            }
        } catch (JSchException e) {
            throw new IOException(e);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int getGid() throws IOException {
        try {
            return channel.stat(slashPath).getGId();
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setGid(int gid) throws IOException {
        String str;
        SftpATTRS stat;
        try {
            if (isDirectory()) {
                str = getRoot().exec("chgrp", Integer.toString(gid), slashPath);
                if (str.length() > 0) {
                    throw new IOException("chgrp failed:" + str);
                }
            } else {
                stat = channel.stat(slashPath);
                stat.setUIDGID(stat.getUId(), gid);
                channel.setStat(slashPath, stat);
            }
        } catch (JSchException e) {
            throw new IOException(e);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public byte[] readBytes() throws IOException {
        ByteArrayOutputStream out;
        out = new ByteArrayOutputStream();
        get(out);
        return out.toByteArray();
    }

    @Override
    public InputStream createInputStream() throws IOException {
        final FileNode tmp;
        OutputStream out;
        tmp = getIO().getTemp().createTempFile();
        out = tmp.createOutputStream();
        get(out);
        out.close();
        return new FilterInputStream(tmp.createInputStream()) {

            @Override
            public void close() throws IOException {
                super.close();
                tmp.deleteOpt();
            }
        };
    }

    @Override
    public OutputStream createOutputStream(boolean append) throws IOException {
        byte[] add;
        if (append) {
            try {
                add = readBytes();
            } catch (FileNotFoundException e) {
                add = null;
            }
        } else {
            add = null;
        }
        return new CheckedByteArrayOutputStream(add) {

            @Override
            public void close() throws IOException {
                super.close();
                try {
                    put(toByteArray());
                } catch (JSchException e) {
                    throw new IOException(e);
                } catch (SftpException e) {
                    throw new IOException(e);
                }
            }
        };
    }

    /**
     * This is the core funktion to get a file.
     *
     * @throws FileNotFoundException if this is not a file
     */
    public void get(OutputStream out) throws IOException {
        try {
            channel.get(slashPath, out);
        } catch (SftpException e) {
            if (e.id == 2 || e.id == 4) {
                throw new FileNotFoundException(slashPath);
            }
            throw new IOException(e);
        }
    }

    public void put(final byte[] data) throws JSchException, IOException, SftpException {
        channel.put(new ByteArrayInputStream(data), slashPath);
    }
}
