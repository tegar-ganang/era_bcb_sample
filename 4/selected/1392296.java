package com.jeecms.cms.manager.main.impl;

import static com.bocoon.entity.cms.main.ContentCheck.DRAFT;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import com.bocoon.app.cms.admin.staticpage.StaticPageSvc;
import com.bocoon.app.cms.admin.staticpage.exception.ContentNotCheckedException;
import com.bocoon.app.cms.admin.staticpage.exception.GeneratedZeroStaticPageException;
import com.bocoon.app.cms.admin.staticpage.exception.StaticPageNotOpenException;
import com.bocoon.app.cms.admin.staticpage.exception.TemplateNotFoundException;
import com.bocoon.app.cms.admin.staticpage.exception.TemplateParseException;
import com.bocoon.common.hibernate3.Updater;
import com.bocoon.common.page.Pagination;
import com.bocoon.entity.cms.main.Channel;
import com.bocoon.entity.cms.main.CmsGroup;
import com.bocoon.entity.cms.main.CmsSite;
import com.bocoon.entity.cms.main.CmsTopic;
import com.bocoon.entity.cms.main.CmsUser;
import com.bocoon.entity.cms.main.CmsUserSite;
import com.bocoon.entity.cms.main.Content;
import com.bocoon.entity.cms.main.ContentCheck;
import com.bocoon.entity.cms.main.ContentCount;
import com.bocoon.entity.cms.main.ContentExt;
import com.bocoon.entity.cms.main.ContentTag;
import com.bocoon.entity.cms.main.ContentTxt;
import com.bocoon.entity.cms.main.Channel.AfterCheckEnum;
import com.bocoon.entity.cms.main.Content.ContentStatus;
import com.jeecms.cms.dao.main.ContentDao;
import com.jeecms.cms.manager.assist.CmsCommentMng;
import com.jeecms.cms.manager.main.ChannelMng;
import com.jeecms.cms.manager.main.CmsGroupMng;
import com.jeecms.cms.manager.main.CmsTopicMng;
import com.jeecms.cms.manager.main.CmsUserMng;
import com.jeecms.cms.manager.main.ContentCheckMng;
import com.jeecms.cms.manager.main.ContentCountMng;
import com.jeecms.cms.manager.main.ContentExtMng;
import com.jeecms.cms.manager.main.ContentMng;
import com.jeecms.cms.manager.main.ContentTagMng;
import com.jeecms.cms.manager.main.ContentTxtMng;
import com.jeecms.cms.manager.main.ContentTypeMng;
import com.jeecms.cms.service.ChannelDeleteChecker;
import com.jeecms.cms.service.ContentListener;
import freemarker.template.TemplateException;

@Service
@Transactional
public class ContentMngImpl implements ContentMng, ChannelDeleteChecker {

    @Transactional(readOnly = true)
    public Pagination getPageByRight(String title, Integer typeId, Integer inputUserId, boolean topLevel, boolean recommend, ContentStatus status, Byte checkStep, Integer siteId, Integer channelId, Integer userId, int orderBy, int pageNo, int pageSize) {
        CmsUser user = cmsUserMng.findById(userId);
        CmsUserSite us = user.getUserSite(siteId);
        Pagination p;
        boolean allChannel = us.getAllChannel();
        boolean selfData = user.getSelfAdmin();
        if (allChannel && selfData) {
            p = dao.getPageBySelf(title, typeId, inputUserId, topLevel, recommend, status, checkStep, siteId, channelId, userId, orderBy, pageNo, pageSize);
        } else if (allChannel && !selfData) {
            p = dao.getPage(title, typeId, inputUserId, topLevel, recommend, status, checkStep, siteId, channelId, orderBy, pageNo, pageSize);
        } else {
            p = dao.getPageByRight(title, typeId, inputUserId, topLevel, recommend, status, checkStep, siteId, channelId, userId, selfData, orderBy, pageNo, pageSize);
        }
        return p;
    }

    public Pagination getPageForMember(String title, Integer channelId, Integer siteId, Integer memberId, int pageNo, int pageSize) {
        return dao.getPage(title, null, memberId, false, false, ContentStatus.all, null, siteId, channelId, 0, pageNo, pageSize);
    }

