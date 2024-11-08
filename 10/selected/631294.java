package com.entelience.test.test06soap;

import java.sql.PreparedStatement;
import java.util.Date;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import org.junit.*;
import static org.junit.Assert.*;
import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.encoding.XMLType;
import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import com.entelience.directory.PeopleFactory;
import com.entelience.directory.People;
import com.entelience.objects.LoginResult;
import com.entelience.objects.ExtendedAuthentication;
import com.entelience.objects.FileInformation;
import com.entelience.objects.Incident;
import com.entelience.objects.IncidentSummary;
import com.entelience.objects.IncidentDetail;
import com.entelience.objects.LostPasswordResult;
import com.entelience.objects.directory.PeopleInfoLine;
import com.entelience.objects.directory.PeopleHistory;
import com.entelience.objects.directory.PeopleLoginHistory;
import com.entelience.sql.Db;
import com.entelience.sql.DbConnection;
import com.entelience.sql.DbHelper;
import com.entelience.util.Config;
import com.entelience.util.DateHelper;
import com.entelience.util.Logs;
import com.entelience.test.SoapTestCase;

/**
 *   Soap (Axis Client) tests for the asset module
 */
public class test01Authentification extends com.entelience.test.SoapTestCase {

    private PeopleLoginHistory[] getUserLoginHistory(Integer userId, Boolean displayAll) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Directory");
        call.setOperationName("getUserLoginHistory");
        call.setOperationStyle("rpc");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        call.addParameter("displayAll", XMLType.XSD_BOOLEAN, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapDirectory", "PeopleLoginHistory");
        call.registerTypeMapping(com.entelience.objects.directory.PeopleLoginHistory.class, qn, new BeanSerializerFactory(com.entelience.objects.directory.PeopleLoginHistory.class, qn), new BeanDeserializerFactory(com.entelience.objects.directory.PeopleLoginHistory.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        PeopleLoginHistory[] plh = null;
        plh = (PeopleLoginHistory[]) call.invoke(new Object[] { userId, displayAll });
        return plh;
    }

    private PeopleHistory[] getUserHistory(Integer userId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Directory");
        call.setOperationName("getUserHistory");
        call.setOperationStyle("rpc");
        call.addParameter("userId", XMLType.XSD_INT, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapDirectory", "PeopleHistory");
        call.registerTypeMapping(com.entelience.objects.directory.PeopleHistory.class, qn, new BeanSerializerFactory(com.entelience.objects.directory.PeopleHistory.class, qn), new BeanDeserializerFactory(com.entelience.objects.directory.PeopleHistory.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        PeopleHistory[] ph = null;
        ph = (PeopleHistory[]) call.invoke(new Object[] { userId });
        return ph;
    }

    private boolean sendLogFileToSupport(String subDirectory, String fileName) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Incidents");
        call.setOperationName("sendLogFileToSupport");
        call.setOperationStyle("rpc");
        call.addParameter("subDirectory", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("fileName", XMLType.XSD_STRING, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { subDirectory, fileName });
    }

    private FileInformation[] listLogFiles(String subDirectory) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Incidents");
        call.setOperationName("listLogFiles");
        call.setOperationStyle("rpc");
        call.addParameter("subDirectory", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapIncidents", "FileInformation");
        call.registerTypeMapping(com.entelience.objects.FileInformation.class, qn, new BeanSerializerFactory(com.entelience.objects.FileInformation.class, qn), new BeanDeserializerFactory(com.entelience.objects.FileInformation.class, qn));
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (FileInformation[]) call.invoke(new Object[] { subDirectory });
    }

    private boolean changePassword(String oldPassword, String newPassword) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("changePassword");
        call.setOperationStyle("rpc");
        call.setReturnType(XMLType.XSD_BOOLEAN);
        call.addParameter("oldPassword", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("newPassword", XMLType.XSD_STRING, ParameterMode.IN);
        return (Boolean) call.invoke(new Object[] { oldPassword, newPassword });
    }

    private LostPasswordResult lostPassword(String userName) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("lostPassword");
        call.setOperationStyle("rpc");
        call.addParameter("userName", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapAuthentication", "LostPasswordResult");
        call.registerTypeMapping(com.entelience.objects.LostPasswordResult.class, qn, new BeanSerializerFactory(com.entelience.objects.LostPasswordResult.class, qn), new BeanDeserializerFactory(com.entelience.objects.LostPasswordResult.class, qn));
        call.setReturnType(qn);
        return (LostPasswordResult) call.invoke(new Object[] { userName });
    }

    /** Initialise the Session */
    @Before
    public void setupSoapSession() throws Exception {
        Logs.logMethodName();
        super.init();
        Db db = DbConnection.defaultCieDbRO();
        try {
            db.enter();
            esisId = PeopleFactory.lookupUserName(db, "esis");
            assertNotNull(esisId);
        } finally {
            db.safeClose();
        }
    }

    @Test
    public void test00_logIn() throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("extLogIn");
        call.setOperationStyle("rpc");
        String passwd = md5sum("esis");
        String login = "esis";
        call.addParameter("login", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("passwd", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapAuthentication", "ExtendedAuthentication");
        call.registerTypeMapping(com.entelience.objects.ExtendedAuthentication.class, qn, new BeanSerializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn), new BeanDeserializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn));
        call.setReturnType(qn);
        ExtendedAuthentication extAuth = null;
        extAuth = (ExtendedAuthentication) call.invoke(new Object[] { login, passwd });
        assertTrue(extAuth != null);
    }

    @Test
    public void test01_logOut() throws Exception {
        Logs.logMethodName();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("logOut");
        call.setOperationStyle("rpc");
        call.setReturnType(XMLType.XSD_BOOLEAN);
        Boolean b = (Boolean) call.invoke(new Object[] {});
        assertTrue(b.booleanValue());
    }

    @Test
    public void test02_changePasswd() throws Exception {
        changePasswd();
        rechangePasswd();
        verifyPasswdTrue();
        verifyPasswdFalse();
    }

    private void changePasswd() throws Exception {
        String oldPasswd = md5sum("esis");
        String newPasswd = md5sum("sise");
        boolean b = changePassword(oldPasswd, newPasswd);
        assertTrue(b);
    }

    private void rechangePasswd() throws Exception {
        String oldPasswd = md5sum("sise");
        String newPasswd = md5sum("esis");
        boolean b = changePassword(oldPasswd, newPasswd);
        assertTrue(b);
    }

    private void verifyPasswdTrue() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("verifyPassword");
        call.setOperationStyle("rpc");
        call.setReturnType(XMLType.XSD_BOOLEAN);
        String oldPasswdTrue = md5sum("esis");
        call.addParameter("oldPasswdTrue", XMLType.XSD_STRING, ParameterMode.IN);
        Boolean b = (Boolean) call.invoke(new Object[] { oldPasswdTrue });
        assertTrue(b.booleanValue());
    }

    private void verifyPasswdFalse() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("verifyPassword");
        call.setOperationStyle("rpc");
        call.setReturnType(XMLType.XSD_BOOLEAN);
        String oldPasswdFalse = md5sum("sise");
        call.addParameter("oldPasswdFalse", XMLType.XSD_STRING, ParameterMode.IN);
        Boolean b = (Boolean) call.invoke(new Object[] { oldPasswdFalse });
        assertFalse(b.booleanValue());
    }

    @Test
    public void test03_checkPing() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("ping");
        call.setOperationStyle("rpc");
        call.setReturnType(new QName("urn:com.entelience.soap.soapAuthentication"), Class.forName("java.lang.Boolean"));
        Boolean b = (Boolean) call.invoke(new Object[] {});
        assertTrue(b.booleanValue());
    }

    @Test
    public void test04_checkPingDb() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("pingDb");
        call.setOperationStyle("rpc");
        call.setReturnType(new QName("urn:com.entelience.soap.soapAuthentication"), Class.forName("java.lang.Boolean"));
        Boolean b = (Boolean) call.invoke(new Object[] {});
        assertTrue(b.booleanValue());
    }

