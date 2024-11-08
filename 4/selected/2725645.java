package org.ourgrid.common.executor.vmachine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.FileUtils;
import org.ourgrid.common.executor.Executor;
import org.ourgrid.common.executor.ExecutorException;
import org.ourgrid.common.executor.ExecutorHandle;
import org.ourgrid.common.executor.ExecutorResult;
import org.ourgrid.common.executor.FolderBasedSandboxedUnixEnvironmentUtil;
import org.ourgrid.common.executor.IntegerExecutorHandle;
import org.ourgrid.common.executor.config.ExecutorConfiguration;
import org.ourgrid.common.executor.config.VirtualMachineExecutorConfiguration;
import org.ourgrid.worker.WorkerConstants;
import br.edu.ufcg.lsd.commune.container.logging.CommuneLogger;

/**
 * Executor that performs application executions on a sandboxed environment. The virtual environment
 * is started using predetermined scripts. Currently the only virtual machine supported by this 
 * executor is <i>VirtualBox (http://www.virtualbox.org)</i>.   
 */
public class VirtualMachineExecutor implements Executor {

    private static final long serialVersionUID = 40L;

    private static final String VIRTUAL_ENV_APP_SCRIPT_PROP = "APP_SCRIPT";

    private static final String VIRTUAL_ENV_APP_EXIT_PROP = "APP_EXIT";

    private static final String VIRTUAL_ENV_APP_STDOUT_PROP = "APP_STDOUT";

    private static final String VIRTUAL_ENV_APP_STDERR_PROP = "APP_STDERR";

    private static final String VIRTUAL_ENV_TERMINATION_FILE_PROP = "TERMINATION_FILE";

    private static final String VIRTUAL_ENV_STORAGE_NAME_PROP = "STORAGE_NAME";

    private static final String VIRTUAL_ENV_PROPS_FILES = "OG_OPTS";

    private static final String APP_EXIT = "app.exit";

    private static final String APP_STDERR = "app.stderr";

    private static final String APP_STDOUT = "app.stdout";

    private static final String APP_SCRIPT_SH = "app.script.sh";

    private static final String TERMINATED = ".terminated";

    private static final String VBOX_VM_STORAGE = "storage";

    private final transient CommuneLogger logger;

    private File virtualEnvPropertiesFile;

    private final Lock EXEC_LOCK = new ReentrantLock();

    private final Lock KILL_LOCK = new ReentrantLock();

    private static final IntegerExecutorHandle HANDLE = new IntegerExecutorHandle(1);

    private final Executor executor;

    private String startvmCmd;

    private String stopvmCmd;

    private String waitForExecutionCmd;

    private String machineName;

    private String vBoxLocation;

    private File terminationFile;

    private File execFile;

    private File stdOut;

    private File stdErr;

    private File exitStatus;

    private File vmStorage;

    private File playpen;

    private final FolderBasedSandboxedUnixEnvironmentUtil unixFolderUtil;

    /**
	 * Creates a new <code>VirtualMachineExecutor</code> using the
	 * given <code>Executor</code> to perform tasks.
	 * 
	 * @param osExecutor <code>Executor</code> that will execute scripts.
	 * @param logger 
	 */
    public VirtualMachineExecutor(Executor osExecutor, CommuneLogger logger) {
        this.executor = osExecutor;
        this.unixFolderUtil = new FolderBasedSandboxedUnixEnvironmentUtil();
        this.logger = logger;
    }

    public void setConfiguration(ExecutorConfiguration executorConfiguratrion) {
        this.startvmCmd = ("\"" + executorConfiguratrion.getProperty(VirtualMachineExecutorConfiguration.PROPERTY_START_VM_COMMAND) + "\"");
        this.stopvmCmd = ("\"" + executorConfiguratrion.getProperty(VirtualMachineExecutorConfiguration.PROPERTY_STOP_VM_COMMAND) + "\"");
        this.waitForExecutionCmd = ("\"" + executorConfiguratrion.getProperty(VirtualMachineExecutorConfiguration.PROPERTY_WAIT_FOR_VM_COMMAND) + "\"");
        this.vBoxLocation = ("\"" + executorConfiguratrion.getProperty(VirtualMachineExecutorConfiguration.PROPERTY_VBOX_LOCATION) + "\"");
        this.machineName = executorConfiguratrion.getProperty(VirtualMachineExecutorConfiguration.PROPERTY_MACHINE_NAME);
    }

    public void beginAllocation() throws ExecutorException {
    }

    public synchronized ExecutorHandle execute(String dirName, String command) throws ExecutorException {
        return execute(dirName, command, new HashMap<String, String>());
    }

    public synchronized ExecutorHandle execute(String dirName, String command, Map<String, String> envVars) throws ExecutorException {
        EXEC_LOCK.lock();
        try {
            if (isExecutionInProcess()) {
                throw new ExecutorException("An execution is in process");
            }
            initEnvironmentVariables(envVars);
            executeRemoteCommand(dirName, command, envVars);
        } finally {
            EXEC_LOCK.unlock();
        }
        return HANDLE;
    }

