package com.tcs.hrr.service.impl;

import java.util.List;
import com.tcs.hrr.dao.ChannelSourceDAO;
import com.tcs.hrr.service.ChannelSourceManager;

public class ChannelSourceManagerImpl implements ChannelSourceManager {

    ChannelSourceDAO channelSourceDAO;

    public ChannelSourceDAO getChannelSourceDAO() {
        return channelSourceDAO;
    }

    public void setChannelSourceDAO(ChannelSourceDAO channelSourceDAO) {
        this.channelSourceDAO = channelSourceDAO;
    }

    @Override
    public List findChannelSourceAll() {
        return this.channelSourceDAO.findAll();
    }

    @Override
    public List findByProperty(String obecjName, Object value) {
        return this.channelSourceDAO.findByProperty(obecjName, value);
    }
}
