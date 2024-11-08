package au.edu.archer.metadata.msf.mss.tool;

import static au.edu.archer.metadata.msf.mss.util.MSSResourceUtils.MSS_EXTENSION;
import static au.edu.archer.metadata.msf.mss.util.MSSResourceUtils.XSD_EXTENSION;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XSGrammar;
import org.apache.xerces.xs.XSObject;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import au.edu.archer.metadata.msf.mss.MSSFactory;
import au.edu.archer.metadata.msf.mss.MetadataSchema;
import au.edu.archer.metadata.msf.mss.Model;
import au.edu.archer.metadata.msf.mss.util.InternationalText;
import au.edu.archer.metadata.msf.mss.util.MSSResourceUtils;
import au.edu.archer.metadata.msf.mss.util.Name;
import au.edu.archer.metadata.msf.mss.util.VersionNumber;

/**
 * Command-line class for the XSD to MSS Schema extractor.
 *
 * @author scrawley@itee.uq.edu.au
 */
public class XSDExtractorTool {

    private static final String[] SEVERITY_NAMES = { "OK", "INFO", "WARNING", null, "ERROR", null, null, null, "CANCEL" };

    private boolean force = false;

    private boolean ignoreWarnings = false;

    private String defaultLang = null;

    private QName rootElementName = null;

    private File modelFile = null;

    private File schemaFile = null;

    private List<File> inputFiles;

    private DiagnosticChain diags = new BasicDiagnostic();

    private static final String USAGE = "Usage: xsdextract [--force] [--ignoreWarnings] [ [--model | --schema] <output.mss>] " + "[--root '{'<ns>'}'<eltName>] [--defaultLang <iso-alpha-2-code>] <schema.xsd> ...";

    private void error(String message) throws ExtractionException {
        throw new ExtractionException(message);
    }

