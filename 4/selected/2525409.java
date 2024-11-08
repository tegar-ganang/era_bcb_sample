package vobs.webapp;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import org.apache.log4j.*;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;
import org.xmldb.api.base.Collection;
import org.xmldb.api.modules.*;
import vobs.dbaccess.*;
import wdc.settings.*;
import vobs.datamodel.VO;

public class LoaderEngineThread extends Thread {

    private Vector sourceDataFilesDirs = null;

    private File sourceMetaFilesDir = null;

    private Vector targetDataFilesDirs = null;

    private SAXBuilder build = new SAXBuilder();

    Logger logger = Logger.getLogger(LoaderEngine.class);

    private XMLOutputter outXml = new XMLOutputter(Format.getPrettyFormat());

    private static String userDB = Settings.get("vo_meta.userProfilesResource");

    private static String httpURI = Settings.get("vo_meta.httpUri");

    private static String xmldbURI = Settings.get("vo_meta.uri");

    private static String logsDB = Settings.get("vo_meta.logsResource");

    private static String settingsDB = Settings.get("vo_meta.settingsResource");

    private static String forumDB = Settings.get("vo_meta.forumResource");

    private static String timeZone = Settings.get("vo_meta.timeZone");

    private static String rootDB = Settings.get("vo_meta.rootCollection");

    private static int curDirWriteTo = 0;

    public LoaderEngineThread(Vector sourceDataFilesDirs, File sourceMetaFilesDir, Vector targetDataFilesDirs) {
        this.sourceDataFilesDirs = sourceDataFilesDirs;
        this.sourceMetaFilesDir = sourceMetaFilesDir;
        this.targetDataFilesDirs = targetDataFilesDirs;
    }

