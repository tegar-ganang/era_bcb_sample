package com.cross.test;

import java.io.File;
import junit.framework.TestCase;
import com.cross.core.Reader;
import com.cross.core.SimpleTractor;
import com.cross.core.Writer;
import com.cross.jdbc.DataBaseHelper;
import com.cross.jdbc.SimpleJdbcReader;
import com.cross.mutil.SimpleMutilWriter;
import com.cross.sql.SimpleSQLWriter;

public class SimpleJdbcReaderTestCase extends TestCase {

    public void testWrite() {
        String file = "e:\\test\\jdbc_export.xls";
        String file2 = "e:\\test\\jdbc_export_2.xls";
        String table = "cross_test";
        try {
            Reader reader = new SimpleJdbcReader(table, DataBaseHelper.getDbConnection("one"));
            Writer writer = new SimpleSQLWriter(reader.getHeader(), new File(file), table);
            Writer writer2 = new SimpleSQLWriter(reader.getHeader(), new File(file2), table);
            Writer mutilWriter = new SimpleMutilWriter(reader.getHeader(), writer, writer2);
            new SimpleTractor(reader, mutilWriter).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
