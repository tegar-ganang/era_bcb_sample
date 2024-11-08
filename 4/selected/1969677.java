package org.vexi.tools.autodoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.Writer;
import org.ibex.util.IOUtil;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class AutoDoc {

    public final SourcePath path;

    public final File outdir;

    protected String name = null;

    protected String version = null;

    protected String copyright = null;

    protected String projectWWW = null;

    protected final Configuration cfg = new Configuration();

    {
        cfg.setClassForTemplateLoading(getResourceClass(), "");
        cfg.setObjectWrapper(new DefaultObjectWrapper());
    }

    public AutoDoc(SourcePath path, File outdir) {
        this.path = path;
        this.outdir = outdir;
        if (!outdir.exists() && !outdir.mkdirs()) throw new RuntimeException("Could not create dir: " + outdir);
    }

    protected Class getResourceClass() {
        return getClass();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCopyRight() {
        return copyright;
    }

    public void setCopyRight(String copyright) {
        this.copyright = copyright;
    }

    public String getProjectWWW() {
        return projectWWW;
    }

    public void setProjectWWW(String projectWWW) {
        this.projectWWW = projectWWW;
    }

    protected void generate(Object rootModel, String fileprefix) throws Exception {
        String fileTemplate = fileprefix + ".ftl";
        String fileGenerate = fileprefix + ".html";
        generate(rootModel, fileTemplate, fileGenerate);
    }

    protected void generate(Object rootModel, String fileTemplate, String fileGenerate) throws Exception {
        System.err.println("* generating " + fileTemplate + " -> " + fileGenerate);
        Writer out = new FileWriter(new File(outdir, fileGenerate));
        Template structFtl = cfg.getTemplate(fileTemplate);
        structFtl.process(rootModel, out);
        out.flush();
        out.close();
    }

    protected long copy(InputStream in, OutputStream out, byte[] copyBuffer) throws IOException {
        long bytesCopied = 0;
        int read = -1;
        while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
            out.write(copyBuffer, 0, read);
            bytesCopied += read;
        }
        return bytesCopied;
    }

    protected void copy(String name) throws IOException {
        System.err.println("* copying " + name);
        InputStream in = getResourceClass().getResourceAsStream(name);
        if (in == null) throw new IOException("No resource: " + name);
        File out = new File(outdir + "/" + name);
        IOUtil.pipe(in, new FileOutputStream(out));
    }

    public static String readComment(Errors err, LineNumberReader in, String trimmed) throws IOException {
        String r = "";
        while (trimmed != null) {
            if (trimmed.startsWith("*") && !trimmed.startsWith("*/")) trimmed = trimmed.substring(1);
            if (trimmed.contains("*/")) {
                r += trimmed.substring(0, trimmed.indexOf("*/"));
                if (!trimmed.endsWith("*/")) err.newWarning("text after */ on the same line");
                return r;
            }
            r += trimmed + "\n";
            trimmed = in.readLine().trim();
        }
        err.newError("expected closing */");
        return null;
    }

    public static String htmlEncode(String s) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"') out.append("&#" + (int) c + ";"); else if (c == '<') out.append("&lt;"); else if (c == '>') out.append("&gt;"); else out.append(c);
        }
        return out.toString();
    }
}
