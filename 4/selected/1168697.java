package com.controltier.ctl.cli.ctldeploy;

import com.controltier.ctl.CtlException;
import com.controltier.ctl.common.Executable;
import com.controltier.ctl.utils.FileUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Copies a file from a local source to a destination within the CTL framework using appropriate
 * locks and synchronization to prevent conflict if the same file is written to
 * with multiple threads or processes.
 */
public class GetLocalFile implements Executable {

    static Logger logger = Logger.getLogger(GetLocalFile.class.getName());

    private final File sourceFile;

    private final File destFile;

    static final boolean USETIMESTAMP = true;

    /**
     * Factory method. Calls the base constructor
     *
     * @param framework Framework instance
     * @param destFile  Destination file path
     * @param sourceFile  Source file path
     *
     * @return new instance of GetLocalFile
     */
    public static GetLocalFile create(final File destFile, final File sourceFile) {
        return new GetLocalFile(destFile, sourceFile);
    }

    /**
     * Base constructor.
     * @param destFile File to write data to
     * @param destFile File to read from
     */
    protected GetLocalFile(final File destFile, final File sourceFile) {
        this.destFile = destFile;
        this.sourceFile = sourceFile;
    }

    /**
     * Execute the file get action. If the davUri is unset then
     * nothing will be done.
     */
    public void execute() {
        final Project p = new Project();
        File lockFile = new File(destFile.getAbsolutePath() + ".lock");
        File newDestFile = new File(destFile.getAbsolutePath() + ".new");
        try {
            synchronized (GetLocalFile.class) {
                FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
                FileLock lock = channel.lock();
                try {
                    FileUtils.copyFileStreams(destFile, newDestFile);
                    newDestFile.setLastModified(destFile.lastModified());
                    FileUtils.copyFileStreams(sourceFile, newDestFile);
                    String osName = System.getProperty("os.name");
                    if (!newDestFile.renameTo(destFile)) {
                        if (osName.toLowerCase().indexOf("windows") > -1 && destFile.exists()) {
                            if (!destFile.delete()) {
                                throw new CtlException("Unable to remove dest file on windows: " + destFile);
                            }
                            if (!newDestFile.renameTo(destFile)) {
                                throw new CtlException("Unable to move temp file to dest file on windows: " + newDestFile + ", " + destFile);
                            }
                        } else {
                            throw new CtlException("Unable to move temp file to dest file: " + newDestFile + ", " + destFile);
                        }
                    }
                } finally {
                    lock.release();
                    channel.close();
                }
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new CtlException("Unable to get and write resources.properties file: " + e.getMessage(), e);
        }
    }
}
