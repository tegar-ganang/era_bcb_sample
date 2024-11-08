package eu.planets_project.services.migration.floppyImageHelper.impl.utils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistry;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.datatypes.Checksum;
import eu.planets_project.services.datatypes.Content;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.DigitalObjectContent;
import eu.planets_project.services.datatypes.MigrationPath;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.datatypes.Tool;
import eu.planets_project.services.datatypes.ServiceReport.Status;
import eu.planets_project.services.datatypes.ServiceReport.Type;
import eu.planets_project.services.migrate.Migrate;
import eu.planets_project.services.migrate.MigrateResult;
import eu.planets_project.services.migration.floppyImageHelper.api.FloppyImageHelper;
import eu.planets_project.services.migration.floppyImageHelper.impl.FloppyImageHelperService;
import eu.planets_project.services.utils.DigitalObjectUtils;
import eu.planets_project.services.utils.FileUtils;
import eu.planets_project.services.utils.ProcessRunner;
import eu.planets_project.services.utils.ServiceUtils;
import eu.planets_project.services.utils.ZipResult;
import eu.planets_project.services.utils.ZipUtils;

public class FloppyImageHelperUnix implements Migrate, FloppyImageHelper {

    public FloppyImageHelperUnix() {
        sessionID = FileUtils.randomizeFileName("");
        DEFAULT_FLOPPY_IMAGE_NAME = "floppy144" + sessionID + ".ima";
        if (TEMP_FOLDER.exists()) {
            FileUtils.deleteAllFilesInFolder(TEMP_FOLDER);
        }
    }

    private static String TEMP_FOLDER_NAME = "FLOPPY_IMAGE_HELPER_UNIX";

    private static File TEMP_FOLDER = FileUtils.createWorkFolderInSysTemp(TEMP_FOLDER_NAME);

    private static String sessionID = null;

    private String DEFAULT_INPUT_NAME = "inputFile" + sessionID;

    private String inputExt = null;

    private static String DEFAULT_FLOPPY_IMAGE_NAME = null;

    private static final long FLOPPY_SIZE = 1474560;

    private static String PROCESS_ERROR = null;

    private static String PROCESS_OUT = null;

    private static Logger log = Logger.getLogger(FloppyImageHelperUnix.class);

    private static int LOOP_DEV_MAX = 5;

    private static FormatRegistry formatReg = FormatRegistryFactory.getFormatRegistry();

    /**
	* @see eu.planets_project.services.migrate.Migrate#describe()
	*/
    public ServiceDescription describe() {
        ServiceDescription.Builder sd = new ServiceDescription.Builder(FloppyImageHelperService.NAME, Migrate.class.getCanonicalName());
        sd.author("Klaus Rechert, mailto:klaus.rechert@rz.uni-freiburg.de");
        sd.description("This service is a wrapper for creating floppy images with UNIX dd and fs-tools\n" + "This tool is able to create Floppy disk images (1.44 MB) from scratch," + "containing files of your choice (up to 1.44 MB!).\n" + "This is the first possible direction. The other one is the Extraction of files from a " + "floppy disk image without mounting that image as a disk drive." + "This service accepts:\n\n" + "1) ZIP files, containing the files you want to be written on the floppy image. " + "The service will unpack the ZIP file and write the contained files to the floppy image, " + "which is returned, if the files in the ZIP do not exceed the capacity limit of 1.44 MB.\n" + "2) a single file which should be written on the floppy image. This file could be of ANY " + "type/format (except the '.ima/.img' type!)\n" + "3) An '.IMA'/'.IMG' file. In this case, the service will extract all files from that floppy " + "image and return a set of files (as a ZIP)");
        sd.classname(this.getClass().getCanonicalName());
        sd.version("1.0");
        sd.tool(new Tool(null, "UNIX 'dd' and 'fs-tools'", "unknown", null, null));
        List<MigrationPath> pathways = new ArrayList<MigrationPath>();
        pathways.add(new MigrationPath(formatReg.createExtensionUri("ZIP"), formatReg.createExtensionUri("IMA"), null));
        pathways.add(new MigrationPath(formatReg.createExtensionUri("ANY"), formatReg.createExtensionUri("IMA"), null));
        pathways.add(new MigrationPath(formatReg.createExtensionUri("IMA"), formatReg.createExtensionUri("ZIP"), null));
        pathways.add(new MigrationPath(formatReg.createExtensionUri("IMG"), formatReg.createExtensionUri("ZIP"), null));
        sd.paths(pathways.toArray(new MigrationPath[] {}));
        return sd.build();
    }

