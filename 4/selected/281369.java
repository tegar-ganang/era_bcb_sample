package uk.ac.cam.caret.minibix.imscp.processor.impl;

import java.util.*;
import java.io.*;
import uk.ac.cam.caret.minibix.general.*;
import uk.ac.cam.caret.imscp.api.*;
import uk.ac.cam.caret.imscp.impl.*;
import uk.ac.cam.caret.minibix.general.io.*;
import uk.ac.cam.caret.minibix.imscp.processor.api.*;
import uk.ac.cam.caret.minibix.metadata.api.*;
import uk.ac.cam.caret.minibix.metadata.impl.MetadataStoreImpl;
import uk.ac.cam.caret.minibix.general.svo.api.*;
import uk.ac.cam.caret.minibix.general.svo.impl.core.*;
import uk.ac.cam.caret.minibix.general.svo.migrate.api.*;
import org.apache.commons.io.*;

public class IMSCPHolderImpl implements IMSCPHolder {

    private InputStreamFountain in;

    private IMSCPUtilsImpl utils;

    private ContentPackage cp = null;

    private File on_disk = null;

    private SvoUniverse universe = new SvoUniverseInCoreImpl();

    private SvoReference svo = null;

    private SvoPreferences prefs = universe.createPreferences();

    private String prefs_string = "";

    public IMSCPHolderImpl(IMSCPUtilsImpl utils, InputStreamFountain in) {
        this.in = in;
        this.utils = utils;
    }

    public IMSCPHolderImpl(IMSCPUtilsImpl utils, ContentPackage cp) {
        this.cp = cp;
        this.utils = utils;
    }

    public void finished() {
        if (on_disk != null) on_disk.delete();
    }

    public void serialize(OutputStream out) throws IOException, BadIMSCPException {
        ensureParsed();
        ZipFilePackageParser parser = utils.getIMSCPParserFactory().createParser();
        parser.setContentPackage(cp);
        if (on_disk != null) on_disk.delete();
        on_disk = createTemporaryFile();
        parser.serialize(on_disk);
        InputStream in = new FileInputStream(on_disk);
        IOUtils.copy(in, out);
    }

    private File createTemporaryFile() throws IOException {
        File tmp = File.createTempFile("cps", "cp");
        tmp.deleteOnExit();
        return tmp;
    }

    private synchronized void ensureParsed() throws IOException, BadIMSCPException {
        if (cp != null) return;
        if (on_disk == null) {
            on_disk = createTemporaryFile();
            OutputStream to_disk = new FileOutputStream(on_disk);
            IOUtils.copy(in.getInputStream(), to_disk);
            to_disk.close();
        }
        try {
            ZipFilePackageParser parser = utils.getIMSCPParserFactory().createParser();
            parser.parse(on_disk);
            cp = parser.getPackage();
        } catch (BadParseException x) {
            throw new BadIMSCPException("Cannot parse content package", x);
        }
    }

    private synchronized void ensureSvo() throws IOException, BadIMSCPException {
        ensureParsed();
        if (svo != null) return;
        svo = universe.createAnonymous("imscp");
        cp.buildSvo(svo, "", prefs);
    }

    private MetadataStore makeStore() throws BadFormatException {
        MetadataStore ms = new MetadataStoreImpl();
        if (!"".equals(prefs_string)) ms.setDefaultsFromSerialisation(prefs_string);
        ms.addRegistry(utils.getRegistry());
        return ms;
    }

    public Map<String, String> extractMetadataForMetadata() throws IOException, BadIMSCPException {
        ensureParsed();
        Manifest mf = cp.getRootManifest();
        try {
            MetadataStore ms = makeStore();
            mf.buildMetadata(null, ms);
            Map<String, String> out = new HashMap<String, String>();
            for (String key : ms.getKeys()) {
                String value = ms.getKey(key).getStringValue();
                out.put(key, value);
            }
            return out;
        } catch (IncompatibleException x) {
            throw new BadIMSCPException("Unexpected metadata migration exception", x);
        } catch (BadFormatException x) {
            throw new BadIMSCPException("Unexpected metadata migration exception", x);
        }
    }

    public Map<String, String[]> extractMetadataForSearchTerms() throws IOException, BadIMSCPException {
        ensureParsed();
        Manifest mf = cp.getRootManifest();
        try {
            MetadataStore ms = makeStore();
            mf.buildMetadata(null, ms);
            Map<String, String[]> out = new HashMap<String, String[]>();
            for (String key : ms.getKeys()) {
                String[] value = ms.getKey(key).getSearchKeys();
                out.put(key, value);
            }
            return out;
        } catch (IncompatibleException x) {
            throw new BadIMSCPException("Unexpected metadata migration exception", x);
        } catch (BadFormatException x) {
            throw new BadIMSCPException("Unexpected metadata migration exception", x);
        }
    }

    private ManifestFile getManifestFileFromHref(Resource r, String href) {
        for (ManifestFile f : r.getFiles()) {
            if (f.getHref().equals(href)) {
                return f;
            }
        }
        return null;
    }

