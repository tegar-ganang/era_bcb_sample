package org.antdepo.cli.addeploy;

import org.antdepo.AntdepoException;
import org.antdepo.common.Deployments;
import org.antdepo.common.Depot;
import org.antdepo.common.Framework;
import org.antdepo.utils.FileUtils;
import org.apache.log4j.Category;
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
 * Gets the deployment.properties file from the AntDepo repository
 */
public class GetDeploymentsFile {

    static Category logger = Category.getInstance(GetDeploymentsFile.class.getName());

    private final URL fileUrl;

    private final File destFile;

    private final String username;

    private final String password;

    static final boolean USETIMESTAMP = true;

    /**
     * Factory method. Calls the base constructor
     *
     * @param framework Framework instance
     * @param depot     Depot to get file for
     *
     * @return new instance of GetDeploymentsFile
     *
     * @throws MalformedURLException
     */
    public static GetDeploymentsFile create(final Framework framework, final Depot depot) throws MalformedURLException {
        final URL url = makeUrl(framework, depot.getName());
        final String user = framework.getProperty("framework.webdav.username");
        final String pass = framework.getProperty("framework.webdav.password");
        final File file = new File(depot.getEtcDir(), Deployments.BASE_NAME);
        return new GetDeploymentsFile(url, file, user, pass);
    }

    /**
     * Base constructor.
     *
     * @param fileUrl  Url to the file on the antdepo repo server
     * @param destFile File to write data to
     * @param username repo authentication username
     * @param password repo authentication password
     */
    private GetDeploymentsFile(final URL fileUrl, final File destFile, final String username, final String password) {
        this.fileUrl = fileUrl;
        this.destFile = destFile;
        this.username = username;
        this.password = password;
    }

    /**
     * Assemble a URL based on the framework configuration and depot name that will reference the depot's
     * deployments.properties file.
     *
     * @param framework Framework instance
     * @param depot     depot to get file for
     *
     * @return URL pointing to the file resource on the Antdepo repo
     *
     * @throws MalformedURLException
     */
    private static URL makeUrl(final Framework framework, final String depot) throws MalformedURLException {
        if (!framework.existsProperty("framework.webdav.uri")) {
            throw new IllegalArgumentException("framework.webdav.uri property not set");
        }
        final String webdavUri = framework.getProperty("framework.webdav.uri");
        final String resource = "/" + depot + "/etc/" + Deployments.BASE_NAME;
        final URL url = new URL(webdavUri + resource);
        logger.debug("url: " + url.toString());
        return url;
    }

    /**
     * Execute the file get action
     */
    public void execute() {
        final Project p = new Project();
        File lockFile = new File(destFile.getAbsolutePath() + ".lock");
        File newDestFile = new File(destFile.getAbsolutePath() + ".new");
        try {
            final Task task = createTask(fileUrl, newDestFile, username, password);
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
                                throw new AntdepoException("Unable to remove dest file on windows: " + destFile);
                            }
                            if (!newDestFile.renameTo(destFile)) {
                                throw new AntdepoException("Unable to move temp file to dest file on windows: " + newDestFile + ", " + destFile);
                            }
                        } else {
                            throw new AntdepoException("Unable to move temp file to dest file: " + newDestFile + ", " + destFile);
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
            throw new AntdepoException("Unable to get and write deployments properties file: " + e.getMessage(), e);
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
