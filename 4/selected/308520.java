package org.bejug.javacareers.feeder;

/**
 * The FeederDaemon config container.
 *
 * @author Stephan Janssen (last modified by $Author: shally $)
 * @version $Revision: 1.3 $ - $Date: 2005/12/20 15:36:45 $
 */
public class FeederDaemonConfig {

    /**
     * Proxy enabled.
     */
    private boolean isProxyEnabled;

    /**
     * Proxy host address.
     */
    private String proxyHost;

    /**
     * Proxy port.
     */
    private String proxyPort;

    /**
     * Number of seconds to wait for feed rescan.
     */
    private int scanCycle;

    /**
     * Number of seconds to wait for feed generation.
     */
    private int generateCycle;

    /**
     * Description to use for RSS feed.
     */
    private String channelDescription;

    /**
     * Link to use for RSS feed.
     */
    private String channelLink;

    /**
     * Title to use for RSS feed.
     */
    private String channelTitle;

    /**
     * Image to use for RSS feed.
     */
    private String channelImage;

    /**
     * Copyright to use for RSS feed.
     */
    private String channelCopyright;

    /**
     * Editor to use for RSS feed.
     */
    private String channelEditor;

    /**
     * Webmaster to use for RSS feed.
     */
    private String channelWebmaster;

    /**
     * Filepath to write RSS feed to.
     */
    private String rssFilepath;

    /**
     * List of keywords to filter feeds with
     */
    private String keywords;

    /**
     * Gets number of seconds to wait for regeneration of RSS file.
     *
     * @return number of seconds to wait for regeneration of RSS file
     */
    public int getGenerateCycle() {
        return generateCycle;
    }

    /**
     * Sets number of seconds to wait for regeneration of RSS file.
     *
     * @param generateCycle number of seconds to wait for regeneration of RSS
     *                      file
     */
    public void setGenerateCycle(int generateCycle) {
        this.generateCycle = generateCycle;
    }

    /**
     * Gets channel description for RSS file.
     *
     * @return channel description for RSS file
     */
    public String getChannelDescription() {
        return channelDescription;
    }

    /**
     * Sets channel description for RSS file.
     *
     * @param channelDescription Description for RSS file
     */
    public void setChannelDescription(String channelDescription) {
        this.channelDescription = channelDescription;
    }

    /**
     * Gets link for RSS file.
     *
     * @return Link for RSS file
     */
    public String getChannelLink() {
        return channelLink;
    }

    /**
     * Sets link for RSS file.
     *
     * @param channelLink Link for RSS file
     */
    public void setChannelLink(String channelLink) {
        this.channelLink = channelLink;
    }

    /**
     * Gets title for RSS file.
     *
     * @return title for RSS file
     */
    public String getChannelTitle() {
        return channelTitle;
    }

    /**
     * Sets title for RSS file.
     *
     * @param channelTitle title for RSS file
     */
    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    /**
     * Gets copyright ozner for RSS file.
     *
     * @return copyright owner for RSS file
     */
    public String getChannelCopyright() {
        return channelCopyright;
    }

    /**
     * Sets copyright owner for RSS file.
     *
     * @param channelCopyright Copyright owner for RSS file
     */
    public void setChannelCopyright(String channelCopyright) {
        this.channelCopyright = channelCopyright;
    }

    /**
     * Gets editor for RSS file.
     *
     * @return Gets editor for RSS file
     */
    public String getChannelEditor() {
        return channelEditor;
    }

    /**
     * Sets channel editor for RSS file.
     *
     * @param channelEditor Editor for RSS file
     */
    public void setChannelEditor(String channelEditor) {
        this.channelEditor = channelEditor;
    }

    /**
     * Gets webmaster for RSS file.
     *
     * @return webmaster for RSS file
     */
    public String getChannelWebmaster() {
        return channelWebmaster;
    }

    /**
     * Sets webmaster for RSS file.
     *
     * @param channelWebmaster Webmaster for RSS file
     */
    public void setChannelWebmaster(String channelWebmaster) {
        this.channelWebmaster = channelWebmaster;
    }

    /**
     * @return Returns the proxyHost.
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * @param proxyHost The proxyHost to set.
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**
     * @return Returns the proxyPort.
     */
    public String getProxyPort() {
        return proxyPort;
    }

    /**
     * @param proxyPort The proxyPort to set.
     */
    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * @return Returns the scanCycle.
     */
    public int getScanCycle() {
        return scanCycle;
    }

    /**
     * @param scanCycle The scanCycle to set.
     */
    public void setScanCycle(int scanCycle) {
        this.scanCycle = scanCycle;
    }

    /**
     * @return Returns the isProxyEnabled.
     */
    public boolean isProxyEnabled() {
        return isProxyEnabled;
    }

    /**
     * @param isProxyEnabled The isProxyEnabled to set.
     */
    public void setProxyEnabled(boolean isProxyEnabled) {
        this.isProxyEnabled = isProxyEnabled;
    }

    /**
     * Gets path to write the generated RSS to.
     *
     * @return path to write RSS file to
     */
    public String getRssFilepath() {
        return rssFilepath;
    }

    /**
     * Sets path to write the generated RSS to.
     *
     * @param rssFilepath to write RSS file to
     */
    public void setRssFilepath(String rssFilepath) {
        this.rssFilepath = rssFilepath;
    }

    /**
     * @return keywords
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * @param keywords String
     */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /**
     * 
     * @return ChannelImage
     */
    public String getChannelImage() {
        return channelImage;
    }

    /**
     * 
     * @param channelImage is the channelImage to be set
     */
    public void setChannelImage(String channelImage) {
        this.channelImage = channelImage;
    }
}
