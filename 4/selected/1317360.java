package br.nic.connector.generics;

import java.util.Date;
import br.nic.connector.general.SimpleLog;

/**
 * Abstract class that implements the Processing Threads that will be used on making the threads. It implements
 * general usage code that will deal with error management making the overall execution to go more smoothly,
 * leaving only the actual test to be implemented by the classes that will extend this.
 * @author Pedro Hadek
 */
public abstract class GenericTest extends Thread {

    protected Object currTestElement;

    protected AutomatedTest tester;

    protected Boolean encrypt;

    protected Date lastUpdate;

    protected int threadID;

    /**
	 * Associates a id with this thread. Used to do some tracking.
	 */
    public final void setThreadID(int id) {
        threadID = id;
    }

    /**
	 * Indicates if the data should be encrypted or not.
	 */
    public final void setEncrypt(boolean encrypt) {
        this.encrypt = encrypt;
    }

    /**
	 * Defines which AutomatedTester implementation will manage the data needed in order to synchronize this
	 * Thread with other threads.
	 */
    public final void setAutomatedTester(AutomatedTest tester) {
        this.tester = tester;
    }

    /**
	 * Defines test element as the next test Element. Used on the class initialization.
	 */
    public final void setCurrTestElement(Object nextTestElement) {
        this.currTestElement = nextTestElement;
    }

    /**
	 * Asynchronously executed routine, by which all tests are done, until the element to be tested becomes null.
	 * Tester and Encrypt MUST be defined beforehand.
	 */
    public final void run() {
        int retries = 0;
        if (tester == null || encrypt == null) {
            throw new NullPointerException("Dados de teste ou encriptação não inicializados.");
        }
        boolean end = false;
        while (!end) {
            lastUpdate = new Date(System.currentTimeMillis());
            if (retries == br.nic.connector.general.Constants.DEFAULT_MAXTESTTRIES) {
                retries = 0;
                currTestElement = tester.finishTestElement(currTestElement, null);
            }
            retries += 1;
            try {
                if (currTestElement == null) {
                    end = true;
                } else {
                    Object result = testThisElement(currTestElement);
                    currTestElement = tester.finishTestElement(currTestElement, result);
                    retries = 0;
                }
            } catch (Exception e) {
                try {
                    SimpleLog.getInstance().writeException(e, 3);
                    if (currTestElement == null) {
                        SimpleLog.getInstance().writeLog(3, "Finalizando Thread em exceção, pois " + "o elemento de testes atual é nulo.");
                        end = true;
                    } else {
                        SimpleLog.getInstance().writeLog(3, "Ocorrida em " + (currTestElement instanceof Object[] ? ((Object[]) currTestElement)[0].toString() : currTestElement.toString()));
                    }
                } catch (Exception ex) {
                    System.err.println("\nExceção no tratamento de exceção!\n");
                    ex.printStackTrace();
                }
            } catch (StackOverflowError e) {
                SimpleLog.getInstance().writeLog(3, "Erro de Stack Over Flow durante teste de " + tester.type + " no elemento " + testElementString());
            } catch (ThreadDeath err) {
                tester.writeTestData(currTestElement, null);
                end = true;
                System.out.println("Passou por aqui o " + testElementString());
            }
        }
        try {
            cleanup();
        } catch (Throwable t) {
        }
        tester.finishThread(this, true);
    }

    /** Must implement the test related to the type of element being used.*/
    public abstract Object testThisElement(Object element);

    /** Function to be overridden by threads that must do cleanup routines before exiting.*/
    public void cleanup() {
    }

    /**
	 * Should return a String representing the current Test Element. Defaults to casting the test
	 * element to a String, but should be overridden in case the test element differs for one of
	 * the tests. <br>
	 * Used in order to generate some of the logs. 
	 */
    public String testElementString() {
        String returnString;
        if (currTestElement instanceof Object[]) {
            Object[] x = (Object[]) currTestElement;
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < x.length; i++) {
                sb.append(x[i]);
            }
            returnString = sb.toString();
        } else {
            returnString = currTestElement.toString();
        }
        return returnString;
    }
}
