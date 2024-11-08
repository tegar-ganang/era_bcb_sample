package com.dcivision.dms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import com.dcivision.dms.bean.DmsRelationalWord;
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

/**
  DmsRelationalWordDAObject.java

  This class is the data access bean for Relational Word .

  @author          Weison Liang
  @company         DCIVision Limited
  @creation date   06/09/2004
  @version         $Revision: 1.19 $
  */
public class DmsRelationalWordDAObject extends AbstractDAObject {

    public static final String REVISION = "$Revision: 1.19 $";

    public static final String TABLE_NAME = "DMS_RELATIONAL_WORD";

    public DmsRelationalWordDAObject(SessionContainer sessionContainer, Connection dbConn) {
        super(sessionContainer, dbConn);
    }

    protected void initDBSetting() {
        this.baseTableName = TABLE_NAME;
        this.vecDBColumn.add("ID");
        this.vecDBColumn.add("RECORD_STATUS");
        this.vecDBColumn.add("UPDATE_COUNT");
        this.vecDBColumn.add("CREATOR_ID");
        this.vecDBColumn.add("CREATE_DATE");
        this.vecDBColumn.add("UPDATER_ID");
        this.vecDBColumn.add("UPDATE_DATE");
        this.vecDBColumn.add("WORD");
        this.vecDBColumn.add("PARENT_ID");
        this.vecDBColumn.add("TYPE");
    }

