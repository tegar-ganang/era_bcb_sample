package com.cross.test;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import com.cross.core.Reader;
import com.cross.core.SimpleTractor;
import com.cross.core.Writer;
import com.cross.excel.SimpleExcelReader;
import com.cross.xml.SimpleXmlWriter;

public class SimpleXMLWriterTestCase extends TestCase {

    public void testWrite() {
        String src = "e:\\test\\jdbc.xls";
        String dest = "e:\\test\\export.xml";
        try {
            Reader reader = new SimpleExcelReader(src);
            Writer writer = new SimpleXmlWriter(reader.getHeader(), new File(dest));
            new SimpleTractor(reader, writer).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