    @Test
    public void test05_checkSessionValidity() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("isSessionValid");
        call.setOperationStyle("rpc");
        call.setReturnType(new QName("urn:com.entelience.soap.soapAuthentication"), Class.forName("java.lang.Boolean"));
        Boolean b = (Boolean) call.invoke(new Object[] {});
        assertTrue(b.booleanValue());
    }

    @Test
    public void test06_checkSessionInvalid() throws Exception {
        logOut();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("isSessionValid");
        call.setOperationStyle("rpc");
        call.setReturnType(new QName("urn:com.entelience.soap.soapAuthentication"), Class.forName("java.lang.Boolean"));
        Boolean b = (Boolean) call.invoke(new Object[] {});
        assertFalse(b.booleanValue());
    }

    @Test
    public void test07_bad_login() throws Exception {
        logOut();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("extLogIn");
        call.setOperationStyle("rpc");
        String passwd = md5sum("esis");
        String login = "fakeUseresis";
        call.addParameter("login", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("passwd", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapAuthentication", "ExtendedAuthentication");
        call.registerTypeMapping(com.entelience.objects.ExtendedAuthentication.class, qn, new BeanSerializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn), new BeanDeserializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn));
        call.setReturnType(qn);
        ExtendedAuthentication extAuth = null;
        try {
            extAuth = (ExtendedAuthentication) call.invoke(new Object[] { login, passwd });
        } catch (Exception e) {
        }
        assertTrue(extAuth == null);
    }

    @Test
    public void test08_bad_passwd() throws Exception {
        logOut();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("extLogIn");
        call.setOperationStyle("rpc");
        String passwd = md5sum("badd password");
        String login = "esis";
        call.addParameter("login", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("passwd", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapAuthentication", "ExtendedAuthentication");
        call.registerTypeMapping(com.entelience.objects.ExtendedAuthentication.class, qn, new BeanSerializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn), new BeanDeserializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn));
        call.setReturnType(qn);
        ExtendedAuthentication extAuth = null;
        try {
            extAuth = (ExtendedAuthentication) call.invoke(new Object[] { login, passwd });
        } catch (Exception e) {
        }
        assertTrue(extAuth == null);
    }

    @Test
    public void test09_bad_login_anonymous() throws Exception {
        logOut();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("extLogIn");
        call.setOperationStyle("rpc");
        String passwd = md5sum("");
        String login = "Anonymous";
        call.addParameter("login", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("passwd", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapAuthentication", "ExtendedAuthentication");
        call.registerTypeMapping(com.entelience.objects.ExtendedAuthentication.class, qn, new BeanSerializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn), new BeanDeserializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn));
        call.setReturnType(qn);
        ExtendedAuthentication extAuth = null;
        try {
            extAuth = (ExtendedAuthentication) call.invoke(new Object[] { login, passwd });
        } catch (Exception e) {
        }
        assertTrue(extAuth == null);
    }

    @Test
    public void test10_timeout() throws Exception {
        logOut();
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("getSessionTimeOut");
        call.setOperationStyle("rpc");
        call.setReturnType(XMLType.XSD_INT);
        Integer timeout = (Integer) call.invoke(new Object[] {});
        assertNotNull(timeout);
        assertTrue(timeout.intValue() > 0);
    }

    private Boolean notifySupport(Incident i) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Preferences");
        call.setOperationName("notifySupport");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapAuthentication", "Incident");
        call.registerTypeMapping(com.entelience.objects.Incident.class, qn, new BeanSerializerFactory(com.entelience.objects.Incident.class, qn), new BeanDeserializerFactory(com.entelience.objects.Incident.class, qn));
        call.addParameter("passwd", qn, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { i });
    }

    private IncidentSummary[] listIncidents(Boolean sent) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Incidents");
        call.setOperationName("listIncidents");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapIncidents", "IncidentSummary");
        call.registerTypeMapping(com.entelience.objects.IncidentSummary.class, qn, new BeanSerializerFactory(com.entelience.objects.IncidentSummary.class, qn), new BeanDeserializerFactory(com.entelience.objects.IncidentSummary.class, qn));
        call.addParameter("sent", XMLType.XSD_BOOLEAN, ParameterMode.IN);
        call.setReturnType(XMLType.SOAP_ARRAY);
        return (IncidentSummary[]) call.invoke(new Object[] { sent });
    }

    private IncidentDetail getIncident(int incidentId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Incidents");
        call.setOperationName("getIncident");
        call.setOperationStyle("rpc");
        QName qn = new QName("urn:com.entelience.soap.soapIncidents", "IncidentDetail");
        call.registerTypeMapping(com.entelience.objects.IncidentDetail.class, qn, new BeanSerializerFactory(com.entelience.objects.IncidentDetail.class, qn), new BeanDeserializerFactory(com.entelience.objects.IncidentDetail.class, qn));
        call.addParameter("incidentId", XMLType.XSD_INT, ParameterMode.IN);
        call.setReturnType(qn);
        return (IncidentDetail) call.invoke(new Object[] { incidentId });
    }

    private boolean sendIncident(int incidentId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Incidents");
        call.setOperationName("sendIncident");
        call.setOperationStyle("rpc");
        call.addParameter("incidentId", XMLType.XSD_INT, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { incidentId });
    }

    private boolean deleteIncident(int incidentId) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Incidents");
        call.setOperationName("deleteIncident");
        call.setOperationStyle("rpc");
        call.addParameter("incidentId", XMLType.XSD_INT, ParameterMode.IN);
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] { incidentId });
    }

    private Boolean throwException() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("throwException");
        call.setOperationStyle("rpc");
        call.setReturnType(XMLType.XSD_BOOLEAN);
        return (Boolean) call.invoke(new Object[] {});
    }

    public static String oldSmtp;

    public static boolean oldSupport;

    @Test
    public void test20_badSmtp() throws Exception {
        Db db = DbConnection.defaultCieDbRW();
        try {
            db.begin();
            oldSmtp = Config.getProperty(db, "com.entelience.mail.MailHelper.hostName", "localhost");
            oldSupport = Config.getProperty(db, "com.entelience.esis.feature.SupportNotifier", false);
            Config.setProperty(db, "com.entelience.mail.MailHelper.hostName", "127.0.10.1", 1);
            Config.setProperty(db, "com.entelience.esis.feature.SupportNotifier", "true", 1);
            PreparedStatement pst = db.prepareStatement("DELETE FROM t_client_errors");
            db.executeUpdate(pst);
            db.commit();
        } catch (Exception e) {
            db.rollback();
        } finally {
            db.safeClose();
        }
    }

    @Test
    public void test21_initIncident() throws Exception {
        IncidentSummary oldIs[] = listIncidents(null);
        assertNotNull(oldIs);
        boolean exc = false;
        try {
            throwException();
            exc = false;
        } catch (Exception e) {
            exc = true;
        }
        Incident i = new Incident();
        i.setSystemOS("dontCare");
        i.setEsisModule("test");
        i.setEsisErrorString("TestError");
        assertFalse(notifySupport(i));
        IncidentSummary newIs[] = listIncidents(null);
        assertNotNull(newIs);
        assertEquals(oldIs.length + 1, newIs.length);
        IncidentDetail det = getIncident(newIs[0].getIncidentId());
        assertFalse(det.isEmailSent());
        assertFalse(sendIncident(det.getIncidentId()));
        assertTrue(deleteIncident(det.getIncidentId()));
        assertNull(getIncident(newIs[0].getIncidentId()));
    }

    @Test
    public void test25_oldSmtp() throws Exception {
        Db db = DbConnection.defaultCieDbRW();
        try {
            db.begin();
            Config.setProperty(db, "com.entelience.mail.MailHelper.hostName", oldSmtp, 1);
            Config.setProperty(db, "com.entelience.esis.feature.SupportNotifier", String.valueOf(oldSupport), 1);
            db.commit();
        } catch (Exception e) {
            db.rollback();
        } finally {
            db.safeClose();
        }
    }

    @Test
    public void test30_passwordAging() throws Exception {
        Db db = DbConnection.defaultCieDbRW();
        try {
            db.begin();
            Config.setProperty(db, "com.entelience.esis.security.passwordAge", "5", 1);
            PreparedStatement pst = db.prepareStatement("UPDATE e_people SET last_passwd_change = '2006-07-01' WHERE user_name = ?");
            pst.setString(1, "esis");
            db.executeUpdate(pst);
            db.commit();
            p_logout();
            t30login1();
            assertTrue(isPasswordExpired());
            PeopleInfoLine me = getCurrentPeople();
            assertNotNull(me.getPasswordExpirationDate());
            assertTrue(me.getPasswordExpirationDate().before(DateHelper.now()));
            t30chgpasswd();
            assertFalse(isPasswordExpired());
            me = getCurrentPeople();
            assertNotNull(me.getPasswordExpirationDate());
            assertTrue(me.getPasswordExpirationDate().after(DateHelper.now()));
            p_logout();
            t30login2();
            assertFalse(isPasswordExpired());
            t30chgpasswd2();
            db.begin();
            Config.setProperty(db, "com.entelience.esis.security.passwordAge", "0", 1);
            db.commit();
        } catch (Exception e) {
            e.printStackTrace();
            db.rollback();
        } finally {
            db.safeClose();
        }
    }

    private void p_logout() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("logOut");
        call.setOperationStyle("rpc");
        call.setReturnType(XMLType.XSD_BOOLEAN);
        Boolean b = (Boolean) call.invoke(new Object[] {});
        assertTrue(b.booleanValue());
    }

    private void t30chgpasswd() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("changePassword");
        call.setOperationStyle("rpc");
        call.setReturnType(new QName("urn:com.entelience.soap.soapAuthentication"), Class.forName("java.lang.Boolean"));
        String oldPasswd = md5sum("esis");
        String newPasswd = md5sum("sise");
        call.addParameter("oldPasswd", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("newPasswd", XMLType.XSD_STRING, ParameterMode.IN);
        Boolean b = (Boolean) call.invoke(new Object[] { oldPasswd, newPasswd });
        assertTrue(b.booleanValue());
    }

    private void t30login1() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("extLogIn");
        call.setOperationStyle("rpc");
        String passwd = md5sum("esis");
        String login = "esis";
        call.addParameter("login", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("passwd", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapAuthentication", "ExtendedAuthentication");
        call.registerTypeMapping(com.entelience.objects.ExtendedAuthentication.class, qn, new BeanSerializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn), new BeanDeserializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn));
        call.setReturnType(qn);
        call.invoke(new Object[] { login, passwd });
    }

    private void t30login2() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("extLogIn");
        call.setOperationStyle("rpc");
        String passwd = md5sum("sise");
        String login = "esis";
        call.addParameter("login", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("passwd", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapAuthentication", "ExtendedAuthentication");
        call.registerTypeMapping(com.entelience.objects.ExtendedAuthentication.class, qn, new BeanSerializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn), new BeanDeserializerFactory(com.entelience.objects.ExtendedAuthentication.class, qn));
        call.setReturnType(qn);
        call.invoke(new Object[] { login, passwd });
    }

    private void t30chgpasswd2() throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("changePassword");
        call.setOperationStyle("rpc");
        call.setReturnType(new QName("urn:com.entelience.soap.soapAuthentication"), Class.forName("java.lang.Boolean"));
        String newPasswd = md5sum("esis");
        String oldPasswd = md5sum("sise");
        call.addParameter("oldPasswd", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("newPasswd", XMLType.XSD_STRING, ParameterMode.IN);
        Boolean b = (Boolean) call.invoke(new Object[] { oldPasswd, newPasswd });
        assertTrue(b.booleanValue());
    }

    @Test
    public void test31_blockedAccount() throws Exception {
        Logs.logMethodName();
        Db db = DbConnection.defaultCieDbRW();
        try {
            db.enter();
            db.begin();
            Config.setProperty(db, "com.entelience.esis.security.numberOfFailedLoginLimit", "3", PeopleFactory.anonymousId);
            Config.setProperty(db, "com.entelience.esis.security.failedLoginTimeLimit", "5", PeopleFactory.anonymousId);
            db.commit();
            p_logout();
            LoginResult lr = null;
            for (int i = 0; i < 2; i++) {
                lr = login("esis", "bad password");
                assertTrue(lr.isLoginFail());
                assertFalse(lr.isAccountLocked());
                assertFalse(lr.isAccountNewlyLocked());
                assertFalse(lr.isSuccess());
                assertEquals(3, lr.getNumberOfFailedLoginLimit());
                assertEquals(5, lr.getFailedLoginTimeLimit());
                assertEquals(i + 1, lr.getConsecutiveFailedLogin());
            }
            lr = login("esis", "esis");
            assertFalse(lr.isLoginFail());
            assertFalse(lr.isAccountLocked());
            assertFalse(lr.isAccountNewlyLocked());
            assertTrue(lr.isSuccess());
            assertEquals(3, lr.getNumberOfFailedLoginLimit());
            assertEquals(5, lr.getFailedLoginTimeLimit());
            assertEquals(0, lr.getConsecutiveFailedLogin());
            for (int i = 0; i < 2; i++) {
                lr = login("esis", "bad password");
                assertTrue(lr.isLoginFail());
                assertFalse(lr.isAccountLocked());
                assertFalse(lr.isAccountNewlyLocked());
                assertFalse(lr.isSuccess());
                assertEquals(3, lr.getNumberOfFailedLoginLimit());
                assertEquals(5, lr.getFailedLoginTimeLimit());
                assertEquals(i + 1, lr.getConsecutiveFailedLogin());
            }
            lr = login("esis", "bad password");
            assertTrue(lr.isLoginFail());
            assertTrue(lr.isAccountLocked());
            assertTrue(lr.isAccountNewlyLocked());
            assertFalse(lr.isSuccess());
            assertEquals(3, lr.getNumberOfFailedLoginLimit());
            assertEquals(5, lr.getFailedLoginTimeLimit());
            assertEquals(3, lr.getConsecutiveFailedLogin());
            lr = login("esis", "esis");
            assertTrue(lr.isLoginFail());
            assertTrue(lr.isAccountLocked());
            assertFalse(lr.isAccountNewlyLocked());
            assertFalse(lr.isSuccess());
            assertEquals(3, lr.getNumberOfFailedLoginLimit());
            assertEquals(5, lr.getFailedLoginTimeLimit());
            assertEquals(4, lr.getConsecutiveFailedLogin());
            db.begin();
            assertTrue("unlock", PeopleFactory.unlockAccount(db, esisId, PeopleFactory.anonymousId));
            db.commit();
            lr = login("esis", "esis");
            assertFalse(lr.isLoginFail());
            assertFalse(lr.isAccountLocked());
            assertFalse(lr.isAccountNewlyLocked());
            assertTrue(lr.isSuccess());
            assertEquals(3, lr.getNumberOfFailedLoginLimit());
            assertEquals(5, lr.getFailedLoginTimeLimit());
            assertEquals(0, lr.getConsecutiveFailedLogin());
            PeopleLoginHistory[] plh = getUserLoginHistory(esisId, false);
            assertNotNull(plh);
            assertTrue(plh.length > 0);
            db.exit();
        } finally {
            db.safeClose();
        }
    }

    @Test
    public void test32_blockedAccount() throws Exception {
        Logs.logMethodName();
        Db db = DbConnection.defaultCieDbRW();
        try {
            db.begin();
            Config.setProperty(db, "com.entelience.esis.security.numberOfFailedLoginLimit", "3", PeopleFactory.anonymousId);
            Config.setProperty(db, "com.entelience.esis.security.failedLoginTimeLimit", "5", PeopleFactory.anonymousId);
            db.commit();
            p_logout();
            LoginResult lr = null;
            for (int i = 0; i < 2; i++) {
                lr = login("esis", "bad password");
                assertTrue(lr.isLoginFail());
                assertFalse(lr.isAccountLocked());
                assertFalse(lr.isAccountNewlyLocked());
                assertFalse(lr.isSuccess());
                assertEquals(3, lr.getNumberOfFailedLoginLimit());
                assertEquals(5, lr.getFailedLoginTimeLimit());
                assertEquals(i + 1, lr.getConsecutiveFailedLogin());
            }
            lr = login("esis", "esis");
            assertFalse(lr.isLoginFail());
            assertFalse(lr.isAccountLocked());
            assertFalse(lr.isAccountNewlyLocked());
            assertTrue(lr.isSuccess());
            assertEquals(3, lr.getNumberOfFailedLoginLimit());
            assertEquals(5, lr.getFailedLoginTimeLimit());
            assertEquals(0, lr.getConsecutiveFailedLogin());
            for (int i = 0; i < 2; i++) {
                lr = login("esis", "bad password");
                assertTrue(lr.isLoginFail());
                assertFalse(lr.isAccountLocked());
                assertFalse(lr.isAccountNewlyLocked());
                assertFalse(lr.isSuccess());
                assertEquals(3, lr.getNumberOfFailedLoginLimit());
                assertEquals(5, lr.getFailedLoginTimeLimit());
                assertEquals(i + 1, lr.getConsecutiveFailedLogin());
            }
            lr = login("esis", "bad password");
            assertTrue(lr.isLoginFail());
            assertTrue(lr.isAccountLocked());
            assertTrue(lr.isAccountNewlyLocked());
            assertFalse(lr.isSuccess());
            assertEquals(3, lr.getNumberOfFailedLoginLimit());
            assertEquals(5, lr.getFailedLoginTimeLimit());
            assertEquals(3, lr.getConsecutiveFailedLogin());
            lr = login("esis", "esis");
            assertTrue(lr.isLoginFail());
            assertTrue(lr.isAccountLocked());
            assertFalse(lr.isAccountNewlyLocked());
            assertFalse(lr.isSuccess());
            assertEquals(3, lr.getNumberOfFailedLoginLimit());
            assertEquals(5, lr.getFailedLoginTimeLimit());
            assertEquals(4, lr.getConsecutiveFailedLogin());
            db.begin();
            assertTrue(PeopleFactory.unlockAccount(db, esisId, PeopleFactory.anonymousId));
            db.commit();
            PeopleHistory[] ph = getUserHistory(esisId);
            assertNotNull(ph);
            assertTrue(ph.length > 0);
            lr = login("esis", "bad password");
            assertEquals(lr.isLoginFail(), true);
            assertEquals(lr.isAccountLocked(), false);
            assertEquals(lr.isAccountNewlyLocked(), false);
            assertEquals(lr.isSuccess(), false);
            assertSame(3, lr.getNumberOfFailedLoginLimit());
            assertSame(5, lr.getFailedLoginTimeLimit());
            assertSame(1, lr.getConsecutiveFailedLogin());
            lr = login("esis", "esis");
            assertEquals(lr.isLoginFail(), false);
            assertEquals(lr.isAccountLocked(), false);
            assertEquals(lr.isAccountNewlyLocked(), false);
            assertEquals(lr.isSuccess(), true);
            assertSame(3, lr.getNumberOfFailedLoginLimit());
            assertSame(5, lr.getFailedLoginTimeLimit());
            assertSame(0, lr.getConsecutiveFailedLogin());
        } catch (Exception e) {
            e.printStackTrace();
            db.rollback();
        } finally {
            db.safeClose();
        }
    }

    @Test
    public void test33_listLogFiles() throws Exception {
        FileInformation[] infos = listLogFiles(null);
        assertNotNull(infos);
        Date previousDate = null;
        boolean dir = true;
        for (int i = 0; i < infos.length; i++) {
            if (previousDate == null && !dir) previousDate = infos[i].getLastModified();
            dir = infos[i].isDirectory();
            if (previousDate != null) assertSame(previousDate + " before " + infos[i].getLastModified(), !previousDate.before(infos[i].getLastModified()), true);
            if (infos[i].isDirectory()) {
                Date previousDate2 = null;
                FileInformation[] subdir = listLogFiles(infos[i].getName());
                assertNotNull(subdir);
                for (int j = 0; j < subdir.length; j++) {
                    if (previousDate2 == null) previousDate2 = subdir[j].getLastModified();
                    assertTrue(previousDate2 + " before " + subdir[j].getLastModified(), !previousDate2.before(subdir[j].getLastModified()));
                }
            }
        }
    }

    private final Pattern p_file_path = Pattern.compile(".*File .* has absolute path.*");

    private final Pattern p_email_disabled = Pattern.compile(".*AuthorizationException: Email sending is not activated");

    @Test
    public void test34_sendLogFileToSupport() throws Exception {
        boolean exc = false;
        try {
            sendLogFileToSupport("../../../../../../etc/", "passwd");
        } catch (AxisFault e) {
            exc = true;
            assertTrue(p_file_path.matcher(e.getFaultString()).matches());
        }
        assertTrue(exc);
        exc = false;
        try {
            sendLogFileToSupport(null, "debug.log");
        } catch (AxisFault e) {
            exc = true;
            assertTrue(e.getFaultString() + "does not match " + p_email_disabled, p_email_disabled.matcher(e.getFaultString()).matches());
        }
        assertTrue(exc);
    }

    @Test
    public void test35_lostPassword() throws Exception {
        Db db = DbConnection.defaultCieDbRW();
        try {
            db.begin();
            Config.setProperty(db, "com.entelience.esis.security.automatedLostPassword", "false", PeopleFactory.anonymousId);
            db.commit();
            LostPasswordResult lpr = lostPassword("esis");
            assertFalse(lpr.isPasswordChanged());
            assertFalse(lpr.isEmailSent());
            assertTrue(lpr.isSupportNotified());
            db.begin();
            Config.setProperty(db, "com.entelience.esis.security.automatedLostPassword", "true", PeopleFactory.anonymousId);
            Config.setProperty(db, "com.entelience.esis.feature.notifyMail", "true", PeopleFactory.anonymousId);
            db.commit();
            lpr = lostPassword("esis");
            assertTrue(lpr.isPasswordChanged());
            assertTrue(lpr.isEmailSent());
            assertFalse(lpr.isSupportNotified());
            db.begin();
            Config.setProperty(db, "com.entelience.esis.security.automatedLostPassword", "false", PeopleFactory.anonymousId);
            Config.setProperty(db, "com.entelience.esis.feature.notifyMail", "false", PeopleFactory.anonymousId);
            db.commit();
            db.begin();
            People p = new People(db, "esis", 0);
            p.setEncryptedPassword(db, md5sum("sise"));
            db.commit();
            assertTrue(changePassword(md5sum("sise"), md5sum("esis")));
        } catch (Exception e) {
            e.printStackTrace();
            db.safeRollback();
        } finally {
            db.safeClose();
        }
    }

    private LoginResult login(String user, String password) throws Exception {
        Call call = (Call) SoapTestCase.service.createCall();
        call.setTargetEndpointAddress(getEndPoint() + "Authentication");
        call.setOperationName("logIn");
        call.setOperationStyle("rpc");
        String passwd = md5sum(password);
        String login = user;
        call.addParameter("login", XMLType.XSD_STRING, ParameterMode.IN);
        call.addParameter("passwd", XMLType.XSD_STRING, ParameterMode.IN);
        QName qn = new QName("urn:com.entelience.soap.soapAuthentication", "LoginResult");
        call.registerTypeMapping(com.entelience.objects.LoginResult.class, qn, new BeanSerializerFactory(com.entelience.objects.LoginResult.class, qn), new BeanDeserializerFactory(com.entelience.objects.LoginResult.class, qn));
        call.setReturnType(qn);
        return (LoginResult) call.invoke(new Object[] { login, passwd });
    }
}