    @Transactional(readOnly = true)
    public Content getSide(Integer id, Integer siteId, Integer channelId, boolean next) {
        return dao.getSide(id, siteId, channelId, next, true);
    }

    @Transactional(readOnly = true)
    public List<Content> getListByIdsForTag(Integer[] ids, int orderBy) {
        if (ids.length == 1) {
            Content content = findById(ids[0]);
            List<Content> list;
            if (content != null) {
                list = new ArrayList<Content>(1);
                list.add(content);
            } else {
                list = new ArrayList<Content>(0);
            }
            return list;
        } else {
            return dao.getListByIdsForTag(ids, orderBy);
        }
    }

    @Transactional(readOnly = true)
    public Pagination getPageBySiteIdsForTag(Integer[] siteIds, Integer[] typeIds, Boolean titleImg, Boolean recommend, String title, int orderBy, int pageNo, int pageSize) {
        return dao.getPageBySiteIdsForTag(siteIds, typeIds, titleImg, recommend, title, orderBy, pageNo, pageSize);
    }

    @Transactional(readOnly = true)
    public List<Content> getListBySiteIdsForTag(Integer[] siteIds, Integer[] typeIds, Boolean titleImg, Boolean recommend, String title, int orderBy, Integer first, Integer count) {
        return dao.getListBySiteIdsForTag(siteIds, typeIds, titleImg, recommend, title, orderBy, first, count);
    }

    @Transactional(readOnly = true)
    public Pagination getPageByChannelIdsForTag(Integer[] channelIds, Integer[] typeIds, Boolean titleImg, Boolean recommend, String title, int orderBy, int option, int pageNo, int pageSize) {
        return dao.getPageByChannelIdsForTag(channelIds, typeIds, titleImg, recommend, title, orderBy, option, pageNo, pageSize);
    }

    @Transactional(readOnly = true)
    public List<Content> getListByChannelIdsForTag(Integer[] channelIds, Integer[] typeIds, Boolean titleImg, Boolean recommend, String title, int orderBy, int option, Integer first, Integer count) {
        return dao.getListByChannelIdsForTag(channelIds, typeIds, titleImg, recommend, title, orderBy, option, first, count);
    }

    @Transactional(readOnly = true)
    public Pagination getPageByChannelPathsForTag(String[] paths, Integer[] siteIds, Integer[] typeIds, Boolean titleImg, Boolean recommend, String title, int orderBy, int pageNo, int pageSize) {
        return dao.getPageByChannelPathsForTag(paths, siteIds, typeIds, titleImg, recommend, title, orderBy, pageNo, pageSize);
    }

    @Transactional(readOnly = true)
    public List<Content> getListByChannelPathsForTag(String[] paths, Integer[] siteIds, Integer[] typeIds, Boolean titleImg, Boolean recommend, String title, int orderBy, Integer first, Integer count) {
        return dao.getListByChannelPathsForTag(paths, siteIds, typeIds, titleImg, recommend, title, orderBy, first, count);
    }

    @Transactional(readOnly = true)
    public Pagination getPageByTopicIdForTag(Integer topicId, Integer[] siteIds, Integer[] channelIds, Integer[] typeIds, Boolean titleImg, Boolean recommend, String title, int orderBy, int pageNo, int pageSize) {
        return dao.getPageByTopicIdForTag(topicId, siteIds, channelIds, typeIds, titleImg, recommend, title, orderBy, pageNo, pageSize);
    }

    @Transactional(readOnly = true)
    public List<Content> getListByTopicIdForTag(Integer topicId, Integer[] siteIds, Integer[] channelIds, Integer[] typeIds, Boolean titleImg, Boolean recommend, String title, int orderBy, Integer first, Integer count) {
        return dao.getListByTopicIdForTag(topicId, siteIds, channelIds, typeIds, titleImg, recommend, title, orderBy, first, count);
    }

    @Transactional(readOnly = true)
    public Pagination getPageByTagIdsForTag(Integer[] tagIds, Integer[] siteIds, Integer[] channelIds, Integer[] typeIds, Integer excludeId, Boolean titleImg, Boolean recommend, String title, int orderBy, int pageNo, int pageSize) {
        return dao.getPageByTagIdsForTag(tagIds, siteIds, channelIds, typeIds, excludeId, titleImg, recommend, title, orderBy, pageNo, pageSize);
    }

