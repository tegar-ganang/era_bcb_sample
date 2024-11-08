package niskala.sej;

import org.apache.commons.io.IOUtils;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

/**
 * Concatenate a bunch of jar files together.
 *
 * NOTE: you must specify a Manifest Version in the manifest,
 * otherwise JarOutputStream will refuse to write it out.
 */
public class JarCat {

    protected Collection includeJars;

    protected OutputStream outputStream;

    protected boolean ignoreDependencies = false;

    protected Manifest manifest;

    public JarCat() {
    }

    public JarCat(OutputStream outputStream, Manifest manifest, Collection includeJars) {
        this.includeJars = includeJars;
        this.outputStream = outputStream;
        this.manifest = manifest;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public void setManifest(Manifest manifest) {
        this.manifest = manifest;
    }

    public boolean isIgnoreDependencies() {
        return ignoreDependencies;
    }

    public void setIgnoreDependencies(boolean ignoreDependencies) {
        this.ignoreDependencies = ignoreDependencies;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public Collection getIncludeJars() {
        return includeJars;
    }

    public void setIncludeJars(Collection includeJars) {
        this.includeJars = includeJars;
    }

    /**
     * @throws IOException
     */
    public void write() throws IOException {
        JarOutputStream jarOut = new JarOutputStream(outputStream, manifest);
        if (includeJars != null) {
            HashSet allEntries = new HashSet(includeJars);
            if (!ignoreDependencies) expandSet(allEntries);
            for (Iterator iterator = allEntries.iterator(); iterator.hasNext(); ) {
                JarFile jar = getJarFile(iterator.next());
                Enumeration jarEntries = jar.entries();
                while (jarEntries.hasMoreElements()) {
                    ZipEntry o1 = (ZipEntry) jarEntries.nextElement();
                    if (o1.getName().equalsIgnoreCase("META-INF/MANIFEST.MF") || o1.getSize() <= 0) continue;
                    jarOut.putNextEntry(o1);
                    InputStream entryStream = jar.getInputStream(o1);
                    IOUtils.copy(entryStream, jarOut);
                    jarOut.closeEntry();
                }
            }
        }
        jarOut.finish();
        jarOut.close();
    }

    /**
     * Reads the manifests of each of the jarfiles in jars and
     * adds any entries found there to jars
     * @param jars
     * @throws IOException
     */
    public void expandSet(HashSet jars) throws IOException {
        HashSet foundEntries = new HashSet();
        for (Iterator iterator = jars.iterator(); iterator.hasNext(); ) {
            JarFile jar = getJarFile(iterator.next());
            Manifest manifest = jar.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            String classpath = (String) attributes.get(Attributes.Name.CLASS_PATH);
            if (classpath != null) {
                StringTokenizer tokenizer = new StringTokenizer(classpath);
                while (tokenizer.hasMoreTokens()) {
                    String entry = tokenizer.nextToken();
                    foundEntries.add(entry);
                }
            }
        }
        jars.addAll(foundEntries);
    }

    protected JarFile getJarFile(Object o) throws IOException {
        if (o instanceof String) {
            try {
                return new JarFile((String) o);
            } catch (IOException e) {
                throw new IOException("No such jar file: " + new File(".").getCanonicalPath() + "/" + o);
            }
        }
        return (JarFile) o;
    }
}
