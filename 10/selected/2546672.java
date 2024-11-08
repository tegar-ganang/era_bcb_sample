package com.dcivision.mail.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import com.dcivision.framework.ApplicationException;
import com.dcivision.framework.ErrorConstant;
import com.dcivision.framework.GlobalConstant;
import com.dcivision.framework.SessionContainer;
import com.dcivision.framework.TextUtility;
import com.dcivision.framework.UserInfoFactory;
import com.dcivision.framework.Utility;
import com.dcivision.framework.bean.AbstractBaseObject;
import com.dcivision.framework.dao.AbstractDAObject;
import com.dcivision.framework.web.AbstractSearchForm;
import com.dcivision.mail.bean.MailSetting;

/**
  MailSettingDAObject.java

  This class is the data access bean for table "MAIL_SETTING".

  @author      Rollo Chan
  @company     DCIVision Limited
  @creation date   20/04/2004
  @version     $Revision: 1.12 $
*/
public class MailSettingDAObject extends AbstractDAObject {

    public static final String REVISION = "$Revision: 1.12 $";

    public static final String TABLE_NAME = "MAIL_SETTING";

    public MailSettingDAObject(SessionContainer sessionContainer, Connection dbConn) {
        super(sessionContainer, dbConn);
    }

    protected void initDBSetting() {
        this.baseTableName = TABLE_NAME;
        this.vecDBColumn.add("ID");
        this.vecDBColumn.add("USER_RECORD_ID");
        this.vecDBColumn.add("PROFILE_NAME");
        this.vecDBColumn.add("MAIL_SERVER_TYPE");
        this.vecDBColumn.add("DISPLAY_NAME");
        this.vecDBColumn.add("EMAIL_ADDRESS");
        this.vecDBColumn.add("REMEMBER_PWD_FLAG");
        this.vecDBColumn.add("SPA_LOGIN_FLAG");
        this.vecDBColumn.add("INCOMING_SERVER_HOST");
        this.vecDBColumn.add("INCOMING_SERVER_PORT");
        this.vecDBColumn.add("INCOMING_SERVER_LOGIN_NAME");
        this.vecDBColumn.add("INCOMING_SERVER_LOGIN_PWD");
        this.vecDBColumn.add("OUTGOING_SERVER_HOST");
        this.vecDBColumn.add("OUTGOING_SERVER_PORT");
        this.vecDBColumn.add("OUTGOING_SERVER_LOGIN_NAME");
        this.vecDBColumn.add("OUTGOING_SERVER_LOGIN_PWD");
        this.vecDBColumn.add("PARAMETER_1");
        this.vecDBColumn.add("PARAMETER_2");
        this.vecDBColumn.add("PARAMETER_3");
        this.vecDBColumn.add("PARAMETER_4");
        this.vecDBColumn.add("PARAMETER_5");
        this.vecDBColumn.add("RECORD_STATUS");
        this.vecDBColumn.add("UPDATE_COUNT");
        this.vecDBColumn.add("CREATOR_ID");
        this.vecDBColumn.add("CREATE_DATE");
        this.vecDBColumn.add("UPDATER_ID");
        this.vecDBColumn.add("UPDATE_DATE");
    }

