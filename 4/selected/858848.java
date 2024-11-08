package org.jaffa.tools.loadtest;

import org.apache.log4j.Logger;
import org.jaffa.tools.loadtest.domain.*;
import org.jaffa.datatypes.DateTime;
import java.util.*;
import java.lang.reflect.*;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import org.xml.sax.SAXException;

/**
 *
 * @author  MaheshD
 */
public class RunTestSet extends Thread {

    /** Set up Logging for Log4J */
    private static Logger log = Logger.getLogger(RunTestSet.class);

    private TestSet m_testSet = null;

    private int threadNumber;

    private TestResultLogger writer;

    private WebConversation conversation = new WebConversation();

    private WebResponse loggedOn = null;

    /** Creates a new instance of RunTest and performs Logon
     * @param ts TestSet object
     * @param threadCount Thread Number
     * @param resultLog TestResultLogger object to write the result to the file
     */
    public RunTestSet(TestSet ts, int threadCount, TestResultLogger resultLog) {
        m_testSet = ts;
        writer = resultLog;
        threadNumber = threadCount;
        if (m_testSet == null) {
            throw new RuntimeException("Can't initialize TestSet !");
        }
        loggedOn = performLogon(ts.getLogOn(), this.conversation);
        if (loggedOn == null) {
            throw new RuntimeException("Failed To Log On");
        }
    }

    /** Executes the TestSet based on the
     * parameteres set like no of Iterations, TestCase
     * If the selectionis random , it picks up a random number and calls the unti tests
     *or else it runs the unti tests sequentially.
     */
    public void run() {
        String random = "random";
        int batchSize = m_testSet.getBatchSize().intValue();
        int delay = m_testSet.getRunDelay().intValue();
        String selection = m_testSet.getSelection().value();
        List unitTests = m_testSet.getUnitTests().getUnitTest();
        int count = 1;
        if (random.equalsIgnoreCase(selection)) {
            Random myRandom = new Random();
            while (batchSize != 0) {
                int randomInt = myRandom.nextInt(unitTests.size());
                this.runUnitTest((UnitTest) unitTests.get(randomInt), count++);
                batchSize--;
            }
        } else {
            while (batchSize != 0) {
                for (int i = 0; i < unitTests.size(); batchSize--, i++) {
                    if (batchSize == 0) {
                        break;
                    }
                    this.runUnitTest((UnitTest) unitTests.get(i), count++);
                }
            }
        }
        this.performLogout(conversation, m_testSet.getLogOn().getWebappRoot());
    }

    /** Runs the UntiTest invoking the unit test specified in the xml file
     * @param unittest unittest to be run
     * @param count  iteration number
     */
    private void runUnitTest(UnitTest unittest, int count) {
        String className = new String();
        String methodName = new String();
        try {
            className = unittest.getClassName();
            Class clazz = Class.forName(className);
            Class tclass = Class.forName("junit.framework.TestCase");
            Class iclass = Class.forName("org.jaffa.tools.loadtest.ILoadTestCase");
            String param = className + ".class";
            Object obj = clazz.getConstructor(new Class[] { String.class }).newInstance(new Object[] { param });
            methodName = unittest.getMethodName();
            Method method = clazz.getDeclaredMethod(methodName, (Class[]) null);
            if (iclass.isInstance(obj)) {
                String setWC = "setWebConversation";
                String setResp = "setWebResponse";
                Method setWcMethod = clazz.getMethod(setWC, new Class[] { WebConversation.class });
                Method setRespMethod = clazz.getMethod(setResp, new Class[] { WebResponse.class });
                setWcMethod.invoke(obj, new Object[] { this.conversation });
                setRespMethod.invoke(obj, new Object[] { this.loggedOn });
            }
            long startTime = System.currentTimeMillis();
            DateTime startdt = new DateTime(startTime);
            method.invoke(obj, (Object[]) null);
            long endTime = System.currentTimeMillis();
            DateTime enddt = new DateTime(endTime);
            long duration = endTime - startTime;
            synchronized (this) {
                writer.output(threadNumber, count, startdt.toString(), enddt.toString(), Long.toString(duration), className + "." + methodName, "Success");
            }
            Thread.sleep(m_testSet.getRunDelay().intValue());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            log.error("No such Method - " + e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            log.error("Can't find Class - " + e.getMessage());
        } catch (InvocationTargetException e) {
            synchronized (this) {
                writer.output(threadNumber, count, "", "", "", className + "." + methodName, "Failure");
            }
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /** Logs on to the system based on the Logon Class specified in the xml file
     * @param logOn LogOn object which contains the List of users who can log on and run the tests
     * @param wc WebConversation object to get the response
     * @return  returns the WebResponse object if sucessfully logged in. Or else returns null
     */
    private WebResponse performLogon(LogOn logOn, WebConversation wc) {
        try {
            Class clazz = Class.forName(logOn.getClassName());
            ILoadTesterLogOn logObj = (ILoadTesterLogOn) clazz.newInstance();
            User user = pickUser(logOn.getUsers().getUser());
            WebResponse r = logObj.logOn(wc, logOn.getWebappRoot(), user.getName(), user.getPassword(), logOn.getLoggedOnTitle());
            if (logOn.getLoggedOnTitle() != null) {
                try {
                    if (!logOn.getLoggedOnTitle().equalsIgnoreCase(r.getTitle())) {
                        log.error("Log On Failed For - " + user.getName() + "/" + user.getPassword());
                        return null;
                    }
                } catch (SAXException e) {
                    log.error("Log On Failed For - " + user.getName() + "/" + user.getPassword(), e);
                    return null;
                }
            }
            return r;
        } catch (ClassNotFoundException e) {
            log.error("Can't find Log On Class Implementation - " + logOn.getClassName());
        } catch (IllegalAccessException e) {
            log.error("Can't create Log On Class Implementation - " + e.getMessage());
        } catch (InstantiationException e) {
            log.error("Can't create Log On Class Implementation - " + e.getMessage());
        } catch (ClassCastException e) {
            log.error("Log On Class Implements Wrong Interface");
        }
        return null;
    }

    /** Logs off from the System
     * @param wc WebConversation to get the response
     * @param webRoot  url from which the system needs to be logged off.
     */
    private void performLogout(WebConversation wc, String webRoot) {
        try {
            Class clazz = Class.forName(m_testSet.getLogOn().getClassName());
            ILoadTesterLogOn logObj = (ILoadTesterLogOn) clazz.newInstance();
            logObj.logOff(wc, webRoot);
        } catch (ClassNotFoundException e) {
            log.error("Can't find Log On Class Implementation - " + m_testSet.getLogOn().getClassName());
        } catch (InstantiationException e) {
            log.error("Can't create Log On Class Implementation - " + e.getMessage());
        } catch (IllegalAccessException e) {
            log.error("Can't create Log On Class Implementation - " + e.getMessage());
        }
    }

    private User pickUser(List users) {
        Random myRandom = new Random();
        int randomNumber = myRandom.nextInt(users.size());
        return (User) users.get(randomNumber);
    }
}
