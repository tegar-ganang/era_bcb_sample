package org.jtestcase.plugin.testgen4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.jtestcase.plugin.JTestCasePlugin;
import com.spikesource.spiketestgen.generators.IVelocityStandardEngine;

public class VelocityStandardEngine implements IVelocityStandardEngine {

    private static final boolean THROW_ON_FAILURE = true;

    public VelocityStandardEngine() {
        Velocity.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
        try {
            Velocity.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void applyTemplate(String templateFilePath, VelocityContext context, Writer writer) throws ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException {
        StringWriter template = new StringWriter();
        InputStream input = null;
        if (JTestCasePlugin.getDefault() != null) {
            input = JTestCasePlugin.getDefault().getFileAsStream("./res" + templateFilePath);
        }
        if (input == null) {
            input = getClass().getResourceAsStream(templateFilePath);
        }
        InputStreamReader reader = new InputStreamReader(input);
        context.put("FormatterUtil", new FormatterUtil());
        int c;
        try {
            while ((c = reader.read()) != -1) template.write(c);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Velocity.evaluate(context, writer, "LOG", template.toString());
    }

    public static void main(String[] args) throws Exception {
        VelocityContext context = new VelocityContext();
        context.put("name", new String("Velocity"));
        VelocityStandardEngine bridge = new VelocityStandardEngine();
        StringWriter sw = new StringWriter();
        bridge.applyTemplate("jtestcase/template/generate.vm", context, sw);
        System.out.println("Output : " + sw.toString());
    }

    private static InputStream load(String name, ClassLoader loader) {
        if (name == null) {
            throw new IllegalArgumentException("null resource name");
        }
        InputStream in = null;
        try {
            if (loader == null) {
            }
            in = loader.getResourceAsStream(name);
        } catch (Exception e) {
            e.printStackTrace();
            in = null;
        } finally {
        }
        if (THROW_ON_FAILURE && in == null) {
            throw new IllegalArgumentException("couldn't load  resource [" + name + "]");
        }
        return in;
    }

    public static InputStream load(String name) {
        return load(name, Thread.currentThread().getContextClassLoader());
    }
}
