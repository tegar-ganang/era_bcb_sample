package com.infineon.dns.action;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.infineon.dns.form.UsageForm;
import com.infineon.dns.model.Usage;
import com.infineon.dns.service.UsageService;
import com.infineon.dns.util.Locator;
import com.infineon.dns.util.PagedListAndTotalCount;

public class UsageAction extends BaseAction {

    public ActionForward listUsages(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        try {
            UsageService usageService = Locator.lookupService(UsageService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PagedListAndTotalCount<Usage> map = usageService.getUsages(request.getParameter("sort"), request.getParameter("dir"), request.getParameter("start"), request.getParameter("limit"));
            StringBuffer json = new StringBuffer("{totalCount:" + map.getTotalCount() + ",usages:[");
            for (Usage usage : map.getPagedList()) {
                json.append("{'usageId':'" + usage.getUsageId() + "','usageName':'" + StringEscapeUtils.escapeHtml(usage.getUsageName()).replace("\\", "\\\\").replace("'", "\\'").replace("/", "\\/") + "','usageNameText':'" + StringEscapeUtils.escapeJavaScript(usage.getUsageName()) + "','usageCode':'" + usage.getUsageCode() + "','usageRemark':'" + StringEscapeUtils.escapeHtml(usage.getUsageRemark()).replace("\r\n", "<br>").replace("\\", "\\\\").replace("'", "\\'").replace("/", "\\/") + "','usageRemarkText':'" + StringEscapeUtils.escapeJavaScript(usage.getUsageRemark()) + "'},");
            }
            if (map.getTotalCount() != 0) {
                json.deleteCharAt(json.length() - 1);
            }
            json.append("]}");
            response.getWriter().write(json.toString());
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            return mapping.findForward("");
        }
    }

    public ActionForward createUsage(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            UsageForm usageForm = (UsageForm) form;
            Usage usage = new Usage();
            UsageService usageService = Locator.lookupService(UsageService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            if (usageService.getUsageByUsageName(usageForm.getUsageName()).size() > 0) {
                response.getWriter().write("{success:false,message:'Usage name: " + usageForm.getUsageName() + " already existed'}");
                return mapping.findForward("");
            }
            if (usageService.getUsageByUsageCode(usageForm.getUsageCode()).size() > 0) {
                response.getWriter().write("{success:false,message:'Usage code: " + usageForm.getUsageCode() + " already existed'}");
                return mapping.findForward("");
            }
            usage.setUsageName(usageForm.getUsageName());
            usage.setUsageCode(usageForm.getUsageCode());
            usage.setUsageRemark(usageForm.getUsageRemark());
            usageService.insertUsage(usage);
            response.getWriter().write("{success:true,message:'New usage successfully added'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }

    public ActionForward updateUsage(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            UsageForm usageForm = (UsageForm) form;
            UsageService usageService = Locator.lookupService(UsageService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            Usage usage = usageService.getUsageByUsageId(usageForm.getUsageId(), true);
            if (usage == null) {
                response.getWriter().write("{success:true,message:'This usage information has already been deleted'}");
                return mapping.findForward("");
            }
            List<Usage> usageList = usageService.getUsageByUsageName(usageForm.getUsageName());
            if (usageList.size() > 0 && usageList.get(0).getUsageId() != usageForm.getUsageId()) {
                response.getWriter().write("{success:false,message:'Usage name: " + usageForm.getUsageName() + " already existed'}");
                return mapping.findForward("");
            }
            usageList = usageService.getUsageByUsageCode(usageForm.getUsageCode());
            if (usageList.size() > 0 && usageList.get(0).getUsageId() != usageForm.getUsageId()) {
                response.getWriter().write("{success:false,message:'Usage code: " + usageForm.getUsageCode() + " already existed'}");
                return mapping.findForward("");
            }
            usage.setUsageName(usageForm.getUsageName());
            usage.setUsageCode(usageForm.getUsageCode());
            usage.setUsageRemark(usageForm.getUsageRemark());
            usageService.updateUsage(usage);
            response.getWriter().write("{success:true,message:'Modify usage information successfully'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }

    public ActionForward deleteUsage(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            UsageForm usageForm = (UsageForm) form;
            UsageService usageService = Locator.lookupService(UsageService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            Usage usage = usageService.getUsageByUsageId(usageForm.getUsageId(), true);
            if (usage == null) {
                response.getWriter().write("{success:true,message:'This usage information has already been deleted'}");
                return mapping.findForward("");
            }
            if (usage.getDocuments().size() != 0) {
                response.getWriter().write("{success:true,message:'This usage information has been attached to some document numbers, it can not be deleted'}");
                return mapping.findForward("");
            }
            usageService.deleteUsage(usageForm.getUsageId());
            response.getWriter().write("{success:true,message:'Successfully delete usage information'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }
}
