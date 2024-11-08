package org.docflower.testsuite.enhancer.tests;

import java.io.*;
import org.apache.commons.io.FileUtils;
import org.docflower.serializer.context.*;
import org.docflower.serializer.serializables.IXmlSerializable;
import org.docflower.testsuite.enhancer.*;
import org.docflower.testsuite.enhancer.data.*;
import org.docflower.testsuite.enhancer.data.EnumSampleClass.EnumField;
import org.docflower.xml.SimpleNamespaceContext;
import org.junit.*;

public class EnumSerializationTest extends BaseEnhancerTest {

    @Before
    public void prepare() throws IOException, ClassNotFoundException {
        FileUtils.forceMkdir(new File("./bin/org/docflower/testsuite/enhancer/data/original"));
        simpleClearFolder("./bin/org/docflower/testsuite/enhancer/data/original");
        FileUtils.copyFile(new File("./bin/org/docflower/testsuite/enhancer/data/EnumFieldClass.class"), new File("./bin/org/docflower/testsuite/enhancer/data/original/EnumFieldClass.class"));
        enhance("org/docflower/testsuite/enhancer/data/EnumFieldClass");
    }

    @Test
    public void makeEnumTest() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        EnumFieldClass sc = new EnumFieldClass();
        if (sc instanceof IXmlSerializable) {
            SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
            nsContext.addPrefix("c5", sc.getClass().getName().replace(".", "/"));
            sc.setEnumField(EnumField.ENUM2);
            sc.setIntField(4);
            PrintWriter pw = new PrintWriter(System.out);
            SimpleSerializationContext context = new SimpleSerializationContext(nsContext, new SimpleOutputGenerator(), pw, null);
            IXmlSerializable ixs = (IXmlSerializable) sc;
            ixs.serializeToXml(context, null);
            pw.flush();
            pw.close();
        }
    }
}
