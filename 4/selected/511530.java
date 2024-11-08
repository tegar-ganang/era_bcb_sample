package eu.planets_project.ifr.core.services.characterisation.extractor.impl;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistry;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.datatypes.Content;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.MigrationPath;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.datatypes.ServiceReport.Type;
import eu.planets_project.services.migrate.Migrate;
import eu.planets_project.services.migrate.MigrateResult;
import eu.planets_project.services.utils.FileUtils;
import eu.planets_project.services.utils.test.ServiceCreator;

/**
 * Test of the extractor (local and remote) using binaries.
 * @author Peter Melms
 */
public class XcdlMigrateTests {

    private static final String WSDL = "/pserv-xcl/XcdlMigrate?wsdl";

    private static Migrate extractor;

    private static String testOut = null;

    private static File testOutFolder = null;

    static MigrationPath[] migrationPaths;

    private static final FormatRegistry format = FormatRegistryFactory.getFormatRegistry();

    /**
     * Set up the testing environment: create files and directories for testing.
     */
    @BeforeClass
    public static void setup() {
        testOut = XcdlMigrateUnitHelper.XCDL_EXTRACTOR_LOCAL_TEST_OUT;
        testOutFolder = FileUtils.createWorkFolderInSysTemp(testOut);
        extractor = ServiceCreator.createTestService(Migrate.QNAME, XcdlMigrate.class, WSDL);
        migrationPaths = extractor.describe().getPaths().toArray(new MigrationPath[] {});
    }

    /**
     * Test describe method.
     */
    @Test
    public void testDescribe() {
        ServiceDescription sd = extractor.describe();
        System.out.println("test: describe()");
        System.out.println("--------------------------------------------------------------------");
        System.out.println();
        System.out.println("Received ServiceDescription from: " + extractor.getClass().getName());
        assertTrue("The ServiceDescription should not be NULL.", sd != null);
        System.out.println(sd.toXmlFormatted());
        System.out.println("--------------------------------------------------------------------");
    }

    @Test
    public void testMigration() throws URISyntaxException {
        testPath(migrationPaths[0]);
    }

    @Test
    public void testRejectInvalidFormat() {
        DigitalObject o = new DigitalObject.Builder(Content.byValue(new byte[] {})).build();
        FormatRegistry registry = FormatRegistryFactory.getFormatRegistry();
        MigrateResult result = extractor.migrate(o, null, null, null);
        Assert.assertEquals(Type.ERROR, result.getReport().getType());
        Assert.assertEquals(null, result.getDigitalObject());
        result = extractor.migrate(o, registry.createExtensionUri("svg"), null, null);
        Assert.assertEquals(Type.ERROR, result.getReport().getType());
        Assert.assertEquals(null, result.getDigitalObject());
    }

    protected void testPath(MigrationPath path) throws URISyntaxException {
        URI inputFormat = path.getInputFormat();
        URI outputFormat = path.getOutputFormat();
        System.out.println();
        System.out.println("Testing migrationPath: [" + inputFormat.toASCIIString() + " --> " + outputFormat.toASCIIString() + "]");
        System.out.println();
        System.out.println("PARAMS: disableNormData = FALSE, enableRawData = FALSE, XCEL = YES");
        if (inputFormat.toASCIIString().equalsIgnoreCase("planets:fmt/ext/gif") || inputFormat.toASCIIString().equalsIgnoreCase("planets:fmt/ext/bmp") || inputFormat.toASCIIString().equalsIgnoreCase("planets:fmt/ext/jpg") || inputFormat.toASCIIString().equalsIgnoreCase("planets:fmt/ext/jpeg")) {
            System.err.println("NOTE: NO Xcel will be passed for this input format. " + "Extractor will find the proper one itself!");
        }
        List<Parameter> parameters = createParameters(false, false, getTestXCEL(format.getFirstExtension(inputFormat)));
        testMigrate(inputFormat, outputFormat, parameters);
        System.out.println("*******************");
        System.out.println();
        System.out.println("PARAMS: disableNormData = FALSE, enableRawData = FALSE, XCEL = NO");
        parameters = createParameters(false, false, null);
        testMigrate(inputFormat, outputFormat, parameters);
        System.out.println("*******************");
        System.out.println();
        System.out.println("PARAMS: disableNormData = TRUE, enableRawData = FALSE, XCEL = YES");
        if (inputFormat.toASCIIString().equalsIgnoreCase("planets:fmt/ext/gif") || inputFormat.toASCIIString().equalsIgnoreCase("planets:fmt/ext/bmp") || inputFormat.toASCIIString().equalsIgnoreCase("planets:fmt/ext/jpg") || inputFormat.toASCIIString().equalsIgnoreCase("planets:fmt/ext/jpeg")) {
            System.err.println("NOTE: NO Xcel will be passed for this input format. " + "Extractor will find the proper one itself!");
        }
        parameters = createParameters(true, false, getTestXCEL(format.getFirstExtension(inputFormat)));
        testMigrate(inputFormat, outputFormat, parameters);
        System.out.println("*******************");
        System.out.println();
        System.out.println("PARAMS: disableNormData = TRUE, enableRawData = FALSE, XCEL = NO");
        parameters = createParameters(true, false, null);
        testMigrate(inputFormat, outputFormat, parameters);
        System.out.println("*******************");
        System.out.println();
    }

