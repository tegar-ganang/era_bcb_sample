package com.wwg.cms.service.impl;

import java.util.Collection;
import com.wwg.cms.bo.*;
import com.wwg.cms.service.*;
import com.css.framework.dao.GeneralDao;
import com.wwg.cms.bo.entity.*;

/**
 * 
 * create by wwl
 * 栏目表维护 
 * @author wwl
 */
public class ChannelServiceImpl implements ChannelService {

    private GeneralDao generalDao;

    public void setGeneralDao(GeneralDao generalDao) {
        this.generalDao = generalDao;
    }

    /**
	 *获取所有栏目表维护 
	 */
    public Collection getChannelList() {
        String hsql = "select ent from ChannelEntity as ent ";
        return generalDao.find(hsql);
    }

    /**
	 * 添加栏目表维护 
	 * @param channel 栏目表维护 
	 * @return
	 */
    public Channel addChannel(Channel channel) {
        generalDao.save(channel);
        return channel;
    }

    /**
	 * 修改栏目表维护 
	 * @param channel 栏目表维护 
	 * @return
	 */
    public Channel updateChannel(Channel channel) {
        generalDao.saveOrUpdate(channel);
        return channel;
    }

    /**
	 * 删除栏目表维护 
	 * @param channel 栏目表维护 
	 * @return
	 */
    public Channel deleteChannel(Channel channel) {
        generalDao.delete(channel);
        return channel;
    }

    /**
     *  获取栏目表维护  by id
     *  @param id 编号
     * @return
	 */
    public Channel getChannelById(Long id) {
        return (Channel) generalDao.fetch(id, ChannelEntity.class);
    }
}
