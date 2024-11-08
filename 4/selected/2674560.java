package com.legstar.xsd.java;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import com.legstar.codegen.CodeGenUtil;
import com.legstar.xsd.AbstractTest;
import com.legstar.xsd.def.Xsd2CobConfig;

/**
 * Test the Java2CobModel class.
 * 
 */
public class Java2CobModelTest extends AbstractTest {

    /** True when references should be created. */
    private static final boolean CREATE_REFERENCES = false;

    /** @{inheritDoc */
    public void setUp() throws Exception {
        super.setUp();
        setCreateReferences(CREATE_REFERENCES);
        FileUtils.forceMkdir(GEN_ANT_DIR);
    }

    public void testSerialize() {
        Java2CobModel model = new Java2CobModel();
        Properties props = model.toProperties();
        assertEquals("4", props.getProperty(Xsd2CobConfig.SHORT_TOTAL_DIGITS));
        model.setNewTargetNamespace("uri:some/namespace");
        props = model.toProperties();
        assertTrue(props.getProperty(Java2CobModel.NEW_TARGET_NAMESPACE).contains("uri:some/namespace"));
        List<String> classNames = Arrays.asList(new String[] { "org.FirstClass", "org.SecondClass" });
        model.setClassNames(classNames);
        props = model.toProperties();
        assertEquals("org.FirstClass", props.getProperty("classNames_0"));
        assertEquals("org.SecondClass", props.getProperty("classNames_1"));
        List<String> pathElementLocations = Arrays.asList(new String[] { "bin", "target/classes" });
        model.setPathElementLocations(pathElementLocations);
        props = model.toProperties();
        assertEquals("bin", props.getProperty("pathElementLocations_0"));
        assertEquals("target/classes", props.getProperty("pathElementLocations_1"));
    }

    public void testDeserialize() {
        Properties props = new Properties();
        Java2CobModel model = new Java2CobModel(props);
        assertEquals(4, model.getXsdConfig().getShortTotalDigits());
        props.setProperty(Java2CobModel.NEW_TARGET_NAMESPACE, "uri:some/namespace");
        model = new Java2CobModel(props);
        assertEquals("uri:some/namespace", model.getNewTargetNamespace());
        assertNull(model.getClassNames());
        props.put("classNames_0", "org.FirstClass");
        props.put("classNames_1", "org.SecondClass");
        model = new Java2CobModel(props);
        assertNotNull(model.getClassNames());
        assertEquals("org.FirstClass", model.getClassNames().get(0));
        assertEquals("org.SecondClass", model.getClassNames().get(1));
        assertNull(model.getPathElementLocations());
        props.put("pathElementLocations_0", "bin");
        props.put("pathElementLocations_1", "target/classes");
        model = new Java2CobModel(props);
        assertNotNull(model.getPathElementLocations());
        assertEquals("bin", model.getPathElementLocations().get(0));
        assertEquals("target/classes", model.getPathElementLocations().get(1));
    }

    /**
     * Use the model capability to generate an ANT script. Then submit to script
     * and check the output.
     * 
     * @throws Exception if something goes wrong
     */
    public void testAntScriptGeneration() throws Exception {
        Java2CobModel model = new Java2CobModel();
        model.setProductLocation("../../..");
        model.setProbeFile(new File("probe.file.tmp"));
        List<String> classNames = Arrays.asList(new String[] { "com.legstar.xsdc.test.cases.jvmquery.JVMQueryRequest", "com.legstar.xsdc.test.cases.jvmquery.JVMQueryReply" });
        model.setClassNames(classNames);
        model.setNewTargetNamespace("http://jvmquery.cases.test.xsdc.legstar.com/");
        List<String> pathElementLocations = Arrays.asList(new String[] { "target/classes", "target/test-classes" });
        model.setPathElementLocations(pathElementLocations);
        model.setTargetXsdFile(GEN_XSD_DIR);
        model.setTargetCobolFile(GEN_COBOL_DIR);
        model.setTargetCobolEncoding("ISO-8859-1");
        File resultFile = genAntScriptAsFile(model);
        check("build", "xml", FileUtils.readFileToString(resultFile, "UTF-8"));
        runAnt(resultFile);
        check("jvmquery", "xsd", FileUtils.readFileToString(new File(GEN_XSD_DIR, "jvmquery.xsd"), "UTF-8"));
        check("jvmquery", "cpy", FileUtils.readFileToString(new File(GEN_COBOL_DIR, "jvmquery.cpy"), "UTF-8"));
    }

    /**
     * Generates the build.xml that will be part of the distribution.
     * 
     * @throws Exception if something goes wrong
     */
    public void testDistributionAntScriptGeneration() throws Exception {
        Java2CobModel model = new Java2CobModel();
        model.setProductLocation(".");
        model.setProbeFile(new File("probe.file.tmp"));
        List<String> classNames = Arrays.asList(new String[] { "com.legstar.xsdc.test.cases.jvmquery.JVMQueryRequest", "com.legstar.xsdc.test.cases.jvmquery.JVMQueryReply" });
        model.setClassNames(classNames);
        model.setTargetXsdFile(new File("cobolschema"));
        model.setTargetCobolFile(new File("cobol"));
        List<String> pathElementLocations = Arrays.asList(new String[] { "${basedir}/java/legstar-test-jvmquery-classes.jar" });
        model.setPathElementLocations(pathElementLocations);
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
    protected File genAntScriptAsFile(final Java2CobModel model) throws Exception {
        File resultFile = CodeGenUtil.getFile(GEN_ANT_DIR, "build.xml");
        model.generateBuild(resultFile);
        return resultFile;
    }
}
