package com.flazr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.flazr.Packet.Type;

public class WriterStatus {

    private static final Logger logger = LoggerFactory.getLogger(WriterStatus.class);

    private Map<Integer, Integer> channelTimeMap = new ConcurrentHashMap<Integer, Integer>();

    private int videoChannel = -1;

    private double lastLoggedSeconds;

    private int seekTime;

    public WriterStatus(int seekTime) {
        this.seekTime = seekTime;
    }

    public void logFinalVideoDuration() {
        Integer time = channelTimeMap.get(videoChannel);
        if (time == null) {
            logger.warn("video duration is null");
            return;
        }
        logger.info("final video duration: " + (time - seekTime) / 1000 + " seconds, start (seek) time: " + seekTime);
    }

    public int getChannelAbsoluteTime(Header header) {
        final int channelId = header.getChannelId();
        Integer channelTime = channelTimeMap.get(channelId);
        if (channelTime == null) {
            logger.debug("first packet!");
            channelTime = seekTime;
        }
        if (videoChannel == -1 && header.getPacketType() == Type.VIDEO_DATA) {
            videoChannel = channelId;
            logger.info("video channel id is: " + videoChannel);
        }
        if (header.isRelative()) {
            channelTime = channelTime + header.getTime();
        } else {
            channelTime = seekTime + header.getTime();
        }
        channelTimeMap.put(channelId, channelTime);
        if (header.getPacketType() == Type.VIDEO_DATA) {
            logVideoProgress(channelTime);
        }
        return channelTime;
    }

    public void updateVideoChannelTime(int time) {
        if (videoChannel == -1) {
            throw new RuntimeException("video channel id not initialized!");
        }
        channelTimeMap.put(videoChannel, time);
        logVideoProgress(time);
    }

    private void logVideoProgress(int time) {
        if (logger.isDebugEnabled()) {
            logger.debug("time: " + time + ", seek: " + seekTime);
        }
        double seconds = (time - seekTime) / 1000;
        if (seconds >= lastLoggedSeconds + 10) {
            logger.info("video write progress: " + seconds + " seconds");
            lastLoggedSeconds = seconds;
        }
    }
}
