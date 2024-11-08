package loader;

import lxl.net.Os;
import lxl.net.ClassLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.MalformedURLException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * 
 * 
 * @author jdp
 */
public final class Jnlp extends org.xml.sax.helpers.DefaultHandler {

    private static final SAXParserFactory PF = SAXParserFactory.newInstance();

    static {
        PF.setValidating(false);
        PF.setNamespaceAware(true);
        PF.setXIncludeAware(false);
    }

    private static final SAXParser Parser;

    static {
        SAXParser parser;
        try {
            parser = PF.newSAXParser();
        } catch (javax.xml.parsers.ParserConfigurationException exc) {
            parser = null;
            exc.printStackTrace();
        } catch (SAXException exc) {
            parser = null;
            exc.printStackTrace();
        }
        Parser = parser;
    }

    private final String source;

    private volatile Locator currentDocument;

    private volatile Type currentType;

    private volatile String codebase, href, mainClassName;

    private volatile Class mainClass;

    private volatile URL codebaseUrl;

    private volatile Jar[] jars;

    private volatile Nativelib[] nativelibs;

    private volatile Extension[] extensions;

    Jnlp(URL source, InputStream in) throws IOException, SAXException {
        this(source.toString(), in);
    }

    Jnlp(String source, InputStream in) throws IOException, SAXException {
        super();
        this.source = source;
        InputSource src = new InputSource(source);
        src.setByteStream(in);
        Parser.parse(src, this);
    }

    boolean usingNative(String base, String full, File file) {
        return file.exists();
    }

    boolean usingShared(String path, File file) {
        return file.exists();
    }

