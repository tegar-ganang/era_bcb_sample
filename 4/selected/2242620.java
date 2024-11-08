package org.antdepo.cli;

import launcher.Setup;
import org.antdepo.Constants;
import org.antdepo.common.*;
import org.apache.commons.cli.*;
import org.apache.log4j.Category;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Framework extension installer tool.
 * <p/>
 * ControlTier Software Inc.
 * User: alexh
 * Date: Aug 10, 2005
 * Time: 9:28:48 AM
 */
public class ExtensionInstallTool implements CLITool {

    public static final Category logger = Category.getInstance(ExtensionInstallTool.class);

    public String ADHOME_BIN = Constants.getSystemAntdepoHome() + File.separator + "bin";

    final File ADHOME_MOD_LIBDIR = new File(Constants.getModulesDir(Constants.getSystemAntdepoHome()));

    static final String DATE_FORMAT = "yyyyMMdd-hhmm";

    /**
     * control property to overwrite existing installations
     */
    boolean overwrite;

    protected CommandLine cli;

    /**
     * reference to string containing args to the Cli instance (e,g args after the "--" )
     */
    protected final Map properties;

    /**
     * reference to the command line {@link org.apache.commons.cli.Options} instance.
     */
    protected static final Options options = new Options();

    /**
     * Add the commandline options
     */
    static {
        options.addOption("h", "help", false, "print this message");
        options.addOption("o", "overwrite", false, "overwrite framework with files from extension");
        options.addOption("f", "file", true, "file to install");
        options.addOption("S", "nosetup", false, "don't re-run setup");
        options.addOption(OptionBuilder.withArgName("property=value").hasArgs().withValueSeparator('=').withDescription("property=value pair used during extension setup").create("D"));
    }

    /**
     * Reference to the Framework instance
     */
    private final Framework framework;

    /**
     * Reference to the {@link ExtensionMgr} object
     */
    private final ExtensionMgr extensionMgr;

    /**
     * Base constructor
     */
    public ExtensionInstallTool() {
        properties = new HashMap();
        framework = Framework.getInstance(Constants.getSystemAntdepoBase());
        extensionMgr = framework.getExtensionMgr();
    }

    /**
     * Runs the installation process. Loads and extracts the archive via {@link ExtensionArchive}.
     * Extracts archive and runs {@link Extension#setup(boolean)}.
     *
     * @param args
     */
    public final void run(final String[] args) {
        int exitCode = 1;
        parseArgs(args);
        try {
            go();
            exitCode = 0;
        } catch (Throwable e) {
            if (e.getMessage() == null) {
                e.printStackTrace();
            } else {
                System.err.println("error: " + e.getMessage());
            }
        }
        exit(exitCode);
    }

    protected void go() throws ExecutionException {
        final ExtensionArchive archive = loadArchive(new File(cli.getOptionValue("f")));
        final IExtensionManifest mf = archive.getManifest();
        archive.extract(extensionMgr.getBaseDir());
        final Extension extension = extensionMgr.getExtension(mf.getExtensionName());
        if (!extension.validate()) {
            throw new ExecutionException("invalid extension: " + extension.getName());
        }
        if (mf.includesModules()) {
            installModules(extension);
        }
        if (mf.includesBins()) {
            installBins(extension);
        }
        if (cli.hasOption('D')) {
            mergeDefaults(extension);
        }
        extension.setup(overwrite);
        if (!cli.hasOption('S')) {
            System.out.println("running antdepo setup");
            runAntdepoSetup();
        }
    }

