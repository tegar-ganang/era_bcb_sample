package com.cross.test;

import java.io.File;
import junit.framework.TestCase;
import com.cross.core.Reader;
import com.cross.core.Row;
import com.cross.core.SimpleTractor;
import com.cross.core.Tractor;
import com.cross.core.Writer;
import com.cross.excel.SimpleExcelReader;
import com.cross.jdbc.SimpleJdbcWriter;

public class SimpleJdbcWriterTestCase extends TestCase {

    public void testWrite() {
        String file = "e:\\test\\jdbc.xls";
        try {
            Reader<Row> reader = new SimpleExcelReader(file);
            Writer writer = new SimpleJdbcWriter("cross_test", reader.getHeader());
            Tractor tractor = new SimpleTractor(reader, writer);
            tractor.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
