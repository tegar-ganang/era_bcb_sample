package org.docflower.testsuite.enhancer.tests;

import java.io.*;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.docflower.serializer.serializables.IXmlSerializable;
import org.docflower.testsuite.enhancer.*;
import org.docflower.testsuite.enhancer.data.*;
import org.docflower.xml.SimpleNamespaceContext;
import org.junit.*;

public class NestedObjectSerializationTest extends BaseEnhancerTest {

    @Before
    public void prepare() throws IOException, ClassNotFoundException {
        FileUtils.forceMkdir(new File("./bin/org/docflower/testsuite/enhancer/data/original"));
        simpleClearFolder("./bin/org/docflower/testsuite/enhancer/data/original");
        FileUtils.copyFile(new File("./bin/org/docflower/testsuite/enhancer/data/SampleClass.class"), new File("./bin/org/docflower/testsuite/enhancer/data/original/SampleClass.class"));
        enhance("org/docflower/testsuite/enhancer/data/original/SampleClass");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void makeNestedObjectSerializationTest() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass("org.docflower.testsuite.enhancer.data.SampleClass");
        Class<?> cl2 = Thread.currentThread().getContextClassLoader().loadClass("org.docflower.testsuite.enhancer.data.SampleClass2");
        SampleClass sc = (SampleClass) cl.newInstance();
        SampleClass2 sc2 = (SampleClass2) cl2.newInstance();
        if (sc instanceof IXmlSerializable) {
            SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
            nsContext.addPrefix("c5", sc.getClass().getName().replace(".", "/"));
            nsContext.addPrefix("c6", sc2.getClass().getName().replace(".", "/"));
            sc.setField1(sc2);
            sc.setField2(new HashMap());
            IXmlSerializable ixs = (IXmlSerializable) sc;
            SampleContext context = new SampleContext(nsContext, new SampleOutputGenerator());
            ixs.serializeToXml(context, null);
        }
    }
}
