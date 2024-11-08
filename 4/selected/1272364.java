package ru.enacu.common.dbupdater;

import org.apache.commons.io.IOUtils;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author enaku_adm
 * @since 08.09.2010 15:55:02
 */
public class DefaultScriptManager extends JdbcDaoSupport implements ScriptManager {

    private String updatesPackage = "sql.updates";

    public void setUpdatesPackage(String updatesPackage) {
        this.updatesPackage = updatesPackage;
    }

    public void updateDb(int scriptNumber) throws SQLException, IOException {
        String pathName = updatesPackage.replace(".", "/");
        InputStream in = getClass().getClassLoader().getResourceAsStream(pathName + "/" + scriptNumber + ".sql");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        String script = out.toString("UTF-8");
        String[] statements = script.split(";");
        for (String statement : statements) {
            getJdbcTemplate().execute(statement);
        }
    }

    public List<Integer> getAvailableUpdateScripts() throws ClassNotFoundException {
        List<String> scripts = getResourcesForPackage(updatesPackage);
        List<Integer> retVal = new ArrayList<Integer>();
        for (String script : scripts) {
            if (script.endsWith(".sql")) {
                String numberPart = script.substring(0, script.length() - 4);
                int num = 0;
                try {
                    num = Integer.parseInt(numberPart);
                } catch (NumberFormatException e) {
                }
                retVal.add(num);
            }
        }
        return retVal;
    }

    public static List<String> getResourcesForPackage(String pckgname) throws ClassNotFoundException {
        List<String> retVal = new ArrayList<String>();
        ArrayList<File> directories = new ArrayList<File>();
        try {
            ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            Enumeration<URL> resources = cld.getResources(pckgname.replace('.', '/'));
            while (resources.hasMoreElements()) {
                URL res = resources.nextElement();
                if (res.getProtocol().equalsIgnoreCase("jar")) {
                    JarURLConnection conn = (JarURLConnection) res.openConnection();
                    JarFile jar = conn.getJarFile();
                    for (JarEntry e : Collections.list(jar.entries())) {
                        if (!e.getName().endsWith(".class") && e.getName().startsWith(pckgname.replace('.', '/'))) {
                            int lastSlashIdx = e.getName().lastIndexOf("/");
                            String s = e.getName().substring(lastSlashIdx + 1);
                            if (s.length() > 0) retVal.add(s);
                        }
                    }
                } else directories.add(new File(URLDecoder.decode(res.getPath(), "UTF-8")));
            }
        } catch (NullPointerException x) {
            throw new ClassNotFoundException(pckgname + " does not appear to be " + "a valid package (Null pointer exception)");
        } catch (UnsupportedEncodingException encex) {
            throw new ClassNotFoundException(pckgname + " does not appear to be " + "a valid package (Unsupported encoding)");
        } catch (IOException ioex) {
            throw new ClassNotFoundException("IOException was thrown when trying " + "to get all resources for " + pckgname);
        }
        for (File directory : directories) {
            if (directory.exists()) {
                String[] files = directory.list();
                for (String file : files) {
                    if (!file.endsWith(".class")) {
                        retVal.add(file);
                    }
                }
            } else {
                throw new ClassNotFoundException(pckgname + " (" + directory.getPath() + ") does not appear to be a valid package");
            }
        }
        return retVal;
    }
}
