package com.mindtree.techworks.infix.plugins.nsis.actions;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;

/**
 * Compiles the generated NSIS script.
 * 
 * @author Bindul Bhowmik
 * @version $Revision: 79 $ $Date: 2010-12-05 08:50:09 -0500 (Sun, 05 Dec 2010) $
 */
@Component(role = NsisAction.class, hint = "compile")
public class NsisCompileAction implements NsisAction, NsisScriptConstants {

    @Override
    public void execute(MojoInfo mojoInfo) throws NsisActionExecutionException {
        int logVerbosity = 0;
        Log log = mojoInfo.getLog();
        if (log.isErrorEnabled()) {
            logVerbosity = 1;
        }
        if (log.isWarnEnabled()) {
            logVerbosity = 2;
        }
        if (log.isInfoEnabled()) {
            logVerbosity = 3;
        }
        if (log.isDebugEnabled()) {
            logVerbosity = 4;
        }
        Commandline commandline = new Commandline();
        commandline.setExecutable(getNsisExecutablePath(mojoInfo));
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            commandline.createArg().setValue("/V" + logVerbosity);
        } else {
            commandline.createArg().setValue("-V" + logVerbosity);
        }
        commandline.createArg().setValue(OP_FILE_SETUP_MUI);
        commandline.setWorkingDirectory(mojoInfo.getWorkDirectory());
        log.info("About to execute: " + commandline.toString());
        try {
            int response = CommandLineUtils.executeCommandLine(commandline, new DefaultConsumer(), new DefaultConsumer());
            if (response != 0) {
                throw new NsisActionExecutionException("NSIS Compiler returned error: " + response);
            }
            File outputFile = new File(mojoInfo.getWorkDirectory(), mojoInfo.getNsisProject().getInstallerSettings().getOutFile());
            FileUtils.copyFileToDirectory(outputFile, new File(mojoInfo.getProject().getBuild().getDirectory()));
            mojoInfo.getProjectHelper().attachArtifact(mojoInfo.getProject(), "exe", "setup", new File(mojoInfo.getProject().getBuild().getDirectory(), outputFile.getName()));
        } catch (CommandLineException e) {
            log.error("Executable failed", e);
            throw new NsisActionExecutionException("Executable failed", e);
        } catch (IOException e) {
            log.error("Error copying final output", e);
            throw new NsisActionExecutionException("Error copying final output", e);
        }
    }

    /**
	 * Gets the executable path
	 */
    private String getNsisExecutablePath(MojoInfo mojoInfo) throws NsisActionExecutionException {
        Log log = mojoInfo.getLog();
        String executable = mojoInfo.getNsisExecutable();
        if (null != mojoInfo.getNsisPath()) {
            File nsisExecPath = new File(mojoInfo.getNsisPath(), executable);
            if (!nsisExecPath.exists()) {
                log.error("The NSIS executable [" + executable + "] is not " + "available at the requested path. [" + mojoInfo.getNsisPath().getAbsolutePath() + "]");
                throw new NsisActionExecutionException("The NSIS executable [" + executable + "] is not available at the requested path. [" + mojoInfo.getNsisPath().getAbsolutePath() + "]");
            }
            return nsisExecPath.getAbsolutePath();
        }
        return executable;
    }
}
