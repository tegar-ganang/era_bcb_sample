package org.hoydaa.codesnippet.java;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.hoydaa.codesnippet.core.CodeSnippetGenerator;
import org.hoydaa.codesnippet.core.Configuration;
import org.hoydaa.codesnippet.core.TestUtils;

/**
 * 
 * @author Utku Utkan
 */
public class JavaTest extends TestCase {

    public JavaTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(JavaTest.class);
    }

    public void testJava() {
        Reader configReader = null;
        Reader reader = null;
        Writer writer = null;
        try {
            configReader = new InputStreamReader(getClass().getResourceAsStream("/java.properties"));
            reader = new InputStreamReader(getClass().getResourceAsStream("/test.java"));
            writer = new OutputStreamWriter(TestUtils.getOutputStream("java.html"));
            Configuration config = new Configuration();
            config.load(configReader);
            CodeSnippetGenerator generator = new CodeSnippetGenerator(config);
            generator.generate(reader, writer);
            writer.flush();
        } catch (Exception e) {
            if (configReader != null) {
                try {
                    configReader.close();
                } catch (IOException e1) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                }
            }
        }
    }
}
