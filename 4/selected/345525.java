package org.ajaxaio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;
import org.ajaxaio.js.Compressor;
import org.ajaxaio.xslt.XSLTransformer;
import de.schlichtherle.io.ArchiveDetector;
import de.schlichtherle.io.File;

/**
 * @author qiangli
 * 
 */
public class AjaxManagerImpl implements AjaxManager {

    private static final String prefix = "aio";

    private Map pathMapping;

    private List welcomeFileList;

    private Compressor compressor;

    public Map getPathMapping() {
        return this.pathMapping;
    }

    public void setPathMapping(Map pathMapping) {
        this.pathMapping = pathMapping;
    }

    public List getWelcomeFileList() {
        return this.welcomeFileList;
    }

    public void setWelcomeFileList(List welcomeFileList) {
        this.welcomeFileList = welcomeFileList;
    }

    public Compressor getCompressor() {
        return this.compressor;
    }

    public void setCompressor(Compressor compressor) {
        this.compressor = compressor;
    }

    private String getSuffix(String s) {
        int idx = s.lastIndexOf("/");
        if (idx > 0) {
            s = s.substring(idx);
        }
        int idx2 = s.lastIndexOf(".");
        if (idx2 > 0) {
            return s.substring(idx2);
        }
        return null;
    }

    private Object findMapping(String path) {
        for (Iterator i = pathMapping.keySet().iterator(); i.hasNext(); ) {
            String key = (String) i.next();
            if (path.equals(key)) {
                return pathMapping.get(key);
            }
        }
        for (Iterator i = pathMapping.entrySet().iterator(); i.hasNext(); ) {
            Entry entry = (Entry) i.next();
            String key = (String) entry.getKey();
            if (key.endsWith("*")) {
                key = key.substring(0, key.length() - 1);
                if (path.startsWith(key)) {
                    String mapping = (String) entry.getValue();
                    return mapping + path.substring(key.length());
                }
            }
        }
        return path;
    }

    private String toXml(String home, String path, File dir) throws Exception {
        XSLTransformer gt = new XSLTransformer("/org/ajaxaio/dir-html.xsl");
        final String pattern = "<file><path>{0}</path><name>{1}</name><size>{2}</size><modified>{3}</modified></file>";
        if (!path.endsWith("/")) {
            path += "/";
        }
        java.io.File[] files = dir.listFiles();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < files.length; i++) {
            java.io.File file = files[i];
            String name = file.getName() + (files[i].isDirectory() ? "/" : "");
            Object[] param = new Object[] { path + name, name, new Long(file.length()), new Date(file.lastModified()) };
            String s = MessageFormat.format(pattern, param);
            sb.append(s);
        }
        String xml = gt.transform("<list>" + "<home>" + home + "</home>" + "<dir>" + path + "</dir>" + sb.toString() + "</list>");
        return xml;
    }

    private void save(java.io.File path, String xml) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(path));
        out.write(xml);
        out.close();
    }

    private File findWelcomeFile(File dir) {
        if (welcomeFileList == null || welcomeFileList.size() == 0) {
            return null;
        }
        for (Iterator i = welcomeFileList.iterator(); i.hasNext(); ) {
            File file = new File(dir, (String) i.next());
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    public java.io.File getFile(String root, String context, String name) throws Exception {
        File.setDefaultArchiveDetector(ArchiveDetector.ALL);
        Object object = findMapping(name);
        java.io.File tmp = null;
        if (object instanceof List) {
            String suffix = getSuffix(name);
            tmp = java.io.File.createTempFile(prefix, suffix);
            tmp.deleteOnExit();
            List list = (List) object;
            BufferedOutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(tmp));
                for (Iterator i = list.iterator(); i.hasNext(); ) {
                    String jarpath = (String) i.next();
                    File file = new File(root, jarpath);
                    file.catTo(os);
                }
                os.flush();
                return tmp;
            } finally {
                os.close();
            }
        }
        String jarpath = (String) object;
        File file = new File(root, jarpath);
        if (file.isFile()) {
            String suffix = getSuffix(name);
            tmp = java.io.File.createTempFile(prefix, suffix);
            tmp.deleteOnExit();
            file.copyTo(tmp);
        } else if (file.isDirectory()) {
            File index = findWelcomeFile(file);
            if (index == null) {
                String xml = toXml(context, context + name, file);
                tmp = java.io.File.createTempFile(prefix, "-dir.html");
                tmp.deleteOnExit();
                save(tmp, xml);
            } else {
                tmp = java.io.File.createTempFile(prefix, index.getName());
                tmp.deleteOnExit();
                index.copyTo(tmp);
            }
        }
        return tmp;
    }

    public java.io.File compress(java.io.File js) {
        return compressor.compress(js);
    }

    public java.io.File gzip(java.io.File file) throws Exception {
        java.io.File tmp = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            tmp = java.io.File.createTempFile(file.getName(), ".gz");
            tmp.deleteOnExit();
            is = new BufferedInputStream(new FileInputStream(file));
            os = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
            byte[] buf = new byte[4096];
            int nread = -1;
            while ((nread = is.read(buf)) != -1) {
                os.write(buf, 0, nread);
            }
            os.flush();
        } finally {
            os.close();
            is.close();
        }
        return tmp;
    }
}
