package com.legstar.cixs.jbossesb.gen;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import com.legstar.cixs.gen.model.options.HttpTransportParameters;
import com.legstar.cixs.gen.model.options.WmqTransportParameters;
import com.legstar.cixs.jbossesb.model.options.JbmTransportParameters;
import com.legstar.codegen.CodeGenHelper;
import com.legstar.codegen.CodeGenUtil;
import com.legstar.coxb.gen.CoxbHelper;
import junit.framework.TestCase;

/**
 * This is code common to all junit tests that exercise the velocity
 * templates.
 */
public class AbstractTestTemplate extends TestCase {

    /** Parent generation folder. */
    public static final File GEN_DIR = new File("target/src/gen");

    /** Location of JAXB classes. */
    public static final File JAXB_BIN_DIR = new File("target/classes");

    /** Code will be generated here. */
    public static final File GEN_SRC_DIR = new File("target/src/gen/java");

    /** Configuration files will be generated here. */
    public static final File GEN_CONF_DIR = new File("target/src/gen/conf");

    /** Web descriptors files will be generated here. */
    public static final File GEN_ANT_DIR = new File("target/src/gen/ant");

    /** Distributable archives are generated here. */
    public static final File GEN_DIST_DIR = new File("target/dist");

    /** Reference to esb files location. */
    public static final File GEN_ESB_DIR = new File("${env.JBOSS_HOME}/server/default/deploy");

    /** Reference to binaries location. */
    public static final File GEN_BIN_DIR = new File("target/gen-classes");

    /** COBOL code will be generated here. */
    public static final File GEN_COBOL_DIR = new File("target/src/gen/cobol");

    /** Additional parameter set passed to templates. */
    private Map<String, Object> mParameters;

    /** Reference files which are not sources. */
    public static final File REF_DIR = new File("src/test/resources/reference");

    /** This means references should be created instead of compared to results. */
    private boolean _createReferences;

    /**
     * @return true if references should be created instead of compared to results
     */
    public boolean isCreateReferences() {
        return _createReferences;
    }

    /**
     * @param createReferences true if references should be created instead of compared to results
     */
    public void setCreateReferences(boolean createReferences) {
        _createReferences = createReferences;
    }