    private List<Parameter> createParameters(boolean disableNormDataFlag, boolean enableRawDataFlag, String optionalXCELString) {
        List<Parameter> parameterList = new ArrayList<Parameter>();
        if (disableNormDataFlag) {
            Parameter normDataFlag = new Parameter.Builder("disableNormDataInXCDL", "-n").description("Disables NormData output in result XCDL. Reduces file size. Allowed value: '-n'").build();
            parameterList.add(normDataFlag);
        }
        if (enableRawDataFlag) {
            Parameter enableRawData = new Parameter.Builder("enableRawDataInXCDL", "-r").description("Enables the output of RAW Data in XCDL file. Allowed value: '-r'").build();
            parameterList.add(enableRawData);
        }
        if (optionalXCELString != null) {
            Parameter xcelStringParam = new Parameter.Builder("optionalXCELString", optionalXCELString).description("Could contain an optional XCEL String which is passed to the Extractor tool.\n\r" + "If no XCEL String is passed, the Extractor tool will try to  find the corresponding XCEL himself.").build();
            parameterList.add(xcelStringParam);
        }
        return parameterList;
    }

    private String getTestXCEL(String srcExtension) {
        if (srcExtension.equalsIgnoreCase("TIFF")) {
            return FileUtils.readTxtFileIntoString(XcdlMigrateUnitHelper.TIFF_XCEL);
        }
        if (srcExtension.equalsIgnoreCase("BMP")) {
            return null;
        }
        if (srcExtension.equalsIgnoreCase("GIF")) {
            return null;
        }
        if (srcExtension.equalsIgnoreCase("PDF")) {
            return FileUtils.readTxtFileIntoString(XcdlMigrateUnitHelper.PDF_XCEL);
        }
        if (srcExtension.equalsIgnoreCase("JPEG") || srcExtension.equalsIgnoreCase("JPG")) {
            return null;
        }
        if (srcExtension.equalsIgnoreCase("PNG")) {
            return FileUtils.readTxtFileIntoString(XcdlMigrateUnitHelper.PNG_XCEL);
        }
        return null;
    }

    private File getTestFile(String srcExtension) {
        System.out.println("Looking for file matching extension: " + srcExtension);
        if (srcExtension.equalsIgnoreCase("TIFF") || srcExtension.equalsIgnoreCase("TIF")) {
            return XcdlMigrateUnitHelper.TIFF_INPUT;
        }
        if (srcExtension.equalsIgnoreCase("BMP")) {
            return XcdlMigrateUnitHelper.BMP_INPUT;
        }
        if (srcExtension.equalsIgnoreCase("GIF")) {
            return XcdlMigrateUnitHelper.GIF_INPUT;
        }
        if (srcExtension.equalsIgnoreCase("PDF")) {
            return XcdlMigrateUnitHelper.PDF_INPUT;
        }
        if (srcExtension.equalsIgnoreCase("JPEG") || srcExtension.equalsIgnoreCase("JPG")) {
            return XcdlMigrateUnitHelper.JPEG_INPUT;
        }
        if (srcExtension.equalsIgnoreCase("PNG")) {
            return XcdlMigrateUnitHelper.PNG_INPUT;
        }
        System.err.println("Found no file matching extension: " + srcExtension);
        return null;
    }

    private DigitalObject createDigitalObject(String srcExtension) {
        File inputFile = getTestFile(srcExtension);
        DigitalObject input = null;
        input = new DigitalObject.Builder(Content.byReference(inputFile)).title("test input file with spaces.bin").build();
        return input;
    }

    private void testMigrate(URI inputFormat, URI outputFormat, List<Parameter> parameters) {
        String extension = format.getFirstExtension(inputFormat);
        DigitalObject digObj = createDigitalObject(extension);
        MigrateResult mr = extractor.migrate(digObj, inputFormat, outputFormat, parameters);
        ServiceReport sr = mr.getReport();
        if (sr.getType() == Type.ERROR) {
            System.err.println("FAILED: " + sr);
        } else {
            System.out.println("Got Report: " + sr);
            DigitalObject doOut = mr.getDigitalObject();
            assertTrue("Resulting digital object is null.", doOut != null);
            File formatFolder = FileUtils.createFolderInWorkFolder(testOutFolder, extension);
            File result = FileUtils.writeInputStreamToFile(doOut.getContent().read(), formatFolder, "xcdlMigrateTest_" + extension + ".xcdl");
            System.out.println("Resulting file size: " + result.length() + " KB.");
            System.out.println("Resulting file path: " + result.getAbsolutePath());
            System.out.println("Result: " + doOut);
            System.out.println("Result.content: " + doOut.getContent());
        }
    }

    @SuppressWarnings("unused")
    private DigitalObject createDigitalObjectByReference(URL permanentURL, URL reference) {
        DigitalObject digObj = new DigitalObject.Builder(Content.byReference(reference)).build();
        return digObj;
    }
}