    public static void main(String[] args) {
        try {
            new XSDExtractorTool().run(args);
        } catch (ExtractionException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    /**
     * Run from command-line.
     *
     * @param args
     * @throws ExtractionException
     */
    private void run(String[] args) throws ExtractionException {
        processArgs(args);
        List<Grammar> grammars = loadGrammars(inputFiles);
        System.err.println("Loaded.");
        List<MetadataSchema> schemas = extractSchemas(grammars);
        if (processDiagnostics(diags) && !ignoreWarnings) {
            error("MSS file(s) not written due to the above errors and/or warnings.  " + "(You could use --ignoreWarnings to attempt to output the file(s) anyway.)");
        }
        if (modelFile != null) {
            checkOutputFile(modelFile, force);
            outputModel(schemas, modelFile);
        } else {
            List<File> schemaFiles = prepareSchemaFiles();
            for (int i = 0; i < schemas.size(); i++) {
                outputSchema(schemas.get(i), schemaFiles.get(i));
            }
        }
        System.err.println("Done.");
    }

    /**
     * Process command-line arguments.
     *
     * @param args
     * @throws ExtractionException
     */
    private void processArgs(String[] args) throws ExtractionException {
        if (args.length < 1) {
            error(USAGE);
        }
        int argNo;
        for (argNo = 0; argNo < args.length; argNo++) {
            String arg = args[argNo];
            if (!arg.startsWith("-")) {
                break;
            } else if (arg.equals("--force")) {
                force = true;
            } else if (arg.equals("--bestEffort")) {
                ignoreWarnings = true;
            } else if (arg.equals("--defaultLang")) {
                argNo++;
                if (argNo >= args.length) {
                    error(USAGE);
                }
                defaultLang = args[argNo];
                if (!InternationalText.isValidLang(defaultLang)) {
                    error("Unrecognized ISO 2 character language code: '" + defaultLang + "'");
                }
            } else if (arg.equals("--root")) {
                argNo++;
                if (argNo >= args.length) {
                    error(USAGE);
                }
                try {
                    rootElementName = QName.valueOf(args[argNo]);
                } catch (IllegalArgumentException ex) {
                    error("Malformed --root QName value: " + ex.getMessage());
                    return;
                }
            } else if (arg.equals("--model")) {
                argNo++;
                if (argNo >= args.length) {
                    error(USAGE);
                }
                if (!args[argNo].endsWith(MSS_EXTENSION)) {
                    error("The --model filename MUST have a suffix of '" + MSS_EXTENSION + "'");
                }
                modelFile = new File(args[argNo]);
            } else if (arg.equals("--schema")) {
                argNo++;
                if (argNo >= args.length) {
                    error(USAGE);
                }
                if (!args[argNo].endsWith(MSS_EXTENSION)) {
                    error("The --schema filename MUST have a suffix of '" + MSS_EXTENSION + "'");
                }
                schemaFile = new File(args[argNo]);
            } else {
                error(USAGE);
            }
        }
        if (argNo >= args.length) {
            error(USAGE);
        }
        inputFiles = new ArrayList<File>(args.length - argNo);
        while (argNo < args.length) {
            File inputFile = new File(args[argNo++]);
            if (!inputFile.exists()) {
                error("File does not exist: " + inputFile);
            }
            if (!inputFile.isFile()) {
                error("Not a file: " + inputFile);
            }
            inputFiles.add(inputFile);
        }
        if (modelFile != null && schemaFile != null) {
            error("You cannot supply both --schema and --model");
        }
        if (inputFiles.size() > 1 && schemaFile != null) {
            error("You cannot use --schema with multiple <xsd> arguments");
        }
    }

    /**
     * Extract MSS schemas for a list of grammars
     *
     * @param grammars
     * @return
     * @throws ExtractionException
     */
    private List<MetadataSchema> extractSchemas(List<Grammar> grammars) throws ExtractionException {
        int len = grammars.size();
        List<MetadataSchema> schemas = new ArrayList<MetadataSchema>(len);
        for (int i = 0; i < len; i++) {
            schemas.add(extractSchema(grammars.get(i), inputFiles.get(i)));
        }
        return schemas;
    }

    /**
     * Extract an MSS schema from a grammar.
     * @param grammar the input grammar
     * @param inputFile the file the grammar was loaded from (for diagnostics).
     * @return the extracted schema
     * @throws ExtractionException
     */
    private MetadataSchema extractSchema(Grammar grammar, File inputFile) throws ExtractionException {
        MetadataSchema schema = null;
        try {
            XSDExtractor ext = new XSDExtractor();
            schema = ext.extract((XSGrammar) grammar, rootElementName, diags, inputFile, defaultLang);
            System.err.println("Extracted schema for " + inputFile);
        } catch (UnsupportedXSDException ex) {
            System.err.println("No MSS schema could be extracted from " + inputFile);
            System.err.println("Reason: " + ex.getMessage());
            XSObject context = ex.getContext();
            if (context != null) {
                System.err.println("The problem was encountered in or near this XSD node: " + context);
            }
            processDiagnostics(diags);
            error("Extraction failed");
        }
        return schema;
    }

    /**
     * Load grammars from a list of XSD files.
     * @param inputFiles
     * @return
     * @throws ExtractionException
     */
    private List<Grammar> loadGrammars(List<File> inputFiles) throws ExtractionException {
        List<Grammar> res = new ArrayList<Grammar>(inputFiles.size());
        for (File inputFile : inputFiles) {
            try {
                res.add(XSDExtractor.loadGrammar(inputFile));
            } catch (IOException ex) {
                error("Cannot load XSD file: " + ex.getMessage());
            }
        }
        return res;
    }

    /**
     * Create a list of MSS files to write schemas to.
     * @return
     * @throws ExtractionException
     */
    private List<File> prepareSchemaFiles() throws ExtractionException {
        List<File> schemaFiles = new ArrayList<File>(inputFiles.size());
        if (schemaFile != null) {
            checkOutputFile(schemaFile, force);
            schemaFiles.add(schemaFile);
        } else {
            for (File inputFile : inputFiles) {
                String iName = inputFile.getName();
                String oName;
                if (iName.endsWith(XSD_EXTENSION)) {
                    oName = iName.substring(0, iName.length() - XSD_EXTENSION.length()) + MSS_EXTENSION;
                } else {
                    oName = iName + MSS_EXTENSION;
                }
                File outputFile = new File(inputFile.getParent(), oName);
                checkOutputFile(outputFile, force);
                schemaFiles.add(outputFile);
            }
        }
        return schemaFiles;
    }

    /**
     * Output file overwrite checking.  Currently prevents overwriting unless
     * the 'force' parameter is <code>true</code>.
     *
     * @param outputFile file to be output
     * @param force if <code>true</code> the file may be silently overwritten.
     * @throws ExtractionException
     */
    private void checkOutputFile(File outputFile, boolean force) throws ExtractionException {
        if (outputFile.exists() && !force) {
            error("Output file '" + outputFile + "' already exists.  " + "(Use --force to overwrite it.)");
        }
    }

    /**
     * Output an MSS schema to a file.
     *
     * @param schema the schema to be written
     * @param file the destination file.
     * @throws ExtractionException
     */
    private void outputSchema(MetadataSchema schema, File file) throws ExtractionException {
        try {
            MSSResourceUtils.saveAsResource(schema, file);
            System.err.println("Wrote output to '" + file.getAbsolutePath());
        } catch (IOException ex) {
            error("Problem writing the schema file '" + file + "': " + ex.getMessage());
        }
    }

    /**
     * Output a list of MSS schemas to a file as an MSS Model.  The Model name
     * is inferred from the filename.
     *
     * @param schemas the schemas to be written.
     * @param file the model file.
     * @throws ExtractionException
     */
    private void outputModel(List<MetadataSchema> schemas, File file) throws ExtractionException {
        String name = file.getName();
        if (name.endsWith(MSS_EXTENSION)) {
            name = name.substring(0, name.length() - MSS_EXTENSION.length());
        }
        Model model = MSSFactory.eINSTANCE.createModel();
        model.getContents().addAll(schemas);
        model.setModelName(Name.valueOf(name));
        model.setModelVersion(VersionNumber.valueOf("1.0"));
        try {
            MSSResourceUtils.saveAsResource(model, file);
            System.err.println("Wrote output to '" + file.getAbsolutePath());
        } catch (IOException ex) {
            error("Problem writing the schema file '" + file + "': " + ex.getMessage());
        }
    }

    /**
     * Examine the diagnostics (if any), list any messages and decide whether
     * there are 'errors'.
     *
     * @param diags a diagnostic chain, or <code>null</code>.
     * @return <code>true</code> if there are errors.
     */
    private static boolean processDiagnostics(DiagnosticChain diags) {
        if (diags != null) {
            Diagnostic diag = (Diagnostic) diags;
            switch(diag.getSeverity()) {
                case Diagnostic.OK:
                    return false;
                case Diagnostic.INFO:
                    listDiagnostics(diag);
                    return false;
                case Diagnostic.WARNING:
                case Diagnostic.ERROR:
                case Diagnostic.CANCEL:
                    listDiagnostics(diag);
                    return true;
                default:
                    throw new AssertionError("Unexpected severity");
            }
        } else {
            return false;
        }
    }

    private static void listDiagnostics(Diagnostic diag) {
        int[] counts = new int[Diagnostic.CANCEL + 1];
        for (Diagnostic child : diag.getChildren()) {
            int severity = child.getSeverity();
            if (severity > Diagnostic.OK) {
                counts[severity]++;
                List<?> data = child.getData();
                System.err.println(SEVERITY_NAMES[severity] + ": " + child.getMessage());
                for (int i = 0; i < data.size(); i++) {
                    System.err.println("  context: " + data.get(i));
                }
            }
        }
        StringBuffer sb = new StringBuffer(30);
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] == 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(counts[i]).append(" ").append(SEVERITY_NAMES[i]);
            sb.append(" message");
            if (counts[i] > 1) {
                sb.append("s");
            }
        }
        if (sb.length() > 0) {
            System.err.println(sb);
        }
    }
}
