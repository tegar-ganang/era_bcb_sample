package de.fzi.wikipipes.impl.filesystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import de.fzi.wikipipes.IWikiPage;
import de.fzi.wikipipes.impl.AbstractWebWikiPage;
import de.fzi.wikipipes.impl.Util;

public class WikiPageFS extends AbstractWebWikiPage {

    private static final Log log = LogFactory.getLog(WikiPageFS.class);

    private String name;

    private File file;

    private String mimetype;

    public static String mimetype2extension(String mimetype) {
        if (mimetype.equalsIgnoreCase(IWikiPage.MIMETYPE_WIF)) return IWikiPage.FILENAME_EXTENSION_WIFPAGE;
        String extension = mimetype;
        extension.replaceAll(" ", "_");
        return extension;
    }

    public WikiPageFS(WikiRepositoryFS repository, String name, String mimetype) {
        super(repository, name);
        this.name = name;
        this.mimetype = mimetype;
        String extension = null;
        if (mimetype.equals(IWikiPage.MIMETYPE_WIF)) extension = IWikiPage.FILENAME_EXTENSION_WIFPAGE; else extension = mimetype;
        String filename;
        if (this.name.indexOf(':') == -1) filename = this.name + "." + extension; else filename = this.name.replaceAll(":", "&#58;") + "." + extension;
        this.file = new File(repository.getStorageDir(), filename);
    }

    public WikiPageFS(WikiRepositoryFS repository, String name) {
        this(repository, name, IWikiPage.MIMETYPE_WIF);
    }

    public boolean delete() {
        return this.file.delete();
    }

    @Override
    public boolean exists() {
        return this.file.exists();
    }

    public Reader getContentAsWif() {
        try {
            InputStream in = new FileInputStream(this.file);
            return new InputStreamReader(in, "UTF-8");
        } catch (FileNotFoundException e) {
            log.warn("Was looking for " + this.file.getAbsolutePath() + " but could not find it");
            return null;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getMimeType() {
        return this.mimetype;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public Collection<IWikiPage> getSubPages() {
        List<IWikiPage> result = null;
        File atta = new File(((WikiRepositoryFS) this.repository).getStorageDir(), this.getName() + ".attachments");
        if (!atta.exists() || (atta.list().length == 0)) {
            log.debug("no attachments");
            return null;
        }
        result = new ArrayList<IWikiPage>();
        String[] atts = atta.list();
        for (String s : atts) {
            result.add(this.repository.getPageByName("attachment:" + s + "/" + "parentname:" + this.getName() + "/" + "file:///" + this.repository.getServerURL() + "\\" + this.getName() + ".attachments\\" + s));
        }
        return result;
    }

    /**@param name from IWikiPage in Collection should be like 
	 * attachment:NAME/parentname:NAME/URLtoressource 
	 * attachment:manual.pdf/parentname:Eclipse/http://ontoworld.org/manual.pdf  */
    public void setSubPages(Collection<IWikiPage> subpages) {
        if ((subpages == null) || (subpages.size() == 0)) return;
        for (IWikiPage page : subpages) {
            String s = page.getName();
            if (s.startsWith("attachment:")) {
                s = s.replaceFirst("attachment:", "");
                String name = s.substring(0, s.indexOf('/'));
                s = s.replaceFirst(name + "/parentname:", "");
                String parent = s.substring(0, s.indexOf('/'));
                s = s.replaceFirst(parent + "/", "");
                File attachStorageDir = new File(((WikiRepositoryFS) this.repository).getStorageDir().getAbsolutePath() + "\\" + parent + "." + "attachments");
                attachStorageDir.mkdirs();
                File file = new File(attachStorageDir, name);
                FileWriter writer = Util.getFileWriter(file);
                InputStream in = null;
                Reader reader = null;
                if (s.startsWith("file:///")) {
                    reader = Util.getAsReader(new File(s.replaceFirst("file:///", "")));
                } else {
                    in = Util.getInputStreamFromUrl(s);
                    reader = new BufferedReader(new InputStreamReader(in));
                }
                Util.copy(reader, writer);
            }
        }
    }

    @Override
    public void setContentFromWif(Reader wifReader) {
        Writer writer = null;
        try {
            this.file.getParentFile().mkdirs();
            this.file.createNewFile();
            FileOutputStream fos = new FileOutputStream(this.file);
            writer = new OutputStreamWriter(fos, Charset.forName("UTF-8"));
            Util.copy(wifReader, writer);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getAuthor() {
        log.warn("author at local saved wif files doesnt exist");
        return "UNKNOWN";
    }

    public long getChangeDate() {
        return this.file.lastModified();
    }

    @Override
    public long getSize() {
        return this.file.length();
    }

    /** not applicable */
    @Override
    public InputStream getBinaryInputStream() {
        return null;
    }

    @Override
    public String getSource() {
        return "" + this.file.toURI();
    }

    public String getNativeWikitext() {
        Reader wifreader = this.getContentAsWif();
        Document wifdoc = Util.getReaderAsDocument(wifreader);
        return wifdoc.asXML();
    }

    public void setNativeWikitext(String nativeWikiSource) {
        InputStream in = Util.getStringAsInputStream(nativeWikiSource);
        Document doc = Util.getInputStreamAsDocument(in);
        this.setContentFromWif(Util.getDocumentAsReader(doc));
    }

    @Override
    protected String getURL() {
        try {
            return this.file.toURL().toString();
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }
}
