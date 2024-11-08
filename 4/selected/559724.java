package com.business.service.cms;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springside.modules.orm.Page;
import org.springside.modules.orm.PropertyFilter;
import com.business.dao.cms.ChannelDao;
import com.business.entity.cms.Channel;

@Component
@Transactional
public class ChannelService {

    private static Logger logger = LoggerFactory.getLogger(ChannelService.class);

    private ChannelDao ChannelDao;

    /**
	 * get object by id
	 */
    @Transactional(readOnly = true)
    public Channel getChannel(Long id) {
        return ChannelDao.get(id);
    }

    /**
	 * update or save object
	 * @param entity
	 */
    public void saveChannel(Channel entity) {
        ChannelDao.save(entity);
    }

    /**
	 * delete object
	 */
    public void deleteChannel(Long id) {
        ChannelDao.delete(id);
    }

    /**
	 * find object by filters
	 */
    @Transactional(readOnly = true)
    public Page<Channel> searchChannel(final Page<Channel> page, final List<PropertyFilter> filters) {
        return ChannelDao.findPage(page, filters);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        ChannelService.logger = logger;
    }

    @Autowired
    public ChannelDao getChannelDao() {
        return ChannelDao;
    }
}
