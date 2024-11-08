package org.xaware.server.engine.instruction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IInstruction;
import org.xaware.server.engine.IInstructionParser;
import org.xaware.server.engine.IResourceManager;
import org.xaware.server.engine.IScriptNode;
import org.xaware.server.engine.context.BizCompContext;
import org.xaware.server.engine.enums.SubstitutionFailureLevel;
import org.xaware.server.engine.instruction.bizcomps.XSLBizCompInst;
import org.xaware.server.engine.instruction.bizcomps.XSLBizCompInst2;
import org.xaware.server.engine.instruction.bizcomps.XSLBizCompInst3;
import org.xaware.server.resources.XAwareBeanFactory;
import org.xaware.shared.util.FileUtils;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.testing.util.BaseTestCase;

public class TestBizCompInstructionParser extends BaseTestCase {

    private static final String dataFolder = "data/org/xaware/server/engine/integration/";

    private static final String docBizDocName = "description.xbd";

    private File bcConfBackupFile = null;

    private File bcConfFile = null;

    private File scriptConfFile = null;

    private File scriptConfBackupFile = null;

    Namespace xaNs = XAwareConstants.xaNamespace;

    Namespace preNs = XAwareConstants.preNamespace;

    Namespace postNs = XAwareConstants.postNamespace;

    IResourceManager rMgr = null;

    IBizViewContext aContext = null;

    IInstructionParser ip;

    private InstructionTestHelper instHelper;

