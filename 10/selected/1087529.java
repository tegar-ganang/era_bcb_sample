package tms.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import javax.servlet.http.HttpSession;
import com.google.gwt.core.client.GWT;
import tms.client.entities.AuditableEvent;
import tms.client.entities.Field;
import tms.client.entities.InputModel;
import tms.client.entities.Record;
import tms.client.entities.RecordAttribute;
import tms.client.entities.Term;
import tms.client.entities.Topic;
import tms.client.exceptions.DataOperationException;
import tms.client.exceptions.PersistenceException;
import tms.client.i18n.TMSMessages;
import tms.client.services.RecordUpdateService;
import tms.client.services.result.DataUpdateResult;
import tms.server.accesscontrol.AccessControlledRemoteService;
import tms.server.i18n.MessagesFactory;
import tms.server.logging.LogUtility;
import tms.server.session.RecordIdTracker;
import tms.shared.Filter;

/**
 * 
 * @author Wildrich Fourie
 * @author Ismail Lavangee
 */
public class RecordUpdateServiceImpl extends AccessControlledRemoteService implements RecordUpdateService {

    private static final long serialVersionUID = 3296208017945712583L;

    private static final TMSMessages messages = MessagesFactory.createInstance(TMSMessages.class);

    @Override
    public DataUpdateResult<Record> updateRecord(String authToken, Record record, Filter filter, Field sourceField, InputModel inputmodel) throws DataOperationException {
        validateUserIsSignedOn(authToken);
        DataUpdateResult<Record> recordUpdateResult = new DataUpdateResult<Record>();
        HttpSession session = getSession();
        if (record != null) {
            Connection connection = null;
            boolean updated = false;
            try {
                connection = DatabaseConnector.getConnection();
                connection.setAutoCommit(false);
                recordUpdateResult.setMessage(messages.server_record_update_success(""));
                recordUpdateResult.setSuccessful(true);
                long userId = getSignedOnUser(authToken).getUserId();
                AuditTrailManager.updateAuditTrail(connection, AuditTrailManager.createAuditTrailEvent(record, userId, AuditableEvent.EVENTYPE_UPDATE), authToken, session);
                if (record.isTopicsChanged()) {
                    ArrayList<Topic> currentTopics = TopicRetrievalServiceImpl.getTopics(record.getRecordid(), getSession(), authToken);
                    TopicUpdateServiceImpl.removeRecordTopics(connection, currentTopics, record.getRecordid());
                    TopicUpdateServiceImpl.insertRecordTopics(connection, record.getTopics(), record.getRecordid());
                }
                ArrayList<RecordAttribute> recordAttributes = record.getRecordattributes();
                if (recordAttributes != null && recordAttributes.size() > 0) {
                    Iterator<RecordAttribute> rItr = recordAttributes.iterator();
                    while (rItr.hasNext()) {
                        RecordAttribute r = rItr.next();
                        if (r.getRecordattributeid() > 0) {
                            if (r.getArchivedtimestamp() == null) {
                                String rAtSql = "update tms.recordattributes set chardata = ? " + "where recordattributeid = ?";
                                PreparedStatement updateRecordAttribute = connection.prepareStatement(rAtSql);
                                updateRecordAttribute.setString(1, r.getChardata());
                                updateRecordAttribute.setLong(2, r.getRecordattributeid());
                                updateRecordAttribute.executeUpdate();
                                AuditTrailManager.updateAuditTrail(connection, AuditTrailManager.createAuditTrailEvent(r, userId, AuditableEvent.EVENTYPE_UPDATE), authToken, session);
                            } else {
                                String rAtSql = "update tms.recordattributes set archivedtimestamp = now() where  recordattributeid = ?";
                                PreparedStatement updateRecordAttribute = connection.prepareStatement(rAtSql);
                                updateRecordAttribute.setLong(1, r.getRecordattributeid());
                                updateRecordAttribute.executeUpdate();
                                AuditTrailManager.updateAuditTrail(connection, AuditTrailManager.createAuditTrailEvent(r, userId, AuditableEvent.EVENTYPE_DELETE), authToken, session);
                            }
                        } else {
                            String rAtSql = "insert into tms.recordattributes " + "(inputmodelfieldid, chardata, recordid) " + "values (?, ?, ?) returning recordattributeid";
                            PreparedStatement insertRecordAttribute = connection.prepareStatement(rAtSql);
                            insertRecordAttribute.setLong(1, r.getInputmodelfieldid());
                            insertRecordAttribute.setString(2, r.getChardata());
                            insertRecordAttribute.setLong(3, record.getRecordid());
                            ResultSet result = insertRecordAttribute.executeQuery();
                            if (result.next()) {
                                long recordattributeid = result.getLong("recordattributeid");
                                r.setRecordattributeid(recordattributeid);
                                AuditTrailManager.updateAuditTrail(connection, AuditTrailManager.createAuditTrailEvent(r, userId, AuditableEvent.EVENTYPE_CREATE), authToken, session);
                            }
                        }
                    }
                }
                ArrayList<Term> terms = record.getTerms();
                Iterator<Term> termsItr = terms.iterator();
                while (termsItr.hasNext()) {
                    Term term = termsItr.next();
                    if (term.getTermid() != -1) TermUpdater.updateTerm(connection, term, userId, authToken, getSession()); else {
                        TermAdditionServiceImpl termAdder = new TermAdditionServiceImpl();
                        termAdder.addTerm(connection, term, userId, authToken, session);
                    }
                }
                connection.commit();
                updated = true;
                if (filter != null) RecordIdTracker.refreshRecordIdsInSessionByFilter(session, connection, true, filter, sourceField, authToken); else RecordIdTracker.refreshRecordIdsInSession(session, connection, false, authToken);
                RecordRetrievalServiceImpl retriever = new RecordRetrievalServiceImpl();
                Record updatedRecord = retriever.retrieveRecordByRecordId(initSignedOnUser(authToken), record.getRecordid(), session, false, inputmodel, authToken);
                recordUpdateResult.setResult(updatedRecord);
            } catch (Exception e) {
                if (!updated && connection != null) {
                    try {
                        connection.rollback();
                    } catch (SQLException e1) {
                        LogUtility.log(Level.SEVERE, session, messages.log_db_rollback(""), e1, authToken);
                        e1.printStackTrace();
                    }
                }
                recordUpdateResult.setFailed(true);
                if (updated) {
                    recordUpdateResult.setMessage(messages.server_record_update_retrieve(""));
                    recordUpdateResult.setException(e);
                    LogUtility.log(Level.SEVERE, session, messages.server_record_update_retrieve(""), e, authToken);
                } else {
                    recordUpdateResult.setMessage(messages.server_record_update_fail(""));
                    recordUpdateResult.setException(new PersistenceException(e));
                    LogUtility.log(Level.SEVERE, session, messages.server_record_update_fail(""), e, authToken);
                }
                GWT.log(recordUpdateResult.getMessage(), e);
            } finally {
                try {
                    if (connection != null) {
                        connection.setAutoCommit(true);
                        connection.close();
                    }
                } catch (Exception e) {
                    LogUtility.log(Level.SEVERE, session, messages.log_db_close(""), e, authToken);
                }
            }
        }
        return recordUpdateResult;
    }

