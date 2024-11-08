package com.qwantech.diesel;

import com.qwantech.diesel.util.*;
import com.qwantech.diesel.util.DirectoryTraverser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * This class uses a cartridge directory and a domain model to create an
 * output directory populated with generated files. If the domain format
 * is set to null, the FileGenerator will use the <code>default.xsl</code>
 * translation file in the cartridge's <code>META-INF/Domains</code>
 * directory.
 * <p>
 * Usage:
 * <blockquote>
 * <pre>
File cartridgeDir = new File("./SampleCartridge");
File domainModelFile = new File("./SampleDomainModel.xml");
String domainModelFormat = "XMI"; // uses META-INF/Domains/XMI.xsl in cartridge
File outputDir = new File("./build/output");
try {
    FileGenerator gen = new FileGenerator(dir);
    gen.buildFiles(domainModelFile, domainModelFormat, outputDir);
}
catch (ApplicationException e) {
    e.printStackTrace();
}
</pre>
 * </blockquote>
 * 
 * @see CartridgeConfigParser
 * @see CartridgeConfig
 * @see ModelTranslator
 * @see GenericModelParser
 * @see GenericModel
 * @see FileMapping
 */
public class FileGenerator {

    private Log log = LogFactory.getLog(getClass());

    protected File cartridgeDir;

    protected CartridgeConfig cartridgeConfig;

    protected String logfile = "velocity.log";

    /**
     * This constructor uses a CartridgeMappingParser to parse the
     * cartridge mapping file contained in the cartridge directory.
     * 
     * @param cartridgeDir Directory with cartridge contents
     * @throws ApplicationException if the cartridge cannot be loaded
     */
    public FileGenerator(File cartridgeDir) throws ApplicationException {
        this.cartridgeDir = cartridgeDir;
        CartridgeConfigParser parser = new CartridgeConfigParser();
        cartridgeConfig = parser.parseMappingConfig(cartridgeDir);
    }

    /**
     * This method generates output files using the loaded cartridge directory
     * with the specified model information. If the directory does not already
     * exist, it will be created. If the directory already exists, then new
     * files will be added and existing files will be overwritten. The model
     * format specified must be one that is defined in the cartridge directory.
     * 
     * @param domainModelFile
     * @param modelFormat
     * @param outputDir
     * @throws ApplicationException
     */
    public void buildFiles(File domainModelFile, String modelFormat, final File outputDir) throws ApplicationException {
        final GenericModel genericModel = getGenericModel(domainModelFile, modelFormat);
        final CartridgeConfig mappingConfig = this.cartridgeConfig;
        final VelocityContext context = createVelocityContext(genericModel, mappingConfig);
        final int prefixLength = mappingConfig.getInputBaseDir().length() + File.separator.length();
        makeOutputDirectory(outputDir);
        DirectoryTraverser traverser = new DirectoryTraverser() {

            public void handleFile(File file) {
                handleFileCallback(file, mappingConfig, context, genericModel, outputDir, prefixLength);
            }
        };
        traverser.traverse(new File(mappingConfig.getInputBaseDir()));
    }

    protected GenericModel getGenericModel(File domainModelFile, String modelFormat) throws ApplicationException {
        File metaDir = new File(cartridgeDir, "META-INF");
        if (!metaDir.exists()) {
            throw new ApplicationException("META-INF directory not found in cartridge");
        }
        File domainsDir = new File(metaDir, "Domains");
        if (!domainsDir.exists()) {
            throw new ApplicationException("META-INF/Domains directory not found in cartridge");
        }
        ModelTranslator translator = new ModelTranslator(domainsDir, modelFormat);
        File genericModelFile = translator.getGenericModelFile(domainModelFile);
        GenericModelParser parser = new GenericModelParser();
        GenericModel genericModel = parser.parse(genericModelFile);
        return genericModel;
    }

    protected void handleFileCallback(File file, CartridgeConfig mappingConfig, VelocityContext context, GenericModel model, File outputDir, int prefixLength) {
        if (mappingConfig.isIgnoredFile(file)) {
            return;
        }
        FileMapping mapping = null;
        mapping = mappingConfig.getTemplateMapping(file);
        if (mapping != null) {
            handleTemplateMapping(model, outputDir, context, prefixLength, mapping);
            return;
        }
        mapping = mappingConfig.getOpaqueMapping(file);
        if (mapping != null) {
            handleOpaqueMapping(model, outputDir, context, prefixLength, mapping);
            return;
        }
        mapping = new FileMapping(file.toString(), file.toString().substring(prefixLength));
        handleDefaultMapping(model, outputDir, context, prefixLength, mapping, mappingConfig);
    }

    protected void handleTemplateMapping(GenericModel model, File outputDir, VelocityContext context, int prefixLength, FileMapping mapping) {
        try {
            if (mapping.isCompound()) {
                String item = mapping.getItem();
                Collection objects = expandMapping(context, mapping);
                if (objects == null) {
                    objects = new java.util.ArrayList();
                    log.error("No value found for " + mapping);
                }
                Iterator i = objects.iterator();
                while (i.hasNext()) {
                    Object object = i.next();
                    context.put(item, object);
                    File output = makeOutputFilename(outputDir, mapping.getOut(), context, model);
                    makeParentDirectory(output);
                    buildFile(context, mapping.getIn().substring(prefixLength), output);
                    context.remove(object);
                }
            } else {
                File output = makeOutputFilename(outputDir, mapping.getOut(), context, model);
                makeParentDirectory(output);
                buildFile(context, mapping.getIn().substring(prefixLength), output);
            }
        } catch (ApplicationException e) {
            e.printStackTrace();
        }
    }

