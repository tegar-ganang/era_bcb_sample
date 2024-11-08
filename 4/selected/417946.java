package org.nexopenframework.tasks.jms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.nexopenframework.tasks.MyJobTask;
import org.nexopenframework.tasks.TaskExecutor;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>Comment here</p>
 * 
 * @author <a href="mailto:fme@nextret.net">Francesc Xavier Magdaleno</a>
 * @version 1.0
 * @since 1.0
 */
public class MessageDrivenBeanServiceExporterTest extends AbstractDependencyInjectionSpringContextTests {

    public void testExecuteTaskMDB() throws IOException {
        ClassLoader cls = Thread.currentThread().getContextClassLoader();
        InputStream is = cls.getResourceAsStream("META-INF/spring/openfrwk-module-tasks-mdb-test.xml");
        if (is == null) {
            this.logger.error("Not found file");
            throw new IllegalStateException("Could not perform test without the given file");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int read = 0;
        while ((read = is.read()) != -1) {
            baos.write(read);
        }
        HashMap context = new HashMap();
        context.put(MyJobTask.XML_BYTES, baos.toByteArray());
        context.put(MyJobTask.FILE_NAME, "META-INF/spring/openfrwk-module-tasks-mdb-test.xml");
        TaskExecutor.executeTask(MyJobTask.class, context);
        logger.info("Executed Task " + MyJobTask.class.getName());
        try {
            Thread.sleep(4 * 1000);
        } catch (InterruptedException e) {
        }
    }

    protected String[] getConfigLocations() {
        return new String[] { "META-INF/spring/openfrwk-module-tasks-mdb-test.xml" };
    }
}
