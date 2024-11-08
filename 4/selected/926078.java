package org.granite.generator.as3;

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
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import org.granite.generator.GenerationException;
import org.granite.generator.Generator;
import org.granite.generator.GeneratorLogger;
import org.granite.generator.gsp.GroovyTemplateEngine;
import org.granite.generator.reflect.JClass;

public class As3BeanGenerator implements Generator {

    private static final String DEFAULT_TEMPLATE_PATH = "org/granite/generator/template/";

    private static final String DEFAULT_TEMPLATE_EXT = ".gsp";

    public static final String BASE = "Base";

    public static final String AS_FILE_EXT = ".as";

    public static final String ENTITY_TID = "entity";

    public static final String ENTITY_BASE_TID = ENTITY_TID + BASE;

    public static final String INTERFACE_TID = "interface";

    public static final String INTERFACE_BASE_TID = INTERFACE_TID + BASE;

    public static final String GENERIC_TID = "generic";

    public static final String GENERIC_BASE_TID = GENERIC_TID + BASE;

    private final GroovyTemplateEngine templateEngine;

    private final GeneratorLogger log;

    private final Java2As3 j2As3;

    private final Map<Class<?>, JClass> jClasspath;

    private final String uidFieldName;

    private final Map<String, Template> templates = new HashMap<String, Template>();

    public As3BeanGenerator(GeneratorLogger log, Map<Class<?>, JClass> jClasspath, Java2As3 java2As3, String uidFieldName) {
        this.templateEngine = new GroovyTemplateEngine();
        this.log = log;
        this.jClasspath = jClasspath;
        this.j2As3 = java2As3;
        this.uidFieldName = uidFieldName;
    }

    public void setTemplateScript(String templateId, String script) {
        try {
            templates.put(templateId, templateEngine.createTemplate(script));
        } catch (Exception e) {
            String message = "Could not create template for " + templateId + " with:\n" + script;
            log.error(message, e);
            throw new GenerationException(message, e);
        }
    }

    public int generate(JClass jClass, String baseDir) {
        Class<?> type = jClass.getType();
        long jTime = jClass.getFile().lastModified();
        if (baseDir == null || baseDir.length() == 0) baseDir = ".";
        final String path = (baseDir + File.separatorChar + type.getPackage().getName().replace('.', File.separatorChar) + File.separatorChar);
        if (type.isInterface()) return generate(jClass, jTime, path, INTERFACE_TID, true);
        if (type.isAnnotationPresent(Entity.class) || type.isAnnotationPresent(MappedSuperclass.class)) return generate(jClass, jTime, path, ENTITY_TID, true);
        return generate(jClass, jTime, path, GENERIC_TID, true);
    }

    protected int generate(JClass jClass, long jTime, String path, String templateId, boolean withBase) {
        Map<String, Object> binding = new HashMap<String, Object>();
        binding.put("gVersion", Generator.VERSION);
        binding.put("jClasspath", jClasspath);
        binding.put("jClass", jClass);
        binding.put("j2As3", j2As3);
        binding.put("withBase", Boolean.valueOf(withBase));
        binding.put("uidFieldName", uidFieldName);
        int count = 0;
        File file = new File(path + jClass.getType().getSimpleName() + AS_FILE_EXT);
        if (!file.exists() || (!withBase && file.lastModified() < jTime)) {
            generate(getTemplate(templateId), binding, file);
            count++;
        }
        file = new File(path + jClass.getType().getSimpleName() + BASE + AS_FILE_EXT);
        if (withBase && (!file.exists() || file.lastModified() < jTime)) {
            generate(getTemplate(templateId + BASE), binding, file);
            count++;
        }
        return count;
    }

    protected void generate(Template template, Map<String, Object> binding, File output) {
        log.info("    Writing AS3 bean: " + output.toString());
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

    protected Template getTemplate(String templateId) {
        Template template = templates.get(templateId);
        if (template == null) {
            template = getResourceTemplate(templateId);
            templates.put(templateId, template);
        }
        return template;
    }

    protected Template getResourceTemplate(String name) {
        String path = DEFAULT_TEMPLATE_PATH + name + DEFAULT_TEMPLATE_EXT;
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
