package util.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Properties;
import java.util.Set;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.NullEnumeration;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 * The Class FileOperator.
 * 
 * @author Timo
 */
public final class FileOperator {

    private static final Logger LOG = Logger.getLogger(FileOperator.class);

    /**
	 * Copy file.
	 * 
	 * @param in
	 *            the in
	 * @param out
	 *            the out
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public static void copyFile(final File in, final File out) throws IOException {
        final FileChannel inChannel = new FileInputStream(in).getChannel();
        final FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    /**
	 * Load file.
	 * 
	 * @param filePath
	 *            the file path
	 * 
	 * @return the string
	 */
    public static String loadFile(final String filePath) {
        return FileOperator.loadFile(new File(filePath));
    }

    /**
	 * Load file.
	 * 
	 * @param file
	 *            the file
	 * 
	 * @return the string
	 */
    public static String loadFile(final File file) {
        final StringBuilder contents = new StringBuilder();
        try {
            final BufferedReader input = new BufferedReader(new FileReader(file));
            try {
                String line = null;
                while ((line = input.readLine()) != null) {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator"));
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        return contents.toString();
    }

    /**
	 * Run build file.
	 * 
	 * @param buildFile
	 *            the build file
	 * @param target
	 *            the target
	 * @param customProperties
	 *            the custom properties
	 */
    public static void runBuildFile(final File buildFile, final String target, final Properties customProperties) {
        new BuildFileRunner(buildFile, target, customProperties).run();
    }

    /**
	 * Load build file.
	 * 
	 * @param buildFile
	 *            the build file
	 * 
	 * @return the project
	 */
    public static Project loadBuildFile(final File buildFile) {
        final Project p = new Project();
        p.setUserProperty("ant.file", buildFile.getAbsolutePath());
        p.init();
        final ProjectHelper helper = ProjectHelper.getProjectHelper();
        p.addReference("ant.projectHelper", helper);
        helper.parse(p, buildFile);
        return p;
    }

    /**
	 * The Class BuildFileRunner.
	 */
    static class BuildFileRunner implements Runnable {

        /** The build file. */
        private final File buildFile;

        /** The target. */
        private final String target;

        /** The custom properties. */
        private final Properties customProperties;

        /**
		 * Instantiates a new builds the file runner.
		 * 
		 * @param buildFile
		 *            the build file
		 * @param target
		 *            the target
		 * @param customProperties
		 *            the custom properties
		 */
        public BuildFileRunner(final File buildFile, final String target, final Properties customProperties) {
            this.buildFile = buildFile;
            this.target = target;
            this.customProperties = customProperties;
        }

        public void run() {
            final Project p = new Project();
            p.setUserProperty("ant.file", buildFile.getAbsolutePath());
            final Set<Object> keySet = customProperties.keySet();
            for (Object item : keySet) {
                final String key = item.toString();
                p.setProperty(key, customProperties.getProperty(key));
            }
            p.init();
            final Log4jListener consoleLogger = new Log4jListener();
            p.addBuildListener(consoleLogger);
            try {
                p.fireBuildStarted();
                p.init();
                final ProjectHelper helper = ProjectHelper.getProjectHelper();
                p.addReference("ant.projectHelper", helper);
                helper.parse(p, buildFile);
                if (target.isEmpty()) {
                    p.executeTarget(p.getDefaultTarget());
                } else {
                    p.executeTarget(target);
                }
                p.fireBuildFinished(null);
            } catch (BuildException e) {
                p.fireBuildFinished(e);
            }
        }
    }

    /**
	 * Gets the current path.
	 * 
	 * @return the current path
	 */
    public static String getCurrentPath() {
        final File file = new File(".");
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            LOG.error(e, e);
        }
        return ".";
    }

    /**
	 * Creates the or load file.
	 * 
	 * @param path
	 *            the path
	 * 
	 * @return the file
	 */
    public static File createOrLoadFile(String path) {
        final File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Can't create file : " + path);
            }
        }
        return file;
    }

