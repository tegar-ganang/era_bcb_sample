package net.deytan.wofee.gwt.server;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import net.deytan.wofee.gwt.bean.UserFeedBean;
import net.deytan.wofee.gwt.client.GWTUpdateService;
import net.deytan.wofee.gwt.convert.UserFeedConverter;
import net.deytan.wofee.persistable.User;
import net.deytan.wofee.persistable.UserFeed;
import net.deytan.wofee.persistable.UserFeedItem;
import net.deytan.wofee.service.UpdateChannel;
import net.deytan.wofee.service.UpdateService;
import net.deytan.wofee.service.bean.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.SerializationPolicy;

public class GWTUpdateServiceImpl extends AbstractGWTService implements GWTUpdateService, UpdateChannel {

    private static final long serialVersionUID = 1L;

    private UpdateService updateService;

    private ChannelService channelService;

    private UserFeedConverter userFeedConverter;

    private Method serviceMethod;

    private SerializationPolicy serializationPolicy;

    public GWTUpdateServiceImpl() {
        try {
            this.serviceMethod = GWTUpdateServiceImpl.class.getDeclaredMethod("dummy");
        } catch (SecurityException exception) {
        } catch (NoSuchMethodException exception) {
        }
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        this.channelService = ChannelServiceFactory.getChannelService();
        this.updateService.addChannel(this);
    }

    @Override
    public String processCall(final String payload) throws SerializationException {
        if (this.serializationPolicy == null) {
            final RPCRequest rpcRequest = RPC.decodeRequest(payload, this.getClass(), this);
            this.serializationPolicy = rpcRequest.getSerializationPolicy();
        }
        return super.processCall(payload);
    }

    @Override
    public String connect() {
        return this.channelService.createChannel(this.getUserKey());
    }

    @Override
    public String disconnect() {
        return AbstractGWTService.RETURN_OK;
    }

    @Override
    public void update(final UpdateResult updateResult) {
        for (Map.Entry<UserFeed, List<UserFeedItem>> entry : updateResult.getModifiedUserFeeds().entrySet()) {
            final UserFeed userFeed = entry.getKey();
            final Long themeKey = userFeed.getTheme().getKey();
            final UserFeedBean feedBean = this.userFeedConverter.convert(themeKey, userFeed, entry.getValue());
            this.send(userFeed.getTheme().getUser(), feedBean);
        }
    }

    private void send(final User user, final UserFeedBean feedBean) {
        String serialized = null;
        try {
            serialized = RPC.encodeResponseForSuccess(this.serviceMethod, feedBean, this.serializationPolicy);
        } catch (SerializationException exception) {
            exception.printStackTrace();
        }
        try {
            ChannelServiceFactory.getChannelService().sendMessage(new ChannelMessage(user.getKey(), serialized));
        } catch (IllegalArgumentException exception) {
            System.out.println("Serialized length:" + serialized.length());
            System.out.println(serialized);
        }
    }

    /**
	 * Dummy method for rpc serialisation.
	 * 
	 * @return object from class we want to serialize
	 */
    public UserFeedBean dummy() {
        return null;
    }

    @Autowired
    @Required
    public void setUpdateService(final UpdateService updateService) {
        this.updateService = updateService;
    }

    @Autowired
    @Required
    public void setUserFeedConverter(final UserFeedConverter userFeedConverter) {
        this.userFeedConverter = userFeedConverter;
    }
}
