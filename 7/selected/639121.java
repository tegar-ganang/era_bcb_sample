package org.hitchhackers.tools.jmx;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.hitchhackers.tools.jmx.util.ParameterParser;

/**
 * base class that centralizes the logic for connecting via JMX to a running VM.
 * 
 * @author ptraeder
 */
public class JMXClientBase {

    private MBeanServerConnection connection;

    private ParameterParser parameterParser;

    protected static final String USAGE_TITLE = "JMXTools v0.0.5";

    protected static final String USAGE_CONNECTION_SYNTAX = "    (url=<url>|host=<hostname> port=<hostport> [protocol=<protocol>] [jndipath=<jndipath>])";

    protected static final String USAGE_CONNECTION_DETAILS = "Connection parameters:\n" + "  You need to either specify a complete JMX Service URL (with url=<url>)\n" + "  or the following parameters:\n" + "    host\tname/IP of the host to connect to\n" + "    port\tport on which a JMX RMI service is running\n" + "  Optionally you can specify:\n" + "    protocol\tthe protocol to use (defaults to 'rmi')\n" + "    jndipath\tthe jndiPath on which the JMX service is bound (defaults to '/jmxrmi')\n" + "  In most cases, it should be sufficient to pass something like this:\n" + "    host=myserver.domain port=4711\n" + "  where <myserver.domain> is the host you want to connect to and <4711> is the port on which\n" + "  your application exposes its JMX/RMI interface.";

    public JMXClientBase() {
        super();
        parameterParser = new ParameterParser();
        parameterParser.setKnownParams(new String[] { "host", "port", "url", "protocol", "jndipath", "debug", "help" });
    }

    public void parse(String[] args) {
        getParameterParser().parse(args);
    }

    protected JMXServiceURL assembleURL(String namingHost, int namingPort, String serverProtocol, String jndiPath) throws MalformedURLException {
        String serverHost = "host";
        if (serverProtocol == null) {
            serverProtocol = "rmi";
        }
        if (jndiPath == null) {
            jndiPath = "/jmxrmi";
        }
        JMXServiceURL url = new JMXServiceURL("service:jmx:" + serverProtocol + "://" + serverHost + "/jndi/rmi://" + namingHost + ":" + namingPort + jndiPath);
        return url;
    }

    protected JMXServiceURL assembleURL(String namingHost, int namingPort) throws MalformedURLException {
        return assembleURL(namingHost, namingPort, null, null);
    }

    protected void establishConnection() throws IOException {
        try {
            JMXServiceURL jmxURL = null;
            if (parameterParser.getNamedParam("url") != null) {
                jmxURL = new JMXServiceURL(parameterParser.getNamedParam("url"));
            } else {
                if ((parameterParser.getNamedParam("host") == null) || (parameterParser.getNamedParam("port") == null)) {
                    throw new IllegalArgumentException("not enough params to establish a connection - at least 'host' and 'port' are required.");
                }
                try {
                    int port = Integer.valueOf(parameterParser.getNamedParam("port"));
                    jmxURL = assembleURL(parameterParser.getNamedParam("host"), port, parameterParser.getNamedParam("protocol"), parameterParser.getNamedParam("jndipath"));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid port '" + parameterParser.getNamedParam("port") + "'");
                }
            }
            connect(jmxURL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid service URL : " + e.toString());
        }
    }

    protected MBeanServerConnection connect(JMXServiceURL url) throws IOException {
        JMXConnector connector = JMXConnectorFactory.connect(url);
        this.connection = connector.getMBeanServerConnection();
        return this.connection;
    }

    public MBeanServerConnection getConnection() {
        return connection;
    }

    public ParameterParser getParameterParser() {
        return parameterParser;
    }

    /**
	 * Single entry method for all subclasses that can be called this way
	 * The first parameter is the name of the class that should be called, all other parameters
	 * are passed through.
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Please pass the name of the class to run as first parameter");
            System.exit(1);
        }
        String className = args[0];
        String[] newArgs = new String[args.length - 1];
        for (int i = 0; i < args.length - 1; i++) {
            newArgs[i] = args[i + 1];
        }
        String fullClassName = "org.hitchhackers.tools.jmx." + className;
        try {
            Class clazz = Class.forName(fullClassName);
            Object newInstance = clazz.newInstance();
            if (newInstance instanceof JMXClientBase) {
                try {
                    JMXClientBase theApplication = (JMXClientBase) newInstance;
                    theApplication.parse(newArgs);
                    theApplication.establishConnection();
                    theApplication.readParams();
                    theApplication.run();
                } catch (IllegalArgumentException e) {
                    System.err.println("ERROR : " + e.getMessage());
                    System.out.println("");
                    Method method;
                    try {
                        method = clazz.getDeclaredMethod("printUsage", new Class[0]);
                        method.invoke(null, new Object[0]);
                    } catch (Throwable t) {
                        System.err.println("There occurred an error, but the called class does not have usage information associated.");
                        System.err.println("Unfortunately I don't know how to help you...good luck!");
                        t.printStackTrace(System.err);
                    }
                    System.exit(1);
                } catch (IOException e) {
                    System.err.println("could not establish connection to VM via JMX : ");
                    e.printStackTrace(System.err);
                }
            } else {
                System.err.println("invalid class name '" + className + "' - I can work only with subclasses of JMXClientBase");
                System.exit(1);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("could not find class '" + fullClassName + "'");
            e.printStackTrace(System.err);
            System.exit(2);
        } catch (InstantiationException e) {
            System.err.println("could not instantiate class '" + fullClassName + "' : ");
            e.printStackTrace(System.err);
        } catch (IllegalAccessException e) {
            System.err.println("could not access class '" + fullClassName + "' : ");
            e.printStackTrace(System.err);
        } catch (Throwable t) {
            System.err.println("There occurred an error: ");
            t.printStackTrace();
            System.exit(2);
        }
    }

    protected void run() throws Exception {
    }

    protected void readParams() {
    }
}