    /** @{inheritDoc}*/
    public void setUp() {
        try {
            CodeGenUtil.initVelocity();
            mParameters = new HashMap<String, Object>();
            CodeGenHelper helper = new CodeGenHelper();
            mParameters.put("helper", helper);
            mParameters.put("coxbHelper", new CoxbHelper());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the mParameters
     */
    public Map<String, Object> getParameters() {
        return mParameters;
    }

    /**
     * Recreates a folder after emptying its content.
     * @param dir the folder to empy
     */
    public void emptyDir(final File dir) {
        deleteDir(dir);
        dir.mkdirs();
    }

    /**
     * Destroys a folder and all of its content.
     * @param dir the folder to destroy
     */
    public void deleteDir(final File dir) {
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    deleteDir(file);
                }
                file.delete();
            }
        }
    }

    /**
     * Set all parameters that relate to the Tomcat transport.
     */
    public void setTomcatTransport() {
        getParameters().put("sampleConfigurationTransport", "HTTP");
        getParameters().put("httpProvider", "tomcat");
        HttpTransportParameters httpTransport = new HttpTransportParameters();
        httpTransport.setHost("localhost");
        httpTransport.setPath("/jbossesb/tomcat/listener/");
        httpTransport.setPort(8765);
        httpTransport.setScheme("http");
        httpTransport.add(getParameters());
    }

    /**
     * Set all parameters that relate to the wmq transport.
     */
    public void setWmqTransport() {
        getParameters().put("sampleConfigurationTransport", "WMQ");
        WmqTransportParameters wmqTParameters = new WmqTransportParameters();
        wmqTParameters.setConnectionFactory("MyQCF");
        wmqTParameters.setErrorQueue("TARGET.ERROR.QUEUE");
        wmqTParameters.setJndiContextFactory("com.legstar.cixs.jbossesb.HostContextFactory");
        wmqTParameters.setJndiUrl("file://src/test/resources/host-jndi");
        wmqTParameters.setReplyQueue("TARGET.REPLY.QUEUE");
        wmqTParameters.setRequestQueue("TARGET.QUEUE");
        wmqTParameters.setZosQueueManager("CSQ1");
        wmqTParameters.add(getParameters());
    }

    /**
     * Set all parameters that relate to the JBM transport.
     * @param serviceName used to generate default values
     */
    public void setJbmTransport(final String serviceName) {
        getParameters().put("sampleConfigurationTransport", "JBM");
        JbmTransportParameters jbmTransportParameters = new JbmTransportParameters();
        jbmTransportParameters.initialize(serviceName);
        jbmTransportParameters.add(getParameters());
    }

    /**
     * TODO move this code to com.legstar.codegen.CodeGenUtil#classNormalize.
     * Create a valid Java class name from a given noun.
     * 
     * @param noun
     *            the characters to turn into a java class name
     * @return the Java class name
     */
    public static String classNormalize(final String noun) {
        boolean start = true;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < noun.length(); i++) {
            char c = noun.charAt(i);
            if (start) {
                if (Character.isJavaIdentifierStart(c)) {
                    sb.append(Character.toUpperCase(c));
                    start = false;
                }
            } else {
                if (Character.isJavaIdentifierPart(c)) {
                    sb.append(c);
                } else {
                    start = true;
                }
            }
        }
        return sb.toString();
    }

    /**
     * Return a class unqualified (no package prefix) name.
     * 
     * @param clazz the class
     * @return the unqualified name
     */
    public static String getUnqualName(final Class<?> clazz) {
        String unqname = clazz.getName();
        if (unqname.lastIndexOf('.') > 0) {
            unqname = unqname.substring(unqname.lastIndexOf('.') + 1);
        }
        return unqname;
    }

    /**
     * Check a result against a reference.
     * 
     * @param fileName the file name to check
     * @param refFolder the reference folder
     * @param resultFolder the result folder
     * @throws Exception if something fails
     */
    public void check(final String fileName, final File resultFolder) throws Exception {
        File refFolder = new File(REF_DIR, getUnqualName(getClass()) + "/" + getName());
        check(fileName, refFolder, resultFolder);
    }

    /**
     * Check a result against a reference.
     * 
     * @param fileName the file name to check
     * @param refFolder the reference folder
     * @param resultFolder the result folder
     * @throws Exception if something fails
     */
    public void check(final String fileName, final File refFolder, final File resultFolder) throws Exception {
        File resultFile = new File(resultFolder, fileName);
        String normalizedResult = normalizeFileLocations(FileUtils.readFileToString(resultFile));
        File normalizedResultFile = File.createTempFile("result", null, new File(System.getProperty("java.io.tmpdir")));
        normalizedResultFile.deleteOnExit();
        FileUtils.writeStringToFile(normalizedResultFile, normalizedResult);
        if (isCreateReferences()) {
            FileUtils.copyFile(normalizedResultFile, new File(refFolder, fileName));
        } else {
            File referenceFile = new File(refFolder, fileName);
            assertEquals(referenceFile, normalizedResultFile);
        }
    }

    /**
     * When comparing file contents we neutralize any platform specific line
     * ending character such as CR (\r).
     * 
     * @param referenceFile the expected file
     * @param resultFile the result file
     * @throws Exception if something fails
     */
    protected void assertEquals(final File referenceFile, final File resultFile) throws Exception {
        String expected = FileUtils.readFileToString(referenceFile);
        String result = FileUtils.readFileToString(resultFile);
        assertEquals(String.format("comparing result file %s with %s", resultFile.getName(), referenceFile.getName()), expected.replace("\r", ""), result.replace("\r", ""));
    }

    /**
     * Reference files should not hold any file physical locations otherwise
     * they won't be portable. Here we:
     * <ul>
     * <li>make all locations relative</li>
     * <li>replace environment variable values by ant style references</li>
     * <li>normalize path separators as UNIX style separators</li>
     * </ul>
     * 
     * @param text the text to normalize
     * @return text where physical locations have been removed
     */
    protected String normalizeFileLocations(final String text) {
        String phyloc = new File("").getAbsolutePath();
        return text.replace(phyloc + File.separatorChar, "").replace(System.getenv("LEGSTAR_HOME"), "${env.LEGSTAR_HOME}").replace(System.getenv("JBOSS_HOME"), "${env.JBOSS_HOME}").replace(File.separatorChar, '/');
    }
}