    /**
     * Creates an instance and executes {@link #run(String[])}.
     *
     * @param args
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        final ExtensionInstallTool c = new ExtensionInstallTool();
        c.run(args);
    }

    /**
     * processes the command line input
     *
     * @param args command line arg vector
     */
    public CommandLine parseArgs(final String[] args) {
        final int argErrorCode = 2;
        final CommandLineParser parser = new PosixParser();
        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            help();
            exit(argErrorCode);
        }
        if (cli.hasOption("h")) {
            help();
            exit(argErrorCode);
        }
        if (cli.hasOption('o')) {
            overwrite = true;
            log("Overwrite flag has been set.");
        }
        if (cli.hasOption('D')) {
            if (cli.getOptionValues('D').length % 2 != 0) {
                error("bad property=value pairs");
                help();
                exit(argErrorCode);
            }
            for (int i = 0; i < cli.getOptionValues('D').length; i++) {
                final String propname = cli.getOptionValues('D')[i];
                final String value = cli.getOptionValues('D')[++i];
                log("property: " + propname + "=" + value);
                properties.put(propname, value);
            }
        }
        if (!cli.hasOption('f')) {
            help();
            exit(argErrorCode);
        }
        return cli;
    }

    /**
     * Calls the exit method
     *
     * @param code return code to exit with
     */
    public void exit(final int code) {
        System.exit(code);
    }

    public void log(final String output) {
        System.out.println(output);
    }

    public void error(final String output) {
        System.err.println(output);
    }

    /**
     * Logs verbose message via implementation specific log facility
     *
     * @param message message to log
     */
    public void verbose(final String message) {
        System.err.println(message);
    }

    /**
     * prints usage info
     */
    public void help() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(80, "ext-setup [options]", "options:", options, "Examples:\n" + "\text-setup -f extension-name.jar\n");
    }

    private void mergeDefaults(final Extension extension) throws ExecutionException {
        try {
            extension.mergeDefaults(properties);
        } catch (IOException e) {
            throw new ExecutionException("failed merging defaults: " + e.getMessage());
        }
    }

    private ExtensionArchive loadArchive(final File archiveFile) throws ExecutionException {
        final ExtensionArchive archive;
        try {
            archive = new ExtensionArchive(archiveFile);
        } catch (IOException e) {
            throw new ExecutionException("failed loading extension archive: " + e.getMessage());
        }
        if (!archive.validate()) {
            throw new ExecutionException("invalid archive file: " + archive.getArchiveFile().getAbsolutePath());
        }
        return archive;
    }

    private void installModules(final Extension extension) throws ExecutionException {
        try {
            dirCopy(extension.getModulesDir(), ADHOME_MOD_LIBDIR, overwrite);
            final File[] jars = extension.getJarFiles(extension.getModulesDir());
            for (int i = 0; i < jars.length; i++) {
                final File jar = new File(ADHOME_MOD_LIBDIR, jars[i].getName());
                final CmdModuleArchive modArchive = new CmdModuleArchive(jar);
                if (!modArchive.validate()) {
                    error("Skipping included extension module archive with invalid manifest: " + jar.getAbsolutePath());
                    continue;
                }
                final CmdModuleArchiveManifest modMf = modArchive.getManifest();
                final File existing = new File(ADHOME_MOD_LIBDIR, modMf.getModuleName());
                if (!existing.exists() || overwrite) {
                    modArchive.extract(ADHOME_MOD_LIBDIR);
                }
                jar.delete();
                log("cleaning up copied jar file: " + jar.getAbsoluteFile());
            }
        } catch (IOException e) {
            throw new ExecutionException("failed installing modules: " + e.getMessage());
        }
    }

    private void installBins(final Extension extension) throws ExecutionException {
        try {
            dirCopy(extension.getBinsDir(), new File(ADHOME_BIN), overwrite);
        } catch (IOException e) {
            throw new ExecutionException("failed installing bins: " + e.getMessage());
        }
    }

    private void fileCopy(final File src, final File dest) throws IOException {
        final FileChannel srcChannel = new FileInputStream(src).getChannel();
        final FileChannel dstChannel = new FileOutputStream(dest).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    private void dirCopy(final File srcDir, final File dstDir, final boolean overwrite) throws IOException {
        if (!srcDir.isDirectory()) {
            throw new IllegalArgumentException("not a directory: " + srcDir);
        }
        if (!dstDir.isDirectory()) {
            throw new IllegalArgumentException("not a directory: " + dstDir);
        }
        final File[] files = srcDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            final File destFile = new File(dstDir, files[i].getName());
            if (!destFile.exists() || (destFile.exists() && overwrite)) {
                if (destFile.exists()) {
                    log("Ovewriting file: " + destFile.getAbsolutePath());
                    final File backup = createBackupFile(destFile);
                    fileCopy(destFile, backup);
                }
                fileCopy(files[i], destFile);
            }
        }
    }

    /**
     * Utility method to create a copy of specified file in the same parent dir with a date stamp suffix.
     *
     * @param file Name of file to backup
     * @return The copied file with suffix
     */
    protected File createBackupFile(final File file) {
        final File backup = new File(file.getParentFile(), file.getName() + "." + new SimpleDateFormat(DATE_FORMAT).format(new Date()));
        return backup;
    }

    protected static class ExecutionException extends Exception {

        public ExecutionException() {
            super();
        }

        public ExecutionException(final String reason) {
            super(reason);
        }

        public ExecutionException(final String reason, final Throwable t) {
            super(reason, t);
        }
    }

    /**
     * Wrapper around the {@link Setup} class to run the Setup procedure
     *
     * @throws ExecutionException thrown if Setup raises an exception
     */
    protected void runAntdepoSetup() throws ExecutionException {
        final String fwkNode = framework.getFrameworkNodeName();
        final Setup setup = new Setup();
        final String[] args = new String[] { "-n", fwkNode };
        try {
            setup.execute(args);
        } catch (Setup.SetupException e) {
            throw new ExecutionException("setup execution failed", e);
        }
    }
}
