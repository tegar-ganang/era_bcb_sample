package com.kyte.api.service;

/**
 * Provides access to instances of 'service' Objects.
 *
 * Created: elliott, Mar 12, 2008
 */
public class ServiceFactory {

    protected static UtilService theUtilService = new UtilService();

    protected static UserService theUserService = new UserService();

    protected static ChannelService theChannelService = new ChannelService();

    protected static MediaService theMediaService = new MediaService();

    public static UtilService getUtilService() {
        return theUtilService;
    }

    public static UserService getUserService() {
        return theUserService;
    }

    public static ChannelService getChannelService() {
        return theChannelService;
    }

    public static MediaService getMediaService() {
        return theMediaService;
    }
}
