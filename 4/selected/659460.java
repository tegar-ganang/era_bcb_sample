package com.dcivision.framework.taglib.channel;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import com.dcivision.alert.bean.UpdateAlertSystemLog;
import com.dcivision.alert.core.AlertManager;
import com.dcivision.alert.dao.UpdateAlertSystemLogDAObject;
import com.dcivision.audit.core.AuditTrailManager;
import com.dcivision.calendar.bean.CalendarRecord;
import com.dcivision.calendar.dao.CalendarRecordDAObject;
import com.dcivision.dms.bean.DmsDocument;
import com.dcivision.dms.bean.DmsRoot;
import com.dcivision.dms.dao.DmsRootDAObject;
import com.dcivision.framework.GlobalConstant;
import com.dcivision.framework.PermissionManager;
import com.dcivision.framework.SessionContainer;
import com.dcivision.framework.SystemParameterConstant;
import com.dcivision.framework.SystemParameterFactory;
import com.dcivision.framework.TextUtility;
import com.dcivision.framework.Utility;
import com.dcivision.framework.web.AbstractSearchForm;
import com.dcivision.framework.web.ListPersonalHomeForm;
import com.dcivision.workflow.dao.WorkflowProgressDAObject;
import com.dcivision.workflow.web.ListWorkflowProgressForm;

/**
 * get the channel List
 * @author Administrator
 *
 */
public class AjaxChannelListProcessor {

    private static Log log = new AjaxLog();

    private static AjaxChannelListProcessor listProcessor = new AjaxChannelListProcessor();

    private AjaxChannelListProcessor() {
    }

    public static AjaxChannelListProcessor getAjaxListProcessor() {
        return listProcessor;
    }

    public List getChannelList(AjaxThreadContext threadContext, AbstractSearchForm form) throws Exception {
        String filterName = threadContext.getRequestAttribute(AjaxConstant.FILTERNAME).toString();
        if (AjaxConstant.MEETINGFILTER.equalsIgnoreCase(filterName)) {
            return getMeetingChannelList(threadContext, form);
        }
        if (AjaxConstant.MESSAGEINBOXFILTER.equalsIgnoreCase(filterName)) {
            return getMessageChannelList(threadContext, form);
        }
        if (AjaxConstant.PROGRESSMONITORFILTER.equalsIgnoreCase(filterName)) {
            return getProgressMonitorChannelList(threadContext, form);
        }
        if (AjaxConstant.RECENTDOCFILTER.equalsIgnoreCase(filterName)) {
            return getRecentDocChannelList(threadContext, form);
        }
        if (AjaxConstant.TASKINBOXFILTER.equalsIgnoreCase(filterName)) {
            return getTaskInboxChannelList(threadContext, form);
        }
        return Collections.EMPTY_LIST;
    }

