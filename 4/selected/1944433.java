package freeguide.plugins.storage.xmltv;

import freeguide.common.lib.fgspecific.data.TVChannel;
import freeguide.common.lib.fgspecific.data.TVChannelsSet;
import freeguide.common.lib.fgspecific.data.TVData;
import freeguide.common.lib.fgspecific.data.TVProgramme;
import freeguide.common.lib.importexport.XMLTVImport;
import freeguide.common.plugininterfaces.BaseModule;
import freeguide.common.plugininterfaces.IModuleStorage;

/**
 * XMLTV xml storage loader.
 *
 * @author Alex Buloichik (mailto: alex73 at zaval.org)
 */
public class XMLTVProcessor extends BaseModule {

    protected IModuleStorage.Info cachedInfo;

    /**
     * DOCUMENT_ME!
     *
     * @return DOCUMENT_ME!
     */
    public Object getConfig() {
        return null;
    }

    /**
     * DOCUMENT_ME!
     *
     * @return DOCUMENT_ME!
     */
    public synchronized IModuleStorage.Info getInfo() {
        return cachedInfo;
    }

    /**
     * Load data from external storage in xmltv XML format to
     * in-memory storage.
     *
     * @param channels filter
     * @param minDate DOCUMENT ME!
     * @param maxDate DOCUMENT ME!
     *
     * @return data
     *
     * @throws Exception
     */
    public TVData load(final TVChannelsSet channels, long minDate, long maxDate) throws Exception {
        final TVData result = new TVData();
        return result;
    }

    /**
     * DOCUMENT_ME!
     *
     * @param data DOCUMENT_ME!
     */
    public void save(final TVData data) {
        cachedInfo = null;
    }

    protected void processAllFiles(TVData data, XMLTVImport.Filter filter) throws Exception {
    }

    protected static class GetInfoFilter extends XMLTVImport.Filter {

        protected IModuleStorage.Info info;

        protected GetInfoFilter() {
            info = new IModuleStorage.Info();
            info.channelsList = new TVChannelsSet();
            info.minDate = Long.MAX_VALUE;
            info.maxDate = Long.MIN_VALUE;
        }

        /**
         * DOCUMENT_ME!
         *
         * @param currentChannel DOCUMENT_ME!
         */
        public void performChannelEnd(final TVChannel currentChannel) {
            TVChannelsSet.Channel ch = new TVChannelsSet.Channel(currentChannel.getID(), currentChannel.getDisplayName());
            if (!info.channelsList.contains(ch.getChannelID())) {
                info.channelsList.add(ch);
            }
        }

        /**
         * DOCUMENT_ME!
         *
         * @param programme DOCUMENT_ME!
         *
         * @return DOCUMENT_ME!
         */
        public boolean checkProgrammeStart(TVProgramme programme) {
            if (programme.getStart() < info.minDate) {
                info.minDate = programme.getStart();
            }
            if (programme.getStart() > info.maxDate) {
                info.maxDate = programme.getStart();
            }
            return false;
        }
    }
}
