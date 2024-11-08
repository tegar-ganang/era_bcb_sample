package com.infineon.dns.action;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.infineon.dns.form.TypeForm;
import com.infineon.dns.model.Type;
import com.infineon.dns.service.TypeService;
import com.infineon.dns.util.Locator;
import com.infineon.dns.util.PagedListAndTotalCount;

public class TypeAction extends BaseAction {

    public ActionForward listTypes(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        try {
            TypeService typeService = Locator.lookupService(TypeService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PagedListAndTotalCount<Type> map = typeService.getTypes(request.getParameter("sort"), request.getParameter("dir"), request.getParameter("start"), request.getParameter("limit"));
            StringBuffer json = new StringBuffer("{totalCount:" + map.getTotalCount() + ",types:[");
            for (Type type : map.getPagedList()) {
                json.append("{'typeId':'" + type.getTypeId() + "','typeName':'" + StringEscapeUtils.escapeHtml(type.getTypeName()).replace("\\", "\\\\").replace("'", "\\'").replace("/", "\\/") + "','typeNameText':'" + StringEscapeUtils.escapeJavaScript(type.getTypeName()) + "','typeCode':'" + type.getTypeCode() + "','typeAbbreviation':'" + type.getTypeAbbreviation() + "','shouldReviewed':'" + type.isShouldReviewed() + "','typeRemark':'" + StringEscapeUtils.escapeHtml(type.getTypeRemark()).replace("\r\n", "<br>").replace("\\", "\\\\").replace("'", "\\'").replace("/", "\\/") + "','typeRemarkText':'" + StringEscapeUtils.escapeJavaScript(type.getTypeRemark()) + "'},");
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

    public ActionForward createType(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            TypeForm typeForm = (TypeForm) form;
            Type type = new Type();
            TypeService typeService = Locator.lookupService(TypeService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            if (typeService.getTypeByTypeName(typeForm.getTypeName()).size() > 0) {
                response.getWriter().write("{success:false,message:'Type name: " + typeForm.getTypeName() + " already existed'}");
                return mapping.findForward("");
            }
            if (typeService.getTypeByTypeCode(typeForm.getTypeCode()).size() > 0) {
                response.getWriter().write("{success:false,message:'Type code: " + typeForm.getTypeCode() + " already existed'}");
                return mapping.findForward("");
            }
            if (typeService.getTypeByTypeAbbreviation(typeForm.getTypeAbbreviation()).size() > 0) {
                response.getWriter().write("{success:false,message:'Type abbreviation: " + typeForm.getTypeAbbreviation() + " already existed'}");
                return mapping.findForward("");
            }
            type.setTypeName(typeForm.getTypeName());
            type.setTypeCode(typeForm.getTypeCode());
            type.setTypeAbbreviation(typeForm.getTypeAbbreviation());
            type.setShouldReviewed(typeForm.isShouldReviewed());
            type.setTypeRemark(typeForm.getTypeRemark());
            typeService.insertType(type);
            response.getWriter().write("{success:true,message:'New type successfully added'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }

    public ActionForward updateType(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            TypeForm typeForm = (TypeForm) form;
            TypeService typeService = Locator.lookupService(TypeService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            Type type = typeService.getTypeByTypeId(typeForm.getTypeId(), true);
            if (type == null) {
                response.getWriter().write("{success:true,message:'This type information has already been deleted'}");
                return mapping.findForward("");
            }
            List<Type> typeList = typeService.getTypeByTypeName(typeForm.getTypeName());
            if (typeList.size() > 0 && typeList.get(0).getTypeId() != typeForm.getTypeId()) {
                response.getWriter().write("{success:false,message:'Type name: " + typeForm.getTypeName() + " already existed'}");
                return mapping.findForward("");
            }
            typeList = typeService.getTypeByTypeCode(typeForm.getTypeCode());
            if (typeList.size() > 0 && typeList.get(0).getTypeId() != typeForm.getTypeId()) {
                response.getWriter().write("{success:false,message:'Type code: " + typeForm.getTypeCode() + " already existed'}");
                return mapping.findForward("");
            }
            typeList = typeService.getTypeByTypeAbbreviation(typeForm.getTypeAbbreviation());
            if (typeList.size() > 0 && typeList.get(0).getTypeId() != typeForm.getTypeId()) {
                response.getWriter().write("{success:false,message:'Type abbreviation: " + typeForm.getTypeAbbreviation() + " already existed'}");
                return mapping.findForward("");
            }
            type.setTypeName(typeForm.getTypeName());
            type.setTypeCode(typeForm.getTypeCode());
            type.setTypeAbbreviation(typeForm.getTypeAbbreviation());
            type.setShouldReviewed(typeForm.isShouldReviewed());
            type.setTypeRemark(typeForm.getTypeRemark());
            typeService.updateType(type);
            response.getWriter().write("{success:true,message:'Modify type information successfully'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }

    public ActionForward deleteType(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            TypeForm typeForm = (TypeForm) form;
            TypeService typeService = Locator.lookupService(TypeService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            Type type = typeService.getTypeByTypeId(typeForm.getTypeId(), true);
            if (type == null) {
                response.getWriter().write("{success:true,message:'This type information has already been deleted'}");
                return mapping.findForward("");
            }
            if (type.getDocuments().size() != 0) {
                response.getWriter().write("{success:true,message:'This type information has been attached to some document numbers, it can not be deleted'}");
                return mapping.findForward("");
            }
            typeService.deleteType(typeForm.getTypeId());
            response.getWriter().write("{success:true,message:'Successfully delete type information'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }
}
