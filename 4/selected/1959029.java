package com.infineon.dns.action;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.infineon.dns.form.LanguageForm;
import com.infineon.dns.model.Language;
import com.infineon.dns.service.LanguageService;
import com.infineon.dns.util.Locator;
import com.infineon.dns.util.PagedListAndTotalCount;

public class LanguageAction extends BaseAction {

    public ActionForward listLanguages(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        try {
            LanguageService languageService = Locator.lookupService(LanguageService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PagedListAndTotalCount<Language> map = languageService.getLanguages(request.getParameter("sort"), request.getParameter("dir"), request.getParameter("start"), request.getParameter("limit"));
            StringBuffer json = new StringBuffer("{totalCount:" + map.getTotalCount() + ",languages:[");
            for (Language language : map.getPagedList()) {
                json.append("{'languageId':'" + language.getLanguageId() + "','languageName':'" + StringEscapeUtils.escapeHtml(language.getLanguageName()).replace("\\", "\\\\").replace("'", "\\'").replace("/", "\\/") + "','languageNameText':'" + StringEscapeUtils.escapeJavaScript(language.getLanguageName()) + "','languageCode':'" + language.getLanguageCode() + "','languageRemark':'" + StringEscapeUtils.escapeHtml(language.getLanguageRemark()).replace("\r\n", "<br>").replace("\\", "\\\\").replace("'", "\\'").replace("/", "\\/") + "','languageRemarkText':'" + StringEscapeUtils.escapeJavaScript(language.getLanguageRemark()) + "'},");
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

    public ActionForward createLanguage(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            LanguageForm languageForm = (LanguageForm) form;
            Language language = new Language();
            LanguageService languageService = Locator.lookupService(LanguageService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            if (languageService.getLanguageByLanguageName(languageForm.getLanguageName()).size() > 0) {
                response.getWriter().write("{success:false,message:'Language name: " + languageForm.getLanguageName() + " already existed'}");
                return mapping.findForward("");
            }
            if (languageService.getLanguageByLanguageCode(languageForm.getLanguageCode()).size() > 0) {
                response.getWriter().write("{success:false,message:'Language code: " + languageForm.getLanguageCode() + " already existed'}");
                return mapping.findForward("");
            }
            language.setLanguageName(languageForm.getLanguageName());
            language.setLanguageCode(languageForm.getLanguageCode());
            language.setLanguageRemark(languageForm.getLanguageRemark());
            languageService.insertLanguage(language);
            response.getWriter().write("{success:true,message:'New language successfully added'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }

    public ActionForward updateLanguage(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            LanguageForm languageForm = (LanguageForm) form;
            LanguageService languageService = Locator.lookupService(LanguageService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            Language language = languageService.getLanguageByLanguageId(languageForm.getLanguageId(), true);
            if (language == null) {
                response.getWriter().write("{success:true,message:'This language information has already been deleted'}");
                return mapping.findForward("");
            }
            List<Language> languageList = languageService.getLanguageByLanguageName(languageForm.getLanguageName());
            if (languageList.size() > 0 && languageList.get(0).getLanguageId() != languageForm.getLanguageId()) {
                response.getWriter().write("{success:false,message:'Language name: " + languageForm.getLanguageName() + " already existed'}");
                return mapping.findForward("");
            }
            languageList = languageService.getLanguageByLanguageCode(languageForm.getLanguageCode());
            if (languageList.size() > 0 && languageList.get(0).getLanguageId() != languageForm.getLanguageId()) {
                response.getWriter().write("{success:false,message:'Language code: " + languageForm.getLanguageCode() + " already existed'}");
                return mapping.findForward("");
            }
            language.setLanguageName(languageForm.getLanguageName());
            language.setLanguageCode(languageForm.getLanguageCode());
            language.setLanguageRemark(languageForm.getLanguageRemark());
            languageService.updateLanguage(language);
            response.getWriter().write("{success:true,message:'Modify language information successfully'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }

    public ActionForward deleteLanguage(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            LanguageForm languageForm = (LanguageForm) form;
            LanguageService languageService = Locator.lookupService(LanguageService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            Language language = languageService.getLanguageByLanguageId(languageForm.getLanguageId(), true);
            if (language == null) {
                response.getWriter().write("{success:true,message:'This language information has already been deleted'}");
                return mapping.findForward("");
            }
            if (language.getDocuments().size() != 0) {
                response.getWriter().write("{success:true,message:'This language information has been attached to some document numbers, it can not be deleted'}");
                return mapping.findForward("");
            }
            languageService.deleteLanguage(languageForm.getLanguageId());
            response.getWriter().write("{success:true,message:'Successfully delete language information'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }
}
