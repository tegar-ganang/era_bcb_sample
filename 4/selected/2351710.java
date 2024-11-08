package org.nbplugin.jpa.execution;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;
import org.nbplugin.jpa.tools.ExceptionUtil;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 * The executor of JPA queries realized as a thread. This thread uses a parent
 * last classloader in order to isolate the Entity Manager and the persistence
 * classes from the IDE. So we need to make heavy use of reflection to not load
 * a persistence class by the IDEs classloader.
 *
 * The thread runs in a loop, looking for new queries to execute. Notifies
 * the listeners of the result of an execution
 * 
 * @author shofmann
 * @version $Revision: 1.4 $
 * last modified by $Author: sebhof $
 */
public class JPAExecutorThread extends Thread {

    /** A logger */
    private static final Logger logger = Logger.getLogger(JPAExecutorThread.class.getName());

    /** The name of the Persistence class */
    private static final String PERSISTENCE_CLASS_NAME = "javax.persistence.Persistence";

    /** The name of the Query class */
    private static final String QUERY_CLASS_NAME = "javax.persistence.Query";

    /** The name of the EntitityManagerFactory class */
    private static final String CREATE_ENTITY_MANAGER_FACTORY_METHOD_NAME = "createEntityManagerFactory";

    /** The name of the EntitityManager class */
    private static final String CREATE_ENTITY_MANAGER_METHOD_NAME = "createEntityManager";

    /** The name of the createQuery method of the EntitityManager class */
    private static final String CREATE_QUERY_METHOD_NAME = "createQuery";

    /** The name of the getResultList method of the Query class */
    private static final String GET_RESULT_LIST_METHOD_NAME = "getResultList";

    /** The name of the persistence unit to load the entity manager for */
    private String persistenceUnitName;

    /** Indicates whether the thread is fully initialized */
    private boolean initialized = false;

    /** Indicates whether the thread is running */
    private boolean running = true;

    /** The listeners to notify */
    private JPAExecutionListenerSupport listenerSupport;

    /** The queue containing queries to be executed. Will be filled from outside */
    private List<String> queryQueue;

    /** NB io */
    private InputOutput io = IOProvider.getDefault().getIO(JPAExecutor.NAME, false);

    /** The createQuery method of the EntitityManager class */
    private Method createQueryMethod;

    /** The getResultList method of Query class */
    private Method getResultListMethod;

    /** The entitx manager */
    private Object entityManager;

    /**
     * Create a new instance
     * @param persistenceUnitName The name of the persistence unit
     * @param queryQueue The queue to pass queries
     * @param listenerSupport The listeners to notify
     */
    public JPAExecutorThread(String persistenceUnitName, List<String> queryQueue, JPAExecutionListenerSupport listenerSupport) {
        this.persistenceUnitName = persistenceUnitName;
        this.queryQueue = queryQueue;
        this.listenerSupport = listenerSupport;
        this.io.setErrVisible(true);
        this.io.setOutputVisible(true);
    }

    /**
     * Run method of Thread
     */
    @Override
    public void run() {
        while (running) {
            if (!initialized) {
                io.getOut().write("Initializing Executor Thread...\n");
                io.getOut().flush();
                InitializationResult initializationResult = new InitializationResult();
                try {
                    Class persistenceClass = this.getContextClassLoader().loadClass(PERSISTENCE_CLASS_NAME);
                    Method createEntityManagerFactoryMethod = persistenceClass.getDeclaredMethod(CREATE_ENTITY_MANAGER_FACTORY_METHOD_NAME, String.class);
                    Object entityManagerFactory = createEntityManagerFactoryMethod.invoke(null, this.persistenceUnitName);
                    Class entityManagerFactoryClass = entityManagerFactory.getClass();
                    Method createEntityManagerMethod = entityManagerFactoryClass.getDeclaredMethod(CREATE_ENTITY_MANAGER_METHOD_NAME);
                    this.entityManager = createEntityManagerMethod.invoke(entityManagerFactory);
                    this.createQueryMethod = this.entityManager.getClass().getMethod(CREATE_QUERY_METHOD_NAME, String.class);
                    Class queryClass = this.getContextClassLoader().loadClass(QUERY_CLASS_NAME);
                    this.getResultListMethod = queryClass.getMethod(GET_RESULT_LIST_METHOD_NAME);
                    this.initialized = true;
                    io.getOut().write("Initialized successfully\n");
                    io.getOut().flush();
                    initializationResult.setErrors(false);
                } catch (Exception ex) {
                    initializationResult.setErrors(true);
                    initializationResult.setThrowable(ex);
                    this.stopThread();
                }
                this.listenerSupport.notifListenersForInitialized(initializationResult);
            } else {
                if (!this.queryQueue.isEmpty()) {
                    String queryString = null;
                    synchronized (this.queryQueue) {
                        queryString = this.queryQueue.remove(0);
                    }
                    JPAExecutorResult result = new JPAExecutorResult();
                    try {
                        io.getOut().write("Executing Query " + queryString + "\n");
                        io.getOut().flush();
                        Object query = this.createQueryMethod.invoke(this.entityManager, queryString);
                        List resultList = (List) this.getResultListMethod.invoke(query);
                        result.setResultList(resultList);
                    } catch (Exception ex) {
                        io.getErr().write("Error executing Query: " + ex.getMessage() + "\n");
                        io.getErr().flush();
                        result.setErrors(true);
                        result.setErrorMessage(ExceptionUtil.findRootCause(ex).getMessage());
                    }
                    this.listenerSupport.notifyListenersForResult(result);
                }
            }
            try {
                sleep(1000);
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Stop the thread
     */
    public void stopThread() {
        io.getOut().write("Stopping ExecutorThread...");
        this.running = false;
    }
}
