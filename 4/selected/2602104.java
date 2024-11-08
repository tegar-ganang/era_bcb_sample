package com.dcivision.framework.web;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.dcivision.alert.AlertOperationConstant;
import com.dcivision.alert.bean.MtmUpdateAlertRecipient;
import com.dcivision.alert.bean.UpdateAlert;
import com.dcivision.alert.bean.UpdateAlertLogAction;
import com.dcivision.alert.bean.UpdateAlertSystemLog;
import com.dcivision.alert.bean.UpdateAlertType;
import com.dcivision.alert.core.AlertManager;
import com.dcivision.alert.dao.MtmUpdateAlertRecipientDAObject;
import com.dcivision.alert.dao.UpdateAlertDAObject;
import com.dcivision.alert.dao.UpdateAlertSystemLogDAObject;
import com.dcivision.alert.dao.UpdateAlertTypeDAObject;
import com.dcivision.audit.core.AuditTrailManager;
import com.dcivision.calendar.bean.CalendarRecord;
import com.dcivision.calendar.dao.CalendarRecordDAObject;
import com.dcivision.calendar.dao.CalendarRecurDAObject;
import com.dcivision.dms.bean.DmsDocument;
import com.dcivision.dms.bean.DmsRoot;
import com.dcivision.dms.dao.DmsRootDAObject;
import com.dcivision.framework.ApplicationException;
import com.dcivision.framework.GlobalConstant;
import com.dcivision.framework.MessageResourcesFactory;
import com.dcivision.framework.PermissionManager;
import com.dcivision.framework.SessionContainer;
import com.dcivision.framework.SystemFunctionConstant;
import com.dcivision.framework.SystemParameterConstant;
import com.dcivision.framework.SystemParameterFactory;
import com.dcivision.framework.TextUtility;
import com.dcivision.framework.Utility;
import com.dcivision.user.UserHomePreferenceConstant;
import com.dcivision.user.bean.PersonalHomePreference;
import com.dcivision.user.core.PreferenceManager;
import com.dcivision.user.dao.PersonalHomePreferenceDAObject;
import com.dcivision.workflow.dao.WorkflowProgressDAObject;

/**
 * <p>Class Name:       ListSysUserDefinedIndexAction.java    </p>
 * <p>Description:      The list action class for Personal Home.jsp</p>
 *
 *    @author           Zoe Shum
 *    @company          DCIVision Limited
 *    @creation date    31/10/2003
 *    @version          $Revision: 1.47.2.7 $
 */
public class ListPersonalHomeAction extends AbstractListAction {

    public static final String REVISION = "$Revision: 1.47.2.7 $";

    public static final String VAR_ALERT_CALENDARTOLIST = GlobalConstant.HOME_CALENDAR_TODO_LIST;

    public static final String VAR_ALERT_CALENDAREVENTLIST = GlobalConstant.HOME_CALENDAR_EVENT_LIST;

    public static final String VAR_ALERT_CALENDARMEETINGLIST = GlobalConstant.HOME_CALENDAR_MEETING_LIST;

    public static final String VAR_ALERT_SYSTEM_LOG = GlobalConstant.HOME_SYSTEM_LOG_LIST;

    public static final String VAR_RECENTLY_ACCESS = GlobalConstant.HOME_RECENTLY_ACCESS_LIST;

    public static final String VAR_WORKFLOW_TASK_LIST = GlobalConstant.HOME_WORKFLOW_TASK_LIST;

    public static final String VAR_WORKFLOW_TRACKING_LIST = GlobalConstant.HOME_WORKFLOW_TRACKING_LIST;

    public static final String PREFERENCE_3_CHANNELLIST_CONFIG_STR = PersonalHomePreference.CALENDAR_TODO_LIST + "|" + PersonalHomePreference.TOOLS + "|" + PersonalHomePreference.SYSTEM_LOGS;

