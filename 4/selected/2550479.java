package de.schwarzrot.install.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import com.jgoodies.binding.PresentationModel;
import de.schwarzrot.app.errors.ApplicationException;
import de.schwarzrot.app.support.ApplicationServiceProvider;
import de.schwarzrot.app.support.ProgressTracker;
import de.schwarzrot.concurrent.Task;
import de.schwarzrot.install.domain.InstallationConfig;
import de.schwarzrot.system.CommandReader;
import de.schwarzrot.system.SysInfo;
import de.schwarzrot.system.support.FileUtils;

/**
 * a task definition executed by the installer. A task may be any processing
 * that could be performed from a daemon thread, as each task will be executed
 * as daemon. The given property will be checked before executing the task. The
 * property must be a boolean and if the value of the property is false, the
 * task will not be executed. That way, the task processing can be configured
 * even after setup of the tasks.
 * <p>
 * This baseclass for installer tasks will handle the property checking, as well
 * as the state handling of the task.
 * <p>
 * Derived classes should use {@code setSuccessState} to set the result of the
 * task. That way, tasks may be validated anonymously by a
 * {@code VCTaskExecutedSuccessfully}-constraint.
 * 
 * @author <a href="mailto:rmantey@users.sourceforge.net">Reinhard Mantey</a>
 * @param <B>
 *            - type of configuration entity
 * @see de.schwarzrot.ui.validation.constraints.VCTaskExecutedSuccessfully
 */
public abstract class AbstractInstallerTask<B extends InstallationConfig> extends Task {

    protected static SysInfo sysInfo = null;

    private static String protocolFilename = null;

    private static final List<String> lockFilenames;

    /**
     * each installer task needs the information collected by the installation
     * wizzard
     * 
     * @param pm
     *            - the presentation model, that manages the task configuration
     *            bean
     * @param dependencyPropertyName
     *            - the property that depends, whether the task should be
     *            executed
     * @param model
     *            - the progress tracker model used to receive messages
     */
    protected AbstractInstallerTask(PresentationModel<B> pm, String dependencyPropertyName, ProgressTracker model) {
        this(pm, dependencyPropertyName, model, "install.err");
    }

    /**
     * each installer task needs the information collected by the installation
     * wizzard
     * 
     * @param pm
     *            - the presentation model, that manages the task configuration
     *            bean
     * @param dependencyPropertyName
     *            - the property that depends, whether the task should be
     *            executed
     * @param model
     *            - the progress tracker model used to receive messages
     * @param errorFileName
     *            - the name of the file, that contains occurred exceptions
     */
    protected AbstractInstallerTask(PresentationModel<B> pm, String dependencyPropertyName, ProgressTracker model, String errorFileName) {
        super(null);
        this.beanModel = pm;
        this.progressModel = model;
        this.propertyName = dependencyPropertyName;
        this.errorFileName = errorFileName;
        if (sysInfo == null) sysInfo = ApplicationServiceProvider.getService(SysInfo.class);
    }

    public final String getErrorFileName() {
        return errorFileName;
    }

    public String getProtocolFilename() {
        return protocolFilename;
    }

