package ge.telasi.tasks.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * This class is used for database migration support.
 *
 * @author dimitri
 */
public final class Migration {

    static final String SELECT_VERSION = "SELECT `version` FROM `${schema_name}`.`schemainfo`";

    static final String UPDATE_VERSION = "UPDATE `${schema_name}`.`schemainfo` SET `version` = ${version}";

    static final NumberFormat nf = new DecimalFormat("000");

    static final String[] files = { "000_schemainfo.sql", "001_structures.sql", "002_users.sql", "003_app_config.sql", "004_binary_data.sql", "005_attachments.sql", "006_groups.sql", "007_tasks.sql", "008_task_receiver_indexing.sql", "009_flow_permissions.sql", "010_task_relations.sql" };

    StringBuilder getPart(String schemaName, String fileName, boolean up) throws IOException {
        InputStream in = Migration.class.getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder buff = new StringBuilder();
        boolean dividerRiched = false;
        while (true) {
            String line = line = reader.readLine();
            if (line == null) break;
            if ("-----".equals(line)) {
                dividerRiched = true;
                continue;
            }
            if ((up && !dividerRiched) || (!up && dividerRiched)) {
                buff.append(line.replace("${schema_name}", schemaName));
                buff.append("\n");
            }
        }
        return buff;
    }

    /**
	 * Read SQL script from migration file.
	 */
    String[] readSQLScript(String schemaName, int version, boolean up) {
        String fileName = getFileName(version);
        StringBuilder buff = new StringBuilder();
        try {
            buff = getPart(schemaName, fileName, up);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        String[] scripts = buff.toString().split(";\n/");
        ArrayList<String> list = new ArrayList<String>();
        for (String script : scripts) {
            if (script != null && script.trim().length() > 0) {
                String sql = script.replace("${schema_name}", schemaName);
                try {
                    sql = new String(sql.getBytes(), "utf-8");
                } catch (UnsupportedEncodingException uee) {
                    uee.printStackTrace();
                }
                list.add(sql);
            }
        }
        return list.toArray(new String[] {});
    }

    /**
	 * Returns path to the version file.
	 */
    String getFileName(int version) {
        return files[version];
    }

    /**
	 * Migrates database to given version.
	 */
    public void migrateDatabaseTo(EntityManager em, String schemaName, int version) throws IOException {
        int localversion = getLocalVersion();
        if (version > localversion) {
            throw new IllegalArgumentException("Version is not yet implemented: " + version);
        }
        int databaseVersion = getDatabaseVersion(em, schemaName);
        if (databaseVersion == version) {
            return;
        }
        boolean direction = databaseVersion < version;
        int start = direction ? databaseVersion + 1 : databaseVersion;
        int end = direction ? version + 1 : version;
        int increment = direction ? +1 : -1;
        em.getTransaction().begin();
        try {
            for (int v = start; v != end; v = v + increment) {
                String[] scripts = readSQLScript(schemaName, v, direction);
                for (int i = 0; i < scripts.length; i++) {
                    String script = scripts[i];
                    Query query = em.createNativeQuery(script);
                    query.executeUpdate();
                }
            }
            if (version != -1) {
                String versionUpdateSql = UPDATE_VERSION.replace("${schema_name}", schemaName).replace("${version}", String.valueOf(version));
                Query query = em.createNativeQuery(versionUpdateSql);
                query.executeUpdate();
            }
            em.getTransaction().commit();
        } catch (RuntimeException rex) {
            rex.printStackTrace();
            em.getTransaction().rollback();
            throw rex;
        }
    }

    /**
     * Database local version.
	 */
    public int getLocalVersion() {
        return files.length - 1;
    }

    /**
	 * Get concrete database version based on recording
	 * in appropriate <code>schemainfo</code> table. Returns
	 * -1 for empty schema.
	 */
    public int getDatabaseVersion(EntityManager em, String schemaName) {
        try {
            String sql = SELECT_VERSION.replace("${schema_name}", schemaName);
            Query query = em.createNativeQuery(sql);
            List<?> row = (List<?>) query.getSingleResult();
            return (Integer) row.get(0);
        } catch (Exception ex) {
            return -1;
        }
    }

    /**
	 * Clean-up the database and build it again.
	 */
    public void cleanAndBuildDatabase(EntityManager em, String schema) throws IOException {
        migrateDatabaseTo(em, schema, -1);
        migrateDatabaseTo(em, schema, getLocalVersion());
    }
}