    @Transactional(readOnly = true)
    public List<Content> getListByTagIdsForTag(Integer[] tagIds, Integer[] siteIds, Integer[] channelIds, Integer[] typeIds, Integer excludeId, Boolean titleImg, Boolean recommend, String title, int orderBy, Integer first, Integer count) {
        return dao.getListByTagIdsForTag(tagIds, siteIds, channelIds, typeIds, excludeId, titleImg, recommend, title, orderBy, first, count);
    }

    @Transactional(readOnly = true)
    public Content findById(Integer id) {
        Content entity = dao.findById(id);
        return entity;
    }

    public Content save(Content bean, ContentExt ext, ContentTxt txt, Integer[] channelIds, Integer[] topicIds, Integer[] viewGroupIds, String[] tagArr, String[] attachmentPaths, String[] attachmentNames, String[] attachmentFilenames, String[] picPaths, String[] picDescs, Integer channelId, Integer typeId, Boolean draft, CmsUser user, boolean forMember) {
        bean.setChannel(channelMng.findById(channelId));
        bean.setType(contentTypeMng.findById(typeId));
        bean.setUser(user);
        Byte userStep;
        if (forMember) {
            userStep = 0;
        } else {
            CmsSite site = bean.getSite();
            userStep = user.getCheckStep(site.getId());
        }
        if (draft != null && draft) {
            bean.setStatus(ContentCheck.DRAFT);
        } else {
            if (userStep >= bean.getChannel().getFinalStepExtends()) {
                bean.setStatus(ContentCheck.CHECKED);
            } else {
                bean.setStatus(ContentCheck.CHECKING);
            }
        }
        bean.setHasTitleImg(!StringUtils.isBlank(ext.getTitleImg()));
        bean.init();
        preSave(bean);
        dao.save(bean);
        contentExtMng.save(ext, bean);
        contentTxtMng.save(txt, bean);
        ContentCheck check = new ContentCheck();
        check.setCheckStep(userStep);
        contentCheckMng.save(check, bean);
        contentCountMng.save(new ContentCount(), bean);
        if (channelIds != null && channelIds.length > 0) {
            for (Integer cid : channelIds) {
                bean.addToChannels(channelMng.findById(cid));
            }
        }
        bean.addToChannels(channelMng.findById(channelId));
        if (topicIds != null && topicIds.length > 0) {
            for (Integer tid : topicIds) {
                bean.addToTopics(cmsTopicMng.findById(tid));
            }
        }
        if (viewGroupIds != null && viewGroupIds.length > 0) {
            for (Integer gid : viewGroupIds) {
                bean.addToGroups(cmsGroupMng.findById(gid));
            }
        }
        List<ContentTag> tags = contentTagMng.saveTags(tagArr);
        bean.setTags(tags);
        if (attachmentPaths != null && attachmentPaths.length > 0) {
            for (int i = 0, len = attachmentPaths.length; i < len; i++) {
                if (!StringUtils.isBlank(attachmentPaths[i])) {
                    bean.addToAttachmemts(attachmentPaths[i], attachmentNames[i], attachmentFilenames[i]);
                }
            }
        }
        if (picPaths != null && picPaths.length > 0) {
            for (int i = 0, len = picPaths.length; i < len; i++) {
                if (!StringUtils.isBlank(picPaths[i])) {
                    bean.addToPictures(picPaths[i], picDescs[i]);
                }
            }
        }
        afterSave(bean);
        return bean;
    }

