package org.docflower.testsuite.enhancer.tests;

import java.io.*;
import org.apache.commons.io.FileUtils;
import org.docflower.serializer.context.*;
import org.docflower.serializer.serializables.IXmlSerializable;
import org.docflower.testsuite.enhancer.BaseEnhancerTest;
import org.docflower.testsuite.enhancer.data.CompositeFieldsSampleClass;
import org.docflower.xml.SimpleNamespaceContext;
import org.junit.*;

public class CompositFIeldsSerializationTest extends BaseEnhancerTest {

    @Before
    public void prepare() throws IOException, ClassNotFoundException {
        FileUtils.forceMkdir(new File("./bin/org/docflower/testsuite/enhancer/data/original"));
        simpleClearFolder("./bin/org/docflower/testsuite/enhancer/data/original");
        FileUtils.copyFile(new File("./bin/org/docflower/testsuite/enhancer/data/CompositeFieldsSampleClass.class"), new File("./bin/org/docflower/testsuite/enhancer/data/original/CompositeFieldsSampleClass.class"));
        enhance("org/docflower/testsuite/enhancer/data/CompositeFieldsSampleClass");
    }

    @Test
    public void makeListSerializationTest() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass("org.docflower.testsuite.enhancer.data.CompositeFieldsSampleClass");
        CompositeFieldsSampleClass sc = (CompositeFieldsSampleClass) cl.newInstance();
        if (sc instanceof IXmlSerializable) {
            SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
            nsContext.addPrefix("c5", sc.getClass().getName().replace(".", "/"));
            IXmlSerializable ixs = (IXmlSerializable) sc;
            PrintWriter pw = new PrintWriter(System.out);
            SimpleSerializationContext context = new SimpleSerializationContext(nsContext, new SimpleOutputGenerator(), pw, null);
            ixs.serializeToXml(context, null);
            pw.flush();
            pw.close();
        }
    }
}
