package org.easygen.core.generators;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.util.StringUtils;
import org.easygen.core.InitException;
import org.easygen.core.config.DataObject;
import org.easygen.core.config.Dependency;
import org.easygen.core.config.ModuleConfig;
import org.easygen.core.config.ProjectConfig;
import org.easygen.core.generators.velocity.VelocityStackUtils;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * @author eveno
 * Created on 2 nov. 06
 * TODO Removed useless javadocs comments in all templates (mainly hibernate POJOs)
 */
public abstract class AbstractGenerator implements Generator, GeneratorConstants {

    protected static final String LIBRARY_EXTENSION = ".jar";

    protected static final String LIBRARY_INDEX_FILE = "dir.list";

    private static final Logger logger = Logger.getLogger(AbstractGenerator.class);

    protected VelocityContext context = null;

    protected Map<String, Template> templates;

    private CompositeConfiguration configuration = new CompositeConfiguration();

    public AbstractGenerator() {
        super();
        loadConfiguration();
        templates = new Hashtable<String, Template>();
    }

    /**
	 * 
	 */
    protected void loadConfiguration() {
        addConfiguration("cfg/generators.properties");
        String moduleConfigFile = getModuleDir() + getModuleName() + PROPERTY_FILE_EXTENSION;
        logger.debug("Trying to load module specific configuration file: " + moduleConfigFile);
        addConfiguration(moduleConfigFile);
    }