    private List getMeetingChannelList(AjaxThreadContext context, AbstractSearchForm form) throws Exception {
        SessionContainer sessionContainer = context.getSessionContainer();
        Connection conn = context.getConnnection();
        String filterItem = (String) context.getRequestAttribute(AjaxConstant.FILTERITEM);
        CalendarRecordDAObject calendarRecordDAO = new CalendarRecordDAObject(sessionContainer, conn);
        calendarRecordDAO.setLog(log);
        List result = null;
        List systemLogList = new ArrayList();
        String[] eventType = null;
        if (Utility.isEmpty(filterItem) || "-".equals(filterItem)) eventType = new String[] { CalendarRecord.EVENT_TYPE_CALENDAR_MEETING, CalendarRecord.EVENT_TYPE_CALENDAR_EVENT };
        if (CalendarRecord.EVENT_TYPE_CALENDAR_EVENT.equals(filterItem)) eventType = new String[] { CalendarRecord.EVENT_TYPE_CALENDAR_EVENT };
        if (CalendarRecord.EVENT_TYPE_CALENDAR_MEETING.equals(filterItem)) eventType = new String[] { CalendarRecord.EVENT_TYPE_CALENDAR_MEETING };
        try {
            UpdateAlertSystemLogDAObject alertSystemDAO = new UpdateAlertSystemLogDAObject(sessionContainer, conn);
            alertSystemDAO.setLog(log);
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
        result = calendarRecordDAO.getOwnerAndAcceptedListByDatePeriodAndEventType(form, sessionContainer.getUserRecordID(), new Timestamp(new Date().getTime()), null, eventType, acceptCalendarIDs);
        return result;
    }

    private List getMessageChannelList(AjaxThreadContext context, AbstractSearchForm form) throws Exception {
        SessionContainer sessionContainer = context.getSessionContainer();
        Connection conn = context.getConnnection();
        String filterItem = (String) context.getRequestAttribute(AjaxConstant.FILTERITEM);
        List result = null;
        AlertManager alertManager = new AlertManager(sessionContainer, conn);
        if (!Utility.isEmpty(filterItem) && !"-".equals(filterItem)) ((ListPersonalHomeForm) form).setSpecifyActionType(FilterUtility.getSystemMsgObjectTypesAndActionTypes(filterItem)); else ((ListPersonalHomeForm) form).setSpecifyActionType(null);
        result = alertManager.getSystemLogListByUserRecordID(sessionContainer.getUserRecordID(), form);
        if (Utility.isEmpty(result)) {
            int startOffset = TextUtility.parseInteger(form.getCurStartRowNo());
            int newStartOffset = startOffset - TextUtility.parseInteger(form.getPageOffset());
            if (newStartOffset > 0) {
                form.setCurStartRowNo("" + newStartOffset);
                result = alertManager.getSystemLogListByUserRecordID(sessionContainer.getUserRecordID(), form);
            }
        }
        return result;
    }

    private List getProgressMonitorChannelList(AjaxThreadContext context, AbstractSearchForm form) throws Exception {
        SessionContainer sessionContainer = context.getSessionContainer();
        Connection conn = context.getConnnection();
        String filterItem = (String) context.getRequestAttribute(AjaxConstant.FILTERITEM);
        List result = null;
        WorkflowProgressDAObject workflowProgressDAO = new WorkflowProgressDAObject(sessionContainer, conn);
        workflowProgressDAO.setLog(log);
        result = workflowProgressDAO.getListTrackingByUserRecordID(form);
        return result;
    }

    private List getRecentDocChannelList(AjaxThreadContext context, AbstractSearchForm form) throws Exception {
        SessionContainer sessionContainer = context.getSessionContainer();
        Connection conn = context.getConnnection();
        String filterItem = (String) context.getRequestAttribute(AjaxConstant.FILTERITEM);
        List result = new ArrayList();
        if (!SystemParameterFactory.getSystemParameterBoolean(SystemParameterConstant.DMS_DISPLALY_NO_ACCESS_DOCUUMENT)) {
            int pageSize = TextUtility.parseInteger(form.getPageOffset());
            AuditTrailManager auditTrailManager = new AuditTrailManager(sessionContainer, conn);
            List tmpResult = null;
            DmsRootDAObject dmsRootDAO = new DmsRootDAObject(sessionContainer, conn);
            dmsRootDAO.setLog(log);
            PermissionManager pm = sessionContainer.getPermissionManager();
            while (result.size() < pageSize) {
                tmpResult = auditTrailManager.getDmsAccessList(GlobalConstant.HOME_RECENTLY_ACCESS_LIST, form);
                if (tmpResult == null || tmpResult.size() < 1) break;
                for (int i = 0; tmpResult != null && tmpResult.size() > i; i++) {
                    if (result.size() >= pageSize) break;
                    DmsDocument dmsDoc = (DmsDocument) tmpResult.get(i);
                    DmsRoot dmsRoot = (DmsRoot) dmsRootDAO.getObjectByID(dmsDoc.getRootID());
                    if (pm.hasAccessRight(conn, GlobalConstant.OBJECT_TYPE_DOCUMENT, dmsDoc.getID(), "R") || dmsRoot.getOwnerID().equals(sessionContainer.getUserRecordID())) {
                        result.add(dmsDoc);
                    }
                }
                form.setCurStartRowNo(TextUtility.parseInteger(form.getCurStartRowNo()) + pageSize + "");
            }
        } else {
            AuditTrailManager auditTrailManager = new AuditTrailManager(sessionContainer, conn);
            result = auditTrailManager.getDmsAccessList(GlobalConstant.HOME_RECENTLY_ACCESS_LIST, form);
        }
        return result;
    }

    private List getTaskInboxChannelList(AjaxThreadContext context, AbstractSearchForm form) throws Exception {
        SessionContainer sessionContainer = context.getSessionContainer();
        Connection conn = context.getConnnection();
        String filterItem = (String) context.getRequestAttribute(AjaxConstant.FILTERITEM);
        List result = null;
        WorkflowProgressDAObject workflowProgressDAO = new WorkflowProgressDAObject(sessionContainer, conn);
        workflowProgressDAO.setLog(log);
        if ("-".equals(filterItem) || Utility.isEmpty(filterItem)) {
            ((ListWorkflowProgressForm) form).setDelegateUserRecordID(null);
            ((ListWorkflowProgressForm) form).setIncludeDelegateReocrd(true);
        } else {
            ((ListWorkflowProgressForm) form).setDelegateUserRecordID(filterItem);
        }
        form.setSortAttribute("CREATE_DATE");
        result = workflowProgressDAO.getPendingList4UserCanRead(form);
        return result;
    }
}
