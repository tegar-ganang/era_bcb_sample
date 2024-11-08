package uk.ac.kingston.aqurate.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.cam.caret.imscp.api.BadParseException;
import uk.ac.cam.caret.imscp.api.ContentPackage;
import uk.ac.cam.caret.imscp.api.Manifest;
import uk.ac.cam.caret.imscp.api.Metadata;
import uk.ac.cam.caret.imscp.api.PackageDirectory;
import uk.ac.cam.caret.imscp.api.Resource;
import uk.ac.cam.caret.imscp.api.OrganizationItemHolder;
import uk.ac.cam.caret.imscp.api.Organization;
import uk.ac.cam.caret.imscp.api.OrganizationItem;
import uk.ac.cam.caret.imscp.impl.ZipFilePackageFactory;
import uk.ac.cam.caret.imscp.impl.ZipFilePackageParser;
import uk.ac.cam.caret.lom.api.Contribution;
import uk.ac.cam.caret.lom.api.LomCreator;
import uk.ac.cam.caret.lom.api.LomDate;
import uk.ac.cam.caret.lom.api.LomGeneral;
import uk.ac.cam.caret.lom.api.LomLifecycle;
import uk.ac.cam.caret.lom.api.LomMetadata;
import uk.ac.cam.caret.lom.api.LomParserFactoryFactory;
import uk.ac.cam.caret.lom.api.SourceValue;
import uk.ac.cam.caret.lom.api.VCard;
import uk.ac.cam.caret.lom.impl.LomParserFactoryFactoryImpl;
import uk.ac.cam.caret.minibix.taggy.Unserializable;
import uk.ac.cam.caret.qticp.api.QTICPCreator;
import uk.ac.cam.caret.qticp.api.QTIMetadata;
import uk.ac.cam.caret.qticp.api.QTIParserFactoryFactory;
import uk.ac.cam.caret.qticp.impl.QTIParserFactoryFactoryImpl;
import uk.ac.kingston.aqurate.author_UI.AqurateFramework;
import uk.ac.kingston.aqurate.author_UI.ExportContentPackagePanel.Imgs;
import uk.ac.kingston.aqurate.author_documents.AssessmentItemDoc;
import uk.ac.kingston.aqurate.author_UI.JDialogNewQuiz;

public class ContentPackageBuilder {

    /********************* rockdrigom*/
    private List<AssessmentItemDoc> documentList_;

    private Document[] doc_ = new Document[10];

    private ArrayList<String> lsImages_ = null;

    private String[] sXml_ = new String[10];

    private String[] fileName_ = new String[10];

    private String[] sTicket_ = new String[10];

    private String[] sAuthor_ = new String[10];

    ;

    private String[] sLang_ = new String[10];

    ;

    private String[] sDesc_ = new String[10];

    ;

    private String[] sInteractionType_ = new String[10];

    ;

    private String[] sFeedbackType_ = new String[10];

    ;

    private String[] sTimeDependent_ = new String[10];

    ;

    private String[] documentTitle_ = new String[10];

    private int num_of_files_;

    private String[] name_questions;

    private String customFileName = "";

    private int[] selectedQuestions_ = null;

    private Document doc = null;

    private ArrayList<String> lsImages = null;

    private String sXml = null;

    private String fileName;

    private String sTicket;

    private String sAuthor;

    private String sLang;

    private String sDesc;

    private String sInteractionType;

    private String sFeedbackType;

    private String sTimeDependent;

    private File fout = null;

    private AqurateFramework owner = null;

    private String documentTitle;

    private String pathZipFile;

    private String sTempLocation;

    private File filetemp;

    private static final String imsqti_type = "imsqti_item_xmlv2p1";

    private static final String sURL = "http://qtitools.caret.cam.ac.uk/qtibank-webserv/deposits/all";

    public ContentPackageBuilder() {
        lsImages = new ArrayList<String>();
    }

