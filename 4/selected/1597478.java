package org.swemas.core;

import java.util.Locale;
import org.swemas.core.kernel.IKernel;
import org.swemas.core.messaging.IMessagingChannel;

/**
 * @author Alexey Chernov
 * 
 */
public abstract class Module implements IChannel {

    protected Module(IKernel kernel) {
        _kernel = kernel;
    }

    protected IKernel kernel() {
        return _kernel;
    }

    @Override
    public String name(Locale locale) {
        try {
            IMessagingChannel imesg = (IMessagingChannel) kernel().getChannel(IMessagingChannel.class);
            return imesg.getText("name", "i18n." + this.getClass().getName(), locale);
        } catch (ModuleNotFoundException m) {
            return this.getClass().getName();
        }
    }

    @Override
    public String description(Locale locale) {
        try {
            IMessagingChannel imesg = (IMessagingChannel) kernel().getChannel(IMessagingChannel.class);
            return imesg.getText("description", "i18n." + this.getClass().getName(), locale);
        } catch (ModuleNotFoundException m) {
            return "Plain Swemas module";
        }
    }

    private IKernel _kernel;
}
