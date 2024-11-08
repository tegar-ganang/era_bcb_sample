package com.jeecms.article.manager.impl;

import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import com.jeecms.article.dao.ArticleDao;
import com.jeecms.article.entity.Article;
import com.jeecms.article.manager.ArticleMng;
import com.jeecms.cms.entity.CmsAdmin;
import com.jeecms.cms.entity.CmsChannel;
import com.jeecms.cms.entity.CmsComment;
import com.jeecms.cms.entity.CmsMember;
import com.jeecms.cms.entity.CmsTopic;
import com.jeecms.cms.entity.ContentCtg;
import com.jeecms.cms.manager.CmsAdminMng;
import com.jeecms.cms.manager.CmsCommentMng;
import com.jeecms.cms.manager.CmsConfigMng;
import com.jeecms.cms.manager.ContentCtgMng;
import com.jeecms.common.hibernate3.Updater;
import com.jeecms.common.hibernate3.Updater.UpdateMode;
import com.jeecms.common.page.Pagination;
import com.jeecms.common.struts2.ContextPvd;
import com.jeecms.common.util.ComUtils;
import com.jeecms.core.JeeCoreManagerImpl;
import com.jeecms.core.entity.Attachment;
import com.jeecms.core.entity.User;
import com.jeecms.core.entity.Website;
import com.jeecms.core.util.UploadRule;
import com.jeecms.core.util.UploadRule.UploadFile;

@Service
@Transactional
public class ArticleMngImpl extends JeeCoreManagerImpl<Article> implements ArticleMng {

    public Pagination getForTag(Long webId, Long chnlId, Long topicId, Long ctgId, String searchKey, Boolean hasTitleImg, boolean recommend, int topLevel, int orderBy, boolean isPage, int firstResult, int pageNo, int pageSize) {
        return getDao().getForTag(webId, chnlId, topicId, ctgId, searchKey, hasTitleImg, recommend, topLevel, orderBy, isPage, firstResult, pageNo, pageSize);
    }

    public Pagination getRightArticle(Long webId, Long chnlId, Long adminId, Long inputAdminId, Long contentCtgId, boolean disabled, boolean topTime, int topLevel, int status, String title, int order, int pageNo, int pageSize) {
        return getDao().getRightArticle(webId, chnlId, adminId, inputAdminId, contentCtgId, disabled, topTime, topLevel, status, title, order, pageNo, pageSize);
    }

    public Pagination getArticleForMember(Long memberId, Long webId, Boolean draft, Boolean check, Boolean reject, int pageNo, int pageSize) {
        return getDao().getArticleForMember(memberId, webId, draft, check, reject, pageNo, pageSize);
    }

    public Pagination getUncheckArticle(Long adminId, int pageNo, int pageSize) {
        return getDao().getUncheckArticle(adminId, pageNo, pageSize);
    }

    public Pagination getUnsigninArticle(Long adminId, int pageNo, int pageSize) {
        return getDao().getUnsigninArticle(adminId, pageNo, pageSize);
    }

    public Article saveArticle(Article bean, CmsAdmin admin, UploadRule rule, String resUrl, int checkCount, long topTime) {
        Assert.notNull(bean);
        Assert.notNull(admin);
        Assert.notNull(rule);
        Assert.notNull(resUrl);
        initDefValue(bean);
        handleTitleImg(bean);
        handleDate(bean, topTime);
        handleCheckRight(bean, admin, checkCount);
        bean.calculatePageCount();
        bean.setContentResPath(resUrl);
        bean.setAdminInput(admin);
        bean = save(bean);
        bean.writeContent(contextPvd.getAppRoot(), 0);
        CmsChannel chnl = bean.getChannel();
        chnl.setDocCount(chnl.getDocCount() + 1);
        addSideArticle(bean);
        addAttachment(bean, rule, admin.getAdmin().getUser(), null);
        return bean;
    }