    private void initEnvironmentVariables(Map<String, String> envVars) throws ExecutorException {
        String uniquifier = Integer.toString((int) (Math.random() * Integer.MAX_VALUE));
        this.playpen = new File(envVars.get(WorkerConstants.ENV_PLAYPEN));
        this.vmStorage = new File(playpen.getAbsolutePath() + File.separator + uniquifier + VBOX_VM_STORAGE);
        this.terminationFile = new File(playpen.getAbsolutePath() + File.separator + uniquifier + TERMINATED);
        this.execFile = new File(playpen.getAbsolutePath() + File.separator + uniquifier + APP_SCRIPT_SH);
        this.stdOut = new File(playpen.getAbsolutePath() + File.separator + uniquifier + APP_STDOUT);
        this.stdErr = new File(playpen.getAbsolutePath() + File.separator + uniquifier + APP_STDERR);
        this.exitStatus = new File(playpen.getAbsolutePath() + File.separator + uniquifier + APP_EXIT);
        this.virtualEnvPropertiesFile = new File(playpen.getAbsolutePath() + File.separator + VIRTUAL_ENV_PROPS_FILES);
    }

    private void executeRemoteCommand(String dirName, String command, Map<String, String> envVars) throws ExecutorException {
        try {
            String env_storage = envVars.get(WorkerConstants.ENV_STORAGE);
            command = command.replace(env_storage, vmStorage.getName());
            logger.info("Asked to run command " + command);
            Map<String, String> clone = new HashMap<String, String>();
            clone.putAll(envVars);
            clone.remove(WorkerConstants.ENV_PLAYPEN);
            clone.remove(WorkerConstants.ENV_STORAGE);
            File script = unixFolderUtil.createScript(command, dirName, clone);
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(virtualEnvPropertiesFile)));
            writer.println(VIRTUAL_ENV_APP_SCRIPT_PROP + "=" + execFile.getName());
            writer.println(VIRTUAL_ENV_APP_EXIT_PROP + "=" + exitStatus.getName());
            writer.println(VIRTUAL_ENV_APP_STDOUT_PROP + "=" + stdOut.getName());
            writer.println(VIRTUAL_ENV_APP_STDERR_PROP + "=" + stdErr.getName());
            writer.println(VIRTUAL_ENV_TERMINATION_FILE_PROP + "=" + terminationFile.getName());
            writer.println(VIRTUAL_ENV_STORAGE_NAME_PROP + "=" + vmStorage.getName());
            try {
                if (writer.checkError()) {
                    throw new IOException("Unable to create Virtual environment");
                }
            } finally {
                writer.close();
            }
            vmStorage.mkdirs();
            FileUtils.copyFile(script, execFile);
            unixFolderUtil.copyStorageFiles(envVars, vmStorage);
        } catch (IOException e) {
            throw new ExecutorException("Unable to create remote execution script", e);
        }
        logger.debug("About to start secure environment");
        ExecutorHandle internalHandle = this.executor.execute(dirName, startvmCmd + " " + vBoxLocation + " " + machineName + " " + ("\"" + playpen.getAbsolutePath() + "\""));
        ExecutorResult result = this.executor.getResult(internalHandle);
        logger.debug("Result: " + result);
        if (result.getExitValue() != 0) {
            cleanup();
            throw new ExecutorException("Unable to start virtual environment \n" + result);
        }
    }

    public synchronized ExecutorResult getResult(ExecutorHandle handle) throws ExecutorException {
        try {
            logger.debug("About to wait for secure environment to exit");
            String command = waitForExecutionCmd + " " + vBoxLocation + " " + machineName + " " + ("\"" + terminationFile.getAbsolutePath() + "\"");
            ExecutorHandle waitHandle = this.executor.execute(playpen.getAbsolutePath(), command);
            ExecutorResult result = this.executor.getResult(waitHandle);
            logger.debug("Result: " + result);
            if (result.getExitValue() != 0) {
                throw new ExecutorException("Wait command has finished with errors\n" + result);
            }
            return getResult();
        } catch (ExecutorException e) {
            throw new ExecutorException("Unable to wait for application", e);
        } finally {
            KILL_LOCK.lock();
            try {
                cleanup();
            } finally {
                KILL_LOCK.unlock();
            }
        }
    }

    private ExecutorResult getResult() throws ExecutorException {
        KILL_LOCK.lock();
        try {
            if (!isExecutionInProcess()) {
                throw new ExecutorException("No execution is in process, probably it was killed.");
            }
            ExecutorResult result = new ExecutorResult();
            try {
                unixFolderUtil.catchOutputFromFile(result, stdOut, stdErr, exitStatus);
            } catch (Throwable e) {
                throw new ExecutorException("Unable to catch output ", e);
            } finally {
                stopVm();
            }
            return result;
        } finally {
            KILL_LOCK.unlock();
        }
    }

    public void kill(ExecutorHandle handle) throws ExecutorException {
        try {
            KILL_LOCK.lock();
            EXEC_LOCK.lock();
            if (!isExecutionInProcess()) {
                throw new ExecutorException("No execution is in process");
            }
            stopVm();
        } finally {
            cleanup();
            KILL_LOCK.unlock();
            EXEC_LOCK.unlock();
        }
    }

    private void stopVm() throws ExecutorException {
        logger.debug("About to kill secure environment");
        ExecutorHandle internalHandle = executor.execute(playpen.getAbsolutePath(), stopvmCmd + " " + vBoxLocation + " " + machineName);
        ExecutorResult result = executor.getResult(internalHandle);
        logger.debug("Result: " + result);
        if (result.getExitValue() != 0) {
            throw new ExecutorException("Unable to kill virtual environment \n" + result);
        }
    }

    private void cleanup() {
        this.playpen = null;
        this.vmStorage = null;
        this.terminationFile = null;
        this.execFile = null;
        this.stdOut = null;
        this.stdErr = null;
        this.exitStatus = null;
    }

    protected boolean isExecutionInProcess() {
        return playpen != null;
    }

    public void chmod(File file, String perm) throws ExecutorException {
        executor.chmod(file, perm);
    }

    public void finishExecution() throws ExecutorException {
    }
}
