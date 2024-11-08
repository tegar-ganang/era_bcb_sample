package bpiwowar.expmanager.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import bpiwowar.argparser.Argument;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.expmanager.experiments.JSHandler;
import bpiwowar.expmanager.experiments.TaskRepository;
import bpiwowar.expmanager.locks.LockType;
import bpiwowar.expmanager.rsrc.CommandLineTask;
import bpiwowar.expmanager.rsrc.LockMode;
import bpiwowar.expmanager.rsrc.Resource;
import bpiwowar.expmanager.rsrc.SimpleData;
import bpiwowar.expmanager.rsrc.Task;
import bpiwowar.expmanager.rsrc.TaskManager;
import bpiwowar.log.Logger;
import bpiwowar.utils.Output;

/**
 * The server displays information about the tasks and responds to XML RPC tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TaskDescription(name = "server", project = { "xpmanager" })
public class ServerTask extends AbstractTask {

    private static final Logger logger = Logger.getLogger();

    @Argument(name = "xmlrpc", help = "XML RPC configuration file")
    File xmlrpcConfigFile;

    @Argument(name = "base", help = "Base directory for the task manager", required = true, checkers = IOChecker.ValidDirectory.class)
    File taskmanagerDirectory;

    @Argument(name = "nb-threads", help = "Number of threads")
    int nbThreads = 10;

    /**
	 * Just for debug purposes, we provide a Calculator with two methods (add
	 * and substract)
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
    public static class Calculator {

        public int add(int i1, int i2) {
            return i1 + i2;
        }

        public int subtract(int i1, int i2) {
            return i1 - i2;
        }
    }

    /**
	 * Our RPC handler for task manager
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
    public static class RPCTaskManager {

        /**
		 * The task manager
		 */
        private TaskManager taskManager;

        /**
		 * Repository
		 */
        TaskRepository repository;

        void setTaskServer(TaskManager taskManager, TaskRepository repository) {
            this.taskManager = taskManager;
            this.repository = repository;
        }

        public boolean addData(String id, String mode, boolean exists) {
            logger.info("Addind data %s [%s/%b]", id, mode, exists);
            taskManager.add(new SimpleData(taskManager, id, LockMode.valueOf(mode), exists));
            return true;
        }

        public static class JSGetEnv {

            private final Map<String, String> environment;

            public JSGetEnv(Map<String, String> environment) {
                this.environment = environment;
            }

            public String get(String key) {
                return environment.get(key);
            }

            public String get(String key, String defaultValue) {
                String value = environment.get(key);
                if (value == null) return defaultValue;
                return value;
            }
        }

        org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            org.mozilla.javascript.Context.exit();
        }

        /**
		 * Run a javascript
		 */
        public boolean runJSScript(boolean isFile, String content, Map<String, String> environment) {
            try {
                Scriptable scope = cx.initStandardObjects();
                ScriptableObject.defineProperty(scope, "env", new JSGetEnv(environment), 0);
                JSHandler jsXPM = new JSHandler(cx, scope, repository, taskManager);
                ScriptableObject.defineProperty(scope, "xpm", jsXPM, 0);
                final Object result;
                if (isFile) result = cx.evaluateReader(scope, new FileReader(content), content, 1, null); else result = cx.evaluateString(scope, content, "stdin", 1, null);
                if (result != null) logger.info(result.toString()); else logger.info("Null result");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        /**
		 * Add a command line job
		 */
        public boolean runCommand(String name, int priority, Object[] command, Object[] envArray, String workingDirectory, Object[] depends, Object[] readLocks, Object[] writeLocks) {
            Map<String, String> env = new TreeMap<String, String>();
            for (Object x : envArray) {
                Object[] o = (Object[]) x;
                if (o.length != 2) throw new RuntimeException();
                env.put((String) o[0], (String) o[1]);
            }
            logger.info("Running command %s [%s] (priority %d); read=%s, write=%s; environment={%s}", name, Arrays.toString(command), priority, Arrays.toString(readLocks), Arrays.toString(writeLocks), Output.toString(", ", env.entrySet()));
            String[] commandArgs = new String[command.length];
            for (int i = command.length; --i >= 0; ) commandArgs[i] = command[i].toString();
            CommandLineTask job = new CommandLineTask(taskManager, name, commandArgs, env, new File(workingDirectory));
            for (Object depend : depends) {
                Resource resource = taskManager.getResource((String) depend);
                if (resource == null) throw new RuntimeException("Resource " + depend + " was not found");
                job.addDependency(resource, LockType.GENERATED);
            }
            for (Object readLock : readLocks) {
                Resource resource = taskManager.getResource((String) readLock);
                if (resource == null) throw new RuntimeException("Resource " + readLock + " was not found");
                job.addDependency(resource, LockType.READ_ACCESS);
            }
            for (Object writeLock : writeLocks) {
                final String id = (String) writeLock;
                Resource resource = taskManager.getResource(id);
                if (resource == null) {
                    resource = new SimpleData(taskManager, id, LockMode.EXCLUSIVE_WRITER, false);
                    resource.register(job);
                }
                job.addDependency(resource, LockType.WRITE_ACCESS);
            }
            taskManager.add(job);
            return true;
        }
    }

    /**
	 * Server thread
	 */
    public int execute() throws Throwable {
        logger.info("Starting server");
        Server server = new Server(8080);
        final TaskManager taskManager = new TaskManager(taskmanagerDirectory, nbThreads);
        final TaskRepository repository = new TaskRepository();
        Context xmlrpcContext = new Context(server, "/xmlrpc");
        final XmlRpcServlet xmlRpcServlet = new XmlRpcServlet() {

            private static final long serialVersionUID = 1L;

            @Override
            protected PropertyHandlerMapping newPropertyHandlerMapping(URL url) throws IOException, XmlRpcException {
                PropertyHandlerMapping mapping = new PropertyHandlerMapping();
                RequestProcessorFactoryFactory factory = new RequestProcessorFactoryFactory() {

                    public RequestProcessorFactory getRequestProcessorFactory(final Class pClass) throws XmlRpcException {
                        return new RequestProcessorFactory() {

                            public Object getRequestProcessor(XmlRpcRequest pRequest) throws XmlRpcException {
                                try {
                                    Object object = pClass.newInstance();
                                    if (object instanceof RPCTaskManager) {
                                        ((RPCTaskManager) object).setTaskServer(taskManager, repository);
                                    }
                                    return object;
                                } catch (InstantiationException e) {
                                    throw new RuntimeException(e);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        };
                    }
                };
                mapping.setRequestProcessorFactoryFactory(factory);
                mapping.addHandler("Calculator", Calculator.class);
                mapping.addHandler("TaskManager", RPCTaskManager.class);
                return mapping;
            }

            @Override
            protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() {
                try {
                    return newPropertyHandlerMapping(null);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
        xmlRpcServlet.init(new ServletConfig() {

            public String getServletName() {
                return xmlRpcServlet.getClass().getName();
            }

            public ServletContext getServletContext() {
                throw new IllegalStateException("Context not available");
            }

            public String getInitParameter(String pArg0) {
                return null;
            }

            @SuppressWarnings("unchecked")
            public Enumeration<?> getInitParameterNames() {
                return new Enumeration() {

                    public boolean hasMoreElements() {
                        return false;
                    }

                    public Object nextElement() {
                        throw new NoSuchElementException();
                    }
                };
            }
        });
        final ServletHolder servletHolder = new ServletHolder(xmlRpcServlet);
        xmlrpcContext.addServlet(servletHolder, "/");
        Context context = new Context(server, "/", Context.SESSIONS);
        context.addServlet(new ServletHolder(new StatusServlet(taskManager)), "/status");
        context.addServlet(new ServletHolder(new HelloServlet()), "/*");
        server.start();
        server.join();
        return 0;
    }

    /**
	 * Gives the current task status
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
    public class StatusServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        private final TaskManager manager;

        public StatusServlet(TaskManager manager) {
            this.manager = manager;
        }

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            final PrintWriter out = response.getWriter();
            out.println("<h1>Waiting tasks</h1>");
            out.println("<ul>");
            for (Task task : manager.tasks()) {
                out.format("<li>%s</li>", task);
            }
            out.println("</ul>");
            out.println("<h1>List of resources</h1>");
            out.println("<ul>");
            for (Resource resource : manager.resources()) {
                out.format("<li>[%s, %b] %s</li>", resource.getClass(), resource, resource.isGenerated());
            }
            out.println("</ul>");
        }
    }

    public static class HelloServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            final PrintWriter out = response.getWriter();
            out.println("<h1>Hello SimpleServlet</h1>");
            out.format("<p>%s</p>", request.getRequestURI());
            out.println("session id is " + request.getSession(true).getId());
        }
    }
}
