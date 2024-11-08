package net.bull.javamelody;

import static net.bull.javamelody.HttpParameters.SESSIONS_PART;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.junit.Before;
import org.junit.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.NthIncludedDayTrigger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

public class TestHtmlReport {

    private List<JavaInformations> javaInformationsList;

    private Counter sqlCounter;

    private Counter servicesCounter;

    private Counter jspCounter;

    private Counter counter;

    private Counter errorCounter;

    private Collector collector;

    private StringWriter writer;

    /** Initialisation. */
    @Before
    public void setUp() {
        Utils.initialize();
        javaInformationsList = Collections.singletonList(new JavaInformations(null, true));
        sqlCounter = new Counter("sql", "db.png");
        sqlCounter.setDisplayed(false);
        servicesCounter = new Counter("services", "beans.png", sqlCounter);
        jspCounter = new Counter(Counter.JSP_COUNTER_NAME, null);
        counter = new Counter("http", "dbweb.png", sqlCounter);
        errorCounter = new Counter(Counter.ERROR_COUNTER_NAME, null);
        final Counter jobCounter = JobGlobalListener.getJobCounter();
        collector = new Collector("test", Arrays.asList(counter, sqlCounter, servicesCounter, jspCounter, errorCounter, jobCounter));
        writer = new StringWriter();
    }

    /** Test.
	 * @throws IOException e */
    @Test
    public void testEmptyCounter() throws IOException {
        final HtmlReport htmlReport = new HtmlReport(collector, null, javaInformationsList, Period.TOUT, writer);
        counter.clear();
        errorCounter.clear();
        htmlReport.toHtml(null, null);
        assertNotEmptyAndClear(writer);
    }

    /** Test.
	 * @throws IOException e */
    @Test
    public void testDoubleJavaInformations() throws IOException {
        final List<JavaInformations> myJavaInformationsList = Arrays.asList(new JavaInformations(null, true), new JavaInformations(null, true));
        final HtmlReport htmlReport = new HtmlReport(collector, null, myJavaInformationsList, Period.TOUT, writer);
        htmlReport.toHtml(null, null);
        assertNotEmptyAndClear(writer);
    }

    /** Test.
	 * @throws IOException e */
    @Test
    public void testCounter() throws IOException {
        setProperty(Parameter.WARNING_THRESHOLD_MILLIS, "500");
        setProperty(Parameter.SEVERE_THRESHOLD_MILLIS, "1500");
        setProperty(Parameter.ANALYTICS_ID, "123456789");
        counter.addRequest("test1", 0, 0, false, 1000);
        counter.addRequest("test2", 1000, 500, false, 1000);
        counter.addRequest("test3", 100000, 50000, true, 10000);
        collector.collectWithoutErrors(javaInformationsList);
        final HtmlReport htmlReport = new HtmlReport(collector, null, javaInformationsList, Period.TOUT, writer);
        htmlReport.toHtml("message 2", null);
        assertNotEmptyAndClear(writer);
        setProperty(Parameter.NO_DATABASE, Boolean.TRUE.toString());
        collector.collectWithoutErrors(javaInformationsList);
        htmlReport.toHtml("message 2", null);
        assertNotEmptyAndClear(writer);
        setProperty(Parameter.NO_DATABASE, Boolean.FALSE.toString());
        setProperty(Parameter.WARNING_THRESHOLD_MILLIS, "-1");
        try {
            htmlReport.toHtml("message 2", null);
        } catch (final IllegalStateException e) {
            assertNotNull("ok", e);
        }
        setProperty(Parameter.WARNING_THRESHOLD_MILLIS, null);
        collector = new Collector("test", Arrays.asList(counter));
        final HtmlReport htmlReport2 = new HtmlReport(collector, null, javaInformationsList, Period.TOUT, writer);
        htmlReport2.toHtml(null, null);
    }

    /** Test.
	 * @throws IOException e */
    @Test
    public void testErrorCounter() throws IOException {
        errorCounter.addRequestForSystemError("error", -1, -1, null);
        errorCounter.addRequestForSystemError("error2", -1, -1, "ma stack-trace");
        collector.collectWithoutErrors(javaInformationsList);
        final HtmlReport htmlReport = new HtmlReport(collector, null, javaInformationsList, Period.TOUT, writer);
        htmlReport.toHtml("message 3", null);
        assertNotEmptyAndClear(writer);
        for (final CounterRequest request : errorCounter.getRequests()) {
            htmlReport.writeRequestAndGraphDetail(request.getId());
        }
        htmlReport.writeRequestAndGraphDetail("n'importe quoi");
        assertNotEmptyAndClear(writer);
    }

