package com.ericdaugherty.mail;

import java.io.*;
import java.util.*;
import junit.framework.*;
import com.ericdaugherty.mail.server.utils.FileUtils;
import org.columba.ristretto.io.*;

/**
 *
 * @author mfg8876
 */
public class TestExecutor extends TestCase {

    private File accountsDir = new File(System.getProperty("basedir"), "target" + File.separator + "test-classes");

    private Source tm1, tm2, tm3, tm4, tm5;

    /**
    * The primary class responsible for executing the available tests
    *
    * @param testName
    * @throws java.io.IOException
    */
    public TestExecutor(String testName) throws IOException {
        super(testName);
        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win")) {
            for (int i = 1; i <= 7; i++) {
                File base = new File(accountsDir, "Test" + i);
                File pwdFile = new File(base, "security" + File.separator + "passwordWin");
                if (pwdFile.exists()) {
                    FileUtils.copyFile(pwdFile, new File(base, "security" + File.separator + "password"));
                    pwdFile.delete();
                }
            }
        }
    }

    public static Test suite() {
        return new TestSuite(TestExecutor.class);
    }

    /**
    * This tests JES's smtp and pop3 server implementations whereby
    * a user is authenticated using SASL Digest-MD5. It is by desing
    * restricted to handle a single smtp/pop3 session.
    *
    * @throws java.lang.Exception
    */
    public void test1() throws Exception {
        System.out.println("Starting Test1");
        getBaseCase1Sources();
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
        new JESBaseCase1("Test Case 1").execute(tasks, "Test1", "account-digest.xml", true, "localhost");
        System.runFinalization();
        System.gc();
    }

    /**
    * This tests JES's smtp and pop3 server implementations whereby
    * a user is authenticated using SASL Digest-MD5 over a SSL/TLS
    * encrypted connection. It is by desing restricted to handle a
    * single smtp/pop3 session.
    *
    * @throws java.lang.Exception
    */
    public void test2() throws Exception {
        System.out.println("Starting Test2");
        getBaseCase1Sources();
        final List<Request> tasks = new ArrayList();
        tasks.add(new Request("ernest", tm1));
        tasks.add(new Request("kara", tm2));
        tasks.add(new Request("perl", tm3));
        tasks.add(new Request("wizard", tm4));
        tasks.add(new Request("kara", tm5));
        new JESBaseCase1("Test Case 2").execute(tasks, "Test2", "account-digest-secure.xml", true, "localhost");
        System.runFinalization();
        System.gc();
    }

    /**
    * This tests JES's smtp and pop3 server implementations whereby
    * a user is authenticated using SASL Plain over a SSL/TLS
    * encrypted connection. It is by desing restricted to handle a
    * single smtp/pop3 session.
    *
    * @throws java.lang.Exception
    */
    public void test3() throws Exception {
        System.out.println("Starting Test3");
        getBaseCase1Sources();
        final List<Request> tasks = new ArrayList();
        tasks.add(new Request("ernest", tm1));
        tasks.add(new Request("kara", tm2));
        tasks.add(new Request("perl", tm3));
        tasks.add(new Request("wizard", tm4));
        tasks.add(new Request("perl", tm5));
        new JESBaseCase1("Test Case 3").execute(tasks, "Test3", "account-secure.xml", true, "localhost");
        System.runFinalization();
        System.gc();
    }

    /**
    * This tests JES's smtp server implementation whereby
    * a user is authenticated using SASL Plain. Unlike the
    * preceding tests, multiple smtp sessions are handled
    * concurrently.
    *
    * @throws java.lang.Exception
    */
    public void test4() throws Exception {
        System.out.println("Starting Test4");
        getBaseCase2Sources();
        List<Request> tasks = getJESBaseCase2Tasks();
        List<String> users = getJESBaseCase2Users(tasks);
        new JESBaseCase2("Test Case 4").execute(tasks, "Test4", "account.xml", 6, true, "localhost", false, users);
        System.runFinalization();
        System.gc();
    }

    /**
    * This tests JES's smtp client & server implementation whereby
    * a user connecting to the server is authenticated using SASL
    * Plain. In this test, multiple smtp sessions are handled
    * concurrently.
    *
    * @throws java.lang.Exception
    */
    public void test5() throws Exception {
        System.out.println("Starting Test5");
        getBaseCase2Sources();
        List<Request> tasks = getJESBaseCase2Tasks();
        List<String> users = getJESBaseCase2Users(tasks);
        new JESBaseCase2("Test Case 5").execute(tasks, "Test5", "account.xml", 6, true, "example.com", true, users);
        System.runFinalization();
        System.gc();
    }

    /**
    * This tests JES's smtp client & server implementation whereby
    * a user connecting to the server is authenticated using SASL
    * Digest-MD5. In this test, multiple smtp sessions are handled
    * concurrently.
    *
    * @throws java.lang.Exception
    */
    public void test6() throws Exception {
        System.out.println("Starting Test6");
        getBaseCase2Sources();
        final List<Request> tasks = getJESBaseCase2Tasks();
        List<String> users = getJESBaseCase2Users(tasks);
        new JESBaseCase2("Test Case 6").execute(tasks, "Test6", "account-digest.xml", 6, true, "example.com", true, users);
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
    public void test7() throws Exception {
        System.out.println("Starting Test7");
        getBaseCase2Sources();
        List<Request> tasks = getJESBaseCase2Tasks();
        List<String> users = getJESBaseCase2Users(tasks);
        new JESBaseCase2("Test Case 7").execute(tasks, "Test7", "account-secure.xml", 6, false, "example.com", true, users);
    }

    private void getBaseCase1Sources() throws IOException {
        tm1 = new FileSource(new File(accountsDir, "mails" + File.separator + "TestMessage1"));
        tm2 = new FileSource(new File(accountsDir, "mails" + File.separator + "TestMessage2"));
        tm3 = new FileSource(new File(accountsDir, "mails" + File.separator + "TestMessage3"));
        tm4 = new FileSource(new File(accountsDir, "mails" + File.separator + "TestMessage4"));
        tm5 = new FileSource(new File(accountsDir, "mails" + File.separator + "TestMessage5"));
    }

    private void getBaseCase2Sources() throws Exception {
        tm1 = getByteBufferSource(new File(accountsDir, "mails" + File.separator + "TestMessage1"));
        tm2 = getByteBufferSource(new File(accountsDir, "mails" + File.separator + "TestMessage2"));
        tm3 = getByteBufferSource(new File(accountsDir, "mails" + File.separator + "TestMessage3"));
        tm5 = getByteBufferSource(new File(accountsDir, "mails" + File.separator + "TestMessage5"));
    }

    private List getJESBaseCase2Tasks() {
        List tasks = new ArrayList();
        tasks.add(new Request("ernest", tm1));
        tasks.add(new Request("kara", tm2));
        tasks.add(new Request("perl", tm3));
        tasks.add(new Request("wizard", tm5));
        tasks.add(new Request("ernest", tm1));
        tasks.add(new Request("kara", tm2));
        tasks.add(new Request("perl", tm3));
        tasks.add(new Request("wizard", tm5));
        tasks.add(new Request("ernest", tm1));
        tasks.add(new Request("kara", tm2));
        tasks.add(new Request("perl", tm3));
        tasks.add(new Request("wizard", tm5));
        tasks.add(new Request("ernest", tm1));
        tasks.add(new Request("kara", tm2));
        tasks.add(new Request("perl", tm3));
        tasks.add(new Request("wizard", tm5));
        tasks.add(new Request("ernest", tm1));
        tasks.add(new Request("kara", tm2));
        tasks.add(new Request("perl", tm3));
        tasks.add(new Request("wizard", tm5));
        tasks.add(new Request("ernest", tm1));
        tasks.add(new Request("kara", tm2));
        tasks.add(new Request("perl", tm3));
        tasks.add(new Request("wizard", tm5));
        return tasks;
    }

    private List getJESBaseCase2Users(List tasks) {
        List users = new ArrayList();
        Iterator<Request> iter = tasks.iterator();
        String aUser;
        while (iter.hasNext()) {
            aUser = iter.next().getUsername();
            if (!users.contains(aUser)) users.add(aUser);
        }
        return users;
    }

    /**
    * Constructs a ByteBufferSource rather than a FileSource
    * from a file.
    *
    * @param file
    * @return a ByteBufferSource
    * @throws java.lang.Exception
    */
    private Source getByteBufferSource(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        try {
            byte[] result = new byte[15360];
            byte[] temp;
            int count = 0;
            int nextByte;
            for (; ; ) {
                nextByte = fis.read();
                if (nextByte == -1) break;
                if (count == result.length) {
                    temp = new byte[result.length * 2];
                    System.arraycopy(result, 0, temp, 0, result.length);
                    result = temp;
                }
                result[count++] = (byte) (nextByte & 0xff);
            }
            temp = new byte[count];
            System.arraycopy(result, 0, temp, 0, count);
            return new ByteBufferSource(temp);
        } finally {
            try {
                fis.close();
            } catch (IOException ioe) {
            }
        }
    }
}
