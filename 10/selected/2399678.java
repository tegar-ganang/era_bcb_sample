package org.speakright.srfsurvey.tests;

import static org.junit.Assert.assertEquals;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.Assert;
import org.junit.Test;
import org.speakright.core.*;
import org.speakright.srfsurvey.App;
import org.speakright.srfsurvey.AppFactory;
import org.speakright.srfsurvey.MGen;
import org.speakright.srfsurvey.Model;

public class TestApp {

    @Test
    public void testProps() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(MGen.dir + "srfsurvey.properties"));
        } catch (IOException e) {
        }
        String s = properties.getProperty("sampleProperty1");
        assertEquals("p1", "abc", s);
    }

    @Test
    public void testPropsInClassPath() {
        Properties properties = new Properties();
        java.net.URL url = ClassLoader.getSystemResource("org/speakright/srfsurvey/srfsurvey.properties");
        System.out.println("URL>>" + url);
        Assert.assertNotNull(url);
        InputStream in = ClassLoader.getSystemResourceAsStream("org/speakright/srfsurvey/srfsurvey.properties");
        properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String s = properties.getProperty("sampleProperty1");
        assertEquals("p1", "abc", s);
    }

    @Test
    public void testPropInAppFactory() {
        SRConfig.init(MGen.dir, "srfsurvey.properties");
        String s = SRConfig.getProperty("sampleProperty1");
        assertEquals("p1", "abc", s);
        boolean b = SRConfig.getBooleanProperty("sampleBoolean2");
        assertEquals("b", true, b);
        b = SRConfig.getBooleanProperty("sampleBoolean3");
        assertEquals("b", false, b);
    }

    @Test
    public void testPropOverrides() {
        String s = SRConfig.getProperty("prop1");
        assertEquals("p1", "", s);
        SRConfig.overrideProperty("prop1", "toronto");
        s = SRConfig.getProperty("prop1");
        assertEquals("p1", "toronto", s);
    }

    @Test
    public void test1() {
        App app = new App();
        SRInstance run = RunIt(app, 8, "yes");
        ChkTrail(run, "PromptFlow;Login;PromptFlow;Q1;Q1.1;Q1.2;Q4;disconnect");
        ChkDone(run);
    }

    @Test
    public void test2() {
        App app = new App();
        SRInstance run = RunIt(app, 6, "no");
        ChkTrail(run, "PromptFlow;Login;PromptFlow;Q1;Q4;disconnect");
        ChkDone(run);
    }

    void ChkTrail(SRInstance run, String trail) {
        assertEquals("trail", trail, run.m_trail.toString());
    }

    void ChkDone(SRInstance run) {
        assertEquals("start", true, run.isStarted());
        assertEquals("fin", true, run.isFinished());
        assertEquals("failed", false, run.isFailed());
    }

    SRLogger m_logger = SRLogger.createLogger();

    public void log(String s) {
        m_logger.log(s);
    }

    SRRunner m_runner;

    SRInstance m_run;

    public SRInstance RunIt(IFlow flow, int numFlows, String answerToQ1) {
        SRConfig.init(MGen.dir, "srfsurvey.properties");
        log("RUNNING " + flow.name() + ".........");
        AppFactory factory = new AppFactory();
        m_runner = factory.createRunner(MGen.dir, "", "", null);
        m_run = m_runner.getImpl();
        String dir = "C:\\Source\\SRFSurvey\\app\\test\\org\\speakright\\srfsurvey\\tests\\testfiles\\";
        SRConfig.overrideProperty("surveyXMLPath", dir);
        SRConfig.overrideProperty("surveyXMLFile", "test_survey.xml");
        boolean b = m_run.start(flow);
        Assert.assertTrue(b);
        for (int i = 0; i < numFlows; i++) {
            IFlow tmp = m_run.peekCurrent();
            if (tmp != null && tmp.name().equals("Q1")) {
                Proceed(answerToQ1);
            } else {
                Proceed("");
            }
        }
        log("RUNNING " + flow.name() + ".........END");
        return m_run;
    }

    public void Proceed(String input) {
        SRResults results = new SRResults(input, SRResults.ResultCode.SUCCESS);
        m_run.proceed(results);
    }

    @Test
    public void testSQLite() {
        log("trying SQLite..");
        for (int i = 0; i < 10; i++) {
            Connection c = null;
            try {
                Class.forName("SQLite.JDBCDriver");
                c = DriverManager.getConnection("jdbc:sqlite:/C:/Source/SRFSurvey/app/src/org/speakright/srfsurvey/results.db", "", "");
                c.setAutoCommit(false);
                Statement st = c.createStatement();
                int rc = st.executeUpdate("INSERT INTO tblAnswers(fQID,fQNAME) VALUES('q1','zoo')");
                st.close();
                c.commit();
                c.close();
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(1);
                try {
                    if (c != null && !c.isClosed()) {
                        c.rollback();
                        c.close();
                    }
                } catch (SQLException sql) {
                }
            }
        }
        log("end");
    }

    @Test
    public void testSQLite2() {
        log("trying SQLite..re-use connection");
        Connection c = null;
        try {
            Class.forName("SQLite.JDBCDriver");
            c = null;
            c = DriverManager.getConnection("jdbc:sqlite:/C:/Source/SRFSurvey/app/src/org/speakright/srfsurvey/results.db", "", "");
            c.setAutoCommit(false);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
        for (int i = 0; i < 100; i++) {
            try {
                Statement st = c.createStatement();
                int rc = st.executeUpdate("INSERT INTO tblAnswers(fQID,fQNAME) VALUES('q1','zoo')");
                st.close();
                c.commit();
            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
            }
        }
        try {
            c.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        log("end2");
    }
}