    public Article updateArticle(Article arti, CmsAdmin admin, UploadRule rule, long topTime) {
        Assert.notNull(arti);
        Assert.notNull(admin);
        Assert.notNull(rule);
        Article entity = findById(arti.getId());
        Website web = entity.getWebsite();
        User user = admin.getAdmin().getUser();
        admin = cmsAdminMng.getAdminByUserId(web.getId(), user.getId());
        handleTitleImg(arti);
        arti.calculatePageCount();
        int origCount = entity.getPageCount();
        CmsChannel origChnl = entity.getChannel();
        boolean origCheck = entity.getCheck();
        updateByUpdater(createUpdater(arti));
        entity.writeContent(contextPvd.getAppRoot(), origCount);
        handleTopTimeForUpdate(entity, topTime);
        handleCheckRight(entity, admin, entity.getConfig().getCheckCount());
        if (entity.getAdminInput() == null) {
            entity.setAdminInput(admin);
        }
        boolean currCheck = entity.getCheck();
        if (currCheck != origCheck) {
            if (currCheck) {
                addSideArticle(entity);
            } else {
                removeSideArticle(entity);
            }
        }
        CmsChannel currChnl = entity.getChannel();
        if (!currChnl.equals(origChnl)) {
            currChnl.setDocCount(currChnl.getDocCount() + 1);
            origChnl.setDocCount(origChnl.getDocCount() - 1);
            removeSideArticle(entity);
            addSideArticle(entity);
        }
        removeAttachment(entity, false);
        addAttachment(entity, rule, user, null);
        return entity;
    }

    @Override
    public Article deleteById(Serializable id) {
        Article entity = findById(id);
        for (CmsTopic topic : entity.getTopics()) {
            topic.getArticles().remove(entity);
        }
        CmsChannel chnl = entity.getChannel();
        removeSideArticle(entity);
        delete(entity);
        chnl.setDocCount(chnl.getDocCount() - 1);
        CmsCommentMng.deleteComment(entity.getId(), CmsComment.DOC_ARTICLE);
        removeAttachment(entity, true);
        entity.deleteContentFile(contextPvd.getAppRoot());
        return entity;
    }

    public Article disableArticle(Long id, CmsAdmin admin, boolean disable) {
        Article entity = findById(id);
        if (disable) {
            removeSideArticle(entity);
        } else {
            addSideArticle(entity);
        }
        entity.setDisabled(disable);
        entity.setAdminDisable(admin);
        entity.setDisableTime(ComUtils.now());
        return entity;
    }

    public List<Article> disableArticle(Long[] ids, CmsAdmin admin, boolean disable) {
        List<Article> dts = new ArrayList<Article>();
        Article entity = null;
        if (ids != null && ids.length > 0) {
            for (Long id : ids) {
                entity = disableArticle(id, admin, disable);
                if (entity != null) {
                    dts.add(entity);
                }
            }
        }
        return dts;
    }

    public Article checkArticle(Long id, CmsAdmin admin) {
        Article entity = findById(id);
        admin = cmsAdminMng.getAdminByUserId(entity.getWebsite().getId(), admin.getAdmin().getUser().getId());
        handleCheckRight(entity, admin, entity.getConfig().getCheckCount());
        removeSideArticle(entity);
        return entity;
    }

    public List<Article> checkArticle(Long[] ids, CmsAdmin admin) {
        List<Article> dts = new ArrayList<Article>();
        Article entity = null;
        if (ids != null && ids.length > 0) {
            for (Long id : ids) {
                entity = checkArticle(id, admin);
                if (entity != null) {
                    dts.add(entity);
                }
            }
        }
        return dts;
    }

    public Article rejectArticle(Long id, CmsAdmin admin, String opinion) {
        Article entity = findById(id);
        admin = cmsAdminMng.getAdminByUserId(entity.getWebsite().getId(), admin.getAdmin().getUser().getId());
        if (entity.getCheckStep() >= admin.getCheckRight()) {
            return entity;
        }
        entity.setAdminCheck(admin);
        entity.setReject(true);
        entity.setCheckTime(ComUtils.now());
        entity.setCheckOpinion(opinion);
        return entity;
    }

