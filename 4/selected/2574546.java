package ces.platform.infoplat.core;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import ces.platform.infoplat.core.base.BaseClass;
import ces.platform.infoplat.utils.Function;
import ces.platform.system.facade.OrgUser;

/**
 *
 * <p>Title: ��Ϣƽ̨2.5���ĵ�������</p>
 * <p>Description: �����ĵ����࣬��������ĵ���Դ�Ĵ��?�ĵ���Դ������ͼƬ������
 * ����ĵ���������ۺ��ĵ�������Դ�������а����Դ��ݣ�</p>
 * <p>Copyright: Copyright (c) 2004 </p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ����
 * @version 2.5
 */
public class Document extends BaseClass {

    protected int id;

    protected String actyInstId;

    protected String doctypePath;

    protected int inputTemplateId;

    protected String channelPath;

    protected String contentFile;

    protected String content;

    protected String attachStatus;

    protected int yearNo;

    protected int periodicalNo;

    protected int wordNo;

    protected String title;

    protected String titleColor;

    protected String hyperlink;

    protected String subTitle;

    protected String author;

    protected Date emitDate;

    protected String emitUnit;

    protected String editorRemark;

    protected String keywords;

    protected String pertinentWords;

    protected String abstractWords;

    protected int sourceId;

    protected int securityLevelId;

    protected String securityLevelName;

    protected int creater;

    protected String createrName;

    protected Date createDate;

    protected Date lastestModifyDate;

    protected int remarkProp;

    protected String remarkPropName;

    protected String notes;

    protected String reservation1;

    protected String reservation2;

    protected String reservation3;

    protected String reservation4;

    protected String reservation5;

    protected String reservation6;

    private String workitemId;

    private String processId;

    private String currActivity;

    /**
     * @return Returns the processId.
     */
    public String getCurrActivity() {
        return currActivity;
    }

    /**
     * @param processId The processId to set.
     */
    public void setCurrActivity(String currActivity) {
        this.currActivity = currActivity;
    }

    /**
     * @return Returns the processId.
     */
    public String getProcessId() {
        return processId;
    }

    /**
     * @param processId The processId to set.
     */
    public void setProcessId(String processId) {
        this.processId = processId;
    }

    /**
     * @return Returns the workitemId.
     */
    public String getWorkitemId() {
        return workitemId;
    }

    /**
     * @param workitemId The workitemId to set.
     */
    public void setWorkitemId(String workitemId) {
        this.workitemId = workitemId;
    }

    public String getObject(String obj) {
        if (obj.equalsIgnoreCase("Title")) {
            return "<FONT COLOR=" + this.getTitleColor() + ">" + (this.getTitle() == null ? "&nbsp;" : this.getTitle() + " &nbsp") + "</FONT>";
        } else if (obj.equalsIgnoreCase("Author")) {
            return this.getAuthor() == null ? "&nbsp" : this.getAuthor() + "&nbsp";
        } else if (obj.equalsIgnoreCase("EmitDate")) {
            return Function.dateFormat(this.getEmitDate()) + "&nbsp";
        } else if (obj.equalsIgnoreCase("SecurityLevelName")) {
            return this.getSecurityLevelName() == null ? "&nbsp" : this.getSecurityLevelName() + "&nbsp";
        } else if (obj.equalsIgnoreCase("Keywords")) {
            return this.getKeywords() == null ? "&nbsp" : this.getKeywords() + "&nbsp";
        } else if (obj.equalsIgnoreCase("CreateDate")) {
            return Function.dateFormat(this.getCreateDate()) + "&nbsp";
        } else if (obj.equalsIgnoreCase("YearNo")) {
            return String.valueOf(this.getYearNo()) + "&nbsp";
        } else if (obj.equalsIgnoreCase("PeriodicalNo")) {
            return String.valueOf(this.getPeriodicalNo()) + "&nbsp";
        } else if (obj.equalsIgnoreCase("EmitUnit")) {
            return this.getEmitUnit() == null ? "&nbsp" : this.getEmitUnit() + "&nbsp";
        } else if (obj.equalsIgnoreCase("PertinentWords")) {
            return this.getPertinentWords() == null ? "&nbsp" : this.getPertinentWords() + "&nbsp";
        } else if (obj.equalsIgnoreCase("AbstractWords")) {
            return this.getAbstractWords() == null ? "&nbsp" : this.getAbstractWords() + "&nbsp";
        } else {
            return "�޶�Ӧ�ֶ�";
        }
    }