    /** Test.
	 * @throws IOException e */
    @Test
    public void testPeriodeNonTout() throws IOException {
        collector.collectWithoutErrors(javaInformationsList);
        final String requestName = "test 1";
        counter.bindContext(requestName, "complete test 1", null, -1);
        sqlCounter.addRequest("sql1", 10, 10, false, -1);
        counter.addRequest(requestName, 0, 0, false, 1000);
        counter.addRequest("test2", 1000, 500, false, 1000);
        counter.addRequest("test3", 10000, 500, true, 10000);
        collector.collectWithoutErrors(javaInformationsList);
        final HtmlReport htmlReport = new HtmlReport(collector, null, javaInformationsList, Period.SEMAINE, writer);
        htmlReport.toHtml("message 6", null);
        assertNotEmptyAndClear(writer);
        final HtmlReport htmlReportRange = new HtmlReport(collector, null, javaInformationsList, Range.createCustomRange(new Date(), new Date()), writer);
        htmlReportRange.toHtml("message 6", null);
        assertNotEmptyAndClear(writer);
    }

    /** Test.
	 * @throws Exception e */
    @Test
    public void testWriteRequests() throws Exception {
        final HtmlReport htmlReport = new HtmlReport(collector, null, javaInformationsList, Period.SEMAINE, writer);
        htmlReport.writeRequestAndGraphDetail("httpHitsRate");
        assertNotEmptyAndClear(writer);
        collector.collectWithoutErrors(javaInformationsList);
        sqlCounter.setDisplayed(true);
        final String requestName = "test 1";
        counter.bindContext(requestName, "complete test 1", null, -1);
        servicesCounter.clear();
        servicesCounter.bindContext("myservices.service1", "service1", null, -1);
        sqlCounter.bindContext("sql1", "complete sql1", null, -1);
        sqlCounter.addRequest("sql1", 5, -1, false, -1);
        servicesCounter.addRequest("myservices.service1", 10, 10, false, -1);
        servicesCounter.bindContext("myservices.service2", "service2", null, -1);
        servicesCounter.addRequest("myservices.service2", 10, 10, false, -1);
        servicesCounter.addRequest("otherservices.service3", 10, 10, false, -1);
        servicesCounter.addRequest("otherservices", 10, 10, false, -1);
        jspCounter.addRequest("jsp1", 10, 10, false, -1);
        counter.addRequest(requestName, 0, 0, false, 1000);
        collector.collectWithoutErrors(javaInformationsList);
        final HtmlReport toutHtmlReport = new HtmlReport(collector, null, javaInformationsList, Period.TOUT, writer);
        for (final Counter collectorCounter : collector.getCounters()) {
            for (final CounterRequest request : collectorCounter.getRequests()) {
                toutHtmlReport.writeRequestAndGraphDetail(request.getId());
                assertNotEmptyAndClear(writer);
                toutHtmlReport.writeRequestUsages(request.getId());
                assertNotEmptyAndClear(writer);
            }
        }
        sqlCounter.setDisplayed(false);
        toutHtmlReport.writeCounterSummaryPerClass(servicesCounter.getName(), null);
        String requestId = new CounterRequest("myservices", servicesCounter.getName()).getId();
        toutHtmlReport.writeCounterSummaryPerClass(servicesCounter.getName(), requestId);
        requestId = new CounterRequest("otherservices", servicesCounter.getName()).getId();
        toutHtmlReport.writeCounterSummaryPerClass(servicesCounter.getName(), requestId);
        toutHtmlReport.writeCounterSummaryPerClass(servicesCounter.getName(), "unknown");
    }

