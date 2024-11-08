package eu.planets_project.services.file;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.jws.WebService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.PlanetsServices;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.datatypes.ServiceReport.Status;
import eu.planets_project.services.datatypes.ServiceReport.Type;
import eu.planets_project.services.file.util.FileServiceSetup;
import eu.planets_project.services.identify.Identify;
import eu.planets_project.services.identify.IdentifyResult;
import eu.planets_project.services.utils.FileUtils;
import eu.planets_project.services.utils.ProcessRunner;

/**
 * Class that implements an eu.planets_project.services.Identify interface.  It wraps the Cygwin
 * file utility, identifying passed digital objects and returning a Planets mimetype Format URI.  
 * 
 * @author <a href="mailto:carl.wilson@bl.uk">Carl Wilson</a>
 */
@Local(Identify.class)
@Remote(Identify.class)
@Stateless
@WebService(name = FileIdentify.NAME, serviceName = Identify.NAME, targetNamespace = PlanetsServices.NS, endpointInterface = "eu.planets_project.services.identify.Identify")
public class FileIdentify implements Identify {

    /** The logger */
    private static Log _log = LogFactory.getLog(FileIdentify.class);

    /** The service name */
    public static final String NAME = "FileIdentify";

    /**
	 * @see eu.planets_project.services.identify.Identify#describe()
	 */
    public ServiceDescription describe() {
        ServiceDescription.Builder mds = new ServiceDescription.Builder(NAME, Identify.class.getCanonicalName());
        mds.description("A DigitalObject Identification Service based on the cygwin File.exe program.");
        mds.author("Carl Wilson <Carl.Wilson@bl.uk>");
        mds.classname(this.getClass().getCanonicalName());
        return mds.build();
    }

    public IdentifyResult identify(DigitalObject digitalObject, List<Parameter> parameters) {
        if (digitalObject.getContent() == null) {
            return this.returnWithErrorMessage("The Content of the DigitalObject should not be NULL.", 1);
        }
        if (!FileServiceSetup.isWindows()) {
            return this.returnWithErrorMessage("OS detected not windows based, this service only runs on windows.", 1);
        }
        if (!FileServiceSetup.isCygwinFileDetected()) {
            return this.returnWithErrorMessage("Cygwin file.exe not found at location given in cygwin.file.location property.", 1);
        }
        byte[] binary = FileUtils.writeInputStreamToBinary(digitalObject.getContent().read());
        File tmpInFile = FileUtils.writeByteArrayToTempFile(binary);
        String[] commands = new String[] { FileServiceSetup.getFileLocation(), "-i", "-b", tmpInFile.getAbsolutePath() };
        ProcessRunner runner = new ProcessRunner();
        runner.setCommand(Arrays.asList(commands));
        runner.run();
        int retCode = runner.getReturnCode();
        if (retCode != 0) {
            return this.returnWithErrorMessage(runner.getProcessErrorAsString(), retCode);
        }
        String mime = runner.getProcessOutputAsString().trim();
        if (mime.indexOf(FileServiceSetup.getProperties().getProperty("cygwin.message.nofile")) != -1) {
            FileIdentify._log.debug("File failed to find an error");
            return this.returnWithErrorMessage(mime, 1);
        }
        ServiceReport rep = new ServiceReport(Type.INFO, Status.SUCCESS, "OK");
        List<URI> types = new ArrayList<URI>();
        URI mimeURI = FormatRegistryFactory.getFormatRegistry().createMimeUri(mime);
        types.add(mimeURI);
        return new IdentifyResult(types, IdentifyResult.Method.MAGIC, rep);
    }

    /**
	 * Method to create the IdentifyResult with an error message, used when things go wrong
	 * @param message
	 * 		The error message for the ServiceReport
	 * @return
	 * 		The IdentifyResult, correctly populated
	 */
    private IdentifyResult returnWithErrorMessage(String message, int errorState) {
        List<URI> type = null;
        FileIdentify._log.error(message);
        ServiceReport rep = new ServiceReport(Type.ERROR, Status.TOOL_ERROR, message);
        return new IdentifyResult(type, null, rep);
    }
}
