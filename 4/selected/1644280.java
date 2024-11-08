package redora.generator;

import freemarker.cache.FileTemplateLoader;
import freemarker.core.ParseException;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import redora.generator.Template.Destination;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import static java.io.File.separator;
import static java.io.File.separatorChar;
import static redora.generator.GenerateMojo.GENERATION_TARGET;
import static redora.generator.XMLUtil.xsltTransform;

/**
 * This is a wrapper for the different template engines. Now we can use Freemarker,
 * XSLT and file copy.
 *
 * @author Nanjing RedOrange (http://www.red-orange.cn)
 */
class TemplateProcessor {

    final Configuration freemarkerConf = new Configuration();

    final FileLocations where;

    /**
     * @param where (Mandatory) A set of directories: for example the model location
     * @throws ModelGenerationException When there are no models
     */
    public TemplateProcessor(@NotNull FileLocations where) throws ModelGenerationException {
        this.where = where;
        prepareFreemarker();
    }

    void prepareFreemarker() throws ModelGenerationException {
        File tplDir = new File(where.templatesDir);
        try {
            freemarkerConf.setTemplateLoader(new FileTemplateLoader(tplDir));
        } catch (IOException e) {
            throw new ModelGenerationException("Can't load template from " + tplDir.getAbsolutePath() + ", it exists " + tplDir.exists() + ". Make sure you have set the Maven Dependency plugin correctly for Redora Templates", e);
        }
        freemarkerConf.setTemplateUpdateDelay(9000);
    }

    /**
     * Creates the generated source file based on given template and model
     * document.
     */
    public void process(@NotNull Template tpl, @NotNull Document model, @NotNull String packageName, @NotNull String outFileName, Map<String, String> xsltParam, String artifact) throws ModelGenerationException {
        System.out.print("Processing with " + tpl);
        String destinationPath;
        switch(tpl.destination) {
            case target:
                if (tpl.path == null) {
                    destinationPath = where.buildDir + separatorChar + "generated-sources" + separatorChar + GENERATION_TARGET;
                } else {
                    destinationPath = where.buildDir;
                }
                break;
            case source:
                if (tpl.path == null) {
                    destinationPath = where.sourceDir;
                } else {
                    destinationPath = "src";
                }
                break;
            case redora:
                destinationPath = where.redoraDir;
                break;
            default:
                throw new IllegalArgumentException("Unused destination " + tpl.destination);
        }
        if (tpl.path == null) {
            if (tpl.destination == Destination.redora) destinationPath += separator + artifact; else destinationPath += separator + packageName.replace('.', separatorChar);
        } else {
            destinationPath += separator + tpl.path.replace('/', separatorChar).replace('\\', separatorChar);
        }
        System.out.println(" to " + destinationPath + "..." + outFileName);
        if (tpl.destination == Destination.source) {
            if (new File(destinationPath, outFileName).exists()) {
                System.out.println("Stub " + outFileName + " already exists.");
                return;
            }
        }
        new File(destinationPath).mkdirs();
        InputStream in = null;
        Writer out;
        try {
            out = new FileWriter(new File(destinationPath, outFileName));
        } catch (IOException e) {
            throw new ModelGenerationException("Can't find: " + destinationPath + separatorChar + outFileName, e);
        }
        switch(tpl.type) {
            case freemarker:
                Map<String, NodeModel> root = new HashMap<String, NodeModel>();
                root.put("doc", NodeModel.wrap(model));
                try {
                    freemarker.template.Template template = freemarkerConf.getTemplate(tpl.getTemplateFileName());
                    template.process(root, out);
                } catch (ParseException e) {
                    throw new ModelGenerationException("There is an error in template: " + tpl + ". I found it when generating " + outFileName, e);
                } catch (IOException e) {
                    throw new ModelGenerationException("Can't find '" + tpl + "' when generating " + outFileName, e);
                } catch (TemplateException e) {
                    throw new ModelGenerationException("There is an error in template: " + tpl + ". I found it when generating " + outFileName, e);
                } catch (RuntimeException e) {
                    throw new ModelGenerationException("There is another error while trying this template: " + tpl + ". I found it when generating " + outFileName, e);
                }
                break;
            case xslt:
                try {
                    in = new FileInputStream(tpl.getAbsolutePath());
                    xsltTransform(model.getFirstChild(), in, out, xsltParam);
                } catch (FileNotFoundException e) {
                    throw new ModelGenerationException("Can't find " + tpl, e);
                } catch (TransformerException e) {
                    throw new ModelGenerationException("Sorry, i failed to use this template: " + tpl + ". It broke when generating " + outFileName, e);
                } finally {
                    IOUtils.closeQuietly(in);
                }
                break;
            case copy:
                try {
                    in = new FileInputStream(tpl.getAbsolutePath());
                    IOUtils.copy(in, out);
                } catch (IOException e) {
                    throw new ModelGenerationException("File copy failed " + tpl.getTemplateFileName(), e);
                } finally {
                    IOUtils.closeQuietly(in);
                }
        }
        IOUtils.closeQuietly(out);
    }
}
