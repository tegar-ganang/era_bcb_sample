package ces.platform.infoplat.core;

import ces.platform.infoplat.core.base.BaseClass;

/**
 *  Description of the Class
 *
 *@Title      ��Ϣƽ̨
 *@Company    �Ϻ�������Ϣ���޹�˾
 *@version    2.5
 *@author     ������
 *@created    2004��2��13��
 */
public class Browse extends BaseClass {

    /**
     *  dddssswwwd�������Ĺ��캯������ݿ��ȡ���
     */
    private int docId;

    private String channelPath;

    private String doctypePath;

    private int publisher;

    private java.sql.Date publishDate;

    private int orderNo;

    private java.sql.Date validStartDate;

    private java.sql.Date validEndDate;

    private String synStatus;

    private String contentFile;

    private String attachState;

    private int yearNo;

    private int periodicalNo;

    private int wordNo;

    private String title;

    private String titleColor;

    private String subTitle;

    private String author;

    private java.sql.Date emitDate;

    private String emitUnit;

    private String editorRemark;

    private String docKeys;

    private String pertinentWords;

    private String docAbstract;

    private String docSource;

    private int securityLevel;

    private int creator;

    private java.sql.Date createDate;

    private java.sql.Date lastestModifyDate;

    private String remarkProp;

    private String notes;

    private String reservation1;

    private String reservation2;

    private String reservation3;

    private String reservation4;

    private String reservation5;

    private String reservation6;

    /**
     *  Constructor for the Browse object
     */
    public Browse() {
    }

    /**
     *  �����ĳ�ʼ�����캯����ͬʱ����ݿ��ȡ���
     *
     *@param  channelPath  Ƶ����·��,
     *@param  docId        �ĵ����
     */
    public Browse(int docId, String channelPath) {
    }

    /**
     *  �����ݲ��뵽��ݿ���
     *
     *@exception  Exception         Description of the Exception
     *@throws  java.lang.Exception
     */
    public void add() throws Exception {
    }

    /**
     *  �ѵ�ǰ���µ���ݿ���
     *
     *@exception  Exception         Description of the Exception
     *@throws  java.lang.Exception
     */
    public void update() throws Exception {
    }

    /**
     *  �ѵ�ǰʵ�����ݴ���ݿ���ɾ��
     *
     *@exception  Exception         Description of the Exception
     *@throws  java.lang.Exception
     */
    public void delete() throws Exception {
    }

    /**
     *  Gets the docId attribute of the Browse object
     *
     *@return    The docId value
     */
    public int getDocId() {
        return docId;
    }

    /**
     *  Sets the docId attribute of the Browse object
     *
     *@param  docId  The new docId value
     */
    public void setDocId(int docId) {
        this.docId = docId;
    }

    /**
     *  Gets the channelPath attribute of the Browse object
     *
     *@return    The channelPath value
     */
    public String getChannelPath() {
        return channelPath;
    }

    /**
     *  Sets the channelPath attribute of the Browse object
     *
     *@param  channelPath  The new channelPath value
     */
    public void setChannelPath(String channelPath) {
        this.channelPath = channelPath;
    }

    /**
     *  Gets the doctypePath attribute of the Browse object
     *
     *@return    The doctypePath value
     */
    public String getDoctypePath() {
        return doctypePath;
    }

    /**
     *  Sets the doctypePath attribute of the Browse object
     *
     *@param  doctypePath  The new doctypePath value
     */
    public void setDoctypePath(String doctypePath) {
        this.doctypePath = doctypePath;
    }

    /**
     *  Gets the publisher attribute of the Browse object
     *
     *@return    The publisher value
     */
    public int getPublisher() {
        return publisher;
    }

    /**
     *  Sets the publisher attribute of the Browse object
     *
     *@param  publisher  The new publisher value
     */
    public void setPublisher(int publisher) {
        this.publisher = publisher;
    }

    /**
     *  Gets the publishDate attribute of the Browse object
     *
     *@return    The publishDate value
     */
    public java.sql.Date getPublishDate() {
        return publishDate;
    }

