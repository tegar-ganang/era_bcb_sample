package net.sf.ahtutils.test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import net.sf.ahtutils.controller.factory.java.security.AbstractJavaSecurityFactoryTest;
import net.sf.ahtutils.xml.ns.AhtUtilsNsPrefixMapper;
import net.sf.exlp.util.io.LoggerInit;
import net.sf.exlp.util.io.RelativePathFactory;
import net.sf.exlp.util.io.StringIO;
import net.sf.exlp.util.xml.JaxbUtil;
import net.sf.exlp.xml.ns.NsPrefixMapperInterface;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.openfuxml.renderer.processor.latex.util.OfxLatexRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractAhtUtilTest {

    static final Logger logger = LoggerFactory.getLogger(AbstractAhtUtilTest.class);

    protected static NsPrefixMapperInterface nsPrefixMapper;

    protected File f;

    protected boolean saveReference = false;

    protected static File fTarget;

    protected static void setfTarget(File fTarget) {
        AbstractJavaSecurityFactoryTest.fTarget = fTarget;
    }

    @BeforeClass
    public static void initFile() {
        if (!LoggerInit.isLog4jInited()) {
            initLogger();
        }
        String dirTarget = System.getProperty("targetDir");
        if (dirTarget == null) {
            dirTarget = "target";
        }
        setfTarget(new File(dirTarget));
        logger.debug("Using targeDir " + fTarget.getAbsolutePath());
    }

    @BeforeClass
    public static void initLogger() {
        if (!LoggerInit.isLog4jInited()) {
            LoggerInit loggerInit = new LoggerInit("log4junit.xml");
            loggerInit.addAltPath("config.ahtutils-util.test");
            loggerInit.init();
        }
    }

    protected void assertJaxbEquals(Object expected, Object actual) {
        Assert.assertEquals("actual XML differes from expected XML", JaxbUtil.toString(expected), JaxbUtil.toString(actual));
    }

    protected NsPrefixMapperInterface getPrefixMapper() {
        if (nsPrefixMapper == null) {
            nsPrefixMapper = new AhtUtilsNsPrefixMapper();
        }
        return nsPrefixMapper;
    }

    protected void saveXml(Object xml, File f, boolean formatted) {
        if (saveReference) {
            logger.debug("Saving Reference XML");
            JaxbUtil.debug2(this.getClass(), xml, getPrefixMapper());
            JaxbUtil.save(f, xml, getPrefixMapper(), formatted);
        }
    }

    protected void debug(OfxLatexRenderer renderer) {
        if (logger.isDebugEnabled()) {
            logger.debug("Debugging " + renderer.getClass().getSimpleName());
            System.out.println("************************************");
            for (String s : renderer.getContent()) {
                System.out.println(s);
            }
            System.out.println("************************************");
        }
    }

    protected void save(OfxLatexRenderer renderer, File f) throws IOException {
        if (saveReference) {
            RelativePathFactory rpf = new RelativePathFactory(new File("src/test/resources"), RelativePathFactory.PathSeparator.CURRENT);
            logger.debug("Saving Reference to " + rpf.relativate(f));
            StringWriter actual = new StringWriter();
            renderer.write(actual);
            StringIO.writeTxt(f, actual.toString());
        }
    }

    protected void assertText(OfxLatexRenderer renderer, File fExpected) throws IOException {
        StringWriter actual = new StringWriter();
        renderer.write(actual);
        assertText(fExpected, actual.toString());
    }

    private void assertText(File fExpected, String actual) {
        String expected = StringIO.loadTxt(fExpected);
        Assert.assertEquals("Texts are different", expected, actual);
    }

    protected void assertText(File fExpected, File fActual) throws IOException {
        if (saveReference) {
            FileUtils.copyFile(fActual, fExpected);
            System.out.println(StringIO.loadTxt(fActual));
        }
        String expected = StringIO.loadTxt(fExpected);
        String actual = StringIO.loadTxt(fActual);
        Assert.assertEquals("Texts are different", expected, actual);
    }

    public void setSaveReference(boolean saveReference) {
        this.saveReference = saveReference;
    }
}
