package eu.planets_project.services.migration.floppyImageHelper.impl.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import eu.planets_project.services.utils.FileUtils;
import eu.planets_project.services.utils.ProcessRunner;
import eu.planets_project.services.utils.ZipResult;
import eu.planets_project.services.utils.ZipUtils;

/**
 * @author melmsp
 *
 */
public class VirtualFloppyDrive {

    private static File TOOL_DIR = null;

    private static String VFD_TOOL_NAME = "VFD.EXE";

    private static String FLOPPY_IMAGE_TOOLS_HOME_PATH = System.getenv("FLOPPY_IMAGE_TOOLS_HOME");

    private File TEMP_FOLDER = null;

    private String TEMP_FOLDER_NAME = "VIRTUAL_FLOPPY_DRIVE_TMP";

    private String DEFAULT_FLOPPY_IMAGE_NAME = FileUtils.randomizeFileName("floppy144.ima");

    private File EXTRACTED_FILES_DIR = null;

    private String EXTRACTION_OUT_FOLDER_NAME = FileUtils.randomizeFileName("EXTRACTED_FILES");

    private static final long FLOPPY_SIZE = 1474560;

    private StringBuffer process_error = new StringBuffer();

    private StringBuffer process_output = new StringBuffer();

    private static String br = System.getProperty("line.separator");

    private Log log = LogFactory.getLog(this.getClass());

    public VirtualFloppyDrive() {
        TEMP_FOLDER = FileUtils.createWorkFolderInSysTemp(TEMP_FOLDER_NAME);
        if (TEMP_FOLDER.exists()) {
            FileUtils.deleteAllFilesInFolder(TEMP_FOLDER);
        }
        EXTRACTED_FILES_DIR = FileUtils.createFolderInWorkFolder(TEMP_FOLDER, EXTRACTION_OUT_FOLDER_NAME);
    }

    public FloppyHelperResult createImageAndInjectFiles(List<File> filesToInject) {
        if (!toolInstalledProperly()) {
            return this.returnWithError(process_error.toString());
        }
        if (FileUtils.filesTooLargeForMedium(filesToInject, FLOPPY_SIZE)) {
            process_error.append("Sorry! File compilation too large to be written to a Floppy (1.44 MB).");
            log.error("Sorry! File compilation too large to be written to a Floppy (1.44 MB).");
            return this.returnWithError(process_error.toString());
        }
        ProcessRunner cmd = new ProcessRunner();
        cmd.setStartingDir(TEMP_FOLDER);
        cmd.setCommand(createNewImage());
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        cmd.setCommand(formatFloppyImage());
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        cmd.setCommand(uLinkDriveLetter());
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        String driveLetter = getDriveLetter();
        if (driveLetter == null) {
            process_error.append("Could not assign drive letter to virtual floppy drive, all letters are in use!");
            return this.returnWithError(process_error.toString());
        }
        cmd.setCommand(linkDriveLetter(driveLetter));
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        File floppy = new File(driveLetter);
        for (File currentFile : filesToInject) {
            File target = new File(floppy, currentFile.getName());
            FileUtils.copyFileTo(currentFile, target);
        }
        cmd.setCommand(saveImageTo(DEFAULT_FLOPPY_IMAGE_NAME));
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        cmd.setCommand(closeImage());
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        cmd.setCommand(uLinkDriveLetter());
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        File newfloppyImage = new File(TEMP_FOLDER, DEFAULT_FLOPPY_IMAGE_NAME);
        return this.returnWithSuccess(process_output.toString(), newfloppyImage, null);
    }