    /** Test.
	 * @throws Exception e */
    @Test
    public void testOtherWrites() throws Exception {
        final HtmlReport htmlReport = new HtmlReport(collector, null, javaInformationsList, Period.SEMAINE, writer);
        htmlReport.writeAllCurrentRequestsAsPart(true);
        assertNotEmptyAndClear(writer);
        htmlReport.writeAllCurrentRequestsAsPart(false);
        assertNotEmptyAndClear(writer);
        htmlReport.writeAllThreadsAsPart();
        assertNotEmptyAndClear(writer);
        htmlReport.writeSessionDetail("", null);
        assertNotEmptyAndClear(writer);
        htmlReport.writeSessions(Collections.<SessionInformations>emptyList(), "message", SESSIONS_PART);
        assertNotEmptyAndClear(writer);
        htmlReport.writeSessions(Collections.<SessionInformations>emptyList(), null, SESSIONS_PART);
        assertNotEmptyAndClear(writer);
        htmlReport.writeMBeans(false);
        assertNotEmptyAndClear(writer);
        htmlReport.writeMBeans(true);
        assertNotEmptyAndClear(writer);
        htmlReport.writeProcesses(ProcessInformations.buildProcessInformations(getClass().getResourceAsStream("/tasklist.txt"), true));
        assertNotEmptyAndClear(writer);
        htmlReport.writeProcesses(ProcessInformations.buildProcessInformations(getClass().getResourceAsStream("/ps.txt"), false));
        assertNotEmptyAndClear(writer);
        HtmlReport.writeAddAndRemoveApplicationLinks(null, writer);
        assertNotEmptyAndClear(writer);
        HtmlReport.writeAddAndRemoveApplicationLinks("test", writer);
        assertNotEmptyAndClear(writer);
        final Connection connection = TestDatabaseInformations.initH2();
        try {
            htmlReport.writeDatabase(new DatabaseInformations(0));
            assertNotEmptyAndClear(writer);
            htmlReport.writeDatabase(new DatabaseInformations(3));
            assertNotEmptyAndClear(writer);
            JavaInformations.setWebXmlExistsAndPomXmlExists(true, true);
            htmlReport.toHtml(null, null);
            assertNotEmptyAndClear(writer);
            setProperty(Parameter.SYSTEM_ACTIONS_ENABLED, Boolean.TRUE.toString());
            htmlReport.toHtml(null, null);
            assertNotEmptyAndClear(writer);
            setProperty(Parameter.NO_DATABASE, Boolean.TRUE.toString());
            htmlReport.toHtml(null, null);
            assertNotEmptyAndClear(writer);
        } finally {
            connection.close();
        }
    }

    /** Test.
	 * @throws Exception e */
    @Test
    public void testWriteConnections() throws Exception {
        final HtmlReport htmlReport = new HtmlReport(collector, null, javaInformationsList, Period.SEMAINE, writer);
        htmlReport.writeConnections(JdbcWrapper.getConnectionInformationsList(), false);
        assertNotEmptyAndClear(writer);
        final Connection connection = TestDatabaseInformations.initH2();
        final ExecutorService executorService = Executors.newFixedThreadPool(1);
        final Callable<Connection> task = new Callable<Connection>() {

            @Override
            public Connection call() {
                return TestDatabaseInformations.initH2();
            }
        };
        final Future<Connection> future = executorService.submit(task);
        final Connection connection2 = future.get();
        executorService.shutdown();
        try {
            htmlReport.writeConnections(JdbcWrapper.getConnectionInformationsList(), false);
            assertNotEmptyAndClear(writer);
            htmlReport.writeConnections(JdbcWrapper.getConnectionInformationsList(), true);
            assertNotEmptyAndClear(writer);
        } finally {
            connection.close();
            connection2.close();
        }
    }

    /** Test.
	 * @throws IOException e */
    @Test
    public void testRootContexts() throws IOException {
        HtmlReport htmlReport;
        counter.addRequest("first request", 100, 100, false, 1000);
        TestCounter.bindRootContexts("first request", counter, 3);
        sqlCounter.bindContext("sql", "sql", null, -1);
        htmlReport = new HtmlReport(collector, null, javaInformationsList, Period.TOUT, writer);
        htmlReport.toHtml("message a", null);
        assertNotEmptyAndClear(writer);
        final Counter myCounter = new Counter("http", null);
        final Collector collector2 = new Collector("test 2", Arrays.asList(myCounter));
        myCounter.bindContext("my context", "my context", null, -1);
        htmlReport = new HtmlReport(collector2, null, javaInformationsList, Period.SEMAINE, writer);
        htmlReport.toHtml("message b", null);
        assertNotEmptyAndClear(writer);
        final HtmlCounterRequestContextReport htmlCounterRequestContextReport = new HtmlCounterRequestContextReport(collector2.getRootCurrentContexts(), null, new ArrayList<ThreadInformations>(), false, 500, writer);
        htmlCounterRequestContextReport.toHtml();
        assertNotEmptyAndClear(writer);
    }

