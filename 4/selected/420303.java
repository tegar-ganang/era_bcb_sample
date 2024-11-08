package eu.planets_project.services.modification.floppyImageModify.impl;

import java.io.File;
import java.net.URI;
import java.util.List;
import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistry;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.PlanetsServices;
import eu.planets_project.services.datatypes.Content;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.datatypes.Tool;
import eu.planets_project.services.datatypes.ServiceReport.Status;
import eu.planets_project.services.datatypes.ServiceReport.Type;
import eu.planets_project.services.migration.floppyImageHelper.impl.utils.FloppyHelperResult;
import eu.planets_project.services.migration.floppyImageHelper.impl.utils.VirtualFloppyDrive;
import eu.planets_project.services.migration.floppyImageHelper.impl.utils.VirtualFloppyDriveResult;
import eu.planets_project.services.modification.floppyImageModify.api.FloppyImageModify;
import eu.planets_project.services.modify.Modify;
import eu.planets_project.services.modify.ModifyResult;
import eu.planets_project.services.utils.FileUtils;
import eu.planets_project.services.utils.ServiceUtils;

/**
 * @author Peter Melms
 *
 */
@Stateless()
@BindingType(value = "http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true")
@WebService(name = FloppyImageModifyWin.NAME, serviceName = Modify.NAME, targetNamespace = PlanetsServices.NS, endpointInterface = "eu.planets_project.services.modify.Modify")
public class FloppyImageModifyWin implements Modify, FloppyImageModify {

    public static final String NAME = "FloppyImageModifyWin";

    private File TEMP_FOLDER = null;

    private String TEMP_FOLDER_NAME = "FLOPPY_IMAGE_MODIFY";

    private String sessionID = FileUtils.randomizeFileName("");

    private String DEFAULT_INPUT_NAME = "floppy144" + sessionID + ".ima";

    private String INPUT_EXT = null;

    private String br = System.getProperty("line.separator");

    private Log log = LogFactory.getLog(this.getClass());

    private static FormatRegistry formatRegistry = FormatRegistryFactory.getFormatRegistry();

    private static VirtualFloppyDrive vfd = new VirtualFloppyDrive();

    public FloppyImageModifyWin() {
        TEMP_FOLDER = FileUtils.createWorkFolderInSysTemp(TEMP_FOLDER_NAME);
        FileUtils.deleteAllFilesInFolder(TEMP_FOLDER);
    }

    public ServiceDescription describe() {
        ServiceDescription.Builder sd = new ServiceDescription.Builder(NAME, Modify.class.getCanonicalName());
        sd.author("Peter Melms, mailto:peter.melms@uni-koeln.de");
        sd.description("This service is a wrapper for the 'Virtual Floppy Drive' Commandline tool for Windows." + br + "This tools is able to create Floppy disk images - 1.44 MB - from scratch, containing files of your choice." + br + "This is the first possible direction. The other one is the Extraction of files from a floppy disk image." + "This service accepts:" + br + "1) ZIP files, containing the files you want to be written on the floppy image. The service will unpack the ZIP file and write the contained files to the floppy image, " + "which is returned, if the files in the ZIP do not exceed the capacity limit of 1.44 MB." + br + "2) a single file which should be written on the floppy image. This file could be of ANY type/format (except the '.ima/.img' type!)" + br + "3) An '.IMA'/'.IMG' file. In this case, the service will extract all files from that floppy image and return a set of files (as a ZIP).");
        sd.classname(this.getClass().getCanonicalName());
        sd.version("1.0");
        sd.tool(Tool.create(null, "Virtual Floppy Drive (vfd.exe)", "v2.1.2008.0206", null, "http://chitchat.at.infoseek.co.jp/vmware/vfd.html"));
        sd.inputFormats(formatRegistry.createExtensionUri("IMA"), formatRegistry.createExtensionUri("IMG"));
        return sd.build();
    }

    public ModifyResult modify(DigitalObject digitalObject, URI inputFormat, List<Parameter> parameters) {
        String inFormat = formatRegistry.getFirstExtension(inputFormat).toUpperCase();
        String fileName = digitalObject.getTitle();
        if (fileName == null) {
            INPUT_EXT = formatRegistry.getExtensions(inputFormat).iterator().next();
            fileName = DEFAULT_INPUT_NAME + "." + INPUT_EXT;
        }
        if (!inFormat.equalsIgnoreCase("IMA") && !inFormat.equalsIgnoreCase("IMG")) {
            log.error("ERROR: Input file ' " + fileName + "' is NOT an '.ima' or '.img' file or is no floppy image at all." + "\nThis service is able to deal with ima/img files only!!!" + "\nSorry, returning with error!");
            return this.returnWithErrorMessage("ERROR: Input file ' " + fileName + "' is NOT an '.ima' or '.img' file or is no floppy image at all." + "\nThis service is able to deal with ima/img files only!!!" + "\nSorry, returning with error!", null);
        }
        File originalImageFile = FileUtils.writeInputStreamToFile(digitalObject.getContent().read(), TEMP_FOLDER, fileName);
        FloppyHelperResult vfdResult = vfd.addFilesToFloppyImage(originalImageFile);
        File modifiedImage = vfdResult.getResultFile();
        DigitalObject result = null;
        if (modifiedImage != null) {
            result = new DigitalObject.Builder(Content.byReference(modifiedImage)).title(modifiedImage.getName()).format(formatRegistry.createExtensionUri(FileUtils.getExtensionFromFile(modifiedImage))).build();
        } else {
            return this.returnWithErrorMessage("Received NO result file from service. Something went terribly wrong somewhere!", null);
        }
        if (vfdResult.getState() == VirtualFloppyDriveResult.SUCCESS) {
            ServiceReport report = new ServiceReport(Type.INFO, Status.SUCCESS, vfdResult.getMessage());
            log.info("Created Service report...");
            return new ModifyResult(result, report);
        } else {
            return this.returnWithErrorMessage(vfdResult.getMessage(), null);
        }
    }

    /**
	 * @param message an optional message on what happened to the service
	 * @param e the Exception e which causes the problem
	 * @return CharacteriseResult containing a Error-Report
	 */
    private ModifyResult returnWithErrorMessage(final String message, final Exception e) {
        if (e == null) {
            return new ModifyResult(null, ServiceUtils.createErrorReport(message));
        } else {
            return new ModifyResult(null, ServiceUtils.createExceptionErrorReport(message, e));
        }
    }
}
