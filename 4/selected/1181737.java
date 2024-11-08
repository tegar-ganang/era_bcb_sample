package com.controltier.ctl.cli.ctldeploy;

import com.controltier.ctl.CtlException;
import com.controltier.ctl.common.Depot;
import com.controltier.ctl.common.Framework;
import com.controltier.ctl.common.Executable;
import com.controltier.ctl.utils.FileUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Get;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Gets a file from the Ctl repository for a specific depot
 */
public class GetServerFile implements Executable {

    static Logger logger = Logger.getLogger(GetServerFile.class.getName());

    private final File destFile;

    private final String davUsername;

    private final String davPassword;

    private final String davUri;

    private final String davPath;

    private final Depot depot;

    static final boolean USETIMESTAMP = true;

    /**
     * Factory method. Calls the base constructor
     *
     * @param framework Framework instance
     * @param depot     Depot to get file for
     * @param destPath  Destination file path
     * @param path      URL path for the source file, relative to depot
     *
     * @return new instance of GetDeploymentsFile
     */
    public static GetServerFile create(final Framework framework, final Depot depot, final File destPath, final String path) {
        final String uri;
        if (framework.getPropertyLookup().hasProperty("framework.webdav.uri")) {
            uri = framework.getPropertyLookup().getProperty("framework.webdav.uri");
        } else {
            uri = null;
        }
        final String user;
        if (framework.getPropertyLookup().hasProperty("framework.webdav.username")) {
            user = framework.getPropertyLookup().getProperty("framework.webdav.username");
        } else {
            user = null;
        }
        final String pass;
        if (framework.getPropertyLookup().hasProperty("framework.webdav.password")) {
            pass = framework.getPropertyLookup().getProperty("framework.webdav.password");
        } else {
            pass = null;
        }
        return new GetServerFile(depot, destPath, path, uri, user, pass);
    }

    /**
     * Base constructor.
     * @param depot Depot instance
     * @param path URL path for the source file, relative to depot
     * @param destFile File to write data to
     * @param davUri repo Url to the file on the ctl repo server
     * @param davUsername repo authentication davUsername
     * @param davPassword repo authentication davPassword
     */
    protected GetServerFile(final Depot depot, final File destFile, final String path, final String davUri, final String davUsername, final String davPassword) {
        this.depot = depot;
        this.destFile = destFile;
        this.davPath = path;
        this.davUri = davUri;
        this.davUsername = davUsername;
        this.davPassword = davPassword;
    }

    /**
     * Assemble a URL based on the framework configuration and depot name that will reference the depot's
     * deployments.properties file.
     *
     * @param path      path for the source file, relative to depot
     * @param depot     depot to get file for
     *
     * @return URL pointing to the file resource on the Ctl repo
     *
     * @throws java.net.MalformedURLException  throws exception if input params result in a bad url
     */
    private URL makeUrl(final String depot, final String path) throws MalformedURLException {
        final String resource = "/" + depot + path;
        final URL url = new URL(davUri + resource);
        logger.debug("formulated url: " + url.toString());
        return url;
    }

    /**
     * Execute the file get action. If the davUri is unset then
     * nothing will be done.
     */
    public void execute() {
        if (null == davUri) {
            logger.debug("Skipping retreival of web resource. The framework.webdav.uri property is unset.");
            return;
        }
        final Project p = new Project();
        File lockFile = new File(destFile.getAbsolutePath() + ".lock");
        File newDestFile = new File(destFile.getAbsolutePath() + ".new");
        final URL fileUrl;
        try {
            fileUrl = makeUrl(depot.getName(), davPath);
        } catch (MalformedURLException e) {
            throw new CtlException("Input data for URL formulation caused an error. " + "Params: depot=" + depot.getName() + ", davUri=" + davUri + ", davPath=" + davPath, e);
        }
        try {
            final Task task = createTask(fileUrl, newDestFile, davUsername, davPassword);
            task.setProject(p);
            FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
            FileLock lock = channel.lock();
            try {
                synchronized (GetDeploymentsFile.class) {
                    int c = 0;
                    FileUtils.copyFileStreams(destFile, newDestFile);
                    newDestFile.setLastModified(destFile.lastModified());
                    task.execute();
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
                }
            } finally {
                lock.release();
                channel.close();
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace(System.err);
            throw new CtlException("Unable to get and write deployments properties file: " + e.getMessage(), e);
        }
    }

    private Task createTask(final URL fileUrl, final File destFile, final String username, final String password) {
        final Get getTask = new Get();
        getTask.setDest(destFile);
        getTask.setPassword(password);
        getTask.setUsername(username);
        getTask.setSrc(fileUrl);
        getTask.setUseTimestamp(USETIMESTAMP);
        return getTask;
    }

    /**
     * Getter to destFile;
     *
     * @return the destination file
     */
    public File getDestFile() {
        return destFile;
    }
}