    public FloppyHelperResult openImageAndGetFiles(File imageFile) {
        if (!toolInstalledProperly()) {
            return this.returnWithError(process_error.toString());
        }
        ProcessRunner cmd = new ProcessRunner();
        cmd.setStartingDir(TEMP_FOLDER);
        cmd.setCommand(uLinkDriveLetter());
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        String driveLetter = getDriveLetter();
        if (driveLetter == null) {
            process_error.append("Could not assign drive letter to virtual floppy drive, all letters are in use!");
            return this.returnWithError(process_error.toString());
        }
        cmd.setCommand(linkDriveLetter(driveLetter));
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        cmd.setCommand(openImage(imageFile));
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        File floppy = new File(driveLetter);
        log.info("Floppy Path name: " + floppy.getAbsolutePath());
        File[] filesOnFloppy = floppy.listFiles();
        for (File file : filesOnFloppy) {
            File dest = new File(EXTRACTED_FILES_DIR, file.getName());
            log.info("Copy file: " + file.getAbsolutePath() + " to : " + dest.getAbsolutePath());
            FileUtils.copyFileTo(file, dest);
        }
        ZipResult zip = ZipUtils.createZipAndCheck(EXTRACTED_FILES_DIR, TEMP_FOLDER, FileUtils.randomizeFileName("extracedFiles.zip"), false);
        cmd.setCommand(closeImage());
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        cmd.setCommand(uLinkDriveLetter());
        cmd.run();
        process_error.append(cmd.getProcessErrorAsString());
        process_output.append(cmd.getProcessOutputAsString());
        return this.returnWithSuccess(process_output.toString(), null, zip);
    }

    public FloppyHelperResult addFilesToFloppyImage(File floppyImage) {
        if (!toolInstalledProperly()) {
            return this.returnWithError(process_error.toString());
        }
        ProcessRunner cmd = new ProcessRunner();
        cmd.setStartingDir(TEMP_FOLDER);
        cmd.setCommand(startVfdDriver());
        cmd.run();
        process_output.append(cmd.getProcessOutputAsString());
        process_error.append(cmd.getProcessErrorAsString());
        cmd.setCommand(uLinkDriveLetter());
        cmd.run();
        process_output.append(cmd.getProcessOutputAsString());
        process_error.append(cmd.getProcessErrorAsString());
        String driveLetter = getDriveLetter();
        if (driveLetter == null) {
            process_error.append("Could not assign drive letter to virtual floppy drive, all letters are in use!");
            return this.returnWithError(process_error.toString());
        }
        cmd.setCommand(linkDriveLetter(driveLetter));
        cmd.run();
        process_output.append(cmd.getProcessOutputAsString());
        process_error.append(cmd.getProcessErrorAsString());
        cmd.setCommand(openImage(floppyImage));
        cmd.run();
        process_output.append(cmd.getProcessOutputAsString());
        process_error.append(cmd.getProcessErrorAsString());
        File floppy = new File(driveLetter);
        List<File> allFiles = new ArrayList<File>();
        List<File> filesOnFloppy = Arrays.asList(floppy.listFiles());
        allFiles.addAll(filesOnFloppy);
        boolean filesTooLarge = FileUtils.filesTooLargeForMedium(allFiles, FLOPPY_SIZE);
        if (!filesTooLarge) {
            List<File> afterCopy = Arrays.asList(floppy.listFiles());
            log.info("Files on floppy: " + br);
            for (File file : afterCopy) {
                log.info(file.getAbsoluteFile() + br);
            }
        } else {
            process_error.append("ERROR: Not enough space! Files could not be added to floppy disk!");
            return this.returnWithError(process_error.toString());
        }
        cmd.setCommand(saveImageTo(floppyImage.getAbsolutePath()));
        cmd.run();
        process_output.append(cmd.getProcessOutputAsString());
        process_error.append(cmd.getProcessErrorAsString());
        cmd.setCommand(uLinkDriveLetter());
        cmd.run();
        process_output.append(cmd.getProcessOutputAsString());
        process_error.append(cmd.getProcessErrorAsString());
        cmd.setCommand(closeImage());
        cmd.run();
        process_output.append(cmd.getProcessOutputAsString());
        process_error.append(cmd.getProcessErrorAsString());
        return this.returnWithSuccess(process_output.toString(), floppyImage, null);
    }

