package com.cross.test;

import java.io.File;
import java.util.List;
import com.cross.core.Column;
import com.cross.core.Header;
import com.cross.core.Reader;
import com.cross.core.Row;
import com.cross.core.SimpleTractor;
import com.cross.core.Tractor;
import com.cross.core.Writer;
import com.cross.excel.SimpleExcelReader;
import com.cross.excel.SimpleExcelWriter;
import com.cross.sql.SimpleSQLWriter;
import junit.framework.TestCase;

public class SimpleExcelReaderTestCase extends TestCase {

    public void testReader() {
        String file = "e:\\test\\d.xls";
        String dest = "e:\\test\\e.xls";
        try {
            Reader<Row> reader = new SimpleExcelReader(file);
            Row row = null;
            while (reader.available() && reader.hasNext()) {
                row = reader.next();
                System.out.println(row.toString());
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testSimpleTractor() {
        String file = "e:\\test\\d.xls";
        String dest = "e:\\test\\e.xls";
        try {
            Reader<Row> reader = new SimpleExcelReader(file);
            Writer writer = new SimpleExcelWriter(reader.getHeader(), dest);
            Tractor tractor = new SimpleTractor(reader, writer);
            tractor.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testSimpleTractor_2() {
        String file = "e:\\test\\d.xls";
        String dest = "e:\\test\\sql.sql";
        try {
            Reader<Row> reader = new SimpleExcelReader(file);
            Writer writer = new SimpleSQLWriter(reader.getHeader(), new File(dest), "table");
            Tractor tractor = new SimpleTractor(reader, writer);
            tractor.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