    protected void handleOpaqueMapping(GenericModel model, File outputDir, VelocityContext context, int prefixLength, FileMapping mapping) {
        File output = new File(outputDir + File.separator + mapping.getOut());
        makeParentDirectory(output);
        File input = new File(mapping.getIn());
        try {
            copyFile(input, makeOutputFilename(outputDir, mapping.getOut(), context, model));
        } catch (ApplicationException e) {
            e.printStackTrace();
        }
    }

    protected void handleDefaultMapping(GenericModel model, File outputDir, VelocityContext context, int prefixLength, FileMapping mapping, CartridgeConfig mappingConfig) {
        switch(mappingConfig.getDefaultType()) {
            case CartridgeConfig.TEMPLATE_TYPE:
                handleTemplateMapping(model, outputDir, context, prefixLength, mapping);
                break;
            case CartridgeConfig.OPAQUE_TYPE:
                handleOpaqueMapping(model, outputDir, context, prefixLength, mapping);
                break;
            case CartridgeConfig.IGNORED_TYPE:
                break;
            default:
                log.error("No default type to handle " + mapping.getOut());
                break;
        }
    }

    protected Collection expandMapping(VelocityContext context, FileMapping mapping) {
        StringTokenizer st = new StringTokenizer(mapping.getList(), ".");
        int count = st.countTokens();
        if (count > 1) {
            String token = st.nextToken();
            Map map = (Map) context.get(token);
            if (map == null) {
                log.error("No model data for " + token + " in " + mapping.getList());
                return new ArrayList(0);
            }
            for (int i = 1; i < (count - 1); ++i) {
                token = st.nextToken();
                map = (Map) context.get(token);
                if (map == null) {
                    log.error("No model data for " + token + " in " + mapping.getList());
                    return new ArrayList(0);
                }
            }
            return (Collection) map.get(st.nextToken());
        }
        return (Collection) context.get(mapping.getList());
    }

    protected void buildFile(VelocityContext context, String templateFilename, File outfile) throws ApplicationException {
        if (log.isDebugEnabled()) {
            log.debug("Creating " + outfile + " from " + templateFilename);
        }
        try {
            Template template = Velocity.getTemplate(templateFilename);
            if (template == null) {
                throw new ApplicationException("Template not created");
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(outfile));
            template.merge(context, writer);
            writer.flush();
            writer.close();
        } catch (ResourceNotFoundException e) {
            throw new ApplicationException("Can't find template: " + templateFilename, e);
        } catch (ParseErrorException e) {
            throw new ApplicationException("Error parsing template " + templateFilename + ": " + e.getMessage(), e);
        } catch (IOException e) {
            log.debug(e);
            log.error("Error writing output file: " + outfile);
        } catch (Exception e) {
            log.debug(e);
            log.error("Velocity substitution failed: " + e.getMessage());
        }
    }

    protected void copyFile(File source, File destination) throws ApplicationException {
        try {
            OutputStream out = new FileOutputStream(destination);
            DataInputStream in = new DataInputStream(new FileInputStream(source));
            byte[] buf = new byte[8192];
            for (int nread = in.read(buf); nread > 0; nread = in.read(buf)) {
                out.write(buf, 0, nread);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            throw new ApplicationException("Can't copy file " + source + " to " + destination);
        }
    }

    protected VelocityContext createVelocityContext(GenericModel model, CartridgeConfig mappingConfig) throws ApplicationException {
        VelocityContext context = null;
        try {
            Properties properties = new Properties();
            properties.setProperty("runtime.log", logfile);
            properties.setProperty("file.resource.loader.path", mappingConfig.getInputBaseDir());
            Velocity.init(properties);
            context = new VelocityContext();
            loadContext(context, mappingConfig.getNamespace(), model);
        } catch (Exception e) {
            throw new ApplicationException("Velocity initialization failed: " + e.getMessage(), e);
        }
        return context;
    }

    protected void loadContext(VelocityContext context, String namespace, GenericModel config) {
        if (namespace != null) {
            context.put(namespace, config.getMap());
        } else {
            Map map = config.getMap();
            Iterator i = map.keySet().iterator();
            while (i.hasNext()) {
                String key = (String) i.next();
                Object value = map.get(key);
                if (value != null) {
                    context.put(key, value);
                }
            }
        }
    }

    protected File makeOutputFilename(File dir, String raw, VelocityContext context, GenericModel model) {
        File defaultFile = new File(dir + File.separator + raw);
        if (raw.indexOf("${") == -1) {
            return defaultFile;
        }
        StringWriter writer = new StringWriter();
        try {
            Velocity.evaluate(context, writer, "LOG", raw);
        } catch (ParseErrorException e) {
            e.printStackTrace();
            return defaultFile;
        } catch (MethodInvocationException e) {
            e.printStackTrace();
            return defaultFile;
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
            return defaultFile;
        } catch (IOException e) {
            e.printStackTrace();
            return defaultFile;
        }
        return new File(dir + File.separator + writer.getBuffer());
    }

    protected void makeOutputDirectory(File dir) throws ApplicationException {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new ApplicationException("Can't create output directory '" + dir + "'");
            }
        } else {
            log.warn("Overwriting existing output directory " + dir);
        }
    }

    protected void makeParentDirectory(File outfile) {
        File parent = outfile.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                log.error("Can't create directory " + parent);
            }
        }
    }
}