    public MigrateResult migrate(DigitalObject digitalObject, URI inputFormat, URI outputFormat, List<Parameter> parameters) {
        String inFormat = formatReg.getFirstExtension(inputFormat).toUpperCase();
        DigitalObjectContent content = digitalObject.getContent();
        Checksum checksum = content.getChecksum();
        String fileName = digitalObject.getTitle();
        if (fileName == null) {
            inputExt = formatReg.getFirstExtension(inputFormat);
            fileName = FileUtils.randomizeFileName(DEFAULT_INPUT_NAME + "." + inputExt);
        } else {
            fileName = FileUtils.randomizeFileName(fileName);
        }
        File inputFile = FileUtils.writeInputStreamToFile(digitalObject.getContent().read(), TEMP_FOLDER, fileName);
        if ((inFormat.endsWith("IMA")) || inFormat.endsWith("IMG")) {
            ZipResult zippedResult = FloppyImageHelperUnix.extractFilesFromFloppyImage(inputFile);
            DigitalObject resultDigObj = DigitalObjectUtils.createZipTypeDigitalObject(zippedResult.getZipFile(), zippedResult.getZipFile().getName(), true, true, false);
            ServiceReport report = new ServiceReport(Type.INFO, Status.SUCCESS, PROCESS_OUT);
            return new MigrateResult(resultDigObj, report);
        }
        List<File> files = null;
        if (inFormat.endsWith("ZIP")) {
            if (checksum != null) {
                files = ZipUtils.checkAndUnzipTo(inputFile, TEMP_FOLDER, checksum);
            } else files = ZipUtils.unzipTo(inputFile, TEMP_FOLDER);
        } else {
            files = new ArrayList<File>();
            files.add(inputFile);
        }
        File floppy = FloppyImageHelperUnix.createFloppyImageWithFiles(files);
        if (floppy == null) return this.returnWithErrorMessage(PROCESS_ERROR, null);
        DigitalObject resultDigObj = new DigitalObject.Builder(Content.byReference(floppy)).format(outputFormat).title(floppy.getName()).build();
        ServiceReport report = new ServiceReport(Type.INFO, Status.SUCCESS, PROCESS_OUT);
        return new MigrateResult(resultDigObj, report);
    }

    private static ZipResult extractFilesFromFloppyImage(File image) {
        int loopdev;
        String mountlabel;
        if (!image.exists()) {
            log.error("Image: " + image.getAbsolutePath() + "not found");
            return null;
        }
        loopdev = bindLoop(image.getAbsolutePath());
        if (loopdev < 0) return null;
        mountlabel = mount(loopdev);
        if (mountlabel == null) {
            unbindLoop(loopdev);
            return null;
        }
        log.info("image: " + image + " mounted to " + mountlabel);
        File srcdir = new File(mountlabel);
        ZipResult result = ZipUtils.createZipAndCheck(srcdir, TEMP_FOLDER, FileUtils.randomizeFileName("extractedFiles.zip"), false);
        umount(mountlabel);
        unbindLoop(loopdev);
        return result;
    }

    private static File createFloppyImageWithFiles(List<File> files) {
        int loopdev;
        String mountlabel;
        if (FileUtils.filesTooLargeForMedium(files, FLOPPY_SIZE)) {
            log.error("Sorry! File set too large to be written to a Floppy (1.44 MB).");
            return null;
        }
        File image = new File(FileUtils.getSystemTempFolder(), DEFAULT_FLOPPY_IMAGE_NAME);
        createFloppy(image);
        if (!image.exists()) {
            log.error("Creating image failed");
            return null;
        }
        loopdev = bindLoop(image.getAbsolutePath());
        if (loopdev < 0) return null;
        mountlabel = mount(loopdev);
        if (mountlabel == null) {
            log.error("mount failed");
            unbindLoop(loopdev);
            return null;
        }
        log.info("image: " + image + " mounted to " + mountlabel);
        boolean success = copyFiles(files, mountlabel);
        umount(mountlabel);
        unbindLoop(loopdev);
        if (success) {
            log.info("successfuly created floppy image");
            return image;
        } else return null;
    }

    /**
	 * @param message an optional message on what happened to the service
	 * @param e the Exception e which causes the problem
	 * @return CharacteriseResult containing a Error-Report
	 */
    private MigrateResult returnWithErrorMessage(final String message, final Exception e) {
        if (e == null) {
            return new MigrateResult(null, ServiceUtils.createErrorReport(message));
        } else {
            return new MigrateResult(null, ServiceUtils.createExceptionErrorReport(message, e));
        }
    }

    private static boolean copyFiles(List<File> files, String dest) {
        ArrayList<String> commands = new ArrayList<String>();
        ProcessRunner cmd = new ProcessRunner();
        cmd.setCommand(commands);
        for (File file : files) {
            commands.clear();
            commands.add("/bin/cp");
            commands.add(file.getAbsolutePath());
            commands.add(dest);
            cmd.run();
            if (cmd.getReturnCode() != 0) {
                log.info("cp " + cmd.getProcessOutputAsString());
                log.info("err: " + cmd.getProcessErrorAsString() + "(" + cmd.getReturnCode() + ")");
                return false;
            }
        }
        return true;
    }