    /**
   *  Constructor - Creates a new instance of ListPersonalHomeAction and define the default listName.
   */
    public ListPersonalHomeAction() {
        super();
        this.setListName("alertSystemLogList");
    }

    /**
   * getMajorDAOClassName
   *
   * @return  The class name of the major DAObject will be used in this action.
   */
    public String getMajorDAOClassName() {
        return (null);
    }

    /**
   * getFunctionCode
   *
   * @return  The corresponding system function code of action.
   */
    public String getFunctionCode() {
        return (null);
    }

    /**
   * setPageTitle
   * set the extend page title and page path.
   * default page path/title will be created by navmode and functionCode
   */
    public void setPageTitle(HttpServletRequest request, HttpServletResponse response, ActionForm form, ActionMapping mapping, ActionForward actionForward) {
        String extendTitle = MessageResourcesFactory.getMessage(this.getSessionContainer(request).getSessionLocale(), "common.label.home");
        request.setAttribute(GlobalConstant.EXTEND_PAGE_TITLE, extendTitle);
        request.setAttribute(GlobalConstant.EXTEND_PAGE_TITLE_SHOW_ONLY, new Boolean(true));
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws ServletException {
        if (!Utility.isEmpty(request.getSession().getAttribute(org.apache.struts.Globals.MESSAGE_KEY))) {
            this.addMessage(request, (String) request.getSession().getAttribute(org.apache.struts.Globals.MESSAGE_KEY));
            request.getSession().removeAttribute(org.apache.struts.Globals.MESSAGE_KEY);
        }
        if (!Utility.isEmpty(request.getSession().getAttribute(org.apache.struts.Globals.ERROR_KEY))) {
            this.addError(request, (String) request.getSession().getAttribute(org.apache.struts.Globals.ERROR_KEY));
            request.getSession().removeAttribute(org.apache.struts.Globals.ERROR_KEY);
        }
        ListPersonalHomeForm listForm = (ListPersonalHomeForm) form;
        String opMode = listForm.getOpMode();
        log.debug("opMode = " + opMode);
        if (!Utility.isEmpty(listForm.getCurFunctionCode())) {
            this.setFunctionCode(listForm.getCurFunctionCode());
        }
        ActionForward forward = this.retrieveFunctionCode(request, response, mapping);
        if (forward != null) {
            return forward;
        }
        if (GlobalConstant.OP_MODE_LIST_DELETE.equals(listForm.getOpMode())) {
            try {
                this.deleteScheduleListData(mapping, listForm, request, response);
            } catch (ApplicationException appEx) {
                this.rollback(request);
                handleApplicationException(request, appEx);
            }
        }
        if (AlertOperationConstant.READ_OPERATION.equals(listForm.getOpMode()) || AlertOperationConstant.UNREAD_OPERATION.equals(listForm.getOpMode())) {
            try {
                this.updateReadOperationLog(mapping, listForm, request, response);
                this.commit(request);
            } catch (ApplicationException appEx) {
                this.rollback(request);
                handleApplicationException(request, appEx);
            }
        }
        this.getCurrentUserDisableViewChannels((AbstractSearchForm) form, request);
        return super.execute(mapping, form, request, response);
    }

    /**
   *  Method getListData() - retrieve the appropriate item list for current page
   *
   *  @param      mapping               ActionMapping from struts
   *  @param      form                  ActionForm from struts
   *  @param      request               HttpServletReuqest
   *  @param      respond               HttpServletRespond
   *  @return     void
   *  @throws     ApplicationException  Throws Exception if cannot get the list data from DAO object
   */
    public synchronized void getListData(ActionMapping mapping, AbstractSearchForm form, HttpServletRequest request, HttpServletResponse response) throws ApplicationException {
        ListPersonalHomeForm listHomeForm = (ListPersonalHomeForm) form;
        listHomeForm.setbAccessCalendar(this.getSessionContainer(request).hasAccessRight(SystemFunctionConstant.SCHEDULE_CALENDAR, "R"));
        listHomeForm.setAccessRecentlyAccessList(this.getSessionContainer(request).hasAccessRight(SystemFunctionConstant.DMS_PUBLIC_FOLDER, "R"));
        listHomeForm.setAccessWorkflowTaskList(this.getSessionContainer(request).hasAccessRight(SystemFunctionConstant.WORKFLOW_TASK, "R"));
        listHomeForm.setAccessWorkflowTrackingList(this.getSessionContainer(request).hasAccessRight(SystemFunctionConstant.WORKFLOW_TRACK, "R"));
        listHomeForm.setAccessSystemLogs(this.getSessionContainer(request).hasAccessRight(SystemFunctionConstant.MESSAGE_INBOX, "R"));
        request.setAttribute("alertSystemLogList", new ArrayList(0));
    }

    protected synchronized List getSystemLogList(HttpServletRequest request, ListPersonalHomeForm listHomeForm) throws ApplicationException {
        Connection conn = this.getConnection(request);
        SessionContainer sessionContainer = this.getSessionContainer(request);
        List result = new ArrayList();
        if (listHomeForm.getAccessSystemLogs()) {
            AlertManager alertManager = new AlertManager(sessionContainer, conn);
            log.debug("Getting System Log...");
            result = alertManager.getSystemLogListByUserRecordID(sessionContainer.getUserRecordID(), listHomeForm);
        }
        return result;
    }

    protected synchronized List getCalendarToList(HttpServletRequest request, ListPersonalHomeForm listHomeForm) throws ApplicationException {
        Connection conn = this.getConnection(request);
        SessionContainer sessionContainer = this.getSessionContainer(request);
        List result = new ArrayList();
        if (listHomeForm.getbAccessCalendar()) {
            AlertManager alertManager = new AlertManager(sessionContainer, conn);
            log.debug("Getting Calendar ToDo list...");
            result = alertManager.getCalendarToListByUserRecordID(sessionContainer.getUserRecordID(), listHomeForm);
        }
        return result;
    }

    protected synchronized List getCalendarEventList(HttpServletRequest request, ListPersonalHomeForm listHomeForm) throws ApplicationException {
        Connection conn = this.getConnection(request);
        SessionContainer sessionContainer = this.getSessionContainer(request);
        List result = new ArrayList();
        if (listHomeForm.getbAccessCalendar()) {
            AlertManager alertManager = new AlertManager(sessionContainer, conn);
            log.debug("Getting Calendar Event list...");
            result = alertManager.getCalendarEventListByUserRecordID(sessionContainer.getUserRecordID(), listHomeForm);
        }
        return result;
    }

    protected synchronized List getCalendarMeetingList(HttpServletRequest request, ListPersonalHomeForm listHomeForm) throws ApplicationException {
        Connection conn = this.getConnection(request);
        SessionContainer sessionContainer = this.getSessionContainer(request);
        CalendarRecordDAObject calendarRecordDAO = new CalendarRecordDAObject(sessionContainer, conn);
        List result = new ArrayList();
        List tempMeetingList = new ArrayList();
        CalendarRecord calendarRecord;
        List systemLogList = new ArrayList();
        ArrayList invitaCalendarList = new ArrayList();
        try {
            UpdateAlertSystemLogDAObject alertSystemDAO = new UpdateAlertSystemLogDAObject(sessionContainer, conn);
            AlertManager alertManager = new AlertManager(sessionContainer, conn);
            List allRelatedUpdateAlertID = alertManager.getInvtatRelatedUpdateAlertIDListByUserID(sessionContainer.getUserRecordID());
            if (!Utility.isEmpty(allRelatedUpdateAlertID)) {
                systemLogList = alertSystemDAO.getReplySystemLogList(allRelatedUpdateAlertID, "Y");
                alertSystemDAO = null;
            }
        } catch (Exception e) {
            log.error(e, e);
        }
        String[] acceptCalendarIDs = new String[systemLogList.size()];
        if (systemLogList.size() > 0) {
            for (int i = 0; i < systemLogList.size(); i++) {
                if (!Utility.isEmpty(systemLogList.get(i))) {
                    UpdateAlertSystemLog updateAlert = (UpdateAlertSystemLog) systemLogList.get(i);
                    acceptCalendarIDs[i] = updateAlert.getObjectID().toString();
                }
            }
        }
        result = calendarRecordDAO.getOwnerAndAcceptedListByDatePeriodAndEventType(sessionContainer.getUserRecordID(), new Timestamp(new Date().getTime()), null, new String[] { CalendarRecord.EVENT_TYPE_CALENDAR_MEETING, CalendarRecord.EVENT_TYPE_CALENDAR_EVENT }, acceptCalendarIDs);
        return result;
    }

    protected synchronized List getRecentlyAccessList(HttpServletRequest request, ListPersonalHomeForm listHomeForm) throws ApplicationException {
        Connection conn = this.getConnection(request);
        SessionContainer sessionContainer = this.getSessionContainer(request);
        List result = new ArrayList();
        if (listHomeForm.getAccessRecentlyAccessList()) {
            if (!SystemParameterFactory.getSystemParameterBoolean(SystemParameterConstant.DMS_DISPLALY_NO_ACCESS_DOCUUMENT)) {
                int pageSize = TextUtility.parseInteger(listHomeForm.getPageOffset());
                AuditTrailManager auditTrailManager = new AuditTrailManager(sessionContainer, conn);
                List tmpResult = null;
                DmsRootDAObject dmsRootDAO = new DmsRootDAObject(sessionContainer, this.getConnection(request));
                PermissionManager pm = sessionContainer.getPermissionManager();
                while (result.size() < pageSize) {
                    tmpResult = auditTrailManager.getDmsAccessList(GlobalConstant.HOME_RECENTLY_ACCESS_LIST, listHomeForm);
                    if (tmpResult == null || tmpResult.size() < 1) {
                        break;
                    }
                    for (int i = 0; tmpResult != null && tmpResult.size() > i; i++) {
                        if (result.size() >= pageSize) {
                            break;
                        }
                        DmsDocument dmsDoc = (DmsDocument) tmpResult.get(i);
                        DmsRoot dmsRoot = (DmsRoot) dmsRootDAO.getObjectByID(dmsDoc.getRootID());
                        if (pm.hasAccessRight(conn, GlobalConstant.OBJECT_TYPE_DOCUMENT, dmsDoc.getID(), "R") || dmsRoot.getOwnerID().equals(this.getSessionContainer(request).getUserRecordID())) {
                            result.add(dmsDoc);
                        }
                    }
                    listHomeForm.setCurStartRowNo(TextUtility.parseInteger(listHomeForm.getCurStartRowNo()) + pageSize + "");
                }
            } else {
                AuditTrailManager auditTrailManager = new AuditTrailManager(sessionContainer, conn);
                log.debug("Getting  Recently Access List ...");
                result = auditTrailManager.getDmsAccessList(GlobalConstant.HOME_RECENTLY_ACCESS_LIST, listHomeForm);
            }
        }
        return result;
    }

    protected synchronized List getWorkflowTaskList(HttpServletRequest request, ListPersonalHomeForm listHomeForm) throws ApplicationException {
        Connection conn = this.getConnection(request);
        SessionContainer sessionContainer = this.getSessionContainer(request);
        List result = new ArrayList();
        if (listHomeForm.getAccessWorkflowTaskList()) {
            WorkflowProgressDAObject workflowProgressDAO = new WorkflowProgressDAObject(sessionContainer, conn);
            log.debug("Getting  Workflow Task List ...");
            result = workflowProgressDAO.getPendingList4UserCanRead(listHomeForm);
        }
        return result;
    }

    protected synchronized List getWorkflowTrackingList(HttpServletRequest request, ListPersonalHomeForm listHomeForm) throws ApplicationException {
        Connection conn = this.getConnection(request);
        SessionContainer sessionContainer = this.getSessionContainer(request);
        List result = new ArrayList();
        if (listHomeForm.getAccessWorkflowTrackingList()) {
            WorkflowProgressDAObject workflowProgressDAO = new WorkflowProgressDAObject(sessionContainer, conn);
            log.debug("Getting  Workflow Tracking List ...");
            result = workflowProgressDAO.getListTrackingByUserRecordID(listHomeForm);
        }
        return result;
    }

    protected synchronized void updateReadOperationLog(ActionMapping mapping, AbstractSearchForm form, HttpServletRequest request, HttpServletResponse response) throws ApplicationException {
        Connection conn = this.getConnection(request);
        SessionContainer sessionContainer = this.getSessionContainer(request);
        ListPersonalHomeForm listForm = (ListPersonalHomeForm) form;
        Integer systemLogID = listForm.getActionSystemLogID();
        if (systemLogID != null) {
            AlertManager alertManager = new AlertManager(sessionContainer, conn);
            log.debug("OpMode:" + listForm.getOpMode());
            if (AlertOperationConstant.UNREAD_OPERATION.equals(listForm.getOpMode()) && alertManager.checkForSystemLogActionByActionType(systemLogID, UpdateAlertLogAction.READ_ACTION)) {
                alertManager.deleteLogActionBySystemLogIDCurActor(systemLogID);
            } else if (AlertOperationConstant.READ_OPERATION.equals(listForm.getOpMode())) {
                UpdateAlertSystemLog systemLog = new UpdateAlertSystemLog();
                systemLog.setID(systemLogID);
                alertManager.createSystemLogAction(systemLog, UpdateAlertLogAction.READ_ACTION, null, null);
            }
        }
        listForm.setOpMode(null);
        listForm.setActionSystemLogID(null);
        try {
        } catch (Exception e) {
            log.error(e, e);
        } finally {
            conn = null;
        }
    }

    /**
   * Get current user's disable view channels.
   * @param form
   * @param request
   * @throws ApplicationException
   */
    protected synchronized void getCurrentUserDisableViewChannels(AbstractSearchForm form, HttpServletRequest request) throws ApplicationException {
        PersonalHomePreferenceDAObject personalPreferenceDAO = new PersonalHomePreferenceDAObject(this.getSessionContainer(request), this.getConnection(request));
        PersonalHomePreference personalPreference = (PersonalHomePreference) personalPreferenceDAO.getByObjectByUserRecordID(this.getSessionContainer(request).getUserRecordID());
        PreferenceManager preferenceMg = new PreferenceManager(this.getSessionContainer(request), this.getConnection(request));
        ListPersonalHomeForm maintForm = (ListPersonalHomeForm) form;
        if (!Utility.isEmpty(personalPreference)) {
            if (!Utility.isEmpty(personalPreference.getDisableViewChannel())) {
                String channeldisableViewAndViewStr = preferenceMg.fitTheOldChannelConfig(personalPreference.getDisableViewChannel());
                String[] preferences = preferenceMg.getChannelPreference(channeldisableViewAndViewStr);
                maintForm.setDisableViewChannels(preferences[0]);
                maintForm.setAbleViewChannels(preferences[1]);
            } else {
                String[] preferences = preferenceMg.getChannelPreference(UserHomePreferenceConstant.SYSTEMPREFERENCESTR);
                maintForm.setDisableViewChannels(preferences[0]);
                maintForm.setAbleViewChannels(preferences[1]);
            }
        } else {
            String[] preferences = preferenceMg.getChannelPreference(UserHomePreferenceConstant.SYSTEMPREFERENCESTR);
            maintForm.setDisableViewChannels(preferences[0]);
            maintForm.setAbleViewChannels(preferences[1]);
        }
        List channelList = preferenceMg.getChannelSequence(maintForm.getAbleViewChannels());
        request.setAttribute("channelList", channelList);
    }

    /**
   * function deleteListData is to delete data in list.
   * @param mapping                        mapping for struts
   * @param form                           ActionForm for struts
   * @param request                        HttpServletRequest
   * @param response                       HttpServletResponse
   * @throws ApplicationException          Throws Exception if cannot get the list data from DAO object
   */
    public void deleteScheduleListData(ActionMapping mapping, AbstractSearchForm form, HttpServletRequest request, HttpServletResponse response) throws ApplicationException {
        Connection conn = null;
        try {
            String[] arg = form.getBasicSelectedID();
            String[] itemToDel = null;
            if (arg != null) {
                conn = this.getConnection(request);
                CalendarRecordDAObject calendarDAO = new CalendarRecordDAObject(this.getSessionContainer(request), this.getConnection(request));
                for (int i = 0; i < arg.length; i++) {
                    itemToDel = TextUtility.splitString(arg[i], "|");
                    if (itemToDel != null) {
                        CalendarRecord calendarRecord = (CalendarRecord) calendarDAO.getObjectByID(new Integer(itemToDel[0]));
                        if (CalendarRecord.EVENT_TYPE_CALENDAR_TODO.equals(itemToDel[1]) || CalendarRecord.EVENT_TYPE_CALENDAR_EVENT.equals(itemToDel[1]) || CalendarRecord.EVENT_TYPE_CALENDAR_MEETING.equals(itemToDel[1])) {
                            calendarDAO.SoftDelete(new Integer(itemToDel[0]));
                        }
                        if (CalendarRecord.EVENT_TYPE_CALENDAR_MEETING.equals(itemToDel[1])) {
                            AlertManager alertManager = new AlertManager(this.getSessionContainer(request), conn);
                            calendarRecord = selectInviteMeeting(request, this.getSessionContainer(request), conn, calendarRecord);
                            UpdateAlertDAObject updateAlertDAO = new UpdateAlertDAObject(this.getSessionContainer(request), conn);
                            UpdateAlert updateAlert = (UpdateAlert) updateAlertDAO.getByObjectTypeObjectID(UpdateAlert.CALENDAR_TYPE, new Integer(itemToDel[0]));
                            if (calendarRecord.getNotifyWay() != null) {
                                updateUpdateAlert(request, this.getSessionContainer(request), conn, calendarRecord, alertManager, "D", true);
                            }
                            if (calendarRecord.getReminderType() != null && (calendarRecord.getReminderType().indexOf(UpdateAlert.EMAIL_NOTIFICATION) >= 0)) {
                                AlertManager am = new AlertManager(this.getSessionContainer(request), conn);
                                UpdateAlert delUpdateAlert = new UpdateAlert();
                                delUpdateAlert.setObjectType(UpdateAlert.CALENDAR_TYPE);
                                delUpdateAlert.setObjectID(calendarRecord.getID());
                                delUpdateAlert.setCreatorName(calendarRecord.getCreatorName());
                                List userRecordIDList = new ArrayList(1);
                                userRecordIDList.add(this.getSessionContainer(request).getUserRecordID());
                                java.util.Date date = new java.util.Date(calendarRecord.getDatetime().getTime());
                                try {
                                    am.delScheduleJobNotification(updateAlert);
                                } catch (Exception e) {
                                }
                            }
                        }
                        if (calendarRecord.getIsRecurrence() != null) {
                            CalendarRecurDAObject calendarRecurDAO = new CalendarRecurDAObject(this.getSessionContainer(request), conn);
                            calendarRecurDAO.deleteByCalendarID(calendarRecord.getID());
                        }
                    }
                }
                this.commit(request);
            }
            conn = null;
            form.setOpMode(null);
            form.setBasicSelectedID(null);
        } catch (ApplicationException appEx) {
        } catch (Exception e) {
        }
    }

    /**
   * update or delete a meeting if the meeting have invitation ,update the updateAlertlog
   *
   *
   *
   * @param request
   * @param sessionContainer
   * @param request
   * @param connection
   * @param calendarRecord
   * @param am
   * @param type : "U"update,"D"delete
   * @throws ApplicationException
   */
    private CalendarRecord updateUpdateAlert(HttpServletRequest request, SessionContainer sessionContainer, Connection connection, CalendarRecord calendarRecord, AlertManager am, String type, boolean isModify) throws ApplicationException {
        String[] notifyWayString = (calendarRecord.getNotifyWay().split(","));
        UpdateAlertDAObject updateAlertDAO = new UpdateAlertDAObject(sessionContainer, connection);
        if (notifyWayString.length > 0) {
            UpdateAlert updateAlert = new UpdateAlert();
            if (calendarRecord.getUpdateAlertID() != null) {
                updateAlert = (UpdateAlert) updateAlertDAO.getObjectByID(calendarRecord.getUpdateAlertID());
            } else {
                if (notifyWayString.length > 0) {
                    updateAlert.setObjectType(UpdateAlert.CALENDAR_TYPE);
                    updateAlert.setObjectID(calendarRecord.getID());
                    StringBuffer str = new StringBuffer();
                    for (int i = 0; i < notifyWayString.length; i++) {
                        str.append(notifyWayString[i]).append(",");
                    }
                    calendarRecord.setNotifyWay(str.toString());
                }
            }
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < notifyWayString.length; i++) {
                str.append(notifyWayString[i]).append(",");
            }
            calendarRecord.setNotifyWay(str.toString());
            List recipientAry = new ArrayList();
            MtmUpdateAlertRecipientDAObject updateRecipientDAO = new MtmUpdateAlertRecipientDAObject(sessionContainer, connection);
            String recipientType = null;
            String[] tmprecipientAry = new String[0];
            if (!Utility.isEmpty(calendarRecord.getUpdateAlertID())) {
                recipientAry = updateRecipientDAO.getByUpdateAlertID(calendarRecord.getUpdateAlertID());
            }
            tmprecipientAry = new String[recipientAry.size()];
            String[] userRecords = new String[recipientAry.size()];
            String[] userGroups = new String[recipientAry.size()];
            String[] userRoles = new String[recipientAry.size()];
            for (int i = 0; i < recipientAry.size(); i++) {
                MtmUpdateAlertRecipient tmprecipient = (MtmUpdateAlertRecipient) recipientAry.get(i);
                if (tmprecipient.getRecipientType().equals(UpdateAlert.USER_RECIPIENT)) {
                    userRecords[i] = tmprecipient.getRecipientID().toString();
                } else if (tmprecipient.getRecipientType().equals(UpdateAlert.GROUP_RECIPIENT)) {
                    userGroups[i] = tmprecipient.getRecipientID().toString();
                } else if (tmprecipient.getRecipientType().equals(UpdateAlert.ROLE_RECIPIENT)) {
                    userRoles[i] = tmprecipient.getRecipientID().toString();
                }
            }
            if (userRecords[0] == null) {
                userRecords = null;
            }
            if (userGroups[0] == null) {
                userGroups = null;
            }
            if (userRoles[0] == null) {
                userRoles = null;
            }
            List notifyUserIDList = am.getDistinctUserIDListByLists(userRecords, userGroups, userRoles);
            MtmUpdateAlertRecipient[] tmpRecipients = am.getRecipient(userRecords, userGroups, userRoles);
            String[] actionTypes = new String[1];
            actionTypes[0] = "I";
            UpdateAlertType[] updateTypes = new UpdateAlertType[actionTypes.length];
            for (int i = 0; i < actionTypes.length; i++) {
                UpdateAlertType alertUpdateType = new UpdateAlertType();
                alertUpdateType.setUpdateAlertID(updateAlert.getID());
                alertUpdateType.setNotifyWay(str.toString());
                alertUpdateType.setActionType(actionTypes[i]);
                alertUpdateType.setNeedReply("Y");
                updateTypes[i] = alertUpdateType;
            }
            if ("U".equals(type)) {
                if (calendarRecord.getUpdateAlertID() != null) {
                    am.deleteUpdateAlert(updateAlert);
                    am.createUpdateAlert(updateAlert, updateTypes, tmpRecipients, null, null, calendarRecord.getDatetime(), calendarRecord.getTitle(), calendarRecord.getCreatorID(), notifyUserIDList);
                    if (isModify) {
                        updateAlert.setObjectType(UpdateAlert.CALENDAR_MODIFY_TYPE);
                        am.createUpdateAlert(updateAlert, updateTypes, null, tmpRecipients);
                    }
                } else {
                    am.createUpdateAlert(updateAlert, updateTypes, tmpRecipients, null, null, calendarRecord.getDatetime(), calendarRecord.getTitle(), calendarRecord.getCreatorID(), notifyUserIDList);
                }
            } else {
                updateAlert.setObjectType(UpdateAlert.CALENDAR_INACTIVE_TYPE);
                am.createUpdateAlert(updateAlert, updateTypes, null, tmpRecipients);
            }
        } else {
            UpdateAlert updateAlert = new UpdateAlert();
            if (calendarRecord.getUpdateAlertID() != null) {
                updateAlert = (UpdateAlert) updateAlertDAO.getObjectByID(calendarRecord.getUpdateAlertID());
                am.deleteUpdateAlert(updateAlert);
            }
        }
        return calendarRecord;
    }