    /**
     *  Sets the publishDate attribute of the Browse object
     *
     *@param  publishDate  The new publishDate value
     */
    public void setPublishDate(java.sql.Date publishDate) {
        this.publishDate = publishDate;
    }

    /**
     *  Gets the orderNo attribute of the Browse object
     *
     *@return    The orderNo value
     */
    public int getOrderNo() {
        return orderNo;
    }

    /**
     *  Sets the orderNo attribute of the Browse object
     *
     *@param  orderNo  The new orderNo value
     */
    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    /**
     *  Gets the validStartDate attribute of the Browse object
     *
     *@return    The validStartDate value
     */
    public java.sql.Date getValidStartDate() {
        return validStartDate;
    }

    /**
     *  Sets the validStartDate attribute of the Browse object
     *
     *@param  validStartDate  The new validStartDate value
     */
    public void setValidStartDate(java.sql.Date validStartDate) {
        this.validStartDate = validStartDate;
    }

    /**
     *  Gets the validEndDate attribute of the Browse object
     *
     *@return    The validEndDate value
     */
    public java.sql.Date getValidEndDate() {
        return validEndDate;
    }

    /**
     *  Sets the validEndDate attribute of the Browse object
     *
     *@param  validEndDate  The new validEndDate value
     */
    public void setValidEndDate(java.sql.Date validEndDate) {
        this.validEndDate = validEndDate;
    }

    /**
     *  Gets the synStatus attribute of the Browse object
     *
     *@return    The synStatus value
     */
    public String getSynStatus() {
        return synStatus;
    }

    /**
     *  Sets the synStatus attribute of the Browse object
     *
     *@param  synStatus  The new synStatus value
     */
    public void setSynStatus(String synStatus) {
        this.synStatus = synStatus;
    }

    /**
     *  Gets the contentFile attribute of the Browse object
     *
     *@return    The contentFile value
     */
    public String getContentFile() {
        return contentFile;
    }

    /**
     *  Sets the contentFile attribute of the Browse object
     *
     *@param  contentFile  The new contentFile value
     */
    public void setContentFile(String contentFile) {
        this.contentFile = contentFile;
    }

    /**
     *  Gets the attachState attribute of the Browse object
     *
     *@return    The attachState value
     */
    public String getAttachState() {
        return attachState;
    }

    /**
     *  Sets the attachState attribute of the Browse object
     *
     *@param  attachState  The new attachState value
     */
    public void setAttachState(String attachState) {
        this.attachState = attachState;
    }

    /**
     *  Gets the yearNo attribute of the Browse object
     *
     *@return    The yearNo value
     */
    public int getYearNo() {
        return yearNo;
    }

    /**
     *  Sets the yearNo attribute of the Browse object
     *
     *@param  yearNo  The new yearNo value
     */
    public void setYearNo(int yearNo) {
        this.yearNo = yearNo;
    }

    /**
     *  Gets the periodicalNo attribute of the Browse object
     *
     *@return    The periodicalNo value
     */
    public int getPeriodicalNo() {
        return periodicalNo;
    }

    /**
     *  Sets the periodicalNo attribute of the Browse object
     *
     *@param  periodicalNo  The new periodicalNo value
     */
    public void setPeriodicalNo(int periodicalNo) {
        this.periodicalNo = periodicalNo;
    }

    /**
     *  Gets the wordNo attribute of the Browse object
     *
     *@return    The wordNo value
     */
    public int getWordNo() {
        return wordNo;
    }

    /**
     *  Sets the wordNo attribute of the Browse object
     *
     *@param  wordNo  The new wordNo value
     */
    public void setWordNo(int wordNo) {
        this.wordNo = wordNo;
    }

    /**
     *  Gets the title attribute of the Browse object
     *
     *@return    The title value
     */
    public String getTitle() {
        return title;
    }

    /**
     *  Sets the title attribute of the Browse object
     *
     *@param  title  The new title value
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     *  Gets the titleColor attribute of the Browse object
     *
     *@return    The titleColor value
     */
    public String getTitleColor() {
        return titleColor;
    }

