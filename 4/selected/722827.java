package com.google.code.guidatv.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.code.guidatv.client.ScheduleRemoteService;
import com.google.code.guidatv.model.Channel;
import com.google.code.guidatv.model.LoginInfo;
import com.google.code.guidatv.model.Schedule;
import com.google.code.guidatv.client.service.ChannelService;
import com.google.code.guidatv.client.service.impl.ChannelServiceImpl;
import com.google.code.guidatv.server.model.PMF;
import com.google.code.guidatv.server.model.PreferredChannels;
import com.google.code.guidatv.server.service.ScheduleService;
import com.google.code.guidatv.server.service.impl.ScheduleServiceImpl;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class ScheduleRemoteServiceServlet extends RemoteServiceServlet implements ScheduleRemoteService {

    private ScheduleService service = new ScheduleServiceImpl();

    private ChannelService channelService = new ChannelServiceImpl();

    @Override
    public LoginInfo getLoginInfo(String requestUri) {
        LoginInfo info;
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        if (user != null) {
            PreferredChannels preferredChannels = getPreferredChannels(user);
            Set<String> channels;
            if (preferredChannels != null) {
                channels = new HashSet<String>(preferredChannels.getChannels());
            } else {
                channels = channelService.getDefaultSelectedChannels();
            }
            info = new LoginInfo(user.getNickname(), userService.createLogoutURL(requestUri), "Esci", channels);
        } else {
            info = new LoginInfo(null, userService.createLoginURL(requestUri), "Entra", channelService.getDefaultSelectedChannels());
        }
        return info;
    }

    @Override
    public List<Schedule> getDaySchedule(Date day, Set<String> channels) {
        List<Schedule> retValue = new ArrayList<Schedule>();
        for (String channelCode : channels) {
            Channel channel = channelService.getChannelByCode(channelCode);
            if (channel != null) {
                retValue.add(service.getSchedule(channel, day));
            }
        }
        return retValue;
    }

    @Override
    public void savePreferredChannels(Set<String> channels) {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            PreferredChannels preferredChannels = getPreferredChannels(user, pm);
            if (preferredChannels == null) {
                preferredChannels = new PreferredChannels(user);
            }
            Set<String> currentChannels = preferredChannels.getChannels();
            if (currentChannels != null) {
                currentChannels.clear();
            } else {
                currentChannels = new HashSet<String>();
                preferredChannels.setChannels(currentChannels);
            }
            currentChannels.addAll(channels);
            pm.makePersistent(preferredChannels);
        } finally {
            pm.close();
        }
    }

    private PreferredChannels getPreferredChannels(User user) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            return getPreferredChannels(user, pm);
        } finally {
            pm.close();
        }
    }

    private PreferredChannels getPreferredChannels(User user, PersistenceManager pm) {
        List<PreferredChannels> storedChannels;
        PreferredChannels preferredChannels = null;
        Query query = pm.newQuery(PreferredChannels.class);
        query.setFilter("user == userParam");
        query.declareParameters(User.class.getName() + " userParam");
        storedChannels = (List<PreferredChannels>) query.execute(user);
        if (storedChannels != null && !storedChannels.isEmpty()) {
            preferredChannels = storedChannels.iterator().next();
        }
        return preferredChannels;
    }
}
