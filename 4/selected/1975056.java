package de.sicari.webservice;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import javax.xml.namespace.QName;
import org.apache.axis.client.Service;
import org.apache.axis.components.compiler.Compiler;
import org.apache.axis.components.compiler.CompilerFactory;
import org.apache.axis.utils.CLArgsParser;
import org.apache.axis.utils.CLOption;
import org.apache.axis.utils.Messages;
import org.apache.axis.wsdl.WSDL2Java;
import de.fhg.igd.logging.LogLevel;
import de.fhg.igd.logging.Logger;
import de.fhg.igd.logging.LoggerFactory;
import de.sicari.kernel.SicariSecurityManager;
import de.sicari.util.CanonicalPath;
import de.sicari.util.WildcardedCharSequence;

/**
 * This <i>Factory</i> class is designed to be used for creating
 * <code>Webservice</code> client stub objects. Calling
 * {@link #getClientProxy(Webservice.Description)} will return a
 * {@link Proxy client proxy} that implements the specified interfaces.
 *
 * @author Matthias Pressfreund
 * @version "$Id: ClientProxyFactory.java 335 2007-09-06 19:20:10Z jpeters $"
 */
public class ClientProxyFactory {

    /**
     * The <code>Logger</code> instance for this class
     */
    private static Logger log_ = LoggerFactory.getLogger("webservice");

    /**
     * Disabled construction.
     */
    private ClientProxyFactory() {
    }

    /**
     * Create a webservice client stub according to the specified
     * <code>Webservice.Description</code>.
     * <p><b>Notice</b>: If the given <code>webserviceDescription</code>
     * is {@link Webservice.Description#isGeneric generic} (i.e. it does not
     * specify any interface classes), the generated client object will
     * accordingly as well implement a generic interface, providing all allowed
     * <code>public</code> methods of the original service. However, since
     * the name of the generic interface is generally unknown at compile time,
     * access then is only possible via the <i>Java Reflection Framework</i>.
     *
     * @param webserviceDescription The description of the corresponding
     *   webservice, typically provided by a
     *   {@link de.sicari.webservice.uddi.UddiService}, but also
     *   user-definable
     *
     * @return The webservice client, which is actually a
     *   {@link java.lang.reflect.Proxy} providing all interfaces specified
     *   within the given <code>webserviceDescription</code>.
     *
     * @throws AxisException if the <i>Axis</i> framework is not accessible
     *   or was unable to process the request
     * @throws ClassNotFoundException if an interface specified by the
     *   <code>webserviceDescription</code> cannot be resolved
     * @throws IllegalArgumentException if the
     *   <code>webserviceDescription</code> is invalid
     */
    public static Object getClientProxy(Webservice.Description webserviceDescription) throws AxisException, ClassNotFoundException, IllegalArgumentException {
        log_.entering(new Object[] { webserviceDescription });
        Class<?>[] ifclasses;
        Class<?> clazz;
        String[] ifnames;
        Object client;
        Object stub;
        String msg;
        int i;
        WebservicePermission.checkPermission(null, "createClient");
        if (webserviceDescription == null) {
            log_.error(msg = "The Webservice description is null");
            throw new IllegalArgumentException(msg);
        }
        ifnames = webserviceDescription.getInterfaces();
        if (ifnames != null) {
            ifclasses = new Class[ifnames.length];
            for (i = 0; i < ifnames.length; i++) {
                ifclasses[i] = Class.forName(ifnames[i]);
            }
        } else {
            ifclasses = null;
        }
        stub = StubGenerator.createStub(webserviceDescription.getWsdlURL());
        clazz = stub.getClass();
        client = Proxy.newProxyInstance(clazz.getClassLoader(), ifclasses != null ? ifclasses : clazz.getInterfaces(), new StubInvocationHandler(stub));
        log_.info("Successfully created webservice client for " + webserviceDescription);
        log_.exiting(client);
        return client;
    }

    /**
     * This tool bases on the <tt>Axis</tt> <code>WSDL2Java</code> command
     * line utility. It {@link #createStub(String) creates} a plain client
     * stub object from a given <tt>WSDL</tt> <code>URL</code>.
     */
    protected static class StubGenerator extends WSDL2Java {

        /**
         * The <code>Logger</code> instance for this class
         */
        private static Logger log2_ = LoggerFactory.getLogger("webservice");

        /**
         * The identifier of the <code>ServiceLocator</code> class
         */
        protected static final String LOCATOR_ID_ = "ServiceLocator";

        /**
         * The corresponding <tt>WSDL</tt> <code>URL</code>
         */
        protected String wsdlURL_;

        /**
         * The temporary directory, used for creating and compiling
         * the stub source files
         */
        protected File tmpdir_;

        /**
         * Create a <code>StubGenerator</code>.
         */
        protected StubGenerator(String wsdlURL) throws IOException {
            super();
            wsdlURL_ = wsdlURL;
            tmpdir_ = createTempDir();
            log2_.debug("Instance successfully created for '" + wsdlURL_ + "'");
        }

