package com.siteeval.dataAcquisition;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import com.siteeval.common.DbConnect;
import com.siteeval.common.Globa;
import com.siteeval.common.UID;

public class InitScoreTable {

    private Globa globa;

    private DbConnect db;

    String strTableName1 = "t_taskAtribute";

    String strTableName2 = "t_siteTotalScore";

    String strTableName3 = "t_siteScore";

    String strTableName4 = "t_siteinfoScore";

    String strTableName5 = "t_sy_user";

    String strTableName6 = "t_sy_site";

    public InitScoreTable() {
    }

    public InitScoreTable(Globa globa) {
        this.globa = globa;
        db = globa.db;
    }

    public InitScoreTable(Globa globa, boolean b) {
        this.globa = globa;
        db = globa.db;
        if (b) globa.setDynamicProperty(this);
    }

    public Vector taskList(String where1, int startRow, int rowCount) {
        Vector beans = new Vector();
        try {
            String sql = "SELECT  t_taskAtribute.strid, t_taskAtribute.strUserId, t_taskAtribute.dTaskBeginTime, t_taskAtribute.dTaskEndTime, t_taskAtribute.strSiteId, t_taskAtribute.strCreateUser,t_taskAtribute.strCreateTime,t_taskAtribute.strYear,t_taskAtribute.templateUrl,t_sy_user.strName,t_sy_site.strSiteName,t_sy_site.strSiteUrl,t_sy_site.strSiteType,t_siteTotalScore.strTaskId,t_siteTotalScore.strSiteState " + "FROM  t_taskAtribute left join t_siteTotalScore on t_taskAtribute.strid = t_siteTotalScore.strTaskId, t_sy_user, t_sy_site WHERE t_taskAtribute.strUserId = t_sy_user.strUserId AND t_sy_site.strid = t_taskAtribute.strSiteId";
            if (where1.length() > 0) sql = String.valueOf(sql) + String.valueOf(where1);
            Statement s = db.getConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            if (startRow != 0 && rowCount != 0) s.setMaxRows((startRow + rowCount) - 1);
            ResultSet rs = s.executeQuery(sql);
            if (rs != null && rs.next()) {
                if (startRow != 0 && rowCount != 0) rs.absolute(startRow);
                do {
                    InitScoreTable theBean = new InitScoreTable();
                    theBean = load(rs);
                    beans.addElement(theBean);
                } while (rs.next());
            }
            rs.close();
            s.close();
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        return beans;
    }

    public InitScoreTable load(ResultSet rs) throws SQLException {
        InitScoreTable theBean = new InitScoreTable();
        try {
            theBean.setStrTaskAssignId(rs.getString("strid"));
            theBean.setStrTaskUserId(rs.getString("strUserId"));
            theBean.setDTaskBeginTime(rs.getString("dTaskBeginTime"));
            theBean.setDTaskEndTime(rs.getString("dTaskEndTime"));
            theBean.setStrTaskSiteId(rs.getString("strSiteId"));
            theBean.setStrTaskCreateUser(rs.getString("strCreateUser"));
            theBean.setStrTaskCreateTime(rs.getString("strCreateTime"));
            theBean.setStrUserName(rs.getString("strName"));
            theBean.setStrSiteName(rs.getString("strSiteName"));
            theBean.setStrSiteUrl(rs.getString("strSiteUrl"));
            theBean.setStrTaskYear(rs.getString("strYear"));
            theBean.setStrTaskSiteType(rs.getString("strSiteType"));
            theBean.setStrTotalScoreTaskId(rs.getString("strTaskId"));
            theBean.setStrTotalScoreSiteState(rs.getString("strSiteState"));
            theBean.setTemplateUrl(rs.getString("templateUrl"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return theBean;
    }

    public int getCount(String where1) {
        int count = 0;
        String where = "";
        if (where1.length() > 0) {
            where = where1.toLowerCase();
            if (where.indexOf("order") > 0) where = where.substring(0, where.indexOf("order"));
        }
        try {
            String sql = "SELECT count(*) FROM  t_taskAtribute,  t_sy_user, t_sy_site WHERE t_taskAtribute.strUserId = t_sy_user.strUserId AND t_sy_site.strid = t_taskAtribute.strSiteId";
            if (where.length() > 0) {
                sql = String.valueOf(sql) + String.valueOf(where);
            }
            ResultSet rs = db.executeQuery(sql);
            if (rs.next()) count = rs.getInt(1);
            rs.close();
            return count;
        } catch (Exception ee) {
            ee.printStackTrace();
            return count;
        }
    }

    public boolean addtoTotalScore() {
        String strSql = "";
        strTotalScoreId = UID.getID();
        try {
            strSql = "INSERT INTO " + strTableName2 + "(strId,strSiteId,strTaskId," + "strWebsite,strSiteName,strEvalYear,strCreator,datCreatedTime,strSiteState,strUserId) " + "VALUES(?,?,?,?,?,?,?,?,?,?)";
            db.prepareStatement(strSql);
            db.setString(1, strTotalScoreId);
            db.setString(2, strTotalScoreSiteId);
            db.setString(3, strTotalScoreTaskId);
            db.setString(4, strTotalScoreWebsite);
            db.setString(5, strTotalScoreSiteName);
            db.setString(6, strTotalScoreEvalYear);
            db.setString(7, strTotalScoreCreator);
            db.setString(8, com.siteeval.common.Format.getDateTime());
            db.setString(9, strTotalScoreSiteState);
            db.setString(10, strTotalScoreUserId);
            if (db.executeUpdate() > 0) {
                Globa.logger0("������վ�����ܱ���Ϣ", globa.loginName, globa.loginIp, strSql, "��ݲɼ�", globa.userSession.getStrDepart());
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("������վ�����ܱ���Ϣʱ���?");
            e.printStackTrace();
            return false;
        }
    }

    public boolean addtoSiteScore(String strSiteScoreTagId, String strSiteScoreTagType, String strSiteScoreTagName, String strSiteScoreParentId) {
        String strSql = "";
        strSiteScoreId = UID.getID();
        try {
            strSql = "INSERT INTO " + strTableName3 + "(strId,strTaskId,strTagId," + "strTagType,strTagName,strParentId,strYear,datCreatedTime,strCreator) " + "VALUES(?,?,?,?,?,?,?,?,?)";
            db.prepareStatement(strSql);
            db.setString(1, strSiteScoreId);
            db.setString(2, strSiteScoreTaskId);
            db.setString(3, strSiteScoreTagId);
            db.setString(4, strSiteScoreTagType);
            db.setString(5, strSiteScoreTagName);
            db.setString(6, strSiteScoreParentId);
            db.setString(7, strSiteScoreYear);
            db.setString(8, com.siteeval.common.Format.getDateTime());
            db.setString(9, strSiteScoreCreator);
            ;
            if (db.executeUpdate() > 0) {
                Globa.logger0("������վ���ֱ���Ϣ", globa.loginName, globa.loginIp, strSql, "��ݲɼ�", globa.userSession.getStrDepart());
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("������վ���ֱ���Ϣʱ���?");
            e.printStackTrace();
            return false;
        }
    }

    /***
	     * ����ύ (�����)
	     * @param siteScores
	     * @return
	     */
    public boolean addSiteScore(ArrayList<InitScoreTable> siteScores, InitScoreTable scoreTable, String filePath, String strTime) {
        boolean bResult = false;
        String strSql = "";
        Connection conn = null;
        Statement stm = null;
        try {
            conn = db.getConnection();
            conn.setAutoCommit(false);
            stm = conn.createStatement();
            strSql = "delete from t_siteScore  where strTaskId = '" + scoreTable.getStrSiteScoreTaskId() + "'";
            stm.executeUpdate(strSql);
            for (int i = 0; i < siteScores.size(); i++) {
                InitScoreTable temp = siteScores.get(i);
                String tempSql = "select * from t_tagConf where strTagName='" + temp.getStrSiteScoreTagName() + "' and strTagYear='" + temp.getStrSiteScoreYear() + "' ";
                System.out.println(tempSql);
                ResultSet rst = stm.executeQuery(tempSql);
                if (rst.next()) {
                    temp.setStrSiteScoreTagId(rst.getString("strId"));
                    temp.setStrSiteinfoScoreParentId(rst.getString("strParentId"));
                }
                rst = null;
            }
            Iterator<InitScoreTable> it = siteScores.iterator();
            String strCreatedTime = com.siteeval.common.Format.getDateTime();
            String taskId = "";
            while (it.hasNext()) {
                InitScoreTable thebean = it.next();
                taskId = thebean.getStrSiteScoreTaskId();
                String strId = UID.getID();
                strSql = "INSERT INTO " + strTableName3 + "(strId,strTaskId,strTagId," + "strTagType,strTagName,strParentId,flaTagScore,strYear,datCreatedTime,strCreator) " + "VALUES('" + strId + "','" + taskId + "','" + thebean.getStrSiteScoreTagId() + "','" + thebean.getStrSiteScoreTagType() + "','" + thebean.getStrSiteScoreTagName() + "','" + thebean.getStrSiteinfoScoreParentId() + "','" + thebean.getFlaSiteScoreTagScore() + "','" + thebean.getStrSiteScoreYear() + "','" + strCreatedTime + "','" + thebean.getStrSiteScoreCreator() + "')";
                stm.executeUpdate(strSql);
            }
            strSql = "update t_siteTotalScore set strSiteState=1,flaSiteScore='" + scoreTable.getFlaSiteScore() + "',flaInfoDisclosureScore='" + scoreTable.getFlaInfoDisclosureScore() + "',flaOnlineServicesScore='" + scoreTable.getFlaOnlineServicesScore() + "',flaPublicParticipationSore='" + scoreTable.getFlaPublicParticipationSore() + "',flaWebDesignScore='" + scoreTable.getFlaWebDesignScore() + "',strSiteFeature='" + scoreTable.getStrTotalScoreSiteFeature() + "',strSiteAdvantage='" + scoreTable.getStrTotalScoreSiteAdvantage() + "',strSiteFailure='" + scoreTable.getStrTotalScoreSiteFailure() + "' where strTaskId='" + scoreTable.getStrSiteScoreTaskId() + "'";
            stm.executeUpdate(strSql);
            strSql = "update " + strTableName1 + " set templateUrl='" + filePath + "',dTaskBeginTime='" + strTime + "',dTaskEndTime='" + strTime + "' where strid = '" + scoreTable.getStrSiteScoreTaskId() + "'";
            stm.executeUpdate(strSql);
            conn.commit();
            bResult = true;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception eee) {
            }
            System.out.println("������վ���ֱ���Ϣʱ���?");
        } finally {
            try {
                conn.setAutoCommit(true);
                if (stm != null) {
                    stm.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ee) {
            }
        }
        return bResult;
    }

    public boolean addtoSiteinfoScore(String strSiteinfoScoreTagId, String strSiteinfoTagType, String strSiteinfoScoreTagName, String strSiteinfoScoreParentId) {
        String strSql = "";
        strSiteinfoScoreId = UID.getID();
        try {
            strSql = "INSERT INTO " + strTableName4 + "(strId,strTaskId,strTagId," + "strTagType,strTagName,strParentId,strYear,datCreatedTime,strCreator) " + "VALUES(?,?,?,?,?,?,?,?,?)";
            db.prepareStatement(strSql);
            db.setString(1, strSiteinfoScoreId);
            db.setString(2, strSiteinfoScoreTaskId);
            db.setString(3, strSiteinfoScoreTagId);
            db.setString(4, strSiteinfoTagType);
            db.setString(5, strSiteinfoScoreTagName);
            db.setString(6, strSiteinfoScoreParentId);
            db.setString(7, strSiteinfoScoreYear);
            db.setString(8, com.siteeval.common.Format.getDateTime());
            db.setString(9, strSiteinfoScoreCreator);
            ;
            if (db.executeUpdate() > 0) {
                Globa.logger0("������վ������ϸ����Ϣ", globa.loginName, globa.loginIp, strSql, "��ݲɼ�", globa.userSession.getStrDepart());
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("������վ������ϸ����Ϣʱ���?");
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(String where) {
        String strSql = "";
        try {
            strSql = "DELETE FROM " + strTableName2 + " ".concat(where);
            strSql += " DELETE FROM " + strTableName3 + " ".concat(where);
            strSql += " DELETE FROM " + strTableName4 + " ".concat(where);
            db.executeUpdate(strSql);
            Globa.logger0("ɾ�����ֱ���Ϣ", globa.loginName, globa.loginIp, strSql, "��ݲɼ�", globa.userSession.getStrDepart());
            return true;
        } catch (Exception e) {
            System.out.println("ɾ����Ϣʱʧ��");
            e.printStackTrace();
            return false;
        }
    }

    /***
	     * ���ֱ��涯��������壩
	     * @param taskId
	     * @param taskUrl
	     */
    public void updateTaskUrl(String taskId, String taskUrl) {
        String strSql = "update " + strTableName1 + " set templateUrl=? where strid = ?";
        try {
            db.prepareStatement(strSql);
            db.setString(1, taskUrl);
            db.setString(2, taskId);
            db.executeUpdate();
        } catch (Exception e) {
            System.out.println("�޸�����ģ��URl��Ϣʱ���?");
        }
    }

    public int getIntLeafLevel(String where) {
        int leafLevel = 0;
        try {
            String sql = "SELECT max(intLevel) FROM t_tagConf ";
            if (where.length() > 0) {
                if (where.indexOf("order") > 0) where = where.substring(0, where.lastIndexOf("order"));
                sql = String.valueOf(sql) + String.valueOf(where);
            }
            ResultSet rs = db.executeQuery(sql);
            if (rs.next()) leafLevel = rs.getInt(1);
            rs.close();
            return leafLevel;
        } catch (Exception ee) {
            ee.printStackTrace();
            return leafLevel;
        }
    }

    private String strTaskAssignId;

    private String strTaskUserId;

    private String dTaskBeginTime;

    private String dTaskEndTime;

    private String strTaskSiteId;

    private String strTaskCreateUser;

    private String strTaskCreateTime;

    private String strTaskYear;

    private String strTaskState;

    private String strTaskSiteType;

    private String templateUrl;

    private String strTotalScoreId;

    private String strTotalScoreSiteId;

    private String strTotalScoreTaskId;

    private String strTotalScoreSiteFeature;

    private String strTotalScoreSiteAdvantage;

    private String strTotalScoreSiteFailure;

    private String strTotalScoreWebsite;

    private String strTotalScoreSiteName;

    private String strTotalScoreEvalYear;

    private String strTotalScoreCreator;

    private String datTotalScoreCreatedTime;

    private String strTotalScoreSiteState;

    private String datTotalScoreSiteReferTime;

    private float flaSiteScore;

    private float flaInfoDisclosureScore;

    private float flaOnlineServicesScore;

    private float flaPublicParticipationSore;

    private float flaWebDesignScore;

    private String strTotalScoreUserId;

    private String strSiteScoreId;

    private String strSiteScoreTaskId;

    private String strSiteScoreTagId;

    private String strSiteScoreTagType;

    private String strSiteScoreTagName;

    private String strSiteScoreTagDescription;

    private String strSiteScoreParentId;

    private String strSiteScoreYear;

    private String datSiteScoreCreatedTime;

    private String strSiteScoreCreator;

    private float flaSiteScoreStandardScore;

    private float flaSiteScoreTagScore;

    private String strSiteinfoScoreId;

    private String strSiteinfoScoreTaskId;

    private String strSiteinfoScoreTagId;

    private String strSiteinfoTagType;

    private String strSiteinfoScoreTagName;

    private String strSiteinfoScoreTagRule;

    private String strSiteinfoScoreParentId;

    private String strSiteinfoScoreYear;

    private String datSiteinfoScoreCreatedTime;

    private String strSiteinfoScoreCreator;

    private float flaSiteinfoScoreStandardScore;

    private float flaSiteinfoScoreTagScore;

    private String strUserName;

    private String strSiteName;

    private String strSiteUrl;

    public String getStrTaskAssignId() {
        return strTaskAssignId;
    }

    public void setStrTaskAssignId(String strTaskAssignId) {
        this.strTaskAssignId = strTaskAssignId;
    }

    public String getStrTaskUserId() {
        return strTaskUserId;
    }

    public void setStrTaskUserId(String strTaskUserId) {
        this.strTaskUserId = strTaskUserId;
    }

    public String getDTaskBeginTime() {
        return dTaskBeginTime;
    }

    public void setDTaskBeginTime(String taskBeginTime) {
        dTaskBeginTime = taskBeginTime;
    }

    public String getDTaskEndTime() {
        return dTaskEndTime;
    }

    public void setDTaskEndTime(String taskEndTime) {
        dTaskEndTime = taskEndTime;
    }

    public String getStrTaskSiteId() {
        return strTaskSiteId;
    }

    public void setStrTaskSiteId(String strTaskSiteId) {
        this.strTaskSiteId = strTaskSiteId;
    }

    public String getStrTaskCreateUser() {
        return strTaskCreateUser;
    }

    public void setStrTaskCreateUser(String strTaskCreateUser) {
        this.strTaskCreateUser = strTaskCreateUser;
    }

    public String getStrTaskCreateTime() {
        return strTaskCreateTime;
    }

    public void setStrTaskCreateTime(String strTaskCreateTime) {
        this.strTaskCreateTime = strTaskCreateTime;
    }

    public String getStrTaskYear() {
        return strTaskYear;
    }

    public void setStrTaskYear(String strTaskYear) {
        this.strTaskYear = strTaskYear;
    }

    public String getStrTaskState() {
        return strTaskState;
    }

    public void setStrTaskState(String strTaskState) {
        this.strTaskState = strTaskState;
    }

    public String getStrTaskSiteType() {
        return strTaskSiteType;
    }

    public void setStrTaskSiteType(String strTaskSiteType) {
        this.strTaskSiteType = strTaskSiteType;
    }

    public String getStrTotalScoreId() {
        return strTotalScoreId;
    }

    public void setStrTotalScoreId(String strTotalScoreId) {
        this.strTotalScoreId = strTotalScoreId;
    }

    public String getStrTotalScoreSiteId() {
        return strTotalScoreSiteId;
    }

    public void setStrTotalScoreSiteId(String strTotalScoreSiteId) {
        this.strTotalScoreSiteId = strTotalScoreSiteId;
    }

    public String getStrTotalScoreTaskId() {
        return strTotalScoreTaskId;
    }

    public void setStrTotalScoreTaskId(String strTotalScoreTaskId) {
        this.strTotalScoreTaskId = strTotalScoreTaskId;
    }

    public String getStrTotalScoreSiteFeature() {
        return strTotalScoreSiteFeature;
    }

    public void setStrTotalScoreSiteFeature(String strTotalScoreSiteFeature) {
        this.strTotalScoreSiteFeature = strTotalScoreSiteFeature;
    }

    public String getStrTotalScoreSiteAdvantage() {
        return strTotalScoreSiteAdvantage;
    }

    public void setStrTotalScoreSiteAdvantage(String strTotalScoreSiteAdvantage) {
        this.strTotalScoreSiteAdvantage = strTotalScoreSiteAdvantage;
    }

    public String getStrTotalScoreSiteFailure() {
        return strTotalScoreSiteFailure;
    }

    public void setStrTotalScoreSiteFailure(String strTotalScoreSiteFailure) {
        this.strTotalScoreSiteFailure = strTotalScoreSiteFailure;
    }

    public String getStrTotalScoreWebsite() {
        return strTotalScoreWebsite;
    }

    public void setStrTotalScoreWebsite(String strTotalScoreWebsite) {
        this.strTotalScoreWebsite = strTotalScoreWebsite;
    }

    public String getStrTotalScoreSiteName() {
        return strTotalScoreSiteName;
    }

    public void setStrTotalScoreSiteName(String strTotalScoreSiteName) {
        this.strTotalScoreSiteName = strTotalScoreSiteName;
    }

    public String getStrTotalScoreEvalYear() {
        return strTotalScoreEvalYear;
    }

    public void setStrTotalScoreEvalYear(String strTotalScoreEvalYear) {
        this.strTotalScoreEvalYear = strTotalScoreEvalYear;
    }

    public String getStrTotalScoreCreator() {
        return strTotalScoreCreator;
    }

    public void setStrTotalScoreCreator(String strTotalScoreCreator) {
        this.strTotalScoreCreator = strTotalScoreCreator;
    }

    public String getDatTotalScoreCreatedTime() {
        return datTotalScoreCreatedTime;
    }

    public void setDatTotalScoreCreatedTime(String datTotalScoreCreatedTime) {
        this.datTotalScoreCreatedTime = datTotalScoreCreatedTime;
    }

    public String getStrTotalScoreSiteState() {
        return strTotalScoreSiteState;
    }

    public void setStrTotalScoreSiteState(String strTotalScoreSiteState) {
        this.strTotalScoreSiteState = strTotalScoreSiteState;
    }

    public String getDatTotalScoreSiteReferTime() {
        return datTotalScoreSiteReferTime;
    }

    public void setDatTotalScoreSiteReferTime(String datTotalScoreSiteReferTime) {
        this.datTotalScoreSiteReferTime = datTotalScoreSiteReferTime;
    }

    public float getFlaSiteScore() {
        return flaSiteScore;
    }

    public void setFlaSiteScore(float flaSiteScore) {
        this.flaSiteScore = flaSiteScore;
    }

    public float getFlaInfoDisclosureScore() {
        return flaInfoDisclosureScore;
    }

    public void setFlaInfoDisclosureScore(float flaInfoDisclosureScore) {
        this.flaInfoDisclosureScore = flaInfoDisclosureScore;
    }

    public float getFlaOnlineServicesScore() {
        return flaOnlineServicesScore;
    }

    public void setFlaOnlineServicesScore(float flaOnlineServicesScore) {
        this.flaOnlineServicesScore = flaOnlineServicesScore;
    }

    public float getFlaPublicParticipationSore() {
        return flaPublicParticipationSore;
    }

    public void setFlaPublicParticipationSore(float flaPublicParticipationSore) {
        this.flaPublicParticipationSore = flaPublicParticipationSore;
    }

    public float getFlaWebDesignScore() {
        return flaWebDesignScore;
    }

    public void setFlaWebDesignScore(float flaWebDesignScore) {
        this.flaWebDesignScore = flaWebDesignScore;
    }

    public String getStrSiteScoreId() {
        return strSiteScoreId;
    }

    public void setStrSiteScoreId(String strSiteScoreId) {
        this.strSiteScoreId = strSiteScoreId;
    }

    public String getStrSiteScoreTaskId() {
        return strSiteScoreTaskId;
    }

    public void setStrSiteScoreTaskId(String strSiteScoreTaskId) {
        this.strSiteScoreTaskId = strSiteScoreTaskId;
    }

    public String getStrSiteScoreTagType() {
        return strSiteScoreTagType;
    }

    public void setStrSiteScoreTagType(String strSiteScoreTagType) {
        this.strSiteScoreTagType = strSiteScoreTagType;
    }

    public String getStrSiteScoreTagId() {
        return strSiteScoreTagId;
    }

    public void setStrSiteScoreTagId(String strSiteScoreTagId) {
        this.strSiteScoreTagId = strSiteScoreTagId;
    }

    public String getStrSiteScoreTagName() {
        return strSiteScoreTagName;
    }

    public void setStrSiteScoreTagName(String strSiteScoreTagName) {
        this.strSiteScoreTagName = strSiteScoreTagName;
    }

    public String getStrSiteScoreTagDescription() {
        return strSiteScoreTagDescription;
    }

    public void setStrSiteScoreTagDescription(String strSiteScoreTagDescription) {
        this.strSiteScoreTagDescription = strSiteScoreTagDescription;
    }

    public String getStrSiteScoreParentId() {
        return strSiteScoreParentId;
    }

    public void setStrSiteScoreParentId(String strSiteScoreParentId) {
        this.strSiteScoreParentId = strSiteScoreParentId;
    }

    public String getStrSiteScoreYear() {
        return strSiteScoreYear;
    }

    public void setStrSiteScoreYear(String strSiteScoreYear) {
        this.strSiteScoreYear = strSiteScoreYear;
    }

    public String getDatSiteScoreCreatedTime() {
        return datSiteScoreCreatedTime;
    }

    public void setDatSiteScoreCreatedTime(String datSiteScoreCreatedTime) {
        this.datSiteScoreCreatedTime = datSiteScoreCreatedTime;
    }

    public String getStrSiteScoreCreator() {
        return strSiteScoreCreator;
    }

    public void setStrSiteScoreCreator(String strSiteScoreCreator) {
        this.strSiteScoreCreator = strSiteScoreCreator;
    }

    public float getFlaSiteScoreStandardScore() {
        return flaSiteScoreStandardScore;
    }

    public void setFlaSiteScoreStandardScore(float flaSiteScoreStandardScore) {
        this.flaSiteScoreStandardScore = flaSiteScoreStandardScore;
    }

    public float getFlaSiteScoreTagScore() {
        return flaSiteScoreTagScore;
    }

    public void setFlaSiteScoreTagScore(float flaSiteScoreTagScore) {
        this.flaSiteScoreTagScore = flaSiteScoreTagScore;
    }

    public String getStrSiteinfoScoreId() {
        return strSiteinfoScoreId;
    }

    public void setStrSiteinfoScoreId(String strSiteinfoScoreId) {
        this.strSiteinfoScoreId = strSiteinfoScoreId;
    }

    public String getStrSiteinfoScoreTaskId() {
        return strSiteinfoScoreTaskId;
    }

    public void setStrSiteinfoScoreTaskId(String strSiteinfoScoreTaskId) {
        this.strSiteinfoScoreTaskId = strSiteinfoScoreTaskId;
    }

    public String getStrSiteinfoTagType() {
        return strSiteinfoTagType;
    }

    public void setStrSiteinfoTagType(String strSiteinfoTagType) {
        this.strSiteinfoTagType = strSiteinfoTagType;
    }

    public String getStrSiteinfoScoreTagId() {
        return strSiteinfoScoreTagId;
    }

    public void setStrSiteinfoScoreTagId(String strSiteinfoScoreTagId) {
        this.strSiteinfoScoreTagId = strSiteinfoScoreTagId;
    }

    public String getStrSiteinfoScoreTagName() {
        return strSiteinfoScoreTagName;
    }

    public void setStrSiteinfoScoreTagName(String strSiteinfoScoreTagName) {
        this.strSiteinfoScoreTagName = strSiteinfoScoreTagName;
    }

    public String getStrSiteinfoScoreTagRule() {
        return strSiteinfoScoreTagRule;
    }

    public void setStrSiteinfoScoreTagRule(String strSiteinfoScoreTagRule) {
        this.strSiteinfoScoreTagRule = strSiteinfoScoreTagRule;
    }

    public String getStrSiteinfoScoreParentId() {
        return strSiteinfoScoreParentId;
    }

    public void setStrSiteinfoScoreParentId(String strSiteinfoScoreParentId) {
        this.strSiteinfoScoreParentId = strSiteinfoScoreParentId;
    }

    public String getStrSiteinfoScoreYear() {
        return strSiteinfoScoreYear;
    }

    public void setStrSiteinfoScoreYear(String strSiteinfoScoreYear) {
        this.strSiteinfoScoreYear = strSiteinfoScoreYear;
    }

    public String getDatSiteinfoScoreCreatedTime() {
        return datSiteinfoScoreCreatedTime;
    }

    public void setDatSiteinfoScoreCreatedTime(String datSiteinfoScoreCreatedTime) {
        this.datSiteinfoScoreCreatedTime = datSiteinfoScoreCreatedTime;
    }

    public String getStrSiteinfoScoreCreator() {
        return strSiteinfoScoreCreator;
    }

    public void setStrSiteinfoScoreCreator(String strSiteinfoScoreCreator) {
        this.strSiteinfoScoreCreator = strSiteinfoScoreCreator;
    }

    public float getFlaSiteinfoScoreStandardScore() {
        return flaSiteinfoScoreStandardScore;
    }

    public void setFlaSiteinfoScoreStandardScore(float flaSiteinfoScoreStandardScore) {
        this.flaSiteinfoScoreStandardScore = flaSiteinfoScoreStandardScore;
    }

    public float getFlaSiteinfoScoreTagScore() {
        return flaSiteinfoScoreTagScore;
    }

    public void setFlaSiteinfoScoreTagScore(float flaSiteinfoScoreTagScore) {
        this.flaSiteinfoScoreTagScore = flaSiteinfoScoreTagScore;
    }

    public String getStrUserName() {
        return strUserName;
    }

    public void setStrUserName(String strUserName) {
        this.strUserName = strUserName;
    }

    public String getStrSiteName() {
        return strSiteName;
    }

    public void setStrSiteName(String strSiteName) {
        this.strSiteName = strSiteName;
    }

    public String getStrSiteUrl() {
        return strSiteUrl;
    }

    public void setStrSiteUrl(String strSiteUrl) {
        this.strSiteUrl = strSiteUrl;
    }

    public String getStrTotalScoreUserId() {
        return strTotalScoreUserId;
    }

    public void setStrTotalScoreUserId(String strTotalScoreUserId) {
        this.strTotalScoreUserId = strTotalScoreUserId;
    }

    /**
		 * @return the templateUrl
		 */
    public String getTemplateUrl() {
        return templateUrl;
    }

    /**
		 * @param templateUrl the templateUrl to set
		 */
    public void setTemplateUrl(String templateUrl) {
        this.templateUrl = templateUrl;
    }
}
