package cn.mmbook.platform.service.manage.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javacommon.base.BaseManager;
import javacommon.base.EntityDao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import cn.mmbook.platform.dao.manage.SiteChannelsDao;
import cn.mmbook.platform.dao.manage.SitePartDao;
import cn.mmbook.platform.model.manage.SiteChannels;
import cn.mmbook.platform.model.manage.SiteChannelsPartReal;
import cn.mmbook.platform.service.manage.SiteChannelsManager;
import cn.mmbook.platform.service.manage.SiteChannelsPartRealManager;
import cn.org.rapid_framework.page.Page;
import cn.org.rapid_framework.page.PageRequest;

/**
 * <p>
 * SiteChannels server类,实现功能,事物处理<br>
 * <p>
 * <br>
 * 
 * @author admin,
 * @version 1.0. 2010-07-08
 * 
 */
@Component("siteChannelsManager")
@Transactional
public class SiteChannelsManagerImpl extends BaseManager<SiteChannels, java.lang.Integer> implements SiteChannelsManager {

    private SiteChannelsDao siteChannelsDao;

    private SitePartDao sitePartDao;

    /** 增加setXXXX()方法,spring就可以通过autowire自动设置对象属性 */
    private SiteChannelsPartRealManager siteChannelsPartRealManager;

    /** 增加setXXXX()方法,spring就可以通过autowire自动设置对象属性 */
    public void setSiteChannelsDao(SiteChannelsDao dao) {
        this.siteChannelsDao = dao;
    }

    public void setSitePartDao(SitePartDao dao) {
        this.sitePartDao = dao;
    }

    /** 获取DAO */
    public void setSiteChannelsPartRealManager(SiteChannelsPartRealManager manager) {
        this.siteChannelsPartRealManager = manager;
    }

    /** 获取DAO */
    public EntityDao getEntityDao() {
        return this.siteChannelsDao;
    }

    /** 单表分页查询 */
    @Transactional(readOnly = true)
    public Page findByPageRequest(PageRequest pr) {
        return siteChannelsDao.findByPageRequest(pr);
    }

    /**
	 * 自定义实现保存 1：保存频道数据 2：保存频道关联的栏目数据
	 */
    public void save(SiteChannels u) {
        siteChannelsDao.save(u);
        String channelsId = getEntityDao().getMaxId("SiteChannels.getMaxId");
        SiteChannelsPartReal siteChannelsPartReal = new SiteChannelsPartReal();
        siteChannelsPartReal.setChannelsId(Integer.parseInt(channelsId));
        siteChannelsPartRealManager.removeByInfo(siteChannelsPartReal);
        String psrtIds = u.getPartIds();
        String[] arrayId = psrtIds.split(",");
        for (int i = 0; null != arrayId && i < arrayId.length; i++) {
            SiteChannelsPartReal cxl = new SiteChannelsPartReal();
            cxl.setChannelsId(Integer.parseInt(channelsId));
            cxl.setPartId(Integer.parseInt(arrayId[i]));
            siteChannelsPartRealManager.save(cxl);
        }
    }

    /**
	 * 自定义实现修改 1：修改频道数据 2：修改频道关联的栏目数据
	 */
    public void update(SiteChannels u) {
        siteChannelsDao.update(u);
        int channelsId = u.getId();
        SiteChannelsPartReal siteChannelsPartReal = new SiteChannelsPartReal();
        siteChannelsPartReal.setChannelsId(channelsId);
        siteChannelsPartRealManager.removeByInfo(siteChannelsPartReal);
        String psrtIds = u.getPartIds();
        String[] arrayId = psrtIds.split(",");
        for (int i = 0; null != arrayId && i < arrayId.length; i++) {
            SiteChannelsPartReal cxl = new SiteChannelsPartReal();
            cxl.setChannelsId(channelsId);
            cxl.setPartId(Integer.parseInt(arrayId[i]));
            siteChannelsPartRealManager.save(cxl);
        }
    }

