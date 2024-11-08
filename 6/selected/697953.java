package net.sf.swarmnet.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.Properties;
import net.sf.swarmnet.common.message.TaskDescriptor;
import net.sf.swarmnet.node.SwarmNodeProxy;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public final class TaskContext {

    private static final String SHARED_WORKSPACE_LOGIN_NAME = "swarm";

    private Properties mProperties;

    private SwarmNodeProxy mSwarm;

    private File mLocalWorkspace;

    private String mWorkspacePassword;

    private final int mSharedWorkspacePort;

    /**
   * Constructor
   * 
   * @param swarm
   * @param taskDescription
   * @param localWorkspace
   * @throws IOException
   */
    public TaskContext(SwarmNodeProxy swarm, Properties taskDescription, File localWorkspace, String workspacePassword, int sharedWorkspacePort) throws IOException {
        mSwarm = swarm;
        mProperties = taskDescription;
        mLocalWorkspace = localWorkspace;
        mWorkspacePassword = workspacePassword;
        mSharedWorkspacePort = sharedWorkspacePort;
        if (!mLocalWorkspace.exists()) {
            if (!mLocalWorkspace.mkdirs()) {
                throw new IOException("Cannot create local workspace: " + mLocalWorkspace);
            }
        }
    }

    /**
   * @return the properties
   */
    public final Properties getProperties() {
        return mProperties;
    }

    public String getProperty(String key) {
        return mProperties.getProperty(key);
    }

    /**
   * @return the swarm
   */
    public final SwarmNodeProxy getSwarm() {
        return mSwarm;
    }

    /**
   * @return the localWorkspace
   */
    public final File getLocalWorkspace() {
        return mLocalWorkspace;
    }

    public void downloadFromSharedWorkspace(String from, String to) throws SocketException, IOException {
        if (!to.startsWith(mLocalWorkspace.getPath())) {
            to = mLocalWorkspace.getPath() + File.separator + to;
        }
        if (mProperties.getProperty(TaskDescriptor.WORKSPACE) != null) {
            from = mProperties.getProperty(TaskDescriptor.WORKSPACE) + "/" + from;
        }
        FTPClient ftp = null;
        try {
            ftp = loginToSharedWorkspace();
            if (!ftp.changeWorkingDirectory(from)) {
                throw new IllegalArgumentException("Unable to change to shared workspace directory " + from);
            }
            FTPFile[] files = ftp.listFiles();
            for (FTPFile f : files) {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(to + File.separator + f.getName()));
                try {
                    ftp.retrieveFile(from + "/" + f.getName(), bos);
                } finally {
                    bos.close();
                }
            }
            ftp.logout();
        } finally {
            if (null != ftp) {
                ftp.disconnect();
            }
        }
    }

    public void uploadToSharedWorkspace(String from, String to) throws SocketException, IOException {
        if (!from.startsWith(mLocalWorkspace.getPath())) {
            from = mLocalWorkspace.getPath() + File.separator + from;
        }
        if (mProperties.getProperty(TaskDescriptor.WORKSPACE) != null) {
            to = mProperties.getProperty(TaskDescriptor.WORKSPACE) + "/" + to;
        }
        FTPClient ftp = null;
        try {
            ftp = loginToSharedWorkspace();
            if (!ftp.changeWorkingDirectory(to)) {
                throw new IllegalArgumentException("Unable to change to shared workspace directory " + to);
            }
            File fromFile = new File(from);
            if (fromFile.isDirectory()) {
                File[] files = fromFile.listFiles();
                for (File f : files) {
                    BufferedInputStream bos = new BufferedInputStream(new FileInputStream(f));
                    try {
                        if (!ftp.storeFile(f.getName(), bos)) {
                            throw new IOException("Unable to store file " + to + "/" + f.getName());
                        }
                    } finally {
                        bos.close();
                    }
                }
            } else {
                BufferedInputStream bos = new BufferedInputStream(new FileInputStream(fromFile));
                try {
                    if (!ftp.storeFile(fromFile.getName(), bos)) {
                        throw new IOException("Unable to store file " + to + "/" + fromFile.getName());
                    }
                } finally {
                    bos.close();
                }
            }
            ftp.logout();
        } finally {
            if (null != ftp) {
                ftp.disconnect();
            }
        }
    }

    public void mkdirSharedWorkspace(String path) throws SocketException, IOException {
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        FTPClient ftp = null;
        try {
            ftp = loginToSharedWorkspace();
            String workingDir = mProperties.getProperty(TaskDescriptor.WORKSPACE);
            if (workingDir != null) {
                if (!ftp.changeWorkingDirectory(workingDir)) {
                    throw new IllegalArgumentException("Unable to change to shared workspace directory " + workingDir);
                }
            }
            if (!ftp.changeWorkingDirectory(path)) {
                if (!ftp.makeDirectory(path)) {
                    throw new IOException("Unable to create directory " + path);
                }
            }
            ftp.logout();
        } finally {
            if (null != ftp) {
                ftp.disconnect();
            }
        }
    }

    private FTPClient loginToSharedWorkspace() throws SocketException, IOException {
        FTPClient ftp = new FTPClient();
        ftp.connect(mSwarm.getHost(), mSharedWorkspacePort);
        if (!ftp.login(SHARED_WORKSPACE_LOGIN_NAME, mWorkspacePassword)) {
            throw new IOException("Unable to login to shared workspace.");
        }
        ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
        return ftp;
    }
}