        /**
         * Create the client stub from a <tt>WSDL</tt> <code>URL</code>.
         *
         * @param wsdlURL The <code>URL</code> to retrieve the <tt>WSDL</tt>
         *   document from
         *
         * @return The client stub
         *
         * @throws AxisException if creating the client stub failed
         * @throws RuntimeException if the <code>StubGenerator</code> could not
         *   be created, the causing <code>Exception</code> will be attached
         */
        public static Object createStub(String wsdlURL) throws AxisException {
            StubGenerator sg;
            try {
                sg = new StubGenerator(wsdlURL);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                return sg.createInstance(sg.compileSources(sg.createSources()));
            } finally {
                sg.deleteTempDir();
            }
        }

        /**
         * Create a new temporary directory within either the standard
         * temporary directory, the user home directory or the current
         * working directory, depending on where read/write permission
         * is provided first.
         *
         * @return The new temporary directory
         *
         * @throws IllegalStateException
         *   <ul>
         *   <li>if no temporary directory with read/write permission
         *       could be found
         *   <li>if the sub-directory could not be created for any reason
         *   </ul>
         */
        protected static File createTempDir() throws IOException {
            String[] tmpnames;
            Random random;
            File tmpdir;
            File tmpsys;
            String msg;
            int cnt;
            tmpnames = new String[] { System.getProperty("java.io.tmpdir"), System.getProperty("user.home"), System.getProperty("user.dir") };
            tmpdir = null;
            for (String name : tmpnames) {
                tmpsys = new File(name);
                if (tmpsys.canRead() && tmpsys.canWrite()) {
                    random = new Random();
                    cnt = 1000;
                    while (cnt-- > 0) {
                        tmpdir = new File(tmpsys, String.valueOf(random.nextInt(Integer.MAX_VALUE)));
                        if (tmpdir.mkdir()) {
                            log2_.debug("Successfully created '" + tmpdir + "'");
                            return tmpdir;
                        }
                        log2_.debug("Creating '" + tmpdir + "' failed");
                    }
                    msg = "Could not create directory within '" + tmpsys + "'";
                    log2_.error(msg);
                    throw new IOException(msg);
                }
            }
            msg = "Could not find directory with read/write permission";
            log2_.error(msg);
            throw new IOException(msg);
        }

        /**
         * Create the client stub Java source files by means of the <i>Axis</i>
         * {@link WSDL2Java} tool.
         *
         * @return The client stub locator source file, or <code>null</code>
         *   if no stub locator source file can be found.
         *
         * @throws AxisException if creating the client stub source files failed
         */
        protected File createSources() throws AxisException {
            FileFilter stublocatorfilter;
            FileFilter dirfilter;
            CLArgsParser clap;
            List<File> dirs;
            String[] args;
            List<?> clargs;
            File[] list;
            File stublocator;
            File tmpdir;
            String msg;
            int size;
            int i;
            args = new String[] { String.valueOf(new char[] { '-', WSDL2Java.OUTPUT_OPT }), tmpdir_.getAbsolutePath(), wsdlURL_ };
            clap = new CLArgsParser(args, options);
            if ((msg = clap.getErrorString()) != null) {
                msg = Messages.getMessage("error01", msg);
                log2_.error(msg);
                throw new AxisException(msg);
            }
            clargs = clap.getArguments();
            size = clargs.size();
            try {
                for (i = 0; i < size; i++) {
                    parseOption((CLOption) clargs.get(i));
                }
                validateOptions();
                parser.run(wsdlURI);
                tmpdir = tmpdir_;
                dirfilter = new FileFilter() {

                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                };
                stublocatorfilter = new FileFilter() {

                    public boolean accept(File pathname) {
                        return (pathname.isFile() && pathname.getName().endsWith(LOCATOR_ID_ + ".java"));
                    }
                };
                stublocator = null;
                dirs = new LinkedList<File>();
                dirs.add(tmpdir);
                while (!dirs.isEmpty()) {
                    tmpdir = dirs.remove(0);
                    if ((list = tmpdir.listFiles(stublocatorfilter)).length == 1) {
                        stublocator = list[0];
                        dirs.clear();
                    } else if ((list = tmpdir.listFiles(dirfilter)).length > 0) {
                        dirs.addAll(Arrays.asList(list));
                    }
                }
            } catch (Exception e) {
                msg = "Creating client stub sources failed";
                log2_.caught(LogLevel.ERROR, msg, e);
                throw new AxisException(msg, e);
            }
            log2_.debug("Successfully created client stub locator '" + stublocator + "' into '" + tmpdir_ + "'");
            return stublocator;
        }

