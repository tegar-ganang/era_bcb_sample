package uk.ac.ncl.cs.instantsoap.r;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import org.bjv2.util.serviceprovider.SpiProvider;
import uk.ac.ncl.cs.instantsoap.stringprocessor.StringProcessor;
import uk.ac.ncl.cs.instantsoap.wsapi.InvalidJobSpecificationException;
import uk.ac.ncl.cs.instantsoap.wsapi.JobExecutionException;
import uk.ac.ncl.cs.instantsoap.wsapi.MetaData;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import static uk.ac.ncl.cs.instantsoap.wsapi.Wsapi.metaData;

/**
 * Describe class RSimpleProcessor here.
 *
 *
 * Created: Thu Jul  3 17:37:13 2008
 *
 * @author <a href="mailto:phillord@ncl.ac.uk">Phillip Lord</a>
 * @version 1.0
 */
public abstract class RDataFrameProcessor extends RProcessorBase implements StringProcessor {

    public abstract String getRScriptName();

    public String getRDriverName() {
        return "r_dataframe_processor.R";
    }

    public String process(String input) throws JobExecutionException {
        try {
            File inputDataFile = File.createTempFile("instantsoap-r", ".inp");
            File outputDataFile = File.createTempFile("instantsoap-r", ".out");
            File rScriptFile = File.createTempFile("instantsoap-r", ".r");
            writeStringToFile(inputDataFile, input);
            writeStringToFile(rScriptFile, readStringFromInputStream(getInputStreamForRScript(getClass(), getRScriptName())));
            Map<String, String> commandline = new HashMap<String, String>();
            commandline.put("input", inputDataFile.getCanonicalPath());
            commandline.put("output", outputDataFile.getCanonicalPath());
            commandline.put("resource", rScriptFile.getCanonicalPath());
            String debug = executeR(commandline);
            System.out.println(debug);
            String retn = readStringFromFile(outputDataFile).trim();
            return retn;
        } catch (IOException exp) {
            throw new JobExecutionException("There has been an error in the execution of R script: " + getRScriptName(), exp);
        }
    }

    public void validate(String input) throws InvalidJobSpecificationException {
    }
}