    /** Test.
	 * @throws IOException e */
    @Test
    public void testCache() throws IOException {
        final String cacheName = "test 1";
        final CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.addCache(cacheName);
        final String cacheName2 = "test 2";
        try {
            final Cache cache = cacheManager.getCache(cacheName);
            cache.put(new Element(1, Math.random()));
            cache.get(1);
            cache.get(0);
            cacheManager.addCache(cacheName2);
            final Cache cache2 = cacheManager.getCache(cacheName2);
            cache2.getCacheConfiguration().setOverflowToDisk(false);
            cache2.getCacheConfiguration().setEternal(true);
            cache2.getCacheConfiguration().setMaxElementsInMemory(0);
            final List<JavaInformations> javaInformationsList2 = Collections.singletonList(new JavaInformations(null, true));
            final HtmlReport htmlReport = new HtmlReport(collector, null, javaInformationsList2, Period.TOUT, writer);
            htmlReport.toHtml(null, null);
            assertNotEmptyAndClear(writer);
            setProperty(Parameter.SYSTEM_ACTIONS_ENABLED, "false");
            htmlReport.toHtml(null, null);
            assertNotEmptyAndClear(writer);
        } finally {
            setProperty(Parameter.SYSTEM_ACTIONS_ENABLED, null);
            cacheManager.removeCache(cacheName);
            cacheManager.removeCache(cacheName2);
        }
    }

    /** Test.
	 * @throws IOException e
	 * @throws SchedulerException e */
    @Test
    public void testJob() throws IOException, SchedulerException {
        JobGlobalListener.initJobGlobalListener();
        JobGlobalListener.getJobCounter().clear();
        final Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        try {
            scheduler.start();
            final Random random = new Random();
            final JobDetail job2 = new JobDetail("job" + random.nextInt(), null, JobTestImpl.class);
            final SimpleTrigger trigger2 = new SimpleTrigger("trigger" + random.nextInt(), null, new Date(System.currentTimeMillis() + 60000));
            trigger2.setRepeatInterval(2 * 24L * 60 * 60 * 1000);
            scheduler.scheduleJob(job2, trigger2);
            scheduler.pauseJob(job2.getName(), job2.getGroup());
            try {
                final JobDetail job3 = new JobDetail("job" + random.nextInt(), null, JobTestImpl.class);
                final Trigger trigger3 = new CronTrigger("crontrigger" + random.nextInt(), null, "0 0 0 * * ? 2030");
                scheduler.scheduleJob(job3, trigger3);
                final NthIncludedDayTrigger trigger4 = new NthIncludedDayTrigger("nth trigger" + random.nextInt(), null);
                trigger4.setN(1);
                trigger4.setIntervalType(NthIncludedDayTrigger.INTERVAL_TYPE_YEARLY);
                trigger4.setJobName(job3.getName());
                scheduler.scheduleJob(trigger4);
            } catch (final ParseException e) {
                throw new IllegalStateException(e);
            }
            final List<JavaInformations> javaInformationsList2 = Collections.singletonList(new JavaInformations(null, true));
            final HtmlReport htmlReport = new HtmlReport(collector, null, javaInformationsList2, Period.TOUT, writer);
            htmlReport.toHtml(null, null);
            assertNotEmptyAndClear(writer);
            final Map<JobDetail, SimpleTrigger> triggersByJob = new LinkedHashMap<JobDetail, SimpleTrigger>();
            for (int i = 0; i < 10; i++) {
                final JobDetail job = new JobDetail("job" + random.nextInt(), null, JobTestImpl.class);
                job.setDescription("description");
                final SimpleTrigger trigger = new SimpleTrigger("trigger" + random.nextInt(), null, new Date());
                scheduler.scheduleJob(job, trigger);
                triggersByJob.put(job, trigger);
            }
            try {
                Thread.sleep(3000);
            } catch (final InterruptedException e) {
                throw new IllegalStateException(e);
            }
            for (final Map.Entry<JobDetail, SimpleTrigger> entry : triggersByJob.entrySet()) {
                entry.getValue().setRepeatInterval(60000);
                scheduler.scheduleJob(entry.getKey(), entry.getValue());
            }
            setProperty(Parameter.SYSTEM_ACTIONS_ENABLED, Boolean.FALSE.toString());
            final List<JavaInformations> javaInformationsList3 = Collections.singletonList(new JavaInformations(null, true));
            final HtmlReport htmlReport3 = new HtmlReport(collector, null, javaInformationsList3, Period.TOUT, writer);
            htmlReport3.toHtml(null, null);
            assertNotEmptyAndClear(writer);
        } finally {
            scheduler.shutdown();
            JobGlobalListener.getJobCounter().clear();
            JobGlobalListener.destroyJobGlobalListener();
        }
    }