    public Content update(Content bean, ContentExt ext, ContentTxt txt, String[] tagArr, Integer[] channelIds, Integer[] topicIds, Integer[] viewGroupIds, String[] attachmentPaths, String[] attachmentNames, String[] attachmentFilenames, String[] picPaths, String[] picDescs, Map<String, String> attr, Integer channelId, Integer typeId, Boolean draft, CmsUser user, boolean forMember) {
        Content entity = findById(bean.getId());
        List<Map<String, Object>> mapList = preChange(entity);
        Updater<Content> updater = new Updater<Content>(bean);
        bean = dao.updateByUpdater(updater);
        Byte userStep;
        if (forMember) {
            userStep = 0;
        } else {
            CmsSite site = bean.getSite();
            userStep = user.getCheckStep(site.getId());
        }
        AfterCheckEnum after = bean.getChannel().getAfterCheckEnum();
        if (after == AfterCheckEnum.BACK_UPDATE && bean.getCheckStep() > userStep) {
            bean.getContentCheck().setCheckStep(userStep);
            if (bean.getCheckStep() >= bean.getChannel().getFinalStepExtends()) {
                bean.setStatus(ContentCheck.CHECKED);
            } else {
                bean.setStatus(ContentCheck.CHECKING);
            }
        }
        if (draft != null) {
            if (draft) {
                bean.setStatus(DRAFT);
            } else {
                if (bean.getStatus() == DRAFT) {
                    if (bean.getCheckStep() >= bean.getChannel().getFinalStepExtends()) {
                        bean.setStatus(ContentCheck.CHECKED);
                    } else {
                        bean.setStatus(ContentCheck.CHECKING);
                    }
                }
            }
        }
        bean.setHasTitleImg(!StringUtils.isBlank(ext.getTitleImg()));
        if (channelId != null) {
            bean.setChannel(channelMng.findById(channelId));
        }
        if (typeId != null) {
            bean.setType(contentTypeMng.findById(typeId));
        }
        contentExtMng.update(ext);
        contentTxtMng.update(txt, bean);
        if (attr != null) {
            Map<String, String> attrOrig = bean.getAttr();
            attrOrig.clear();
            attrOrig.putAll(attr);
        }
        Set<Channel> channels = bean.getChannels();
        channels.clear();
        if (channelIds != null && channelIds.length > 0) {
            for (Integer cid : channelIds) {
                channels.add(channelMng.findById(cid));
            }
        }
        channels.add(bean.getChannel());
        Set<CmsTopic> topics = bean.getTopics();
        topics.clear();
        if (topicIds != null && topicIds.length > 0) {
            for (Integer tid : topicIds) {
                topics.add(cmsTopicMng.findById(tid));
            }
        }
        Set<CmsGroup> groups = bean.getViewGroups();
        groups.clear();
        if (viewGroupIds != null && viewGroupIds.length > 0) {
            for (Integer gid : viewGroupIds) {
                groups.add(cmsGroupMng.findById(gid));
            }
        }
        contentTagMng.updateTags(bean.getTags(), tagArr);
        bean.getAttachments().clear();
        if (attachmentPaths != null && attachmentPaths.length > 0) {
            for (int i = 0, len = attachmentPaths.length; i < len; i++) {
                if (!StringUtils.isBlank(attachmentPaths[i])) {
                    bean.addToAttachmemts(attachmentPaths[i], attachmentNames[i], attachmentFilenames[i]);
                }
            }
        }
        bean.getPictures().clear();
        if (picPaths != null && picPaths.length > 0) {
            for (int i = 0, len = picPaths.length; i < len; i++) {
                if (!StringUtils.isBlank(picPaths[i])) {
                    bean.addToPictures(picPaths[i], picDescs[i]);
                }
            }
        }
        afterChange(bean, mapList);
        return bean;
    }

    public Content check(Integer id, CmsUser user) {
        Content content = findById(id);
        List<Map<String, Object>> mapList = preChange(content);
        ContentCheck check = content.getContentCheck();
        byte userStep = user.getCheckStep(content.getSite().getId());
        byte contentStep = check.getCheckStep();
        byte finalStep = content.getChannel().getFinalStepExtends();
        if (userStep < contentStep) {
            return content;
        }
        check.setRejected(false);
        if (userStep > contentStep) {
            check.setCheckOpinion(null);
        }
        check.setCheckStep(userStep);
        if (userStep >= finalStep) {
            content.setStatus(ContentCheck.CHECKED);
            check.setCheckOpinion(null);
        }
        afterChange(content, mapList);
        return content;
    }

    public Content[] check(Integer[] ids, CmsUser user) {
        Content[] beans = new Content[ids.length];
        for (int i = 0, len = ids.length; i < len; i++) {
            beans[i] = check(ids[i], user);
        }
        return beans;
    }

