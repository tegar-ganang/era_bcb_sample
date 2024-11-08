package freeguide.plugins.ui.vertical.simple.filter;

import freeguide.common.lib.fgspecific.data.TVProgramme;
import java.util.ArrayList;

/**
 * Filter programs by channel. If there are no channels set, all programs
 * are shown.
 *
 * @author Christian Weiske (cweiske at cweiske.de)
 */
public class ChannelFilter extends ProgrammeFilter {

    protected ArrayList arAllowedChannelIds = new ArrayList();

    /**
     * DOCUMENT_ME!
     *
     * @param strChannelId DOCUMENT_ME!
     */
    public void addChannel(String strChannelId) {
        addChannel(strChannelId, true);
    }

    /**
     * DOCUMENT_ME!
     *
     * @param strChannelId DOCUMENT_ME!
     * @param notify DOCUMENT_ME!
     */
    public void addChannel(String strChannelId, boolean notify) {
        this.arAllowedChannelIds.add(strChannelId);
        if (notify) {
            this.notifyFilterChange();
        }
    }

    /**
     * DOCUMENT_ME!
     *
     * @param strChannelId DOCUMENT_ME!
     */
    public void removeChannel(String strChannelId) {
        removeChannel(strChannelId, true);
    }

    /**
     * DOCUMENT_ME!
     *
     * @param strChannelId DOCUMENT_ME!
     * @param notify DOCUMENT_ME!
     */
    public void removeChannel(String strChannelId, boolean notify) {
        this.arAllowedChannelIds.remove(strChannelId);
        if (notify) {
            this.notifyFilterChange();
        }
    }

    /**
     * DOCUMENT_ME!
     */
    public void removeAllChannels() {
        this.removeAllChannels(true);
    }

    /**
     * DOCUMENT_ME!
     *
     * @param notify DOCUMENT_ME!
     */
    public void removeAllChannels(boolean notify) {
        this.arAllowedChannelIds.clear();
        if (notify) {
            this.notifyFilterChange();
        }
    }

    /**
     * DOCUMENT_ME!
     *
     * @param programme DOCUMENT_ME!
     *
     * @return DOCUMENT_ME!
     */
    public boolean showProgramme(TVProgramme programme) {
        return (this.arAllowedChannelIds.size() == 0) || this.arAllowedChannelIds.contains(programme.getChannel().getID());
    }
}
