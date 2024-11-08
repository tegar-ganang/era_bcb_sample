package com.city.itis.action;

import java.io.File;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import com.city.itis.domain.Member;
import com.city.itis.domain.Site;
import com.city.itis.domain.SiteCategory;
import com.city.itis.service.MemberCategoryService;
import com.city.itis.service.MemberService;
import com.city.itis.service.SiteCategoryService;
import com.city.itis.service.SiteService;
import com.city.itis.util.Constants;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

/**
 * SiteAction
 * @author WY
 *
 */
public class SiteAction extends ActionSupport implements ModelDriven<Site> {

    private static final long serialVersionUID = 1L;

    private Site site = new Site();

    private List<Site> siteList;

    private SiteService siteService;

    private List<SiteCategory> siteCategoryList;

    private SiteCategory siteCategory;

    private SiteCategoryService siteCategoryService;

    private String command = null;

    private String url = null;

    private String dealPhoto = null;

    private File photo;

    private String photoFileName;

    private String photoContentType;

    Map<String, Object> map = null;

    private MemberService memberService;

    private MemberCategoryService memberCategoryService;

    private String msg = null;

    private String result = null;

    public SiteService getSiteService() {
        return siteService;
    }

    @Resource
    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    /**
	 * 显示添加方法
	 * @return
	 */
    public String show_add() throws Exception {
        map = ActionContext.getContext().getSession();
        String loginType = (String) map.get("loginType");
        if (Constants.ADMIN.equals(loginType)) {
            show_category(site.getCategory().getCategoryNo());
            site.getCategory().setCategoryName(siteCategory.getCategoryName());
        }
        siteCategoryList = siteCategoryService.findAll();
        if (siteCategoryList != null) {
            url = "/site/site_add.jsp";
            return SUCCESS;
        } else {
            return ERROR;
        }
    }

    /**
	 * 添加方法
	 * @return
	 */
    public String add() throws Exception {
        map = ActionContext.getContext().getSession();
        String loginType = (String) map.get("loginType");
        if (Constants.ADMIN.equals(loginType)) {
            site.setPhotoName(photoFileName);
            site.getCategory().setCategoryNo(site.getCategoryNo());
            int flag = (Integer) this.siteService.add(site);
            if (flag > 0) {
                save_photo();
                url = "/site/list.action?site.category.categoryNo=" + site.getCategory().getCategoryNo();
                return SUCCESS;
            } else {
                return ERROR;
            }
        } else {
            Member login_user = (Member) map.get("login_user");
            Member m = memberService.getMemberById(login_user.getId());
            if (m != null) {
                Integer siteCount = m.getSiteCount();
                if (siteCount == null) {
                    siteCount = 0;
                }
                siteCount++;
                if (Constants.COPPERMEMBER.equals(m.getCategory().getName())) {
                    Integer level = m.getLevel();
                    if (level / 100 > 0 && level % 100 == 0) {
                        if (siteCount > 11) {
                            msg = "我亲爱的会员，您添加的站点数目已经超过上限了。建议您付费升级会员";
                            url = "/member/member_alert.jsp";
                            return "member_alert";
                        }
                    } else {
                        if (siteCount > 10) {
                            msg = "我亲爱的会员，您添加的站点数目已经超过上限了。建议您付费升级会员";
                            url = "/member/member_alert.jsp";
                            return "member_alert";
                        }
                    }
                } else if (Constants.SILVERMEMBER.equals(m.getCategory().getName())) {
                    Integer level = m.getLevel();
                    if (level / 100 > 0 && level % 100 == 0) {
                        if (siteCount > 21) {
                            msg = "我亲爱的会员，您添加的站点数目已经超过上限了。建议您付费升级会员";
                            url = "/member/member_alert.jsp";
                            return "member_alert";
                        }
                    } else {
                        if (siteCount > 20) {
                            msg = "我亲爱的会员，您添加的站点数目已经超过上限了。建议您付费升级会员";
                            url = "/member/member_alert.jsp";
                            return "member_alert";
                        }
                    }
                } else {
                    Integer level = m.getLevel();
                    if (level / 100 > 0 && level % 100 == 0) {
                        if (siteCount > 31) {
                            msg = "我亲爱的会员，您添加的站点数目已经超过上限了。建议您付费升级会员";
                            url = "/member/member_alert.jsp";
                            return "member_alert";
                        }
                    } else {
                        if (siteCount > 30) {
                            msg = "我亲爱的会员，您添加的站点数目已经超过上限了。建议您付费";
                            url = "/member/member_alert.jsp";
                            return "member_alert";
                        }
                    }
                }
                m.setSiteCount(siteCount);
                site.setPhotoName(photoFileName);
                site.setMember(m);
                site.getCategory().setCategoryNo(site.getCategoryNo());
                int flag = (Integer) this.siteService.add(site);
                if (flag > 0) {
                    if (site.getPhotoName() != null) {
                        save_photo();
                    }
                    memberService.modify(m);
                    url = "/site/listByMemberId.action";
                    return SUCCESS;
                } else {
                    return ERROR;
                }
            } else {
                return ERROR;
            }
        }
    }

