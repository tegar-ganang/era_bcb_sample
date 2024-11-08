package net.sf.asyncobjects.jca.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Semaphore;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.asyncobjects.AsyncAction;
import net.sf.asyncobjects.Promise;
import net.sf.asyncobjects.When;
import net.sf.asyncobjects.j2ee.AServiceRegistry;
import net.sf.asyncobjects.jca.JCARunnerConnection;
import net.sf.asyncobjects.jca.JCARunnerConnectionFactory;
import net.sf.asyncobjects.util.AQueue;
import net.sf.asyncobjects.util.ASemaphore;
import net.sf.asyncobjects.util.Queue;
import net.sf.asyncobjects.util.TypeLiteral;
import net.sf.asyncobjects.util.timer.ATimer;
import net.sf.asyncobjects.util.timer.TimerTaskAdapter;
import net.sf.asyncobjects.vats.Vat;
import net.sf.asyncobjects.vats.VatRunner;

/**
 * Servlet that tests JCA Vat Runner connector.
 */
public class JCAVatRunnerTestServlet extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    /**
     * 
     */
    private static final long serialVersionUID = -3989759977874658166L;

    /**
     * Random generator used by tests.
     */
    private final Random rand = new Random();

    /**
     * {@link JCARunnerConnectionFactory} instance.
     */
    private JCARunnerConnectionFactory connectionFactory;

    /**
     * {@inheritDoc}
     */
    public void init() throws ServletException {
        try {
            InitialContext ctx = new InitialContext();
            connectionFactory = ((JCARunnerConnectionFactory) ctx.lookup("java:comp/env/ra/vatrunner_connector"));
        } catch (NamingException ex) {
            throw new ServletException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        try {
            JCARunnerConnection con = connectionFactory.getConnection();
            try {
                writer.println("<html><body><pre>");
                writer.println("Running VatExecutor test...");
                testSimpleExec(con, writer);
                writer.println("VatExecutor test completed.");
                writer.println("Running Timer test...");
                testTimer(con, writer);
                writer.println("Timer test completed.");
                writer.println("Running ServiceRegistry test...");
                testServiceRegistry(con, writer);
                writer.println("ServiceRegistry test completed.");
            } finally {
                con.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace(writer);
            throw new ServletException(ex);
        } finally {
            writer.println("</pre></body></html>");
        }
    }

    /**
     * Simple test for {@link JCARunnerConnection#getServiceRegistry()}.
     * 
     * @param con {@link JCARunnerConnection}.
     * @param writer servlet writer.
     * @throws Exception if any error occurs.
     */
    private void testServiceRegistry(final JCARunnerConnection con, final PrintWriter writer) throws Exception {
        new AsyncAction<Void>() {

            @Override
            public Promise<Void> run() throws Throwable {
                final AServiceRegistry registry = con.getServiceRegistry().willBe(AServiceRegistry.class);
                registry.bind("1", new Queue<String>().export());
                return new When<AQueue<String>, Void>(registry.lookup("1", new TypeLiteral<AQueue<String>>() {
                })) {

                    @Override
                    protected Promise<Void> resolved(AQueue<String> value) throws Throwable {
                        if (value == null) {
                            throw new Exception("Did not find the expected object in the registry.");
                        }
                        writer.println("Registry lookup works.");
                        writer.flush();
                        return null;
                    }
                }.promise();
            }
        }.doInCurrentThread();
    }

    /**
     * Simple test for {@link JCARunnerConnection#getTimer()}.
     * 
     * @param con {@link JCARunnerConnection}.
     * @param writer servlet writer.
     * @throws Exception if any error occurs.
     */
    private void testTimer(final JCARunnerConnection con, final PrintWriter writer) throws Exception {
        writer.println("Obtaining ATimer...");
        final long start = System.nanoTime();
        final long current = System.currentTimeMillis();
        new AsyncAction<Void>() {

            @Override
            public Promise<Void> run() throws Throwable {
                return new When<ATimer, Void>(con.getTimer()) {

                    @Override
                    protected Promise<Void> resolved(ATimer timer) throws Throwable {
                        final ASemaphore s = new net.sf.asyncobjects.util.Semaphore(0).export();
                        writer.println("Scheduling a task...");
                        timer.scheduleAtFixedRate(new Date(current + 100), 100, new TimerTaskAdapter() {

                            int actions = 4;

                            @Override
                            protected void run(long scheduledExecutionTime) throws Throwable {
                                if (actions > 0) {
                                    s.release();
                                    writer.println("Task #" + actions + ". Time elapsed: " + elapsedMs(start) + ".");
                                    actions--;
                                    if (actions == 0) {
                                        cancel();
                                    }
                                } else {
                                }
                            }
                        }.export());
                        writer.println("The task scheduled.");
                        writer.println("Waiting for task complete...");
                        return s.acquire(4);
                    }
                }.promise();
            }
        }.doInCurrentThread();
        writer.println("Task completed. Time elapsed: " + elapsedMs(start) + " nanoseconds.");
    }

    /**
     * Simple test for {@link JCARunnerConnection#getRunner()}.
     *  
     * @param con {@link JCARunnerConnection}.
     * @param writer servlet writer.
     * @throws Exception if any error occurs.
     */
    private void testSimpleExec(JCARunnerConnection con, final PrintWriter writer) throws Exception {
        final Semaphore s = new Semaphore(0);
        Runnable r = new Runnable() {

            public void run() {
                try {
                    Thread.sleep(rand.nextInt(100));
                } catch (InterruptedException e) {
                    e.printStackTrace(writer);
                    writer.flush();
                }
                s.release();
            }
        };
        writer.println("Before starting vats: " + Thread.currentThread().getName());
        writer.flush();
        VatRunner runner = con.getRunner();
        new Vat(runner, "vat 1").enqueue(r);
        new Vat(runner, "vat 2").enqueue(r);
        writer.println("Vats started");
        writer.flush();
        try {
            s.acquire(2);
            if (s.availablePermits() != 0) {
                writer.println("Something went wrong. " + "The Semaphore contains more permits than expected.");
                writer.flush();
            }
        } catch (InterruptedException e) {
            e.printStackTrace(writer);
            writer.flush();
        }
    }

    /**
     * Calculate elapsed ms for nano seconds
     * 
     * @param startNano
     *          start time got with {@link System#nanoTime()}.
     * @return elapsed time in milliseconds
     */
    private static long elapsedMs(final long startNano) {
        return ((System.nanoTime() - startNano) / 1000000);
    }
}