    public ContentPackageBuilder(AqurateFramework owner, Document doc, String sXml, String sTicket, String pathZipFile, String docTitle, String author, String lang, String desc, String intype, String fbtype, String timedep) {
        this.doc = doc;
        this.sXml = sXml;
        this.owner = owner;
        this.sTicket = sTicket;
        this.sAuthor = author;
        this.sLang = lang;
        this.sDesc = desc;
        this.sInteractionType = intype;
        this.sFeedbackType = fbtype;
        this.sTimeDependent = timedep;
        this.pathZipFile = pathZipFile;
        customFileName = docTitle;
        if (this.pathZipFile.equals("")) {
            sTempLocation = System.getProperty("java.io.tmpdir");
            filetemp = new File(System.getProperty("java.io.tmpdir") + "temp.xml");
        } else {
            sTempLocation = this.pathZipFile;
            filetemp = new File(pathZipFile + "temp.xml");
        }
        documentTitle = docTitle;
        fileName = docTitle + ".xml";
        lsImages = new ArrayList<String>();
    }

    public ContentPackageBuilder(AqurateFramework owner, List<AssessmentItemDoc> documentList, String pathZipFile, String customNameFile, int[] selectedQuestions, String[] nom_preguntas, int files) {
        this.owner = owner;
        this.documentList_ = documentList;
        this.pathZipFile = pathZipFile;
        this.name_questions = nom_preguntas;
        this.selectedQuestions_ = selectedQuestions;
        this.customFileName = customNameFile;
        if (this.pathZipFile.equals("")) {
            sTempLocation = System.getProperty("java.io.tmpdir");
            filetemp = new File(System.getProperty("java.io.tmpdir") + "temp.xml");
        } else {
            sTempLocation = this.pathZipFile;
            filetemp = new File(pathZipFile + "temp.xml");
        }
        for (int i = 0; i < files; i++) {
            doc_[i] = documentList_.get(selectedQuestions_[i]).getDomXML();
            sXml_[i] = documentList_.get(selectedQuestions_[i]).getXML();
            sTicket_[i] = documentList_.get(selectedQuestions_[i]).getTicket();
            sAuthor_[i] = documentList_.get(selectedQuestions_[i]).getAuthor();
            sLang_[i] = documentList_.get(selectedQuestions_[i]).getLanguage();
            sDesc_[i] = documentList_.get(selectedQuestions_[i]).getResourceDescription();
            sInteractionType_[i] = documentList_.get(selectedQuestions_[i]).getInteractionType();
            sFeedbackType_[i] = documentList_.get(selectedQuestions_[i]).getFeedbackType();
            sTimeDependent_[i] = "" + documentList_.get(selectedQuestions_[i]).isTimeDependent();
            documentTitle_[i] = name_questions[i];
            fileName_[i] = name_questions[i] + ".xml";
        }
        lsImages = new ArrayList<String>();
    }

    public String obtainTicket() {
        return this.sTicket;
    }

    public File obtainZipFile() {
        return this.fout;
    }

    public void buildPackage(int numfiles) {
        buildContentPackage(numfiles);
        lsImages.clear();
        filetemp.delete();
    }

