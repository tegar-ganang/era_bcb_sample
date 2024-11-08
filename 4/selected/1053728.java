package bsys.launcher;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.selectors.FilenameSelector;

/**
 * Launcher (actually an Ant launcher wrap-around)
 * 
 * @author <a href="mailto:bbou@ac-toulouse.fr">Bernard Bou</a>
 */
public class Launcher {

    /**
	 * Max heap
	 */
    public static String MAXMEM = "1024M";

    /**
	 * JVNM args
	 */
    public static String JVMARGS = "-Xms1024M -Xmx1024M";

    /**
	 * Name tag for process
	 */
    public static String NAME = "wnscope";

    /**
	 * Run
	 * 
	 * @param spawn
	 *            whether to span process
	 * @param thisClassName
	 *            class name
	 * @param theseArguments
	 *            args
	 * @param theseVars
	 *            system variables
	 * @param theseExtraJars
	 *            extra jars to add in path
	 * @return code
	 */
    public static int run(final boolean spawn, final String thisClassName, final String[] theseArguments, final Environment.Variable[] theseVars, final String... theseExtraJars) {
        final DefaultLogger logger = new DefaultLogger();
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        logger.setMessageOutputLevel(Project.MSG_INFO);
        final Project thisProject = new Project();
        thisProject.setBaseDir(new File(System.getProperty("user.dir")));
        thisProject.init();
        thisProject.addBuildListener(logger);
        System.setOut(new PrintStream(new DemuxOutputStream(thisProject, false)));
        System.setErr(new PrintStream(new DemuxOutputStream(thisProject, true)));
        thisProject.fireBuildStarted();
        Throwable thisCaughtThrowable = null;
        int thisReturnCode = -1;
        try {
            final Echo thisEcho = new Echo();
            thisEcho.setTaskName("control");
            thisEcho.setProject(thisProject);
            thisEcho.init();
            thisEcho.setMessage("Launching " + thisClassName);
            thisEcho.execute();
            final Path thisPath = Launcher.makePath(thisProject, theseExtraJars);
            final Java thisTask = new Java();
            thisTask.setTaskName(Launcher.NAME);
            thisTask.setProject(thisProject);
            thisTask.setFork(true);
            thisTask.setSpawn(spawn);
            thisTask.setFailonerror(true);
            thisTask.setClassname(thisClassName);
            thisTask.setClasspath(thisPath);
            thisTask.setMaxmemory(Launcher.MAXMEM);
            if (theseVars != null) {
                for (final Environment.Variable thisVar : theseVars) {
                    thisTask.addSysproperty(thisVar);
                }
            }
            if (theseArguments != null) {
                for (final String thisArg : theseArguments) {
                    final Commandline.Argument thisCommandLineArg = thisTask.getCommandLine().createArgument();
                    thisCommandLineArg.setValue(thisArg);
                }
            }
            thisTask.init();
            thisReturnCode = thisTask.executeJava();
            final Echo thisEcho2 = new Echo();
            thisEcho2.setTaskName("control");
            thisEcho2.setProject(thisProject);
            thisEcho2.init();
            thisEcho2.setMessage("Terminated");
            thisEcho2.execute();
        } catch (final BuildException e) {
            thisCaughtThrowable = e;
        }
        thisProject.fireBuildFinished(thisCaughtThrowable);
        return thisReturnCode;
    }

    /**
	 * Make environment variable
	 * 
	 * @param thisKey
	 *            key
	 * @param thisValue
	 *            value
	 * @return environment variable
	 */
    public static Environment.Variable makeEnvironmentVariable(final String thisKey, final String thisValue) {
        final Environment.Variable thisVar = new Environment.Variable();
        thisVar.setKey(thisKey);
        thisVar.setValue(thisValue);
        thisVar.validate();
        return thisVar;
    }

    /**
	 * Make filepath
	 * 
	 * @param thisProject
	 *            project
	 * @param theseFilePaths
	 *            filepaths to add
	 * @return path
	 */
    private static Path makePath(final Project thisProject, final String... theseFilePaths) {
        final Path path = Launcher.clonePath(thisProject);
        Launcher.addFilePaths(path, theseFilePaths);
        return path;
    }

    /**
	 * Clone current path
	 * 
	 * @param thisProject
	 *            project
	 * @return cloned path
	 */
    private static Path clonePath(final Project thisProject) {
        final Path thisPath = new Path(thisProject);
        final Path thisSystemBootPath = thisPath.concatSystemBootClasspath("first");
        thisPath.append(thisSystemBootPath);
        final Path thisSystemClassPath = thisPath.concatSystemClasspath("first");
        thisPath.append(thisSystemClassPath);
        return thisPath;
    }

    /**
	 * Add filepaths to path
	 * 
	 * @param thisPath
	 *            path to add to
	 * @param theseFilePaths
	 *            filepaths
	 * @return path
	 */
    private static Path addFilePaths(final Path thisPath, final String... theseFilePaths) {
        for (final String thisFilePath : theseFilePaths) {
            final File thisBase = new File(thisFilePath);
            final File thisDir = thisBase.getParentFile();
            final FileSet thisFileset = new FileSet();
            thisFileset.setDir(thisDir);
            final FilenameSelector thisfilenameSelector = new FilenameSelector();
            thisfilenameSelector.setName(thisBase.getName());
            thisFileset.addFilename(thisfilenameSelector);
            thisFileset.add(thisfilenameSelector);
            thisPath.add(thisFileset);
        }
        return thisPath;
    }

    /**
	 * @param args
	 * @throws IOException
	 */
    public static void main(final String[] args) throws IOException {
        final File thisPropertyFile = new File("../WordNetTransWizard/read-write.properties");
        System.out.println("run entered");
        Launcher.run(false, Test.class.getName(), new String[] { thisPropertyFile.getCanonicalPath() }, null, "/home/bbou/.m2/repository/org/xerial/sqlite-jdbc/3.7.2/sqlite-jdbc-3.7.2.jar");
        System.out.println("run returned");
    }
}
