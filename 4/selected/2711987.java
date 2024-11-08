package cn.mmbook.platform.service.tag.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javacommon.base.BaseManager;
import javacommon.base.EntityDao;
import javacommon.util.SpringContextUtil;
import javacommon.util.StringTool;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import cn.mmbook.freemark.generator.DaoGenerator;
import cn.mmbook.platform.dao.tag.CmsTagCategoryDao;
import cn.mmbook.platform.model.manage.SiteChannels;
import cn.mmbook.platform.model.manage.SiteContentSort;
import cn.mmbook.platform.model.manage.SitePart;
import cn.mmbook.platform.model.tag.CmsTagCategory;
import cn.mmbook.platform.model.tag.CmsTemplet;
import cn.mmbook.platform.service.manage.SiteChannelsManager;
import cn.mmbook.platform.service.manage.SiteContentSortManager;
import cn.mmbook.platform.service.manage.SitePartManager;
import cn.mmbook.platform.service.tag.CmsSkinsManager;
import cn.mmbook.platform.service.tag.CmsTagCategoryManager;
import cn.mmbook.platform.service.tag.CmsTempletManager;
import cn.org.rapid_framework.page.Page;
import cn.org.rapid_framework.page.PageRequest;

/**
 * 
 * @author Felix
 *
 */
@Component("cmsTagCategoryManager")
@Transactional
public class CmsTagCategoryManagerImpl extends BaseManager<CmsTagCategory, java.lang.String> implements CmsTagCategoryManager {

    private CmsTagCategoryDao cmsTagCategoryDao;

    private CmsTempletManager cmsTempletManager;

    private SiteChannelsManager siteChannelsManager;

    private SitePartManager sitePartManager;

    private SiteContentSortManager siteContentSortManager;

    private CmsSkinsManager cmsSkinsManager;

    /**增加setXXXX()方法,spring就可以通过autowire自动设置对象属性*/
    public void setCmsTagCategoryDao(CmsTagCategoryDao dao) {
        this.cmsTagCategoryDao = dao;
    }

    public void setCmsTempletManager(CmsTempletManager manager) {
        this.cmsTempletManager = manager;
    }

    public void setSiteChannelsManager(SiteChannelsManager manager) {
        this.siteChannelsManager = manager;
    }

    public void setSitePartManager(SitePartManager manager) {
        this.sitePartManager = manager;
    }

    public void setSiteContentSortManager(SiteContentSortManager manager) {
        this.siteContentSortManager = manager;
    }

    public void setCmsSkinsManager(CmsSkinsManager manager) {
        this.cmsSkinsManager = manager;
    }

    public EntityDao getEntityDao() {
        return this.cmsTagCategoryDao;
    }

    @Transactional(readOnly = true)
    public Page findByPageRequest(PageRequest pr) {
        return cmsTagCategoryDao.findByPageRequest(pr);
    }

    /**
	 * 查询 栏目标签 列表(不分页)
	 * @author Felix add
	 * @param CmsTagCategory u 
	 * @return
	 */
    public List getList(CmsTagCategory u) {
        return cmsTagCategoryDao.getList(u);
    }

    /**
	 * 栏目标表(tg_cms_tag_category) 栏目(tg_cms_category)  模板(tg_cms_templet) 三表关联查询
	 * @author Felix add
	 * @param q
	 * @return
	 */
    @Transactional(readOnly = true)
    public Page findByPageRequestNew(PageRequest<Map> pr) {
        return cmsTagCategoryDao.findByPageRequestNew(pr);
    }

    /**
	 * 按栏目名称查询栏目对象
	 * 栏目标表(tg_cms_tag_category)  
	 * @author qiongguo add
	 * @param tagName 栏目名称
	 * @return
	 */
    public CmsTagCategory getCmsTagCategoryByTagName(String tagname) {
        return cmsTagCategoryDao.getCmsTagCategoryByTagName(tagname);
    }

    /**
	 * 自定义保存 栏目标签
	 * 栏目标表(tg_cms_tag_category)  
	 * @author qiongguo add
	 * @param CmsTagCategory 栏目数据类
	 * @return
	 */
    public void saveCmsTagCategory(CmsTagCategory entity) {
        String maxId = getEntityDao().getMaxId();
        entity.setId(maxId);
        save(entity);
        CmsTemplet cmsTemplet = cmsTempletManager.getById(entity.getTempletId());
        entity.setCmsTemplet(cmsTemplet);
        entity.setTagListInfo(getTagInfoList(entity));
        String templetPath = cmsSkinsManager.getTempletPath(entity.getVersionId());
        cmsTemplet.setTempletPath(templetPath);
        DaoGenerator daoGenerator = (DaoGenerator) SpringContextUtil.getBean("categorytag");
        daoGenerator.init(entity);
        daoGenerator.generate();
    }

    /**
	 * 自定义修改 栏目标签
	 * 栏目标表(tg_cms_tag_category)  
	 * @author qiongguo add
	 * @param CmsTagCategory 栏目数据类
	 * @return
	 */
    public void updateCmsTagCategory(CmsTagCategory entity) {
        update(entity);
        CmsTemplet cmsTemplet = cmsTempletManager.getById(entity.getTempletId());
        entity.setCmsTemplet(cmsTemplet);
        entity.setTagListInfo(getTagInfoList(entity));
        String templetPath = cmsSkinsManager.getTempletPath(entity.getVersionId());
        cmsTemplet.setTempletPath(templetPath);
        DaoGenerator daoGenerator = (DaoGenerator) SpringContextUtil.getBean("categorytag");
        daoGenerator.init(entity);
        daoGenerator.generate();
    }

    /**
	 * 封装数据
	 * @author qiongguo
	 * @param info
	 * @return
	 */
    private List getTagInfoList(CmsTagCategory entity) {
        if (null != entity) {
            List list = new ArrayList();
            String tagSort = entity.getTagSort();
            String categoryId = entity.getCategoryId();
            List id_list = new ArrayList();
            try {
                id_list = StringTool.stringToArray(categoryId);
                if (null != id_list && "channels".equals(tagSort)) {
                    for (int i = 0; i < id_list.size(); i++) {
                        String channelsId = (String) id_list.get(i);
                        channelsId = channelsId.replaceAll(" ", "");
                        if (StringTool.isNull(channelsId)) {
                            SiteChannels data = (SiteChannels) siteChannelsManager.getById(Integer.parseInt(channelsId));
                            data.setName(data.getChannelsName());
                            list.add(data);
                        }
                    }
                }
                if (null != id_list && "category".equals(tagSort)) {
                    for (int i = 0; i < id_list.size(); i++) {
                        String channelsId = (String) id_list.get(i);
                        channelsId = channelsId.replaceAll(" ", "");
                        if (StringTool.isNull(channelsId)) {
                            SitePart data = (SitePart) sitePartManager.getById(Integer.parseInt(channelsId));
                            data.setName(data.getPartName());
                            list.add(data);
                        }
                    }
                }
                if (null != id_list && "sort".equals(tagSort)) {
                    for (int i = 0; i < id_list.size(); i++) {
                        String channelsId = (String) id_list.get(i);
                        channelsId = channelsId.replaceAll(" ", "");
                        if (StringTool.isNull(channelsId)) {
                            SiteContentSort data = (SiteContentSort) siteContentSortManager.getById(Integer.parseInt(channelsId));
                            data.setName(data.getClassifyName());
                            list.add(data);
                        }
                    }
                }
            } catch (Exception exe) {
            }
            return list;
        } else {
            return null;
        }
    }
}
