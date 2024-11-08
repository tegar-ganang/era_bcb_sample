package com.tcs.hrr.service.impl;

import java.util.List;
import com.tcs.hrr.dao.ChannelDAO;
import com.tcs.hrr.domain.Channel;
import com.tcs.hrr.service.ChannelManager;

public class ChannelManagerImpl implements ChannelManager {

    ChannelDAO channelDAO;

    public ChannelDAO getChannelDAO() {
        return channelDAO;
    }

    public void setChannelDAO(ChannelDAO channelDAO) {
        this.channelDAO = channelDAO;
    }

    @Override
    public List findChannelAll() {
        return this.channelDAO.findAll();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
