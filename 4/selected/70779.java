package org.jtestcase.plugin.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

public class VelocityStandardEngine {

    private static final boolean THROW_ON_FAILURE = true;

    public VelocityStandardEngine() {
        Velocity.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
        try {
            Velocity.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String applyTemplate(String templateResourceName, VelocityContext context) throws ParseErrorException, MethodInvocationException, ResourceNotFoundException, IOException {
        StringWriter writer = new StringWriter();
        StringWriter template = new StringWriter();
        InputStream input = load(templateResourceName);
        InputStreamReader reader = new InputStreamReader(input);
        int c;
        try {
            while ((c = reader.read()) != -1) template.write(c);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Velocity.evaluate(context, writer, "LOG", template.toString());
        return writer.toString();
    }

    public static void main(String[] args) throws Exception {
        VelocityContext context = new VelocityContext();
        context.put("name", new String("Velocity"));
        VelocityStandardEngine bridge = new VelocityStandardEngine();
        System.out.println("output : " + bridge.applyTemplate("jtestcase/template/generate.vm", context));
    }

    private static InputStream load(String name, ClassLoader loader) {
        if (name == null) {
            throw new IllegalArgumentException("null resource name");
        }
        InputStream in = null;
        try {
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
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
