package net.walend.somnifugi;

import java.util.Map;
import java.util.HashMap;
import javax.naming.Context;
import javax.naming.NamingException;

/**
ChannelFactoryCache holds a map of Context names to ChannelFactories. This class uses an internal MultiplexChannelFactory. You can set properties for it in the Context to control which ChannelFactories get used for what destinations.

@author @dwalend@
 */
class ChannelFactoryCache {

    public static final ChannelFactoryCache IT = new ChannelFactoryCache();

    private Map contextNamesToChannelFactories = new HashMap();

    private final Object guard = new Object();

    private ChannelFactoryCache() {
    }

    ChannelFactory getChannelFactoryForContext(Context context) throws NamingException {
        String key = context.getNameInNamespace();
        synchronized (guard) {
            if (containsChannelFactory(key)) {
                return (ChannelFactory) contextNamesToChannelFactories.get(key);
            } else {
                ChannelFactory channelFactory = new MultiplexChannelFactory();
                contextNamesToChannelFactories.put(key, channelFactory);
                SomniLogger.IT.config("Added " + key + " ChannelFactory.");
                return channelFactory;
            }
        }
    }

    boolean containsChannelFactory(String key) {
        synchronized (guard) {
            return contextNamesToChannelFactories.containsKey(key);
        }
    }
}
