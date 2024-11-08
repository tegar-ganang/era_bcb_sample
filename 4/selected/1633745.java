package org.xBaseJ.test;

import junit.framework.TestCase;
import org.xBaseJ.DBF;
import org.xBaseJ.fields.Field;

/**
 * @author Joe McVerry - American Coders, Ltd.
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestLockRead extends TestCase {

    /**
	 *
	 */
    public void testReadLock() {
    }

    public void threadThis() {
        try {
            DBF writer = new DBF("testfiles/temp.dbf");
            for (int i = 0; i < writer.getRecordCount(); i++) {
                writer.read(true);
                Field str_field = writer.getField(1);
                System.out.println(str_field.get());
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
