package uk.ac.cam.caret.imscp.impl;

import org.junit.*;
import java.net.*;
import java.util.zip.*;
import org.xml.sax.*;
import java.io.*;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import junit.framework.TestCase;
import uk.ac.cam.caret.imscp.api.*;
import org.apache.commons.io.*;
import uk.ac.cam.caret.lom.api.*;
import uk.ac.cam.caret.lom.impl.*;
import uk.ac.cam.caret.qticp.api.*;
import uk.ac.cam.caret.qticp.impl.*;
import uk.ac.cam.caret.minibix.general.*;
import uk.ac.cam.caret.minibix.general.svo.api.*;
import uk.ac.cam.caret.minibix.general.svo.impl.core.*;
import uk.ac.cam.caret.minibix.metadata.api.MetadataKey;
import uk.ac.cam.caret.minibix.metadata.api.MetadataStore;
import uk.ac.cam.caret.minibix.metadata.impl.MetadataStoreImpl;
import uk.ac.cam.caret.tagphage.parser.*;
import uk.ac.cam.caret.tagphage.parser.ruleparser.*;

public class ZipFilePackageParserTest extends TestCase {

    private File full, xmlbase;

    private File writeResourceToFile(String resource) throws IOException {
        File tmp = File.createTempFile("zfppt" + resource, null);
        InputStream res = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        OutputStream out = new FileOutputStream(tmp);
        IOUtils.copy(res, out);
        out.close();
        return tmp;
    }

    @Before
    public void setUp() throws Exception {
        full = writeResourceToFile("full.zip");
        xmlbase = writeResourceToFile("xmlbase.zip");
    }

    @After
    public void tearDown() throws Exception {
        full.delete();
        xmlbase.delete();
    }

    private void checkMetadata(Metadata md, String cmp, boolean and_qti) {
        assertNotNull(md);
        assertEquals("IMS Content", md.getSchema());
        assertEquals("1.1.4", md.getSchemaVersion());
        BuildsSvo[] mh = md.getMetadata();
        assertNotNull(mh);
        if (and_qti) {
            assertEquals(2, mh.length);
            if (mh[1] instanceof LomMetadata) {
                BuildsSvo x = mh[0];
                mh[0] = mh[1];
                mh[1] = x;
            }
        } else assertEquals(1, mh.length);
        assertTrue(mh[0] instanceof LomMetadata);
        LomMetadata lom = (LomMetadata) mh[0];
        assertEquals(cmp, lom.getGeneral().getTitle().getValue(null));
        if (and_qti) {
            assertTrue(mh[1] instanceof QTIMetadata);
            assertEquals("false", ((QTIMetadata) mh[1]).getComposite());
        }
    }

    private void checkExtras(Object[] extras, String cmp) {
        assertNotNull(extras);
        assertEquals(1, extras.length);
        assertTrue(extras[0] instanceof ExtraTag);
        assertEquals(cmp, ((ExtraTag) extras[0]).getText());
    }

    private ParserFactory getExtraParser() throws Exception {
        InputStream extra_rules = Thread.currentThread().getContextClassLoader().getResourceAsStream("tagphage-extra.xml");
        RuleParser rp = new RuleParser();
        RuleSet rs = rp.parseRules(new InputSource(extra_rules));
        Schema s = rs.getSchemaByName("extra");
        return s.getParserFactory();
    }

