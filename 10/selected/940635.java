package de.lema.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

public class DbInit {

    public static void main(String[] args) throws Exception {
        String filenameDdl = "./etc/sql/ddl_postgres.sql";
        DbInit.createDdlFile("de.lema.bo", filenameDdl);
        System.out.println("Achtung!!! die gesamte DB wird geloescht!");
        System.out.println("Sicher? Dann Return druecken!");
        System.in.read();
        Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/lema", "lema", "lema!");
        con.setAutoCommit(false);
        insertFiles(con, new File(filenameDdl));
        insertFiles(con, new File("./etc/sql/createIndicesSequences.sql"));
        insertConfigFiles(con, new File("./etc/config"));
        con.close();
    }

    public static void createDdlFile(String packageName, String filename) throws Exception {
        AnnotationConfiguration cfg = new AnnotationConfiguration();
        cfg.setProperty("hibernate.hbm2ddl.auto", "create");
        for (Class<?> clazz : getClasses(packageName)) {
            cfg.addAnnotatedClass(clazz);
        }
        cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        SchemaExport export = new SchemaExport(cfg);
        export.setDelimiter(";");
        export.setOutputFile(filename);
        export.execute(false, false, false, false);
    }

    private static List<Class<?>> getClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        File directory = null;
        try {
            ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            String path = packageName.replace('.', '/');
            URL resource = cld.getResource(path);
            if (resource == null) {
                throw new ClassNotFoundException("No resource for " + path);
            }
            directory = new File(resource.getFile());
        } catch (NullPointerException x) {
            throw new ClassNotFoundException(packageName + " (" + directory + ") does not appear to be a valid package");
        }
        if (directory.exists()) {
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".class")) {
                    classes.add(Class.forName(packageName + '.' + files[i].substring(0, files[i].length() - 6)));
                }
            }
        } else {
            throw new ClassNotFoundException(packageName + " is not a valid package");
        }
        return classes;
    }

    private static void insertFiles(Connection con, File file) throws IOException {
        BufferedReader bf = new BufferedReader(new FileReader(file));
        String line = bf.readLine();
        while (line != null) {
            if (!line.startsWith(" ") && !line.startsWith("#")) {
                try {
                    System.out.println("Exec: " + line);
                    PreparedStatement prep = con.prepareStatement(line);
                    prep.executeUpdate();
                    prep.close();
                    con.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        con.rollback();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            line = bf.readLine();
        }
        bf.close();
    }

    private static void insertConfigFiles(Connection con, File pfad) throws IOException, SQLException {
        File[] list = pfad.listFiles();
        for (File file : list) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (fileName.endsWith(".xml")) {
                    StringBuilder builder = new StringBuilder();
                    BufferedReader bf = new BufferedReader(new FileReader(file));
                    String line = bf.readLine();
                    while (line != null) {
                        builder.append(line);
                        line = bf.readLine();
                        if (line != null) {
                            builder.append("\n");
                        }
                    }
                    bf.close();
                    String fileOhneXml = fileName.substring(0, fileName.length() - 4);
                    PreparedStatement q1 = con.prepareStatement("delete from xmlconfiguration where key=?");
                    q1.setString(1, fileOhneXml);
                    q1.executeUpdate();
                    q1.close();
                    PreparedStatement q2 = con.prepareStatement("Insert INTO xmlconfiguration (key,config) values (?,?)");
                    q2.setString(1, fileOhneXml);
                    q2.setString(2, builder.toString());
                    q2.executeUpdate();
                    q2.close();
                    con.commit();
                }
            }
        }
    }
}