    protected synchronized AbstractBaseObject getByID(Integer id) throws ApplicationException {
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        synchronized (dbConn) {
            try {
                sqlStat.append("SELECT A.ID, A.USER_RECORD_ID, A.PROFILE_NAME, A.MAIL_SERVER_TYPE, A.DISPLAY_NAME, A.EMAIL_ADDRESS, A.REMEMBER_PWD_FLAG, A.SPA_LOGIN_FLAG, A.INCOMING_SERVER_HOST, A.INCOMING_SERVER_PORT, A.INCOMING_SERVER_LOGIN_NAME, A.INCOMING_SERVER_LOGIN_PWD, A.OUTGOING_SERVER_HOST, A.OUTGOING_SERVER_PORT, A.OUTGOING_SERVER_LOGIN_NAME, A.OUTGOING_SERVER_LOGIN_PWD, A.PARAMETER_1, A.PARAMETER_2, A.PARAMETER_3, A.PARAMETER_4, A.PARAMETER_5, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE ");
                sqlStat.append("FROM   MAIL_SETTING A ");
                sqlStat.append("WHERE  A.ID = ? AND A.RECORD_STATUS = ? ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                this.setPrepareStatement(preStat, 1, id);
                this.setPrepareStatement(preStat, 2, GlobalConstant.RECORD_STATUS_ACTIVE);
                rs = preStat.executeQuery();
                if (rs.next()) {
                    MailSetting tmpMailSetting = new MailSetting();
                    tmpMailSetting.setID(getResultSetInteger(rs, "ID"));
                    tmpMailSetting.setUserRecordID(getResultSetInteger(rs, "USER_RECORD_ID"));
                    tmpMailSetting.setProfileName(getResultSetString(rs, "PROFILE_NAME"));
                    tmpMailSetting.setMailServerType(getResultSetString(rs, "MAIL_SERVER_TYPE"));
                    tmpMailSetting.setDisplayName(getResultSetString(rs, "DISPLAY_NAME"));
                    tmpMailSetting.setEmailAddress(getResultSetString(rs, "EMAIL_ADDRESS"));
                    tmpMailSetting.setRememberPwdFlag(getResultSetString(rs, "REMEMBER_PWD_FLAG"));
                    tmpMailSetting.setSpaLoginFlag(getResultSetString(rs, "SPA_LOGIN_FLAG"));
                    tmpMailSetting.setIncomingServerHost(getResultSetString(rs, "INCOMING_SERVER_HOST"));
                    tmpMailSetting.setIncomingServerPort(getResultSetInteger(rs, "INCOMING_SERVER_PORT"));
                    tmpMailSetting.setIncomingServerLoginName(getResultSetString(rs, "INCOMING_SERVER_LOGIN_NAME"));
                    tmpMailSetting.setIncomingServerLoginPwd(getResultSetString(rs, "INCOMING_SERVER_LOGIN_PWD"));
                    tmpMailSetting.setOutgoingServerHost(getResultSetString(rs, "OUTGOING_SERVER_HOST"));
                    tmpMailSetting.setOutgoingServerPort(getResultSetInteger(rs, "OUTGOING_SERVER_PORT"));
                    tmpMailSetting.setOutgoingServerLoginName(getResultSetString(rs, "OUTGOING_SERVER_LOGIN_NAME"));
                    tmpMailSetting.setOutgoingServerLoginPwd(getResultSetString(rs, "OUTGOING_SERVER_LOGIN_PWD"));
                    tmpMailSetting.setParameter1(getResultSetString(rs, "PARAMETER_1"));
                    tmpMailSetting.setParameter2(getResultSetString(rs, "PARAMETER_2"));
                    tmpMailSetting.setParameter3(getResultSetString(rs, "PARAMETER_3"));
                    tmpMailSetting.setParameter4(getResultSetString(rs, "PARAMETER_4"));
                    tmpMailSetting.setParameter5(getResultSetString(rs, "PARAMETER_5"));
                    tmpMailSetting.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                    tmpMailSetting.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                    tmpMailSetting.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                    tmpMailSetting.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                    tmpMailSetting.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                    tmpMailSetting.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                    tmpMailSetting.setCreatorName(UserInfoFactory.getUserFullName(tmpMailSetting.getCreatorID()));
                    tmpMailSetting.setUpdaterName(UserInfoFactory.getUserFullName(tmpMailSetting.getUpdaterID()));
                    return (tmpMailSetting);
                } else {
                    throw new ApplicationException(ErrorConstant.DB_RECORD_NOT_FOUND_ERROR);
                }
            } catch (ApplicationException appEx) {
                throw appEx;
            } catch (SQLException sqle) {
                log.error(sqle, sqle);
                throw new ApplicationException(ErrorConstant.DB_GENERAL_ERROR, sqle, sqle.toString());
            } catch (Exception e) {
                log.error(e, e);
                throw new ApplicationException(ErrorConstant.DB_SELECT_ERROR, e);
            } finally {
                try {
                    rs.close();
                } catch (Exception ignore) {
                } finally {
                    rs = null;
                }
                try {
                    preStat.close();
                } catch (Exception ignore) {
                } finally {
                    preStat = null;
                }
            }
        }
    }

    protected synchronized List getList(AbstractSearchForm searchForm) throws ApplicationException {
        PreparedStatement preStat = null;
        PreparedStatement preStatCnt = null;
        ResultSet rs = null;
        ResultSet rsCnt = null;
        StringBuffer sqlStat = new StringBuffer();
        StringBuffer sqlStatCnt = new StringBuffer();
        List result = new ArrayList();
        int totalNumOfRecord = 0;
        int rowLoopCnt = 0;
        int startOffset = TextUtility.parseInteger(searchForm.getCurStartRowNo());
        int pageSize = TextUtility.parseInteger(searchForm.getPageOffset());
        synchronized (dbConn) {
            try {
                sqlStat.append("SELECT A.ID, A.USER_RECORD_ID, A.PROFILE_NAME, A.MAIL_SERVER_TYPE, A.DISPLAY_NAME, A.EMAIL_ADDRESS, A.REMEMBER_PWD_FLAG, A.SPA_LOGIN_FLAG, A.INCOMING_SERVER_HOST, A.INCOMING_SERVER_PORT, A.INCOMING_SERVER_LOGIN_NAME, A.INCOMING_SERVER_LOGIN_PWD, A.OUTGOING_SERVER_HOST, A.OUTGOING_SERVER_PORT, A.OUTGOING_SERVER_LOGIN_NAME, A.OUTGOING_SERVER_LOGIN_PWD, A.PARAMETER_1, A.PARAMETER_2, A.PARAMETER_3, A.PARAMETER_4, A.PARAMETER_5, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE ");
                sqlStat.append("FROM   MAIL_SETTING A ");
                sqlStat.append("WHERE  A.RECORD_STATUS = ? ");
                if (searchForm.isSearchable()) {
                    String searchField = getSearchColumn(searchForm.getBasicSearchField());
                    sqlStat.append("AND  " + searchField + " " + searchForm.getBasicSearchType() + " ? ");
                }
                sqlStat = this.getFormattedSQL(sqlStat.toString());
                if (searchForm.isSortable()) {
                    String sortAttribute = searchForm.getSortAttribute();
                    if (sortAttribute.indexOf(".") < 0) {
                        sortAttribute = "A." + sortAttribute;
                    }
                    sqlStat.append("ORDER BY " + sortAttribute + " " + searchForm.getSortOrder());
                }
                sqlStatCnt = this.getSelectCountSQL(sqlStat);
                preStatCnt = dbConn.prepareStatement(sqlStatCnt.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                this.setPrepareStatement(preStatCnt, 1, GlobalConstant.RECORD_STATUS_ACTIVE);
                if (searchForm.isSearchable()) {
                    String searchKeyword = this.getFormattedKeyword(searchForm.getBasicSearchKeyword(), searchForm.getBasicSearchType());
                    this.setPrepareStatement(preStatCnt, 2, searchKeyword);
                }
                rsCnt = preStatCnt.executeQuery();
                if (rsCnt.next()) {
                    totalNumOfRecord = rsCnt.getInt(1);
                }
                try {
                    rsCnt.close();
                } catch (Exception ignore) {
                } finally {
                    rsCnt = null;
                }
                try {
                    preStatCnt.close();
                } catch (Exception ignore) {
                } finally {
                    preStatCnt = null;
                }
                sqlStat = this.getSelectListSQL(sqlStat, startOffset, pageSize);
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                this.setPrepareStatement(preStat, 1, GlobalConstant.RECORD_STATUS_ACTIVE);
                if (searchForm.isSearchable()) {
                    String searchKeyword = this.getFormattedKeyword(searchForm.getBasicSearchKeyword(), searchForm.getBasicSearchType());
                    this.setPrepareStatement(preStat, 2, searchKeyword);
                }
                rs = preStat.executeQuery();
                this.positionCursor(rs, startOffset, pageSize);
                while (rs.next() && rowLoopCnt < pageSize) {
                    MailSetting tmpMailSetting = new MailSetting();
                    tmpMailSetting.setID(getResultSetInteger(rs, "ID"));
                    tmpMailSetting.setUserRecordID(getResultSetInteger(rs, "USER_RECORD_ID"));
                    tmpMailSetting.setProfileName(getResultSetString(rs, "PROFILE_NAME"));
                    tmpMailSetting.setMailServerType(getResultSetString(rs, "MAIL_SERVER_TYPE"));
                    tmpMailSetting.setDisplayName(getResultSetString(rs, "DISPLAY_NAME"));
                    tmpMailSetting.setEmailAddress(getResultSetString(rs, "EMAIL_ADDRESS"));
                    tmpMailSetting.setRememberPwdFlag(getResultSetString(rs, "REMEMBER_PWD_FLAG"));
                    tmpMailSetting.setSpaLoginFlag(getResultSetString(rs, "SPA_LOGIN_FLAG"));
                    tmpMailSetting.setIncomingServerHost(getResultSetString(rs, "INCOMING_SERVER_HOST"));
                    tmpMailSetting.setIncomingServerPort(getResultSetInteger(rs, "INCOMING_SERVER_PORT"));
                    tmpMailSetting.setIncomingServerLoginName(getResultSetString(rs, "INCOMING_SERVER_LOGIN_NAME"));
                    tmpMailSetting.setIncomingServerLoginPwd(getResultSetString(rs, "INCOMING_SERVER_LOGIN_PWD"));
                    tmpMailSetting.setOutgoingServerHost(getResultSetString(rs, "OUTGOING_SERVER_HOST"));
                    tmpMailSetting.setOutgoingServerPort(getResultSetInteger(rs, "OUTGOING_SERVER_PORT"));
                    tmpMailSetting.setOutgoingServerLoginName(getResultSetString(rs, "OUTGOING_SERVER_LOGIN_NAME"));
                    tmpMailSetting.setOutgoingServerLoginPwd(getResultSetString(rs, "OUTGOING_SERVER_LOGIN_PWD"));
                    tmpMailSetting.setParameter1(getResultSetString(rs, "PARAMETER_1"));
                    tmpMailSetting.setParameter2(getResultSetString(rs, "PARAMETER_2"));
                    tmpMailSetting.setParameter3(getResultSetString(rs, "PARAMETER_3"));
                    tmpMailSetting.setParameter4(getResultSetString(rs, "PARAMETER_4"));
                    tmpMailSetting.setParameter5(getResultSetString(rs, "PARAMETER_5"));
                    tmpMailSetting.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                    tmpMailSetting.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                    tmpMailSetting.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                    tmpMailSetting.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                    tmpMailSetting.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                    tmpMailSetting.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                    tmpMailSetting.setCreatorName(UserInfoFactory.getUserFullName(tmpMailSetting.getCreatorID()));
                    tmpMailSetting.setUpdaterName(UserInfoFactory.getUserFullName(tmpMailSetting.getUpdaterID()));
                    tmpMailSetting.setRecordCount(totalNumOfRecord);
                    tmpMailSetting.setRowNum(startOffset++);
                    ++rowLoopCnt;
                    result.add(tmpMailSetting);
                }
                return (result);
            } catch (ApplicationException appEx) {
                throw appEx;
            } catch (SQLException sqle) {
                log.error(sqle, sqle);
                throw new ApplicationException(ErrorConstant.DB_GENERAL_ERROR, sqle, sqle.toString());
            } catch (Exception e) {
                log.error(e, e);
                throw new ApplicationException(ErrorConstant.DB_SELECT_ERROR, e);
            } finally {
                try {
                    rs.close();
                } catch (Exception ignore) {
                } finally {
                    rs = null;
                }
                try {
                    preStat.close();
                } catch (Exception ignore) {
                } finally {
                    preStat = null;
                }
                try {
                    rsCnt.close();
                } catch (Exception ignore) {
                } finally {
                    rsCnt = null;
                }
                try {
                    preStatCnt.close();
                } catch (Exception ignore) {
                } finally {
                    preStatCnt = null;
                }
            }
        }
    }

    protected synchronized List getList() throws ApplicationException {
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        List result = new ArrayList();
        synchronized (dbConn) {
            try {
                sqlStat.append("SELECT A.ID, A.USER_RECORD_ID, A.PROFILE_NAME, A.MAIL_SERVER_TYPE, A.DISPLAY_NAME, A.EMAIL_ADDRESS, A.REMEMBER_PWD_FLAG, A.SPA_LOGIN_FLAG, A.INCOMING_SERVER_HOST, A.INCOMING_SERVER_PORT, A.INCOMING_SERVER_LOGIN_NAME, A.INCOMING_SERVER_LOGIN_PWD, A.OUTGOING_SERVER_HOST, A.OUTGOING_SERVER_PORT, A.OUTGOING_SERVER_LOGIN_NAME, A.OUTGOING_SERVER_LOGIN_PWD, A.PARAMETER_1, A.PARAMETER_2, A.PARAMETER_3, A.PARAMETER_4, A.PARAMETER_5, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE ");
                sqlStat.append("FROM   MAIL_SETTING A ");
                sqlStat.append("WHERE  A.RECORD_STATUS = ? ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                this.setPrepareStatement(preStat, 1, GlobalConstant.RECORD_STATUS_ACTIVE);
                rs = preStat.executeQuery();
                while (rs.next()) {
                    MailSetting tmpMailSetting = new MailSetting();
                    tmpMailSetting.setID(getResultSetInteger(rs, "ID"));
                    tmpMailSetting.setUserRecordID(getResultSetInteger(rs, "USER_RECORD_ID"));
                    tmpMailSetting.setProfileName(getResultSetString(rs, "PROFILE_NAME"));
                    tmpMailSetting.setMailServerType(getResultSetString(rs, "MAIL_SERVER_TYPE"));
                    tmpMailSetting.setDisplayName(getResultSetString(rs, "DISPLAY_NAME"));
                    tmpMailSetting.setEmailAddress(getResultSetString(rs, "EMAIL_ADDRESS"));
                    tmpMailSetting.setRememberPwdFlag(getResultSetString(rs, "REMEMBER_PWD_FLAG"));
                    tmpMailSetting.setSpaLoginFlag(getResultSetString(rs, "SPA_LOGIN_FLAG"));
                    tmpMailSetting.setIncomingServerHost(getResultSetString(rs, "INCOMING_SERVER_HOST"));
                    tmpMailSetting.setIncomingServerPort(getResultSetInteger(rs, "INCOMING_SERVER_PORT"));
                    tmpMailSetting.setIncomingServerLoginName(getResultSetString(rs, "INCOMING_SERVER_LOGIN_NAME"));
                    tmpMailSetting.setIncomingServerLoginPwd(getResultSetString(rs, "INCOMING_SERVER_LOGIN_PWD"));
                    tmpMailSetting.setOutgoingServerHost(getResultSetString(rs, "OUTGOING_SERVER_HOST"));
                    tmpMailSetting.setOutgoingServerPort(getResultSetInteger(rs, "OUTGOING_SERVER_PORT"));
                    tmpMailSetting.setOutgoingServerLoginName(getResultSetString(rs, "OUTGOING_SERVER_LOGIN_NAME"));
                    tmpMailSetting.setOutgoingServerLoginPwd(getResultSetString(rs, "OUTGOING_SERVER_LOGIN_PWD"));
                    tmpMailSetting.setParameter1(getResultSetString(rs, "PARAMETER_1"));
                    tmpMailSetting.setParameter2(getResultSetString(rs, "PARAMETER_2"));
                    tmpMailSetting.setParameter3(getResultSetString(rs, "PARAMETER_3"));
                    tmpMailSetting.setParameter4(getResultSetString(rs, "PARAMETER_4"));
                    tmpMailSetting.setParameter5(getResultSetString(rs, "PARAMETER_5"));
                    tmpMailSetting.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                    tmpMailSetting.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                    tmpMailSetting.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                    tmpMailSetting.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                    tmpMailSetting.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                    tmpMailSetting.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                    tmpMailSetting.setCreatorName(UserInfoFactory.getUserFullName(tmpMailSetting.getCreatorID()));
                    tmpMailSetting.setUpdaterName(UserInfoFactory.getUserFullName(tmpMailSetting.getUpdaterID()));
                    result.add(tmpMailSetting);
                }
                return (result);
            } catch (ApplicationException appEx) {
                throw appEx;
            } catch (SQLException sqle) {
                log.error(sqle, sqle);
                throw new ApplicationException(ErrorConstant.DB_GENERAL_ERROR, sqle, sqle.toString());
            } catch (Exception e) {
                log.error(e, e);
                throw new ApplicationException(ErrorConstant.DB_SELECT_ERROR, e);
            } finally {
                try {
                    rs.close();
                } catch (Exception ignore) {
                } finally {
                    rs = null;
                }
                try {
                    preStat.close();
                } catch (Exception ignore) {
                } finally {
                    preStat = null;
                }
            }
        }
    }

    protected void validateInsert(AbstractBaseObject obj) throws ApplicationException {
    }

    protected void validateUpdate(AbstractBaseObject obj) throws ApplicationException {
    }

    protected void validateDelete(AbstractBaseObject obj) throws ApplicationException {
    }

    public synchronized AbstractBaseObject insert(AbstractBaseObject obj) throws ApplicationException {
        PreparedStatement preStat = null;
        StringBuffer sqlStat = new StringBuffer();
        MailSetting tmpMailSetting = (MailSetting) ((MailSetting) obj).clone();
        synchronized (dbConn) {
            try {
                Integer nextID = getNextPrimaryID();
                Timestamp currTime = Utility.getCurrentTimestamp();
                sqlStat.append("INSERT ");
                sqlStat.append("INTO   MAIL_SETTING(ID, USER_RECORD_ID, PROFILE_NAME, MAIL_SERVER_TYPE, DISPLAY_NAME, EMAIL_ADDRESS, REMEMBER_PWD_FLAG, SPA_LOGIN_FLAG, INCOMING_SERVER_HOST, INCOMING_SERVER_PORT, INCOMING_SERVER_LOGIN_NAME, INCOMING_SERVER_LOGIN_PWD, OUTGOING_SERVER_HOST, OUTGOING_SERVER_PORT, OUTGOING_SERVER_LOGIN_NAME, OUTGOING_SERVER_LOGIN_PWD, PARAMETER_1, PARAMETER_2, PARAMETER_3, PARAMETER_4, PARAMETER_5, RECORD_STATUS, UPDATE_COUNT, CREATOR_ID, CREATE_DATE, UPDATER_ID, UPDATE_DATE) ");
                sqlStat.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                setPrepareStatement(preStat, 1, nextID);
                setPrepareStatement(preStat, 2, tmpMailSetting.getUserRecordID());
                setPrepareStatement(preStat, 3, tmpMailSetting.getProfileName());
                setPrepareStatement(preStat, 4, tmpMailSetting.getMailServerType());
                setPrepareStatement(preStat, 5, tmpMailSetting.getDisplayName());
                setPrepareStatement(preStat, 6, tmpMailSetting.getEmailAddress());
                setPrepareStatement(preStat, 7, tmpMailSetting.getRememberPwdFlag());
                setPrepareStatement(preStat, 8, tmpMailSetting.getSpaLoginFlag());
                setPrepareStatement(preStat, 9, tmpMailSetting.getIncomingServerHost());
                setPrepareStatement(preStat, 10, tmpMailSetting.getIncomingServerPort());
                setPrepareStatement(preStat, 11, tmpMailSetting.getIncomingServerLoginName());
                setPrepareStatement(preStat, 12, tmpMailSetting.getIncomingServerLoginPwd());
                setPrepareStatement(preStat, 13, tmpMailSetting.getOutgoingServerHost());
                setPrepareStatement(preStat, 14, tmpMailSetting.getOutgoingServerPort());
                setPrepareStatement(preStat, 15, tmpMailSetting.getOutgoingServerLoginName());
                setPrepareStatement(preStat, 16, tmpMailSetting.getOutgoingServerLoginPwd());
                setPrepareStatement(preStat, 17, tmpMailSetting.getParameter1());
                setPrepareStatement(preStat, 18, tmpMailSetting.getParameter2());
                setPrepareStatement(preStat, 19, tmpMailSetting.getParameter3());
                setPrepareStatement(preStat, 20, tmpMailSetting.getParameter4());
                setPrepareStatement(preStat, 21, tmpMailSetting.getParameter5());
                setPrepareStatement(preStat, 22, GlobalConstant.RECORD_STATUS_ACTIVE);
                setPrepareStatement(preStat, 23, new Integer(0));
                setPrepareStatement(preStat, 24, sessionContainer.getUserRecordID());
                setPrepareStatement(preStat, 25, currTime);
                setPrepareStatement(preStat, 26, sessionContainer.getUserRecordID());
                setPrepareStatement(preStat, 27, currTime);
                preStat.executeUpdate();
                tmpMailSetting.setID(nextID);
                tmpMailSetting.setCreatorID(sessionContainer.getUserRecordID());
                tmpMailSetting.setCreateDate(currTime);
                tmpMailSetting.setUpdaterID(sessionContainer.getUserRecordID());
                tmpMailSetting.setUpdateDate(currTime);
                tmpMailSetting.setUpdateCount(new Integer(0));
                tmpMailSetting.setCreatorName(UserInfoFactory.getUserFullName(tmpMailSetting.getCreatorID()));
                tmpMailSetting.setUpdaterName(UserInfoFactory.getUserFullName(tmpMailSetting.getUpdaterID()));
                dbConn.commit();
                return (tmpMailSetting);
            } catch (SQLException sqle) {
                log.error(sqle, sqle);
            } catch (Exception e) {
                try {
                    dbConn.rollback();
                } catch (Exception ex) {
                }
                log.error(e, e);
            } finally {
                try {
                    preStat.close();
                } catch (Exception ignore) {
                } finally {
                    preStat = null;
                }
            }
            return null;
        }
    }

    public synchronized AbstractBaseObject update(AbstractBaseObject obj) throws ApplicationException {
        PreparedStatement preStat = null;
        StringBuffer sqlStat = new StringBuffer();
        MailSetting tmpMailSetting = (MailSetting) ((MailSetting) obj).clone();
        synchronized (dbConn) {
            try {
                int updateCnt = 0;
                Timestamp currTime = Utility.getCurrentTimestamp();
                sqlStat.append("UPDATE MAIL_SETTING ");
                sqlStat.append("SET  USER_RECORD_ID=?, PROFILE_NAME=?, MAIL_SERVER_TYPE=?, DISPLAY_NAME=?, EMAIL_ADDRESS=?, REMEMBER_PWD_FLAG=?, SPA_LOGIN_FLAG=?, INCOMING_SERVER_HOST=?, INCOMING_SERVER_PORT=?, INCOMING_SERVER_LOGIN_NAME=?, INCOMING_SERVER_LOGIN_PWD=?, OUTGOING_SERVER_HOST=?, OUTGOING_SERVER_PORT=?, OUTGOING_SERVER_LOGIN_NAME=?, OUTGOING_SERVER_LOGIN_PWD=?, PARAMETER_1=?, PARAMETER_2=?, PARAMETER_3=?, PARAMETER_4=?, PARAMETER_5=?, UPDATE_COUNT=?, UPDATER_ID=?, UPDATE_DATE=? ");
                sqlStat.append("WHERE  ID=? AND UPDATE_COUNT=? ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                setPrepareStatement(preStat, 1, tmpMailSetting.getUserRecordID());
                setPrepareStatement(preStat, 2, tmpMailSetting.getProfileName());
                setPrepareStatement(preStat, 3, tmpMailSetting.getMailServerType());
                setPrepareStatement(preStat, 4, tmpMailSetting.getDisplayName());
                setPrepareStatement(preStat, 5, tmpMailSetting.getEmailAddress());
                setPrepareStatement(preStat, 6, tmpMailSetting.getRememberPwdFlag());
                setPrepareStatement(preStat, 7, tmpMailSetting.getSpaLoginFlag());
                setPrepareStatement(preStat, 8, tmpMailSetting.getIncomingServerHost());
                setPrepareStatement(preStat, 9, tmpMailSetting.getIncomingServerPort());
                setPrepareStatement(preStat, 10, tmpMailSetting.getIncomingServerLoginName());
                setPrepareStatement(preStat, 11, tmpMailSetting.getIncomingServerLoginPwd());
                setPrepareStatement(preStat, 12, tmpMailSetting.getOutgoingServerHost());
                setPrepareStatement(preStat, 13, tmpMailSetting.getOutgoingServerPort());
                setPrepareStatement(preStat, 14, tmpMailSetting.getOutgoingServerLoginName());
                setPrepareStatement(preStat, 15, tmpMailSetting.getOutgoingServerLoginPwd());
                setPrepareStatement(preStat, 16, tmpMailSetting.getParameter1());
                setPrepareStatement(preStat, 17, tmpMailSetting.getParameter2());
                setPrepareStatement(preStat, 18, tmpMailSetting.getParameter3());
                setPrepareStatement(preStat, 19, tmpMailSetting.getParameter4());
                setPrepareStatement(preStat, 20, tmpMailSetting.getParameter5());
                setPrepareStatement(preStat, 21, new Integer(tmpMailSetting.getUpdateCount().intValue() + 1));
                setPrepareStatement(preStat, 22, sessionContainer.getUserRecordID());
                setPrepareStatement(preStat, 23, currTime);
                setPrepareStatement(preStat, 24, tmpMailSetting.getID());
                setPrepareStatement(preStat, 25, tmpMailSetting.getUpdateCount());
                updateCnt = preStat.executeUpdate();
                dbConn.commit();
                if (updateCnt == 0) {
                    throw new ApplicationException(ErrorConstant.DB_CONCURRENT_ERROR);
                } else {
                    tmpMailSetting.setUpdaterID(sessionContainer.getUserRecordID());
                    tmpMailSetting.setUpdateDate(currTime);
                    tmpMailSetting.setUpdateCount(new Integer(tmpMailSetting.getUpdateCount().intValue() + 1));
                    tmpMailSetting.setCreatorName(UserInfoFactory.getUserFullName(tmpMailSetting.getCreatorID()));
                    tmpMailSetting.setUpdaterName(UserInfoFactory.getUserFullName(tmpMailSetting.getUpdaterID()));
                    return (tmpMailSetting);
                }
            } catch (Exception e) {
                try {
                    dbConn.rollback();
                } catch (Exception ex) {
                }
                log.error(e, e);
                throw new ApplicationException(ErrorConstant.DB_UPDATE_ERROR, e);
            } finally {
                try {
                    preStat.close();
                } catch (Exception ignore) {
                } finally {
                    preStat = null;
                }
            }
        }
    }

    protected synchronized AbstractBaseObject delete(AbstractBaseObject obj) throws ApplicationException {
        PreparedStatement preStat = null;
        StringBuffer sqlStat = new StringBuffer();
        MailSetting tmpMailSetting = (MailSetting) ((MailSetting) obj).clone();
        synchronized (dbConn) {
            try {
                int updateCnt = 0;
                sqlStat.append("DELETE ");
                sqlStat.append("FROM   MAIL_SETTING ");
                sqlStat.append("WHERE  ID=? AND UPDATE_COUNT=? ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                setPrepareStatement(preStat, 1, tmpMailSetting.getID());
                setPrepareStatement(preStat, 2, tmpMailSetting.getUpdateCount());
                updateCnt = preStat.executeUpdate();
                if (updateCnt == 0) {
                    throw new ApplicationException(ErrorConstant.DB_CONCURRENT_ERROR);
                } else {
                    return (tmpMailSetting);
                }
            } catch (ApplicationException appEx) {
                throw appEx;
            } catch (SQLException sqle) {
                log.error(sqle, sqle);
                throw new ApplicationException(ErrorConstant.DB_GENERAL_ERROR, sqle, sqle.toString());
            } catch (Exception e) {
                log.error(e, e);
                throw new ApplicationException(ErrorConstant.DB_DELETE_ERROR, e);
            } finally {
                try {
                    preStat.close();
                } catch (Exception ignore) {
                } finally {
                    preStat = null;
                }
            }
        }
    }

    protected synchronized void auditTrail(String opMode, AbstractBaseObject obj) throws ApplicationException {
        Vector oldValues = new Vector();
        Vector newValues = new Vector();
        MailSetting tmpMailSetting = (MailSetting) this.oldValue;
        if (tmpMailSetting != null) {
            oldValues.add(toAuditTrailValue(tmpMailSetting.getID()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getUserRecordID()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getProfileName()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getMailServerType()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getDisplayName()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getEmailAddress()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getRememberPwdFlag()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getSpaLoginFlag()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getIncomingServerHost()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getIncomingServerPort()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getIncomingServerLoginName()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getIncomingServerLoginPwd()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getOutgoingServerHost()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getOutgoingServerPort()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getOutgoingServerLoginName()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getOutgoingServerLoginPwd()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getParameter1()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getParameter2()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getParameter3()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getParameter4()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getParameter5()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getRecordStatus()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getUpdateCount()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getCreatorID()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getCreateDate()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getUpdaterID()));
            oldValues.add(toAuditTrailValue(tmpMailSetting.getUpdateDate()));
        }
        tmpMailSetting = (MailSetting) obj;
        if (tmpMailSetting != null) {
            newValues.add(toAuditTrailValue(tmpMailSetting.getID()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getUserRecordID()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getProfileName()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getMailServerType()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getDisplayName()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getEmailAddress()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getRememberPwdFlag()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getSpaLoginFlag()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getIncomingServerHost()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getIncomingServerPort()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getIncomingServerLoginName()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getIncomingServerLoginPwd()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getOutgoingServerHost()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getOutgoingServerPort()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getOutgoingServerLoginName()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getOutgoingServerLoginPwd()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getParameter1()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getParameter2()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getParameter3()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getParameter4()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getParameter5()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getRecordStatus()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getUpdateCount()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getCreatorID()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getCreateDate()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getUpdaterID()));
            newValues.add(toAuditTrailValue(tmpMailSetting.getUpdateDate()));
        }
        auditTrailBase(opMode, oldValues, newValues);
    }

    /***********************************************************************
   * DON'T Modify the codes above unless you know what you are doing!!!  *
   * Put your own functions beblow.                                      *
   * For FINDER methods, the function name should be in the notation:    *
   *   public Object getObjectBy<Criteria>()                             *
   *   - e.g. public Object getObjectByCode()                            *
   *   public List getListBy<Criteria>()                                 *
   *   - e.g. public List getListByUserID()                              *
   * For OPERATION methods, the function name should be in the notation: *
   *   public void <Operation>ObjectBy<Criteria>()                       *
   *   - e.g. public void deleteObjectByCode()                           *
   *   public void <Operation>ListBy<Criteria>()                         *
   *   - e.g. public void deleteListByUserID()                           *
   ***********************************************************************/
    public synchronized AbstractBaseObject getObjectByUserRecordID(Integer userRecordID) throws ApplicationException {
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        synchronized (dbConn) {
            try {
                sqlStat.append("SELECT A.ID, A.USER_RECORD_ID, A.PROFILE_NAME, A.MAIL_SERVER_TYPE, A.DISPLAY_NAME, A.EMAIL_ADDRESS, A.REMEMBER_PWD_FLAG, A.SPA_LOGIN_FLAG, A.INCOMING_SERVER_HOST, A.INCOMING_SERVER_PORT, A.INCOMING_SERVER_LOGIN_NAME, A.INCOMING_SERVER_LOGIN_PWD, A.OUTGOING_SERVER_HOST, A.OUTGOING_SERVER_PORT, A.OUTGOING_SERVER_LOGIN_NAME, A.OUTGOING_SERVER_LOGIN_PWD, A.PARAMETER_1, A.PARAMETER_2, A.PARAMETER_3, A.PARAMETER_4, A.PARAMETER_5, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE ");
                sqlStat.append("FROM   MAIL_SETTING A ");
                sqlStat.append("WHERE  A.USER_RECORD_ID = ? AND A.RECORD_STATUS = ? ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                this.setPrepareStatement(preStat, 1, userRecordID);
                this.setPrepareStatement(preStat, 2, GlobalConstant.RECORD_STATUS_ACTIVE);
                rs = preStat.executeQuery();
                if (rs.next()) {
                    MailSetting tmpMailSetting = new MailSetting();
                    tmpMailSetting.setID(getResultSetInteger(rs, "ID"));
                    tmpMailSetting.setUserRecordID(getResultSetInteger(rs, "USER_RECORD_ID"));
                    tmpMailSetting.setProfileName(getResultSetString(rs, "PROFILE_NAME"));
                    tmpMailSetting.setMailServerType(getResultSetString(rs, "MAIL_SERVER_TYPE"));
                    tmpMailSetting.setDisplayName(getResultSetString(rs, "DISPLAY_NAME"));
                    tmpMailSetting.setEmailAddress(getResultSetString(rs, "EMAIL_ADDRESS"));
                    tmpMailSetting.setRememberPwdFlag(getResultSetString(rs, "REMEMBER_PWD_FLAG"));
                    tmpMailSetting.setSpaLoginFlag(getResultSetString(rs, "SPA_LOGIN_FLAG"));
                    tmpMailSetting.setIncomingServerHost(getResultSetString(rs, "INCOMING_SERVER_HOST"));
                    tmpMailSetting.setIncomingServerPort(getResultSetInteger(rs, "INCOMING_SERVER_PORT"));
                    tmpMailSetting.setIncomingServerLoginName(getResultSetString(rs, "INCOMING_SERVER_LOGIN_NAME"));
                    tmpMailSetting.setIncomingServerLoginPwd(getResultSetString(rs, "INCOMING_SERVER_LOGIN_PWD"));
                    tmpMailSetting.setOutgoingServerHost(getResultSetString(rs, "OUTGOING_SERVER_HOST"));
                    tmpMailSetting.setOutgoingServerPort(getResultSetInteger(rs, "OUTGOING_SERVER_PORT"));
                    tmpMailSetting.setOutgoingServerLoginName(getResultSetString(rs, "OUTGOING_SERVER_LOGIN_NAME"));
                    tmpMailSetting.setOutgoingServerLoginPwd(getResultSetString(rs, "OUTGOING_SERVER_LOGIN_PWD"));
                    tmpMailSetting.setParameter1(getResultSetString(rs, "PARAMETER_1"));
                    tmpMailSetting.setParameter2(getResultSetString(rs, "PARAMETER_2"));
                    tmpMailSetting.setParameter3(getResultSetString(rs, "PARAMETER_3"));
                    tmpMailSetting.setParameter4(getResultSetString(rs, "PARAMETER_4"));
                    tmpMailSetting.setParameter5(getResultSetString(rs, "PARAMETER_5"));
                    tmpMailSetting.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                    tmpMailSetting.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                    tmpMailSetting.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                    tmpMailSetting.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                    tmpMailSetting.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                    tmpMailSetting.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                    tmpMailSetting.setCreatorName(UserInfoFactory.getUserFullName(tmpMailSetting.getCreatorID()));
                    tmpMailSetting.setUpdaterName(UserInfoFactory.getUserFullName(tmpMailSetting.getUpdaterID()));
                    return (tmpMailSetting);
                } else {
                    return (null);
                }
            } catch (ApplicationException appEx) {
                throw appEx;
            } catch (SQLException sqle) {
                log.error(sqle, sqle);
                throw new ApplicationException(ErrorConstant.DB_GENERAL_ERROR, sqle, sqle.toString());
            } catch (Exception e) {
                log.error(e, e);
                throw new ApplicationException(ErrorConstant.DB_SELECT_ERROR, e);
            } finally {
                try {
                    rs.close();
                } catch (Exception ignore) {
                } finally {
                    rs = null;
                }
                try {
                    preStat.close();
                } catch (Exception ignore) {
                } finally {
                    preStat = null;
                }
            }
        }
    }
}
