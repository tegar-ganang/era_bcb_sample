package org.swemas.core.messaging;

import java.util.Locale;
import org.swemas.core.Module;
import org.swemas.core.ModuleNotFoundException;
import org.swemas.core.config.IConfigProvidingChannel;
import org.swemas.core.config.SwConfigurationFaultException;
import org.swemas.core.kernel.IKernel;

/**
 * @author Alexey Chernov
 * 
 */
public class SwLocaleProvider extends Module implements ILocaleProvidingChannel {

    /**
	 * @param kernel
	 */
    public SwLocaleProvider(IKernel kernel) {
        super(kernel);
        try {
            IConfigProvidingChannel iconf = (IConfigProvidingChannel) kernel.getChannel(IConfigProvidingChannel.class);
            String dlocale = iconf.getItem("i18n.locale");
            String[] spl = dlocale.split("_");
            String lang = spl[0], country = spl[1], variant = spl[2];
            if (lang != null && country != null && variant != null) _locale = new Locale(lang, country, variant); else if (lang != null && country != null) _locale = new Locale(lang, country); else if (lang != null) _locale = new Locale(lang); else _locale = new Locale("en", "US");
        } catch (ModuleNotFoundException e) {
            _locale = new Locale("en", "US");
        } catch (SwConfigurationFaultException e) {
            _locale = new Locale("en", "US");
        }
    }

    @Override
    public void changeLocale(Locale locale) {
        _locale = locale;
        try {
            IConfigProvidingChannel iconf = (IConfigProvidingChannel) kernel().getChannel(IConfigProvidingChannel.class);
            iconf.setItem("i18n.locale", locale.toString());
        } catch (ModuleNotFoundException e) {
        } catch (SwConfigurationFaultException e) {
        }
    }

    @Override
    public Locale getCurrentLocale() {
        return _locale;
    }

    private Locale _locale;
}