    public void run() {
        XmlFilesFilter filter = new XmlFilesFilter();
        String pathTemp = Settings.get("vo_store.databaseMetaCollection");
        String sectionName = pathTemp.substring(1, pathTemp.indexOf("/", 2));
        String templateName = VOAccess.getElementByName(settingsDB, "TEMPLATE", sectionName);
        String schemaName = VOAccess.getElementByName(settingsDB, "SCHEMA", sectionName);
        byte[] buf = new byte[1024];
        Hashtable templateElements = null;
        try {
            URL xmlTemplateUrl = new URL(httpURI + settingsDB + "/" + templateName);
            URL getDocPathsAndValuesXslUrl = new URL(httpURI + settingsDB + "/" + "getDocPathsValuesAndDisplays.xsl");
            org.w3c.dom.Document curTemplateXml = VOAccess.readDocument(xmlTemplateUrl);
            DOMResult templateResult = new DOMResult();
            InputStream tempInput = getDocPathsAndValuesXslUrl.openStream();
            javax.xml.transform.sax.SAXSource tempXslSource = new javax.xml.transform.sax.SAXSource(new org.xml.sax.InputSource(tempInput));
            Transformer trans = TransformerFactory.newInstance().newTransformer(tempXslSource);
            trans.setParameter("schemaUrl", httpURI + settingsDB + "/" + schemaName);
            trans.transform(new javax.xml.transform.dom.DOMSource(curTemplateXml), templateResult);
            tempInput.close();
            templateElements = VOAccess.displaysToHashtable(templateResult);
            ((CollectionManagementService) CollectionsManager.getService(xmldbURI + rootDB, false, "CollectionManager")).createCollection(rootDB + pathTemp);
        } catch (Exception ex) {
            logger.error("Error parsing input document", ex);
            ex.printStackTrace();
        }
        while (true) {
            File[] fileList = sourceMetaFilesDir.listFiles(filter);
            for (int i = 0; i < Math.min(fileList.length, 500); i++) {
                File newFile = fileList[i];
                try {
                    Document metaDoc = build.build(newFile);
                    Element metaElm = metaDoc.getRootElement();
                    String dataFileName = metaElm.getChildText("Content"), previewFileName = metaElm.getChildText("Preview");
                    String objId = VOAccess.getUniqueId();
                    metaElm.getChild("Content").setText("videostore?type=doc&objId=" + objId);
                    metaElm.getChild("Preview").setText("videostore?type=preview&objId=" + objId);
                    boolean found = false;
                    for (Iterator it = sourceDataFilesDirs.iterator(); it.hasNext() && !found; ) {
                        String sourceDataFilesDir = (String) it.next();
                        File dataInput = new File(sourceDataFilesDir + "/" + dataFileName);
                        if (dataInput.exists()) {
                            found = true;
                            BufferedInputStream inp = new BufferedInputStream(new FileInputStream(dataInput));
                            FileOutputStream outp = new FileOutputStream(new File(targetDataFilesDirs.get(curDirWriteTo) + "/" + objId + ".dat"));
                            int read = inp.read(buf, 0, buf.length);
                            while (read > 0) {
                                outp.write(buf, 0, read);
                                read = inp.read(buf, 0, buf.length);
                            }
                            inp.close();
                            outp.flush();
                            outp.close();
                            dataInput = new File(sourceDataFilesDir + "/" + previewFileName);
                            inp = new BufferedInputStream(new FileInputStream(dataInput));
                            outp = new FileOutputStream(new File(targetDataFilesDirs.get(curDirWriteTo) + "/" + objId + ".jpg"));
                            read = inp.read(buf, 0, buf.length);
                            while (read > 0) {
                                outp.write(buf, 0, read);
                                read = inp.read(buf, 0, buf.length);
                            }
                            inp.close();
                            outp.flush();
                            outp.close();
                            curDirWriteTo++;
                            if (curDirWriteTo >= targetDataFilesDirs.size()) {
                                curDirWriteTo = 0;
                            }
                        }
                    }
                    if (!found) {
                        newFile.renameTo(new File(newFile.getAbsolutePath() + ".not_found"));
                    } else {
                        String title = getValueByPath((String) templateElements.get("title"), metaDoc.getRootElement());
                        String description = getValueByPath((String) templateElements.get("description"), metaDoc.getRootElement());
                        String onlink = "";
                        if (null != templateElements.get("onlink")) {
                            onlink = getValueByPath((String) templateElements.get("onlink"), metaDoc.getRootElement());
                        }
                        String ncover = "";
                        if (null != templateElements.get("ncover")) {
                            ncover = getValueByPath((String) templateElements.get("ncover"), metaDoc.getRootElement());
                        }
                        String wcover = "";
                        if (null != templateElements.get("wcover")) {
                            wcover = getValueByPath((String) templateElements.get("wcover"), metaDoc.getRootElement());
                        }
                        String ecover = "";
                        if (null != templateElements.get("ecover")) {
                            ecover = getValueByPath((String) templateElements.get("ecover"), metaDoc.getRootElement());
                        }
                        String scover = "";
                        if (null != templateElements.get("scover")) {
                            scover = getValueByPath((String) templateElements.get("scover"), metaDoc.getRootElement());
                        }
                        String datefrom = "";
                        if (null != templateElements.get("datefrom")) {
                            datefrom = getValueByPath((String) templateElements.get("datefrom"), metaDoc.getRootElement());
                        }
                        String dateto = "";
                        if (null != templateElements.get("dateto")) {
                            dateto = getValueByPath((String) templateElements.get("dateto"), metaDoc.getRootElement());
                        }
                        String previewimg = "";
                        if (null != templateElements.get("previewimg")) {
                            previewimg = getValueByPath((String) templateElements.get("previewimg"), metaDoc.getRootElement());
                        }
                        String discRestr = "false";
                        String votingRestr = "false";
                        datefrom = VOAccess.parseDate(datefrom, "yyyy-MM-dd'T'HH:mm:ss", VO.defaultTimeFormat.toPattern());
                        dateto = VOAccess.parseDate(datefrom, "yyyy-MM-dd'T'HH:mm:ss", VO.defaultTimeFormat.toPattern());
                        Hashtable discussionFields = new Hashtable();
                        discussionFields.put("OBJECT_ID", objId);
                        discussionFields.put("AUTHOR_ID", "auto");
                        discussionFields.put("AUTHOR_NAME", "auto");
                        discussionFields.put("OBJECT_SECTION", sectionName);
                        discussionFields.put("OBJECT_PATH", pathTemp);
                        discussionFields.put("FILE_PATH", "");
                        discussionFields.put("TITLE", title);
                        discussionFields.put("DESCRIPTION", description);
                        discussionFields.put("ONLINK", onlink);
                        discussionFields.put("NCOVER", ncover);
                        discussionFields.put("ECOVER", ecover);
                        discussionFields.put("SCOVER", scover);
                        discussionFields.put("WCOVER", wcover);
                        discussionFields.put("PERIOD_START", datefrom);
                        discussionFields.put("PERIOD_END", dateto);
                        discussionFields.put("PREVIEW_IMG", previewimg);
                        discussionFields.put("DISCUSSRESTRICTION", discRestr);
                        discussionFields.put("VOTINGRESTRICTION", votingRestr);
                        VOAccess.createDiscussionFile(discussionFields);
                        VOAccess.updateLastItem(objId, sectionName);
                        Collection col = CollectionsManager.getCollection(rootDB + pathTemp, true);
                        XMLResource document = (XMLResource) col.createResource(objId + ".xml", XMLResource.RESOURCE_TYPE);
                        document.setContent(outXml.outputString(metaElm));
                        col.storeResource(document);
                        Indexer.index(objId);
                        newFile.delete();
                    }
                } catch (Exception ex) {
                    logger.error("Error parsing input document", ex);
                    ex.printStackTrace();
                    newFile.renameTo(new File(newFile.getAbsolutePath() + ".bad"));
                }
            }
            try {
                this.sleep(600000);
            } catch (InterruptedException ex1) {
                ex1.printStackTrace();
            }
        }
    }

    private String getValueByPath(String path, Element doc) {
        StringTokenizer tok = new StringTokenizer(path, "/");
        Element nextElm = doc;
        tok.nextToken();
        while (tok.hasMoreElements()) {
            String nextChild = tok.nextToken();
            nextElm = nextElm.getChild(nextChild);
            if (null == nextElm) return "";
        }
        return nextElm.getText();
    }

    private class XmlFilesFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            if (name.endsWith(".xml") || name.endsWith(".XML")) {
                return true;
            }
            return false;
        }
    }
}
