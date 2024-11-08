package org.dbe.kb.usgman;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.dbe.kb.rdb.DBObject;
import org.dbe.kb.rdb.DBObject;

public class UsageManager {

    public UsageManager() {
    }

    public void storeUsageDataBatch(InputStream in) throws java.io.IOException, java.sql.SQLException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int k;
        while ((k = in.read()) != -1) bout.write(k);
        bout.flush();
        DBObject dbo = new DBObject();
        System.out.println(bout.toString());
        dbo.executeBuffer(bout.toString());
    }
}
