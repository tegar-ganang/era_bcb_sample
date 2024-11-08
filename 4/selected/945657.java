package com.busfm;

import com.busfm.media.PlayController;
import com.busfm.media.PlayControllerListener;
import com.busfm.model.ChannelEntity;
import com.busfm.model.ChannelList;
import com.busfm.model.PlayList;
import com.busfm.util.PrefUtil;
import android.app.Application;

/**
 * <p>
 * Title:busfmApplication
 * </p>
 * <p>
 * Description: busfmApplication
 * </p>
 * <p>
 * Copyright (c) 2011 www.bus.fm Inc. All rights reserved.
 * </p>
 * <p>
 * Company: bus.fm
 * </p>
 * 
 * 
 * @author jingguo0@gmail.com
 * 
 */
public class BusfmApplication extends Application {

    private PlayList mPlaylist;

    private static BusfmApplication instance;

    private PlayControllerListener mPlayControllerListener;

    private ChannelList mChannelList;

    private int currentID = -1;

    private PlayController mServicePlayerEngine;

    private ChannelEntity mPriaveChannelEntity;

    private int mCurrentDuration = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        PrefUtil.init(this);
    }

    public static BusfmApplication getInstance() {
        return instance;
    }

    public void setPlayList(PlayList playList) {
        mPlaylist = playList;
    }

    public PlayList getPlayList() {
        return mPlaylist;
    }

    public void addPlayList(PlayList playList) {
        if (null != mPlaylist) {
            mPlaylist.addList(playList.getPlayList());
        }
    }

    public void setPlayerListener(PlayControllerListener playControllerListener) {
        mPlayControllerListener = playControllerListener;
    }

    public PlayControllerListener getPlayerListener() {
        return mPlayControllerListener;
    }

    public void setChannelList(ChannelList channelList) {
        mChannelList = channelList;
    }

    public ChannelList getChannelList() {
        return mChannelList;
    }

    public void setCurrentID(int currentID) {
        this.currentID = currentID;
    }

    public int getCurrentID() {
        return currentID;
    }

    public void setConcretePlayerEngine(PlayController playerEngine) {
        mServicePlayerEngine = playerEngine;
    }

    public PlayController getConcretePlayerEngine() {
        return mServicePlayerEngine;
    }

    public void setPrivateEntitiy(ChannelEntity privateEntity) {
        mPriaveChannelEntity = privateEntity;
    }

    public ChannelEntity getPrivateEntity() {
        return mPriaveChannelEntity;
    }

    public void setDuration(int duration) {
        mCurrentDuration = duration;
    }

    public int getDuration() {
        return mCurrentDuration;
    }
}
