package org.aspencloud.simple9.server.migrations;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.sql.Statement;
import org.aspencloud.simple9.persist.persistables.DbManager;

public abstract class Migration {

    public abstract void migrateDown();

    public abstract void migrateUp();

    public void execDownSql() {
    }

    public void execUpSql() {
        String name = getClass().getCanonicalName().replace(".", "/");
        InputStream in = getClass().getResourceAsStream(name + ".sql");
        try {
            StringWriter writer = new StringWriter();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while (reader.ready()) {
                writer.write(reader.readLine());
            }
            reader.close();
            Statement statement = DbManager.instance().getConnection().createStatement();
            statement.execute(writer.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
