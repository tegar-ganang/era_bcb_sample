package net.assimilator.tools.deploymentdirectory.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Description:
 *
 * @author Larry Mitchell
 * @version $Id: CopyOarFileCommand.java 344 2007-10-09 23:11:43Z khartig $
 */
public class CopyOarFileCommand implements Command {

    /**
     * the logger for this class
     */
    private static final Logger logger = Logger.getLogger("net.assimilator.tools.deploymentdirectory.commands");

    /**
     * the target deployment directory
     */
    private String deploymentDirectory;

    /**
     * the path to the oar file that we are going to copy
     */
    private String oarfilePath;

    /**
     * ctor for copy command
     *
     * @param directory the target deployment directory
     * @param path      the path to the oar file that we are going to copy
     * @throws CommandException if the command was improperly formatted
     */
    public CopyOarFileCommand(String directory, String path) throws CommandException {
        if (directory == null) {
            logger.severe("CopyOarFileCommand: base directory is null");
            throw new CommandException("CopyOarFileCommand: base directory is null");
        }
        this.deploymentDirectory = directory;
        if (path == null) {
            logger.severe("CopyOarFileCommand: oar path is null");
            throw new CommandException("CopyOarFileCommand: oar path is null");
        }
        this.oarfilePath = path;
    }

    /**
     * execute the command
     */
    public void execute() {
        File sourceFile = new File(oarfilePath);
        File destinationFile = new File(deploymentDirectory + File.separator + sourceFile.getName());
        try {
            FileInputStream fis = new FileInputStream(sourceFile);
            FileOutputStream fos = new FileOutputStream(destinationFile);
            byte[] readArray = new byte[2048];
            while (fis.read(readArray) != -1) {
                fos.write(readArray);
            }
            fis.close();
            fos.flush();
            fos.close();
        } catch (IOException ioe) {
            logger.severe("failed to copy the file:" + ioe);
        }
    }
}
