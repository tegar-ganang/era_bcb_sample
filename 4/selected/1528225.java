package org.docflower.testsuite.enhancer.tests;

import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.docflower.serializer.context.*;
import org.docflower.serializer.serializables.IXmlSerializable;
import org.docflower.testsuite.enhancer.BaseEnhancerTest;
import org.docflower.testsuite.enhancer.data.SampleClassWithList;
import org.docflower.xml.SimpleNamespaceContext;
import org.junit.*;

public class ListSerializationTest extends BaseEnhancerTest {

    @Before
    public void prepare() throws IOException, ClassNotFoundException {
        FileUtils.forceMkdir(new File("./bin/org/docflower/testsuite/enhancer/data/original"));
        simpleClearFolder("./bin/org/docflower/testsuite/enhancer/data/original");
        FileUtils.copyFile(new File("./bin/org/docflower/testsuite/enhancer/data/SampleClassWithList.class"), new File("./bin/org/docflower/testsuite/enhancer/data/original/SampleClassWithList.class"));
        enhance("org/docflower/testsuite/enhancer/data/SampleClassWithList");
    }

    @Test
    public void makeListSerializationTest() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass("org.docflower.testsuite.enhancer.data.SampleClassWithList");
        SampleClassWithList sc = (SampleClassWithList) cl.newInstance();
        if (sc instanceof IXmlSerializable) {
            SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
            nsContext.addPrefix("c5", sc.getClass().getName().replace(".", "/"));
            List<String> testList = new ArrayList<String>();
            testList.add("Str1");
            testList.add("Str2");
            testList.add("Str3");
            sc.setListField(testList);
            testList = new ArrayList<String>();
            testList.add("Str4");
            testList.add("Str5");
            testList.add("Str6");
            sc.setListField2(testList);
            IXmlSerializable ixs = (IXmlSerializable) sc;
            PrintWriter pw = new PrintWriter(System.out);
            SimpleSerializationContext context = new SimpleSerializationContext(nsContext, new SimpleOutputGenerator(), pw, null);
            ixs.serializeToXml(context, null);
            pw.flush();
            pw.close();
        }
    }
}
