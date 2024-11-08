package org.eiichiro.gig.appengine;

import java.util.HashSet;
import java.util.Set;
import org.eiichiro.jazzmaster.service.Provider;
import org.eiichiro.jazzmaster.service.Service;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;

/**
 * {@code AppEngineChannelService} is a service definition of App Engine's 
 * {@code ChannelService} API.
 *
 * @author <a href="mailto:eiichiro@eiichiro.org">Eiichiro Uchiumi</a>
 */
public class AppEngineChannelService extends Service {

    /** Returns {@code ChannelService}. */
    @Override
    public Set<Class<?>> getInterfaces() {
        Set<Class<?>> interfaces = new HashSet<Class<?>>(1);
        interfaces.add(ChannelService.class);
        return interfaces;
    }

    /**
	 * Just returns <code>null</code>.
	 * Implementation class is 
	 * <code>com.google.appengine.api.channel.ChannelServiceImpl</code>.
	 * However, this method cannot return it because it is nonpublic.
	 * 
	 * @see org.eiichiro.jazzmaster.service.Service#getImplementation()
	 */
    @Override
    public Class<?> getImplementation() {
        return null;
    }

    /** Returns {@code Provider} for {@code ChannelService}. */
    @Override
    @SuppressWarnings("unchecked")
    public Provider<ChannelService> getProvider() {
        return new Provider<ChannelService>() {

            @Override
            public ChannelService get() {
                ChannelService channelService = ChannelServiceFactory.getChannelService();
                return channelService;
            }
        };
    }
}