    /**
	 * Merge template.
	 * 
	 * @param template
	 *            the template
	 * @param context
	 *            the context
	 * @param fileName
	 *            the file name
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public static void mergeTemplate(final Template template, final VelocityContext context, final String fileName) throws IOException {
        final File file = FileOperator.createOrLoadFile(fileName);
        final FileWriter fileWriter = new FileWriter(file);
        final BufferedWriter writer = new BufferedWriter(fileWriter);
        mergeTemplate(template, context, writer);
    }

    /**
	 * Merge template.
	 * 
	 * @param template
	 *            the template
	 * @param context
	 *            the context
	 * @param writer
	 *            the writer
	 * 
	 * @throws ResourceNotFoundException
	 *             the resource not found exception
	 * @throws ParseErrorException
	 *             the parse error exception
	 * @throws MethodInvocationException
	 *             the method invocation exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public static void mergeTemplate(final Template template, final VelocityContext context, final BufferedWriter writer) throws ResourceNotFoundException, ParseErrorException, MethodInvocationException, IOException {
        template.merge(context, writer);
        writer.flush();
        writer.close();
    }

    public static void mergeTemplate(final Template template, final VelocityContext context, final File file) throws ResourceNotFoundException, ParseErrorException, MethodInvocationException, IOException {
        final FileWriter fileWriter = new FileWriter(file);
        final BufferedWriter writer = new BufferedWriter(fileWriter);
        mergeTemplate(template, context, writer);
    }

    /**
	 * The listener interface for receiving log4j events. The class that is
	 * interested in processing a log4j event implements this interface, and the
	 * object created with that class is registered with a component using the
	 * component's <code>addLog4jListener<code> method. When
	 * the log4j event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see Log4jEvent
	 */
    static class Log4jListener implements BuildListener {

        /** Indicates if the listener was initialized. */
        private boolean initialized = false;

        /** log category we log into. */
        public static final String LOG_ANT = "org.apache.tools.ant";

        /**
		 * Construct the listener and make sure there is a valid appender.
		 */
        public Log4jListener() {
            initialized = false;
            final Logger log = Logger.getLogger(LOG_ANT);
            final Logger rootLog = Logger.getRootLogger();
            if (!(rootLog.getAllAppenders() instanceof NullEnumeration)) {
                initialized = true;
            } else {
                log.error("No log4j.properties in build area");
            }
        }

        /** {@inheritDoc}. */
        public void buildStarted(final BuildEvent event) {
            if (initialized) {
                final Logger log = Logger.getLogger(Project.class.getName());
                log.info("Build started.");
            }
        }

        /** {@inheritDoc}. */
        public void buildFinished(final BuildEvent event) {
            if (initialized) {
                final Logger log = Logger.getLogger(Project.class.getName());
                if (event.getException() == null) {
                    log.info("Build finished.");
                } else {
                    log.error("Build finished with error.", event.getException());
                }
            }
        }

        /** {@inheritDoc}. */
        public void targetStarted(BuildEvent event) {
            if (initialized) {
                final Logger log = Logger.getLogger(Target.class.getName());
                log.info("Target \"" + event.getTarget().getName() + "\" started.");
            }
        }

        /** {@inheritDoc}. */
        public void targetFinished(BuildEvent event) {
            if (initialized) {
                final String targetName = event.getTarget().getName();
                final Logger cat = Logger.getLogger(Target.class.getName());
                if (event.getException() == null) {
                    cat.info("Target \"" + targetName + "\" finished.");
                } else {
                    cat.error("Target \"" + targetName + "\" finished with error.", event.getException());
                }
            }
        }

        /** {@inheritDoc}. */
        public void taskStarted(BuildEvent event) {
            if (initialized) {
                final Task task = event.getTask();
                final Logger log = Logger.getLogger(task.getClass().getName());
                log.info("Task \"" + task.getTaskName() + "\" started.");
            }
        }

        /** {@inheritDoc}. */
        public void taskFinished(BuildEvent event) {
            if (initialized) {
                final Task task = event.getTask();
                final Logger log = Logger.getLogger(task.getClass().getName());
                if (event.getException() == null) {
                    log.info("Task \"" + task.getTaskName() + "\" finished.");
                } else {
                    log.error("Task \"" + task.getTaskName() + "\" finished with error.", event.getException());
                }
            }
        }

        /** {@inheritDoc}. */
        public void messageLogged(BuildEvent event) {
            if (initialized) {
                Object categoryObject = event.getTask();
                if (categoryObject == null) {
                    categoryObject = event.getTarget();
                    if (categoryObject == null) {
                        categoryObject = event.getProject();
                    }
                }
                final Logger log = Logger.getLogger(categoryObject.getClass().getName());
                switch(event.getPriority()) {
                    case Project.MSG_ERR:
                        log.error(event.getMessage());
                        break;
                    case Project.MSG_WARN:
                        log.warn(event.getMessage());
                        break;
                    case Project.MSG_INFO:
                        log.info(event.getMessage());
                        break;
                    case Project.MSG_VERBOSE:
                        log.debug(event.getMessage());
                        break;
                    case Project.MSG_DEBUG:
                        log.debug(event.getMessage());
                        break;
                    default:
                        log.error(event.getMessage());
                        break;
                }
            }
        }
    }

    /**
	 * File exists.
	 * 
	 * @param path
	 *            the path
	 * 
	 * @return true, if successful
	 */
    public static final boolean fileExists(final String path) {
        return fileExists(new File(path));
    }

    /**
	 * File exists.
	 * 
	 * @param file
	 *            the file
	 * 
	 * @return true, if successful
	 */
    public static final boolean fileExists(final File file) {
        return file.exists();
    }
}