    /**
	 * 删除方法
	 * @return
	 */
    public String delete() throws Exception {
        map = ActionContext.getContext().getSession();
        String loginType = (String) map.get("loginType");
        if (Constants.ADMIN.equals(loginType)) {
            site.getCategory().setCategoryNo(site.getCategoryNo());
            int flag = (Integer) this.siteService.delete(site);
            if (flag > 0) {
                if (site.getPhotoName() != null) {
                    delete_photo();
                }
                url = "/site/list.action?site.category.categoryNo=" + site.getCategory().getCategoryNo();
                return SUCCESS;
            } else {
                return ERROR;
            }
        } else {
            Member login_user = (Member) map.get("login_user");
            Member m = memberService.getMemberById(login_user.getId());
            Integer siteCount = m.getSiteCount();
            if (siteCount == null) {
                siteCount = 0;
            }
            siteCount--;
            if (siteCount < 0) {
                return ERROR;
            }
            site.getCategory().setCategoryNo(site.getCategoryNo());
            int flag = (Integer) this.siteService.delete(site);
            if (flag > 0) {
                if (site.getPhotoName() != null) {
                    delete_photo();
                }
                m.setSiteCount(siteCount);
                site.setMember(m);
                memberService.modify(m);
                url = "/site/listByMemberId.action";
                return SUCCESS;
            } else {
                return ERROR;
            }
        }
    }