    public void imagesParserAssesmentItem(int file, int currentquestion, Resource resTemp) {
        NodeList nl = null;
        Node n = null;
        NamedNodeMap nnp = null;
        Node nsrc = null;
        URL url = null;
        String sFilename = "";
        String sNewPath = "";
        int index;
        String sOldPath = "";
        try {
            if (file == 1) {
                nl = doc.getElementsByTagName("img");
            } else {
                nl = doc_[currentquestion].getElementsByTagName("img");
            }
            for (int i = 0; i < nl.getLength(); i++) {
                n = nl.item(i);
                nnp = n.getAttributes();
                nsrc = nnp.getNamedItem("src");
                String sTemp = nsrc.getTextContent();
                url = new URL("file", "localhost", sTemp);
                sOldPath = url.getPath();
                sOldPath = sOldPath.replace('/', File.separatorChar);
                int indexFirstSlash = sOldPath.indexOf(File.separatorChar);
                String sSourcePath = sOldPath.substring(indexFirstSlash + 1);
                index = sOldPath.lastIndexOf(File.separatorChar);
                sFilename = sOldPath.substring(index + 1);
                sNewPath = this.sTempLocation + sFilename;
                FileChannel in = null;
                FileChannel out = null;
                try {
                    in = new FileInputStream(sSourcePath).getChannel();
                    out = new FileOutputStream(sNewPath).getChannel();
                    long size = in.size();
                    MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
                    out.write(buf);
                } finally {
                    if (in != null) in.close();
                    if (out != null) out.close();
                }
                if (file == 1) {
                    sXml = sXml.replace(nsrc.getTextContent(), sFilename);
                } else {
                    sXml_[currentquestion] = sXml_[currentquestion].replace(nsrc.getTextContent(), sFilename);
                }
                lsImages.add(sFilename);
                resTemp.addFile(sFilename);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void objectParserAssesmentItem(int file, int currentquestion, Resource resTemp) {
        NodeList nl = null;
        Node n = null;
        NamedNodeMap nnp = null;
        Node nsrc = null;
        URL url = null;
        String sFilename = "";
        String sNewPath = "";
        int indexLastSeparator;
        String sOldPath = "";
        try {
            if (file == 1) {
                nl = doc.getElementsByTagName("object");
            } else {
                nl = doc_[currentquestion].getElementsByTagName("object");
            }
            for (int i = 0; i < nl.getLength(); i++) {
                n = nl.item(i);
                nnp = n.getAttributes();
                nsrc = nnp.getNamedItem("data");
                String sTemp = nsrc.getTextContent();
                url = new URL("file", "localhost", sTemp);
                sOldPath = url.getFile();
                sOldPath = sOldPath.replace('/', File.separatorChar);
                indexLastSeparator = sOldPath.lastIndexOf(File.separatorChar);
                String sSourcePath = sOldPath;
                sFilename = sOldPath.substring(indexLastSeparator + 1);
                sNewPath = this.sTempLocation + sFilename;
                FileChannel in = null;
                FileChannel out = null;
                try {
                    in = new FileInputStream(sSourcePath).getChannel();
                    out = new FileOutputStream(sNewPath).getChannel();
                    long size = in.size();
                    MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
                    out.write(buf);
                } finally {
                    if (in != null) in.close();
                    if (out != null) out.close();
                }
                if (file == 1) {
                    sXml = sXml.replace(nsrc.getTextContent(), sFilename);
                } else {
                    sXml_[currentquestion] = sXml_[currentquestion].replace(nsrc.getTextContent(), sFilename);
                }
                lsImages.add(sFilename);
                resTemp.addFile(sFilename);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LomMetadata createExampleMetadata(LomCreator creator, String title) {
        creator.setDefaultLanguage("en");
        LomMetadata md = creator.getMetadata();
        md.addGeneral();
        md.getGeneral().setTitle(creator.createString(title));
        return md;
    }

    private void buildContentPackage(int files) {
        LomParserFactoryFactory lomfactory = null;
        QTIParserFactoryFactory qtifactory = null;
        ZipFilePackageFactory zipfactory = null;
        ContentPackage cp = null;
        Manifest manifest = null;
        LomCreator creator = null;
        QTICPCreator qticreator = null;
        LomMetadata lommd = null;
        QTIMetadata qtimd = null;
        LomGeneral general = null;
        LomLifecycle lifecycle = null;
        Contribution contrib = null;
        SourceValue role = null;
        VCard vcard = null;
        LomDate datetime = null;
        Resource res = null;
        Iterator<String> iter = null;
        ZipFilePackageParser parser = null;
        Metadata md = null;
        try {
            lomfactory = new LomParserFactoryFactoryImpl();
            qtifactory = new QTIParserFactoryFactoryImpl();
            zipfactory = new ZipFilePackageFactory();
            zipfactory.registerMetadataParser(lomfactory.getParserFactory());
            zipfactory.registerSubSerializer(lomfactory.createCreator().getSerizalizer());
            zipfactory.registerSubSerializer(qtifactory.createCreator().getSerizalizer());
            cp = zipfactory.createEmptyPackage();
            cp.prefix("lom");
            manifest = cp.getRootManifest();
            manifest.setIdentifier("IMSCP_manifest_Aqurate_" + System.currentTimeMillis());
            manifest.addOrganization();
            for (int i = 0; i < files; i++) {
                res = manifest.addResource();
                res.addMetadata();
                res.getMetadata().addMetadata(general);
                res.setIdentifier("Aqurate_Item_" + i);
                res.setType(imsqti_type);
                if (files == 1) {
                    res.setHref(fileName);
                    res.addFile(fileName);
                } else {
                    res.setHref(fileName_[i]);
                    res.addFile(fileName_[i]);
                }
                imagesParserAssesmentItem(files, i, res);
                objectParserAssesmentItem(files, i, res);
                creator = lomfactory.createCreator();
                creator.setDefaultLanguage(this.sLang);
                qticreator = qtifactory.createCreator();
                lommd = creator.getMetadata();
                lommd.addGeneral();
                if (files == 1) {
                    lommd.getGeneral().setTitle(creator.createString(documentTitle));
                    lommd.getGeneral().addDescription(creator.createString(this.sDesc));
                } else {
                    lommd.getGeneral().setTitle(creator.createString(documentTitle_[i]));
                    lommd.getGeneral().addDescription(creator.createString(this.sDesc_[i]));
                }
                manifest.addMetadata();
                lommd.addLifecycle();
                lifecycle = lommd.getLifecycle();
                contrib = creator.createContribution();
                role = creator.createSourceValue();
                role.setSource(creator.createString("LOMv1.0"));
                role.setValue(creator.createString("Author"));
                contrib.setRole(role);
                vcard = creator.createVCard();
                if (files == 1) {
                    vcard.setCard("BEGIN:vcard FN:" + this.sAuthor + " END:vcard");
                } else {
                    vcard.setCard("BEGIN:vcard FN:" + this.sAuthor_[i] + " END:vcard");
                }
                contrib.addContributor(vcard);
                datetime = creator.createDate();
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                java.util.Date date = new java.util.Date();
                String dt = dateFormat.format(date);
                datetime.setDateTime(dt);
                contrib.setDate(datetime);
                lifecycle.addContribution(contrib);
                qtimd = qticreator.getMetadata();
                if (files == 1) {
                    qtimd.addInteractionType(this.sInteractionType);
                    qtimd.setFeedbackType(this.sFeedbackType);
                    qtimd.setTimeDependent(this.sTimeDependent);
                } else {
                    qtimd.addInteractionType(this.sInteractionType_[i]);
                    qtimd.setFeedbackType(this.sFeedbackType_[i]);
                    qtimd.setTimeDependent(this.sTimeDependent_[i]);
                }
                qtimd.setToolName("AQuRate");
                qtimd.setToolVersion("2.18.4");
                qtimd.setSolutionAvailable("true");
                md = res.getMetadata();
                md.setSchema("IMS Content");
                md.setSchemaVersion("1.1");
                md.addMetadata(qtimd);
                md.addMetadata(lommd);
            }
            try {
                if (this.pathZipFile.equals("")) {
                    fout = File.createTempFile("preview", ".zip");
                } else {
                    if (customFileName == "") {
                        File fDirectory = new File(pathZipFile);
                        fout = File.createTempFile("preview", ".zip", fDirectory);
                    } else {
                        File fDirectory = new File(pathZipFile);
                        fout = File.createTempFile(customFileName, ".zip", fDirectory);
                    }
                }
                if (files == 1) {
                    add_file(cp, fileName, sXml);
                } else {
                    for (int i = 0; i < files; i++) {
                        add_file(cp, fileName_[i], sXml_[i]);
                    }
                }
                JDialogNewQuiz.setZipPath(fout.getAbsolutePath());
                ;
                ArrayList<String> lImgFiles = new ArrayList();
                iter = lsImages.iterator();
                while (iter.hasNext()) {
                    String sImage = (String) iter.next();
                    String sNewPath = sTempLocation + sImage;
                    if (!lImgFiles.contains(sNewPath)) lImgFiles.add(sNewPath);
                    InputStream in = new FileInputStream(new File(sNewPath));
                    PackageDirectory root = cp.getRootDirectory();
                    root.addFile(sImage, in);
                    in.close();
                }
                Iterator it = lImgFiles.iterator();
                while (it.hasNext()) {
                    String sPath = (String) it.next();
                    File fToDelete = new File(sPath);
                    fToDelete.delete();
                }
                parser = zipfactory.createParser();
                FileOutputStream fw = new FileOutputStream(filetemp);
                try {
                    manifest.serialize(fw);
                } catch (Unserializable e) {
                    e.printStackTrace();
                }
                fw.close();
                parser.setContentPackage(cp);
                parser.serialize(fout);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("CP building successful!!");
        } catch (BadParseException e) {
            e.printStackTrace();
        }
    }

    private static void add_file(ContentPackage cp, String filename, String data) throws IOException {
        PackageDirectory root = cp.getRootDirectory();
        root.addFile(filename, new ByteArrayInputStream(data.getBytes("UTF-8")));
    }
}
