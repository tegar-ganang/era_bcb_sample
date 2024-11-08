package org.lindenb.tinytools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.lindenb.io.IOUtils;
import org.lindenb.io.NullOutputStream;
import org.lindenb.util.Base64;
import org.lindenb.util.Compilation;
import org.lindenb.util.StringUtils;
import org.lindenb.util.TimeUtils;

/**
 * 
 * GoogleCodeUpload
 *
 */
public class GoogleCodeUpload {

    private static final String DEFAULT_CONFIG = ".google-code.properties";

    private static final String GC_USERNAME = "google-code-username";

    private static final String GC_PASSWORD = "google-code-password";

    private static final String GC_PASSWORD64 = "google-code-password-base64";

    private String projectName = null;

    private File config = null;

    private String summary = null;

    private Set<String> labels = new HashSet<String>();

    private Properties cfg = new Properties();

    /** initialize the config */
    private void init() throws IOException {
        if (StringUtils.isBlank(this.projectName)) throw new RuntimeException("undefined project");
        if (this.config == null) {
            this.config = new File(System.getProperty("user.home"), DEFAULT_CONFIG);
        }
        if (!this.config.exists()) throw new FileNotFoundException("file not found :" + config);
        if (!this.config.isFile()) throw new IOException("not a file :" + config);
        FileInputStream fin = new FileInputStream(this.config);
        cfg.loadFromXML(fin);
        fin.close();
        if (!cfg.containsKey(GC_USERNAME)) throw new IOException("undefined " + GC_USERNAME + " in " + this.config);
        if (!cfg.containsKey(GC_PASSWORD) && !cfg.containsKey(GC_PASSWORD64)) {
            throw new IOException("undefined " + GC_PASSWORD + " in " + this.config);
        }
    }

    /** transfert the file to google */
    private void execute(File file) throws IOException {
        if (file == null) throw new RuntimeException("undefined file");
        if (!file.exists()) throw new RuntimeException("file not found :" + file);
        if (!file.isFile()) throw new RuntimeException("not a file :" + file);
        String login = cfg.getProperty(GC_USERNAME);
        String password = null;
        if (cfg.containsKey(GC_PASSWORD)) {
            password = cfg.getProperty(GC_PASSWORD);
        } else {
            password = new String(Base64.decode(cfg.getProperty(GC_PASSWORD64)));
        }
        PostMethod post = null;
        try {
            HttpClient client = new HttpClient();
            post = new PostMethod("https://" + projectName + ".googlecode.com/files");
            post.addRequestHeader("User-Agent", getClass().getName());
            post.addRequestHeader("Authorization", "Basic " + Base64.encode(login + ":" + password));
            List<Part> parts = new ArrayList<Part>();
            String s = this.summary;
            if (StringUtils.isBlank(s)) {
                s = file.getName() + " (" + TimeUtils.toYYYYMMDD() + ")";
            }
            parts.add(new StringPart("summary", s));
            for (String lbl : this.labels) {
                if (StringUtils.isBlank(lbl)) continue;
                parts.add(new StringPart("label", lbl.trim()));
            }
            parts.add(new FilePart("filename", file));
            MultipartRequestEntity requestEntity = new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), post.getParams());
            post.setRequestEntity(requestEntity);
            int status = client.executeMethod(post);
            if (status != 201) {
                throw new IOException("http status !=201 : " + post.getResponseBodyAsString());
            } else {
                IOUtils.copyTo(post.getResponseBodyAsStream(), new NullOutputStream());
            }
        } finally {
            if (post != null) post.releaseConnection();
        }
    }

    public static void main(String[] args) {
        GoogleCodeUpload app = new GoogleCodeUpload();
        try {
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h") || args[optind].equals("-help") || args[optind].equals("--help")) {
                    System.err.println(Compilation.getLabel());
                    System.err.println("Options:");
                    System.err.println(" -h help; This screen.");
                    System.err.println(" -k <keyword>");
                    System.err.println(" -c <config-file> default: ${HOME}/" + DEFAULT_CONFIG);
                    System.err.println(" -s <summary>");
                    System.err.println(" -p <project>");
                    System.err.println("<files>");
                    return;
                } else if (args[optind].equals("-k")) {
                    for (String s : args[++optind].split("[, ]")) {
                        s = s.trim();
                        if (s.isEmpty()) continue;
                        app.labels.add(s);
                    }
                } else if (args[optind].equals("-c")) {
                    for (String s : args[++optind].split("[, ]")) {
                        s = s.trim();
                        if (s.isEmpty()) continue;
                        app.labels.add(s);
                    }
                } else if (args[optind].equals("-s")) {
                    app.summary = args[++optind];
                } else if (args[optind].equals("-p")) {
                    app.projectName = args[++optind];
                } else if (args[optind].equals("--")) {
                    optind++;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("Unknown option " + args[optind]);
                    return;
                } else {
                    break;
                }
                ++optind;
            }
            if (optind == args.length) {
                System.err.println("Illegal number of arguments.");
                return;
            }
            app.init();
            while (optind != args.length) {
                app.execute(new File(args[optind++]));
            }
        } catch (Throwable err) {
            err.printStackTrace();
        }
    }
}
