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
  @version         $Revision: 1.5 $
  */
public class DmsSearchRelationalWordDAObject extends AbstractDAObject {

    public static final String REVISION = "$Revision: 1.5 $";

    public static final String TABLE_NAME = "DMS_RELATIONAL_WORD";

    public DmsSearchRelationalWordDAObject(SessionContainer sessionContainer, Connection dbConn) {
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
                sqlStat.append("WHERE  A.RECORD_STATUS = ? AND A.PARENT_ID=? ");
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
                this.setPrepareStatement(preStatCnt, 2, "0");
                if (searchForm.isSearchable()) {
                    String searchKeyword = this.getFormattedKeyword(searchForm.getBasicSearchKeyword(), searchForm.getBasicSearchType());
                    this.setPrepareStatement(preStatCnt, 3, searchKeyword);
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
                this.setPrepareStatement(preStat, 2, "0");
                if (searchForm.isSearchable()) {
                    String searchKeyword = this.getFormattedKeyword(searchForm.getBasicSearchKeyword(), searchForm.getBasicSearchType());
                    this.setPrepareStatement(preStat, 3, searchKeyword);
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
   * Gets the list of dmsRelationalWord objects which is keyword.
   * 
   * @return list of dmsRelationalWord objects
   * @throws ApplicationException
   */
    public List getKeyWordList() throws ApplicationException {
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        StringBuffer sqlStatCnt = new StringBuffer();
        List listByRelationalWord = new ArrayList();
        try {
            sqlStat.append("SELECT A.ID, A.RECORD_STATUS, A.UPDATE_COUNT, A.CREATOR_ID, A.CREATE_DATE, A.UPDATER_ID, A.UPDATE_DATE, A.WORD, A.PARENT_ID, A.TYPE");
            sqlStat.append(" FROM DMS_RELATIONAL_WORD A ");
            sqlStat.append(" WHERE A.PARENT_ID=?  AND A.RECORD_STATUS = ?  ");
            preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            this.setPrepareStatement(preStat, 1, "0");
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
   * Gets the number of the relational word corresponding to the keyword by keyword ID provided.
   * 
   * @param ID the dmsRelationWord object ID
   * @return the no. of the relational word
   * @throws ApplicationException
   */
    public Integer getNumberOfRelationWord(Integer ID) throws ApplicationException {
        PreparedStatement preStat = null;
        ResultSet rs = null;
        StringBuffer sqlStat = new StringBuffer();
        StringBuffer sqlStatCnt = new StringBuffer();
        Integer result = null;
        try {
            sqlStat.append("SELECT COUNT(*)");
            sqlStat.append(" FROM DMS_RELATIONAL_WORD A ");
            sqlStat.append(" WHERE A.PARENT_ID=?  AND A.RECORD_STATUS = ?  ");
            preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            this.setPrepareStatement(preStat, 1, ID);
            this.setPrepareStatement(preStat, 2, GlobalConstant.RECORD_STATUS_ACTIVE);
            rs = preStat.executeQuery();
            if (rs.next()) {
                result = this.getResultSetInteger(rs, 1);
            }
            if (result == null) {
                result = new Integer(0);
            }
            return result;
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
   * Delete all the corresponding relational word by parent ID.
   * 
   * @param parentID the parent ID
   * @return the empty DmsRelationalWord object if delete success
   * @throws ApplicationException
   */
    public void deleteByKeyWord(Integer parentID) throws ApplicationException {
        PreparedStatement preStat = null;
        StringBuffer sqlStat = new StringBuffer();
        DmsRelationalWord tmpDmsRelationalWord = new DmsRelationalWord();
        synchronized (dbConn) {
            try {
                int updateCnt = 0;
                sqlStat.append("DELETE ");
                sqlStat.append("FROM   DMS_RELATIONAL_WORD ");
                sqlStat.append("WHERE  PARENT_ID=? ");
                preStat = dbConn.prepareStatement(sqlStat.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                setPrepareStatement(preStat, 1, parentID);
                updateCnt = preStat.executeUpdate();
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
}