        /**
         * Compile the client stub sources in the temporary directory.
         *
         * @param stublocator The client stub locator source file
         *
         * @return The locator class name
         *
         * @throws AxisException if compiling the client stub sources failed
         */
        protected String compileSources(File stublocator) throws AxisException {
            CanonicalPath source;
            CanonicalPath base;
            Compiler compiler;
            boolean compiled;
            String locator;
            String tmpdir;
            compiler = CompilerFactory.getCompiler();
            tmpdir = tmpdir_.getAbsolutePath();
            compiler.setClasspath(System.getProperty("java.class.path"));
            compiler.setDestination(tmpdir);
            compiler.setSource(tmpdir);
            compiler.addFile(stublocator.getAbsolutePath());
            try {
                compiled = compiler.compile();
            } catch (Exception e) {
                log2_.caught(e);
                compiled = false;
            }
            base = new CanonicalPath(tmpdir_.getAbsolutePath(), File.separatorChar);
            source = new CanonicalPath(stublocator.getAbsolutePath(), File.separatorChar);
            source = source.tail(base.length());
            locator = source.toString();
            locator = locator.substring(0, locator.lastIndexOf('.'));
            locator = locator.replace(File.separatorChar, '.');
            if (compiled) {
                log2_.debug("Successfully compiled client stub locator source '" + locator + "' in '" + tmpdir_ + "'");
            } else {
                String msg = "Failed compiling client stub locator source '" + locator + "' in '" + tmpdir + "'";
                log2_.error(msg);
                throw new AxisException(msg);
            }
            return locator;
        }

        /**
         * Create client stub instance by means of the service locator class.
         *
         * @param locator The fully qualified service locator class name
         *
         * @return The client stub instance
         *
         * @throws AxisException if creating the client stub instance failed
         */
        protected Object createInstance(String locator) throws AxisException {
            Service locatorInstance;
            Iterator<?> itr;
            Class<?> clazz;
            Object instance;
            QName serviceName;
            int n;
            serviceName = null;
            instance = null;
            try {
                if ((n = locator.indexOf('.')) > 0) {
                    SicariSecurityManager.addTrusted(new WildcardedCharSequence(locator.substring(0, n) + ".*"));
                } else {
                    log2_.warning("Failed registering default package/class '" + locator + "' with " + SicariSecurityManager.class.getName());
                }
                clazz = Class.forName(locator, true, new URLClassLoader(new URL[] { tmpdir_.toURL() }));
                log2_.debug("Successfully created service locator class '" + locator + "'");
                locatorInstance = (Service) clazz.newInstance();
                log2_.debug("Successfully created service locator instance '" + locator + "'");
                itr = locatorInstance.getPorts();
                if (itr.hasNext()) {
                    serviceName = (QName) itr.next();
                    log2_.debug("Retrieved port '" + serviceName.getLocalPart() + "' from service locator");
                    instance = locatorInstance.getPort(serviceName, null);
                    log2_.debug("Successfully created '" + serviceName.getLocalPart() + "' client stub instance " + instance);
                }
            } catch (Exception e) {
                String msg = "Failed creating '" + serviceName + "' client stub instance from '" + locator + "'";
                log2_.caught(LogLevel.ERROR, msg, e);
                throw new AxisException(msg, e);
            }
            return instance;
        }

        /**
         * Recursively delete the temporary directory.
         */
        protected void deleteTempDir() {
            if (tmpdir_ == null) {
                return;
            }
            if (purge(tmpdir_)) {
                log2_.debug("Successfully deleted '" + tmpdir_ + "'");
            } else {
                log2_.warning("Failed deleting '" + tmpdir_ + "'");
            }
        }

        /**
         * Delete the specified <code>File</code>. If it is a directory,
         * recursively delete all contained files/directories first.
         *
         * @param file The file or directory to delete
         *
         * @return Indicates whether or not the specified file/directory
         *   was deleted successfully
         */
        protected static boolean purge(File file) {
            File[] dir;
            int i;
            if (file.isDirectory()) {
                dir = file.listFiles();
                for (i = 0; i < dir.length; i++) {
                    if (!purge(dir[i])) {
                        log2_.warning("Failed deleting '" + dir[i] + "'");
                    }
                }
            }
            return file.delete();
        }
    }

    /**
     * This <code>InvocationHandler</code> is needed for creating
     * {@link java.lang.reflect.Proxy} objects for <i>Axis</i> webservice
     * clients. Method invocations will be delegated to the corresponding
     * methods of the client stub object, nested exceptions will be unwrapped
     * and re-thrown.
     */
    protected static class StubInvocationHandler implements InvocationHandler {

        /**
         * The <code>Logger</code> instance for this class
         */
        private static Logger log2_ = LoggerFactory.getLogger("webservice");

        /**
         * The client stub object
         */
        protected Object stub_;

        /**
         * Create a <code>WebserviceInvocationHandler</code>.
         */
        public StubInvocationHandler(Object stub) {
            if ((stub_ = stub) == null) {
                throw new NullPointerException("stub");
            }
            log2_.debug("Instance successfully created for " + stub_);
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Throwable cause;
            Method _method;
            _method = stub_.getClass().getMethod(method.getName(), method.getParameterTypes());
            if (log2_.isEnabled(LogLevel.DEBUG)) {
                log2_.debug("Invoking [" + _method + "] with arguments " + Arrays.asList(args != null ? args : new Object[0]) + " on " + stub_);
            }
            try {
                return _method.invoke(stub_, args);
            } catch (InvocationTargetException e) {
                log2_.caught(e);
                if ((cause = e.getCause()) != null) {
                    throw cause;
                }
                log2_.warning("Could not find target Exception");
                throw e;
            }
        }
    }
}
