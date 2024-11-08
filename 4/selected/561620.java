package com.csam.syringe;

import com.csam.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * This class represents a registry of objects, dependencies, properties,
 * and everything else which pertains to the day-to-day operation of the
 * Syringe library.
 *
 * @author Nathan Crause <ncrause at clarkesolomou.com>
 */
public final class Registry {

    public static String GLOBAL_ENVIRONMENT = "global".intern();

    private Map<String, Environment> environments;

    /**
     * Simple do-nothing constructor. Simply initializes all the objects
     */
    public Registry() {
        __init();
    }

    /**
     * Internal use method which initializes all the instance variables.
     */
    protected void __init() {
        environments = new HashMap<String, Environment>();
        createEnvironment(GLOBAL_ENVIRONMENT);
    }

    /**
     * Adds a new named environment to the internal list of known environments.
     * 
     * @param name the name of the environment
     * @param env the environment to add to the internal list
     */
    public void addEnvironment(String name, Environment env) {
        environments.put(name, env);
    }

    /**
     * Creates a new named environment, and returns the newly created instance.
     *
     * @param name the name of the environment to create
     * @return the newly created (and added) environment
     * @see #addEnvironment(java.lang.String, com.csam.syringe.Environment) 
     */
    public Environment createEnvironment(String name) {
        return environments.put(name, new Environment());
    }

    private String workingEnvironment;

    /**
     * Get the value of workingEnvironment
     *
     * @return the value of workingEnvironment
     */
    public String getWorkingEnvironment() {
        return workingEnvironment;
    }

    /**
     * Set the value of workingEnvironment
     *
     * @param workingEnvironment new value of workingEnvironment
     */
    public void setWorkingEnvironment(String workingEnvironment) {
        if (workingEnvironment.equals(GLOBAL_ENVIRONMENT)) throw new RuntimeException("You cannot specify the global environment as your " + "working environment - it is used only in conjunction " + "with other environments.");
        this.workingEnvironment = workingEnvironment;
    }

    /**
     * This method tries to automatically detect the environment. When
     * searching for possible environment identifies, the environment (or other)
     * variable names will be prefixed by the <code>appPrefix</code>
     * parameter. This helps keep application environments independent of each
     * other.
     *
     * @param appPrefix the string prefix to apply to named environment markers
     * (may be null if no prefix is to be used)
     */
    public void determineWorkingEnvironment(String appPrefix) {
        setWorkingEnvironment(getWorkingEnvironment(appPrefix));
    }

    private String generateName(String appPrefix, String name) {
        StringBuilder builder = new StringBuilder();
        if (appPrefix != null) {
            builder.append(appPrefix);
            builder.append("_");
        }
        builder.append(name);
        return builder.toString();
    }

    private String generateEnvName(String appPrefix) {
        return generateName(appPrefix, "ENV".intern());
    }

    private String getWorkingEnvironment(String appPrefix) {
        return getWorkingEnvironmentFromSystemProperties(appPrefix);
    }

    private String getWorkingEnvironmentFromSystemProperties(String appPrefix) {
        String result = System.getProperty(generateEnvName(appPrefix));
        return result != null ? result : getWorkingEnvironmentFromOSEnvironment(appPrefix);
    }

    private String getWorkingEnvironmentFromOSEnvironment(String appPrefix) {
        Map<String, String> env = System.getenv();
        String name = generateEnvName(appPrefix);
        return env.containsKey(name) ? env.get(name) : "development".intern();
    }

    /**
     * Convenience method which performs the same task as
     * <code>determineWorkingEnvironment(String)</code> but without an
     * app prefix.
     */
    public void determineWorkingEnvironment() {
        determineWorkingEnvironment(null);
    }

    /**
     * Returns the named environment as it exists in the internal list.
     *
     * @param name the name of the environment
     * @return the environment instance
     * @throws com.csam.syringe.UnknownEnvironmentException if the named
     * environment hasn't been registered
     */
    public Environment getEnvironment(String name) throws UnknownEnvironmentException {
        if (!environments.containsKey(name)) throw new UnknownEnvironmentException("The named environment '" + name + "' does not appear" + "to have been defined.");
        return environments.get(name);
    }

    /**
     * Returns the working environment, as determined by
     * <code>determineWorkingEnvironment()</code> or manually specified by
     * <code>setWorkingEnvironment(String)</code>.
     *
     * @return the working environment
     * @throws com.csam.syringe.UndeterminedEnvironmentException if no 
     * working environment has been specified/detected
     * @throws com.csam.syringe.UnknownEnvironmentException if the current
     * working environment name hasn't been registered
     */
    public Environment getEnvironment() throws UndeterminedEnvironmentException, UnknownEnvironmentException {
        if (getWorkingEnvironment() == null) throw new UndeterminedEnvironmentException("The working environment has not been determined. " + "You may be missing either a " + "'determineWorkingEnvironment(String)' or a " + "'setWorkingEnvironment(String)' invocation in " + "your application.");
        return getEnvironment(getWorkingEnvironment());
    }

    /**
     * Configures this registry using an XML document
     *
     * @param xml the XML document to parse
     */
    public void configure(Document xml) {
    }

    /**
     * Configures this registry using a JSON "document"
     *
     * @param json the JSON object to parse
     */
    public void configure(JSONObject json) {
    }

    /**
     * Configures this registry using the input stream to read an XML document.
     *
     * @param in the stream from which to read the XML document
     * @throws IOException if something goes wrong while reading
     */
    public void configureXML(InputStream in) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setCoalescing(true);
        factory.setIgnoringComments(true);
        factory.setValidating(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(in);
        configure(doc);
    }

    /**
     * Configures this registry using the input stream to read a JSON document.
     *
     * @param in the stream from which to read the JSON document
     * @throws IOException if something goes wrong while reading
     */
    public void configureJSON(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        configureJSON(out.toString());
    }

    /**
     * Configures this registry using a string which contains an XML
     * document.
     *
     * @param body the string containing the XML document
     */
    public void configureXML(String body) throws ParserConfigurationException, SAXException {
        InputStream in = new ByteArrayInputStream(body.getBytes());
        try {
            configureXML(in);
        } catch (IOException ex) {
            throw new RuntimeException("Unexpected I/O exception", ex);
        }
    }

    /**
     * Configure this registry using a string which contains a JSON document.
     *
     * @param body the string containing the JSON document
     */
    public void configureJSON(String body) {
        configure(JSONObject.valueOf(body));
    }
}
