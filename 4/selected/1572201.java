package net.sf.bootstrap.generator.template;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import net.sf.bootstrap.generator.util.VelocityHelper;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeSingleton;

/**
 *
 * @author Mark Moloney
 */
public class VelocityTemplateEngine implements TemplateEngine {

    private String encoding;

    public VelocityTemplateEngine() throws TemplateEngineException {
        try {
            Velocity.init();
        } catch (Exception ex) {
            throw new TemplateEngineException("Error initialising Velocity: " + ex.getMessage(), ex);
        }
        this.encoding = RuntimeSingleton.getString(Velocity.INPUT_ENCODING, Velocity.ENCODING_DEFAULT);
    }

    public void mergeTemplate(File template, Map vars, File out) throws TemplateEngineException {
        mergeTemplate(template.getAbsolutePath(), vars, out);
    }

    public void mergeTemplate(String templatePath, Map vars, File out) throws TemplateEngineException {
        VelocityContext vc = VelocityHelper.createVelocityContext(vars);
        try {
            FileWriter writer = new FileWriter(out);
            Velocity.mergeTemplate(templatePath, encoding, vc, writer);
            writer.close();
        } catch (Exception ex) {
            throw new TemplateEngineException("Error merging template " + templatePath + ": " + ex.getMessage(), ex);
        }
    }

    public void mergeTemplate(Reader reader, Map vars, File out) throws TemplateEngineException {
        VelocityContext vc = VelocityHelper.createVelocityContext(vars);
        try {
            FileWriter writer = new FileWriter(out);
            Velocity.evaluate(vc, writer, out.getName(), reader);
            writer.close();
        } catch (Exception ex) {
            System.out.println(">>> " + out.getAbsolutePath());
            throw new TemplateEngineException("Error merging template: " + ex.getMessage(), ex);
        }
    }
}