    /**
     *  Sets the titleColor attribute of the Browse object
     *
     *@param  titleColor  The new titleColor value
     */
    public void setTitleColor(String titleColor) {
        this.titleColor = titleColor;
    }

    /**
     *  Gets the subTitle attribute of the Browse object
     *
     *@return    The subTitle value
     */
    public String getSubTitle() {
        return subTitle;
    }

    /**
     *  Sets the subTitle attribute of the Browse object
     *
     *@param  subTitle  The new subTitle value
     */
    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    /**
     *  Gets the author attribute of the Browse object
     *
     *@return    The author value
     */
    public String getAuthor() {
        return author;
    }

    /**
     *  Sets the author attribute of the Browse object
     *
     *@param  author  The new author value
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     *  Gets the emitDate attribute of the Browse object
     *
     *@return    The emitDate value
     */
    public java.sql.Date getEmitDate() {
        return emitDate;
    }

    /**
     *  Sets the emitDate attribute of the Browse object
     *
     *@param  emitDate  The new emitDate value
     */
    public void setEmitDate(java.sql.Date emitDate) {
        this.emitDate = emitDate;
    }

    /**
     *  Gets the emitUnit attribute of the Browse object
     *
     *@return    The emitUnit value
     */
    public String getEmitUnit() {
        return emitUnit;
    }

    /**
     *  Sets the emitUnit attribute of the Browse object
     *
     *@param  emitUnit  The new emitUnit value
     */
    public void setEmitUnit(String emitUnit) {
        this.emitUnit = emitUnit;
    }

    /**
     *  Gets the editorRemark attribute of the Browse object
     *
     *@return    The editorRemark value
     */
    public String getEditorRemark() {
        return editorRemark;
    }

    /**
     *  Sets the editorRemark attribute of the Browse object
     *
     *@param  editorRemark  The new editorRemark value
     */
    public void setEditorRemark(String editorRemark) {
        this.editorRemark = editorRemark;
    }

    /**
     *  Gets the docKeys attribute of the Browse object
     *
     *@return    The docKeys value
     */
    public String getDocKeys() {
        return docKeys;
    }

    /**
     *  Sets the docKeys attribute of the Browse object
     *
     *@param  docKeys  The new docKeys value
     */
    public void setDocKeys(String docKeys) {
        this.docKeys = docKeys;
    }

    /**
     *  Gets the pertinentWords attribute of the Browse object
     *
     *@return    The pertinentWords value
     */
    public String getPertinentWords() {
        return pertinentWords;
    }

    /**
     *  Sets the pertinentWords attribute of the Browse object
     *
     *@param  pertinentWords  The new pertinentWords value
     */
    public void setPertinentWords(String pertinentWords) {
        this.pertinentWords = pertinentWords;
    }

    /**
     *  Gets the docAbstract attribute of the Browse object
     *
     *@return    The docAbstract value
     */
    public String getDocAbstract() {
        return docAbstract;
    }

    /**
     *  Sets the docAbstract attribute of the Browse object
     *
     *@param  docAbstract  The new docAbstract value
     */
    public void setDocAbstract(String docAbstract) {
        this.docAbstract = docAbstract;
    }

    /**
     *  Gets the docSource attribute of the Browse object
     *
     *@return    The docSource value
     */
    public String getDocSource() {
        return docSource;
    }

    /**
     *  Sets the docSource attribute of the Browse object
     *
     *@param  docSource  The new docSource value
     */
    public void setDocSource(String docSource) {
        this.docSource = docSource;
    }

    /**
     *  Gets the securityLevel attribute of the Browse object
     *
     *@return    The securityLevel value
     */
    public int getSecurityLevel() {
        return securityLevel;
    }

    /**
     *  Sets the securityLevel attribute of the Browse object
     *
     *@param  securityLevel  The new securityLevel value
     */
    public void setSecurityLevel(int securityLevel) {
        this.securityLevel = securityLevel;
    }

    /**
     *  Gets the creator attribute of the Browse object
     *
     *@return    The creator value
     */
    public int getCreator() {
        return creator;
    }

