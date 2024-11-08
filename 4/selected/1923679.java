package com.legstar.xsd.def;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import com.legstar.codegen.CodeGenUtil;
import com.legstar.xsd.AbstractTest;
import com.legstar.xsd.XsdRootElement;

/**
 * Test the Xsd2CobModel class.
 * 
 */
public class Xsd2CobModelTest extends AbstractTest {

    /** True when references should be created. */
    private static final boolean CREATE_REFERENCES = false;

    /** @{inheritDoc */
    public void setUp() throws Exception {
        super.setUp();
        setCreateReferences(CREATE_REFERENCES);
        FileUtils.forceMkdir(GEN_ANT_DIR);
    }

    public void testSerialize() {
        Xsd2CobModel model = new Xsd2CobModel();
        Properties props = model.toProperties();
        assertEquals("4", props.getProperty(Xsd2CobConfig.SHORT_TOTAL_DIGITS));
        model.setInputXsdUri((new File(XSD_DIR, "customertype.xsd")).toURI());
        props = model.toProperties();
        assertTrue(props.getProperty(Xsd2CobModel.INPUT_XSD_URI).contains("src/test/resources/cases/customertype.xsd"));
        List<XsdRootElement> newRootElements = new ArrayList<XsdRootElement>();
        newRootElements.add(new XsdRootElement("el1:Type1"));
        newRootElements.add(new XsdRootElement("el2:Type2"));
        model.setNewRootElements(newRootElements);
        props = model.toProperties();
        assertEquals("el1:Type1", props.getProperty("newRootElements_0"));
        assertEquals("el2:Type2", props.getProperty("newRootElements_1"));
    }

    public void testDeserialize() {
        Properties props = new Properties();
        Xsd2CobModel model = new Xsd2CobModel(props);
        assertEquals(4, model.getXsdConfig().getShortTotalDigits());
        props.setProperty(Xsd2CobConfig.SHORT_TOTAL_DIGITS, "7");
        model = new Xsd2CobModel(props);
        assertEquals(7, model.getXsdConfig().getShortTotalDigits());
        assertNull(model.getNewRootElements());
        props.put("newRootElements_0", "el1:Type1");
        props.put("newRootElements_1", "el2:Type2");
        model = new Xsd2CobModel(props);
        assertNotNull(model.getNewRootElements());
        assertEquals("el1:Type1", model.getNewRootElements().get(0).toString());
        assertEquals("el2:Type2", model.getNewRootElements().get(1).toString());
    }

    /**
     * Use the model capability to generate an ANT script. Then submit to script
     * and check the output.
     * 
     * @throws Exception if something goes wrong
     */
    public void testAntScriptGeneration() throws Exception {
        Xsd2CobModel model = new Xsd2CobModel();
        model.setProductLocation("../../../..");
        model.setProbeFile(new File("probe.file.tmp"));
        model.setInputXsdUri(new URI("src/test/resources/cases/customertype.xsd"));
        model.setTargetXsdFile(GEN_XSD_DIR);
        model.setTargetCobolFile(GEN_COBOL_DIR);
        model.setTargetCobolEncoding("ISO-8859-1");
        model.addNewRootElement(new XsdRootElement("customer", "CustomerType"));
        model.setCustomXsltFileName("src/test/resources/xslt/customertype.xsl");
        File resultFile = genAntScriptAsFile(model);
        check("build", "xml", FileUtils.readFileToString(resultFile, "UTF-8"));
        runAnt(resultFile);
        check("customertype", "xsd", FileUtils.readFileToString(new File(GEN_XSD_DIR, "customertype.xsd"), "UTF-8"));
        check("customertype", "cpy", FileUtils.readFileToString(new File(GEN_COBOL_DIR, "customertype.cpy"), "UTF-8"));
    }

    /**
     * Generates the build.xml that will be part of the distribution.
     * 
     * @throws Exception if something goes wrong
     */
    public void testDistributionAntScriptGeneration() throws Exception {
        Xsd2CobModel model = new Xsd2CobModel();
        model.setProductLocation(".");
        model.setProbeFile(new File("probe.file.tmp"));
        model.setInputXsdUri(new URI("schema"));
        model.setTargetXsdFile(new File("cobolschema"));
        model.setTargetCobolFile(new File("cobol"));
        File resultFile = genAntScriptAsFile(model);
        FileUtils.copyFileToDirectory(resultFile, new File("target/gen-distro"));
    }

    /**
     * Generates an ant script from a VLC template.
     * 
     * @param model the generation model
     * @return the script as a string
     * @throws Exception if generation fails
     */
    protected File genAntScriptAsFile(final Xsd2CobModel model) throws Exception {
        File resultFile = CodeGenUtil.getFile(GEN_ANT_DIR, "build.xml");
        model.generateBuild(resultFile);
        return resultFile;
    }
}
