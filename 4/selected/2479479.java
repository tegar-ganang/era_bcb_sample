package com.myres.struts2.action;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import com.myres.dao.CategoryDao;
import com.myres.model.Category;
import com.myres.service.CategoryService;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.validator.annotations.RegexFieldValidator;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;

public class CategoryManageAction extends ActionSupport {

    private CategoryService categoryService;

    private String newCategoryName;

    private String newCategoryDescription;

    private int categoryId;

    private File upload;

    private String uploadFileName;

    private String savePath;

    private List<Category> categories;

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public File getUpload() {
        return upload;
    }

    public void setUpload(File upload) {
        this.upload = upload;
    }

    public String getUploadFileName() {
        return uploadFileName;
    }

    public void setUploadFileName(String uploadFileName) {
        this.uploadFileName = uploadFileName;
    }

    public CategoryManageAction() {
        super();
        System.out.println("constructor of CategoryManageAction");
    }

    public CategoryService getCategoryService() {
        return categoryService;
    }

    public void setCategoryService(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public String getNewCategoryName() {
        return newCategoryName;
    }

    @RequiredStringValidator(message = "必须填写")
    public void setNewCategoryName(String newCategoryName) {
        this.newCategoryName = newCategoryName;
    }

    public String getNewCategoryDescription() {
        return newCategoryDescription;
    }

    public void setNewCategoryDescription(String newCategoryDescription) {
        this.newCategoryDescription = newCategoryDescription;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String input() throws Exception {
        return SUCCESS;
    }

    public String showAllCategories() throws Exception {
        categories = categoryService.findAll();
        return SUCCESS;
    }

    public String testUtf8() {
        System.out.println(newCategoryName);
        return SUCCESS;
    }

    public String addCategory() throws Exception {
        try {
            List<Category> categories = categoryService.findByName(newCategoryName);
            if (categories.size() != 0) {
                this.addFieldError("newCategoryName", "该分类已存在");
                return INPUT;
            }
            String targetFileName;
            String targetDirectory = ServletActionContext.getServletContext().getRealPath("/" + savePath);
            if (upload != null) {
                targetFileName = generateFileName(uploadFileName);
                File target = new File(targetDirectory, targetFileName);
                FileUtils.copyFile(upload, target);
            } else {
                targetFileName = "default.png";
            }
            Category category = new Category();
            category.setName(newCategoryName);
            category.setSummary(newCategoryDescription);
            category.setPriority(0);
            category.setIcon(savePath + "/" + targetFileName);
            categoryService.save(category);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return SUCCESS;
    }

    private String generateFileName(String fileName) {
        DateFormat format = new SimpleDateFormat("yyMMddHHmmss");
        String formatDate = format.format(new Date());
        int random = new Random().nextInt(10000);
        int position = fileName.lastIndexOf(".");
        String extension = fileName.substring(position);
        return formatDate + random + extension;
    }
}
