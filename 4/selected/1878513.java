package org.docflower.testsuite.enhancer.tests;

import java.io.*;
import org.apache.commons.io.FileUtils;
import org.docflower.serializer.serializables.IXmlSerializable;
import org.docflower.testsuite.enhancer.*;
import org.docflower.testsuite.enhancer.data.IntSampleClass;
import org.docflower.xml.SimpleNamespaceContext;
import org.junit.*;

public class IntSerializationTest extends BaseEnhancerTest {

    @Before
    public void prepare() throws IOException, ClassNotFoundException {
        FileUtils.forceMkdir(new File("./bin/org/docflower/testsuite/enhancer/data/original"));
        simpleClearFolder("./bin/org/docflower/testsuite/enhancer/data/original");
        FileUtils.copyFile(new File("./bin/org/docflower/testsuite/enhancer/data/IntSampleClass.class"), new File("./bin/org/docflower/testsuite/enhancer/data/original/IntSampleClass.class"));
        enhance("org/docflower/testsuite/enhancer/data/IntSampleClass");
    }

    @Test
    public void makeIntTest() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        IntSampleClass sc = new IntSampleClass();
        if (sc instanceof IXmlSerializable) {
            SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
            nsContext.addPrefix("c5", sc.getClass().getName().replace(".", "/"));
            sc.setIntField1(10);
            sc.setIntField2(5);
            IXmlSerializable ixs = (IXmlSerializable) sc;
            SampleContext context = new SampleContext(nsContext, new SampleOutputGenerator());
            ixs.serializeToXml(context, null);
        }
    }
}