    public TestBizCompInstructionParser(final String arg0) {
        super(arg0);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        rMgr = XAwareBeanFactory.getResourceManager();
        ip = rMgr.getInstructionParser(IResourceManager.BIZ_COMP_INST_PARSER);
        assertNotNull("Failed to establish the InstructionParser for a BizComp", ip);
        instHelper = new InstructionTestHelper(getClass(), dataFolder, rMgr);
        aContext = instHelper.getBizDocContext(docBizDocName);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testInstructionParserInit() {
        assertTrue("failed to create the expected InstructionParser", ip instanceof InstructionParser);
        final InstructionParser instParser = (InstructionParser) ip;
        System.out.println(instParser);
    }

    /**
     * Test to verify that versions on bizcomps are handled correctly
     */
    public void testBizCompInstructionVersioning() {
        try {
            this.setupBizcompVersioning();
            final String bizCompFileName1 = "ElementReferencesExample.xbc";
            final String inputXmlFileName = "ElementReferencesExampleInput.xml";
            instHelper.setInputXmlFile(dataFolder + inputXmlFileName);
            BizCompContext bcContext = instHelper.getBizCompContext(bizCompFileName1);
            Element elem = new Element("xsl");
            elem.setAttribute(XAwareConstants.BIZCOMPONENT_ATTR_TYPE, XAwareConstants.XSL_BIZCOMPONENT_TYPE, xaNs);
            IScriptNode sn = bcContext.getScriptNode(elem);
            sn.setEffectiveSubstitutionFailureLevel(SubstitutionFailureLevel.ERROR);
            sn.configure();
            assertTrue("The XSL bizcomp instruction should be available", sn.hasMoreInstructions());
            IInstruction inst = sn.getNextInstruction();
            assertNotNull("Get next instruction did not return any instruction", inst);
            assertTrue("The returned instruction was not the expected SqlBizCompInst", inst instanceof XSLBizCompInst);
            elem = new Element("xsl");
            elem.setAttribute(XAwareConstants.BIZCOMPONENT_ATTR_TYPE, XAwareConstants.XSL_BIZCOMPONENT_TYPE, xaNs);
            bcContext.getOperationState().setVersionNumber(5.4F);
            sn = bcContext.getScriptNode(elem);
            sn.setEffectiveSubstitutionFailureLevel(SubstitutionFailureLevel.ERROR);
            sn.configure();
            assertTrue("The XSL bizcomp instruction should be available", sn.hasMoreInstructions());
            inst = sn.getNextInstruction();
            assertNotNull("Get next instruction did not return any instruction", inst);
            assertTrue("The returned instruction was not the expected SqlBizCompInst3", inst instanceof XSLBizCompInst3);
            elem = new Element("xsl");
            elem.setAttribute(XAwareConstants.BIZCOMPONENT_ATTR_TYPE, XAwareConstants.XSL_BIZCOMPONENT_TYPE, xaNs);
            bcContext.getOperationState().setVersionNumber(5.0F);
            sn = bcContext.getScriptNode(elem);
            sn.setEffectiveSubstitutionFailureLevel(SubstitutionFailureLevel.ERROR);
            sn.configure();
            assertTrue("The XSL bizcomp instruction should be available", sn.hasMoreInstructions());
            inst = sn.getNextInstruction();
            assertNotNull("Get next instruction did not return any instruction", inst);
            assertTrue("The returned instruction was not the expected SqlBizCompInst2", inst instanceof XSLBizCompInst2);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (JDOMException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (XAwareException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            revertBackup(bcConfBackupFile, bcConfFile);
            bcConfFile = null;
            bcConfBackupFile = null;
            revertBackup(scriptConfBackupFile, scriptConfFile);
            scriptConfFile = null;
            scriptConfBackupFile = null;
        }
    }

    private void setupBizcompVersioning() throws Exception {
        String xaHome = System.getProperty("xaware.home");
        if (xaHome == null || xaHome.length() == 0) {
            fail("environment variable 'xaware.home' is not set");
        }
        if (!xaHome.endsWith(File.separator)) {
            xaHome += File.separator;
        }
        setupBizCompConf(xaHome);
        setupBizCompScriptConfFile(xaHome);
        XAwareBeanFactory.destroy();
        this.setUp();
    }

    /**
     * @param xaHome
     * @throws IOException
     * @throws JDOMException
     * @throws FileNotFoundException
     */
    @SuppressWarnings("unchecked")
    private void setupBizCompConf(String xaHome) throws IOException, JDOMException, FileNotFoundException {
        String bizcomp_config = xaHome + "conf/instructions/bizcomp/bizcomp_config.xml";
        bcConfFile = FileUtils.getFile(bizcomp_config);
        bcConfBackupFile = FileUtils.getFile(bizcomp_config + ".bac");
        if (!bcConfFile.exists()) {
            fail(bizcomp_config + " does not exist");
        }
        FileUtils.copyFile(bcConfFile, bcConfBackupFile);
        Document bdConfDoc = FileUtils.getDocumentFromFile(bcConfFile);
        Element iSetElement = bdConfDoc.getRootElement();
        if (iSetElement == null) {
            fail(bizcomp_config + " does not contain the root element: InstructionSet");
        }
        Element e = iSetElement.getChild("element");
        Element bcAttrElement = null;
        List attrElements = e.getChildren("attribute");
        for (Object o : attrElements) {
            if (o instanceof Element) {
                bcAttrElement = (Element) o;
                String attrName = bcAttrElement.getAttributeValue("name");
                if (attrName != null && attrName.equals(XAwareConstants.BIZCOMPONENT_ATTR_TYPE)) {
                    break;
                } else {
                    bcAttrElement = null;
                }
            }
        }
        if (bcAttrElement == null) {
            fail("Unable to find the bizcomptype configuration");
        }
        Element sqlInst = null;
        List instElements = bcAttrElement.getChildren("instruction");
        for (Object o : instElements) {
            if (o instanceof Element) {
                sqlInst = (Element) o;
                String attrName = sqlInst.getAttributeValue("value");
                if (attrName != null && attrName.equals(XAwareConstants.XSL_BIZCOMPONENT_TYPE)) {
                    break;
                } else {
                    sqlInst = null;
                }
            }
        }
        if (sqlInst == null) {
            fail("unable to find the SQL bizcomp configuration");
        }
        Element instElement2 = new Element("instruction");
        instElement2.setAttribute("value", XAwareConstants.XSL_BIZCOMPONENT_TYPE);
        instElement2.setAttribute("beanName", "XslBizCompInst2");
        instElement2.setAttribute("class", "org.xaware.server.engine.instruction.bizcomps.XSLBizCompInst2");
        instElement2.setAttribute("version", "5.2");
        Element instElement3 = (Element) instElement2.clone();
        instElement3.setAttribute("value", XAwareConstants.XSL_BIZCOMPONENT_TYPE);
        instElement3.setAttribute("beanName", "XslBizCompInst3");
        instElement3.setAttribute("class", "org.xaware.server.engine.instruction.bizcomps.XSLBizCompInst3");
        instElement3.setAttribute("version", "5.4");
        bcAttrElement.addContent(instElement2);
        bcAttrElement.addContent(instElement3);
        FileOutputStream bizDocOutStream = null;
        try {
            final XMLOutputter outputter = new XMLOutputter();
            bizDocOutStream = new FileOutputStream(bcConfFile, false);
            outputter.output(bdConfDoc, bizDocOutStream);
        } finally {
            if (bizDocOutStream != null) {
                bizDocOutStream.close();
            }
        }
    }

    private void setupBizCompScriptConfFile(String xaHome) throws IOException, JDOMException {
        String script_config = xaHome + "conf/common/spring/BizComponentConfig.xml";
        scriptConfFile = FileUtils.getFile(script_config);
        scriptConfBackupFile = FileUtils.getFile(script_config + ".bac");
        if (!scriptConfFile.exists()) {
            fail(script_config + " does not exist");
        }
        FileUtils.copyFile(scriptConfFile, scriptConfBackupFile);
        Document bdConfDoc = FileUtils.getDocumentFromFile(scriptConfFile);
        Element root = bdConfDoc.getRootElement();
        if (root == null) {
            fail(script_config + " does not contain a root element");
        }
        Element xslBean = new Element("bean", root.getNamespace());
        xslBean.setAttribute("id", "XslBizCompInst2");
        xslBean.setAttribute("class", "org.xaware.server.engine.instruction.bizcomps.XSLBizCompInst2");
        xslBean.setAttribute("scope", "prototype");
        root.addContent(xslBean);
        xslBean = new Element("bean", root.getNamespace());
        xslBean.setAttribute("id", "XslBizCompInst3");
        xslBean.setAttribute("class", "org.xaware.server.engine.instruction.bizcomps.XSLBizCompInst3");
        xslBean.setAttribute("scope", "prototype");
        root.addContent(xslBean);
        FileOutputStream outStream = null;
        try {
            final XMLOutputter outputter = new XMLOutputter();
            outStream = new FileOutputStream(scriptConfFile, false);
            outputter.output(bdConfDoc, outStream);
        } finally {
            if (outStream != null) {
                outStream.close();
            }
        }
    }

    private void revertBackup(File backupFile, File origFile) {
        if (backupFile.exists()) {
            try {
                FileUtils.copyFile(backupFile, origFile);
                backupFile.deleteOnExit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