    public List<Article> rejectArticle(Long[] ids, CmsAdmin admin, String opinion) {
        List<Article> dts = new ArrayList<Article>();
        Article entity = null;
        if (ids != null && ids.length > 0) {
            for (Long id : ids) {
                entity = rejectArticle(id, admin, opinion);
                if (entity != null) {
                    dts.add(entity);
                }
            }
        }
        return dts;
    }

    public Article signinArticle(Long id, CmsAdmin admin) {
        Article entity = findById(id);
        admin = cmsAdminMng.getAdminByUserId(entity.getWebsite().getId(), admin.getAdmin().getUser().getId());
        if (entity.getCheckStep() >= admin.getCheckRight()) {
            return entity;
        }
        entity.setAdminInput(admin);
        handleCheckRight(entity, admin, entity.getConfig().getCheckCount());
        return entity;
    }

    public List<Article> signinArticle(Long[] ids, CmsAdmin admin) {
        List<Article> dts = new ArrayList<Article>();
        Article entity = null;
        if (ids != null && ids.length > 0) {
            for (Long id : ids) {
                entity = signinArticle(id, admin);
                if (entity != null) {
                    dts.add(entity);
                }
            }
        }
        return dts;
    }

    public Article memberSave(Article bean, CmsMember member, UploadRule rule) {
        Assert.notNull(bean);
        Assert.notNull(member);
        Assert.notNull(rule);
        Website web = bean.getWebsite();
        ContentCtg ctg = contentCtgMng.getFirstCtg(web.getRootWebId());
        bean.setContentCtg(ctg);
        bean.setConfig(cmsConfigMng.findById(web.getId()));
        initDefValue(bean);
        handleDate(bean, 0);
        bean.calculatePageCount();
        bean.setContentResPath(web.getResUrl());
        bean.setAdminInput(null);
        bean.setMember(member);
        bean = save(bean);
        bean.writeContent(contextPvd.getAppRoot(), 0);
        CmsChannel chnl = bean.getChannel();
        chnl.setDocCount(chnl.getDocCount() + 1);
        addAttachment(bean, rule, member.getMember().getUser(), member);
        return bean;
    }

    public Article memberUpdate(Article bean, CmsMember cmsMember, UploadRule rule) {
        Assert.notNull(bean);
        Assert.notNull(cmsMember);
        Assert.notNull(rule);
        Article entity = findById(bean.getId());
        entity.setCheckStep(-1);
        entity.setReject(false);
        entity.calculatePageCount();
        int origCount = entity.getPageCount();
        CmsChannel origChnl = entity.getChannel();
        updateByUpdater(createMemberUpdate(bean));
        entity.writeContent(contextPvd.getAppRoot(), origCount);
        CmsChannel currChnl = entity.getChannel();
        if (!currChnl.equals(origChnl)) {
            currChnl.setDocCount(currChnl.getDocCount() + 1);
            origChnl.setDocCount(origChnl.getDocCount() - 1);
        }
        removeAttachment(entity, false);
        addAttachment(entity, rule, cmsMember.getMember().getUser(), null);
        return entity;
    }

    private Updater createUpdater(Article bean) {
        Updater updater = Updater.create(bean);
        updater.exclude(Article.PROP_WEBSITE);
        updater.exclude(Article.PROP_CONFIG);
        updater.exclude(Article.PROP_CONTENT_RES_PATH);
        updater.exclude(Article.PROP_ADMIN_CHECK);
        updater.exclude(Article.PROP_ADMIN_DISABLE);
        updater.exclude(Article.PROP_ADMIN_INPUT);
        updater.exclude(Article.PROP_CHECK);
        updater.exclude(Article.PROP_CHECK_OPINION);
        updater.exclude(Article.PROP_CHECK_STEP);
        updater.exclude(Article.PROP_CHECK_TIME);
        updater.exclude(Article.PROP_CONTENT_RES_PATH);
        updater.exclude(Article.PROP_DISABLE_TIME);
        updater.exclude(Article.PROP_REJECT);
        return updater;
    }

