package com.loribel.tools.sa;

import java.io.File;
import junit.framework.TestCase;
import com.loribel.commons.abstraction.GB_StringAction;
import com.loribel.commons.business.GB_BORandomTools;
import com.loribel.commons.util.CTools;
import com.loribel.commons.util.FTools;
import com.loribel.tools.GB_ToolsInitializer;
import com.loribel.tools.sa.abstraction.GB_StringActionFile;
import com.loribel.tools.sa.action.GB_SAReplace;
import com.loribel.tools.sa.bo.GB_SAReplaceBO;

/**
 * Test.
 *
 * @author Gregory Borelli
 */
public class GB_SAXmlToolsTest extends TestCase {

    public GB_SAXmlToolsTest(String a_name) {
        super(a_name);
    }

    public void setUp() {
        GB_ToolsInitializer.initAllForTest();
    }

    public void test_write_read() throws Exception {
        GB_StringAction l_sa;
        GB_SAReplaceBO l_bo = new GB_SAReplaceBO();
        l_bo.setFind("find");
        l_bo.setReplace("replace");
        l_bo.setIgnoreCase(true);
        l_bo.setRespectCase(false);
        l_sa = new GB_SAReplace(l_bo);
        test_write_read(1, l_sa);
        l_bo = (GB_SAReplaceBO) GB_BORandomTools.newBusinessObjectRandom(GB_SAReplaceBO.BO_NAME, false);
        l_sa = new GB_SAReplace(l_bo);
        test_write_read(2, l_sa);
        GB_StringAction[] l_sas = GB_SATestTools.getStringActionsTest();
        int len = CTools.getSize(l_sas);
        for (int i = 0; i < len; i++) {
            l_sa = l_sas[i];
            test_write_read(10 + i, l_sa);
        }
    }

    public void test_write_read(int a_index, GB_StringAction a_sa) throws Exception {
        File l_file = FTools.getTempFile("java-test/sa.xml");
        l_file.delete();
        assertEquals(a_index + ".1", false, l_file.exists());
        GB_SAXmlTools.writeFile(a_sa, null, l_file);
        assertEquals(a_index + ".2", true, l_file.exists());
        GB_StringActionFile l_saFile = GB_SAXmlTools.readFile(l_file);
        GB_StringAction l_sa = l_saFile.getStringAction();
        GB_SATestTools.assertEquals(a_index + ".3", a_sa, l_sa);
    }
}
