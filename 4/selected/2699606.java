package net.sf.archimede.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.jcr.Repository;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import net.sf.archimede.model.folder.FolderImplTest;
import net.sf.archimede.util.StartupJcrUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class AllTests {

    public static final String ADMIN_USERNAME = "admin";

    public static final String ADMIN_PASSWORD = "admin";

    private static final String REPOSITORY_HOME = "temp/repository";

    private static StartupJcrUtil startupUtil;

    private static void prepare() {
        System.err.println("PREPARING-----------------------------------------");
        deleteHome();
        InputStream configStream = null;
        FileOutputStream tempStream = null;
        try {
            configStream = AllTests.class.getClassLoader().getResourceAsStream("net/sf/archimede/test/resources/repository.xml");
            new File("temp").mkdir();
            tempStream = new FileOutputStream(new File("temp/repository.xml"));
            IOUtils.copy(configStream, tempStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (configStream != null) {
                    configStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (tempStream != null) {
                    try {
                        tempStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        String repositoryName = "jackrabbit.repository";
        Properties jndiProperties = new Properties();
        jndiProperties.put("java.naming.provider.url", "http://sf.net/projects/archimede#1");
        jndiProperties.put("java.naming.factory.initial", "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
        startupUtil = new StartupJcrUtil(REPOSITORY_HOME, "temp/repository.xml", repositoryName, jndiProperties);
        startupUtil.init();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for net.sf.archimede.model") {

            public void run(TestResult result) {
                try {
                    prepare();
                    super.run(result);
                    flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    deleteHome();
                }
            }
        };
        suite.addTestSuite(FolderImplTest.class);
        return suite;
    }

    private static void flush() {
        System.err.println("FLUSHING-----------------------------------------");
        startupUtil.destroy();
    }

    private static void deleteHome() {
        File repositoryDir = new File("temp");
        try {
            FileUtils.deleteDirectory(repositoryDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
