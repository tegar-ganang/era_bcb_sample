package org.echarts;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.Watchdog;

/**
 * ContainerHandler for local invocation of WebSphere's wsadmin script.
 */
public class LocalWebSphereContainerHandler implements ContainerHandler {

    /**
     * Called to deploy a set of artifacts to the server and start the
   	 * server.
 	 */
    public void deploy(List<Deployable> deployables, AbstractDeployMojo mojo) throws MojoExecutionException {
        try {
            if (mojo.useDFCApplicationRouter && (mojo.dfcApplicationRouterConfig != null) && (mojo.dfcApplicationRouterConfig.length() > 0)) {
                File approuter = new File(mojo.dfcApplicationRouterConfig);
                if ((mojo.wsadminApprouterConfigPath != null) && (mojo.wsadminApprouterConfigPath.length() > 0)) {
                    File dest = new File(mojo.wsadminApprouterConfigPath);
                    if (dest.isDirectory()) {
                        FileUtils.copyFileToDirectory(approuter, dest);
                    } else {
                        File parent = dest.getParentFile();
                        if (!parent.exists()) {
                            throw new MojoExecutionException("Directory for wsadminApprouterConfigPath (" + mojo.wsadminApprouterConfigPath + ") does not exist.");
                        }
                        FileUtils.copyFile(approuter, dest);
                    }
                }
            }
            for (Deployable d : mojo.deployables) {
                boolean ignoreFailureToStart = false;
                try {
                    executeWsadminCommand(mojo.getLog(), mojo.wsadminPath, WebSphereCommands.getInstallCommand(d.path, d.context, d.context));
                } catch (ExecException exe) {
                    String onAppExists = mojo.onApplicationExists;
                    if (onAppExists == null) {
                        onAppExists = "redeploy";
                    }
                    if (onAppExists.equalsIgnoreCase("warn")) {
                        ignoreFailureToStart = true;
                        mojo.getLog().warn("Attempt to deploy " + d + " failed because the app is already " + " installed. Ignoring (onApplicationExists==" + "warn)");
                    } else if (onAppExists.equalsIgnoreCase("fail")) {
                        throw exe;
                    } else {
                        mojo.getLog().warn("Attempt to deploy " + d + " failed with exit code " + exe.getExitCode() + ". Attempting to uninstall and redeploy. " + "Set attemptRedeploy=false to disable this.");
                        ArrayList<Deployable> l = new ArrayList<Deployable>();
                        l.add(d);
                        undeploy(l, mojo);
                        executeWsadminCommand(mojo.getLog(), mojo.wsadminPath, WebSphereCommands.getInstallCommand(d.path, d.context, d.context));
                    }
                }
                try {
                    executeWsadminCommand(mojo.getLog(), mojo.wsadminPath, WebSphereCommands.getStartCommand(d.context));
                } catch (ExecException exe) {
                    if (ignoreFailureToStart) {
                        mojo.getLog().warn("Ingoring failure to start " + d + " (app was already installed)");
                    } else {
                        throw exe;
                    }
                }
            }
        } catch (IOException jse) {
            throw new MojoExecutionException("Error deploying to local WebSphere instance", jse);
        }
    }

    /**
     * Called to undeploy a set of artifacts to the server and stop the
   	 * server.
 	 */
    public void undeploy(List<Deployable> deployables, AbstractDeployMojo mojo) throws MojoExecutionException {
        try {
            for (Deployable d : mojo.deployables) {
                mojo.getLog().info("Stopping and uninstalling " + d.context);
                try {
                    executeWsadminCommand(mojo.getLog(), mojo.wsadminPath, WebSphereCommands.getStopCommand(d.context));
                } catch (ExecException exe) {
                    if (exe.getOutput().indexOf("not started") > -1) {
                        mojo.getLog().warn("Ignoring failure to stop " + d.context);
                    } else {
                        throw exe;
                    }
                }
                executeWsadminCommand(mojo.getLog(), mojo.wsadminPath, WebSphereCommands.getUninstallCommand(d.context));
            }
        } catch (Exception jse) {
            throw new MojoExecutionException("Error undeploying from local WebSphere instance", jse);
        }
    }

    /**
	 * 120130: wsadmin fails silently, so I've tried switching from
	 * Apache CommandLine to the JDK ProcessBuilder. This doesn't help,
	 * but it does make it slightly easier to get at the command output.
	 */
    private void executeWsadminCommand(Log log, String wsadmin, String command) throws MojoExecutionException {
        try {
            ProcessBuilder pb = new ProcessBuilder(wsadmin, "-lang", "jython", "-c", command);
            pb.redirectErrorStream(true);
            log.debug("Executing: " + pb.command());
            Process p = pb.start();
            StringBuffer output = new StringBuffer();
            String line = "";
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                while ((line = in.readLine()) != null) {
                    log.info(line);
                    output.append(line);
                }
            } finally {
                in.close();
            }
            int exitValue = p.waitFor();
            log.debug("Process existed with code: " + exitValue);
            if (exitValue != 0) {
                throw new ExecException(wsadmin + " failed with exit code " + exitValue, exitValue, output.toString());
            }
        } catch (MojoExecutionException mee) {
            throw mee;
        } catch (Exception ee) {
            throw new MojoExecutionException("Error executing " + wsadmin, ee);
        }
    }
}
