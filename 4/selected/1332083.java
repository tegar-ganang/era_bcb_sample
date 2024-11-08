package com.apelon.apps.dts.mcdeployment;

import com.apelon.common.sql.ConnectionParams;
import com.apelon.common.sql.SQL;
import oracle.sql.BLOB;
import java.io.*;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WarFileCreator {

    private Properties fRequiredJars;

    private WarMetaData[] fDatasources;

    private Logger fLog;

    public WarFileCreator(WarMetaData[] datasources, ArrayList required_jars, Logger log) {
        fDatasources = datasources;
        if (log == null) fLog = Logger.getLogger(getClass().getName()); else fLog = log;
        loadRequiredJars(required_jars);
    }

    public static void main(String[] args) {
    }

    private void SerializeToFile(InputStream in, File f) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private void SerializeToFile(byte[] bytes, File f) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        out.write(bytes);
        out.close();
    }

    public static String fixName(String name) {
        return name.toLowerCase().replaceAll(" ", "").replaceAll("\\p{Punct}", "");
    }

    public boolean createWarFiles(File deployment_dir, Connection conn, ConnectionParams cp) {
        if (deployment_dir == null) return false;
        ArrayList jar_files = new ArrayList();
        try {
            for (int k = 0; k < fDatasources.length; k++) {
                WarMetaData wmd = fDatasources[k];
                String ds = fixName(wmd.getNamespaceInfo().getName());
                File war_dir = new File(deployment_dir.getPath() + File.separator + ds);
                war_dir.mkdir();
                File serialized_classifier = null;
                try {
                    String connType = SQL.getConnType(conn);
                    if (wmd.getDeSerializationType().equals(WarMetaData.DESERIALIZE_FILE)) {
                        serialized_classifier = new File(war_dir.getPath() + File.separator + ds);
                        InputStream is = null;
                        if (connType.equals(SQL.ORACLE)) {
                            Statement stmt = conn.createStatement();
                            ResultSet rs = stmt.executeQuery("Select SERIALIZED_CLASSIFIER from DTS_CLASSIFIER_GRAPH where NAMESPACE_ID ='" + wmd.getNamespaceInfo().getNamespaceID() + "'");
                            while (rs.next()) {
                                BLOB blob = (BLOB) rs.getBlob(1);
                                is = blob.getBinaryStream();
                            }
                            SerializeToFile(is, serialized_classifier);
                        }
                        if (connType.equals(SQL.SQL2K)) {
                            byte[] bytes = null;
                            Statement stmt = conn.createStatement();
                            ResultSet rs = stmt.executeQuery("Select SERIALIZED_CLASSIFIER from DTS_CLASSIFIER_GRAPH where NAMESPACE_ID ='" + wmd.getNamespaceInfo().getNamespaceID() + "'");
                            while (rs.next()) {
                                bytes = rs.getBytes(1);
                            }
                            SerializeToFile(bytes, serialized_classifier);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
                File meta_inf = new File(war_dir.getPath() + File.separator + "META-INF");
                meta_inf.mkdir();
                File web_inf = new File(war_dir.getPath() + File.separator + "WEB-INF");
                web_inf.mkdir();
                StringBuffer buf = new StringBuffer();
                buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                buf.append("<!DOCTYPE web-app PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\" \"http://java.sun.com/dtd/web-app_2_3.dtd\">\n");
                buf.append("<web-app>\n");
                buf.append("\t<servlet>\n");
                buf.append("\t\t<servlet-name>MCServlet" + ds + "</servlet-name>\n");
                buf.append("\t\t<servlet-class>com.apelon.modularclassifier.servlet.ModularClassifierServlet</servlet-class>\n");
                addInitParam("kbID", Integer.toString(wmd.getNamespaceInfo().getNamespaceID()), buf);
                addInitParam("kbName", wmd.getNamespaceInfo().getName(), buf);
                addInitParam("ClassifierSerialization", wmd.getDeSerializationType(), buf);
                if (wmd.getDeSerializationType().equalsIgnoreCase(WarMetaData.DESERIALIZE_FILE)) {
                    addInitParam("FileLocation", wmd.getFileLocation(), buf);
                } else {
                    buf.append(buildConnectionParamsData(cp));
                }
                buf.append("\t\t<load-on-startup/>\n");
                buf.append("\t</servlet>\n");
                buf.append("\t<servlet-mapping>\n\n");
                buf.append("\t\t<servlet-name>MCServlet" + ds + "</servlet-name>\n");
                buf.append("\t\t <url-pattern>/classify/" + ds + "</url-pattern>\n");
                buf.append("\t</servlet-mapping>\n");
                buf.append("</web-app>\n");
                File web_xml = new File(web_inf.getPath() + File.separator + "web.xml");
                FileOutputStream fo = new FileOutputStream(web_xml);
                fo.write(buf.toString().getBytes("UTF-8"));
                fo.close();
                File lib = new File(web_inf.getPath() + File.separator + "lib");
                lib.mkdir();
                lib.exists();
                String[] jars = System.getProperty("java.class.path").split(File.pathSeparator);
                ArrayList jar_count = new ArrayList();
                for (int x = 0; x < jars.length; x++) {
                    Iterator iter_jar = fRequiredJars.values().iterator();
                    while (iter_jar.hasNext()) {
                        String jar_name = (String) iter_jar.next();
                        if (jars[x].indexOf(File.separator + jar_name) > -1) {
                            File jf = new File(lib.getPath() + File.separator + jar_name);
                            copyJar(new File(jars[x]), jf);
                            jar_files.add(jf);
                            jar_count.add(jar_name);
                            iter_jar.remove();
                        }
                    }
                }
                if (fRequiredJars.size() != 0) {
                    StringBuffer err_buf = new StringBuffer();
                    Iterator iter_missing = fRequiredJars.values().iterator();
                    err_buf.append("The following jar files were missing and are required: ");
                    while (iter_missing.hasNext()) {
                        err_buf.append(iter_missing.next());
                        err_buf.append(";");
                    }
                    fLog.log(Level.SEVERE, err_buf.toString());
                    deleteDir(war_dir);
                    return false;
                }
                if (wmd.getDeSerializationType().equalsIgnoreCase(WarMetaData.DESERIALIZE_FILE)) jar_files.add(serialized_classifier);
                jar_files.add(web_xml);
                jarEm(deployment_dir.getPath(), ds, (File[]) jar_files.toArray(new File[jar_files.size()]));
                jar_files.clear();
                deleteDir(war_dir);
                loadRequiredJars(jar_count);
            }
        } catch (Exception e) {
            fLog.log(Level.SEVERE, e.getMessage(), e);
            return false;
        }
        return true;
    }

    private void loadRequiredJars(ArrayList jars) {
        if (fRequiredJars == null) {
            try {
                fRequiredJars = new Properties();
                fRequiredJars.load(WarFileCreator.class.getResourceAsStream("reqjars.properties"));
            } catch (IOException e) {
                fLog.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        if (jars != null) {
            Iterator iter = jars.iterator();
            while (iter.hasNext()) {
                String jar = (String) iter.next();
                fRequiredJars.put(jar, jar);
            }
        }
    }

    private void addInitParam(String name, String value, StringBuffer buf) {
        buf.append("\t\t<init-param>\n");
        buf.append("\t\t\t<param-name>" + name + "</param-name>\n");
        buf.append("\t\t\t<param-value>" + value + "</param-value>\n");
        buf.append("\t\t</init-param>\n");
    }

    private String buildConnectionParamsData(ConnectionParams cp) {
        StringBuffer buf = new StringBuffer();
        addInitParam("blockSize", cp.getBlockSize(), buf);
        addInitParam("hostName", cp.getHostName(), buf);
        addInitParam("instance", cp.getInstance(), buf);
        addInitParam("jdbcDriver", cp.getJDBCDriver(), buf);
        addInitParam("password", cp.getPassword(), buf);
        addInitParam("port", cp.getPort(), buf);
        addInitParam("type", cp.getType(), buf);
        addInitParam("url", cp.getUrl(), buf);
        addInitParam("urlTemplate", cp.getUrlTemplate(), buf);
        addInitParam("userName", cp.getUserName(), buf);
        return buf.toString();
    }

    private boolean deleteDir(File dir) {
        System.gc();
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    private void copyJar(File src, File dst) throws IOException {
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        try {
            srcChannel = new FileInputStream(src).getChannel();
            dstChannel = new FileOutputStream(dst).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } catch (IOException e) {
            fLog.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            srcChannel.close();
            dstChannel.close();
        }
    }

    private void jarEm(String out_path, String ds, File[] files) {
        HashMap exists = new HashMap();
        byte[] buf = new byte[1024];
        try {
            String outFilename = out_path + File.separator + ds + ".war";
            JarOutputStream out = new JarOutputStream(new FileOutputStream(outFilename));
            for (int i = 0; i < files.length; i++) {
                FileInputStream in = new FileInputStream(files[i]);
                String path = files[i].getPath().substring(files[i].getPath().indexOf(ds) + ds.length() + File.separator.length());
                path = path.replaceAll("\\\\", "/");
                if (exists.get(path) != null) continue; else exists.put(path, path);
                out.putNextEntry(new JarEntry(path));
                int len = 0;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
        } catch (IOException e) {
            fLog.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
