package com.infineon.dns.action;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.infineon.dns.form.CategoryForm;
import com.infineon.dns.model.Category;
import com.infineon.dns.service.CategoryService;
import com.infineon.dns.util.Locator;
import com.infineon.dns.util.PagedListAndTotalCount;

public class CategoryAction extends BaseAction {

    public ActionForward listCategories(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        try {
            CategoryService categoryService = Locator.lookupService(CategoryService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            PagedListAndTotalCount<Category> map = categoryService.getCategories(request.getParameter("sort"), request.getParameter("dir"), request.getParameter("start"), request.getParameter("limit"));
            StringBuffer json = new StringBuffer("{totalCount:" + map.getTotalCount() + ",categorys:[");
            for (Category category : map.getPagedList()) {
                json.append("{'categoryId':'" + category.getCategoryId() + "','categoryName':'" + StringEscapeUtils.escapeHtml(category.getCategoryName()).replace("\\", "\\\\").replace("'", "\\'").replace("/", "\\/") + "','categoryNameText':'" + StringEscapeUtils.escapeJavaScript(category.getCategoryName()) + "','categoryCode':'" + category.getCategoryCode() + "','categoryRemark':'" + StringEscapeUtils.escapeHtml(category.getCategoryRemark()).replace("\r\n", "<br>").replace("\\", "\\\\").replace("'", "\\'").replace("/", "\\/") + "','categoryRemarkText':'" + StringEscapeUtils.escapeJavaScript(category.getCategoryRemark()) + "'},");
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

    public ActionForward createCategory(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            CategoryForm categoryForm = (CategoryForm) form;
            Category category = new Category();
            CategoryService categoryService = Locator.lookupService(CategoryService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            if (categoryService.getCategoryByCategoryName(categoryForm.getCategoryName()).size() > 0) {
                response.getWriter().write("{success:false,message:'Category name: " + categoryForm.getCategoryName() + " already existed'}");
                return mapping.findForward("");
            }
            if (categoryService.getCategoryByCategoryCode(categoryForm.getCategoryCode()).size() > 0) {
                response.getWriter().write("{success:false,message:'Category code: " + categoryForm.getCategoryCode() + " already existed'}");
                return mapping.findForward("");
            }
            category.setCategoryName(categoryForm.getCategoryName());
            category.setCategoryCode(categoryForm.getCategoryCode());
            category.setCategoryRemark(categoryForm.getCategoryRemark());
            categoryService.insertCategory(category);
            response.getWriter().write("{success:true,message:'New category successfully added'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }

    public ActionForward updateCategory(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            CategoryForm categoryForm = (CategoryForm) form;
            CategoryService categoryService = Locator.lookupService(CategoryService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            Category category = categoryService.getCategoryByCategoryId(categoryForm.getCategoryId(), true);
            if (category == null) {
                response.getWriter().write("{success:true,message:'This category information has already been deleted'}");
                return mapping.findForward("");
            }
            List<Category> categoryList = categoryService.getCategoryByCategoryName(categoryForm.getCategoryName());
            if (categoryList.size() > 0 && categoryList.get(0).getCategoryId() != categoryForm.getCategoryId()) {
                response.getWriter().write("{success:false,message:'Category name: " + categoryForm.getCategoryName() + " already existed'}");
                return mapping.findForward("");
            }
            categoryList = categoryService.getCategoryByCategoryCode(categoryForm.getCategoryCode());
            if (categoryList.size() > 0 && categoryList.get(0).getCategoryId() != categoryForm.getCategoryId()) {
                response.getWriter().write("{success:false,message:'Category code: " + categoryForm.getCategoryCode() + " already existed'}");
                return mapping.findForward("");
            }
            category.setCategoryName(categoryForm.getCategoryName());
            category.setCategoryCode(categoryForm.getCategoryCode());
            category.setCategoryRemark(categoryForm.getCategoryRemark());
            categoryService.updateCategory(category);
            response.getWriter().write("{success:true,message:'Modify category information successfully'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }

    public ActionForward deleteCategory(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            CategoryForm categoryForm = (CategoryForm) form;
            CategoryService categoryService = Locator.lookupService(CategoryService.class);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            Category category = categoryService.getCategoryByCategoryId(categoryForm.getCategoryId(), true);
            if (category == null) {
                response.getWriter().write("{success:true,message:'This category information has already been deleted'}");
                return mapping.findForward("");
            }
            if (category.getDocuments().size() != 0) {
                response.getWriter().write("{success:true,message:'This category information has been attached to some document numbers, it can not be deleted'}");
                return mapping.findForward("");
            }
            categoryService.deleteCategory(categoryForm.getCategoryId());
            response.getWriter().write("{success:true,message:'Successfully delete category information'}");
            return mapping.findForward("");
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{success:false,message:'Unexpected exception occurred'}");
            return mapping.findForward("");
        }
    }
}