    protected synchronized AbstractBaseObject getByID(Integer id) throws ApplicationException {
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        synchronized (dbConn) {
            try {
                sqlStat.append("SELECT A.ID, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE, A.WORD, A.PARENT_ID, A.TYPE ");
                sqlStat.append("FROM   DMS_RELATIONAL_WORD A ");
                sqlStat.append("WHERE  A.ID = ? AND A.RECORD_STATUS = ? ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                this.setPrepareStatement(preStat, 1, id);
                this.setPrepareStatement(preStat, 2, GlobalConstant.RECORD_STATUS_ACTIVE);
                rs = preStat.executeQuery();
                if (rs.next()) {
                    DmsRelationalWord tmpDmsRelationalWord = new DmsRelationalWord();
                    tmpDmsRelationalWord.setID(getResultSetInteger(rs, "ID"));
                    tmpDmsRelationalWord.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                    tmpDmsRelationalWord.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                    tmpDmsRelationalWord.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                    tmpDmsRelationalWord.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                    tmpDmsRelationalWord.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                    tmpDmsRelationalWord.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                    tmpDmsRelationalWord.setWord(getResultSetString(rs, "WORD"));
                    tmpDmsRelationalWord.setParentID(getResultSetInteger(rs, "PARENT_ID"));
                    tmpDmsRelationalWord.setType(getResultSetString(rs, "TYPE"));
                    tmpDmsRelationalWord.setCreatorName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getCreatorID()));
                    tmpDmsRelationalWord.setUpdaterName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getUpdaterID()));
                    return (tmpDmsRelationalWord);
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
                sqlStat.append("SELECT A.ID, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE, A.WORD, A.PARENT_ID, A.TYPE ");
                sqlStat.append("FROM   DMS_RELATIONAL_WORD A ");
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
                    DmsRelationalWord tmpDmsRelationalWord = new DmsRelationalWord();
                    tmpDmsRelationalWord.setID(getResultSetInteger(rs, "ID"));
                    tmpDmsRelationalWord.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                    tmpDmsRelationalWord.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                    tmpDmsRelationalWord.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                    tmpDmsRelationalWord.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                    tmpDmsRelationalWord.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                    tmpDmsRelationalWord.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                    tmpDmsRelationalWord.setWord(getResultSetString(rs, "WORD"));
                    tmpDmsRelationalWord.setParentID(getResultSetInteger(rs, "PARENT_ID"));
                    tmpDmsRelationalWord.setType(getResultSetString(rs, "TYPE"));
                    tmpDmsRelationalWord.setCreatorName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getCreatorID()));
                    tmpDmsRelationalWord.setUpdaterName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getUpdaterID()));
                    tmpDmsRelationalWord.setRecordCount(totalNumOfRecord);
                    tmpDmsRelationalWord.setRowNum(startOffset++);
                    ++rowLoopCnt;
                    result.add(tmpDmsRelationalWord);
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
                sqlStat.append("SELECT A.ID, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE, A.WORD, A.PARENT_ID, A.TYPE ");
                sqlStat.append("FROM   DMS_RELATIONAL_WORD A ");
                sqlStat.append("WHERE  A.RECORD_STATUS = ? ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                this.setPrepareStatement(preStat, 1, GlobalConstant.RECORD_STATUS_ACTIVE);
                rs = preStat.executeQuery();
                while (rs.next()) {
                    DmsRelationalWord tmpDmsRelationalWord = new DmsRelationalWord();
                    tmpDmsRelationalWord.setID(getResultSetInteger(rs, "ID"));
                    tmpDmsRelationalWord.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                    tmpDmsRelationalWord.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                    tmpDmsRelationalWord.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                    tmpDmsRelationalWord.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                    tmpDmsRelationalWord.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                    tmpDmsRelationalWord.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                    tmpDmsRelationalWord.setWord(getResultSetString(rs, "WORD"));
                    tmpDmsRelationalWord.setParentID(getResultSetInteger(rs, "PARENT_ID"));
                    tmpDmsRelationalWord.setType(getResultSetString(rs, "TYPE"));
                    tmpDmsRelationalWord.setCreatorName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getCreatorID()));
                    tmpDmsRelationalWord.setUpdaterName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getUpdaterID()));
                    result.add(tmpDmsRelationalWord);
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

    protected synchronized AbstractBaseObject insert(AbstractBaseObject obj) throws ApplicationException {
        PreparedStatement preStat = null;
        StringBuffer sqlStat = new StringBuffer();
        DmsRelationalWord tmpDmsRelationalWord = (DmsRelationalWord) ((DmsRelationalWord) obj).clone();
        synchronized (dbConn) {
            try {
                Integer nextID = getNextPrimaryID();
                Timestamp currTime = Utility.getCurrentTimestamp();
                sqlStat.append("INSERT ");
                sqlStat.append("INTO   DMS_RELATIONAL_WORD(ID, RECORD_STATUS, UPDATE_COUNT, CREATOR_ID, CREATE_DATE, UPDATER_ID, UPDATE_DATE, WORD, PARENT_ID, TYPE) ");
                sqlStat.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                setPrepareStatement(preStat, 1, nextID);
                setPrepareStatement(preStat, 2, tmpDmsRelationalWord.getRecordStatus());
                setPrepareStatement(preStat, 3, new Integer(0));
                setPrepareStatement(preStat, 4, tmpDmsRelationalWord.getCreatorID());
                setPrepareStatement(preStat, 5, currTime);
                setPrepareStatement(preStat, 6, tmpDmsRelationalWord.getUpdaterID());
                setPrepareStatement(preStat, 7, currTime);
                if (tmpDmsRelationalWord.getWord() == null || "".equals(tmpDmsRelationalWord.getWord().trim())) {
                    return null;
                }
                setPrepareStatement(preStat, 8, tmpDmsRelationalWord.getWord());
                setPrepareStatement(preStat, 9, tmpDmsRelationalWord.getParentID());
                setPrepareStatement(preStat, 10, tmpDmsRelationalWord.getType());
                preStat.executeUpdate();
                tmpDmsRelationalWord.setID(nextID);
                tmpDmsRelationalWord.setCreatorID(tmpDmsRelationalWord.getCreatorID());
                tmpDmsRelationalWord.setCreateDate(currTime);
                tmpDmsRelationalWord.setUpdaterID(tmpDmsRelationalWord.getUpdaterID());
                tmpDmsRelationalWord.setUpdateDate(currTime);
                tmpDmsRelationalWord.setUpdateCount(new Integer(0));
                tmpDmsRelationalWord.setCreatorName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getCreatorID()));
                tmpDmsRelationalWord.setUpdaterName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getUpdaterID()));
                dbConn.commit();
                return (tmpDmsRelationalWord);
            } catch (Exception e) {
                try {
                    dbConn.rollback();
                } catch (Exception ee) {
                }
                log.error(e, e);
                throw new ApplicationException(ErrorConstant.DB_INSERT_ERROR, e);
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

    protected synchronized AbstractBaseObject update(AbstractBaseObject obj) throws ApplicationException {
        PreparedStatement preStat = null;
        StringBuffer sqlStat = new StringBuffer();
        DmsRelationalWord tmpDmsRelationalWord = (DmsRelationalWord) ((DmsRelationalWord) obj).clone();
        synchronized (dbConn) {
            try {
                int updateCnt = 0;
                Timestamp currTime = Utility.getCurrentTimestamp();
                sqlStat.append("UPDATE DMS_RELATIONAL_WORD ");
                sqlStat.append("SET  UPDATE_COUNT=?, UPDATER_ID=?, UPDATE_DATE=?, WORD=?, PARENT_ID=?, TYPE=? ,RECORD_STATUS=? ");
                sqlStat.append("WHERE  ID=? AND UPDATE_COUNT=? ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                setPrepareStatement(preStat, 1, new Integer(tmpDmsRelationalWord.getUpdateCount().intValue() + 1));
                setPrepareStatement(preStat, 2, tmpDmsRelationalWord.getUpdaterID());
                setPrepareStatement(preStat, 3, currTime);
                setPrepareStatement(preStat, 4, tmpDmsRelationalWord.getWord());
                setPrepareStatement(preStat, 5, tmpDmsRelationalWord.getParentID());
                setPrepareStatement(preStat, 6, tmpDmsRelationalWord.getType());
                setPrepareStatement(preStat, 7, tmpDmsRelationalWord.getRecordStatus());
                setPrepareStatement(preStat, 8, tmpDmsRelationalWord.getID());
                setPrepareStatement(preStat, 9, tmpDmsRelationalWord.getUpdateCount());
                updateCnt = preStat.executeUpdate();
                if (updateCnt == 0) {
                    throw new ApplicationException(ErrorConstant.DB_CONCURRENT_ERROR);
                } else {
                    tmpDmsRelationalWord.setUpdaterID(sessionContainer.getUserRecordID());
                    tmpDmsRelationalWord.setUpdateDate(currTime);
                    tmpDmsRelationalWord.setUpdateCount(new Integer(tmpDmsRelationalWord.getUpdateCount().intValue() + 1));
                    tmpDmsRelationalWord.setCreatorName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getCreatorID()));
                    tmpDmsRelationalWord.setUpdaterName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getUpdaterID()));
                    return (tmpDmsRelationalWord);
                }
            } catch (ApplicationException appEx) {
                throw appEx;
            } catch (SQLException sqle) {
                log.error(sqle, sqle);
                throw new ApplicationException(ErrorConstant.DB_GENERAL_ERROR, sqle, sqle.toString());
            } catch (Exception e) {
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
        DmsRelationalWord tmpDmsRelationalWord = (DmsRelationalWord) ((DmsRelationalWord) obj).clone();
        synchronized (dbConn) {
            try {
                int updateCnt = 0;
                sqlStat.append("DELETE ");
                sqlStat.append("FROM   DMS_RELATIONAL_WORD ");
                sqlStat.append("WHERE  ID=? AND UPDATE_COUNT=? ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                setPrepareStatement(preStat, 1, tmpDmsRelationalWord.getID());
                setPrepareStatement(preStat, 2, tmpDmsRelationalWord.getUpdateCount());
                updateCnt = preStat.executeUpdate();
                if (updateCnt == 0) {
                    throw new ApplicationException(ErrorConstant.DB_CONCURRENT_ERROR);
                } else {
                    return (tmpDmsRelationalWord);
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
        DmsRelationalWord tmpDmsRelationalWord = (DmsRelationalWord) this.oldValue;
        if (tmpDmsRelationalWord != null) {
            oldValues.add(toAuditTrailValue(tmpDmsRelationalWord.getID()));
            oldValues.add(toAuditTrailValue(tmpDmsRelationalWord.getRecordStatus()));
            oldValues.add(toAuditTrailValue(tmpDmsRelationalWord.getUpdateCount()));
            oldValues.add(toAuditTrailValue(tmpDmsRelationalWord.getCreatorID()));
            oldValues.add(toAuditTrailValue(tmpDmsRelationalWord.getCreateDate()));
            oldValues.add(toAuditTrailValue(tmpDmsRelationalWord.getUpdaterID()));
            oldValues.add(toAuditTrailValue(tmpDmsRelationalWord.getUpdateDate()));
            oldValues.add(toAuditTrailValue(tmpDmsRelationalWord.getWord()));
            oldValues.add(toAuditTrailValue(tmpDmsRelationalWord.getParentID()));
            oldValues.add(toAuditTrailValue(tmpDmsRelationalWord.getType()));
        }
        tmpDmsRelationalWord = (DmsRelationalWord) obj;
        if (tmpDmsRelationalWord != null) {
            newValues.add(toAuditTrailValue(tmpDmsRelationalWord.getID()));
            newValues.add(toAuditTrailValue(tmpDmsRelationalWord.getRecordStatus()));
            newValues.add(toAuditTrailValue(tmpDmsRelationalWord.getUpdateCount()));
            newValues.add(toAuditTrailValue(tmpDmsRelationalWord.getCreatorID()));
            newValues.add(toAuditTrailValue(tmpDmsRelationalWord.getCreateDate()));
            newValues.add(toAuditTrailValue(tmpDmsRelationalWord.getUpdaterID()));
            newValues.add(toAuditTrailValue(tmpDmsRelationalWord.getUpdateDate()));
            newValues.add(toAuditTrailValue(tmpDmsRelationalWord.getWord()));
            newValues.add(toAuditTrailValue(tmpDmsRelationalWord.getParentID()));
            newValues.add(toAuditTrailValue(tmpDmsRelationalWord.getType()));
        }
        auditTrailBase(opMode, oldValues, newValues);
    }

    /**
   * Gets the DmsRelationalWord object by keyword.
   * 
   * @param keyWord the keyword
   * @return the DmsRelationalWord object
   * @throws ApplicationException
   */
    public AbstractBaseObject getByKeyWord(String keyWord) throws ApplicationException {
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        StringBuffer sqlStatCnt = new StringBuffer();
        try {
            sqlStat.append("SELECT A.ID, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE, A.WORD, A.PARENT_ID, A.TYPE");
            sqlStat.append(" FROM DMS_RELATIONAL_WORD A ");
            sqlStat.append(" WHERE A.PARENT_ID=0 AND A.WORD = ? AND A.RECORD_STATUS = ?  ");
            preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            this.setPrepareStatement(preStat, 1, keyWord.trim());
            this.setPrepareStatement(preStat, 2, GlobalConstant.RECORD_STATUS_ACTIVE);
            rs = preStat.executeQuery();
            if (rs != null && rs.next()) {
                DmsRelationalWord tmpDmsRelationalWord = new DmsRelationalWord();
                tmpDmsRelationalWord.setID(getResultSetInteger(rs, "ID"));
                tmpDmsRelationalWord.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                tmpDmsRelationalWord.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                tmpDmsRelationalWord.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                tmpDmsRelationalWord.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                tmpDmsRelationalWord.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                tmpDmsRelationalWord.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                tmpDmsRelationalWord.setWord(getResultSetString(rs, "WORD"));
                tmpDmsRelationalWord.setParentID(getResultSetInteger(rs, "PARENT_ID"));
                tmpDmsRelationalWord.setType(getResultSetString(rs, "TYPE"));
                tmpDmsRelationalWord.setCreatorName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getCreatorID()));
                tmpDmsRelationalWord.setUpdaterName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getUpdaterID()));
                return (tmpDmsRelationalWord);
            } else {
                return null;
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

    /**
   * Gets list of the DmsRelationalWord objects by the word which are keyword or relational word 
   * 
   * @param keyWord the word (keyword or relational word)
   * @return list of the DmsRelationalWord objects 
   * @throws ApplicationException
   */
    public List getByRelationWordList(String keyWord) throws ApplicationException {
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        StringBuffer sqlStatCnt = new StringBuffer();
        try {
            sqlStat.append("SELECT A.ID, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE, A.WORD, A.PARENT_ID, A.TYPE");
            sqlStat.append(" FROM DMS_RELATIONAL_WORD A ");
            sqlStat.append(" WHERE A.WORD = ? AND A.RECORD_STATUS = ?  ");
            preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            this.setPrepareStatement(preStat, 1, keyWord.trim());
            this.setPrepareStatement(preStat, 2, GlobalConstant.RECORD_STATUS_ACTIVE);
            rs = preStat.executeQuery();
            List getRelationWordList = new ArrayList();
            while (rs.next()) {
                DmsRelationalWord tmpDmsRelationalWord = new DmsRelationalWord();
                tmpDmsRelationalWord.setID(getResultSetInteger(rs, "ID"));
                tmpDmsRelationalWord.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                tmpDmsRelationalWord.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                tmpDmsRelationalWord.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                tmpDmsRelationalWord.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                tmpDmsRelationalWord.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                tmpDmsRelationalWord.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                tmpDmsRelationalWord.setWord(getResultSetString(rs, "WORD"));
                tmpDmsRelationalWord.setParentID(getResultSetInteger(rs, "PARENT_ID"));
                tmpDmsRelationalWord.setType(getResultSetString(rs, "TYPE"));
                tmpDmsRelationalWord.setCreatorName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getCreatorID()));
                tmpDmsRelationalWord.setUpdaterName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getUpdaterID()));
                getRelationWordList.add(tmpDmsRelationalWord);
            }
            return getRelationWordList;
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

    /**
   * Gets list of the DmsRelationalWord objects by parent ID.
   * 
   * @param parentID the parent ID
   * @return list of the DmsRelationalWord objects
   * @throws ApplicationException
   */
    public List getRelationalWordListByParentID(Integer parentID) throws ApplicationException {
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        StringBuffer sqlStatCnt = new StringBuffer();
        try {
            sqlStat.append("SELECT A.ID, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE, A.WORD, A.PARENT_ID, A.TYPE");
            sqlStat.append(" FROM DMS_RELATIONAL_WORD A ");
            sqlStat.append(" WHERE A.PARENT_ID = ? AND A.RECORD_STATUS = ?");
            preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            this.setPrepareStatement(preStat, 1, parentID);
            this.setPrepareStatement(preStat, 2, GlobalConstant.RECORD_STATUS_ACTIVE);
            rs = preStat.executeQuery();
            List relationWordList = new ArrayList();
            while (rs.next()) {
                DmsRelationalWord tmpDmsRelationalWord = new DmsRelationalWord();
                tmpDmsRelationalWord.setID(getResultSetInteger(rs, "ID"));
                tmpDmsRelationalWord.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                tmpDmsRelationalWord.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                tmpDmsRelationalWord.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                tmpDmsRelationalWord.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                tmpDmsRelationalWord.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                tmpDmsRelationalWord.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                tmpDmsRelationalWord.setWord(getResultSetString(rs, "WORD"));
                tmpDmsRelationalWord.setParentID(getResultSetInteger(rs, "PARENT_ID"));
                tmpDmsRelationalWord.setType(getResultSetString(rs, "TYPE"));
                tmpDmsRelationalWord.setCreatorName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getCreatorID()));
                tmpDmsRelationalWord.setUpdaterName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getUpdaterID()));
                relationWordList.add(tmpDmsRelationalWord);
            }
            return relationWordList;
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

    /**
   * Gets the list of the relational word by the keyword provided
   * 
   * @param keyWord the keyword
   * @return list of the relational word
   * @throws ApplicationException
   */
    public List getRelationalWordListByKeyWord(String keyWord) throws ApplicationException {
        List relationWordListByType = new ArrayList();
        List dmsRelationalWordList = this.getByRelationWordList(keyWord);
        List relationList = new ArrayList();
        for (int i = 0; i < dmsRelationalWordList.size(); i++) {
            DmsRelationalWord dmsRelationalWord = (DmsRelationalWord) dmsRelationalWordList.get(i);
            if (!Utility.isEmpty(dmsRelationalWord)) {
                Integer parentID = dmsRelationalWord.getParentID();
                if (dmsRelationalWord == null) {
                } else {
                    if (parentID.intValue() == 0) {
                        Integer ID = dmsRelationalWord.getID();
                        relationList = this.getRelationalWordListByParentID(ID);
                        if (relationList.size() > 0) {
                            for (int n = 0; n < relationList.size(); n++) {
                                DmsRelationalWord relation = (DmsRelationalWord) relationList.get(n);
                                relationWordListByType.add(relation);
                            }
                        }
                    } else {
                        if (DmsRelationalWord.BI_DIRECTION.equals(dmsRelationalWord.getType())) {
                            DmsRelationalWord relationWord = (DmsRelationalWord) this.getByID(parentID);
                            if (this.isContains(relationWordListByType, relationWord)) relationWordListByType.add(relationWord);
                        } else {
                        }
                    }
                }
            }
        }
        return relationWordListByType;
    }

    /**
   * Checks the checkedList contains the specified relational word or not
   * 
   * @param checkedList list of the dmsRelationalWord object
   * @param relationword dmsRelationalWord object for checking contain in the checkedList or not
   * @return true if contains, false if not contains. If the checklist is empty, the value of isSoftDeleteEnabled
   * is returned.
   * 
   */
    public boolean isContains(List checkedList, DmsRelationalWord relationword) {
        for (int i = 0; i < checkedList.size(); i++) {
            DmsRelationalWord dmsRelationalWord = (DmsRelationalWord) checkedList.get(i);
            String ckeckedListWord = dmsRelationalWord.getWord();
            String sWord = relationword.getWord();
            if (ckeckedListWord.equals(sWord)) {
                return false;
            } else {
                return true;
            }
        }
        return isSoftDeleteEnabled;
    }

    /**
   * Gets list of DmsRelationalWord objects by the keyword and parent ID
   * 
   * @param keyWord the keyword
   * @param parentID the parent iD
   * @return list of the DmsRelationalWord objects matched.
   * @throws ApplicationException
   */
    public List getListByRelationalWord(String keyWord, Integer parentID) throws ApplicationException {
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        StringBuffer sqlStatCnt = new StringBuffer();
        List listByRelationalWord = new ArrayList();
        try {
            sqlStat.append("SELECT A.ID, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE, A.WORD, A.PARENT_ID, A.TYPE");
            sqlStat.append(" FROM DMS_RELATIONAL_WORD A ");
            sqlStat.append(" WHERE A.PARENT_ID=? AND A.WORD = ? AND A.RECORD_STATUS = ?  ");
            preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            this.setPrepareStatement(preStat, 1, parentID);
            this.setPrepareStatement(preStat, 2, keyWord.trim());
            this.setPrepareStatement(preStat, 3, GlobalConstant.RECORD_STATUS_ACTIVE);
            rs = preStat.executeQuery();
            while (rs.next()) {
                DmsRelationalWord tmpDmsRelationalWord = new DmsRelationalWord();
                tmpDmsRelationalWord.setID(getResultSetInteger(rs, "ID"));
                tmpDmsRelationalWord.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                tmpDmsRelationalWord.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                tmpDmsRelationalWord.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                tmpDmsRelationalWord.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                tmpDmsRelationalWord.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                tmpDmsRelationalWord.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                tmpDmsRelationalWord.setWord(getResultSetString(rs, "WORD"));
                tmpDmsRelationalWord.setParentID(getResultSetInteger(rs, "PARENT_ID"));
                tmpDmsRelationalWord.setType(getResultSetString(rs, "TYPE"));
                tmpDmsRelationalWord.setCreatorName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getCreatorID()));
                tmpDmsRelationalWord.setUpdaterName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getUpdaterID()));
                listByRelationalWord.add(tmpDmsRelationalWord);
            }
            return listByRelationalWord;
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

    /**
   * Gets the abstract DmsRelationalWord object with Bi-direction type by the keyword provided.
   * 
   * @param keyWord the keyword
   * @return the abstract DmsRelationalWord object
   * @throws ApplicationException
   */
    public AbstractBaseObject getRelationalWordByType(String keyWord) throws ApplicationException {
        List relationalWordListByType = new ArrayList();
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        StringBuffer sqlStatCnt = new StringBuffer();
        try {
            sqlStat.append("SELECT A.ID, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE, A.WORD, A.PARENT_ID, A.TYPE");
            sqlStat.append(" FROM DMS_RELATIONAL_WORD A ");
            sqlStat.append(" WHERE A.WORD = ? AND A.RECORD_STATUS = ?  AND A.TYPE = ?");
            preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            this.setPrepareStatement(preStat, 1, keyWord.trim());
            this.setPrepareStatement(preStat, 2, GlobalConstant.RECORD_STATUS_ACTIVE);
            this.setPrepareStatement(preStat, 3, DmsRelationalWord.BI_DIRECTION);
            rs = preStat.executeQuery();
            if (rs != null && rs.next()) {
                DmsRelationalWord tmpDmsRelationalWord = new DmsRelationalWord();
                tmpDmsRelationalWord.setID(getResultSetInteger(rs, "ID"));
                tmpDmsRelationalWord.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                tmpDmsRelationalWord.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                tmpDmsRelationalWord.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                tmpDmsRelationalWord.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                tmpDmsRelationalWord.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                tmpDmsRelationalWord.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                tmpDmsRelationalWord.setWord(getResultSetString(rs, "WORD"));
                tmpDmsRelationalWord.setParentID(getResultSetInteger(rs, "PARENT_ID"));
                tmpDmsRelationalWord.setType(getResultSetString(rs, "TYPE"));
                tmpDmsRelationalWord.setCreatorName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getCreatorID()));
                tmpDmsRelationalWord.setUpdaterName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getUpdaterID()));
                return (tmpDmsRelationalWord);
            } else {
                return null;
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

    /**
   * Gets the DmsRelationalWord object by ID.
   * 
   * @param id the ID of the DmsRelationalWord object
   * @return the DmsRelationalWord object
   * @throws ApplicationException
   */
    public List getListByID(Integer id) throws ApplicationException {
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        List listByID = new ArrayList();
        try {
            sqlStat.append("SELECT A.ID, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE, A.WORD, A.PARENT_ID, A.TYPE ");
            sqlStat.append("FROM   DMS_RELATIONAL_WORD A ");
            sqlStat.append("WHERE  A.ID = ? AND A.RECORD_STATUS = ? ");
            preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            this.setPrepareStatement(preStat, 1, id);
            this.setPrepareStatement(preStat, 2, GlobalConstant.RECORD_STATUS_ACTIVE);
            rs = preStat.executeQuery();
            while (rs.next()) {
                DmsRelationalWord tmpDmsRelationalWord = new DmsRelationalWord();
                tmpDmsRelationalWord.setID(getResultSetInteger(rs, "ID"));
                tmpDmsRelationalWord.setRecordStatus(getResultSetString(rs, "RECORD_STATUS"));
                tmpDmsRelationalWord.setUpdateCount(getResultSetInteger(rs, "UPDATE_COUNT"));
                tmpDmsRelationalWord.setCreatorID(getResultSetInteger(rs, "CREATOR_ID"));
                tmpDmsRelationalWord.setCreateDate(getResultSetTimestamp(rs, "CREATE_DATE"));
                tmpDmsRelationalWord.setUpdaterID(getResultSetInteger(rs, "UPDATER_ID"));
                tmpDmsRelationalWord.setUpdateDate(getResultSetTimestamp(rs, "UPDATE_DATE"));
                tmpDmsRelationalWord.setWord(getResultSetString(rs, "WORD"));
                tmpDmsRelationalWord.setParentID(getResultSetInteger(rs, "PARENT_ID"));
                tmpDmsRelationalWord.setType(getResultSetString(rs, "TYPE"));
                tmpDmsRelationalWord.setCreatorName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getCreatorID()));
                tmpDmsRelationalWord.setUpdaterName(UserInfoFactory.getUserFullName(tmpDmsRelationalWord.getUpdaterID()));
                listByID.add(tmpDmsRelationalWord);
            }
            return listByID;
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

    /**
   * Gets list of the DmsRelationalWord objects which is relational word by keyword.
   * 
   * @param keyWord the keyword
   * @return list of the relational word DmsRelationalWord objects 
   * @throws ApplicationException
   */
    public List getWordListByKeyWord(String keyWord) throws ApplicationException {
        List relationWordListByType = new ArrayList();
        List dmsRelationalWordList = this.getByRelationWordList(keyWord);
        for (int i = 0; i < dmsRelationalWordList.size(); i++) {
            DmsRelationalWord dmsRelationalWord = (DmsRelationalWord) dmsRelationalWordList.get(i);
            if (!Utility.isEmpty(dmsRelationalWord)) {
                Integer parentID = dmsRelationalWord.getParentID();
                if (dmsRelationalWord == null) {
                } else {
                    if (parentID.intValue() == 0) {
                        Integer ID = dmsRelationalWord.getID();
                        relationWordListByType = this.getRelationalWordListByParentID(ID);
                    } else {
                        relationWordListByType = this.getRelationalWordListByKeyWord(keyWord);
                    }
                }
            }
        }
        return relationWordListByType;
    }
}