    /**
	 * Get the first free drive letter...
	 * @return the first unused drive letter on your system to map it to the floppy drive
	 */
    private String getDriveLetter() {
        List<String> freeDriveLetters = FileUtils.listAvailableDriveLetters();
        String driveLetter = null;
        if (freeDriveLetters != null) {
            driveLetter = freeDriveLetters.get(0);
        } else {
            process_error.append("Virtual floppy drive could not be mapped to a drive letter; all drive letters are in use!!!" + br + "Returning NULL");
            return null;
        }
        return driveLetter;
    }

    private ArrayList<String> startVfdDriver() {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("START");
        return commands;
    }

    private ArrayList<String> stopVfdDriver() {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("STOP");
        commands.add("/FORCE");
        return commands;
    }

    private ArrayList<String> installVfdDriver() {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("INSTALL");
        commands.add("/AUTO");
        return commands;
    }

    private ArrayList<String> removeVfdDriver() {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("REMOVE");
        commands.add("/FORCE");
        return commands;
    }

    private ArrayList<String> openImage(File floppyImage) {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("OPEN");
        commands.add(floppyImage.getAbsolutePath());
        commands.add("/W");
        commands.add("/FORCE");
        return commands;
    }

    private ArrayList<String> createNewImage() {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("OPEN");
        commands.add("0:");
        commands.add("/NEW");
        commands.add("/W");
        commands.add("/1.44");
        commands.add("/FORCE");
        return commands;
    }

    private ArrayList<String> closeImage() {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("CLOSE");
        commands.add("0:");
        commands.add("/FORCE");
        return commands;
    }

    private ArrayList<String> uLinkDriveLetter() {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("ULINK");
        return commands;
    }

    private ArrayList<String> linkDriveLetter(String driveLetter) {
        if (driveLetter.endsWith("\\")) {
            driveLetter = driveLetter.replace("\\", "");
        }
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("LINK");
        commands.add("0:");
        commands.add(driveLetter);
        return commands;
    }

    private ArrayList<String> saveImageTo(String destFileName) {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("SAVE");
        commands.add("0:");
        commands.add(destFileName);
        commands.add("/FORCE");
        return commands;
    }

    private ArrayList<String> status() {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("STATUS");
        return commands;
    }

    private ArrayList<String> formatFloppyImage() {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(TOOL_DIR + "\\" + VFD_TOOL_NAME);
        commands.add("FORMAT");
        commands.add("0:");
        commands.add("/FORCE");
        return commands;
    }

    private boolean toolInstalledProperly() {
        if (FLOPPY_IMAGE_TOOLS_HOME_PATH == null) {
            log.error("FLOPPY_IMAGE_TOOLS_HOME = null! " + br + "Could not find floppy image tools! " + br + "Please install the tool 'Virtual Floppy Drive' on your system and point a System variable to the installation folder!" + br + "Otherwise this service will carry on to refuse to do its work! (you can find the tool here: http://chitchat.at.infoseek.co.jp/vmware/vfd.html)");
            process_error.append("FLOPPY_IMAGE_TOOLS_HOME = null! " + br + "Could not find floppy image tools! " + br + "Please install the tool 'Virtual Floppy Drive' on your system and point a System variable to the installation folder!" + br + "Otherwise this service will carry on to refuse to do its work! (you can find the tool here: http://chitchat.at.infoseek.co.jp/vmware/vfd.html)");
            return false;
        } else {
            TOOL_DIR = new File(FLOPPY_IMAGE_TOOLS_HOME_PATH);
            return true;
        }
    }

    private FloppyHelperResult returnWithError(String message) {
        FloppyHelperResult vfdResult = new FloppyHelperResult();
        vfdResult.setMessage(message);
        vfdResult.setResultFile(null);
        vfdResult.setZipResult(null);
        vfdResult.setState(FloppyHelperResult.ERROR);
        return vfdResult;
    }

    private FloppyHelperResult returnWithSuccess(String message, File resultFile, ZipResult zip) {
        FloppyHelperResult vfdResult = new FloppyHelperResult();
        vfdResult.setMessage(message);
        if (resultFile != null) {
            vfdResult.setResultFile(resultFile);
        } else {
            vfdResult.setZipResult(zip);
        }
        vfdResult.setState(FloppyHelperResult.SUCCESS);
        return vfdResult;
    }
}