    public Content reject(Integer id, CmsUser user, Byte step, String opinion) {
        Content content = findById(id);
        Integer siteId = content.getSite().getId();
        byte userStep = user.getCheckStep(siteId);
        byte contentStep = content.getCheckStep();
        if (userStep < contentStep) {
            return content;
        }
        List<Map<String, Object>> mapList = preChange(content);
        ContentCheck check = content.getContentCheck();
        if (!StringUtils.isBlank(opinion)) {
            check.setCheckOpinion(opinion);
        }
        check.setRejected(true);
        content.setStatus(ContentCheck.CHECKING);
        if (step != null) {
            if (step < userStep) {
                check.setCheckStep(step);
            } else {
                check.setCheckStep(userStep);
            }
        } else {
            if (contentStep < userStep) {
            } else if (contentStep == userStep) {
                check.setCheckStep((byte) (check.getCheckStep() - 1));
            }
        }
        afterChange(content, mapList);
        return content;
    }

    public Content[] reject(Integer[] ids, CmsUser user, Byte step, String opinion) {
        Content[] beans = new Content[ids.length];
        for (int i = 0, len = ids.length; i < len; i++) {
            beans[i] = reject(ids[i], user, step, opinion);
        }
        return beans;
    }

    public Content cycle(Integer id) {
        Content content = findById(id);
        List<Map<String, Object>> mapList = preChange(content);
        content.setStatus(ContentCheck.RECYCLE);
        afterChange(content, mapList);
        return content;
    }

    public Content[] cycle(Integer[] ids) {
        Content[] beans = new Content[ids.length];
        for (int i = 0, len = ids.length; i < len; i++) {
            beans[i] = cycle(ids[i]);
        }
        return beans;
    }

    public Content recycle(Integer id) {
        Content content = findById(id);
        List<Map<String, Object>> mapList = preChange(content);
        byte contentStep = content.getCheckStep();
        byte finalStep = content.getChannel().getFinalStepExtends();
        if (contentStep >= finalStep && !content.getRejected()) {
            content.setStatus(ContentCheck.CHECKED);
        } else {
            content.setStatus(ContentCheck.CHECKING);
        }
        afterChange(content, mapList);
        return content;
    }

    public Content[] recycle(Integer[] ids) {
        Content[] beans = new Content[ids.length];
        for (int i = 0, len = ids.length; i < len; i++) {
            beans[i] = recycle(ids[i]);
        }
        return beans;
    }

    public Content deleteById(Integer id) {
        Content bean = findById(id);
        preDelete(bean);
        contentTagMng.removeTags(bean.getTags());
        cmsCommentMng.deleteByContentId(id);
        bean.clear();
        bean = dao.deleteById(id);
        afterDelete(bean);
        return bean;
    }

    public Content[] deleteByIds(Integer[] ids) {
        Content[] beans = new Content[ids.length];
        for (int i = 0, len = ids.length; i < len; i++) {
            beans[i] = deleteById(ids[i]);
        }
        return beans;
    }

    public Content[] contentStatic(Integer[] ids) throws TemplateNotFoundException, TemplateParseException, GeneratedZeroStaticPageException, StaticPageNotOpenException, ContentNotCheckedException {
        int count = 0;
        List<Content> list = new ArrayList<Content>();
        for (int i = 0, len = ids.length; i < len; i++) {
            Content content = findById(ids[i]);
            try {
                if (!content.getChannel().getStaticContent()) {
                    throw new StaticPageNotOpenException("content.staticNotOpen", count, content.getTitle());
                }
                if (!content.isChecked()) {
                    throw new ContentNotCheckedException("content.notChecked", count, content.getTitle());
                }
                if (staticPageSvc.content(content)) {
                    list.add(content);
                    count++;
                }
            } catch (IOException e) {
                throw new TemplateNotFoundException("content.tplContentNotFound", count, content.getTitle());
            } catch (TemplateException e) {
                throw new TemplateParseException("content.tplContentException", count, content.getTitle());
            }
        }
        if (count == 0) {
            throw new GeneratedZeroStaticPageException("content.staticGenerated");
        }
        Content[] beans = new Content[count];
        return list.toArray(beans);
    }

    public Pagination getPageForCollection(Integer siteId, Integer memberId, int pageNo, int pageSize) {
        return dao.getPageForCollection(siteId, memberId, pageNo, pageSize);
    }