    /**
	 * 修改方法
	 * @return
	 */
    public String modify() {
        try {
            if (dealPhoto.equals(Constants.MODIFY)) {
                save_photo();
                site.setPhotoName(photoFileName);
            } else if (dealPhoto.equals(Constants.DELETE)) {
                delete_photo();
                site.setPhotoName(null);
            } else {
                site.setPhotoName(site.getPhotoName());
            }
            map = ActionContext.getContext().getSession();
            String loginType = (String) map.get("loginType");
            if (Constants.ADMIN.equals(loginType)) {
                site.getCategory().setCategoryNo(site.getCategoryNo());
                int flag = (Integer) this.siteService.modify(site);
                if (flag > 0) {
                    url = "/site/list.action?site.category.categoryNo=" + site.getCategory().getCategoryNo();
                    return SUCCESS;
                } else {
                    return ERROR;
                }
            } else {
                Member login_user = (Member) map.get("login_user");
                site.setMember(login_user);
                site.getCategory().setCategoryNo(site.getCategoryNo());
                int flag = (Integer) this.siteService.modify(site);
                if (flag > 0) {
                    url = "/site/listByMemberId.action";
                    return SUCCESS;
                } else {
                    return ERROR;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return SUCCESS;
    }

    /**
	 * 查询方法
	 * @return
	 */
    public String find() throws Exception {
        site = this.siteService.getSiteById(site.getSiteNo());
        if (site != null) {
            if (Constants.MODIFY.equals(command)) {
                show_category_list();
                url = "/site/site_modify.jsp";
                return SUCCESS;
            } else if (Constants.DELETE.equals(command)) {
                show_category_list();
                url = "/site/site_delete.jsp";
                return SUCCESS;
            } else {
                url = "/site/site_detail.jsp";
                return SUCCESS;
            }
        } else {
            return ERROR;
        }
    }

    /**
	 * 根据站点名称，查询站点信息方法。
	 * @return
	 * @throws Exception
	 */
    public String getSiteBySiteName() throws Exception {
        String name = new String(site.getSiteName().getBytes("iso-8859-1"), "UTF-8");
        Site s = siteService.getSiteBySiteName(name);
        if (s != null) {
            result = "站点已经存在";
        } else {
            result = "恭喜您，该站点可用。";
        }
        return SUCCESS;
    }

    /**
	 * 显示所有信息方法
	 * @return
	 */
    public String list() throws Exception {
        show_category(site.getCategory().getCategoryNo());
        site.getCategory().setCategoryName(siteCategory.getCategoryName());
        siteList = this.siteService.findAllByCategoryNo(site.getCategory().getCategoryNo());
        if (siteList != null) {
            url = "/site/site_maint.jsp";
            return SUCCESS;
        } else {
            return ERROR;
        }
    }

    /**
	 * 根据会员编号，显示所有站点信息
	 * @return
	 * @throws Exception
	 */
    public String listByMemberId() throws Exception {
        map = ActionContext.getContext().getSession();
        Member m = (Member) map.get("login_user");
        if (m != null) {
            siteList = siteService.listByMemberId(m.getId());
            url = "/site/site_maint.jsp";
            return SUCCESS;
        } else {
            return LOGIN;
        }
    }

    /**
	 *显示类别列表方法
	 * @return
	 */
    public String show_category_list() throws Exception {
        siteCategoryList = siteCategoryService.findAll();
        if (siteCategoryList != null) {
            return NONE;
        } else {
            return ERROR;
        }
    }

    /**
	 * 根据站点类别方法，调用查询站点类别信息方法。
	 * @param id	站点类别编号
	 * @throws Exception
	 */
    private void show_category(Integer id) throws Exception {
        siteCategory = siteCategoryService.getSiteCategoryById(id);
    }

    /**
     * 保存图片方法
     */
    public void save_photo() throws Exception {
        String realPath = ServletActionContext.getServletContext().getRealPath("/upload");
        if (photo != null) {
            File saveFile = new File(new File(realPath), photoFileName);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            if (saveFile != null) {
                FileUtils.copyFile(photo, saveFile);
            }
        }
    }

    /**
	 * 删除图片方法
	 */
    public void delete_photo() throws Exception {
        String realPath = ServletActionContext.getServletContext().getRealPath("/upload");
        if (site.getPhotoName() != null) {
            File deleteFile = new File(new File(realPath), site.getPhotoName());
            if (!deleteFile.getParentFile().exists()) {
                deleteFile.getParentFile().mkdirs();
            }
            if (deleteFile != null) {
                deleteFile.delete();
            }
        }
    }

    @Override
    public Site getModel() {
        return site;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public List<Site> getSiteList() {
        return siteList;
    }

    public void setSiteList(List<Site> siteList) {
        this.siteList = siteList;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<SiteCategory> getSiteCategoryList() {
        return siteCategoryList;
    }

    public void setSiteCategoryList(List<SiteCategory> siteCategoryList) {
        this.siteCategoryList = siteCategoryList;
    }

    public SiteCategoryService getSiteCategoryService() {
        return siteCategoryService;
    }

    @Resource
    public void setSiteCategoryService(SiteCategoryService siteCategoryService) {
        this.siteCategoryService = siteCategoryService;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public File getPhoto() {
        return photo;
    }

    public void setPhoto(File photo) {
        this.photo = photo;
    }

    public String getPhotoFileName() {
        return photoFileName;
    }

    public void setPhotoFileName(String photoFileName) {
        this.photoFileName = photoFileName;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }

    public String getDealPhoto() {
        return dealPhoto;
    }

    public void setDealPhoto(String dealPhoto) {
        this.dealPhoto = dealPhoto;
    }

    public MemberService getMemberService() {
        return memberService;
    }

    @Resource
    public void setMemberService(MemberService memberService) {
        this.memberService = memberService;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public MemberCategoryService getMemberCategoryService() {
        return memberCategoryService;
    }

    public void setMemberCategoryService(MemberCategoryService memberCategoryService) {
        this.memberCategoryService = memberCategoryService;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