    public InputStream[] getInputStreamsForTopLevelResources(String[] resource_heads, boolean href_only, boolean guess_xml) throws IOException, BadIMSCPException {
        if (guess_xml) {
            throw new IllegalArgumentException("guess_xml not yet implemented");
        }
        try {
            ensureParsed();
            List<InputStream> out = new ArrayList<InputStream>();
            for (Resource r : getTargetResources(resource_heads, false)) {
                String href = r.getHref();
                ManifestFile mf = null;
                if (href != null) {
                    mf = getManifestFileFromHref(r, r.getHref());
                } else if (!href_only) {
                    mf = r.getFiles()[0];
                } else {
                    mf = null;
                }
                if (mf != null) {
                    out.add(mf.getPackageFile().getDataStream());
                } else {
                    out.add(null);
                }
            }
            return out.toArray(new InputStream[0]);
        } catch (BadParseException x) {
            throw new BadIMSCPException("Could not parse IMSCP", x);
        }
    }

    public void updateMetadata(String key, String value) throws IOException, BadIMSCPException {
        ensureParsed();
        Manifest mf = cp.getRootManifest();
        try {
            MetadataStore ms = makeStore();
            MetadataKey mk = ms.getOrCreateKey(key);
            mk.setStringValue(value);
            mf.setMetadata(ms, key, null);
        } catch (IncompatibleException x) {
            throw new BadIMSCPException("Unexpected metadata migration exception", x);
        } catch (BadFormatException x) {
            throw new BadIMSCPException("Unexpected metadata migration exception", x);
        }
    }

    public Map<String, String[]> extractMetadataFromIMSCP(Migrator m) throws IOException, BadIMSCPException {
        ensureSvo();
        Migrator migrator = m;
        if (migrator == null) migrator = utils.getMigrator();
        return migrator.migrate(svo);
    }

    private boolean addDependencies(Set<Resource> in) {
        Set<Resource> more = new HashSet<Resource>();
        for (Resource r : in) {
            for (ManifestDependency dep : r.getDependencies()) {
                Resource dest = cp.getRootManifest().getResourceFromIdentifier(dep.getIdentifierRef());
                if (dest == null) continue;
                if (!in.contains(dest)) more.add(dest);
            }
        }
        in.addAll(more);
        return more.size() != 0;
    }

    private Resource[] getTargetResources(String[] resource_heads, boolean plus_deps) {
        Set<String> heads = new HashSet<String>(Arrays.asList(resource_heads));
        Set<Resource> out = new HashSet<Resource>();
        Resource[] ins = cp.getRootManifest().getResources();
        if (ins != null) for (Resource in : ins) if (heads.contains(in.getType())) out.add(in);
        if (plus_deps) while (addDependencies(out)) ;
        return out.toArray(new Resource[0]);
    }

    private IMSCPHolder makeEmptyPackage() {
        return new IMSCPHolderImpl(utils, utils.getIMSCPParserFactory().createEmptyPackage());
    }

    private IMSCPHolder createSubPackage(Resource r) throws IOException, BadIMSCPException, BadParseException {
        IMSCPHolder out = makeEmptyPackage();
        ContentPackage cp = out.getDatamodelFromIMSCP();
        Set<Resource> res = new HashSet<Resource>();
        res.add(r);
        while (addDependencies(res)) ;
        for (Resource dep : res) {
            cp.getRootManifest().addResourceLike(dep);
        }
        Metadata res_md = r.getMetadata();
        if (res_md != null) {
            cp.getRootManifest().addMetadata();
            Metadata top_md = cp.getRootManifest().getMetadata();
            if (res_md.getSchema() != null) top_md.setSchema(res_md.getSchema());
            if (res_md.getSchemaVersion() != null) top_md.setSchemaVersion(res_md.getSchemaVersion());
            for (BuildsSvo md : res_md.getMetadata()) {
                if (md instanceof Reproducable) {
                    top_md.addMetadata((BuildsSvo) ((Reproducable) md).reproduce());
                }
            }
        }
        for (Resource dep : res) {
            for (ManifestFile mfile : dep.getFiles()) {
                PackageFile pfile = mfile.getPackageFile();
                if (pfile == null) throw new BadIMSCPException("No such member of archive " + mfile.getHref());
                pfile.cloneInto(cp);
            }
        }
        return out;
    }

    public IMSCPHolder[] splitIMSCPIntoResources(String[] resource_heads) throws IOException, BadIMSCPException {
        try {
            ensureParsed();
            List<IMSCPHolder> out = new ArrayList<IMSCPHolder>();
            for (Resource r : getTargetResources(resource_heads, false)) out.add(createSubPackage(r));
            return out.toArray(new IMSCPHolder[0]);
        } catch (BadParseException x) {
            throw new BadIMSCPException("Could not parse IMSCP", x);
        }
    }

    public synchronized void addSvoPreference(String key, String value, double score) {
        prefs.addPreference(key, value, score);
        svo = null;
    }

    public SvoReference extractSvoFromIMSCP() throws IOException, BadIMSCPException {
        ensureSvo();
        return svo;
    }

    public ContentPackage getDatamodelFromIMSCP() throws IOException, BadIMSCPException {
        ensureParsed();
        return cp;
    }

    public void addMetadataPreference(String key, String value) {
        try {
            MetadataStore ms = makeStore();
            ms.addDefault(key, value);
            prefs_string = ms.serialiseDefaults();
        } catch (BadFormatException x) {
            System.err.println("Bad prefs string in IMSCPHolderImpl. Should be impossible.");
            assert false;
        }
    }

    public void setPreferencesFromSerialisation(String in) throws BadFormatException {
        MetadataStore ms = new MetadataStoreImpl();
        ms.setDefaultsFromSerialisation(in);
        prefs_string = ms.serialiseDefaults();
    }

    public String serialisePreferences() {
        return prefs_string;
    }
}