    /**
	 * @param filename
	 * @throws InitException
	 */
    protected void addConfiguration(String filename) throws InitException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(filename);
        if (resource != null) {
            logger.debug("Loading module specific configuration from: " + filename);
            try {
                configuration.addConfiguration(new PropertiesConfiguration(resource));
            } catch (ConfigurationException e) {
                throw new InitException("Can't load configuration file: " + resource.toString(), e);
            }
        }
    }

    /**
	 * @return the configuration
	 */
    protected CompositeConfiguration getConfiguration() {
        return configuration;
    }

    /**
	 * @see org.easygen.core.generators.Generator#init()
	 */
    public void init(ProjectConfig projectConfig) {
        try {
            logger.debug("Initializing Velocity");
            initVelocity();
            context.put("stringutils", new StringUtils());
            if (projectConfig != null) {
                context.put(PROJECT_INFO, projectConfig);
            }
        } catch (Exception e) {
            throw new InitException("Velocity initialization failed", e);
        }
    }

    /**
	 * @throws Exception
	 */
    protected void initVelocity() throws Exception {
        Properties properties = new Properties();
        InputStream inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("cfg/velocity.properties");
        if (inStream == null) throw new GenerationException("Velocity initialization failed");
        properties.load(inStream);
        Velocity.init(properties);
        context = new VelocityContext();
    }

    /**
	 * @param filename
	 * @return
	 * @throws ResourceNotFoundException
	 * @throws ParseErrorException
	 * @throws Exception
	 */
    protected Template loadTemplate(String filename) throws InitException {
        String templateFilename = getTemplateDir() + filename;
        return internalLoadTemplate(templateFilename);
    }

    private Template internalLoadTemplate(String templateFilename) {
        try {
            logger.debug("Loading Template file: " + templateFilename);
            Template template = Velocity.getTemplate(templateFilename);
            return template;
        } catch (ResourceNotFoundException e) {
            throw new InitException("Velocity file template not found: " + templateFilename, e);
        } catch (ParseErrorException e) {
            throw new InitException("Velocity template parse error: " + templateFilename, e);
        } catch (Exception e) {
            throw new InitException("Velocity template initialization failed: " + templateFilename, e);
        }
    }

    /**
	 * @param filename
	 * @return
	 * @throws GenerationException
	 */
    protected Template getTemplate(String filename) throws GenerationException {
        if (templates.containsKey(filename)) {
            return templates.get(filename);
        }
        Template template = loadTemplate(filename);
        templates.put(filename, template);
        return template;
    }

    /**
     * @see org.easygen.core.generators.Generator#generate()
     */
    public abstract void generate(ProjectConfig projectConfig) throws GenerationException;

    protected void copyFile(String inputFilePath, String outputFilePath) throws GenerationException {
        String from = getTemplateDir() + inputFilePath;
        try {
            logger.debug("Copying from " + from + " to " + outputFilePath);
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(from);
            if (inputStream == null) {
                throw new GenerationException("Source file not found: " + from);
            }
            FileOutputStream outputStream = new FileOutputStream(new File(outputFilePath));
            IOUtils.copy(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            throw new GenerationException("Error while copying file: " + from, e);
        }
    }

    protected void generateFile(String templateFilename, String outputFilePath) throws GenerationException {
        generateFile(getTemplate(templateFilename), outputFilePath);
    }

    private void generateFile(Template template, String outputFilePath) throws GenerationException {
        Validate.notNull(template, "Can't generate file : No given template");
        logger.debug("Generating file: " + outputFilePath);
        try {
            generateToWriter(template, new FileWriter(outputFilePath));
        } catch (IOException e) {
            throw new GenerationException("Error creating file: " + outputFilePath, e);
        }
    }

    private void generateToWriter(Template template, Writer writer) throws GenerationException {
        try {
            template.merge(context, writer);
        } catch (Exception e) {
            throw new GenerationException("Error while generating file from template: " + template.getName(), e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    /**
     * @see org.easygen.core.generators.Generator#postProcess(ProjectConfig)
     */
    public abstract void postProcess(ProjectConfig projectConfig) throws GenerationException;

    /**
	 * Convert package to path with trailing path separator
	 * Ex: java.lang -> java/lang/
	 */
    protected String convertPackageNameToPath(String packageName) {
        return packageName.replace('.', File.separatorChar) + File.separatorChar;
    }

    protected void createPath(String pPath) throws GenerationException {
        createPath(new File(pPath));
    }

    protected void createPackagePath(String pPath, String packageName) throws GenerationException {
        Validate.notNull("packageName in createPackagePath", packageName);
        String packagePath = convertPackageNameToPath(packageName);
        createPath(pPath, packagePath);
    }

    protected void createPath(String pPath, String subPath) throws GenerationException {
        createPath(new File(pPath, subPath));
    }

    protected void createPath(File pFile) throws GenerationException {
        if (!pFile.exists()) {
            boolean result = pFile.mkdirs();
            if (!result) throw new GenerationException("G�n�ration impossible", new IOException("Impossible de cr�er le r�pertoire : " + pFile.toString()));
        }
    }

    protected String createJavaFilename(ModuleConfig moduleConfig, DataObject object) {
        return createFilename(moduleConfig, object, JAVA_FILE_EXTENSION);
    }

    protected String createJavaFilename(ModuleConfig moduleConfig, String baseName) {
        return createFilename(moduleConfig, baseName, JAVA_FILE_EXTENSION);
    }

    protected String createFilename(ModuleConfig moduleConfig, DataObject object, String extension) {
        return createFilename(moduleConfig, object.getClassName(), extension);
    }

    protected String createFilename(ModuleConfig moduleConfig, String baseName, String extension) {
        String packageName = moduleConfig.getPackageName();
        String packagePath = convertPackageNameToPath(packageName);
        String baseFilename = packagePath + baseName;
        String filename = baseFilename + extension;
        return filename;
    }

    protected void delete(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
	 * Adds {@link VelocityStackUtils} the the Velocity context under "stack" variable name.
	 */
    protected void addStackToVelocityContext() {
        context.put(STACK, new VelocityStackUtils());
    }

    /**
	 * Removes {@link VelocityStackUtils} the the Velocity context
	 */
    protected void removeStackFromVelocityContext() {
        context.remove(STACK);
    }

    protected String getConfigurationDir() {
        String configDir = configuration.getString(CONFIG_DIR_KEY);
        return configDir + SEPARATOR_CHAR;
    }

    protected String getModuleDir() {
        return getConfigurationDir() + getModuleName() + SEPARATOR_CHAR;
    }

    protected String getTemplateDir() {
        return getModuleDir() + TEMPLATE_DIR + SEPARATOR_CHAR;
    }

    protected String getLibraryDir() {
        return getModuleDir() + LIBRARY_DIR + SEPARATOR_CHAR;
    }

    @SuppressWarnings("unchecked")
    public void addMavenDependencies(ProjectConfig projectConfig) throws GenerationException {
        String dependencyFile = getModuleDir() + "dependencies.xml";
        Template template = null;
        try {
            logger.debug("Loading Template file: " + dependencyFile);
            template = Velocity.getTemplate(dependencyFile);
        } catch (ResourceNotFoundException e) {
            return;
        } catch (ParseErrorException e) {
            throw new InitException("Velocity template parse error: " + dependencyFile, e);
        } catch (Exception e) {
            throw new InitException("Velocity template initialization failed: " + dependencyFile, e);
        }
        StringWriter writer = new StringWriter();
        generateToWriter(template, writer);
        XStream xstream = new XStream(new DomDriver());
        xstream.alias("dependencies", LinkedList.class);
        xstream.alias("dependency", Dependency.class);
        List<Dependency> dependencies = (List<Dependency>) xstream.fromXML(writer.toString());
        for (Dependency dependency : dependencies) {
            logger.debug(dependency);
        }
        projectConfig.addMavenDependencies(dependencies);
    }

    /**
     * @return
     */
    protected abstract String getModuleName();
}