    /**
	 * 自定义实现删除 1：删除频道数据 2：删除频道关联的栏目数据void removeById (java.lang.Integer id);
	 */
    public void removeById(java.lang.Integer id) {
        int channelsId = (int) id;
        super.removeById(channelsId);
        SiteChannelsPartReal siteChannelsPartReal = new SiteChannelsPartReal();
        siteChannelsPartReal.setChannelsId(channelsId);
        siteChannelsPartRealManager.removeByInfo(siteChannelsPartReal);
    }

    public List getList(SiteChannels u) {
        return siteChannelsDao.getList(u);
    }

    /** 多表分页查询 */
    @Transactional(readOnly = true)
    public Page listPageAnyTable(PageRequest pr) {
        return siteChannelsDao.listPageAnyTable(pr);
    }

    /**
	 * 查询 频道列表数据
	 * 
	 * @param map
	 *            String sortColums 1: 按ID排序 2,按排序值(sortValue),3 随机 String
	 *            siteId 频道ID String page 1 分页 2 不分页 String count 返回记录数
	 * @return Map String size 符合条件记录总数 List<SiteChannels> 结果集
	 */
    public Page getSiteChannelList(Map map) {
        PageRequest pageRequest = new PageRequest();
        Map map_param = new HashMap();
        String sitePartId = (String) map.get("sitePartId");
        if (javacommon.util.StringTool.isNull(sitePartId)) {
            map_param.put("sitePartId", sitePartId);
            pageRequest.setFilters(map_param);
        }
        String count = (String) map.get("count");
        int limit = 10;
        if (javacommon.util.StringTool.isNull(count)) {
            limit = Integer.parseInt(count);
        }
        String start = (String) map.get("start");
        if (javacommon.util.StringTool.isNull(start)) {
            if (limit == 0) {
                pageRequest.setPageNumber(1);
            } else {
                int pageIndex = Integer.parseInt(start);
                pageRequest.setPageNumber(pageIndex / limit + 1);
            }
        } else {
            pageRequest.setPageNumber(1);
        }
        pageRequest.setPageSize(limit);
        String sort = (String) map.get("sort");
        if (javacommon.util.StringTool.isNull(sort)) {
            pageRequest.setSortColumns(sort);
        }
        Page page = siteChannelsDao.getChannelsByPartId(pageRequest);
        System.out.println("page.getPageSize()=" + page.getPageSize());
        List listChannels = (List) page.getResult();
        System.out.println("listChannels.size()=" + listChannels.size());
        return page;
    }

    /**
	 * 查询 网站对应的 所有频道及 频道对应的一级栏目
	 * 
	 * @param map
	 *            String siteId 网站ID
	 * @return Map String size 符合条件记录总数 List<SiteChannels> 结果集
	 */
    public List getAllChannelPart(Map map) {
        System.out.println("getAllChannelPart begin!!!");
        List<SiteChannels> listChannel = new ArrayList<SiteChannels>();
        Map paraMap = new HashMap();
        List<SiteChannels> siteChannelsList = siteChannelsDao.findSiteChannelsDoor(map);
        System.out.println("siteChannelsList.size = " + siteChannelsList.size());
        for (int i = 0; null != siteChannelsList && i < siteChannelsList.size(); i++) {
            SiteChannels info = (SiteChannels) siteChannelsList.get(i);
            if (null != info) {
                Map paraMap2 = new HashMap();
                paraMap2.put("channelsId", info.getId());
                List listPart = sitePartDao.getTagSitePartList(paraMap2);
                if (null != listPart) {
                    info.setSitePartList(listPart);
                }
            }
            listChannel.add(info);
        }
        return listChannel;
    }

    /**
	 * 按ID 查询频道对象
	 * */
    public SiteChannels getById(java.lang.Integer id) {
        return (SiteChannels) siteChannelsDao.getById(id);
    }

    /**
	 * 查询所有频道数据
	 */
    public List findAll() {
        return siteChannelsDao.findAll();
    }
}
