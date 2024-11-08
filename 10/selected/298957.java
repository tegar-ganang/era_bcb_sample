package com.entelience.test.test00admin;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.apache.axis.client.Call;
import org.apache.axis.encoding.XMLType;
import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import com.entelience.esis.ModuleManager;
import com.entelience.module.Module;
import com.entelience.module.ModuleHelper;
import com.entelience.objects.module.ModuleList;
import com.entelience.objects.DropDown;
import com.entelience.objects.PageCounter;
import com.entelience.objects.FileInformation;
import com.entelience.objects.directory.PeopleInfoLine;
import com.entelience.objects.module.ModuleDetail;
import com.entelience.objects.module.ModuleInfoLine;
import com.entelience.objects.module.ModuleReport;
import com.entelience.objects.module.Preference;
import com.entelience.objects.module.PreferenceHistory;
import com.entelience.objects.raci.RACI;
import com.entelience.probe.SupportedProductsXMLParser;
import com.entelience.raci.DbRaci;
import com.entelience.sql.Db;
import com.entelience.sql.DbConnection;
import com.entelience.test.SoapTestCase;
import com.entelience.util.StaticConfig;
import com.entelience.util.Logs;
import com.entelience.directory.PeopleFactory;

/**
  *   Soap (Axis Client) tests for the module management
  */
public class test08SoapModules extends SoapTestCase {

    private static Integer esisId = null;

    private static Integer guestId = null;

    private static Integer extenId = null;

    private ModuleDetail[] listAllModules() throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("listAllModules");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapModule", "ModuleDetail");
        call.registerTypeMapping(com.entelience.objects.module.ModuleDetail.class, qn, new BeanSerializerFactory(com.entelience.objects.module.ModuleDetail.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.ModuleDetail.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (ModuleDetail[]) call.invoke(new Object[] {});
    }

    private ModuleDetail[] listRegisteredModules() throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("listRegisteredModules");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapModule", "ModuleDetail");
        call.registerTypeMapping(com.entelience.objects.module.ModuleDetail.class, qn, new BeanSerializerFactory(com.entelience.objects.module.ModuleDetail.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.ModuleDetail.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (ModuleDetail[]) call.invoke(new Object[] {});
    }

    private ModuleInfoLine[] getActiveModulesInformations(Integer userId) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("getActiveModulesInformations");
        call.setOperationStyle("rpc");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "ModuleInfoLine");
        call.registerTypeMapping(com.entelience.objects.module.ModuleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.module.ModuleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.ModuleInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (ModuleInfoLine[]) call.invoke(new Object[] { userId });
    }

    private ModuleInfoLine[] getRegisteredModulesInformations(Integer userId) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("getRegisteredModulesInformations");
        call.setOperationStyle("rpc");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "ModuleInfoLine");
        call.registerTypeMapping(com.entelience.objects.module.ModuleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.module.ModuleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.ModuleInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (ModuleInfoLine[]) call.invoke(new Object[] { userId });
    }

