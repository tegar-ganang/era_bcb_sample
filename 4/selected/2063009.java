package org.scub.foundation.plugin.gwt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Lancement du Hosted Mode.
 * @author Martin Algesten
 */
public abstract class AbstractGwtShellMojo extends AbstractGwtScriptMojo {

    /**
     * The url.
     * @required
     * @parameter
     */
    private String url;

    /**
     * The module.
     * @required
     * @parameter
     */
    private String module;

    /**
     * Runs an embedded Tomcat instance on the specified port (defaults to 8888).
     * @parameter
     */
    private int port = -1;

    /**
     * Prevents the embedded Tomcat server from running, even if a port is specified.
     * @parameter
     */
    private boolean noserver = false;

    /**
     * The 'style' parameter, should be one of <code>OBF[USCATED]</code>, <code>PRETTY</code> or <code>DETAILED</code>. (Defaults to <code>OBF</code>).
     * @parameter
     */
    private String style = null;

    /**
     * The maximum amount of heap space to use, default is '256m' (-Xmx256m).
     * @parameter expression="256m"
     */
    private String maxmem = null;

    /**
     * Mise ne place du debug sur l'application GWT (defaults to 8189).
     * @parameter
     */
    protected int debugport = -1;

    /**
     * Demande de debug.
     */
    protected boolean debugIsEnable = false;

    /**
     * Port par defaut pour l'execution en mode debug.
     */
    protected static final int DEFAULT_DEBUG_PORT = 8189;

    /**
     * {@inheritDoc}
     */
    public void executeProcess() throws MojoExecutionException, MojoFailureException {
        try {
            extractNativeLibs();
            final File file = createScriptFile("shell");
            final FileWriter fwriter = new FileWriter(file);
            final PrintWriter writer = new PrintWriter(fwriter);
            final List<String> classpath = filterClasspath(buildClasspath(), false);
            startScript(writer);
            writeClasspath(writer, classpath);
            writeCommand(writer);
            writer.close();
            fwriter.close();
            final int exitValue = executeProcess(gwtHome, new String[] { file.getAbsolutePath() });
            if (exitValue != 0) {
                throw new MojoExecutionException("GWT compilation failed." + " Exit value: " + exitValue);
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoFailureException("GWT compilation failed.");
        }
    }

    /**
     * Ecriture de la commande.
     * @param writer .
     * @param urls .
     * @throws IOException .
     */
    private void writeCommand(PrintWriter writer) throws IOException {
        writer.write("java");
        if (isMac()) {
            writer.write(" -XstartOnFirstThread");
        }
        if (maxmem != null) {
            writer.write(" -Xmx" + maxmem);
        }
        if (isWindows()) {
            writer.write(" -cp %CLASSPATH%");
        }
        if (debugIsEnable) {
            writer.write(" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,address=");
            writer.write(String.valueOf(debugport == -1 ? DEFAULT_DEBUG_PORT : debugport));
            writer.write(",suspend=y ");
        }
        writer.write(" com.google.gwt.dev.HostedMode");
        if (port != -1) {
            writer.write(" -port " + port);
        }
        if (noserver) {
            writer.write(" -noserver");
        }
        if (getLog().isDebugEnabled()) {
            writer.write(" -logLevel DEBUG");
        }
        if (style != null) {
            writer.write(" -style " + style);
        }
        if (url != null && !"".equals(url)) {
            writer.write(" -startupUrl " + url);
        }
        if (module != null && !"".equals(module)) {
            writer.write(" " + module);
        }
        writer.println();
    }

    /**
     * Extracts the native libs from the .zip found on the classpath.
     * @throws MojoExecutionException
     */
    private void extractNativeLibs() throws MojoExecutionException, IOException {
        final URL url = findNativeZip();
        if (url == null) {
            throw new MojoExecutionException("No classloader is of type URLClassLoader");
        }
        if (!"file".equals(url.getProtocol())) {
            throw new MojoExecutionException("Native library zip is not a " + "file URL: " + url.toString());
        }
        Zip.decompress(new File(url.getPath()), false);
    }

    /**
     * Finds the native zip on the classpath, first looks at the thread context classloader, then the class classloader.
     * @return the URL for the found zip or null if not found.
     * @throws MojoExecutionException
     */
    private URL findNativeZip() {
        URL url = null;
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();
        if (!(cl instanceof URLClassLoader)) {
            getLog().info("Context ClassLoader is not a URLClassLoader");
        } else {
            url = findZip((URLClassLoader) cl);
        }
        if (url != null) {
            return url;
        }
        cl = getClass().getClassLoader();
        if (!(cl instanceof URLClassLoader)) {
            getLog().info("Local ClassLoader is not a URLClassLoader");
        } else {
            url = findZip((URLClassLoader) cl);
        }
        if (url != null) {
            return url;
        }
        return null;
    }

    /**
     * Goes through the URLs in a URLClassLoader and find the entry matching the name in ZIPREGEX.
     * @param cl the classloader to inspect.
     * @return the found .zip or null.
     */
    private URL findZip(URLClassLoader cl) {
        final URL[] urlsInternal = cl.getURLs();
        for (int i = 0; i < urlsInternal.length; i++) {
            if (isNativeLibZip(urlsInternal[i].toString())) {
                return urlsInternal[i];
            }
        }
        return null;
    }
}
