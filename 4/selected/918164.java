package de.dgrid.bisgrid.client.tools;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import de.fzj.unicore.wsrflite.ResourcePool;
import de.fzj.unicore.wsrflite.security.ISecurityProperties;
import de.fzj.unicore.wsrflite.security.SecurityProperties;

public class ExecutionUtilities {

    private static PrintWriter clientLog;

    private static PrintWriter statistics;

    public static AbstractWorkflowClient createClientInstance(String url, Class<? extends AbstractWorkflowClient> client, ISecurityProperties sec) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<? extends AbstractWorkflowClient> constructor = client.getConstructor(new Class<?>[] { String.class, ISecurityProperties.class });
        return constructor.newInstance(new Object[] { url, sec });
    }

    public static void fullExecuteInParallel(int numInvokations, int numThreads, String url, ISecurityProperties secFile, Class<? extends AbstractWorkflowClient> client, Object... parameters) throws Exception {
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
        try {
            Collection<CreateExecuteDestroyExecutor> colAll = new ArrayList<CreateExecuteDestroyExecutor>(numInvokations);
            for (int i = 0; i < numInvokations; i++) {
                colAll.add(new CreateExecuteDestroyExecutor(ExecutionUtilities.createClientInstance(url, client, secFile), parameters));
            }
            long start, end;
            start = System.currentTimeMillis();
            List<Future<ClientRecord>> invokeAll = executorService.invokeAll(colAll);
            end = System.currentTimeMillis();
            long[] results = printInstanceStats(invokeAll);
            printFileAndConsole(numThreads, numInvokations, results[0], results[1], 0, end - start, 0);
        } finally {
            executorService.shutdown();
            ResourcePool.getExecutorService().shutdown();
            ResourcePool.getScheduledExecutorService().shutdown();
        }
    }

    public static void plainServiceExecuteInParallel(int numInvokations, int numThreads, String url, String secFile, Class<? extends AbstractWorkflowClient> client, Object... parameters) throws Exception {
        SecurityProperties sec = new SecurityProperties(secFile);
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
        try {
            Collection<ExecuteExecutor> colAll = new ArrayList<ExecuteExecutor>(numInvokations);
            for (int i = 0; i < numInvokations; i++) {
                colAll.add(new ExecuteExecutor(ExecutionUtilities.createClientInstance(url, client, sec), parameters));
            }
            long start, end;
            start = System.currentTimeMillis();
            List<Future<ClientRecord>> invokeAll = executorService.invokeAll(colAll);
            end = System.currentTimeMillis();
            long[] results = printInstanceStats(invokeAll);
            printFileAndConsole(numThreads, numInvokations, results[0], results[1], 0, end - start, 0);
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            executorService.shutdown();
            ResourcePool.getExecutorService().shutdown();
            ResourcePool.getScheduledExecutorService().shutdown();
        }
    }

    /**
	 * 
	 * @param loops
	 * @param numThreads
	 * @param url
	 * @param secFile
	 * @param client
	 * @param parameters
	 * @throws Exception
	 */
    public static void fullExecuteFixedNumberClients(int numInvokations, int numThreads, String url, String secFile, Class<? extends AbstractWorkflowClient> client, Object... parameters) throws Exception {
        SecurityProperties sec = new SecurityProperties(secFile);
        ExecutorService executorService1 = Executors.newSingleThreadExecutor();
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newScheduledThreadPool(numThreads);
        try {
            Collection<CreateExecuteDestroyExecutor> col = new ArrayList<CreateExecuteDestroyExecutor>(1);
            col.add(new CreateExecuteDestroyExecutor(ExecutionUtilities.createClientInstance(url, client, sec), parameters));
            executorService1.invokeAll(col);
            executorService1.shutdown();
            LinkedList<Future<ClientRecord>> invokeAll = new LinkedList<Future<ClientRecord>>();
            long start = System.currentTimeMillis();
            for (int i = 0; i < numInvokations; i += numThreads) {
                Collection<CreateExecuteDestroyExecutor> colAll = new ArrayList<CreateExecuteDestroyExecutor>(numThreads);
                for (int j = 0; j < numThreads; j++) {
                    colAll.add(new CreateExecuteDestroyExecutor(ExecutionUtilities.createClientInstance(url, client, sec), parameters));
                }
                List<Future<ClientRecord>> invoke = executorService.invokeAll(colAll);
                invokeAll.addAll(invoke);
            }
            long end = System.currentTimeMillis();
            long[] results = printInstanceStats(invokeAll);
            printFileAndConsole(numThreads, numInvokations, results[0], results[1], 0, end - start, 0);
        } catch (Exception e) {
            System.err.println(e);
        } finally {
            executorService.shutdown();
            ResourcePool.getExecutorService().shutdown();
            ResourcePool.getScheduledExecutorService().shutdown();
        }
    }

    /**
	 * makes x invokations and uses thread pooly of size 
	 * 
	 * @param numInvokations
	 * @param numThreads
	 * @param url
	 * @param secFile
	 * @param client
	 * @param parameters
	 * @throws Exception
	 */
    public static void executeIn3PhasesParallel(int numInvokations, int numThreads, String url, ISecurityProperties sec, Class<? extends AbstractWorkflowClient> client, Object... parameters) throws Exception {
        long creation = 0;
        long execution = 0;
        long destruction = 0;
        ThreadPoolExecutor executorService = null;
        try {
            executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
            Collection<AbstractWorkflowClient> clients = new ArrayList<AbstractWorkflowClient>(numInvokations);
            for (int i = 0; i < numInvokations; i++) {
                clients.add(ExecutionUtilities.createClientInstance(url, client, sec));
            }
            Collection<Callable<ClientRecord>> colAll = new ArrayList<Callable<ClientRecord>>(numInvokations);
            for (AbstractWorkflowClient cClient : clients) {
                colAll.add(new CreateExecutor(cClient));
            }
            long start, end;
            start = System.currentTimeMillis();
            executorService.invokeAll(colAll);
            end = System.currentTimeMillis();
            executorService.shutdownNow();
            System.out.println("created instances in " + (end - start));
            creation = (end - start);
            executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
            colAll = new ArrayList<Callable<ClientRecord>>(numInvokations);
            for (AbstractWorkflowClient cClient : clients) {
                colAll.add(new ExecuteExecutor(cClient, parameters));
            }
            start = System.currentTimeMillis();
            List<Future<ClientRecord>> invokeAll = executorService.invokeAll(colAll);
            end = System.currentTimeMillis();
            executorService.shutdownNow();
            System.out.println("executed service in " + (end - start));
            execution = (end - start);
            long[] results = printInstanceStats(invokeAll);
            colAll = new ArrayList<Callable<ClientRecord>>(numInvokations);
            for (AbstractWorkflowClient cClient : clients) {
                colAll.add(new DestroyExecutor(cClient));
            }
            executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(numThreads);
            long startDestroy = System.currentTimeMillis();
            executorService.invokeAll(colAll);
            long endDestroy = System.currentTimeMillis();
            executorService.shutdownNow();
            destruction = (endDestroy - startDestroy);
            System.out.println("destroyed instances in " + (endDestroy - startDestroy));
            printFileAndConsole(numThreads, numInvokations, results[0], results[1], creation, execution, destruction);
        } catch (Exception e) {
            System.err.println("error during 3 phases execution: " + e);
            e.printStackTrace();
        } finally {
            executorService.shutdownNow();
            ResourcePool.getExecutorService().shutdownNow();
            ResourcePool.getScheduledExecutorService().shutdownNow();
            executorService.shutdown();
            ResourcePool.getExecutorService().shutdown();
            ResourcePool.getScheduledExecutorService().shutdown();
        }
    }

    private static void print(int threads, int numberInvokes, long successfull, long summedDurations, long creation, long execution, long destruction, PrintWriter writer) {
        if (writer == null) return;
        writer.println("-----------------------");
        writer.println("successfull: " + successfull);
        writer.println("duration creation: " + creation);
        writer.println("duration execution: " + execution);
        writer.println("duration destruction: " + destruction);
        writer.println("summed durations for execution: " + summedDurations);
        writer.println("average response time for execution: " + (summedDurations / numberInvokes));
        writer.println("threads: " + threads);
    }

    private static void printFileAndConsole(int threads, int numberInvokes, long successfull, long summedDurations, long creation, long execution, long destruction) {
        print(threads, numberInvokes, successfull, summedDurations, creation, execution, destruction, new PrintWriter(System.out));
        System.out.flush();
        if (statistics == null) return;
        print(threads, numberInvokes, successfull, summedDurations, creation, execution, destruction, statistics);
        statistics.flush();
    }

    private static long[] printInstanceStats(List<Future<ClientRecord>> results) {
        int successfull = 0;
        long overallDuration = 0;
        for (Future<ClientRecord> future : results) {
            ClientRecord clientRecord;
            try {
                clientRecord = future.get();
            } catch (Exception e) {
                System.err.println(e);
                clientRecord = null;
            }
            if (clientLog != null) {
                if (clientRecord != null) {
                    String instanceId = clientRecord.getInstanceid();
                    long startCall = clientRecord.getStartCallTimestamp();
                    long endCall = clientRecord.getEndCallTimestamp();
                    long duration = (endCall - startCall);
                    overallDuration += duration;
                    successfull++;
                    clientLog.println(instanceId + "\t" + duration);
                } else {
                    clientLog.println("missing" + "\t" + "-1");
                }
            }
        }
        return new long[] { successfull, overallDuration };
    }

    public static void openLogFile(String logfile, String statisticsLogfile) throws Exception {
        clientLog = new PrintWriter(logfile);
        statistics = new PrintWriter(statisticsLogfile);
    }

    public static void closeLogFile() throws Exception {
        clientLog.flush();
        clientLog.close();
        statistics.flush();
        statistics.close();
    }
}