    public Document() {
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setAbstractWords(String abstractWords) {
        this.abstractWords = abstractWords;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setActyInstId(String actyInstId) {
        this.actyInstId = actyInstId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setAttachStatus(String attachStatus) {
        this.attachStatus = attachStatus;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setChannelPath(String channelPath) {
        this.channelPath = channelPath;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setContentFile(String contentFile) {
        this.contentFile = contentFile;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setCreater(int creater) {
        this.creater = creater;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getCreater() {
        return creater;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public Date getCreateDate() {
        return createDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getContentFile() {
        return contentFile;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getChannelPath() {
        return channelPath;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getAuthor() {
        return author;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getAttachStatus() {
        return attachStatus;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getActyInstId() {
        return actyInstId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getAbstractWords() {
        return abstractWords;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getDoctypePath() {
        return doctypePath;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getEditorRemark() {
        return editorRemark;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public Date getEmitDate() {
        return emitDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getId() {
        return id;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getKeywords() {
        return keywords;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public Date getLastestModifyDate() {
        return lastestModifyDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getNotes() {
        return notes;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getPeriodicalNo() {
        return periodicalNo;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getPertinentWords() {
        return pertinentWords;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getRemarkProp() {
        return remarkProp;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getSecurityLevelId() {
        return securityLevelId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getSourceId() {
        return sourceId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getTitle() {
        return title;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getSubTitle() {
        return subTitle;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getTitleColor() {
        return titleColor;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getWordNo() {
        return wordNo;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getYearNo() {
        return yearNo;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setYearNo(int yearNo) {
        this.yearNo = yearNo;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setWordNo(int wordNo) {
        this.wordNo = wordNo;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setTitleColor(String titleColor) {
        this.titleColor = titleColor;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setSecurityLevelId(int securityLevelId) {
        this.securityLevelId = securityLevelId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setRemarkProp(int remarkProp) {
        this.remarkProp = remarkProp;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setPertinentWords(String pertinentWords) {
        this.pertinentWords = pertinentWords;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setPeriodicalNo(int periodicalNo) {
        this.periodicalNo = periodicalNo;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setId(int id) {
        this.id = id;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setLastestModifyDate(Date lastestModifyDate) {
        this.lastestModifyDate = lastestModifyDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setEmitDate(Date emitDate) {
        this.emitDate = emitDate;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setEditorRemark(String editorRemark) {
        this.editorRemark = editorRemark;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setDoctypePath(String doctypePath) {
        this.doctypePath = doctypePath;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setEmitUnit(String emitUnit) {
        this.emitUnit = emitUnit;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getEmitUnit() {
        return emitUnit;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getReservation1() {
        return reservation1;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getReservation2() {
        return reservation2;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getReservation3() {
        return reservation3;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getReservation4() {
        return reservation4;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getReservation5() {
        return reservation5;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getReservation6() {
        return reservation6;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setReservation1(String reservation1) {
        this.reservation1 = reservation1;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setReservation2(String reservation2) {
        this.reservation2 = reservation2;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setReservation3(String reservation3) {
        this.reservation3 = reservation3;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setReservation4(String reservation4) {
        this.reservation4 = reservation4;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setReservation5(String reservation5) {
        this.reservation5 = reservation5;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setReservation6(String reservation6) {
        this.reservation6 = reservation6;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getRemarkPropName() {
        return remarkPropName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setRemarkPropName(String remarkPropName) {
        this.remarkPropName = remarkPropName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getSecurityLevelName() {
        return securityLevelName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setSecurityLevelName(String securityLevelName) {
        this.securityLevelName = securityLevelName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getHyperlink() {
        return hyperlink;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setHyperlink(String hyperlink) {
        this.hyperlink = hyperlink;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public String getCreaterName() {
        if (createrName == null) {
            try {
                createrName = new OrgUser().getUser(creater).getUserName();
            } catch (Exception e) {
                log.error("�õ�createrName���?");
            }
        }
        return createrName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setCreaterName(String createrName) {
        this.createrName = createrName;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public int getInputTemplateId() {
        return inputTemplateId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setInputTemplateId(int inputTemplateId) {
        this.inputTemplateId = inputTemplateId;
    }

    /**
	 * һ���Get/Set���� 
	 */
    public void setContent(String content) {
        this.content = content;
    }
}