    boolean copyMain(InputStream in) throws IOException {
        try {
            File target = new File(Main.GetTempDir(), "main.jnlp");
            FileOutputStream out = new FileOutputStream(target);
            try {
                byte[] iob = new byte[0x200];
                int read;
                while (0 < (read = in.read(iob, 0, 0x200))) {
                    out.write(iob, 0, read);
                }
                return true;
            } catch (IOException exc) {
                exc.printStackTrace();
                target.delete();
                return false;
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    void init(java.lang.ClassLoader loader) throws IOException {
        Main main = (Main) loader;
        Extension[] extensions = this.extensions;
        if (null != extensions) {
            for (int cc = 0, zz = extensions.length; cc < zz; cc++) {
                Extension ext = extensions[cc];
                if (main.runInitJnlpAccept(this, ext)) {
                    if (!ext.download(loader)) throw new RuntimeException("Extension failed to download '" + ext.source + "'.");
                }
            }
        }
        Nativelib[] nativelibs = this.nativelibs;
        if (null != nativelibs) {
            for (int cc = 0, zz = nativelibs.length; cc < zz; cc++) {
                Nativelib lib = nativelibs[cc];
                if (main.runInitJnlpAccept(this, lib)) {
                    if (!lib.download(loader)) throw new RuntimeException("Nativelib failed to download '" + lib.source + "'.");
                }
            }
        }
        Jar[] jars = this.jars;
        if (null != jars) {
            for (int cc = 0, zz = jars.length; cc < zz; cc++) {
                Jar jar = jars[cc];
                if (jar.lazy) continue; else if (main.runInitJnlpAccept(this, jar)) {
                    if (!jar.download(loader)) throw new RuntimeException("Jar failed to download '" + jar.source + "'.");
                }
            }
        }
    }

    void main() throws IOException {
        String mainclassName = this.mainClassName;
        if (null != mainclassName) {
            try {
                this.mainClass = Main.Current().loadClass(mainclassName);
                Method mainMethod = this.mainClass.getMethod("main", MainArgsTypes);
                mainMethod.invoke(MethodStatic, MethodNoArgs);
            } catch (ClassNotFoundException exc) {
                throw new IllegalStateException(mainclassName, exc);
            } catch (NoSuchMethodException exc) {
            } catch (IllegalAccessException exc) {
                throw new IllegalStateException(mainclassName, exc);
            } catch (java.lang.reflect.InvocationTargetException exc) {
                throw new IllegalStateException(mainclassName, exc);
            }
        }
    }

    public boolean hasMainClassName() {
        return (null != this.mainClassName);
    }

    public String getMainClassName() {
        return this.mainClassName;
    }

    public boolean hasMainClass() {
        return (null != this.mainClass);
    }

    public Class getMainClass() {
        return this.mainClass;
    }

    public String getCodebase() {
        return this.codebase;
    }

    public URL getCodebaseUrl() {
        return this.codebaseUrl;
    }

    public String getHref() {
        return this.href;
    }

    public String getHrefBase() {
        String href = this.href;
        int idx = href.lastIndexOf('.');
        if (-1 != idx && (".jnlp".equalsIgnoreCase(href.substring(idx)))) return href.substring(0, idx).replace('/', '_'); else return href.replace('/', '_');
    }

    public void setDocumentLocator(Locator locator) {
        this.currentDocument = locator;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch(Elen(localName)) {
            case ELEN_JNLP:
                String[] topAtts = Attribute(attributes, TopAtts);
                if (null != topAtts) {
                    this.codebase = topAtts[0];
                    try {
                        this.codebaseUrl = new URL(this.codebase);
                        if (2 == topAtts.length) this.href = topAtts[1]; else throw new IllegalStateException("Missing attribute 'href' on document element in '" + this.source + "'.");
                    } catch (MalformedURLException exc) {
                        throw new IllegalStateException("Attribute 'codebase' on document element in '" + this.source + "'.", exc);
                    }
                } else throw new IllegalStateException("Missing attribute 'codebase' on document element in '" + this.source + "'.");
                break;
            case ELEN_RESOURCES:
                this.currentType = new Type(attributes);
                break;
            case ELEN_JAR:
                if (this.currentType.shared || this.currentType.osarch) {
                    this.addJar(attributes);
                }
                break;
            case ELEN_EXTENSION:
                if (this.currentType.shared || this.currentType.osarch) {
                    this.addExtension(attributes);
                }
                break;
            case ELEN_NATIVELIB:
                if (this.currentType.osarch) {
                    this.addNativelib(attributes);
                }
                break;
            case ELEN_APPLICATION_DESC:
                this.mainClassName = Attribute(attributes, "main-class");
                break;
            case ELEN_PROPERTY:
                if (this.currentType.shared || this.currentType.osarch) {
                    this.setProperty(attributes);
                }
                break;
            default:
                break;
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch(Elen(localName)) {
            case ELEN_JNLP:
                break;
            case ELEN_RESOURCES:
                this.currentType = null;
                break;
            case ELEN_JAR:
                break;
            case ELEN_EXTENSION:
                break;
            case ELEN_NATIVELIB:
                break;
            case ELEN_APPLICATION_DESC:
                break;
            default:
                break;
        }
    }

    private void addJar(Attributes attributes) {
        String[] values = Attribute(attributes, JarAtts);
        String href = (null != values) ? (values[0]) : (null);
        if (null != href) {
            boolean lazy = (2 == values.length) ? ("lazy".equals(values[1])) : (false);
            try {
                URL url = new URL(href);
                this.jars = Jar.Add(this.jars, new Jar(url, lazy));
            } catch (MalformedURLException exc) {
                try {
                    URL url = ClassLoader.NewURL(this.codebaseUrl, href);
                    this.jars = Jar.Add(this.jars, new Jar(url, lazy));
                } catch (MalformedURLException exc2) {
                    throw new IllegalStateException(href, exc2);
                }
            }
        }
    }

    private void addExtension(Attributes attributes) {
        String href = Attribute(attributes, "href");
        if (null != href) {
            try {
                URL url = new URL(href);
                this.extensions = Extension.Add(this.extensions, new Extension(url));
            } catch (MalformedURLException exc) {
                try {
                    URL url = ClassLoader.NewURL(this.codebaseUrl, href);
                    this.extensions = Extension.Add(this.extensions, new Extension(url));
                } catch (MalformedURLException exc2) {
                    throw new IllegalStateException(href, exc2);
                }
            }
        }
    }

    private void addNativelib(Attributes attributes) {
        String href = Attribute(attributes, "href");
        if (null != href) {
            try {
                URL url = new URL(href);
                this.nativelibs = Nativelib.Add(this.nativelibs, new Nativelib(url));
            } catch (MalformedURLException exc2) {
                try {
                    URL url = ClassLoader.NewURL(this.codebaseUrl, href);
                    this.nativelibs = Nativelib.Add(this.nativelibs, new Nativelib(url));
                } catch (MalformedURLException exc) {
                    throw new IllegalStateException(href, exc2);
                }
            }
        }
    }

    private void setProperty(Attributes attributes) {
        String[] values = Attribute(attributes, PropAtts);
        if (null != values) {
            if (2 == values.length) {
                String name = values[0];
                String value = values[1];
                System.setProperty(name, value);
            } else if (1 == values.length) System.err.println("Property named not recognized for value '" + values[0] + "' from " + ToString(attributes));
        }
    }

    private static final int Elen(String name) {
        switch(name.charAt(0)) {
            case 'a':
                if (name.equals("application-desc")) return ELEN_APPLICATION_DESC; else return 0;
            case 'e':
                if (name.equals("extension")) return ELEN_EXTENSION; else return 0;
            case 'j':
                if (name.equals("jar")) return ELEN_JAR; else if (name.equals("jnlp")) return ELEN_JNLP; else return 0;
            case 'n':
                if (name.equals("nativelib")) return ELEN_NATIVELIB; else return 0;
            case 'p':
                if (name.equals("property")) return ELEN_PROPERTY; else return 0;
            case 'r':
                if (name.equals("resources")) return ELEN_RESOURCES; else return 0;
            default:
                return 0;
        }
    }

    private static final int ELEN_JNLP = 1;

    private static final int ELEN_RESOURCES = 2;

    private static final int ELEN_JAR = 3;

    private static final int ELEN_EXTENSION = 4;

    private static final int ELEN_NATIVELIB = 5;

    private static final int ELEN_APPLICATION_DESC = 6;

    private static final int ELEN_PROPERTY = 7;

    private static final String[] TopAtts = { "codebase", "href" };

    private static final String[] JarAtts = { "href", "download" };

    private static final String[] PropAtts = { "name", "value" };

    private static final Class[] MainArgsTypes = { String[].class };

    private static final Object MethodStatic = null;

    private static final String[] MethodNoArgs0 = {};

    private static final Object[] MethodNoArgs = { MethodNoArgs0 };

    /**
     * The safest, most portable and interoperative way for a document
     * having one namespace.
     */
    public static final String Attribute(Attributes list, String ln) {
        for (int cc = 0, zz = list.getLength(); cc < zz; cc++) {
            String name = list.getLocalName(cc);
            if (name.equals(ln)) {
                String vv = list.getValue(cc);
                if (null != vv && 0 != vv.length()) return vv;
            }
        }
        return null;
    }

    public static final String[] Attribute(Attributes list, String[] ln) {
        int lnlen = ln.length;
        int lnc = 0;
        String[] re = null;
        for (int cc = 0, zz = list.getLength(); cc < zz; cc++) {
            String name = list.getLocalName(cc);
            if (name.equals(ln[lnc])) {
                lnc += 1;
                String vv = list.getValue(cc);
                if (null != vv && 0 != vv.length()) {
                    if (null == re) {
                        re = new String[] { vv };
                        if (lnc == lnlen) return re;
                    } else {
                        int relen = re.length;
                        String[] copier = new String[relen + 1];
                        System.arraycopy(re, 0, copier, 0, relen);
                        copier[relen] = vv;
                        if (lnc == lnlen) return copier; else re = copier;
                    }
                }
            }
        }
        return re;
    }

    public static final String ToString(Attributes list) {
        StringBuilder string = new StringBuilder();
        string.append('{');
        for (int cc = 0, zz = list.getLength(); cc < zz; cc++) {
            if (0 != cc) string.append(',');
            String name = list.getLocalName(cc);
            String value = list.getValue(cc);
            if (null != value) {
                string.append(name);
                string.append(':');
                string.append(value);
            } else string.append(name);
        }
        string.append('}');
        return string.toString();
    }

    private static class Type {

        final String os, arch;

        final boolean osarch, shared;

        Type(Attributes atts) {
            super();
            this.os = Jnlp.Attribute(atts, "os");
            this.arch = Jnlp.Attribute(atts, "arch");
            if (null != this.os) {
                if (null != this.arch) {
                    this.osarch = Os.Is(this.os, this.arch);
                    this.shared = false;
                } else {
                    this.osarch = Os.Is(this.os);
                    this.shared = false;
                }
            } else {
                this.osarch = false;
                this.shared = true;
            }
        }
    }

    static void LogFileWrite(File file) {
        if (Main.Test) {
            System.err.println(String.format("%60s %20s", file.getPath(), file.length()));
        }
    }
}
