package com.ericdaugherty.mail;

import java.io.*;
import java.util.*;
import junit.framework.*;
import com.ericdaugherty.mail.server.utils.FileUtils;

/**
 *
 * @author Andreas Kyrmegalos
 */
public class TestExecutorDb extends TestCase {

    private File mavenTargetTestClassesDir = new File(System.getProperty("basedir"), "target" + File.separator + "test-classes");

    private File tm1, tm2, tm3, tm4, tm5;

    private Properties rcptInfo = new Properties();

    private PasswordAuthenticator senderCredentials;

    /**
    * The primary class responsible for executing the available tests
    *
    * @param testName
    * @throws java.io.IOException
    */
    @SuppressWarnings("CallToThreadDumpStack")
    public TestExecutorDb(String testName) throws IOException {
        super(testName);
        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win")) {
            for (int i = 1; i <= 9; i++) {
                File base = new File(mavenTargetTestClassesDir, "Test" + i);
                File pwdFile = new File(base, "security" + File.separator + "passwordWin");
                if (pwdFile.exists()) {
                    FileUtils.copyFile(pwdFile, new File(base, "security" + File.separator + "password"));
                    pwdFile.delete();
                }
            }
        }
        getBaseCase1Sources();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(mavenTargetTestClassesDir, "users"));
            rcptInfo.load(fis);
            String sender = null;
            Locale locale = Locale.ENGLISH;
            Iterator iter = rcptInfo.keySet().iterator();
            while (iter.hasNext()) {
                sender = (String) iter.next();
                if (sender.toLowerCase(locale).startsWith("sender")) {
                    break;
                }
                sender = null;
            }
            if (sender == null) {
                throw new Exception("You have to specify the sender by including an entry in the users file that starts with \"sender.\"");
            }
            senderCredentials = new PasswordAuthenticator(sender.substring(7), rcptInfo.getProperty(sender));
            rcptInfo.remove(sender);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(999);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    public static Test suite() {
        return new TestSuite(TestExecutorDb.class);
    }

    /**
    * This tests JES's smtp and pop3 server implementations whereby
    * a user is authenticated using SASL PLAIN. It is by design
    * restricted to handle a single smtp/pop3 session.
    *
    * @throws java.lang.Exception
    */
    public void test8() throws Exception {
        System.out.println("Starting Test8");
        final List<Request> tasks = new ArrayList();
        tasks.add(new Request("ernest", tm1));
        tasks.add(new Request("kara", tm2));
        tasks.add(new Request("perl", tm3));
        tasks.add(new Request("wizard", tm4));
        tasks.add(new Request("ernest", tm5));
        tasks.add(new Request("kara", tm1));
        tasks.add(new Request("perl", tm2));
        tasks.add(new Request("wizard", tm3));
        tasks.add(new Request("ernest", tm4));
        tasks.add(new Request("kara", tm5));
        Properties configurationProperties = new Properties();
        configurationProperties.put("SASL", "PLAIN");
        PasswordAuthenticator senderCredentials = new PasswordAuthenticator(this.senderCredentials.getPasswordAuthentication().getUserName() + "@localhost", this.senderCredentials.getPasswordAuthentication().getPassword());
        senderCredentials.setEmailAddress(this.senderCredentials.getPasswordAuthentication().getUserName() + "@localhost");
        new JESBaseCase3("Test Case 8").execute(tasks, "Test8", "localhost", rcptInfo, configurationProperties, senderCredentials);
        System.runFinalization();
        System.gc();
    }

    /**
    * This tests JES's smtp client & server implementation whereby
    * a user connecting to the server is authenticated using SASL
    * Plain over a secure SSL/TLS connection. In this test,
    * multiple smtp sessions are handled concurrently.
    *
    * @throws java.lang.Exception
    */
    public void test9() throws Exception {
        System.out.println("Starting Test9");
        getBaseCase2Sources();
        getBaseCase2Sources();
        List<Request> tasks = getJESBaseCase2Tasks();
        Properties configurationProperties = new Properties();
        configurationProperties.put("SASL", "PLAIN");
        configurationProperties.put("STARTTLS", "true");
        configurationProperties.put("PROTOCOL", "TLSv1");
        configurationProperties.put("CIPHERS", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA TLS_DHE_DSS_WITH_AES_128_CBC_SHA TLS_RSA_WITH_AES_128_CBC_SHA");
        PasswordAuthenticator senderCredentials = new PasswordAuthenticator(this.senderCredentials.getPasswordAuthentication().getUserName() + "@localhost", this.senderCredentials.getPasswordAuthentication().getPassword());
        senderCredentials.setEmailAddress(this.senderCredentials.getPasswordAuthentication().getUserName() + "@localhost");
        new JESBaseCase4("Test Case 9").execute(tasks, "Test9", "localhost", rcptInfo, configurationProperties, senderCredentials, 6, false);
        System.runFinalization();
        System.gc();
    }

    private void getBaseCase1Sources() throws IOException {
        tm1 = new File(mavenTargetTestClassesDir, "mails" + File.separator + "TestMessage1");
        tm2 = new File(mavenTargetTestClassesDir, "mails" + File.separator + "TestMessage2");
        tm3 = new File(mavenTargetTestClassesDir, "mails" + File.separator + "TestMessage3");
        tm4 = new File(mavenTargetTestClassesDir, "mails" + File.separator + "TestMessage4");
        tm5 = new File(mavenTargetTestClassesDir, "mails" + File.separator + "TestMessage5");
    }

    private void getBaseCase2Sources() throws Exception {
        tm1 = new File(mavenTargetTestClassesDir, "mails" + File.separator + "TestMessage1");
        tm2 = new File(mavenTargetTestClassesDir, "mails" + File.separator + "TestMessage2");
        tm3 = new File(mavenTargetTestClassesDir, "mails" + File.separator + "TestMessage3");
        tm5 = new File(mavenTargetTestClassesDir, "mails" + File.separator + "TestMessage5");
    }

    private List getJESBaseCase2Tasks() {
        List<Request> tasks = new ArrayList();
        tasks.add(new Request("ernest", tm1));
        tasks.add(new Request("kara", tm2));
        tasks.add(new Request("perl", tm3));
        tasks.add(new Request("wizard", tm5));
        tasks.add(new Request("ernest", tm2));
        tasks.add(new Request("kara", tm3));
        tasks.add(new Request("perl", tm5));
        tasks.add(new Request("wizard", tm1));
        tasks.add(new Request("ernest", tm3));
        tasks.add(new Request("kara", tm5));
        tasks.add(new Request("perl", tm1));
        tasks.add(new Request("wizard", tm2));
        tasks.add(new Request("ernest", tm5));
        tasks.add(new Request("kara", tm1));
        tasks.add(new Request("perl", tm2));
        tasks.add(new Request("wizard", tm3));
        tasks.add(new Request("ernest", tm1));
        tasks.add(new Request("kara", tm2));
        tasks.add(new Request("perl", tm3));
        tasks.add(new Request("wizard", tm5));
        tasks.add(new Request("ernest", tm2));
        tasks.add(new Request("kara", tm3));
        tasks.add(new Request("perl", tm5));
        tasks.add(new Request("wizard", tm1));
        return tasks;
    }
}
