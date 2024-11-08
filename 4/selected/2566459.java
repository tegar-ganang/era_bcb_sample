package ces.platform.infoplat.service.indexserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import ces.platform.infoplat.core.DocAffix;
import ces.platform.infoplat.core.DocumentCBF;
import ces.platform.infoplat.core.DocumentPublish;
import ces.platform.infoplat.core.base.BaseClass;
import ces.platform.infoplat.service.indexserver.parser.parser;
import ces.platform.infoplat.service.indexserver.parser.html.HtmlParser;
import ces.platform.infoplat.utils.Function;

/**
 * 
 * <p>
 * Title: ��Ϣƽ̨2.5 ȫ�ļ����ĵ���
 * </p>
 * <p>
 * Description: �����ɱ෢������վ���о�����Ҫ����������ļ���Ҫ������������Դ
 * </p>
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * Company: �Ϻ�������Ϣ��չ���޹�˾
 * </p>
 * 
 * @author ����
 * @version 2.5
 */
public class FTDocument extends BaseClass {

    String docId = "";

    String uid = "";

    private String fullpath = "";

    private String channel_path = "";

    private int doc_type = 0;

    public static final int doc_notPublished = 0;

    public static final int doc_published = 1;

    /**
     * @return Returns the channel_path.
     */
    public String getChannel_path() {
        return channel_path;
    }

    /**
     * @param channel_path The channel_path to set.
     */
    public void setChannel_path(String channel_path) {
        this.channel_path = channel_path;
    }

    /**
     * @return Returns the doc_type.
     */
    public int getDoc_type() {
        return doc_type;
    }

    /**
     * @param doc_type The doc_type to set.
     */
    public void setDoc_type(int doc_type) {
        this.doc_type = doc_type;
    }