    private CalendarRecord selectInviteMeeting(HttpServletRequest request, SessionContainer sessionContainer, Connection connection, CalendarRecord calendarRecord) throws ApplicationException {
        AlertManager alertManager = new AlertManager(sessionContainer, connection);
        UpdateAlert updateAlert = null;
        List alertList = new ArrayList();
        alertList = alertManager.listUpdateAlertByObjectTypeObjectID(UpdateAlert.CALENDAR_TYPE, calendarRecord.getID());
        if (alertList != null) {
            if (alertList.size() > 0) {
                updateAlert = (UpdateAlert) alertList.get(0);
                calendarRecord.setUpdateAlertID(updateAlert.getID());
                List actionTypes = new ArrayList();
                UpdateAlertTypeDAObject updateAlertTypeDAO = new UpdateAlertTypeDAObject(sessionContainer, connection);
                actionTypes = updateAlertTypeDAO.getByUpdateAlertIDByOwnerID(updateAlert.getID(), UpdateAlert.INVITE_TYPE, updateAlert.getCreatorID());
                if (actionTypes != null) {
                    String[] actionString = new String[actionTypes.size()];
                    for (int i = 0; i < actionTypes.size(); i++) {
                        UpdateAlertType alertUpdateType = (UpdateAlertType) actionTypes.get(i);
                        calendarRecord.setNeedReply(alertUpdateType.getNeedReply());
                        calendarRecord.setNotifyWay(alertUpdateType.getNotifyWay());
                        UpdateAlertSystemLogDAObject updateSystemLogDAO = new UpdateAlertSystemLogDAObject(sessionContainer, connection);
                        UpdateAlertSystemLog updateSystemLog = new UpdateAlertSystemLog();
                        updateSystemLog.setUpdateAlertTypeID(alertUpdateType.getID());
                        updateSystemLog.setActionUserID(sessionContainer.getUserRecordID());
                        UpdateAlertSystemLog newUpdateSystemLog = (UpdateAlertSystemLog) updateSystemLogDAO.getByUpdateAlertTypeIDActionUserID(updateSystemLog.getUpdateAlertTypeID(), updateSystemLog.getActionUserID());
                        if (newUpdateSystemLog != null) {
                            request.getSession().setAttribute("replyType", updateSystemLog.getActionReply());
                        } else {
                            request.getSession().setAttribute("replyType", "");
                        }
                    }
                }
            }
        }
        return calendarRecord;
    }
}