    private Updater createMemberUpdate(Article bean) {
        Updater updater = Updater.create(bean, UpdateMode.MIN);
        updater.include(Article.PROP_CHANNEL);
        updater.include(Article.PROP_TITLE);
        updater.include(Article.PROP_AUTHOR);
        updater.include(Article.PROP_ORIGIN);
        updater.include(Article.PROP_DESCRIPTION);
        updater.include(Article.PROP_DRAFT);
        updater.include("content");
        return updater;
    }

    private void handleTopTimeForUpdate(Article entity, long topTime) {
        if (topTime == -1) {
            entity.setSortDate(entity.getReleaseDate());
        } else if (topTime > 0) {
            topTime *= 60 * 60 * 1000;
            entity.setSortDate(new Timestamp(entity.getSortDate().getTime() + topTime));
        } else {
        }
    }

    private void handleTitleImg(Article arti) {
        if (StringUtils.isBlank(arti.getTitleImg())) {
            arti.setTitleImg("");
            arti.setHasTitleImg(false);
        } else {
            arti.setHasTitleImg(true);
        }
    }

    private void initDefValue(Article arti) {
        arti.setDisabled(false);
        arti.setReject(false);
        arti.setCheck(false);
        arti.setHasTitleImg(false);
        arti.setCheckStep(-1);
        arti.setCheckOpinion("");
        if (arti.getContent() == null) {
            arti.setContent("");
        }
        if (arti.getBold() == null) {
            arti.setBold(false);
        }
        if (arti.getTopLevel() == null) {
            arti.setTopLevel(0);
        }
        if (arti.getAllowComment() == null) {
            arti.setAllowComment(true);
        }
        if (arti.getDraft() == null) {
            arti.setDraft(false);
        }
        if (arti.getRecommend() == null) {
            arti.setRecommend(false);
        }
        arti.setCommentCount(0);
        arti.setVisitTotal(0L);
        arti.setStatDate(ComUtils.now());
        arti.setVisitToday(0L);
        arti.setVisitWeek(0L);
        arti.setVisitMonth(0L);
        arti.setVisitQuarter(0L);
        arti.setVisitYear(0L);
    }

    private void handleDate(Article arti, long topTime) {
        Date now = ComUtils.now();
        arti.setReleaseSysDate(now);
        Date relDate = arti.getReleaseDate();
        if (relDate == null) {
            relDate = now;
            arti.setReleaseDate(relDate);
        }
        topTime *= 60 * 60 * 1000;
        arti.setSortDate(new Date(relDate.getTime() + topTime));
    }

    /**
	 * ����Ա���Ȩ����Ϊ������˼���Ȼ���ж������Ƿ����ͨ��
	 * 
	 * @param arti
	 * @param admin
	 * @param checkCount
	 *            վ����˲�����
	 */
    private void handleCheckRight(Article arti, CmsAdmin admin, int checkCount) {
        int checkRight = admin.getCheckRight();
        arti.setCheckStep(checkRight);
        if (arti.getDraft() || checkCount > checkRight) {
            arti.setCheck(false);
        } else {
            arti.setCheck(true);
        }
        arti.setReject(false);
        arti.setCheckOpinion("");
        arti.setAdminCheck(admin);
        arti.setCheckTime(ComUtils.now());
    }

    private void addSideArticle(Article entity) {
        if (!entity.getCheck() && entity.getDisabled()) {
            return;
        }
        Long webId = entity.getWebsite().getId();
        Long chnlId = entity.getChannel().getId();
        Article pre = getDao().getSideArticle(webId, chnlId, entity.getId(), false);
        if (pre != null) {
            Article next = pre.getNext();
            pre.setNext(entity);
            entity.setPre(pre);
            entity.setNext(next);
        } else {
            Article next = getDao().getSideArticle(webId, chnlId, entity.getId(), true);
            if (next != null) {
                next.setPre(entity);
                entity.setNext(next);
            }
        }
    }

    private void removeSideArticle(Article entity) {
        Article pre = entity.getPre();
        Article next = entity.getNext();
        if (pre != null) {
            pre.setNext(next);
        }
        if (next != null) {
            next.setPre(pre);
        }
    }