    /**
	 * get lucene Document Function
	 * @param strUID��Ψһ��
	 * @param htDataField:���Field���ֶ���
	 *            ��Ӧ��ֵ��
	 * @param htFileField:�ļ�Field���ֶ���
	 *            �ļ���ַ��
	 * @return org.apache.lucene.document.Document
	 * @throws java.io.FileNotFoundException
	 */
    public Document getDocument4WF() throws java.io.FileNotFoundException {
        ces.platform.infoplat.core.Document doc = null;
        DocumentPublish docPublish = null;
        DocumentCBF docCBF = null;
        Document indexDoc = new Document();
        try {
            if (doc_type == doc_notPublished) {
                doc = (ces.platform.infoplat.core.Document) DocumentCBF.getInstance(Integer.parseInt(this.getDocId()));
            } else {
                doc = (ces.platform.infoplat.core.Document) DocumentPublish.getInstance(channel_path, Integer.parseInt(this.getDocId()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("init Document ERROR:", ex);
        }
        if (doc == null) {
            return null;
        }
        indexDoc.add(new Field("uid", this.getUid(), false, true, false));
        indexDoc.add(Field.Keyword("modified", DateField.timeToString((new Date().getTime()))));
        indexDoc.add(Field.Text("docId", String.valueOf(doc.getId())));
        indexDoc.add(Field.Text("title", (doc.getTitle() == null ? "" : doc.getTitle())));
        indexDoc.add(Field.Text("abstractWords", (doc.getAbstractWords() == null ? "" : doc.getAbstractWords())));
        indexDoc.add(Field.Text("editorRemark", (doc.getEditorRemark() == null ? "" : doc.getEditorRemark())));
        indexDoc.add(Field.Text("keywords", (doc.getKeywords() == null ? "" : doc.getKeywords())));
        indexDoc.add(Field.Text("pertinentWords", (doc.getPertinentWords() == null ? "" : doc.getPertinentWords())));
        indexDoc.add(Field.Text("subTitle", (doc.getSubTitle() == null ? "" : doc.getSubTitle())));
        indexDoc.add(Field.Text("FullPath", this.getFullpath()));
        if (doc_type == doc_notPublished) {
            docCBF = (DocumentCBF) doc;
            indexDoc.add(Field.Text("publishName", ""));
            indexDoc.add(Field.Text("publishDate", ""));
        } else {
            docPublish = (DocumentPublish) doc;
            indexDoc.add(Field.Text("publishName", docPublish.getPublisherName() == null ? "" : docPublish.getPublisherName()));
            indexDoc.add(Field.Text("publishDate", docPublish.getPublishDate() == null ? "" : Function.dateFormat(docPublish.getPublishDate(), "yyyy-MM-dd")));
        }
        indexDoc.add(Field.Text("emitUnit", doc.getEmitUnit() == null ? "" : doc.getEmitUnit()));
        indexDoc.add(Field.Text("createrName", doc.getCreaterName() == null ? "" : doc.getCreaterName()));
        indexDoc.add(Field.Text("createDate", doc.getCreateDate() == null ? "" : Function.dateFormat(doc.getCreateDate(), "yyyy-MM-dd")));
        File file = null;
        String strTemp = Function.getNYofDate(doc.getCreateDate());
        String fileName = cfg.getInfoplatDataDir() + "workflow" + File.separator + "docs" + File.separator + strTemp + File.separator + "d_" + doc.getId();
        file = new File(fileName + ".htm");
        if (file != null && file.exists()) {
            strTemp = "docs/" + strTemp + "/d_" + doc.getId() + ".htm";
            indexDoc.add(Field.Text("docUrl", strTemp));
        } else {
            file = new File(fileName + ".data");
            strTemp = "docs/" + strTemp + "/d_" + doc.getId() + ".data";
            indexDoc.add(Field.Text("docUrl", strTemp));
        }
        try {
            if (file != null && file.exists()) {
                String content = "";
                HtmlParser parser = new HtmlParser();
                content = parser.getFileContent(file.getPath());
                if (content == null) {
                    content = "";
                }
                indexDoc.add(Field.Text("docContent", content));
            }
        } catch (Exception ex3) {
            log.error("�����ļ���ֵ", ex3);
        }
        if (docCBF != null) {
            try {
                StringBuffer sbAffixContent = null;
                DocAffix[] aff = docCBF.getAffixList();
                for (int i = 0; aff != null && i < aff.length; i++) {
                    if (sbAffixContent == null) {
                        sbAffixContent = new StringBuffer();
                    }
                    fileName = cfg.getInfoplatDataDir() + "workflow" + File.separator + "docs" + File.separator + Function.getNYofDate(docCBF.getCreateDate()) + File.separator + "res" + File.separator + aff[i].getUri();
                    file = new File(fileName);
                    String fileExt = aff[i].getFileExt();
                    if (fileExt != null) {
                        String parserClass = fileExt.indexOf(".") < 0 ? ("." + fileExt) : fileExt;
                        parserClass = (String) cfg.getFtDocParser().get(parserClass);
                        if (parserClass != null) {
                            try {
                                Class cls = Class.forName(parserClass);
                                parser ps = (parser) cls.newInstance();
                                sbAffixContent.append(ps.getFileContent(fileName));
                            } catch (IllegalAccessException ex2) {
                                log.error("", ex2);
                            } catch (InstantiationException ex2) {
                                log.error("", ex2);
                            } catch (ClassNotFoundException ex2) {
                                log.error("", ex2);
                            }
                        }
                    }
                }
                if (sbAffixContent != null) {
                    indexDoc.add(Field.Text("docAffixContent", sbAffixContent.toString()));
                }
            } catch (Exception ex1) {
                log.error("�����ļ���ֵ", ex1);
            }
        } else {
            indexDoc.add(Field.Text("docAffixContent", ""));
        }
        return indexDoc;
    }

    /**
	 * һ���Get/Set����
	 */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
	 * һ���Get/Set����
	 */
    public String getUid() {
        return uid;
    }

    /**
	 * һ���Get/Set����
	 */
    public String getDocId() {
        return docId;
    }

    /**
	 * һ���Get/Set����
	 * 
	 * @param docId
	 */
    public void setDocId(String docId) {
        this.docId = docId;
    }

    /**
	 * һ���Get/Set����
	 * 
	 * @param fullpath
	 */
    public String getFullpath() {
        return fullpath;
    }

    public void setFullpath(String fullpath) {
        this.fullpath = fullpath;
    }

    public static void main(String[] args) {
        String content = "";
        try {
            content = Function.readTextFile("e:/d_3026.data");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long lTime = System.currentTimeMillis();
        lTime = (System.currentTimeMillis() - lTime);
    }
}
