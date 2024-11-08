package eu.planets_project.services.migration.floppyImageHelper.impl.utils;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
import eu.planets_project.services.utils.PlanetsLogger;
import eu.planets_project.services.utils.ServiceUtils;
import eu.planets_project.services.utils.ZipResult;
import eu.planets_project.services.utils.ZipUtils;

public class FloppyImageHelperWin implements Migrate, FloppyImageHelper {

    private File TEMP_FOLDER = null;

    private String TEMP_FOLDER_NAME = "FLOPPY_IMAGE_HELPER_WIN";

    private File EXTRACTED_FILES = null;

    private String sessionID = FileUtils.randomizeFileName("");

    private String EXTRACTED_FILES_DIR = "EXTRACTED_FILES" + sessionID;

    private String DEFAULT_INPUT_NAME = "inputFile" + sessionID;

    private String PROCESS_ERROR = "";

    private String PROCESS_OUT = "";

    private String br = System.getProperty("line.separator");

    private static FormatRegistry format = FormatRegistryFactory.getFormatRegistry();

    private PlanetsLogger log = PlanetsLogger.getLogger(this.getClass());

    private VirtualFloppyDrive vfd = new VirtualFloppyDrive();

    public FloppyImageHelperWin() {
        TEMP_FOLDER = FileUtils.createWorkFolderInSysTemp(TEMP_FOLDER_NAME);
        FileUtils.deleteAllFilesInFolder(TEMP_FOLDER);
        EXTRACTED_FILES = FileUtils.createFolderInWorkFolder(TEMP_FOLDER, EXTRACTED_FILES_DIR);
    }

    public ServiceDescription describe() {
        ServiceDescription.Builder sd = new ServiceDescription.Builder(FloppyImageHelperService.NAME, Migrate.class.getCanonicalName());
        sd.author("Peter Melms, mailto:peter.melms@uni-koeln.de");
        sd.description("This service is a wrapper for the 'Virtual Floppy Drive' Commandline tool for Windows." + br + "This tools is able to create Floppy disk images - 1.44 MB - from scratch, containing files of your choice." + br + "This is the first possible direction. The other one is the Extraction of files from a floppy disk image." + "This service accepts:" + br + "1) ZIP files, containing the files you want to be written on the floppy image. The service will unpack the ZIP file and write the contained files to the floppy image, " + "which is returned, if the files in the ZIP do not exceed the capacity limit of 1.44 MB." + br + "2) a single file which should be written on the floppy image. This file could be of ANY type/format (except the '.ima/.img' type!)" + br + "3) An '.IMA'/'.IMG' file. In this case, the service will extract all files from that floppy image and return a set of files (as a ZIP).");
        sd.classname(this.getClass().getCanonicalName());
        sd.version("1.0");
        sd.tool(Tool.create(null, "Virtual Floppy Drive (vfd.exe)", "v2.1.2008.0206", null, "http://chitchat.at.infoseek.co.jp/vmware/vfd.html"));
        List<MigrationPath> pathways = new ArrayList<MigrationPath>();
        pathways.add(new MigrationPath(format.createExtensionUri("ZIP"), format.createExtensionUri("IMA"), null));
        pathways.add(new MigrationPath(format.createExtensionUri("ANY"), format.createExtensionUri("IMA"), null));
        pathways.add(new MigrationPath(format.createExtensionUri("IMA"), format.createExtensionUri("ZIP"), null));
        pathways.add(new MigrationPath(format.createExtensionUri("IMG"), format.createExtensionUri("ZIP"), null));
        sd.paths(pathways.toArray(new MigrationPath[] {}));
        return sd.build();
    }

    public MigrateResult migrate(DigitalObject digitalObject, URI inputFormat, URI outputFormat, List<Parameter> parameters) {
        FloppyHelperResult vfdResult = null;
        String inFormat = format.getFirstExtension(inputFormat).toUpperCase();
        List<File> extractedFiles = null;
        String fileName = digitalObject.getTitle();
        DigitalObjectContent content = digitalObject.getContent();
        Checksum checksum = content.getChecksum();
        if (fileName == null) {
            fileName = DEFAULT_INPUT_NAME + "." + inFormat;
        }
        File inputFile = FileUtils.writeInputStreamToFile(digitalObject.getContent().read(), TEMP_FOLDER, fileName);
        File imageFile = null;
        ZipResult zippedResult = null;
        if ((inFormat.endsWith("IMA")) || inFormat.endsWith("IMG")) {
            vfdResult = vfd.openImageAndGetFiles(inputFile);
            if (vfdResult.resultIsZip) {
                zippedResult = vfdResult.getZipResult();
            } else {
                return this.returnWithErrorMessage(vfdResult.getMessage(), null);
            }
            DigitalObject resultDigObj = DigitalObjectUtils.createZipTypeDigitalObject(zippedResult.getZipFile(), zippedResult.getZipFile().getName(), true, true, false);
            ServiceReport report = new ServiceReport(Type.INFO, Status.SUCCESS, PROCESS_OUT);
            log.info("Created Service report...");
            return new MigrateResult(resultDigObj, report);
        }
        if (inFormat.endsWith("ZIP")) {
            if (checksum != null) {
                extractedFiles = ZipUtils.checkAndUnzipTo(inputFile, EXTRACTED_FILES, checksum);
            } else {
                extractedFiles = ZipUtils.unzipTo(inputFile, EXTRACTED_FILES);
            }
            vfdResult = vfd.createImageAndInjectFiles(extractedFiles);
            if (!vfdResult.resultIsZip) {
                imageFile = vfdResult.getResultFile();
            } else {
                return this.returnWithErrorMessage(vfdResult.getMessage(), null);
            }
        } else {
            List<File> tmpList = new ArrayList<File>();
            tmpList.add(inputFile);
            vfdResult = vfd.createImageAndInjectFiles(tmpList);
            imageFile = vfdResult.getResultFile();
            if (imageFile == null) {
                return this.returnWithErrorMessage(PROCESS_ERROR, null);
            }
        }
        DigitalObject resultDigObj = new DigitalObject.Builder(Content.byReference(imageFile)).format(outputFormat).title(imageFile.getName()).build();
        ServiceReport report = new ServiceReport(Type.INFO, Status.SUCCESS, PROCESS_OUT);
        log.info("Created Service report...");
        return new MigrateResult(resultDigObj, report);
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
}
