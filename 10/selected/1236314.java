package net.tralfamadore.persistence;

import net.tralfamadore.cmf.JpaContentManager;
import net.tralfamadore.config.CmfContext;
import net.tralfamadore.config.ConfigFile;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * User: billreh
 * Date: 1/30/11
 * Time: 9:06 AM
 */
public class JpaEntityManagerProvider implements EntityManagerProvider {

    private EntityManager em;

    private EntityManagerFactory emFactory;

    @Override
    public void shutdown() {
        if (em != null) em.close();
        if (emFactory != null) emFactory.close();
    }

    @Override
    public EntityManager get() {
        if (em == null) {
            ConfigFile configFile = CmfContext.getInstance().getConfigFile();
            Map<String, String> properties = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : configFile.getPersistenceProperties().entrySet()) {
                properties.put(entry.getKey(), entry.getValue());
            }
            emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, properties);
            em = emFactory.createEntityManager();
            boolean embedded = properties.get("javax.persistence.jdbc.driver").equals("org.apache.derby.jdbc.EmbeddedDriver");
            boolean mem = properties.get("javax.persistence.jdbc.url").matches("^jdbc:derby:memory:.*");
            if (embedded && mem) {
                CmfContext.getInstance().setEmbeddedDbNeedsConfig(true);
                try {
                    dropEmbeddedTables();
                } catch (Exception ignore) {
                }
                createEmbeddedTables();
                CmfContext.getInstance().setInMemory(true);
            }
        }
        return em;
    }

    public void createEmbeddedTables() {
        em.getTransaction().begin();
        try {
            String[] tableSql = getDerbyCreate();
            for (String query : tableSql) {
                if (query.matches("^\\s*$")) continue;
                em.createNativeQuery(query).executeUpdate();
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
        }
    }

    public void dropEmbeddedTables() {
        try {
            em.getTransaction().begin();
            for (String query : dropDerbyTables) em.createNativeQuery(query).executeUpdate();
            em.getTransaction().commit();
        } catch (Exception ignore) {
            em.getTransaction().rollback();
        }
    }

    public void createEmbeddedDb(Properties properties) {
        shutdown();
        emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, properties);
        em = emFactory.createEntityManager();
        createEmbeddedTables();
        ((JpaContentManager) CmfContext.getInstance().getContentManager()).setEm(em);
        try {
            editWebXml(properties);
        } catch (Exception e) {
            e.printStackTrace();
        }
        CmfContext.getInstance().setInMemory(false);
    }

    public void editWebXml(Properties properties) throws Exception {
        String url = FacesContext.getCurrentInstance().getExternalContext().getRealPath("/WEB-INF/cmf-config.xml");
        File file = new File(url);
        String contents = readFileAsString(file);
        String newUrl = properties.getProperty("javax.persistence.jdbc.url");
        contents = contents.replaceAll("jdbc:derby:memory:cmf;create=true", newUrl);
        writeStringToFile(file, contents);
    }

    private String[] getDerbyCreate() throws Exception {
        String url = "/Users/billreh/IdeaProjects/content-management-faces/scripts/cmf-derby.sql";
        File file = new File(url);
        return readFileAsString(file).split(";");
    }

    static void writeStringToFile(File file, String string) throws java.io.IOException {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(string.getBytes());
        } finally {
            try {
                if (outputStream != null) outputStream.close();
            } catch (Exception ignore) {
            }
        }
    }

    static String readFileAsString(File file) throws java.io.IOException {
        byte[] buffer = new byte[(int) file.length()];
        FileInputStream f = null;
        try {
            f = new FileInputStream(file);
            f.read(buffer);
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException ignore) {
                }
            }
        }
        return new String(buffer);
    }

    String dropDerbyTables[] = { "drop table group_permissions_to_content\n", "drop table group_permissions_to_namespace\n", "drop table group_permissions_to_style\n", "drop table group_permissions\n", "drop table user_to_group\n", "drop table groups\n", "drop table users\n", "drop table style_to_content\n", "drop table style\n", "drop table content\n", "drop table namespace\n", "drop table id_gen\n" };

    String createDerbyTables[] = { "CREATE TABLE namespace (\n" + "    id bigint NOT NULL,\n" + "    name varchar(32000) NOT NULL,\n" + "    parent_id bigint,\n" + "\t primary key(id)\n" + ")\n", "CREATE TABLE content (\n" + "    id bigint NOT NULL,\n" + "    namespace_id bigint NOT NULL,\n" + "    name character varying(256) NOT NULL,\n" + "    content varchar(32000),\n" + "    date_created timestamp,\n" + "    date_modified timestamp,\n" + "\t primary key(id),\n" + "\t foreign key (namespace_id) references namespace(id)\n" + ")\n", "CREATE TABLE style (\n" + "    id bigint NOT NULL,\n" + "    namespace_id bigint NOT NULL,\n" + "    name character varying(256) NOT NULL,\n" + "    style varchar(32000),\n" + "\t primary key(id),\n" + "\t foreign key (namespace_id) references namespace(id)\n" + ")\n", "CREATE TABLE style_to_content (\n" + "    id bigint NOT NULL,\n" + "    content_id bigint NOT NULL,\n" + "    style_id bigint NOT NULL,\n" + "\t primary key(id),\n" + "\t foreign key (content_id) references content(id),\n" + "\t foreign key (style_id) references style(id)\n" + ")\n", "CREATE TABLE id_gen (\n" + "    gen_name character varying(80) NOT NULL,\n" + "    gen_val integer\n" + ")\n", "INSERT INTO id_gen VALUES('style_id', 100)\n", "INSERT INTO id_gen VALUES('content_id', 100)\n", "INSERT INTO id_gen VALUES('namespace_id', 100)\n", "INSERT INTO id_gen VALUES('style_to_content_id', 100)" };
}
