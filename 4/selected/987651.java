package cn.mmbook.platform.dao.manage;

import java.util.*;
import javacommon.base.*;
import javacommon.util.*;
import cn.org.rapid_framework.util.*;
import cn.org.rapid_framework.web.util.*;
import cn.org.rapid_framework.page.*;
import cn.org.rapid_framework.page.impl.*;
import cn.mmbook.platform.model.manage.*;
import cn.mmbook.platform.dao.manage.*;
import cn.mmbook.platform.service.manage.impl.*;
import cn.mmbook.platform.service.manage.*;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * <p> SiteChannels DAO层,数据映射处理<br>
 * <p>   <br>
 * @author admin,
 * @version 1.0. 2010-07-08
 *
 */
@Component
public class SiteChannelsDao extends BaseIbatis3Dao<SiteChannels, java.lang.Integer> {

    public Class getEntityClass() {
        return SiteChannels.class;
    }

    public void saveOrUpdate(SiteChannels entity) {
        if (entity.getId() == null) save(entity); else update(entity);
    }

    public Page findByPageRequest(PageRequest pageRequest) {
        return pageQuery("SiteChannels.pageSelect", pageRequest);
    }

    public List getList(SiteChannels u) {
        return getSqlSessionTemplate().selectList("SiteChannels.getSiteChannelsList", u);
    }

    /**
	 * 
	 * 要自己写出 查询关联表，及记录数 
	 * @param pageRequest
	 * @return
	 */
    public Page listPageAnyTable(PageRequest pageRequest) {
        return pageQuery("SiteChannels.anytablelist", "SiteChannels.anytablecount", pageRequest);
    }

    /** 
	 * 门面类查询没有分页 arye
	 */
    public List<SiteChannels> findSiteChannelsDoor(Map paraMap) {
        return getSqlSessionTemplate().selectList("SiteChannels.findSiteChannelsDoor", paraMap);
    }

    /**
	 * 频道列表数据
	 * arye
	 * @param pageRequest
	 * @return
	 */
    public Page getChannelsByPartId(PageRequest pageRequest) {
        return pageQueryMysql("SiteChannels.getChannelsByPartIdlist", "SiteChannels.getCannelsByPartIdcount", pageRequest);
    }
}
