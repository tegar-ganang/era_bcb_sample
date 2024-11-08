package com.goodcodeisbeautiful.syndic8.rss20;

import java.util.ArrayList;
import com.goodcodeisbeautiful.syndic8.Syndic8Exception;

/**
 * Root instance for rss 2.0.
 * 
 */
public class Rss20 {

    /** channel */
    private Rss20Channel m_channel;

    /** modules */
    private ArrayList<Rss20Module> m_modules;

    /**
     * Default constructor.
     */
    public Rss20() {
    }

    /**
     * Get channel.
     * @return channel instance.
     */
    public Rss20Channel getChannel() {
        return m_channel;
    }

    /**
     * Get modules if it is set.
     * @return modules instance or null.
     */
    public Rss20Module[] getModules() {
        return m_modules != null ? m_modules.toArray(new Rss20Module[m_modules.size()]) : null;
    }

    /**
     * Add a new module.
     * @param module is a new module to be added.
     * @exception Syndic8Exception is thrown if some error happened.
     */
    public void addModule(Rss20Module module) throws Syndic8Exception {
        if (m_modules == null) {
            m_modules = new ArrayList<Rss20Module>();
        }
        if (!m_modules.contains(module)) m_modules.add(module);
    }

    /**
     * Set a new channel.
     * @param channel is a new channel.
     * @exception Syndic8Exception is thrown if some error happened.
     */
    public void setChannel(Rss20Channel channel) throws Syndic8Exception {
        m_channel = channel;
    }
}
