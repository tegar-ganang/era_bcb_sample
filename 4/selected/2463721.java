package com.google.code.guidatv.server.service.rest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import org.restlet.resource.ServerResource;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.code.guidatv.client.service.ChannelService;
import com.google.code.guidatv.client.service.impl.ChannelServiceImpl;
import com.google.code.guidatv.model.LoginInfo;
import com.google.code.guidatv.rest.LoginInfoResource;
import com.google.code.guidatv.server.model.PMF;
import com.google.code.guidatv.server.model.PreferredChannels;

public class LoginInfoServerResource extends ServerResource implements LoginInfoResource {

    public static final String REQUEST_URI_PARAM_NAME = "requestUri";

    private ChannelService channelService = new ChannelServiceImpl();

    @Override
    public LoginInfo retrieve() {
        String requestUri = getRequest().getResourceRef().getQueryAsForm().getValues(REQUEST_URI_PARAM_NAME);
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

    private PreferredChannels getPreferredChannels(User user) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            return getPreferredChannels(user, pm);
        } finally {
            pm.close();
        }
    }

    @SuppressWarnings("unchecked")
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
