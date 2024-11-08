package org.sourceforge.jerd;

import java.io.Reader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.mozilla.javascript.tools.shell.Main;
import javax.script.*;

public class App {

    private static Logger log = Logger.getLogger(App.class);

    public static String render(String template, Object data) throws Exception {
        Reader reader = new InputStreamReader(App.class.getClassLoader().getResourceAsStream(template));
        VelocityContext context = new VelocityContext();
        context.put("data", data);
        StringWriter writer = new StringWriter();
        Velocity.evaluate(context, writer, "", reader);
        return writer.toString();
    }

    public static void main(String[] args) throws Exception {
        log.info("Start...");
        PropertyConfigurator.configure(App.class.getClassLoader().getResource("log4j.properties"));
        ApplicationContext appContext = new ClassPathXmlApplicationContext("applicationContext.xml");
        Version version = (Version) appContext.getBean("version");
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = null;
        java.util.List<ScriptEngineFactory> scriptFactories = mgr.getEngineFactories();
        engine = mgr.getEngineByName("rhino-nonjdk");
        engine.eval(new java.io.FileReader(args != null && args.length > 0 ? args[0] : version.getCore()));
        log.info("Shutdown...");
    }
}
