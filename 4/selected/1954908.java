package net.sf.docbook_utils.maven.plugins.java2html;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import net.sf.docbook_utils.maven.plugins.java2html.Java2HtmlRunner;
import org.junit.Test;

public class Java2HtmlRunnerTest {

    @Test
    public void testProviderJava2Html() throws IOException {
        File userDir = null;
        Properties java2HtmlConfig = null;
        String conversionType = "html";
        Java2HtmlRunner runner = new Java2HtmlRunner(userDir, java2HtmlConfig, null);
        StringReader reader = new StringReader(getJavaExample());
        StringWriter writer = new StringWriter();
        runner.execute(reader, writer, conversionType, java2HtmlConfig);
        System.out.println(writer.toString());
    }

    private String getJavaExample() {
        return "/** Simple Java2Html Demo */\r\n" + "public static int doThis(String text) { return text.length() + 2; }";
    }
}