    /**
     *  Sets the creator attribute of the Browse object
     *
     *@param  creator  The new creator value
     */
    public void setCreator(int creator) {
        this.creator = creator;
    }

    /**
     *  Gets the createDate attribute of the Browse object
     *
     *@return    The createDate value
     */
    public java.sql.Date getCreateDate() {
        return createDate;
    }

    /**
     *  Sets the createDate attribute of the Browse object
     *
     *@param  createDate  The new createDate value
     */
    public void setCreateDate(java.sql.Date createDate) {
        this.createDate = createDate;
    }

    /**
     *  Gets the lastestModifyDate attribute of the Browse object
     *
     *@return    The lastestModifyDate value
     */
    public java.sql.Date getLastestModifyDate() {
        return lastestModifyDate;
    }

    /**
     *  Sets the lastestModifyDate attribute of the Browse object
     *
     *@param  lastestModifyDate  The new lastestModifyDate value
     */
    public void setLastestModifyDate(java.sql.Date lastestModifyDate) {
        this.lastestModifyDate = lastestModifyDate;
    }

    /**
     *  Gets the remarkProp attribute of the Browse object
     *
     *@return    The remarkProp value
     */
    public String getRemarkProp() {
        return remarkProp;
    }

    /**
     *  Sets the remarkProp attribute of the Browse object
     *
     *@param  remarkProp  The new remarkProp value
     */
    public void setRemarkProp(String remarkProp) {
        this.remarkProp = remarkProp;
    }

    /**
     *  Gets the notes attribute of the Browse object
     *
     *@return    The notes value
     */
    public String getNotes() {
        return notes;
    }

    /**
     *  Sets the notes attribute of the Browse object
     *
     *@param  notes  The new notes value
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     *  Gets the reservation1 attribute of the Browse object
     *
     *@return    The reservation1 value
     */
    public String getReservation1() {
        return reservation1;
    }

    /**
     *  Sets the reservation1 attribute of the Browse object
     *
     *@param  reservation1  The new reservation1 value
     */
    public void setReservation1(String reservation1) {
        this.reservation1 = reservation1;
    }

    /**
     *  Gets the reservation2 attribute of the Browse object
     *
     *@return    The reservation2 value
     */
    public String getReservation2() {
        return reservation2;
    }

    /**
     *  Sets the reservation2 attribute of the Browse object
     *
     *@param  reservation2  The new reservation2 value
     */
    public void setReservation2(String reservation2) {
        this.reservation2 = reservation2;
    }

    /**
     *  Gets the reservation3 attribute of the Browse object
     *
     *@return    The reservation3 value
     */
    public String getReservation3() {
        return reservation3;
    }

    /**
     *  Sets the reservation3 attribute of the Browse object
     *
     *@param  reservation3  The new reservation3 value
     */
    public void setReservation3(String reservation3) {
        this.reservation3 = reservation3;
    }

    /**
     *  Gets the reservation4 attribute of the Browse object
     *
     *@return    The reservation4 value
     */
    public String getReservation4() {
        return reservation4;
    }

    /**
     *  Sets the reservation4 attribute of the Browse object
     *
     *@param  reservation4  The new reservation4 value
     */
    public void setReservation4(String reservation4) {
        this.reservation4 = reservation4;
    }

    /**
     *  Gets the reservation5 attribute of the Browse object
     *
     *@return    The reservation5 value
     */
    public String getReservation5() {
        return reservation5;
    }

    /**
     *  Sets the reservation5 attribute of the Browse object
     *
     *@param  reservation5  The new reservation5 value
     */
    public void setReservation5(String reservation5) {
        this.reservation5 = reservation5;
    }

    /**
     *  Gets the reservation6 attribute of the Browse object
     *
     *@return    The reservation6 value
     */
    public String getReservation6() {
        return reservation6;
    }

    /**
     *  Sets the reservation6 attribute of the Browse object
     *
     *@param  reservation6  The new reservation6 value
     */
    public void setReservation6(String reservation6) {
        this.reservation6 = reservation6;
    }
}