    public String checkForChannelDelete(Integer channelId) {
        int count = dao.countByChannelId(channelId);
        if (count > 0) {
            return "content.error.cannotDeleteChannel";
        } else {
            return null;
        }
    }

    private void preSave(Content content) {
        if (listenerList != null) {
            for (ContentListener listener : listenerList) {
                listener.preSave(content);
            }
        }
    }

    private void afterSave(Content content) {
        if (listenerList != null) {
            for (ContentListener listener : listenerList) {
                listener.afterSave(content);
            }
        }
    }

    private List<Map<String, Object>> preChange(Content content) {
        if (listenerList != null) {
            int len = listenerList.size();
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(len);
            for (ContentListener listener : listenerList) {
                list.add(listener.preChange(content));
            }
            return list;
        } else {
            return null;
        }
    }

    private void afterChange(Content content, List<Map<String, Object>> mapList) {
        if (listenerList != null) {
            Assert.notNull(mapList);
            Assert.isTrue(mapList.size() == listenerList.size());
            int len = listenerList.size();
            ContentListener listener;
            for (int i = 0; i < len; i++) {
                listener = listenerList.get(i);
                listener.afterChange(content, mapList.get(i));
            }
        }
    }

    private void preDelete(Content content) {
        if (listenerList != null) {
            for (ContentListener listener : listenerList) {
                listener.preDelete(content);
            }
        }
    }

    private void afterDelete(Content content) {
        if (listenerList != null) {
            for (ContentListener listener : listenerList) {
                listener.afterDelete(content);
            }
        }
    }

    private List<ContentListener> listenerList;

    public void setListenerList(List<ContentListener> listenerList) {
        this.listenerList = listenerList;
    }

    private ChannelMng channelMng;

    private ContentExtMng contentExtMng;

    private ContentTxtMng contentTxtMng;

    private ContentTypeMng contentTypeMng;

    private ContentCountMng contentCountMng;

    private ContentCheckMng contentCheckMng;

    private ContentTagMng contentTagMng;

    private CmsGroupMng cmsGroupMng;

    private CmsUserMng cmsUserMng;

    private CmsTopicMng cmsTopicMng;

    private CmsCommentMng cmsCommentMng;

    private ContentDao dao;

    private StaticPageSvc staticPageSvc;

    @Autowired
    public void setChannelMng(ChannelMng channelMng) {
        this.channelMng = channelMng;
    }

    @Autowired
    public void setContentTypeMng(ContentTypeMng contentTypeMng) {
        this.contentTypeMng = contentTypeMng;
    }

    @Autowired
    public void setContentCountMng(ContentCountMng contentCountMng) {
        this.contentCountMng = contentCountMng;
    }

    @Autowired
    public void setContentExtMng(ContentExtMng contentExtMng) {
        this.contentExtMng = contentExtMng;
    }

    @Autowired
    public void setContentTxtMng(ContentTxtMng contentTxtMng) {
        this.contentTxtMng = contentTxtMng;
    }

    @Autowired
    public void setContentCheckMng(ContentCheckMng contentCheckMng) {
        this.contentCheckMng = contentCheckMng;
    }

    @Autowired
    public void setCmsTopicMng(CmsTopicMng cmsTopicMng) {
        this.cmsTopicMng = cmsTopicMng;
    }

    @Autowired
    public void setContentTagMng(ContentTagMng contentTagMng) {
        this.contentTagMng = contentTagMng;
    }

    @Autowired
    public void setCmsGroupMng(CmsGroupMng cmsGroupMng) {
        this.cmsGroupMng = cmsGroupMng;
    }

    @Autowired
    public void setCmsUserMng(CmsUserMng cmsUserMng) {
        this.cmsUserMng = cmsUserMng;
    }

    @Autowired
    public void setCmsCommentMng(CmsCommentMng cmsCommentMng) {
        this.cmsCommentMng = cmsCommentMng;
    }

    @Autowired
    public void setDao(ContentDao dao) {
        this.dao = dao;
    }

    @Autowired
    public void setStaticPageSvc(StaticPageSvc staticPageSvc) {
        this.staticPageSvc = staticPageSvc;
    }
}