    /**
	 * ��������
	 * 
	 * @param entity
	 * @param rule
	 * @param web
	 * @param user
	 */
    private void addAttachment(Article entity, UploadRule rule, User user, CmsMember member) {
        Website web = entity.getWebsite();
        Map<String, UploadFile> uploadFiles = rule.getUploadFiles();
        if (uploadFiles != null) {
            String content = entity.getContent();
            String titleImg = entity.getTitleImg();
            String contentImg = entity.getContentImg();
            Set<String> rmFile = new HashSet<String>();
            Attachment attach;
            UploadFile uf;
            String rootPath = contextPvd.getAppRealPath(web.getUploadRoot().toString());
            for (String name : uploadFiles.keySet()) {
                if (StringUtils.contains(content, name) || StringUtils.contains(titleImg, name) || StringUtils.contains(contentImg, name)) {
                    rmFile.add(name);
                    attach = new Attachment();
                    uf = uploadFiles.get(name);
                    attach.setWebsite(web);
                    attach.setUser(user);
                    attach.setName(uf.getOrigName());
                    attach.setFileName(uf.getFileName());
                    attach.setFilePath(uf.getRelPath(rootPath));
                    attach.setFileSize((int) (uf.getSize() / 1024) + 1);
                    attach.setOwnerCtg(Article.ATTACHMENT_CTG);
                    attach.setOwnerId(entity.getId());
                    attach.setOwnerName(entity.getTitle());
                    attach.setPicture(true);
                    attach.setOwnerUrl(entity.getUrl().replace(entity.getWebUrl(), ""));
                    attach.setDownCount(0L);
                    attach.setCreateTime(ComUtils.now());
                    if (entity.getGroup() == null) {
                        attach.setFree(true);
                    } else {
                        attach.setFree(false);
                    }
                    attach.setLost(false);
                    entity.addToAttachments(attach);
                    if (member != null) {
                        member.addUploadSize((int) uf.getSize());
                    }
                }
            }
            for (String name : rmFile) {
                rule.removeUploadFile(name);
            }
        }
    }

    private void removeAttachment(Article entity, boolean removeAll) {
        Set<Attachment> attachs = entity.getAttachments();
        String content = entity.getContentFromFile();
        String titleImg = entity.getTitleImg();
        String contentImg = entity.getContentImg();
        Set<Attachment> rmAttachs = new HashSet<Attachment>();
        String filename;
        for (Attachment attach : attachs) {
            filename = attach.getFileName();
            if (removeAll || (!StringUtils.contains(content, filename) && !StringUtils.contains(titleImg, filename) && !StringUtils.contains(contentImg, filename))) {
                String realPath = contextPvd.getAppRealPath(attach.getRelPath());
                if (new File(realPath).delete()) {
                    log.info("ɾ�����{}", realPath);
                } else {
                    log.warn("ɾ���ʧ�ܣ�{}", realPath);
                }
                rmAttachs.add(attach);
            }
        }
        attachs.removeAll(rmAttachs);
    }

    public Article findById(Serializable id) {
        Article arti = super.findById(id);
        if (arti == null) {
            return null;
        }
        arti.setRootReal(contextPvd.getAppRoot());
        return arti;
    }

    public Article findAndCheckResPath(Serializable id) {
        Article arti = super.findById(id);
        if (arti == null) {
            return null;
        }
        arti.setRootReal(contextPvd.getAppRoot());
        if (arti.isResPathChannge()) {
            arti.updateResPath();
        }
        return arti;
    }

    @Autowired
    private ContextPvd contextPvd;

    @Autowired
    private CmsAdminMng cmsAdminMng;

    @Autowired
    private ContentCtgMng contentCtgMng;

    @Autowired
    private CmsConfigMng cmsConfigMng;

    @Autowired
    private CmsCommentMng CmsCommentMng;

    @Autowired
    public void setDao(ArticleDao dao) {
        super.setDao(dao);
    }

    protected ArticleDao getDao() {
        return (ArticleDao) super.getDao();
    }
}
