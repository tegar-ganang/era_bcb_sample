package com.cross.test;

import java.io.File;
import org.dom4j.DocumentException;
import junit.framework.TestCase;
import com.cross.core.Reader;
import com.cross.core.SimpleTractor;
import com.cross.core.Tractor;
import com.cross.core.Writer;
import com.cross.sql.SimpleSQLWriter;
import com.cross.xml.SimpleXmlReader;

public class SimpleXMLReaderTestCase extends TestCase {

    public void testReader() {
        String file = "e:\\test\\crossdata.xml";
        String dest = "e:\\test\\crosssql.sql";
        try {
            Reader reader = new SimpleXmlReader(new File(file));
            Writer writer = new SimpleSQLWriter(reader.getHeader(), new File(dest), "test_table");
            Tractor tractor = new SimpleTractor(reader, writer);
            tractor.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