    @Override
    public DataUpdateResult<Record> archiveRecord(String authToken, Record record, Filter filter, Field sourceField, InputModel inputmodel) throws DataOperationException {
        validateUserIsSignedOn(authToken);
        validateUserHasAdminRights(authToken);
        DataUpdateResult<Record> recordUpdateResult = new DataUpdateResult<Record>();
        if (record != null) {
            Connection connection = null;
            boolean archived = false;
            try {
                long userId = getSignedOnUser(authToken).getUserId();
                connection = DatabaseConnector.getConnection();
                connection.setAutoCommit(false);
                recordUpdateResult.setMessage(messages.server_record_delete_success(""));
                recordUpdateResult.setSuccessful(true);
                String sql = "update tms.records set archivedtimestamp = now() where recordid = ?";
                PreparedStatement updateRecord = connection.prepareStatement(sql);
                updateRecord.setLong(1, record.getRecordid());
                int recordArchived = 0;
                recordArchived = updateRecord.executeUpdate();
                if (recordArchived > 0) AuditTrailManager.updateAuditTrail(connection, AuditTrailManager.createAuditTrailEvent(record, userId, AuditableEvent.EVENTYPE_DELETE), authToken, getSession());
                TopicUpdateServiceImpl.archiveRecordTopics(connection, record.getTopics(), record.getRecordid());
                ArrayList<RecordAttribute> recordAttributes = record.getRecordattributes();
                if (recordAttributes != null && recordAttributes.size() > 0) {
                    Iterator<RecordAttribute> rItr = recordAttributes.iterator();
                    while (rItr.hasNext()) {
                        RecordAttribute r = rItr.next();
                        String rAtSql = "update tms.recordattributes set archivedtimestamp = now() where recordattributeid = ?";
                        PreparedStatement updateRecordAttribute = connection.prepareStatement(rAtSql);
                        updateRecordAttribute.setLong(1, r.getRecordattributeid());
                        int recordAttribArchived = 0;
                        recordAttribArchived = updateRecordAttribute.executeUpdate();
                        if (recordAttribArchived > 0) AuditTrailManager.updateAuditTrail(connection, AuditTrailManager.createAuditTrailEvent(r, userId, AuditableEvent.EVENTYPE_DELETE), authToken, getSession());
                    }
                }
                ArrayList<Term> terms = record.getTerms();
                Iterator<Term> termsItr = terms.iterator();
                while (termsItr.hasNext()) {
                    Term term = termsItr.next();
                    TermUpdater.archiveTerm(connection, term, userId, authToken, getSession());
                }
                connection.commit();
                archived = true;
                if (filter != null) RecordIdTracker.refreshRecordIdsInSessionByFilter(this.getThreadLocalRequest().getSession(), connection, true, filter, sourceField, authToken); else RecordIdTracker.refreshRecordIdsInSession(this.getThreadLocalRequest().getSession(), connection, false, authToken);
                RecordRetrievalServiceImpl retriever = new RecordRetrievalServiceImpl();
                RecordIdTracker.refreshRecordIdsInSession(this.getThreadLocalRequest().getSession(), connection, false, authToken);
                Record updatedRecord = retriever.retrieveRecordByRecordId(initSignedOnUser(authToken), record.getRecordid(), this.getThreadLocalRequest().getSession(), false, inputmodel, authToken);
                recordUpdateResult.setResult(updatedRecord);
            } catch (Exception e) {
                if (!archived && connection != null) {
                    try {
                        connection.rollback();
                    } catch (SQLException e1) {
                        LogUtility.log(Level.SEVERE, getSession(), messages.log_db_rollback(""), e1, authToken);
                        e1.printStackTrace();
                    }
                }
                recordUpdateResult.setFailed(true);
                if (archived) {
                    recordUpdateResult.setMessage(messages.server_record_delete_retrieve(""));
                    recordUpdateResult.setException(e);
                    LogUtility.log(Level.SEVERE, getSession(), messages.server_record_delete_retrieve(""), e, authToken);
                } else {
                    recordUpdateResult.setMessage(messages.server_record_delete_fail(""));
                    recordUpdateResult.setException(new PersistenceException(e));
                    LogUtility.log(Level.SEVERE, getSession(), messages.server_record_delete_fail(""), e, authToken);
                }
                GWT.log(recordUpdateResult.getMessage(), e);
            } finally {
                try {
                    if (connection != null) {
                        connection.setAutoCommit(true);
                        connection.close();
                    }
                } catch (Exception e) {
                    LogUtility.log(Level.SEVERE, getSession(), messages.log_db_close(""), e, authToken);
                }
            }
        }
        return recordUpdateResult;
    }
}
