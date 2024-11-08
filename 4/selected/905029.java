package gov.sns.apps.diagnostics.corede.corede;

import gov.sns.ca.*;

public class Utilities {

    private static ChannelFactory channelFactory;

    public static synchronized ChannelFactory getChannelFactory() {
        if (channelFactory == null) {
            System.setProperty("gov.sns.jca.Context", "com.cosylab.epics.caj.CAJContext");
            channelFactory = ChannelFactory.defaultFactory();
            channelFactory.init();
        }
        return channelFactory;
    }
}