    @Override
    public final void run() {
        try {
            if (shouldExecuteTask()) performTask(); else skipTask();
        } catch (Throwable t) {
            File log = new File(sysInfo.getTempDirectory(), "install.err");
            PrintStream ps;
            try {
                ps = new PrintStream(new FileOutputStream(log));
                t.printStackTrace(ps);
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl instanceof URLClassLoader) {
                    URL[] urls = null;
                    urls = ((URLClassLoader) cl).getURLs();
                    for (URL url : urls) {
                        ps.println("classpath-entry: " + url);
                    }
                }
                ps.close();
                t.printStackTrace();
            } catch (FileNotFoundException e1) {
            }
            if (t instanceof ApplicationException) throw (ApplicationException) t;
            throw new ApplicationException(t);
        } finally {
            setExecutionTerminated(true);
            ((TaskTrigger) getConfig()).triggerTaskWatcher();
        }
    }

    public final void setErrorFileName(String errorFileName) {
        this.errorFileName = errorFileName;
    }

    protected void copyFiles(List<String> files2Copy, Writer protocol) throws IOException {
        for (String curFile : files2Copy) {
            File srcFile = new File(curFile);
            File target = new File(getConfig().getTargetDir(), curFile);
            getModel().setMessage(new StringBuffer("install file ").append(curFile).toString());
            if (getConfig().isSudoTarget()) {
                if (!FileUtils.copyFile(target, srcFile, getConfig().getNixUser())) throw new ApplicationException("failed to copy " + curFile);
            } else {
                if (!FileUtils.copyFile(target, srcFile)) throw new ApplicationException("failed to copy " + curFile);
            }
            protocol.write(target.getAbsolutePath());
            protocol.write("\n");
        }
    }

    protected void execute(List<String> cmdArgs, boolean redirectError, File workingDir) {
        CommandReader cr = new CommandReader(cmdArgs);
        cr.setRedirectError(redirectError);
        cr.setDirectory(workingDir);
        try {
            Process proc = cr.start();
            BufferedReader br = cr.startReader(proc);
            String line;
            while ((line = br.readLine()) != null) {
                getModel().setMessage(line);
            }
        } catch (Exception e) {
            new ApplicationException("error executing command", e);
        }
    }

    protected final B getConfig() {
        return beanModel.getBean();
    }

    protected final ProgressTracker getModel() {
        return progressModel;
    }

    protected Writer getProtocolWriter() {
        Writer rv = null;
        try {
            if (protocolFilename == null) {
                File tmp = File.createTempFile("filelist", "log");
                tmp.deleteOnExit();
                protocolFilename = tmp.getAbsolutePath();
            }
            rv = new FileWriter(protocolFilename, true);
        } catch (Exception e) {
            throw new ApplicationException("error creating protocol file", e);
        }
        return rv;
    }

    protected void openConfigBaseDir() throws IOException {
        openConfigBaseDir(true);
    }

    protected void openConfigBaseDir(boolean mode) throws IOException {
        File configBase = getConfig().getConfigDir();
        String permission = mode ? "og+w" : "og-w";
        if (getConfig().getNixUser() != null) {
            String msg = new StringBuffer(mode ? "open" : "close").append(" configDir: ").append(configBase.getAbsolutePath()).toString();
            getModel().setMessage(msg);
            getLogger().info(msg);
            FileUtils.createDirectory(configBase, getConfig().getNixUser());
            FileUtils.setFilePermission(configBase, permission, getConfig().getNixUser(), true);
            if (mode) {
                for (String curLockFilename : lockFilenames) {
                    File lockFile = new File(configBase, curLockFilename);
                    if (lockFile.exists()) {
                        FileUtils.setFileOwner(lockFile, sysInfo.getUserName(), getConfig().getNixUser());
                    } else {
                        lockFile.createNewFile();
                    }
                }
            } else {
                FileUtils.setFileOwner(configBase, "0:0", getConfig().getNixUser(), true);
            }
        } else configBase.setWritable(mode);
    }

    protected abstract void performTask();

    protected boolean shouldExecuteTask() {
        boolean rv = true;
        String[] propertyNames = null;
        if (propertyName.contains("|")) {
            propertyNames = propertyName.split("\\s*\\|\\s*");
        } else propertyNames = new String[] { propertyName };
        for (String pn : propertyNames) {
            if (!(pn.compareTo("true") == 0 || (beanModel.getValue(pn) instanceof Boolean && (Boolean) beanModel.getValue(pn)))) {
                rv = false;
                break;
            }
        }
        return rv;
    }

    protected void skipTask() {
    }

    private final PresentationModel<B> beanModel;

    private final ProgressTracker progressModel;

    private final String propertyName;

    private String errorFileName;

    static {
        lockFilenames = new ArrayList<String>();
        lockFilenames.add(".system.lock");
        lockFilenames.add(".systemRootModFile");
    }
}