    private static File createFloppy(File imageFile) {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("/bin/dd");
        commands.add("if=/dev/zero");
        commands.add("of=" + imageFile.getAbsolutePath());
        commands.add("bs=512");
        commands.add("count=2880");
        ProcessRunner cmd = new ProcessRunner();
        cmd.setCommand(commands);
        cmd.run();
        if (cmd.getReturnCode() != 0) {
            log.error("dd: " + cmd.getProcessOutputAsString());
            log.error("err: " + cmd.getProcessErrorAsString() + "(" + cmd.getReturnCode() + ")");
            return null;
        }
        commands.clear();
        commands.add("/sbin/mkfs.vfat");
        commands.add(imageFile.getAbsolutePath());
        cmd.run();
        if (cmd.getReturnCode() != 0) {
            log.info("mkfs " + cmd.getProcessOutputAsString());
            log.info("err: " + cmd.getProcessErrorAsString() + "(" + cmd.getReturnCode() + ")");
            return null;
        }
        return null;
    }

    public static boolean isLoopDevAvailable(int i) {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("/usr/bin/touch");
        commands.add("/dev/loop" + i);
        ProcessRunner cmd = new ProcessRunner();
        cmd.setCommand(commands);
        cmd.run();
        if (cmd.getReturnCode() != 0) {
            log.error("/dev/loop" + i + " not writeable");
            log.error("err: " + cmd.getProcessErrorAsString() + "(" + cmd.getReturnCode() + ")");
            return false;
        }
        commands.clear();
        commands.add("/sbin/losetup");
        commands.add("/dev/loop" + i);
        cmd.run();
        if (cmd.getReturnCode() == 0) {
            log.info("losetup " + cmd.getProcessOutputAsString());
            log.info("err: " + cmd.getProcessErrorAsString() + "(" + cmd.getReturnCode() + ")");
            return false;
        }
        return true;
    }

    private static int bindLoop(String imageFile) {
        ProcessRunner cmd = new ProcessRunner();
        ArrayList<String> commands = new ArrayList<String>();
        int loopdev;
        for (loopdev = 0; loopdev <= LOOP_DEV_MAX; loopdev++) {
            if (isLoopDevAvailable(loopdev)) break;
        }
        if (loopdev > LOOP_DEV_MAX) {
            log.error("no suitable loop dev found");
            return -1;
        }
        commands.add("/sbin/losetup");
        commands.add("/dev/loop" + loopdev);
        commands.add(imageFile);
        cmd.setCommand(commands);
        cmd.run();
        if (cmd.getReturnCode() != 0) {
            log.error("losetup " + cmd.getProcessOutputAsString());
            log.error("err: " + cmd.getProcessErrorAsString() + "(" + cmd.getReturnCode() + ")");
            return -1;
        }
        return loopdev;
    }

    private static boolean unbindLoop(int loopdev) {
        ProcessRunner cmd = new ProcessRunner();
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("/sbin/losetup");
        commands.add("-d");
        commands.add("/dev/loop" + loopdev);
        cmd.setCommand(commands);
        cmd.run();
        if (cmd.getReturnCode() != 0) {
            log.error("losetup unbind" + cmd.getProcessOutputAsString());
            log.error("err: " + cmd.getProcessErrorAsString() + "(" + cmd.getReturnCode() + ")");
            return false;
        }
        return true;
    }

    private static String mount(int loopdev) {
        ProcessRunner cmd = new ProcessRunner();
        ArrayList<String> commands = new ArrayList<String>();
        String mountlabel = "/media/loop" + loopdev;
        commands.add("/usr/bin/pmount");
        commands.add("/dev/loop" + loopdev);
        commands.add(mountlabel);
        cmd.setCommand(commands);
        cmd.run();
        if (cmd.getReturnCode() != 0) {
            log.error("pmount " + cmd.getProcessOutputAsString());
            log.error("err: " + cmd.getProcessErrorAsString() + "(" + cmd.getReturnCode() + ")");
            return null;
        }
        return mountlabel;
    }

    private static boolean umount(String mountlabel) {
        ProcessRunner cmd = new ProcessRunner();
        ArrayList<String> commands = new ArrayList<String>();
        commands.add("/usr/bin/pumount");
        commands.add(mountlabel);
        cmd.setCommand(commands);
        cmd.run();
        if (cmd.getReturnCode() != 0) {
            log.error("pumount " + cmd.getProcessOutputAsString());
            log.error("err: " + cmd.getProcessErrorAsString() + "(" + cmd.getReturnCode() + ")");
            return false;
        }
        return true;
    }
}