    @Test
    public void testBasicParse() throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        QTIParserFactoryFactory qticpp = new QTIParserFactoryFactoryImpl();
        ParserFactory extra = getExtraParser();
        factory.registerMetadataParser(qticpp.getParserFactory());
        factory.registerMetadataParser(lomp.getParserFactory());
        factory.registerExtraParser(null, extra);
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(full);
        ContentPackage cp1 = parser.getPackage();
        assertNotNull(cp1);
        ContentPackage cp = (ContentPackage) cp1.reproduce();
        Manifest m = cp.getRootManifest();
        assertNotNull(m);
        assertEquals("Manifest1-CEC3D3-3201-DF8E-8F42-3CEED12F4197", m.getIdentifier());
        assertEquals("IMS CP 1.1.4", m.getVersion());
        assertEquals(".", m.getManifestXmlBase());
        assertEquals("TOC1", m.getDefaultOrganization());
        assertEquals(1, m.getOrganizations().length);
        Organization g = m.getOrganizations()[0];
        assertNotNull(g);
        assertEquals("TOC1", g.getIdentifier());
        assertEquals("hierarchical", g.getStructure());
        assertEquals("default", g.getTitle());
        assertEquals(3, g.getChildItems().length);
        OrganizationItem t1 = g.getChildItems()[0];
        assertEquals("ITEM1", t1.getIdentifier());
        assertEquals("RESOURCE1", t1.getIdentifierRef());
        assertEquals("true", t1.isVisible());
        OrganizationItem t9 = g.getChildItems()[2];
        assertNotNull(t9);
        OrganizationItem t10 = t9.getChildItems()[0];
        assertNotNull(t10);
        assertEquals(t9.getIdentifier(), t10.getParentItem().getIdentifier());
        assertEquals("foo", t10.getParameters());
        assertEquals("Introduction 3", t10.getTitle());
        assertEquals(".", m.getResourcesXmlBase());
        assertEquals(14, m.getResources().length);
        Resource r1 = m.getResources()[0];
        assertNotNull(r1);
        assertEquals("RESOURCE1", r1.getIdentifier());
        assertEquals("webcontent", r1.getType());
        assertEquals(".", r1.getXmlBase());
        assertEquals("lesson1.htm", r1.getHref());
        assertEquals(1, r1.getFiles().length);
        ManifestFile mf = r1.getFiles()[0];
        assertNotNull(mf);
        assertEquals("lesson1.htm", mf.getHref());
        assertEquals(1, r1.getDependencies().length);
        ManifestDependency md = r1.getDependencies()[0];
        assertEquals("RESOURCE2", md.getIdentifierRef());
        checkMetadata(m.getMetadata(), "IMS Content Packaging Sample - All Elements", true);
        checkMetadata(t1.getMetadata(), "Item One", false);
        checkMetadata(r1.getMetadata(), "Resource One", false);
        checkMetadata(mf.getMetadata(), "Lesson One", false);
        checkExtras(m.getRootExtras(), "manifest level");
        checkExtras(m.getMetadata().getExtras(), "root metadata level");
        checkExtras(g.getMetadata().getExtras(), "organization metadata level");
        checkExtras(t1.getMetadata().getExtras(), "item metadata level");
        checkExtras(r1.getMetadata().getExtras(), "resource metadata level");
        checkExtras(mf.getMetadata().getExtras(), "file metadata level");
        checkExtras(m.getOrganizationsExtras(), "organizations level");
        checkExtras(g.getRootExtras(), "organization level");
        checkExtras(t1.getRootExtras(), "item level");
        checkExtras(m.getResourcesExtras(), "resources level");
        checkExtras(r1.getExtras(), "resource level");
        checkExtras(mf.getExtras(), "file level");
    }

    private void streamContains(String in, InputStream stream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(stream, baos);
        byte[] bytes = baos.toByteArray();
        String cmp = new String(bytes, "UTF-8");
        assertTrue(cmp.contains(in));
        baos.close();
    }

    @Test
    public void testPackageParse() throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(full);
        ContentPackage cp1 = parser.getPackage();
        assertNotNull(cp1);
        ContentPackage cp = (ContentPackage) cp1.reproduce();
        PackageDirectory d1 = cp.getRootDirectory();
        assertNotNull(d1);
        Entry e1 = d1.getChild("content1.htm");
        assertNotNull(e1);
        assertTrue(e1 instanceof PackageFile);
        PackageFile f1 = (PackageFile) e1;
        Entry e2 = d1.getChild("nest");
        assertNotNull(e2);
        assertTrue(e2 instanceof PackageDirectory);
        PackageDirectory d2 = (PackageDirectory) e2;
        Entry e3 = d2.getChild("nest.html");
        Entry e4 = d2.getChild("nest2.html");
        assertNotNull(e3);
        assertTrue(e3 instanceof PackageFile);
        assertNotNull(e4);
        assertTrue(e4 instanceof PackageFile);
        assertEquals(15, d1.getChildren().length);
        assertEquals("content1.htm", f1.getFullName());
        assertEquals("nest", d2.getFullName());
        assertEquals("nest/nest.html", e3.getFullName());
        assertEquals(d2, e3.getParent());
        assertNull(d1.getParent());
        InputStream in1 = f1.getDataStream();
        streamContains("Content 1", in1);
        in1.close();
    }

    @Test
    public void testSvoMetadata() throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        QTIParserFactoryFactory qticpp = new QTIParserFactoryFactoryImpl();
        factory.registerMetadataParser(lomp.getParserFactory());
        factory.registerMetadataParser(qticpp.getParserFactory());
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(full);
        ContentPackage cp = parser.getPackage();
        assertNotNull(cp);
        SvoUniverseInCoreImpl universe = new SvoUniverseInCoreImpl();
        SvoPreferences prefs = universe.createPreferences();
        SvoReference root = universe.createAnonymous("imscp.metadata");
        cp.buildSvo(root, "", prefs);
        assertNotNull(root);
        String[] t1 = root.getRelationS("lom.general.title");
        assertEquals(1, t1.length);
        assertTrue(t1[0].contains("Content Packaging Sample"));
        SvoReference[] orgs = root.getRelationR("imscp.organization");
        assertEquals(1, orgs.length);
        String[] t2 = orgs[0].getRelationS("lom.general.title");
        assertEquals(1, t2.length);
        assertEquals("Example Organization", t2[0]);
        SvoReference[] res = root.getRelationR("imscp.resource");
        assertEquals(14, res.length);
        SvoReference r = null;
        for (SvoReference ref : res) {
            String[] t3 = ref.getRelationS("lom.general.title");
            if (t3.length > 0) {
                r = ref;
                assertEquals("Resource One", t3[0]);
            }
        }
        assertNotNull(r);
        SvoReference[] files = r.getRelationR("imscp.file");
        assertEquals(1, files.length);
        String[] t4 = files[0].getRelationS("lom.general.title");
        assertEquals(1, t4.length);
        assertEquals("Lesson One", t4[0]);
        String[] composite = root.getRelationS("qticp.composite");
        assertEquals(1, composite.length);
    }

    @Test
    public void testGeneration() throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        ContentPackage cp = factory.createEmptyPackage();
        assertNotNull(cp);
        Manifest manifest = cp.getRootManifest();
        assertNotNull(manifest);
        manifest.setIdentifier("ID");
        assertEquals("ID", manifest.getIdentifier());
        manifest.setVersion("2.71828");
        assertEquals("2.71828", manifest.getVersion());
        manifest.setManifestXmlBase("urn");
        assertEquals("urn", manifest.getManifestXmlBase());
        manifest.setDefaultOrganization("ORG1");
        assertEquals("ORG1", manifest.getDefaultOrganization());
        manifest.setResourcesXmlBase("urn2");
        assertEquals("urn2", manifest.getResourcesXmlBase());
        Organization r1 = manifest.addOrganization();
        Organization r2 = manifest.addOrganization();
        assertEquals(2, manifest.getOrganizations().length);
        r1.setIdentifier("ORG1");
        r1.setStructure("chaos");
        r1.setTitle("Default Organization");
        r2.setIdentifier("ORG2");
        assertEquals("ORG1", r1.getIdentifier());
        assertEquals("Default Organization", r1.getTitle());
        assertEquals("chaos", r1.getStructure());
        assertEquals("ORG1", manifest.getOrganizations()[0].getIdentifier());
        assertEquals("ORG2", manifest.getOrganizations()[1].getIdentifier());
        OrganizationItem[] items = new OrganizationItem[10];
        for (int i = 0; i < 10; i++) {
            items[i] = manifest.createItem();
            items[i].setIdentifier("I" + (i + 1));
        }
        assertEquals("I1", items[0].getIdentifier());
        items[0].setIdentifierRef("R1");
        assertEquals("R1", items[0].getIdentifierRef());
        items[0].setIsVisible("false");
        assertEquals("false", items[0].isVisible());
        items[0].setParameters("k=v");
        assertEquals("k=v", items[0].getParameters());
        items[0].setTitle("The First Item");
        assertEquals("The First Item", items[0].getTitle());
        int[] links = new int[] { 1, 2, 2, 3, 2, 4, 1, 5, 5, 6, 5, 7, 1, 8, 9, 10 };
        for (int i = 0; i < links.length / 2; i++) items[links[i * 2] - 1].addItem(items[links[i * 2 + 1] - 1]);
        r1.addItem(items[0]);
        r1.addItem(items[8]);
        assertEquals(2, r1.getChildItems().length);
        assertEquals("I1", r1.getChildItems()[0].getIdentifier());
        assertEquals("I9", r1.getChildItems()[1].getIdentifier());
        assertEquals(3, r1.getChildItems()[0].getChildItems().length);
        assertEquals("I2", r1.getChildItems()[0].getChildItems()[0].getIdentifier());
        assertEquals("I5", r1.getChildItems()[0].getChildItems()[1].getIdentifier());
        assertEquals("I8", r1.getChildItems()[0].getChildItems()[2].getIdentifier());
        assertEquals(2, r1.getChildItems()[0].getChildItems()[0].getChildItems().length);
        assertEquals("I3", r1.getChildItems()[0].getChildItems()[0].getChildItems()[0].getIdentifier());
        assertEquals("I4", r1.getChildItems()[0].getChildItems()[0].getChildItems()[1].getIdentifier());
        assertEquals(2, r1.getChildItems()[0].getChildItems()[1].getChildItems().length);
        assertEquals("I6", r1.getChildItems()[0].getChildItems()[1].getChildItems()[0].getIdentifier());
        assertEquals("I7", r1.getChildItems()[0].getChildItems()[1].getChildItems()[1].getIdentifier());
        assertEquals(1, r1.getChildItems()[1].getChildItems().length);
        assertEquals("I10", r1.getChildItems()[1].getChildItems()[0].getIdentifier());
        Resource s1 = manifest.addResource();
        Resource s2 = manifest.addResource();
        s1.setXmlBase("urn3");
        assertEquals("urn3", s1.getXmlBase());
        s1.setHref("urn4");
        assertEquals("urn4", s1.getHref());
        s1.setType("test");
        assertEquals("test", s1.getType());
        s1.setIdentifier("R1");
        s2.setIdentifier("R2");
        assertEquals(2, manifest.getResources().length);
        assertEquals("R1", manifest.getResources()[0].getIdentifier());
        assertEquals("R2", manifest.getResources()[1].getIdentifier());
        ManifestFile m1 = s1.addFile("urn5");
        assertEquals("urn5", m1.getHref());
        m1.setHref("urn6");
        assertEquals("urn6", m1.getHref());
        assertEquals(1, s1.getFiles().length);
        assertEquals("urn6", s1.getFiles()[0].getHref());
        ManifestDependency d1 = s1.addDependency("urn7");
        assertEquals("urn7", d1.getIdentifierRef());
        d1.setIdentifierRef("urn8");
        assertEquals("urn8", d1.getIdentifierRef());
        assertEquals(1, s1.getDependencies().length);
        assertEquals("urn8", s1.getDependencies()[0].getIdentifierRef());
    }

    private LomMetadata createExampleMetadata(LomCreator creator, String title) {
        creator.setDefaultLanguage("en");
        LomMetadata md = creator.getMetadata();
        md.addGeneral();
        md.getGeneral().setTitle(creator.createString(title));
        return md;
    }

    private void checkExampleMetadataOkay(BuildsSvo md, String title) {
        assertTrue(md instanceof LomMetadata);
        assertEquals(title, ((LomMetadata) md).getGeneral().getTitle().getValue(null));
    }

    @Test
    public void testMetadataGeneration() throws Exception {
        ZipFilePackageFactory cpfactory = new ZipFilePackageFactory();
        ContentPackage cp = cpfactory.createEmptyPackage();
        assertNotNull(cp);
        Manifest manifest = cp.getRootManifest();
        assertNotNull(manifest);
        LomParserFactoryFactory lomfactory = new LomParserFactoryFactoryImpl();
        LomCreator creator = lomfactory.createCreator();
        Manifest mf = cp.getRootManifest();
        mf.addMetadata();
        mf.getMetadata().addMetadata(createExampleMetadata(creator, "root"));
        assertEquals(1, mf.getMetadata().getMetadata().length);
        checkExampleMetadataOkay(mf.getMetadata().getMetadata()[0], "root");
        Organization r1 = mf.addOrganization();
        r1.addMetadata();
        r1.getMetadata().addMetadata(createExampleMetadata(creator, "organization"));
        assertEquals(1, r1.getMetadata().getMetadata().length);
        checkExampleMetadataOkay(r1.getMetadata().getMetadata()[0], "organization");
        OrganizationItem i1 = mf.createItem();
        r1.addItem(i1);
        i1.addMetadata();
        i1.getMetadata().addMetadata(createExampleMetadata(creator, "item"));
        assertEquals(1, i1.getMetadata().getMetadata().length);
        checkExampleMetadataOkay(i1.getMetadata().getMetadata()[0], "item");
        Resource s1 = mf.addResource();
        s1.addMetadata();
        s1.getMetadata().addMetadata(createExampleMetadata(creator, "resource"));
        assertEquals(1, s1.getMetadata().getMetadata().length);
        checkExampleMetadataOkay(s1.getMetadata().getMetadata()[0], "resource");
        ManifestFile f1 = s1.addFile("f1");
        f1.addMetadata();
        f1.getMetadata().addMetadata(createExampleMetadata(creator, "file"));
        assertEquals(1, f1.getMetadata().getMetadata().length);
        checkExampleMetadataOkay(f1.getMetadata().getMetadata()[0], "file");
    }

    @Test
    public void testMetadataModification() throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        QTIParserFactoryFactory qticpp = new QTIParserFactoryFactoryImpl();
        factory.registerMetadataParser(qticpp.getParserFactory());
        factory.registerMetadataParser(lomp.getParserFactory());
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(full);
        ContentPackage cp = parser.getPackage();
        assertNotNull(cp);
        Manifest mf = cp.getRootManifest();
        assertEquals("IMS CP 1.1.4", mf.getVersion());
        mf.setVersion("hello");
        assertEquals("hello", mf.getVersion());
        assertEquals(2, mf.getMetadata().getMetadata().length);
        BuildsSvo[] mds = mf.getMetadata().getMetadata();
        LomMetadata md = null;
        if (mds[0] instanceof LomMetadata) md = (LomMetadata) mds[0]; else md = (LomMetadata) mds[1];
        md.addGeneral();
        md.getGeneral().setTitle(md.getCreator().createString("test"));
        assertEquals("test", md.getGeneral().getTitle().getValue(null));
        QTIMetadata qmd = null;
        if (mds[0] instanceof QTIMetadata) qmd = (QTIMetadata) mds[0]; else qmd = (QTIMetadata) mds[1];
        qmd.setComposite("true");
        assertEquals("true", qmd.getComposite());
    }

    private void checkInputStream(InputStream in, byte[] cmp, boolean all) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        IOUtils.copy(in, stream);
        byte[] out = stream.toByteArray();
        if (all) assertEquals(cmp.length, out.length);
        for (int i = 0; i < cmp.length; i++) assertEquals(cmp[i], out[i]);
    }

    @Test
    public void testFileCreation() throws Exception {
        ZipFilePackageFactory cpfactory = new ZipFilePackageFactory();
        ContentPackage cp = cpfactory.createEmptyPackage();
        assertNotNull(cp);
        PackageDirectory root = cp.getRootDirectory();
        assertNotNull(root);
        PackageDirectory sub = root.createChild("sub");
        root.addFile("f1", new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
        sub.addFile("f2", new ByteArrayInputStream(new byte[] { 4, 5, 6 }));
        checkInputStream(((PackageFile) root.getChild("f1")).getDataStream(), new byte[] { 1, 2, 3 }, true);
        checkInputStream(((PackageFile) ((PackageDirectory) root.getChild("sub")).getChild("f2")).getDataStream(), new byte[] { 4, 5, 6 }, true);
    }

    @Test
    public void testFileModification() throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        factory.registerMetadataParser(lomp.getParserFactory());
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(full);
        ContentPackage cp = parser.getPackage();
        assertNotNull(cp);
        PackageDirectory root = cp.getRootDirectory();
        assertNotNull(root);
        PackageFile file = (PackageFile) root.getChild("intro1.htm");
        checkInputStream(file.getDataStream(), new byte[] { '<', 'h', 't' }, false);
        root.addFile("intro1.htm", new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
        checkInputStream(file.getDataStream(), new byte[] { 1, 2, 3 }, false);
    }

    private void validate(InputStream in) throws Exception {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        InputStream rules = Thread.currentThread().getContextClassLoader().getResourceAsStream("imscp_v1p1.xsd");
        javax.xml.validation.Schema schema = sf.newSchema(new StreamSource(rules));
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(in));
    }

    private void validate_zip(File file) throws Exception {
        ZipInputStream in = new ZipInputStream(new FileInputStream(file));
        while (true) {
            ZipEntry entry = in.getNextEntry();
            if (entry == null) break;
            if (!"imsmanifest.xml".equals(entry.getName())) continue;
            validate(in);
            return;
        }
        assertTrue(false);
    }

    @Test
    public void testSerialization() throws Exception {
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        QTIParserFactoryFactory qticpp = new QTIParserFactoryFactoryImpl();
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        factory.registerMetadataParser(lomp.getParserFactory());
        factory.registerMetadataParser(qticpp.getParserFactory());
        factory.registerSubSerializer(lomp.createCreator().getSerizalizer());
        factory.registerSubSerializer(qticpp.createCreator().getSerizalizer());
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(full);
        ContentPackage cp = parser.getPackage();
        assertNotNull(cp);
        File out = File.createTempFile("test", "zip");
        out.deleteOnExit();
        parser.serialize(out);
        validate_zip(out);
        ZipFilePackageParser parser2 = factory.createParser();
        parser2.parse(out);
        ContentPackage cp2 = parser2.getPackage();
        assertNotNull(cp2);
        BuildsSvo[] mds = cp2.getRootManifest().getMetadata().getMetadata();
        assertEquals(2, mds.length);
        LomMetadata lmd = null;
        if (mds[0] instanceof LomMetadata) lmd = (LomMetadata) mds[0]; else lmd = (LomMetadata) mds[1];
        QTIMetadata qmd = null;
        if (mds[0] instanceof QTIMetadata) qmd = (QTIMetadata) mds[0]; else qmd = (QTIMetadata) mds[1];
        assertEquals("IMS Content Packaging Sample - All Elements", lmd.getGeneral().getTitle().getValue(null));
        assertEquals("false", qmd.getComposite());
        PackageDirectory root = cp2.getRootDirectory();
        assertNotNull(root);
        PackageFile file = (PackageFile) root.getChild("intro1.htm");
        checkInputStream(file.getDataStream(), new byte[] { '<', 'h', 't' }, false);
    }

    private void add_file(ContentPackage cp, String filename, String data) throws Exception {
        PackageDirectory root = cp.getRootDirectory();
        root.addFile(filename, new ByteArrayInputStream(data.getBytes("UTF-8")));
    }

    private void check_file(InputStream in, String cmp) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        assertEquals(out.toString(), cmp);
    }

    @Test
    public void testCompletelyNew() throws Exception {
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        QTIParserFactoryFactory qticpp = new QTIParserFactoryFactoryImpl();
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        factory.registerMetadataParser(lomp.getParserFactory());
        factory.registerSubSerializer(lomp.createCreator().getSerizalizer());
        factory.registerMetadataParser(qticpp.getParserFactory());
        factory.registerSubSerializer(qticpp.createCreator().getSerizalizer());
        ContentPackage cp = factory.createEmptyPackage();
        Manifest manifest = cp.getRootManifest();
        manifest.setDefaultOrganization("default");
        manifest.setIdentifier("imscp-pgs-unit-test");
        Organization org = manifest.addOrganization();
        org.setIdentifier("default");
        OrganizationItem item = manifest.createItem();
        item.setIdentifier("item1");
        item.setIdentifierRef("resource1");
        item.setTitle("Hello, World!");
        org.addItem(item);
        Resource res = manifest.addResource();
        res.setIdentifier("resource1");
        res.addDependency("resource2");
        res.addFile("hello.html");
        Resource res2 = manifest.addResource();
        res2.setIdentifier("resource2");
        res2.addFile("hello2.html");
        add_file(cp, "hello.html", "Hello, World!");
        add_file(cp, "hello2.html", "Goodbye, Earth?");
        File out = File.createTempFile("test", "zip");
        out.deleteOnExit();
        ZipFilePackageParser parser = factory.createParser();
        parser.setContentPackage(cp);
        parser.serialize(out);
        ZipFilePackageParser parser2 = factory.createParser();
        parser2.parse(out);
        ContentPackage cp2 = parser2.getPackage();
        Manifest manifest2 = cp2.getRootManifest();
        assertEquals("imscp-pgs-unit-test", manifest2.getIdentifier());
        assertEquals("default", manifest2.getDefaultOrganization());
        Organization org2 = manifest2.getOrganizations()[0];
        assertEquals("default", org2.getIdentifier());
        OrganizationItem item2 = org2.getChildItems()[0];
        assertEquals("item1", item2.getIdentifier());
        assertEquals("resource1", item2.getIdentifierRef());
        assertEquals("Hello, World!", item2.getTitle());
        Resource res3 = manifest2.getResources()[0];
        assertEquals("resource1", res3.getIdentifier());
        assertEquals("resource2", res3.getDependencies()[0].getIdentifierRef());
        assertEquals("resource1", manifest2.getResourceFromIdentifier("resource1").getIdentifier());
        assertEquals("hello.html", res3.getFiles()[0].getHref());
        Resource res4 = manifest2.getResources()[1];
        assertEquals("resource2", res4.getIdentifier());
        assertEquals("hello2.html", res4.getFiles()[0].getHref());
        PackageDirectory root = cp2.getRootDirectory();
        check_file(((PackageFile) root.getChild("hello.html")).getDataStream(), "Hello, World!");
        check_file(((PackageFile) root.getChild("hello2.html")).getDataStream(), "Goodbye, Earth?");
    }

    @Test
    public void testGetPackageFile() throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        ParserFactory extra = getExtraParser();
        factory.registerMetadataParser(lomp.getParserFactory());
        factory.registerExtraParser(null, extra);
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(full);
        ContentPackage cp = parser.getPackage();
        assertNotNull(cp);
        Manifest m = cp.getRootManifest();
        assertNotNull(m);
        Resource res = m.getResourceFromIdentifier("RESOURCE14");
        assertNotNull(res);
        ManifestFile[] mfile = res.getFiles();
        assertNotNull(mfile);
        assertEquals(1, mfile.length);
        PackageFile pfile = mfile[0].getPackageFile();
        assertNotNull(pfile);
        assertEquals("nest/nest2.html", pfile.getFullName());
    }

    @Test
    public void testIdentifierSource() throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        ParserFactory extra = getExtraParser();
        factory.registerMetadataParser(lomp.getParserFactory());
        factory.registerExtraParser(null, extra);
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(full);
        ContentPackage cp = parser.getPackage();
        assertNotNull(cp);
        Manifest m = cp.getRootManifest();
        assertNotNull(m);
        IdentifierSource is = m.getIdentifierSource();
        assertTrue(is.isIdentifierUsed("Manifest1-CEC3D3-3201-DF8E-8F42-3CEED12F4197"));
        assertFalse(is.isIdentifierUsed("Manifest1-CEC3D3-3201-DF8E-8F42-3CEED12F4198"));
        assertTrue(is.isIdentifierUsed("TOC1"));
        assertTrue(is.isIdentifierUsed("ITEM1"));
        assertTrue(is.isIdentifierUsed("ITEM12"));
        assertTrue(is.isIdentifierUsed("RESOURCE2"));
        String id = is.getUnusedIdentifier();
        System.err.println(id);
    }

    @Test
    public void testIdentifierRewriting() throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        ParserFactory extra = getExtraParser();
        factory.registerMetadataParser(lomp.getParserFactory());
        factory.registerExtraParser(null, extra);
        factory.registerSubSerializer(lomp.createCreator().getSerizalizer());
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(full);
        ContentPackage cp = parser.getPackage();
        assertNotNull(cp);
        Manifest m = cp.getRootManifest();
        assertNotNull(m);
        IdentifierSource s1 = m.getIdentifierSource();
        ContentPackage cp2 = (ContentPackage) cp.reproduce();
        assertNotNull(cp2);
        cp2.getRootManifest().reidentifyAgainst(s1);
        Manifest m2 = cp2.getRootManifest();
        assertFalse("Manifest1-CEC3D3-3201-DF8E-8F42-3CEED12F4197".equals(m2.getIdentifier()));
        assertTrue("Manifest1-CEC3D3-3201-DF8E-8F42-3CEED12F4197".equals(m.getIdentifier()));
        assertFalse("TOC1".equals(m2.getDefaultOrganization()));
        assertTrue("TOC1".equals(m.getDefaultOrganization()));
        assertFalse("TOC1".equals(m2.getOrganizations()[0].getIdentifier()));
        assertTrue("TOC1".equals(m.getOrganizations()[0].getIdentifier()));
        OrganizationItem item = m.getOrganizations()[0].getChildItems()[0].getChildItems()[0];
        OrganizationItem item2 = m2.getOrganizations()[0].getChildItems()[0].getChildItems()[0];
        assertFalse("ITEM2".equals(item2.getIdentifier()));
        assertTrue("ITEM2".equals(item.getIdentifier()));
        assertFalse("RESOURCE2".equals(item2.getIdentifierRef()));
        assertTrue("RESOURCE2".equals(item.getIdentifierRef()));
        Resource r = m.getResources()[0];
        Resource r2 = m2.getResources()[0];
        assertTrue("RESOURCE1".equals(r.getIdentifier()));
        assertFalse("RESOURCE1".equals(r2.getIdentifier()));
        assertTrue("RESOURCE2".equals(r.getDependencies()[0].getIdentifierRef()));
        assertFalse("RESOURCE2".equals(r2.getDependencies()[0].getIdentifierRef()));
    }

    @Test
    public void testXmlBaseResolution() throws Exception {
        resolutionAndStripTest(false);
    }

    private void resolutionAndStripTest(boolean strip) throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        ParserFactory extra = getExtraParser();
        factory.registerMetadataParser(lomp.getParserFactory());
        factory.registerExtraParser(null, extra);
        factory.registerSubSerializer(lomp.createCreator().getSerizalizer());
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(xmlbase);
        ContentPackage cp = parser.getPackage();
        assertNotNull(cp);
        Manifest m = cp.getRootManifest();
        assertNotNull(m);
        if (strip) m.stripXmlBases();
        ManifestFile f1 = m.getResourceFromIdentifier("ID1").getFiles()[0];
        assertNotNull(f1);
        ManifestFile f2 = m.getResourceFromIdentifier("ID2").getFiles()[0];
        assertNotNull(f2);
        PackageFile pf = f1.getPackageFile();
        assertNotNull(pf);
        PackageFile pf2 = f2.getPackageFile();
        assertNull(pf2);
        assertEquals("a/b/c/d/e/f/g", pf.getFullName());
        assertEquals("a/b/c/d/e/f/g", f1.getURI().toASCIIString());
        assertEquals("http://foo.bar/baz/quux.html", f2.getURI().toASCIIString());
        if (strip) {
            assertNull(m.getManifestXmlBase());
            assertNull(m.getResourcesXmlBase());
            assertNull(m.getResourceFromIdentifier("ID1").getXmlBase());
            assertNull(m.getResourceFromIdentifier("ID2").getXmlBase());
        }
    }

    @Test
    public void testPrefixing() throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        ParserFactory extra = getExtraParser();
        factory.registerMetadataParser(lomp.getParserFactory());
        factory.registerExtraParser(null, extra);
        factory.registerSubSerializer(lomp.createCreator().getSerizalizer());
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(full);
        ContentPackage cp = parser.getPackage();
        assertNotNull(cp);
        cp.prefix("nest");
        File tmp = File.createTempFile("test", ".zip");
        tmp.deleteOnExit();
        ZipFilePackageParser parser2 = factory.createParser();
        parser2.setContentPackage(cp);
        parser2.serialize(tmp);
        ZipFilePackageParser parser3 = factory.createParser();
        parser3.parse(tmp);
        ContentPackage cp2 = parser.getPackage();
        assertNotNull(cp2);
        PackageDirectory root = cp2.getRootDirectory();
        assertNotNull(root);
        PackageDirectory next = (PackageDirectory) root.getChild("nest");
        assertNotNull(next);
        PackageDirectory nest = (PackageDirectory) next.getChild("nest");
        assertNotNull(nest);
        PackageFile file = (PackageFile) nest.getChild("nest2.html");
        assertNotNull(file);
        Resource r2 = cp2.getRootManifest().getResourceFromIdentifier("RESOURCE2");
        assertEquals("nest/intro1.htm", r2.getHref());
        assertEquals("nest/intro1.htm", r2.getFiles()[0].getHref());
    }

    @Test
    public void testXmlBaseStripping() throws Exception {
        resolutionAndStripTest(true);
    }

    @Test
    public void testMergingFull() throws Exception {
        testMerging(true);
    }

    @Test
    public void testMergingXmlBase() throws Exception {
        testMerging(false);
    }

    private void testMerging(boolean usefull) throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        ParserFactory extra = getExtraParser();
        factory.registerMetadataParser(lomp.getParserFactory());
        factory.registerExtraParser(null, extra);
        factory.registerSubSerializer(lomp.createCreator().getSerizalizer());
        ZipFilePackageParser parser = factory.createParser();
        if (usefull) parser.parse(full); else parser.parse(xmlbase);
        ContentPackage cp = parser.getPackage();
        assertNotNull(cp);
        ContentPackage cp2 = cp.reproduce();
        cp2.prefix("two");
        cp.prefix("one");
        cp2.getRootManifest().reidentifyAgainst(cp.getRootManifest().getIdentifierSource());
        cp2.getRootManifest().stripXmlBases();
        cp.merge(cp2);
        if (usefull) {
            Organization g = cp.getRootManifest().getOrganizationFromIdentifier("TOC1");
            OrganizationItem i1 = g.getChildItems()[0];
            assertNotNull(i1);
            Resource r1 = i1.getCorrespondingResource();
            assertNotNull(r1);
            PackageFile f1 = r1.getFiles()[0].getPackageFile();
            assertNotNull(f1);
            assertEquals("one/lesson1.htm", f1.getFullName());
            streamContains("Lesson 1", f1.getDataStream());
            OrganizationItem i2 = g.getChildItems()[3];
            assertNotNull(i2);
            Resource r2 = i2.getCorrespondingResource();
            assertNotNull(r2);
            PackageFile f2 = r2.getFiles()[0].getPackageFile();
            assertNotNull(f2);
            assertEquals("two/lesson1.htm", f2.getFullName());
            streamContains("Lesson 1", f2.getDataStream());
        } else {
            Organization g = cp.getRootManifest().getOrganizationFromIdentifier("DEFAULT");
            assertEquals(4, g.getChildItems().length);
            String[] urls = new String[] { "one/a/b/c/d/e/f/g", "two/a/b/c/d/e/f/g", "http://foo.bar/baz/quux.html" };
            for (int i = 0; i < 4; i++) {
                OrganizationItem t = g.getChildItems()[i];
                assertNotNull(t);
                Resource r = t.getCorrespondingResource();
                assertNotNull(r);
                boolean k = false;
                for (int j = 0; j < urls.length; j++) if (urls[j].equals(r.getFiles()[0].getURI().toASCIIString())) k = true;
                assertTrue(k);
                PackageFile f = r.getFiles()[0].getPackageFile();
                if (f != null) streamContains("Hello, World", f.getDataStream());
            }
        }
    }

    @Test
    public void testMetadata() throws Exception {
        ZipFilePackageFactory factory = new ZipFilePackageFactory();
        LomParserFactoryFactory lomp = new LomParserFactoryFactoryImpl();
        QTIParserFactoryFactory qticpp = new QTIParserFactoryFactoryImpl();
        factory.registerMetadataParser(lomp.getParserFactory());
        factory.registerSubSerializer(lomp.createCreator().getSerizalizer());
        factory.registerMetadataParser(qticpp.getParserFactory());
        factory.registerSubSerializer(qticpp.createCreator().getSerizalizer());
        ZipFilePackageParser parser = factory.createParser();
        parser.parse(full);
        ContentPackage cp = parser.getPackage();
        assertNotNull(cp);
        Manifest m = cp.getRootManifest();
        assertNotNull(m);
        MetadataStore ms = new MetadataStoreImpl();
        ms.addDefault("language", "en");
        m.buildMetadata(null, ms);
        for (String key : ms.getKeys()) {
            System.err.println(key + " ::= " + ms.getKey(key).getStringValue());
        }
        assertTrue(ms.getKey("http://www.caret.cam.ac.uk/minibix/lom/general/title").getStringValue().contains("en-US = IMS Content Packaging Sample - All Elements"));
        assertEquals("false", ms.getKey("http://www.caret.cam.ac.uk/minibix/cp-qti/composite").getStringValue());
        ms.addRegistry(lomp.getTypeRegistry());
        ms.addRegistry(qticpp.getTypeRegistry());
        MetadataKey k = ms.getOrCreateKey("http://www.caret.cam.ac.uk/minibix/lom/general/title");
        k.setStringValue("fr = bozo / bobo");
        m.setMetadata(ms, "http://www.caret.cam.ac.uk/minibix/lom/general/title", null);
        MetadataKey k2 = ms.getOrCreateKey("http://www.caret.cam.ac.uk/minibix/cp-qti/composite");
        k2.setStringValue("true");
        m.setMetadata(ms, "http://www.caret.cam.ac.uk/minibix/cp-qti/composite", null);
        LomMetadata lmd = null;
        QTIMetadata qmd = null;
        BuildsSvo[] mds = cp.getRootManifest().getMetadata().getMetadata();
        if (mds[0] instanceof LomMetadata) lmd = (LomMetadata) mds[0]; else lmd = (LomMetadata) mds[1];
        if (mds[0] instanceof QTIMetadata) qmd = (QTIMetadata) mds[0]; else qmd = (QTIMetadata) mds[1];
        assertEquals("bobo", lmd.getGeneral().getTitle().getValue("en"));
        assertEquals("bozo", lmd.getGeneral().getTitle().getValue("fr"));
        assertEquals("true", qmd.getComposite());
    }
}