    private ModuleInfoLine[] getAllModulesInformations(Integer userId) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("getAllModulesInformations");
        call.setOperationStyle("rpc");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "ModuleInfoLine");
        call.registerTypeMapping(com.entelience.objects.module.ModuleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.module.ModuleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.ModuleInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (ModuleInfoLine[]) call.invoke(new Object[] { userId });
    }

    private ModuleInfoLine getModuleInformation(Integer moduleId) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("getModuleInformation");
        call.setOperationStyle("rpc");
        call.addParameter("moduleId", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "ModuleInfoLine");
        call.registerTypeMapping(com.entelience.objects.module.ModuleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.module.ModuleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.ModuleInfoLine.class, qn));
        call.setReturnType(qn);
        return (ModuleInfoLine) call.invoke(new Object[] { moduleId });
    }

    private DropDown[] getListOfStatus() throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "VulnerabilityReview");
        call.setOperationName("getListOfStatus");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapVulnerabilityReview", "DropDown");
        call.registerTypeMapping(com.entelience.objects.DropDown.class, qn, new BeanSerializerFactory(com.entelience.objects.DropDown.class, qn), new BeanDeserializerFactory(com.entelience.objects.DropDown.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        DropDown[] dp = null;
        dp = (DropDown[]) call.invoke(new Object[] {});
        return dp;
    }

    private PeopleInfoLine[] getListPeople(Boolean showDisabled) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Directory");
        call.setOperationName("getListPeople");
        call.setOperationStyle("rpc");
        call.addParameter("showDisabled", XMLType.XSD_BOOLEAN, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapDirectory", "PeopleInfoLine");
        call.registerTypeMapping(com.entelience.objects.directory.PeopleInfoLine.class, qn, new BeanSerializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn), new BeanDeserializerFactory(com.entelience.objects.directory.PeopleInfoLine.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        PeopleInfoLine[] pil = null;
        pil = (PeopleInfoLine[]) call.invoke(new Object[] { showDisabled });
        return pil;
    }

    private Preference[] listModulePreferences(Integer moduleId) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("listModulePreferences");
        call.setOperationStyle("rpc");
        call.addParameter("moduleId", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "Preference");
        call.registerTypeMapping(com.entelience.objects.module.Preference.class, qn, new BeanSerializerFactory(com.entelience.objects.module.Preference.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.Preference.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        Preference[] prefs = null;
        prefs = (Preference[]) call.invoke(new Object[] { moduleId });
        return prefs;
    }

    private PreferenceHistory[] getPreferenceHistory(String prefName) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("getPreferenceHistory");
        call.setOperationStyle("rpc");
        call.addParameter("moduleId", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "PreferenceHistory");
        call.registerTypeMapping(com.entelience.objects.module.PreferenceHistory.class, qn, new BeanSerializerFactory(com.entelience.objects.module.PreferenceHistory.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.PreferenceHistory.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        PreferenceHistory[] prefs = null;
        prefs = (PreferenceHistory[]) call.invoke(new Object[] { prefName });
        return prefs;
    }

    private Preference[] listGlobalPreferences() throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("listGlobalPreferences");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapModule", "Preference");
        call.registerTypeMapping(com.entelience.objects.module.Preference.class, qn, new BeanSerializerFactory(com.entelience.objects.module.Preference.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.Preference.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        Preference[] prefs = null;
        prefs = (Preference[]) call.invoke(new Object[] {});
        return prefs;
    }

    protected ModuleReport[] listAllActiveModulesReports(Integer page, String order, String way) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("listAllActiveModulesReports");
        call.setOperationStyle("rpc");
        call.addParameter("page", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("order", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("way", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "ModuleReport");
        call.registerTypeMapping(com.entelience.objects.module.ModuleReport.class, qn, new BeanSerializerFactory(com.entelience.objects.module.ModuleReport.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.ModuleReport.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        ModuleReport[] reps = null;
        reps = (ModuleReport[]) call.invoke(new Object[] { page, order, way });
        return reps;
    }

    protected PageCounter countAllActiveModulesReports() throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("countAllActiveModulesReports");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapModule", "PageCounter");
        call.registerTypeMapping(com.entelience.objects.PageCounter.class, qn, new BeanSerializerFactory(com.entelience.objects.PageCounter.class, qn), new BeanDeserializerFactory(com.entelience.objects.PageCounter.class, qn));
        call.setReturnType(qn);
        PageCounter pc = (PageCounter) call.invoke(new Object[] {});
        return pc;
    }

    protected ModuleReport[] listModuleReports(Integer moduleId, Integer page, String order, String way) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("listModuleReports");
        call.setOperationStyle("rpc");
        call.addParameter("moduleId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("page", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("order", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("way", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "ModuleReport");
        call.registerTypeMapping(com.entelience.objects.module.ModuleReport.class, qn, new BeanSerializerFactory(com.entelience.objects.module.ModuleReport.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.ModuleReport.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        ModuleReport[] reps = null;
        reps = (ModuleReport[]) call.invoke(new Object[] { moduleId, page, order, way });
        return reps;
    }

    protected PageCounter countModuleReports(Integer moduleId) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("countModuleReports");
        call.setOperationStyle("rpc");
        call.addParameter("moduleId", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "PageCounter");
        call.registerTypeMapping(com.entelience.objects.PageCounter.class, qn, new BeanSerializerFactory(com.entelience.objects.PageCounter.class, qn), new BeanDeserializerFactory(com.entelience.objects.PageCounter.class, qn));
        call.setReturnType(qn);
        PageCounter pc = (PageCounter) call.invoke(new Object[] { moduleId });
        return pc;
    }

    private Boolean setPreference(Integer moduleId, Preference pref) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("setPreference");
        call.setOperationStyle("rpc");
        call.addParameter("moduleId", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "Preference");
        call.registerTypeMapping(com.entelience.objects.module.Preference.class, qn, new BeanSerializerFactory(com.entelience.objects.module.Preference.class, qn), new BeanDeserializerFactory(com.entelience.objects.module.Preference.class, qn));
        call.addParameter("pref", qn, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { moduleId, pref });
    }

    private String getPreference(String parameter) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Preferences");
        call.setOperationName("getPreference");
        call.setOperationStyle("rpc");
        call.addParameter("parameter", XMLType.XSD_STRING, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_STRING);
        return (String) call.invoke(new Object[] { parameter });
    }

    private FileInformation[] listReportFiles(String subDirectory, String matchingFilter) throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Module");
        call.setOperationName("listReportFiles");
        call.setOperationStyle("rpc");
        call.addParameter("subDirectory", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("matchingFilter", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapModule", "FileInformation");
        call.registerTypeMapping(com.entelience.objects.FileInformation.class, qn, new BeanSerializerFactory(com.entelience.objects.FileInformation.class, qn), new BeanDeserializerFactory(com.entelience.objects.FileInformation.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        FileInformation[] files = null;
        files = (FileInformation[]) call.invoke(new Object[] { subDirectory, matchingFilter });
        return files;
    }

    @Test
    public void test00_reinitData() throws Exception {
        Logs.logMethodName();
        init();
        Db db = DbConnection.defaultCieDbRW();
        try {
            db.begin();
            PreparedStatement pst = db.prepareStatement("TRUNCATE e_module;");
            pst.executeUpdate();
            pst = db.prepareStatement("TRUNCATE e_application_version;");
            pst.executeUpdate();
            ModuleHelper.synchronizeDbWithModuleList(db);
            ModuleHelper.declareNewVersion(db);
            ModuleHelper.updateModuleVersions(db);
            esisId = com.entelience.directory.PeopleFactory.lookupUserName(db, "esis");
            assertNotNull(esisId);
            guestId = com.entelience.directory.PeopleFactory.lookupUserName(db, "guest");
            assertNotNull(guestId);
            extenId = com.entelience.directory.PeopleFactory.lookupUserName(db, "exten");
            assertNotNull(extenId);
            db.commit();
        } catch (Exception e) {
            db.rollback();
            throw e;
        }
    }

    @Test
    public void test01a_listAllModules() throws Exception {
        Logs.logMethodName();
        init();
        ModuleDetail[] md = listAllModules();
        assertNotNull(md);
        assertEquals(md.length, 8);
    }

    @Test
    public void test01b_getAllModulesInformations() throws Exception {
        Logs.logMethodName();
        init();
        ModuleInfoLine[] mi = getAllModulesInformations(null);
        assertNotNull(mi);
        assertEquals(mi.length, 8);
    }

    @Test
    public void test02a_listRegisteredModules() throws Exception {
        Logs.logMethodName();
        init();
        ModuleDetail[] md = listRegisteredModules();
        assertNotNull(md);
    }

    @Test
    public void test02b_getRegisteredModulesInformations() throws Exception {
        Logs.logMethodName();
        init();
        ModuleInfoLine[] mi = getRegisteredModulesInformations(null);
        assertNotNull(mi);
    }

    @Test
    public void test03a_listActiveModules() throws Exception {
        Logs.logMethodName();
        init();
        ModuleDetail[] md = listActiveModules();
        assertNotNull(md);
        assertEquals(md.length, 0);
    }

    @Test
    public void test03b_getActiveModulesInformations() throws Exception {
        Logs.logMethodName();
        init();
        ModuleInfoLine[] mi = getActiveModulesInformations(null);
        assertNotNull(mi);
        assertEquals(mi.length, 0);
    }

    @Test(expected = Exception.class)
    public void test04_tryANonValidModule() throws Exception {
        Logs.logMethodName();
        init();
        getListOfStatus();
    }

    @Test(expected = Exception.class)
    public void test06_toBeSure() throws Exception {
        Logs.logMethodName();
        init();
        getListOfStatus();
    }

    @Test
    public void test07_registerModule() throws Exception {
        Logs.logMethodName();
        init();
        Db db = DbConnection.defaultCieDbRW();
        db.begin();
        String[] modules = ModuleList.getModules();
        for (int i = 0; i < modules.length; i++) {
            Module m = ModuleHelper.getModule(modules[i]);
            m.register(db);
        }
        ModuleHelper.updateModuleVersions(db);
        db.commit();
    }

    @Test(expected = Exception.class)
    public void test08_tryAValidModule() throws Exception {
        Logs.logMethodName();
        getListOfStatus();
    }

    @Test
    public void test09_listRegisteredModules() throws Exception {
        Logs.logMethodName();
        init();
        ModuleDetail[] md = listRegisteredModules();
        assertNotNull(md);
        assertEquals(md.length, 8);
    }

    @Test
    public void test09b_getRegisteredModulesInformations() throws Exception {
        Logs.logMethodName();
        init();
        ModuleInfoLine[] mi = getRegisteredModulesInformations(null);
        assertNotNull(mi);
        assertEquals(mi.length, 8);
    }

    @Test
    public void test10_activateModules() throws Exception {
        Logs.logMethodName();
        init();
        Db db = DbConnection.defaultCieDbRW();
        db.begin();
        ModuleManager.activateAllModules(db);
        db.commit();
    }

    @Test
    public void test11_listActiveModules() throws Exception {
        Logs.logMethodName();
        init();
        ModuleDetail[] md = listActiveModules();
        assertNotNull(md);
        assertEquals(md.length, 8);
        for (int i = 0; i < md.length; i++) {
            if ("Admin".equals(md[i].getName())) assertEquals(md[i].getName(), md[i].isShownInSnail(), true); else assertEquals(md[i].getName(), md[i].isShownInSnail(), false);
        }
    }

    @Test
    public void test12_getActiveModulesInformations() throws Exception {
        Logs.logMethodName();
        init();
        ModuleInfoLine[] mi = getActiveModulesInformations(null);
        assertNotNull(mi);
        assertEquals(mi.length, 8);
        for (int i = 0; i < mi.length; i++) {
            if ("Admin".equals(mi[i].getName())) assertFalse(mi[i].getName(), mi[i].isUseRacis()); else if ("Portal".equals(mi[i].getName())) assertFalse(mi[i].getName(), mi[i].isUseRacis()); else assertTrue(mi[i].getName(), mi[i].isUseRacis());
        }
    }

    @Test
    public void test13_getModuleInformation() throws Exception {
        Logs.logMethodName();
        init();
        ModuleInfoLine[] mi = getActiveModulesInformations(null);
        assertNotNull(mi);
        assertTrue(mi.length > 0);
        ModuleInfoLine mil = getModuleInformation(new Integer(mi[0].getModule_id()));
        assertNotNull(mil);
        assertEquals(mil.getModule_id(), mi[0].getModule_id());
    }

    @Test
    public void test14_listActiveModules() throws Exception {
        Logs.logMethodName();
        init("guest", "esis");
        ModuleDetail[] md = listActiveModules();
        assertNotNull(md);
        assertEquals(md.length, 8);
        for (ModuleDetail m : md) {
            assertTrue(m.getName(), m.isActive());
        }
    }

    @Test
    public void test20_initRacis() throws Exception {
        Logs.logMethodName();
        init("guest", "esis");
        Db db = DbConnection.defaultCieDbRW();
        try {
            db.begin();
            PreparedStatement pst = db.prepareStatement("SELECT e_raci_obj, class_name FROM e_module");
            ResultSet rs = db.executeQuery(pst);
            RACI esis = new RACI();
            esis.setA(true);
            esis.setUserId(esisId.intValue());
            RACI guest = new RACI();
            guest.setC(true);
            guest.setUserId(guestId.intValue());
            RACI exten = new RACI();
            exten.setI(true);
            exten.setUserId(extenId.intValue());
            while (rs.next()) {
                esis.setRaciObjectId(rs.getInt(1));
                guest.setRaciObjectId(rs.getInt(1));
                exten.setRaciObjectId(rs.getInt(1));
                String name = rs.getString(2);
                if ("com.entelience.module.Admin".equals(name)) continue;
                DbRaci.addRaci(db, esis, PeopleFactory.anonymousId);
                DbRaci.addRaci(db, guest, PeopleFactory.anonymousId);
                if (!"com.entelience.module.Assets".equals(name)) DbRaci.addRaci(db, exten, PeopleFactory.anonymousId);
            }
            db.commit();
        } finally {
            db.exit();
        }
    }

    @Test
    public void test21_listActiveModules() throws Exception {
        Logs.logMethodName();
        init("guest", "esis");
        ModuleDetail[] md = listActiveModules();
        assertNotNull(md);
        assertEquals(md.length, ModuleList.size());
        for (ModuleDetail m : md) {
            if ("Admin".equals(m.getName()) || "Portal".equals(m.getName())) assertFalse("not in snail :" + m.getName() + ":" + m.isShownInSnail(), m.isShownInSnail()); else assertTrue("in snail :" + m.getName(), m.isShownInSnail());
        }
    }

    @Test
    public void test22_listActiveModules() throws Exception {
        Logs.logMethodName();
        init("exten", "esis");
        ModuleDetail[] md = listActiveModules();
        assertNotNull(md);
        assertEquals(md.length, ModuleList.size());
        for (int i = 0; i < md.length; i++) {
            if ("Admin".equals(md[i].getName()) || "Assets".equals(md[i].getName()) || "Portal".equals(md[i].getName())) assertFalse(md[i].getName(), md[i].isShownInSnail()); else assertTrue(md[i].getName(), md[i].isShownInSnail());
        }
    }

    @Test
    public void test23_listActiveModules() throws Exception {
        Logs.logMethodName();
        init();
        ModuleDetail[] md = listActiveModules();
        assertNotNull(md);
        assertEquals(md.length, ModuleList.size());
        for (int i = 0; i < md.length; i++) {
            if ("Portal".equals(md[i].getName())) assertFalse(md[i].getName(), md[i].isShownInSnail()); else assertTrue(md[i].getName(), md[i].isShownInSnail());
        }
    }

    @Test
    public void test24_getActiveModulesInformations_a() throws Exception {
        Logs.logMethodName();
        init();
        ModuleInfoLine[] mi = getActiveModulesInformations(null);
        assertNotNull(mi);
        assertEquals(mi.length, ModuleList.size());
    }

    @Test
    public void test25_getActiveModulesInformations() throws Exception {
        Logs.logMethodName();
        init();
        ModuleInfoLine[] mi = getActiveModulesInformations(guestId);
        assertNotNull(mi);
        assertEquals(mi.length, ModuleList.size() - 1);
    }

    @Test
    public void test26_getActiveModulesInformations() throws Exception {
        Logs.logMethodName();
        init();
        ModuleInfoLine[] mi = getActiveModulesInformations(extenId);
        assertNotNull(mi);
        assertEquals(mi.length, ModuleList.size() - 2);
    }

    @Test
    public void test27_register_products_supported_by_probes() throws Exception {
        Logs.logMethodName();
        init();
        Db db = DbConnection.npMainDbRW();
        try {
            db.begin();
            SupportedProductsXMLParser parser = new SupportedProductsXMLParser();
            parser.parse("data/supported-products.xml", db);
            db.commit();
        } catch (Exception e) {
            db.rollback();
        }
    }
}
