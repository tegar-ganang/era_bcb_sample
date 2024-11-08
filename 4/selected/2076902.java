package com.adpython.service.impl;

import java.util.Date;
import java.util.List;
import com.adpython.dao.ChannelDao;
import com.adpython.domain.Channel;
import com.adpython.service.ChannelService;
import com.adpython.utils.StrUtil;

public class ChannelServiceImpl implements ChannelService {

    private ChannelDao channelDao;

    public Channel getChannelById(Long id) {
        return channelDao.get(id);
    }

    public List<Channel> queryAllChannel() {
        return channelDao.queryAll();
    }

    public Channel queryChannelByName(String name) {
        return channelDao.queryByName(name);
    }

    public void updateChannel(Channel channel) {
        channelDao.update(channel);
    }

    public void initData() {
        List<Channel> list = this.queryAllChannel();
        for (Channel channel : list) {
            channelDao.remove(channel);
        }
        String[] init = { "Home,home,0", "Java,java,10", "Python,python,20", "JavaScript,javascript,30", "Database,database,40", "Logic,logic,50", "Philosophy,philosophy,60", "Language,language,70", "Film,film,80", "Music,music,90", "Mind,mind,100" };
        for (String s : init) {
            Channel channel = new Channel(s.split(",")[1], s.split(",")[0]);
            channel.setRank(StrUtil.parseInt(s.split(",")[2], -1));
            channel.setUpdateTime(new Date());
            channel.setUpdateUserId(1);
            channelDao.save(channel);
        }
    }

    public ChannelDao getChannelDao() {
        return this.channelDao;
    }

    public void setChannelDao(ChannelDao channelDao) {
        this.channelDao = channelDao;
    }
}