    /** Test.
	 * @throws IOException e */
    @Test
    public void testWithCollectorServer() throws IOException {
        final CollectorServer collectorServer = new CollectorServer();
        try {
            final HtmlReport htmlReport = new HtmlReport(collector, collectorServer, javaInformationsList, Period.TOUT, writer);
            htmlReport.toHtml(null, null);
            assertNotEmptyAndClear(writer);
            Utils.setProperty(Parameters.PARAMETER_SYSTEM_PREFIX + "mockLabradorRetriever", "true");
            collectorServer.collectWithoutErrors();
            htmlReport.toHtml(null, null);
            assertNotEmptyAndClear(writer);
        } finally {
            collectorServer.stop();
        }
    }

    /** Test.
	 * @throws IOException e */
    @Test
    public void testWithNoDatabase() throws IOException {
        final HtmlReport htmlReport = new HtmlReport(collector, null, javaInformationsList, Period.TOUT, writer);
        htmlReport.toHtml(null, null);
        assertNotEmptyAndClear(writer);
    }

    /** Test.
	 * @throws IOException e */
    @Test
    public void testHtmlCounterRequestContext() throws IOException {
        assertNotNull("HtmlCounterRequestContextReport", new HtmlCounterRequestContextReport(Collections.<CounterRequestContext>emptyList(), null, Collections.<ThreadInformations>emptyList(), true, 500, writer));
        final HtmlCounterRequestContextReport report = new HtmlCounterRequestContextReport(Collections.<CounterRequestContext>emptyList(), Collections.<String, HtmlCounterReport>emptyMap(), Collections.<ThreadInformations>emptyList(), true, 500, writer);
        report.toHtml();
        assertNotEmptyAndClear(writer);
        final List<CounterRequestContext> counterRequestContexts = Collections.singletonList(new CounterRequestContext(sqlCounter, null, "Test", "Test", null, -1));
        final HtmlCounterRequestContextReport report2 = new HtmlCounterRequestContextReport(counterRequestContexts, null, Collections.<ThreadInformations>emptyList(), true, 0, writer);
        report2.toHtml();
        assertNotEmptyAndClear(writer);
        report2.writeTitleAndDetails();
    }

    private static void setProperty(Parameter parameter, String value) {
        Utils.setProperty(parameter, value);
    }

    private static void assertNotEmptyAndClear(final StringWriter writer) {
        assertTrue("rapport vide", writer.getBuffer().length() > 0);
        writer.getBuffer().setLength(0);
    }

    /** Test.
	 * @throws IOException e */
    @Test
    public void testToHtmlEn() throws IOException {
        I18N.bindLocale(Locale.US);
        Locale.setDefault(Locale.US);
        try {
            assertEquals("locale en", Locale.US, I18N.getCurrentLocale());
            counter.addRequest("test1", 0, 0, false, 1000);
            counter.addRequest("test2", 1000, 500, false, 1000);
            counter.addRequest("test3", 10000, 5000, true, 10000);
            final HtmlReport htmlReport = new HtmlReport(collector, null, javaInformationsList, Period.TOUT, writer);
            htmlReport.toHtml("message", null);
            assertNotEmptyAndClear(writer);
        } finally {
            I18N.unbindLocale();
            Locale.setDefault(Locale.FRENCH);
        }
    }
}
