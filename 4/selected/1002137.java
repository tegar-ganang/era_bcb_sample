package org.granite.generator.gsp.ejb3.hibernate;

import groovy.lang.Writable;
import groovy.text.Template;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.granite.generator.AntLogger;
import org.granite.generator.GenerationException;
import org.granite.generator.Generator;
import org.granite.generator.gsp.GroovyTemplateEngine;

public class AS3BeanGenerator implements Generator {

    private final GroovyTemplateEngine templateEngine;

    private final AntLogger log;

    private final String id;

    private final String uid;

    private Template entityTemplate;

    private Template entityBaseTemplate;

    public AS3BeanGenerator(AntLogger log, String id, String uid) {
        this.templateEngine = new GroovyTemplateEngine();
        this.log = log;
        this.id = id;
        this.uid = uid;
    }

    public void setEntityScript(String script) {
        if (script == null) entityTemplate = null; else {
            try {
                entityTemplate = templateEngine.createTemplate(script);
            } catch (Exception e) {
                log.error("", e);
                throw new GenerationException("Could not create template for: " + script, e);
            }
        }
    }

    public void setEntityBaseScript(String script) {
        if (script == null) entityBaseTemplate = null; else {
            try {
                entityBaseTemplate = templateEngine.createTemplate(script);
            } catch (Exception e) {
                log.error("", e);
                throw new GenerationException("Could not create base template for: " + script, e);
            }
        }
    }

    public void generate(Class<?> jType, long jTime, String baseDir) {
        if (baseDir == null || baseDir.length() == 0) baseDir = ".";
        String path = baseDir + File.separatorChar + jType.getPackage().getName().replace('.', File.separatorChar) + File.separatorChar;
        Map<String, Object> binding = new HashMap<String, Object>();
        binding.put("jType", jType);
        binding.put("id", id);
        binding.put("uid", uid);
        File file = new File(path + jType.getSimpleName() + ".as");
        if (!file.exists()) generate(getEntityTemplate(), binding, file);
        file = new File(path + jType.getSimpleName() + "Base.as");
        if (!file.exists() || file.lastModified() < jTime) generate(getEntityBaseTemplate(), binding, file);
    }

    private void generate(Template template, Map<String, Object> binding, File output) {
        log.info("Writing AS3 bean: " + output.toString());
        try {
            output.getParentFile().mkdirs();
            Writer writer = null;
            try {
                Writable writable = template.make(binding);
                String result = writable.toString();
                writer = new BufferedWriter(new FileWriter(output));
                writer.write(result);
            } finally {
                if (writer != null) writer.close();
            }
        } catch (Exception e) {
            throw new GenerationException("Could not generate AS3 bean to: " + output, e);
        }
    }

    private Template getEntityTemplate() {
        if (entityTemplate == null) entityTemplate = getResourceTemplate("entity.gsp");
        return entityTemplate;
    }

    private Template getEntityBaseTemplate() {
        if (entityBaseTemplate == null) entityBaseTemplate = getResourceTemplate("entityBase.gsp");
        return entityBaseTemplate;
    }

    private Template getResourceTemplate(String name) {
        String path = AS3BeanGenerator.class.getPackage().getName().replace('.', '/') + '/' + name;
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null) throw new GenerationException("Resource not found exception: " + path);
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"));
            StringWriter sw = new StringWriter();
            int c = -1;
            while ((c = reader.read()) != -1) sw.write(c);
            return templateEngine.createTemplate(sw.toString());
        } catch (Exception e) {
            throw new GenerationException("Could not create template for: " + path, e);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
    }
}
