package org.tolven.mqkeystore;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.tolven.plugin.TolvenCommandPlugin;

/**
 * This plugin can deploy the MQKeyStore RAR and its API.
 * 
 * @author Joseph Isaac
 *
 */
public class MQKeyStorePlugin extends TolvenCommandPlugin {

    public static final String CMD_LINE_DEPLOY_API_OPTION = "deployAPI";

    public static final String CMD_LINE_RAR_FILE_OPTION = "rarFile";

    public static final String CMD_LINE_DESTDIR_OPTION = "destDir";

    private Logger logger = Logger.getLogger(MQKeyStorePlugin.class);

    @Override
    protected void doStart() throws Exception {
        logger.debug("*** start ***");
    }

    @Override
    public void execute(String[] args) throws Exception {
        logger.debug("*** execute ***");
        CommandLine commandLine = getCommandLine(args);
        String destDirname = commandLine.getOptionValue(CMD_LINE_DESTDIR_OPTION);
        if (destDirname == null) {
            destDirname = new File(getStageDir(), getDescriptor().getId()).getPath();
        }
        File destDir = new File(destDirname);
        destDir.mkdirs();
        if (commandLine.hasOption(CMD_LINE_DEPLOY_API_OPTION)) {
            File jar = getFilePath("mqKeyStore-api.jar");
            logger.debug("Copy " + jar.getPath() + " to " + destDir.getPath());
            FileUtils.copyFileToDirectory(jar, destDir);
        }
        String rarFilename = commandLine.getOptionValue(CMD_LINE_RAR_FILE_OPTION);
        if (rarFilename != null) {
            File sourceRAR = getFilePath("mqKeyStore.rar");
            File destinationRAR = new File(destDir, rarFilename);
            logger.debug("Copy " + sourceRAR.getPath() + " to " + destDir.getPath());
            FileUtils.copyFile(sourceRAR, destinationRAR);
        }
    }

    private CommandLine getCommandLine(String[] args) {
        GnuParser parser = new GnuParser();
        try {
            return parser.parse(getCommandOptions(), args, true);
        } catch (ParseException ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(getClass().getName(), getCommandOptions());
            throw new RuntimeException("Could not parse command line for: " + getClass().getName(), ex);
        }
    }

    private Options getCommandOptions() {
        Options cmdLineOptions = new Options();
        Option deployAPIOption = new Option(CMD_LINE_DEPLOY_API_OPTION, CMD_LINE_DEPLOY_API_OPTION, false, "deploy API");
        cmdLineOptions.addOption(deployAPIOption);
        Option deployRAROption = new Option(CMD_LINE_RAR_FILE_OPTION, CMD_LINE_RAR_FILE_OPTION, true, "deploy RAR");
        cmdLineOptions.addOption(deployRAROption);
        Option destDirPluginOption = new Option(CMD_LINE_DESTDIR_OPTION, CMD_LINE_DESTDIR_OPTION, true, "destination directory");
        cmdLineOptions.addOption(destDirPluginOption);
        return cmdLineOptions;
    }

    @Override
    protected void doStop() throws Exception {
        logger.debug("*** stop ***");
    }
}
